package io.github.pobsteta.surculus.rendu

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import io.github.pobsteta.surculus.controle.AffichageEcoute
import io.github.pobsteta.surculus.controle.BancControle
import io.github.pobsteta.surculus.controle.DUREE_BANDEAU_MS
import io.github.pobsteta.surculus.controle.EcouteApp
import io.github.pobsteta.surculus.controle.EtatPermissionMicro
import io.github.pobsteta.surculus.controle.MoteurVoix
import io.github.pobsteta.surculus.controle.PilotageEcoute
import io.github.pobsteta.surculus.controle.PocheApp
import io.github.pobsteta.surculus.controle.PortMicro
import io.github.pobsteta.surculus.controle.RaisonSortie
import io.github.pobsteta.surculus.controle.SortiesPoche
import io.github.pobsteta.surculus.controle.Vibreur
import io.github.pobsteta.surculus.controle.VueSession
import io.github.pobsteta.surculus.tournee.DepotTournees
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Mission 080 : le contrôle d'une placette rendu à la taille du téléphone de Pascal, debout et couché, avec et sans plein écran,
 * en mode normal puis en mode poche, avec le bloc d'écoute (« J'ÉCOUTE », « ÉCOUTE FERMÉE ») pendant son temps puis après.
 * Échoue si un bouton est coupé ou recouvert, si la bande du mode poche sort de l'écran, ou si le bloc d'écoute reste
 * au-delà de `DUREE_BANDEAU_MS`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class RenduControleTest {
    @get:Rule val regle = createEmptyComposeRule()

    private class MicroFaux : PortMicro {
        override fun ouvrir(generation: Int) {}
        override fun fermer() {}
    }

    private class VoixFausse : MoteurVoix {
        override val francaisDisponible get() = true
        override var surFin: (() -> Unit)? = null
        override val parle get() = false
        override fun dire(texte: String): Boolean = true
        override fun arreter() {}
    }

    private class VibreurFaux : Vibreur {
        override fun vibrer(motif: LongArray) {}
    }

    @After fun remettre() {
        PocheApp.mode.sortir(RaisonSortie.VUE_QUITTEE)
        EcouteApp.poserPilotePourTest(null)
    }

    private fun session(): @androidx.compose.runtime.Composable () -> Unit {
        val controleur = BancControle().controleur()
        val reglages = DepotTournees.reglagesControle(RuntimeEnvironment.getApplication())
        return { VueSession(controleur, "couchey-essai-plants", reglages, stockageRefuse = false, onRetour = {}, onParametres = {}) }
    }

    private val MODE_POCHE = "Mode poche"

    /** Les contrôles communs : cliquables entiers, rien ne recouvre un cliquable, « Mode poche » là et entier (mode normal). */
    private fun verifier(cas: String, r: BancRendu.Resultat, modePocheAttendu: Boolean, permises: (BancRendu.Noeud, BancRendu.Noeud) -> Boolean = { _, _ -> false }): List<String> {
        val defauts = ArrayList<String>()
        r.cliquables.filterNot { BancRendu.entier(r, it) }.forEach { defauts += "cliquable « ${it.nom} » ${BancRendu.fmt(it.rect)} déborde de la racine ${BancRendu.fmt(r.racine)}" }
        defauts += BancRendu.recouvrements(r, permises)
        if (modePocheAttendu) {
            val b = BancRendu.cliquable(r, MODE_POCHE)
            if (b == null) defauts += "bouton « $MODE_POCHE » absent"
            else if (!BancRendu.vu(r, b)) defauts += "bouton « $MODE_POCHE » ${BancRendu.fmt(b.rect)} n'est pas vu en entier dans la racine ${BancRendu.fmt(r.racine)}"
        }
        BancRendu.rapport(cas, r, defauts)
        return defauts
    }

    private fun normal(cas: String) {
        val r = BancRendu.rendre(contenu = session(), regle = regle)
        val defauts = verifier(cas, r, modePocheAttendu = true)
        assertTrue(defauts.isEmpty(), "Rendu « $cas » :\n" + defauts.joinToString("\n"))
    }

    @Test @Config(qualifiers = BancRendu.COUCHE_PLEIN) fun `controle couche en plein ecran`() = normal("controle-couche-plein")
    @Test @Config(qualifiers = BancRendu.COUCHE_SANS_PLEIN) fun `controle couche sans plein ecran`() = normal("controle-couche-sans-plein")
    @Test @Config(qualifiers = BancRendu.DEBOUT_PLEIN) fun `controle debout en plein ecran`() = normal("controle-debout-plein")
    @Test @Config(qualifiers = BancRendu.DEBOUT_SANS_PLEIN) fun `controle debout sans plein ecran`() = normal("controle-debout-sans-plein")

    // --- mode poche : la bande reste entière même quand la zone a défilé (mission 067)

    private fun poche(cas: String) {
        PocheApp.mode.entrer(0L)
        val r = BancRendu.rendre(
            contenu = session(),
            apres = { onNode(hasScrollAction()).performTouchInput { swipeUp() } },
            regle = regle,
        )
        val defauts = verifier(cas, r, modePocheAttendu = false)
        val bande = r.noeuds.firstOrNull { it.nom == "MODE POCHE" }
        val ecrit = ArrayList(defauts)
        if (bande == null) ecrit += "la bande « MODE POCHE » est absente"
        else if (!BancRendu.vu(r, bande)) ecrit += "la bande « MODE POCHE » ${BancRendu.fmt(bande.rect)} n'est pas vue en entier dans la racine ${BancRendu.fmt(r.racine)} après défilement"
        BancRendu.rapport(cas, r, ecrit)
        assertTrue(ecrit.isEmpty(), "Rendu « $cas » :\n" + ecrit.joinToString("\n"))
    }

    @Test @Config(qualifiers = BancRendu.COUCHE_PLEIN) fun `mode poche couche en plein ecran, apres defilement`() = poche("poche-couche-plein")
    @Test @Config(qualifiers = BancRendu.COUCHE_SANS_PLEIN) fun `mode poche couche sans plein ecran, apres defilement`() = poche("poche-couche-sans-plein")
    @Test @Config(qualifiers = BancRendu.DEBOUT_PLEIN) fun `mode poche debout en plein ecran, apres defilement`() = poche("poche-debout-plein")

    // --- le bloc d'écoute : peut recouvrir « Mode poche » pendant l'écoute et DUREE_BANDEAU_MS après, plus ensuite (mission 076)

    private class Ecoute {
        var t = 0L
        val pilote = PilotageEcoute(MicroFaux(), SortiesPoche(VoixFausse(), VibreurFaux()), { t }, { true }, { EtatPermissionMicro.ACCORDEE })
            .also { it.surPhrase = {} }
    }

    private fun ecoute(cas: String) {
        val e = Ecoute()
        EcouteApp.poserPilotePourTest(e.pilote)
        // L'écoute est tenue 2 s puis lâchée : « ÉCOUTE FERMÉE » est affiché, l'échéance part de la fermeture.
        e.pilote.tenir(); e.t = 300; e.pilote.moteurPret(1); e.t = 2_000; e.pilote.relacher()
        val fermeeA = e.t
        assertEquals(AffichageEcoute.FERMEE, e.pilote.affichage)
        val defauts = ArrayList<String>()

        // Dans le délai : le bloc est là et peut recouvrir « Mode poche » ; le reste ne recouvre rien.
        e.t = fermeeA + DUREE_BANDEAU_MS - 1
        val dedans = BancRendu.rendre(session(), apres = { runOnIdle { e.pilote.avancer() } }, regle = regle)
        if (dedans.noeuds.none { it.nom == "ÉCOUTE FERMÉE" }) defauts += "« ÉCOUTE FERMÉE » n'est plus affiché ${DUREE_BANDEAU_MS - 1} ms après la fermeture (il doit l'être jusqu'à ${DUREE_BANDEAU_MS} ms)"
        defauts += BancRendu.recouvrements(dedans) { _, n -> n.nom.startsWith("ÉCOUTE") || n.nom.startsWith("J'ÉCOUTE") }
        BancRendu.rapport("$cas-dans-le-delai", dedans, defauts)

        // Après le délai : plus de bloc, et plus rien sur « Mode poche ».
        e.t = fermeeA + DUREE_BANDEAU_MS
        regle.runOnIdle { e.pilote.avancer() }
        regle.waitForIdle()
        val apres = BancRendu.lire(regle, dedans.densite)
        val restant = apres.noeuds.filter { it.nom == "ÉCOUTE FERMÉE" }
        if (restant.isNotEmpty()) defauts += "« ÉCOUTE FERMÉE » est encore affiché ${DUREE_BANDEAU_MS} ms après la fermeture : il cache « Mode poche »"
        defauts += BancRendu.recouvrements(apres)
        assertNotNull(BancRendu.cliquable(apres, MODE_POCHE), "« Mode poche » absent après le délai")
        BancRendu.rapport("$cas-apres-le-delai", apres, defauts)
        assertTrue(defauts.isEmpty(), "Rendu « $cas » :\n" + defauts.joinToString("\n"))
    }

    @Test @Config(qualifiers = BancRendu.COUCHE_PLEIN) fun `bloc d'ecoute couche, il s'efface apres DUREE_BANDEAU_MS`() = ecoute("ecoute-couche-plein")
    @Test @Config(qualifiers = BancRendu.DEBOUT_PLEIN) fun `bloc d'ecoute debout, il s'efface apres DUREE_BANDEAU_MS`() = ecoute("ecoute-debout-plein")
}
