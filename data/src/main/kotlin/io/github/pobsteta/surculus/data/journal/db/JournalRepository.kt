package io.github.pobsteta.surculus.data.journal.db

import io.github.pobsteta.surculus.data.journal.CleProjection
import io.github.pobsteta.surculus.data.journal.EtatCle
import io.github.pobsteta.surculus.data.journal.GenerateurUuidV7
import io.github.pobsteta.surculus.data.journal.Horloge
import io.github.pobsteta.surculus.data.journal.LigneJournal
import io.github.pobsteta.surculus.data.journal.Projection
import io.github.pobsteta.surculus.data.journal.TypeLigne
import java.util.concurrent.Callable

/** Horloges de l'appareil, injectées pour que les tests rejouent coupures et sauts d'horloge. */
interface SourceHorloge {
    /** Heure du téléphone, ms Unix UTC. */
    fun maintenantUtcMs(): Long

    /** Horloge monotone (`SystemClock.elapsedRealtime`), ms depuis le démarrage de l'appareil. */
    fun monotoneMs(): Long

    /** Numéro de démarrage : change quand l'horloge monotone repart de zéro. */
    fun demarrage(): Long
}

/** Dernier fix GNSS connu : son heure (UTC) et l'instant monotone où il a été reçu. */
data class FixGnss(val heureUtcMs: Long, val monotoneMs: Long)

sealed interface Reception {
    data object Ajoutee : Reception
    data object Doublon : Reception
    data object Refusee : Reception

    /** Type hors de la liste fermée de la spec § 5 : la ligne n'est pas écrite. */
    data object TypeInvalide : Reception
}

/**
 * Écriture et lecture du journal (ADR 0005). Les méthodes sont **bloquantes** : à appeler hors du fil
 * principal. Chaque ajout est **une transaction** (ligne, éventuel saut d'horloge, projection des clés
 * touchées) : tout ou rien.
 *
 * Un `remplace_uuid` doit désigner une ligne du **même objet** et du **même type**, et une `annulation`
 * porte l'objet de la ligne annulée (spec § 5). Une saisie qui y déroge est **gardée** (aucune saisie n'est
 * perdue), tête de sa propre clé, avec l'anomalie `remplace_autre_objet` ou `remplace_autre_type`
 * rattachée à cette clé ; la ligne désignée garde sa valeur. Aussi bien par `saisir` que par `recevoir` :
 * le contrôle est dans la projection, donc identique incrémentale et reconstruite.
 */
