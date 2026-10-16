package io.github.pobsteta.surculus.core.gnss

import io.github.pobsteta.surculus.core.geo.EmpriseLambert93
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.geo.Projections
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** ADR 0003 : fix trop vieux refusé, corrections trop vieilles rétrogradées, position simulée détectée. */
class PolitiqueFixTest {
    private val politique = PolitiqueFix(ageMaxFixMs = 5_000, ageMaxCorrectionsS = 30.0)
    private val maintenant = 1_000_000L

    // Couchey, forêt communale (lat 47.259315, lon 4.933034), ordre explicite.
    private fun fix(
        qualite: QualiteFix = QualiteFix.RTK_FIXE,
        recuA: Long = maintenant,
        ageCorrections: Double? = 1.0,
        simule: Boolean = false,
    ) = FixGnss(
        position = Position(latitude = 47.259315, longitude = 4.933034),
        qualite = qualite, nbSatellites = 12, hdop = 0.6, altitudeM = 300.0,
        ageCorrectionsS = ageCorrections, precisionHorizontaleM = 0.02,
        horodatageMs = recuA, simule = simule,
    )

    @Test
    fun `un fix frais est accepte tel quel`() {
        val r = politique.evaluer(fix(), maintenant) as EvaluationFix.Accepte
        assertEquals(QualiteFix.RTK_FIXE, r.fix.qualite)
        assertFalse(r.retrograde)
    }

    @Test
    fun `un fix plus vieux que le seuil est refuse, au seuil il passe`() {
        assertEquals(EvaluationFix.Refuse(RaisonRefus.TROP_VIEUX), politique.evaluer(fix(recuA = maintenant - 5_001), maintenant))
        assertTrue(politique.evaluer(fix(recuA = maintenant - 5_000), maintenant) is EvaluationFix.Accepte)
    }

    @Test
    fun `un fix date du futur au-dela de la tolerance signale une horloge incoherente`() {
        assertEquals(EvaluationFix.Refuse(RaisonRefus.HORLOGE_INCOHERENTE), politique.evaluer(fix(recuA = maintenant + 1_001), maintenant))
        assertTrue(politique.evaluer(fix(recuA = maintenant + 1_000), maintenant) is EvaluationFix.Accepte)
    }

    @Test
    fun `sans position le fix est refuse`() {
        assertEquals(EvaluationFix.Refuse(RaisonRefus.SANS_POSITION), politique.evaluer(fix(qualite = QualiteFix.AUCUN), maintenant))
    }

    @Test
    fun `un RTK fixe aux corrections trop vieilles est retrograde en autonome`() {
        val r = politique.evaluer(fix(ageCorrections = 30.1), maintenant) as EvaluationFix.Accepte
        assertEquals(QualiteFix.AUTONOME, r.fix.qualite)
        assertEquals(QualiteFix.RTK_FIXE, r.qualiteAnnoncee)
        assertTrue(r.retrograde)
        assertFalse(r.fix.qualite.estRtk)
    }

    @Test
    fun `un RTK flottant aux corrections trop vieilles est aussi retrograde`() {
        val r = politique.evaluer(fix(qualite = QualiteFix.RTK_FLOTTANT, ageCorrections = 120.0), maintenant) as EvaluationFix.Accepte
        assertEquals(QualiteFix.AUTONOME, r.fix.qualite)
        assertEquals(QualiteFix.RTK_FLOTTANT, r.qualiteAnnoncee)
    }

    @Test
    fun `au seuil des corrections le RTK est conserve`() {
        val r = politique.evaluer(fix(ageCorrections = 30.0), maintenant) as EvaluationFix.Accepte
        assertEquals(QualiteFix.RTK_FIXE, r.fix.qualite)
    }

    @Test
    fun `un age de corrections inconnu ne retrograde pas, un fix autonome non plus`() {
        assertEquals(QualiteFix.RTK_FIXE, (politique.evaluer(fix(ageCorrections = null), maintenant) as EvaluationFix.Accepte).fix.qualite)
        val auto = politique.evaluer(fix(qualite = QualiteFix.AUTONOME, ageCorrections = 999.0), maintenant) as EvaluationFix.Accepte
        assertFalse(auto.retrograde)
    }

    @Test
    fun `une position simulee est conservee avec son drapeau`() {
        val r = politique.evaluer(fix(simule = true), maintenant) as EvaluationFix.Accepte
        assertTrue(r.fix.simule)
        assertFalse(fix().simule)
    }

    @Test
    fun `des seuils invalides sont refuses`() {
        assertThrows<IllegalArgumentException> { PolitiqueFix(ageMaxFixMs = 0) }
        assertThrows<IllegalArgumentException> { PolitiqueFix(ageMaxCorrectionsS = -1.0) }
    }

    @Test
    fun `un fix decode du NMEA de Couchey tombe en France (ordre lat lon)`() {
        // 47°15.5589' N = 47,259315° ; 004°55.9820' E = 4,933033° (Couchey, sommieR R/demo.R).
        val corps = "GNGGA,141322.00,4715.5589,N,00455.9820,E,4,12,0.65,300.0,M,47.5,M,1.0,0000"
        val p = Nmea.gga("\$$corps*${Nmea.sommeControle(corps)}")!!.position
        assertEquals(47.259315, p.latitude, 1e-5)
        assertEquals(4.933034, p.longitude, 1e-5)
        val (x, y) = Projections.versLambert93(lon = p.longitude, lat = p.latitude)
        assertTrue(EmpriseLambert93.FRANCE_METROPOLITAINE.contient(PointLambert93(xMetres = x, yMetres = y)))
    }
}
