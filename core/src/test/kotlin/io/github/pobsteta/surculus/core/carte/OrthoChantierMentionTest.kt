package io.github.pobsteta.surculus.core.carte

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Mission 059 : la mention de l'Ortho chantier dit sa source et sa date, ou dit qu'elles manquent ; elle n'invente rien. */
class OrthoChantierMentionTest {
    private val essai = "© IGN, BD ORTHO (ORTHOIMAGERY.ORTHOPHOTOS), Géoplateforme, téléchargée le 2026-10-27 pour un essai de Surculus"

    @Test
    fun `attribution et date de mise a jour du fichier`() {
        assertEquals(
            "Ortho chantier · © IGN, BD ORTHO® · Licence Ouverte 2.0 · mise à jour le 2024-06-15",
            OrthoChantier.mention(attribution = "© IGN, BD ORTHO® · Licence Ouverte 2.0", dateMiseAJour = "2024-06-15"),
        )
    }

    @Test
    fun `sans date de mise a jour rien n'est ajoute, ni prise de vue ni non indiquee`() {
        val m = OrthoChantier.mention(attribution = essai, dateMiseAJour = null)
        assertEquals("Ortho chantier · $essai", m)
        assertFalse("prise de vue" in m || "non indiqu" in m.substringAfter(essai))
    }

    @Test
    fun `sans attribution la mention dit source non indiquee`() {
        assertEquals("Ortho chantier · source non indiquée par le fichier", OrthoChantier.mention(null, null))
        assertEquals(
            "Ortho chantier · source non indiquée par le fichier · mise à jour le 2024-06-15",
            OrthoChantier.mention(null, "2024-06-15"),
        )
    }

    @Test
    fun `la licence n'est pas ajoutee par Surculus`() {
        assertFalse("Licence" in OrthoChantier.mention("© IGN", null))
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
        assertEquals("Ortho illisible : le fichier d'ortho de la tournée ne s'ouvre pas.", t)
        assertFalse(t.contains("mbtiles", ignoreCase = true) || t.contains("sqlite", ignoreCase = true) || t.contains("exception", ignoreCase = true))
    }
}
