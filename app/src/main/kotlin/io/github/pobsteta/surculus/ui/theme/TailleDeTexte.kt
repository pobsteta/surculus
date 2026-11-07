package io.github.pobsteta.surculus.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/**
 * Le même style du thème à une autre taille (en sp). Les tailles se posent ici, dans le thème, et non dans l'écran du
 * contrôle, où aucun `fontSize` n'est permis (garde de `BoussoleTest`). Seul usage : le libellé sous un code de la grille
 * du besoin, 11 sp puis 10 sp au plus petit (mission 102, décision de Pascal).
 */
fun TextStyle.enSp(taille: Float): TextStyle = copy(fontSize = taille.sp)
