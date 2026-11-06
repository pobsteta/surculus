/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.core.session

import io.github.pobsteta.surculus.core.placette.EmpriseEtFrance
import io.github.pobsteta.surculus.core.placette.EtatDsf
import io.github.pobsteta.surculus.core.placette.Observation
import io.github.pobsteta.surculus.core.placette.UuidPlant
import io.github.pobsteta.surculus.core.placette.Verdict
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Mission 095, tests du `testeur` (spec § 4, § 4.3, § 6 ; décision de Pascal du 2026-11-06, « bloquer et nommer ») :
 * « Terminer » est refusé tant qu'un plant pointé du contrôle n'a reçu ni état (sain, atteint, mort, absent) ni « non observé » ;
 * rien n'est écrit ; un plant remplacé par un regarni n'est pas concerné.
 *
 * Coordonnées : Lambert-93 (EPSG:2154), x = Est, y = Nord, mètres ; le piquet P35-01 est à Couchey (Côte-d'Or), pas en Somalie.
 * Les plants sont pointés à des azimuts croissants (10°, 100°, 200°) : leurs numéros provisoires sont 1, 2, 3 dans l'ordre du pointage.
 */
class TerminerPlantSansEtatTest {
    private fun BancSession.controlees() = PlacettesControlees.de(journal.lignesCourantes())

    /** Trois plants pointés (n° 1, 2, 3) ; rend leurs UUID dans l'ordre des numéros. Aucun état dit. */
    private fun BancSession.troisPlants(s: SessionControle): List<UuidPlant> {
        assertIs<SuiteSession.Fait>(s.pointe(1.0, 10.0))
        assertIs<SuiteSession.Fait>(s.pointe(2.0, 100.0))
        assertIs<SuiteSession.Fait>(s.pointe(3.0, 200.0))
        return s.etat().plants.sortedBy { it.numero }.map { it.uuid }
    }

    private fun choixFaits(s: SessionControle) {
        assertIs<SuiteSession.Fait>(s.direBesoin("aucun")); assertIs<SuiteSession.Fait>(s.direConcurrence("faible"))
    }

    /** Dit l'état du plant [u] ; `null` : « non observé ». */
    private fun dis(s: SessionControle, u: UuidPlant, etat: String?) {
        assertTrue(s.choisir(u))
        assertIs<SuiteSession.Fait>(if (etat == null) s.nonObserve() else s.dire(etat))
    }

    @Test
    fun `le piquet P35-01 tombe en France - pas en Somalie`() {
        val p = BancSession.POSITION_P1
        assertTrue(EmpriseEtFrance.controler(p).enFrance, "x = ${p.xMetres} E, y = ${p.yMetres} N (EPSG:2154)")
    }

    @Test
    fun `un plant pointe sans etat - Terminer est refuse et rien n'est ecrit`() = with(BancSession()) {
        val s = session()
        val (u1, _, u3) = troisPlants(s)
        dis(s, u1, "sain"); dis(s, u3, "mort") // le plant n° 2 n'a rien reçu (cas de P35-03, 2026-11-06)
        choixFaits(s)
        val avant = journal.lignes.size
        val r = s.terminer()
        assertFalse(r is SuiteSession.Fait, "refusé tant que le plant n° 2 n'a ni état ni « non observé » : $r")
        assertEquals(avant, journal.lignes.size, "rien n'est écrit")
        assertFalse(s.etat().termine)
        assertTrue(controlees().isEmpty(), "la placette n'est pas Contrôlée")
        assertEquals(0, ecoute.fins, "aucune fin de placette n'est déclenchée")
        // Refuser deux fois ne change rien non plus.
        assertFalse(s.terminer() is SuiteSession.Fait)
        assertEquals(avant, journal.lignes.size)
    }

    @Test
    fun `le plant manquant recoit un etat - Terminer est accepte`() = with(BancSession()) {
        val s = session()
        val (u1, u2, u3) = troisPlants(s)
        dis(s, u1, "sain"); dis(s, u3, "mort")
        choixFaits(s)
        assertFalse(s.terminer() is SuiteSession.Fait)
        dis(s, u2, "atteint")
        assertIs<SuiteSession.Fait>(s.terminer())
        assertTrue(s.etat().termine)
        assertEquals(setOf(BancSession.P1.valeur), controlees())
        assertEquals(3, (s.etat().totaux as Verdict.Accepte).valeur.nbTotal)
    }

    @Test
    fun `le meme plant dit non observe - Terminer est accepte et il sort des totaux`() = with(BancSession()) {
        val s = session()
        val (u1, u2, u3) = troisPlants(s)
        dis(s, u1, "sain"); dis(s, u3, "mort")
        choixFaits(s)
        assertFalse(s.terminer() is SuiteSession.Fait)
        dis(s, u2, null) // non observé
        assertIs<SuiteSession.Fait>(s.terminer())
        assertTrue(s.etat().termine)
        assertEquals(2, (s.etat().totaux as Verdict.Accepte).valeur.nbTotal, "3 plants pointés, 2 comptés : le non observé sort des totaux")
        assertEquals(1, ecoute.fins)
    }

    @Test
    fun `plusieurs plants sans etat - Terminer reste refuse tant qu'il en manque un seul`() = with(BancSession()) {
        val s = session()
        val (u1, u2, u3) = troisPlants(s)
        dis(s, u2, "sain") // les n° 1 et n° 3 manquent
        choixFaits(s)
        assertFalse(s.terminer() is SuiteSession.Fait)
        dis(s, u1, "sain")
        val enCours = journal.lignes.size
        assertFalse(s.terminer() is SuiteSession.Fait, "le n° 3 manque encore")
        assertEquals(enCours, journal.lignes.size, "le refus n'écrit rien")
        dis(s, u3, null)
        assertTrue(s.terminer() is SuiteSession.Fait)
    }

    @Test
    fun `besoin, concurrence et plant manquants ensemble - le refus dit le besoin et la concurrence, rien n'est ecrit`() = with(BancSession()) {
        val s = session()
        val (u1, _, u3) = troisPlants(s)
        dis(s, u1, "sain"); dis(s, u3, "sain")
        val avant = journal.lignes.size
        val manque = assertIs<SuiteSession.ChoixManquant>(s.terminer(), "le besoin et la concurrence manquent aussi")
        assertTrue(manque.besoin); assertTrue(manque.concurrence)
        assertEquals(1, manque.plantsSansEtat.size, "le plant n° 2 est aussi dit manquant, avec le besoin et la concurrence")
        assertEquals(avant, journal.lignes.size)
        assertIs<SuiteSession.Fait>(s.direBesoin("aucun"))
        assertFalse(s.terminer() is SuiteSession.Fait, "la concurrence et le plant n° 2 manquent encore")
        assertIs<SuiteSession.Fait>(s.direConcurrence("faible"))
        assertFalse(s.terminer() is SuiteSession.Fait, "le plant n° 2 manque encore")
        assertEquals(avant + 2, journal.lignes.size, "seuls le besoin et la concurrence ont été écrits")
    }

    @Test
    fun `un plant remplace par un regarni, sans etat ce jour - n'empeche pas Terminer`() {
        val b = BancSession("2026-10-26T09:00:00Z")
        with(b) {
            val s = session()
            s.pointe(1.0, 10.0); val mort = s.etat().courant!!; s.dire("mort")
            s.remplace(); s.pointe(1.1, 12.0); s.dire("sain")
            s.direBesoin("aucun"); s.direConcurrence("faible")
            assertIs<SuiteSession.Fait>(s.terminer())
            horloge.a("2026-10-27T09:00:00Z")
            val s2 = session()
            assertTrue(s2.etat().plants.first { it.uuid == mort }.remplaceAvant, "le plant mort est remplacé depuis hier")
            // Aujourd'hui : le regarni reçoit son état ; le plant remplacé, sorti des totaux, n'a rien et ne bloque pas.
            assertIs<SuiteSession.Fait>(s2.dire("sain"))
            choixFaits(s2)
            assertIs<SuiteSession.Fait>(s2.terminer())
            assertTrue(s2.etat().termine)
        }
    }

    // ------------------------------------------------------------------ plants anciens (décision de Pascal du 2026-11-06 :
    // tous les plants actifs comptent, d'un passage précédent ou pointés ce jour, sauf un plant remplacé par un regarni)

    private val anciens = listOf(1, 2, 3).map { PlantAnterieur(UuidPlant("ancien-$it"), it, "CHS", null, Observation.Observe(EtatDsf.SAIN)) }
    private fun avecAnciens() = BancSession(plantsAnterieurs = mapOf(BancSession.P1 to anciens))
    private fun ancien(n: Int) = UuidPlant("ancien-$n")

    @Test
    fun `un plant ancien sans etat ce jour - Terminer est refuse et rien n'est ecrit`() = with(avecAnciens()) {
        val s = session()
        assertEquals(3, s.etat().nbPlants)
        dis(s, ancien(1), "sain"); dis(s, ancien(3), "mort") // l'ancien n° 2 n'a rien reçu aujourd'hui
        choixFaits(s)
        val avant = journal.lignes.size
        assertFalse(s.terminer() is SuiteSession.Fait, "l'ancien n° 2 n'a ni état ni « non observé »")
        assertEquals(avant, journal.lignes.size, "rien n'est écrit")
        assertFalse(s.etat().termine)
        assertTrue(controlees().isEmpty())
        dis(s, ancien(2), "atteint")
        assertTrue(s.terminer() is SuiteSession.Fait)
    }

    @Test
    fun `un plant ancien dit non observe ne bloque plus et sort des totaux`() = with(avecAnciens()) {
        val s = session()
        dis(s, ancien(1), "sain"); dis(s, ancien(3), "mort")
        choixFaits(s)
        assertFalse(s.terminer() is SuiteSession.Fait)
        dis(s, ancien(2), null)
        assertIs<SuiteSession.Fait>(s.terminer())
        assertEquals(2, (s.etat().totaux as Verdict.Accepte).valeur.nbTotal, "3 plants anciens, 2 comptés : le non observé sort des totaux")
    }

    @Test
    fun `un ancien et un plant du jour sans etat bloquent chacun`() = with(avecAnciens()) {
        val s = session()
        dis(s, ancien(1), "sain"); dis(s, ancien(2), "sain")
        assertIs<SuiteSession.Fait>(s.pointe(1.0, 10.0))
        val nouveau = s.etat().courant!!
        choixFaits(s)
        assertFalse(s.terminer() is SuiteSession.Fait, "ancien n° 3 et plant du jour manquent")
        dis(s, ancien(3), "sain")
        assertFalse(s.terminer() is SuiteSession.Fait, "le plant du jour manque encore")
        dis(s, nouveau, "sain")
        assertTrue(s.terminer() is SuiteSession.Fait)
    }

    @Test
    fun `un ancien remplace par un regarni ne bloque pas, un autre ancien sans etat bloque`() {
        val b = BancSession("2026-10-26T09:00:00Z")
        with(b) {
            val s = session()
            s.pointe(1.0, 10.0); val mort = s.etat().courant!!; s.dire("mort")
            s.remplace(); s.pointe(1.1, 12.0); val regarni = s.etat().courant!!; s.dire("sain")
            s.pointe(2.0, 100.0); val autre = s.etat().courant!!; s.dire("sain")
            s.direBesoin("aucun"); s.direConcurrence("faible")
            assertIs<SuiteSession.Fait>(s.terminer())
            horloge.a("2026-10-27T09:00:00Z")
            val s2 = session()
            assertTrue(s2.etat().plants.first { it.uuid == mort }.remplaceAvant)
            dis(s2, regarni, "sain")
            choixFaits(s2)
            assertFalse(s2.terminer() is SuiteSession.Fait, "l'autre ancien n'a pas d'état aujourd'hui ; le plant remplacé, lui, ne compte pas")
            dis(s2, autre, null)
            assertIs<SuiteSession.Fait>(s2.terminer())
        }
    }

    @Test
    fun `sans aucun etat dit, le refus d'avant reste - rien a terminer`() = with(BancSession()) {
        val s = session()
        troisPlants(s)
        choixFaits(s)
        assertEquals(SuiteSession.RienASaisir, s.terminer(), "aucun état dit : la règle de la 012-A est inchangée")
    }
}
