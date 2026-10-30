package io.github.pobsteta.surculus.rendu

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.unit.Density
import io.github.pobsteta.surculus.ui.theme.SurculusTheme
import org.robolectric.Robolectric
import java.io.File

/**
 * Banc du test de rendu (mission 080) : lance l'activité sous Robolectric à la taille d'un téléphone (qualificatifs de
 * `@Config`, 450 dpi comme le S23 Ultra de Pascal), pose l'écran, lit l'arbre sémantique et dit où sont les cliquables et les
 * textes. Tout en `testImplementation` : l'activité est lancée ici, pas par `ui-test-manifest`.
 */
object BancRendu {
    /**
     * Les tailles rendues, en dp, à 450 dpi (2316 × 1080 px). La hauteur « sans plein écran » est mesurée sur le téléphone
     * couché (≈ 306 dp, capture v0.7.95 du 2026-10-30). **Valeur provisoire** debout : 745 dp, même perte (78 dp) que couché,
     * à remplacer par la capture.
     */
    const val COUCHE_PLEIN = "w823dp-h384dp-land-450dpi"
    const val COUCHE_SANS_PLEIN = "w823dp-h306dp-land-450dpi"
    const val DEBOUT_PLEIN = "w384dp-h823dp-port-450dpi"
    const val DEBOUT_SANS_PLEIN = "w384dp-h745dp-port-450dpi"

    /** La dernière activité lancée par [rendre] (pour en tirer une image). */
    var activite: ComponentActivity? = null
        private set

    /** Un nœud de l'arbre, en dp, avec ce qui le nomme. */
    class Noeud(val nom: String, val rect: Rect, val cliquable: Boolean, val texte: Boolean, val descendance: Set<Int>, val id: Int)

    class Resultat(val racine: Rect, val noeuds: List<Noeud>, val densite: Float) {
        val cliquables: List<Noeud> get() = noeuds.filter { it.cliquable }
    }

    /**
     * Pose [contenu] dans une activité vide et rend l'arbre lu. [apres] est joué sur la règle une fois l'écran posé (avancer
     * l'horloge, défiler…) ; l'arbre est alors relu.
     */
    fun rendre(contenu: @Composable () -> Unit, apres: (ComposeTestRule.() -> Unit)? = null, regle: ComposeTestRule): Resultat {
        val activite = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        this.activite = activite
        activite.setContent { SurculusTheme { contenu() } }
        attendreUnEcran(regle)
        apres?.invoke(regle)
        regle.waitForIdle()
        return lire(regle, activite.resources.displayMetrics.density)
    }

    /**
     * Le contrôle charge son écran sur un fil d'entrées-sorties (`Dispatchers.IO`) : sur une machine lente (la CI), l'arbre est
     * vide un instant. On attend jusqu'à 20 s qu'il porte au moins un nœud, sinon on le dit au lieu de juger un écran vide.
     */
    private fun attendreUnEcran(regle: ComposeTestRule) {
        val limite = System.nanoTime() + 20_000_000_000L
        while (true) {
            regle.waitForIdle()
            if (regle.onRoot().fetchSemanticsNode().children.isNotEmpty()) return
            check(System.nanoTime() < limite) { "l'écran n'a rien rendu en 20 s : le test n'a rien à juger" }
            Thread.sleep(50)
        }
    }

