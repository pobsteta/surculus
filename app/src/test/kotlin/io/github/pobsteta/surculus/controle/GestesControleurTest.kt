package io.github.pobsteta.surculus.controle

import io.github.pobsteta.surculus.core.gnss.Position
import io.github.pobsteta.surculus.core.placette.EtatDsf
import io.github.pobsteta.surculus.core.session.Geste
import io.github.pobsteta.surculus.core.session.PlacettesControlees
import io.github.pobsteta.surculus.core.session.Touche
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Mission 012-C1 : un geste de volume est appliqué par la session (table de la spec § 4.3) et dit à l'écran ce qu'il a fait. */
class GestesControleurTest {
    private fun BancControle.controlees() = PlacettesControlees.de(journal.lignesCourantes())

    @Test fun `volume haut court - sain, puis plant suivant, dit a l'ecran`() = with(BancControle()) {
        val e = controleur().geste(Geste.VOLUME_HAUT_COURT)
        assertEquals("Dit : n° 1, sain.", e.message?.texte)
        assertFalse(e.message!!.erreur)
        assertEquals(2, e.plant.numero)
        assertEquals("1 sur 12", e.plant.compteur)
    }

    @Test fun `volume bas court - mort, puis plant suivant`() = with(BancControle()) {
        val e = controleur().geste(Geste.VOLUME_BAS_COURT)
        assertEquals("Dit : n° 1, mort.", e.message?.texte)
        assertEquals(2, e.plant.numero)
    }

    @Test fun `volume haut long - atteint, le plant est garde, sans cause`() = with(BancControle()) {
        val e = controleur().geste(Geste.VOLUME_HAUT_LONG)
        assertEquals("Dit : n° 1, atteint.", e.message?.texte)
        assertEquals(1, e.plant.numero)
        assertEquals(EtatDsf.ATTEINT, e.plant.etatDuJour)
        assertNull(e.plant.groupeCause)
        assertTrue(e.plant.causesVisibles, "la cause se met a l'ecran")
    }

    @Test fun `volume bas long - la dictee n'existe pas encore, rien n'est ecrit`() = with(BancControle()) {
        val c = controleur()
        val avant = journal.nombreDeLignes
        val e = c.geste(Geste.VOLUME_BAS_LONG)
        assertEquals("La dictée n'est pas encore disponible.", e.message?.texte)
        assertFalse(e.message!!.erreur)
        assertEquals(avant, journal.nombreDeLignes)
    }

    @Test fun `plus de plant a dire - refus clair, rien d'ecrit`() = with(BancControle(nbPlantsAnterieurs = 1)) {
        val c = controleur()
        c.geste(Geste.VOLUME_HAUT_COURT)
        val avant = journal.nombreDeLignes
        val e = c.geste(Geste.VOLUME_HAUT_COURT)
        assertEquals("Aucun plant à qui dire un état.", e.message?.texte)
        assertTrue(e.message!!.erreur)
        assertEquals(avant, journal.nombreDeLignes)
    }

    @Test fun `double appui bas annule la derniere saisie`() = with(BancControle()) {
        val c = controleur()
        c.geste(Geste.VOLUME_HAUT_COURT)
        val e = c.geste(Geste.VOLUME_BAS_DOUBLE_COURT)
        assertEquals("Annulé.", e.message?.texte)
        assertEquals("0 sur 12", e.plant.compteur)
    }

    @Test fun `rien a annuler - dit, pas d'erreur rouge`() = with(BancControle()) {
        val e = controleur().geste(Geste.VOLUME_BAS_DOUBLE_COURT)
        assertEquals("Rien à annuler.", e.message?.texte)
        assertFalse(e.message!!.erreur)
    }

    @Test fun `une saisie annulee ne rend pas la placette Controlee`() = with(BancControle()) {
        val c = controleur()
        c.geste(Geste.VOLUME_HAUT_COURT)
        c.demanderTerminer()
        c.terminer()
        assertEquals(setOf(BancControle.P1.valeur), controlees())
        // Annuler la fin, puis l'état : la placette n'est plus Contrôlée.
        c.geste(Geste.VOLUME_BAS_DOUBLE_COURT)
        assertTrue(controlees().isEmpty())
        c.geste(Geste.VOLUME_BAS_DOUBLE_COURT)
        assertTrue(controlees().isEmpty())
    }

    @Test fun `apres Terminer, le geste est permis avec l'alerte avant le numero`() = with(BancControle()) {
        val c = controleur()
        c.geste(Geste.VOLUME_HAUT_COURT)
        c.demanderTerminer()
        c.terminer()
        c.reprendre()
        val e = c.geste(Geste.VOLUME_HAUT_COURT)
        assertEquals("Placette déjà terminée. Dit : n° 2, sain.", e.message?.texte)
        assertTrue(e.message!!.erreur)
        assertTrue(e.plant.alerteTerminee)
    }

    @Test fun `deux touches ensemble - un message, rien d'ecrit`() = with(BancControle()) {
        val c = controleur()
        val avant = journal.nombreDeLignes
        val e = c.deuxTouches()
        assertEquals("Une touche à la fois.", e.message?.texte)
        assertEquals(avant, journal.nombreDeLignes)
    }

    @Test fun `l'annonce de sortie s'affiche a l'ecran`() = with(BancControle()) {
        val e = controleur().annoncer("Mode poche terminé.")
        assertEquals("Mode poche terminé.", e.message?.texte)
        assertFalse(e.message!!.erreur)
    }

    /** Placette de la Somalie (chapitre 15) : un fix hors de France ne pointe rien, même par le volume. */
    @Test fun `un fix en Somalie n'ouvre aucune session - les touches de volume n'ont rien a ecrire`() = with(BancControle()) {
        val somalie = Position(latitude = 2.0469, longitude = 45.3182)
        assertIs<EtatArrivee.Refus>(ouvrir(positions = listOf(somalie)))
        val mode = ModePoche().apply { entrer(0) }
        val pont = PontPoche(mode) // aucune écoute : pas de contrôleur, donc personne pour appliquer le geste
        pont.touche(Touche.HAUT, true, 0)
        pont.touche(Touche.HAUT, false, 100)
        assertEquals(0, journal.nombreDeLignes)
        assertEquals(0, sauvegarde.saisies)
    }
}
