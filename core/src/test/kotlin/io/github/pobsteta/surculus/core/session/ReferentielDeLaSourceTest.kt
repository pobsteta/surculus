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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import io.github.pobsteta.surculus.core.gnss.QualiteFix as QualiteGga

/**
 * Mission 094 : le référentiel de la source de chaque position mesurée (spec § 3, l. 88 : « RGF93 (ETRS89) si RTK
 * corrigé par Centipède, dont les bases diffusent des coordonnées RGF93 ; WGS 84 (EPSG:4326) si autonome » ; spec § 5,
 * `positions_controles.referentiel_source` ; ADR 0003, l. 94 : « enregistré tel quel »).
 *
 * Décisions de Pascal du 2026-11-06 (elles priment sur la fiche) :
 * 1. téléphone → `WGS84` ; récepteur en qualité GGA autonome (1, et 3, 6, 7, 8 comme dans la 091) → `WGS84` ; DGPS ou RTK
 *    (flottant, fixe) avec un flux NTRIP **Centipède** actif vu par Surculus (dernier RTCM d'au plus 30 s,
 *    `PolitiqueFix.ageMaxCorrectionsS`) → `RGF93` ; DGPS ou RTK **sans** flux vu (NTRIP désactivé, corrections de plus de
 *    30 s, caster qui n'est pas Centipède) → **NULL**, jamais une valeur devinée. DGPS par EGNOS sans NTRIP → NULL.
 *    **Amendée par Pascal le 2026-11-06 (relecture de la PR #159)** : un fix **DGPS** donne NULL, même avec un flux
 *    Centipède vu (le récepteur peut se corriger par EGNOS : on ne sait pas d'où vient la correction) ; `RGF93`
 *    seulement pour le RTK flottant ou fixe avec un flux Centipède vu (au plus 30 s). Le caster est reconnu par son nom
 *    seul (`caster.centipede.fr`, `crtk.net`), jamais par son adresse IP (66.185.123.66 → NULL).
 * 2. Position moyennée : un seul fix NULL → NULL ; sinon un seul `WGS84` → `WGS84` ; sinon `RGF93`.
 *
 * Écrit par le `testeur` sans lire le code de la mission. API proposée par le `dev` (le paquet de `ReferentielsSource`
 * est supposé `core.session`, à côté de `QualiteFix` ; le `dev` corrige l'import s'il le met ailleurs) :
 * ```kotlin
 * object ReferentielsSource { const val RGF93 = "RGF93"; const val WGS84 = "WGS84"; val DOMAINE: Set<String>
 *                             fun de(fix: FixGnss): String? }
 * FixGnss(..., casterNtrip: String? = null)   // l'hôte du caster dont le flux était vu ; null = pas de flux vu
 * QualiteFix(precisionM, sourceGnss, simulee, referentielSource: String? = null) ; QualiteFix.deFix(fix).referentielSource
 * ```
 * Hôtes Centipède reconnus : `caster.centipede.fr` et `crtk.net`.
 *
 * **Supposé par le `testeur`** (dit dans son rendu) : `de` lit aussi `ageCorrectionsS` du fix : un DGPS ou RTK dont les
 * corrections ont plus de 30 s rend NULL même si un caster Centipède est nommé ; au seuil (30 s) il rend `RGF93`, comme
 * `PolitiqueFix` garde un RTK au seuil. Un RTK **rétrogradé** par `PolitiqueFix` (qualité autonome, `qualiteAnnoncee`
 * RTK) rend NULL : ce sont les « corrections de plus de 30 s » de la décision 1, la position n'est pas autonome.
 *
 * Coordonnées des fix : WGS 84, degrés (latitude, longitude), à Couchey (Côte-d'Or).
 */
class ReferentielDeLaSourceTest {
    /** Couchey : 47,26° N, 4,98° E (latitude d'abord). */
    private val couchey = Position(latitude = 47.26, longitude = 4.98)

    private val centipede = "caster.centipede.fr"

    private fun fix(
        qualite: QualiteGga,
        caster: String? = null,
        ageCorrectionsS: Double? = if (qualite.estRtk || qualite == QualiteGga.DGPS) 1.0 else null,
        origine: OrigineFix = OrigineFix.RECEPTEUR,
        precisionM: Double? = 0.5,
        annoncee: QualiteGga? = null,
    ) = FixGnss(
        position = couchey, qualite = qualite, nbSatellites = 12, hdop = 0.8, altitudeM = 250.0,
        ageCorrectionsS = ageCorrectionsS, precisionHorizontaleM = precisionM,
        horodatageMs = 1_793_706_400_000, origine = origine, qualiteAnnoncee = annoncee, casterNtrip = caster,
    )

    private val corriges = listOf(QualiteGga.DGPS, QualiteGga.RTK_FLOTTANT, QualiteGga.RTK_FIXE)

    /** Les seules qualités qui peuvent donner RGF93 (décision amendée du 2026-11-06). */
    private val rtk = listOf(QualiteGga.RTK_FLOTTANT, QualiteGga.RTK_FIXE)

