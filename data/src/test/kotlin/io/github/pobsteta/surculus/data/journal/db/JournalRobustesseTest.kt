package io.github.pobsteta.surculus.data.journal.db

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import io.github.pobsteta.surculus.data.journal.LigneJournal
import io.github.pobsteta.surculus.data.journal.Projection
import io.github.pobsteta.surculus.data.journal.ScenariosJournal
import io.github.pobsteta.surculus.data.journal.TypeLigne
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Arbitrages de Pascal du 2026-10-17 : autre objet, annulation à cible inconnue, types, migration, horloge. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JournalRobustesseTest {

    private val contexte: Context get() = ApplicationProvider.getApplicationContext()
    private val bases = mutableListOf<JournalDatabase>()

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), JournalDatabase::class.java)

    @After
    fun fermer() = bases.forEach { it.close() }

    private fun depot(nom: String? = null): JournalRepository {
        val db = JournalDatabase.construire(contexte, nom).allowMainThreadQueries().build().also { bases += it }
        return JournalRepository(db, "tel-A", HorlogeFixe())
    }

    private fun ligne(uuid: String, objet: String, type: String, remplace: String? = null, seq: Long, valeur: String? = null) =
        LigneJournal(
            uuid, objet, type, valeur, remplace, "op1", "A", seq, 1_760_000_000_000L + seq * 1000,
            null, seq * 1000, 1, null,
        )

    /** Incrémentale = reconstruction = projection pure, et rend la table. */
    private fun verifierTroisVoies(r: JournalRepository, entrees: List<LigneJournal>, nom: String) {
        val incrementale = r.projection()
        assertEquals(Projection.projeter(entrees).cles, incrementale, "$nom : incrémentale ≠ pure")
        r.reconstruireProjection()
        assertEquals(incrementale, r.projection(), "$nom : reconstruction ≠ incrémentale")
    }

    // --- remplacement ou annulation d'un AUTRE objet ---

    @Test
    fun `une correction d'un autre objet est gardee, sans effet, avec anomalie - dans les deux ordres`() {
        val u1 = ligne("U1", "P1", TypeLigne.ETAT, seq = 1, valeur = """{"etat":"sain"}""")
        val u2 = ligne("U2", "P2", TypeLigne.ETAT, remplace = "U1", seq = 2, valeur = """{"etat":"mort"}""")
        for (ordre in listOf(listOf(u1, u2), listOf(u2, u1))) {
            val r = depot()
            ordre.forEach { assertEquals(Reception.Ajoutee, r.recevoir(it)) }
            assertEquals("U1", r.etat("P1", "etat")?.courant, "la valeur de P1 ne doit pas être effacée")
            assertNull(r.etat("P2", "etat"), "la ligne d'un autre objet n'agit sur aucun objet")
            assertEquals(listOf("remplace_autre_objet"), r.anomalies())
            assertEquals(2, r.lignes().size, "gardée au journal")
            verifierTroisVoies(r, ordre, "correction autre objet")
        }
    }

    @Test
    fun `une annulation dont l'objet differe de celui de sa cible est gardee, sans effet, avec anomalie`() {
        val u1 = ligne("U1", "P1", TypeLigne.ETAT, seq = 1, valeur = """{"etat":"sain"}""")
        val u2 = ligne("U2", "P2", TypeLigne.ANNULATION, remplace = "U1", seq = 2)
        for (ordre in listOf(listOf(u1, u2), listOf(u2, u1))) {
            val r = depot()
            ordre.forEach { r.recevoir(it) }
            assertEquals("U1", r.etat("P1", "etat")?.courant)
            assertEquals(listOf("remplace_autre_objet"), r.anomalies())
            assertEquals(2, r.lignes().size)
            verifierTroisVoies(r, ordre, "annulation autre objet")
        }
    }

    @Test
    fun `saisir contrôle aussi l'autre objet - ligne gardee, sans effet, avec anomalie`() {
        val r = depot()
        val a = r.saisir("P1", TypeLigne.ETAT, """{"etat":"sain"}""", "op1")
        r.saisir("P2", TypeLigne.ETAT, """{"etat":"mort"}""", "op1", remplaceUuid = a.uuid)
        r.saisir("P3", TypeLigne.ANNULATION, null, "op1", remplaceUuid = a.uuid)
        assertEquals(a.uuid, r.etat("P1", "etat")?.courant)
        assertNull(r.etat("P2", "etat"))
        assertEquals(listOf("remplace_autre_objet"), r.anomalies())
        assertEquals(3, r.lignes().size)
        verifierTroisVoies(r, r.lignes(), "saisir autre objet")
    }

    // --- annulation à cible inconnue ---

    @Test
    fun `annulation a cible inconnue - ignoree et signalee, appliquee quand la cible arrive (deux ordres)`() {
        val u1 = ligne("U1", "P1", TypeLigne.ETAT, seq = 1, valeur = """{"etat":"sain"}""")
        val u2 = ligne("U2", "P1", TypeLigne.ANNULATION, remplace = "U1", seq = 2)

        val r = depot()
        r.recevoir(u2) // l'annulation arrive d'abord
        assertEquals(listOf("remplace_inconnu"), r.anomalies())
        assertTrue(r.projection().isEmpty())
        r.recevoir(u1) // puis sa cible
        assertEquals(emptyList(), r.anomalies())
        assertNull(r.etat("P1", "etat")?.courant, "l'annulation s'applique : plus de valeur courante")
        verifierTroisVoies(r, listOf(u2, u1), "annulation d'abord")

        val s = depot()
        s.recevoir(u1)
        s.recevoir(u2)
        assertNull(s.etat("P1", "etat")?.courant)
        assertEquals(emptyList(), s.anomalies())
        assertEquals(r.projection(), s.projection(), "l'ordre d'arrivée ne change rien")
    }

    // --- type ---

    @Test
    fun `liste fermee des cinq types - saisir refuse, recevoir ne l'ecrit pas`() {
        val r = depot()
        assertEquals(
            setOf("controle", "pointage", "etat", "annulation", "saut_horloge"), TypeLigne.TOUS,
        )
        assertFailsWith<IllegalArgumentException> { r.saisir("P1", "inventé", null, "op1") }
        assertFailsWith<IllegalArgumentException> { r.saisir("P1", "", null, "op1") }
        assertEquals(Reception.TypeInvalide, r.recevoir(ligne("U1", "P1", "ETAT", seq = 1)))
        assertEquals(Reception.TypeInvalide, r.recevoir(ligne("U2", "P1", "autre", seq = 2)))
        assertTrue(r.lignes().isEmpty())
        TypeLigne.TOUS.forEachIndexed { i, t -> assertEquals(Reception.Ajoutee, r.recevoir(ligne("V$i", "P$i", t, seq = i + 1L))) }
    }

    // --- scénarios d'horloge : l'heure brute est conservée en base ---

    @Test
    fun `les heures brutes de horloge json sont conservees telles quelles en base`() {
        for (c in ScenariosJournal.cas("horloge")) {
            val r = depot()
            c.lignes.forEach { r.recevoir(it) }
            assertEquals(c.lignes.sortedWith(Projection.ORDRE_TOTAL), r.lignes(), c.nom)
        }
    }

    // --- migration v1 -> v2 ---

    private val insertionV1 =
        "INSERT INTO journal (uuid, objet_uuid, type, valeur, remplace_uuid, operateur, appareil, sequence, " +
            "saisi_le_utc, heure_gnss, monotone_ms, demarrage, age_fix_ms) " +
            "VALUES ('U1','P1','etat','{\"etat\":\"sain\"}',NULL,'op1','A',7,1760000000123,1760000000000,4242,3,250)"

    @Test
    fun `migration 1 vers 2 - la ligne ecrite en v1 est intacte`() {
        contexte.deleteDatabase("m.db")
        helper.createDatabase("m.db", 1).use { it.execSQL(insertionV1) }
        helper.runMigrationsAndValidate("m.db", 2, true, JournalDatabase.MIGRATION_1_2).use { sql ->
            sql.query("SELECT uuid, objet_uuid, type, valeur, remplace_uuid, operateur, appareil, sequence, saisi_le_utc, heure_gnss, monotone_ms, demarrage, age_fix_ms FROM journal").use { c ->
                assertEquals(1, c.count)
                c.moveToFirst()
                assertEquals(
                    listOf("U1", "P1", "etat", """{"etat":"sain"}""", null, "op1", "A"),
                    (0..6).map { if (c.isNull(it)) null else c.getString(it) },
                )
                assertEquals(listOf(7L, 1760000000123L, 1760000000000L, 4242L, 3L, 250L), (7..12).map { c.getLong(it) })
            }
            sql.query("SELECT name FROM sqlite_master WHERE type='index' AND name='index_journal_saisi_le_utc'").use {
                assertEquals(1, it.count, "l'index de la v2 doit exister")
            }
        }
    }

    @Test
    fun `la base v1 s'ouvre par Room en v2 sans rien perdre, et le journal reste en ajout seul`() {
        contexte.deleteDatabase("m2.db")
        helper.createDatabase("m2.db", 1).use { it.execSQL(insertionV1) }
        val db = JournalDatabase.construire(contexte, "m2.db").allowMainThreadQueries().build().also { bases += it }
        assertEquals(2, db.openHelper.writableDatabase.version)
        val l = db.dao().trouver("U1")!!.versLigne()
        assertEquals(7L, l.sequence)
        assertEquals(250L, l.ageFixMs)
        assertFailsWith<Exception> { db.openHelper.writableDatabase.execSQL("DELETE FROM journal") }
        assertEquals(1L, db.dao().compter())
    }

    // --- horloge de l'appareil : démarrage sans BOOT_COUNT ---

    @Test
    fun `sans BOOT_COUNT, un redemarrage change le numero de demarrage et ne fait noter aucun faux saut`() {
        android.provider.Settings.Global.putString(contexte.contentResolver, android.provider.Settings.Global.BOOT_COUNT, null)
        var mono = 5_000_000L
        val horloge = HorlogeAppareil(contexte) { mono }
        val d1 = horloge.demarrage()
        mono += 60_000
        assertEquals(d1, horloge.demarrage(), "même démarrage tant que l'horloge monotone avance")
        mono = 3_000 // redémarrage : l'horloge monotone repart de zéro
        val d2 = horloge.demarrage()
        assertTrue(d2 > d1, "le redémarrage doit changer le numéro")

        // Bout en bout : deux saisies de part et d'autre du redémarrage, heure civile cohérente.
        var utc = 1_760_000_000_000L
        mono = 5_000_000L
        val source = object : SourceHorloge {
            override fun maintenantUtcMs() = utc
            override fun monotoneMs() = mono
            override fun demarrage() = horloge.demarrage()
        }
        val r = JournalRepository(
            JournalDatabase.construire(contexte, null).allowMainThreadQueries().build().also { bases += it },
            "tel-A", source,
        )
        r.saisir("P1", TypeLigne.ETAT, null, "op1")
        utc += 600_000; mono = 1_000 // 10 minutes plus tard, après un redémarrage
        r.saisir("P2", TypeLigne.ETAT, null, "op1")
        assertTrue(r.lignes().none { it.type == TypeLigne.SAUT_HORLOGE }, "faux saut noté au redémarrage")
    }
}
