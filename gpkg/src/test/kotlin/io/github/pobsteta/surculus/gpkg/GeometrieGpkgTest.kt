package io.github.pobsteta.surculus.gpkg

import io.github.pobsteta.surculus.core.geo.PointLambert93
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * La géométrie GeoPackage écrite par Surculus est comparée octet par octet à celle de GDAL 3.11.4
 * (`ogr2ogr -f GPKG`, EPSG:2154, 2026-10-17) : un codage qui n'était vérifié que par son propre décodage
 * ne prouverait rien.
 */
class GeometrieGpkgTest {
    private fun hex(s: String) = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private val gdalPoint = hex("475000016A080000010100000000000000B1172A4100000010C46D5941")
    private val gdalPolygone = hex(
        "475000036A08000000000000B0172A4100000000C4172A4100000000C46D594100000080C66D59410103000000010000000500000000000000B0172A41" +
            "00000000C46D594100000000C4172A4100000000C46D594100000000C4172A4100000080C66D594100000000B0172A4100000080C66D59410000" +
            "0000B0172A4100000000C46D5941",
    )
    private val gdalMulti = hex(
        "475000036A08000000000000B0172A4100000000C4172A4100000000C46D594100000080C66D59410106000000010000000103000000010000000400" +
            "000000000000B0172A4100000000C46D594100000000C4172A4100000000C46D594100000000C4172A4100000080C66D594100000000B0172A41" +
            "00000000C46D5941",
    )

    private fun p(x: Double, y: Double) = PointLambert93(x, y)
    private val carre = listOf(p(855000.0, 6666000.0), p(855010.0, 6666000.0), p(855010.0, 6666010.0), p(855000.0, 6666010.0), p(855000.0, 6666000.0))

    @Test
    fun `un point s ecrit comme GDAL, sans enveloppe`() {
        assertContentEquals(gdalPoint, GeometrieGpkg.encoder(Geometrie.Point(p(855000.5, 6666000.25)), 2154))
    }

    @Test
    fun `un polygone s ecrit comme GDAL, avec l enveloppe minx maxx miny maxy`() {
        assertContentEquals(gdalPolygone, GeometrieGpkg.encoder(Geometrie.Polygone(listOf(carre)), 2154))
    }

    @Test
    fun `un multipolygone s ecrit comme GDAL`() {
        val triangle = listOf(p(855000.0, 6666000.0), p(855010.0, 6666000.0), p(855010.0, 6666010.0), p(855000.0, 6666000.0))
        assertContentEquals(gdalMulti, GeometrieGpkg.encoder(Geometrie.MultiPolygone(listOf(Geometrie.Polygone(listOf(triangle)))), 2154))
    }

    @Test
    fun `le decodage lit les blobs de GDAL et rend le srs_id et l enveloppe`() {
        val pt = GeometrieGpkg.decoder(gdalPoint)
        assertEquals(2154, pt.srsId)
        assertEquals(Enveloppe(855000.5, 855000.5, 6666000.25, 6666000.25), pt.enveloppe)
        assertEquals(p(855000.5, 6666000.25), GeometrieGpkg.point(gdalPoint))
        val poly = GeometrieGpkg.decoder(gdalPolygone)
        assertEquals(Enveloppe(855000.0, 855010.0, 6666000.0, 6666010.0), poly.enveloppe)
        assertNull(GeometrieGpkg.point(gdalPolygone), "un polygone n'est pas un point")
        // Multipolygone : l'enveloppe de l'en-tête est lue.
        assertEquals(Enveloppe(855000.0, 855010.0, 6666000.0, 6666010.0), GeometrieGpkg.decoder(gdalMulti).enveloppe)
    }

    @Test
    fun `sans enveloppe dans l en-tete, elle est calculee sur le WKB`() {
        // Polygone de GDAL dont on retire l'enveloppe : indicateur 0x01, 32 octets en moins.
        val sans = gdalPolygone.copyOfRange(0, 3) + byteArrayOf(0x01) + gdalPolygone.copyOfRange(4, 8) + gdalPolygone.copyOfRange(40, gdalPolygone.size)
        assertEquals(Enveloppe(855000.0, 855010.0, 6666000.0, 6666010.0), GeometrieGpkg.decoder(sans).enveloppe)
    }

    @Test
    fun `la boucle ecrire puis lire retrouve le point au millimetre pres`() {
        val x = 855123.456
        val y = 6665987.654
        val lu = GeometrieGpkg.point(GeometrieGpkg.encoder(Geometrie.Point(p(x, y)), 9794))!!
        assertEquals(x, lu.xMetres, 1e-9)
        assertEquals(y, lu.yMetres, 1e-9)
        assertEquals(9794, GeometrieGpkg.decoder(GeometrieGpkg.encoder(Geometrie.Point(p(x, y)), 9794)).srsId)
    }

    @Test
    fun `une geometrie vide est reconnue vide`() {
        // En-tête GeoPackage avec l'indicateur « vide » (bit 4) : OGC 12-128 § 2.1.3.
        val vide = byteArrayOf('G'.code.toByte(), 'P'.code.toByte(), 0, 0x11, 0x6A, 0x08, 0, 0, 1, 7, 0, 0, 0, 0, 0, 0, 0)
        val lue = GeometrieGpkg.decoder(vide)
        assertTrue(lue.vide)
        assertNull(lue.enveloppe)
        // Un point WKB à coordonnées NaN est vide aussi.
        val nan = GeometrieGpkg.encoder(Geometrie.Point(p(1.0, 1.0)), 2154).also {
            java.nio.ByteBuffer.wrap(it).order(java.nio.ByteOrder.LITTLE_ENDIAN).putDouble(13, Double.NaN).putDouble(21, Double.NaN)
        }
        assertTrue(GeometrieGpkg.decoder(nan).vide)
        assertNotNull(GeometrieGpkg.decoder(gdalPoint).enveloppe)
        assertFalse(GeometrieGpkg.decoder(gdalPoint).vide)
    }

    @Test
    fun `un blob qui n est pas du GeoPackage binaire est refuse`() {
        assertFailsWith<IllegalArgumentException> { GeometrieGpkg.decoder(byteArrayOf(1, 2, 3)) }
        assertFailsWith<IllegalArgumentException> { GeometrieGpkg.decoder(ByteArray(16)) }
    }

    @Test
    fun `un anneau non ferme ou trop court est refuse`() {
        assertFailsWith<IllegalArgumentException> { Geometrie.Polygone(listOf(carre.dropLast(1))) }
        assertFailsWith<IllegalArgumentException> { Geometrie.Polygone(listOf(listOf(p(0.0, 0.0), p(1.0, 0.0), p(0.0, 0.0)))) }
    }

    @Test
    fun `un point en Lambert-93 refuse des valeurs non finies`() {
        assertFailsWith<IllegalArgumentException> { PointLambert93(Double.NaN, 1.0) }
    }
}
