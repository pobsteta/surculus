package io.github.pobsteta.surculus.gpkg

import java.nio.file.Path
import java.sql.Connection

/**
 * Le contenu d'un GeoPackage, sans son empreinte : l'en-tête SQLite (autovacuum, compteur de changements) peut
 * différer d'une version de SQLite à l'autre, donc on compare tables, schéma et lignes, jamais le hash du fichier
 * (ADR 0011).
 */
object Contenu {
    /** Schéma (`sqlite_master`) et lignes de chaque table, rangés par nom puis par `rowid`. */
    fun de(chemin: Path): Map<String, List<String>> = Sqlite.ouvrir(chemin, lectureSeule = true).use { de(it) }

    fun de(cnx: Connection): Map<String, List<String>> {
        val sortie = sortedMapOf<String, List<String>>()
        val objets = cnx.lignes("SELECT type, name, sql FROM sqlite_master ORDER BY type, name")
        sortie["~schema"] = objets.map { "${it["type"]} ${it["name"]} ${it["sql"]}" }
        for (o in objets.filter { it["type"] == "table" }) {
            val nom = o["name"] as String
            if ((o["sql"] as String?)?.startsWith("CREATE VIRTUAL TABLE") == true) continue
            val lignes = cnx.lignes("SELECT * FROM ${nom.ident()} ORDER BY rowid").map { l ->
                l.entries.joinToString("|") { (k, v) ->
                    "$k=" + when (v) {
                        is ByteArray -> v.joinToString("") { "%02x".format(it) }
                        else -> v.toString()
                    }
                }
            }
            sortie[nom] = lignes
        }
        return sortie
    }

    /** Lignes de [table] pour les colonnes [colonnes], dans l'ordre du `fid`. */
    fun colonnes(chemin: Path, table: String, vararg colonnes: String): List<List<Any?>> =
        Sqlite.ouvrir(chemin, lectureSeule = true).use { cnx ->
            cnx.lignes("SELECT ${colonnes.joinToString(", ") { it.ident() }} FROM ${table.ident()} ORDER BY 1").map { l -> colonnes.map { l[it] } }
        }

    fun compter(chemin: Path, table: String): Int = Sqlite.ouvrir(chemin, lectureSeule = true).use {
        it.lignes("SELECT count(*) AS n FROM ${table.ident()}").first().entier("n")!!
    }
}
