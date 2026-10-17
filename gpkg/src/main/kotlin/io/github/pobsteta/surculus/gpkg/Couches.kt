/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.gpkg

import io.github.pobsteta.surculus.core.geo.UniteAzimut
import io.github.pobsteta.surculus.core.placette.EtatDsf
import io.github.pobsteta.surculus.core.placette.GroupeCause
import io.github.pobsteta.surculus.core.placette.Observations
import io.github.pobsteta.surculus.core.reperes.CodesUniteAzimut

/** Méthodes d'un constat de reprise (spec § 5, `constats_reprise.methode`). */
enum class MethodeConstat(val code: String) {
    PLEIN("plein"),
    CEMAGREF("cemagref"),
    GRAPPES("grappes"),
}

/** Origine d'un vide (spec § 5, `vides.origine`). */
enum class OrigineVide(val code: String) {
    TRACE("trace"),
    PROPOSE("propose"),
}

/** Une contrainte de valeurs de l'extension Schema (`gpkg_data_column_constraints`, type `enum`). */
data class ContrainteEnum(val nom: String, val valeurs: List<String>, val description: String)

/** Une colonne d'une couche ajoutée : son nom, son type SQLite/GeoPackage, et ce que l'extension Schema en dit. */
data class Colonne(
    val nom: String,
    val type: String,
    val titre: String,
    val description: String,
    val obligatoire: Boolean = false,
    val unique: Boolean = false,
    val contrainte: ContrainteEnum? = null,
)

/** Une couche ajoutée par Surculus (ADR 0007) : `features` si [geometrie] est renseignée, sinon `attributes`. */
data class CoucheAjoutee(
    val nom: String,
    /** Type de géométrie (`POINT`, `POLYGON`), `null` pour une table d'attributs. */
    val geometrie: String?,
    val colonnes: List<Colonne>,
)

/**
 * Les six couches que Surculus ajoute au `travaux.gpkg` de sommieR : noms et champs de `docs/spec.md` § 5
 * (identiques au brief `vers-sommier` lot 2). Le schéma appartient à sommieR (ADR 0007) ; Surculus l'écrit
 * par anticipation. Les valeurs permises viennent des enums de `:core` (états et groupes de causes du
 * référentiel `docs/referentiels/etats-plants.yaml`), jamais d'une liste recopiée ici.
 *
 * Types, validés par Pascal le 2026-10-17 (plan de la mission 006) : TEXT, INTEGER, REAL, BOOLEAN, DATETIME ;
 * `departs` est un tableau JSON dans un TEXT.
 */
object Couches {
    /** Modèle géomagnétique écrit avec chaque plant (décision de Pascal, 2026-10-17). */
    const val MODELE_GEOMAGNETIQUE = "WMM2025"

    /** Contraintes de valeurs de l'extension Schema (`gpkg_data_column_constraints`, type `enum`). */
    val ETAT_PLANT = ContrainteEnum(
        "etat_plant",
        EtatDsf.entries.map { it.code } + Observations.CODE_NON_OBSERVE,
        "États d'un plant (DSF) ; non_observe : plant non retrouvé, hors totaux.",
    )
    val CAUSE_GROUPE = ContrainteEnum(
        "cause_groupe",
        GroupeCause.entries.map { it.code },
        "Quatre groupes de causes du bilan DSF.",
    )
    val UNITE_AZIMUT = ContrainteEnum(
        "unite_azimut",
        UniteAzimut.entries.map { CodesUniteAzimut.code(it) },
        "Unité de la boussole : degrés (deg) ou grades (gr).",
    )
    val METHODE_CONSTAT = ContrainteEnum(
        "methode_constat",
        MethodeConstat.entries.map { it.code },
        "Méthode du constat de reprise.",
    )
    val ORIGINE_VIDE = ContrainteEnum(
        "origine_vide",
        OrigineVide.entries.map { it.code },
        "Origine d'un vide : tracé par l'agent ou proposé par l'application.",
    )

