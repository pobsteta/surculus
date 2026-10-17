package io.github.pobsteta.surculus.data.journal.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Delete
import androidx.room.Update
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import io.github.pobsteta.surculus.data.journal.Horloge
import io.github.pobsteta.surculus.data.journal.TypeLigne
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Journal Room : ajout seul, durabilité, horloge, annulation, absence de repli destructif, schéma exporté. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JournalDurableTest {

    private val contexte: Context get() = ApplicationProvider.getApplicationContext()
    private val horloge = HorlogeFixe()
    private var db: JournalDatabase? = null

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), JournalDatabase::class.java)

    @Before
    fun nettoyer() {
        listOf("t.db", "coupure.db", "copie.db", "migration.db").forEach { contexte.deleteDatabase(it) }
    }

    @After
    fun fermer() {
        db?.close()
    }

    private fun ouvrir(nom: String = "t.db") =
        JournalDatabase.construire(contexte, nom).allowMainThreadQueries().build().also { db = it }

    private fun depot(base: JournalDatabase = ouvrir()) = JournalRepository(base, "tel-A", horloge)

    // --- ajout seul ---

    @Test
    fun `le DAO n'offre ni mise a jour ni suppression du journal`() {
        val interdits = JournalDao::class.java.methods.filter { m ->
            m.isAnnotationPresent(Update::class.java) || m.isAnnotationPresent(Delete::class.java)
        }
        assertTrue(interdits.isEmpty(), "méthodes interdites : $interdits")
    }

    @Test
    fun `les declencheurs SQLite refusent UPDATE et DELETE sur le journal et sur les refus`() {
        val base = ouvrir()
        val r = depot(base)
        val l = r.saisir("P1", TypeLigne.ETAT, """{"etat":"sain"}""", "op1")
        val sql = base.openHelper.writableDatabase
        assertFailsWith<Exception> { sql.execSQL("UPDATE journal SET valeur = 'x' WHERE uuid = '${l.uuid}'") }
        assertFailsWith<Exception> { sql.execSQL("DELETE FROM journal WHERE uuid = '${l.uuid}'") }
        r.recevoir(l.copy(valeur = """{"etat":"mort"}""")) // crée une ligne dans refus_journal
        assertFailsWith<Exception> { sql.execSQL("UPDATE refus_journal SET valeur = 'x'") }
        assertFailsWith<Exception> { sql.execSQL("DELETE FROM refus_journal") }
        assertEquals(l, r.lignes().single())
    }

    @Test
    fun `un uuid deja present n'est jamais ecrase par le DAO`() {
        val base = ouvrir()
        val l = depot(base).saisir("P1", TypeLigne.ETAT, """{"etat":"sain"}""", "op1")
        assertFailsWith<Exception> { base.dao().inserer(LigneJournalEntity.de(l.copy(valeur = "autre"))) }
        assertEquals(l, base.dao().trouver(l.uuid)?.versLigne())
    }

    // --- saisie : uuid v7, séquence, horloges ---

    @Test
    fun `chaque saisie recoit un uuid v7 croissant et la sequence locale suivante`() {
        val r = depot()
        val lignes = List(5) { r.saisir("P$it", TypeLigne.ETAT, null, "op1") }
        assertEquals(listOf(1L, 2, 3, 4, 5), lignes.map { it.sequence })
        assertTrue(lignes.all { Regex("^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").matches(it.uuid) })
        assertEquals(lignes.map { it.uuid }.sorted(), lignes.map { it.uuid })
        assertTrue(lignes.all { it.appareil == "tel-A" && it.demarrage == 1L && it.monotoneMs == 1_000_000L })
    }

    @Test
    fun `l'age du fix GNSS est mesure sur l'horloge monotone`() {
        val r = depot()
        val fix = FixGnss(heureUtcMs = 1_759_999_998_000L, monotoneMs = horloge.monoMs - 2_000)
        val l = r.saisir("C1", TypeLigne.CONTROLE, null, "op1", fix = fix)
        assertEquals(2_000L, l.ageFixMs)
        assertEquals(1_759_999_998_000L, l.heureGnssMs)
        assertNull(r.saisir("C2", TypeLigne.CONTROLE, null, "op1").ageFixMs)
    }

    @Test
    fun `horloge reculee d'une heure en cours de demarrage - saut note, heures brutes intactes`() {
        val r = depot()
        val a = r.saisir("P1", TypeLigne.ETAT, null, "op1")
        horloge.utcMs -= 3_480_000L // le téléphone recule d'une heure…
        horloge.monoMs += 120_000L //  …alors que 2 minutes réelles passent
        val b = r.saisir("P2", TypeLigne.ETAT, null, "op1")
        val toutes = r.lignes()
        val saut = toutes.single { it.type == TypeLigne.SAUT_HORLOGE }
        val valeurSaut = saut.valeur.orEmpty()
        assertEquals("tel-A", saut.objetUuid)
        assertTrue("\"ecart_ms\":-3600000" in valeurSaut, valeurSaut)
        assertTrue(a.uuid in valeurSaut && b.uuid in valeurSaut)
        // L'heure brute n'est pas réécrite.
        assertEquals(a.saisiLeUtcMs, toutes.single { it.uuid == a.uuid }.saisiLeUtcMs)
        assertEquals(1_760_000_000_000L - 3_480_000L, toutes.single { it.uuid == b.uuid }.saisiLeUtcMs)
        // Le saut n'est pas un état : pas de clé de projection.
        assertTrue(r.projection().keys.none { it.type == TypeLigne.SAUT_HORLOGE })
    }

    @Test
    fun `pas de saut apres un redemarrage ni pour 60 s de derive`() {
        val r = depot()
        r.saisir("P1", TypeLigne.ETAT, null, "op1")
        horloge.utcMs += 60_000L + 120_000L
        horloge.monoMs += 120_000L
        r.saisir("P2", TypeLigne.ETAT, null, "op1") // dérive de 60 s exactement : rien
        horloge.boot = 2
        horloge.monoMs = 500
        horloge.utcMs -= 7_200_000L
        r.saisir("P3", TypeLigne.ETAT, null, "op1") // autre démarrage : rien
        assertTrue(r.lignes().none { it.type == TypeLigne.SAUT_HORLOGE })
    }

    @Test
    fun `jour civil Europe-Paris au changement d'heure du 25 octobre 2026`() {
        val avant = Horloge.jourCivil(java.time.Instant.parse("2026-10-24T22:30:00Z").toEpochMilli(), null, null)
        val apres = Horloge.jourCivil(java.time.Instant.parse("2026-10-25T22:59:59Z").toEpochMilli(), null, null)
        assertEquals("2026-10-25", avant.toString())
        assertEquals("2026-10-25", apres.toString())
    }

    // --- annulation ---

    @Test
    fun `annuler defait, annuler l'annulation refait`() {
        val r = depot()
        val a = r.saisir("P1", TypeLigne.ETAT, """{"etat":"sain"}""", "op1")
        val b = r.saisir("P1", TypeLigne.ETAT, """{"etat":"mort"}""", "op1", remplaceUuid = a.uuid)
        assertEquals(b.uuid, r.etat("P1", "etat")?.courant)
        val an = r.annuler(b.uuid, "op1")
        assertEquals(TypeLigne.ANNULATION, an.type)
        assertEquals("P1", an.objetUuid)
        assertEquals(a.uuid, r.etat("P1", "etat")?.courant)
        r.annuler(an.uuid, "op1")
        assertEquals(b.uuid, r.etat("P1", "etat")?.courant)
        r.reconstruireProjection()
        assertEquals(b.uuid, r.etat("P1", "etat")?.courant)
    }

    @Test
    fun `deux corrections concurrentes sont signalees puis resolues par annulation d'une tete`() {
        val r = depot()
        val a = r.saisir("P1", TypeLigne.ETAT, """{"etat":"sain"}""", "op1")
        val b = r.saisir("P1", TypeLigne.ETAT, """{"etat":"atteint"}""", "op1", remplaceUuid = a.uuid)
        val c = r.saisir("P1", TypeLigne.ETAT, """{"etat":"mort"}""", "op2", remplaceUuid = a.uuid)
        val e = r.etat("P1", "etat")!!
        assertTrue(e.conflit)
        assertEquals(listOf(b.uuid, c.uuid), e.tetes)
        assertEquals(c.uuid, e.courant)
        r.annuler(c.uuid, "op1")
        val apres = r.etat("P1", "etat")!!
        assertEquals(false, apres.conflit)
        assertEquals(b.uuid, apres.courant)
    }

    @Test
    fun `reception - doublon ignore, contenu different refuse, et la version presente est ecartee`() {
        val r = depot()
        val l = r.saisir("P1", TypeLigne.ETAT, """{"etat":"sain"}""", "op1")
        assertEquals(Reception.Doublon, r.recevoir(l))
        assertEquals(l.uuid, r.etat("P1", "etat")?.courant)
        assertEquals(Reception.Refusee, r.recevoir(l.copy(valeur = """{"etat":"mort"}""")))
        assertNull(r.etat("P1", "etat"), "les deux versions sont écartées")
        assertEquals(listOf("uuid_contenu_different"), r.anomalies())
        assertEquals(Reception.Refusee, r.recevoir(l.copy(valeur = """{"etat":"mort"}""")))
        assertEquals(1, db!!.dao().tousLesRefus().size, "la même version refusée n'est gardée qu'une fois")
    }

    // --- durabilité ---

    @Test
    fun `PRAGMA synchronous FULL et journal WAL a l'ouverture`() {
        val base = ouvrir()
        val sql = base.openHelper.writableDatabase
        // Les écritures passent par la connexion principale : on s'y place avec une transaction
        // (en WAL, une lecture simple peut être servie par une connexion secondaire).
        sql.beginTransaction()
        try {
            sql.query("PRAGMA synchronous").use { c -> c.moveToFirst(); assertEquals(2, c.getInt(0)) }
            sql.setTransactionSuccessful()
        } finally {
            sql.endTransaction()
        }
        sql.query("PRAGMA journal_mode").use { c -> c.moveToFirst(); assertEquals("wal", c.getString(0).lowercase()) }
    }

    @Test
    fun `coupure en pleine saisie - la derniere saisie validee est la au redemarrage`() {
        val base = ouvrir("coupure.db")
        val r = depot(base)
        r.saisir("P1", TypeLigne.ETAT, """{"etat":"sain"}""", "op1")
        val derniere = r.saisir("P1", TypeLigne.ETAT, """{"etat":"mort"}""", "op1")
        // « On coupe » : sans fermer la base, on prend l'état des fichiers sur le disque (principal + WAL),
        // comme les verrait un redémarrage après coupure de courant.
        val src = contexte.getDatabasePath("coupure.db")
        val dst = contexte.getDatabasePath("copie.db")
        dst.parentFile?.mkdirs()
        src.copyTo(dst, overwrite = true)
        File(src.path + "-wal").takeIf { it.exists() }?.copyTo(File(dst.path + "-wal"), overwrite = true)

        val apres = JournalDatabase.construire(contexte, "copie.db").allowMainThreadQueries().build()
        try {
            val lignes = apres.dao().toutes()
            assertEquals(2, lignes.size)
            assertNotNull(lignes.singleOrNull { it.uuid == derniere.uuid })
            assertEquals(derniere.uuid, JournalRepository(apres, "tel-A", horloge).etat("P1", "etat")?.courant)
        } finally {
            apres.close()
        }
    }

    @Test
    fun `une saisie rejetee en cours de transaction ne laisse rien derriere elle`() {
        val base = ouvrir()
        val r = depot(base)
        r.saisir("P1", TypeLigne.ETAT, null, "op1")
        val avant = r.lignes()
        assertFailsWith<Exception> {
            // Le premier ajout réussit, le second échoue (uuid déjà pris) : la transaction entière est annulée.
            base.runInTransaction {
                base.dao().inserer(LigneJournalEntity.de(avant.single().copy(uuid = "nouvelle", sequence = 2)))
                base.dao().inserer(LigneJournalEntity.de(avant.single()))
            }
        }
        assertEquals(avant, r.lignes())
    }

    // --- schéma et migrations ---

    @Test
    fun `aucun repli destructif - une base plus recente que le code n'est pas effacee`() {
        val r = depot(ouvrir("t.db"))
        val l = r.saisir("P1", TypeLigne.ETAT, """{"etat":"sain"}""", "op1")
        db!!.close(); db = null
        SQLiteDatabase.openDatabase(contexte.getDatabasePath("t.db").path, null, SQLiteDatabase.OPEN_READWRITE).use {
            it.execSQL("PRAGMA user_version = 3")
        }
        assertFailsWith<Exception> { ouvrir("t.db").openHelper.writableDatabase }
        db = null
        SQLiteDatabase.openDatabase(contexte.getDatabasePath("t.db").path, null, SQLiteDatabase.OPEN_READONLY).use {
            it.rawQuery("SELECT uuid FROM journal", null).use { c ->
                assertTrue(c.moveToFirst(), "la ligne a été effacée")
                assertEquals(l.uuid, c.getString(0))
            }
        }
    }

    @Test
    fun `le schema 1 exporte correspond aux entites, et la base y revient identique`() {
        helper.createDatabase("migration.db", 1).use { sql ->
            sql.execSQL(
                "INSERT INTO journal (uuid, objet_uuid, type, valeur, remplace_uuid, operateur, appareil, sequence, " +
                    "saisi_le_utc, heure_gnss, monotone_ms, demarrage, age_fix_ms) " +
                    "VALUES ('U1','P1','etat','{}',NULL,'op1','A',1,1760000000000,NULL,1000,1,NULL)",
            )
        }
        // Le schéma 1 reste exporté (data/schemas/1.json) : la validation le compare aux entités de l'époque.
        helper.runMigrationsAndValidate("migration.db", 1, true).use { sql ->
            sql.query("SELECT COUNT(*) FROM journal").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
        }
    }
}
