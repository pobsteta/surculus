/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.carte

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.pobsteta.surculus.core.gnss.QualiteFix
import io.github.pobsteta.surculus.gpkg.EtatSuivi
import io.github.pobsteta.surculus.gpkg.FormePastille
import io.github.pobsteta.surculus.gpkg.Pastille
import io.github.pobsteta.surculus.gpkg.Teinte
import io.github.pobsteta.surculus.ui.theme.LocalSombre
import io.github.pobsteta.surculus.ui.theme.SchemaClair
import io.github.pobsteta.surculus.ui.theme.SchemaSombre

/**
 * Contraste WCAG entre deux couleurs opaques, de 1 à 21 (mission 051) : (L claire + 0,05) / (L sombre + 0,05).
 * Seuil de lisibilité du texte : [CONTRASTE_MINIMAL] ; seuil d'une pastille (WCAG 1.4.11) : [CONTRASTE_PASTILLE].
 */
fun contraste(a: Color, b: Color): Float {
    val haut = maxOf(a.luminance(), b.luminance())
    val bas = minOf(a.luminance(), b.luminance())
    return (haut + 0.05f) / (bas + 0.05f)
}

const val CONTRASTE_MINIMAL = 4.5f

/** Contraste minimal d'un élément qui n'est pas du texte contre ses voisins (WCAG 2.2, 1.4.11). */
const val CONTRASTE_PASTILLE = 3f

/**
 * Couleurs d'état lues selon le thème (mission 063, décision de Pascal du 2026-10-27 ; remplace la plaque blanche de la 051).
 * En clair, rien ne change. En sombre, aucun texte n'est écrit en couleur d'état : le texte est en `onSurface`, les
 * erreurs et refus en `error` du thème (comme Marculus), et l'état est porté par une pastille cernée d'un filet blanc
 * ([PastilleEtat], [PastilleDisque]). Fonctions pures : le test juge les mêmes valeurs que l'écran.
 */
object CouleursEtat {
    /** Texte neutre : noir en clair, `onSurface` en sombre. */
    fun neutre(sombre: Boolean): Color = if (sombre) SchemaSombre.onSurface else Palette.Noir

    /** Texte d'erreur ou de refus : rouge d'état en clair, `error` du thème en sombre. */
    fun erreur(sombre: Boolean): Color = if (sombre) SchemaSombre.error else Palette.Rouge

    /** Texte d'un état (vert, orange, noir) : sa teinte en clair, `onSurface` en sombre. */
    fun texte(teinte: Color, sombre: Boolean): Color = if (sombre) SchemaSombre.onSurface else teinte

    /** Fond réel sous les textes et les pastilles (Surface de MainActivity = `surface`). */
    fun fond(sombre: Boolean): Color = if (sombre) SchemaSombre.surface else SchemaClair.surface

    /** Filet des pastilles : blanc, en sombre seulement. */
    val filet: Color = Color(0xFFFFFFFF)
}

@Composable fun couleurNeutre(): Color = CouleursEtat.neutre(LocalSombre.current)

@Composable fun couleurErreur(): Color = CouleursEtat.erreur(LocalSombre.current)

@Composable fun couleurTexteEtat(teinte: Color): Color = CouleursEtat.texte(teinte, LocalSombre.current)

/** Texte d'un état : en clair, dans sa teinte ; en sombre, en `onSurface` précédé d'une pastille cernée de blanc. */
@Composable
fun TexteEtat(
    texte: String,
    teinte: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight? = null,
) {
    val sombre = LocalSombre.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (sombre) {
            PastilleDisque(teinte, 14.dp)
            Spacer(Modifier.width(6.dp))
        }
        Text(texte, style = style, fontWeight = fontWeight, color = CouleursEtat.texte(teinte, sombre))
    }
}

/** Disque de couleur cerné d'un filet blanc (en clair : disque nu, comme avant). */
@Composable
fun PastilleDisque(teinte: Color, taille: Dp) {
    val sombre = LocalSombre.current
    val filet = if (sombre) (taille.value / 7f).dp else 0.dp
    Box(Modifier.size(taille).background(if (sombre) CouleursEtat.filet else Color.Transparent, CircleShape).padding(filet).background(teinte, CircleShape))
}

/**
 * Pastille d'une placette (liste, légende) : disque plein, anneau, ou disque noir marqué d'un « ? ». En sombre, un filet
 * blanc l'entoure (comme sur la carte) ; en clair, rien ne change.
 */
@Composable
fun PastilleEtat(etat: EtatSuivi, taille: Dp, grisee: Boolean = false) {
    val p = Pastille.de(etat)
    val teinte = Palette.de(p.teinte)
    val couleur = if (grisee) teinte.copy(alpha = 0.55f) else teinte
    val sombre = LocalSombre.current
    val filet = if (sombre) (taille.value / 14f).dp else 0.dp
    val lisere = if (sombre) Modifier.border(filet, CouleursEtat.filet, CircleShape) else Modifier
    val epaisseur = (taille.value * 5f / 28f).dp
    val blanc = if (sombre) CouleursEtat.filet else Color.Transparent
    when (p.forme) {
        FormePastille.ANNEAU -> Box(Modifier.size(taille).then(lisere).padding(filet).border(epaisseur, couleur, CircleShape).padding(epaisseur).then(lisere))
        FormePastille.DISQUE_PLEIN -> Box(Modifier.size(taille).background(blanc, CircleShape).padding(filet).background(couleur, CircleShape))
        FormePastille.DISQUE_AVEC_POINT_INTERROGATION -> Box(
            Modifier.size(taille).background(blanc, CircleShape).padding(filet).background(couleur, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("?", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

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
