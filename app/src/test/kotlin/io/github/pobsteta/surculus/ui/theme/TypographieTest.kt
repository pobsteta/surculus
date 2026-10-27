package io.github.pobsteta.surculus.ui.theme

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Mission 042 : Surculus a les tailles de texte de Marculus, c'est-à-dire les styles par défaut de Material 3
 * (BOM 2026.02.01), sans `Typography` propre et sans taille écrite à la main, sauf les exceptions ci-dessous.
 * Le test lit les sources de `app/src/main/kotlin` (le répertoire de travail des tests unitaires est `app/`).
 */
class TypographieTest {
    private val racine = File("src/main/kotlin")

    private fun sources(): List<File> = racine.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun fontSizes(): List<Pair<String, String>> =
        sources().flatMap { f ->
            f.readLines().flatMap { l ->
                Regex("""fontSize\s*=\s*([0-9.]+\.sp)""").findAll(l).map { f.name to it.groupValues[1] }.toList()
            }
        }

    @Test
    fun lesSourcesSontTrouvees() {
        // Garde-fou du test lui-même : sans sources lues, les autres tests passeraient à vide.
        assertTrue(sources().size > 10, "sources lues : ${sources().map { it.name }}")
        assertTrue(sources().any { it.name == "EcranGuidage.kt" })
    }

    @Test
    fun aucuneTypographieProprePosee() {
        val fautifs = sources().filter { f ->
            f.readLines().any { Regex("""\bTypography\s*\(""").containsMatchIn(it) || Regex("""typography\s*=""").containsMatchIn(it) }
        }.map { it.name }
        assertTrue(fautifs.isEmpty(), "Typography propre dans : $fautifs")
    }

    @Test
    fun aucuneFontFamilyPosee() {
        val fautifs = sources().filter { f -> f.readLines().any { "fontFamily" in it } }.map { it.name }
        assertTrue(fautifs.isEmpty(), "police posée dans : $fautifs")
    }

    @Test
    fun aucuneTailleEcriteALaMainHorsExceptions() {
        val exceptions = listOf(
            // Décidées par Pascal le 2026-10-27 : lues au soleil, à bout de bras.
            "EcranGuidage.kt" to "72.sp", // distance à la cible
            "EcranEssaiGnss.kt" to "44.sp", // qualité du fix
            "EcranEssaiGnss.kt" to "44.sp", // précision
        )
        val ordre = compareBy<Pair<String, String>>({ it.first }, { it.second })
        assertEquals(exceptions.sortedWith(ordre), fontSizes().sortedWith(ordre),
            "tailles écrites à la main, hors exceptions : ${fontSizes()}")
    }
}
