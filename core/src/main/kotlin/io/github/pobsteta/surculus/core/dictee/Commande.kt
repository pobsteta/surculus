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
 * États de plant (`docs/referentiels/etats-plants.yaml`, nomenclature du DSF) ; `non_observe` n'est pas un
 * état mais une absence de mesure (spec § 4.3).
 */
enum class EtatDicte(val code: String) {
    SAIN("sain"), ATTEINT("atteint"), MORT("mort"), ABSENT("absent"), NON_OBSERVE("non_observe");

    companion object {
        fun deCode(code: String?): EtatDicte? = entries.firstOrNull { it.code == code }
    }
}

/** Une commande dictée : une phrase en porte une seule. */
sealed interface Commande {
    data class Etat(val etat: EtatDicte) : Commande

    /** Groupe de cause du DSF (`etats-plants.yaml`). */
    data class Cause(val causeGroupe: String) : Commande

    data class Detail(val causeDetail: String) : Commande

    data class Hauteur(val hauteurCm: Int) : Commande

    data object Suivant : Commande

    /** « remplace » : le plant courant est un regarni lié au plant mort ou absent qu'il remplace. */
    data object Regarni : Commande

    data object VideDebut : Commande

    data object VideFin : Commande

    data object Annule : Commande
}

/** Résultat de l'analyse d'une phrase. */
sealed interface ResultatDictee {
    /** Phrase hors grammaire ou refusée par le contexte ; [motif] est un libellé libre. */
    data class Refus(val motif: String) : ResultatDictee

    /**
     * Phrase acceptée. Les indicateurs ne servent qu'aux commandes d'état : [correction] (un état déjà
     * saisi est remplacé, `remplace_uuid`), [annonceVocale], [ligneAjoutee] (faux pour une redite),
     * [confirmationVocale] (redite) et [mesuresConservees] (null : non déterminé par la spec).
     */
    data class Accepte(
        val commande: Commande,
        val correction: Boolean = false,
        val annonceVocale: Boolean = false,
        val ligneAjoutee: Boolean = true,
        val confirmationVocale: Boolean = false,
        val mesuresConservees: Boolean? = null,
    ) : ResultatDictee
}
