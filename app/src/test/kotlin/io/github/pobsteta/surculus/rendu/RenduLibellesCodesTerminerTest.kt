package io.github.pobsteta.surculus.rendu

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextLayoutResult
import io.github.pobsteta.surculus.controle.ArriveeControle
import io.github.pobsteta.surculus.controle.BancControle
import io.github.pobsteta.surculus.controle.EtatArrivee
import io.github.pobsteta.surculus.controle.VueSession
import io.github.pobsteta.surculus.core.placette.EtatDsf
import io.github.pobsteta.surculus.tournee.DepotTournees
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.hypot
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Mission 102, vue « Terminer » : le début du libellé sous chaque code de la grille du besoin se lit en entier (aucun mot coupé,
 * rien de rogné par le bord du bouton), le code reste lisible en gros, la grille garde 4 codes par ligne debout. Écrit par le
 * `testeur` depuis la fiche 102 et la spec, **sans lire** `EcranControle.kt` ni `ControleurPlacette.kt`.
 *
 * Les tests jugent le **résultat rendu** (textes, mise en page des textes, boîtes des boutons), pas la façon d'y arriver : une
 * ligne, coupe aux mots, taille plus petite ou autre forme de bouton passent tous, pourvu que le rendu soit juste.
 *
 * Les 15 libellés : couche `codes` telle que sommieR 0.31.1 l'écrit (`R/registre6.R`, `SOMMIER_CODES_TRAVAUX`, préfixés
 * « CODE - » par `R/travaux_qfield.R` l. 60) ; lus de la tournée, jamais embarqués par l'application : cette liste n'est qu'une
 * donnée d'essai. « aucun » a son grand bouton, il n'est pas dans la grille.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class RenduLibellesCodesTerminerTest {
    val regle = BancRendu.nouvelleRegle()

    @get:Rule val chaine: org.junit.rules.RuleChain = org.junit.rules.RuleChain.outerRule(BancRendu.surveillant()).around(regle)

    @After fun remettre() = BancRendu.fermer()

    companion object {
        val LIBELLES: Map<String, String> = linkedMapOf(
            "PS" to "Préparation du sol (broyage, andainage, crochetage, potets)",
            "PL" to "Plantation en plein, placeaux, enrichissement",
            "RG" to "Regarnis, compléments",
            "PG" to "Clôture",
            "PI" to "Protections individuelles, répulsifs",
            "DG" to "Dégagement (manuel, mécanique)",
            "CL" to "Cloisonnement sylvicole (ouverture, entretien)",
            "NT" to "Nettoiement, dépressage",
            "TF" to "Taille de formation",
            "EL" to "Élagage",
            "DE" to "Désignation d'arbres objectif",
            "RN" to "Travaux en régénération naturelle (dégagement de semis, crochetage localisé)",
            "BI" to "Îlots, arbres habitat, mares, lisières",
            "DS" to "Entretien de piste, fossés, places de dépôt",
            "PA" to "Entretien des limites (peinture, bornes)",
        )

        /** Décision de Pascal du 2026-11-07 (fiche 102, « Coins arrondis, 2 lignes ») : rayon (dp) des coins d'un bouton de code. */
        const val COIN_CODE_DP = 12f

        /** Valeur provisoire : corps minimal (sp) du code « en gros » (titleLarge = 22 sp aujourd'hui). */
        const val CODE_MIN_SP = 20f
    }

    private val codes: List<String> = LIBELLES.keys.toList()

    /** Un texte du bouton : sa mise en page et sa place dans le bouton (px, repère du bouton). */
    private class Texte(val texte: String, val mise: TextLayoutResult, val x: Float, val y: Float)

    private class Bouton(val code: String, val x: Float, val y: Float, val l: Float, val h: Float, val textes: List<Texte>)

    private fun contenu(): @androidx.compose.runtime.Composable () -> Unit {
        val b = BancControle(nbPlantsAnterieurs = 1)
        val contexte = b.contexte.copy(
            codesTravaux = codes.toSet() + "aucun",
            libellesCodes = LIBELLES.mapValues { (c, l) -> "$c - $l" } + ("aucun" to "Aucun"),
        )
        val ouverte = assertIs<EtatArrivee.Ouverte>(
            ArriveeControle.ouvrir(contexte, b.journal, b.horloge, b.essences, b.positionsAuPiquet(), null, b.sauvegarde),
        )
        val c = ouverte.controleur
        c.dire(EtatDsf.SAIN)
        c.demanderTerminer()
        val reglages = DepotTournees.reglagesControle(RuntimeEnvironment.getApplication())
        return { VueSession(c, "couchey-essai-plants", reglages, stockageRefuse = false, onRetour = {}, onParametres = {}) }
    }

    private fun textesDe(n: SemanticsNode, out: MutableList<SemanticsNode>) {
        if (n.config.getOrNull(SemanticsProperties.Text) != null) out += n
        n.children.forEach { textesDe(it, out) }
    }

    private fun miseDe(n: SemanticsNode): TextLayoutResult? {
        val liste = ArrayList<TextLayoutResult>()
        val action = n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action ?: return null
        return if (action(liste) && liste.isNotEmpty()) liste[0] else null
    }

    private fun texteDe(n: SemanticsNode) = n.config[SemanticsProperties.Text].joinToString("") { it.text }

    private fun estLeCode(t: String, code: String) = t == code || t.startsWith("$code\n")

    /** Les boutons de la grille : l'ancêtre cliquable du texte du code, avec tous les textes qu'il porte (arbre non fusionné). */
    private fun lireBoutons(): Pair<List<Bouton>, List<String>> {
        regle.waitForIdle()
        val racine = regle.onRoot(useUnmergedTree = true).fetchSemanticsNode()
        val tous = ArrayList<SemanticsNode>()
        textesDe(racine, tous)
        val boutons = ArrayList<Bouton>()
        val absents = ArrayList<String>()
        for (code in codes) {
            val porteur = tous.firstOrNull { estLeCode(texteDe(it), code) }
            var cliquable = porteur?.parent
            while (cliquable != null && cliquable.config.getOrNull(SemanticsActions.OnClick) == null) cliquable = cliquable.parent
            if (porteur == null || cliquable == null) { absents += code; continue }
            val enfants = ArrayList<SemanticsNode>().also { textesDe(cliquable, it) }
            val ox = cliquable.positionInRoot.x
            val oy = cliquable.positionInRoot.y
            val textes = enfants.mapNotNull { n ->
                val mise = miseDe(n) ?: return@mapNotNull null
                Texte(texteDe(n), mise, n.positionInRoot.x - ox, n.positionInRoot.y - oy)
            }
            boutons += Bouton(code, ox, oy, cliquable.size.width.toFloat(), cliquable.size.height.toFloat(), textes)
        }
        return boutons to absents
    }

    private fun lettre(c: Char) = c.isLetterOrDigit()

    /** Les mots du libellé sommieR, sans ponctuation ni « … » collés (« Regarnis, » = « Regarnis »). */
    private fun mots(s: String) = s.split(Regex("\\s+")).map { it.trim(',', '.', ';', ':', '(', ')', '…') }.filter { it.isNotEmpty() }

    /** Le libellé affiché sous le code : le texte du bouton hors le code lui-même (et hors « CODE - » éventuel). */
    private fun libelleAffiche(b: Bouton): String =
        b.textes.joinToString("\n") { t -> if (t.texte.startsWith("${b.code}\n")) t.texte.removePrefix("${b.code}\n") else if (t.texte == b.code) "" else t.texte }
            .replace('\n', ' ').trim()

    private fun dedans(b: Bouton, px: Float, py: Float, coin: Float): Boolean {
        if (px < -0.5f || py < -0.5f || px > b.l + 0.5f || py > b.h + 0.5f) return false
        val cx = when { px < coin -> coin; px > b.l - coin -> b.l - coin; else -> px }
        val cy = when { py < coin -> coin; py > b.h - coin -> b.h - coin; else -> py }
        return hypot(px - cx, py - cy) <= coin + 0.5f
    }

    private fun defautsDeLibelle(b: Bouton, densite: Float): List<String> {
        val d = ArrayList<String>()
        val attendu = mots(LIBELLES.getValue(b.code))
        val affiche = libelleAffiche(b)
        val vus = mots(affiche)
        if (vus.isEmpty()) d += "${b.code} : aucun début de libellé sous le code"
        else if (vus.size > attendu.size || vus != attendu.take(vus.size)) {
            d += "${b.code} : « $affiche » n'est pas un début du libellé « ${LIBELLES.getValue(b.code)} » en mots entiers"
        }
        val coin = COIN_CODE_DP * densite
        for (t in b.textes) {
            val m = t.mise
            val plein = m.layoutInput.text.text
            for (i in 0 until m.lineCount - 1) {
                val fin = m.getLineEnd(i)
                if (fin in 1 until plein.length && lettre(plein[fin - 1]) && lettre(plein[fin])) {
                    d += "${b.code} : mot coupé entre les lignes ${i + 1} et ${i + 2} de « $plein » (« ${plein.substring(0, fin)} / ${plein.substring(fin)} »)"
                }
            }
            for (i in 0 until m.lineCount) {
                if (m.isLineEllipsized(i)) d += "${b.code} : ligne ${i + 1} de « $plein » rognée par Compose (ellipse à la lettre)"
            }
            // `didOverflowWidth` est vrai même pour « PS » sous Robolectric : on mesure soi-même, ligne par ligne, contre la zone du texte.
            for (i in 0 until m.lineCount) {
                if (m.getLineRight(i) > m.size.width + 1f || m.getLineBottom(i) > m.size.height + 1f) {
                    d += "${b.code} : ligne ${i + 1} de « $plein » sort de sa zone de texte (droite %.0f > %d px, bas %.0f > %d px)"
                        .format(m.getLineRight(i), m.size.width, m.getLineBottom(i), m.size.height)
                }
            }
            for (i in 0 until m.lineCount) {
                val g = t.x + m.getLineLeft(i); val dr = t.x + m.getLineRight(i)
                val h = t.y + m.getLineTop(i); val bas = t.y + m.getLineBottom(i)
                val sorti = listOf(g to h, dr to h, g to bas, dr to bas).any { (x, y) -> !dedans(b, x, y, coin) }
                if (sorti) {
                    d += "${b.code} : ligne ${i + 1} de « $plein » déborde du bouton ou de son bord rond (ligne %.0f→%.0f px, haut %.0f, bas %.0f ; bouton %.0f × %.0f px, coin %.0f px)"
                        .format(g, dr, h, bas, b.l, b.h, coin)
                }
            }
        }
        return d
    }

    private fun libelles(cas: String) {
        val r = BancRendu.rendre(contenu = contenu(), regle = regle)
        val (boutons, absents) = lireBoutons()
        val defauts = ArrayList<String>()
        if (absents.isNotEmpty()) defauts += "boutons de la grille introuvables pour les codes $absents"
        boutons.forEach { defauts += defautsDeLibelle(it, r.densite) }
        val tailles = boutons.flatMap { b -> b.textes.filterNot { estLeCode(it.texte, b.code) }.map { "${b.code} : libellé « ${it.texte} » en ${it.mise.layoutInput.style.fontSize}" } }
        BancRendu.rapport(cas, r, defauts, entete = tailles)
        assertTrue(defauts.isEmpty(), "Rendu « $cas » :\n" + defauts.joinToString("\n"))
    }

    private fun codeEnGrosEtParLigne(cas: String, auMoinsParLigne: Int?) {
        val r = BancRendu.rendre(contenu = contenu(), regle = regle)
        val (boutons, absents) = lireBoutons()
        val defauts = ArrayList<String>()
        if (absents.isNotEmpty()) defauts += "boutons de la grille introuvables pour les codes $absents"
        for (b in boutons) {
            val t = b.textes.firstOrNull { estLeCode(it.texte, b.code) }
            if (t == null) { defauts += "${b.code} : texte du code introuvable"; continue }
            val taille = t.mise.layoutInput.style.fontSize
            if (!taille.isSp || taille.value < CODE_MIN_SP) defauts += "${b.code} : code en $taille, sous $CODE_MIN_SP sp (lisible en gros)"
            if (t.mise.lineCount != 1 || t.mise.getLineRight(0) > t.mise.size.width + 1f) {
                defauts += "${b.code} : le code « ${t.texte} » est coupé"
            }
        }
        if (auMoinsParLigne != null && boutons.isNotEmpty()) {
            val lignes = boutons.groupBy { Math.round(it.y / 4f) }.values
            val plus = lignes.maxOf { it.size }
            if (plus < auMoinsParLigne) {
                defauts += "la grille a au plus $plus codes par ligne (${lignes.map { it.size }}), moins de $auMoinsParLigne : si la grille change, le dire à Pascal"
            }
        }
        BancRendu.rapport(cas, r, defauts)
        assertTrue(defauts.isEmpty(), "Rendu « $cas » :\n" + defauts.joinToString("\n"))
    }

    @Test @Config(qualifiers = BancRendu.DEBOUT_PLEIN) fun `libelles des 15 codes debout en plein ecran - mots entiers, rien de rogne`() = libelles("libelles-debout-plein")
    @Test @Config(qualifiers = BancRendu.DEBOUT_SANS_PLEIN) fun `libelles des 15 codes debout sans plein ecran - mots entiers, rien de rogne`() = libelles("libelles-debout-sans-plein")
    @Test @Config(qualifiers = BancRendu.COUCHE_PLEIN) fun `libelles des 15 codes couche en plein ecran - mots entiers, rien de rogne`() = libelles("libelles-couche-plein")
    @Test @Config(qualifiers = BancRendu.COUCHE_SANS_PLEIN) fun `libelles des 15 codes couche sans plein ecran - mots entiers, rien de rogne`() = libelles("libelles-couche-sans-plein")

    @Test @Config(qualifiers = BancRendu.DEBOUT_PLEIN) fun `code en gros et 4 par ligne debout en plein ecran`() = codeEnGrosEtParLigne("code-debout-plein", 4)
    @Test @Config(qualifiers = BancRendu.DEBOUT_SANS_PLEIN) fun `code en gros et 4 par ligne debout sans plein ecran`() = codeEnGrosEtParLigne("code-debout-sans-plein", 4)
    @Test @Config(qualifiers = BancRendu.COUCHE_PLEIN) fun `code en gros couche en plein ecran`() = codeEnGrosEtParLigne("code-couche-plein", null)
    @Test @Config(qualifiers = BancRendu.COUCHE_SANS_PLEIN) fun `code en gros couche sans plein ecran`() = codeEnGrosEtParLigne("code-couche-sans-plein", null)

    /**
     * Garde de la forme (décision de Pascal) : le libellé doit tenir dans un rectangle à coins de 12 dp. Un retour au quasi-cercle
     * de rayon 40 dp (82 × 80 dp debout) pousse les coins des lignes hors du bouton et fait échouer [libelles].
     */
    @Test @Config(qualifiers = BancRendu.DEBOUT_PLEIN) fun `forme du bouton de code - le libelle tient dans un rectangle a coins de 12 dp, pas dans un quasi-cercle`() {
        assertEquals(12f, COIN_CODE_DP)
        libelles("forme-debout-plein")
    }

    @Test
    fun `la donnee d'essai porte les 15 codes de sommieR`() {
        assertEquals(15, LIBELLES.size)
        assertEquals(listOf("PS", "PL", "RG", "PG", "PI", "DG", "CL", "NT", "TF", "EL", "DE", "RN", "BI", "DS", "PA"), codes)
    }
}
