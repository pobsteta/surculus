package io.github.pobsteta.surculus.data.journal.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.pobsteta.surculus.data.journal.LigneJournal
import io.github.pobsteta.surculus.data.journal.Projection

/**
 * Base du journal (ADR 0005).
 *
 * - `exportSchema = true` : les schémas sont versionnés dans `data/schemas/` ; chaque migration est testée.
 * - **Aucun repli destructif** : ni `fallbackToDestructiveMigration`, ni sur rétrogradation. Une base que le
 *   code ne sait pas ouvrir fait échouer l'ouverture, elle n'est jamais effacée (Marculus le faisait ;
 *   Surculus ne le reprend pas).
 * - Écriture durable : mode WAL et `PRAGMA synchronous=FULL`, une transaction par saisie.
 */
@Database(
    entities = [LigneJournalEntity::class, RefusJournalEntity::class, ProjectionEntity::class],
    version = 4,
    exportSchema = true,
)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun dao(): JournalDao

    companion object {
        const val NOM = "journal.db"

        /**
         * v1 -> v2 : un index sur `journal(saisi_le_utc)` pour l'ordre total. Ne touche à aucune ligne
         * (`CREATE INDEX` seul : ni copie, ni suppression de table), donc sans perte.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_saisi_le_utc` ON `journal` (`saisi_le_utc`)")
            }
        }

        /**
         * v2 -> v3 : le code de la tournée de chaque ligne (mission 031). `ADD COLUMN` seul, avec la valeur
         * `''` (ligne **non rattachée**) : aucune ligne n'est copiée, modifiée ni supprimée (les déclencheurs
         * d'ajout seul ne se déclenchent pas), et aucune ligne n'est rattachée à une tournée par supposition.
         * Les lignes non rattachées restent au journal et n'entrent dans aucune sauvegarde.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `journal` ADD COLUMN `code_tournee` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `refus_journal` ADD COLUMN `code_tournee` TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_code_tournee` ON `journal` (`code_tournee`)")
            }
        }

        /**
         * v3 -> v4 : l'état courant se calcule par tournée (mission 032). La table `projection`, **dérivée**,
         * change de clé primaire : elle est recréée puis reconstruite depuis le journal (`Projection`). `journal` et
         * `refus_journal` ne sont ni lus en écriture ni modifiés : aucune ligne n'est copiée, modifiée ni supprimée.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `projection`")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `projection` (`code_tournee` TEXT NOT NULL, `objet_uuid` TEXT NOT NULL, " +
                        "`type` TEXT NOT NULL, `courant_uuid` TEXT, `conflit` INTEGER NOT NULL, `tetes` TEXT NOT NULL, " +
                        "`anomalies` TEXT NOT NULL, PRIMARY KEY(`code_tournee`, `objet_uuid`, `type`))",
                )
                val lignes = lire(db, "journal") + lire(db, "refus_journal")
                Projection.projeter(lignes).cles.values.forEach { e ->
                    db.execSQL(
                        "INSERT INTO `projection` (code_tournee, objet_uuid, type, courant_uuid, conflit, tetes, anomalies) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)",
                        arrayOf<Any?>(
                            e.cle.codeTournee, e.cle.objetUuid, e.cle.type, e.courant, if (e.conflit) 1 else 0,
                            e.tetes.joinToString(","), e.anomalies.joinToString(","),
                        ),
                    )
                }
            }

            private fun lire(db: SupportSQLiteDatabase, table: String): List<LigneJournal> =
                db.query(
                    "SELECT uuid, objet_uuid, type, valeur, remplace_uuid, operateur, appareil, sequence, saisi_le_utc, " +
                        "heure_gnss, monotone_ms, demarrage, age_fix_ms, code_tournee FROM `$table`",
                ).use { c ->
                    generateSequence { if (c.moveToNext()) c else null }.map {
                        LigneJournal(
                            uuid = c.getString(0), objetUuid = c.getString(1), type = c.getString(2),
                            valeur = if (c.isNull(3)) null else c.getString(3),
                            remplaceUuid = if (c.isNull(4)) null else c.getString(4),
                            operateur = c.getString(5), appareil = c.getString(6), sequence = c.getLong(7),
                            saisiLeUtcMs = c.getLong(8), heureGnssMs = if (c.isNull(9)) null else c.getLong(9),
                            monotoneMs = c.getLong(10), demarrage = c.getLong(11),
                            ageFixMs = if (c.isNull(12)) null else c.getLong(12), codeTournee = c.getString(13),
                        )
                    }.toList()
                }
        }

        /** Toutes les migrations connues. Jamais de repli destructif. */
        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)

        /** Déclencheurs qui refusent toute modification ou suppression d'une ligne du journal. */
        internal val DECLENCHEURS = listOf(
            "CREATE TRIGGER IF NOT EXISTS journal_pas_de_maj BEFORE UPDATE ON journal " +
                "BEGIN SELECT RAISE(ABORT, 'journal en ajout seul'); END",
            "CREATE TRIGGER IF NOT EXISTS journal_pas_de_suppression BEFORE DELETE ON journal " +
                "BEGIN SELECT RAISE(ABORT, 'journal en ajout seul'); END",
            "CREATE TRIGGER IF NOT EXISTS refus_journal_pas_de_maj BEFORE UPDATE ON refus_journal " +
                "BEGIN SELECT RAISE(ABORT, 'journal en ajout seul'); END",
            "CREATE TRIGGER IF NOT EXISTS refus_journal_pas_de_suppression BEFORE DELETE ON refus_journal " +
                "BEGIN SELECT RAISE(ABORT, 'journal en ajout seul'); END",
            // INSERT OR REPLACE n'appelle BEFORE DELETE que si recursive_triggers est actif (SQLite, « ON CONFLICT »,
            // REPLACE) : on refuse donc l'insertion d'une clé déjà présente. Clé de journal : uuid ; clé de
            // refus_journal : id (plusieurs refus d'un même uuid sont légitimes). Id non fourni : selon la doc
            // SQLite, la valeur de NEW.rowid dans un BEFORE INSERT n'est alors pas définie. Sans effet ici : le DAO
            // ne fournit jamais d'id, la garde ne vise que l'id explicite d'un REPLACE ; indéfini, il ne trouve
            // aucune ligne et l'insertion reste permise (ce que veulent les refus successifs).
            "CREATE TRIGGER IF NOT EXISTS journal_pas_de_remplacement BEFORE INSERT ON journal " +
                "WHEN EXISTS (SELECT 1 FROM journal WHERE uuid = NEW.uuid) " +
                "BEGIN SELECT RAISE(ABORT, 'journal en ajout seul'); END",
            "CREATE TRIGGER IF NOT EXISTS refus_journal_pas_de_remplacement BEFORE INSERT ON refus_journal " +
                "WHEN EXISTS (SELECT 1 FROM refus_journal WHERE id = NEW.id) " +
                "BEGIN SELECT RAISE(ABORT, 'journal en ajout seul'); END",
        )

        /** À l'ouverture : durabilité maximale et déclencheurs (idempotent, donc aussi sur une base existante). */
        internal val OUVERTURE = object : Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                if (db.isReadOnly) return
                db.execSQL("PRAGMA synchronous=FULL")
                DECLENCHEURS.forEach(db::execSQL)
            }
        }

        fun construire(context: Context, nom: String? = NOM): Builder<JournalDatabase> {
            val appli = context.applicationContext
            val b = if (nom == null) Room.inMemoryDatabaseBuilder(appli, JournalDatabase::class.java)
            else Room.databaseBuilder(appli, JournalDatabase::class.java, nom)
            return b.setJournalMode(JournalMode.WRITE_AHEAD_LOGGING).addMigrations(*MIGRATIONS).addCallback(OUVERTURE)
        }

        fun ouvrir(context: Context, nom: String = NOM): JournalDatabase = construire(context, nom).build()
    }
}
