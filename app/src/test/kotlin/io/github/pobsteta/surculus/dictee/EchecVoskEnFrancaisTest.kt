package io.github.pobsteta.surculus.dictee

import io.github.pobsteta.surculus.core.dictee.EchecInstallation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Mission 081, reprise 1/3 : les échecs connus gardent leur phrase française ; la phrase du réseau n'a plus de « 0.22 ». */
class EchecVoskEnFrancaisTest {
    private fun cause(t: Throwable): String {
        val s = SuiviInstallation(CoroutineScope(SupervisorJob() + Dispatchers.Unconfined))
        s.demarrer(PhaseInstallation.RESEAU, { false }) { _, _, _ -> throw t }
        return (s.etat.value.issue as IssueInstallation.Echec).cause
    }

    @Test fun `un echec connu garde sa phrase francaise`() {
        assertEquals("téléchargement interrompu", cause(EchecInstallation("téléchargement interrompu")))
    }

    @Test fun `pas de reseau ou delai depasse, une phrase dediee`() {
        listOf(
            java.net.UnknownHostException("alphacephei.com"),
            java.net.SocketTimeoutException("timeout"),
            java.net.ConnectException("Connection refused"),
            java.io.IOException("enveloppe", java.net.UnknownHostException("alphacephei.com")),
        ).forEach { assertEquals(TEXTE_PAS_DE_RESEAU, cause(it), it.toString()) }
        assertEquals("pas de réseau : réessayer au bureau, ou importer le fichier du modèle", TEXTE_PAS_DE_RESEAU)
    }

    @Test fun `disque plein, une phrase dediee`() {
        listOf(
            java.io.IOException("write failed: ENOSPC (No space left on device)"),
            java.io.IOException("No space left on device"),
            RuntimeException("enveloppe", java.io.IOException("No space left on device")),
        ).forEach { assertEquals(TEXTE_DISQUE_PLEIN, cause(it), it.toString()) }
        assertEquals("plus de place sur le téléphone : libérer de la place puis réessayer", TEXTE_DISQUE_PLEIN)
    }

    @Test fun `la reponse 404 dit quoi faire`() {
        val m = ModeleVoskTelechargement.refusServeur(404).message!!
        assertEquals("le serveur ne donne pas le modèle (réponse 404) : réessayer plus tard, ou importer le fichier", m)
    }

    @Test fun `un echec inconnu tombe sur la phrase generique`() {
        assertEquals(TEXTE_ECHEC, cause(IllegalStateException("Téléchargement interrompu")))
    }

    @Test fun `la phrase du reseau coupe n'a ni nom de modele ni version`() {
        val xml = File("src/main/res/values/strings_dictee.xml").readText()
        val ligne = xml.lines().first { "dictee_refus_reseau" in it }
        assertFalse("0.22" in ligne || "vosk-model" in ligne, ligne)
        assertTrue("portail Wi-Fi" in ligne, ligne)
    }
}
