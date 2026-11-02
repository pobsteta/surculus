package io.github.pobsteta.surculus.rendu

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import io.github.pobsteta.surculus.controle.AffichageEcoute
import io.github.pobsteta.surculus.controle.AnnoncePoche
import io.github.pobsteta.surculus.controle.MotifsVibration
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
    val regle = BancRendu.nouvelleRegle()

    /** Le garde-fou entoure la règle Compose : ses piles couvrent aussi l'installation et le démontage de celle-ci. */
    @get:Rule val chaine: org.junit.rules.RuleChain = org.junit.rules.RuleChain.outerRule(BancRendu.surveillant()).around(regle)

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
        BancRendu.fermer()
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

    private fun estDuBloc(nom: String) = nom.startsWith("ÉCOUTE") || nom.startsWith("J'ÉCOUTE") ||
        // les lignes de la dictée font partie du bloc d'écoute (même permission : seul « Mode poche » peut être recouvert)
        nom.startsWith("entendu : ") || nom.startsWith("compris : ") || nom.startsWith("refusé : ")

    /** Seul « Mode poche » peut être recouvert par le bloc d'écoute, et seulement dans le délai (décision 2 de Pascal) ; le reste est un défaut. */
    private fun permises(): (BancRendu.Noeud, BancRendu.Noeud) -> String? = { c, n ->
        when {
            !estDuBloc(n.nom) -> null
            c.nom == MODE_POCHE -> "permis : le bloc d'écoute sur « Mode poche », pendant l'écoute et jusqu'à ${DUREE_BANDEAU_MS} ms après"
            else -> null
        }
    }

    /** Une phase dans le délai : rend, juge, écrit le rapport ; ajoute les défauts à [defauts]. */
    private fun phaseDansLeDelai(cas: String, r: BancRendu.Resultat, grand: String, defauts: MutableList<String>) {
        val toleres = ArrayList<String>()
        if (r.noeuds.none { it.nom == grand }) defauts += "« $grand » n'est pas affiché dans la phase « $cas »"
        defauts += BancRendu.recouvrements(r, permises(), toleres)
        BancRendu.rapport(cas, r, defauts, toleres)
    }

    private fun ecoute(cas: String) {
        val e = Ecoute()
        EcouteApp.poserPilotePourTest(e.pilote)
        val defauts = ArrayList<String>()

        // Un seul écran pour les quatre instants : l'écoute change, l'écran se redessine (comme sur le téléphone), l'arbre est relu.
        // Fermer et rouvrir l'écran ferait oublier l'écoute (`arreterEtOublier`), ce n'est pas la situation à juger.
        e.pilote.tenir(); e.t = 300; e.pilote.moteurPret(1)
        assertEquals(AffichageEcoute.ECOUTE, e.pilote.affichage)
        val pendant = BancRendu.rendre(session(), regle = regle)
        phaseDansLeDelai("$cas-pendant-l-ecoute", pendant, "J'ÉCOUTE", defauts)

        // L'écoute est lâchée à 2 s : « ÉCOUTE FERMÉE » est affiché, l'échéance part de la fermeture.
        regle.runOnIdle { e.t = 2_000; e.pilote.relacher() }
        regle.waitForIdle()
        val fermeeA = e.t
        assertEquals(AffichageEcoute.FERMEE, e.pilote.affichage)

        // Dans le délai : le bloc est là et ne peut recouvrir que « Mode poche ».
        regle.runOnIdle { e.t = fermeeA + DUREE_BANDEAU_MS - 1; e.pilote.avancer() }
        regle.waitForIdle()
        val dedans = BancRendu.lire(regle, pendant.densite)
        phaseDansLeDelai("$cas-dans-le-delai", dedans, "ÉCOUTE FERMÉE", defauts)

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

    @Test @Config(qualifiers = BancRendu.COUCHE_PLEIN) fun `bloc d'ecoute couche, il s'efface apres DUREE_BANDEAU_MS`() = ecoute("ecoute-couche-plein")
    @Test @Config(qualifiers = BancRendu.DEBOUT_PLEIN) fun `bloc d'ecoute debout, il s'efface apres DUREE_BANDEAU_MS`() = ecoute("ecoute-debout-plein")

    // --- mission 083 (reprise 1/3) : après une phrase et son résultat, « entendu » et « compris » restent entiers pendant le délai

    /**
     * L'écoute reçoit une phrase entendue et un résultat (compris, ou refusé si [erreur]) ; l'écran est relu à `DUREE_BANDEAU_MS - 1`
     * après la fermeture. Les deux lignes doivent y être, non nulles, sans texte tronqué.
     */
    private fun phraseEtResultat(cas: String, erreur: Boolean, defauts: MutableList<String>, apresLecture: (Ecoute, BancRendu.Resultat) -> Unit = { _, _ -> }) {
        val e = Ecoute()
        EcouteApp.poserPilotePourTest(e.pilote)
        e.pilote.tenir(); e.t = 300; e.pilote.moteurPret(1)
        val depart = BancRendu.rendre(session(), regle = regle)
        regle.runOnIdle { e.t = 1_500; e.pilote.phrase("sept sain") }
        regle.runOnIdle { e.t = 2_000; e.pilote.relacher() }
        regle.waitForIdle()
        val fermeeA = e.t
        assertNotNull(e.pilote.entendu, "la phrase n'a pas été entendue par l'écoute")
        val texteCompris = if (erreur) "refusé : numéro hors plantation" else "compris : numéro 7, sain"
        regle.runOnIdle {
            e.pilote.resultat(if (erreur) AnnoncePoche("numéro hors plantation", MotifsVibration.REFUS, erreur = true) else AnnoncePoche("numéro 7, sain", MotifsVibration.SAIN))
        }
        regle.waitForIdle()
        regle.runOnIdle { e.t = fermeeA + DUREE_BANDEAU_MS - 1; e.pilote.avancer() }
        regle.waitForIdle()
        val r = BancRendu.lire(regle, depart.densite)
        val toleres = ArrayList<String>()
        if (r.noeuds.none { it.nom == "ÉCOUTE FERMÉE" }) defauts += "« ÉCOUTE FERMÉE » n'est pas affiché dans le délai"
        for ((prefixe, attendu) in listOf("entendu" to "entendu : « sept sain »", "compris" to texteCompris)) {
            val motif = if (erreur && prefixe == "compris") "refusé" else prefixe
            val n = r.noeuds.firstOrNull { it.texte && it.nom.startsWith(motif) }
            if (n == null) { defauts += "la ligne « $attendu » est absente de l'arbre"; continue }
            if (n.rect.width <= 0f || n.rect.height <= 0f) defauts += "la ligne « ${n.nom} » est affichée ${BancRendu.fmt(n.rect)} : largeur ou hauteur nulle"
            if (!BancRendu.entier(r, n)) defauts += "la ligne « ${n.nom} » ${BancRendu.fmt(n.rect)} déborde de la racine ${BancRendu.fmt(r.racine)}"
            val sn = regle.onNode(hasText(n.nom)).fetchSemanticsNode()
            val mesure = sn.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action
            val lignes = ArrayList<androidx.compose.ui.text.TextLayoutResult>()
            if (mesure?.invoke(lignes) == true) lignes.firstOrNull()?.let { l ->
                val tronquee = (0 until l.lineCount).any { k -> l.isLineEllipsized(k) } || l.getLineEnd(l.lineCount - 1, visibleEnd = true) < l.layoutInput.text.length
                if (l.didOverflowHeight || tronquee) defauts += "la ligne « ${n.nom} » est coupée (boîte ${BancRendu.fmt(n.rect)}, ${l.lineCount} ligne(s), tronqué=$tronquee, débord visuel=${l.hasVisualOverflow}, hauteur=${l.didOverflowHeight}, largeur=${l.didOverflowWidth}, texte ${"%.0f".format(l.size.height / r.densite)} dp)"
                else if (n.rect.height * r.densite < l.size.height - 1f) defauts += "la ligne « ${n.nom} » est plus basse que son texte"
            }
        }
        defauts += BancRendu.recouvrements(r, permises(), toleres)
        defauts += cadreDuBloc(r)
        BancRendu.rapport(cas, r, defauts, toleres)
        apresLecture(e, r)
    }

    /**
     * Le banc ne mesure que les nœuds de texte : le cadre du bloc (sa marge) n'est exposé par aucun nœud ni testTag. On le suit donc
     * par approximation : le rectangle des lignes du bloc (non nulles) élargi de [MARGE_CADRE_DP] (la marge mesurée par le relecteur,
     * 12 dp) ne doit toucher ni « Plant précédent » ni le numéro du plant (« Plant n° … »).
     */
    private val MARGE_CADRE_DP = 12f

    private fun cadreDuBloc(r: BancRendu.Resultat): List<String> {
        val lignes = r.noeuds.filter { it.texte && estDuBloc(it.nom) && BancRendu.aire(it.rect) > 0f }
        if (lignes.isEmpty()) return emptyList()
        val m = MARGE_CADRE_DP
        val cadre = androidx.compose.ui.geometry.Rect(lignes.minOf { it.rect.left } - m, lignes.minOf { it.rect.top } - m, lignes.maxOf { it.rect.right } + m, lignes.maxOf { it.rect.bottom } + m)
        val gardes = r.noeuds.filter { it.nom == "Plant précédent" || it.nom.startsWith("Plant n°") }
        return gardes.filter { BancRendu.aire(it.rect) > 0f && BancRendu.intersection(cadre, it.rect) > 1f }
            .map { "le cadre du bloc d'écoute (lignes + ${m.toInt()} dp) ${BancRendu.fmt(cadre)} recouvre « ${it.nom} » ${BancRendu.fmt(it.rect)}" }
    }

    private fun phrase(cas: String, erreur: Boolean = false) {
        val defauts = ArrayList<String>()
        phraseEtResultat(cas, erreur, defauts)
        assertTrue(defauts.isEmpty(), "Rendu « $cas » :\n" + defauts.joinToString("\n"))
    }

    @Test @Config(qualifiers = BancRendu.DEBOUT_PLEIN) fun `apres une phrase compris, debout plein ecran, entendu et compris entiers`() = phrase("phrase-compris-debout-plein")
    @Test @Config(qualifiers = BancRendu.DEBOUT_PLEIN) fun `apres une phrase refusee, debout plein ecran, entendu et refus entiers`() = phrase("phrase-refus-debout-plein", erreur = true)
    @Test @Config(qualifiers = BancRendu.DEBOUT_SANS_PLEIN) fun `apres une phrase compris, debout sans plein ecran, entendu et compris entiers`() = phrase("phrase-compris-debout-sans-plein")
    @Test @Config(qualifiers = BancRendu.COUCHE_PLEIN) fun `apres une phrase compris, couche plein ecran, entendu et compris entiers`() = phrase("phrase-compris-couche-plein")
    @Test @Config(qualifiers = BancRendu.COUCHE_PLEIN) fun `apres une phrase refusee, couche plein ecran, entendu et refus entiers`() = phrase("phrase-refus-couche-plein", erreur = true)
    @Test @Config(qualifiers = BancRendu.COUCHE_SANS_PLEIN) fun `apres une phrase compris, couche sans plein ecran, entendu et compris entiers`() = phrase("phrase-compris-couche-sans-plein")

    // --- décision « Rien » : un appui sur le bloc d'écoute n'est pas transmis aux boutons dessous

    private fun appuiSurLeBloc(cas: String) {
        val defauts = ArrayList<String>()
        phraseEtResultat(cas, false, defauts) { e, avant ->
            val bloc = avant.noeuds.filter { it.texte && (it.nom == "ÉCOUTE FERMÉE" || it.nom.startsWith("entendu") || it.nom.startsWith("compris")) }
            if (bloc.isEmpty()) { defauts += "aucune ligne du bloc d'écoute à toucher"; return@phraseEtResultat }
            val d = avant.densite
            val gauche = bloc.minOf { it.rect.left }; val haut = bloc.minOf { it.rect.top }
            val droite = bloc.maxOf { it.rect.right }; val bas = bloc.maxOf { it.rect.bottom }
            val points = listOf(Offset((gauche + droite) / 2 * d, (haut + bas) / 2 * d)) +
                bloc.map { Offset((it.rect.left + it.rect.right) / 2 * d, (it.rect.top + it.rect.bottom) / 2 * d) }
            val avantNoms = avant.noeuds.map { it.nom }.sorted()
            val avantAffichage = e.pilote.affichage
            for (p in points) {
                regle.onRoot().performTouchInput { click(p) }
                regle.waitForIdle()
            }
            val apres = BancRendu.lire(regle, d)
            if (PocheApp.mode.actif) defauts += "un appui sur le bloc d'écoute a déclenché « Mode poche »"
            if (apres.noeuds.map { it.nom }.sorted() != avantNoms) defauts += "un appui sur le bloc d'écoute a changé l'écran (nœuds avant/après différents)"
            if (e.pilote.affichage != avantAffichage) defauts += "un appui sur le bloc d'écoute a changé l'état de l'écoute"
        }
        assertTrue(defauts.isEmpty(), "Rendu « $cas » :\n" + defauts.joinToString("\n"))
    }

    @Test @Config(qualifiers = BancRendu.DEBOUT_PLEIN) fun `appui sur le bloc d'ecoute debout, rien ne se declenche dessous`() = appuiSurLeBloc("appui-bloc-debout-plein")
    @Test @Config(qualifiers = BancRendu.COUCHE_PLEIN) fun `appui sur le bloc d'ecoute couche, rien ne se declenche dessous`() = appuiSurLeBloc("appui-bloc-couche-plein")
}
