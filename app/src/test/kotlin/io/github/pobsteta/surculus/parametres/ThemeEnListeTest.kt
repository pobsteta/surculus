/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.parametres

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Thème en liste déroulante (mission 056), comme Marculus : lecture des sources, pas de bouton radio. */
class ThemeEnListeTest {
    private val source = File("src/main/kotlin/io/github/pobsteta/surculus/parametres/EcranParametres.kt")
        .also { assertTrue(it.exists(), "absent : ${it.path}") }.readText()

    @Test fun `le theme est une liste deroulante`() {
        assertTrue("DropdownMenu(" in source)
        assertTrue("Role.DropdownList" in source)
        assertTrue("Icons.Default.ArrowDropDown" in source)
        assertTrue("Icons.Default.Check" in source)
    }

    @Test fun `aucun bouton radio dans Parametres`() {
        assertFalse("RadioButton" in source)
    }

    @Test fun `les cibles du menu font au moins 48 dp et aucune taille de police en dur`() {
        assertTrue(source.contains("DropdownMenuItem(") && source.contains("heightIn(min = 48.dp)"))
        assertFalse("fontSize" in source)
    }
}
