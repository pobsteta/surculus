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
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.reperes.CodeEpsg
import io.github.pobsteta.surculus.core.reperes.PointDerive
import io.github.pobsteta.surculus.core.reperes.Resultat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import io.github.pobsteta.surculus.core.gnss.QualiteFix as QualiteGga

/**
 * Tests du `dev` de la mission 091 (décisions de Pascal du 2026-11-04) : qualité du fix d'arrivée, besoin et concurrence
 * obligatoires à « Terminer », valeurs v3 du journal, recalcul d'un plant avec la déclinaison du journal.
 *
 * Coordonnées : Lambert-93 (EPSG:2154), x = Est, y = Nord, mètres ; fix en degrés décimaux (latitude, longitude). Le piquet
 * P35-01 est à Couchey (Côte-d'Or) : un test vérifie qu'il tombe en France, pas en Somalie.
 */
class BesoinEtFixDevTest {
    private fun fix(
        origine: OrigineFix = OrigineFix.TELEPHONE,
        qualite: QualiteGga = QualiteGga.AUTONOME,
        precision: Double? = 3.0,
        simule: Boolean = false,
        annoncee: QualiteGga? = null,
    ) = FixGnss(
        position = Position(47.2591, 4.9325), qualite = qualite, nbSatellites = 9, hdop = null, altitudeM = null,
        ageCorrectionsS = null, precisionHorizontaleM = precision, horodatageMs = 0L, origine = origine, simule = simule,
        qualiteAnnoncee = annoncee,
    )

    // ------------------------------------------------------------------ QualiteFix.de (question 4)

    @Test
    fun `sans fix, pas de qualite`() {
        assertNull(QualiteFix.de(emptyList()))
    }

    @Test
    fun `la source de chaque fix suit l'origine et la qualite GGA`() {
        assertEquals(SourcesGnss.TELEPHONE, SourcesGnss.de(fix()))
        val r = OrigineFix.RECEPTEUR
        assertEquals(SourcesGnss.RECEPTEUR_RTK_FIXE, SourcesGnss.de(fix(r, QualiteGga.RTK_FIXE)))
        assertEquals(SourcesGnss.RECEPTEUR_RTK_FLOTTANT, SourcesGnss.de(fix(r, QualiteGga.RTK_FLOTTANT)))
        assertEquals(SourcesGnss.RECEPTEUR_DGPS, SourcesGnss.de(fix(r, QualiteGga.DGPS)))
        assertEquals(SourcesGnss.RECEPTEUR_AUTONOME, SourcesGnss.de(fix(r, QualiteGga.AUTONOME)))
        // Jamais dit meilleur qu'il n'est : PPS, estimé, manuel, simulation comptent autonome.
        for (q in listOf(QualiteGga.PPS, QualiteGga.ESTIME, QualiteGga.MANUEL, QualiteGga.SIMULATION)) {
            assertEquals(SourcesGnss.RECEPTEUR_AUTONOME, SourcesGnss.de(fix(r, q)), q.name)
        }
        // Un RTK rétrogradé (corrections trop vieilles) compte ce qu'il est devenu, pas ce qu'il annonçait.
        assertEquals(SourcesGnss.RECEPTEUR_AUTONOME, SourcesGnss.de(fix(r, QualiteGga.AUTONOME, annoncee = QualiteGga.RTK_FIXE)))
    }

    @Test
    fun `une moyenne prend la plus mauvaise source et la plus mauvaise precision`() {
        val r = OrigineFix.RECEPTEUR
        val q = assertNotNull(QualiteFix.de(listOf(fix(r, QualiteGga.RTK_FIXE, 0.02), fix(r, QualiteGga.RTK_FLOTTANT, 0.3), fix(r, QualiteGga.RTK_FIXE, 0.03))))
        assertEquals(SourcesGnss.RECEPTEUR_RTK_FLOTTANT, q.sourceGnss)
        assertEquals(0.3, q.precisionM)
        assertFalse(q.simulee)
        // Un fix du téléphone dans la moyenne : la source est « telephone ».
        assertEquals(SourcesGnss.TELEPHONE, QualiteFix.de(listOf(fix(r, QualiteGga.RTK_FIXE, 0.02), fix()))!!.sourceGnss)
    }

