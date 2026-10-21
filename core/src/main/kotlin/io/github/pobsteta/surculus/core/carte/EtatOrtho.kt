/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.core.carte

/** État de l'ortho d'une tournée, visible dans la liste (ADR 0009) : complète, partielle n / N, absente. */
sealed interface EtatOrtho {
    data object Absente : EtatOrtho

    data class Partielle(val presentes: Long, val attendues: Long) : EtatOrtho

    data object Complete : EtatOrtho

    companion object {
        /**
         * [presentes] tuiles sur disque sur [attendues] ; [termine] vaut vrai quand le préchargement est allé au bout
         * (les tuiles hors couverture de l'IGN ne sont jamais écrites : sans ce drapeau, l'ortho resterait « partielle »).
         * Une ortho sans aucune tuile est toujours absente. [attendues] à 0 ou moins veut dire « pas de zone » : absente.
         */
        fun depuis(presentes: Long, attendues: Long, termine: Boolean = false): EtatOrtho = when {
            attendues <= 0L -> Absente
            presentes <= 0L -> Absente // jamais « complète » sans une seule tuile
            termine -> Complete
            presentes >= attendues -> Complete
            else -> Partielle(presentes, attendues)
        }

        /**
         * La marque de fin n'est écrite que si **aucune** tuile n'a échoué (réseau perdu, erreur du serveur) : un échec
         * ignoré ferait passer une ortho trouée pour « complète ». Les tuiles hors couverture (404) ne sont pas des échecs.
         */
        fun marqueAEcrire(echecs: Int): Boolean = echecs == 0
    }
}
