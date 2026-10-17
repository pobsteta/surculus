package io.github.pobsteta.surculus.data.journal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Rejeu pur (sans base) de `fixtures/journal/{projection,conflits,ordre-total}.json`, chaque ordre d'arrivée. */
class ProjectionScenariosTest {

    private val fichiers = listOf("projection", "conflits", "ordre-total", "remplacements-anormaux")

    @Test
    fun `chaque cas des scenarios de projection est rejoue dans chaque ordre d'arrivee`() {
        var rejoues = 0
        for (f in fichiers) for (c in ScenariosJournal.cas(f)) {
            val resultats = c.ordresDArrivee.map { Projection.projeter(it) }
            resultats.forEach { verifier(c, it); rejoues++ }
            // Le résultat est identique pour tous les ordres d'arrivée.
            resultats.forEach { assertEquals(resultats.first(), it, "${c.nom} : dépend de l'ordre d'arrivée") }
        }
        assertTrue(rejoues >= 30, "trop peu de cas rejoués : $rejoues")
    }

    private fun verifier(c: CasJournal, r: ResultatProjection) {
        val att = c.attendu
        if (att.has("projection")) {
            val obtenu = r.cles.entries.associate { it.key.toString() to EtatAttendu.de(it.value) }
            assertEquals(ScenariosJournal.attenduProjection(att, obtenu), obtenu, c.nom)
        }
        att.getAsJsonArray("cles")?.let { cles ->
            assertEquals(cles.map { it.asString }.toSet(), r.cles.keys.map { it.toString() }.toSet(), c.nom)
        }
        ScenariosJournal.chaines(att, "ordre_total")?.let { assertEquals(it, r.ordreTotal, c.nom) }
        if (att.has("lignes_retenues")) assertEquals(att.get("lignes_retenues").asInt, r.lignesRetenues, c.nom)
        ScenariosJournal.chaines(att, "lignes_ignorees")?.let { assertEquals(it, r.lignesIgnorees, c.nom) }
        ScenariosJournal.chaines(att, "lignes_refusees")?.let { assertEquals(it, r.lignesRefusees, c.nom) }
        ScenariosJournal.chaines(att, "anomalies")?.let { assertEquals(it, r.anomalies, c.nom) }
    }
}
