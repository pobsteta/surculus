/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.ui.theme

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IconesBarreEtatTest {
    @Test fun `ecran a bandeau, icones claires en clair comme en sombre`() {
        assertTrue(iconesBarreEtatClaires(ecranAvecBandeau = true, sombre = false))
        assertTrue(iconesBarreEtatClaires(ecranAvecBandeau = true, sombre = true))
    }

    @Test fun `un ecran sans bandeau suivrait le theme`() {
        assertFalse(iconesBarreEtatClaires(ecranAvecBandeau = false, sombre = false))
        assertTrue(iconesBarreEtatClaires(ecranAvecBandeau = false, sombre = true))
    }

    /** Mission 055 : la carte et le guidage ont un bandeau compact, donc tous les écrans : MainActivity ne distingue plus. */
    @Test fun `MainActivity compte la carte et le guidage parmi les ecrans a bandeau`() {
        val racine = java.io.File("src/main/kotlin/io/github/pobsteta/surculus").takeIf { it.isDirectory }
            ?: java.io.File("app/src/main/kotlin/io/github/pobsteta/surculus")
        val source = java.io.File(racine, "MainActivity.kt").readText()
        assertFalse("ecranAvecBandeau = nav.tourneeId == null" in source)
        assertTrue("ecranAvecBandeau = true" in source)
        assertTrue("isAppearanceLightNavigationBars = !sombre" in source)
    }
}
