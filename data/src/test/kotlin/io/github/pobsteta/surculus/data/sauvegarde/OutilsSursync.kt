package io.github.pobsteta.surculus.data.sauvegarde

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.pobsteta.surculus.data.journal.LigneJournal
import io.github.pobsteta.surculus.data.journal.ScenariosJournal
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

/** Outils des tests de la sauvegarde : construction et altération de fichiers `.sursync`, sans passer par l'écriture testée. */
object OutilsSursync {
    /** `journal/projection.json#JR-02` : les lignes d'entrée du cas cité de `fixtures/journal/`. */
    fun journalDe(ref: String, tournee: String = "COUCHEY-2026"): List<LigneJournal> {
        val (fichier, id) = ref.split("#")
        // Les fixtures du journal n'ont pas de champ tournée : l'assistant affecte celle du fichier d'essai.
        return ScenariosJournal.cas(fichier.removePrefix("journal/").removeSuffix(".json")).first { it.id == id }.lignes
            .map { it.copy(codeTournee = tournee) }
    }

    fun entete(version: Any? = 1, nb: Int = 0): JsonObject = JsonObject().apply {
        addProperty("format", "sursync")
        when (version) {
            null -> {}
            is Number -> addProperty("version", version)
            else -> addProperty("version", version.toString())
        }
        addProperty("code_tournee", "COUCHEY-2026")
        add("version_sommier", com.google.gson.JsonNull.INSTANCE)
        addProperty("appareil", "a3f9c2d1")
        addProperty("cree_le_utc_ms", 1_760_000_000_000L)
        addProperty("nb_lignes", nb)
    }

    /** Fichier bâti à la main : lignes JSON déjà sérialisées, pied recalculé ou omis. */
    fun fichier(lignes: List<String>, pied: Boolean = true, empreinte: String? = null): ByteArray {
        val contenu = lignes.joinToString("") { it + "\n" }.toByteArray(Charsets.UTF_8)
        val tout = if (pied) contenu + """{"sha256":"${empreinte ?: FormatSursync.sha256Hex(contenu)}"}""".plus("\n").toByteArray() else contenu
        return EcritureSursync.compresser(tout)
    }

    fun fichierDe(lignes: List<LigneJournal>, version: Any? = 1): ByteArray =
        fichier(listOf(entete(version, lignes.size).toString()) + lignes.map { EcritureSursync.versJson(it).toString() })

    fun ecrire(lignes: List<LigneJournal>, code: String = "COUCHEY-2026", ms: Long = 1_760_000_000_000L): ByteArray =
        EcritureSursync.ecrire(EnTeteSursync(FormatSursync.VERSION_COURANTE, code, "0.31.1", "a3f9c2d1", ms, lignes.size), lignes)

    fun decompresser(octets: ByteArray): String = GZIPInputStream(ByteArrayInputStream(octets)).readBytes().toString(Charsets.UTF_8)

    /** Lignes du contenu décompressé, pied compris. */
    fun lignesTexte(octets: ByteArray): List<String> = decompresser(octets).trimEnd('\n').split("\n")

    /** Recompresse des lignes en recalculant le pied (les lignes ne comprennent pas l'ancien pied). */
    fun refaire(lignes: List<String>): ByteArray = fichier(lignes)

    fun json(s: String): JsonObject = JsonParser.parseString(s).asJsonObject
}
