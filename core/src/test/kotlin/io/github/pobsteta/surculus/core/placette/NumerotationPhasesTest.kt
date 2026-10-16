package io.github.pobsteta.surculus.core.placette

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Numéros provisoires pendant le premier contrôle, figés à sa clôture, puis numéro suivant le plus grand
 * (spec § 6, arbitrage de Pascal 2026-10-16, Q4). Azimuts géographiques en degrés, distances horizontales en m.
 */
class NumerotationPhasesTest {

    private val a = PlantANumeroter("A", azimutGeoDeg = 90.0, distanceHorizontaleM = 2.0)
    private val b = PlantANumeroter("B", azimutGeoDeg = 10.0, distanceHorizontaleM = 3.0)
    private val c = UuidPlant("C")

    @Test
    fun `Q4a les numeros provisoires sont recalcules a chaque pointage`() {
        assertEquals(mapOf(UuidPlant("A") to 1), Numerotation.provisoires(listOf(a)).numeros)
        assertEquals(mapOf(UuidPlant("B") to 1, UuidPlant("A") to 2), Numerotation.provisoires(listOf(a, b)).numeros)
    }

    @Test
    fun `Q4b a la cloture du premier controle les numeros sont figes`() {
        val figes = Numerotation.figer(Numerotation.provisoires(listOf(a, b))).valeurOuEchec()
        assertTrue(figes is NumerosPlacette.Figes)
        assertEquals(mapOf(UuidPlant("B") to 1, UuidPlant("A") to 2), figes.numeros)
    }

    @Test
    fun `Q4c au controle suivant un plant ajoute prend le numero suivant, les autres ne changent pas`() {
        val figes = Numerotation.figer(Numerotation.provisoires(listOf(a, b))).valeurOuEchec()
        // C serait le n° 1 à 5° s'il était numéroté par azimut : il prend le n° 3.
        val apres = Numerotation.ajouter(figes, c).valeurOuEchec()
        assertEquals(mapOf(UuidPlant("B") to 1, UuidPlant("A") to 2, c to 3), apres.numeros)
    }

    @Test
    fun `Q4d placette vide, cloture, puis premier ajout n° 1`() {
        val figes = Numerotation.figer(Numerotation.provisoires(emptyList())).valeurOuEchec()
        assertEquals(1, Numerotation.ajouter(figes, c).valeurOuEchec().numeros.getValue(c))
    }

    @Test
    fun `Q4e figer une placette deja figee est refuse`() {
        val figes = Numerotation.figer(Numerotation.provisoires(listOf(a))).valeurOuEchec()
        assertEquals(Verdict.Refuse(RefusPlacette.NumerosDejaFiges), Numerotation.figer(figes))
    }

    @Test
    fun `ajouter un UUID deja numerote est refuse`() {
        val figes = Numerotation.figer(Numerotation.provisoires(listOf(a))).valeurOuEchec()
        assertEquals("uuid_en_double", (Numerotation.ajouter(figes, UuidPlant("A")) as Verdict.Refuse).refus.motif)
    }

    @Test
    fun `un azimut hors 0-360 ou un identifiant en double est refuse`() {
        assertThrows<IllegalArgumentException> { PlantANumeroter("X", 360.0, 1.0) }
        assertThrows<IllegalArgumentException> { PlantANumeroter("X", -0.1, 1.0) }
        assertThrows<IllegalArgumentException> { Numerotation.ordonner(listOf(a, a)) }
    }
}
