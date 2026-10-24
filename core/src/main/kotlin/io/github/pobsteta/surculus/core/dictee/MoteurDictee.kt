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

/** Reçoit les phrases rendues par le moteur de dictée (minuscules, sans ponctuation). */
interface EcouteurDictee {
    /** Une phrase reconnue, telle que le moteur la rend (non analysée, voir [AnalyseurDictee]). */
    fun surPhrase(phrase: String)

    /** Erreur du moteur (micro refusé, modèle absent...) : message lisible, jamais d'exception muette. */
    fun surErreur(message: String)
}

/**
 * Moteur de dictée hors ligne à grammaire fermée (ADR 0004). Neutre vis-à-vis de Vosk : l'implémentation
 * Android est dans `:app`. L'analyse des phrases rendues est dans [AnalyseurDictee].
 */
interface MoteurDictee {
    /** Les phrases que le moteur est autorisé à rendre (grammaire fermée), et rien d'autre. */
    fun grammaire(): List<String> = Grammaire.phrases

    /** Tous les mots de la grammaire, tels que le moteur doit les connaître. */
    fun motsDeLaGrammaire(): Set<String> = Grammaire.mots

    /** Démarre l'écoute ; les phrases arrivent à [ecouteur]. */
    fun demarrer(ecouteur: EcouteurDictee)

    /** Arrête l'écoute et libère le micro. Sans effet si le moteur n'écoute pas. */
    fun arreter()
}