    fun lire(regle: ComposeTestRule, densite: Float): Resultat {
        val racine = regle.onRoot().fetchSemanticsNode()
        val rects = HashMap<Int, SemanticsNode>()
        fun parcourir(n: SemanticsNode) { rects[n.id] = n; n.children.forEach(::parcourir) }
        parcourir(racine)
        val noeuds = rects.values.filter { it.id != racine.id }.map { n ->
            val cliquable = n.config.getOrNull(SemanticsActions.OnClick) != null
            val texte = n.config.getOrNull(SemanticsProperties.Text)?.isNotEmpty() == true
            val nom = (n.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" ")
                ?: n.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text }
                ?: n.config.getOrNull(SemanticsProperties.TestTag)
                ?: "(sans nom #${n.id})")
            Noeud(nom, en(n.boundsInRoot, densite), cliquable, texte, descendants(n), n.id)
        }
        return Resultat(en(racine.boundsInRoot, densite), noeuds.sortedBy { it.rect.top * 10_000 + it.rect.left }, densite)
    }

    private fun descendants(n: SemanticsNode): Set<Int> {
        val ids = HashSet<Int>()
        fun rec(x: SemanticsNode) { x.children.forEach { ids += it.id; rec(it) } }
        rec(n)
        return ids
    }

    private fun en(r: Rect, d: Float) = Rect(r.left / d, r.top / d, r.right / d, r.bottom / d)

    /** Les cliquables que le test attend, nommés par leur description (ex. « Zoom avant »). */
    fun cliquable(r: Resultat, description: String): Noeud? = r.cliquables.firstOrNull { it.nom == description }

    fun aire(r: Rect): Float = maxOf(0f, r.width) * maxOf(0f, r.height)

    fun intersection(a: Rect, b: Rect): Float = aire(a.intersect(b))

    private fun contient(dehors: Rect, dedans: Rect, marge: Float = 0.5f) =
        dedans.left >= dehors.left - marge && dedans.top >= dehors.top - marge && dedans.right <= dehors.right + marge && dedans.bottom <= dehors.bottom + marge

    /** Un nœud est-il entièrement dans la racine ? */
    fun entier(r: Resultat, n: Noeud) = contient(r.racine, n.rect)

    /**
     * Un nœud est-il **vu** en entier ? Compose rend un rectangle vide (0 × 0) pour un nœud rogné par la zone qui défile : un
     * bouton ou une bande attendus à l'écran doivent avoir une surface, pas seulement tenir dans la racine.
     */
    fun vu(r: Resultat, n: Noeud) = aire(n.rect) > 0f && entier(r, n)

    /**
     * La règle de recouvrement (assertion 3) : sur **tous** les cliquables et **tous** les textes de l'arbre, aucun nœud ne coupe un
     * cliquable, sauf ses descendants, ses ancêtres, et les paires listées dans [permises]. Rend les violations, une phrase chacune.
     */
    fun recouvrements(r: Resultat, permises: (cliquable: Noeud, autre: Noeud) -> Boolean = { _, _ -> false }): List<String> {
        val out = ArrayList<String>()
        for (c in r.cliquables) for (n in r.noeuds) {
            if (n.id == c.id || !(n.texte || n.cliquable)) continue
            if (n.id in c.descendance || c.id in n.descendance) continue
            if (n.cliquable && c.id > n.id) continue // la paire de deux cliquables n'est dite qu'une fois
            if (aire(n.rect) <= 0f || aire(c.rect) <= 0f) continue
            if (intersection(c.rect, n.rect) > 1f && !permises(c, n)) {
                out += "« ${n.nom} » ${fmt(n.rect)} recouvre le cliquable « ${c.nom} » ${fmt(c.rect)}"
            }
        }
        return out
    }

    fun fmt(r: Rect) = "[%.0f,%.0f → %.0f,%.0f]".format(r.left, r.top, r.right, r.bottom)

    /** Écrit la sortie lisible du cas dans `build/reports/rendu/` (l'artefact de la CI) et la rend. */
    fun rapport(cas: String, r: Resultat, verdicts: List<String>): String {
        val sb = StringBuilder()
        sb.appendLine("Cas : $cas")
        sb.appendLine("Racine : ${fmt(r.racine)} dp (densité ${r.densite})")
        sb.appendLine("Cliquables : ${r.cliquables.size}")
        for (n in r.noeuds) {
            val genre = (if (n.cliquable) "cliquable" else "texte   ") + if (entier(r, n)) "" else "  HORS DE LA RACINE"
            sb.appendLine("  $genre  ${fmt(n.rect)}  %.0f × %.0f dp  « %s »".format(n.rect.width, n.rect.height, n.nom))
        }
        sb.appendLine("Verdict : " + if (verdicts.isEmpty()) "aucun recouvrement, tout est dans la racine" else "${verdicts.size} défaut(s)")
        verdicts.forEach { sb.appendLine("  - $it") }
        val dossier = File("build/reports/rendu").also { it.mkdirs() }
        File(dossier, cas.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".txt").writeText(sb.toString())
        return sb.toString()
    }

    fun nouvelleRegle(): ComposeTestRule = createEmptyComposeRule()
}
