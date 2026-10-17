/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.gpkg

import org.sqlite.Function
import org.sqlite.SQLiteConfig
import java.nio.file.Path
import java.sql.Connection

/**
 * Accès SQLite du module (ADR 0011) : `sqlite-jdbc`, le même sur la JVM et sur l'appareil. On ouvre la
 * connexion sans passer par `DriverManager` (inutile, et le chargement des pilotes par `ServiceLoader`
 * est fragile sous Android), puis on déclare les fonctions `ST_*` qu'appellent les déclencheurs rtree.
 */
object Sqlite {
    /**
     * Réservé aux tests : saute les `fsync` (synchronous = OFF, journal en mémoire). Sur un disque saturé, chaque
     * validation prend des secondes et les centaines d'écritures des tests deviennent interminables. Jamais vrai
     * en production : un fichier écrit ainsi pourrait être corrompu par une coupure de courant.
     */
    internal var sansSynchronisationDisque = false

    /** Ouvre (ou crée) le fichier [chemin] ; les fonctions spatiales sont déclarées. */
    fun ouvrir(chemin: Path, lectureSeule: Boolean = false): Connection {
        val config = SQLiteConfig()
        if (lectureSeule) config.setReadOnly(true)
        if (sansSynchronisationDisque) {
            config.setSynchronous(SQLiteConfig.SynchronousMode.OFF)
            config.setJournalMode(SQLiteConfig.JournalMode.MEMORY)
        }
        val cnx = config.createConnection("jdbc:sqlite:${chemin.toAbsolutePath()}")
        try {
            FonctionsSpatiales.declarer(cnx)
        } catch (e: Throwable) {
            cnx.close()
            throw e
        }
        return cnx
    }

    /** Ouvre une base en mémoire (tests). */
    fun enMemoire(): Connection {
        val cnx = SQLiteConfig().createConnection("jdbc:sqlite::memory:")
        FonctionsSpatiales.declarer(cnx)
        return cnx
    }
}

/**
 * Fonctions SQL `ST_IsEmpty`, `ST_MinX`, `ST_MaxX`, `ST_MinY`, `ST_MaxY` du GeoPackage (annexe F.3, extension
 * `gpkg_rtree_index`), que les déclencheurs de sommieR appellent à chaque écriture dans une couche à géométrie.
 * SQLite ne les connaît pas : sans elles, tout `INSERT` dans `controles` échoue (« no such function »).
 *
 * Si une version de `sqlite-jdbc` ne les déclarait plus, le repli de l'ADR 0011 est la voie (e) :
 * suspendre les déclencheurs pendant l'écriture et remplir l'index en Kotlin.
 */
object FonctionsSpatiales {
    val NOMS = listOf("ST_IsEmpty", "ST_MinX", "ST_MaxX", "ST_MinY", "ST_MaxY")

    fun declarer(cnx: Connection) {
        Function.create(cnx, "ST_IsEmpty", Calcul { env -> if (env == null) 1 else 0 }, 1, Function.FLAG_DETERMINISTIC)
        Function.create(cnx, "ST_MinX", Calcul { env -> env?.xMin }, 1, Function.FLAG_DETERMINISTIC)
        Function.create(cnx, "ST_MaxX", Calcul { env -> env?.xMax }, 1, Function.FLAG_DETERMINISTIC)
        Function.create(cnx, "ST_MinY", Calcul { env -> env?.yMin }, 1, Function.FLAG_DETERMINISTIC)
        Function.create(cnx, "ST_MaxY", Calcul { env -> env?.yMax }, 1, Function.FLAG_DETERMINISTIC)
    }

    /**
     * Une fonction d'un argument (le blob de géométrie) : l'enveloppe, `null` si vide. Un argument NULL donne
     * NULL (les déclencheurs testent déjà `NOT NULL`).
     */
    private class Calcul(private val sur: (Enveloppe?) -> Number?) : Function() {
        override fun xFunc() {
            val blob = value_blob(0)
            if (blob == null) {
                result()
                return
            }
            val env = GeometrieGpkg.decoder(blob).enveloppe
            when (val r = sur(env)) {
                null -> result()
                is Int -> result(r)
                else -> result(r.toDouble())
            }
        }
    }
}
