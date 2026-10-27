/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.carte

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import io.github.pobsteta.surculus.core.gnss.QualiteFix
import io.github.pobsteta.surculus.gpkg.Teinte
import io.github.pobsteta.surculus.ui.theme.LocalSombre

/**
 * Contraste WCAG entre deux couleurs opaques, de 1 à 21 (mission 051) : (L claire + 0,05) / (L sombre + 0,05).
 * Seuil de lisibilité du texte : [CONTRASTE_MINIMAL].
 */
fun contraste(a: Color, b: Color): Float {
    val haut = maxOf(a.luminance(), b.luminance())
    val bas = minOf(a.luminance(), b.luminance())
    return (haut + 0.05f) / (bas + 0.05f)
}

const val CONTRASTE_MINIMAL = 4.5f

/**
 * Mission 051 : en thème sombre, les teintes d'état (vert, orange, rouge, noir de [Palette]) tombent sous 4,5:1 sur le
 * fond (voir `ContrasteTest`). On ne change pas ces couleurs : le texte d'état se pose sur une plaque blanche, comme le
 * badge du fix pose du blanc sur sa teinte. En thème clair, rien ne change.
 */
@Composable
fun Modifier.plaqueEtat(): Modifier =
    if (LocalSombre.current) clip(RoundedCornerShape(6.dp)).background(Palette.Blanc).padding(horizontal = 8.dp, vertical = 2.dp) else this

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

    /**
     * Teinte d'une placette selon son `etat_suivi` (pilotage/journal.md, 2026-10-22, décisions de Pascal, mission 033).
     * Même palette que le fix, sens différent : le badge est un texte en bandeau, la placette un disque sur la carte,
     * et la forme du disque redit l'état.
     */
    fun de(t: Teinte): Color = when (t) {
        Teinte.ROUGE -> Rouge
        Teinte.ORANGE -> Orange
        Teinte.VERT -> Vert
        Teinte.NOIR -> Noir
    }
}
