/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.gnss

import androidx.annotation.StringRes
import io.github.pobsteta.surculus.R

/**
 * Ce que la carte dit du récepteur externe en erreur (mission 052) : une phrase courte, en français, jamais le message
 * technique de l'exception (celui-ci reste dans l'Essai GNSS). Kotlin pur, testé en JVM.
 */
enum class ErreurRecepteurCarte(@StringRes val texte: Int) {
    /** Le service n'a pas pu démarrer (récepteur non choisi, notification refusée…). */
    NON_DEMARRE(R.string.carte_recepteur_non_demarre),

    /** Le service tourne mais la liaison avec le récepteur est coupée ; il retente tout seul. */
    LIAISON_PERDUE(R.string.carte_recepteur_liaison_perdue),

    /** La liaison au récepteur marche, les corrections (NTRIP) non : la position reste moins précise. */
    CORRECTIONS(R.string.carte_recepteur_corrections);

    companion object {
        /**
         * @param reglageActif Paramètres → « Utiliser un récepteur GNSS externe » ; inactif, la carte ne dit rien.
         * @param enMarche le service est démarré.
         * @param erreur dernière erreur de liaison du service (jamais affichée telle quelle).
         * @param erreurCorrections dernière erreur du serveur de corrections (idem).
         */
        fun pour(reglageActif: Boolean, enMarche: Boolean, erreur: String?, erreurCorrections: String?): ErreurRecepteurCarte? = when {
            !reglageActif -> null
            erreur != null && !enMarche -> NON_DEMARRE
            erreur != null -> LIAISON_PERDUE
            enMarche && erreurCorrections != null -> CORRECTIONS
            else -> null
        }
    }
}
