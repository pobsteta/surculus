/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.tournee

import java.text.Normalizer
import java.util.Locale

/**
 * Ce que la recherche lit d'une tournée : forêt, opérateur, date de préparation, nom du dossier, et nom du projet
 * (toujours cherché, même quand la forêt existe : décision de Pascal, 2026-10-24).
 */
data class ChampsTournee(
    val foret: String?,
    val operateur: String?,
    val prepareLe: String?,
    val dossier: String,
    val nomProjet: String? = null,
)

/** Sans accents ni casse, « œ » = « oe » et « æ » = « ae » (« boeuf » trouve « Bœuf »). Le « ß » ne change pas. */
internal fun normaliser(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)
        .replace("œ", "oe").replace("æ", "ae")

/** « 2026-10-12 08:30:00 » devient « 12/10/2026 » ; un autre texte reste tel quel. */
internal fun dateFrancaise(d: String): String =
    Regex("^(\\d{4})-(\\d{2})-(\\d{2})").find(d)?.destructured?.let { (a, m, j) -> "$j/$m/$a" } ?: d

/** Recherche vide = tout. Sinon : la saisie se trouve dans la forêt, l'opérateur, la date (fichier ou française) ou le nom du dossier. */
fun correspond(champs: ChampsTournee, recherche: String): Boolean {
    val n = normaliser(recherche.trim())
    if (n.isEmpty()) return true
    val lus = listOfNotNull(champs.foret, champs.operateur, champs.prepareLe, champs.prepareLe?.let(::dateFrancaise), champs.dossier, champs.nomProjet)
    return lus.any { normaliser(it).contains(n) }
}

/** Ce que l'écran dit quand la liste affichée est vide. */
enum class MessageVide { AUCUNE_TROUVEE, AUCUNE_RENDUE, AUCUNE_OUVERTE }

/** Recherche non vide : rien ne correspond. Sinon, c'est la liste de la puce choisie qui est vide. */
fun messageVide(recherche: String, voirRendues: Boolean): MessageVide = when {
    recherche.isNotBlank() -> MessageVide.AUCUNE_TROUVEE
    voirRendues -> MessageVide.AUCUNE_RENDUE
    else -> MessageVide.AUCUNE_OUVERTE
}

/** (rendues, ouvertes), dans l'ordre d'origine. */
fun <T> partager(lignes: List<T>, rendue: (T) -> Boolean): Pair<List<T>, List<T>> = lignes.partition(rendue)
