package io.github.pobsteta.surculus.gpkg

import io.github.pobsteta.surculus.core.geo.PointLambert93
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.sqlite.SQLiteConfig
import java.nio.file.Path
import java.sql.SQLException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Les déclencheurs rtree de sommieR appellent `ST_IsEmpty`, `ST_MinX`, `ST_MaxX`, `ST_MinY`, `ST_MaxY` à chaque
 * écriture (ADR 0011). Ces tests établissent le constat et la parade.
 */
class FonctionsSpatialesTest {
    @TempDir
    lateinit var dossier: Path

    private fun blob(x: Double, y: Double) = GeometrieGpkg.encoder(Geometrie.Point(PointLambert93(x, y)), 2154)
    private val insertion = "INSERT INTO controles (geom, uuid, nb_total, nb_vivants) VALUES (?, '00000000-0000-4000-8000-000000000001', 4, 3)"

    @Test
    fun `sans les fonctions ST, ecrire dans controles echoue - le constat de l ADR 0011`() {
        val f = TourneeDeTest.modeleVide(dossier)
        SQLiteConfig().createConnection("jdbc:sqlite:$f").use { cnx ->
            val e = assertFailsWith<SQLException> {
                cnx.prepareStatement(insertion).use { it.setBytes(1, blob(855000.0, 6666000.0)); it.executeUpdate() }
            }
            assertTrue("ST_IsEmpty" in e.message.orEmpty(), "message : ${e.message}")
        }
    }

    @Test
    fun `avec les fonctions declarees en Kotlin, l ecriture passe et l index rtree est rempli`() {
        val f = TourneeDeTest.modeleVide(dossier)
        Sqlite.ouvrir(f).use { cnx ->
            cnx.prepareStatement(insertion).use { it.setBytes(1, blob(855000.5, 6666000.25)); it.executeUpdate() }
            val r = cnx.lignes("SELECT id, minx, maxx, miny, maxy FROM rtree_controles_geom").single()
            assertEquals(1, r.entier("id"))
            // L'index rtree de SQLite stocke des flottants 32 bits (pas de 0,5 m à 6 600 km) : minimum arrondi vers
            // le bas, maximum vers le haut avec une marge, la boîte contient toujours le point.
            assertTrue(r.reel("minx")!! <= 855000.5 && 855000.5 <= r.reel("maxx")!!)
            assertTrue(r.reel("miny")!! <= 6666000.25 && 6666000.25 <= r.reel("maxy")!!)
            assertTrue(r.reel("maxy")!! - r.reel("miny")!! <= 2.0)
            // Le déclencheur de mise à jour et de suppression passe aussi.
            cnx.createStatement().use { it.executeUpdate("DELETE FROM controles") }
            assertEquals(0, cnx.lignes("SELECT count(*) AS n FROM rtree_controles_geom").first().entier("n"))
        }
    }

    @Test
    fun `les cinq fonctions repondent sur un blob de GDAL, et NULL sur NULL`() {
        Sqlite.enMemoire().use { cnx ->
            val b = blob(10.0, 20.0)
            cnx.prepareStatement("SELECT ST_IsEmpty(?), ST_MinX(?), ST_MaxX(?), ST_MinY(?), ST_MaxY(?)").use { ps ->
                (1..5).forEach { ps.setBytes(it, b) }
                ps.executeQuery().use { rs ->
                    rs.next()
                    assertEquals(0, rs.getInt(1))
                    assertEquals(10.0, rs.getDouble(2)); assertEquals(10.0, rs.getDouble(3))
                    assertEquals(20.0, rs.getDouble(4)); assertEquals(20.0, rs.getDouble(5))
                }
            }
            cnx.createStatement().use { st ->
                st.executeQuery("SELECT ST_MinX(NULL) IS NULL").use { rs -> rs.next(); assertEquals(1, rs.getInt(1)) }
            }
        }
    }
}
