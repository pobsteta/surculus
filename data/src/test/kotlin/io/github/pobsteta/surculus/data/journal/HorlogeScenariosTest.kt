package io.github.pobsteta.surculus.data.journal

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Rejeu de `fixtures/journal/horloge.json` et `jour-civil.json`. */
class HorlogeScenariosTest {

    @Test
    fun `sauts d'horloge notes comme dans les scenarios`() {
        val cas = ScenariosJournal.cas("horloge")
        assertTrue(cas.size >= 10)
        for (c in cas) {
            val lignes = c.lignes
            val avant = lignes.toList()
            val obtenus = Horloge.sauts(lignes)
            val attendus = c.attendu.getAsJsonArray("sauts_notes")
            assertEquals(attendus.size(), obtenus.size, c.nom)
            attendus.forEachIndexed { i, a ->
                val o = a.asJsonObject
                val s = obtenus[i]
                assertEquals(o.getAsJsonArray("entre").map { it.asString }, listOf(s.avantUuid, s.apresUuid), c.nom)
                assertEquals(o.get("ecart_s").asLong * 1000, s.ecartMs, c.nom)
                assertEquals(ScenariosJournal.instantMs(o.get("heure_avant").asString), s.heureAvantMs, c.nom)
                assertEquals(ScenariosJournal.instantMs(o.get("heure_apres").asString), s.heureApresMs, c.nom)
            }
            // L'heure brute n'est jamais réécrite : les heures rendues par le code sont exactement les
            // `saisiLeUtcMs` des lignes d'entrée (retrouvées par uuid), et les lignes ne sont pas touchées.
            assertEquals(false, c.attendu.get("heures_brutes_reecrites").asBoolean)
            val parUuid = lignes.associateBy { it.uuid }
            obtenus.forEach { s ->
                assertEquals(parUuid.getValue(s.avantUuid).saisiLeUtcMs, s.heureAvantMs, c.nom)
                assertEquals(parUuid.getValue(s.apresUuid).saisiLeUtcMs, s.heureApresMs, c.nom)
            }
            assertEquals(avant, lignes, "${c.nom} : lignes modifiées")
            // (La conservation en base est vérifiée par JournalRoomScenariosTest.)
        }
    }

    @Test
    fun `jour civil Europe-Paris comme dans les scenarios, dont le changement d'heure du 25 octobre 2026`() {
        val cas = ScenariosJournal.cas("jour-civil")
        assertTrue(cas.size >= 15)
        for (c in cas) {
            val e = c.entree
            val gnss = e.get("heure_gnss")?.takeIf { !it.isJsonNull }?.asString?.let(ScenariosJournal::instantMs)
            val age = e.get("age_fix_ms")?.takeIf { !it.isJsonNull }?.asLong
            val jour = Horloge.jourCivil(ScenariosJournal.instantMs(e.get("saisi_le_utc").asString), gnss, age)
            assertEquals(LocalDate.parse(c.attendu.get("jour_civil").asString), jour, c.nom)
        }
    }

    @Test
    fun `deux appareils ou deux demarrages ne se comparent pas`() {
        fun l(uuid: String, app: String, dem: Long, s: Long, utc: Long, mono: Long) = LigneJournal(
            uuid, "P", TypeLigne.ETAT, null, null, "op", app, s, utc, null, mono, dem, null,
        )
        assertNull(Horloge.ecartMs(l("a", "A", 1, 1, 0, 0), l("b", "B", 1, 1, 9_000_000, 0)))
        assertNull(Horloge.ecartMs(l("a", "A", 1, 1, 0, 0), l("b", "A", 2, 2, 9_000_000, 0)))
    }
}
