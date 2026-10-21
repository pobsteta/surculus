package io.github.pobsteta.surculus.data.journal.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.pobsteta.surculus.data.journal.CleProjection
import io.github.pobsteta.surculus.data.journal.EtatAttendu
import io.github.pobsteta.surculus.data.journal.Projection
import io.github.pobsteta.surculus.data.journal.ScenariosJournal
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Rejeu de `fixtures/journal/{projection,conflits,ordre-total}.json` **à travers Room** : chaque ligne est
 * reçue dans la base (chaque ordre d'arrivée), puis la table `projection` est comparée aux attendus, et à la
 * reconstruction complète depuis le journal.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JournalRoomScenariosTest {

    private val contexte: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `chaque cas est rejoue dans la base, dans chaque ordre d'arrivee`() {
        var rejoues = 0
        for (f in listOf("projection", "conflits", "ordre-total", "remplacements-anormaux")) for (c in ScenariosJournal.cas(f)) {
            for (ordre in c.ordresDArrivee) {
                val db = JournalDatabase.construire(contexte, null).allowMainThreadQueries().build()
                try {
                    val depot = JournalRepository(db, "local", HorlogeFixe(), tourneeObligatoire = false)
                    ordre.forEach { depot.recevoir(it) }

                    val incrementale = depot.projection()
                    // Même résultat que la projection pure et que la reconstruction complète.
                    val pure = Projection.projeter(ordre)
                    assertEquals(pure.cles, incrementale, "${c.nom} : table incrémentale ≠ projection pure")
                    depot.reconstruireProjection()
                    assertEquals(incrementale, depot.projection(), "${c.nom} : reconstruction ≠ incrémentale")

                    if (c.attendu.has("projection")) {
                        val obtenu = incrementale.entries.associate { it.key.toString() to EtatAttendu.de(it.value) }
                        assertEquals(ScenariosJournal.attenduProjection(c.attendu), obtenu, c.nom)
                    }
                    ScenariosJournal.chaines(c.attendu, "anomalies")?.let { assertEquals(it, depot.anomalies(), c.nom) }
                    c.attendu.get("lignes_retenues")?.let { assertEquals(it.asInt, pure.lignesRetenues, c.nom) }
                    rejoues++
                } finally {
                    db.close()
                }
            }
        }
        assertTrue(rejoues >= 30, "trop peu de rejeux : $rejoues")
    }

    @Test
    fun `la cle de projection se lit par objet et type`() {
        val db = JournalDatabase.construire(contexte, null).allowMainThreadQueries().build()
        try {
            val depot = JournalRepository(db, "local", HorlogeFixe(), tourneeObligatoire = false)
            val l = ScenariosJournal.cas("projection").first { it.id == "JR-08" }.lignes
            l.forEach(depot::recevoir)
            assertEquals("U1", depot.etat("P1", "pointage")?.courant)
            assertEquals("U2", depot.etat("P1", "etat")?.courant)
            assertEquals(setOf(CleProjection("P1", "pointage"), CleProjection("P1", "etat")), depot.projection().keys)
        } finally {
            db.close()
        }
    }
}
