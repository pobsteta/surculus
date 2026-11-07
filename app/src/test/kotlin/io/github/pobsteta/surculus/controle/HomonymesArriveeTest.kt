package io.github.pobsteta.surculus.controle

import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.gnss.Position
import io.github.pobsteta.surculus.core.placette.Essences
import io.github.pobsteta.surculus.core.placette.EtatDsf
import io.github.pobsteta.surculus.core.placette.Observation
import io.github.pobsteta.surculus.core.placette.Piquet
import io.github.pobsteta.surculus.core.placette.UuidPlacette
import io.github.pobsteta.surculus.core.placette.UuidPlant
import io.github.pobsteta.surculus.core.reperes.CodeEpsg
import io.github.pobsteta.surculus.core.session.ContexteControle
import io.github.pobsteta.surculus.core.session.PlacetteControlable
import io.github.pobsteta.surculus.core.session.PlantAnterieur
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Mission 086 : deux placettes « P01 » dans deux UG (cas de test), l'écran d'arrivée demande l'UG au lieu de prendre la première.
 *
 * API supposée, à confirmer par le dev :
 * - `Piquet(..., ug: String? = null)` ;
 * - `EtatArrivee.UgAChoisir(code: String, candidats: List<Piquet>)` ;
 * - `ArriveeControle.ouvrir(..., sauvegarde, ugChoisie: String? = null)` (paramètre nommé, en dernier).
 *
 * On reconnaît la placette ouverte à ses plants antérieurs : UG 12 en porte 3, UG 14 aucun (`plant.aucunPlant`).
 */
class HomonymesArriveeTest {
    private val posA = BancControle.POSITION_P1
    private val posB = PointLambert93(posA.xMetres + 100.0, posA.yMetres)
    private val ug12 = UuidPlacette("placette-ug12")
    private val ug14 = UuidPlacette("placette-ug14")

    private fun contexte(inverse: Boolean): ContexteControle {
        val a = PlacetteControlable(Piquet(ug12, "P01", posA, ug = "12"), 3.99)
        val b = PlacetteControlable(Piquet(ug14, "P01", posB, ug = "14"), 3.99)
        return ContexteControle(
            operateur = "Agent d'essai",
            placettes = if (inverse) listOf(b, a) else listOf(a, b),
            epsg = CodeEpsg.RGF93_V1_LAMBERT93,
            empriseForet = null,
            plantsAnterieurs = mapOf(
                ug12 to (1..3).map { PlantAnterieur(UuidPlant("ancien-$it"), it, null, null, Observation.Observe(EtatDsf.ATTEINT)) },
            ),
        )
    }

    private fun ouvrir(positions: List<Position>, codeLu: String? = "P01", ug: String? = null, inverse: Boolean = false): EtatArrivee =
        ArriveeControle.ouvrir(
            contexte(inverse), JournalEnMemoire(), HorlogeDeTest(), Essences(emptySet()), positions, codeLu, EcouteComptee(),
            ugChoisie = ug,
        )

    private fun fixes(p: PointLambert93) = List(5) { BancControle.fixSur(p) }
    private fun auNordDeA(m: Double) = fixes(PointLambert93(posA.xMetres, posA.yMetres + m))

    private fun ugsProposees(e: EtatArrivee): Set<String?> {
        val c = assertIs<EtatArrivee.UgAChoisir>(e)
        assertEquals("P01", c.code)
        return c.candidats.map { it.ug }.toSet()
    }

    private fun estUg12(e: EtatArrivee) = !assertIs<EtatArrivee.Ouverte>(e).controleur.instantane().plant.aucunPlant

    @Test fun `une seule homonyme a 14,9 m - ouverte sans question, dans les deux ordres`() {
        for (inverse in listOf(false, true)) assertTrue(estUg12(ouvrir(auNordDeA(14.9), inverse = inverse)))
    }

    @Test fun `une seule homonyme a 15 m - borne incluse, ouverte sans question`() {
        assertTrue(estUg12(ouvrir(auNordDeA(15.0))))
    }

    @Test fun `aucune homonyme a 15 m - l'ecran propose le choix de l'UG, rien n'est ouvert`() {
        for (inverse in listOf(false, true)) {
            assertEquals(setOf<String?>("12", "14"), ugsProposees(ouvrir(auNordDeA(15.1), inverse = inverse)))
        }
    }

    @Test fun `le choix de l'UG ouvre la placette choisie, meme a plus de 15 m`() {
        assertTrue(estUg12(ouvrir(auNordDeA(40.0), ug = "12")))
        assertFalse(estUg12(ouvrir(auNordDeA(40.0), ug = "14")))
    }

    // Le cas « les deux a 15 m au plus » est couvert dans :core (RattachementHomonymesTest) : ici les placettes sont à 100 m.

    /** Placette de la Somalie (chapitre 15) : le refus hors de France passe avant tout choix d'UG. */
    @Test fun `un fix en Somalie est refuse hors_france avant tout choix`() {
        val somalie = Position(latitude = 2.0469, longitude = 45.3182)
        for (ug in listOf(null, "12")) {
            assertEquals("hors_france", assertIs<EtatArrivee.Refus>(ouvrir(listOf(somalie), ug = ug)).refus.code)
        }
    }

    @Test fun `latitude et longitude inversees - hors_france, pas de choix d'UG`() {
        val bon = BancControle.fixSur(posA)
        val inverse = Position(latitude = bon.longitude, longitude = bon.latitude)
        assertEquals("hors_france", assertIs<EtatArrivee.Refus>(ouvrir(listOf(inverse))).refus.code)
    }
}
