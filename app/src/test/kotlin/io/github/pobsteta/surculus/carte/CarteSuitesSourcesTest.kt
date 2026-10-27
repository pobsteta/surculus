package io.github.pobsteta.surculus.carte

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Mission 057 : plus de bouton « Ma position » ; le marqueur passe par `MarqueurPosition`. Lecture des sources. */
class CarteSuitesSourcesTest {
    private val main = File("src/main").takeIf { it.isDirectory } ?: File("app/src/main")
    private val ecran by lazy { File(main, "kotlin/io/github/pobsteta/surculus/carte/EcranCarte.kt").readText() }

    @Test
    fun `plus de bouton Ma position ni de chaine associee`() {
        assertTrue(ecran.isNotEmpty())
        val motif = Regex("carte_ma_position|centrerSurMoi|Ma position")
        val trouves = main.walkTopDown()
            .filter { it.isFile && it.extension in setOf("kt", "xml") && motif.containsMatchIn(it.readText()) }
            .map { it.name }.toList()
        assertEquals(emptyList<String>(), trouves)
    }

    @Test
    fun `le bouton recentrer reste`() {
        assertTrue("MyLocation" in ecran)
    }

    @Test
    fun `le marqueur n'est active que par MarqueurPosition`() {
        assertTrue("MarqueurPosition.aDessiner" in ecran)
        assertFalse(Regex("l\\.position\\.isEnabled\\s*=\\s*fix\\s*!=\\s*null").containsMatchIn(ecran))
    }
}
