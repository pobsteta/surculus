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
    @get:Rule val surveille = BancRendu.surveillant()

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
    private fun verifier(cas: String, r: BancRendu.Resultat, modePocheAttendu: Boolean): List<String> {
        val defauts = ArrayList<String>()
        r.cliquables.filterNot { BancRendu.entier(r, it) }.forEach { defauts += "cliquable « ${it.nom} » ${BancRendu.fmt(it.rect)} déborde de la racine ${BancRendu.fmt(r.racine)}" }
        defauts += BancRendu.recouvrements(r)
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

    // --- le bloc d'écoute : ne peut recouvrir que « Mode poche », pendant l'écoute et DUREE_BANDEAU_MS après, plus ensuite (mission 076)

    private class Ecoute {
        var t = 0L
        val pilote = PilotageEcoute(MicroFaux(), SortiesPoche(VoixFausse(), VibreurFaux()), { t }, { true }, { EtatPermissionMicro.ACCORDEE })
            .also { it.surPhrase = {} }
    }

    /**
     * DÉFAUT CONNU (2026-10-30, décision de Pascal « corriger l'écran, test strict ») : debout, le bloc d'écoute, posé par-dessus le
     * haut de la zone, recouvre la flèche « Plant précédent » pendant l'écoute et les 6 s qui suivent. La mission 083 corrige l'écran.
     * Le test ne l'ignore pas : il l'écrit en tête du rapport et **casse s'il ne le voit plus** (levée de la marque à la fusion de la 083).
     */
    private val CLIQUABLES_DU_DEFAUT_083 = setOf("Plant précédent")
    private fun estDuBloc(nom: String) = nom.startsWith("ÉCOUTE") || nom.startsWith("J'ÉCOUTE")

    /** Seul « Mode poche » peut être recouvert par le bloc d'écoute, et seulement dans le délai (décision 2 de Pascal) ; le reste est un défaut. */
    private fun permises(defautConnu: Boolean): (BancRendu.Noeud, BancRendu.Noeud) -> String? = { c, n ->
        when {
            !estDuBloc(n.nom) -> null
            c.nom == MODE_POCHE -> "permis : le bloc d'écoute sur « Mode poche », pendant l'écoute et jusqu'à ${DUREE_BANDEAU_MS} ms après"
            defautConnu && c.nom in CLIQUABLES_DU_DEFAUT_083 -> "DÉFAUT CONNU 2026-10-30, mission 083 : le bloc d'écoute recouvre « ${c.nom} »"
            else -> null
        }
    }

    private fun enteteConnu(defautConnu: Boolean) =
        if (defautConnu) listOf("DÉFAUT CONNU (2026-10-30, mission 083 la corrige) : debout, le bloc d'écoute recouvre « Plant précédent » ; le test casse s'il ne le voit plus.") else emptyList()

    /** Une phase dans le délai : rend, juge, écrit le rapport ; ajoute les défauts à [defauts]. */
    private fun phaseDansLeDelai(cas: String, r: BancRendu.Resultat, grand: String, defautConnu: Boolean, defauts: MutableList<String>) {
        val toleres = ArrayList<String>()
        if (r.noeuds.none { it.nom == grand }) defauts += "« $grand » n'est pas affiché dans la phase « $cas »"
        defauts += BancRendu.recouvrements(r, permises(defautConnu), toleres)
        if (defautConnu && toleres.none { it.contains("DÉFAUT CONNU") }) {
            defauts += "le défaut connu de la mission 083 n'est plus vu dans « $cas » : lever la marque (CLIQUABLES_DU_DEFAUT_083) à la fusion de la 083"
        }
        BancRendu.rapport(cas, r, defauts, toleres, enteteConnu(defautConnu))
    }

    private fun ecoute(cas: String, defautConnu: Boolean) {
        val e = Ecoute()
        EcouteApp.poserPilotePourTest(e.pilote)
        val defauts = ArrayList<String>()

        // Un seul écran pour les quatre instants : l'écoute change, l'écran se redessine (comme sur le téléphone), l'arbre est relu.
        // Fermer et rouvrir l'écran ferait oublier l'écoute (`arreterEtOublier`), ce n'est pas la situation à juger.
        e.pilote.tenir(); e.t = 300; e.pilote.moteurPret(1)
        assertEquals(AffichageEcoute.ECOUTE, e.pilote.affichage)
        val pendant = BancRendu.rendre(session(), regle = regle)
        phaseDansLeDelai("$cas-pendant-l-ecoute", pendant, "J'ÉCOUTE", defautConnu, defauts)

        // L'écoute est lâchée à 2 s : « ÉCOUTE FERMÉE » est affiché, l'échéance part de la fermeture.
        regle.runOnIdle { e.t = 2_000; e.pilote.relacher() }
        regle.waitForIdle()
        val fermeeA = e.t
        assertEquals(AffichageEcoute.FERMEE, e.pilote.affichage)

        // Dans le délai : le bloc est là et ne peut recouvrir que « Mode poche ».
        regle.runOnIdle { e.t = fermeeA + DUREE_BANDEAU_MS - 1; e.pilote.avancer() }
        regle.waitForIdle()
        val dedans = BancRendu.lire(regle, pendant.densite)
        phaseDansLeDelai("$cas-dans-le-delai", dedans, "ÉCOUTE FERMÉE", defautConnu, defauts)

        // Après le délai : plus de bloc, et plus aucun recouvrement, connu ou non.
        regle.runOnIdle { e.t = fermeeA + DUREE_BANDEAU_MS; e.pilote.avancer() }
        regle.waitForIdle()
        val apres = BancRendu.lire(regle, pendant.densite)
        if (apres.noeuds.any { it.nom == "ÉCOUTE FERMÉE" }) defauts += "« ÉCOUTE FERMÉE » est encore affiché ${DUREE_BANDEAU_MS} ms après la fermeture : il cache « Mode poche »"
        defauts += BancRendu.recouvrements(apres)
        assertNotNull(BancRendu.cliquable(apres, MODE_POCHE), "« Mode poche » absent après le délai")
        BancRendu.rapport("$cas-apres-le-delai", apres, defauts)
        assertTrue(defauts.isEmpty(), "Rendu « $cas » :\n" + defauts.joinToString("\n"))
    }

    @Test @Config(qualifiers = BancRendu.COUCHE_PLEIN) fun `bloc d'ecoute couche, il s'efface apres DUREE_BANDEAU_MS`() = ecoute("ecoute-couche-plein", defautConnu = false)
    @Test @Config(qualifiers = BancRendu.DEBOUT_PLEIN) fun `bloc d'ecoute debout, il s'efface apres DUREE_BANDEAU_MS`() = ecoute("ecoute-debout-plein", defautConnu = true)
}
