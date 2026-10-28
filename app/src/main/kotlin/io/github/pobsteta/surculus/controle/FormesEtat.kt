/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.controle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.pobsteta.surculus.carte.CouleursEtat
import io.github.pobsteta.surculus.carte.Palette
import io.github.pobsteta.surculus.core.placette.EtatDsf
import io.github.pobsteta.surculus.ui.theme.LocalSombre

/** La forme redit l'état : la couleur ne le porte jamais seule (décision de Pascal du 2026-10-27, § 7 du plan). */
enum class FormeEtat(val symbole: String) {
    DISQUE("●"),
    TRIANGLE("▲"),
    CROIX("✖"),
    ANNEAU("○"),
}

/** Sain vert ●, atteint orange ▲, mort rouge ✖, absent noir ○ : les quatre teintes de [Palette], rien d'autre (règle du 1664). */
object ApparenceEtat {
    fun forme(etat: EtatDsf): FormeEtat = when (etat) {
        EtatDsf.SAIN -> FormeEtat.DISQUE
        EtatDsf.ATTEINT -> FormeEtat.TRIANGLE
        EtatDsf.MORT -> FormeEtat.CROIX
        EtatDsf.ABSENT -> FormeEtat.ANNEAU
    }

    fun teinte(etat: EtatDsf): Color = when (etat) {
        EtatDsf.SAIN -> Palette.Vert
        EtatDsf.ATTEINT -> Palette.Orange
        EtatDsf.MORT -> Palette.Rouge
        EtatDsf.ABSENT -> Palette.Noir
    }
}

/** Dimensions de l'écran : cibles tactiles pour un doigt ganté (jamais sous 48 dp). */
object DimensionsControle {
    val CIBLE_MIN = 48.dp
    val BOUTON_ETAT = 64.dp
    val BOUTON_CAUSE = 56.dp
    val ACTION = 56.dp
    val FLECHE = 48.dp
    val PASTILLE = 28.dp

    fun toutes() = listOf(CIBLE_MIN, BOUTON_ETAT, BOUTON_CAUSE, ACTION, FLECHE)
}

/**
 * Pastille d'un état : sa forme dans sa teinte. En sombre, cernée d'un filet blanc (règle de la 063) ; en clair, nue.
 */
@Composable
fun PastilleForme(etat: EtatDsf, taille: Dp = DimensionsControle.PASTILLE, modifier: Modifier = Modifier) {
    val sombre = LocalSombre.current
    Canvas(modifier.size(taille)) {
        dessinerEtat(etat, Offset(size.width / 2, size.height / 2), size.minDimension, sombre)
    }
}

/** Une pastille d'état de côté [cote] (pixels) centrée en [centre] ; en sombre, cernée d'un filet blanc (règle de la 063). Aussi pour le plan. */
fun DrawScope.dessinerEtat(etat: EtatDsf, centre: Offset, cote: Float, sombre: Boolean) {
    val trait = cote / 6f
    val filet = if (sombre) cote / 12f else 0f
    val forme = ApparenceEtat.forme(etat)
    if (sombre) dessiner(forme, CouleursEtat.filet, trait + 2 * filet, filet * 2, centre, cote)
    dessiner(forme, ApparenceEtat.teinte(etat), trait, 0f, centre, cote)
}

private fun DrawScope.dessiner(forme: FormeEtat, couleur: Color, trait: Float, elargi: Float, c: Offset, cote: Float) {
    val marge = cote / 6f
    val r = cote / 2 - marge
    when (forme) {
        FormeEtat.DISQUE -> drawCircle(couleur, radius = r + elargi / 2, center = c, style = Fill)
        FormeEtat.ANNEAU -> drawCircle(couleur, radius = r, center = c, style = Stroke(width = trait))
        FormeEtat.TRIANGLE -> {
            val p = Path().apply {
                moveTo(c.x, c.y - r - elargi / 2)
                lineTo(c.x + r + elargi / 2, c.y + r * 0.8f + elargi / 2)
                lineTo(c.x - r - elargi / 2, c.y + r * 0.8f + elargi / 2)
                close()
            }
            drawPath(p, couleur, style = Fill)
        }
        FormeEtat.CROIX -> {
            drawLine(couleur, Offset(c.x - r, c.y - r), Offset(c.x + r, c.y + r), strokeWidth = trait, cap = StrokeCap.Round)
            drawLine(couleur, Offset(c.x - r, c.y + r), Offset(c.x + r, c.y - r), strokeWidth = trait, cap = StrokeCap.Round)
        }
    }
}
