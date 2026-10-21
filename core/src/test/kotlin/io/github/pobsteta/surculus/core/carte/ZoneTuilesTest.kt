package io.github.pobsteta.surculus.core.carte

import io.github.pobsteta.surculus.core.geo.EmpriseLambert93
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Préchargement de l'ortho (ADR 0009) : zooms 12 à 18, emprise testée en France métropolitaine, état visible. */
class ZoneTuilesTest {
    /** Carré de 400 m autour du piquet de Couchey (21) : voir PlacetteEnFranceTest. */
    private val couchey = EmpriseLambert93(846_000.0, 6_686_000.0, 846_400.0, 6_686_400.0)

    @Test
    fun `une UG de Couchey donne une zone de quelques centaines de tuiles, zooms 12 a 18`() {
        val zone = assertInstanceOf(ZoneTuiles.Resultat.Zone::class.java, ZoneTuiles.pourTournee(couchey)).zone
        assertEquals(12, zone.zoomMin)
        assertEquals(18, zone.zoomMax)
        assertTrue(zone.nombre in 20..5_000, "nombre = ${zone.nombre}")
        assertEquals(zone.nombre, zone.tuiles().count().toLong())
        assertEquals(12, zone.tuiles().first().z)
        assertEquals(18, zone.tuiles().last().z)
    }

    @Test
    fun `la zone entoure Couchey`() {
        val zone = (ZoneTuiles.pourTournee(couchey) as ZoneTuiles.Resultat.Zone).zone
        assertTrue(zone.emprise.contient(47.26, 4.933), "${zone.emprise}")
    }

    @Test
    fun `emprise aux axes inverses refusee et signalee`() {
        val inversee = EmpriseLambert93(6_686_000.0, 846_000.0, 6_686_400.0, 846_400.0)
        val r = assertInstanceOf(ZoneTuiles.Resultat.HorsFrance::class.java, ZoneTuiles.pourTournee(inversee))
        assertTrue(r.inversionDetectee)
    }

    @Test
    fun `emprise en Somalie refusee`() {
        // Degrés pris pour des mètres : tout près de (0, 0) en Lambert-93, loin de la France.
        val somalie = EmpriseLambert93(45.0, 2.0, 46.0, 3.0)
        val r = assertInstanceOf(ZoneTuiles.Resultat.HorsFrance::class.java, ZoneTuiles.pourTournee(somalie))
        assertFalse(r.inversionDetectee)
    }

    @Test
    fun `un departement entier est refuse`() {
        val grande = EmpriseLambert93(800_000.0, 6_650_000.0, 900_000.0, 6_750_000.0)
        assertInstanceOf(ZoneTuiles.Resultat.TropGrande::class.java, ZoneTuiles.pourTournee(grande))
    }

    @Test
    fun `etat de l'ortho absente partielle complete`() {
        assertEquals(EtatOrtho.Absente, EtatOrtho.depuis(0, 500))
        assertEquals(EtatOrtho.Absente, EtatOrtho.depuis(0, 0))
        assertEquals(EtatOrtho.Partielle(120, 500), EtatOrtho.depuis(120, 500))
        assertEquals(EtatOrtho.Complete, EtatOrtho.depuis(500, 500))
        // Préchargement allé au bout : complète même si des tuiles hors couverture manquent.
        assertEquals(EtatOrtho.Complete, EtatOrtho.depuis(480, 500, termine = true))
        assertEquals(EtatOrtho.Absente, EtatOrtho.depuis(0, 0, termine = true))
        // Jamais « complète » sans une seule tuile, même avec la marque de fin.
        assertEquals(EtatOrtho.Absente, EtatOrtho.depuis(0, 500, termine = true))
    }

    @Test
    fun `la marque de fin n'est ecrite que sans aucun echec`() {
        assertTrue(EtatOrtho.marqueAEcrire(0))
        assertFalse(EtatOrtho.marqueAEcrire(1))
        assertFalse(EtatOrtho.marqueAEcrire(19))
    }
}
