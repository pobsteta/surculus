package io.github.pobsteta.surculus.core.fixtures

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Cas d'acceptation de la sauvegarde `.sursync` (mission 024, `fixtures/sauvegarde/`).
 * Actif dès maintenant : vérifie la forme des cas, pas la sauvegarde elle-même, que la mission 009
 * branchera. Les cas ne sont pas rejoués ici.
 */
class FixturesSauvegardeBienFormeesTest {
    private val dossier = File(Depot.racine, "fixtures/sauvegarde")
    private val fichiers = dossier.listFiles { f -> f.extension == "json" }!!.sortedBy { it.name }
    private val cas = fichiers.flatMap { f ->
        JsonParser.parseString(f.readText()).asJsonObject.getAsJsonArray("cas").map { f.name to it.asJsonObject }
    }

    @Test
    fun `les cinq familles et les identifiants SV attendus sont presents`() {
        assertEquals(
            listOf("declenchement.json", "ecrasement.json", "fichiers-refuses.json", "noms-fichier.json", "restauration.json", "versions.json"),
            fichiers.map { it.name },
        )
        val attendus = (1..8) + (10..13) + (20..25) + (30..34) + (40..42) + (50..55)
        assertEquals(attendus.map { "SV-%02d".format(it) }, cas.map { it.second.get("id").asString }.sorted())
    }

    @Test
    fun `chaque cas a un identifiant unique dans tout fixtures, une description, une entree, un attendu et une source`() {
        val autres = File(Depot.racine, "fixtures").walkTopDown().filter { it.extension == "json" && it.parentFile.name != "sauvegarde" }
            .flatMap { Regex("\"id\": \"(SV-\\d+)\"").findAll(it.readText()).map { m -> m.groupValues[1] } }.toList()
        assertTrue(autres.isEmpty(), "ids SV déjà pris ailleurs : $autres")
        cas.forEach { (f, c) ->
            val id = c.get("id").asString
            assertTrue(Regex("^SV-\\d{2}$").matches(id), "$f : id $id")
            listOf("description", "source").forEach { assertTrue(c.get(it)?.asString?.isNotBlank() == true, "$id : $it absent") }
            listOf("entree", "attendu").forEach { assertTrue(c.has(it) && c.get(it).isJsonObject, "$id : $it absent") }
        }
        assertEquals(cas.size, cas.map { it.second.get("id").asString }.toSet().size)
    }

    @Test
    fun `les journaux cites existent dans fixtures-journal`() {
        val ref = Regex("^(journal/[a-z-]+\\.json)#(JR-\\d+)$")
        val cites = mutableListOf<String>()
        fun parcourir(e: com.google.gson.JsonElement) {
            if (e.isJsonObject) e.asJsonObject.entrySet().forEach { (k, v) -> if (k == "journal_de") cites += v.asString else parcourir(v) }
            else if (e.isJsonArray) e.asJsonArray.forEach { parcourir(it) }
        }
        cas.forEach { parcourir(it.second.get("entree")) }
        assertTrue(cites.isNotEmpty())
        cites.forEach { c ->
            val m = requireNotNull(ref.matchEntire(c)) { "référence mal formée : $c" }
            val texte = File(Depot.racine, "fixtures/${m.groupValues[1]}").readText()
            assertTrue(texte.contains("\"id\": \"${m.groupValues[2]}\""), "$c : cas introuvable")
        }
    }

    @Test
    fun `les motifs de refus sont fixes et les decisions de Pascal du 2026-10-21 sont citees`() {
        val motifs = setOf("version_future", "version_illisible", "fichier_tronque", "ligne_illisible", "empreinte_differente", "uuid_contenu_different")
        cas.forEach { (_, c) ->
            val texte = c.get("attendu").toString()
            Regex("\"motif\":\"([a-z_]+)\"").findAll(texte).forEach { assertTrue(it.groupValues[1] in motifs, "${c.get("id").asString} : motif ${it.groupValues[1]}") }
        }
        val sources = cas.associate { it.second.get("id").asString to it.second.get("source").asString }
        listOf("SV-23", "SV-32", "SV-41", "SV-42").forEach { assertTrue("pilotage/journal.md, 2026-10-21" in sources[it]!!, "$it : décision de Pascal non citée") }
    }

    @Test
    fun `les noms de fichier attendus ne contiennent ni deux-points ni nom de personne`() {
        cas.forEach { (_, c) ->
            val nom = c.get("entree").asJsonObject.get("nom_demande")?.asString ?: return@forEach
            assertTrue(':' !in nom && !nom.contains("Jean") && !nom.contains("Dupont"), "$nom")
        }
    }

    @Test
    fun `N et M restent des parametres non fixes`() {
        val texte = File(dossier, "declenchement.json").readText()
        assertTrue(texte.contains("parametres_non_fixes"))
        assertTrue(!Regex("\"saisies_depuis_derniere_sauvegarde\": ?\\d{2,}").containsMatchIn(texte), "une valeur numérique de N est fixée")
    }
}
