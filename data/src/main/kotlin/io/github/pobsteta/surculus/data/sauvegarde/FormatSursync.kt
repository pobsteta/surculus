package io.github.pobsteta.surculus.data.sauvegarde

import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.pobsteta.surculus.data.journal.LigneJournal
import io.github.pobsteta.surculus.data.journal.TypeLigne
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Format `.sursync` (docs/formats/sursync-v1.md, ADR 0005) : JSON Lines compressé en gzip.
 * Ligne 1 : en-tête ; lignes suivantes : une saisie du journal ; dernière ligne : `{"sha256": ...}`, empreinte
 * SHA-256 (hexadécimal minuscule) des octets décompressés de toutes les lignes précédentes, fins de ligne comprises.
 */
object FormatSursync {
    /** Version écrite par cette application. */
    const val VERSION_COURANTE = 1

    /** Déclenchement (décision de Pascal, 2026-10-21) : 20 saisies ou 5 minutes, au premier des deux. */
    const val N_SAISIES = 20
    const val M_MINUTES = 5

    /** Nombre de sauvegardes gardées par tournée. */
    const val SAUVEGARDES_GARDEES = 5

    const val EXTENSION = ".sursync"

    fun sha256Hex(octets: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(octets).joinToString("") { "%02x".format(it) }
}

/** En-tête du fichier. Instants en millisecondes Unix, UTC. */
data class EnTeteSursync(
    val version: Int,
    val codeTournee: String,
    val versionSommier: String?,
    val appareil: String,
    val creeLeUtcMs: Long,
    val nbLignes: Int,
)

object EcritureSursync {
    fun ecrire(entete: EnTeteSursync, lignes: List<LigneJournal>): ByteArray {
        val contenu = ByteArrayOutputStream()
        fun ajouter(o: JsonObject) = contenu.write((o.toString() + "\n").toByteArray(Charsets.UTF_8))
        ajouter(JsonObject().apply {
            addProperty("format", "sursync")
            addProperty("version", entete.version)
            addProperty("code_tournee", entete.codeTournee)
            if (entete.versionSommier == null) add("version_sommier", JsonNull.INSTANCE)
            else addProperty("version_sommier", entete.versionSommier)
            addProperty("appareil", entete.appareil)
            addProperty("cree_le_utc_ms", entete.creeLeUtcMs)
            addProperty("nb_lignes", lignes.size)
        })
        lignes.forEach { ajouter(versJson(it)) }
        val empreinte = FormatSursync.sha256Hex(contenu.toByteArray())
        ajouter(JsonObject().apply { addProperty("sha256", empreinte) })
        return compresser(contenu.toByteArray())
    }

    internal fun compresser(octets: ByteArray): ByteArray {
        val sortie = ByteArrayOutputStream()
        GZIPOutputStream(sortie).use { it.write(octets) }
        return sortie.toByteArray()
    }