    val PLANTS = CoucheAjoutee(
        "plants", "POINT",
        listOf(
            Colonne("uuid", "TEXT", "Identifiant", "UUID du plant, qu'il garde d'un passage à l'autre.", obligatoire = true, unique = true),
            Colonne("placette_uuid", "TEXT", "Placette", "UUID de la placette (placettes.uuid).", obligatoire = true),
            Colonne("numero", "INTEGER", "Numéro", "Numéro du plant dans sa placette, figé à la clôture du premier contrôle.", obligatoire = true),
            Colonne("essence", "TEXT", "Essence", "Code d'essence ONF (essences.csv)."),
            Colonne("regarni", "BOOLEAN", "Regarni", "Vrai si le plant remplace un plant mort ou absent."),
            Colonne("remplace_uuid", "TEXT", "Plant remplacé", "UUID du plant mort ou absent que ce regarni remplace."),
            Colonne("pointe_le", "DATETIME", "Pointé le", "Instant du pointage (UTC)."),
            Colonne("distance_m", "REAL", "Distance", "Distance horizontale du piquet au plant, en mètres."),
            Colonne("azimut_mag", "REAL", "Azimut magnétique", "Azimut magnétique tel que lu, dans l'unité unite_azimut."),
            Colonne("unite_azimut", "TEXT", "Unité de l'azimut", "Unité de la boussole.", contrainte = UNITE_AZIMUT),
            Colonne("declinaison_deg", "REAL", "Déclinaison", "Déclinaison magnétique, en degrés, positive vers l'est."),
            Colonne("convergence_deg", "REAL", "Convergence", "Convergence des méridiens, en degrés."),
            Colonne("azimut_geo_deg", "REAL", "Azimut géographique", "Azimut géographique, en degrés."),
            Colonne("modele_geomagnetique", "TEXT", "Modèle géomagnétique", "Modèle de la déclinaison (WMM2025)."),
        ),
    )

    val POSITIONS_CONTROLES = CoucheAjoutee(
        "positions_controles", "POINT",
        listOf(
            Colonne("controle_uuid", "TEXT", "Contrôle", "UUID du contrôle (controles.uuid).", obligatoire = true, unique = true),
            Colonne("precision_m", "REAL", "Précision", "Précision estimée de la position, en mètres."),
            Colonne("source_gnss", "TEXT", "Source GNSS", "Récepteur ou source de la position."),
            Colonne("referentiel_source", "TEXT", "Référentiel de la source", "Référentiel dans lequel la position a été mesurée."),
            Colonne("simulee", "BOOLEAN", "Simulée", "Vrai si la position est simulée (essai)."),
            Colonne("rattache_par_identite", "BOOLEAN", "Rattaché par identité", "Vrai si le code lu a rattaché le contrôle à un piquet à plus de 15 m."),
        ),
    )

    val ETATS_PLANTS = CoucheAjoutee(
        "etats_plants", null,
        listOf(
            Colonne("uuid", "TEXT", "Identifiant", "UUID de la ligne d'état.", obligatoire = true, unique = true),
            Colonne("controle_uuid", "TEXT", "Contrôle", "UUID du contrôle (controles.uuid).", obligatoire = true),
            Colonne("plant_uuid", "TEXT", "Plant", "UUID du plant (plants.uuid).", obligatoire = true),
            Colonne("etat", "TEXT", "État", "État du plant à ce contrôle.", obligatoire = true, contrainte = ETAT_PLANT),
            Colonne("cause_groupe", "TEXT", "Groupe de cause", "Groupe de cause d'une atteinte ou d'une mort.", contrainte = CAUSE_GROUPE),
            Colonne("cause_detail", "TEXT", "Détail de la cause", "Détail libre ; « abroutissement » compte dans nb_abroutis."),
            Colonne("hauteur_cm", "INTEGER", "Hauteur", "Hauteur en centimètres."),
        ),
    )

