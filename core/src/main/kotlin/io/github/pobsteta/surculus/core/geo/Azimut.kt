/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 *
 * Origine : dépôt Groma (https://github.com/pobsteta/groma), fichier
 *           android/groma-core/src/main/kotlin/groma/core/Azimut.kt,
 *           état au commit f12b644 (2026-10-16). Reprise provisoire (ADR 0003, mission 003)
 *           jusqu'à l'extraction en bibliothèque commune (ADR 0008).
 * Modifications : paquet ; unité d'azimut typée (UniteAzimut) au lieu d'une chaîne ; un azimut
 *           SAISI hors plage est refusé avant toute normalisation (ADR 0003), là où Groma le
 *           ramenait silencieusement dans [0, 360[ ; la normalisation ne subsiste que sous le nom
 *           ramenerDans360, pour des résultats de calcul (jamais pour une saisie).
 */
package io.github.pobsteta.surculus.core.geo

import kotlin.math.atan2
import kotlin.math.sqrt

/** Unité de la boussole de l'agent, déclarée à l'ouverture de la tournée (ADR 0003). */
enum class UniteAzimut(
    /** Valeur du tour complet dans cette unité : l'azimut valide est dans [0, tourComplet[. */
    val tourComplet: Double,
) {
    DEGRES(360.0),
    GRADES(400.0),
}

/**
 * Arithmétique des azimuts, mêmes conventions que `groma/azimut.py` : x vers l'est, y vers le nord,
 * sens horaire, 0 = nord.
 *
 * **Règle de l'ADR 0003** : un azimut saisi (lu sur la boussole) hors de [0, 360[ en degrés ou de
 * [0, 400[ en grades est **refusé** ([IllegalArgumentException]) avant toute normalisation. Une valeur
 * hors plage est presque toujours une erreur de saisie ou d'unité (grades pris pour des degrés), pas
 * un azimut à corriger en silence.
 */
object Azimut {
    const val DEGRES_PAR_GRADE = 0.9
    const val LAMBERT93_N = 0.7256077650532670
    const val LAMBERT93_LON0_DEG = 3.0

    /** Renvoie [valeur] si elle est dans [0, tour complet[ de [unite], sinon refuse. Ne normalise jamais. */
    fun verifier(valeur: Double, unite: UniteAzimut): Double {
        require(valeur.isFinite() && valeur >= 0.0 && valeur < unite.tourComplet) {
            "Azimut hors plage : $valeur ${unite.name.lowercase()} (attendu [0, ${unite.tourComplet}[)"
        }
        return valeur
    }

    /** Azimut saisi dans [unite] → degrés, après vérification de la plage. */
    fun versDegres(valeur: Double, unite: UniteAzimut): Double = when (unite) {
        UniteAzimut.DEGRES -> verifier(valeur, unite)
        UniteAzimut.GRADES -> verifier(valeur, unite) * DEGRES_PAR_GRADE
    }

    /** Azimut en degrés (déjà dans [0, 360[, sinon refusé) → valeur exprimée dans [unite]. */
    fun depuisDegres(azDeg: Double, unite: UniteAzimut): Double = when (unite) {
        UniteAzimut.DEGRES -> verifier(azDeg, UniteAzimut.DEGRES)
        UniteAzimut.GRADES -> verifier(azDeg, UniteAzimut.DEGRES) / DEGRES_PAR_GRADE
    }

    /**
     * Ramène un **résultat de calcul** (somme, différence, atan2) dans [0, 360[.
     * Ne jamais l'appliquer à un azimut saisi : utiliser [verifier] ou [versDegres].
     */
    fun ramenerDans360(azDeg: Double): Double {
        var a = azDeg % 360.0
        if (a < 0) a += 360.0
        return if (a >= 360.0) 0.0 else a
    }

    /** Écart signé a − b, dans ]-180, 180] ; opérandes de calcul (déjà valides). */
    fun ecart(aDeg: Double, bDeg: Double): Double {
        val d = ramenerDans360(aDeg - bDeg)
        return if (d > 180.0) d - 360.0 else d
    }

    /** Azimut de grille du vecteur (dx vers l'est, dy vers le nord), dans [0, 360[. */
    fun azimut(dx: Double, dy: Double): Double = ramenerDans360(Math.toDegrees(atan2(dx, dy)))

    /** γ = n·(λ − 3°), convergence des méridiens en Lambert-93 (degrés), λ longitude en degrés. */
    fun convergenceLambert93(lonDeg: Double): Double = LAMBERT93_N * (lonDeg - LAMBERT93_LON0_DEG)

    /** az_géographique = az_magnétique + déclinaison ; [azMagDeg] saisi, vérifié dans [0, 360[. */
    fun magnetiqueVersGeographique(azMagDeg: Double, declinaisonDeg: Double): Double =
        ramenerDans360(verifier(azMagDeg, UniteAzimut.DEGRES) + declinaisonDeg)

    /** az_grille = az_géographique − convergence ; [azGeoDeg] vérifié dans [0, 360[. */
    fun geographiqueVersGrille(azGeoDeg: Double, convergenceDeg: Double): Double =
        ramenerDans360(verifier(azGeoDeg, UniteAzimut.DEGRES) - convergenceDeg)

    /** Distance horizontale d'une distance suivant la pente et du dénivelé (refusé si |dénivelé| > distance). */
    fun distanceHorizontale(distancePente: Double, denivele: Double): Double {
        require(kotlin.math.abs(denivele) <= distancePente) { "Le dénivelé dépasse la distance mesurée." }
        return sqrt(distancePente * distancePente - denivele * denivele)
    }
}
