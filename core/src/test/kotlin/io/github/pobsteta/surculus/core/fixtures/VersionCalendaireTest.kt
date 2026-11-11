package io.github.pobsteta.surculus.core.fixtures

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Mission 108 : la version d'une release est `AAAA.M.N` (calendaire, comme nemeton). `scripts/prochaine-version.sh`
 * prend l'instant du lancement (ISO 8601, en UTC, suffixe Z) en argument et la liste des tags sur l'entrée standard ;
 * il affiche la version. Pas de réseau, pas de dépôt Git : des listes de tags et des dates données.
 * Le mois est celui de Paris (Europe/Paris), pas celui d'UTC.
 */
class VersionCalendaireTest {

    // Expression régulière de nemeton (.github/workflows/r.yml) : mois sans zéro devant, N >= 1.
    private val formeNemeton = Regex("""^[0-9]{4}\.([1-9]|1[0-2])\.[1-9][0-9]*$""")

    private fun prochaine(instantUtc: String, vararg tags: String): String {
        val script = File(Depot.racine, "scripts/prochaine-version.sh")
        assertTrue(script.isFile, "script absent : $script")
        val p = ProcessBuilder("bash", script.path, instantUtc)
            .redirectErrorStream(true)
            .start()
        p.outputStream.bufferedWriter().use { w -> tags.forEach { w.write(it + "\n") } }
        val sortie = p.inputStream.bufferedReader().readText().trim()
        assertTrue(p.waitFor(30, TimeUnit.SECONDS), "le script ne rend pas la main")
        assertEquals(0, p.exitValue(), "script en échec : $sortie")
        assertTrue(formeNemeton.matches(sortie), "forme hors nemeton : « $sortie »")
        return sortie
    }

    @Test fun `aucun tag du mois, seulement des v0_7_x, rang 1`() =
        assertEquals("2026.11.1", prochaine("2026-11-11T10:00:00Z", "v0.7.131", "v0.7.132", "v0.7.133"))

    @Test fun `aucun tag du tout, rang 1`() =
        assertEquals("2026.11.1", prochaine("2026-11-11T10:00:00Z"))

    @Test fun `deux tags du mois, rang 3`() =
        assertEquals("2026.11.3", prochaine("2026-11-11T10:00:00Z", "v0.7.133", "v2026.11.1", "v2026.11.2"))

    @Test fun `décembre repart de 1 malgré novembre et octobre`() =
        assertEquals("2026.12.1", prochaine("2026-12-05T10:00:00Z", "v2026.11.22", "v2026.10.19"))

    @Test fun `janvier, mois sans zéro devant`() =
        assertEquals("2027.1.1", prochaine("2027-01-03T10:00:00Z", "v2026.12.4"))

    @Test fun `un trou dans la suite n'est pas comblé`() =
        assertEquals("2026.11.4", prochaine("2026-11-11T10:00:00Z", "v2026.11.1", "v2026.11.3"))

    @Test fun `tri numérique, v2026_11_10 après v2026_11_9`() =
        assertEquals("2026.11.11", prochaine("2026-11-11T10:00:00Z", "v2026.11.9", "v2026.11.10", "v2026.11.2"))

    @Test fun `le mois 1 ne se confond pas avec le mois 11 ni 12`() =
        assertEquals("2027.1.1", prochaine("2027-01-03T10:00:00Z", "v2027.11.5", "v2027.12.3", "v2026.1.7"))

    @Test fun `30 novembre 23 h 30 à Paris reste novembre`() =
        assertEquals("2026.11.1", prochaine("2026-11-30T22:30:00Z"))

    @Test fun `1er décembre 0 h 30 à Paris est décembre`() =
        assertEquals("2026.12.1", prochaine("2026-11-30T23:30:00Z", "v2026.11.4"))

    @Test fun `heure d'été, 31 juillet 22 h 30 UTC est déjà août à Paris`() =
        assertEquals("2026.8.1", prochaine("2026-07-31T22:30:00Z", "v2026.7.2"))

    @Test fun `des tags qui ne sont pas des versions sont ignorés`() =
        assertEquals("2026.11.1", prochaine("2026-11-11T10:00:00Z", "v2026.11.x", "essai", "v2026.11.2-rc1", "2026.11.7"))
}
