/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.core.session

import io.github.pobsteta.surculus.core.gnss.FixGnss
import io.github.pobsteta.surculus.core.gnss.OrigineFix
import io.github.pobsteta.surculus.core.gnss.Position
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import io.github.pobsteta.surculus.core.gnss.QualiteFix as QualiteGga

/**
 * Mission 091, décision de Pascal du 2026-11-04, point 4 : la qualité du fix d'un contrôle, tirée des fix **moyennés** à
 * l'arrivée (spec § 5, `positions_controles` ; ADR 0003, ligne « Fix »).
 *
 * - `source_gnss` ∈ `telephone`, `recepteur_autonome`, `recepteur_dgps`, `recepteur_rtk_flottant`, `recepteur_rtk_fixe`
 *   (indicateur de qualité GGA, NMEA 0183) ; la **plus mauvaise** des fix moyennés, **après rétrogradation** ;
 * - `precision_m` = la plus mauvaise précision des fix moyennés ; **NULL si un fix n'en donne pas, jamais 0** ;
 * - `simulee` vrai si un fix est `isMock` (`FixGnss.simule`) ou de qualité GGA 8 (simulation).
 *
 * Écrit par le `testeur` sans lire le code de la mission. Lu pour l'API seulement : les signatures publiques et la KDoc
 * de `core/gnss/Gnss.kt` (`FixGnss`, `OrigineFix`, `QualiteFix` GGA, `PolitiqueFix` et sa rétrogradation d'un RTK aux
 * corrections trop vieilles en autonome, la qualité annoncée gardée dans `qualiteAnnoncee`).
 *
 * **Rouge (ne compile pas) jusqu'au `dev`.** API supposée, à confirmer ou remplacer par le `dev` (proposée dans le
 * rapport du `testeur`) :
 * ```kotlin
 * // core.session
 * data class QualiteFix(val precisionM: Double?, val sourceGnss: String, val simulee: Boolean) {
 *     companion object {
 *         /** Les cinq valeurs de source_gnss (l'ordre n'est pas vérifié). */
 *         val SOURCES_GNSS: List<String>
 *         /** La qualité des fix moyennés (déjà passés par PolitiqueFix) ; null pour une liste vide. */
 *         fun deFix(fix: List<io.github.pobsteta.surculus.core.gnss.FixGnss>): QualiteFix?
 *     }
 * }
 * ```
 * **Non supposé** (laissé au `dev` et à `journal-valeurs-v3.md`) : la source de PPS (3), estimé (6), manuel (7) et
 * simulation (8), sinon qu'elle reste dans le domaine ; le rang du téléphone face à un récepteur dans un même moyennage.
 *
 * Coordonnées des fix : WGS 84, degrés (latitude, longitude), à Couchey (Côte-d'Or).
 */
class QualiteDuFixTest {
    private val domaine = listOf("telephone", "recepteur_autonome", "recepteur_dgps", "recepteur_rtk_flottant", "recepteur_rtk_fixe")

    /** Couchey : 47,26° N, 4,98° E (latitude d'abord). */
    private val couchey = Position(latitude = 47.26, longitude = 4.98)

    private fun fix(
        qualite: QualiteGga,
        precisionM: Double?,
        origine: OrigineFix = OrigineFix.RECEPTEUR,
        simule: Boolean = false,
        annoncee: QualiteGga? = null,
    ) = FixGnss(
        position = couchey, qualite = qualite, nbSatellites = 12, hdop = 0.8, altitudeM = 250.0,
        ageCorrectionsS = if (qualite.estRtk) 1.0 else null, precisionHorizontaleM = precisionM,
        horodatageMs = 1_793_706_400_000, origine = origine, qualiteAnnoncee = annoncee, simule = simule,
    )

    private fun qualite(vararg f: FixGnss): QualiteFix = assertNotNull(QualiteFix.deFix(f.toList()), "aucune qualité pour ${f.toList()}")

    // ------------------------------------------------------------------ domaine de source_gnss

    @Test
    fun `le domaine de source_gnss est celui decide, cinq valeurs, ni plus ni moins`() {
        assertEquals(domaine.toSet(), QualiteFix.SOURCES_GNSS.toSet())
        assertEquals(5, QualiteFix.SOURCES_GNSS.size, "sans doublon")
    }

    @Test
    fun `toute source produite appartient au domaine, quelle que soit la qualite GGA et l'origine`() {
        for (o in OrigineFix.entries) for (q in QualiteGga.entries.filter { it != QualiteGga.AUCUN }) {
            val s = qualite(fix(q, 1.0, origine = o)).sourceGnss
            assertTrue(s in domaine, "$o, GGA ${q.codeNmea} : « $s » hors du domaine")
        }
    }

    @Test
    fun `recepteur - autonome, DGPS, RTK flottant et RTK fixe d'apres l'indicateur GGA`() {
        assertEquals("recepteur_autonome", qualite(fix(QualiteGga.AUTONOME, 2.5)).sourceGnss)
        assertEquals("recepteur_dgps", qualite(fix(QualiteGga.DGPS, 0.9)).sourceGnss)
        assertEquals("recepteur_rtk_flottant", qualite(fix(QualiteGga.RTK_FLOTTANT, 0.2)).sourceGnss)
        assertEquals("recepteur_rtk_fixe", qualite(fix(QualiteGga.RTK_FIXE, 0.02)).sourceGnss)
    }

    @Test
    fun `le GNSS du telephone donne telephone, meme quand sa precision ressemble a du DGPS`() {
        // Le téléphone n'est jamais RTK (OrigineFix) ; sa qualité GGA est approchée de sa précision (depuisPrecision).
        val f = fix(QualiteGga.depuisPrecision(1.2), 1.2, origine = OrigineFix.TELEPHONE)
        assertEquals("telephone", qualite(f).sourceGnss)
    }

    @Test
    fun `la source retenue est la plus mauvaise des fix moyennes`() {
        val fixe = fix(QualiteGga.RTK_FIXE, 0.02)
        assertEquals("recepteur_rtk_flottant", qualite(fixe, fix(QualiteGga.RTK_FLOTTANT, 0.2), fixe).sourceGnss)
        assertEquals("recepteur_dgps", qualite(fixe, fix(QualiteGga.DGPS, 0.9)).sourceGnss)
        assertEquals("recepteur_autonome", qualite(fix(QualiteGga.DGPS, 0.9), fix(QualiteGga.AUTONOME, 2.5), fixe).sourceGnss)
        assertEquals("recepteur_rtk_fixe", qualite(fixe, fixe, fixe).sourceGnss)
    }

    @Test
    fun `apres retrogradation - un RTK fixe annonce mais retrograde compte pour autonome`() {
        // PolitiqueFix rétrograde un RTK aux corrections trop vieilles : qualite = AUTONOME, qualiteAnnoncee = RTK_FIXE.
        val retrograde = fix(QualiteGga.AUTONOME, 0.02, annoncee = QualiteGga.RTK_FIXE)
        assertEquals("recepteur_autonome", qualite(retrograde).sourceGnss)
        assertEquals("recepteur_autonome", qualite(fix(QualiteGga.RTK_FIXE, 0.02), retrograde).sourceGnss)
    }

    // ------------------------------------------------------------------ precision_m

    @Test
    fun `la precision retenue est la plus mauvaise des fix moyennes`() {
        val q = qualite(fix(QualiteGga.DGPS, 0.4), fix(QualiteGga.DGPS, 1.2), fix(QualiteGga.DGPS, 0.8))
        assertEquals(1.2, assertNotNull(q.precisionM), 0.0)
        assertEquals(0.02, assertNotNull(qualite(fix(QualiteGga.RTK_FIXE, 0.02)).precisionM), 0.0)
    }

    @Test
    fun `un fix sans precision - precision_m NULL, jamais 0 ni la precision des autres`() {
        val q = qualite(fix(QualiteGga.DGPS, 0.4), fix(QualiteGga.DGPS, null), fix(QualiteGga.DGPS, 0.8))
        assertNull(q.precisionM, "un fix ne dit pas sa précision : la plus mauvaise est inconnue")
        assertEquals("recepteur_dgps", q.sourceGnss, "la source reste dite")
        assertNull(qualite(fix(QualiteGga.AUTONOME, null)).precisionM)
    }

    @Test
    fun `une precision de 0 n'est jamais ecrite`() {
        // Un récepteur qui écrit 0 ne mesure pas une position parfaite : 0 ne sort jamais (décision 4, « jamais 0 »).
        assertNotEquals(0.0, qualite(fix(QualiteGga.RTK_FIXE, 0.0)).precisionM)
        assertNotEquals(0.0, qualite(fix(QualiteGga.RTK_FIXE, 0.0), fix(QualiteGga.RTK_FIXE, 0.0)).precisionM)
    }

    // ------------------------------------------------------------------ simulee

    @Test
    fun `simulee faux quand aucun fix n'est simule`() {
        assertEquals(false, qualite(fix(QualiteGga.RTK_FIXE, 0.02), fix(QualiteGga.DGPS, 0.9)).simulee)
        assertEquals(false, qualite(fix(QualiteGga.depuisPrecision(4.0), 4.0, origine = OrigineFix.TELEPHONE)).simulee)
    }

    @Test
    fun `simulee vrai si un seul fix est isMock`() {
        val vrai = fix(QualiteGga.depuisPrecision(3.8), 3.8, origine = OrigineFix.TELEPHONE)
        val mock = vrai.copy(simule = true)
        assertTrue(qualite(mock).simulee)
        assertTrue(qualite(vrai, mock, vrai).simulee, "un seul fix simulé suffit")
    }

    @Test
    fun `simulee vrai si un fix est de qualite GGA 8, simulation`() {
        assertEquals(8, QualiteGga.SIMULATION.codeNmea)
        assertTrue(qualite(fix(QualiteGga.SIMULATION, 1.0)).simulee)
        assertTrue(qualite(fix(QualiteGga.RTK_FIXE, 0.02), fix(QualiteGga.SIMULATION, 1.0)).simulee)
    }

    // ------------------------------------------------------------------ sans fix

    @Test
    fun `aucun fix moyenne - aucune qualite, rien d'invente`() {
        assertNull(QualiteFix.deFix(emptyList()))
    }
}
