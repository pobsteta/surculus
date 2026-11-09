package io.github.pobsteta.surculus.rendu

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import io.github.pobsteta.surculus.controle.ArriveeControle
import io.github.pobsteta.surculus.controle.BancControle
import io.github.pobsteta.surculus.controle.EtatArrivee
import io.github.pobsteta.surculus.controle.TextesControle
import io.github.pobsteta.surculus.controle.VueConfirmerSansPosition
import io.github.pobsteta.surculus.core.placette.EtatDsf
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Mission 100, écran de contrôle : la vue de confirmation de « Ouvrir sans position » (`VueConfirmerSansPosition`) ne lance
 * qu'une ouverture pour deux appuis (spec § 4 et § 6 : un contrôle par placette et par jour civil Europe/Paris ; § 4.3 « Arrivée
 * sans position », points 4 et 5). Écrit par le `testeur` depuis la spec, la fiche 100 et la signature décrite par le `dev`, sans
 * lire l'implémentation. L'état à confirmer vient de la règle de la 087 sur `BancControle` (P35-01, Couchey, en France).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = BancRendu.DEBOUT_PLEIN)
class RenduDoubleAppuiEcranControleTest {
    val regle = BancRendu.nouvelleRegle()

    @get:Rule val chaine: org.junit.rules.RuleChain = org.junit.rules.RuleChain.outerRule(BancRendu.surveillant()).around(regle)

    @After fun remettre() = BancRendu.fermer()

    private inner class Vue(val retenue: Boolean = true) {
        val banc = BancControle()
        val etat = assertIs<EtatArrivee.ConfirmerSansPosition>(
            ArriveeControle.ouvrirSansPosition(banc.contexte, banc.journal, banc.horloge, banc.essences, "P35-01", banc.sauvegarde),
        )
        var appels = 0
        val enAttente = ArrayList<CompletableDeferred<EtatArrivee>>()
        val etats = ArrayList<EtatArrivee>()
        var annulations = 0

        fun ouvrirReel(code: String = "P35-01"): EtatArrivee =
            ArriveeControle.ouvrirSansPosition(banc.contexte, banc.journal, banc.horloge, banc.essences, code, banc.sauvegarde, confirmee = true)

        val contenu: @Composable () -> Unit = {
            VueConfirmerSansPosition(
                etat = etat,
                ouvrir = {
                    appels++
                    if (retenue) CompletableDeferred<EtatArrivee>().also { enAttente += it }.await() else ouvrirReel()
                },
                onEtat = { etats += it },
                onAnnuler = { annulations++ },
            )
        }

        fun rendre() = BancRendu.rendre(contenu, regle = regle)
        fun bouton() = regle.onAllNodes(hasText(TextesControle.OUVRIR_SANS_POSITION) and hasClickAction())[0]
        fun appuyer() { bouton().performClick(); regle.waitForIdle() }
        fun doubleAppui() { bouton().performTouchInput { click(); click() }; regle.waitForIdle() }
        fun liberer(i: Int, e: EtatArrivee) { enAttente[i].complete(e); regle.waitForIdle() }
        fun libererTout() { for (i in enAttente.indices) if (!enAttente[i].isCompleted) liberer(i, ouvrirReel()) }
    }

    @Test
    fun `constat - deux appuis pendant une ouverture lente - appels a ouvrir et contrôles ouverts`() {
        val v = Vue()
        v.rendre()
        val plantage = runCatching { v.doubleAppui(); v.libererTout() }.exceptionOrNull()
        val ouverts = v.etats.filterIsInstance<EtatArrivee.Ouverte>()
        val lignes = ouverts.map { o -> runCatching { o.controleur.dire(EtatDsf.SAIN) }.fold({ "écrit" }, { it::class.simpleName }) }
        println(
            "CONSTAT 100 écran de contrôle : appels à ouvrir = ${v.appels}, onEtat = ${v.etats.size}, contrôles ouverts = ${ouverts.size}, " +
                "lignes totales = ${v.banc.journal.nombreDeLignes}, sauvegardes = ${v.banc.sauvegarde.saisies}, plantage = $plantage, $lignes",
        )
    }

