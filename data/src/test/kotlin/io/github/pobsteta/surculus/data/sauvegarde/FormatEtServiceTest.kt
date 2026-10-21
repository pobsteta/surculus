package io.github.pobsteta.surculus.data.sauvegarde

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonParser
import io.github.pobsteta.surculus.data.journal.db.HorlogeFixe
import io.github.pobsteta.surculus.data.journal.db.JournalDatabase
import io.github.pobsteta.surculus.data.journal.db.JournalRepository
import io.github.pobsteta.surculus.data.sauvegarde.OutilsSursync.journalDe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Format publié (`docs/formats/`, `fixtures/sursync/`) et service de sauvegarde de bout en bout (Room + stockage factice). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FormatEtServiceTest {
    private val racine: File by lazy {
        var d: File? = File(System.getProperty("user.dir")).absoluteFile
        while (d != null && !File(d, "fixtures/sursync").isDirectory) d = d.parentFile
        checkNotNull(d)
    }
    private fun exemple(nom: String) = File(racine, "fixtures/sursync/$nom").readBytes()
    /** JR-02 dans l'ordre total du journal (le fichier suit cet ordre, pas celui des lignes d'entrée). */
    private val jr02 get() = journalDe("journal/projection.json#JR-02").sortedWith(compareBy({ it.saisiLeUtcMs }, { it.appareil }, { it.sequence }, { it.uuid }))

    @Test fun `l'exemple publie se lit et redonne le journal d'origine`() {
        val r = LectureSursync.lire(exemple("exemple-v1.sursync"))
        assertTrue(r.accepte); assertTrue(r.signalements.isEmpty())
        assertEquals(jr02, r.lignes)
        assertEquals("COUCHEY-2026", r.entete?.codeTournee)
        assertEquals(String(exemple("exemple-v1.jsonl")), OutilsSursync.decompresser(exemple("exemple-v1.sursync")))
    }

    @Test fun `l'ecriture redonne exactement le contenu de l'exemple publie`() {
        val octets = EcritureSursync.ecrire(EnTeteSursync(1, "COUCHEY-2026", "0.31.1", "a3f9c2d1", 1_791_968_707_000L, 2), jr02)
        assertEquals(String(exemple("exemple-v1.jsonl")), OutilsSursync.decompresser(octets))
    }

    @Test fun `exemples tronque et a empreinte fausse`() {
        assertTrue(Signalement.FICHIER_TRONQUE in LectureSursync.lire(exemple("exemple-v1-tronque.sursync")).signalements)
        val faux = LectureSursync.lire(exemple("exemple-v1-empreinte-fausse.sursync"))
        assertEquals(setOf(Signalement.EMPREINTE_DIFFERENTE), faux.signalements); assertEquals(2, faux.lignes.size)
    }

    @Test fun `le JSON Schema decrit les memes champs que l'ecriture`() {
        val schema = JsonParser.parseString(File(racine, "docs/formats/sursync-v1.schema.json").readText()).asJsonObject.getAsJsonObject("\$defs")
        fun requis(n: String) = schema.getAsJsonObject(n).getAsJsonArray("required").map { it.asString }.toSet()
        val lignes = OutilsSursync.lignesTexte(exemple("exemple-v1.sursync")).map { JsonParser.parseString(it).asJsonObject }
        assertEquals(requis("entete"), lignes.first().keySet())
        assertEquals(requis("saisie"), lignes[1].keySet())
        assertEquals(requis("pied"), lignes.last().keySet())
    }

    @Test fun `le service sauvegarde, suffixe un nom pris et fait tourner par tournee`() {
        val db = JournalDatabase.construire(ApplicationProvider.getApplicationContext<Context>(), null).allowMainThreadQueries().build()
        try {
            val depot = JournalRepository(db, "a3f9c2d1", HorlogeFixe()).also { d -> journalDe("journal/projection.json#JR-02").forEach(d::recevoir) }
            val fichiers = linkedMapOf<String, ByteArray>()
            val stockage = object : StockageSauvegardes {
                override fun noms() = fichiers.keys.toSet()
                override fun ecrire(nom: String, octets: ByteArray) { check(nom !in fichiers); fichiers[nom] = octets }
                override fun supprimer(nom: String) { fichiers.remove(nom) }
            }
            val autre = NomSauvegarde.nom("AUTRE", "a3f9c2d1", 1L).also { fichiers[it] = byteArrayOf(7) }
            val service = ServiceSauvegarde(depot, stockage, "a3f9c2d1", "0.31.1")
            val t = 1_791_968_707_000L
            val premier = service.sauvegarder("COUCHEY-2026", t)
            val second = service.sauvegarder("COUCHEY-2026", t) // même instant : jamais écrasé
            assertTrue(premier.nom != second.nom)
            assertEquals(2, fichiers.keys.count { it.startsWith("COUCHEY-2026-") })
            repeat(6) { service.sauvegarder("COUCHEY-2026", t + (it + 1) * 1000L * 60) }
            assertEquals(FormatSursync.SAUVEGARDES_GARDEES, fichiers.keys.count { it.startsWith("COUCHEY-2026-") })
            assertTrue(autre in fichiers)
            val dernier = fichiers.keys.filter { it.startsWith("COUCHEY-2026-") }.max()
            assertEquals(jr02, LectureSursync.lire(fichiers.getValue(dernier)).lignes)
        } finally { db.close() }
    }
}
