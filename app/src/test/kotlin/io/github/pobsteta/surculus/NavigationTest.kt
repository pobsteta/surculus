package io.github.pobsteta.surculus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NavigationTest {
    @Test fun `la racine est la liste des tournees`() {
        val e = EtatNavigation()
        assertEquals(Destination.TOURNEES, e.destination)
        assertTrue(e.estRacine)
    }

    @Test fun `retour sur la liste des tournees quitte l'application`() {
        assertNull(EtatNavigation().retour())
    }

    @Test fun `parametres depuis les tournees, retour vers les tournees`() {
        val e = EtatNavigation().ouvrirParametres()
        assertEquals(Destination.PARAMETRES, e.destination)
        assertEquals(EtatNavigation(), e.retour())
    }

    @Test fun `essai GNSS depuis parametres, retour vers parametres puis tournees`() {
        val e = EtatNavigation().ouvrirParametres().ouvrirEssai()
        assertEquals(Destination.ESSAI, e.destination)
        assertEquals(EtatNavigation().ouvrirParametres(), e.retour())
        assertEquals(EtatNavigation(), e.retour()!!.retour())
    }

    @Test fun `restauration depuis parametres, retour vers parametres puis tournees`() {
        val e = EtatNavigation().ouvrirParametres().ouvrirRestauration()
        assertEquals(Destination.RESTAURATION, e.destination)
        assertEquals(EtatNavigation().ouvrirParametres(), e.retour())
        assertEquals(EtatNavigation(), e.retour()!!.retour())
    }

    @Test fun `parametres n'est pas la racine`() {
        assertTrue(!EtatNavigation().ouvrirParametres().estRacine)
    }

    @Test fun `le menu a trois points ne garde que parametres et a propos`() {
        assertEquals(listOf(EntreeMenu.PARAMETRES, EntreeMenu.A_PROPOS), ENTREES_MENU)
    }

    @Test fun `la rotation restitue parametres et les ecrans qui en dependent`() {
        for (e in listOf(EtatNavigation().ouvrirParametres(), EtatNavigation().ouvrirEssai(), EtatNavigation().ouvrirRestauration())) {
            assertEquals(e, EtatNavigation.depuisListe(e.enListe()))
        }
    }

    @Test fun `guidage vers carte puis tournees puis quitter`() {
        val guidage = EtatNavigation().ouvrirTournee("t1").guider("p1")
        assertEquals("p1", guidage.placetteId)
        val carte = guidage.retour()!!
        assertEquals("t1", carte.tourneeId)
        assertNull(carte.placetteId)
        val liste = carte.retour()!!
        assertTrue(liste.estRacine)
        assertNull(liste.retour())
    }

    @Test fun `la sauvegarde pour la rotation restitue l'etat`() {
        val e = EtatNavigation().ouvrirTournee("t1").guider("p1")
        assertEquals(e, EtatNavigation.depuisListe(e.enListe()))
        assertEquals(EtatNavigation(), EtatNavigation.depuisListe(EtatNavigation().enListe()))
    }
}
