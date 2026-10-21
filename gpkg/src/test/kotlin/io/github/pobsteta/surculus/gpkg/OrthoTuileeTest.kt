package io.github.pobsteta.surculus.gpkg

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Un seul prédicat pour l'état, la planification et la carte : un fichier inexploitable n'est pas une ortho (ADR 0009). */
class OrthoTuileeTest {
    @TempDir
    lateinit var dossier: Path

    private fun gpkgTuile(nom: String, srs: Int): Path {
        val f = dossier.resolve(nom)
        Sqlite.ouvrir(f).use { cnx ->
            cnx.createStatement().use { st ->
                st.execute("CREATE TABLE gpkg_contents (table_name TEXT, data_type TEXT)")
                st.execute("CREATE TABLE gpkg_tile_matrix_set (table_name TEXT, srs_id INTEGER)")
                st.execute("CREATE TABLE ortho (zoom_level INTEGER, tile_column INTEGER, tile_row INTEGER, tile_data BLOB)")
                st.execute("INSERT INTO gpkg_contents VALUES ('ortho', 'tiles')")
                st.execute("INSERT INTO gpkg_tile_matrix_set VALUES ('ortho', $srs)")
            }
        }
        return f
    }

    @Test
    fun `un gpkg tuile en 3857 est exploitable, rangees en XYZ`() {
        val d = assertNotNull(OrthoTuilee.lire(gpkgTuile("ortho.gpkg", 3857)))
        assertEquals("ortho", d.table)
        assertEquals(false, d.tms)
    }

    @Test
    fun `un gpkg tuile dans un autre repere n'est pas une ortho`() {
        assertNull(OrthoTuilee.lire(gpkgTuile("ortho.gpkg", 4326)))
        assertNull(OrthoTuilee.dans(dossier))
    }

    @Test
    fun `un fichier illisible n'est pas une ortho`() {
        Files.writeString(dossier.resolve("ortho.mbtiles"), "pas du sqlite")
        Files.writeString(dossier.resolve("ortho.gpkg"), "pas du sqlite")
        assertNull(OrthoTuilee.dans(dossier))
    }

    @Test
    fun `un mbtiles avec sa table tiles est exploitable en TMS`() {
        val f = dossier.resolve("ortho.mbtiles")
        Sqlite.ouvrir(f).use { it.createStatement().use { st -> st.execute("CREATE TABLE tiles (zoom_level INTEGER, tile_column INTEGER, tile_row INTEGER, tile_data BLOB)") } }
        val d = assertNotNull(OrthoTuilee.dans(dossier))
        assertEquals(true, d.tms)
    }

    @Test
    fun `sans fichier, rien`() {
        assertNull(OrthoTuilee.dans(dossier))
    }
}
