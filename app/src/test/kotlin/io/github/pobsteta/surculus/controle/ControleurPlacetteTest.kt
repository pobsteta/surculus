package io.github.pobsteta.surculus.controle

import io.github.pobsteta.surculus.core.placette.EtatDsf
import io.github.pobsteta.surculus.core.placette.GroupeCause
import io.github.pobsteta.surculus.core.session.PlacettesControlees
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Mission 012-B1 : ce que l'écran du contrôle montre, en Kotlin pur, sur la session de la 012-A. */
class ControleurPlacetteTest {
    private fun BancControle.controlees() = PlacettesControlees.de(journal.lignesCourantes())

    @Test fun `le plant courant, 7 sur 12 et l'etat precedent en petit`() = with(BancControle()) {
        val c = controleur()
        repeat(6) { c.dire(EtatDsf.SAIN) }
        val e = c.instantane().plant
        assertEquals("Plant n° 7", e.titre)
        assertEquals(7, e.numero)
        assertEquals("6 sur 12", e.compteur)
        assertEquals(TextesControle.PAS_ENCORE_DIT, e.libelleEtat)
        assertEquals("avant : atteint", e.precedent)
        val apres = c.dire(EtatDsf.SAIN).plant
        assertEquals("7 sur 12", apres.compteur)
        assertEquals("Plant n° 8", apres.titre)
    }

    @Test fun `un plant sans etat precedent n'affiche rien en gris`() = with(BancControle(precedent = null)) {
        assertNull(controleur().instantane().plant.precedent)
    }

    @Test fun `atteint offre les causes, sain et absent passent au suivant, la cause se dit puis on passe`() = with(BancControle()) {
        val c = controleur()
        val atteint = c.dire(EtatDsf.ATTEINT).plant
        assertEquals(1, atteint.numero)
        assertTrue(atteint.causesVisibles)
        assertEquals(EtatDsf.ATTEINT, atteint.etatDuJour)
        val apresCause = c.cause(GroupeCause.ANIMAUX)
        assertNull(apresCause.message)
        assertEquals(2, apresCause.plant.numero)
        val absent = c.dire(EtatDsf.ABSENT).plant
        assertEquals(3, absent.numero)
        assertFalse(absent.causesVisibles)
    }

    @Test fun `la cause choisie est celle du plant, relue du journal`() = with(BancControle()) {
        val c = controleur()
        c.dire(EtatDsf.MORT)
        c.cause(GroupeCause.PATHOLOGIQUE)
        c.plantPrecedent()
        assertEquals(GroupeCause.PATHOLOGIQUE, c.instantane().plant.groupeCause)
    }

    @Test fun `Terminer sans etat dit est refuse par la session seule et l'ecran le dit clairement`() = with(BancControle()) {
        val c = controleur()
        // Le pilote ne juge plus : il ouvre la confirmation, c'est la session qui refuse au moment de terminer.
        assertEquals(VueControle.TERMINER, c.demanderTerminer().vue)
        val e = c.terminer()
        assertEquals(VueControle.PLANT, e.vue)
        val m = assertNotNull(e.message)
        assertTrue(m.erreur)
        assertEquals("Aucun état dit aujourd'hui : rien à terminer.", m.texte)
        assertTrue(controlees().isEmpty())
        // Même par la session directement, rien n'est écrit : pas de contrôle, pas de « Contrôlée ».
        assertEquals(0, journal.nombreDeLignes)
    }

    @Test fun `Terminer avec un etat dit ouvre la confirmation avec les totaux, puis la placette est Controlee`() = with(BancControle()) {
        val c = controleur()
        c.dire(EtatDsf.SAIN)
        c.dire(EtatDsf.MORT)
        val confirmation = c.demanderTerminer()
        assertEquals(VueControle.TERMINER, confirmation.vue)
        assertEquals("2 plants · 1 vivant · 0 abrouti", confirmation.totaux.texte)
        assertTrue(controlees().isEmpty(), "pas encore terminée : encore à faire")
        val fini = c.terminer()
        assertEquals(VueControle.TERMINEE, fini.vue)
        assertEquals(setOf(BancControle.P1.valeur), controlees())
        assertEquals(1, sauvegarde.fins)
    }

    @Test fun `sortir sans Terminer ne rend pas la placette Controlee et ne perd rien`() = with(BancControle()) {
        val c = controleur()
        c.dire(EtatDsf.SAIN)
        assertTrue(controlees().isEmpty())
        // L'application rouverte : la session relit le journal.
        val relu = controleur().instantane().plant
        assertEquals("1 sur 12", relu.compteur)
    }

    @Test fun `saisie apres Terminer le meme jour - alerte placette deja terminee, la placette reste Controlee`() = with(BancControle()) {
        val c = controleur()
        c.dire(EtatDsf.SAIN)
        c.demanderTerminer()
        c.terminer()
        assertTrue(c.instantane().plant.alerteTerminee, "l'alerte est vraie dès que la placette est terminée")
        val repris = c.reprendre()
        assertEquals(VueControle.PLANT, repris.vue)
        assertTrue(repris.plant.alerteTerminee)
        val dit = c.dire(EtatDsf.MORT)
        assertNull(dit.message, "la saisie reste permise")
        assertTrue(dit.plant.alerteTerminee)
        assertEquals(setOf(BancControle.P1.valeur), controlees())
    }

    @Test fun `terminer deux fois - la session refuse, l'ecran le dit`() = with(BancControle()) {
        val c = controleur()
        c.dire(EtatDsf.SAIN)
        c.terminer()
        val m = assertNotNull(c.terminer().message)
        assertEquals("Placette déjà terminée aujourd'hui.", m.texte)
    }

    @Test fun `annuler defait la derniere saisie et annuler la fin rend la placette a faire`() = with(BancControle()) {
        val c = controleur()
        c.dire(EtatDsf.SAIN)
        c.terminer()
        c.annuler()
        assertTrue(controlees().isEmpty())
        c.annuler()
        assertEquals("0 sur 12", c.instantane().plant.compteur)
    }

    @Test fun `la cause sur un plant sain est un refus montre avec son code`() = with(BancControle()) {
        val c = controleur()
        c.dire(EtatDsf.SAIN)
        c.plantPrecedent()
        val m = assertNotNull(c.cause(GroupeCause.ANIMAUX).message)
        assertTrue(m.erreur)
        assertTrue(m.texte.startsWith("cause_sur_etat : "), m.texte)
    }

    @Test fun `un placette sans plant dit qu'il n'y a rien a contrôler, sans inventer de plant`() = with(BancControle(nbPlantsAnterieurs = 0)) {
        val e = controleur().instantane().plant
        assertTrue(e.aucunPlant)
        assertEquals("Aucun plant", e.titre)
        assertNull(e.numero)
    }

    @Test fun `plant precedent et suivant parcourent les plants dans l'ordre des numeros`() = with(BancControle()) {
        val c = controleur()
        assertFalse(c.instantane().plant.aPrecedent)
        assertEquals(2, c.plantSuivant().plant.numero)
        assertEquals(1, c.plantPrecedent().plant.numero)
        repeat(11) { c.plantSuivant() }
        val dernier = c.instantane().plant
        assertEquals(12, dernier.numero)
        assertFalse(dernier.aSuivant)
    }
}