    @Test
    fun `l'ecran de controle - un appui ouvre une fois et rend l'etat a onEtat`() {
        val v = Vue(retenue = false)
        v.rendre()
        v.appuyer()
        assertEquals(1, v.appels)
        assertEquals(1, v.etats.filterIsInstance<EtatArrivee.Ouverte>().size)
    }

    @Test
    fun `l'ecran de controle - deux appuis en un geste pendant une ouverture lente - un seul appel a ouvrir`() {
        val v = Vue()
        v.rendre()
        v.doubleAppui()
        assertEquals(1, v.appels, "une seule ouverture lancée")
        v.libererTout()
        assertEquals(1, v.etats.size, "un seul état rendu")
    }

    @Test
    fun `l'ecran de controle - appuis successifs pendant l'ouverture - le bouton ne repond plus`() {
        val v = Vue()
        v.rendre()
        v.appuyer(); v.appuyer(); v.appuyer()
        assertEquals(1, v.appels, "le bouton ne répond plus pendant l'ouverture")
        assertEquals(emptyList(), v.etats, "rien n'est rendu avant la fin de l'ouverture")
    }

    @Test
    fun `l'ecran de controle - deux appuis - un seul controle ouvert, une seule ecriture, aucun plantage`() {
        val v = Vue()
        v.rendre()
        v.doubleAppui()
        v.libererTout()
        val ouverts = v.etats.filterIsInstance<EtatArrivee.Ouverte>()
        assertEquals(1, ouverts.size, "un seul contrôle ouvert")
        ouverts.forEach { it.controleur.dire(EtatDsf.SAIN) }
        val seul = BancControle()
        (ArriveeControle.ouvrirSansPosition(seul.contexte, seul.journal, seul.horloge, seul.essences, "P35-01", seul.sauvegarde, confirmee = true) as EtatArrivee.Ouverte)
            .controleur.dire(EtatDsf.SAIN)
        assertEquals(seul.journal.nombreDeLignes, v.banc.journal.nombreDeLignes, "lignes écrites, pour un seul contrôle")
        assertEquals(seul.sauvegarde.saisies, v.banc.sauvegarde.saisies, "sauvegardes, pour un seul contrôle")
    }

    @Test
    fun `l'ecran de controle - apres un refus, le bouton repond de nouveau`() {
        val v = Vue()
        v.rendre()
        v.appuyer(); v.appuyer()
        assertEquals(1, v.appels)
        val refus = assertIs<EtatArrivee.Refus>(v.ouvrirReel("ZZZ"))
        v.liberer(0, refus)
        assertEquals(listOf<EtatArrivee>(refus), v.etats, "le refus est rendu à onEtat")
        v.bouton().assertIsEnabled()
        v.appuyer()
        assertEquals(2, v.appels, "après le refus, un nouvel appui relance l'ouverture")
    }

    @Test
    fun `l'ecran de controle - apres un refus, un nouvel appui qui aboutit n'ouvre qu'un controle`() {
        val v = Vue()
        v.rendre()
        v.appuyer()
        v.liberer(0, assertIs<EtatArrivee.Refus>(v.ouvrirReel("ZZZ")))
        v.doubleAppui()
        assertEquals(2, v.appels)
        v.liberer(1, v.ouvrirReel())
        assertTrue(v.etats.last() is EtatArrivee.Ouverte)
        assertEquals(1, v.etats.filterIsInstance<EtatArrivee.Ouverte>().size)
    }

    @Test
    fun `l'ecran de controle - Annuler reste possible et n'ouvre rien`() {
        val v = Vue()
        v.rendre()
        regle.onAllNodes(hasText(TextesControle.ANNULER) and hasClickAction())[0].performClick()
        regle.waitForIdle()
        assertEquals(1, v.annulations)
        assertEquals(0, v.appels)
        assertEquals(0, v.banc.journal.nombreDeLignes)
    }
}
