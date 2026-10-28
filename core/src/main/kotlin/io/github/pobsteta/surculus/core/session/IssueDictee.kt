/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 *
 * Écrit pour Surculus (mission 012-D1, plan 012d-plan.md § 4 et § 5) ; aucun code repris de Marculus.
 */
package io.github.pobsteta.surculus.core.session

/**
 * Ce qu'une phrase dictée a fait à la session. [annonce] est le texte dit à la voix (écrit dans la bande sans voix
 * française) ; il commence par « Placette déjà terminée. » quand la placette l'était (décision Q9). Le numéro est celui
 * du journal, lu après l'écriture.
 */
sealed interface IssueDictee {
    val annonce: String

    /** La placette était déjà terminée avant la phrase : la saisie reste permise, avec l'alerte. */
    val placetteTerminee: Boolean

    /** Vrai si la phrase a écrit ou annulé une ligne du journal. */
    val ecrit: Boolean get() = false

    /** Une ligne `etat` écrite (état, non observé, cause, détail, hauteur) ; [retire] : mesures retirées par une correction (Q5). */
    data class Ecrite(
        val numero: Int,
        val lignes: List<String>,
        val correction: Boolean,
        val retire: List<String>,
        override val annonce: String,
        override val placetteTerminee: Boolean,
    ) : IssueDictee {
        override val ecrit get() = true
    }

    /** Redite de l'état déjà dit : aucune ligne, la voix confirme. */
    data class Redite(val numero: Int, override val annonce: String, override val placetteTerminee: Boolean) : IssueDictee

    /** « suivant » : le courant change, aucune ligne. [numero] null : plus de plant à dire. */
    data class Suivant(val numero: Int?, override val annonce: String, override val placetteTerminee: Boolean) : IssueDictee

    /** « remplace » : le regarni est en attente, aucune ligne (le pointage se fait à l'écran). */
    data class RemplaceEnAttente(val numero: Int, override val annonce: String, override val placetteTerminee: Boolean) : IssueDictee

    /** « annule » : une ligne annulée. */
    data class Annulee(override val annonce: String, override val placetteTerminee: Boolean) : IssueDictee {
        override val ecrit get() = true
    }

    /** Refus : rien n'est écrit. [refusSession] porte le code de la spec § 6 quand c'est la session qui refuse. */
    data class Refusee(
        val raison: Raison,
        override val annonce: String,
        override val placetteTerminee: Boolean,
        val refusSession: SuiteSession.Refus? = null,
    ) : IssueDictee

    enum class Raison {
        PAS_COMPRIS, HAUTEUR_SANS_ETAT, HAUTEUR_SUR_ABSENT, HAUTEUR_SUR_NON_OBSERVE, HAUTEUR_HORS_BORNES, VIDE,
        CAUSE_SUR_SAIN, CAUSE_SUR_ABSENT, CAUSE_SUR_NON_OBSERVE, ETAT_D_ABORD, DETAIL_REFUSE, REMPLACE_REFUSE,
        AUCUN_PLANT, REFUS_SESSION, RIEN_A_ANNULER,
    }
}
