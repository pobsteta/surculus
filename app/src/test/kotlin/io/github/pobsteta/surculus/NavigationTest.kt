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

    @Test fun `le menu a trois points range ouvrir un dossier, ouvrir un zip, parametres, a propos`() {
        assertEquals(
            listOf(EntreeMenu.OUVRIR_DOSSIER, EntreeMenu.OUVRIR_ZIP, EntreeMenu.PARAMETRES, EntreeMenu.A_PROPOS),
            ENTREES_MENU,
        )
    }

    private fun source(chemin: String) = java.io.File("src/main/kotlin/io/github/pobsteta/surculus/$chemin").readText()

    @Test fun `l'accueil n'a plus de bouton d'ouverture, les deux ouvertures sont dans le menu`() {
        val ecran = source("tournee/EcranTournees.kt")
        assertTrue(!Regex("""\bButton\(""").containsMatchIn(ecran), "plus aucun Button plein sur l'accueil")
        assertTrue(!ecran.contains("Modifier.weight(1f).height(64.dp)"))
        assertTrue(ecran.contains("EntreeMenu.OUVRIR_DOSSIER") && ecran.contains("EntreeMenu.OUVRIR_ZIP"))
    }

    @Test fun `le message de liste vide renvoie au menu`() {
        val chaines = java.io.File("src/main/res/values/strings_tournees.xml").readText()
        assertTrue(chaines.contains("Ouvrez le dossier ou le .zip de sommieR depuis le menu ⋮."))
    }

    @Test fun `les selecteurs s'ouvrent d'abord dans Download Surculus`() {
        assertTrue(source("tournee/EcranTournees.kt").contains("primary:Download/Surculus"))
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
