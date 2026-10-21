/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.core.guidage

import io.github.pobsteta.surculus.core.geo.EmpriseDegres
import io.github.pobsteta.surculus.core.geo.Geodesie
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.geo.Projections
import io.github.pobsteta.surculus.core.gnss.Position
import kotlin.math.hypot

/** Où aller : distance horizontale (m) et azimut **géographique** (° depuis le nord vrai, sens horaire, [0, 360[). */
data class Cap(val distanceM: Double, val azimutDeg: Double) {
    val arrive: Boolean get() = Guidage.estArrive(distanceM)
}

object Guidage {
    /** Cercle de la placette : arrivée à une distance **strictement inférieure** à 30 m (décision de Pascal, 2026-10-21). */
    const val SEUIL_ARRIVEE_M = 30.0

    /** Distance à partir de laquelle un nouveau bip d'arrivée est permis, après une sortie du cercle (évite le bégaiement). */
    const val SEUIL_REARMEMENT_M = 40.0

    fun estArrive(distanceM: Double): Boolean = distanceM < SEUIL_ARRIVEE_M

    /** Position GNSS (latitude, longitude) en Lambert-93 ; `null` hors de France (axes inversés, position nulle). */
    fun versLambert93(p: Position): PointLambert93? {
        if (!EmpriseDegres.FRANCE_METROPOLITAINE.contient(p.latitude, p.longitude)) return null
        val (x, y) = Projections.versLambert93(lon = p.longitude, lat = p.latitude)
        return PointLambert93(x, y)
    }

    /**
     * Distance et azimut de [depuis] vers [cible] (Lambert-93). `null` si [depuis] est hors de France.
     * Distance : plan Lambert-93 (mètres, comme le rattachement à 15 m) ; azimut : géographique, calculé en WGS 84.
     */
    fun cap(depuis: Position, cible: PointLambert93): Cap? {
        val a = versLambert93(depuis) ?: return null
        val distance = hypot(a.xMetres - cible.xMetres, a.yMetres - cible.yMetres)
        val (lon, lat) = Projections.versWgs84(cible.xMetres, cible.yMetres)
        return Cap(distance, Geodesie.azimutDeg(depuis, Position(latitude = lat, longitude = lon)))
    }

    /**
     * Période du bip (ms) : 2 s à 180 m et au-delà, de plus en plus rapide à l'approche, 0,5 s à 30 m.
     * `null` dans le cercle : le bip d'arrivée remplace le bip périodique.
     */
    fun periodeBipMs(distanceM: Double): Long? {
        if (estArrive(distanceM)) return null
        return (200.0 + 10.0 * distanceM).coerceIn(500.0, 2000.0).toLong()
    }
}

/**
 * Détecte l'entrée dans le cercle : un seul bip d'arrivée par entrée, ré-armé au-delà de [Guidage.SEUIL_REARMEMENT_M]
 * (le GNSS oscille autour de 30 m).
 */
class DetecteurArrivee {
    private var dedans = false

    /** Vrai une seule fois à chaque entrée dans le cercle. */
    fun entree(distanceM: Double): Boolean {
        if (!dedans && Guidage.estArrive(distanceM)) {
            dedans = true
            return true
        }
        if (dedans && distanceM >= Guidage.SEUIL_REARMEMENT_M) dedans = false
        return false
    }
}

/** Moyenne de quelques positions (Lambert-93) avant de rattacher la placette (spec § 4.3). */
class MoyennePosition {
    private var n = 0
    private var sx = 0.0
    private var sy = 0.0

    val nombre: Int get() = n

    fun ajouter(p: PointLambert93) { n++; sx += p.xMetres; sy += p.yMetres }

    fun moyenne(): PointLambert93? = if (n == 0) null else PointLambert93(sx / n, sy / n)
}