    @Test
    fun `une precision inconnue, nulle ou non finie rend la precision de la moyenne inconnue, jamais 0`() {
        for (p in listOf(null, 0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertNull(QualiteFix.de(listOf(fix(precision = 2.0), fix(precision = p)))!!.precisionM, "précision $p")
        }
    }

    @Test
    fun `simulee si un fix est isMock ou de qualite GGA 8`() {
        assertTrue(QualiteFix.de(listOf(fix(), fix(simule = true)))!!.simulee)
        assertTrue(QualiteFix.de(listOf(fix(OrigineFix.RECEPTEUR, QualiteGga.SIMULATION)))!!.simulee)
        assertFalse(QualiteFix.de(listOf(fix(), fix()))!!.simulee)
    }

    // ------------------------------------------------------------------ session : besoin, concurrence, qualité

    private fun banc() = BancSession()

    private fun dernierControle(b: BancSession): ValeurControle =
        ValeurControle.lire(b.journal.deType(FormatValeurs.TYPE_CONTROLE).last().valeur)!!

    @Test
    fun `le piquet P35-01 du banc tombe en France - pas en Somalie`() {
        val p = BancSession.POSITION_P1
        val c = io.github.pobsteta.surculus.core.placette.EmpriseEtFrance.controler(p)
        assertTrue(c.enFrance, "x = ${p.xMetres} E, y = ${p.yMetres} N (EPSG:2154)")
    }

    @Test
    fun `terminer sans besoin ou sans concurrence est refuse, rien n'est ecrit, et la reponse dit ce qui manque`() = with(banc()) {
        val s = session()
        assertIs<SuiteSession.Fait>(s.pointe(1.2, 10.0)); assertIs<SuiteSession.Fait>(s.dire("sain", hauteurCm = 30))
        val avant = journal.lignes.size
        assertEquals(SuiteSession.ChoixManquant(besoin = true, concurrence = true), s.terminer())
        assertEquals(avant, journal.lignes.size)
        assertIs<SuiteSession.Fait>(s.direBesoin("aucun"))
        assertEquals(SuiteSession.ChoixManquant(besoin = false, concurrence = true), s.terminer())
        assertIs<SuiteSession.Fait>(s.direConcurrence("faible"))
        assertIs<SuiteSession.Fait>(s.terminer())
        val v = dernierControle(this)
        assertTrue(v.termine); assertEquals("aucun", v.besoin); assertEquals("faible", v.concurrence)
        assertEquals("aucun", s.etat().besoin); assertEquals("faible", s.etat().concurrence)
    }

    @Test
    fun `un besoin redit apres terminer garde le controle termine, et le meme besoin redit n'ecrit rien`() = with(banc()) {
        val s = session()
        s.pointe(1.2, 10.0); s.dire("sain", hauteurCm = 30); s.direBesoin("aucun"); s.direConcurrence("faible")
        assertIs<SuiteSession.Fait>(s.terminer())
        val avant = journal.lignes.size
        assertEquals(SuiteSession.Fait(emptyList()), s.direConcurrence("faible"))
        assertEquals(avant, journal.lignes.size, "rien ne change, rien n'est écrit")
        assertIs<SuiteSession.Fait>(s.direConcurrence("forte"))
        val v = dernierControle(this)
        assertTrue(v.termine, "toujours terminé"); assertEquals("forte", v.concurrence)
        assertTrue(v.numeros!!.isNotEmpty(), "numéros toujours figés")
    }

    @Test
    fun `la qualite dite a l'arrivee est ecrite au journal, et rien sans position mesuree`() {
        val b = banc()
        val q = QualiteFix(0.4, SourcesGnss.RECEPTEUR_RTK_FIXE, simulee = false)
        val o = SessionControle.ouvrir(b.contexte, b.journal, b.horloge, b.essences, Arrivee.Lambert93(BancSession.POSITION_P1, q))
        val s = assertIs<OuvertureControle.Ouverte>(o).session
        with(b) { s.pointe(1.2, 10.0) }
        assertEquals(q, dernierControle(b).qualiteFix)

        val b2 = banc()
        val s2 = assertIs<OuvertureControle.Ouverte>(SessionControle.ouvrir(b2.contexte, b2.journal, b2.horloge, b2.essences, Arrivee.SansPosition, "P35-01")).session
        with(b2) { s2.pointe(1.2, 10.0) }
        assertNull(dernierControle(b2).qualiteFix)
    }

    @Test
    fun `un code de la couche codes du contexte est un besoin permis, au caractere pres`() {
        val b = banc()
        val contexte = b.contexte.copy(codesTravaux = linkedSetOf("PL", "RG"))
        val s = (SessionControle.ouvrir(contexte, b.journal, b.horloge, b.essences, Arrivee.Lambert93(BancSession.POSITION_P1)) as OuvertureControle.Ouverte).session
        assertIs<SuiteSession.Fait>(s.direBesoin("RG"))
        assertEquals("RG", s.etat().besoin)
        assertEquals("hors_referentiel", assertIs<SuiteSession.Refus>(s.direBesoin("rg")).code)
        assertEquals("hors_referentiel", assertIs<SuiteSession.Refus>(s.direConcurrence("Forte")).code)
    }

    // ------------------------------------------------------------------ valeurs v3 du journal (question 3)

    private val v3 = ValeurControle(
        placetteUuid = "11111111-1111-4111-8111-111111111111", codePlacette = "P35-01", jourCivil = "2026-10-20",
        operateur = "Agent d'essai", termine = true, visiteLeUtcMs = Instant.parse("2026-10-20T09:30:00Z").toEpochMilli(),
        positionX = 846_165.36, positionY = 6_686_122.86, positionMesureeX = 846_165.36, positionMesureeY = 6_686_122.86,
        rattacheParIdentite = false, repereEpsg = 2154, numeros = mapOf("p" to 1),
        besoin = "RG", concurrence = "forte", qualiteFix = QualiteFix(0.4, SourcesGnss.TELEPHONE, simulee = true),
    )

    @Test
    fun `une valeur v3 s'ecrit avec v 3 et se relit a l'identique`() {
        val t = v3.versJson()
        assertTrue(t.startsWith("{\"v\":3,"), t)
        val traces = ArrayList<String>()
        assertEquals(v3, ValeurControle.lire(t) { traces += it })
        assertEquals(emptyList(), traces)
        // Sans besoin ni qualité : clés à null, relues à null.
        val vide = v3.copy(besoin = null, concurrence = null, qualiteFix = null)
        assertEquals(vide, ValeurControle.lire(vide.versJson()))
        // Précision inconnue : la qualité garde sa source et « simulée ».
        val sansPrecision = v3.copy(qualiteFix = QualiteFix(null, SourcesGnss.RECEPTEUR_AUTONOME, false))
        assertEquals(sansPrecision, ValeurControle.lire(sansPrecision.versJson()))
    }

    @Test
    fun `les valeurs v1 et v2 restent lues, sans besoin ni qualite - jamais devines`() {
        for (v in listOf(1, 2)) {
            val ancien = v3.versJson().replace("{\"v\":3,", "{\"v\":$v,")
            val lu = assertNotNull(ValeurControle.lire(ancien), "v$v")
            assertNull(lu.besoin); assertNull(lu.concurrence); assertNull(lu.qualiteFix)
        }
    }

    @Test
    fun `une valeur v3 incoherente est ignoree et tracee - qualite sans position, source sans simulee, precision 0, type faux`() {
        val sans = v3.copy(positionMesureeX = null, positionMesureeY = null, rattacheParIdentite = true, sansPositionMesuree = true)
        val incoherentes = listOf(
            sans.versJson(),
            v3.versJson().replace("\"simulee\":true", "\"simulee\":null"),
            v3.versJson().replace("\"precision_m\":0.4", "\"precision_m\":0.0"),
            v3.versJson().replace("\"besoin\":\"RG\"", "\"besoin\":12"),
            v3.versJson().replace("\"simulee\":true", "\"simulee\":\"oui\""),
        )
        for (t in incoherentes) {
            val traces = ArrayList<String>()
            assertNull(ValeurControle.lire(t) { traces += it }, t)
            assertEquals(1, traces.size, t)
        }
    }

    @Test
    fun `une valeur v4 est ignoree et tracee`() {
        val traces = ArrayList<String>()
        assertNull(ValeurControle.lire(v3.versJson().replace("{\"v\":3,", "{\"v\":4,")) { traces += it })
        assertEquals(1, traces.size)
    }

    // ------------------------------------------------------------------ recalcul avec la déclinaison du journal (point 6)

    @Test
    fun `deriver avec la declinaison du modele donne au bit pres la meme position que deriver`() {
        val piquet = BancSession.POSITION_P1
        val az = BancSession.degres(143.0)
        val jour = LocalDate.parse("2026-10-20")
        val d = (PointDerive.deriver(piquet, CodeEpsg.RGF93_V1_LAMBERT93, 6.2, az, jour) as Resultat.Accepte).valeur
        val e = (PointDerive.deriverAvecDeclinaison(piquet, CodeEpsg.RGF93_V1_LAMBERT93, 6.2, az, jour, d.orientation.declinaisonDeg) as Resultat.Accepte).valeur
        assertEquals(d.position, e.position)
        assertEquals(d.orientation, e.orientation)
    }

    @Test
    fun `deriver avec une autre declinaison deplace le plant - le recalcul suit le journal, pas le modele`() {
        val piquet = BancSession.POSITION_P1
        val az = BancSession.degres(0.0)
        val jour = LocalDate.parse("2026-10-20")
        val a = (PointDerive.deriverAvecDeclinaison(piquet, CodeEpsg.RGF93_V1_LAMBERT93, 10.0, az, jour, 2.0) as Resultat.Accepte).valeur
        val b = (PointDerive.deriverAvecDeclinaison(piquet, CodeEpsg.RGF93_V1_LAMBERT93, 10.0, az, jour, 3.0) as Resultat.Accepte).valeur
        // 1° à 10 m : environ 17 cm vers l'est, très au-delà de la tolérance d'arrondi.
        assertEquals(0.1745, b.position.xMetres - a.position.xMetres, 0.001)
        val horsFrance = PointDerive.deriverAvecDeclinaison(PointLambert93(xMetres = 6_686_123.26, yMetres = 846_164.76), CodeEpsg.RGF93_V1_LAMBERT93, 1.0, az, jour, 2.0)
        assertIs<Resultat.Refuse>(horsFrance, "axes inversés : piquet hors de France, refusé")
    }
}
