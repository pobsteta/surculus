package io.github.pobsteta.surculus.core.fixtures

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Mission 107 (ADR 0010) : les secrets de signature `RELEASE_*` ne sortent que dans le job de signature de
 * `release.yml` (« signer »), qui déclare `environment: release` (environnement limité à `main`, approuvé par
 * Pascal). Le job de tests (« tester ») ne déclare aucun environnement : il tourne sans approbation. Aucun autre job,
 * aucun autre workflow ne lit ces secrets. Lecture ligne à ligne des workflows : pas de bibliothèque YAML ici,
 * et le test ne contient aucune valeur de secret.
 */
class SecretsDeSignatureDansEnvironnementTest {

    companion object {
        const val JOB_SIGNATURE = "signer"
        const val JOB_TESTS = "tester"
    }

    /** Une lecture `secrets.RELEASE_*` : fichier, job qui la contient (ou null), nom du secret, environnement du job. */
    data class Lecture(val fichier: String, val job: String?, val secret: String, val environnement: String?)

    private val lectureSecret = Regex("""secrets\.(RELEASE_[A-Z0-9_]+)""")
    private val enteteJob = Regex("""^ {2}([A-Za-z0-9_-]+):\s*$""")
    private val environnementDuJob = Regex("""^ {4}environment:\s*['"]?([A-Za-z0-9_-]+)['"]?\s*(#.*)?$""")

    private fun sortDeJobs(l: String) = l.isNotBlank() && !l.startsWith(" ") && !l.startsWith("#")

    /** L'environnement déclaré par chaque job (les jobs sans `environment:` n'y figurent pas). */
    fun environnements(texte: String): Map<String, String> {
        val resultat = mutableMapOf<String, String>()
        var dansJobs = false
        var courant: String? = null
        for (l in texte.lines()) {
            if (l.startsWith("jobs:")) { dansJobs = true; continue }
            if (dansJobs && sortDeJobs(l)) dansJobs = false
            if (!dansJobs) continue
            enteteJob.find(l)?.let { courant = it.groupValues[1] }
            environnementDuJob.find(l)?.let { m -> courant?.let { resultat[it] = m.groupValues[1] } }
        }
        return resultat
    }

    fun lectures(fichier: String, texte: String): List<Lecture> {
        val env = environnements(texte)
        val resultat = mutableListOf<Lecture>()
        var dansJobs = false
        var courant: String? = null
        for (l in texte.lines()) {
            if (l.startsWith("jobs:")) { dansJobs = true; continue }
            if (dansJobs && sortDeJobs(l)) dansJobs = false
            if (dansJobs) enteteJob.find(l)?.let { courant = it.groupValues[1] }
            if (l.trimStart().startsWith("#")) continue
            val job = if (dansJobs) courant else null
            for (m in lectureSecret.findAll(l)) {
                resultat += Lecture(fichier, job, m.groupValues[1], job?.let { env[it] })
            }
        }
        return resultat
    }

    /** Les écarts à la règle, un message par écart ; vide si la règle est tenue. */
    fun ecarts(lectures: List<Lecture>): List<String> = lectures.mapNotNull { l ->
        when {
            l.fichier != "release.yml" -> "${l.fichier} lit ${l.secret} : seul release.yml le peut"
            l.job != JOB_SIGNATURE -> "${l.fichier} : le job « ${l.job} » lit ${l.secret} : seul « $JOB_SIGNATURE » le peut"
            l.environnement != "release" ->
                "${l.fichier} : le job « ${l.job} » lit ${l.secret} sans déclarer « environment: release »"
            else -> null
        }
    }

    private fun workflow(nom: String) = File(Depot.racine, ".github/workflows/$nom")

    private fun toutesLesLectures(): List<Lecture> {
        val dossier = File(Depot.racine, ".github/workflows")
        val fichiers = dossier.listFiles { f -> f.extension == "yml" || f.extension == "yaml" }.orEmpty().sortedBy { it.name }
        assertTrue(fichiers.any { it.name == "release.yml" }, "release.yml introuvable dans $dossier")
        return fichiers.flatMap { lectures(it.name, it.readText()) }
    }

    @Test
    fun `les secrets RELEASE ne sont lus que par le job signer de release-yml, sous environment release`() {
        assertEquals(emptyList<String>(), ecarts(toutesLesLectures()))
    }

    @Test
    fun `le job de tests existe et ne declare aucun environnement, seul le job de signature le fait`() {
        val texte = workflow("release.yml").readText()
        assertTrue(Regex("^ {2}$JOB_TESTS:\\s*$", RegexOption.MULTILINE).containsMatchIn(texte), "job « $JOB_TESTS » absent")
        assertEquals(mapOf(JOB_SIGNATURE to "release"), environnements(texte))
    }

    @Test
    fun `release-yml lit bien les quatre secrets de signature`() {
        val noms = toutesLesLectures().filter { it.fichier == "release.yml" }.map { it.secret }.toSet()
        assertEquals(
            setOf("RELEASE_KEYSTORE_BASE64", "RELEASE_KEYSTORE_PASSWORD", "RELEASE_KEY_ALIAS", "RELEASE_KEY_PASSWORD"),
            noms,
        )
    }

    // Preuve que la garde n'est pas aveugle : des workflows factices fautifs la font échouer.

    @Test
    fun `la garde voit un job sans environnement`() {
        val y = "jobs:\n  signer:\n    runs-on: x\n    steps:\n      - env:\n          A: \${{ secrets.RELEASE_KEY_ALIAS }}\n"
        assertEquals(1, ecarts(lectures("release.yml", y)).size)
    }

    @Test
    fun `la garde voit un autre job ou un autre workflow`() {
        val y = "jobs:\n  signer:\n    environment: release\n    steps: []\n" +
            "  publier:\n    steps:\n      - run: \${{ secrets.RELEASE_KEY_ALIAS }}\n"
        assertEquals(1, ecarts(lectures("release.yml", y)).size)
        val t = "jobs:\n  tester:\n    environment: release\n    steps:\n      - run: \${{ secrets.RELEASE_KEY_ALIAS }}\n"
        assertEquals(1, ecarts(lectures("release.yml", t)).size)
        val z = "jobs:\n  signer:\n    environment: release\n    steps:\n      - run: \${{ secrets.RELEASE_KEY_ALIAS }}\n"
        assertEquals(1, ecarts(lectures("ci.yml", z)).size)
        assertEquals(0, ecarts(lectures("release.yml", z)).size)
    }

    @Test
    fun `la garde voit un environnement declare sur le job de tests`() {
        val y = "jobs:\n  tester:\n    environment: release\n    steps: []\n  signer:\n    environment: release\n    steps: []\n"
        assertEquals(mapOf("tester" to "release", "signer" to "release"), environnements(y))
    }

    @Test
    fun `la garde refuse un autre environnement`() {
        val y = "jobs:\n  signer:\n    environment: test\n    steps:\n      - run: \${{ secrets.RELEASE_KEY_ALIAS }}\n"
        assertEquals(1, ecarts(lectures("release.yml", y)).size)
    }
}