    val CONSTATS_REPRISE = CoucheAjoutee(
        "constats_reprise", "POLYGON",
        listOf(
            Colonne("uuid", "TEXT", "Identifiant", "UUID du constat.", obligatoire = true, unique = true),
            Colonne("methode", "TEXT", "Méthode", "Méthode du constat.", obligatoire = true, contrainte = METHODE_CONSTAT),
            Colonne("pas_lignes", "REAL", "Pas des lignes", "Écartement des lignes, en mètres."),
            Colonne("departs", "TEXT", "Départs", "Les deux départs tirés au sort, en tableau JSON."),
            Colonne("essence", "TEXT", "Essence", "Code d'essence ONF (essences.csv)."),
            Colonne("nb_plants_chantier", "INTEGER", "Plants du chantier", "Nombre de plants du chantier."),
            Colonne("nb_sains", "INTEGER", "Sains", "Plants sains comptés."),
            Colonne("nb_atteints", "INTEGER", "Atteints", "Plants atteints comptés."),
            Colonne("nb_morts", "INTEGER", "Morts", "Plants morts comptés."),
            Colonne("nb_absents", "INTEGER", "Absents", "Plants absents comptés."),
            Colonne("taux_brut_pct", "REAL", "Taux brut", "Taux de reprise brut, en pourcent."),
            Colonne("ic_bas_pct", "REAL", "Intervalle bas", "Borne basse de l'intervalle de confiance, en pourcent."),
            Colonne("ic_haut_pct", "REAL", "Intervalle haut", "Borne haute de l'intervalle de confiance, en pourcent."),
            Colonne("seuil_pct", "REAL", "Seuil", "Seuil de reprise applicable, en pourcent."),
            Colonne("seuil_source", "TEXT", "Source du seuil", "D'où vient le seuil."),
            Colonne("alerte", "BOOLEAN", "Alerte", "Vrai si le constat déclenche une alerte."),
            Colonne("visite_le", "DATETIME", "Visite", "Instant du constat (UTC)."),
            Colonne("operateur", "TEXT", "Opérateur", "Agent qui a constaté."),
        ),
    )

    val VIDES = CoucheAjoutee(
        "vides", "POLYGON",
        listOf(
            Colonne("uuid", "TEXT", "Identifiant", "UUID du vide.", obligatoire = true, unique = true),
            Colonne("constat_uuid", "TEXT", "Constat", "UUID du constat de reprise, s'il en vient."),
            Colonne("placette_uuid", "TEXT", "Placette", "UUID de la placette, s'il en vient."),
            Colonne("surface_are", "REAL", "Surface", "Surface du vide, en ares."),
            Colonne("origine", "TEXT", "Origine", "Origine du vide.", contrainte = ORIGINE_VIDE),
            Colonne("visite_le", "DATETIME", "Visite", "Instant de la saisie (UTC)."),
            Colonne("operateur", "TEXT", "Opérateur", "Agent qui a saisi."),
        ),
    )

    val PROJET = CoucheAjoutee(
        "projet", null,
        listOf(
            Colonne("nom", "TEXT", "Nom", "Nom du projet (écrit par sommieR)."),
            Colonne("foret", "TEXT", "Forêt", "Forêt (écrit par sommieR)."),
            Colonne("operateur", "TEXT", "Opérateur", "Agent de la tournée (écrit par Surculus)."),
            Colonne("prepare_le", "DATETIME", "Préparé le", "Date de préparation de la tournée (écrit par sommieR)."),
            Colonne("version_sommier", "TEXT", "Version de sommieR", "Version de sommieR qui a préparé la tournée (écrit par sommieR)."),
            Colonne("version_surculus", "TEXT", "Version de Surculus", "Version unique du fichier : celle de Surculus qui l'a écrit (écrit par Surculus)."),
        ),
    )

    /** Dans l'ordre de création. */
    val TOUTES = listOf(PLANTS, POSITIONS_CONTROLES, ETATS_PLANTS, CONSTATS_REPRISE, VIDES, PROJET)

    val NOMS = TOUTES.map { it.nom }

    /** Les couches de la tournée de sommieR 0.31.1 que l'import lit (`R/travaux_qfield.R`) ; Surculus n'y touche pas, sauf pour y ajouter des lignes. */
    val COUCHES_SOMMIER_ECRITES = listOf("placettes", "controles", "travaux_pt", "travaux_surf")
    val COUCHES_SOMMIER_LUES = listOf("travaux_surf", "travaux_lin", "travaux_pt", "placettes", "controles", "photos")
    val COUCHES_SOMMIER_ALLER = listOf("foret", "ug", "parcelles", "codes", "suivis")

    /** Toutes les couches du modèle de sommieR 0.31.1. */
    val COUCHES_SOMMIER = (COUCHES_SOMMIER_LUES + COUCHES_SOMMIER_ALLER).distinct()

    fun ddl(c: CoucheAjoutee): String {
        val colonnes = buildList {
            add("\"fid\" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL")
            if (c.geometrie != null) add("\"geom\" ${c.geometrie}")
            for (col in c.colonnes) {
                add("\"${col.nom}\" ${col.type}" + (if (col.obligatoire) " NOT NULL" else "") + (if (col.unique) " UNIQUE" else ""))
            }
        }
        return "CREATE TABLE \"${c.nom}\" (${colonnes.joinToString(", ")})"
    }
}
