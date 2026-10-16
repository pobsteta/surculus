package io.github.pobsteta.surculus.core.placette

import io.github.pobsteta.surculus.core.fixtures.Referentiels
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** États, causes, détails et hauteurs (spec § 4.3 et § 6 ; arbitrages de Pascal 2026-10-16, Q1, Q2, Q8, Q10). */
class ObservationsTest {

    private fun motif(v: Verdict<*>) = (v as Verdict.Refuse).refus.motif

    @Test
    fun `les etats du domaine sont exactement ceux de etats-plants yaml`() {
        assertEquals(Referentiels.etats, EtatDsf.entries.map { it.code }.toSet())
    }

    @Test
    fun `les groupes de cause du domaine sont exactement ceux de etats-plants yaml`() {
        assertEquals(Referentiels.groupesCauses, GroupeCause.entries.map { it.code }.toSet())
    }

    @Test
    fun `vivants = sains et atteints`() {
        assertEquals(setOf(EtatDsf.SAIN, EtatDsf.ATTEINT), EtatDsf.entries.filter { it.vivant }.toSet())
    }

    // Q2 : détail sur un absent refusé ; détail seul permis sur un atteint ou un mort.

    @Test
    fun `Q2a absent avec le detail gel refuse`() {
        assertEquals("detail_sur_etat", motif(Observations.valider("absent", causeDetail = "gel")))
    }

    @Test
    fun `Q2b absent avec le detail abroutissement refuse`() {
        assertEquals("detail_sur_etat", motif(Observations.valider("absent", causeDetail = "abroutissement")))
    }

    @Test
    fun `Q2c atteint avec le detail frottis sans groupe accepte`() {
        assertTrue(Observations.valider("atteint", causeDetail = "frottis").estAccepte)
    }

    @Test
    fun `Q2d mort avec le detail gel sans groupe accepte`() {
        assertTrue(Observations.valider("mort", causeDetail = "gel").estAccepte)
    }

    @Test
    fun `Q2e pas de controle de coherence entre detail et groupe`() {
        assertTrue(Observations.valider("mort", causeGroupe = "animaux", causeDetail = "gel").estAccepte)
    }

    @Test
    fun `Q2f detail vide ou fait d'espaces traite comme absent`() {
        val o = Observations.valider("absent", causeDetail = "   ").valeurOuEchec() as Observation.Observe
        assertEquals(null, o.causeDetail)
        assertTrue(Observations.valider("sain", causeDetail = "").estAccepte)
    }

    @Test
    fun `le detail est garde sans espaces autour`() {
        val o = Observations.valider("atteint", causeDetail = " hylobe ").valeurOuEchec() as Observation.Observe
        assertEquals("hylobe", o.causeDetail)
    }

    // Q8 : hauteur entière strictement positive.

    @Test
    fun `Q8a hauteur nulle refusee`() {
        assertEquals("hauteur_invalide", motif(Observations.valider("sain", hauteurCm = 0)))
    }

    @Test
    fun `Q8b hauteur negative refusee`() {
        assertEquals("hauteur_invalide", motif(Observations.valider("atteint", hauteurCm = -5)))
    }

    @Test
    fun `Q8c hauteur de 1 cm acceptee`() {
        assertTrue(Observations.valider("sain", hauteurCm = 1).estAccepte)
    }

    @Test
    fun `Q8d un mort a 0 cm refuse`() {
        assertEquals("hauteur_invalide", motif(Observations.valider("mort", hauteurCm = 0)))
    }

    @Test
    fun `un groupe inconnu est refuse avec son motif`() {
        assertEquals("cause_inconnue", motif(Observations.valider("mort", causeGroupe = "gel")))
        assertEquals("etat_inconnu", motif(Observations.valider("Sain")))
    }

    @Test
    fun `non observe n'est pas un etat et ne porte aucune mesure`() {
        assertEquals(Observation.NonObserve, Observations.valider("non_observe").valeurOuEchec())
        assertEquals("mesure_sur_non_observe", motif(Observations.valider("non_observe", hauteurCm = 30)))
        assertEquals("mesure_sur_non_observe", motif(Observations.valider("non_observe", causeGroupe = "animaux")))
        assertEquals("etat_inconnu", motif(Observations.valider("non observé")))
    }

    @Test
    fun `une observation invalide ne se construit pas`() {
        assertThrows<IllegalStateException> { Observation.Observe(EtatDsf.SAIN, causeGroupe = GroupeCause.ANIMAUX) }
        assertThrows<IllegalStateException> { Observation.Observe(EtatDsf.ATTEINT, hauteurCm = 0) }
    }

    @Test
    fun `abrouti seulement pour un vivant`() {
        assertTrue(Observation.Observe(EtatDsf.SAIN, causeDetail = "abroutissement").abrouti)
        assertTrue(Observation.Observe(EtatDsf.ATTEINT, GroupeCause.ANIMAUX, "abroutissement").abrouti)
        assertFalse(Observation.Observe(EtatDsf.MORT, GroupeCause.ANIMAUX, "abroutissement").abrouti)
        assertFalse(Observation.Observe(EtatDsf.ATTEINT, GroupeCause.ANIMAUX, "Abroutissement").abrouti)
    }

    @Test
    fun `essence injectee, code invente refuse`() {
        val essences = Essences(Referentiels.essences)
        assertTrue(essences.valider("DOU").estAccepte)
        assertEquals("essence_inconnue", motif(essences.valider("XYZ")))
    }
}
