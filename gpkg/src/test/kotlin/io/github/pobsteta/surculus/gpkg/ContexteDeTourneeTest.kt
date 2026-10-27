package io.github.pobsteta.surculus.gpkg

import io.github.pobsteta.surculus.core.placette.EtatDsf
import io.github.pobsteta.surculus.core.placette.Observation
import io.github.pobsteta.surculus.core.placette.UuidPlacette
import io.github.pobsteta.surculus.core.placette.UuidPlant
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Mission 012-A : ce que la session lit de la tournée ouverte (placettes, repère, plants et états du passage précédent). */
class ContexteDeTourneeTest {
    @TempDir
    lateinit var dossier: Path

    private fun tournee() = LecteurTournee.lire(TourneeDeTest.couchey(Files.createDirectories(dossier.resolve("t")), nom = "travaux.gpkg"))

    @Test
    fun `les placettes de la tournee deviennent des piquets en Lambert-93 avec leur rayon`() {
        val t = tournee()
        val c = ContexteDeTournee.de(t)
        assertEquals(3, c.placettes.size)
        assertEquals(t.epsg, c.epsg)
        val p = c.placettes.first { it.piquet.code == "P35-01" }
        assertEquals(3.99, p.rayonM)
        assertEquals(TourneeDeTest.placetteUuid(0), p.piquet.placette.valeur)
        assertEquals(TourneeDeTest.centre(0).xMetres, p.piquet.position.xMetres, 1e-6)
        assertEquals(TourneeDeTest.centre(0).yMetres, p.piquet.position.yMetres, 1e-6)
        assertEquals(t.empriseForet, c.empriseForet)
    }

    @Test
    fun `l'operateur est celui du projet, vide si la tournee n'en porte pas`() {
        val t = tournee()
        assertEquals("", ContexteDeTournee.de(t.copy(projet = null)).operateur)
        val projet = ProjetLu("Couchey", "Forêt", "Agent d'essai", null, null, null)
        assertEquals("Agent d'essai", ContexteDeTournee.de(t.copy(projet = projet)).operateur)
    }

    @Test
    fun `une placette sans position ou sans rayon n'est pas controlable, rien n'est invente`() {
        val t = tournee()
        val abimee = t.copy(placettes = t.placettes.mapIndexed { k, p -> if (k == 0) p.copy(position = null) else if (k == 1) p.copy(rayonM = null) else p })
        assertEquals(listOf("P35-03"), ContexteDeTournee.de(abimee).placettes.map { it.piquet.code })
    }

    @Test
    fun `les plants du passage precedent portent leur numero et leur dernier etat`() {
        val t = tournee()
        val p = TourneeDeTest.placetteUuid(0)
        val avecPlants = t.copy(
            controles = listOf(ControleTournee("c-hier", p, "2026-10-10 09:00:00"), ControleTournee("c-avant", p, "2026-09-01 09:00:00")),
            plants = listOf(PlantLu("pl-1", p, 1, "CHS", false, null, null), PlantLu("pl-2", p, 2, null, false, null, null)),
            etats = listOf(
                EtatLu("e1", "c-avant", "pl-1", "sain", null, null, 30),
                EtatLu("e2", "c-hier", "pl-1", "atteint", "animaux", "abroutissement", 35),
                EtatLu("e3", "c-hier", "pl-2", "hors_entreprise", null, null, null), // hors nomenclature : ignoré, jamais corrigé
            ),
        )
        val m = ContexteDeTournee.plantsAnterieurs(avecPlants)
        val l = m.getValue(UuidPlacette(p))
        assertEquals(listOf(1, 2), l.map { it.numero })
        val un = l.first { it.uuid == UuidPlant("pl-1") }
        assertEquals("CHS", un.essence)
        assertEquals(EtatDsf.ATTEINT, (un.observation as Observation.Observe).etat, "le contrôle le plus récent l'emporte")
        assertNull(l.first { it.uuid == UuidPlant("pl-2") }.observation)
        assertTrue(ContexteDeTournee.de(avecPlants).plantsAnterieurs.isNotEmpty())
    }
}
