package io.github.pobsteta.surculus.carte

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Mission 063 : plus aucune plaque blanche ni aucun texte en teinte d'état nue dans les sources de l'application. */
class SansPlaqueSourcesTest {
    private val racine = File("src/main/kotlin").takeIf { it.isDirectory } ?: File("app/src/main/kotlin")
    private val sources = racine.walkTopDown().filter { it.extension == "kt" }.map { it.path to it.readText() }.toList()

    @Test fun `les sources sont lues`() {
        assertTrue(sources.size > 20, "${sources.size} fichiers")
    }

    @Test fun `plaqueEtat n'existe plus nulle part`() {
        val restes = sources.filter { "plaqueEtat" in it.second }.map { it.first }
        assertTrue(restes.isEmpty(), "plaqueEtat reste dans : $restes")
    }

    /**
     * Un texte ne prend jamais une teinte d'état directement : il passe par CouleursEtat (couleurErreur, couleurNeutre, TexteEtat).
     * Motif multiligne (mission 012-B1, décision de Pascal sur la #107) : l'appel `Text(` est suivi jusqu'à sa parenthèse
     * fermante, sur autant de lignes qu'il en faut, parenthèses imbriquées comprises ; un `color = Palette.X` sur la ligne
     * suivante est vu comme sur la même.
     */
    @Test fun `aucun Text n'est colore directement par une teinte d'etat, meme sur plusieurs lignes`() {
        val restes = sources.filter { (chemin, _) -> !chemin.endsWith("Palette.kt") }
            .flatMap { (chemin, texte) -> appelsText(texte).filter { teinteNue.containsMatchIn(it) }.map { "$chemin : ${it.lines().first().trim()}…" } }
        assertTrue(restes.isEmpty(), restes.joinToString("\n"))
    }

    @Test fun `le motif multiligne voit un Text colore sur plusieurs lignes, et laisse passer les textes neutres`() {
        val fautif = "Text(\n    stringResource(R.string.x, f(1)),\n    style = s,\n    color = Palette.Rouge,\n)"
        assertTrue(appelsText(fautif).any { teinteNue.containsMatchIn(it) })
        assertTrue(appelsText("Text(\"a\", color = Palette.Rouge)").any { teinteNue.containsMatchIn(it) })
        assertTrue(appelsText("Text(\n  \"a\",\n  color = couleurErreur(),\n)").none { teinteNue.containsMatchIn(it) })
        // Un `color = Palette.X` hors d'un Text (un dessin, une pastille) n'est pas un texte.
        assertTrue(appelsText("drawCircle(\n  color = Palette.Rouge\n)").isEmpty())
    }

    private val teinteNue = Regex("""color\s*=\s*Palette\.(Rouge|Vert|Orange|Noir)\b""")

    /** Chaque appel `Text(...)` du texte, de « Text( » à sa parenthèse fermante (équilibrée, guillemets ignorés). */
    private fun appelsText(texte: String): List<String> {
        val appels = ArrayList<String>()
        Regex("""(?<![A-Za-z0-9_.])Text\(""").findAll(texte).forEach { m ->
            var profondeur = 0
            var dansChaine = false
            var i = m.range.last
            while (i < texte.length) {
                val c = texte[i]
                if (c == '"' && texte.getOrNull(i - 1) != '\\') dansChaine = !dansChaine
                if (!dansChaine) {
                    if (c == '(') profondeur++
                    if (c == ')') { profondeur--; if (profondeur == 0) break }
                }
                i++
            }
            appels += texte.substring(m.range.first, minOf(i + 1, texte.length))
        }
        return appels
    }

    @Test fun `l'ecran d'essai n'a plus sa copie de la palette ni de fond blanc en sombre`() {
        val essai = sources.first { it.first.endsWith("EcranEssaiGnss.kt") }.second
        assertFalse("private val Vert" in essai || "private val Rouge" in essai || "private val Noir" in essai)
        assertTrue("LocalSombre.current) Modifier else Modifier.background(Palette.Blanc)" in essai)
    }
}
