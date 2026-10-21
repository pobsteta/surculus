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
                    if (cnx.existeTable("tiles")) Description(nom, "tiles", tms = true) else null
                } else {
                    if (!cnx.existeTable("gpkg_contents") || !cnx.existeTable("gpkg_tile_matrix_set")) return null
                    val table = cnx.lignes("SELECT table_name FROM gpkg_contents WHERE data_type = 'tiles' LIMIT 1").firstOrNull()?.texte("table_name")
                        ?: return null
                    val srs = cnx.lignes("SELECT srs_id FROM gpkg_tile_matrix_set WHERE table_name = '${table.replace("'", "''")}'").firstOrNull()?.entier("srs_id")
                    if (srs == 3857 && cnx.existeTable(table)) Description(nom, table, tms = false) else null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Premier fichier exploitable du [dossier] (`ortho.mbtiles` d'abord), `null` si aucun. */
    fun dans(dossier: Path): Description? = NOMS.firstNotNullOfOrNull { lire(dossier.resolve(it)) }
}
