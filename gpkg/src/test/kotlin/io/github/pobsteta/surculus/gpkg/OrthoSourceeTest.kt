package io.github.pobsteta.surculus.gpkg

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/** Mission 059 : la source et la date de l'Ortho chantier viennent du fichier, jamais d'ailleurs ; un fichier illisible se dit. */
class OrthoSourceeTest {
    @TempDir
    lateinit var dossier: Path

    private val vide = OrthoTuilee.Metadonnees(null, null, null)

    private fun mbtiles(metadata: Map<String, String>, avecTable: Boolean = true): Path {
        val f = dossier.resolve("ortho.mbtiles")
        Sqlite.ouvrir(f).use { cnx ->
            cnx.createStatement().use { st ->
                st.execute("CREATE TABLE tiles (zoom_level INTEGER, tile_column INTEGER, tile_row INTEGER, tile_data BLOB)")
                st.execute("INSERT INTO tiles VALUES (14, 8000, 5000, x'FFD8')")
                if (avecTable) {
                    st.execute("CREATE TABLE metadata (name TEXT, value TEXT)")
                    metadata.forEach { (k, v) -> st.execute("INSERT INTO metadata VALUES ('$k', '${v.replace("'", "''")}')") }
                }
            }
        }
        return f
    }

    private fun gpkg(description: String?): Path {
        val f = dossier.resolve("ortho.gpkg")
        Sqlite.ouvrir(f).use { cnx ->
            cnx.createStatement().use { st ->
                st.execute("CREATE TABLE gpkg_contents (table_name TEXT, data_type TEXT, identifier TEXT, description TEXT, last_change TEXT)")
                st.execute("CREATE TABLE gpkg_tile_matrix_set (table_name TEXT, srs_id INTEGER)")
                st.execute("CREATE TABLE ortho (zoom_level INTEGER, tile_column INTEGER, tile_row INTEGER, tile_data BLOB)")
                val d = description?.let { "'${it.replace("'", "''")}'" } ?: "NULL"
                st.execute("INSERT INTO gpkg_contents VALUES ('ortho', 'tiles', 'ortho', $d, '2026-10-27T10:00:00.000Z')")
                st.execute("INSERT INTO gpkg_tile_matrix_set VALUES ('ortho', 3857)")
                st.execute("INSERT INTO ortho VALUES (14, 8000, 5000, x'FFD8')")
            }
        }
        return f
    }

    @Test
    fun `un mbtiles donne son attribution, sa description et sa date`() {
        val m = OrthoTuilee.metadonnees(mbtiles(mapOf("attribution" to "© IGN, BD ORTHO® · Licence Ouverte 2.0", "description" to "Couchey", "date" to "2024")))
        assertEquals("© IGN, BD ORTHO® · Licence Ouverte 2.0", m.attribution)
        assertEquals("2024", m.date)
        assertEquals("Couchey", m.description)
    }

    @Test
    fun `un mbtiles sans date ne donne pas de date, il ne la devine pas`() {
        val m = OrthoTuilee.metadonnees(mbtiles(mapOf("attribution" to "© IGN", "name" to "ortho 2024", "version" to "1.2")))
        assertEquals("© IGN", m.attribution)
        assertNull(m.date)
    }

    @Test
    fun `un mbtiles sans table metadata n'a ni source ni date, sans exception`() {
        assertEquals(vide, OrthoTuilee.metadonnees(mbtiles(emptyMap(), avecTable = false)))
    }

    @Test
    fun `une valeur vide est une valeur absente`() {
        assertEquals(vide, OrthoTuilee.metadonnees(mbtiles(mapOf("attribution" to "  ", "date" to ""))))
    }

    @Test
    fun `un geopackage donne la description de sa table de tuiles, sa date de modification n'est pas une prise de vue`() {
        val m = OrthoTuilee.metadonnees(gpkg("© IGN, BD ORTHO®"))
        assertEquals("© IGN, BD ORTHO®", m.description)
        assertNull(m.attribution)
        assertNull(m.date)
    }

    @Test
    fun `un fichier illisible ou absent donne des metadonnees vides, pas d'exception`() {
        val f = dossier.resolve("ortho.mbtiles")
        Files.writeString(f, "pas du sqlite")
        assertEquals(vide, OrthoTuilee.metadonnees(f))
        assertEquals(vide, OrthoTuilee.metadonnees(dossier.resolve("absent.mbtiles")))
    }

    @Test
    fun `etat - dossier sans fichier d'ortho`() {
        assertIs<OrthoTuilee.Etat.Absente>(OrthoTuilee.etat(dossier))
    }

    @Test
    fun `etat - fichier exploitable`() {
        mbtiles(emptyMap())
        val e = assertIs<OrthoTuilee.Etat.Prete>(OrthoTuilee.etat(dossier))
        assertEquals("ortho.mbtiles", e.description.nom)
    }

    @Test
    fun `etat - fichier corrompu est illisible, il n'est pas absent`() {
        Files.writeString(dossier.resolve("ortho.mbtiles"), "pas du sqlite")
        val e = assertIs<OrthoTuilee.Etat.Illisible>(OrthoTuilee.etat(dossier))
        assertEquals("ortho.mbtiles", e.nom)
    }

    @Test
    fun `etat - geopackage hors EPSG 3857 est illisible`() {
        val f = gpkg("x")
        Sqlite.ouvrir(f).use { it.createStatement().use { st -> st.execute("UPDATE gpkg_tile_matrix_set SET srs_id = 4326") } }
        assertIs<OrthoTuilee.Etat.Illisible>(OrthoTuilee.etat(dossier))
    }

    @Test
    fun `etat - un mbtiles corrompu n'empeche pas un geopackage exploitable`() {
        Files.writeString(dossier.resolve("ortho.mbtiles"), "pas du sqlite")
        gpkg("x")
        assertIs<OrthoTuilee.Etat.Prete>(OrthoTuilee.etat(dossier))
    }
}
