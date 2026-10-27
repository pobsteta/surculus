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

    /** Un texte ne prend jamais une teinte d'état directement : il passe par CouleursEtat (couleurErreur, couleurNeutre, TexteEtat). */
    @Test fun `aucun Text n'est colore directement par une teinte d'etat`() {
        val motif = Regex("""Text\([^\n]*color = Palette\.(Rouge|Vert|Orange|Noir)\b""")
        val restes = sources.filter { (chemin, _) -> !chemin.endsWith("Palette.kt") }
            .flatMap { (chemin, texte) -> texte.lines().filter { motif.containsMatchIn(it) }.map { "$chemin : ${it.trim()}" } }
        assertTrue(restes.isEmpty(), restes.joinToString("\n"))
    }

    @Test fun `l'ecran d'essai n'a plus sa copie de la palette ni de fond blanc en sombre`() {
        val essai = sources.first { it.first.endsWith("EcranEssaiGnss.kt") }.second
        assertFalse("private val Vert" in essai || "private val Rouge" in essai || "private val Noir" in essai)
        assertTrue("LocalSombre.current) Modifier else Modifier.background(Palette.Blanc)" in essai)
    }
}
