/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.controle

import android.os.Build
import io.github.pobsteta.surculus.carte.SECONDES_DE_MOYENNE_ARRIVEE
import io.github.pobsteta.surculus.core.gnss.Position
import io.github.pobsteta.surculus.core.guidage.Guidage
import io.github.pobsteta.surculus.gpkg.Tournee
import io.github.pobsteta.surculus.core.guidage.MoyennePosition
import io.github.pobsteta.surculus.core.placette.Essences
import io.github.pobsteta.surculus.core.placette.Piquet
import io.github.pobsteta.surculus.core.reperes.PointGeographique
import io.github.pobsteta.surculus.core.reperes.Resultat
import io.github.pobsteta.surculus.core.session.Arrivee
import io.github.pobsteta.surculus.core.session.ContexteControle
import io.github.pobsteta.surculus.core.session.EcouteSauvegarde
import io.github.pobsteta.surculus.core.session.HorlogeSession
import io.github.pobsteta.surculus.core.session.JournalControle
import io.github.pobsteta.surculus.core.session.OuvertureControle
import io.github.pobsteta.surculus.core.session.SessionControle
import io.github.pobsteta.surculus.core.session.SuiteSession
import io.github.pobsteta.surculus.core.session.FormatValeurs
import io.github.pobsteta.surculus.core.session.ValeurPointage
import io.github.pobsteta.surculus.core.placette.CodesRefus
import io.github.pobsteta.surculus.core.placette.Rattachement
import io.github.pobsteta.surculus.core.placette.Verdict
import io.github.pobsteta.surculus.messages.JournalTechnique

/** Où en est l'arrivée : on moyenne, ou on a un refus avec son code, ou la session est ouverte. */
sealed interface EtatArrivee {
    /** Décompte : [n] secondes écoulées sur [total]. */
    data class Moyenne(val n: Int, val total: Int = SECONDES_DE_MOYENNE_ARRIVEE) : EtatArrivee

    data class Refus(val refus: SuiteSession.Refus) : EtatArrivee

    /** Aucune position reçue pendant la moyenne (pas de fix) : on le dit, rien n'est ouvert ; l'attente de 30 s commence. */
    data object SansPosition : EtatArrivee

    /**
     * Ouvrir sans position (ADR 0003, amendement du 2026-11-02, point 4) : la placette désignée par le code lu, à
     * confirmer ; rien n'est ouvert ni écrit. [plantsPointes] : plants connus de la placette (passage précédent et journal).
     */
    data class ConfirmerSansPosition(val piquet: Piquet, val plantsPointes: Int) : EtatArrivee

    /**
     * Le code lu existe dans plusieurs UG et la position ne tranche pas (une seule à 15 m au plus, sinon on demande) :
     * rien n'est ouvert, l'agent choisit l'UG parmi les [candidats] (ADR 0003, amendement du 2026-11-02, point 3).
     */
    data class UgAChoisir(val code: String, val candidats: List<Piquet>) : EtatArrivee

    data class Ouverte(val controleur: ControleurPlacette) : EtatArrivee
}

/** L'arrivée de l'agent : la position moyennée, et la règle des 15 m ou le code du piquet lu (spec § 4.3). */
object ArriveeControle {
    /**
     * La position d'arrivée à partir des fix reçus pendant la moyenne. Un fix **hors de France** n'est pas « pas de position » :
     * il est rendu tel quel (latitude, longitude) pour que la session le refuse avec `hors_france` (la placette en Somalie).
     * Sinon, moyenne en Lambert-93 (x = Est, y = Nord, mètres). Aucun fix : `null`.
     */
    fun arrivee(positions: List<Position>): Arrivee? {
        if (positions.isEmpty()) return null
        val moyenne = MoyennePosition()
        for (p in positions) {
            val l93 = Guidage.versLambert93(p)
            if (l93 == null) {
                val point = PointGeographique.de(p.latitude, p.longitude)
                return if (point is Resultat.Accepte) Arrivee.Gnss(point.valeur) else null
            }
            moyenne.ajouter(l93)
        }
        return moyenne.moyenne()?.let { Arrivee.Lambert93(it) }
    }

    /**
     * Les essences connues de la session : les codes déjà portés par les plants de la tournée (écrits par sommieR depuis
     * `essences.csv`, jamais une liste de l'application). Servent au regarni (qui garde l'essence du plant remplacé) et au
     * refus `hors_referentiel` pour un code étranger à la tournée.
     */
    fun essencesDe(tournee: Tournee): Essences = Essences(tournee.plants.mapNotNull { it.essence?.takeIf(String::isNotBlank) }.toSet())

    /** Le journal technique de l'application : une ligne de diagnostic, jamais à l'écran. */
    val JOURNAL_TECHNIQUE: (String) -> Unit = { JournalTechnique.diagnostic("Arrivee", it) }

