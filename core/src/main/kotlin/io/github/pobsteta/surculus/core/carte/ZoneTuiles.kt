/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 *
 * Origine : dépôt Groma (pobsteta/groma), fichier android/groma-core/src/main/kotlin/groma/core/ZoneTuiles.kt,
 *           lu en l'état de la copie locale (2026-10-21). Reprise prévue par l'ADR 0009, avant l'extraction en bibliothèque commune.
 * Modifications : paquet ; emprise en degrés de Surculus (`EmpriseDegres`) ; zone construite depuis une emprise Lambert-93,
 *           refusée hors de France métropolitaine ou trop grande (`Resultat`) ; commentaires en français.
 */
package io.github.pobsteta.surculus.core.carte

import io.github.pobsteta.surculus.core.geo.EmpriseDegres
import io.github.pobsteta.surculus.core.geo.EmpriseLambert93
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.geo.Projections
import io.github.pobsteta.surculus.core.placette.EmpriseEtFrance
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

/** Une tuile Web Mercator (schéma XYZ, comme osmdroid et la grille `PM_0_19` de l'IGN). */
data class Tuile(val z: Int, val x: Int, val y: Int)

/**
 * Tuiles à précharger pour travailler hors ligne (ADR 0009) : l'emprise de la tournée plus une marge, des zooms
 * [zoomMin] à [zoomMax]. Reprise de `groma-core/…/ZoneTuiles.kt` (Groma, lecture seule), avec une emprise en degrés
 * (latitude, longitude) et un contrôle en France métropolitaine.
 */
class ZoneTuiles(val emprise: EmpriseDegres, val zoomMin: Int = ZOOM_MIN, val zoomMax: Int = ZOOM_MAX) {
    init { require(zoomMin in 0..zoomMax) { "Zooms invalides : $zoomMin à $zoomMax" } }

    /** Plages de colonnes et de lignes au zoom [z]. */
    fun plage(z: Int): Pair<IntRange, IntRange> {
        val (x0, y0) = tuile(emprise.longitudeMinDeg, emprise.latitudeMaxDeg, z)
        val (x1, y1) = tuile(emprise.longitudeMaxDeg, emprise.latitudeMinDeg, z)
        return (x0..x1) to (y0..y1)
    }

    val nombre: Long get() = (zoomMin..zoomMax).sumOf { z -> plage(z).let { (xs, ys) -> xs.count().toLong() * ys.count() } }

    /** Toutes les tuiles, zoom par zoom (les petits zooms d'abord : une carte utile tôt). */
    fun tuiles(): Sequence<Tuile> = sequence {
        for (z in zoomMin..zoomMax) {
            val (xs, ys) = plage(z)
            for (x in xs) for (y in ys) yield(Tuile(z, x, y))
        }
    }

    sealed interface Resultat {
        data class Zone(val zone: ZoneTuiles) : Resultat

        /** Un coin de l'emprise tombe hors de France métropolitaine : axes inversés ou oubli de reprojection (ADR 0009). */
        data class HorsFrance(val coin: PointLambert93, val inversionDetectee: Boolean) : Resultat

        /** Plus de [NOMBRE_MAX] tuiles : une forêt, pas un département. */
        data class TropGrande(val nombre: Long) : Resultat
    }

    companion object {
        /** Zooms 12 à 18 (décision de Pascal, 2026-10-21 ; ceux de Groma). */
        const val ZOOM_MIN = 12
        const val ZOOM_MAX = 18
        const val MARGE_M = 200.0

        /** Au-delà, le préchargement est refusé (usage raisonnable de la Géoplateforme). */
        const val NOMBRE_MAX = 30_000L

        private const val M_PAR_DEGRE = 111_320.0
        private const val LAT_MAX = 85.05112878

        /** Zone d'une tournée : son emprise Lambert-93 élargie de [margeM] mètres de chaque côté, testée en France. */
        fun pourTournee(e: EmpriseLambert93, margeM: Double = MARGE_M): Resultat {
            val coins = listOf(
                PointLambert93(e.xMinMetres, e.yMinMetres), PointLambert93(e.xMaxMetres, e.yMaxMetres),
                PointLambert93(e.xMinMetres, e.yMaxMetres), PointLambert93(e.xMaxMetres, e.yMinMetres),
            )
            for (c in coins) {
                val ctl = EmpriseEtFrance.controler(c)
                if (!ctl.enFrance) return Resultat.HorsFrance(c, ctl.inversionDetectee)
            }
            val lonLat = coins.map { Projections.versWgs84(it.xMetres, it.yMetres) }
            val lonMin = lonLat.minOf { it.first }
            val lonMax = lonLat.maxOf { it.first }
            val latMin = lonLat.minOf { it.second }
            val latMax = lonLat.maxOf { it.second }
            val dLat = margeM / M_PAR_DEGRE
            val dLon = margeM / (M_PAR_DEGRE * cos(Math.toRadians((latMin + latMax) / 2)))
            val zone = ZoneTuiles(EmpriseDegres(latMin - dLat, lonMin - dLon, latMax + dLat, lonMax + dLon))
            return if (zone.nombre > NOMBRE_MAX) Resultat.TropGrande(zone.nombre) else Resultat.Zone(zone)
        }

        /** Tuile (x, y) qui contient (lon, lat) au zoom [z]. */
        fun tuile(lon: Double, lat: Double, z: Int): Pair<Int, Int> {
            val n = 1 shl z
            val phi = Math.toRadians(lat.coerceIn(-LAT_MAX, LAT_MAX))
            val x = floor((lon + 180.0) / 360.0 * n).toInt().coerceIn(0, n - 1)
            val y = floor((1.0 - ln(tan(phi) + 1.0 / cos(phi)) / PI) / 2.0 * n).toInt().coerceIn(0, n - 1)
            return x to y
        }
    }
}