    /** Qualités GGA « autonomes » de la décision 1 : 1, et 3 (PPS), 6 (estimé), 7 (manuel), 8 (simulation). */
    private val autonomes: List<QualiteGga> by lazy {
        val l = QualiteGga.entries.filter { it.codeNmea in setOf(1, 3, 6, 7, 8) }
        assertEquals(setOf(1, 3, 6, 7, 8), l.map { it.codeNmea }.toSet(), "banc : les qualités GGA 1, 3, 6, 7, 8 existent")
        l
    }

    // ------------------------------------------------------------------ le domaine

    @Test
    fun `le domaine est RGF93 et WGS84, ecrits ainsi, ni plus ni moins`() {
        assertEquals("RGF93", ReferentielsSource.RGF93)
        assertEquals("WGS84", ReferentielsSource.WGS84)
        assertEquals(setOf("RGF93", "WGS84"), ReferentielsSource.DOMAINE.toSet())
        assertEquals(2, ReferentielsSource.DOMAINE.size, "sans doublon")
    }

    @Test
    fun `tout referentiel produit appartient au domaine ou vaut NULL, quels que soient l'origine, la qualite et le caster`() {
        val casters = listOf(null, centipede, "crtk.net", "caster.rtk2go.com", "")
        val ages = listOf(null, 0.5, 30.0, 30.1, 600.0)
        for (o in OrigineFix.entries) for (q in QualiteGga.entries.filter { it != QualiteGga.AUCUN }) for (c in casters) for (a in ages) {
            val r = ReferentielsSource.de(fix(q, caster = c, ageCorrectionsS = a, origine = o))
            assertTrue(r == null || r in ReferentielsSource.DOMAINE, "$o, GGA ${q.codeNmea}, caster $c, âge $a : « $r » hors du domaine")
        }
    }

    // ------------------------------------------------------------------ décision 1 : WGS84

    @Test
    fun `une position mesuree au telephone est en WGS84, meme si sa precision ressemble a du DGPS`() {
        // La Colombière, 2026-11-06 : précision 3,79 m, source telephone ; referentiel_source était NULL.
        assertEquals("WGS84", ReferentielsSource.de(fix(QualiteGga.depuisPrecision(3.79), origine = OrigineFix.TELEPHONE, precisionM = 3.79)))
        assertEquals("WGS84", ReferentielsSource.de(fix(QualiteGga.depuisPrecision(1.2), origine = OrigineFix.TELEPHONE, precisionM = 1.2)))
    }

    @Test
    fun `le telephone reste WGS84 meme si un flux Centipede est vu - il ne recoit pas les corrections`() {
        for (q in corriges + QualiteGga.AUTONOME) {
            assertEquals("WGS84", ReferentielsSource.de(fix(q, caster = centipede, origine = OrigineFix.TELEPHONE)), "téléphone, GGA ${q.codeNmea}")
        }
    }

    @Test
    fun `un recepteur en qualite GGA autonome (1, 3, 6, 7, 8) est en WGS84, avec ou sans flux NTRIP`() {
        for (q in autonomes) for (c in listOf(null, centipede)) {
            assertEquals("WGS84", ReferentielsSource.de(fix(q, caster = c)), "récepteur, GGA ${q.codeNmea}, caster $c")
        }
    }

    // ------------------------------------------------------------------ décision 1 : RGF93

    @Test
    fun `RTK flottant et RTK fixe corriges par un flux Centipede actif sont en RGF93`() {
        for (q in rtk) for (h in listOf("caster.centipede.fr", "crtk.net")) {
            assertEquals("RGF93", ReferentielsSource.de(fix(q, caster = h, ageCorrectionsS = 1.0)), "GGA ${q.codeNmea}, caster $h")
        }
    }

    @Test
    fun `au seuil de 30 s des corrections, le fix reste RGF93 (comme PolitiqueFix garde le RTK au seuil)`() {
        for (q in rtk) assertEquals("RGF93", ReferentielsSource.de(fix(q, caster = centipede, ageCorrectionsS = 30.0)), "GGA ${q.codeNmea}")
    }

    @Test
    fun `un DGPS reste NULL meme avec un flux Centipede vu - la correction peut venir d'EGNOS (decision du 2026-11-06)`() {
        for (h in listOf("caster.centipede.fr", "crtk.net")) for (a in listOf(null, 1.0, 30.0)) {
            assertNull(ReferentielsSource.de(fix(QualiteGga.DGPS, caster = h, ageCorrectionsS = a)), "DGPS, caster $h, corrections de $a s")
        }
    }

    @Test
    fun `un caster designe par son adresse IP (66_185_123_66) - NULL, reconnu par son nom seul`() {
        for (q in rtk) {
            assertNull(ReferentielsSource.de(fix(q, caster = "66.185.123.66")), "GGA ${q.codeNmea}, caster désigné par son IP")
            assertEquals("RGF93", ReferentielsSource.de(fix(q, caster = "crtk.net")), "témoin : le même fix, caster nommé")
        }
    }

