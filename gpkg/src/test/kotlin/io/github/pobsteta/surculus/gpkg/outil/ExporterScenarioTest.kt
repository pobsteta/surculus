package io.github.pobsteta.surculus.gpkg.outil

import io.github.pobsteta.surculus.core.placette.EmpriseEtFrance
import io.github.pobsteta.surculus.gpkg.Contenu
import io.github.pobsteta.surculus.gpkg.Couches
import io.github.pobsteta.surculus.gpkg.LecteurTournee
import io.github.pobsteta.surculus.gpkg.TourneeDeTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipFile
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * L'outil de la CI (mission 007) rejoue `fixtures/export/scenario-couchey.json` sur une tournée de départ : son
 * export contient des saisies (sinon le critère (a) de l'ADR 0007 ne mesurerait rien) et se relit.
 */
class ExporterScenarioTest {
    @TempDir
    lateinit var dossier: Path

    private val racine: Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }.first { Files.exists(it.resolve("settings.gradle.kts")) }
    private val scenario = racine.resolve("fixtures/export/scenario-couchey.json")

    private fun tourneeDeDepart(): Path {
        val d = Files.createDirectories(dossier.resolve("tournee-sans"))
        TourneeDeTest.couchey(d, nom = "travaux.gpkg")
        return d
    }

    @Test
    fun `le scenario de Couchey produit un export avec deux controles, leurs plants et leurs etats`() {
        val sortie = dossier.resolve("sortie")
        ExporterScenario.jouer(tourneeDeDepart(), scenario, sortie)
        val gpkg = sortie.resolve("export-surculus/travaux.gpkg")
        val t = LecteurTournee.lire(gpkg)
        assertEquals(2, t.controles.size, "au moins une saisie, sinon la CI ne mesure rien")
        assertEquals(Couches.NOMS, t.couchesAjoutees)
        assertEquals(5 + 3, t.plants.size)
        assertEquals(5 + 3, t.etats.size)
        assertEquals("Agent d'essai (scénario CI)", t.projet?.operateur)
        // Les contrôles désignent les placettes par leur UUID dans la tournée (retrouvées par leur code).
        val parCode = t.placettes.associateBy { it.code }
        assertEquals(setOf(parCode["P35-01"]!!.uuid, parCode["P35-02"]!!.uuid), t.controles.map { it.placetteUuid }.toSet())
        // Totaux de P35-01 : sain, atteint abrouti, mort, absent, sain -> 5 comptés, 3 vivants, 1 abrouti, hauteurs 35, 40, 42.
        Contenu.colonnes(gpkg, "controles", "placette_uuid", "nb_total", "nb_vivants", "nb_abroutis", "h_moy_cm")
            .first { it[0] == parCode["P35-01"]!!.uuid }.let { assertEquals(listOf(5, 3, 1, 39), it.drop(1)) }
        // Chaque plant est dérivé du piquet, en France, dans le rayon (+ tolérance) de sa placette.
        for (p in t.plants) {
            val pos = p.position!!
            assertTrue(EmpriseEtFrance.enFrance(pos))
            val piquet = parCode.values.first { it.uuid == p.placetteUuid }.position!!
            assertTrue(Math.hypot(pos.xMetres - piquet.xMetres, pos.yMetres - piquet.yMetres) <= 3.99 + 0.1)
        }
    }

    @Test
    fun `le bilan d'export en JSON redit ce que l'export contient`() {
        val sortie = dossier.resolve("sortie")
        ExporterScenario.jouer(tourneeDeDepart(), scenario, sortie)
        val j = com.google.gson.JsonParser.parseString(Files.readString(sortie.resolve(ExporterScenario.BILAN_JSON))).asJsonObject
        val t = LecteurTournee.lire(sortie.resolve("export-surculus/travaux.gpkg"))
        assertEquals(2, j.get("controles").asInt)
        assertEquals(0, j.get("placettes").asInt)
        assertEquals(0, j.get("travaux").asInt)
        assertEquals(t.plants.size, j.get("plants").asInt)
        assertEquals(t.etats.size, j.get("etats_plants").asInt)
        assertEquals(0, j.get("deja_presentes").asInt)
        // Les UUID écrits sont ceux des contrôles de l'export, en minuscules et triés.
        val uuids = j.getAsJsonObject("uuid_ecrits").getAsJsonArray("controles").map { it.asString }
        assertEquals(t.controles.map { it.uuid.lowercase() }.sorted(), uuids)
        assertEquals(0, j.getAsJsonObject("uuid_ecrits").getAsJsonArray("placettes").size())
    }

    @Test
    fun `le zip est a plat et contient le meme travaux gpkg que le dossier`() {
        val sortie = dossier.resolve("sortie")
        ExporterScenario.jouer(tourneeDeDepart(), scenario, sortie)
        ZipFile(sortie.resolve("export-surculus.zip").toFile()).use { z ->
            assertEquals(listOf("travaux.gpkg"), z.entries().toList().map { it.name })
            val dansLeZip = dossier.resolve("depuis-le-zip.gpkg")
            z.getInputStream(z.getEntry("travaux.gpkg")).use { Files.copy(it, dansLeZip) }
            assertEquals(Contenu.de(sortie.resolve("export-surculus/travaux.gpkg")), Contenu.de(dansLeZip))
        }
    }

    @Test
    fun `deux rejeux du scenario sur la meme tournee donnent le meme contenu - les UUID derivent du scenario`() {
        val d = tourneeDeDepart()
        ExporterScenario.jouer(d, scenario, dossier.resolve("a"))
        ExporterScenario.jouer(d, scenario, dossier.resolve("b"))
        assertEquals(Contenu.de(dossier.resolve("a/export-surculus/travaux.gpkg")), Contenu.de(dossier.resolve("b/export-surculus/travaux.gpkg")))
    }

    @Test
    fun `une placette qui n est pas dans la tournee fait refuser le scenario`() {
        val autre = dossier.resolve("autre.json")
        Files.writeString(autre, Files.readString(scenario).replace("P35-02", "P99-99"))
        val e = assertFailsWith<ExporterScenario.ScenarioInvalide> { ExporterScenario.jouer(tourneeDeDepart(), autre, dossier.resolve("s")) }
        assertTrue("P99-99" in e.message.orEmpty())
    }

    @Test
    fun `un code d essence hors de essences_csv fait refuser le scenario`() {
        val autre = dossier.resolve("autre.json")
        Files.writeString(autre, Files.readString(scenario).replaceFirst("\"CHS\"", "\"ZZZ\""))
        val e = assertFailsWith<ExporterScenario.ScenarioInvalide> { ExporterScenario.jouer(tourneeDeDepart(), autre, dossier.resolve("s")) }
        assertTrue("ZZZ" in e.message.orEmpty())
    }
}
