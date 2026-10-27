/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.gnss

import io.github.pobsteta.surculus.Destination
import io.github.pobsteta.surculus.EtatNavigation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Le récepteur suit la navigation : démarré à l'ouverture de la carte, arrêté à la sortie (mission 047). */
class PilotageRecepteurTest {
    private class Faux(var marche: Boolean = false) : Recepteur {
        val appels = mutableListOf<String>()
        override fun enMarche() = marche
        override fun demarrer() { appels += "demarrer"; marche = true }
        override fun arreter() { appels += "arreter"; marche = false }
    }

    /** Ce que fait `MainActivity` : entrer à l'ouverture, sortir à la fermeture, rien tant que `recepteurDemande` ne change pas. */
    private class Suivi(val pilote: PilotageRecepteur) {
        private var demande = false
        fun aller(e: EtatNavigation) {
            if (e.recepteurDemande == demande) return
            demande = e.recepteurDemande
            if (demande) pilote.entrer() else pilote.sortir()
        }
    }

    @Test fun `ouvrir la carte avec le recepteur actif le demarre`() {
        val r = Faux()
        PilotageRecepteur(r) { true }.entrer()
        assertEquals(listOf("demarrer"), r.appels)
    }

    @Test fun `ouvrir la carte avec le recepteur inactif ne le demarre jamais`() {
        val r = Faux()
        PilotageRecepteur(r) { false }.entrer()
        assertTrue(r.appels.isEmpty())
    }

    @Test fun `sortir de la tournee arrete le recepteur`() {
        val r = Faux()
        val p = PilotageRecepteur(r) { true }
        p.entrer()
        p.sortir()
        assertEquals(listOf("demarrer", "arreter"), r.appels)
        assertFalse(r.marche)
    }

    @Test fun `entrer alors qu'il tourne deja ne le redemarre pas`() {
        val r = Faux(marche = true)
        PilotageRecepteur(r) { true }.entrer()
        assertTrue(r.appels.isEmpty())
    }

    @Test fun `le reglage est relu a chaque entree`() {
        val r = Faux()
        var actif = false
        val p = PilotageRecepteur(r) { actif }
        p.entrer()
        actif = true
        p.sortir()
        p.entrer()
        assertEquals(listOf("arreter", "demarrer"), r.appels)
    }

    @Test fun `la carte, le guidage et l'essai demandent le recepteur, pas les parametres ni la liste`() {
        assertFalse(EtatNavigation().recepteurDemande)
        assertFalse(EtatNavigation().ouvrirParametres().recepteurDemande)
        // Mission 061, décision de Pascal (2026-10-27) : comme Groma, pas de récepteur sur l'écran « Placettes ».
        assertFalse(EtatNavigation().ouvrirTournee("t1").recepteurDemande)
        assertTrue(EtatNavigation().ouvrirTournee("t1").ouvrirCarte().recepteurDemande)
        assertTrue(EtatNavigation().ouvrirTournee("t1").ouvrirCarte("p1").recepteurDemande)
        assertTrue(EtatNavigation().ouvrirTournee("t1").guider("p1").recepteurDemande)
        assertTrue(EtatNavigation().ouvrirParametres().ouvrirEssai().recepteurDemande)
    }

    @Test fun `ouvrir la carte, guider, revenir a la liste, demarre une fois, arrete une fois`() {
        val r = Faux()
        val suivi = Suivi(PilotageRecepteur(r) { true })
        val placettes = EtatNavigation().ouvrirTournee("t1")
        val carte = placettes.ouvrirCarte()
        suivi.aller(placettes)
        assertEquals(emptyList(), r.appels)
        suivi.aller(carte)
        suivi.aller(carte.guider("p1"))
        suivi.aller(carte)
        assertEquals(listOf("demarrer"), r.appels)
        suivi.aller(carte.retour()!!)
        assertEquals(listOf("demarrer", "arreter"), r.appels)
        assertEquals(Destination.TOURNEES, carte.retour()!!.destination)
    }

    @Test fun `actif faux, toute la navigation ne demarre jamais le recepteur`() {
        val r = Faux()
        val suivi = Suivi(PilotageRecepteur(r) { false })
        val carte = EtatNavigation().ouvrirTournee("t1").ouvrirCarte()
        suivi.aller(carte)
        suivi.aller(carte.guider("p1"))
        suivi.aller(EtatNavigation().ouvrirParametres().ouvrirEssai())
        assertFalse(r.appels.contains("demarrer"))
    }
}
