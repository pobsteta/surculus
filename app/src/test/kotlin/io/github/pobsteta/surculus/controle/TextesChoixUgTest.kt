package io.github.pobsteta.surculus.controle

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Mission 086, reprise 1/3 : textes de l'écran de choix de l'UG et du guidage (lus dans les ressources, sans Android). */
class TextesChoixUgTest {
    private fun chaine(fichier: String, nom: String): String {
        val dossiers = listOf("src/main/res/values", "app/src/main/res/values")
        val f = dossiers.map { File("$it/$fichier") }.first { it.exists() }
        val m = Regex("<string name=\"$nom\"[^>]*>(.*?)</string>", RegexOption.DOT_MATCHES_ALL).find(f.readText())
        return (m ?: error("chaîne $nom absente de $fichier")).groupValues[1].replace("\\'", "'").trim()
    }

    @Test fun `l'aide de l'ecran de choix dit Cliquez sur le bouton de l'UG ou vous etes`() {
        assertEquals("Cliquez sur le bouton de l'UG où vous êtes.", chaine("strings_controle.xml", "controle_ug_aide"))
    }

    /** Critère vérifiable : le texte ne promet plus un choix « à faire dans le contrôle » (« ouvrez le contrôle et choisissez »). */
    @Test fun `le texte du guidage ne promet pas un choix que l'ecran ne ferait pas`() {
        val t = chaine("strings_tournees.xml", "guidage_ug_a_choisir")
        assertFalse(t.contains("ouvrez le contrôle et choisissez", ignoreCase = true), t)
        assertFalse(t.contains("choisissez", ignoreCase = true), "l'ordre « choisissez » n'a pas sa place dans le guidage : $t")
        assertTrue("%1\$s" in t && "%2\$s" in t, "le code et les UG restent dits : $t")
    }
}
