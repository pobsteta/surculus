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
import io.github.pobsteta.surculus.core.placette.EmpriseEtFrance
import java.sql.Connection

/**
 * Un objet des couches `foret`, `ug` ou `parcelles`. Géométrie en Lambert-93 (x = Est, y = Nord, mètres), trous compris.
 *
 * Identifiant, d'après sommieR 0.31.1 (lecture seule) :
 * - `foret.nom` : `R/travaux_qfield.R` l. 96-98 (`st_sf(nom = terrain$foret$nom[[1L]], …)`) ;
 * - `ug.numero` : `R/travaux_qfield.R` l. 99-101 (`st_sf(numero = ug$numero_affichage, …)`) ;
 * - `parcelles.reference` et `parcelles.designation` : `R/qfield.R` l. 345-356 (`ecrire_parcelles`).
 * Aucun UUID n'existe pour ces couches ; [fid] est le repli technique (clé du GeoPackage).
 */
data class ObjetSurfacique(
    val fid: Long,
    /** `nom`, `numero` ou `reference` selon la couche ; `null` si le fichier ne le renseigne pas. */
    val id: String?,
    /** `designation` des parcelles (libellé), `null` pour les autres couches. */
    val designation: String?,
    /** [Geometrie.Polygone] ou [Geometrie.MultiPolygone]. */
    val geometrie: Geometrie,
)

enum class RaisonAnomalie { NULLE, VIDE, ILLISIBLE, TYPE_NON_POLYGONAL }

/** Un objet qui n'a pas pu être lu : la lecture des autres continue. */
data class AnomalieGeometrie(val couche: String, val fid: Long, val raison: RaisonAnomalie, val detail: String? = null)

/** Géométries de la tournée pour la carte (mission 011). Lecture seule. */
data class Cartographie(
    val foret: List<ObjetSurfacique>,
    val ug: List<ObjetSurfacique>,
    val parcelles: List<ObjetSurfacique>,
    val anomalies: List<AnomalieGeometrie>,
    /** Boîte englobante des UG (Lambert-93, mètres) ; `null` si aucune UG n'a de géométrie valide. */
    val empriseUg: EmpriseLambert93?,
)

internal object LectureCartographie {
    private val COLONNES_ID = mapOf("foret" to "nom", "ug" to "numero", "parcelles" to "reference")

    fun lire(cnx: Connection, geometries: Map<String, String>): Cartographie {
        val anomalies = ArrayList<AnomalieGeometrie>()
        val couches = listOf("foret", "ug", "parcelles").associateWith { lireCouche(cnx, it, geometries[it], anomalies) }
        for ((couche, objets) in couches) {
            val e = emprise(objets) ?: continue
            controlerFrance(couche, e)
        }
        return Cartographie(couches.getValue("foret"), couches.getValue("ug"), couches.getValue("parcelles"), anomalies, emprise(couches.getValue("ug")))
    }

    private fun lireCouche(cnx: Connection, couche: String, colonneGeom: String?, anomalies: MutableList<AnomalieGeometrie>): List<ObjetSurfacique> {
        if (colonneGeom == null || !cnx.existeTable(couche)) return emptyList()
        val objets = ArrayList<ObjetSurfacique>()
        for (l in cnx.lignes("SELECT * FROM ${couche.ident()} ORDER BY fid")) {
            val fid = (l["fid"] as? Number)?.toLong() ?: continue
            val blob = l.blob(colonneGeom)
            if (blob == null) {
                anomalies.add(AnomalieGeometrie(couche, fid, RaisonAnomalie.NULLE))
                continue
            }
            val g = try {
                GeometrieGpkg.lire(blob)
            } catch (e: IllegalArgumentException) {
                val m = e.message ?: ""
                anomalies.add(AnomalieGeometrie(couche, fid, if (m.contains("non polygonal")) RaisonAnomalie.TYPE_NON_POLYGONAL else RaisonAnomalie.ILLISIBLE, m))
                continue
            }
            if (g == null) {
                anomalies.add(AnomalieGeometrie(couche, fid, RaisonAnomalie.VIDE))
                continue
            }
            objets.add(ObjetSurfacique(fid, l.texte(COLONNES_ID.getValue(couche)), if (couche == "parcelles") l.texte("designation") else null, g))
        }
        return objets
    }

    private fun emprise(objets: List<ObjetSurfacique>): EmpriseLambert93? {
        val pts = objets.flatMap { it.geometrie.points() }
        if (pts.isEmpty()) return null
        val x0 = pts.minOf { it.xMetres }
        val x1 = pts.maxOf { it.xMetres }
        val y0 = pts.minOf { it.yMetres }
        val y1 = pts.maxOf { it.yMetres }
        return if (x0 < x1 && y0 < y1) EmpriseLambert93(x0, y0, x1, y1) else null
    }

    /** Refus si un coin de l'emprise tombe hors de France (axes inversés, degrés pris pour des mètres, oubli de reprojection). */
    private fun controlerFrance(couche: String, e: EmpriseLambert93) {
        val coins = listOf(
            PointLambert93(e.xMinMetres, e.yMinMetres), PointLambert93(e.xMaxMetres, e.yMaxMetres),
            PointLambert93(e.xMinMetres, e.yMaxMetres), PointLambert93(e.xMaxMetres, e.yMinMetres),
        )
        for (p in coins) {
            val c = EmpriseEtFrance.controler(p)
            if (!c.enFrance) throw GpkgRefuse(RefusGpkg.PointHorsFrance(couche, "emprise", p.xMetres, p.yMetres, c.inversionDetectee))
        }
    }
}
