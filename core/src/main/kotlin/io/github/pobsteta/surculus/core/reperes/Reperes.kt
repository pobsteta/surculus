/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.core.reperes

import io.github.pobsteta.surculus.core.geo.Azimut
import io.github.pobsteta.surculus.core.geo.EmpriseLambert93
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.geo.Projections
import io.github.pobsteta.surculus.core.reperes.wmm.Declinaison
import java.time.LocalDate
import kotlin.math.cos
import kotlin.math.sin

/**
 * Point géographique en **degrés décimaux**, ordre explicite (latitude, longitude), RGF93 ou WGS 84
 * (confondus au mètre près, `docs/referentiels/referentiel-centipede.md`). Jamais une `Pair`.
 */
data class PointGeographique private constructor(val latitudeDeg: Double, val longitudeDeg: Double) {
    companion object {
        fun de(latitudeDeg: Double, longitudeDeg: Double): Resultat<PointGeographique> =
            if (latitudeDeg.isFinite() && longitudeDeg.isFinite() && latitudeDeg in -90.0..90.0 && longitudeDeg in -180.0..180.0) {
                Resultat.Accepte(PointGeographique(latitudeDeg, longitudeDeg))
            } else {
                Resultat.Refuse(RefusRepere.PointHorsLimites(latitudeDeg, longitudeDeg))
            }

        /**
         * Lambert-93 (x = E, y = N, mètres) vers (latitude, longitude). Même projection pour EPSG:2154
         * et EPSG:9794 (Lambert conique conforme sur GRS 80, mêmes paramètres ; seule la réalisation
         * RGF93 diffère, au centimètre). Attention : `Projections.versWgs84` rend **(lon, lat)**.
         */
        fun depuisLambert93(p: PointLambert93): PointGeographique {
            val (lon, lat) = Projections.versWgs84(p.xMetres, p.yMetres)
            return PointGeographique(latitudeDeg = lat, longitudeDeg = lon)
        }
    }
}

/**
 * Déclinaison magnétique (degrés, **positive vers l'est**) par le modèle WMM2025 hors ligne, avec les
 * bornes de validité de Surculus.
 */
object DeclinaisonWmm2025 {
    /** Premier jour de validité : WMM2025 valable à partir de 2025.0 (`docs/referentiels/modele-geomagnetique.md`). */
    val PREMIER_JOUR: LocalDate = LocalDate.of(2025, 1, 1)

    /**
     * Dernier jour de validité, **inclus** : NCEI, « expires on December 31, 2029 » ; spec § 6 (date après
     * le 31/12/2029 refusée). Constante posée, **non déduite** de l'époque du fichier de coefficients
     * (époque + 5 donnerait 2030.0 inclus, c'est-à-dire le 2030-01-01). À revoir à chaque nouveau modèle.
     */
    val DERNIER_JOUR: LocalDate = LocalDate.of(2029, 12, 31)

    /** Époque du fichier de coefficients embarqué (2025.0 attendu). */
    val epoqueDuModele: Double get() = Declinaison.epoque

    /** Année décimale du logiciel WMM : année + (jour de l'année − 1) / nombre de jours de l'année. */
    fun anneeDecimale(date: LocalDate): Double = Declinaison.anneeDecimale(date)

    /**
     * Déclinaison au [point] et au jour civil [date], à [altitudeEllipsoideM] mètres au-dessus de
     * l'ellipsoïde. Hors de [[PREMIER_JOUR], [DERNIER_JOUR]] : refus, jamais d'extrapolation.
     */
    fun calculer(point: PointGeographique, date: LocalDate, altitudeEllipsoideM: Double): Resultat<Double> {
        if (date.isBefore(PREMIER_JOUR) || date.isAfter(DERNIER_JOUR)) {
            return Resultat.Refuse(RefusRepere.DateHorsValidite(date, PREMIER_JOUR, DERNIER_JOUR))
        }
        return Resultat.Accepte(Declinaison.declinaisonDeg(point.latitudeDeg, point.longitudeDeg, anneeDecimale(date), altitudeEllipsoideM))
    }
}

/** Convergence des méridiens en Lambert-93, en degrés, positive à l'est de 3° E : γ = n·(λ − 3°) (`core.geo.Azimut`). */
object Convergence {
    fun lambert93(point: PointGeographique): Double = Azimut.convergenceLambert93(point.longitudeDeg)
}

/** Les termes de la chaîne d'azimut, tous en degrés décimaux, tous à enregistrer (ADR 0003). */
data class Orientation(
    /** Azimut magnétique converti en degrés, [0, 360[. */
    val azimutMagnetiqueDeg: Double,
    /** Déclinaison, positive vers l'est. */
    val declinaisonDeg: Double,
    /** Convergence des méridiens Lambert-93, positive à l'est de 3° E. */
    val convergenceDeg: Double,
    /** az_mag + D, ramené dans [0, 360[. */
    val azimutGeographiqueDeg: Double,
    /** az_géo − γ, ramené dans [0, 360[ : c'est lui qui place le plant dans le quadrillage. */
    val azimutGrilleDeg: Double,
)

