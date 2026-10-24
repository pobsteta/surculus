package io.github.pobsteta.surculus.core.dictee

import fr.nemeton.terrain.voix.InstallationModele
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * `InstallationModele.installer` (terrain-voix d840329) n'est testée nulle part en amont (JitPack construit
 * avec `-x test`) : ces tests fabriquent un petit zip et vérifient ce que la documentation de la classe promet.
 */
class InstallationModeleTest {
    private val racine: File = Files.createTempDirectory("surculus-installation").toFile()

    @AfterTest
    fun nettoyer() {
        racine.deleteRecursively()
    }

    private fun zip(vararg entrees: Pair<String, String>): File {
        val sortie = ByteArrayOutputStream()
        ZipOutputStream(sortie).use { z ->
            for ((nom, contenu) in entrees) {
                z.putNextEntry(ZipEntry(nom))
                z.write(contenu.toByteArray())
                z.closeEntry()
            }
        }
        return File(racine, "modele-${entrees.size}.zip").also { it.writeBytes(sortie.toByteArray()) }
    }

    private fun sha(f: File) = f.inputStream().use { InstallationModele.empreinte(it) }

    private fun installation(sha256: String?) =
        InstallationModele(File(racine, "installe"), File(racine, "cache").apply { mkdirs() }, "", sha256)

    @Test
    fun `le dossier racine de l'archive est retire et le temoin est pose`() {
        val archive = zip("vosk-model-small-fr-0.22/am/final.mdl" to "mdl", "vosk-model-small-fr-0.22/graph/Gr.fst" to "fst")
        val i = installation(sha(archive))
        assertFalse(i.estInstalle)
        assertTrue(i.installer(archive, "essai").isSuccess)
        assertTrue(i.estInstalle)
        assertEquals("mdl", File(racine, "installe/am/final.mdl").readText())
        assertEquals("fst", File(racine, "installe/graph/Gr.fst").readText())
        assertFalse(File(racine, "installe/vosk-model-small-fr-0.22").exists())
        assertEquals("essai", File(racine, "installe/.installe").readText())
    }

    @Test
    fun `une entree qui sort du dossier est refusee et rien n'est ecrit dehors`() {
        val archive = zip("vosk-model-small-fr-0.22/a.txt" to "a", "vosk-model-small-fr-0.22/../../hors.txt" to "pirate")
        val i = installation(sha(archive))
        assertTrue(i.installer(archive).isFailure)
        assertFalse(i.estInstalle)
        assertFalse(File(racine, "installe").exists())
        assertFalse(File(racine.parentFile, "hors.txt").exists())
        assertFalse(File(racine, "hors.txt").exists())
    }

    @Test
    fun `une empreinte fausse est refusee et l'ancien modele n'est pas touche en silence`() {
        val archive = zip("vosk-model-small-fr-0.22/a.txt" to "a")
        val i = installation("0".repeat(64))
        val r = i.installer(archive)
        assertTrue(r.isFailure)
        assertTrue(r.exceptionOrNull()!!.message!!.contains("Empreinte SHA-256"), r.exceptionOrNull()!!.message)
        assertFalse(i.estInstalle)
        assertFalse(File(racine, "installe/a.txt").exists())
    }

    @Test
    fun `ECART terrain-voix - un import refuse par installer efface le modele deja installe`() {
        // Constat (terrain-voix d840329) : `installer` efface le dossier cible sur tout échec, empreinte fausse comprise.
        // Surculus s'en protège : `ModeleVoskImport` contrôle nom, empreinte et contenu non vide AVANT d'appeler
        // `installer`, donc une empreinte fausse n'arrive jamais jusqu'ici. Ce test fixe le constat pour le brief vers
        // `terrain` ; s'il devient rouge parce que `terrain` a corrigé, on l'inverse.
        val bon = zip("vosk-model-small-fr-0.22/a.txt" to "a")
        assertTrue(installation(sha(bon)).installer(bon).isSuccess)
        val autre = zip("vosk-model-small-fr-0.22/b.txt" to "b", "vosk-model-small-fr-0.22/c.txt" to "c")
        assertTrue(installation("0".repeat(64)).installer(autre).isFailure)
        assertFalse(File(racine, "installe/.installe").exists())
    }

    @Test
    fun `l'empreinte en majuscules est acceptee`() {
        val archive = zip("vosk-model-small-fr-0.22/a.txt" to "a")
        assertTrue(installation(sha(archive).uppercase()).installer(archive).isSuccess)
    }

    @Test
    fun `une archive absente donne un echec, pas une exception`() {
        val i = installation("a".repeat(64))
        assertTrue(i.installer(File(racine, "absent.zip")).isFailure)
        assertFalse(i.estInstalle)
    }
}
