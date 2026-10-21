package io.github.pobsteta.surculus.core.dictee

import com.google.gson.JsonObject

/**
 * Port de test de la mission 013 (dictée à grammaire fermée), décrit depuis `fixtures/dictee/` et
 * `fixtures/README.md` (section Dictée), sans lire de code de production.
 *
 * Le `dev` de la 013 branche son implémentation, éventuellement par un adaptateur, dans
 * [ImplementationDictee.dictee]. Les cas de `fixtures/dictee/` ne changent pas.
 *
 * Résultats (JSON, comparés par [io.github.pobsteta.surculus.core.fixtures.Comparaison]) :
 * `{"accepte": true, "commande": {"type": ...}}` ou `{"accepte": false}` (libellé du refus libre).
 */
interface Dictee {
    /** Analyse la phrase rendue par le moteur (minuscules, sans ponctuation), sans contexte. */
    fun analyser(phrase: String): JsonObject

    /** Même phrase confrontée à l'état courant du plant (`sain`, `atteint`, `mort`, `absent`, `non_observe`, ou null : aucun état). */
    fun appliquer(phrase: String, etatCourant: String?): JsonObject

    /** Tous les mots de la grammaire, tels que le moteur doit les connaître. */
    fun motsDeLaGrammaire(): Set<String>

    /** Vérifie nom et empreinte SHA-256 d'un modèle ; `contenu` null = fichier absent. Résultat : `{"accepte": bool}`. */
    fun verifierModele(nomModele: String, contenu: ByteArray?, sha256Attendu: String): JsonObject
}

/** La mission 013 remplace cette fonction ; tant que non, les tests de rejeu restent désactivés. */
object ImplementationDictee {
    fun dictee(): Dictee = throw NotImplementedError("mission 013 : brancher l'implémentation de la dictée")
}
