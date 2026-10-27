package io.github.pobsteta.surculus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Mission 012-B1 : les deux portes du contrôle, leurs retours, la rotation, le récepteur GNSS, pas de réouverture en boucle. */
class NavigationControleTest {
    private val enTournee = EtatNavigation().ouvrirTournee("t1")
    private val surCarte = enTournee.ouvrirCarte("p1")
    private val enGuidage = surCarte.guider("p1")

    @Test fun `porte 1, Controler sur la carte - le controle s'ouvre, le retour rend la carte centree sur la placette`() {
        val e = surCarte.controler("p1")
        assertEquals("p1", e.controleId)
        assertNull(e.placetteId)
        assertEquals(surCarte.copy(centreId = "p1"), e.retour())
        assertEquals("p1", e.retour()!!.centreId)
        assertTrue(e.retour()!!.carte)
    }

    @Test fun `porte 2, arrivee du guidage - le controle s'ouvre, le retour rend le guidage`() {
        val e = enGuidage.controlerDepuisGuidage()
        assertEquals("p1", e.controleId)
        assertEquals("p1", e.placetteId)
        assertTrue(e.arriveeVue)
        val retour = e.retour()!!
        assertNull(retour.controleId)
        assertEquals("p1", retour.placetteId)
    }

    @Test fun `un seul pas en arriere ferme le controle, puis les retours habituels`() {
        val e = enGuidage.controlerDepuisGuidage()
        assertEquals(enGuidage.copy(arriveeVue = true), e.retour())
        assertEquals(surCarte.copy(arriveeVue = false), e.retour()!!.retour())
    }

    @Test fun `la rotation garde le controle ouvert et le drapeau d'arrivee`() {
        val e = enGuidage.controlerDepuisGuidage()
        assertEquals(e, EtatNavigation.depuisListe(e.enListe()))
        val c = surCarte.controler("p1")
        assertEquals(c, EtatNavigation.depuisListe(c.enListe()))
    }

    @Test fun `un ancien etat sauvegarde de six elements reste lisible`() {
        val ancien = listOf("TOURNEES", "t1", "p1", "true", "", "true")
        val e = EtatNavigation.depuisListe(ancien)
        assertNull(e.controleId)
        assertFalse(e.arriveeVue)
        assertEquals("p1", e.placetteId)
    }

    @Test fun `le recepteur GNSS reste ouvert par les deux portes, tant que le controle est ouvert`() {
        assertTrue(surCarte.controler("p1").recepteurDemande)
        assertTrue(enGuidage.controlerDepuisGuidage().recepteurDemande)
        // La porte de la carte part aussi d'un état sans récepteur déjà posé : elle le pose.
        assertTrue(enTournee.copy(carte = true).controler("p1").recepteurOuvert)
        // Le récepteur n'est pas levé en fermant le controle (la carte ou le guidage le gardent).
        assertTrue(surCarte.controler("p1").retour()!!.recepteurDemande)
        // Il est levé en revenant a l'accueil.
        assertFalse(EtatNavigation().recepteurDemande)
    }

    @Test fun `sans tournee ouverte, aucune porte ne s'ouvre`() {
        assertEquals(EtatNavigation(), EtatNavigation().controler("p1"))
        assertEquals(EtatNavigation(), EtatNavigation().controlerDepuisGuidage())
    }

    @Test fun `a l'arrivee, le guidage ouvre le controle une fois, a moins de 30 m seulement`() {
        assertFalse(enGuidage.ouvreALArrivee(30.0), "30 m exactement n'est pas dans le cercle (strictement moins)")
        assertFalse(enGuidage.ouvreALArrivee(120.0))
        assertTrue(enGuidage.ouvreALArrivee(29.9))
        assertFalse(surCarte.ouvreALArrivee(5.0), "pas de guidage, pas d'ouverture")
    }

    @Test fun `pas de reouverture en boucle - ouverture, retour, puis rien tant qu'on reste a moins de 40 m`() {
        var e = enGuidage
        assertTrue(e.ouvreALArrivee(25.0))
        e = e.controlerDepuisGuidage()
        e = e.retour()!!
        assertEquals("p1", e.placetteId)
        // Le guidage est recomposé à neuf à moins de 30 m : il ne doit pas rouvrir.
        assertFalse(e.ouvreALArrivee(25.0))
        // L'oscillation du GNSS entre 29 et 39 m ne réarme pas.
        listOf(29.0, 35.0, 39.9, 31.0).forEach { d ->
            e = e.apresDistance(d)
            assertFalse(e.ouvreALArrivee(d - 5), "à $d m")
            assertTrue(e.arriveeVue)
        }
        // À 40 m : réarmé ; une nouvelle arrivée rouvre.
        e = e.apresDistance(40.0)
        assertFalse(e.arriveeVue)
        assertTrue(e.ouvreALArrivee(28.0))
    }

    @Test fun `le controle deja ouvert ne se rouvre pas, et un nouveau guidage repart d'un drapeau baisse`() {
        val ouvert = enGuidage.controlerDepuisGuidage()
        assertFalse(ouvert.ouvreALArrivee(10.0))
        val reguide = ouvert.retour()!!.retour()!!.retour()!!.guider("p2")
        assertFalse(reguide.arriveeVue)
        assertTrue(reguide.ouvreALArrivee(10.0))
    }
}
