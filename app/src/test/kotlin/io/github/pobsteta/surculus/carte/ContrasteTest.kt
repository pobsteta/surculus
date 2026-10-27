/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.carte

import androidx.compose.ui.graphics.Color
import io.github.pobsteta.surculus.ui.theme.SchemaClair
import io.github.pobsteta.surculus.ui.theme.SchemaSombre
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Contraste de la palette d'état (mission 051) : sur le fond clair, sur le fond sombre, et sur la plaque blanche du sombre. */
class ContrasteTest {
    private val teintes = mapOf("Vert" to Palette.Vert, "Orange" to Palette.Orange, "Rouge" to Palette.Rouge, "Noir" to Palette.Noir)

    @Test fun `la formule donne 21 pour noir sur blanc et 1 pour deux couleurs egales`() {
        assertEquals(21f, contraste(Color.Black, Color.White), 0.01f)
        assertEquals(1f, contraste(Color.White, Color.White), 0.001f)
    }

    @Test fun `en clair, chaque teinte d'etat se lit sur le fond (4,5 au moins)`() {
        teintes.forEach { (nom, c) ->
            assertTrue(contraste(c, SchemaClair.background) >= CONTRASTE_MINIMAL, "$nom sur fond clair : ${contraste(c, SchemaClair.background)}")
        }
    }

    @Test fun `en sombre, chaque teinte d'etat se lit sur la plaque blanche (4,5 au moins)`() {
        teintes.forEach { (nom, c) ->
            assertTrue(contraste(c, Palette.Blanc) >= CONTRASTE_MINIMAL, "$nom sur plaque blanche : ${contraste(c, Palette.Blanc)}")
        }
    }

    @Test fun `en sombre, aucune teinte d'etat ne se lit directement sur le fond, d'ou la plaque`() {
        teintes.forEach { (nom, c) ->
            assertTrue(contraste(c, SchemaSombre.background) < CONTRASTE_MINIMAL, "$nom sur fond sombre : ${contraste(c, SchemaSombre.background)}")
        }
    }
}
