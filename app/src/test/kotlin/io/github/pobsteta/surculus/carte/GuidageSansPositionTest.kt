package io.github.pobsteta.surculus.carte

import io.github.pobsteta.surculus.controle.ArriveeControle
import io.github.pobsteta.surculus.controle.ArriveeSansPosition
import io.github.pobsteta.surculus.controle.BancControle
import io.github.pobsteta.surculus.controle.EtatArrivee
import io.github.pobsteta.surculus.controle.SuiteSansFix
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.placette.EtatDsf
import io.github.pobsteta.surculus.core.session.ValeurControle
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Mission 089 : « Ouvrir sans position » depuis le guidage (fiche 089, rubrique « Tests » ; spec § 4.3 « Arrivée sans
 * position » points 1, 2, 4, 5, 6 et 10 ; ADR 0003, amendement du 2026-11-02). Écrit par le testeur sans lire
 * `EcranGuidage.kt`, `ArriveeControle.kt`, `MoyenneArrivee.kt` ni `SessionControle.kt`.
 *
 * Deux familles :
 * - **règle partagée** (verte d'emblée : c'est la règle de la 087, que le guidage doit réutiliser) : ce que le guidage doit
 *   obtenir de l'arrivée dans chacun des cas de la fiche ;
 * - **branchement** (rouge jusqu'au `dev`) : le guidage lit la règle de la 087 au lieu de la recopier, comme les autres
 *   tests de sources du projet (`BandeauCarteGuidageTest`). Aucun test ne fige la place du champ, sa visibilité avant 30 s,
 *   ni le départ du compte des 30 s (questions 1 à 3 de la fiche, à trancher par Pascal).
 *
 * Coordonnées : Lambert-93 (EPSG:2154), x = Est, y = Nord, mètres ; fix en (latitude, longitude) degrés WGS 84 ; P35-01 à
 * Couchey (Côte-d'Or), en France.
 */
class GuidageSansPositionTest {
    private val sec = 1_000L

    // ------------------------------------------------------------------ règle partagée (fiche : « Tests »)

    @Test
    fun `guidage sans fix avant 30 s - pas d'ouverture sans position`() {
        for (ms in listOf(0L, 15 * sec, 30 * sec - 1)) {
            assertEquals(SuiteSansFix.ATTENDRE, ArriveeSansPosition.suite(ms, 0, localisationActive = true, permissionAccordee = true), "$ms ms")
        }
        assertEquals(30, ArriveeSansPosition.SECONDES_SANS_FIX, "valeur provisoire, une seule source")
    }

    @Test
    fun `guidage sans fix apres 30 s - propose, sans code le refus est code_requis, rien d'ecrit`() = with(BancControle()) {
        assertEquals(SuiteSansFix.PROPOSER, ArriveeSansPosition.suite(30 * sec, 0, localisationActive = true, permissionAccordee = true))
        for (code in listOf(null, "", "  ")) {
            val e = ArriveeControle.ouvrirSansPosition(contexte, journal, horloge, essences, code, sauvegarde, confirmee = true)
            assertEquals("code_requis", assertIs<EtatArrivee.Refus>(e).refus.code, "code « $code »")
        }
        assertEquals(0, journal.nombreDeLignes)
        assertEquals(0, sauvegarde.saisies)
    }

    @Test
    fun `guidage sans fix apres 30 s, code de la tournee - confirmation puis controle ouvert sans position mesuree`() = with(BancControle()) {
        val conf = assertIs<EtatArrivee.ConfirmerSansPosition>(ArriveeControle.ouvrirSansPosition(contexte, journal, horloge, essences, "P35-01", sauvegarde))
        assertEquals(BancControle.P1, conf.piquet.placette)
        assertEquals(0, journal.nombreDeLignes, "confirmer n'écrit rien")
        val c = assertIs<EtatArrivee.Ouverte>(ArriveeControle.ouvrirSansPosition(contexte, journal, horloge, essences, "P35-01", sauvegarde, confirmee = true)).controleur
        c.dire(EtatDsf.SAIN)
        val v = ValeurControle.lire(journal.lignesCourantes().single { it.type == "controle" }.valeur)!!
        assertTrue(v.sansPositionMesuree)
        assertNull(v.positionMesureeX)
        assertEquals(BancControle.POSITION_P1.xMetres, v.positionX, 0.01, "position du contrôle = piquet")
    }

    @Test
    fun `guidage sans fix, code inconnu de la tournee - refuse, rien d'ecrit`() = with(BancControle()) {
        val e = ArriveeControle.ouvrirSansPosition(contexte, journal, horloge, essences, "ZZZ", sauvegarde, confirmee = true)
        assertEquals("code_inconnu", assertIs<EtatArrivee.Refus>(e).refus.code)
        assertEquals(0, journal.nombreDeLignes)
        assertEquals(0, sauvegarde.saisies)
    }

    @Test
    fun `localisation coupee ou permission refusee - demander de l'activer, jamais d'ouverture, a toute duree`() {
        for (ms in listOf(0L, 30 * sec, 3600 * sec)) {
            assertEquals(SuiteSansFix.ACTIVER_LOCALISATION, ArriveeSansPosition.suite(ms, 0, localisationActive = false, permissionAccordee = true))
            assertEquals(SuiteSansFix.ACTIVER_LOCALISATION, ArriveeSansPosition.suite(ms, 0, localisationActive = true, permissionAccordee = false))
        }
    }

    @Test
    fun `un fix arrive pendant l'attente - retour a la regle des 15 m`() = with(BancControle()) {
        assertEquals(SuiteSansFix.POSITION_RECUE, ArriveeSansPosition.suite(10 * sec, 1, localisationActive = true, permissionAccordee = true))
        assertEquals(SuiteSansFix.POSITION_RECUE, ArriveeSansPosition.suite(600 * sec, 1, localisationActive = true, permissionAccordee = true))
        val loin = BancControle.fixSur(PointLambert93(BancControle.POSITION_P1.xMetres, BancControle.POSITION_P1.yMetres + 40.0))
        assertEquals("hors_15_m", assertIs<EtatArrivee.Refus>(ArriveeControle.ouvrir(contexte, journal, horloge, essences, listOf(loin), null, sauvegarde)).refus.code)
        assertIs<EtatArrivee.Ouverte>(ArriveeControle.ouvrir(contexte, journal, horloge, essences, listOf(BancControle.fixSur(BancControle.POSITION_P1)), null, sauvegarde))
        assertEquals(0, journal.nombreDeLignes, "l'arrivée n'écrit rien")
    }

    @Test
    fun `avec un fix, comportement inchange - code lu a 40 m l'emporte, position mesuree gardee`() = with(BancControle()) {
        val loinL93 = PointLambert93(BancControle.POSITION_P1.xMetres, BancControle.POSITION_P1.yMetres + 40.0)
        val e = ArriveeControle.ouvrir(contexte, journal, horloge, essences, listOf(BancControle.fixSur(loinL93)), "P35-01", sauvegarde)
        assertIs<EtatArrivee.Ouverte>(e).controleur.dire(EtatDsf.SAIN)
        val v = ValeurControle.lire(journal.lignesCourantes().single { it.type == "controle" }.valeur)!!
        assertFalse(v.sansPositionMesuree)
        assertEquals(loinL93.yMetres, v.positionMesureeY!!, 0.01)
    }

    // ------------------------------------------------------------------ branchement (rouge jusqu'au dev)

    private val racine = File("src/main/kotlin/io/github/pobsteta/surculus").takeIf { it.isDirectory }
        ?: File("app/src/main/kotlin/io/github/pobsteta/surculus")
    private val guidage get() = File(racine, "carte/EcranGuidage.kt").readText()

    private val ecranControle get() = File(racine, "controle/EcranControle.kt").readText()

    @Test
    fun `le guidage et l'ecran de controle appellent le meme morceau partage de la 087`() {
        for ((nom, source) in listOf("EcranGuidage" to guidage, "EcranControle" to ecranControle)) {
            for (appel in listOf("OuvertureSansPosition.suite(", "OuvertureSansPosition.ouvrir(", "AttenteSansFix(", "ConfirmationSansPosition(")) {
                assertTrue(appel in source, "$nom doit appeler $appel (délai, code requis, placette à confirmer : une seule source)")
            }
        }
    }

    @Test
    fun `ArriveeControle ouvrirSansPosition n'apparait que dans le morceau partage - pas de recopie`() {
        assertTrue(File(racine, "controle/OuvertureSansPosition.kt").isFile, "le morceau partagé existe")
        val ailleurs = racine.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "OuvertureSansPosition.kt" && "ArriveeControle.ouvrirSansPosition(" in it.readText() }
            .map { it.name }.toList()
        assertEquals(emptyList(), ailleurs, "ArriveeControle.ouvrirSansPosition( ne doit être appelé que par OuvertureSansPosition.kt")
    }

    @Test
    fun `le guidage ne recopie pas le delai de 30 s`() {
        assertFalse(Regex("30_000|30\\s*\\*\\s*1_?000|30L\\s*\\*").containsMatchIn(guidage), "le délai vient de ArriveeSansPosition")
    }

    @Test
    fun `avec un fix le guidage garde l'arrivee et le rattachement d'aujourd'hui`() {
        val g = guidage
        assertTrue("cap?.arrive" in g, "l'arrivée à 30 m reste")
        assertTrue("R.string.controle_controler" in g, "le bouton Contrôler reste")
    }
}
