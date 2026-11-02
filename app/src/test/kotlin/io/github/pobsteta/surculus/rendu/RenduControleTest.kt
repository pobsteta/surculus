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
import io.github.pobsteta.surculus.core.dictee.Grammaire
import io.github.pobsteta.surculus.core.session.TextesDictee
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
    private fun phraseEtResultat(
        cas: String,
        erreur: Boolean,
        defauts: MutableList<String>,
        dite: String = "sept sain",
        annonce: String = if (erreur) "numéro hors plantation" else "numéro 7, sain",
        apresLecture: (Ecoute, BancRendu.Resultat) -> Unit = { _, _ -> },
    ) {
        val e = Ecoute()
        EcouteApp.poserPilotePourTest(e.pilote)
        e.pilote.tenir(); e.t = 300; e.pilote.moteurPret(1)
        val depart = BancRendu.rendre(session(), regle = regle)
        regle.runOnIdle { e.t = 1_500; e.pilote.phrase(dite) }
        regle.runOnIdle { e.t = 2_000; e.pilote.relacher() }
        regle.waitForIdle()
        val fermeeA = e.t
        assertNotNull(e.pilote.entendu, "la phrase n'a pas été entendue par l'écoute")
        val texteCompris = (if (erreur) "refusé : " else "compris : ") + annonce
        regle.runOnIdle {
            e.pilote.resultat(if (erreur) AnnoncePoche(annonce, MotifsVibration.REFUS, erreur = true) else AnnoncePoche(annonce, MotifsVibration.SAIN))
        }
        regle.waitForIdle()
        regle.runOnIdle { e.t = fermeeA + DUREE_BANDEAU_MS - 1; e.pilote.avancer() }
        regle.waitForIdle()
        val r = BancRendu.lire(regle, depart.densite)
        val toleres = ArrayList<String>()
        if (r.noeuds.none { it.nom == "ÉCOUTE FERMÉE" }) defauts += "« ÉCOUTE FERMÉE » n'est pas affiché dans le délai"
        for ((prefixe, attendu) in listOf("entendu" to "entendu : « $dite »", "compris" to texteCompris)) {
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
        phraseEtResultat(cas, false, defauts, apresLecture = { e, avant ->
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
        })
        assertTrue(defauts.isEmpty(), "Rendu « $cas » :\n" + defauts.joinToString("\n"))
    }

    @Test @Config(qualifiers = BancRendu.DEBOUT_PLEIN) fun `appui sur le bloc d'ecoute debout, rien ne se declenche dessous`() = appuiSurLeBloc("appui-bloc-debout-plein")
    @Test @Config(qualifiers = BancRendu.COUCHE_PLEIN) fun `appui sur le bloc d'ecoute couche, rien ne se declenche dessous`() = appuiSurLeBloc("appui-bloc-couche-plein")

    // --- mission 083 (reprise 2/3) : la commande la plus longue de la grammaire fermée et le refus le plus long

    /** La commande la plus longue (en caractères) de la grammaire fermée de la dictée (spec, « Dictée hors ligne à grammaire fermée »). */
    private val COMMANDE_LA_PLUS_LONGUE: String = Grammaire.phrases.maxByOrNull { it.length }!!

    /** Mesurée par le relecteur de la PR #148 : une phrase qui enchaîne état, cause et hauteur (l'API de l'écoute accepte tout texte). */
    private val PHRASE_MESUREE_PAR_LE_RELECTEUR = "atteint insectes hauteur quarante-cinq"

    /**
     * Le refus le plus long que la dictée peut produire : l'alerte « Placette déjà terminée. » (qui précède tout refus compris) devant
     * le plus long texte de refus de [TextesDictee].
     */
    private val REFUS_LE_PLUS_LONG: String = TextesDictee.DEJA_TERMINEE + listOf(
        TextesDictee.HAUTEUR_SANS_ETAT, TextesDictee.HAUTEUR_ABSENT, TextesDictee.HAUTEUR_NON_OBSERVE, TextesDictee.HAUTEUR_HORS_BORNES,
        TextesDictee.VIDE, TextesDictee.CAUSE_SUR_SAIN, TextesDictee.CAUSE_SUR_ABSENT, TextesDictee.CAUSE_SUR_NON_OBSERVE,
        TextesDictee.ETAT_D_ABORD, TextesDictee.MANGE_REFUSE, TextesDictee.REMPLACE_REFUSE, TextesDictee.AUCUN_PLANT,
        TextesDictee.REFUS_SESSION, TextesDictee.RIEN_A_ANNULER,
    ).maxByOrNull { it.length }!!

    private val COMPRIS_LE_PLUS_LONG = TextesDictee.hauteurDite(7, 399)

    private fun longue(cas: String, dite: String, erreur: Boolean = false) {
        val defauts = ArrayList<String>()
        phraseEtResultat(cas, erreur, defauts, dite = dite, annonce = if (erreur) REFUS_LE_PLUS_LONG else COMPRIS_LE_PLUS_LONG)
        assertTrue(defauts.isEmpty(), "Rendu « $cas » (dite « $dite ») :\n" + defauts.joinToString("\n"))
    }

    @Test @Config(qualifiers = BancRendu.DEBOUT_PLEIN) fun `commande la plus longue, debout plein ecran, entendu et compris entiers`() = longue("longue-compris-debout-plein", COMMANDE_LA_PLUS_LONGUE)
    @Test @Config(qualifiers = BancRendu.DEBOUT_SANS_PLEIN) fun `commande la plus longue, debout sans plein ecran, entendu et compris entiers`() = longue("longue-compris-debout-sans-plein", COMMANDE_LA_PLUS_LONGUE)
    @Test @Config(qualifiers = BancRendu.COUCHE_PLEIN) fun `commande la plus longue, couche plein ecran, entendu et compris entiers`() = longue("longue-compris-couche-plein", COMMANDE_LA_PLUS_LONGUE)
    @Test @Config(qualifiers = BancRendu.COUCHE_SANS_PLEIN) fun `commande la plus longue, couche sans plein ecran, entendu et compris entiers`() = longue("longue-compris-couche-sans-plein", COMMANDE_LA_PLUS_LONGUE)

    @Test @Config(qualifiers = BancRendu.DEBOUT_PLEIN) fun `phrase enchainee du relecteur, debout plein ecran, entendu et compris entiers`() = longue("enchainee-compris-debout-plein", PHRASE_MESUREE_PAR_LE_RELECTEUR)
    @Test @Config(qualifiers = BancRendu.DEBOUT_SANS_PLEIN) fun `phrase enchainee du relecteur, debout sans plein ecran, entendu et compris entiers`() = longue("enchainee-compris-debout-sans-plein", PHRASE_MESUREE_PAR_LE_RELECTEUR)
    @Test @Config(qualifiers = BancRendu.COUCHE_PLEIN) fun `phrase enchainee du relecteur, couche plein ecran, entendu et compris entiers`() = longue("enchainee-compris-couche-plein", PHRASE_MESUREE_PAR_LE_RELECTEUR)
    @Test @Config(qualifiers = BancRendu.COUCHE_SANS_PLEIN) fun `phrase enchainee du relecteur, couche sans plein ecran, entendu et compris entiers`() = longue("enchainee-compris-couche-sans-plein", PHRASE_MESUREE_PAR_LE_RELECTEUR)

    @Test @Config(qualifiers = BancRendu.DEBOUT_PLEIN) fun `refus le plus long, debout plein ecran, entendu et refus entiers`() = longue("longue-refus-debout-plein", COMMANDE_LA_PLUS_LONGUE, erreur = true)
    @Test @Config(qualifiers = BancRendu.DEBOUT_SANS_PLEIN) fun `refus le plus long, debout sans plein ecran, entendu et refus entiers`() = longue("longue-refus-debout-sans-plein", COMMANDE_LA_PLUS_LONGUE, erreur = true)
    @Test @Config(qualifiers = BancRendu.COUCHE_PLEIN) fun `refus le plus long, couche plein ecran, entendu et refus entiers`() = longue("longue-refus-couche-plein", COMMANDE_LA_PLUS_LONGUE, erreur = true)
    @Test @Config(qualifiers = BancRendu.COUCHE_SANS_PLEIN) fun `refus le plus long, couche sans plein ecran, entendu et refus entiers`() = longue("longue-refus-couche-sans-plein", COMMANDE_LA_PLUS_LONGUE, erreur = true)

    // --- décision « Oui, et le figer » : zone défilée jusqu'en bas, le bloc ne couvre que le haut d'un bouton du pied, d'une hauteur bornée

    /**
     * Borne du texte du bloc sur un bouton du pied : le relecteur a mesuré 9 dp de texte sur « Installer la dictée » ; 12 dp laissent 3 dp
     * (un tiers) de marge pour l'arrondi de la mise en page, pas pour une ligne de texte de plus (17 dp debout selon le corps de PR).
     */
    private val BORNE_TEXTE_SUR_BOUTON_DP = 12f

    /**
     * Borne du cadre (lignes + [MARGE_CADRE_DP]) sur un bouton du pied : le relecteur a mesuré 21 dp avec le cadre sur « Installer la
     * dictée » ; 24 dp laissent 3 dp de marge, soit moins de la moitié d'une cible de 48 dp : le bouton reste atteignable par son bas.
     */
    private val BORNE_CADRE_SUR_BOUTON_DP = 24f

    private fun estBoutonDuPied(nom: String) = nom.startsWith("+ Pointer") || nom == "Annuler" || nom.startsWith("Installer la dictée")

    /** Profondeur (dp) dont [bloc] recouvre le haut de [c] ; 0 s'il ne le touche pas. */
    private fun profondeur(bloc: androidx.compose.ui.geometry.Rect, c: androidx.compose.ui.geometry.Rect): Float =
        if (BancRendu.intersection(bloc, c) > 1f) minOf(bloc.bottom, c.bottom) - maxOf(bloc.top, c.top) else 0f

    private fun controlerZoneDefilee(
        cas: String, r: BancRendu.Resultat, defauts: MutableList<String>, toleres: MutableList<String>,
        borneTexte: Float = BORNE_TEXTE_SUR_BOUTON_DP, borneCadre: Float = BORNE_CADRE_SUR_BOUTON_DP, hauteurMax: Float? = null,
    ) {
        val lignes = r.noeuds.filter { it.texte && estDuBloc(it.nom) && BancRendu.aire(it.rect) > 0f }
        if (lignes.isEmpty()) { defauts += "aucune ligne du bloc d'écoute dans la phase « $cas »"; return }
        val texte = androidx.compose.ui.geometry.Rect(lignes.minOf { it.rect.left }, lignes.minOf { it.rect.top }, lignes.maxOf { it.rect.right }, lignes.maxOf { it.rect.bottom })
        val m = MARGE_CADRE_DP
        val cadre = androidx.compose.ui.geometry.Rect(texte.left - m, texte.top - m, texte.right + m, texte.bottom + m)
        if (hauteurMax != null && texte.height > hauteurMax) defauts += "le bloc d'écoute (lignes) ${BancRendu.fmt(texte)} fait %.0f dp de haut : la borne est $hauteurMax dp".format(texte.height)
        // Ce qui est permis : le bloc sur « Mode poche » (comme ailleurs) et, bornés, sur le haut des boutons du pied.
        defauts += BancRendu.recouvrements(r, { c, n ->
            when {
                !estDuBloc(n.nom) -> null
                c.nom == MODE_POCHE -> "permis : le bloc d'écoute sur « Mode poche »"
                estBoutonDuPied(c.nom) && profondeur(n.rect, c.rect) <= borneTexte ->
                    "toléré : le texte du bloc recouvre %.0f dp du haut de « ${c.nom} » (borne $borneTexte dp)".format(profondeur(n.rect, c.rect))
                else -> null
            }
        }, toleres)
        // Le cadre, que le banc ne voit pas, est suivi par approximation : sur un bouton du pied, borné ; sur tout autre cliquable, rien.
        for (c in r.cliquables) {
            if (BancRendu.aire(c.rect) <= 0f || c.nom == MODE_POCHE || c.id in lignes.map { it.id }) continue
            val p = profondeur(cadre, c.rect)
            if (p <= 0f) continue
            when {
                !estBoutonDuPied(c.nom) -> defauts += "le cadre du bloc (lignes + ${m.toInt()} dp) ${BancRendu.fmt(cadre)} recouvre le cliquable « ${c.nom} » ${BancRendu.fmt(c.rect)} : seuls « + Pointer », « Annuler » et « Installer la dictée » sont permis"
                p > borneCadre -> defauts += "le cadre du bloc ${BancRendu.fmt(cadre)} recouvre %.0f dp du haut de « ${c.nom} » ${BancRendu.fmt(c.rect)} : la borne est $borneCadre dp".format(p)
                else -> toleres += "toléré : le cadre du bloc recouvre %.0f dp du haut de « ${c.nom} » (borne $borneCadre dp)".format(p)
            }
        }
    }

    private fun zoneDefilee(
        cas: String, dite: String = "sept sain", annonce: String = "numéro 7, sain",
        borneTexte: Float = BORNE_TEXTE_SUR_BOUTON_DP, borneCadre: Float = BORNE_CADRE_SUR_BOUTON_DP, hauteurMax: Float? = null,
    ) {
        val e = Ecoute()
        EcouteApp.poserPilotePourTest(e.pilote)
        val defauts = ArrayList<String>()
        // L'écran est posé sans écoute, la zone est défilée jusqu'en bas, puis l'écoute commence : c'est la situation du forestier.
        val depart = BancRendu.rendre(
            session(),
            apres = { repeat(6) { onNode(hasScrollAction()).performTouchInput { swipeUp() } } },
            regle = regle,
        )
        regle.runOnIdle { e.t = 300; e.pilote.tenir(); e.pilote.moteurPret(1) }
        regle.waitForIdle()
        assertEquals(AffichageEcoute.ECOUTE, e.pilote.affichage)
        val pendant = BancRendu.lire(regle, depart.densite)
        val toleres = ArrayList<String>()
        if (pendant.noeuds.none { it.nom == "J'ÉCOUTE" }) defauts += "« J'ÉCOUTE » n'est pas affiché pendant l'écoute"
        if (pendant.cliquables.none { estBoutonDuPied(it.nom) }) defauts += "la zone n'est pas défilée jusqu'en bas : aucun bouton du pied (« + Pointer », « Annuler », « Installer la dictée ») à l'écran"
        controlerZoneDefilee("pendant", pendant, defauts, toleres, borneTexte, borneCadre, hauteurMax)
        BancRendu.rapport("$cas-pendant-l-ecoute", pendant, defauts, toleres)

        regle.runOnIdle { e.t = 1_500; e.pilote.phrase(dite) }
        regle.runOnIdle { e.t = 2_000; e.pilote.relacher() }
        regle.waitForIdle()
        val fermeeA = e.t
        regle.runOnIdle { e.pilote.resultat(AnnoncePoche(annonce, MotifsVibration.SAIN)) }
        regle.waitForIdle()
        regle.runOnIdle { e.t = fermeeA + DUREE_BANDEAU_MS - 1; e.pilote.avancer() }
        regle.waitForIdle()
        val apres = BancRendu.lire(regle, depart.densite)
        val toleres2 = ArrayList<String>()
        if (apres.noeuds.none { it.nom == "ÉCOUTE FERMÉE" }) defauts += "« ÉCOUTE FERMÉE » n'est pas affiché dans le délai"
        controlerZoneDefilee("apres-une-phrase", apres, defauts, toleres2, borneTexte, borneCadre, hauteurMax)
        BancRendu.rapport("$cas-apres-une-phrase", apres, defauts, toleres2)
        assertTrue(defauts.isEmpty(), "Rendu « $cas » :\n" + defauts.joinToString("\n"))
    }

    @Test @Config(qualifiers = BancRendu.DEBOUT_PLEIN) fun `zone defilee jusqu'en bas pendant l'ecoute, debout plein ecran, le bloc ne couvre que le haut d'un bouton du pied`() = zoneDefilee("defilee-debout-plein")
    @Test @Config(qualifiers = BancRendu.DEBOUT_SANS_PLEIN) fun `zone defilee jusqu'en bas pendant l'ecoute, debout sans plein ecran, le bloc ne couvre que le haut d'un bouton du pied`() = zoneDefilee("defilee-debout-sans-plein")

    // --- décision de Pascal (2026-11-02) « Accepté 6 s, le figer » : la commande la plus longue, zone défilée jusqu'en bas

    /**
     * Mesure du 2026-11-02 (debout, plein écran, « hauteur trois cent quatre vingt quatorze ») : le texte du bloc recouvre 54 dp du haut de
     * « Installer la dictée » (56 dp de haut), le bloc fait 169 dp de haut (633 → 802). Borne du texte : 57 dp (54 + 3 dp d'arrondi) ; borne
     * du cadre : 59 dp (le cadre est capé par la hauteur du bouton, 56 dp, + 3 dp) ; hauteur des lignes du bloc : 172 dp (169 + 3 dp) : sans
     * elle, un bloc qui grandirait vers le haut ne casserait rien tant qu'il ne touche qu'un bouton permis.
     */
    private val BORNE_TEXTE_COMMANDE_LONGUE_DP = 57f
    private val BORNE_CADRE_COMMANDE_LONGUE_DP = 59f
    private val HAUTEUR_MAX_BLOC_COMMANDE_LONGUE_DP = 172f

    private fun zoneDefileeLongue(cas: String) = zoneDefilee(
        cas, dite = COMMANDE_LA_PLUS_LONGUE, annonce = COMPRIS_LE_PLUS_LONG,
        borneTexte = BORNE_TEXTE_COMMANDE_LONGUE_DP, borneCadre = BORNE_CADRE_COMMANDE_LONGUE_DP, hauteurMax = HAUTEUR_MAX_BLOC_COMMANDE_LONGUE_DP,
    )

    @Test @Config(qualifiers = BancRendu.DEBOUT_PLEIN) fun `zone defilee jusqu'en bas, commande la plus longue, debout plein ecran, le bloc reste dans sa borne`() = zoneDefileeLongue("defilee-longue-debout-plein")
    @Test @Config(qualifiers = BancRendu.DEBOUT_SANS_PLEIN) fun `zone defilee jusqu'en bas, commande la plus longue, debout sans plein ecran, le bloc reste dans sa borne`() = zoneDefileeLongue("defilee-longue-debout-sans-plein")
}
