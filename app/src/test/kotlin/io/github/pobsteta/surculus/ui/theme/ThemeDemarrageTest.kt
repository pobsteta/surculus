/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.ui.theme

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Thème de démarrage (mission 051), comme Marculus : clair par défaut, sombre quand le téléphone est en sombre,
 * pour qu'il n'y ait pas d'éclair blanc au lancement. Le manifeste pointe sur ce thème.
 */
class ThemeDemarrageTest {
    private val res = File("src/main/res")

    private fun lire(chemin: String) = File(res, chemin).also { assertTrue(it.exists(), "absent : ${it.path}") }.readText()

    @Test fun `le theme de demarrage est clair par defaut`() {
        assertTrue("@android:style/Theme.Material.Light.NoActionBar" in lire("values/themes.xml"))
    }

    @Test fun `le theme de demarrage est sombre quand le telephone est en sombre`() {
        val nuit = lire("values-night/themes.xml")
        assertTrue("@android:style/Theme.Material.NoActionBar" in nuit && "Light" !in nuit)
    }

    @Test fun `le manifeste utilise le theme de demarrage`() {
        assertTrue("android:theme=\"@style/Theme.Surculus\"" in File("src/main/AndroidManifest.xml").readText())
    }
}
