package io.github.pobsteta.surculus.core.geo

import io.github.pobsteta.surculus.core.gnss.Position
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * La placette en Somalie (chapitre 15), côté projections reprises de Groma : un point de forêt française
 * doit tomber en France, et une inversion lat/lon ou x/y doit se voir.
 *
 * Référence : place de dépôt PD-01 de Couchey, **lon = 4.933034, lat = 47.259315** (WGS 84, EPSG:4326),
 * soit x = 846 164,76 m, y = 6 686 123,26 m en Lambert-93 (pyproj 3.6.1 / PROJ 9.4.0, `always_xy=True`),
 * valeurs de [PlacetteEnFranceTest].
 */
class ProjectionsFranceTest {
    private val france = EmpriseLambert93.FRANCE_METROPOLITAINE
    private val couchey = Position(latitude = 47.259315, longitude = 4.933034)

    @Test
    fun `Couchey en longitude latitude donne le Lambert-93 de pyproj`() {
        val (x, y) = Projections.versLambert93(lon = couchey.longitude, lat = couchey.latitude)
        assertEquals(846_164.76, x, 0.01)
        assertEquals(6_686_123.26, y, 0.01)
        assertTrue(france.contient(PointLambert93(xMetres = x, yMetres = y)))
    }

    @Test
    fun `Couchey avec longitude et latitude inversees tombe hors de France`() {
        val (x, y) = Projections.versLambert93(lon = couchey.latitude, lat = couchey.longitude)
        assertFalse(france.contient(PointLambert93(xMetres = x, yMetres = y)))
    }

    @Test
    fun `le retour Lambert-93 vers WGS 84 rend longitude puis latitude`() {
        val (lon, lat) = Projections.versWgs84(846_164.76, 6_686_123.26)
        assertEquals(4.933034, lon, 1e-6)
        assertEquals(47.259315, lat, 1e-6)
    }
}
