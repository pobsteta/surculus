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

    private fun copierDossier(de: File, vers: File) {
        de.copyRecursively(vers, overwrite = true)
    }

    @Test
    fun `un arret entre les deux renommages est repare et le modele est retrouve`() {
        val v1 = zip("v1")
        installateur(v1).installer(source(v1), "a").getOrThrow()
        // Crash simulé : l'ancien déplacé, le nouveau (complet) jamais mis en place.
        val ancien = File(racine, "vosk-model-fr.ancien")
        val nouveau = File(racine, "vosk-model-fr.nouveau")
        copierDossier(dossier, nouveau)
        assertTrue(dossier.renameTo(ancien))
        assertFalse(dossier.exists())
        assertTrue(installateur(v1).estInstalle)
        assertEquals("v1", marque())
        assertFalse(ancien.exists())
        assertFalse(nouveau.exists())
    }

    @Test
    fun `sans ancien, un nouveau complet est remis en place`() {
        val v1 = zip("v1")
        installateur(v1).installer(source(v1), "a").getOrThrow()
        assertTrue(dossier.renameTo(File(racine, "vosk-model-fr.nouveau")))
        assertTrue(installateur(v1).estInstalle)
        assertEquals("v1", marque())
    }

    @Test
    fun `un nouveau incomplet n'est pas pris pour un modele`() {
        val v1 = zip("v1")
        File(racine, "vosk-model-fr.nouveau/am").mkdirs()
        assertFalse(installateur(v1).estInstalle)
        assertFalse(dossier.exists())
        assertFalse(File(racine, "vosk-model-fr.nouveau").exists())
    }

    @Test
    fun `les orphelins sont nettoyes quand le modele installe est complet`() {
        val v1 = zip("v1")
        installateur(v1).installer(source(v1), "a").getOrThrow()
        File(racine, "vosk-model-fr.ancien/x").mkdirs()
        File(racine, "vosk-model-fr.nouveau/y").mkdirs()
        assertTrue(installateur(v1).estInstalle)
        assertEquals("v1", marque())
        assertFalse(File(racine, "vosk-model-fr.ancien").exists())
        assertFalse(File(racine, "vosk-model-fr.nouveau").exists())
    }

    @Test
    fun `une installation repare d'abord l'etat laisse par un arret`() {
        val v1 = zip("v1")
        val v2 = zip("v2")
        installateur(v1).installer(source(v1), "a").getOrThrow()
        assertTrue(dossier.renameTo(File(racine, "vosk-model-fr.ancien")))
        // Le nouveau téléchargement échoue : le v1 déplacé par le crash doit être revenu.
        assertTrue(installateur(v2).installer(source(zip("autre")), "b").isFailure)
        assertEquals("v1", marque())
    }

    @Test
    fun `un echec de decompression laisse le modele precedent intact`() {
        val v1 = zip("v1")
        installateur(v1).installer(source(v1), "a").getOrThrow()
        // Archive à la bonne empreinte mais piégée (entrée hors du dossier) : refusée à la décompression.
        val sortie = ByteArrayOutputStream()
        ZipOutputStream(sortie).use { z ->
            z.putNextEntry(ZipEntry("vosk-model-small-fr-0.22/a.txt"))
            z.write(1)
            z.closeEntry()
            z.putNextEntry(ZipEntry("vosk-model-small-fr-0.22/../../hors.txt"))
            z.write(2)
            z.closeEntry()
        }
        val piege = sortie.toByteArray()
        val r = installateur(piege).installer(source(piege), "b")
        assertTrue(r.isFailure)
        assertEquals("v1", marque())
        assertTrue(installateur(v1).estInstalle)
        assertFalse(File(racine, "vosk-model-fr.nouveau").exists())
    }

    @Test
    fun `une annulation en cours de copie laisse le modele precedent intact`() {
        val v1 = zip("v1")
        installateur(v1).installer(source(v1), "a").getOrThrow()
        val gros = ByteArray(400_000) { it.toByte() }
        var consultations = 0
        val r = installateur(v1).installer(source(gros), "b", continuer = { ++consultations <= 3 })
        assertTrue(consultations > 3, "la copie a bien commencé avant l'annulation : $consultations")
        assertTrue(r.isFailure)
        assertEquals("v1", marque())
        assertFalse(File(racine, "cache/modele-vosk-recu.part").exists())
    }

    @Test
    fun `apres la verification l'annulation n'arrete plus l'installation`() {
        val v1 = zip("v1")
        val v2 = zip("v2")
        installateur(v1).installer(source(v1), "a").getOrThrow()
        var verifie = false
        val r = installateur(v2).installer(source(v2), "b", continuer = { !verifie }, apresVerification = { verifie = true })
        assertTrue(r.isSuccess)
        assertTrue(verifie)
        assertEquals("v2", marque())
    }

    @Test
    fun `le message de refus est celui de l'appelant`() {
        val v1 = zip("v1")
        val r = installateur(v1).installer(source(zip("autre")), "b", messageRefus = "portail Wi-Fi")
        assertEquals("portail Wi-Fi", r.exceptionOrNull()!!.message)
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
