/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.gpkg

import io.github.pobsteta.surculus.core.geo.PointLambert93

/**
 * Titre d'une tournée (mission 061, règle de Pascal du 2026-10-27) : le nom de la forêt, lu dans `projet.foret` ; s'il est
 * vide, dans la couche `foret` ([Tournee.nomForet]) ; en dernier recours, le nom du dossier de la tournée.
 */
fun titreForet(projetForet: String?, nomCoucheForet: String?, dossier: String): String =
    projetForet?.takeIf { it.isNotBlank() } ?: nomCoucheForet?.takeIf { it.isNotBlank() } ?: dossier

/**
 * « À faire » : un contrôle prévu dans cette tournée et pas encore saisi (décision de Pascal, 2026-10-27, « 1 oui »).
 *
 * Ce qui est lisible aujourd'hui : le `travaux.gpkg` ne porte aucun « contrôle prévu » par placette ; la liste du départ
 * de sommieR est celle des placettes de la tournée (spec § 4.1), `etat_suivi` est figé à la préparation et `a_programmer`
 * dit un besoin de travaux, pas une placette due. **Prévue = toute placette de la tournée.** « Saisi » = un contrôle de
 * cette tournée au journal des saisies pour cette placette : [controlees] porte les `uuid` de ces placettes.
 * Hypothèse à confirmer par Pascal (voir la PR) : le journal n'écrit pas encore de contrôle (mission 012), donc
 * [controlees] est vide aujourd'hui et toutes les placettes sont « à faire ».
 */
fun estAFaire(placette: PlacetteTournee, controlees: Set<String>): Boolean = placette.uuid !in controlees

/** Les placettes de la liste : « à faire » d'abord, puis les autres ; dans chaque groupe, ordre du code puis de l'`uuid`. */
data class PlacettesOrdonnees(val aFaire: List<PlacetteTournee>, val autres: List<PlacetteTournee>) {
    val toutes: List<PlacetteTournee> get() = aFaire + autres
    val nombre: Int get() = aFaire.size + autres.size
}

private val ORDRE_CODE: Comparator<PlacetteTournee> = compareBy({ it.code ?: "￿" }, { it.uuid })

fun ordonnerPlacettes(placettes: List<PlacetteTournee>, controlees: Set<String>): PlacettesOrdonnees {
    val (faire, autres) = placettes.partition { estAFaire(it, controlees) }
    return PlacettesOrdonnees(faire.sortedWith(ORDRE_CODE), autres.sortedWith(ORDRE_CODE))
}

/**
 * La prochaine placette à faire, pour « naviguer vers » sans placette choisie : la plus proche de [depuis] (Lambert-93,
 * x = Est, y = Nord, mètres) si la position est connue, sinon la première dans l'ordre de la liste ; `null` s'il n'y en a
 * aucune (le bouton est alors inactif). Seules les placettes qui ont un code et une position sont atteignables par le guidage.
 */
fun prochaineAFaire(placettes: List<PlacetteTournee>, controlees: Set<String>, depuis: PointLambert93?): PlacetteTournee? {
    val candidates = ordonnerPlacettes(placettes, controlees).aFaire.filter { it.position != null && it.code != null }
    if (depuis == null) return candidates.firstOrNull()
    return candidates.minByOrNull { kotlin.math.hypot(it.position!!.xMetres - depuis.xMetres, it.position!!.yMetres - depuis.yMetres) }
}

/**
 * La placette vers laquelle « naviguer vers » lance le guidage (mission 061, « 2 oui » de Pascal, 2026-10-27) : la placette
 * [choisie] sur la carte ; sinon la [prochaineAFaire]. `null` : aucune, le bouton est inactif.
 */
fun cibleNavigation(choisie: PlacetteTournee?, placettes: List<PlacetteTournee>, controlees: Set<String>, depuis: PointLambert93?): PlacetteTournee? =
    choisie ?: prochaineAFaire(placettes, controlees, depuis)

/**
 * Où centrer la carte à l'ouverture depuis une placette : sa position (Lambert-93, x = Est, y = Nord, mètres), **seulement
 * si elle tombe en France** ; sinon `null` et la carte se cadre sur la tournée (une position aux axes inversés tomberait en Somalie).
 */
fun centrageInitial(centre: PlacetteTournee?): PointLambert93? =
    centre?.position?.takeIf { io.github.pobsteta.surculus.core.placette.EmpriseEtFrance.controler(it).enFrance }
