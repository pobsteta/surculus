package io.github.pobsteta.surculus.data.session

import io.github.pobsteta.surculus.core.session.EcouteSauvegarde
import io.github.pobsteta.surculus.core.session.FixSession
import io.github.pobsteta.surculus.core.session.HorlogeSession
import io.github.pobsteta.surculus.core.session.JournalControle
import io.github.pobsteta.surculus.core.session.LigneCourante
import io.github.pobsteta.surculus.core.session.PlacettesControlees
import io.github.pobsteta.surculus.data.journal.GenerateurUuidV7
import io.github.pobsteta.surculus.data.journal.TypeLigne
import io.github.pobsteta.surculus.data.journal.db.FixGnss
import io.github.pobsteta.surculus.data.journal.db.JournalRepository
import io.github.pobsteta.surculus.data.journal.db.SourceHorloge
import io.github.pobsteta.surculus.data.sauvegarde.Evenement
import io.github.pobsteta.surculus.data.sauvegarde.SuiviDeclenchement

/**
 * Le journal Room (`JournalRepository`) vu par la session de contrôle, pour **une** tournée : [codeTournee] est lié à
 * l'instance, donc écrit à chaque saisie et appliqué à chaque lecture. Aucun appel ne peut l'oublier : l'état d'une
 * clé sans code se lirait dans les lignes non rattachées (`""`), c'est-à-dire pas dans la tournée (spec § 5, 032).
 *
 * Méthodes bloquantes : à appeler hors du fil principal.
 */
class JournalDeTournee(
    private val depot: JournalRepository,
    val codeTournee: String,
    private val uuids: GenerateurUuidV7 = GenerateurUuidV7(),
) : JournalControle {
    init { require(codeTournee.isNotBlank()) { "code de tournée vide : le journal d'une tournée n'est pas celui des lignes non rattachées" } }

    override fun nouvelUuid(): String = uuids.suivant()

    override fun saisir(objetUuid: String, type: String, valeur: String, operateur: String, remplaceUuid: String?, fix: FixSession?): String =
        depot.saisir(
            objetUuid, type, valeur, operateur, remplaceUuid,
            fix = fix?.let { FixGnss(it.heureUtcMs, it.monotoneMs) }, codeTournee = codeTournee,
        ).uuid

    override fun annuler(uuidCible: String, operateur: String) {
        depot.annuler(uuidCible, operateur)
    }

    override fun lignesCourantes(): List<LigneCourante> {
        val courants = depot.projection(codeTournee).values
            .filter { it.cle.type != TypeLigne.ANNULATION }
            .mapNotNull { it.courant }
            .toSet()
        // Lignes dans l'ordre total du journal ; seules celles qui sont la valeur courante de leur clé.
        return depot.lignes(codeTournee).filter { it.uuid in courants }
            .map { LigneCourante(it.uuid, it.objetUuid, it.type, it.valeur, it.remplaceUuid) }
    }

    /** `uuid` des placettes « Contrôlées » de cette tournée : un contrôle terminé et non annulé. À appeler hors du fil principal. */
    fun placettesControlees(): Set<String> = PlacettesControlees.de(lignesCourantes())
}

/** Horloges de l'appareil et dernier fix GNSS, vus par la session. */
class HorlogeSessionAppareil(
    private val horloge: SourceHorloge,
    private val dernierFix: () -> FixGnss?,
) : HorlogeSession {
    override fun maintenantUtcMs(): Long = horloge.maintenantUtcMs()
    override fun monotoneMs(): Long = horloge.monotoneMs()
    override fun fix(): FixSession? = dernierFix()?.let { FixSession(it.heureUtcMs, it.monotoneMs) }
}

/**
 * Branche la session sur la règle de déclenchement de la sauvegarde (spec § 4.8) : chaque ligne écrite est une saisie,
 * « terminer » une fin de placette. [sauvegarder] écrit le `.sursync` ; il est lancé par [executeur] (en tâche de fond
 * dans l'application, jamais sur le fil principal) et la sauvegarde n'est comptée « faite » que si elle a réussi : un
 * échec laisse les compteurs, la suivante réessaie.
 */
class DeclencheurSauvegarde(
    private val suivi: SuiviDeclenchement,
    private val monotoneMs: () -> Long,
    private val sauvegarder: () -> Unit,
    private val executeur: (Runnable) -> Unit = { it.run() },
) : EcouteSauvegarde {
    override fun surSaisie() = evenement(Evenement.SAISIE)

    override fun surFinPlacette() = evenement(Evenement.FIN_PLACETTE)

    private var enCours = false

    private fun evenement(e: Evenement) {
        val doit = synchronized(suivi) {
            val d = suivi.surEvenement(e, monotoneMs())
            // Une sauvegarde déjà lancée : pas de seconde ; la prochaine saisie redemandera.
            (d && !enCours).also { if (it) enCours = true }
        }
        if (!doit) return
        executeur(
            Runnable {
                val reussie = runCatching { sauvegarder() }.isSuccess
                synchronized(suivi) {
                    enCours = false
                    if (reussie) suivi.sauvegardeFaite(monotoneMs())
                }
            },
        )
    }
}
