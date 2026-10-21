/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.carte

import androidx.compose.ui.graphics.Color
import io.github.pobsteta.surculus.core.gnss.QualiteFix

/** Quatre couleurs au plus (règle du 1664), contrastées pour le plein soleil : noir sur blanc et trois teintes d'état. */
object Palette {
    val Vert = Color(0xFF0B6B2E)
    val Orange = Color(0xFFB45300)
    val Rouge = Color(0xFFB00020)
    val Noir = Color(0xFF000000)
    val Blanc = Color(0xFFFFFFFF)

    /** Teinte du badge du fix : RTK fixe en vert, flottant et DGPS en orange, autonome ou rien en rouge. */
    fun du(q: QualiteFix?): Color = when (q) {
        QualiteFix.RTK_FIXE -> Vert
        QualiteFix.RTK_FLOTTANT, QualiteFix.DGPS -> Orange
        else -> Rouge
    }
}
