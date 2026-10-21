/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.core.guidage

import java.util.Locale

/** Libellés lus sur le terrain : des mètres, un cap en degrés avec son point cardinal. */
object Libelles {
    /** « 85 m » sous le kilomètre, « 1,3 km » au-delà (le forestier lit des mètres). */
    fun distance(m: Double): String =
        if (m < 1000.0) "${m.toInt()} m" else String.format(Locale.FRANCE, "%.1f km", m / 1000.0)

    /** Huit points cardinaux, en français : N, NE, E, SE, S, SO, O, NO. [azimutDeg] dans [0, 360[. */
    fun pointCardinal(azimutDeg: Double): String {
        val noms = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
        return noms[(((azimutDeg % 360.0) + 22.5) / 45.0).toInt() % 8]
    }

    /** « 047° NE » : trois chiffres, jamais « 360° » (359,6° s'arrondit à 000°). */
    fun cap(azimutDeg: Double): String = "%03d° %s".format(Math.round(azimutDeg).toInt() % 360, pointCardinal(azimutDeg))
}
