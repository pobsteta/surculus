/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.gpkg

import io.github.pobsteta.surculus.core.geo.EmpriseLambert93
import io.github.pobsteta.surculus.core.geo.PointLambert93

/**
 * Titre d'une tournée (mission 061, règle de Pascal du 2026-10-27) : le nom de la forêt, lu dans `projet.foret` ; s'il est
 * vide, dans la couche `foret` ([Tournee.nomForet]) ; en dernier recours, le nom du dossier de la tournée.
 */
fun titreForet(projetForet: String?, nomCoucheForet: String?, dossier: String): String =
    projetForet?.takeIf { it.isNotBlank() } ?: nomCoucheForet?.takeIf { it.isNotBlank() } ?: dossier

/**
 * « À faire » (décision de Pascal, 2026-10-27, « 1a ») : une placette dont l'`etat_suivi` **n'est pas** `a_jour` et qui
 * n'est pas encore saisie dans cette tournée. Les `a_jour` passent en bas avec les saisies, grisées : un `a_jour` ancien
 * (dernier contrôle lointain) reste « à jour », on ne recalcule rien (l'état vient de sommieR). `jamais`, `a_programmer` et
 * un état inconnu sont « à faire ».
 *
 * « Saisie » = un contrôle de cette tournée au journal pour cette placette : [controlees] porte les `uuid` de ces placettes,
 * **propres à la tournée** : une saisie dans une tournée ne rend pas la placette faite dans une autre (critère (d)). Le
 * journal n'écrit pas encore de contrôle (mission 012) : [controlees] est vide aujourd'hui.
 */
fun estAFaire(placette: PlacetteTournee, controlees: Set<String>): Boolean =
    placette.etatSuivi != EtatSuivi.A_JOUR && placette.uuid !in controlees

/** Ce que dit la ligne d'une placette sur l'écran « Placettes ». */
enum class StatutLigne { A_FAIRE, CONTROLEE, ETAT_SEUL }

/**
 * « À faire » ; « Contrôlée » **seulement** si un contrôle est saisi dans cette tournée (son `uuid` est dans [controlees]) ;
 * sinon (une `a_jour` jamais saisie ici) on ne dit que son état.
 */
fun statutLigne(placette: PlacetteTournee, controlees: Set<String>): StatutLigne = when {
    estAFaire(placette, controlees) -> StatutLigne.A_FAIRE
    placette.uuid in controlees -> StatutLigne.CONTROLEE
    else -> StatutLigne.ETAT_SEUL
}

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
    val candidates = ordonnerPlacettes(placettes, controlees).aFaire.filter { probleme(it) == null }
    if (depuis == null) return candidates.firstOrNull()
    return candidates.minByOrNull { kotlin.math.hypot(it.position!!.xMetres - depuis.xMetres, it.position!!.yMetres - depuis.yMetres) }
}

/** Pourquoi on ne peut pas naviguer vers une placette. */
enum class ProblemePlacette { SANS_POSITION, HORS_FRANCE }

/** `null` si le guidage peut aller à la placette : un code, une position (Lambert-93, x = Est, y = Nord) qui tombe en France. */
fun probleme(p: PlacetteTournee): ProblemePlacette? {
    val pos = p.position ?: return ProblemePlacette.SANS_POSITION
    if (p.code == null) return ProblemePlacette.SANS_POSITION
    return if (io.github.pobsteta.surculus.core.placette.EmpriseEtFrance.controler(pos).enFrance) null else ProblemePlacette.HORS_FRANCE
}

/**
 * La placette vers laquelle « naviguer vers » lance le guidage (mission 061, « 2 oui » de Pascal) : la placette [choisie] sur
 * la carte, **seulement si elle est atteignable** (jamais une autre en silence : sinon `null`, bouton inactif et message) ;
 * sans placette choisie, la [prochaineAFaire]. `null` : aucune, le bouton est inactif.
 */
fun cibleNavigation(choisie: PlacetteTournee?, placettes: List<PlacetteTournee>, controlees: Set<String>, depuis: PointLambert93?): PlacetteTournee? =
    if (choisie != null) choisie.takeIf { probleme(it) == null } else prochaineAFaire(placettes, controlees, depuis)

/**
 * Où centrer la carte à l'ouverture depuis une placette : sa position (Lambert-93, x = Est, y = Nord, mètres), **seulement
 * si elle tombe en France** ; sinon `null` et la carte se cadre sur la tournée (une position aux axes inversés tomberait en Somalie).
 */
fun centrageInitial(centre: PlacetteTournee?): PointLambert93? =
    centre?.position?.takeIf { io.github.pobsteta.surculus.core.placette.EmpriseEtFrance.controler(it).enFrance }

/**
 * Ce que le bouton « maison » cadre (mission 061, « 2 oui ») : l'[emprise] de la tournée (UG, sinon forêt) si elle existe ;
 * sinon la boîte des placettes de la tournée **qui tombent en France** (élargie de [MARGE_M] mètres : une seule placette ne
 * fait pas une surface) ; `null` s'il n'y a rien à cadrer (bouton grisé).
 */
fun empriseDeCadrage(emprise: EmpriseLambert93?, placettes: List<PlacetteTournee>): EmpriseLambert93? {
    if (emprise != null) return emprise
    val pts = placettes.mapNotNull { it.position }.filter { io.github.pobsteta.surculus.core.placette.EmpriseEtFrance.controler(it).enFrance }
    if (pts.isEmpty()) return null
    return EmpriseLambert93(
        pts.minOf { it.xMetres } - MARGE_M, pts.minOf { it.yMetres } - MARGE_M,
        pts.maxOf { it.xMetres } + MARGE_M, pts.maxOf { it.yMetres } + MARGE_M,
    )
}

const val MARGE_M = 50.0
