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
     * Les tailles rendues, en dp, à 450 dpi (2316 × 1080 px). La hauteur de la **zone sous le bandeau** (48 dp) est celle que la
     * capture du 2026-10-30 (v0.7.95) a mesurée : 336 dp couché en plein écran, ≈ 306 dp couché sans plein écran, d'où un écran de
     * 384 dp puis 354 dp (30 dp de barre d'état). **Valeur provisoire** debout sans plein écran : 745 dp (823 dp moins la barre
     * d'état, 30 dp, et la barre de navigation à trois boutons, 48 dp, qui est en bas debout) ; largeur couché sans plein écran
     * gardée à 823 dp (la barre de navigation, sur le côté, n'est pas mesurée). À remplacer par la capture.
     */
    const val COUCHE_PLEIN = "w823dp-h384dp-land-450dpi"
    const val COUCHE_SANS_PLEIN = "w823dp-h354dp-land-450dpi"
    const val DEBOUT_PLEIN = "w384dp-h823dp-port-450dpi"
    const val DEBOUT_SANS_PLEIN = "w384dp-h745dp-port-450dpi"

    /**
     * La règle Compose du banc. `createEmptyComposeRule()` seul compose avec un dispatcher « unconfined » : un `LaunchedEffect` qui
     * revient d'un `withContext(Dispatchers.IO)` reprend alors sur le fil d'entrées-sorties, et la recomposition qui suit s'y fait
     * (les `BackHandler` ajoutent un observateur de cycle de vie hors du fil principal : `IllegalStateException`). En production
     * `AndroidUiDispatcher` ramène sur le fil principal ; un dispatcher standard fait de même ici (les effets repartent à
     * `waitForIdle`).
     */
    fun nouvelleRegle(): ComposeTestRule = androidx.compose.ui.test.junit4.createEmptyComposeRule(effectContext = kotlinx.coroutines.test.StandardTestDispatcher())

    /** La dernière activité lancée par [rendre] (pour en tirer une image). */
    var activite: ComponentActivity? = null
        private set
    private var controleur: org.robolectric.android.controller.ActivityController<ComponentActivity>? = null

    /**
     * Ferme l'activité rendue (à jouer en `@After`) : les `onDispose` tournent, `PocheApp` et `EcouteApp` ne gardent pas
     * d'abonnés morts d'un cas à l'autre.
     */
    fun fermer() {
        controleur?.let { runCatching { it.pause().stop().destroy() } }
        controleur = null
        activite = null
    }

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
        val c = Robolectric.buildActivity(ComponentActivity::class.java)
        controleur = c
        val activite = c.setup().get()
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
     * cliquable, sauf ses descendants, ses ancêtres, et les paires que [permises] nomme (elle rend la raison, `null` : défaut). Rend les
     * violations, une phrase chacune ; les paires permises ou connues sont ajoutées à [toleres], pour que le rapport les dise.
     */
    fun recouvrements(
        r: Resultat,
        permises: (cliquable: Noeud, autre: Noeud) -> String? = { _, _ -> null },
        toleres: MutableList<String>? = null,
    ): List<String> {
        val out = ArrayList<String>()
        for (c in r.cliquables) for (n in r.noeuds) {
            if (n.id == c.id || !(n.texte || n.cliquable)) continue
            if (n.id in c.descendance || c.id in n.descendance) continue
            if (n.cliquable && c.id > n.id) continue // la paire de deux cliquables n'est dite qu'une fois
            if (aire(n.rect) <= 0f || aire(c.rect) <= 0f) continue
            if (intersection(c.rect, n.rect) > 1f) {
                val phrase = "« ${n.nom} » ${fmt(n.rect)} recouvre le cliquable « ${c.nom} » ${fmt(c.rect)}"
                val raison = permises(c, n)
                if (raison == null) out += phrase else toleres?.add("$phrase  [$raison]")
            }
        }
        return out
    }

    fun fmt(r: Rect) = "[%.0f,%.0f → %.0f,%.0f]".format(r.left, r.top, r.right, r.bottom)

    /** Écrit la sortie lisible du cas dans `build/reports/rendu/` (l'artefact de la CI) et la rend. */
    fun rapport(cas: String, r: Resultat, verdicts: List<String>, toleres: List<String> = emptyList(), entete: List<String> = emptyList()): String {
        val sb = StringBuilder()
        entete.forEach { sb.appendLine(it) }
        sb.appendLine("Cas : $cas")
        sb.appendLine("Racine : ${fmt(r.racine)} dp (densité ${r.densite})")
        sb.appendLine("Cliquables : ${r.cliquables.size}")
        for (n in r.noeuds) {
            val genre = (if (n.cliquable) "cliquable" else "texte   ") + if (entier(r, n)) "" else "  HORS DE LA RACINE"
            sb.appendLine("  $genre  ${fmt(n.rect)}  %.0f × %.0f dp  « %s »".format(n.rect.width, n.rect.height, n.nom))
        }
        if (toleres.isEmpty()) sb.appendLine("Recouvrements permis ou connus : aucun")
        else {
            sb.appendLine("Recouvrements permis ou connus : ${toleres.size}")
            toleres.forEach { sb.appendLine("  ~ $it") }
        }
        sb.appendLine("Verdict : " + if (verdicts.isEmpty()) "aucun défaut, tout est dans la racine" else "${verdicts.size} défaut(s)")
        verdicts.forEach { sb.appendLine("  - $it") }
        val dossier = File("build/reports/rendu").also { it.mkdirs() }
        File(dossier, cas.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".txt").writeText(sb.toString())
        return sb.toString()
    }

    /** Garde la pile complète d'un échec dans build/reports/rendu/ (publiée avec le reste) : la CI n'en montre que la première ligne. */
    fun surveillant(): org.junit.rules.TestRule = object : org.junit.rules.TestWatcher() {
        private var garde: Thread? = null

        /**
         * Garde-fou : un cas qui dure plus de 100 s est bloqué (il dure ≈ 1 s). On écrit la pile de tous les fils dans le rapport
         * publié, puis on interrompt le fil du test pour qu'il échoue au lieu de tenir la CI en attente.
         */
        override fun starting(description: org.junit.runner.Description) {
            val test = Thread.currentThread()
            garde = Thread {
                try { Thread.sleep(100_000) } catch (_: InterruptedException) { return@Thread }
                val dossier = File("build/reports/rendu").also { it.mkdirs() }
                File(dossier, "_blocage-" + description.methodName.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".txt").writeText(
                    Thread.getAllStackTraces().entries.joinToString("\n\n") { (t, pile) -> "== ${t.name}\n" + pile.joinToString("\n") { "    at $it" } },
                )
                test.interrupt()
            }.also { it.isDaemon = true; it.start() }
        }

        override fun finished(description: org.junit.runner.Description) { garde?.interrupt() }

        override fun failed(e: Throwable, description: org.junit.runner.Description) {
            val dossier = File("build/reports/rendu").also { it.mkdirs() }
            File(dossier, "_echec-" + description.methodName.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".txt")
                .writeText("fil : " + Thread.currentThread().name + "\n" + e.stackTraceToString())
        }
    }

}
