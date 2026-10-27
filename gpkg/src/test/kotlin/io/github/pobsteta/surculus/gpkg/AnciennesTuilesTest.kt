package io.github.pobsteta.surculus.gpkg

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Mission 058 : seul `tuiles/` disparaît ; aucun lien symbolique n'est suivi ; l'export ne l'emporte jamais. */
class AnciennesTuilesTest {
    @TempDir
    lateinit var tmp: Path

    private fun ecrire(dossier: File, rel: String, contenu: String = rel) {
        File(dossier, rel).apply { parentFile.mkdirs() }.writeText(contenu)
    }

    /** Tournée complète : ce qu'il faut garder, plus (si demandé) les anciennes tuiles de la 055. */
    private fun tourneeComplete(d: File, avecTuiles: Boolean = true): File {
        d.mkdirs()
        listOf(
            "travaux.gpkg", "DCIM/photo1.jpg", "DCIM/sous/photo2.jpg", "ortho.mbtiles", "ortho.gpkg",
            "journal.jsonl", "tuiles-notes.txt", "autre/tuiles/garde.txt", "ortho-terminee.txt",
        ).forEach { ecrire(d, it) }
        if (avecTuiles) {
            ecrire(d, "tuiles/ign-ortho/19/1/2.jpg")
            ecrire(d, "tuiles/ign-ortho/19/1/3.jpg.tmp")
            ecrire(d, "tuiles/ortho-terminee.txt", "2026-10-01") // où la 055 écrivait la marque
        }
        return d
    }

    private fun tourneeComplete(nom: String = "couchey", avecTuiles: Boolean = true) =
        tourneeComplete(tmp.resolve(nom).toFile(), avecTuiles)

    private fun contenu(d: File) =
        d.walkTopDown().filter { it.isFile }.map { it.relativeTo(d).invariantSeparatorsPath }.sorted().toList()

    /** `ortho-terminee.txt` à la racine n'a jamais été écrit par la 055 : il reste. */
    private val gardes = listOf(
        "DCIM/photo1.jpg", "DCIM/sous/photo2.jpg", "autre/tuiles/garde.txt", "journal.jsonl",
        "ortho-terminee.txt", "ortho.gpkg", "ortho.mbtiles", "travaux.gpkg", "tuiles-notes.txt",
    )

    private fun existeSansSuivre(f: File) = Files.exists(f.toPath(), LinkOption.NOFOLLOW_LINKS)

    private fun dossierExterieur(): File {
        val cible = tmp.resolve("ailleurs").toFile().apply { mkdirs() }
        ecrire(cible, "precieux.txt")
        ecrire(cible, "sous/autre.txt")
        return cible
    }

    @Test
    fun `seul tuiles disparait d'une tournee complete`() {
        val d = tourneeComplete()
        assertTrue(AnciennesTuiles.effacer(d))
        assertEquals(gardes, contenu(d))
        assertFalse(File(d, "tuiles").exists())
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
        val a = tourneeComplete(File(racine, "a"), true)
        val b = tourneeComplete(File(racine, "b"), false)
        ecrire(racine, "tuiles/hors-tournee.txt")
        assertEquals(1, AnciennesTuiles.effacerPartout(racine))
        assertEquals(gardes, contenu(a))
        assertEquals(gardes, contenu(b))
        assertTrue(File(racine, "tuiles/hors-tournee.txt").isFile)
        assertFalse(AnciennesTuiles.resteDesTuiles(racine))
    }

    @Test
    fun `tuiles en lien symbolique n'efface pas sa cible`() {
        val d = tourneeComplete(avecTuiles = false)
        val cible = dossierExterieur()
        Files.createSymbolicLink(File(d, "tuiles").toPath(), cible.toPath())
        AnciennesTuiles.effacer(d)
        assertTrue(File(cible, "precieux.txt").isFile)
        assertFalse(existeSansSuivre(File(d, "tuiles")))
    }

    @Test
    fun `un lien vers un dossier exterieur a l'interieur de tuiles n'efface pas sa cible`() {
        val d = tourneeComplete()
        val cible = dossierExterieur()
        Files.createSymbolicLink(File(d, "tuiles/ign-ortho/lien").toPath(), cible.toPath())
        assertTrue(AnciennesTuiles.effacer(d))
        assertTrue(File(cible, "precieux.txt").isFile)
        assertTrue(File(cible, "sous/autre.txt").isFile)
        assertFalse(existeSansSuivre(File(d, "tuiles")))
        assertEquals(gardes, contenu(d))
    }

    @Test
    fun `un dossier de tournee en lien n'est pas suivi`() {
        val racine = tmp.resolve("tournees").toFile().apply { mkdirs() }
        val reelle = tourneeComplete(tmp.resolve("reelle").toFile())
        Files.createSymbolicLink(File(racine, "lien").toPath(), reelle.toPath())
        assertEquals(0, AnciennesTuiles.effacerPartout(racine))
        assertTrue(File(reelle, "tuiles/ign-ortho/19/1/2.jpg").isFile)
        assertFalse(AnciennesTuiles.effacer(File(racine, "lien")))
        assertTrue(File(reelle, "tuiles/ign-ortho/19/1/2.jpg").isFile)
    }

    @Test
    fun `resteDesTuiles voit un tuiles encore present`() {
        val racine = tmp.resolve("tournees").toFile().apply { mkdirs() }
        tourneeComplete(File(racine, "a"), true)
        assertTrue(AnciennesTuiles.resteDesTuiles(racine))
        AnciennesTuiles.effacerPartout(racine)
        assertFalse(AnciennesTuiles.resteDesTuiles(racine))
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
