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

import fr.nemeton.terrain.voix.GrammaireFermee
import fr.nemeton.terrain.voix.NombresFrancais

/**
 * Grammaire fermée de la dictée (spec § 4.3). Les mots sont ceux de `vosk-model-small-fr-0.22` : un test
 * vérifie que chacun figure dans la liste de mots du modèle (ADR 0004). Formes du modèle : minuscules,
 * accents, sans trait d'union.
 */
object Grammaire {
    const val MOT_HAUTEUR = "hauteur"
    const val HAUTEUR_MIN_CM = 1
    const val HAUTEUR_MAX_CM = 400

    /** Phrases à commande fixe, dans l'ordre de la spec. */
    internal val commandesFixes: Map<String, Commande> = linkedMapOf(
        "sain" to Commande.Etat(EtatDicte.SAIN),
        "atteint" to Commande.Etat(EtatDicte.ATTEINT),
        "mort" to Commande.Etat(EtatDicte.MORT),
        "absent" to Commande.Etat(EtatDicte.ABSENT),
        "non observé" to Commande.Etat(EtatDicte.NON_OBSERVE),
        // Mot dicté -> groupe DSF : fixtures/README.md (section Dictée) et etats-plants.yaml.
        "insectes" to Commande.Cause("entomologique"),
        "champignon" to Commande.Cause("pathologique"),
        "animaux" to Commande.Cause("animaux"),
        "climat" to Commande.Cause("abiotique_complexe_indetermine"),
        "indéterminé" to Commande.Cause("abiotique_complexe_indetermine"),
        "mangé" to Commande.Detail("abroutissement"),
        "suivant" to Commande.Suivant,
        "remplace" to Commande.Regarni,
        "vide" to Commande.VideDebut,
        "fin du vide" to Commande.VideFin,
        "annule" to Commande.Annule,
    )

    /** Les phrases données au moteur : les commandes fixes, puis « hauteur » suivi de chaque nombre de 1 à 400. */
    val phrases: List<String> by lazy {
        commandesFixes.keys.toList() +
            (HAUTEUR_MIN_CM..HAUTEUR_MAX_CM).map { "$MOT_HAUTEUR ${NombresFrancais.jetons(it).joinToString(" ")}" }
    }

    /** Les mots distincts des phrases. */
    val mots: Set<String> by lazy { phrases.flatMap { it.split(' ') }.toSortedSet() }

    /** Grammaire au format JSON de Vosk (`terrain-voix`) : liste de phrases, `[unk]` en dernier. */
    fun jsonVosk(): String = GrammaireFermee.json(phrases)
}
