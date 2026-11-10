package io.github.pobsteta.surculus.core.fixtures

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Mission 107 (ADR 0010) : les secrets de signature `RELEASE_*` ne sortent que dans le job de `release.yml`
 * qui déclare `environment: release` (environnement limité à `main`, approuvé par Pascal). Aucun autre job,
 * aucun autre workflow ne les lit. Lecture ligne à ligne des workflows : pas de bibliothèque YAML ici,
 * et le test ne contient aucune valeur de secret.
 */
class SecretsDeSignatureDansEnvironnementTest {

    /** Une lecture `secrets.RELEASE_*` : fichier, job qui la contient (ou null), nom du secret, environnement du job. */
    data class Lecture(val fichier: String, val job: String?, val secret: String, val environnement: String?)

    private val lectureSecret = Regex("""secrets\.(RELEASE_[A-Z0-9_]+)""")
    private val enteteJob = Regex("""^ {2}([A-Za-z0-9_-]+):\s*$""")
    private val environnementDuJob = Regex("""^ {4}environment:\s*['"]?([A-Za-z0-9_-]+)['"]?\s*(#.*)?$""")

    private fun sortDeJobs(l: String) = l.isNotBlank() && !l.startsWith(" ") && !l.startsWith("#")

    fun lectures(fichier: String, texte: String): List<Lecture> {
        val lignes = texte.lines()
        // Passe 1 : l'environnement déclaré par chaque job.
        val environnements = mutableMapOf<String, String>()
        var dansJobs = false
        var courant: String? = null
        for (l in lignes) {
            if (l.startsWith("jobs:")) { dansJobs = true; continue }
            if (dansJobs && sortDeJobs(l)) dansJobs = false
            if (!dansJobs) continue
            enteteJob.find(l)?.let { courant = it.groupValues[1] }
            environnementDuJob.find(l)?.let { m -> courant?.let { environnements[it] = m.groupValues[1] } }
        }
        // Passe 2 : chaque lecture d'un secret RELEASE_*, rattachée à son job.
        val resultat = mutableListOf<Lecture>()
        dansJobs = false
        courant = null
        for (l in lignes) {
            if (l.startsWith("jobs:")) { dansJobs = true; continue }
            if (dansJobs && sortDeJobs(l)) dansJobs = false
            if (dansJobs) enteteJob.find(l)?.let { courant = it.groupValues[1] }
            if (l.trimStart().startsWith("#")) continue
            val job = if (dansJobs) courant else null
            for (m in lectureSecret.findAll(l)) {
                resultat += Lecture(fichier, job, m.groupValues[1], job?.let { environnements[it] })
            }
        }
        return resultat
    }

    /** Les écarts à la règle, un message par écart ; vide si la règle est tenue. */
    fun ecarts(lectures: List<Lecture>): List<String> = lectures.mapNotNull { l ->
        when {
            l.fichier != "release.yml" -> "${l.fichier} lit ${l.secret} : seul release.yml le peut"
            l.job != "construire" -> "${l.fichier} : le job « ${l.job} » lit ${l.secret} : seul « construire » le peut"
            l.environnement != "release" ->
                "${l.fichier} : le job « ${l.job} » lit ${l.secret} sans déclarer « environment: release »"
            else -> null
        }
    }

    private fun toutesLesLectures(): List<Lecture> {
        val dossier = File(Depot.racine, ".github/workflows")
        val fichiers = dossier.listFiles { f -> f.extension == "yml" || f.extension == "yaml" }.orEmpty().sortedBy { it.name }
        assertTrue(fichiers.any { it.name == "release.yml" }, "release.yml introuvable dans $dossier")
        return fichiers.flatMap { lectures(it.name, it.readText()) }
    }

    @Test
    fun `les secrets RELEASE ne sont lus que par le job construire de release-yml, sous environment release`() {
        assertEquals(emptyList<String>(), ecarts(toutesLesLectures()))
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
        val y = "jobs:\n  construire:\n    runs-on: x\n    steps:\n      - env:\n          A: \${{ secrets.RELEASE_KEY_ALIAS }}\n"
        assertEquals(1, ecarts(lectures("release.yml", y)).size)
    }

    @Test
    fun `la garde voit un autre job ou un autre workflow`() {
        val y = "jobs:\n  construire:\n    environment: release\n    steps: []\n" +
            "  publier:\n    steps:\n      - run: \${{ secrets.RELEASE_KEY_ALIAS }}\n"
        assertEquals(1, ecarts(lectures("release.yml", y)).size)
        val z = "jobs:\n  construire:\n    environment: release\n    steps:\n      - run: \${{ secrets.RELEASE_KEY_ALIAS }}\n"
        assertEquals(1, ecarts(lectures("ci.yml", z)).size)
        assertEquals(0, ecarts(lectures("release.yml", z)).size)
    }

    @Test
    fun `la garde refuse un autre environnement`() {
        val y = "jobs:\n  construire:\n    environment: test\n    steps:\n      - run: \${{ secrets.RELEASE_KEY_ALIAS }}\n"
        assertEquals(1, ecarts(lectures("release.yml", y)).size)
    }
}
