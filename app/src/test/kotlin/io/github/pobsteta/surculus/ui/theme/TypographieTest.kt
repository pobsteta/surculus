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

    /** La vue Kanban est recopiée de Marculus (ListeContextesScreen.kt l. 335, 377, 646, 652, 656) : mêmes styles. */
    @Test
    fun vueKanbanAuxStylesDeMarculus() {
        val src = sources().first { it.name == "VueKanban.kt" }.readText()
        fun nb(style: String) = Regex("""typography\.$style\b""").findAll(src).count()
        assertEquals(3, nb("titleSmall"), "titre de colonne, carte glissée, nom de forêt")
        assertEquals(2, nb("bodySmall"), "date et nombre de saisies")
        assertEquals(3, Regex("""titleSmall,\s*fontWeight = FontWeight\.Bold|titleSmall, fontWeight = FontWeight\.Bold""").findAll(src).count())
        assertEquals(0, nb("titleMedium") + nb("titleLarge"))
    }

    // --- Mission 053 : le bandeau de Marculus (BandeauCompact : primary, titleMedium, 48 dp) ---

    private fun source(nom: String) = sources().first { it.name == nom }.readText()

    @Test
    fun bandeauCompactCommeMarculus() {
        val src = source("BandeauCompact.kt")
        assertTrue("fun BandeauCompact(" in src)
        assertTrue("typography.titleMedium" in src, "titre en titleMedium")
        assertEquals(0, Regex("""typography\.(titleLarge|headline\w*)""").findAll(src).count())
        assertTrue("height(48.dp)" in src, "48 dp de haut")
        assertTrue("statusBarsPadding()" in src, "sous la barre d'état")
        assertTrue("colorScheme.primary" in src && "colorScheme.onPrimary" in src, "couleurs inchangées")
        assertTrue("WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)" in src, "marges latérales de la 040 (paysage)")
    }

    @Test
    fun unSeulBandeauEtChaqueEcranATitreLEmploie() {
        for (nom in listOf("EcranTournees.kt", "EcranParametres.kt", "EcranRestauration.kt", "EcranEssaiGnss.kt")) {
            assertTrue("BandeauCompact(" in source(nom), "$nom doit employer BandeauCompact")
        }
        // Aucune autre Surface de fond « primary » : pas de second bandeau.
        val autres = sources().filter { it.name != "BandeauCompact.kt" && Regex("""Surface\(color = MaterialTheme\.colorScheme\.primary""").containsMatchIn(it.readText()) }
        assertTrue(autres.isEmpty(), "bandeau maison dans : ${autres.map { it.name }}")
    }

    @Test
    fun aucunTitreDEcranEnTitleLargeHorsBandeau() {
        for (nom in listOf("EcranParametres.kt", "EcranRestauration.kt", "EcranEssaiGnss.kt")) {
            assertEquals(0, Regex("""titleLarge""").findAll(source(nom)).count(), "$nom : titleLarge")
        }
        // La liste des tournées garde titleLarge pour le seul nom de forêt d'une ligne, pas pour le titre.
        val lignes = source("EcranTournees.kt").lines().filter { "titleLarge" in it }
        assertEquals(1, lignes.size, "titleLarge dans EcranTournees : $lignes")
        assertTrue("ligne.foret" in lignes.single())
    }
}
