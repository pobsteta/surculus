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
import io.github.pobsteta.surculus.core.reperes.CodeEpsg
import java.time.Instant

/** Une couche telle que le fichier la déclare dans `gpkg_contents`. */
data class CoucheLue(
    val nom: String,
    /** `features` ou `attributes` (ou `tiles`, `2d-gridded-coverage`…). */
    val typeDonnees: String,
    /** `null` pour une table d'attributs. */
    val srsId: Int?,
    /** Type de géométrie de `gpkg_geometry_columns`, `null` pour une table d'attributs. */
    val geometrie: String?,
    val nbLignes: Long,
)

/** Une placette de l'aller (`placettes`, spec § 5). */
data class PlacetteTournee(
    val uuid: String,
    val code: String?,
    val travauxId: String?,
    val rayonM: Double?,
    val materialisation: String?,
    val ug: String?,
    /** Lambert-93 (x = Est, y = Nord, mètres) ; `null` si la géométrie est vide. */
    val position: PointLambert93?,
    /** Calculé par sommieR, lu tel quel ; [EtatSuivi.INCONNU] si nul, absent (ancienne tournée) ou valeur inconnue. */
    val etatSuivi: EtatSuivi = EtatSuivi.INCONNU,
)

/** Un contrôle déjà inscrit dans le fichier (`controles`). */
data class ControleTournee(
    val uuid: String,
    val placetteUuid: String?,
    /** Texte tel que le fichier le porte (`DATETIME`). */
    val visiteLe: String?,
)

/** La ligne unique de la table `projet`, telle que le fichier la porte (version unique du fichier). */
data class ProjetLu(
    val nom: String?,
    val foret: String?,
    val operateur: String?,
    val prepareLe: String?,
    val versionSommier: String?,
    val versionSurculus: String?,
)

/** Un plant déjà écrit dans le fichier (tournée rendue précédemment, ADR 0007 « plants à l'aller »). */
data class PlantLu(
    val uuid: String,
    val placetteUuid: String,
    val numero: Int,
    val essence: String?,
    val regarni: Boolean,
    val remplaceUuid: String?,
    val position: PointLambert93?,
)

/** Une ligne d'état déjà écrite dans le fichier. */
data class EtatLu(
    val uuid: String,
    val controleUuid: String,
    val plantUuid: String,
    val etat: String,
    val causeGroupe: String?,
    val causeDetail: String?,
    val hauteurCm: Int?,
)

/**
 * Une tournée lue dans un `travaux.gpkg` (aller de sommieR 0.31.1, ou export d'une tournée précédente).
 *
 * Le repère est **lu dans le fichier** ([epsg], depuis `gpkg_spatial_ref_sys`), jamais supposé.
 */
data class Tournee(
    val epsg: CodeEpsg,
    /** `srs_id` des couches à géométrie, tel que le fichier le porte. */
    val srsId: Int,
    val couches: List<CoucheLue>,
    val placettes: List<PlacetteTournee>,
    val controles: List<ControleTournee>,
    /** Boîte englobante de la couche `foret`, `null` si elle est vide. */
    val empriseForet: EmpriseLambert93?,
    /** `null` si le fichier n'a pas de table `projet` (tournée de départ 0.31.1). */
    val projet: ProjetLu?,
    val plants: List<PlantLu>,
    val etats: List<EtatLu>,
    /** Codes de la couche `codes` (nomenclature de sommieR) ; vide si la couche l'est. */
    val codesTravaux: Set<String> = emptySet(),
    /** Nom de la forêt lu dans la colonne `nom` de la couche `foret` (première ligne renseignée) ; `null` si la couche est vide ou sans nom. */
    val nomForet: String? = null,
) {
    val nomsCouches: Set<String> get() = couches.map { it.nom }.toSet()

    /** Les couches ajoutées par Surculus présentes dans le fichier. */
    val couchesAjoutees: List<String> get() = Couches.NOMS.filter { it in nomsCouches }
}

/** Bilan d'un export, pour l'écran de fin de tournée et la CI. */
data class BilanExport(
    val exporteLe: Instant,
    val placettesInstallees: Int,
    val controles: Int,
    val plants: Int,
    val etats: Int,
    val positions: Int,
    val constats: Int,
    val vides: Int,
    /** Lignes déjà présentes à l'identique, ignorées (rejeu d'un export). */
    val dejaPresentes: Int,
)
