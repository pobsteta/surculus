package io.github.pobsteta.surculus.data.journal.db

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Mission 031 : migration v2 vers v3 (code de tournée), sur une base de la version précédente. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrationTourneeTest {
    private val contexte: Context get() = ApplicationProvider.getApplicationContext()
    private var db: JournalDatabase? = null

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), JournalDatabase::class.java)

    @After fun fermer() { db?.close() }

    private fun colonnes(t: String) =
        "uuid, objet_uuid, type, valeur, remplace_uuid, operateur, appareil, sequence, saisi_le_utc, heure_gnss, monotone_ms, demarrage, age_fix_ms"

    private val insertions = listOf(
        "INSERT INTO journal (uuid, objet_uuid, type, valeur, remplace_uuid, operateur, appareil, sequence, saisi_le_utc, heure_gnss, monotone_ms, demarrage, age_fix_ms) " +
            "VALUES ('U1','P1','etat','{\"etat\":\"sain\"}',NULL,'op1','A',1,1760000000123,1760000000000,4242,3,250)",
        "INSERT INTO journal (uuid, objet_uuid, type, valeur, remplace_uuid, operateur, appareil, sequence, saisi_le_utc, heure_gnss, monotone_ms, demarrage, age_fix_ms) " +
            "VALUES ('U2','P1','annulation',NULL,'U1','op1','A',2,1760000001000,NULL,5242,3,NULL)",
        "INSERT INTO refus_journal (uuid, objet_uuid, type, valeur, remplace_uuid, operateur, appareil, sequence, saisi_le_utc, heure_gnss, monotone_ms, demarrage, age_fix_ms) " +
            "VALUES ('U1','P1','etat','{\"etat\":\"mort\"}',NULL,'op1','A',1,1760000000123,NULL,4242,3,NULL)",
    )

    private fun lignes(sql: androidx.sqlite.db.SupportSQLiteDatabase, table: String, cols: String): List<List<String?>> =
        sql.query("SELECT $cols FROM $table ORDER BY 1").use { c ->
            generateSequence { if (c.moveToNext()) (0 until c.columnCount).map { if (c.isNull(it)) null else c.getString(it) } else null }.toList()
        }

    @Test
    fun `migration 2 vers 3 - toutes les lignes sont conservees, non rattachees, et le schema 3 est valide`() {
        contexte.deleteDatabase("m23.db")
        val avant = helper.createDatabase("m23.db", 2).use { sql ->
            insertions.forEach(sql::execSQL)
            lignes(sql, "journal", colonnes("journal")) to lignes(sql, "refus_journal", colonnes("refus_journal"))
        }
        assertEquals(2, avant.first.size)
        helper.runMigrationsAndValidate("m23.db", 3, true, JournalDatabase.MIGRATION_2_3).use { sql ->
            assertEquals(avant.first, lignes(sql, "journal", colonnes("journal")))
            assertEquals(avant.second, lignes(sql, "refus_journal", colonnes("refus_journal")))
            assertEquals(listOf(listOf<String?>("")), lignes(sql, "journal", "DISTINCT code_tournee").also { assertEquals(1, it.size) })
            assertEquals(listOf(listOf<String?>("")), lignes(sql, "refus_journal", "DISTINCT code_tournee"))
            sql.query("SELECT name FROM sqlite_master WHERE type='index' AND name='index_journal_code_tournee'").use {
                assertEquals(1, it.count)
            }
        }
    }

    @Test
    fun `la base v2 s'ouvre par Room en v3 sans rien perdre, les lignes non rattachees sont dans aucune tournee, ajout seul conserve`() {
        contexte.deleteDatabase("m23b.db")
        helper.createDatabase("m23b.db", 2).use { sql -> insertions.forEach(sql::execSQL) }
        val base = JournalDatabase.construire(contexte, "m23b.db").allowMainThreadQueries().build().also { db = it }
        assertEquals(4, base.openHelper.writableDatabase.version)
        assertEquals(2L, base.dao().compter())
        assertEquals("", base.dao().trouver("U1")!!.codeTournee)
        assertEquals(1, base.dao().tousLesRefus().size)
        assertTrue(base.dao().deLaTournee("").isEmpty())
        assertFailsWith<Exception> { base.openHelper.writableDatabase.execSQL("DELETE FROM journal") }
        assertFailsWith<Exception> { base.openHelper.writableDatabase.execSQL("UPDATE journal SET code_tournee = 'X'") }
        assertEquals(2L, base.dao().compter())
    }
}
