/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.core.session

import io.github.pobsteta.surculus.core.placette.EtatDsf

/** Gestes des boutons de volume (spec § 4.3). Les deux touches ensemble sont évitées (raccourci d'accessibilité d'Android). */
enum class Geste {
    VOLUME_HAUT_COURT,
    VOLUME_BAS_COURT,
    VOLUME_HAUT_LONG,
    VOLUME_BAS_LONG,
    VOLUME_BAS_DOUBLE_COURT,
}

/** Ce qu'un geste commande en placette. */
sealed interface ActionGeste {
    /** Dire l'état du plant courant ; [puisSuivant] : passer ensuite au plant suivant. */
    data class Dire(val etat: EtatDsf, val puisSuivant: Boolean) : ActionGeste

    /** Ouvre la dictée (012-D) : la session ne fait rien d'autre que le dire. */
    data object OuvrirDictee : ActionGeste

    data object AnnulerDerniereSaisie : ActionGeste
}

/**
 * Table des boutons, colonne « en placette » de la spec § 4.3, en fonction pure. La lecture des vraies touches
 * (appui court, long, double) est celle de la mission 012-C ; la colonne « en cheminement » (§ 4.4) n'est pas ici.
 *
 * | Geste | En placette |
 * |---|---|
 * | volume haut, appui court | sain, puis plant suivant |
 * | volume bas, appui court | mort, puis plant suivant |
 * | volume haut, appui long | atteint (la cause se dicte ensuite) |
 * | volume bas, appui long | ouvre la dictée |
 * | volume bas, double appui court | annule la dernière saisie |
 */
object GestesPlacette {
    fun action(geste: Geste): ActionGeste = when (geste) {
        Geste.VOLUME_HAUT_COURT -> ActionGeste.Dire(EtatDsf.SAIN, puisSuivant = true)
        Geste.VOLUME_BAS_COURT -> ActionGeste.Dire(EtatDsf.MORT, puisSuivant = true)
        Geste.VOLUME_HAUT_LONG -> ActionGeste.Dire(EtatDsf.ATTEINT, puisSuivant = false)
        Geste.VOLUME_BAS_LONG -> ActionGeste.OuvrirDictee
        Geste.VOLUME_BAS_DOUBLE_COURT -> ActionGeste.AnnulerDerniereSaisie
    }
}
