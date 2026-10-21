package io.github.pobsteta.surculus.data.sauvegarde

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.pobsteta.surculus.data.journal.TypeLigne
import io.github.pobsteta.surculus.data.journal.db.HorlogeFixe
import io.github.pobsteta.surculus.data.journal.db.JournalDatabase
import io.github.pobsteta.surculus.data.journal.db.JournalRepository
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Mission 031 : deux tournées sur le même téléphone, la sauvegarde et la restauration d'une seule. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TourneeParLigneTest {
    private val bases = mutableListOf<JournalDatabase>()
    private val contexte: Context get() = ApplicationProvider.getApplicationContext()

    @After fun fermer() { bases.forEach { it.close() } }

    private fun depot(appareil: String = "a3f9c2d1"): JournalRepository {
        val db = JournalDatabase.construire(contexte, null).allowMainThreadQueries().build().also { bases += it }
        return JournalRepository(db, appareil, HorlogeFixe())
    }

    private fun deuxTournees(): JournalRepository = depot().also {
        it.saisir("P-A1", TypeLigne.ETAT, """{"etat":"sain"}""", "op1", codeTournee = "TOURNEE-A")
        it.saisir("P-A2", TypeLigne.ETAT, """{"etat":"mort"}""", "op1", codeTournee = "TOURNEE-A")
        it.saisir("P-B1", TypeLigne.ETAT, """{"etat":"sain"}""", "op2", codeTournee = "TOURNEE-B")
        it.saisir("P-X", TypeLigne.ETAT, """{"etat":"sain"}""", "op2") // non rattachée
    }

    private fun stockage(fichiers: MutableMap<String, ByteArray>) = object : StockageSauvegardes {
        override fun noms() = fichiers.keys.toSet()
        override fun ecrire(nom: String, octets: ByteArray) { fichiers[nom] = octets }
        override fun supprimer(nom: String) { fichiers.remove(nom) }
    }

    @Test fun `la sauvegarde de A ne contient aucune ligne de B ni aucune ligne non rattachee`() {
        val depot = deuxTournees()
        val fichiers = linkedMapOf<String, ByteArray>()
        val ecrite = ServiceSauvegarde(depot, stockage(fichiers), "a3f9c2d1", null).sauvegarder("TOURNEE-A", 1_791_968_707_000L)
        assertEquals(2, ecrite.lignes)
        val lues = LectureSursync.lire(fichiers.getValue(ecrite.nom))
        assertEquals(setOf("P-A1", "P-A2"), lues.lignes.map { it.objetUuid }.toSet())
        assertTrue(lues.lignes.all { it.codeTournee == "TOURNEE-A" })
        assertTrue(lues.signalements.isEmpty())
        // Le journal du téléphone, lui, garde tout.
        assertEquals(4, depot.lignes().size)
    }

    @Test fun `la restauration de A ne touche pas B et ne rattache rien a B`() {
        val source = deuxTournees()
        val octetsA = OutilsSursync.ecrire(source.lignes("TOURNEE-A"), code = "TOURNEE-A")
        val cible = depot("b7e1d4f2").also { it.saisir("P-B9", TypeLigne.ETAT, """{"etat":"sain"}""", "op3", codeTournee = "TOURNEE-B") }
        val avantB = cible.lignes("TOURNEE-B")
        val cr = Restauration.restaurer(octetsA, cible)
        assertEquals(2, cr.ajoutees)
        assertEquals(avantB, cible.lignes("TOURNEE-B"))
        assertEquals(source.lignes("TOURNEE-A"), cible.lignes("TOURNEE-A"))
        assertTrue(cible.lignes().none { it.codeTournee == "" })
    }

    @Test fun `une ligne d'une autre tournee dans le fichier de A est refusee et signalee`() {
        val source = deuxTournees()
        val deA = source.lignes("TOURNEE-A")
        val deB = source.lignes("TOURNEE-B").single()
        val entete = OutilsSursync.entete(1, 3).apply { addProperty("code_tournee", "TOURNEE-A") }
        val saisies = (deA + deB).map { EcritureSursync.versJson(it).apply { addProperty("code_tournee", it.codeTournee) }.toString() }
        val cible = depot("b7e1d4f2")
        val cr = Restauration.restaurer(OutilsSursync.fichier(listOf(entete.toString()) + saisies), cible)
        assertTrue(cr.accepte)
        assertEquals(setOf(Signalement.LIGNE_AUTRE_TOURNEE), cr.signalements)
        assertEquals(2, cr.ajoutees)
        assertEquals(1, cr.refusees)
        assertTrue(cible.lignes("TOURNEE-B").isEmpty())
        assertEquals(deA, cible.lignes("TOURNEE-A"))
    }

    @Test fun `une ligne sans code_tournee prend celui de l'en-tete`() {
        val l = OutilsSursync.journalDe("journal/projection.json#JR-01", "AUTRE").single()
        val brut = EcritureSursync.versJson(l)
        assertFalse(brut.has("code_tournee"))
        val lu = LectureSursync.lire(OutilsSursync.fichier(listOf(OutilsSursync.entete(1, 1).toString(), brut.toString())))
        assertEquals("COUCHEY-2026", lu.lignes.single().codeTournee)
    }

    @Test fun `un code_tournee de ligne qui n'est pas un texte est une ligne illisible`() {
        val l = OutilsSursync.journalDe("journal/projection.json#JR-01").single()
        val brut = EcritureSursync.versJson(l).apply { addProperty("code_tournee", 3) }
        val lu = LectureSursync.lire(OutilsSursync.fichier(listOf(OutilsSursync.entete(1, 1).toString(), brut.toString())))
        assertEquals(setOf(Signalement.LIGNE_ILLISIBLE), lu.signalements)
    }

    @Test fun `un en-tete sans code de tournee n'est pas lu`() {
        val entete = OutilsSursync.entete(1, 0).apply { addProperty("code_tournee", "") }
        assertEquals(MotifRefus.VERSION_ILLISIBLE, LectureSursync.lire(OutilsSursync.fichier(listOf(entete.toString()))).motif)
    }

    @Test fun `un meme uuid avec une autre tournee est un contenu different`() {
        val r = depot()
        val a = r.saisir("P1", TypeLigne.ETAT, """{"etat":"sain"}""", "op1", codeTournee = "TOURNEE-A")
        assertEquals(io.github.pobsteta.surculus.data.journal.db.Reception.Refusee, r.recevoir(a.copy(codeTournee = "TOURNEE-B")))
        assertEquals(listOf(a), r.lignes("TOURNEE-A"))
    }

    @Test fun `l'annulation et le saut d'horloge suivent la tournee de la ligne`() {
        val h = HorlogeFixe()
        val db = JournalDatabase.construire(contexte, null).allowMainThreadQueries().build().also { bases += it }
        val r = JournalRepository(db, "a3f9c2d1", h)
        val l = r.saisir("P1", TypeLigne.ETAT, """{"etat":"sain"}""", "op1", codeTournee = "TOURNEE-A")
        h.avancer(120_000); h.monoMs -= 0; h.utcMs += 3_600_000 // saut : l'heure avance, pas la monotone
        val ann = r.annuler(l.uuid, "op1")
        assertEquals("TOURNEE-A", ann.codeTournee)
        val dansA = r.lignes("TOURNEE-A")
        assertTrue(dansA.any { it.type == TypeLigne.SAUT_HORLOGE })
        assertEquals(r.lignes().size, dansA.size)
    }
}
