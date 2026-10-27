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

    @Test fun `une tournee ouverte donne les placettes, pas la carte`() {
        val e = EtatNavigation().ouvrirTournee("t1")
        assertEquals("t1", e.tourneeId)
        assertEquals(false, e.carte)
        assertNull(e.placetteId)
    }

    @Test fun `placettes vers carte centree sur une placette, puis guidage, et les retours dans l'ordre inverse`() {
        val placettes = EtatNavigation().ouvrirTournee("t1")
        val carte = placettes.ouvrirCarte("p1")
        assertTrue(carte.carte)
        assertEquals("p1", carte.centreId)
        val guidage = carte.guider("p1")
        assertEquals("p1", guidage.placetteId)
        // Guidage -> Carte (toujours centrée) -> Placettes -> Tournees -> quitter.
        assertEquals(carte, guidage.retour())
        // Le récepteur, lancé à la première carte, reste lancé en revenant aux placettes (comme Groma).
        assertEquals(placettes.copy(recepteurOuvert = true), guidage.retour()!!.retour())
        assertEquals(EtatNavigation(), guidage.retour()!!.retour()!!.retour())
        assertTrue(guidage.retour()!!.retour()!!.retour()!!.estRacine)
        assertNull(guidage.retour()!!.retour()!!.retour()!!.retour())
    }

    @Test fun `l'icone carte ouvre la carte cadree sur la tournee, sans placette centrale`() {
        val carte = EtatNavigation().ouvrirTournee("t1").ouvrirCarte()
        assertTrue(carte.carte)
        assertNull(carte.centreId)
        assertEquals(EtatNavigation().ouvrirTournee("t1").copy(recepteurOuvert = true), carte.retour())
    }

    @Test fun `naviguer vers depuis la carte sans placette choisie donne le guidage de la prochaine`() {
        val guidage = EtatNavigation().ouvrirTournee("t1").ouvrirCarte().guider("p2")
        assertEquals("p2", guidage.placetteId)
        assertEquals(EtatNavigation().ouvrirTournee("t1").ouvrirCarte(), guidage.retour())
    }

    @Test fun `la carte ne s'ouvre pas sans tournee`() {
        assertEquals(EtatNavigation(), EtatNavigation().ouvrirCarte("p1"))
        assertEquals(EtatNavigation(), EtatNavigation().guider("p1"))
    }

    @Test fun `la rotation restitue placettes, carte centree et guidage`() {
        val etats = listOf(
            EtatNavigation().ouvrirTournee("t1"),
            EtatNavigation().ouvrirTournee("t1").ouvrirCarte(),
            EtatNavigation().ouvrirTournee("t1").ouvrirCarte("p1"),
            EtatNavigation().ouvrirTournee("t1").ouvrirCarte("p1").guider("p1"),
        )
        for (e in etats) assertEquals(e, EtatNavigation.depuisListe(e.enListe()), e.toString())
        // Une ancienne liste de trois éléments (avant la 061) se relit : tournée ouverte, sur les placettes.
        assertEquals(EtatNavigation().ouvrirTournee("t1"), EtatNavigation.depuisListe(listOf("TOURNEES", "t1", "")))
    }

    @Test fun `la sauvegarde pour la rotation restitue l'etat`() {
        val e = EtatNavigation().ouvrirTournee("t1").guider("p1")
        assertEquals(e, EtatNavigation.depuisListe(e.enListe()))
        assertEquals(EtatNavigation(), EtatNavigation.depuisListe(EtatNavigation().enListe()))
    }
}
