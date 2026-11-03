package io.github.pobsteta.surculus.sauvegarde

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Mission 088 : un Surculus refuse une sauvegarde plus récente que lui avec un message qui dit quoi faire, « mettre
 * l'application à jour » (`docs/formats/sursync-v1.md`, « Version » ; spec § 4.8). Garde-fou du texte : le refus
 * `version_future` existe déjà, il ne doit pas perdre sa consigne quand la v2 arrive. Lit la ressource
 * `restauration_version_future` (nom relevé dans `app/src/main/res/values/strings.xml`), pas le code de l'écran.
 */
class MessageVersionFutureTest {
    private val racine: File = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").isFile }

    @Test
    fun `le refus d'une version plus recente dit de mettre l'application a jour et que rien n'a change`() {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(racine, "app/src/main/res/values/strings.xml"))
        val chaines = doc.getElementsByTagName("string")
        val texte = assertNotNull(
            (0 until chaines.length).map { chaines.item(it) }.firstOrNull { it.attributes.getNamedItem("name")?.nodeValue == "restauration_version_future" },
            "ressource restauration_version_future absente",
        ).textContent.replace("\\'", "'")
        assertTrue(Regex("(?i)application à jour").containsMatchIn(texte), texte)
        assertTrue(Regex("(?i)rien n'a été changé").containsMatchIn(texte), "le journal local est inchangé, et on le dit : $texte")
    }
}
