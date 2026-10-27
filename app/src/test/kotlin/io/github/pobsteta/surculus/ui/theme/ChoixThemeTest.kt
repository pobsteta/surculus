/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChoixThemeTest {
    @Test fun `Systeme suit le telephone`() {
        assertTrue(ChoixTheme.SYSTEME.estSombre(systemeSombre = true))
        assertFalse(ChoixTheme.SYSTEME.estSombre(systemeSombre = false))
    }

    @Test fun `Clair force le clair, meme si le telephone est en sombre`() {
        assertFalse(ChoixTheme.CLAIR.estSombre(systemeSombre = true))
        assertFalse(ChoixTheme.CLAIR.estSombre(systemeSombre = false))
    }

    @Test fun `Sombre force le sombre, meme si le telephone est en clair`() {
        assertTrue(ChoixTheme.SOMBRE.estSombre(systemeSombre = true))
        assertTrue(ChoixTheme.SOMBRE.estSombre(systemeSombre = false))
    }

    @Test fun `le defaut est Systeme quand rien n'est enregistre ou que le nom est inconnu`() {
        assertEquals(ChoixTheme.SYSTEME, ChoixTheme.depuis(null))
        assertEquals(ChoixTheme.SYSTEME, ChoixTheme.depuis("violet"))
    }

    @Test fun `chaque choix est relu par son nom`() {
        ChoixTheme.entries.forEach { assertEquals(it, ChoixTheme.depuis(it.name)) }
    }
}
