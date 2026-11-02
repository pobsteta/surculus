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
    CORRECTIONS(R.string.carte_recepteur_corrections),

    /** Le service s'est arrêté sur une erreur non rattrapée : il reste « en marche » mais rien ne retente (phrase de l'Essai GNSS). */
    SERVICE_INTERROMPU(R.string.carte_recepteur_service_interrompu);

    companion object {
        /**
         * Début de la phrase `rtk_service_interrompu` (le service la pose dans l'erreur) ; un test la compare à la ressource.
         * La phrase est la seule erreur du service qui ne promet pas de nouvelle tentative.
         */
        const val DEBUT_SERVICE_INTERROMPU = "Le service du récepteur GNSS s'est interrompu"

        /**
         * @param reglageActif Paramètres → « Utiliser un récepteur GNSS externe » ; inactif, la carte ne dit rien.
         * @param enMarche le service est démarré.
         * @param erreur dernière erreur de liaison du service (jamais affichée telle quelle).
         * @param erreurCorrections dernière erreur du serveur de corrections (idem).
         */
        fun pour(reglageActif: Boolean, enMarche: Boolean, erreur: String?, erreurCorrections: String?): ErreurRecepteurCarte? = when {
            !reglageActif -> null
            erreur != null && !enMarche -> NON_DEMARRE
            erreur != null && erreur.startsWith(DEBUT_SERVICE_INTERROMPU) -> SERVICE_INTERROMPU
            erreur != null -> LIAISON_PERDUE
            enMarche && erreurCorrections != null -> CORRECTIONS
            else -> null
        }
    }
}
