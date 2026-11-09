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
        // Garde muette de la 104 : « heightIn(min = 48.dp) » est écrit ailleurs dans le fichier ; un item du menu ou la ligne qui
        // l'ouvre ramenés sous 48 dp passaient. On juge donc le voisinage de chacun.
        val item = source.indexOf("DropdownMenuItem(")
        assertTrue("heightIn(min = 48.dp)" in source.substring(item, item + 120), "l'item du menu doit faire 48 dp au moins")
        val ouvre = source.indexOf(".clickable(role = Role.DropdownList)")
        assertTrue(ouvre > 0, "la ligne qui ouvre la liste a disparu")
        assertTrue("heightIn(min = 48.dp)" in source.substring(maxOf(0, ouvre - 80), ouvre), "la ligne qui ouvre la liste doit faire 48 dp au moins")
        assertFalse("fontSize" in source)
    }
}
