package io.github.pobsteta.surculus.gpkg

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Mission 087, test du `dev` : une tournée rendue par un Surculus d'avant la 087 (sa table `positions_controles` n'a pas
 * la colonne `sans_position_mesuree`, et ne porte de ligne que pour un contrôle avec position mesurée) se réexporte :
 * la colonne est ajoutée, l'ancienne ligne passe à 0 (faux, ce qui est vrai : elle avait une position), et le rejeu la
 * reconnaît à l'identique.
 *
 * Coordonnées : Lambert-93 (EPSG:2154), x = Est, y = Nord, mètres ; les placettes de [TourneeDeTest] sont à Couchey.
 */
class ExportTourneeRendueAvantTest {
    @TempDir
    lateinit var dossier: Path

    private val v = TourneeDeTest

    @Test
    fun `une tournee rendue sans la colonne sans_position_mesuree se reexporte, l'ancienne ligne a faux`() {
        val s = v.saisiesDeLaJournee()
        val premier = dossier.resolve("a.gpkg")
        ExportTravaux.exporter(v.couchey(dossier), premier, s, v.VERSION, v.EXPORTE_LE)
        // Le fichier d'avant la 087 : pas de colonne, pas de ligne pour le contrôle sans position (3002).
        Sqlite.ouvrir(premier).use { c ->
            c.createStatement().use { st ->
                st.executeUpdate("DELETE FROM positions_controles WHERE controle_uuid = '${v.uuid(3002)}'")
                st.executeUpdate("ALTER TABLE positions_controles DROP COLUMN sans_position_mesuree")
            }
        }
        val sortie = dossier.resolve("b.gpkg")
        val bilan = ExportTravaux.exporter(premier, sortie, s, v.VERSION, v.EXPORTE_LE)
        assertEquals(1, bilan.positions, "seule la ligne sans position est nouvelle")
        Sqlite.ouvrir(sortie, lectureSeule = true).use { c ->
            val l = c.lignes("SELECT controle_uuid, sans_position_mesuree AS s, geom IS NULL AS n FROM positions_controles ORDER BY controle_uuid")
            assertEquals(listOf(v.uuid(3001), v.uuid(3002)), l.map { it.texte("controle_uuid") })
            assertEquals(listOf(0, 1), l.map { it.entier("s") })
            assertEquals(listOf(0, 1), l.map { it.entier("n") })
        }
        // Rejouer encore : rien de nouveau.
        val troisieme = dossier.resolve("c.gpkg")
        assertEquals(0, ExportTravaux.exporter(sortie, troisieme, s, v.VERSION, v.EXPORTE_LE).positions)
        assertNull(Sqlite.ouvrir(troisieme, lectureSeule = true).use { c -> c.lignes("SELECT precision_m FROM positions_controles WHERE controle_uuid = '${v.uuid(3002)}'").single()["precision_m"] })
    }
}
