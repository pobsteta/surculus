package io.github.pobsteta.surculus.data.journal.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 1,
    exportSchema = true,
)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun dao(): JournalDao

    companion object {
        const val NOM = "journal.db"

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
            return b.setJournalMode(JournalMode.WRITE_AHEAD_LOGGING).addCallback(OUVERTURE)
        }

        fun ouvrir(context: Context, nom: String = NOM): JournalDatabase = construire(context, nom).build()
    }
}
