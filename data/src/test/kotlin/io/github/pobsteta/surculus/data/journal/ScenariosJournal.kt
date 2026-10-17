package io.github.pobsteta.surculus.data.journal

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.time.Instant

/** Cas de les fichiers de `fixtures/journal/` (écrits par le testeur, jamais modifiés ici : voir fixtures/README.md). */
class CasJournal(val fichier: String, val json: JsonObject) {
    val id: String get() = json.get("id").asString
    val entree: JsonObject get() = json.getAsJsonObject("entree")
    val attendu: JsonObject get() = json.getAsJsonObject("attendu")
    val nom: String get() = "$id : ${json.get("description").asString}"

    /** Lignes dans l'ordre du fichier. */
    val lignes: List<LigneJournal> get() = entree.getAsJsonArray("lignes")?.map { ScenariosJournal.ligne(it.asJsonObject) }.orEmpty()

    /** Les ordres d'arrivée du cas (au moins un : l'ordre du fichier). */
    val ordresDArrivee: List<List<LigneJournal>>
        get() {
            val l = lignes
            val ordres = entree.getAsJsonArray("ordres_d_arrivee") ?: return listOf(l)
            return ordres.map { o -> o.asJsonArray.map { l[it.asInt] } }
        }
}

object ScenariosJournal {
    private val racine: File by lazy {
        var d: File? = File(System.getProperty("user.dir")).absoluteFile
        while (d != null && !File(d, "fixtures/journal").isDirectory) d = d.parentFile
        checkNotNull(d) { "racine du dépôt introuvable depuis ${System.getProperty("user.dir")}" }
    }

    fun cas(fichier: String): List<CasJournal> {
        val f = File(racine, "fixtures/journal/$fichier.json")
        val racineJson = JsonParser.parseString(f.readText()).asJsonObject
        return racineJson.getAsJsonArray("cas").map { CasJournal(fichier, it.asJsonObject) }
    }

    fun instantMs(iso: String): Long = Instant.parse(iso).toEpochMilli()

    private fun JsonObject.texteOuNull(cle: String): String? = get(cle)?.takeIf { !it.isJsonNull }?.asString
    private fun JsonObject.longOuNull(cle: String): Long? = get(cle)?.takeIf { !it.isJsonNull }?.asLong

    fun ligne(o: JsonObject) = LigneJournal(
        uuid = o.get("uuid").asString,
        objetUuid = o.get("objet_uuid").asString,
        type = o.get("type").asString,
        valeur = o.get("valeur")?.takeIf { !it.isJsonNull }?.toString(),
        remplaceUuid = o.texteOuNull("remplace_uuid"),
        operateur = o.get("operateur").asString,
        appareil = o.get("appareil").asString,
        sequence = o.get("sequence").asLong,
        saisiLeUtcMs = instantMs(o.get("saisi_le_utc").asString),
        heureGnssMs = o.texteOuNull("heure_gnss")?.let(::instantMs),
        monotoneMs = o.get("monotone_ms").asLong,
        demarrage = o.get("demarrage").asLong,
        ageFixMs = o.longOuNull("age_fix_ms"),
    )

    /** Sous-ensemble de `attendu.projection` sous forme comparable : `objet|type` -> description. */
    fun attenduProjection(attendu: JsonObject): Map<String, EtatAttendu> =
        attendu.getAsJsonObject("projection").entrySet().associate { (cle, v) ->
            val o = v.asJsonObject
            cle to EtatAttendu(
                courant = o.texteOuNull("courant"),
                conflit = o.get("conflit").asBoolean,
                tetes = o.getAsJsonArray("tetes").map(JsonElement::getAsString),
                anomalies = o.getAsJsonArray("anomalies").map(JsonElement::getAsString),
            )
        }

    fun chaines(attendu: JsonObject, cle: String): List<String>? =
        attendu.getAsJsonArray(cle)?.map(JsonElement::getAsString)
}

data class EtatAttendu(val courant: String?, val conflit: Boolean, val tetes: List<String>, val anomalies: List<String>) {
    companion object {
        fun de(e: EtatCle) = EtatAttendu(e.courant, e.conflit, e.tetes, e.anomalies)
    }
}
