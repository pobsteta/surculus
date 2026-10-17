package io.github.pobsteta.surculus.core.fixtures

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/** Actif dès maintenant : les fixtures de la mission 002 sont bien formées et fidèles aux référentiels. */
class FixturesBienFormeesTest {

    private val fichiers = Fixtures.tous()

    @Test
    fun `les fixtures attendues existent`() {
        val attendus = listOf(
            "rattachement-15m", "un-controle-par-jour", "emprise-et-france", "numerotation", "totaux-controle",
            "regarni", "identite", "etats-causes", "convergence", "declinaison-wmm2025", "chaine-azimut",
            "unites-azimut", "distance-horizontale",
            // fixtures/journal (mission 008)
            "projection", "conflits", "ordre-total", "horloge", "jour-civil",
        )
        assertEquals(attendus.sorted(), fichiers.map { it.famille }.sorted())
    }

    @Test
    fun `les scenarios du journal sont couverts, bien formes, et leurs identifiants sont JR`() {
        val journal = fichiers.filter { it.chemin.replace('\\', '/').startsWith("fixtures/journal/") }
        assertEquals(setOf("projection", "conflits", "ordre-total", "horloge", "jour-civil"), journal.map { it.famille }.toSet())
        val champs = listOf(
            "uuid", "objet_uuid", "type", "valeur", "remplace_uuid", "operateur", "appareil", "sequence",
            "saisi_le_utc", "monotone_ms", "demarrage",
        )
        val types = setOf("controle", "pointage", "etat", "annulation", "saut_horloge")
        for (f in journal) for (c in f.cas) {
            assertTrue(c.id.startsWith("JR-"), "${f.chemin} : identifiant ${c.id}")
            c.entree.getAsJsonArray("lignes")?.forEach { l ->
                val o = l.asJsonObject
                champs.forEach { assertTrue(o.has(it), "${c.id} : champ « $it » absent") }
                assertTrue(o.get("type").asString in types, "${c.id} : type ${o.get("type")}")
            }
            if (f.famille == "jour-civil") assertTrue(c.entree.has("saisi_le_utc"), "${c.id} : forme à plat")
            if (f.famille == "horloge") assertTrue(c.attendu.has("sauts_notes"), "${c.id} : sauts_notes absent")
            if (f.famille in setOf("projection", "conflits", "ordre-total")) {
                assertTrue(c.entree.getAsJsonArray("lignes").size() > 0, "${c.id} : aucune ligne")
            }
        }
    }

    @Test
    fun `les identifiants sont uniques dans chaque fichier et entre fichiers`() {
        val tous = fichiers.flatMap { f -> f.cas.map { f.chemin to it.id } }
        val doublons = tous.groupBy { it.second }.filter { it.value.size > 1 }.keys
        assertTrue(doublons.isEmpty(), "ids en double : $doublons")
    }

    @TestFactory
    fun `chaque cas est bien forme`(): List<DynamicTest> = fichiers.flatMap { f ->
        f.cas.map { c ->
            dynamicTest("${f.famille} / ${c.nom}") {
                assertTrue(c.id.isNotBlank() && c.id != "?", "id vide")
                assertTrue(c.description.isNotBlank(), "description vide")
                assertTrue(c.source.isNotBlank(), "source vide")
                assertTrue(c.json.get("entree")?.isJsonObject == true, "entree absente")
                assertTrue(c.json.get("attendu")?.isJsonObject == true, "attendu absent")
                // Une valeur numérique attendue avec tolérance doit dire que la tolérance est une proposition.
                if (c.attendu.keySet().any { it.startsWith("tolerance") }) {
                    val proposee = c.json.get("tolerance_proposee")?.asBoolean == true || f.json.has("tolerance_proposee_deg")
                    assertTrue(proposee, "tolérance non marquée comme proposition")
                }
            }
        }
    }

    @Test
    fun `les fichiers ont un nom de regle et une liste de cas`() {
        fichiers.forEach {
            assertTrue(it.json.get("regle")?.asString?.isNotBlank() == true, "${it.chemin} : regle absente")
            assertTrue(it.json.getAsJsonArray("cas").size() > 0, "${it.chemin} : aucun cas")
        }
    }

    @Test
    fun `les referentiels se lisent dans docs-referentiels`() {
        assertEquals(setOf("sain", "atteint", "mort", "absent"), Referentiels.etats)
        assertEquals(4, Referentiels.groupesCauses.size)
        assertTrue("DOU" in Referentiels.essences && "EPC" in Referentiels.essences)
        assertTrue(Referentiels.essences.all { it.length == 3 }, "codes ONF à 3 caractères")
    }

