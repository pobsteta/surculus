/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 *
 * Écrit pour Surculus (mission 013, ADR 0004) ; aucun code repris de Marculus ni de terrain.
 */
package io.github.pobsteta.surculus.core.dictee

/**
 * Lecture d'un nombre français de 1 à 400 dit en lettres. La génération des phrases de la grammaire
 * vient de `terrain-voix` (`fr.nemeton.terrain.voix.NombresFrancais.jetons`) ; `terrain-voix` ne sait
 * pas lire un nombre, d'où cette analyse. Elle accepte les formes à espaces ou à traits d'union
 * (« quarante-cinq », « quarante cinq »), « quatre vingt » et « quatre vingts », « cent » et « cents »
 * en fin de nombre. Hors forme : `null`, jamais d'approximation.
 */
object LectureNombres {
    private val unites = listOf("un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf")
    private val dixASeize = listOf("dix", "onze", "douze", "treize", "quatorze", "quinze", "seize")
    private val dizaines = mapOf("vingt" to 20, "trente" to 30, "quarante" to 40, "cinquante" to 50, "soixante" to 60)

    /** Valeur de [texte] (mots séparés par des espaces ou des traits d'union), ou `null` si ce n'est pas un nombre de 1 à 400. */
    fun analyser(texte: String): Int? {
        if (texte.startsWith("-") || texte.endsWith("-") || texte.contains("--") || texte.contains("- ") || texte.contains(" -")) return null
        val t = texte.split(' ', '-').filter { it.isNotEmpty() }
        if (t.isEmpty()) return null
        var centaines = 0
        var cents = false
        var i = 0
        if (t.size >= 2 && t[0] in listOf("deux", "trois", "quatre") && (t[1] == "cent" || t[1] == "cents")) {
            centaines = unites.indexOf(t[0]) + 1
            cents = t[1] == "cents"
            i = 2
        } else if (t[0] == "cent" || t[0] == "cents") {
            centaines = 1
            cents = t[0] == "cents"
            i = 1
        }
        val reste = t.drop(i)
        val v = when {
            reste.isEmpty() -> if (centaines == 0) return null else 0
            cents -> return null // « cents » ne se dit qu'en fin de nombre
            else -> sousCent(reste) ?: return null
        }
        val total = centaines * 100 + v
        return if (total in 1..400) total else null
    }

    private fun sousVingt(t: List<String>): Int? = when {
        t.size == 1 -> unites.indexOf(t[0]).takeIf { it >= 0 }?.plus(1)
            ?: dixASeize.indexOf(t[0]).takeIf { it >= 0 }?.plus(10)
        t.size == 2 && t[0] == "dix" -> unites.indexOf(t[1]).takeIf { it in 6..8 }?.plus(11)
        else -> null
    }

    private fun sousCent(t: List<String>): Int? {
        sousVingt(t)?.let { return it }
        if (t[0] == "quatre" && t.size >= 2 && (t[1] == "vingt" || t[1] == "vingts")) {
            val r = t.drop(2)
            if (r.isEmpty()) return 80
            if (t[1] == "vingts") return null
            return sousVingt(r)?.plus(80)
        }
        val base = dizaines[t[0]] ?: return null
        val r = t.drop(1)
        if (r.isEmpty()) return base
        if (r == listOf("et", "un")) return base + 1
        if (r == listOf("et", "onze") && base == 60) return 71
        val s = sousVingt(r) ?: return null
        // « un » et « onze » ne se disent qu'avec « et » (vingt et un, soixante et onze), traités plus haut.
        if (s == 1 || (base == 60 && s == 11)) return null
        return if (base == 60 || s in 1..9) base + s else null
    }
}
