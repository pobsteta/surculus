/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.gpkg

import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.placette.Observation
import io.github.pobsteta.surculus.core.placette.Plant
import io.github.pobsteta.surculus.core.placette.Totaux
import io.github.pobsteta.surculus.core.placette.UuidControle
import io.github.pobsteta.surculus.core.placette.UuidPlacette
import io.github.pobsteta.surculus.core.placette.UuidPlant
import java.time.Instant

/**
 * Ce que l'agent a saisi pendant la tournée et que l'export ajoute au `travaux.gpkg` (spec § 5). Ces classes
 * sont l'entrée de l'écrivain ; le journal des saisies (mission 008) les construira. Tous les points sont en
 * Lambert-93 (x = Est, y = Nord, mètres), dans le repère de la tournée.
 */
data class Saisies(
    /** Agent de la tournée : écrit dans `projet.operateur`. */
    val operateur: String?,
    val placettesInstallees: List<PlacetteInstallee> = emptyList(),
    val controles: List<ControleSaisi> = emptyList(),
    val plants: List<PlantNumerote> = emptyList(),
    val constats: List<ConstatReprise> = emptyList(),
    val vides: List<Vide> = emptyList(),
)

/** Placette installée pendant la tournée (spec § 4.7) : une ligne de plus dans la couche `placettes`. */
data class PlacetteInstallee(
    val uuid: UuidPlacette,
    val code: String,
    /** Identifiant des travaux suivis (`suivis.id`) : sommieR refuse une placette dont les travaux sont inconnus. */
    val travauxId: String,
    val rayonM: Double,
    val materialisation: String? = null,
    val ug: String? = null,
    val position: PointLambert93,
)

/** Un plant pointé et le numéro qu'il porte dans sa placette (le numéro n'est pas dans [Plant], voir `NumerosPlacette`). */
data class PlantNumerote(val plant: Plant, val numero: Int) {
    init { require(numero >= 1) { "Numéro de plant $numero : 1 au moins" } }
}

/** Ligne d'état d'un plant à un contrôle : une ligne de `etats_plants`. */
data class LigneEtat(val uuid: String, val plant: UuidPlant, val observation: Observation)

/** Position GNSS mesurée (moyennée) d'un contrôle : une ligne de `positions_controles`. */
data class PositionMesuree(
    val position: PointLambert93,
    val precisionM: Double? = null,
    val sourceGnss: String? = null,
    val referentielSource: String? = null,
    val simulee: Boolean = false,
    val rattacheParIdentite: Boolean = false,
)

/**
 * Un contrôle de placette : une ligne de `controles` (totaux, lus par l'import de sommieR 0.31.1), une ligne
 * de `positions_controles` si [positionMesuree] est donnée, et une ligne d'`etats_plants` par plant.
 */
data class ControleSaisi(
    val uuid: UuidControle,
    val placette: UuidPlacette,
    val visiteLe: Instant,
    /** Position portée par `controles.geom` : celle du piquet si rattaché par identité, la mesurée sinon. */
    val position: PointLambert93,
    val positionMesuree: PositionMesuree? = null,
    val etats: List<LigneEtat> = emptyList(),
    /** Plants déjà remplacés par un regarni avant ce contrôle : ils sortent des totaux. */
    val remplaces: Set<UuidPlant> = emptySet(),
    val totaux: Totaux,
    val operateur: String? = null,
    val concurrence: String? = null,
    val besoin: String? = null,
    val observations: String? = null,
)

/** Constat de reprise en cheminement (spec § 4.4) : une ligne de `constats_reprise`, géométrie = surface constatée. */
data class ConstatReprise(
    val uuid: String,
    val surface: Geometrie.Polygone,
    val methode: MethodeConstat,
    val pasLignes: Double? = null,
    /** Les deux départs tirés : un tableau JSON (type validé par Pascal, 2026-10-17), recopié tel quel. */
    val departsJson: String? = null,
    val essence: String? = null,
    val nbPlantsChantier: Int? = null,
    val nbSains: Int? = null,
    val nbAtteints: Int? = null,
    val nbMorts: Int? = null,
    val nbAbsents: Int? = null,
    val tauxBrutPct: Double? = null,
    val icBasPct: Double? = null,
    val icHautPct: Double? = null,
    val seuilPct: Double? = null,
    val seuilSource: String? = null,
    val alerte: Boolean = false,
    val visiteLe: Instant? = null,
    val operateur: String? = null,
)

/** Vide (spec § 4.5) : une ligne de `vides`, rattachée à un constat ou à une placette. */
data class Vide(
    val uuid: String,
    val surface: Geometrie.Polygone,
    val constatUuid: String? = null,
    val placetteUuid: String? = null,
    val surfaceAre: Double? = null,
    val origine: OrigineVide,
    val visiteLe: Instant? = null,
    val operateur: String? = null,
)
