package io.github.pobsteta.surculus.core.session

import io.github.pobsteta.surculus.core.geo.EmpriseLambert93
import io.github.pobsteta.surculus.core.reperes.PointGeographique
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.geo.UniteAzimut
import io.github.pobsteta.surculus.core.reperes.RefusRepere
import io.github.pobsteta.surculus.core.reperes.UnitePente
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Mission 081, reprise 1/3 (décision de Pascal du 2026-10-30) : les refus à l'ouverture du contrôle et
 * `PiquetHorsFrance` disent leurs coordonnées par les `Libelles`, ordre et unités explicites, sans décimal à point.
 */
class RefusOuvertureLisiblesTest {
    private val decimalRe = Regex("\\d\\.\\d")
    private val fin = " "

    private fun propre(texte: String) {
        assertFalse(decimalRe.containsMatchIn(texte), "« $texte » contient un décimal à point")
        assertFalse('_' in texte, "« $texte » contient un « _ »")
        assertFalse("NaN" in texte || "Infinity" in texte, "« $texte » montre NaN ou Infinity")
    }

    @Test fun `la placette en Somalie est refusee avec latitude puis longitude en degres, sans decimal a point`() = with(BancSession()) {
        val somalie = PointGeographique.de(latitudeDeg = 5.15, longitudeDeg = 46.2).valeurOuEchec()
        val r = assertIs<OuvertureControle.Refusee>(SessionControle.ouvrir(contexte, journal, horloge, essences, Arrivee.Gnss(somalie)))
        propre(r.refus.message)
        assertTrue("latitude 5,2" in r.refus.message && "longitude 46,2" in r.refus.message, r.refus.message)
        assertTrue("(degrés)" in r.refus.message, r.refus.message)
        assertTrue(r.refus.message.indexOf("latitude") < r.refus.message.indexOf("longitude"), "ordre latitude, longitude")
    }

    @Test fun `un point Lambert-93 hors de France dit x puis y en metres entiers`() = with(BancSession()) {
        val r = assertIs<OuvertureControle.Refusee>(ouvrir(PointLambert93(6_686_123.26, 846_164.76)))
        propre(r.refus.message)
        assertTrue("x = 6${fin}686${fin}123 m, y = 846${fin}165 m (Lambert-93)" in r.refus.message, r.refus.message)
    }

    @Test fun `un point hors de l'emprise de la foret dit x puis y en metres entiers`() = with(BancSession()) {
        val ctx = contexte.copy(empriseForet = EmpriseLambert93(0.0, 0.0, 100_000.0, 100_000.0))
        val r = assertIs<OuvertureControle.Refusee>(
            SessionControle.ouvrir(ctx, journal, horloge, essences, Arrivee.Lambert93(BancSession.POSITION_P1)),
        )
        propre(r.refus.message)
        assertEquals("point_hors_foret", r.refus.code)
        assertTrue("(x = " in r.refus.message && " m, y = " in r.refus.message && "Lambert-93" in r.refus.message, r.refus.message)
    }

    @Test fun `un piquet hors de France dit x puis y en metres entiers`() {
        val m = RefusRepere.PiquetHorsFrance(6_686_123.26, 846_164.76).message
        propre(m)
        assertTrue("x = 6${fin}686${fin}123 m, y = 846${fin}165 m" in m, m)
    }

    @Test fun `un nombre infini s'accorde avec le nom`() {
        assertTrue("Azimut infini " in RefusRepere.AzimutHorsPlage(Double.POSITIVE_INFINITY, UniteAzimut.DEGRES).message)
        assertTrue("Pente infinie " in RefusRepere.PenteHorsPlage(Double.POSITIVE_INFINITY, UnitePente.DEGRES).message)
        assertTrue("Distance infinie " in RefusRepere.DistanceInvalide(Double.POSITIVE_INFINITY).message)
    }
}
