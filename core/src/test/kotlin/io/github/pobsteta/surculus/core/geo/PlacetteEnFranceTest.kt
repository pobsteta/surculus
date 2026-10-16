package io.github.pobsteta.surculus.core.geo

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * La placette en Somalie (chapitre 15) : un point de forêt française doit tomber en France.
 *
 * Point de référence : place de dépôt PD-01 de la forêt communale de Couchey,
 * sommieR `R/demo.R` l. 318-321, `geom_point(4.933034, 47.259315)`, soit
 * **lon = 4.933034, lat = 47.259315** en WGS84 (EPSG:4326).
 * Reprojeté en Lambert-93 (EPSG:2154) avec pyproj 3.6.1 / PROJ 9.4.0 :
 * `Transformer.from_crs(4326, 2154, always_xy=True).transform(4.933034, 47.259315)`
 * donne **x = 846 164,76 m, y = 6 686 123,26 m**.
 */
class PlacetteEnFranceTest {

    private val france = EmpriseLambert93.FRANCE_METROPOLITAINE

    @Test
    fun `la place de depot de Couchey tombe en France metropolitaine`() {
        assertTrue(france.contient(PointLambert93(xMetres = 846_164.76, yMetres = 6_686_123.26)))
    }

    @Test
    fun `le chene de la Justice de Couchey tombe en France metropolitaine`() {
        // sommieR R/demo.R l. 468 : lon 4.961429, lat 47.255078 -> x 848 323,08, y 6 685 705,71 (pyproj, always_xy).
        assertTrue(france.contient(PointLambert93(xMetres = 848_323.08, yMetres = 6_685_705.71)))
    }

    @Test
    fun `Couchey avec x et y inverses tombe hors de France`() {
        assertFalse(france.contient(PointLambert93(xMetres = 6_686_123.26, yMetres = 846_164.76)))
    }

    @Test
    fun `des degres pris pour des metres tombent hors de France`() {
        assertFalse(france.contient(PointLambert93(xMetres = 4.933034, yMetres = 47.259315)))
    }

    @Test
    fun `Couchey reprojete en ordre lat lon pris pour x y tombe hors de France`() {
        // `cs2cs EPSG:4326 EPSG:2154` attend lat puis lon ; nourri en « lon lat », il rend
        // (6 572 345,74 ; 3 299 697,51) sans erreur (PROJ 9.4.0, essai du 2026-10-16).
        assertFalse(france.contient(PointLambert93(xMetres = 6_572_345.74, yMetres = 3_299_697.51)))
    }

    @Test
    fun `les bornes de l emprise sont incluses`() {
        assertTrue(france.contient(PointLambert93(france.xMinMetres, france.yMinMetres)))
        assertTrue(france.contient(PointLambert93(france.xMaxMetres, france.yMaxMetres)))
    }

    @Test
    fun `une coordonnee non finie est refusee`() {
        assertThrows<IllegalArgumentException> { PointLambert93(Double.NaN, 6_686_123.26) }
        assertThrows<IllegalArgumentException> { PointLambert93(846_164.76, Double.POSITIVE_INFINITY) }
    }

    @Test
    fun `une emprise inversee est refusee`() {
        assertThrows<IllegalArgumentException> { EmpriseLambert93(10.0, 0.0, 0.0, 10.0) }
    }
}
