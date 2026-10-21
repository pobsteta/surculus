package io.github.pobsteta.surculus.data.journal.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.pobsteta.surculus.data.journal.CleProjection
import io.github.pobsteta.surculus.data.journal.LigneJournal
import io.github.pobsteta.surculus.data.journal.Projection
import io.github.pobsteta.surculus.data.journal.TypeLigne
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Mission 032 : l'état courant se calcule par tournée ; `saisir` refuse un code de tournée vide. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EtatParTourneeTest {
    private val contexte: Context get() = ApplicationProvider.getApplicationContext()
    private val bases = mutableListOf<JournalDatabase>()

    @After fun fermer() = bases.forEach { it.close() }

    private fun depot(obligatoire: Boolean = true): JournalRepository {
        val db = JournalDatabase.construire(contexte, null).allowMainThreadQueries().build().also { bases += it }
        return JournalRepository(db, "tel-A", HorlogeFixe(), tourneeObligatoire = obligatoire)
    }

    private fun ligne(uuid: String, tournee: String, type: String = "etat", objet: String = "PLANT-1",
                      remplace: String? = null, seq: Long) =
        LigneJournal(uuid, objet, type, null, remplace, "op1", "A", seq, 1_760_000_000_000L + seq * 1000, null, seq * 1000, 1, null, tournee)

    @Test
    fun `meme objet_uuid dans deux tournees - l'etat de chacune ne depend que de ses lignes`() {
        val d = depot()
        val a1 = d.saisir("PLANT-1", "etat", """{"etat":"sain"}""", "op1", codeTournee = "A")
        val b1 = d.saisir("PLANT-1", "etat", """{"etat":"sain"}""", "op1", codeTournee = "B")
        val a2 = d.saisir("PLANT-1", "etat", """{"etat":"mort"}""", "op1", remplaceUuid = a1.uuid, codeTournee = "A")
        println("RAPPORT 032 : A=${d.etat("PLANT-1", "etat", "A")?.courant} (attendu ${a2.uuid}), B=${d.etat("PLANT-1", "etat", "B")?.courant} (attendu ${b1.uuid})")
        assertEquals(a2.uuid, d.etat("PLANT-1", "etat", "A")?.courant)
        assertEquals(b1.uuid, d.etat("PLANT-1", "etat", "B")?.courant)
        assertNotEquals(d.etat("PLANT-1", "etat", "A")?.courant, d.etat("PLANT-1", "etat", "B")?.courant)
        assertEquals(setOf("A", "B"), d.projection().keys.map { it.codeTournee }.toSet())
        assertEquals(setOf(CleProjection("PLANT-1", "etat", "B")), d.projection("B").keys)
        // annuler dans A ne change rien dans B ; la projection reconstruite est la même
        d.annuler(a2.uuid, "op1")
        assertEquals(a1.uuid, d.etat("PLANT-1", "etat", "A")?.courant)
        assertEquals(b1.uuid, d.etat("PLANT-1", "etat", "B")?.courant)
        val avant = d.projection()
        d.reconstruireProjection()
        assertEquals(avant, d.projection())
    }

    @Test
    fun `un conflit dans A n'en cree pas dans B`() {
        val d = depot()
        d.saisir("P", "etat", "1", "op1", codeTournee = "A")
        d.saisir("P", "etat", "2", "op1", codeTournee = "A")
        d.saisir("P", "etat", "3", "op1", codeTournee = "B")
        assertTrue(d.etat("P", "etat", "A")!!.conflit)
        assertEquals(false, d.etat("P", "etat", "B")!!.conflit)
    }

    @Test
    fun `un remplacement ou une annulation visant une autre tournee est sans effet et leve remplace_autre_tournee`() {
        val d = depot(obligatoire = false)
        val a = ligne("U1", "A", seq = 1)
        val b = ligne("U2", "B", remplace = "U1", seq = 2)
        val annB = ligne("U3", "B", type = TypeLigne.ANNULATION, remplace = "U1", seq = 3)
        listOf(a, b, annB).forEach(d::recevoir)
        assertEquals("U1", d.etat("PLANT-1", "etat", "A")?.courant)
        val etB = d.etat("PLANT-1", "etat", "B")!!
        assertEquals("U2", etB.courant)
        assertEquals(listOf(Projection.REMPLACE_AUTRE_TOURNEE), etB.anomalies)
        assertEquals(listOf(Projection.REMPLACE_AUTRE_TOURNEE), d.etat("PLANT-1", TypeLigne.ANNULATION, "B")!!.anomalies)
        // ordre d'arrivée sans effet
        val d2 = depot(obligatoire = false)
        listOf(annB, b, a).forEach(d2::recevoir)
        assertEquals(d.projection(), d2.projection())
    }

    @Test
    fun `les lignes non rattachees forment leur propre etat et ne contaminent aucune tournee`() {
        val d = depot(obligatoire = false)
        d.recevoir(ligne("U1", "", seq = 1))
        d.recevoir(ligne("U2", "A", seq = 2))
        assertEquals("U1", d.etat("PLANT-1", "etat", "")?.courant)
        assertEquals("U2", d.etat("PLANT-1", "etat", "A")?.courant)
        assertNull(d.etat("PLANT-1", "etat", "B"))
    }

    @Test
    fun `saisir sans code de tournee est refuse par defaut, vide ou blanc`() {
        val d = depot()
        assertFailsWith<IllegalArgumentException> { d.saisir("P", "etat", null, "op1") }
        assertFailsWith<IllegalArgumentException> { d.saisir("P", "etat", null, "op1", codeTournee = "  ") }
        assertEquals(0, d.lignes().size)
        assertEquals("T1", d.saisir("P", "etat", null, "op1", codeTournee = "T1").codeTournee)
    }

    @Test
    fun `tourneeObligatoire false garde l'ancien comportement`() {
        assertEquals("", depot(obligatoire = false).saisir("P", "etat", null, "op1").codeTournee)
    }
}