    /** `valeur` est écrite en **texte** (la chaîne JSON de la ligne) : l'aller-retour est exact au caractère. */
    internal fun versJson(l: LigneJournal) = JsonObject().apply {
        addProperty("uuid", l.uuid)
        addProperty("objet_uuid", l.objetUuid)
        addProperty("type", l.type)
        if (l.valeur == null) add("valeur", JsonNull.INSTANCE) else addProperty("valeur", l.valeur)
        if (l.remplaceUuid == null) add("remplace_uuid", JsonNull.INSTANCE) else addProperty("remplace_uuid", l.remplaceUuid)
        addProperty("operateur", l.operateur)
        addProperty("appareil", l.appareil)
        addProperty("sequence", l.sequence)
        addProperty("saisi_le_utc_ms", l.saisiLeUtcMs)
        if (l.heureGnssMs == null) add("heure_gnss_ms", JsonNull.INSTANCE) else addProperty("heure_gnss_ms", l.heureGnssMs)
        addProperty("monotone_ms", l.monotoneMs)
        addProperty("demarrage", l.demarrage)
        if (l.ageFixMs == null) add("age_fix_ms", JsonNull.INSTANCE) else addProperty("age_fix_ms", l.ageFixMs)
    }
}

/** Motifs de refus du fichier en entier. */
enum class MotifRefus(val code: String) {
    VERSION_FUTURE("version_future"),
    VERSION_ILLISIBLE("version_illisible"),
}

/** Avertissements et anomalies remontés à l'utilisateur. */
enum class Signalement(val code: String) {
    FICHIER_TRONQUE("fichier_tronque"),
    LIGNE_ILLISIBLE("ligne_illisible"),
    EMPREINTE_DIFFERENTE("empreinte_differente"),
    UUID_CONTENU_DIFFERENT("uuid_contenu_different"),
}

/**
 * Résultat de la lecture. Refusé en entier : `motif` non nul et aucune ligne. Sinon les lignes saines sont
 * dans l'ordre du fichier, les fautives comptées dans `lignesRefusees` (avec leur uuid quand il se lit).
 */
data class ResultatLecture(
    val motif: MotifRefus?,
    val entete: EnTeteSursync?,
    val lignes: List<LigneJournal>,
    val signalements: Set<Signalement>,
    val lignesRefusees: Int,
    val uuidRefuses: List<String>,
    val lignesIgnorees: Int,
) {
    val accepte: Boolean get() = motif == null
}

object LectureSursync {
    /**
     * Lit un `.sursync`. `versionCourante` : version de l'application ; sont lues `[courante-1, courante]`
     * (et au moins 1), une version plus haute est refusée (`version_future`), une version absente, illisible
     * ou trop ancienne l'est aussi (`version_illisible`), de même qu'un fichier de 0 octet ou non gzip.
     */
    fun lire(octets: ByteArray, versionCourante: Int = FormatSursync.VERSION_COURANTE): ResultatLecture {
        val decompresse = decompresser(octets) ?: return refus(MotifRefus.VERSION_ILLISIBLE)
        val signalements = linkedSetOf<Signalement>()
        if (decompresse.tronque) signalements += Signalement.FICHIER_TRONQUE

        // Découpage en lignes terminées par « \n » ; un reste sans fin de ligne est une ligne coupée.
        val brut = decompresse.octets
        val lignesBrutes = mutableListOf<Pair<Int, Int>>() // début, fin (exclue, sans \n)
        var debut = 0
        for (i in brut.indices) if (brut[i] == '\n'.code.toByte()) { lignesBrutes += debut to i; debut = i + 1 }

        fun texte(p: Pair<Int, Int>) = String(brut, p.first, p.second - p.first, Charsets.UTF_8)
        fun json(p: Pair<Int, Int>): JsonObject? = try {
            JsonParser.parseString(texte(p)).takeIf { it.isJsonObject }?.asJsonObject
        } catch (e: RuntimeException) { null }

        if (lignesBrutes.isEmpty()) return refus(MotifRefus.VERSION_ILLISIBLE)
        val jEntete = json(lignesBrutes[0]) ?: return refus(MotifRefus.VERSION_ILLISIBLE)
        val version = try {
            jEntete.get("version")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asBigDecimal
        } catch (e: RuntimeException) { null }
        if (version == null || version.stripTrailingZeros().scale() > 0 || version.signum() < 1) {
            return refus(MotifRefus.VERSION_ILLISIBLE)
        }
        val v = version.toBigInteger()
        if (v > versionCourante.toBigInteger()) return refus(MotifRefus.VERSION_FUTURE)
        if (v < maxOf(1, versionCourante - 1).toBigInteger()) return refus(MotifRefus.VERSION_ILLISIBLE)

        val entete = try {
            EnTeteSursync(
                version = v.toInt(),
                codeTournee = jEntete.get("code_tournee").asString,
                versionSommier = jEntete.get("version_sommier")?.takeIf { !it.isJsonNull }?.asString,
                appareil = jEntete.get("appareil").asString,
                creeLeUtcMs = jEntete.get("cree_le_utc_ms").asLong,
                nbLignes = jEntete.get("nb_lignes").asInt,
            )
        } catch (e: RuntimeException) { return refus(MotifRefus.VERSION_ILLISIBLE) }

        // Pied : dernière ligne terminée, objet à la seule clé « sha256 ».
        var finSaisies = lignesBrutes.size
        var pied: String? = null
        if (lignesBrutes.size >= 2) {
            val dernier = json(lignesBrutes.last())
            if (dernier != null && dernier.keySet() == setOf("sha256")) {
                pied = try { dernier.get("sha256").asString } catch (e: RuntimeException) { "" }
                finSaisies = lignesBrutes.size - 1
            }
        }
        if (pied == null) {
            signalements += Signalement.FICHIER_TRONQUE
        } else {
            val attendu = FormatSursync.sha256Hex(brut.copyOfRange(0, lignesBrutes.last().first))
            if (!attendu.equals(pied, ignoreCase = false)) signalements += Signalement.EMPREINTE_DIFFERENTE
        }

        var refusees = 0
        val uuidRefuses = mutableListOf<String>()
        val lus = mutableListOf<LigneJournal>()
        for (i in 1 until finSaisies) {
            val o = json(lignesBrutes[i])
            val ligne = o?.let(::versLigne)
            if (ligne == null) {
                refusees++
                signalements += Signalement.LIGNE_ILLISIBLE
                o?.get("uuid")?.takeIf { it.isJsonPrimitive }?.asString?.let { uuidRefuses += it }
            } else lus += ligne
        }

        // Doublons d'uuid dans le fichier.
        val parUuid = lus.groupBy { it.uuid }
        val fautifs = parUuid.filterValues { copies -> copies.any { it != copies[0] } }.keys
        var ignorees = 0
        val gardees = mutableListOf<LigneJournal>()
        val deja = mutableSetOf<String>()
        for (l in lus) {
            when {
                l.uuid in fautifs -> {
                    refusees++
                    if (l.uuid !in uuidRefuses) uuidRefuses += l.uuid
                }
                deja.add(l.uuid) -> gardees += l
                else -> ignorees++
            }
        }
        if (fautifs.isNotEmpty()) signalements += Signalement.UUID_CONTENU_DIFFERENT
        return ResultatLecture(null, entete, gardees, signalements, refusees, uuidRefuses, ignorees)
    }

