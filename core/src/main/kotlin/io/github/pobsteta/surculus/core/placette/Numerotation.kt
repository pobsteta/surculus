/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 *
 * Origine : dépôt Groma (https://github.com/pobsteta/groma), fichier
 *           android/groma-core/src/main/kotlin/groma/core/Numerotation.kt, état au commit f12b644 ;
 *           règle normative : fixtures/numerotation/README.md de Groma (commit 6f953e1).
 * Modifications : paquet ; entrée en (azimut géographique, distance horizontale) depuis le piquet au lieu
 *           de (x, y) ; **sans masquage** par le diamètre, ni rayon, ni seuil de diamètre (les plants n'ont
 *           pas de diamètre, décision de Pascal 2026-10-16) ; numéros provisoires puis figés, et numéro
 *           suivant pour un plant ajouté (spec § 6).
 */
package io.github.pobsteta.surculus.core.placette

import io.github.pobsteta.surculus.core.geo.Azimut
import io.github.pobsteta.surculus.core.reperes.PlantDerive

/**
 * Plant à numéroter : azimut **géographique** depuis le piquet, en degrés dans [0, 360[, sens horaire
 * depuis le nord géographique, et distance **horizontale** en mètres (spec § 3).
 */
data class PlantANumeroter(val id: String, val azimutGeoDeg: Double, val distanceHorizontaleM: Double) {
    init {
        require(azimutGeoDeg.isFinite() && azimutGeoDeg >= 0.0 && azimutGeoDeg < 360.0) { "Azimut géographique hors [0, 360[ : $azimutGeoDeg" }
        require(distanceHorizontaleM.isFinite() && distanceHorizontaleM >= 0.0) { "Distance invalide : $distanceHorizontaleM" }
    }

    companion object {
        fun de(uuid: UuidPlant, derive: PlantDerive) =
            PlantANumeroter(uuid.valeur, derive.orientation.azimutGeographiqueDeg, derive.distanceHorizontaleM)
    }
}

/** Numéros des plants d'une placette, par UUID. */
sealed interface NumerosPlacette {
    val numeros: Map<UuidPlant, Int>

    /** Pendant le premier contrôle : recalculés à chaque pointage (numéros provisoires pendant le premier contrôle, figés à sa clôture ; arbitrage de Pascal du 2026-10-16 (spec § 6)). */
    data class Provisoires(override val numeros: Map<UuidPlant, Int>) : NumerosPlacette

    /** Après la clôture du premier contrôle : un numéro ne change plus jamais. */
    data class Figes(override val numeros: Map<UuidPlant, Int>) : NumerosPlacette
}

object Numerotation {
    /** Tolérance d'égalité d'azimut de Groma, en degrés. */
    const val TOLERANCE_DEG = 1.0

    /**
     * Règle de Groma, sans masquage. Le n° 1 est le plant de plus petit azimut à partir de 0° (pas de
     * bouclage) ; parmi les plants dont l'azimut est à moins de [toleranceDeg] du plus petit, le plus
     * proche est le n° 1. Les suivants sont pris par azimut relatif croissant, en groupes d'égalité ancrés
     * sur leur premier plant (écart < [toleranceDeg]) ; dans un groupe, le plus proche d'abord. Départage
     * final par l'identifiant (ordre des octets UTF-8). Rend les ids dans l'ordre des numéros 1, 2, 3…
     */
    fun ordonner(plants: List<PlantANumeroter>, toleranceDeg: Double = TOLERANCE_DEG): List<String> {
        require(plants.map { it.id }.toSet().size == plants.size) { "Identifiants de plants en double." }
        if (plants.isEmpty()) return emptyList()
        val azMin = plants.minOf { it.azimutGeoDeg }
        val candidats = plants.filter { it.azimutGeoDeg - azMin < toleranceDeg }
        val premier = candidats.sortedWith(
            compareBy<PlantANumeroter>({ it.distanceHorizontaleM }, { it.azimutGeoDeg }).thenBy(ordreId) { it.id },
        ).first()
        val idsCandidats = candidats.map { it.id }.toSet()
        fun rel(p: PlantANumeroter) = if (p.id in idsCandidats) 0.0 else Azimut.ramenerDans360(p.azimutGeoDeg - premier.azimutGeoDeg)

        val autres = plants.filter { it.id != premier.id }.sortedWith(
            compareBy<PlantANumeroter>({ rel(it) }, { it.distanceHorizontaleM }).thenBy(ordreId) { it.id },
        )
        val ordre = arrayListOf(premier.id)
        var i = 0
        while (i < autres.size) {
            val ancre = rel(autres[i])
            var j = i
            while (j < autres.size && rel(autres[j]) - ancre < toleranceDeg) j++
            ordre += autres.subList(i, j).sortedWith(
                compareBy<PlantANumeroter>({ it.distanceHorizontaleM }, { rel(it) }).thenBy(ordreId) { it.id },
            ).map { it.id }
            i = j
        }
        return ordre
    }

    /** Numéros provisoires du premier contrôle, recalculés sur tous les plants pointés jusqu'ici. */
    fun provisoires(plants: List<PlantANumeroter>, toleranceDeg: Double = TOLERANCE_DEG): NumerosPlacette.Provisoires =
        NumerosPlacette.Provisoires(ordonner(plants, toleranceDeg).withIndex().associate { (k, id) -> UuidPlant(id) to k + 1 })

    /** Clôture du premier contrôle : les numéros provisoires deviennent définitifs. Refusé s'ils le sont déjà. */
    fun figer(numeros: NumerosPlacette): Verdict<NumerosPlacette.Figes> = when (numeros) {
        is NumerosPlacette.Provisoires -> Verdict.Accepte(NumerosPlacette.Figes(numeros.numeros))
        is NumerosPlacette.Figes -> Verdict.Refuse(RefusPlacette.NumerosDejaFiges)
    }

    /** Numéro d'un plant pointé après coup : le plus grand numéro déjà donné plus un (1 si aucun), quel que soit son azimut. */
    fun numeroSuivant(figes: NumerosPlacette.Figes): Int = (figes.numeros.values.maxOrNull() ?: 0) + 1

    /** Ajoute un plant à une placette aux numéros figés, sans renuméroter les autres. */
    fun ajouter(figes: NumerosPlacette.Figes, uuid: UuidPlant): Verdict<NumerosPlacette.Figes> =
        if (uuid in figes.numeros) {
            Verdict.Refuse(RefusPlacette.UuidEnDouble(uuid))
        } else {
            Verdict.Accepte(NumerosPlacette.Figes(figes.numeros + (uuid to numeroSuivant(figes))))
        }

    /** Comparaison octet par octet en UTF-8, identique à l'ordre des chaînes de Python (Groma). */
    private val ordreId = Comparator<String> { a, b ->
        val x = a.toByteArray(Charsets.UTF_8)
        val y = b.toByteArray(Charsets.UTF_8)
        for (k in 0 until minOf(x.size, y.size)) {
            val c = (x[k].toInt() and 0xFF) - (y[k].toInt() and 0xFF)
            if (c != 0) return@Comparator c
        }
        x.size - y.size
    }
}
