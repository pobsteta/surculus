package io.github.pobsteta.surculus.core.carte

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Mission 059 : la mention de l'Ortho chantier dit sa source et sa date, ou dit qu'elles manquent ; elle n'invente rien. */
class OrthoChantierMentionTest {
    @Test
    fun `source et date du fichier`() {
        assertEquals(
            "Ortho chantier · © IGN, BD ORTHO® · Licence Ouverte 2.0 · prise de vue 2024",
            OrthoChantier.mention(attribution = "© IGN, BD ORTHO® · Licence Ouverte 2.0", date = "2024"),
        )
    }

    @Test
    fun `sans date dans le fichier, la mention le dit et n'en invente pas`() {
        val m = OrthoChantier.mention(attribution = "© IGN, BD ORTHO®", date = null)
        assertEquals("Ortho chantier · © IGN, BD ORTHO® · date de prise de vue non indiquée par le fichier", m)
        assertFalse(Regex("20\\d\\d").containsMatchIn(m))
    }

    @Test
    fun `sans source dans le fichier, la mention le dit`() {
        assertEquals(
            "Ortho chantier · source non indiquée par le fichier · prise de vue 2024",
            OrthoChantier.mention(attribution = null, date = "2024"),
        )
    }

    @Test
    fun `ni source ni date`() {
        assertEquals(
            "Ortho chantier · source non indiquée par le fichier · date de prise de vue non indiquée par le fichier",
            OrthoChantier.mention(null, null),
        )
    }

    @Test
    fun `la mention remplace l'attribution du fond et se complete du cadastre`() {
        val m = "Ortho chantier · © IGN · prise de vue 2024"
        assertEquals(listOf(m), FondsCarte.mentions(Fond.ORTHO, cadastre = false, orthoChantier = m))
        assertEquals(listOf(m, "cadastre © IGN"), FondsCarte.mentions(Fond.ORTHO, cadastre = true, orthoChantier = m))
    }

    @Test
    fun `la mention de l'ortho n'apparait que sur le fond Ortho chantier`() {
        val m = "Ortho chantier · © IGN"
        assertEquals(listOf(Attributions.IGN), FondsCarte.mentions(Fond.PLAN_IGN, cadastre = false, orthoChantier = m))
        assertTrue(m !in FondsCarte.mentions(Fond.OSM, cadastre = true, orthoChantier = m))
    }

    @Test
    fun `le message d'ortho illisible est en francais sans code technique`() {
        val t = OrthoChantier.ILLISIBLE
        assertEquals("Ortho illisible : la carte montre le Plan IGN.", t)
        assertFalse(t.contains("mbtiles", ignoreCase = true) || t.contains("sqlite", ignoreCase = true) || t.contains("exception", ignoreCase = true))
    }
}
