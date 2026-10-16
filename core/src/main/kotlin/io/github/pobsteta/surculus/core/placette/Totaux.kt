/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.core.placette

import kotlin.math.floor

/**
 * Totaux d'un contrôle, écrits dans la couche `controles` pour l'import sommieR 0.31.1. Toujours déduits
 * des plants, jamais saisis (spec § 4.3). Invariants de sommieR 0.28.0 (spec § 6) vérifiés à la construction.
 */
data class Totaux(
    val nbTotal: Int,
    val nbVivants: Int,
    val nbAbroutis: Int,
    /** Hauteur moyenne des vivants mesurés, en centimètres entiers ; `null` si aucune hauteur. */
    val hMoyCm: Int?,
) {
    init {
        require(nbTotal >= 0 && nbVivants >= 0 && nbAbroutis >= 0) { "Totaux négatifs" }
        require(nbVivants <= nbTotal) { "nb_vivants ($nbVivants) > nb_total ($nbTotal)" }
        require(nbAbroutis <= nbVivants) { "nb_abroutis ($nbAbroutis) > nb_vivants ($nbVivants)" }
        require(hMoyCm == null || hMoyCm >= 0) { "h_moy_cm négatif" }
    }

    companion object {
        /**
         * Décompte des [etats] d'un contrôle :
         * - `nb_total` = sains + atteints + morts + absents, hors non observés et hors plants de [remplaces]
         *   (remplacés par un regarni posé à un contrôle antérieur, voir [Regarnis.remplacesAvant]) ;
         * - `nb_vivants` = sains + atteints ;
         * - `nb_abroutis` = vivants dont le détail est « abroutissement » ;
         * - `h_moy_cm` = moyenne arithmétique des hauteurs des vivants mesurés, arrondie à l'entier le plus
         *   proche, demi vers le haut (sommieR exige un entier, `R/registre6.R` l. 174 ; arbitrage Q7).
         */
        fun deduire(etats: List<EtatPlant>, remplaces: Set<UuidPlant> = emptySet()): Totaux {
            val comptes = etats.filter { it.plant !in remplaces }.mapNotNull { it.observation as? Observation.Observe }
            val vivants = comptes.filter { it.etat.vivant }
            val hauteurs = vivants.mapNotNull { it.hauteurCm }
            return Totaux(
                nbTotal = comptes.size,
                nbVivants = vivants.size,
                nbAbroutis = vivants.count { it.abrouti },
                hMoyCm = if (hauteurs.isEmpty()) null else arrondiDemiVersLeHaut(hauteurs.sum().toDouble() / hauteurs.size),
            )
        }

        /** Arrondi à l'entier le plus proche, demi vers le haut : 42,5 → 43. */
        fun arrondiDemiVersLeHaut(x: Double): Int = floor(x + 0.5).toInt()
    }
}
