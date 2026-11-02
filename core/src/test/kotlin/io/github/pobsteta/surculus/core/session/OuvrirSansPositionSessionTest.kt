package io.github.pobsteta.surculus.core.session

import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.placette.Piquet
import io.github.pobsteta.surculus.core.placette.UuidPlacette
import io.github.pobsteta.surculus.core.reperes.CodeEpsg
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Mission 087 : la session de contrôle ouverte sans aucune position mesurée, sur le seul code lu (ADR 0003, amendement
 * du 2026-11-02, points 2, 3, 5, 8 et 10 ; spec § 4.3 « Arrivée sans position » ; `docs/formats/journal-valeurs-v2.md`).
 * Écrit par le testeur depuis l'ADR et la spec, sans lire `SessionControle.kt`.
 *
 * Rouge (ne compile pas) jusqu'au `dev`. API supposée, à confirmer par le dev :
 * - `Arrivee.SansPosition` : nouveau cas (objet) de [Arrivee], « aucun fix reçu » ;
 * - `SessionControle.ouvrir(..., Arrivee.SansPosition, codeLu, sauvegarde, ugChoisie)` : même fonction qu'avec position ;
 * - `ValeurControle.sansPositionMesuree`, `positionMesureeX/Y: Double?` (voir [JournalValeursV2Test]).
 *
 * La confirmation de la placette (point 4) et l'attente de 30 s (point 1) sont des affaires de l'écran : voir
 * `app/.../controle/OuvrirSansPositionArriveeTest`.
 *
 * Coordonnées : Lambert-93 (EPSG:2154), x = Est, y = Nord, en mètres ; P35-01 à Couchey (Côte-d'Or), en France.
 */
class OuvrirSansPositionSessionTest {
    private fun BancSession.ouvrirSans(codeLu: String?, ugChoisie: String? = null, ctx: ContexteControle = contexte): OuvertureControle =
        SessionControle.ouvrir(ctx, journal, horloge, essences, Arrivee.SansPosition, codeLu, ecoute, ugChoisie)

    private fun BancSession.controles(): List<ValeurControle> = journal.deType(FormatValeurs.TYPE_CONTROLE).map { assertNotNull(ValeurControle.lire(it.valeur), it.valeur) }

    // ------------------------------------------------------------------ cas nominal

    @Test
    fun `code lu connu - ouvert a la position du piquet, sans position mesuree, rattache par identite`() = with(BancSession()) {
        val s = assertIs<OuvertureControle.Ouverte>(ouvrirSans("P35-01")).session
        assertEquals(0, journal.lignes.size, "ouvrir n'écrit rien : la première saisie écrit le contrôle")
        assertIs<SuiteSession.Fait>(s.pointe(1.0, 10.0))
        assertIs<SuiteSession.Fait>(s.dire("sain"))
        val brut = journal.deType(FormatValeurs.TYPE_CONTROLE).single().valeur!!
        assertTrue(brut.startsWith("{\"v\":2,"), brut)
        assertTrue("\"position_mesuree_x\":null,\"position_mesuree_y\":null" in brut, brut)
        assertTrue("\"sans_position_mesuree\":true" in brut, brut)
        val c = controles().single()
        assertEquals(BancSession.P1.valeur, c.placetteUuid)
        assertEquals("P35-01", c.codePlacette)
        assertEquals(BancSession.POSITION_P1.xMetres, c.positionX, "x (Est) du piquet de sommieR")
        assertEquals(BancSession.POSITION_P1.yMetres, c.positionY, "y (Nord) du piquet de sommieR")
        assertNull(c.positionMesureeX)
        assertNull(c.positionMesureeY)
        assertTrue(c.rattacheParIdentite, "définition élargie : le code lu a désigné la placette")
        assertTrue(c.sansPositionMesuree)
        // Le plant pointé est dérivé du piquet : à 1 m de lui, en Lambert-93.
        val p = ValeurPointage.lire(journal.deType(FormatValeurs.TYPE_POINTAGE).single().valeur)!!
        assertEquals(1.0, Math.hypot(p.x - BancSession.POSITION_P1.xMetres, p.y - BancSession.POSITION_P1.yMetres), 1e-6)
    }

    @Test
    fun `le code lu est pris sans les espaces autour, comme avec position`() = with(BancSession()) {
        val s = assertIs<OuvertureControle.Ouverte>(ouvrirSans("  P35-02 ")).session
        s.pointe(1.0, 10.0); s.dire("sain")
        assertEquals(BancSession.P2.valeur, controles().single().placetteUuid)
    }

    @Test
    fun `sans fix, le jour civil et visite_le viennent de l'heure du telephone`() = with(BancSession(heure = "2026-11-01T23:30:00Z")) {
        val s = assertIs<OuvertureControle.Ouverte>(ouvrirSans("P35-01")).session
        s.pointe(1.0, 10.0); s.dire("sain")
        val c = controles().single()
        // 23 h 30 UTC le 1er novembre = 0 h 30 le 2 à Paris (heure d'hiver) : jour civil de Paris, pas le jour UTC.
        assertEquals("2026-11-02", c.jourCivil)
        assertEquals(horloge.maintenantMs, c.visiteLeUtcMs)
    }

    // ------------------------------------------------------------------ sans code, code inconnu

    @Test
    fun `sans code lu, rien ne s'ouvre et rien n'est ecrit`() = with(BancSession()) {
        for (code in listOf(null, "", "   ")) {
            val o = ouvrirSans(code)
            assertFalse(o is OuvertureControle.Ouverte, "code « $code » : $o")
            assertFalse(o is OuvertureControle.UgAChoisir, "code « $code » : $o")
        }
        assertEquals(0, journal.lignes.size)
        assertEquals(0, ecoute.saisies)
    }

    @Test
    fun `code lu inconnu de la tournee - refus code_inconnu, rien d'ecrit`() = with(BancSession()) {
        val r = assertIs<OuvertureControle.Refusee>(ouvrirSans("ZZZ")).refus
        assertEquals("code_inconnu", r.code)
        assertEquals(0, journal.lignes.size)
    }

    // ------------------------------------------------------------------ homonymes : sans position, rien ne tranche

    private val origine = BancSession.POSITION_P1
    private val ug12 = Piquet(UuidPlacette("placette-ug12"), "P01", origine, ug = "12")
    private val ug14 = Piquet(UuidPlacette("placette-ug14"), "P01", PointLambert93(origine.xMetres + 10.0, origine.yMetres), ug = "14")
    private fun homonymes(inverse: Boolean) = ContexteControle(
        operateur = "Agent d'essai",
        placettes = (if (inverse) listOf(ug14, ug12) else listOf(ug12, ug14)).map { PlacetteControlable(it, 3.99) },
        epsg = CodeEpsg.RGF93_V1_LAMBERT93,
    )

    @Test
    fun `code present dans deux UG, sans position - l'agent choisit l'UG, jamais la premiere trouvee`() = with(BancSession()) {
        for (inverse in listOf(false, true)) {
            val c = assertIs<OuvertureControle.UgAChoisir>(ouvrirSans("P01", ctx = homonymes(inverse)))
            assertEquals("P01", c.code)
            assertEquals(setOf<String?>("12", "14"), c.candidats.map { it.ug }.toSet())
        }
        assertEquals(0, journal.lignes.size)
    }

    @Test
    fun `l'UG choisie ouvre sa placette, sans position mesuree`() {
        for (ug in listOf("12", "14")) with(BancSession()) {
            val s = assertIs<OuvertureControle.Ouverte>(ouvrirSans("P01", ugChoisie = ug, ctx = homonymes(inverse = false))).session
            s.pointe(1.0, 10.0); s.dire("sain")
            val c = controles().single()
            val attendu = if (ug == "12") ug12 else ug14
            assertEquals(attendu.placette.valeur, c.placetteUuid)
            assertEquals(attendu.position.xMetres, c.positionX)
            assertEquals(attendu.position.yMetres, c.positionY)
            assertTrue(c.sansPositionMesuree)
            assertNull(c.positionMesureeX)
        }
    }

    // ------------------------------------------------------------------ point 10 : un fix reçu ensuite est ignoré

    @Test
    fun `un fix recu apres l'ouverture sans position est ignore - Terminer reprend le controle tel quel`() = with(BancSession()) {
        val s = assertIs<OuvertureControle.Ouverte>(ouvrirSans("P35-01")).session
        s.pointe(1.0, 10.0)
        // Le fix arrive : frais (âge 0), à l'heure du téléphone.
        horloge.fixCourant = FixSession(heureUtcMs = horloge.maintenantMs, monotoneMs = horloge.monotoneMs)
        s.dire("sain")
        s.pointe(2.0, 200.0); s.dire("mort")
        assertIs<SuiteSession.Fait>(s.terminer())
        val toutes = controles()
        assertTrue(toutes.size >= 2, "contrôle écrit puis réécrit à la fin")
        for (c in toutes) {
            assertTrue(c.sansPositionMesuree, "fixé à l'arrivée : $c")
            assertNull(c.positionMesureeX); assertNull(c.positionMesureeY)
            assertTrue(c.rattacheParIdentite)
            assertEquals(BancSession.POSITION_P1.xMetres, c.positionX); assertEquals(BancSession.POSITION_P1.yMetres, c.positionY)
        }
        assertTrue(toutes.last().termine)
        assertEquals(setOf(BancSession.P1.valeur), PlacettesControlees.de(journal.lignesCourantes()))
    }

    // ------------------------------------------------------------------ point 8 : relu après redémarrage

    @Test
    fun `fermer et rouvrir l'application - le controle sans position est relu, rien n'est perdu ni double`() = with(BancSession()) {
        val s = assertIs<OuvertureControle.Ouverte>(ouvrirSans("P35-01")).session
        s.pointe(1.0, 10.0); s.pointe(2.0, 200.0)
        s.suivant(); s.suivant()
        s.dire("sain")
        // Nouvelle instance, même journal : l'application a redémarré, toujours sans fix.
        val rouverte = assertIs<OuvertureControle.Ouverte>(ouvrirSans("P35-01")).session
        assertEquals(2, rouverte.etat().nbPlants)
        assertEquals(1, rouverte.etat().nbDits)
        rouverte.dire("atteint")
        assertIs<SuiteSession.Fait>(rouverte.terminer())
        assertEquals(1, journal.deType(FormatValeurs.TYPE_CONTROLE).map { it.objetUuid }.toSet().size, "un seul contrôle par placette et par jour")
        assertTrue(controles().all { it.sansPositionMesuree })
        assertEquals(setOf(BancSession.P1.valeur), PlacettesControlees.de(journal.lignesCourantes()))
    }

    // ------------------------------------------------------------------ avec position : inchangé, mais en v2

    @Test
    fun `avec une position mesuree, le controle s'ecrit en v2 avec sans_position_mesuree faux`() = with(BancSession()) {
        val s = session()
        s.pointe(1.0, 10.0); s.dire("sain")
        val brut = journal.deType(FormatValeurs.TYPE_CONTROLE).single().valeur!!
        assertTrue(brut.startsWith("{\"v\":2,"), brut)
        val c = controles().single()
        assertFalse(c.sansPositionMesuree)
        assertEquals(BancSession.POSITION_P1.xMetres, c.positionMesureeX)
        assertEquals(BancSession.POSITION_P1.yMetres, c.positionMesureeY)
        assertFalse(c.rattacheParIdentite)
    }
}
