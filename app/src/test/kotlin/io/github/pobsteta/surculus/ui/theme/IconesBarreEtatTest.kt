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

    @Test fun `carte et guidage, icones selon le theme`() {
        assertFalse(iconesBarreEtatClaires(ecranAvecBandeau = false, sombre = false))
        assertTrue(iconesBarreEtatClaires(ecranAvecBandeau = false, sombre = true))
    }
}
