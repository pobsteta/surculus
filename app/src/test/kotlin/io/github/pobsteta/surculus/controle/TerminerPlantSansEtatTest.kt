package io.github.pobsteta.surculus.controle

import io.github.pobsteta.surculus.core.placette.EtatDsf
import io.github.pobsteta.surculus.core.session.PlacettesControlees
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Mission 095, tests du `testeur` : à l'écran, « Terminer » est grisé tant qu'un plant pointé n'a pas d'état, et la phrase
 * nomme le plant par son numéro avec les mots « sans état » (décision de Pascal du 2026-11-06 ; la phrase exacte n'est pas figée).
 *
 * Pas de plant d'un passage précédent ici (`nbPlantsAnterieurs = 0`) : seuls comptent les plants pointés dans ce contrôle.
 * Coordonnées : caps magnétiques en degrés (10°, 100°, 200°), distances en mètres ; piquet P35-01 à Couchey (EPSG:2154).
 */
class TerminerPlantSansEtatTest {
    private fun BancControle.controlees() = PlacettesControlees.de(journal.lignesCourantes())

    private fun cap(d: Double) = assertNotNull(CapMagnetique.de(d))

    /** Trois plants pointés dans l'ordre des azimuts (n° 1, 2, 3) ; les n° 1 et 3 reçoivent un état, le n° 2 aucun. */
    private fun ControleurPlacette.troisPlantsSansLeDeux() {
        pointerAvecBoussole("2", cap(10.0)); dire(EtatDsf.SAIN)
        pointerAvecBoussole("3", cap(100.0))
        pointerAvecBoussole("4", cap(200.0)); dire(EtatDsf.MORT)
    }

    private fun mots(texte: String) = Regex("\\d+").findAll(texte).map { it.value.toInt() }.toList()

    @Test
    fun `un plant sans etat - Terminer refuse, la phrase nomme le plant n° 2 et dit sans etat`() = with(BancControle(nbPlantsAnterieurs = 0)) {
        val c = controleur()
        c.troisPlantsSansLeDeux()
        c.direBesoin("aucun"); c.direConcurrence("faible")
        val avant = journal.nombreDeLignes
        val e = c.terminer()
        assertEquals(VueControle.TERMINER, e.vue, "on reste sur la vue Terminer")
        val m = assertNotNull(e.message)
        assertTrue(m.erreur)
        assertTrue(m.texte.contains("sans état", ignoreCase = true), m.texte)
        assertEquals(listOf(2), mots(m.texte), "seul le plant n° 2 est nommé : ${m.texte}")
        assertEquals(avant, journal.nombreDeLignes, "rien n'est écrit")
        assertTrue(controlees().isEmpty())
        assertEquals(0, sauvegarde.fins)
    }

    @Test
    fun `la vue Terminer est grisee et dit le plant qui manque`() = with(BancControle(nbPlantsAnterieurs = 0)) {
        val c = controleur()
        c.troisPlantsSansLeDeux()
        c.direBesoin("aucun"); c.direConcurrence("faible")
        val e = c.demanderTerminer()
        assertEquals(VueControle.TERMINER, e.vue)
        assertFalse(e.besoin.terminerPermis, "grisé : le plant n° 2 n'a pas d'état")
        val manque = assertNotNull(e.besoin.manque)
        assertTrue(manque.contains("sans état", ignoreCase = true), manque)
        assertEquals(listOf(2), mots(manque), manque)
    }

    @Test
    fun `plusieurs plants sans etat - tous sont nommes`() = with(BancControle(nbPlantsAnterieurs = 0)) {
        val c = controleur()
        c.pointerAvecBoussole("2", cap(10.0))
        c.pointerAvecBoussole("3", cap(100.0)); c.dire(EtatDsf.SAIN)
        c.pointerAvecBoussole("4", cap(200.0))
        c.direBesoin("aucun"); c.direConcurrence("faible")
        val m = assertNotNull(c.terminer().message)
        assertTrue(m.texte.contains("sans état", ignoreCase = true), m.texte)
        assertEquals(listOf(1, 3), mots(m.texte).sorted(), "les plants n° 1 et n° 3 sont nommés, pas le n° 2 : ${m.texte}")
        assertFalse(c.demanderTerminer().besoin.terminerPermis)
    }

    @Test
    fun `besoin, concurrence et plant manquants ensemble - la phrase dit tout ce qui manque`() = with(BancControle(nbPlantsAnterieurs = 0)) {
        val c = controleur()
        c.troisPlantsSansLeDeux()
        c.demanderTerminer()
        val avant = journal.nombreDeLignes
        val m = assertNotNull(c.terminer().message)
        assertTrue(m.erreur)
        assertTrue(m.texte.contains("besoin", ignoreCase = true), m.texte)
        assertTrue(m.texte.contains("concurrence", ignoreCase = true), m.texte)
        assertTrue(m.texte.contains("sans état", ignoreCase = true), m.texte)
        assertTrue(2 in mots(m.texte), m.texte)
        assertEquals(avant, journal.nombreDeLignes, "rien n'est écrit")
    }

    @Test
    fun `le plant dit, Terminer est permis et la placette est Controlee`() = with(BancControle(nbPlantsAnterieurs = 0)) {
        val c = controleur()
        c.troisPlantsSansLeDeux()
        c.direBesoin("aucun"); c.direConcurrence("faible")
        assertFalse(c.demanderTerminer().besoin.terminerPermis)
        c.plantPrecedent() // du n° 3 au n° 2
        c.dire(EtatDsf.ATTEINT)
        val e = c.demanderTerminer()
        assertTrue(e.besoin.terminerPermis)
        assertEquals(null, e.besoin.manque)
        assertEquals(VueControle.TERMINEE, c.terminer().vue)
        assertEquals(setOf(BancControle.P1.valeur), controlees())
    }
}
