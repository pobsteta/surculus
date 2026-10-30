package io.github.pobsteta.surculus.core.dictee

import fr.nemeton.terrain.voix.InstallationModele
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs

/**
 * Mission 081, reprise 1/3 : un échec d'installation connu garde sa phrase française (portée par `EchecInstallation`,
 * montrable telle quelle), sans nom de modèle ni version ; le reste tombe sur la phrase générique.
 */
class EchecInstallationFrancaisTest {
    private val racine: File = Files.createTempDirectory("surculus-echec").toFile()

    @AfterTest fun nettoyer() { racine.deleteRecursively() }

    private fun zip(marque: String): ByteArray {
        val sortie = ByteArrayOutputStream()
        ZipOutputStream(sortie).use { z ->
            z.putNextEntry(ZipEntry("vosk-model-small-fr-0.22/am/final.mdl"))
            z.write(marque.toByteArray())
            z.closeEntry()
        }
        return sortie.toByteArray()
    }

    private fun installateur(attendu: ByteArray) =
        InstallateurModele(File(racine, "vosk-model-fr"), File(racine, "cache"), InstallationModele.empreinte(ByteArrayInputStream(attendu)))

    private fun source(b: ByteArray, annonce: Long = b.size.toLong()) = SourceArchive { FluxArchive(ByteArrayInputStream(b), annonce) }

    private fun propre(t: Throwable?) {
        val e = assertIs<EchecInstallation>(t, "l'échec doit porter un message français montrable : $t")
        val m = e.message!!
        assertFalse("0.22" in m || "vosk" in m.lowercase() || '_' in m || "SHA" in m, "« $m » reste technique")
        assertFalse(Regex("\\d\\.\\d").containsMatchIn(m), "« $m » contient un décimal à point")
    }

    @Test fun `une mauvaise empreinte (message par defaut) est dite sans nom de modele`() {
        val v1 = zip("v1")
        propre(installateur(v1).installer(source(zip("autre")), "b").exceptionOrNull())
    }

    @Test fun `un telechargement interrompu est dit en francais`() {
        val v1 = zip("v1")
        propre(installateur(v1).installer(source(v1), "b", continuer = { false }).exceptionOrNull())
    }

    @Test fun `un telechargement incomplet est dit en francais`() {
        val v1 = zip("v1")
        propre(installateur(v1).installer(source(v1, annonce = v1.size + 100L), "b").exceptionOrNull())
    }
}