    private fun refus(m: MotifRefus) = ResultatLecture(m, null, emptyList(), emptySet(), 0, emptyList(), 0)

    private class Decompresse(val octets: ByteArray, val tronque: Boolean)

    /** `null` : pas un gzip lisible du tout (0 octet, en-tête gzip absent). Gzip coupé : le début lisible est gardé. */
    private fun decompresser(octets: ByteArray): Decompresse? {
        if (octets.isEmpty()) return null
        val sortie = ByteArrayOutputStream()
        val entree = try { GZIPInputStream(ByteArrayInputStream(octets)) } catch (e: IOException) { return null }
        var tronque = false
        try {
            val tampon = ByteArray(8192)
            while (true) {
                val n = entree.read(tampon)
                if (n < 0) break
                sortie.write(tampon, 0, n)
            }
        } catch (e: IOException) {
            tronque = true
        }
        return Decompresse(sortie.toByteArray(), tronque)
    }

    /** `null` si la ligne n'a pas tous ses champs avec le bon type, ou un type hors de la liste fermée. */
    internal fun versLigne(o: JsonObject): LigneJournal? = try {
        fun texte(c: String) = o.get(c).also { require(it.isJsonPrimitive && it.asJsonPrimitive.isString) }.asString
        fun texteOuNull(c: String) = o.get(c).let { if (it == null || it.isJsonNull) null else { require(it.asJsonPrimitive.isString); it.asString } }
        fun entier(c: String) = o.get(c).also { require(it.isJsonPrimitive && it.asJsonPrimitive.isNumber) }.asBigDecimal.longValueExact()
        fun entierOuNull(c: String) = o.get(c).let { if (it == null || it.isJsonNull) null else entier(c) }
        require(o.has("valeur") && o.has("remplace_uuid") && o.has("heure_gnss_ms") && o.has("age_fix_ms"))
        val type = texte("type")
        require(type in TypeLigne.TOUS)
        LigneJournal(
            uuid = texte("uuid"), objetUuid = texte("objet_uuid"), type = type, valeur = texteOuNull("valeur"),
            remplaceUuid = texteOuNull("remplace_uuid"), operateur = texte("operateur"), appareil = texte("appareil"),
            sequence = entier("sequence"), saisiLeUtcMs = entier("saisi_le_utc_ms"),
            heureGnssMs = entierOuNull("heure_gnss_ms"), monotoneMs = entier("monotone_ms"),
            demarrage = entier("demarrage"), ageFixMs = entierOuNull("age_fix_ms"),
        )
    } catch (e: RuntimeException) { null }
}
