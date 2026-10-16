/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.core.essai

import io.github.pobsteta.surculus.core.geo.EmpriseLambert93
import io.github.pobsteta.surculus.core.geo.Geodesie
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.geo.Projections
import io.github.pobsteta.surculus.core.gnss.FixGnss
import io.github.pobsteta.surculus.core.gnss.OrigineFix
import io.github.pobsteta.surculus.core.gnss.PointMontage
import io.github.pobsteta.surculus.core.gnss.Position
import io.github.pobsteta.surculus.core.gnss.QualiteFix
import io.github.pobsteta.surculus.core.gnss.RaisonRefus
import java.util.Locale

/** Teinte de l'écran d'essai : trois états lisibles de loin, au soleil (règle des quatre couleurs). */
enum class Ton { BON, A_SURVEILLER, MAUVAIS }

/** Les trois drapeaux de l'essai (mission 018), chacun avec sa phrase. */
sealed interface Drapeau {
    val texte: String

    /** Corrections trop vieilles : le récepteur annonçait [annoncee], l'application affiche « autonome ». */
    data class Retrograde(val annoncee: QualiteFix) : Drapeau {
        override val texte get() = "Rétrogradé : corrections trop vieilles (annoncé ${annoncee.libelle})"
    }

    data class Refuse(val raison: RaisonRefus) : Drapeau {
        override val texte
            get() = when (raison) {
                RaisonRefus.SANS_POSITION -> "Refusé : le récepteur n'a pas de position"
                RaisonRefus.TROP_VIEUX -> "Refusé : fix trop vieux"
                RaisonRefus.HORLOGE_INCOHERENTE -> "Refusé : horloge incohérente"
            }
    }

    data object Simule : Drapeau {
        override val texte get() = "Position simulée"
    }
}

/**
 * Ce que l'écran d'essai affiche : six lignes au plus (qualité, précision, âge du fix, âge des
 * corrections, drapeaux, Lambert-93). Calculé ici, sans Android, pour être testé.
 */
data class AffichageEssai(
    val qualite: String,
    val ton: Ton,
    val precision: String,
    val ageFix: String,
    val ageCorrections: String,
    val drapeaux: List<Drapeau>,
    /** « X … m » en Lambert-93 (EPSG:2154), null sans position. */
    val lambertX: String?,
    /** « Y … m » en Lambert-93 (EPSG:2154), null sans position. */
    val lambertY: String?,
    /** Faux si la position, une fois en Lambert-93, tombe hors de la France métropolitaine (inversion lat/lon). */
    val enFrance: Boolean?,
)

object EssaiGnss {
    /**
     * @param fix fix **déjà évalué** par la politique de fraîcheur (null si absent ou refusé)
     * @param refus raison du refus du dernier fix du récepteur, sinon null
     * @param maintenantMs horloge murale, en millisecondes
     */
    fun afficher(fix: FixGnss?, refus: RaisonRefus?, maintenantMs: Long): AffichageEssai {
        val drapeaux = buildList {
            if (refus != null) add(Drapeau.Refuse(refus))
            fix?.qualiteAnnoncee?.let { add(Drapeau.Retrograde(it)) }
            if (fix?.simule == true) add(Drapeau.Simule)
        }
        if (fix == null) {
            return AffichageEssai(
                qualite = if (refus != null) "Fix refusé" else "Sans position",
                ton = Ton.MAUVAIS,
                precision = "—",
                ageFix = "Fix : —",
                ageCorrections = "Corrections : —",
                drapeaux = drapeaux,
                lambertX = null,
                lambertY = null,
                enFrance = null,
            )
        }
        val (x, y) = Projections.versLambert93(lon = fix.position.longitude, lat = fix.position.latitude)
        val ageS = (maintenantMs - fix.horodatageMs).coerceAtLeast(0L) / 1000.0
        val origine = if (fix.origine == OrigineFix.TELEPHONE) "Téléphone" else "Récepteur"
        return AffichageEssai(
            qualite = fix.qualite.libelle,
            ton = ton(fix),
            // Décision de Pascal (2026-10-16) : rétrogradé, la précision du récepteur n'est plus valable ;
            // on n'affiche pas un chiffre centimétrique sous « Autonome », le drapeau dit pourquoi.
            precision = if (fix.qualiteAnnoncee != null) "—" else fix.precisionHorizontaleM?.let { precision(it) } ?: "—",
            ageFix = "$origine · fix il y a ${secondes(ageS)}",
            ageCorrections = fix.ageCorrectionsS?.let { "Corrections : ${secondes(it)}" } ?: "Corrections : aucune",
            drapeaux = drapeaux,
            lambertX = String.format(Locale.FRANCE, "X %,.2f m", x),
            lambertY = String.format(Locale.FRANCE, "Y %,.2f m", y),
            enFrance = EmpriseLambert93.FRANCE_METROPOLITAINE.contient(PointLambert93(x, y)),
        )
    }

    /**
     * Points de montage triés par distance à [position] quand elle est connue (les points sans coordonnées
     * en dernier, par nom), sinon par nom.
     */
    fun trierPoints(points: List<PointMontage>, position: Position?): List<PointMontage> {
        val parNom = compareBy<PointMontage> { it.nom.lowercase(Locale.ROOT) }
        if (position == null) return points.sortedWith(parNom)
        fun distance(p: PointMontage): Double {
            val lat = p.latitude
            val lon = p.longitude
            return if (lat != null && lon != null) Geodesie.distanceM(position, Position(lat, lon)) else Double.MAX_VALUE
        }
        return points.sortedWith(compareBy<PointMontage> { distance(it) }.then(parNom))
    }

    private fun ton(fix: FixGnss) = when {
        fix.simule -> Ton.MAUVAIS
        fix.qualite == QualiteFix.RTK_FIXE -> Ton.BON
        fix.qualite == QualiteFix.AUCUN -> Ton.MAUVAIS
        else -> Ton.A_SURVEILLER
    }

    /** Centimètres sous le mètre, décimètres au-delà : « 0,02 m », « 1,8 m ». */
    private fun precision(m: Double) =
        if (m < 1.0) String.format(Locale.FRANCE, "± %.2f m", m) else String.format(Locale.FRANCE, "± %.1f m", m)

    private fun secondes(s: Double) = String.format(Locale.FRANCE, "%.0f s", s)
}
