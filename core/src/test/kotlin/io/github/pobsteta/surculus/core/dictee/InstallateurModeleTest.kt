package io.github.pobsteta.surculus.core.dictee

import fr.nemeton.terrain.voix.InstallationModele
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Mission 048 : le modèle précédent n'est jamais perdu par un échec ; sans réseau (source injectée). */
class InstallateurModeleTest {
    private val racine: File = Files.createTempDirectory("surculus-installateur").toFile()

    @AfterTest
    fun nettoyer() {
        racine.deleteRecursively()
    }

    private fun zip(marque: String): ByteArray {
        val sortie = ByteArrayOutputStream()
        ZipOutputStream(sortie).use { z ->
            z.putNextEntry(ZipEntry("vosk-model-small-fr-0.22/am/final.mdl"))
            z.write(marque.toByteArray())
            z.closeEntry()
        }
        return sortie.toByteArray()
    }

    private fun sha(b: ByteArray) = InstallationModele.empreinte(ByteArrayInputStream(b))

    private val dossier get() = File(racine, "vosk-model-fr")

    private fun installateur(attendu: ByteArray) = InstallateurModele(dossier, File(racine, "cache"), sha(attendu))

    private fun source(octets: ByteArray, annonce: Long = octets.size.toLong()) =
        SourceArchive { FluxArchive(ByteArrayInputStream(octets), annonce) }

    private fun marque() = File(dossier, "am/final.mdl").readText()

    @Test
    fun `un telechargement complet installe le modele et la progression atteint 1`() {
        val a = zip("v1")
        val i = installateur(a)
        val vues = mutableListOf<Float>()
        assertFalse(i.estInstalle)
        assertTrue(i.installer(source(a), "reseau", { vues += it }).isSuccess)
        assertTrue(i.estInstalle)
        assertEquals("v1", marque())
        assertEquals(1f, vues.last())
        assertTrue(vues.zipWithNext().all { (x, y) -> x <= y }, "progression non décroissante : $vues")
        assertFalse(File(racine, "vosk-model-fr.nouveau").exists())
        assertFalse(File(racine, "vosk-model-fr.ancien").exists())
    }

    @Test
    fun `une version plus recente remplace la precedente`() {
        val v1 = zip("v1")
        val v2 = zip("v2")
        installateur(v1).installer(source(v1), "a").getOrThrow()
        assertTrue(installateur(v2).installer(source(v2), "b").isSuccess)
        assertEquals("v2", marque())
    }

    @Test
    fun `un telechargement interrompu laisse le modele precedent intact`() {
        val v1 = zip("v1")
        installateur(v1).installer(source(v1), "a").getOrThrow()
        val coupe = SourceArchive {
            FluxArchive(
                object : InputStream() {
                    var lus = 0
                    override fun read(): Int = throw UnsupportedOperationException()
                    override fun read(b: ByteArray, off: Int, len: Int): Int {
                        if (lus++ > 0) throw IOException("réseau coupé")
                        b[off] = 1
                        return 1
                    }
                },
                1000,
            )
        }
        val r = installateur(v1).installer(coupe, "b")
        assertTrue(r.isFailure)
        assertTrue(installateur(v1).estInstalle)
        assertEquals("v1", marque())
        assertFalse(File(racine, "vosk-model-fr.nouveau").exists())
    }

    @Test
    fun `une archive tronquee a la taille annoncee est refusee et le precedent reste`() {
        val v1 = zip("v1")
        val v2 = zip("v2")
        installateur(v1).installer(source(v1), "a").getOrThrow()
        val r = installateur(v2).installer(source(v2.copyOf(v2.size - 5), v2.size.toLong()), "b")
        assertTrue(r.isFailure)
        assertEquals("v1", marque())
    }

    @Test
    fun `une annulation en cours laisse le modele precedent intact`() {
        val v1 = zip("v1")
        installateur(v1).installer(source(v1), "a").getOrThrow()
        val v2 = zip("v2")
        assertTrue(installateur(v2).installer(source(v2), "b", continuer = { false }).isFailure)
        assertEquals("v1", marque())
    }

    @Test
    fun `une mauvaise empreinte est refusee et le precedent reste`() {
        val v1 = zip("v1")
        installateur(v1).installer(source(v1), "a").getOrThrow()
        val autre = zip("autre")
        val r = installateur(v1).installer(source(autre), "b")
        assertTrue(r.isFailure)
        assertTrue(r.exceptionOrNull()!!.message!!.contains("empreinte"))
        assertTrue(installateur(v1).estInstalle)
        assertEquals("v1", marque())
    }

    @Test
    fun `une mauvaise empreinte sans modele precedent ne laisse rien d'installe`() {
        val v1 = zip("v1")
        val i = installateur(v1)
        assertTrue(i.installer(source(zip("autre")), "b").isFailure)
        assertFalse(i.estInstalle)
        assertFalse(dossier.exists())
    }

    @Test
    fun `la suppression rend le modele non installe`() {
        val v1 = zip("v1")
        val i = installateur(v1)
        i.installer(source(v1), "a").getOrThrow()
        assertTrue(i.estInstalle)
        i.supprimer()
        assertFalse(i.estInstalle)
        assertFalse(dossier.exists())
    }
}
