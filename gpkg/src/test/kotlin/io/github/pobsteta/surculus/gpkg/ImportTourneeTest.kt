package io.github.pobsteta.surculus.gpkg

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Ouverture d'une tournée en dossier ou en `.zip` (spec § 4.1) : copie privée, jamais d'écrasement, zip slip refusé. */
class ImportTourneeTest {
    @TempDir
    lateinit var dossier: Path

    private val maintenant = Instant.parse("2026-10-21T08:30:00Z")

    private fun zipDe(vararg entrees: Pair<String, ByteArray>): ByteArray {
        val sortie = ByteArrayOutputStream()
        ZipOutputStream(sortie).use { z ->
            for ((nom, octets) in entrees) {
                z.putNextEntry(ZipEntry(nom))
                z.write(octets)
                z.closeEntry()
            }
        }
        return sortie.toByteArray()
    }

    private fun gpkg() = Files.readAllBytes(TourneeDeTest.couchey(dossier.resolve("src").also { Files.createDirectories(it) }))

    @Test
    fun `un zip avec dossier racine est range sous un nom neuf`() {
        val octets = zipDe("tournee/travaux.gpkg" to gpkg(), "tournee/DCIM/lisez-moi.txt" to "x".toByteArray())
        val tmp = dossier.resolve("tmp")
        ImportTournee.extraireZip(ByteArrayInputStream(octets), tmp)
        val racine = dossier.resolve("tournees")
        val rangee = ImportTournee.finaliser(tmp, racine, maintenant)
        assertTrue(Files.isRegularFile(rangee.resolve("travaux.gpkg")))
        assertTrue(Files.isRegularFile(rangee.resolve("DCIM/lisez-moi.txt")))
        assertEquals(racine, rangee.parent)
        assertTrue(rangee.fileName.toString().endsWith("-20261021-103000"), rangee.fileName.toString())
        assertFalse(Files.exists(tmp), "le dossier temporaire est supprimé")
    }

    @Test
    fun `le meme zip ouvert deux fois donne deux tournees, la premiere n'est pas ecrasee`() {
        val octets = zipDe("travaux.gpkg" to gpkg())
        val racine = dossier.resolve("tournees")
        val a = dossier.resolve("a").also { ImportTournee.extraireZip(ByteArrayInputStream(octets), it) }
        val b = dossier.resolve("b").also { ImportTournee.extraireZip(ByteArrayInputStream(octets), it) }
        val premiere = ImportTournee.finaliser(a, racine, maintenant)
        Files.writeString(premiere.resolve("marque.txt"), "premiere")
        val seconde = ImportTournee.finaliser(b, racine, maintenant)
        assertTrue(premiere != seconde)
        assertEquals("premiere", Files.readString(premiere.resolve("marque.txt")))
        assertFalse(Files.exists(seconde.resolve("marque.txt")))
    }

    @Test
    fun `un zip qui sort de son dossier est refuse`() {
        val octets = zipDe("../evil.txt" to "x".toByteArray())
        assertFailsWith<IOException> { ImportTournee.extraireZip(ByteArrayInputStream(octets), dossier.resolve("tmp")) }
        assertFalse(Files.exists(dossier.resolve("evil.txt")))
    }

    @Test
    fun `un dossier sans travaux gpkg est refuse et nettoye`() {
        val tmp = dossier.resolve("tmp").also { Files.createDirectories(it) }
        Files.writeString(tmp.resolve("lisez-moi.txt"), "rien")
        val refus = assertFailsWith<GpkgRefuse> { ImportTournee.finaliser(tmp, dossier.resolve("tournees"), maintenant) }
        assertEquals("pas_un_geopackage", refus.refus.motif)
        assertFalse(Files.exists(tmp))
        assertFalse(Files.exists(dossier.resolve("tournees")), "rien n'est rangé")
    }

    @Test
    fun `un travaux gpkg qui n'est pas un GeoPackage est refuse`() {
        val tmp = dossier.resolve("tmp").also { Files.createDirectories(it) }
        Files.writeString(tmp.resolve("travaux.gpkg"), "pas du sqlite")
        assertFailsWith<GpkgRefuse> { ImportTournee.finaliser(tmp, dossier.resolve("tournees"), maintenant) }
        assertFalse(Files.exists(dossier.resolve("tournees")) && Files.list(dossier.resolve("tournees")).use { it.findAny().isPresent })
    }

    @Test
    fun `trouver la tournee a la racine, dans un sous-dossier, ou nulle part`() {
        val a = dossier.resolve("a").also { Files.createDirectories(it) }
        Files.writeString(a.resolve("travaux.gpkg"), "x")
        assertEquals(a, ImportTournee.trouverTournee(a))
        val b = dossier.resolve("b/sous").also { Files.createDirectories(it) }
        Files.writeString(b.resolve("travaux.gpkg"), "x")
        assertEquals(b, ImportTournee.trouverTournee(dossier.resolve("b")))
        val c = dossier.resolve("c").also { Files.createDirectories(it) }
        assertNull(ImportTournee.trouverTournee(c))
        assertNotNull(a)
    }

    @Test
    fun `le nom de la tournee est lisible et sans caracteres douteux`() {
        val nom = ImportTournee.nomUnique(dossier, "Forêt de Couchey (démo)/../x", maintenant)
        assertEquals("Foret-de-Couchey-demo-x-20261021-103000", nom)
    }

    @Test
    fun `une tournee hors de France est refusee a l'import, rien n'est range (placette en Somalie)`() {
        val f = TourneeDeTest.modeleVide(dossier.resolve("src").also { Files.createDirectories(it) })
        fun carre(x: Double, y: Double) = listOf(x to y, x + 400 to y, x + 400 to y + 400, x to y + 400, x to y).map { io.github.pobsteta.surculus.core.geo.PointLambert93(it.first, it.second) }
        val o = TourneeDeTest.centre(0)
        val inverse = Geometrie.MultiPolygone(listOf(Geometrie.Polygone(listOf(carre(o.yMetres, o.xMetres))))) // x et y permutés
        Sqlite.ouvrir(f).use { cnx ->
            cnx.prepareStatement("INSERT INTO ug (geom, numero) VALUES (?, '35')").use { it.setBytes(1, GeometrieGpkg.encoder(inverse, 2154)); it.executeUpdate() }
        }
        val tmp = dossier.resolve("tmp").also { Files.createDirectories(it) }
        Files.copy(f, tmp.resolve("travaux.gpkg"))
        val refus = assertFailsWith<GpkgRefuse> { ImportTournee.finaliser(tmp, dossier.resolve("tournees"), maintenant) }.refus
        assertEquals("hors_france", refus.motif)
        assertFalse(Files.exists(tmp), "le temporaire est nettoyé")
        assertFalse(Files.exists(dossier.resolve("tournees")), "rien n'est rangé")
    }

    @Test
    fun `une ortho posee a la racine du zip rejoint la tournee`() {
        val octets = zipDe("tournee/travaux.gpkg" to gpkg(), "ortho.mbtiles" to "x".toByteArray())
        val tmp = dossier.resolve("tmp")
        ImportTournee.extraireZip(ByteArrayInputStream(octets), tmp)
        val rangee = ImportTournee.finaliser(tmp, dossier.resolve("tournees"), maintenant)
        assertTrue(Files.isRegularFile(rangee.resolve("ortho.mbtiles")))
    }
}
