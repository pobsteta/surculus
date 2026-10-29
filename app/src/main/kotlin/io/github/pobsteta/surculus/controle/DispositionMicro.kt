/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.controle

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Mission 075 : dimensions du bouton micro, mesurées sur celui de Marculus (`DicteeVocale.kt`). */
object DimensionsMicro {
    val BOUTON = 112.dp
    val ICONE = 48.dp

    /** Hauteur du pied (debout) ou largeur de la colonne (couché) : le bouton et 8 dp de marge de chaque côté. */
    val ZONE = 128.dp
}

enum class EmplacementMicro {
    /** Debout : pied fixe sous la zone qui défile, micro à droite. */
    PIED,

    /** Couché : colonne fixe à droite, micro en bas. */
    COLONNE,
}

/** Où se pose le micro : fonction de l'écran seulement, jamais de l'écoute (rien ne bouge quand « J'ÉCOUTE » apparaît). */
object DispositionMicro {
    fun emplacement(largeurDp: Float, hauteurDp: Float): EmplacementMicro =
        if (largeurDp > hauteurDp) EmplacementMicro.COLONNE else EmplacementMicro.PIED
}

/** Couleurs du micro : les rôles du thème, comme Marculus (`primaryContainer`, `error`, `surfaceVariant`). */
object CouleursMicro {
    fun fond(e: EtatMicro, s: ColorScheme): Color = when (e) {
        EtatMicro.PRET -> s.primaryContainer
        EtatMicro.ECOUTE -> s.error
        EtatMicro.BARRE -> s.surfaceVariant
    }

    fun icone(e: EtatMicro, s: ColorScheme): Color = when (e) {
        EtatMicro.PRET -> s.onPrimaryContainer
        EtatMicro.ECOUTE -> s.onError
        EtatMicro.BARRE -> s.onSurfaceVariant
    }

    /** Filet de 2 dp, comme les cadres de l'écran : le bouton se distingue du fond quel que soit son état. */
    fun filet(s: ColorScheme): Color = s.onSurface
}
