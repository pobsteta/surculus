/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.gpkg

import io.github.pobsteta.surculus.core.geo.EmpriseLambert93
import io.github.pobsteta.surculus.core.reperes.ReferentielTournee
import io.github.pobsteta.surculus.core.reperes.Resultat
import java.nio.file.Path
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException

/** Identifiant d'application d'un GeoPackage : « GPKG » (OGC 12-128 Req. 2). */
internal const val APPLICATION_ID_GPKG = 0x47504B47

/** Guillemets d'identifiant SQL. */
internal fun String.ident(): String = "\"" + replace("\"", "\"\"") + "\""

/** Une ligne de table : colonne → valeur. Lire par nom rend la lecture tolérante aux champs nouveaux. */
internal typealias Ligne = Map<String, Any?>

internal fun Connection.lignes(sql: String): List<Ligne> = createStatement().use { st ->
    st.executeQuery(sql).use { rs ->
        val meta = rs.metaData
        val noms = (1..meta.columnCount).map { meta.getColumnLabel(it) }
        buildList {
            while (rs.next()) add(noms.withIndex().associate { (i, n) -> n to rs.getObject(i + 1) })
        }
    }
}

internal fun Connection.existeTable(nom: String): Boolean =
    prepareStatement("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?").use { ps ->
        ps.setString(1, nom)
        ps.executeQuery().use(ResultSet::next)
    }

internal fun Ligne.texte(nom: String): String? = this[nom]?.toString()
internal fun Ligne.entier(nom: String): Int? = (this[nom] as? Number)?.toInt()
internal fun Ligne.reel(nom: String): Double? = (this[nom] as? Number)?.toDouble()
internal fun Ligne.booleen(nom: String): Boolean = (this[nom] as? Number)?.toInt() == 1
internal fun Ligne.blob(nom: String): ByteArray? = this[nom] as? ByteArray

/**
 * Lecture d'une tournée sommieR 0.31.1 (aller) ou d'un export de Surculus (tournée rendue). Rien n'est écrit.
 *
 * Refus (voir [RefusGpkg]) : fichier qui n'est pas un GeoPackage, couche de sommieR manquante, repère inconnu,
 * 2154 et 9794 mêlés (ADR 0003, ADR 0007).
 */
object LecteurTournee {
    fun lire(chemin: Path): Tournee {
        val cnx = try {
            Sqlite.ouvrir(chemin, lectureSeule = true)
        } catch (e: SQLException) {
            throw GpkgRefuse(RefusGpkg.PasUnGeoPackage(e.message ?: "ouverture impossible"))
        }
        cnx.use { return lire(it) }
    }

    fun lire(cnx: Connection): Tournee {
        try {
            return lireSansTraduction(cnx)
        } catch (e: SQLException) {
            throw GpkgRefuse(RefusGpkg.PasUnGeoPackage(e.message ?: "lecture impossible"))
        }
    }

    /**
     * Géométries de `foret`, `ug` et `parcelles` pour la carte, en lecture seule ; [lire] n'est pas modifié.
     * Mêmes refus que [lire], et [RefusGpkg.PointHorsFrance] si l'emprise d'une couche sort de France (axes inversés).
     *
     * Décisions de Pascal, 2026-10-21 :
     * - une `foret` illisible fait refuser la tournée dès [lire] ; [lire] n'est pas modifié ;
     * - un seul objet hors de France, quelle que soit la couche (`foret`, `ug`, `parcelles`), refuse toute la cartographie
     *   (un point hors du rectangle de France fait sortir un coin de l'emprise de sa couche).
     */
    fun lireCartographie(chemin: Path): Cartographie {
        val cnx = try {
            Sqlite.ouvrir(chemin, lectureSeule = true)
        } catch (e: SQLException) {
            throw GpkgRefuse(RefusGpkg.PasUnGeoPackage(e.message ?: "ouverture impossible"))
        }
        cnx.use { return lireCartographie(it) }
    }

