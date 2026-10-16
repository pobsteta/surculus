package io.github.pobsteta.surculus.core.fixtures

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import kotlin.math.abs

/** Racine du dépôt : premier dossier parent qui contient `docs/referentiels/`. */
object Depot {
    val racine: File by lazy {
        var d: File? = File(System.getProperty("user.dir")).absoluteFile
        while (d != null && !File(d, "docs/referentiels").isDirectory) d = d.parentFile
        checkNotNull(d) { "racine du dépôt introuvable depuis ${System.getProperty("user.dir")}" }
    }
}

/** Un cas de `fixtures/` : `id`, `entree`, `attendu`, `source` (voir fixtures/README.md). */
class CasFixture(val fichier: String, val json: JsonObject) {
    val id: String get() = json.get("id")?.asString ?: "?"
    val description: String get() = json.get("description")?.asString ?: ""
    val entree: JsonObject get() = json.getAsJsonObject("entree")
    val attendu: JsonObject get() = json.getAsJsonObject("attendu")
    val source: String get() = json.get("source")?.asString ?: ""
    val nom: String get() = "$id : $description"
}

class FichierFixture(val chemin: String, val json: JsonObject) {
    /** Nom sans extension, par exemple `rattachement-15m`. */
    val famille: String get() = chemin.substringAfterLast('/').removeSuffix(".json")
    val cas: List<CasFixture> get() = json.getAsJsonArray("cas").map { CasFixture(chemin, it.asJsonObject) }
}

object Fixtures {
    /** Tous les `*.json` sous `fixtures/regles/` et `fixtures/reperes/`, triés. */
    fun tous(): List<FichierFixture> =
        listOf("fixtures/regles", "fixtures/reperes").flatMap { dossier ->
            File(Depot.racine, dossier).walkTopDown().filter { it.isFile && it.extension == "json" }.toList()
        }.sortedBy { it.path }.map { f ->
            FichierFixture(f.relativeTo(Depot.racine).path, JsonParser.parseString(f.readText()).asJsonObject)
        }

    fun sousDossier(prefixe: String): List<FichierFixture> = tous().filter { it.chemin.startsWith(prefixe) }
}

/** Référentiels lus dans `docs/referentiels/`, jamais recopiés dans les tests. */
object Referentiels {
    private fun lignes(nom: String) = File(Depot.racine, "docs/referentiels/$nom").readLines()

    private fun yamlListe(section: String, cle: String): Set<String> {
        var courante = ""
        val re = Regex("""^\s*-\s*$cle:\s*"?([^"\s]+)"?""")
        val out = linkedSetOf<String>()
        for (l in lignes("etats-plants.yaml")) {
            if (l.isNotEmpty() && !l[0].isWhitespace() && !l.startsWith("#") && l.endsWith(":")) courante = l.dropLast(1)
            if (courante == section) re.find(l)?.let { out += it.groupValues[1] }
        }
        return out
    }

    val etats: Set<String> by lazy { yamlListe("etats_dsf", "etat") }
    val groupesCauses: Set<String> by lazy { yamlListe("causes_dsf_groupes_bilan", "groupe") }
    val essences: Set<String> by lazy {
        lignes("essences.csv").filter { !it.startsWith("#") && !it.startsWith("code_onf") && it.isNotBlank() }
            .map { it.substringBefore(',').trim('"') }.toSet()
    }
}

/**
 * Compare le résultat d'une implémentation à `attendu`. Pour chaque clé de `attendu` (hors `tolerance_*`
 * et `limite`) la clé doit exister dans le résultat avec la même valeur ; les nombres sont comparés avec
 * la tolérance du cas (`tolerance_deg`, `tolerance_m`), sinon 1e-4 (azimuts de Groma). Retourne les écarts.
 */
object Comparaison {
    private const val TOLERANCE_PAR_DEFAUT = 1e-4

    fun ecarts(attendu: JsonObject, obtenu: JsonObject): List<String> {
        val tol = listOf("tolerance_deg", "tolerance_m").firstNotNullOfOrNull { attendu.get(it)?.asDouble } ?: TOLERANCE_PAR_DEFAUT
        val out = mutableListOf<String>()
        for ((k, v) in attendu.entrySet()) {
            if (k.startsWith("tolerance") || k == "limite") continue
            egal(k, v, obtenu.get(k), tol, out)
        }
        return out
    }

    private fun egal(chemin: String, a: JsonElement, o: JsonElement?, tol: Double, out: MutableList<String>) {
        when {
            a.isJsonNull -> if (o != null && !o.isJsonNull) out += "$chemin : attendu null, obtenu $o"
            o == null || o.isJsonNull -> out += "$chemin : attendu $a, obtenu absent"
            a.isJsonObject -> {
                if (!o.isJsonObject) out += "$chemin : objet attendu, obtenu $o"
                else for ((k, v) in a.asJsonObject.entrySet()) egal("$chemin.$k", v, o.asJsonObject.get(k), tol, out)
            }
            a.isJsonArray -> if (!(o.isJsonArray && sameArray(a.asJsonArray, o.asJsonArray))) out += "$chemin : attendu $a, obtenu $o"
            a.isJsonPrimitive && a.asJsonPrimitive.isNumber ->
                if (!(o.isJsonPrimitive && o.asJsonPrimitive.isNumber && abs(a.asDouble - o.asDouble) <= tol)) out += "$chemin : attendu $a (± $tol), obtenu $o"
            else -> if (a != o) out += "$chemin : attendu $a, obtenu $o"
        }
    }

    private fun sameArray(a: JsonArray, b: JsonArray) = a.size() == b.size() && a.zip(b).all { (x, y) -> x == y }
}
