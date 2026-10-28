/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.carte

import io.github.pobsteta.surculus.core.gnss.Position
import kotlinx.coroutines.delay

/** Durée de la moyenne de position à l'arrivée (spec § 4.3) : la même pour le guidage et pour le contrôle. */
const val SECONDES_DE_MOYENNE_ARRIVEE = 5

/**
 * La moyenne de l'arrivée, **écrite une seule fois** (reprise de la 012-B1, décision de Pascal sur la #108, 2026-10-28) :
 * une position par seconde pendant [secondes] secondes, le décompte rendu par [surSeconde] (0 à [secondes] - 1). Le guidage
 * (rattachement) et le contrôle (ouverture de la session) s'en servent tous deux ; ils moyennent ensuite eux-mêmes.
 * [attendre] vaut une seconde ; les tests la remplacent.
 */
suspend fun collecterPositionsArrivee(
    position: () -> Position?,
    surSeconde: (Int) -> Unit = {},
    secondes: Int = SECONDES_DE_MOYENNE_ARRIVEE,
    attendre: suspend () -> Unit = { delay(1000) },
): List<Position> {
    val positions = ArrayList<Position>()
    repeat(secondes) { n ->
        surSeconde(n)
        attendre()
        position()?.let(positions::add)
    }
    return positions
}
