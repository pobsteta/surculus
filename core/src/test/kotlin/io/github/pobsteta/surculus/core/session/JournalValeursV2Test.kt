package io.github.pobsteta.surculus.core.session

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Mission 087 : format v2 de la `valeur` des lignes du journal (`docs/formats/journal-valeurs-v2.md` ; ADR 0003,
 * amendement du 2026-11-02, point 8). Écrit par le testeur depuis le format, sans lire `Valeurs.kt`.
 *
 * Rouge jusqu'au `dev`. API supposée, à confirmer par le dev :
 * - `FormatValeurs.VERSION` = 2 ;
 * - `ValeurControle.positionMesureeX` / `positionMesureeY` : `Double?` ; nouveau champ `sansPositionMesuree: Boolean`
 *   (appelé ici par son nom) ;
 * - `ValeurControle.lire(texte) { motif -> ... }`, `ValeurPointage.lire(texte) { ... }`, `ValeurEtat.lire(texte) { ... }` :
 *   second paramètre, une fonction appelée une fois par valeur ignorée à la lecture (la trace que l'application envoie au
 *   journal technique ; choix de format du `relecteur` de la PR #150, non tranché par Pascal). L'appel à un seul
 *   paramètre reste.
 */
class JournalValeursV2Test {
    private val racine: Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }.first { Files.exists(it.resolve("settings.gradle.kts")) }

    /** Les blocs ```json d'un document de format : « ses tests rejouent chaque exemple de ce document ». */
    private fun exemples(doc: String): List<String> =
        Regex("```json\\s*\\n(.*?)\\n```", RegexOption.DOT_MATCHES_ALL).findAll(Files.readString(racine.resolve("docs/formats/$doc")))
            .map { it.groupValues[1].trim() }.toList()

    private val exempleV2 by lazy { exemples("journal-valeurs-v2.md").single { "\"v\":2" in it } }
    private val exempleV1Controle by lazy { exemples("journal-valeurs-v1.md").single { "\"placette_uuid\"" in it && "\"v\":1" in it } }

    private val numeros = mapOf("019...a" to 1, "019...b" to 2)

    /** Le contrôle de l'exemple du format v2 : terminé, ouvert sans position (position du piquet, Lambert-93, x = E, y = N, m). */
    private val sansPosition = ValeurControle(
        placetteUuid = "11111111-1111-4111-8111-111111111111", codePlacette = "P35-01", jourCivil = "2026-11-02",
        operateur = "Agent d'essai", termine = true, visiteLeUtcMs = 1_793_620_000_000,
        positionX = 846164.76, positionY = 6686123.26, positionMesureeX = null, positionMesureeY = null,
        rattacheParIdentite = true, repereEpsg = 2154, numeros = numeros, sansPositionMesuree = true,
    )

    private fun lireTrace(texte: String): Pair<ValeurControle?, List<String>> {
        val traces = ArrayList<String>()
        return ValeurControle.lire(texte) { traces += it } to traces
    }

    // ------------------------------------------------------------------ version

    @Test
    fun `Surculus ecrit la version 2, une seule version pour tout le format`() {
        assertEquals(2L, FormatValeurs.VERSION.toLong())
        assertTrue(sansPosition.versJson().startsWith("{\"v\":2,"), sansPosition.versJson())
        val p = ValeurPointage("c", "p", 2.0, 250.0, "gr", "saisie", 1.7, 0.4, 105.9, 105.5, "2026-10-27", 846166.1, 6686122.0, 2154, "CHS", null, false, 1_790_000_000_000)
        assertTrue(p.versJson().startsWith("{\"v\":2,"), "pointage aussi en v2 : ${p.versJson()}")
        val e = ValeurEtat("c", "u", "sain", null, null, 35)
        assertTrue(e.versJson().startsWith("{\"v\":2,"), "état aussi en v2 : ${e.versJson()}")
    }

    // ------------------------------------------------------------------ l'exemple du format v2

    @Test
    fun `l'exemple v2 du format se lit, sans position mesuree`() {
        val (lu, traces) = lireTrace(exempleV2)
        assertEquals(sansPosition, lu)
        assertEquals(emptyList(), traces, "une valeur lisible n'est pas tracée")
    }

    @Test
    fun `le controle sans position s'ecrit au caractere pres comme l'exemple du format - null explicite, cle apres rattache_par_identite`() {
        assertEquals(exempleV2, sansPosition.versJson())
        val t = sansPosition.versJson()
        assertTrue("\"position_mesuree_x\":null,\"position_mesuree_y\":null" in t, t)
        assertTrue("\"rattache_par_identite\":true,\"sans_position_mesuree\":true,\"repere_epsg\"" in t, t)
    }

    @Test
    fun `un controle avec position s'ecrit en v2 avec sans_position_mesuree faux et se relit`() {
        val avec = sansPosition.copy(positionMesureeX = 846166.0, positionMesureeY = 6686125.0, rattacheParIdentite = false, sansPositionMesuree = false)
        val t = avec.versJson()
        assertTrue("\"rattache_par_identite\":false,\"sans_position_mesuree\":false" in t, t)
        assertEquals(avec, ValeurControle.lire(t))
    }

    @Test
    fun `aller-retour v2 - un controle sans position non termine, sans numeros`() {
        val ouvert = sansPosition.copy(termine = false, numeros = null)
        assertEquals(ouvert, ValeurControle.lire(ouvert.versJson()))
    }

    // ------------------------------------------------------------------ v1 toujours relu (principe N-1)

    @Test
    fun `l'exemple v1 du format est toujours relu a l'identique, sans_position_mesuree faux`() {
        val (lu, traces) = lireTrace(exempleV1Controle)
        val attendu = ValeurControle(
            placetteUuid = "11111111-1111-4111-8111-111111111111", codePlacette = "P35-01", jourCivil = "2026-10-27",
            operateur = "Agent d'essai", termine = true, visiteLeUtcMs = 1_792_000_000_000,
            positionX = 846164.76, positionY = 6686123.26, positionMesureeX = 846164.76, positionMesureeY = 6686123.26,
            rattacheParIdentite = false, repereEpsg = 2154, numeros = numeros, sansPositionMesuree = false,
        )
        assertEquals(attendu, lu)
        assertEquals(emptyList(), traces)
    }

    @Test
    fun `une valeur v1 sans position mesuree reste illisible - la v1 l'exige`() {
        val v1SansMesure = exempleV1Controle.replace("\"position_mesuree_x\":846164.76,\"position_mesuree_y\":6686123.26", "\"position_mesuree_x\":null,\"position_mesuree_y\":null")
        assertTrue(v1SansMesure != exempleV1Controle, "le remplacement doit porter")
        assertNull(lireTrace(v1SansMesure).first)
    }

    @Test
    fun `pointage et etat v1 toujours relus`() {
        val p = ValeurPointage("c", "p", 2.0, 250.0, "gr", "saisie", 1.7, 0.4, 105.9, 105.5, "2026-10-27", 846166.1, 6686122.0, 2154, "CHS", "m1", true, 1_790_000_000_000)
        assertEquals(p, ValeurPointage.lire(p.versJson().replace("{\"v\":2,", "{\"v\":1,")))
        val e = ValeurEtat("c", "u", "atteint", "animaux", "abroutissement", 35)
        assertEquals(e, ValeurEtat.lire(e.versJson().replace("{\"v\":2,", "{\"v\":1,")))
    }

    // ------------------------------------------------------------------ cohérence, vérifiée à la lecture

    @Test
    fun `sans position mesuree vrai avec une position mesuree - incoherent, ignore et trace`() {
        val t = exempleV2.replace("\"position_mesuree_x\":null,\"position_mesuree_y\":null", "\"position_mesuree_x\":846166.0,\"position_mesuree_y\":6686125.0")
        val (lu, traces) = lireTrace(t)
        assertNull(lu, "jamais complétée")
        assertEquals(1, traces.size)
    }

    @Test
    fun `sans position mesuree vrai mais rattache_par_identite faux - incoherent, ignore`() {
        val t = exempleV2.replace("\"rattache_par_identite\":true", "\"rattache_par_identite\":false")
        assertNull(lireTrace(t).first)
    }

    @Test
    fun `sans position mesuree faux sans position mesuree - incoherent, ignore`() {
        val t = exempleV2.replace("\"sans_position_mesuree\":true", "\"sans_position_mesuree\":false")
        assertNull(lireTrace(t).first)
    }

    @Test
    fun `une seule des deux positions mesurees nulle - incoherent, ignore`() {
        val t = exempleV2.replace("\"position_mesuree_x\":null", "\"position_mesuree_x\":846166.0")
        assertNull(lireTrace(t).first)
    }

    @Test
    fun `en v2, sans_position_mesuree est obligatoire`() {
        val t = exempleV2.replace(",\"sans_position_mesuree\":true", "")
        assertNull(lireTrace(t).first)
    }

    // ------------------------------------------------------------------ autre version : illisible, ignorée, tracée

    @Test
    fun `une valeur v3 est ignoree et tracee, jamais devinee`() {
        val (lu, traces) = lireTrace(exempleV2.replace("{\"v\":2,", "{\"v\":3,"))
        assertNull(lu)
        assertEquals(1, traces.size, "une trace par valeur ignorée")
        assertTrue(traces.single().isNotBlank())
    }

    @Test
    fun `v absent ou 0 - ignore et trace (regle nouvelle de la v2)`() {
        for (t in listOf(exempleV2.replace("{\"v\":2,", "{"), exempleV2.replace("{\"v\":2,", "{\"v\":0,"), exempleV1Controle.replace("{\"v\":1,", "{"))) {
            val (lu, traces) = lireTrace(t)
            assertNull(lu, t)
            assertEquals(1, traces.size, t)
        }
    }

    @Test
    fun `pointage et etat v3 ignores et traces`() {
        val p = ValeurPointage("c", "p", 2.0, 250.0, "gr", "saisie", 1.7, 0.4, 105.9, 105.5, "2026-10-27", 846166.1, 6686122.0, 2154, "CHS", null, false, 1_790_000_000_000)
        val tracesP = ArrayList<String>()
        assertNull(ValeurPointage.lire(p.versJson().replace("{\"v\":2,", "{\"v\":3,")) { tracesP += it })
        assertEquals(1, tracesP.size)
        val e = ValeurEtat("c", "u", "sain", null, null, 35)
        val tracesE = ArrayList<String>()
        assertNull(ValeurEtat.lire(e.versJson().replace("{\"v\":2,", "{\"v\":3,")) { tracesE += it })
        assertEquals(1, tracesE.size)
        // Lisibles : aucune trace.
        val rien = ArrayList<String>()
        assertNotNull(ValeurPointage.lire(p.versJson()) { rien += it })
        assertNotNull(ValeurEtat.lire(e.versJson()) { rien += it })
        assertEquals(emptyList(), rien)
    }

    @Test
    fun `un JSON illisible est trace aussi`() {
        val (lu, traces) = lireTrace("{\"v\":2,")
        assertNull(lu)
        assertEquals(1, traces.size)
    }

    @Test
    fun `un controle sans position termine compte comme Controlee - la valeur v2 n'est pas perdue`() {
        val lignes = listOf(LigneCourante("l1", "c1", FormatValeurs.TYPE_CONTROLE, sansPosition.versJson(), null))
        assertEquals(setOf("11111111-1111-4111-8111-111111111111"), PlacettesControlees.de(lignes))
        assertFalse(PlacettesControlees.de(listOf(LigneCourante("l1", "c1", "controle", exempleV2.replace("{\"v\":2,", "{\"v\":3,"), null))).isNotEmpty())
    }
}