class JournalRepository(
    private val db: JournalDatabase,
    private val appareil: String,
    private val horloge: SourceHorloge,
    private val uuids: GenerateurUuidV7 = GenerateurUuidV7(),
) {
    private val dao get() = db.dao()

    /**
     * Ajoute une saisie de cet appareil : `uuid` v7, `sequence` locale suivante, horloges relevées ici.
     * Si l'écart avec la ligne précédente (même appareil, même démarrage) dépasse 60 s, une ligne
     * `saut_horloge` est ajoutée dans la même transaction ; l'heure brute n'est jamais réécrite.
     */
    fun saisir(
        objetUuid: String,
        type: String,
        valeur: String?,
        operateur: String,
        remplaceUuid: String? = null,
        fix: FixGnss? = null,
    ): LigneJournal {
        require(type in TypeLigne.TOUS) { "type de ligne inconnu : $type" }
        return saisirDansTransaction(objetUuid, type, valeur, operateur, remplaceUuid, fix)
    }

    private fun saisirDansTransaction(
        objetUuid: String,
        type: String,
        valeur: String?,
        operateur: String,
        remplaceUuid: String?,
        fix: FixGnss?,
    ): LigneJournal = db.runInTransaction(Callable {
        val sequence = (dao.derniereSequence(appareil) ?: 0L) + 1
        val maintenant = horloge.maintenantUtcMs()
        val monotone = horloge.monotoneMs()
        val demarrage = horloge.demarrage()
        val ligne = LigneJournal(
            uuid = uuids.suivant(), objetUuid = objetUuid, type = type, valeur = valeur,
            remplaceUuid = remplaceUuid, operateur = operateur, appareil = appareil, sequence = sequence,
            saisiLeUtcMs = maintenant, heureGnssMs = fix?.heureUtcMs, monotoneMs = monotone, demarrage = demarrage,
            ageFixMs = fix?.let { monotone - it.monotoneMs },
        )
        val precedente = dao.precedente(appareil)?.versLigne()
        ajouter(ligne)
        val ecart = precedente?.let { Horloge.ecartMs(it, ligne) }
        if (precedente != null && ecart != null && Horloge.estUnSaut(ecart)) {
            ajouter(
                ligne.copy(
                    uuid = uuids.suivant(), objetUuid = appareil, type = TypeLigne.SAUT_HORLOGE,
                    valeur = valeurSaut(precedente, ligne, ecart), remplaceUuid = null,
                    sequence = sequence + 1, heureGnssMs = null, ageFixMs = null,
                ),
            )
        }
        ligne
    })

    /** Annule une ligne (défait ; annuler une annulation refait). L'annulation agit sur la clé de la cible. */
    fun annuler(uuidCible: String, operateur: String): LigneJournal {
        val cible = dao.trouver(uuidCible) ?: throw IllegalArgumentException("ligne inconnue : $uuidCible")
        return saisir(cible.objetUuid, TypeLigne.ANNULATION, null, operateur, remplaceUuid = cible.uuid)
    }

    /**
     * Reçoit une ligne venue d'ailleurs (restauration, binôme, fusion) : mêmes règles quel que soit l'ordre
     * d'arrivée. Doublon à l'identique : ignoré. Même `uuid`, contenu différent : refusée, et la version
     * déjà présente est aussi écartée de la projection, avec l'anomalie `uuid_contenu_different`.
     */
    fun recevoir(ligne: LigneJournal): Reception =
        if (ligne.type !in TypeLigne.TOUS) Reception.TypeInvalide else db.runInTransaction(Callable { ajouter(ligne) })

    /** Toutes les lignes du journal, dans l'ordre total. */
    fun lignes(): List<LigneJournal> = dao.toutes().map { it.versLigne() }

    /** Projection lue dans la table. */
    fun projection(): Map<CleProjection, EtatCle> = dao.projection().associate { it.versEtat().let { e -> e.cle to e } }

    fun etat(objetUuid: String, type: String): EtatCle? = dao.projectionDe(objetUuid, type)?.versEtat()

    /** Anomalies du journal, hors clé (par exemple `uuid_contenu_different`), recalculées. */
    fun anomalies(): List<String> = recalculer().anomalies

    /** Reconstruit toute la table `projection` depuis le journal (elle est dérivée, jamais source). */
    fun reconstruireProjection() {
        db.runInTransaction {
            val resultat = recalculer()
            dao.effacerToutesLesProjections()
            dao.ecrireProjection(resultat.cles.values.map { it.versEntite() })
        }
    }

    // --- interne (toujours dans une transaction) ---

    private fun recalculer() =
        Projection.projeter(dao.toutes().map { it.versLigne() } + dao.tousLesRefus().map { it.versLigne() })

    private fun ajouter(ligne: LigneJournal): Reception {
        val existante = dao.trouver(ligne.uuid)?.versLigne()
        val reception: Reception
        val touches = mutableSetOf(ligne.objetUuid)
        when {
            existante == null -> {
                dao.inserer(LigneJournalEntity.de(ligne))
                reception = Reception.Ajoutee
                if (ligne.type == TypeLigne.ANNULATION) {
                    ligne.remplaceUuid?.let { cible -> dao.trouver(cible)?.let { touches += it.objetUuid } }
                }
            }
            existante == ligne -> return Reception.Doublon
            else -> {
                if (dao.tousLesRefus().none { it.uuid == ligne.uuid && it.versLigne() == ligne }) {
                    dao.insererRefus(RefusJournalEntity.de(ligne))
                }
                touches += existante.objetUuid
                reception = Reception.Refusee
            }
        }
        // Les lignes qui désignaient cet uuid (remplacement inconnu jusque-là) changent de statut.
        dao.remplacantes(ligne.uuid).forEach { touches += it.objetUuid }
        mettreAJour(touches)
        return reception
    }

    /** Recalcule la projection des seuls objets touchés (pas de rejeu complet à chaque geste). */
    private fun mettreAJour(objets: Set<String>) {
        val lignes = dao.lignesPourObjets(objets).map { it.versLigne() } + dao.tousLesRefus().map { it.versLigne() }
        val resultat = Projection.projeter(lignes)
        dao.effacerProjection(objets)
        dao.ecrireProjection(resultat.cles.values.filter { it.cle.objetUuid in objets }.map { it.versEntite() })
    }

    private fun valeurSaut(avant: LigneJournal, apres: LigneJournal, ecartMs: Long) =
        """{"entre":["${avant.uuid}","${apres.uuid}"],"ecart_ms":$ecartMs,""" +
            """"heure_avant_utc_ms":${avant.saisiLeUtcMs},"heure_apres_utc_ms":${apres.saisiLeUtcMs}}"""

    private fun EtatCle.versEntite() = ProjectionEntity(
        objetUuid = cle.objetUuid, type = cle.type, courantUuid = courant, conflit = conflit,
        tetes = tetes.joinToString(","), anomalies = anomalies.joinToString(","),
    )

    private fun ProjectionEntity.versEtat() = EtatCle(
        cle = CleProjection(objetUuid, type), courant = courantUuid, conflit = conflit,
        tetes = tetes.split(",").filter { it.isNotEmpty() }, anomalies = anomalies.split(",").filter { it.isNotEmpty() },
    )
}