    fun lireCartographie(cnx: Connection): Cartographie {
        lire(cnx) // repère et couches de sommieR : mêmes refus que la lecture de la tournée
        try {
            val geometries = if (cnx.existeTable("gpkg_geometry_columns")) {
                cnx.lignes("SELECT table_name, column_name FROM gpkg_geometry_columns").associate { it.texte("table_name")!! to it.texte("column_name")!! }
            } else {
                emptyMap()
            }
            return LectureCartographie.lire(cnx, geometries)
        } catch (e: SQLException) {
            throw GpkgRefuse(RefusGpkg.PasUnGeoPackage(e.message ?: "lecture impossible"))
        }
    }

    private fun lireSansTraduction(cnx: Connection): Tournee {
        val applicationId = cnx.createStatement().use { st -> st.executeQuery("PRAGMA application_id").use { it.next(); it.getInt(1) } }
        if (applicationId != APPLICATION_ID_GPKG && !cnx.existeTable("gpkg_contents")) {
            throw GpkgRefuse(RefusGpkg.PasUnGeoPackage("ni identifiant d'application GPKG, ni table gpkg_contents"))
        }
        if (!cnx.existeTable("gpkg_contents") || !cnx.existeTable("gpkg_spatial_ref_sys")) {
            throw GpkgRefuse(RefusGpkg.PasUnGeoPackage("tables gpkg_contents ou gpkg_spatial_ref_sys absentes"))
        }
        val geometries: Map<String, Pair<String, String>> = if (cnx.existeTable("gpkg_geometry_columns")) {
            cnx.lignes("SELECT table_name, column_name, geometry_type_name FROM gpkg_geometry_columns").associate {
                it.texte("table_name")!! to (it.texte("column_name")!! to it.texte("geometry_type_name")!!)
            }
        } else {
            emptyMap()
        }
        val couches = cnx.lignes("SELECT table_name, data_type, srs_id FROM gpkg_contents ORDER BY table_name").map { l ->
            val nom = l.texte("table_name")!!
            val nb = if (cnx.existeTable(nom)) cnx.lignes("SELECT count(*) AS n FROM ${nom.ident()}").first().entier("n")!!.toLong() else 0L
            CoucheLue(nom, l.texte("data_type")!!, if (nom in geometries) l.entier("srs_id") else null, geometries[nom]?.second, nb)
        }
        for (nom in Couches.COUCHES_SOMMIER_LUES) {
            if (couches.none { it.nom == nom }) throw GpkgRefuse(RefusGpkg.CoucheAbsente(nom))
        }

        // Repère : lu dans le fichier. Un code EPSG par couche à géométrie, tous lus dans gpkg_spatial_ref_sys.
        val codes = LinkedHashMap<Int, Int>() // srs_id -> code EPSG
        // Le standard veut le même srs_id dans gpkg_contents et gpkg_geometry_columns : on lit les deux, un désaccord
        // est un mélange de repères.
        val srsGeometries = if (cnx.existeTable("gpkg_geometry_columns")) {
            cnx.lignes("SELECT srs_id FROM gpkg_geometry_columns").mapNotNull { it.entier("srs_id") }
        } else {
            emptyList()
        }
        for (srs in couches.mapNotNull { it.srsId } + srsGeometries) {
            if (srs in codes) continue
            val l = cnx.lignes("SELECT organization, organization_coordsys_id FROM gpkg_spatial_ref_sys WHERE srs_id = $srs").firstOrNull()
            if (l == null || !"EPSG".equals(l.texte("organization"), ignoreCase = true)) throw GpkgRefuse(RefusGpkg.ReferentielSpatialInconnu(srs))
            codes[srs] = l.entier("organization_coordsys_id")!!
        }
        val epsg = when (val r = ReferentielTournee.verifier(codes.values)) {
            is Resultat.Accepte -> r.valeur
            is Resultat.Refuse -> throw GpkgRefuse(RefusGpkg.Referentiel(r.refus))
        }
        val srsId = codes.entries.first { it.value == epsg.code }.key

        fun geom(table: String) = geometries[table]?.first
        val placettes = cnx.lignes("SELECT * FROM \"placettes\" ORDER BY fid").map { l ->
            PlacetteTournee(
                uuid = l.texte("uuid") ?: "",
                code = l.texte("code_placette"),
                travauxId = l.texte("travaux_id"),
                rayonM = l.reel("rayon_m"),
                materialisation = l.texte("materialisation"),
                ug = l.texte("ug"),
                position = geom("placettes")?.let { g -> l.blob(g)?.let(GeometrieGpkg::point) },
                etatSuivi = EtatSuivi.depuis(l.texte("etat_suivi")),
            )
        }
        val controles = cnx.lignes("SELECT * FROM \"controles\" ORDER BY fid").map {
            ControleTournee(it.texte("uuid") ?: "", it.texte("placette_uuid"), it.texte("visite_le"))
        }
        val empriseForet = if (cnx.existeTable("foret") && geom("foret") != null) {
            val g = geom("foret")!!
            val enveloppes = cnx.lignes("SELECT ${g.ident()} AS g FROM \"foret\"").mapNotNull { l -> l.blob("g")?.let { GeometrieGpkg.decoder(it).enveloppe } }
            if (enveloppes.isEmpty()) {
                null
            } else {
                val x0 = enveloppes.minOf { it.xMin }
                val x1 = enveloppes.maxOf { it.xMax }
                val y0 = enveloppes.minOf { it.yMin }
                val y1 = enveloppes.maxOf { it.yMax }
                if (x0 < x1 && y0 < y1) EmpriseLambert93(x0, y0, x1, y1) else null
            }
        } else {
            null
        }
        val projet = if (cnx.existeTable("projet")) {
            cnx.lignes("SELECT * FROM \"projet\" ORDER BY rowid LIMIT 1").firstOrNull()?.let {
                ProjetLu(it.texte("nom"), it.texte("foret"), it.texte("operateur"), it.texte("prepare_le"), it.texte("version_sommier"), it.texte("version_surculus"))
            }
        } else {
            null
        }
        val plants = if (cnx.existeTable("plants")) {
            val g = geom("plants")
            cnx.lignes("SELECT * FROM \"plants\" ORDER BY placette_uuid, numero, uuid").map { l ->
                PlantLu(
                    uuid = l.texte("uuid") ?: "",
                    placetteUuid = l.texte("placette_uuid") ?: "",
                    numero = l.entier("numero") ?: 0,
                    essence = l.texte("essence"),
                    regarni = l.booleen("regarni"),
                    remplaceUuid = l.texte("remplace_uuid"),
                    position = g?.let { n -> l.blob(n)?.let(GeometrieGpkg::point) },
                )
            }
        } else {
            emptyList()
        }
        val etats = if (cnx.existeTable("etats_plants")) {
            cnx.lignes("SELECT * FROM \"etats_plants\" ORDER BY controle_uuid, plant_uuid, uuid").map { l ->
                EtatLu(
                    l.texte("uuid") ?: "", l.texte("controle_uuid") ?: "", l.texte("plant_uuid") ?: "", l.texte("etat") ?: "",
                    l.texte("cause_groupe"), l.texte("cause_detail"), l.entier("hauteur_cm"),
                )
            }
        } else {
            emptyList()
        }
        val codesTravaux = if (cnx.existeTable("codes")) {
            cnx.lignes("SELECT code FROM \"codes\"").mapNotNull { it.texte("code") }.toSet()
        } else {
            emptySet()
        }
        return Tournee(epsg, srsId, couches, placettes, controles, empriseForet, projet, plants, etats, codesTravaux)
    }
}
