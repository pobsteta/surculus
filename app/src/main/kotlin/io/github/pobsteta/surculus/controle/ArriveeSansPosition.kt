/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.controle

/** Ce que l'écran d'arrivée fait quand aucune position n'arrive (ADR 0003, amendement du 2026-11-02, point 1). */
enum class SuiteSansFix {
    /** Moins de [ArriveeSansPosition.SECONDES_SANS_FIX] sans fix : on attend. */
    ATTENDRE,

    /** Assez attendu, une source écoute : « Ouvrir sans position » est proposé. */
    PROPOSER,

    /** Rien n'écoute (localisation coupée, permission refusée, aucun récepteur branché) : on demande de l'activer. */
    ACTIVER_LOCALISATION,

    /** Un fix est arrivé, même unique, médiocre ou simulé : c'est une position, la règle ordinaire s'applique (point 6). */
    POSITION_RECUE,
}

/** L'attente avant « Ouvrir sans position » : fonction pure, l'écran lui donne le temps écoulé et ce qui écoute. */
object ArriveeSansPosition {
    /**
     * Secondes sans **aucun** fix avant de proposer « Ouvrir sans position ». **Valeur provisoire** (ADR 0003, amendement
     * du 2026-11-02, point 1) : mesurée par Pascal à l'essai en forêt sous couvert (premier fix à froid), gelée avant la
     * publication de la tranche 1.
     */
    const val SECONDES_SANS_FIX = 30

    /**
     * [msSansFix] : millisecondes écoulées depuis le début de l'attente sans aucun fix ; [fixRecus] : fix reçus depuis.
     * « Récepteur actif » (décision de Pascal du 2026-11-03) : Surculus écoute une source, le récepteur RTK s'il est
     * branché, sinon le GNSS du téléphone ; l'appelant passe donc [localisationActive] et [permissionAccordee] vrais quand
     * un récepteur RTK est branché. Un fix reçu l'emporte toujours : c'est une position.
     */
    fun suite(msSansFix: Long, fixRecus: Int, localisationActive: Boolean, permissionAccordee: Boolean): SuiteSansFix = when {
        fixRecus > 0 -> SuiteSansFix.POSITION_RECUE
        !localisationActive || !permissionAccordee -> SuiteSansFix.ACTIVER_LOCALISATION
        msSansFix >= SECONDES_SANS_FIX * 1_000L -> SuiteSansFix.PROPOSER
        else -> SuiteSansFix.ATTENDRE
    }
}
