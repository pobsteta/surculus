package io.github.pobsteta.surculus.carte

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Mission 012-B1 (décision de Pascal sur la #107) : en sombre, la légende de la carte retire le symbole du libellé avec
 * `libelle.substringAfter(' ')` (`EcranCarte.kt`, `LigneLegende`). Ce test garde les quatre libellés : le symbole est en tête,
 * suivi d'une espace, puis le nombre. Un libellé qui perdrait son symbole ou son espace ferait perdre un mot à la légende.
 */
class LegendeLibellesTest {
    private val fichier = File("src/main/res/values/strings_tournees.xml").takeIf { it.isFile } ?: File("app/src/main/res/values/strings_tournees.xml")
    private val xml = fichier.readText()

    private fun libelle(nom: String): String {
        val m = Regex("""<string name="$nom">([^<]*)</string>""").find(xml) ?: error("libellé $nom absent")
        // Même lecture qu'Android : \? est un « ? », %1$d prend le nombre.
        return m.groupValues[1].replace("\\?", "?").replace("%1\$d", "3")
    }

    private val attendu = mapOf(
        "carte_legende_jamais" to ("●" to "3 jamais"),
        "carte_legende_a_programmer" to ("●" to "3 travaux à prévoir"),
        "carte_legende_a_jour" to ("○" to "3 à jour"),
        "carte_legende_inconnu" to ("?" to "3 sans état"),
    )

    @Test fun `les quatre libelles de la legende portent leur symbole en tete`() {
        assertEquals(4, attendu.size)
        attendu.forEach { (nom, p) ->
            val l = libelle(nom)
            assertTrue(l.startsWith(p.first + " "), "$nom : « $l » ne commence pas par « ${p.first} » et une espace")
        }
    }

    @Test fun `substringAfter(' ') retire le symbole et garde tout le texte`() {
        attendu.forEach { (nom, p) -> assertEquals(p.second, libelle(nom).substringAfter(' '), nom) }
    }

    @Test fun `le symbole est un seul caractere, sans espace dedans`() {
        attendu.keys.forEach { nom -> assertEquals(1, libelle(nom).substringBefore(' ').length, nom) }
    }
}
