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

    private val vide = OrthoTuilee.Metadonnees(null, null)

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
    fun `un mbtiles donne son attribution et sa date de mise a jour`() {
        val m = OrthoTuilee.metadonnees(mbtiles(mapOf("attribution" to "© IGN, BD ORTHO® · Licence Ouverte 2.0", "description" to "Couchey", "date_mise_a_jour" to "2024-06-15")))
        assertEquals(OrthoTuilee.Metadonnees("© IGN, BD ORTHO® · Licence Ouverte 2.0", "2024-06-15"), m)
    }

    @Test
    fun `l'ancienne cle date n'est plus lue, la description non plus`() {
        val m = OrthoTuilee.metadonnees(mbtiles(mapOf("attribution" to "© IGN", "name" to "ortho 2024", "date" to "2024", "description" to "Couchey")))
        assertEquals(OrthoTuilee.Metadonnees("© IGN", null), m)
    }

    @Test
    fun `un mbtiles sans table metadata n'a ni source ni date, sans exception`() {
        assertEquals(vide, OrthoTuilee.metadonnees(mbtiles(emptyMap(), avecTable = false)))
    }

    @Test
    fun `une valeur vide est une valeur absente`() {
        assertEquals(vide, OrthoTuilee.metadonnees(mbtiles(mapOf("attribution" to "  ", "date_mise_a_jour" to ""))))
    }

    @Test
    fun `un geopackage n'a pas d'attribution, sa description n'est jamais prise pour une source`() {
        val f = gpkg("© IGN, BD ORTHO®")
        assertEquals(vide, OrthoTuilee.metadonnees(f))
        assertEquals("Ortho chantier · source non indiquée par le fichier", OrthoTuilee.mention(f))
    }

    @Test
    fun `mention - attribution ou rien, sans date le fichier d'essai de Couchey`() {
        val essai = "© IGN, BD ORTHO (ORTHOIMAGERY.ORTHOPHOTOS), Géoplateforme, téléchargée le 2026-10-27 pour un essai de Surculus"
        assertEquals("Ortho chantier · $essai", OrthoTuilee.mention(mbtiles(mapOf("attribution" to essai, "description" to "Couchey", "name" to "ortho"))))
    }

    @Test
    fun `mention - avec date de mise a jour`() {
        assertEquals(
            "Ortho chantier · © IGN · mise à jour le 2024-06-15",
            OrthoTuilee.mention(mbtiles(mapOf("attribution" to "© IGN", "date_mise_a_jour" to "2024-06-15"))),
        )
    }

    @Test
    fun `mention - fichier sans metadata`() {
        assertEquals("Ortho chantier · source non indiquée par le fichier", OrthoTuilee.mention(mbtiles(emptyMap(), avecTable = false)))
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
