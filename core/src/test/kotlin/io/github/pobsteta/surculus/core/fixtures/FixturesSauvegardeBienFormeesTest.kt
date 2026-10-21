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
    fun `les six familles et les identifiants SV attendus sont presents`() {
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

    private fun cherche(e: com.google.gson.JsonElement, cle: String, sortie: MutableList<com.google.gson.JsonElement>) {
        if (e.isJsonObject) e.asJsonObject.entrySet().forEach { (k, v) -> if (k == cle) sortie += v else cherche(v, cle, sortie) }
        else if (e.isJsonArray) e.asJsonArray.forEach { cherche(it, cle, sortie) }
    }

    @Test
    fun `les motifs de refus et les signalements sont fixes, et uuid_contenu_different n'est jamais un motif de refus`() {
        val refus = setOf("version_future", "version_illisible")
        val signalements = setOf("fichier_tronque", "ligne_illisible", "empreinte_differente", "uuid_contenu_different")
        val vus = mutableSetOf<String>()
        cas.forEach { (_, c) ->
            val id = c.get("id").asString
            val motifs = mutableListOf<com.google.gson.JsonElement>().also { cherche(c.get("attendu"), "motif", it) }
            motifs.forEach { assertTrue(it.asString in refus, "$id : motif de refus ${it.asString}") ; vus += it.asString }
            val sigs = mutableListOf<com.google.gson.JsonElement>().also { cherche(c.get("attendu"), "signalements", it) }
            sigs.forEach { l -> l.asJsonArray.forEach { assertTrue(it.asString in signalements, "$id : signalement ${it.asString}") ; vus += it.asString } }
        }
        assertEquals(refus + signalements, vus, "tous les codes fixés sont exercés par au moins un cas")
    }

    @Test
    fun `un fichier abime n'est refuse en entier que pour une version N+1 ou illisible`() {
        val parId = cas.associate { it.second.get("id").asString to it.second }
        listOf("SV-20", "SV-22", "SV-23", "SV-25").forEach {
            assertTrue(parId[it]!!.get("attendu").asJsonObject.get("accepte").asBoolean, "$it : un fichier abîmé garde ses lignes saines")
        }
        val refuses = cas.filter { (_, c) -> mutableListOf<com.google.gson.JsonElement>().also { l -> cherche(c.get("attendu"), "accepte", l) }.any { !it.asBoolean } }
        refuses.forEach { (_, c) ->
            val m = mutableListOf<com.google.gson.JsonElement>().also { cherche(c.get("attendu"), "motif", it) }.map { it.asString }
            assertTrue(m.all { it in setOf("version_future", "version_illisible") }, "${c.get("id").asString} : refus pour $m")
        }
        listOf("SV-23", "SV-24", "SV-25", "SV-32", "SV-41", "SV-42", "SV-54").forEach {
            assertTrue("pilotage/journal.md, 2026-10-21" in parId[it]!!.get("source").asString, "$it : décision de Pascal non citée")
        }
        assertTrue("JR-13" in parId["SV-24"]!!.get("source").asString && "JR-12" in parId["SV-24"]!!.get("source").asString)
    }

    @Test
    fun `le gabarit de nom de SV-30 accepte un appareil opaque quelconque et refuse deux-points et segment vide`() {
        val c = cas.first { it.second.get("id").asString == "SV-30" }.second
        val gabarit = Regex(c.get("attendu").asJsonObject.get("gabarit").asString)
        listOf(
            "COUCHEY-2026-a3f9c2d1-20261014T090507Z.sursync",
            "COUCHEY-2026-Pixel7-20261014T090507Z.sursync",
            "COUCHEY-2026-0f3a-2026-10-14T09-05-07Z.sursync",
        ).forEach { assertTrue(gabarit.matches(it), "devrait être accepté : $it") }
        listOf(
            "COUCHEY-2026-a3f9c2d1-20261014T09:05:07Z.sursync",
            "COUCHEY-2026-a:3f9-20261014T090507Z.sursync",
            "COUCHEY-2026--20261014T090507Z.sursync",
            "COUCHEY-2026-a3f9c2d1-20261014T090507Z.zip",
        ).forEach { assertTrue(!gabarit.matches(it), "devrait être refusé : $it") }
    }

    @Test
    fun `les noms et les caracteres interdits des cas de nom sont coherents`() {
        cas.forEach { (_, c) ->
            val id = c.get("id").asString
            val entree = c.get("entree").asJsonObject
            listOf("nom_demande", "nom_existant").forEach { k -> entree.get(k)?.let { assertTrue(':' !in it.asString, "$id : $k contient un deux-points") } }
            val interdits = c.get("attendu").asJsonObject.get("caracteres_absents")?.asJsonArray?.map { it.asString } ?: return@forEach
            assertTrue(":" in interdits, "$id : le deux-points doit être interdit")
        }
        val sv32 = cas.first { it.second.get("id").asString == "SV-32" }.second.get("attendu").asJsonObject
        assertEquals(setOf("Jean", "Dupont", "Téléphone"), sv32.getAsJsonArray("sous_chaines_absentes").map { it.asString }.toSet())
    }

    @Test
    fun `N et M restent des parametres non fixes, y compris a un chiffre`() {
        val texte = JsonParser.parseString(File(dossier, "declenchement.json").readText()).asJsonObject
        val params = texte.getAsJsonObject("parametres_non_fixes")
        assertEquals(setOf("N", "M"), params.keySet())
        params.entrySet().forEach { (k, v) -> assertTrue(v.isJsonPrimitive && v.asJsonPrimitive.isString && v.asString.toIntOrNull() == null, "$k : valeur numérique fixée") }
        val nombres = mutableListOf<com.google.gson.JsonElement>()
        cherche(texte.getAsJsonArray("cas"), "saisies_depuis_derniere_sauvegarde", nombres)
        nombres.filter { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }.forEach { assertTrue(it.asInt in 0..1, "N fixé par une valeur ${it.asInt}") }
        val minutes = mutableListOf<com.google.gson.JsonElement>().also { l -> listOf("minutes_depuis_derniere_sauvegarde", "minutes_monotones_depuis_derniere_sauvegarde").forEach { cherche(texte.getAsJsonArray("cas"), it, l) } }
        minutes.filter { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }.forEach { assertTrue(it.asInt == 0, "M fixé par une valeur ${it.asInt}") }
    }
}
