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

import io.github.pobsteta.surculus.core.placette.GroupeCause

/**
 * Les textes dits à la voix (et écrits dans la bande, sans voix française) par la dictée : `pilotage/missions/012d-plan.md`
 * § 4 et § 5, **tels quels** (décision Q10 de Pascal, 2026-10-28), à revoir à Couchey. Fonctions pures, sans Android.
 * Jamais de code technique à la voix : il reste à l'écran.
 *
 * Le seul texte que le plan ne donne pas est celui des mesures retirées par une correction (Q5) : « Retiré : cause, mangé,
 * hauteur. » (décision de Pascal, 2026-10-28 : « détail » se dit « mangé » ; seuls les mots concernés sont dits).
 *
 * Alerte « Placette déjà terminée. » (décision de Pascal, 2026-10-28) : elle précède toute phrase **comprise**, refus
 * compris, mais jamais « Pas compris. ».
 */
object TextesDictee {
    const val PAS_COMPRIS = "Pas compris."
    const val HAUTEUR_SANS_ETAT = "Hauteur refusée. Dites d'abord l'état."
    const val HAUTEUR_ABSENT = "Pas de hauteur sur un plant absent."
    const val HAUTEUR_NON_OBSERVE = "Pas de hauteur sur un plant non observé."
    const val HAUTEUR_HORS_BORNES = "Hauteur de 1 à 400 centimètres."
    const val VIDE = "Vide : pas encore disponible."
    const val CAUSE_SUR_SAIN = "Une cause demande un plant atteint ou mort."
    const val CAUSE_SUR_ABSENT = "Pas de cause sur un plant absent."
    const val CAUSE_SUR_NON_OBSERVE = "Pas de cause sur un plant non observé."
    const val ETAT_D_ABORD = "Dites d'abord l'état."
    const val MANGE_REFUSE = "Mangé : plant sain, atteint ou mort seulement."
    const val REMPLACE_REFUSE = "Remplace : plant mort ou absent seulement."
    const val AUCUN_PLANT = "Plus de plant à dire."
    const val DEJA_TERMINEE = "Placette déjà terminée. "
    const val REFUS_SESSION = "Refusé. Regardez l'écran."
    const val RIEN_A_ANNULER = "Rien à annuler."
    const val ANNULE = "Annulé."
    const val TOUS_DITS = "Tous les plants sont dits."

    fun numero(n: Int) = "Numéro $n"

    /** Libellé dit d'un état (`non_observe` : « non observé »). */
    fun etat(code: String) = if (code == "non_observe") "non observé" else code

    /** Libellé dit d'un groupe de cause (celui de l'écran ; `climat` et `indéterminé` : « autre ou inconnu »). */
    fun cause(groupe: String) = cause(checkNotNull(GroupeCause.lire(groupe)) { "groupe de cause inconnu : $groupe" })

    // Un seul libellé pour la voix et l'écran (012-D2) : `TextesControle.libelle(GroupeCause)` de `:app` le reprend, avec une majuscule.
    fun cause(groupe: GroupeCause) = when (groupe) {
        GroupeCause.ENTOMOLOGIQUE -> "insectes"
        GroupeCause.PATHOLOGIQUE -> "champignons"
        GroupeCause.ANIMAUX -> "animaux"
        GroupeCause.ABIOTIQUE_COMPLEXE_INDETERMINE -> "autre ou inconnu"
    }

    /** « Numéro 7, atteint. » */
    fun etatDit(n: Int, etat: String) = "${numero(n)}, ${etat(etat)}."

    /** « Numéro 7, déjà atteint. » */
    fun redite(n: Int, etat: String) = "${numero(n)}, déjà ${etat(etat)}."

    /** « Correction. Numéro 7, sain. » ; vers « non observé » : « …, non observé, sans mesure. » ; sinon les mesures retirées suivent. */
    fun correction(n: Int, etat: String, retire: List<String>): String {
        if (etat == "non_observe") return "Correction. ${numero(n)}, non observé, sans mesure."
        val dit = "Correction. ${numero(n)}, ${etat(etat)}."
        return if (retire.isEmpty()) dit else "$dit ${retires(retire)}"
    }

    /** Ce que la correction a retiré (décision Q5) : « Retiré : cause, mangé, hauteur. » */
    fun retires(retire: List<String>) = "Retiré : ${retire.joinToString(", ")}."

    fun causeDite(n: Int, groupe: String) = "${numero(n)}, ${cause(groupe)}."

    fun mange(n: Int) = "${numero(n)}, mangé."

    fun hauteurDite(n: Int, cm: Int) = "${numero(n)}, hauteur $cm centimètres."

    fun suivant(n: Int) = "${numero(n)}."

    fun remplaceEnAttente(n: Int) = "${numero(n)} sera remplacé. Pointez le regarni à l'écran."
}
