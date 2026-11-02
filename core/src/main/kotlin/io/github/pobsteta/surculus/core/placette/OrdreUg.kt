/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.core.placette

import java.math.BigInteger

/**
 * Ordre d'affichage des UG candidates (mission 086, reprise 1/3) : comme des nombres quand l'UG en est un (9, 10, 12, 100),
 * les UG non numériques ensuite (ordre du texte), les placettes sans UG en dernier ; à égalité, l'UUID de la placette,
 * pour que l'ordre de la tournée ne change rien.
 */
object OrdreUg : Comparator<Piquet> {
    private fun nombre(ug: String?): BigInteger? = ug?.trim()?.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }?.let(::BigInteger)

    override fun compare(a: Piquet, b: Piquet): Int {
        val rang = { p: Piquet -> if (p.ug == null) 2 else if (nombre(p.ug) != null) 0 else 1 }
        val r = rang(a).compareTo(rang(b))
        if (r != 0) return r
        val parUg = when (rang(a)) {
            0 -> nombre(a.ug)!!.compareTo(nombre(b.ug)!!)
            1 -> a.ug!!.compareTo(b.ug!!)
            else -> 0
        }
        return if (parUg != 0) parUg else a.placette.valeur.compareTo(b.placette.valeur)
    }
}
