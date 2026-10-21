package io.github.pobsteta.surculus.data.journal.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    version = 3,
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

        /** Toutes les migrations connues. Jamais de repli destructif. */
        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)

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
