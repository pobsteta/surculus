/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.controle

import io.github.pobsteta.surculus.core.session.QualiteFix
import io.github.pobsteta.surculus.core.session.SourcesGnss
import io.github.pobsteta.surculus.core.session.ValeurControle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mission 091, tests du `dev` : la vue « Terminer » du besoin et de la concurrence (décision de Pascal du 2026-11-04,
 * question 1 : à l'écran, obligatoires, sans valeur par défaut, « Terminer » grisé avec la phrase de ce qui manque), et la
 * qualité du fix portée de l'arrivée au journal.
 *
 * Coordonnées : Lambert-93 (EPSG:2154), x = Est, y = Nord, mètres ; fix en (latitude, longitude) degrés, à Couchey.
 */
class BesoinTerminerDevTest {
    /** Libellés tels que sommieR 0.31.1 les écrit dans la couche `codes` (`R/travaux_qfield.R` l. 61) : lus, jamais embarqués. */
    private val codes = listOf("PL" to "PL - Plantation en plein, placeaux, enrichissement", "RG" to "RG - Regarnis, compléments", "aucun" to "Aucun")

    @Test
    fun `rien n'est choisi d'avance - Terminer grise, la phrase dit ce qui manque`() {
        val v = TextesControle.besoin(null, null, codes)
        assertFalse(v.terminerPermis)
        assertEquals("Choisir le besoin et la concurrence pour terminer.", v.manque)
        assertFalse(v.aucunChoisi)
        assertTrue(v.codes.none { it.choisi })
        assertTrue(v.concurrences.none { it.choisi })
        assertEquals(listOf("PL", "RG"), v.codes.map { it.code }) // « aucun » a son grand bouton, pas une case de la grille
        assertEquals(listOf("faible", "moyenne", "forte"), v.concurrences.map { it.niveau })
        assertEquals("Choisir la concurrence pour terminer.", TextesControle.besoin("RG", null, codes).manque)
        assertEquals("Choisir le besoin pour terminer.", TextesControle.besoin(null, "forte", codes).manque)
        val ok = TextesControle.besoin("aucun", "forte", codes)
        assertTrue(ok.terminerPermis); assertTrue(ok.aucunChoisi); assertNull(ok.manque)
    }

    @Test
    fun `le libelle court retire le code et ne coupe rien (la coupe aux mots se fait a l'ecran)`() {
        assertEquals("Regarnis, compléments", TextesControle.libelleCourt("RG", "RG - Regarnis, compléments"))
        assertEquals("Aucun", TextesControle.libelleCourt("aucun", "Aucun"))
        assertEquals("Plantation en plein, placeaux, enrichissement", TextesControle.libelleCourt("PL", "PL - Plantation en plein, placeaux, enrichissement"))
        assertEquals("", TextesControle.libelleCourt("RG", ""))
    }

    @Test
    fun `choisirLibelle prend le plus de mots entiers qui tiennent, avec des points de suspension seulement si coupe`() {
        val tient = { t: String, sp: Float -> t.length <= (if (sp >= 11f) 12 else 14) }
        assertEquals("Clôture" to 11f, choisirLibelle("Clôture", listOf(11f, 10f), tient))
        assertEquals("Entretien…" to 11f, choisirLibelle("Entretien des limites (peinture, bornes)", listOf(11f, 10f), tient))
        assertEquals("Entretien…" to 11f, choisirLibelle("Entretien de piste", listOf(11f, 10f)) { t, _ -> t.length <= 10 })
        assertEquals("Dégagement…" to 11f, choisirLibelle("Dégagement (manuel, mécanique)", listOf(11f, 10f)) { t, _ -> t.length <= 11 })
        assertEquals("" to 10f, choisirLibelle("Cloisonnement sylvicole", listOf(11f, 10f)) { _, _ -> false })
    }

    @Test
    fun `terminer sans choix reste sur la vue Terminer, puis termine une fois les deux choix faits`() {
        val b = BancControle(nbPlantsAnterieurs = 1)
        val c = b.controleur()
        c.dire(io.github.pobsteta.surculus.core.placette.EtatDsf.SAIN)
        c.demanderTerminer()
        val refuse = c.terminer()
        assertEquals(VueControle.TERMINER, refuse.vue)
        assertEquals("Choisir le besoin et la concurrence pour terminer.", refuse.message!!.texte)
        assertEquals(true, c.direBesoin("aucun").besoin.aucunChoisi)
        val e = c.direConcurrence("moyenne")
        assertTrue(e.besoin.terminerPermis)
        assertTrue(e.besoin.concurrences.single { it.niveau == "moyenne" }.choisi)
        assertEquals(VueControle.TERMINEE, c.terminer().vue)
    }

    @Test
    fun `un code absent de la couche codes est refuse, l'ecran le dit`() {
        val c = BancControle(nbPlantsAnterieurs = 1).controleur()
        val e = c.direBesoin("ZZ")
        assertTrue(e.message!!.erreur)
        assertFalse(e.besoin.aucunChoisi)
    }

    @Test
    fun `la qualite des fix moyennes passe de l'arrivee au journal`() {
        val b = BancControle(nbPlantsAnterieurs = 1)
        val q = QualiteFix(2.5, SourcesGnss.TELEPHONE, simulee = true)
        val e = ArriveeControle.ouvrir(b.contexte, b.journal, b.horloge, b.essences, b.positionsAuPiquet(), null, b.sauvegarde, qualite = q)
        val c = (e as EtatArrivee.Ouverte).controleur
        c.dire(io.github.pobsteta.surculus.core.placette.EtatDsf.SAIN)
        val controle = b.journal.lignesCourantes().last { it.type == "controle" }
        assertEquals(q, ValeurControle.lire(controle.valeur)!!.qualite)
    }
}
