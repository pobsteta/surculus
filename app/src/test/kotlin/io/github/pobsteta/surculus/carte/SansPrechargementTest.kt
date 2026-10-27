package io.github.pobsteta.surculus.carte

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test
import java.io.File

/**
 * Mission 055 : comme Marculus, les fonds se chargent en ligne. Plus de préchargement de l'ortho, plus de réglage
 * « Wi-Fi seulement », plus d'état de l'ortho par tournée. Le test lit les sources de `app/src/main`.
 */
class SansPrechargementTest {
    private val main = File("src/main").takeIf { it.isDirectory } ?: File("app/src/main")

    private fun fichiers(extensions: Set<String>): List<File> =
        main.walkTopDown().filter { it.isFile && it.extension in extensions }.toList()

    private fun occurrences(motif: Regex, extensions: Set<String>): List<String> =
        fichiers(extensions).filter { motif.containsMatchIn(it.readText()) }.map { it.name }

    @Test
    fun `les sources sont trouvees`() {
        assertTrue(fichiers(setOf("kt")).size > 20, "dossier lu : ${main.absolutePath}")
    }

    @Test
    fun `plus de prechargement dans le code`() {
        val motif = Regex("TuilesHorsLigne|TelechargementTuiles|ReglagesOrtho|WorkManager|androidx\\.work|EtatOrtho|ZoneTuiles|OrthoTournee|planifierOrtho")
        assertEquals(emptyList<String>(), occurrences(motif, setOf("kt")))
    }

    @Test
    fun `plus de Wi-Fi seulement ni d'etat de l'ortho dans les chaines`() {
        val motif = Regex("Wi-Fi seulement|[Pp]récharg|Ortho (complète|partielle|absente)|ortho_(complete|partielle|absente|relancer|planifiee|refus)|tournees_wifi")
        assertEquals(emptyList<String>(), occurrences(motif, setOf("xml", "kt")))
    }

    @Test
    fun `le manifeste ne declare plus de travail de fond`() {
        val manifeste = File(main, "AndroidManifest.xml").readText()
        assertTrue("androidx.work" !in manifeste && "WorkManager" !in manifeste)
    }
}
