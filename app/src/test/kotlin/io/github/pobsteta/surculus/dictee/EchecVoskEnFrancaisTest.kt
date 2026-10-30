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
        assertEquals("Téléchargement interrompu", cause(EchecInstallation("Téléchargement interrompu")))
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
