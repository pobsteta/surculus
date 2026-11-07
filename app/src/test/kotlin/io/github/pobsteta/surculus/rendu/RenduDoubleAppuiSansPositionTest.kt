package io.github.pobsteta.surculus.rendu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import io.github.pobsteta.surculus.carte.GuidagePret
import io.github.pobsteta.surculus.controle.ArriveeControle
import io.github.pobsteta.surculus.controle.ArriveeSansPosition
import io.github.pobsteta.surculus.controle.BancControle
import io.github.pobsteta.surculus.controle.ControleursOuverts
import io.github.pobsteta.surculus.controle.EcouteComptee
import io.github.pobsteta.surculus.controle.EtatArrivee
import io.github.pobsteta.surculus.controle.HorlogeDeTest
import io.github.pobsteta.surculus.controle.JournalEnMemoire
import io.github.pobsteta.surculus.controle.SansPositionEnAttente
import io.github.pobsteta.surculus.controle.TextesControle
import io.github.pobsteta.surculus.core.placette.Essences
import io.github.pobsteta.surculus.core.placette.EtatDsf
import io.github.pobsteta.surculus.gpkg.PlacetteTournee
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
 * Mission 100 : un double appui sur « Ouvrir sans position » n'ouvre et n'écrit qu'un seul contrôle (spec § 4 et § 6 : un
 * contrôle par placette et par jour civil Europe/Paris dans une tournée ; § 4.3 « Arrivée sans position », points 4 et 5 : la
 * confirmation, puis l'ouverture). Écrit par le `testeur` depuis la spec et la fiche 100, sans lire `OuvertureSansPosition.kt`,
 * `EcranControle.kt`, `EcranGuidage.kt` ni `SessionControle.kt` ; le banc est celui de `RenduGuidageSansPositionTest` (089).
 *
 * Le « double appui » est joué en **un seul geste** (`click()` deux fois dans le même `performTouchInput`) : aucune
 * recomposition entre les deux appuis, comme deux doigts gantés qui arrivent presque ensemble.
 *
 * Le guidage est rendu tournée chargée (`GuidagePret`) ; l'ouverture est injectée et **comptée** ici. Les coordonnées sont celles
 * de `BancControle` : Lambert-93 (EPSG:2154), x = Est, y = Nord, mètres ; P35-01 est à Couchey (Côte-d'Or), en France.
 *
 * L'écran de contrôle est couvert par `RenduDoubleAppuiEcranControleTest` (vue de confirmation `VueConfirmerSansPosition`) :
 * `EcranControle` charge la tournée depuis un `travaux.gpkg` du disque, sans banc d'essai, d'où ce point d'entrée injectable.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = BancRendu.DEBOUT_PLEIN)
class RenduDoubleAppuiSansPositionTest {
    val regle = BancRendu.nouvelleRegle()

    @get:Rule val chaine: org.junit.rules.RuleChain = org.junit.rules.RuleChain.outerRule(BancRendu.surveillant()).around(regle)

    private val cle = ControleursOuverts.cle("tournee-essai", BancControle.P1.valeur)
    private val t0 = 1_000_000L
    private val sec = 1_000L

    private val p1 = PlacetteTournee(BancControle.P1.valeur, "P35-01", null, 3.99, null, null, BancControle.POSITION_P1)
    private val p2 = PlacetteTournee(BancControle.P2.valeur, "P35-02", null, 3.99, null, null, BancControle.POSITION_P2)

    /** Un appel à l'ouverture sans position, tel que le guidage le fait. */
    private data class Appel(val code: String, val ug: String?, val confirmee: Boolean)

    /**
     * Le guidage sans fix. Avec [retenue] vrai, chaque appel attend qu'on le libère (l'ouverture « en cours ») avec le résultat
     * choisi par le test ; sinon la règle de la 087 répond tout de suite.
     */
    private inner class Ecran(val retenue: Boolean = false) {
        val bancControle = BancControle()
        val journal = JournalEnMemoire()
        val sauvegarde = EcouteComptee()
        val maintenant: MutableLongState = mutableLongStateOf(t0)
        val ouvertures = ArrayList<String?>()
        val appels = ArrayList<Appel>()
        val ouverts = ArrayList<EtatArrivee.Ouverte>()
        val enAttente = ArrayList<CompletableDeferred<EtatArrivee>>()

        fun regleDe087(code: String, ug: String?, confirmee: Boolean): EtatArrivee =
            ArriveeControle.ouvrirSansPosition(bancControle.contexte, journal, HorlogeDeTest(), Essences(emptySet()), code, sauvegarde, ug, confirmee)

        val contenu: @Composable () -> Unit = {
            GuidagePret(
                cible = p1, toutes = listOf(p1, p2), fix = null, arriveeVue = false,
                onArrivee = { ouvertures += it }, onRearme = {}, cle = cle,
                ouvrirSansPosition = { code, ug, confirmee ->
                    appels += Appel(code, ug, confirmee)
                    val e = if (retenue) CompletableDeferred<EtatArrivee>().also { enAttente += it }.await() else regleDe087(code, ug, confirmee)
                    if (e is EtatArrivee.Ouverte) ouverts += e
                    e
                },
                suiteSansFix = { ms, recu -> ArriveeSansPosition.suite(ms, if (recu) 1 else 0, localisationActive = true, permissionAccordee = true) },
                maintenant = maintenant,
            )
        }

        fun rendre() = BancRendu.rendre(contenu, regle = regle)

        fun aLaSeconde(s: Long) {
            regle.runOnIdle { maintenant.longValue = t0 + s * sec }
            regle.waitForIdle()
        }

        fun saisir(code: String) {
            regle.onNode(hasSetTextAction()).performTextInput(code)
            regle.waitForIdle()
        }

        fun bouton() = regle.onAllNodes(hasText(TextesControle.OUVRIR_SANS_POSITION) and hasClickAction())[0]

        fun appuyer() {
            bouton().performClick()
            regle.waitForIdle()
        }

        /** Deux appuis dans le même geste : rien ne se recompose entre les deux. */
        fun doubleAppui() {
            bouton().performTouchInput { click(); click() }
            regle.waitForIdle()
        }

        fun liberer(i: Int, etat: EtatArrivee) {
            enAttente[i].complete(etat)
            regle.waitForIdle()
        }

        val confirmees get() = appels.count { it.confirmee }
        fun controles() = journal.lignesCourantes().count { it.type == "controle" }
    }

    private fun present(texte: String) = regle.onAllNodes(hasText(texte)).fetchSemanticsNodes().isNotEmpty()

    @After fun remettre() {
        BancRendu.fermer()
        ControleursOuverts.liberer(cle)
        SansPositionEnAttente.liberer(cle)
    }

    /** Amène le guidage à la confirmation de P35-01 (30 s sans fix, code saisi, un premier appui). */
    private fun Ecran.jusquALaConfirmation() {
        rendre()
        aLaSeconde(31)
        saisir("P35-01")
        appuyer()
        assertTrue(present(TextesControle.ANNULER), "la confirmation est affichée")
        assertEquals(0, journal.nombreDeLignes, "confirmer n'écrit rien")
    }

    /** Amène le guidage retenu à la confirmation : le premier appel (non confirmé) est libéré avec la règle de la 087. */
    private fun Ecran.jusquALaConfirmationRetenue() {
        rendre()
        aLaSeconde(31)
        saisir("P35-01")
        appuyer()
        liberer(0, regleDe087("P35-01", null, false))
        assertTrue(present(TextesControle.ANNULER), "la confirmation est affichée")
    }

    // ------------------------------------------------------------------ le constat d'aujourd'hui (ne juge pas : il dit)

    @Test
    fun `constat - deux appuis a la confirmation du guidage - ce qui est ouvert et ecrit`() {
        val g = Ecran()
        g.jusquALaConfirmation()
        val plantage = runCatching { g.doubleAppui() }.exceptionOrNull()
        val ecritures = ArrayList<String>()
        // Chaque contrôleur ouvert est utilisé comme le ferait l'écran qui l'aurait reçu ; une exception est notée, pas levée.
        g.ouverts.forEachIndexed { i, o ->
            ecritures += runCatching { o.controleur.dire(EtatDsf.SAIN) }.fold({ "contrôleur $i : écrit" }, { "contrôleur $i : ${it::class.simpleName} (${it.message})" })
        }
        println(
            "CONSTAT 100 guidage-confirmation : appels confirmés = ${g.confirmees}, contrôles ouverts = ${g.ouverts.size}, " +
                "onArrivee = ${g.ouvertures}, lignes controle au journal = ${g.controles()}, lignes totales = ${g.journal.nombreDeLignes}, " +
                "sauvegardes = ${g.sauvegarde.saisies}, plantage au geste = $plantage, $ecritures",
        )
    }

    // ------------------------------------------------------------------ deux appuis rapides
    //
    // L'ouverture se fait hors du fil de l'écran (entrées-sorties) : elle prend un instant. Les deux appuis arrivent pendant cet
    // instant : l'ouverture est donc « retenue » jusqu'à ce que le test la libère (chaque appel obtient la réponse de la règle de la 087).

    /** Deux appuis en un geste sur la confirmation, puis chaque ouverture lancée aboutit. */
    private fun Ecran.doubleAppuiALaConfirmation() {
        jusquALaConfirmationRetenue()
        doubleAppui()
        for (i in enAttente.indices) if (!enAttente[i].isCompleted) liberer(i, regleDe087("P35-01", null, true))
    }

    @Test
    fun `constat - deux appuis a la confirmation du guidage, ouverture qui prend un instant - ce qui est ouvert et ecrit`() {
        val g = Ecran(retenue = true)
        val plantage = runCatching { g.doubleAppuiALaConfirmation() }.exceptionOrNull()
        val ecritures = ArrayList<String>()
        g.ouverts.forEachIndexed { i, o ->
            ecritures += runCatching { o.controleur.dire(EtatDsf.SAIN) }.fold({ "contrôleur $i : écrit" }, { "contrôleur $i : ${it::class.simpleName} (${it.message})" })
        }
        println(
            "CONSTAT 100 guidage-confirmation-lente : appels confirmés = ${g.confirmees}, contrôles ouverts = ${g.ouverts.size}, " +
                "onArrivee = ${g.ouvertures}, lignes controle au journal = ${g.controles()}, lignes totales = ${g.journal.nombreDeLignes}, " +
                "sauvegardes = ${g.sauvegarde.saisies}, plantage = $plantage, $ecritures",
        )
    }

    @Test
    fun `deux appuis a la confirmation du guidage - un seul controle ouvert, une seule ouverture confiee a l'ecran de controle`() {
        val g = Ecran(retenue = true)
        g.doubleAppuiALaConfirmation()
        assertEquals(1, g.confirmees, "une seule ouverture demandée à la règle de la 087 : ${g.appels}")
        assertEquals(1, g.ouverts.size, "un seul contrôle ouvert")
        assertEquals(listOf<String?>("P35-01"), g.ouvertures, "l'écran de contrôle n'est averti qu'une fois")
        assertTrue(ControleursOuverts.get(cle) != null, "le contrôle ouvert est confié à l'écran de contrôle")
    }

    @Test
    fun `deux appuis a la confirmation du guidage - une seule ecriture au journal, meme si chaque controleur ouvert dit un etat`() {
        val g = Ecran(retenue = true)
        g.doubleAppuiALaConfirmation()
        // Tout contrôleur qui aurait été ouvert reçoit l'état : le journal ne doit porter qu'un contrôle.
        g.ouverts.forEach { runCatching { it.controleur.dire(EtatDsf.SAIN) } }
        assertEquals(1, g.controles(), "un seul contrôle pour (P35-01, ce jour)")
        // Une seule écriture : autant de lignes (et de sauvegardes) qu'un contrôle ouvert une seule fois puis rempli du même état.
        val seul = BancControle()
        (ArriveeControle.ouvrirSansPosition(seul.contexte, seul.journal, seul.horloge, Essences(emptySet()), "P35-01", seul.sauvegarde, confirmee = true) as EtatArrivee.Ouverte)
            .controleur.dire(EtatDsf.SAIN)
        assertEquals(seul.journal.nombreDeLignes, g.journal.nombreDeLignes, "lignes écrites au journal, pour un seul contrôle")
        assertEquals(seul.sauvegarde.saisies, g.sauvegarde.saisies, "sauvegardes déclenchées, pour un seul contrôle")
        assertEquals(0, g.journal.lignesCourantes().count { it.type == "annulation" })
        assertEquals(1, g.ouverts.size, "un seul contrôleur a pu écrire")
    }

    @Test
    fun `deux appuis a la confirmation du guidage - aucun plantage`() {
        val g = Ecran(retenue = true)
        g.doubleAppuiALaConfirmation()   // une exception ici fait échouer le test
        g.ouverts.forEach { it.controleur.dire(EtatDsf.SAIN) }   // et le contrôle ouvert se laisse remplir sans exception
        assertEquals(1, g.ouverts.size)
    }

    @Test
    fun `deux appuis a la confirmation, ouverture instantanee - un seul controle (le geste seul ne suffit pas a doubler)`() {
        val g = Ecran()
        g.jusquALaConfirmation()
        g.doubleAppui()
        assertEquals(1, g.ouverts.size)
        g.ouverts.forEach { it.controleur.dire(EtatDsf.SAIN) }
        assertEquals(1, g.controles())
    }

    @Test
    fun `deux appuis sur le premier Ouvrir sans position (champ rempli) - une seule confirmation, rien d'ouvert ni d'ecrit`() {
        val g = Ecran()
        g.rendre()
        g.aLaSeconde(31)
        g.saisir("P35-01")
        g.doubleAppui()
        assertTrue(present(TextesControle.ANNULER), "la confirmation est affichée")
        assertEquals(emptyList(), g.ouverts, "confirmer n'ouvre rien")
        assertEquals(emptyList(), g.ouvertures)
        assertEquals(0, g.journal.nombreDeLignes)
        assertTrue(g.appels.none { it.confirmee }, "personne n'a confirmé : ${g.appels}")
    }

    @Test
    fun `deux appuis puis un controle mene a bout - le journal ne garde qu'un controle pour la placette`() {
        val g = Ecran(retenue = true)
        g.doubleAppuiALaConfirmation()
        assertTrue(g.ouverts.isNotEmpty(), "au moins un contrôle ouvert")
        assertIs<EtatArrivee.Ouverte>(g.ouverts.first()).controleur.dire(EtatDsf.SAIN)
        assertEquals(1, g.controles())
        assertEquals(1, g.journal.lignesCourantes().filter { it.type == "controle" }.map { it.objetUuid }.distinct().size)
    }

    // ------------------------------------------------------------------ le bouton se tait pendant l'ouverture

    @Test
    fun `pendant l'ouverture - le bouton ne repond plus, un second appui ne lance rien`() {
        val g = Ecran(retenue = true)
        g.jusquALaConfirmationRetenue()
        g.appuyer()                       // lance l'ouverture, qui reste en cours
        assertEquals(1, g.confirmees, "l'ouverture est lancée")
        g.appuyer()                       // second appui, ouverture toujours en cours
        g.appuyer()
        assertEquals(1, g.confirmees, "le bouton ne répond plus pendant l'ouverture : ${g.appels}")
        assertEquals(emptyList(), g.ouvertures, "rien n'est encore confié à l'écran de contrôle")
        g.liberer(g.enAttente.lastIndex, g.regleDe087("P35-01", null, true))
        assertEquals(1, g.ouverts.size)
        assertEquals(listOf<String?>("P35-01"), g.ouvertures)
    }

    @Test
    fun `pendant l'ouverture - le double appui en un geste ne lance qu'une ouverture`() {
        val g = Ecran(retenue = true)
        g.jusquALaConfirmationRetenue()
        g.doubleAppui()
        assertEquals(1, g.confirmees, "une seule ouverture lancée : ${g.appels}")
    }

    // ------------------------------------------------------------------ après un refus, le bouton répond de nouveau

    @Test
    fun `apres un refus - le bouton repond de nouveau`() {
        val g = Ecran(retenue = true)
        g.rendre()
        g.aLaSeconde(31)
        g.saisir("ZZZ")
        g.appuyer()                       // lance la demande pour un code inconnu, en cours
        g.appuyer()                       // ignoré pendant la demande
        assertEquals(1, g.appels.size, "un seul appel pendant la demande : ${g.appels}")
        val refus = assertIs<EtatArrivee.Refus>(g.regleDe087("ZZZ", null, false))
        assertEquals("code_inconnu", refus.refus.code)
        g.liberer(0, refus)
        assertTrue(present(TextesControle.message(refus.refus)!!.texte), "le refus est affiché")
        g.appuyer()                       // le bouton répond de nouveau
        assertEquals(2, g.appels.size, "après le refus, un nouvel appui relance la demande : ${g.appels}")
        assertEquals(0, g.journal.nombreDeLignes)
    }

    @Test
    fun `apres un refus - le bouton est de nouveau actif`() {
        val g = Ecran(retenue = true)
        g.rendre()
        g.aLaSeconde(31)
        g.saisir("ZZZ")
        g.appuyer()
        g.liberer(0, g.regleDe087("ZZZ", null, false))
        g.bouton().assertIsEnabled()
    }
}
