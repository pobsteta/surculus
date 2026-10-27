package io.github.pobsteta.surculus.gpkg

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Mission 058 : seuls `tuiles/` et `ortho-terminee.txt` disparaissent ; l'export ne les emporte jamais. */
class AnciennesTuilesTest {
    @TempDir
    lateinit var tmp: Path

    private fun ecrire(dossier: File, rel: String, contenu: String = rel) {
        File(dossier, rel).apply { parentFile.mkdirs() }.writeText(contenu)
    }

    /** Tournée complète : ce qu'il faut garder, plus (si demandé) les anciennes tuiles. */
    private fun tourneeComplete(nom: String = "couchey", avecTuiles: Boolean = true): File {
        val d = tmp.resolve(nom).toFile().apply { mkdirs() }
        listOf(
            "travaux.gpkg", "DCIM/photo1.jpg", "DCIM/sous/photo2.jpg", "ortho.mbtiles", "ortho.gpkg",
            "journal.jsonl", "tuiles-notes.txt", "autre/tuiles/garde.txt",
        ).forEach { ecrire(d, it) }
        if (avecTuiles) {
            ecrire(d, "tuiles/ign-ortho/19/1/2.jpg")
            ecrire(d, "tuiles/ign-ortho/19/1/3.jpg.tmp")
            ecrire(d, "tuiles/ortho-terminee.txt", "2026-10-01")
            ecrire(d, "ortho-terminee.txt", "2026-10-01")
        }
        return d
    }

    private fun contenu(d: File) =
        d.walkTopDown().filter { it.isFile }.map { it.relativeTo(d).invariantSeparatorsPath }.sorted().toList()

    private val gardes = listOf(
        "DCIM/photo1.jpg", "DCIM/sous/photo2.jpg", "autre/tuiles/garde.txt", "journal.jsonl",
        "ortho.gpkg", "ortho.mbtiles", "travaux.gpkg", "tuiles-notes.txt",
    )

    @Test
    fun `seuls tuiles et ortho-terminee disparaissent d'une tournee complete`() {
        val d = tourneeComplete()
        assertTrue(AnciennesTuiles.effacer(d))
        assertEquals(gardes, contenu(d))
        assertFalse(File(d, "tuiles").exists())
        assertFalse(File(d, "ortho-terminee.txt").exists())
        assertTrue(File(d, "DCIM").isDirectory)
    }

    @Test
    fun `le second passage est sans effet`() {
        val d = tourneeComplete()
        AnciennesTuiles.effacer(d)
        assertFalse(AnciennesTuiles.effacer(d))
        assertEquals(gardes, contenu(d))
    }

    @Test
    fun `un dossier sans tuiles est inchange`() {
        val d = tourneeComplete(avecTuiles = false)
        assertFalse(AnciennesTuiles.effacer(d))
        assertEquals(gardes, contenu(d))
    }

    @Test
    fun `effacerPartout nettoie chaque tournee et pas la racine`() {
        val racine = tmp.resolve("tournees").toFile().apply { mkdirs() }
        val a = File(racine, "a").also { tourneeCompleteDans(it, true) }
        val b = File(racine, "b").also { tourneeCompleteDans(it, false) }
        ecrire(racine, "tuiles/hors-tournee.txt")
        assertEquals(1, AnciennesTuiles.effacerPartout(racine))
        assertEquals(gardes, contenu(a))
        assertEquals(gardes, contenu(b))
        assertTrue(File(racine, "tuiles/hors-tournee.txt").isFile)
    }

    private fun tourneeCompleteDans(d: File, avecTuiles: Boolean) {
        d.mkdirs()
        listOf(
            "travaux.gpkg", "DCIM/photo1.jpg", "DCIM/sous/photo2.jpg", "ortho.mbtiles", "ortho.gpkg",
            "journal.jsonl", "tuiles-notes.txt", "autre/tuiles/garde.txt",
        ).forEach { ecrire(d, it) }
        if (avecTuiles) {
            ecrire(d, "tuiles/ign-ortho/19/1/2.jpg")
            ecrire(d, "ortho-terminee.txt", "2026-10-01")
        }
    }

    @Test
    fun `un lien symbolique tuiles n'efface pas sa cible`() {
        val d = tourneeComplete(avecTuiles = false)
        val cible = tmp.resolve("ailleurs").toFile().apply { mkdirs() }
        ecrire(cible, "precieux.txt")
        Files.createSymbolicLink(File(d, "tuiles").toPath(), cible.toPath())
        AnciennesTuiles.effacer(d)
        assertTrue(File(cible, "precieux.txt").isFile)
        assertFalse(Files.exists(File(d, "tuiles").toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS))
    }

    @Test
    fun `l'export zip ne contient ni tuiles ni ortho-terminee`() {
        val d = tourneeComplete()
        val zip = tmp.resolve("sortie/export.zip")
        ExportZip.ecrire(d.toPath(), zip)
        val noms = ZipFile(zip.toFile()).use { z -> z.entries().toList().map { it.name } }
        assertTrue(noms.none { it == "tuiles/" || it.startsWith("tuiles/") || it == "ortho-terminee.txt" }, noms.toString())
        assertEquals("travaux.gpkg", noms.first())
        assertTrue("DCIM/photo1.jpg" in noms && "autre/tuiles/garde.txt" in noms && "tuiles-notes.txt" in noms)
    }
}
