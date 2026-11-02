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
import io.github.pobsteta.surculus.core.session.ValeurControle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Mission 087 : l'écran d'arrivée sans position (ADR 0003, amendement du 2026-11-02, points 1 à 6 ; spec § 4.3,
 * « Arrivée sans position » ; décision de Pascal du 2026-11-02 : confirmation **sans parcelle**). Écrit par le testeur
 * depuis l'ADR, la spec et la fiche 087, sans lire `ArriveeControle.kt`, `MoyenneArrivee.kt` ni `EcranGuidage.kt`.
 *
 * Rouge (ne compile pas) jusqu'au `dev`. API supposée, à confirmer par le dev :
 * - `ArriveeSansPosition.SECONDES_SANS_FIX` = 30 (valeur **provisoire**, nommée) ;
 * - `ArriveeSansPosition.suite(msSansFix: Long, fixRecus: Int, localisationActive: Boolean, permissionAccordee: Boolean)`
 *   rend `SuiteSansFix` : `ATTENDRE`, `PROPOSER` (« Ouvrir sans position »), `ACTIVER_LOCALISATION`, `POSITION_RECUE` ;
 * - `ArriveeControle.ouvrirSansPosition(contexte, journal, horloge, essences, codeLu, sauvegarde, ugChoisie = null,
 *   confirmee = false)` rend un [EtatArrivee] ;
 * - `EtatArrivee.ConfirmerSansPosition(piquet: Piquet, plantsPointes: Int)` : la placette à confirmer, rien d'ouvert ;
 * - `TextesControle.confirmationSansPosition(piquet, plantsPointes): String` ;
 * - `ArriveeControle.ouvrir(..., ugChoisie, diagnostic: (String) -> Unit)` : la trace envoyée au journal technique
 *   (`JournalTechnique`) ; par défaut, le journal technique.
 *
 * Coordonnées : Lambert-93 (EPSG:2154), x = Est, y = Nord, mètres ; fix en (latitude, longitude) degrés WGS 84, noms
 * explicites. Les placettes du banc sont à Couchey (Côte-d'Or), en France.
 */
class OuvrirSansPositionArriveeTest {
    private val sec = 1_000L

    // ------------------------------------------------------------------ point 1 : 30 s sans aucun fix

    @Test
    fun `l'attente avant Ouvrir sans position est de 30 s, nommee`() {
        assertEquals(30, ArriveeSansPosition.SECONDES_SANS_FIX)
    }

    @Test
    fun `moins de 30 s sans fix - pas encore propose`() {
        for (ms in listOf(0L, 1L, 29 * sec, 30 * sec - 1)) {
            assertEquals(SuiteSansFix.ATTENDRE, ArriveeSansPosition.suite(ms, fixRecus = 0, localisationActive = true, permissionAccordee = true), "$ms ms")
        }
    }

    @Test
    fun `30 s sans aucun fix, localisation et permission accordees - Ouvrir sans position est propose`() {
        for (ms in listOf(30 * sec, 31 * sec, 600 * sec)) {
            assertEquals(SuiteSansFix.PROPOSER, ArriveeSansPosition.suite(ms, fixRecus = 0, localisationActive = true, permissionAccordee = true), "$ms ms")
        }
    }

    @Test
    fun `localisation coupee ou permission refusee - demander de l'activer, jamais Ouvrir sans position`() {
        for (ms in listOf(0L, 30 * sec, 600 * sec)) {
            assertEquals(SuiteSansFix.ACTIVER_LOCALISATION, ArriveeSansPosition.suite(ms, 0, localisationActive = false, permissionAccordee = true))
            assertEquals(SuiteSansFix.ACTIVER_LOCALISATION, ArriveeSansPosition.suite(ms, 0, localisationActive = true, permissionAccordee = false))
            assertEquals(SuiteSansFix.ACTIVER_LOCALISATION, ArriveeSansPosition.suite(ms, 0, localisationActive = false, permissionAccordee = false))
        }
    }

    @Test
    fun `un seul fix recu, meme apres 30 s - c'est une position, pas d'ouverture sans position`() {
        for (n in listOf(1, 5)) {
            val s = ArriveeSansPosition.suite(600 * sec, fixRecus = n, localisationActive = true, permissionAccordee = true)
            assertEquals(SuiteSansFix.POSITION_RECUE, s, "$n fix")
        }
    }

    // ------------------------------------------------------------------ point 6 : un seul fix suit la règle des 15 m

    @Test
    fun `un seul fix, au piquet - ouvert avec cette position, regle des 15 m`() = with(BancControle()) {
        val e = ArriveeControle.ouvrir(contexte, journal, horloge, essences, listOf(BancControle.fixSur(BancControle.POSITION_P1)), null, sauvegarde)
        val c = assertIs<EtatArrivee.Ouverte>(e).controleur
        c.dire(EtatDsf.SAIN)
        val v = controle(journal)
        assertFalse(v.sansPositionMesuree)
        assertNotNull(v.positionMesureeX)
        assertEquals(BancControle.POSITION_P1.xMetres, v.positionMesureeX!!, 0.01)
        assertEquals(BancControle.POSITION_P1.yMetres, v.positionMesureeY!!, 0.01)
    }

    @Test
    fun `un seul fix a 40 m, sans code - hors_15_m, pas d'ouverture sans position`() = with(BancControle()) {
        val loin = BancControle.fixSur(PointLambert93(BancControle.POSITION_P1.xMetres, BancControle.POSITION_P1.yMetres + 40.0))
        val e = ArriveeControle.ouvrir(contexte, journal, horloge, essences, listOf(loin), null, sauvegarde)
        assertEquals("hors_15_m", assertIs<EtatArrivee.Refus>(e).refus.code)
        assertEquals(0, journal.nombreDeLignes)
    }

    @Test
    fun `un seul fix a 40 m, code lu - le code l'emporte, la position mesuree est gardee`() = with(BancControle()) {
        val loinL93 = PointLambert93(BancControle.POSITION_P1.xMetres, BancControle.POSITION_P1.yMetres + 40.0)
        val e = ArriveeControle.ouvrir(contexte, journal, horloge, essences, listOf(BancControle.fixSur(loinL93)), "P35-01", sauvegarde)
        assertIs<EtatArrivee.Ouverte>(e).controleur.dire(EtatDsf.SAIN)
        val v = controle(journal)
        assertFalse(v.sansPositionMesuree)
        assertTrue(v.rattacheParIdentite)
        assertEquals(BancControle.POSITION_P1.xMetres, v.positionX)
        assertEquals(loinL93.yMetres, v.positionMesureeY!!, 0.01)
    }

    // ------------------------------------------------------------------ point 6 : hors de France, refusé (placette en Somalie)

    /** Chapitre 15 : la placette en Somalie. Un fix hors de France, même avec un code lu connu, est refusé. */
    @Test
    fun `fix en Somalie avec un code lu connu - refus, phrase qui dit quoi faire, journal technique, rien au journal des saisies`() = with(BancControle()) {
        val somalie = Position(latitude = 2.0469, longitude = 45.3182)
        val traces = ArrayList<String>()
        val e = ArriveeControle.ouvrir(contexte, journal, horloge, essences, listOf(somalie), "P35-01", sauvegarde, diagnostic = { traces += it })
        val r = assertIs<EtatArrivee.Refus>(e).refus
        assertEquals("hors_france", r.code)
        val texte = TextesControle.message(r)!!.texte
        assertTrue(texte.contains("couper puis rallumer la localisation", ignoreCase = true), texte)
        assertFalse(texte.contains("hors_france"), texte)
        assertEquals(1, traces.size, "le refus est écrit au journal technique")
        assertEquals(0, journal.nombreDeLignes, "pas au journal des saisies")
        assertEquals(0, sauvegarde.saisies)
    }

    @Test
    fun `latitude et longitude inversees, code lu - hors_france, trace, rien d'ouvert`() = with(BancControle()) {
        val bon = BancControle.fixSur(BancControle.POSITION_P1)
        val inverse = Position(latitude = bon.longitude, longitude = bon.latitude)
        val traces = ArrayList<String>()
        val e = ArriveeControle.ouvrir(contexte, journal, horloge, essences, listOf(inverse), "P35-01", sauvegarde, diagnostic = { traces += it })
        assertEquals("hors_france", assertIs<EtatArrivee.Refus>(e).refus.code)
        assertEquals(1, traces.size)
        assertEquals(0, journal.nombreDeLignes)
    }

    @Test
    fun `un fix en France ne laisse aucune trace de refus`() = with(BancControle()) {
        val traces = ArrayList<String>()
        ArriveeControle.ouvrir(contexte, journal, horloge, essences, positionsAuPiquet(), null, sauvegarde, diagnostic = { traces += it })
        assertEquals(emptyList(), traces)
    }

    // ------------------------------------------------------------------ points 2, 4, 5 : code lu, confirmation, ouverture

    @Test
    fun `aucun fix, aucun code - rien ne s'ouvre, rien d'ecrit`() = with(BancControle()) {
        for (code in listOf(null, "", "   ")) {
            val e = ArriveeControle.ouvrirSansPosition(contexte, journal, horloge, essences, code, sauvegarde, confirmee = true)
            assertFalse(e is EtatArrivee.Ouverte, "code « $code » : $e")
            assertFalse(e is EtatArrivee.ConfirmerSansPosition, "code « $code » : $e")
        }
        assertEquals(0, journal.nombreDeLignes)
    }

    @Test
    fun `aucun fix, code inconnu - code_inconnu, rien d'ouvert`() = with(BancControle()) {
        val e = ArriveeControle.ouvrirSansPosition(contexte, journal, horloge, essences, "ZZZ", sauvegarde, confirmee = true)
        assertEquals("code_inconnu", assertIs<EtatArrivee.Refus>(e).refus.code)
        assertEquals(0, journal.nombreDeLignes)
    }

    @Test
    fun `aucun fix, code lu connu - la placette est d'abord a confirmer, rien d'ouvert ni d'ecrit`() = with(BancControle()) {
        val e = ArriveeControle.ouvrirSansPosition(contexte, journal, horloge, essences, "P35-01", sauvegarde)
        val c = assertIs<EtatArrivee.ConfirmerSansPosition>(e)
        assertEquals(BancControle.P1, c.piquet.placette)
        assertEquals("P35-01", c.piquet.code)
        assertEquals(12, c.plantsPointes, "les 12 plants pointés du passage précédent")
        assertEquals(0, journal.nombreDeLignes)
    }

    @Test
    fun `confirmee, la placette s'ouvre a la position du piquet, sans position mesuree`() = with(BancControle()) {
        val e = ArriveeControle.ouvrirSansPosition(contexte, journal, horloge, essences, "P35-01", sauvegarde, confirmee = true)
        val c = assertIs<EtatArrivee.Ouverte>(e).controleur
        assertEquals(0, journal.nombreDeLignes, "ouvrir n'écrit rien")
        assertEquals("1 sur 12", c.instantane().plant.compteur, "les plants de P35-01 sont là")
        c.dire(EtatDsf.SAIN)
        val v = controle(journal)
        assertEquals(BancControle.P1.valeur, v.placetteUuid)
        assertTrue(v.sansPositionMesuree)
        assertTrue(v.rattacheParIdentite)
        assertNull(v.positionMesureeX); assertNull(v.positionMesureeY)
        assertEquals(BancControle.POSITION_P1.xMetres, v.positionX)
        assertEquals(BancControle.POSITION_P1.yMetres, v.positionY)
    }

    // ------------------------------------------------------------------ point 3 : homonymes, sans position rien ne tranche

    private val ug12 = UuidPlacette("placette-ug12")
    private val ug14 = UuidPlacette("placette-ug14")
    private val posA = BancControle.POSITION_P1
    private val posB = PointLambert93(posA.xMetres + 100.0, posA.yMetres)

    private fun homonymes(inverse: Boolean = false, nbUg12: Int = 25): ContexteControle {
        val a = PlacetteControlable(Piquet(ug12, "P01", posA, ug = "12"), 3.99)
        val b = PlacetteControlable(Piquet(ug14, "P01", posB, ug = "14"), 3.99)
        return ContexteControle(
            operateur = "Agent d'essai",
            placettes = if (inverse) listOf(b, a) else listOf(a, b),
            epsg = CodeEpsg.RGF93_V1_LAMBERT93,
            empriseForet = null,
            plantsAnterieurs = mapOf(ug12 to (1..nbUg12).map { PlantAnterieur(UuidPlant("ancien-$it"), it, null, null, Observation.Observe(EtatDsf.SAIN)) }),
        )
    }

    @Test
    fun `aucun fix, code dans deux UG - l'UG est demandee avant la confirmation, dans les deux ordres`() {
        for (inverse in listOf(false, true)) {
            val j = JournalEnMemoire()
            val e = ArriveeControle.ouvrirSansPosition(homonymes(inverse), j, HorlogeDeTest(), Essences(emptySet()), "P01", EcouteComptee(), confirmee = true)
            val c = assertIs<EtatArrivee.UgAChoisir>(e)
            assertEquals("P01", c.code)
            assertEquals(setOf<String?>("12", "14"), c.candidats.map { it.ug }.toSet())
            assertEquals(0, j.nombreDeLignes)
        }
    }

    @Test
    fun `aucun fix, UG choisie - la confirmation porte la placette de cette UG`() {
        val e = ArriveeControle.ouvrirSansPosition(homonymes(), JournalEnMemoire(), HorlogeDeTest(), Essences(emptySet()), "P01", EcouteComptee(), ugChoisie = "12")
        val c = assertIs<EtatArrivee.ConfirmerSansPosition>(e)
        assertEquals(ug12, c.piquet.placette)
        assertEquals("12", c.piquet.ug)
        assertEquals(25, c.plantsPointes)
        val f = ArriveeControle.ouvrirSansPosition(homonymes(), JournalEnMemoire(), HorlogeDeTest(), Essences(emptySet()), "P01", EcouteComptee(), ugChoisie = "14")
        assertEquals(ug14, assertIs<EtatArrivee.ConfirmerSansPosition>(f).piquet.placette)
    }

    @Test
    fun `aucun fix, UG choisie et confirmee - ouverte sur la placette choisie, sans position mesuree`() {
        val j = JournalEnMemoire()
        val e = ArriveeControle.ouvrirSansPosition(homonymes(), j, HorlogeDeTest(), Essences(emptySet()), "P01", EcouteComptee(), ugChoisie = "14", confirmee = true)
        assertIs<EtatArrivee.Ouverte>(e).controleur.let { c ->
            assertTrue(c.instantane().plant.aucunPlant, "UG 14 n'a aucun plant pointé")
        }
    }

    // ------------------------------------------------------------------ point 4 : le texte de la confirmation (sans parcelle)

    @Test
    fun `la confirmation dit le code, l'UG et le nombre de plants pointes, et demande d'ouvrir sans position`() {
        val t = TextesControle.confirmationSansPosition(Piquet(ug12, "P35-01", posA, ug = "12"), 25)
        assertTrue("P35-01" in t, t)
        assertTrue("UG 12" in t, t)
        assertTrue("25 plants pointés" in t, t)
        assertTrue(t.contains("ouvrir sans position", ignoreCase = true), t)
        assertTrue(t.trimEnd().endsWith("?"), "c'est une question : $t")
    }

    @Test
    fun `la confirmation ne parle pas de parcelle - la tournee ne la porte pas (decision de Pascal du 2026-11-02)`() {
        val t = TextesControle.confirmationSansPosition(Piquet(ug12, "P35-01", posA, ug = "12"), 25)
        assertFalse(t.contains("parcelle", ignoreCase = true), t)
    }

    @Test
    fun `placette sans UG dans la tournee - ni UG vide ni null dans le texte`() {
        val t = TextesControle.confirmationSansPosition(Piquet(BancControle.P1, "P35-01", posA), 12)
        assertTrue("P35-01" in t && "12 plants pointés" in t, t)
        assertFalse("null" in t || "UG ·" in t || "UG  " in t, t)
    }

    @Test
    fun `aucun texte technique dans la confirmation`() {
        val t = TextesControle.confirmationSansPosition(Piquet(ug12, "P35-01", posA, ug = "12"), 25)
        assertFalse("_" in t, t)
        assertFalse("placette-ug12" in t, "pas d'UUID : $t")
    }

    // ------------------------------------------------------------------ outils

    private fun controle(journal: JournalEnMemoire): ValeurControle {
        val l = journal.lignesCourantes().filter { it.type == "controle" }
        assertEquals(1, l.size, "un contrôle écrit")
        return assertNotNull(ValeurControle.lire(l.single().valeur), l.single().valeur)
    }
}
