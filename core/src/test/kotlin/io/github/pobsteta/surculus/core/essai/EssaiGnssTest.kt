/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.core.essai

import io.github.pobsteta.surculus.core.gnss.EvaluationFix
import io.github.pobsteta.surculus.core.gnss.FixGnss
import io.github.pobsteta.surculus.core.gnss.OrigineFix
import io.github.pobsteta.surculus.core.gnss.PointMontage
import io.github.pobsteta.surculus.core.gnss.PolitiqueFix
import io.github.pobsteta.surculus.core.gnss.Position
import io.github.pobsteta.surculus.core.gnss.QualiteFix
import io.github.pobsteta.surculus.core.gnss.RaisonRefus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Terrain de la mission 018 : RTK fixe, corrections coupées, récepteur éteint, position simulée, Couchey. */
class EssaiGnssTest {
    private val maintenant = 1_000_000L
    private val politique = PolitiqueFix()

    // Place de dépôt PD-01 de Couchey (WGS 84) : x = 846 164,76 m, y = 6 686 123,26 m en Lambert-93.
    private val couchey = Position(latitude = 47.259315, longitude = 4.933034)

    private fun fix(
        qualite: QualiteFix = QualiteFix.RTK_FIXE,
        position: Position = couchey,
        ageMs: Long = 1_000,
        corrections: Double? = 2.0,
        precision: Double? = 0.02,
        simule: Boolean = false,
        origine: OrigineFix = OrigineFix.RECEPTEUR,
    ) = FixGnss(
        position = position, qualite = qualite, nbSatellites = 20, hdop = 0.6, altitudeM = 240.0,
        ageCorrectionsS = corrections, precisionHorizontaleM = precision, horodatageMs = maintenant - ageMs,
        origine = origine, simule = simule,
    )

    private fun evalue(f: FixGnss, apres: Long = 0): Pair<FixGnss?, RaisonRefus?> =
        when (val v = politique.evaluer(f, maintenant + apres)) {
            is EvaluationFix.Accepte -> v.fix to null
            is EvaluationFix.Refuse -> null to v.raison
        }

    @Test
    fun `RTK fixe centimetrique sans drapeau`() {
        val a = EssaiGnss.afficher(fix(), null, maintenant)
        assertEquals("RTK fixe", a.qualite)
        assertEquals(Ton.BON, a.ton)
        assertEquals("± 0,02 m", a.precision)
        assertEquals("Récepteur · fix il y a 1 s", a.ageFix)
        assertEquals("Corrections : 2 s", a.ageCorrections)
        assertTrue(a.drapeaux.isEmpty())
    }

    @Test
    fun `corrections de 31 s, retrograde en autonome et drapeau`() {
        val (retenu, refus) = evalue(fix(corrections = 31.0))
        val a = EssaiGnss.afficher(retenu, refus, maintenant)
        assertEquals("Autonome", a.qualite)
        assertEquals(Ton.A_SURVEILLER, a.ton)
        // Décision de Pascal : pas de précision centimétrique sous « Autonome » rétrogradé.
        assertEquals("—", a.precision)
        assertEquals(listOf<Drapeau>(Drapeau.Retrograde(QualiteFix.RTK_FIXE)), a.drapeaux)
        assertTrue(a.drapeaux.single().texte.contains("RTK fixe"))
    }

    @Test
    fun `recepteur eteint, apres 6 s le fix est refuse et l ecran le dit`() {
        val (retenu, refus) = evalue(fix(), apres = 6_000)
        val a = EssaiGnss.afficher(retenu, refus, maintenant + 6_000)
        assertEquals("Fix refusé", a.qualite)
        assertEquals(Ton.MAUVAIS, a.ton)
        assertEquals(listOf<Drapeau>(Drapeau.Refuse(RaisonRefus.TROP_VIEUX)), a.drapeaux)
        assertNull(a.lambertX)
        assertNull(a.lambertY)
    }

    @Test
    fun `sans fix ni refus, sans position`() {
        val a = EssaiGnss.afficher(null, null, maintenant)
        assertEquals("Sans position", a.qualite)
        assertTrue(a.drapeaux.isEmpty())
    }

    @Test
    fun `position simulee, drapeau et teinte mauvaise meme en RTK fixe`() {
        val a = EssaiGnss.afficher(fix(simule = true), null, maintenant)
        assertEquals(listOf<Drapeau>(Drapeau.Simule), a.drapeaux)
        assertEquals(Ton.MAUVAIS, a.ton)
    }

    @Test
    fun `Couchey est affiche en Lambert-93 et tombe en France`() {
        val a = EssaiGnss.afficher(fix(), null, maintenant)
        // Espaces insécables ou fines selon la version de l'ICU : on les normalise.
        val espaces = Regex("[\\u00A0\\u202F]")
        assertEquals("X 846 164,76 m", a.lambertX!!.replace(espaces, " "))
        assertEquals("Y 6 686 123,26 m", a.lambertY!!.replace(espaces, " "))
        assertEquals(true, a.enFrance)
    }

    @Test
    fun `latitude et longitude inversees tombent hors de France`() {
        val inverse = Position(latitude = couchey.longitude, longitude = couchey.latitude)
        val a = EssaiGnss.afficher(fix(position = inverse), null, maintenant)
        assertEquals(false, a.enFrance)
    }

    @Test
    fun `points de montage tries par distance puis sans coordonnees par nom`() {
        val loin = PointMontage("LOIN", "x", "RTCM 3.2", 43.3, 5.4)
        val proche = PointMontage("PROCHE", "x", "RTCM 3.2", 47.3, 4.9)
        val sans = PointMontage("ANONYME", "x", "RTCM 3.2")
        assertEquals(listOf("PROCHE", "LOIN", "ANONYME"), EssaiGnss.trierPoints(listOf(sans, loin, proche), couchey).map { it.nom })
    }

    @Test
    fun `points de montage tries par nom sans position`() {
        val l = listOf(PointMontage("b", "x", "f"), PointMontage("A", "x", "f"), PointMontage("c", "x", "f", 47.0, 5.0))
        assertEquals(listOf("A", "b", "c"), EssaiGnss.trierPoints(l, null).map { it.nom })
    }

    @Test
    fun `le telephone est nomme et sa precision en metres`() {
        val a = EssaiGnss.afficher(
            fix(QualiteFix.AUTONOME, precision = 4.3, corrections = null, origine = OrigineFix.TELEPHONE), null, maintenant,
        )
        assertEquals("± 4,3 m", a.precision)
        assertEquals("Téléphone · fix il y a 1 s", a.ageFix)
        assertEquals("Corrections : aucune", a.ageCorrections)
    }
}