    /**
     * Ouvre la session : refus avec son code, sans position, ou prête. Bloquant (journal) : hors du fil principal.
     *
     * Un fix **hors de France** est refusé (`hors_france`) avec une phrase qui dit quoi faire, et le refus est écrit au
     * journal technique par [diagnostic], jamais au journal des saisies (ADR 0003, amendement du 2026-11-02, point 6).
     */
    fun ouvrir(
        contexte: ContexteControle,
        journal: JournalControle,
        horloge: HorlogeSession,
        essences: Essences,
        positions: List<Position>,
        codeLu: String?,
        sauvegarde: EcouteSauvegarde,
        ugChoisie: String? = null,
        diagnostic: (String) -> Unit = JOURNAL_TECHNIQUE,
    ): EtatArrivee {
        val arrivee = arrivee(positions) ?: return EtatArrivee.SansPosition
        val etat = etatDe(contexte, essences, SessionControle.ouvrir(contexte, journal, horloge, essences, arrivee, codeLu, sauvegarde, ugChoisie, diagnostic))
        if (etat is EtatArrivee.Refus && etat.refus.code == CodesRefus.HORS_FRANCE) {
            diagnostic("fix refusé à l'arrivée, hors de France : ${etat.refus.message}")
            return EtatArrivee.Refus(etat.refus.copy(message = TextesControle.POSITION_ABERRANTE))
        }
        return etat
    }

    /**
     * Ouvre sans position (ADR 0003, amendement du 2026-11-02, points 2 à 5) : code lu obligatoire (`code_requis`) et
     * connu (`code_inconnu`) ; code dans plusieurs UG : [EtatArrivee.UgAChoisir] (sans position, rien ne tranche) ; tant
     * que [confirmee] est faux, [EtatArrivee.ConfirmerSansPosition] et rien n'est ouvert ; confirmée, la session s'ouvre
     * à la position du piquet, sans position mesurée. Bloquant (journal) : hors du fil principal.
     */
    fun ouvrirSansPosition(
        contexte: ContexteControle,
        journal: JournalControle,
        horloge: HorlogeSession,
        essences: Essences,
        codeLu: String?,
        sauvegarde: EcouteSauvegarde,
        ugChoisie: String? = null,
        confirmee: Boolean = false,
        diagnostic: (String) -> Unit = JOURNAL_TECHNIQUE,
    ): EtatArrivee {
        val code = codeLu?.trim()?.ifEmpty { null }
        if (!confirmee) {
            return when (val v = Rattachement.rattacher(contexte.placettes.map { it.piquet }, null, code, ugChoisie)) {
                is Verdict.Accepte -> EtatArrivee.ConfirmerSansPosition(v.valeur.piquet, plantsPointes(contexte, journal, v.valeur.piquet))
                is Verdict.Refuse -> EtatArrivee.Refus(SuiteSession.Refus.de(v.refus))
                is Verdict.AChoisirUg -> EtatArrivee.UgAChoisir(code!!, v.candidats)
                is Verdict.AConfirmer -> error("pas d'alerte attendue au rattachement")
            }
        }
        return etatDe(contexte, essences, SessionControle.ouvrir(contexte, journal, horloge, essences, Arrivee.SansPosition, code, sauvegarde, ugChoisie, diagnostic))
    }

    /** Plants connus de la placette, pour la confirmation : ceux du passage précédent et ceux pointés au journal. */
    private fun plantsPointes(contexte: ContexteControle, journal: JournalControle, piquet: Piquet): Int {
        val anciens = contexte.plantsAnterieurs[piquet.placette].orEmpty().map { it.uuid.valeur }
        val pointes = journal.lignesCourantes().filter { it.type == FormatValeurs.TYPE_POINTAGE }
            .filter { ValeurPointage.lire(it.valeur)?.placetteUuid == piquet.placette.valeur }.map { it.objetUuid }
        return (anciens + pointes).toSet().size
    }

    private fun etatDe(contexte: ContexteControle, essences: Essences, o: OuvertureControle): EtatArrivee = when (o) {
        is OuvertureControle.Ouverte -> {
            val rayon = contexte.placettes.firstOrNull { it.piquet.placette == o.session.rattachement.piquet.placette }?.rayonM ?: RAYON_PAR_DEFAUT_M
            EtatArrivee.Ouverte(ControleurPlacette(o.session, rayon, essences.liste()))
        }
        is OuvertureControle.Refusee -> EtatArrivee.Refus(o.refus)
        is OuvertureControle.UgAChoisir -> EtatArrivee.UgAChoisir(o.code, o.candidats)
    }
}

/** Permission de stockage (spec § 4.8) : demandée seulement sur Android 8 et 9 (API 26 à 28), jamais ailleurs. */
object PermissionStockage {
    const val DERNIERE_API_A_DEMANDER = 28

    /** Demandée une seule fois : si l'agent a déjà répondu, on ne redemande pas (décision de Pascal sur la #108, 2026-10-28). */
    fun aDemander(apiSdk: Int, accordee: Boolean, dejaDemandee: Boolean = false): Boolean =
        apiSdk <= DERNIERE_API_A_DEMANDER && !accordee && !dejaDemandee

    /** La ligne « la sauvegarde attend la permission » s'affiche tant que la permission manque, sur ces versions seulement. */
    fun manque(apiSdk: Int, accordee: Boolean): Boolean = apiSdk <= DERNIERE_API_A_DEMANDER && !accordee

    fun aDemanderSurCetAppareil(accordee: Boolean, dejaDemandee: Boolean): Boolean = aDemander(Build.VERSION.SDK_INT, accordee, dejaDemandee)

    fun manqueSurCetAppareil(accordee: Boolean): Boolean = manque(Build.VERSION.SDK_INT, accordee)
}