    // ------------------------------------------------------------------ décision 1 : NULL, jamais deviné

    @Test
    fun `DGPS ou RTK sans flux NTRIP vu par Surculus - NULL, jamais WGS84 ni RGF93 devine`() {
        for (q in corriges) assertNull(ReferentielsSource.de(fix(q, caster = null)), "GGA ${q.codeNmea}, NTRIP désactivé")
    }

    @Test
    fun `DGPS par EGNOS, sans NTRIP - NULL (decision du 2026-11-06, elle remplace le WGS84 de la fiche)`() {
        assertNull(ReferentielsSource.de(fix(QualiteGga.DGPS, caster = null, ageCorrectionsS = null)))
        assertNull(ReferentielsSource.de(fix(QualiteGga.DGPS, caster = null, ageCorrectionsS = 4.0)))
    }

    @Test
    fun `un caster qui n'est pas Centipede - NULL`() {
        for (q in corriges) for (h in listOf("caster.rtk2go.com", "caster.centipede.fr.example.org", "")) {
            assertNull(ReferentielsSource.de(fix(q, caster = h)), "GGA ${q.codeNmea}, caster « $h »")
        }
    }

    @Test
    fun `corrections Centipede de plus de 30 s - NULL`() {
        for (q in corriges) for (a in listOf(30.1, 45.0, 600.0)) {
            assertNull(ReferentielsSource.de(fix(q, caster = centipede, ageCorrectionsS = a)), "GGA ${q.codeNmea}, corrections de $a s")
        }
    }

    @Test
    fun `un RTK retrograde par PolitiqueFix (corrections trop vieilles) - NULL, pas WGS84`() {
        // PolitiqueFix rétrograde un RTK aux corrections de plus de 30 s : qualite = AUTONOME, qualiteAnnoncee = RTK.
        for (annoncee in listOf(QualiteGga.RTK_FIXE, QualiteGga.RTK_FLOTTANT)) {
            val r = fix(QualiteGga.AUTONOME, caster = centipede, ageCorrectionsS = 45.0, annoncee = annoncee)
            assertNull(ReferentielsSource.de(r), "RTK ${annoncee.codeNmea} rétrogradé : corrections de plus de 30 s")
        }
    }

    // ------------------------------------------------------------------ décision 2 : la position moyennée

    private fun moyenne(vararg f: FixGnss): String? = assertNotNull(QualiteFix.deFix(f.toList()), "aucune qualité pour ${f.toList()}").referentielSource

    private val rgf93 get() = fix(QualiteGga.RTK_FIXE, caster = centipede)
    private val wgs84 get() = fix(QualiteGga.AUTONOME)
    private val inconnu get() = fix(QualiteGga.RTK_FLOTTANT, caster = null)
    private val telephone get() = fix(QualiteGga.depuisPrecision(3.8), origine = OrigineFix.TELEPHONE, precisionM = 3.8)

    @Test
    fun `banc - chaque fix du moyennage a le referentiel attendu`() {
        assertEquals("RGF93", ReferentielsSource.de(rgf93))
        assertEquals("WGS84", ReferentielsSource.de(wgs84))
        assertNull(ReferentielsSource.de(inconnu))
        assertEquals("WGS84", ReferentielsSource.de(telephone))
    }

    @Test
    fun `tous les fix moyennes en RGF93 - RGF93`() {
        assertEquals("RGF93", moyenne(rgf93))
        assertEquals("RGF93", moyenne(rgf93, fix(QualiteGga.RTK_FLOTTANT, caster = "crtk.net"), rgf93))
    }

    @Test
    fun `tous les fix moyennes en WGS84 - WGS84 (le contrôle de la Colombiere au telephone)`() {
        assertEquals("WGS84", moyenne(telephone))
        assertEquals("WGS84", moyenne(telephone, telephone, telephone))
        assertEquals("WGS84", moyenne(wgs84, wgs84))
    }

    @Test
    fun `un seul fix WGS84 parmi des RGF93 - WGS84`() {
        assertEquals("WGS84", moyenne(rgf93, wgs84, rgf93))
        assertEquals("WGS84", moyenne(rgf93, rgf93, telephone))
    }

    @Test
    fun `un seul fix NULL - NULL, meme parmi des RGF93 et des WGS84`() {
        assertNull(moyenne(inconnu))
        assertNull(moyenne(rgf93, inconnu, rgf93))
        assertNull(moyenne(wgs84, inconnu))
        assertNull(moyenne(rgf93, wgs84, inconnu), "NULL l'emporte sur WGS84")
        assertNull(moyenne(rgf93, fix(QualiteGga.DGPS, caster = centipede), rgf93), "un DGPS, même avec Centipède vu, rend le moyennage NULL")
    }

    @Test
    fun `la qualite du fix d'avant la 094 se construit encore - referentiel NULL par defaut, jamais devine`() {
        assertNull(QualiteFix(precisionM = 0.02, sourceGnss = "recepteur_rtk_fixe", simulee = false).referentielSource)
    }
}