    @TestFactory
    fun `les etats et causes valides des fixtures viennent des referentiels`(): List<DynamicTest> =
        fichiers.flatMap { f ->
            f.cas.filter { acceptable(it) }.map { c ->
                dynamicTest("${f.famille} / ${c.id}") {
                    valeurs(c.entree, "etat").forEach {
                        assertTrue(it in Referentiels.etats || it == "non_observe", "état hors référentiel : $it")
                    }
                    valeurs(c.entree, "cause_groupe").forEach {
                        assertTrue(it in Referentiels.groupesCauses, "cause hors référentiel : $it")
                    }
                    valeurs(c.entree, "essence").forEach {
                        assertTrue(it in Referentiels.essences, "essence hors essences.csv : $it")
                    }
                }
            }
        }

    @Test
    fun `les cas d'etat invalide le sont vraiment`() {
        val invalides = fichiers.flatMap { it.cas }.filter { it.id.startsWith("E2-") }
        assertTrue(invalides.isNotEmpty())
        invalides.forEach { c ->
            val e = valeurs(c.entree, "etat").single()
            assertFalse(e in Referentiels.etats, "${c.id} : « $e » est dans le référentiel")
        }
        val essenceInventee = fichiers.flatMap { it.cas }.single { it.id == "E8" }
        assertFalse(valeurs(essenceInventee.entree, "essence").single() in Referentiels.essences)
    }

    @Test
    fun `aucune liste d'etats ou de causes n'est copiee dans les fixtures`() {
        val etats = Referentiels.etats
        fichiers.forEach { f -> f.cas.forEach { c -> tableaux(c.json).forEach { arr ->
            val chaines = arr.mapNotNull { if (it.isJsonPrimitive && it.asJsonPrimitive.isString) it.asString else null }
            assertTrue(chaines.toSet().intersect(etats).size < 3, "${f.chemin} ${c.id} : liste d'états copiée")
            assertTrue(chaines.toSet().intersect(Referentiels.groupesCauses).size < 3, "${f.chemin} ${c.id} : liste de causes copiée")
        } } }
    }

    @Test
    fun `les cas en France ou hors de France sont coherents avec l'emprise grossiere`() {
        // Boîte grossière de la France métropolitaine, propre à ce test (lat 41 à 52, lon -5,5 à 10).
        fun dedans(lat: Double, lon: Double) = lat in 41.0..52.0 && lon in -5.5..10.0
        val cas = fichiers.flatMap { it.cas }.filter { it.entree.has("lat") && it.attendu.has("en_france") }
        assertTrue(cas.size >= 8)
        cas.forEach {
            val lat = it.entree.get("lat").asDouble; val lon = it.entree.get("lon").asDouble
            assertEquals(it.attendu.get("en_france").asBoolean, dedans(lat, lon), "${it.id} : (lat $lat, lon $lon)")
        }
    }

    @Test
    fun `chaque regle du paragraphe 6 de la tranche 1 a au moins un cas`() {
        val familles = fichiers.map { it.famille }.toSet()
        listOf(
            "rattachement-15m", "un-controle-par-jour", "totaux-controle", "regarni", "identite", "etats-causes",
            "emprise-et-france", "numerotation", "unites-azimut", "distance-horizontale", "chaine-azimut",
            "convergence", "declinaison-wmm2025",
        )
            .forEach { assertTrue(it in familles, "règle sans fixture : $it") }
    }

    @Test
    fun `la comparaison des resultats detecte un ecart et accepte la tolerance`() {
        fun o(s: String) = com.google.gson.JsonParser.parseString(s).asJsonObject
        assertTrue(Comparaison.ecarts(o("""{"a":1.0,"tolerance_deg":0.01,"b":"x","c":null}"""), o("""{"a":1.005,"b":"x"}""")).isEmpty())
        assertEquals(1, Comparaison.ecarts(o("""{"a":1.0,"tolerance_deg":0.01}"""), o("""{"a":1.02}""")).size)
        assertEquals(1, Comparaison.ecarts(o("""{"b":"x"}"""), o("""{"b":"y"}""")).size)
        assertEquals(1, Comparaison.ecarts(o("""{"b":"x"}"""), o("""{}""")).size)
        assertEquals(1, Comparaison.ecarts(o("""{"c":null}"""), o("""{"c":3}""")).size)
    }

    // --- outils ---

    /** Les cas qui décrivent une saisie valide (ni refusée ni « accepte: false »). */
    private fun acceptable(c: CasFixture) =
        c.attendu.get("accepte")?.let { it.isJsonPrimitive && it.asBoolean } ?: (c.attendu.get("accepte") == null)

    private fun valeurs(e: JsonElement, cle: String): List<String> = when {
        e.isJsonObject -> e.asJsonObject.entrySet().flatMap { (k, v) ->
            if (k == cle && v.isJsonPrimitive && v.asJsonPrimitive.isString) listOf(v.asString) else valeurs(v, cle)
        }
        e.isJsonArray -> e.asJsonArray.flatMap { valeurs(it, cle) }
        else -> emptyList()
    }

    private fun tableaux(e: JsonElement): List<JsonArray> = when {
        e.isJsonArray -> listOf(e.asJsonArray) + e.asJsonArray.flatMap { tableaux(it) }
        e.isJsonObject -> e.asJsonObject.entrySet().flatMap { tableaux(it.value) }
        else -> emptyList()
    }
}
