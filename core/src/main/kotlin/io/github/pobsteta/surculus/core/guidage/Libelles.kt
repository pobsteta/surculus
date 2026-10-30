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
    /** Mètres entiers arrondis, milliers séparés par l'espace fine insécable U+202F : 9979,54 -> « 9 980 m ». */
    fun metres(m: Double): String =
        Math.round(m).toString().reversed().chunked(3).joinToString("\u202F").reversed() + " m"

    /**
     * L'unique écriture d'une distance de navigation : « 85 m » sous le kilomètre, « 1,3 km » (une décimale, virgule) au-delà.
     * Le seuil se juge sur la valeur **arrondie** (999,5 m -> « 1,0 km », jamais « 1 000 m »), et l'on arrondit : plus de troncature.
     */
    fun distance(m: Double): String =
        if (Math.round(m) < 1000L) metres(m) else String.format(Locale.FRANCE, "%.1f km", m / 1000.0)

    /** Distance au décimètre, virgule française, sans unité : 7,912 -> « 7,9 » (pointage et alerte « hors rayon »). */
    fun decimetres(m: Double): String = String.format(Locale.FRANCE, "%.1f", m)

    /** Huit points cardinaux, en français : N, NE, E, SE, S, SO, O, NO. [azimutDeg] dans [0, 360[. */
    fun pointCardinal(azimutDeg: Double): String {
        val noms = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
        return noms[(((azimutDeg % 360.0) + 22.5) / 45.0).toInt() % 8]
    }

    /** « 047° NE » : trois chiffres, jamais « 360° » (359,6° s'arrondit à 000°). */
    fun cap(azimutDeg: Double): String = "%03d° %s".format(Math.round(azimutDeg).toInt() % 360, pointCardinal(azimutDeg))
}
