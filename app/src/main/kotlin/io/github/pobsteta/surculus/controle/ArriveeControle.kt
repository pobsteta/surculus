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

/** Où en est l'arrivée : on moyenne, ou on a un refus avec son code, ou la session est ouverte. */
sealed interface EtatArrivee {
    /** Décompte : [n] secondes écoulées sur [total]. */
    data class Moyenne(val n: Int, val total: Int = SECONDES_DE_MOYENNE_ARRIVEE) : EtatArrivee

    data class Refus(val refus: SuiteSession.Refus) : EtatArrivee

    /** Aucune position reçue pendant la moyenne (pas de fix) : on le dit, rien n'est ouvert. */
    data object SansPosition : EtatArrivee

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

    /** Ouvre la session : refus avec son code, sans position, ou prête. Bloquant (journal) : hors du fil principal. */
    fun ouvrir(
        contexte: ContexteControle,
        journal: JournalControle,
        horloge: HorlogeSession,
        essences: Essences,
        positions: List<Position>,
        codeLu: String?,
        sauvegarde: EcouteSauvegarde,
    ): EtatArrivee {
        val arrivee = arrivee(positions) ?: return EtatArrivee.SansPosition
        return when (val o = SessionControle.ouvrir(contexte, journal, horloge, essences, arrivee, codeLu?.trim()?.ifEmpty { null }, sauvegarde)) {
            is OuvertureControle.Ouverte -> {
                val rayon = contexte.placettes.firstOrNull { it.piquet.placette == o.session.rattachement.piquet.placette }?.rayonM ?: RAYON_PAR_DEFAUT_M
                EtatArrivee.Ouverte(ControleurPlacette(o.session, rayon, essences.liste()))
            }
            is OuvertureControle.Refusee -> EtatArrivee.Refus(o.refus)
        }
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
