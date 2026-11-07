package io.github.pobsteta.surculus.controle

import io.github.pobsteta.surculus.EtatNavigation
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.placette.Essences
import io.github.pobsteta.surculus.core.placette.Piquet
import io.github.pobsteta.surculus.core.placette.UuidPlacette
import io.github.pobsteta.surculus.core.reperes.CodeEpsg
import io.github.pobsteta.surculus.core.session.ContexteControle
import io.github.pobsteta.surculus.core.session.PlacetteControlable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Mission 086, reprise 1/3 : le code lu au guidage est transmis à l'écran de contrôle ; textes de l'aide et du guidage.
 *
 * API supposée, minimale (la seule couture testable hors Compose, sans lire les écrans) :
 * - `EtatNavigation.codeLu: String? = null` : le code lu au guidage, que l'écran de contrôle prend comme valeur initiale du
 *   champ « Code lu sur le piquet » ;
 * - `EtatNavigation.controlerDepuisGuidage(codeLu: String? = null)` : le pose en ouvrant le contrôle ;
 * - le code est sauvegardé avec l'état (rotation : `enListe` / `depuisListe`, ancien état lisible) et levé au retour.
 */
class CodeLuTransmisControleTest {
    private val enGuidage = EtatNavigation().ouvrirTournee("t1").ouvrirCarte("p1").guider("p1")

    @Test fun `le guidage transmet le code lu au controle qu'il ouvre`() {
        val e = enGuidage.controlerDepuisGuidage(codeLu = "P01")
        assertEquals("p1", e.controleId)
        assertEquals("P01", e.codeLu)
    }

    @Test fun `sans code lu, le champ reste vide (inchange)`() {
        assertNull(enGuidage.controlerDepuisGuidage().codeLu)
        assertNull(enGuidage.controlerDepuisGuidage(codeLu = null).codeLu)
        assertNull(EtatNavigation().codeLu)
    }

    @Test fun `un code blanc n'est pas transmis`() {
        assertNull(enGuidage.controlerDepuisGuidage(codeLu = "  ").codeLu)
    }

    @Test fun `la porte de la carte ne transmet aucun code`() {
        assertNull(EtatNavigation().ouvrirTournee("t1").ouvrirCarte("p1").controler("p1").codeLu)
    }

    @Test fun `la rotation garde le code lu, un ancien etat sauvegarde reste lisible`() {
        val e = enGuidage.controlerDepuisGuidage(codeLu = "P01")
        assertEquals(e, EtatNavigation.depuisListe(e.enListe()))
        assertEquals("P01", EtatNavigation.depuisListe(e.enListe()).codeLu)
        val ancien = e.enListe().take(8)
        assertNull(EtatNavigation.depuisListe(ancien).codeLu)
    }

    @Test fun `le retour ferme le controle et leve le code lu`() {
        val retour = enGuidage.controlerDepuisGuidage(codeLu = "P01").retour()!!
        assertNull(retour.controleId)
        assertNull(retour.codeLu)
        assertEquals("p1", retour.placetteId)
    }

    /** Avec ce code et deux homonymes non départagées, l'arrivée propose l'UG sans ressaisie (le code est celui de l'état). */
    @Test fun `le code transmis suffit - deux homonymes non departagees, le choix de l'UG est propose`() {
        val posA = BancControle.POSITION_P1
        val a = PlacetteControlable(Piquet(UuidPlacette("a"), "P01", posA, ug = "9"), 3.99)
        val b = PlacetteControlable(Piquet(UuidPlacette("b"), "P01", PointLambert93(posA.xMetres + 100.0, posA.yMetres), ug = "12"), 3.99)
        val ctx = ContexteControle("Agent d'essai", listOf(b, a), CodeEpsg.RGF93_V1_LAMBERT93, null, emptyMap())
        val loin = List(5) { BancControle.fixSur(PointLambert93(posA.xMetres + 50.0, posA.yMetres + 50.0)) }
        val codeDeLEtat = enGuidage.controlerDepuisGuidage(codeLu = "P01").codeLu
        val e = ArriveeControle.ouvrir(ctx, JournalEnMemoire(), HorlogeDeTest(), Essences(emptySet()), loin, codeDeLEtat, EcouteComptee())
        val c = assertIs<EtatArrivee.UgAChoisir>(e)
        assertEquals(listOf("9", "12"), c.candidats.map { it.ug }, "UG 9 avant UG 12")
    }
}
