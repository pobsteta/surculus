/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.gpkg

import java.nio.file.Files
import java.nio.file.Path

/**
 * Ortho tuilée du dossier de la tournée (ADR 0009) : `ortho.mbtiles` ou `ortho.gpkg` en EPSG:3857.
 * Un seul prédicat, [lire], décide si un fichier est exploitable : l'état de l'ortho, la planification du préchargement
 * et la carte l'emploient tous les trois, pour qu'un fichier inutilisable ne passe jamais pour une ortho « complète ».
 */
object OrthoTuilee {
    val NOMS = listOf("ortho.mbtiles", "ortho.gpkg")

    /** Où lire les tuiles : [table], et [tms] vrai si les rangées sont numérotées du sud (MBTiles). */
    data class Description(val nom: String, val table: String, val tms: Boolean)

    /** Description du fichier [fichier], `null` s'il est illisible, tuilé dans un autre repère que EPSG:3857, ou sans tuiles. */
    fun lire(fichier: Path): Description? {
        if (!Files.isRegularFile(fichier)) return null
        val nom = fichier.fileName.toString()
        return try {
            Sqlite.ouvrir(fichier, lectureSeule = true).use { cnx ->
                if (nom.endsWith(".mbtiles")) {
                    if (cnx.existeTable("tiles") && cnx.aDesTuiles("tiles")) Description(nom, "tiles", tms = true) else null
                } else {
                    if (!cnx.existeTable("gpkg_contents") || !cnx.existeTable("gpkg_tile_matrix_set")) return null
                    val table = cnx.lignes("SELECT table_name FROM gpkg_contents WHERE data_type = 'tiles' LIMIT 1").firstOrNull()?.texte("table_name")
                        ?: return null
                    val srs = cnx.lignes("SELECT srs_id FROM gpkg_tile_matrix_set WHERE table_name = '${table.replace("'", "''")}'").firstOrNull()?.entier("srs_id")
                    if (srs == 3857 && cnx.existeTable(table) && cnx.aDesTuiles(table)) Description(nom, table, tms = false) else null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Ce que le fichier dit de lui-même ; `null` pour ce qu'il ne dit pas (rien n'est deviné ni pris ailleurs). */
    data class Metadonnees(val attribution: String?, val description: String?, val date: String?)

    /**
     * Source et date lues dans le fichier. MBTiles : table `metadata`, clés `attribution`, `description` et `date`.
     * GeoPackage : `description` de la table de tuiles de `gpkg_contents` ; sa `last_change` est la date de dernière
     * modification du fichier, pas celle de la prise de vue : elle n'est pas lue comme date. Ne lève jamais.
     */
    fun metadonnees(fichier: Path): Metadonnees {
        val vide = Metadonnees(null, null, null)
        if (!Files.isRegularFile(fichier)) return vide
        return try {
            Sqlite.ouvrir(fichier, lectureSeule = true).use { cnx ->
                if (fichier.fileName.toString().endsWith(".mbtiles")) {
                    if (!cnx.existeTable("metadata")) return vide
                    val valeurs = cnx.lignes("SELECT name, value FROM metadata").associate { it.texte("name") to it.texte("value") }
                    Metadonnees(valeurs["attribution"].nonVide(), valeurs["description"].nonVide(), valeurs["date"].nonVide())
                } else {
                    if (!cnx.existeTable("gpkg_contents")) return vide
                    val d = cnx.lignes("SELECT description FROM gpkg_contents WHERE data_type = 'tiles' LIMIT 1").firstOrNull()?.texte("description")
                    Metadonnees(null, d.nonVide(), null)
                }
            }
        } catch (e: Exception) {
            vide
        }
    }

    /** Où en est l'ortho d'un dossier : pas de fichier, fichier exploitable, ou fichier présent mais inutilisable. */
    sealed interface Etat {
        data object Absente : Etat
        data class Prete(val description: Description) : Etat
        data class Illisible(val nom: String) : Etat
    }

    fun etat(dossier: Path): Etat {
        dans(dossier)?.let { return Etat.Prete(it) }
        val present = NOMS.firstOrNull { Files.exists(dossier.resolve(it)) } ?: return Etat.Absente
        return Etat.Illisible(present)
    }

    /** Premier fichier exploitable du [dossier] (`ortho.mbtiles` d'abord), `null` si aucun. */
    fun dans(dossier: Path): Description? = NOMS.firstNotNullOfOrNull { lire(dossier.resolve(it)) }
}

/** Vrai si la table de tuiles porte au moins une tuile : un fichier vide n'est pas une ortho. */
private fun java.sql.Connection.aDesTuiles(table: String): Boolean = lignes("SELECT 1 AS x FROM ${table.ident()} LIMIT 1").isNotEmpty()

private fun String?.nonVide(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
