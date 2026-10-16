package io.github.pobsteta.surculus.core.fixtures

import com.google.gson.JsonObject

/**
 * Port de test de la mission 004 (domaine du contrôle de placette) : rattachement à 15 m, un contrôle
 * par jour, emprise, plants (totaux, regarni, identité, états et causes, numérotation).
 *
 * `famille` est le nom du fichier de fixtures sans extension (`rattachement-15m`, `totaux-controle`...),
 * `entree` l'objet `entree` du cas ; le résultat porte les clés de l'objet `attendu` (voir fixtures/README.md).
 * La mission 004 branche son implémentation, éventuellement par un adaptateur.
 */
fun interface ReglesPlacette {
    fun appliquer(famille: String, entree: JsonObject): JsonObject
}

/**
 * Port de test de la mission 005 (chaîne des repères) : convergence, déclinaison WMM2025, chaîne d'azimut,
 * unités d'azimut, distance horizontale. Même convention que [ReglesPlacette].
 */
fun interface Reperes {
    fun appliquer(famille: String, entree: JsonObject): JsonObject
}

/** Les missions 004 et 005 remplacent ces deux fonctions ; tant que non, les tests de rejeu restent désactivés. */
object Implementations {
    fun reglesPlacette(): ReglesPlacette = throw NotImplementedError("en attente de la mission 004")
    fun reperes(): Reperes = AdaptateurReperes
}
