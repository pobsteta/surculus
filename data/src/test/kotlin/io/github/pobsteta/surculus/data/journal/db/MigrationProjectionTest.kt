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

/** Mission 032 : migration v3 vers v4 (projection par tournée), sur une base de la version précédente. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrationProjectionTest {
    private val contexte: Context get() = ApplicationProvider.getApplicationContext()
    private var db: JournalDatabase? = null

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), JournalDatabase::class.java)

    @After fun fermer() { db?.close() }

    private val cols = "uuid, objet_uuid, type, valeur, remplace_uuid, operateur, appareil, sequence, saisi_le_utc, heure_gnss, monotone_ms, demarrage, age_fix_ms, code_tournee"
    private fun ins(uuid: String, tournee: String, seq: Int) =
        "INSERT INTO journal ($cols) VALUES ('$uuid','P1','etat','{\"etat\":\"sain\"}',NULL,'op1','A',$seq,${1760000000000L + seq},NULL,$seq,1,NULL,'$tournee')"

    private fun lire(sql: androidx.sqlite.db.SupportSQLiteDatabase, req: String): List<List<String?>> =
        sql.query(req).use { c ->
            generateSequence { if (c.moveToNext()) (0 until c.columnCount).map { if (c.isNull(it)) null else c.getString(it) } else null }.toList()
        }

    @Test
    fun `migration 3 vers 4 - journal intact, projection reconstruite par tournee, schema 4 valide`() {
        contexte.deleteDatabase("m34.db")
        val avant = helper.createDatabase("m34.db", 3).use { sql ->
            sql.execSQL(ins("U1", "A", 1)); sql.execSQL(ins("U2", "B", 2)); sql.execSQL(ins("U3", "", 3))
            // ancienne projection (clé objet, type) : sera recréée
            sql.execSQL("INSERT INTO projection VALUES ('P1','etat','U3',1,'U1,U2,U3','')")
            sql.execSQL(
                "INSERT INTO refus_journal ($cols) VALUES ('U1','P1','etat','{\"etat\":\"mort\"}',NULL,'op1','A',1,1760000000001,NULL,1,1,NULL,'A')",
            )
            lire(sql, "SELECT $cols FROM journal ORDER BY uuid") to lire(sql, "SELECT id, $cols FROM refus_journal ORDER BY id")
        }
        helper.runMigrationsAndValidate("m34.db", 4, true, JournalDatabase.MIGRATION_3_4).use { sql ->
            assertEquals(avant.first, lire(sql, "SELECT $cols FROM journal ORDER BY uuid"))
            assertEquals(1, avant.second.size)
            assertEquals(avant.second, lire(sql, "SELECT id, $cols FROM refus_journal ORDER BY id"))
            // U1 existe en deux contenus (journal et refus_journal) : la projection écarte les deux versions.
            assertEquals(
                listOf(
                    listOf<String?>("", "P1", "etat", "U3", "0"),
                    listOf<String?>("B", "P1", "etat", "U2", "0"),
                ),
                lire(sql, "SELECT code_tournee, objet_uuid, type, courant_uuid, conflit FROM projection ORDER BY code_tournee"),
            )
        }
    }
}
