package io.github.pobsteta.surculus.data.sauvegarde

import java.text.Normalizer
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Nom d'un `.sursync` : `<code-tournée>-<appareil>-<horodatage UTC>.sursync` (ADR 0005, confidentialité).
 * - le code de tournée est assaini (accents retirés, tout caractère hors lettres, chiffres, `.`, `_`, `-`
 *   devient `_`) : ni `:`, ni `/`, ni `\`, ni espace ;
 * - `<appareil>` est un **identifiant opaque** (lettres et chiffres seulement) : jamais le nom du téléphone, de
 *   son propriétaire ni de l'agent (décision 1 de Pascal, 2026-10-21) ;
 * - l'horodatage s'écrit `yyyyMMdd'T'HHmmss'Z'` (UTC), sans `:`.
 * Un nom déjà pris n'est jamais écrasé : le nouveau reçoit un suffixe `-2`, `-3`... (décision 3).
 */
object NomSauvegarde {
    private val FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)
    private val MOTIF = Regex("^(.+)-([A-Za-z0-9]+)-(\\d{8}T\\d{6}Z)(?:-(\\d+))?\\.sursync$")

    fun assainirCode(code: String): String {
        val sansAccent = Normalizer.normalize(code, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
        val propre = sansAccent.replace(Regex("[^A-Za-z0-9._-]+"), "_").trim('_', '.')
        return propre.ifEmpty { "tournee" }
    }

    /** Génère un identifiant d'appareil opaque (8 caractères hexadécimaux), à garder une fois par installation. */
    fun nouvelIdentifiantAppareil(hasard: java.util.Random = java.security.SecureRandom()): String =
        "%08x".format(hasard.nextInt())

    fun nom(codeTournee: String, appareilOpaque: String, instantUtcMs: Long): String {
        require(appareilOpaque.isNotEmpty() && appareilOpaque.all { it.isLetterOrDigit() && it.code < 128 }) {
            "l'identifiant d'appareil doit être opaque : lettres et chiffres ASCII, non vide"
        }
        return "${assainirCode(codeTournee)}-$appareilOpaque-${FORMAT.format(Instant.ofEpochMilli(instantUtcMs))}$EXT"
    }

    /** Premier nom libre : `nom`, sinon `<base>-2.sursync`, `-3`... Les fichiers existants ne sont jamais touchés. */
    fun nomLibre(nom: String, existants: Set<String>): String {
        if (nom !in existants) return nom
        val base = nom.removeSuffix(EXT)
        var n = 2
        while ("$base-$n$EXT" in existants) n++
        return "$base-$n$EXT"
    }

    /** Décompose un nom produit par [nom] ; `null` s'il n'en a pas la forme. */
    fun decomposer(nom: String): NomDecompose? = MOTIF.matchEntire(nom)?.let {
        NomDecompose(it.groupValues[1], it.groupValues[2], it.groupValues[3], it.groupValues[4].toIntOrNull() ?: 1)
    }

    private const val EXT = FormatSursync.EXTENSION
}

data class NomDecompose(val codeAssaini: String, val appareil: String, val horodatage: String, val rang: Int)

/** Rotation : par tournée, on ne supprime que les plus anciennes de la même tournée. */
object Rotation {
    /** Noms à supprimer pour ne garder que les `garder` plus récents de la tournée `codeTournee`. */
    fun asupprimer(
        existants: Collection<String>,
        codeTournee: String,
        garder: Int = FormatSursync.SAUVEGARDES_GARDEES,
    ): List<String> {
        val code = NomSauvegarde.assainirCode(codeTournee)
        val deLaTournee = existants.mapNotNull { n -> NomSauvegarde.decomposer(n)?.takeIf { it.codeAssaini == code }?.let { n to it } }
            .sortedWith(compareBy({ it.second.horodatage }, { it.second.rang }, { it.first }))
        return deLaTournee.dropLast(garder).map { it.first }
    }
}
