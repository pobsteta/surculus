package io.github.pobsteta.surculus.controle

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Mission 072 : le bouton micro hors poche montre l'icône du micro (comme Marculus), pas une phrase. Pas d'infrastructure de test
 * Compose dans le projet : on garde la garde sur les sources (le dossier de travail des tests unitaires est `app/`).
 */
class BoutonMicroIconeTest {
    private fun source(chemin: String): String {
        val f = File(chemin)
        require(f.exists()) { "source introuvable : ${f.absolutePath}" }
        return f.readText()
    }

    private fun corpsBoutonMicro(): String {
        val s = source("src/main/kotlin/io/github/pobsteta/surculus/controle/EcranControle.kt")
        val debut = s.indexOf("private fun BoutonMicro(")
        val fin = s.indexOf("@Composable", debut)
        assertTrue(debut >= 0 && fin > debut, "BoutonMicro introuvable")
        return s.substring(debut, fin)
    }

    @Test fun `le bouton micro montre l'icone Mic et plus de texte visible`() {
        val corps = corpsBoutonMicro()
        assertTrue("Icons.Filled.Mic" in corps, "l'icône Mic est attendue")
        assertFalse(Regex("""\bText\(""").containsMatchIn(corps), "aucun Text visible dans le bouton micro")
    }

    @Test fun `le texte reste en description d'accessibilite - un seul bouton, la description change selon l'etat`() {
        val corps = corpsBoutonMicro()
        assertTrue(Regex("""contentDescription\s*=\s*libelle""").containsMatchIn(corps))
        assertTrue("R.string.controle_micro_ecoute" in corps && "R.string.controle_micro)" in corps)
        assertEquals(1, Regex("""OutlinedButton\(""").findAll(corps).count(), "un seul composable sous le doigt")
    }

    @Test fun `les descriptions sont celles de la fiche`() {
        val xml = source("src/main/res/values/strings_controle.xml")
        assertTrue("""<string name="controle_micro">Tenir pour parler</string>""" in xml)
        assertTrue("""<string name="controle_micro_ecoute">J\'écoute…</string>""" in xml)
    }
}
