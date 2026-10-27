package io.github.pobsteta.surculus.core.carte

import io.github.pobsteta.surculus.core.geo.EmpriseLambert93
import io.github.pobsteta.surculus.core.geo.Projections
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** La carte ne se cadre que sur une emprise en France métropolitaine : la placette en Somalie (`CLAUDE.md`, chapitre 15). */
class EmpriseCarteTest {
    /** Carré de 400 m autour du piquet de Couchey (21) : voir PlacetteEnFranceTest. */
    private val couchey = EmpriseLambert93(846_000.0, 6_686_000.0, 846_400.0, 6_686_400.0)

    @Test
    fun `une UG de Couchey est en France`() {
        assertEquals(EmpriseCarte.Resultat.EnFrance, EmpriseCarte.controler(couchey))
    }

    @Test
    fun `Couchey tombe en France en degres, longitude (E) et latitude (N) dans l'ordre`() {
        // Projections.versWgs84 rend (longitude, latitude).
        val (lon, lat) = Projections.versWgs84(846_200.0, 6_686_200.0)
        assertTrue(lat in 47.0..47.5 && lon in 4.8..5.1, "lat=$lat lon=$lon")
    }

    @Test
    fun `emprise aux axes inverses refusee et signalee`() {
        val inversee = EmpriseLambert93(6_686_000.0, 846_000.0, 6_686_400.0, 846_400.0)
        val r = assertInstanceOf(EmpriseCarte.Resultat.HorsFrance::class.java, EmpriseCarte.controler(inversee))
        assertTrue(r.inversionDetectee)
    }

    @Test
    fun `emprise en Somalie refusee`() {
        // Degrés pris pour des mètres : tout près de (0, 0) en Lambert-93, loin de la France.
        val somalie = EmpriseLambert93(45.0, 2.0, 46.0, 3.0)
        val r = assertInstanceOf(EmpriseCarte.Resultat.HorsFrance::class.java, EmpriseCarte.controler(somalie))
        assertFalse(r.inversionDetectee)
    }
}
