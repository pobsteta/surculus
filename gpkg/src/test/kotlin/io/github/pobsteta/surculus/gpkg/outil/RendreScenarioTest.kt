package io.github.pobsteta.surculus.gpkg.outil

import com.google.gson.JsonParser
import io.github.pobsteta.surculus.gpkg.Contenu
import io.github.pobsteta.surculus.gpkg.LecteurTournee
import io.github.pobsteta.surculus.gpkg.ResultatRendu
import io.github.pobsteta.surculus.gpkg.TourneeDeTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Mission 090 : l'outil de la CI joue le scénario de Couchey par la session de contrôle et rend la tournée comme
 * l'application. Sa sortie est celle qu'attend `ci/sommier/verifier.R` (`--avec` et `--bilan-export`), et son bilan
 * compte les mêmes lignes que celui de l'export direct du scénario ([ExporterScenario]).
 */
class RendreScenarioTest {
    @TempDir
    lateinit var dossier: Path

    private val racine: Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }.first { Files.exists(it.resolve("settings.gradle.kts")) }
    private val scenario = racine.resolve("fixtures/export/scenario-couchey.json")

    private fun depart(nom: String): Path = Files.createDirectories(dossier.resolve(nom)).also { TourneeDeTest.couchey(it, nom = "travaux.gpkg") }

    @Test
    fun `le rendu du scenario depose le zip, le dossier decompresse et un bilan comparable a l'export direct`() {
        val d = depart("depart")
        val avant = Contenu.de(d.resolve("travaux.gpkg"))
        val sortie = dossier.resolve("rendu")
        assertNotNull(RendreScenario.jouer(d, scenario, sortie) as? ResultatRendu.Rendue)
        assertEquals(avant, Contenu.de(d.resolve("travaux.gpkg")), "la tournée de départ n'est pas touchée")
        assertTrue(Files.isRegularFile(sortie.resolve("rendu-surculus.zip")))
        assertTrue(Files.isDirectory(sortie.resolve("rendu-surculus/DCIM")))
        val t = LecteurTournee.lire(sortie.resolve("rendu-surculus/travaux.gpkg"))
        assertEquals(2, t.controles.size)
        assertEquals(8, t.plants.size)

        val direct = dossier.resolve("direct")
        ExporterScenario.jouer(depart("depart-direct"), scenario, direct)
        val a = JsonParser.parseString(Files.readString(sortie.resolve(ExporterScenario.BILAN_JSON))).asJsonObject
        val b = JsonParser.parseString(Files.readString(direct.resolve(ExporterScenario.BILAN_JSON))).asJsonObject
        for (cle in listOf("exporte_le", "placettes", "controles", "travaux", "plants", "etats_plants", "positions_controles")) {
            assertEquals(b.get(cle), a.get(cle), cle)
        }
        assertEquals(2, a.getAsJsonObject("uuid_ecrits").getAsJsonArray("controles").size())
    }
}