/** Chaîne az_grille = az_mag + D − γ (ADR 0003). */
object ChaineAzimut {
    /**
     * Altitude prise pour la déclinaison sur le terrain : 0 m au-dessus de l'ellipsoïde. Effet négligé :
     * moins de 0,005° pour 500 m en France (vérifié par un test), contre 0,36° d'incertitude du modèle.
     */
    const val ALTITUDE_TERRAIN_M = 0.0

    fun orienter(azimut: AzimutSaisi, point: PointGeographique, date: LocalDate): Resultat<Orientation> =
        DeclinaisonWmm2025.calculer(point, date, ALTITUDE_TERRAIN_M).puis { d ->
            val gamma = Convergence.lambert93(point)
            val azMag = azimut.degres
            val azGeo = Azimut.magnetiqueVersGeographique(azMag, d)
            val azGrille = Azimut.geographiqueVersGrille(azGeo, gamma)
            Resultat.Accepte(Orientation(azMag, d, gamma, azGeo, azGrille))
        }
}

/** Référentiel (EPSG) d'une tournée : Lambert-93 sur RGF93 v1 (contrat sommieR 0.31.1) ou v2b (cible, ADR 0003). */
enum class CodeEpsg(val code: Int) {
    RGF93_V1_LAMBERT93(2154),
    RGF93_V2B_LAMBERT93(9794),
}

/** Contrôle des référentiels d'un fichier de tournée. La lecture du GeoPackage relève de la mission 006. */
object ReferentielTournee {
    /**
     * [codes] : les codes EPSG des couches géométriques du fichier. Un seul code parmi {2154, 9794} :
     * accepté. Les deux ensemble : refusé (une tournée est en 2154 **ou** en 9794, ADR 0003). Tout autre
     * code, ou aucun : refusé.
     */
    fun verifier(codes: Collection<Int>): Resultat<CodeEpsg> {
        val distincts = codes.toSet()
        if (distincts.isEmpty()) return Resultat.Refuse(RefusRepere.AucunReferentiel)
        val inconnus = distincts.filter { c -> CodeEpsg.entries.none { it.code == c } }.toSet()
        if (inconnus.isNotEmpty()) return Resultat.Refuse(RefusRepere.ReferentielNonPrisEnCharge(inconnus))
        if (distincts.size > 1) return Resultat.Refuse(RefusRepere.ReferentielsMeles(distincts))
        return Resultat.Accepte(CodeEpsg.entries.first { it.code == distincts.single() })
    }
}

/**
 * Plant pointé depuis le piquet et sa position dérivée, avec tous les termes à enregistrer.
 * [position] est dans le référentiel [epsg] du piquet. API stable, réutilisée par la mission 004.
 */
data class PlantDerive(
    val piquet: PointLambert93,
    val epsg: CodeEpsg,
    /** Distance horizontale du piquet au plant, en mètres. */
    val distanceHorizontaleM: Double,
    /** Observation primaire : azimut magnétique tel que lu, avec son unité. */
    val azimut: AzimutSaisi,
    /** Jour civil de l'observation (date de la déclinaison). */
    val date: LocalDate,
    val orientation: Orientation,
    /** Décalage vers l'est, mètres de quadrillage. */
    val dxM: Double,
    /** Décalage vers le nord du quadrillage, mètres. */
    val dyM: Double,
    val position: PointLambert93,
)

object PointDerive {
    /**
     * Position du plant : x = x_piquet + d·sin(az_grille), y = y_piquet + d·cos(az_grille).
     *
     * **Limite assumée (arbitrage de Pascal 2026-10-16) : pas de correction du facteur d'échelle du
     * Lambert-93**, ni de réduction à l'ellipsoïde : la distance horizontale au sol est portée telle quelle
     * dans le quadrillage. À Couchey, k = 0,99913 (pyproj 3.6.1) : 8,7 mm d'écart à 10 m, 2,6 cm à 30 m,
     * loin sous le critère de 0,5 m.
     *
     * Refus : piquet hors de France (axes inversés, point non reprojeté), distance invalide, date hors
     * validité du WMM2025.
     */
    fun deriver(
        piquet: PointLambert93,
        epsg: CodeEpsg,
        distanceHorizontaleM: Double,
        azimut: AzimutSaisi,
        date: LocalDate,
    ): Resultat<PlantDerive> {
        if (!EmpriseLambert93.FRANCE_METROPOLITAINE.contient(piquet)) {
            return Resultat.Refuse(RefusRepere.PiquetHorsFrance(piquet.xMetres, piquet.yMetres))
        }
        if (!distanceHorizontaleM.isFinite() || distanceHorizontaleM < 0.0) {
            return Resultat.Refuse(RefusRepere.DistanceInvalide(distanceHorizontaleM))
        }
        val geo = PointGeographique.depuisLambert93(piquet)
        return ChaineAzimut.orienter(azimut, geo, date).puis { o ->
            val a = Math.toRadians(o.azimutGrilleDeg)
            val dx = distanceHorizontaleM * sin(a)
            val dy = distanceHorizontaleM * cos(a)
            Resultat.Accepte(
                PlantDerive(
                    piquet, epsg, distanceHorizontaleM, azimut, date, o, dx, dy,
                    PointLambert93(piquet.xMetres + dx, piquet.yMetres + dy),
                ),
            )
        }
    }
}
