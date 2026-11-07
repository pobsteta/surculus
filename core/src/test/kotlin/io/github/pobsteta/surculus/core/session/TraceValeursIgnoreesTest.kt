package io.github.pobsteta.surculus.core.session

import io.github.pobsteta.surculus.core.placette.CodesRefus
import io.github.pobsteta.surculus.core.placette.RefusPlacette
import io.github.pobsteta.surculus.core.placette.Verdict
import io.github.pobsteta.surculus.core.placette.Rattachement
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Mission 087, tests du `dev` : la trace des valeurs ignorées (`docs/formats/journal-valeurs-v2.md`, « Lecture » : chaque
 * valeur ignorée tracée au journal technique) ne part qu'une fois par ligne, alors que la session relit le journal à chaque
 * commande ; et le refus `code_requis` (décision de Pascal du 2026-11-03).
 *
 * Coordonnées : Lambert-93 (EPSG:2154), x = Est, y = Nord, mètres ; P35-01 à Couchey (Côte-d'Or).
 */
class TraceValeursIgnoreesTest {
    @Test
    fun `une valeur v4 du journal est tracee une seule fois par session, meme relue a chaque commande`() = with(BancSession()) {
        // Mission 091 : la v3 est désormais écrite et lue ; la version inconnue est la v4.
        val v4 = "{\"v\":4,\"controle_uuid\":\"c\",\"placette_uuid\":\"placette-1\"}"
        journal.saisir("plant-futur", FormatValeurs.TYPE_POINTAGE, v4, "Agent d'essai", null, null)
        val traces = ArrayList<String>()
        val s = assertIs<OuvertureControle.Ouverte>(
            SessionControle.ouvrir(contexte, journal, horloge, essences, Arrivee.Lambert93(BancSession.POSITION_P1), null, ecoute, diagnostic = { traces += it }),
        ).session
        s.pointe(1.0, 10.0); s.dire("sain"); s.etat(); s.etat()
        assertEquals(1, traces.size, traces.toString())
        assertTrue("version 4" in traces.single(), traces.single())
        assertFalse("Agent" in traces.single(), "le contenu de la valeur n'est pas recopié dans la trace")
        assertEquals(1, s.etat().nbPlants, "la valeur v4 est ignorée, jamais devinée")
    }

    @Test
    fun `sans position ni code - refus code_requis, en francais, sans texte technique`() {
        val v = Rattachement.rattacher(BancSession().contexte.placettes.map { it.piquet }, null, null)
        val r = assertIs<Verdict.Refuse>(v).refus
        assertEquals(RefusPlacette.CodeRequis, r)
        assertEquals("code_requis", CodesRefus.code(RefusPlacette.CodeRequis))
        assertTrue("« Code lu sur le piquet »" in r.message && "« Ouvrir sans position »" in r.message, r.message)
        assertFalse("_" in r.message, r.message)
    }

    @Test
    fun `sans position, code lu - aucune distance, ni 0 ni NaN`() {
        val r = assertIs<Verdict.Accepte<*>>(Rattachement.rattacher(BancSession().contexte.placettes.map { it.piquet }, null, "P35-01")).valeur
            as io.github.pobsteta.surculus.core.placette.Rattache
        assertEquals(null, r.distanceM)
        assertTrue(r.sansPositionMesuree && r.parIdentite)
        assertEquals(BancSession.POSITION_P1, r.positionControle)
    }
}
