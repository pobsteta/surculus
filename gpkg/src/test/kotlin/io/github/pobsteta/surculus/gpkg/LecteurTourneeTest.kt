package io.github.pobsteta.surculus.gpkg

import io.github.pobsteta.surculus.core.placette.EmpriseEtFrance
import io.github.pobsteta.surculus.core.reperes.CodeEpsg
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LecteurTourneeTest {
    @TempDir
    lateinit var dossier: Path

    private fun refus(bloc: () -> Unit): RefusGpkg = assertFailsWith<GpkgRefuse> { bloc() }.refus

    @Test
    fun `le modele de sommieR 0_31_1 se lit - onze couches, repere 2154, aucune ligne`() {
        val t = LecteurTournee.lire(TourneeDeTest.modeleVide(dossier))
        assertEquals(CodeEpsg.RGF93_V1_LAMBERT93, t.epsg)
        assertEquals(2154, t.srsId)
        assertEquals(Couches.COUCHES_SOMMIER.toSet(), t.nomsCouches)
        assertEquals(emptyList(), t.placettes)
        assertNull(t.empriseForet)
        assertNull(t.projet)
        assertEquals(emptyList(), t.couchesAjoutees)
        assertEquals("attributes", t.couches.single { it.nom == "codes" }.typeDonnees)
        assertNull(t.couches.single { it.nom == "codes" }.srsId, "une table d'attributs n'a pas de repère")
        assertEquals("POINT", t.couches.single { it.nom == "placettes" }.geometrie)
    }

    @Test
    fun `la tournee de Couchey se lit - trois placettes, leur code, leur position en Lambert-93`() {
        val t = LecteurTournee.lire(TourneeDeTest.couchey(dossier))
        assertEquals(listOf("P35-01", "P35-02", "P35-03"), t.placettes.map { it.code })
        assertEquals(listOf(TourneeDeTest.TRAVAUX_ID), t.placettes.map { it.travauxId }.distinct())
        assertEquals(3.99, t.placettes[0].rayonM)
        assertEquals("piquet bois", t.placettes[0].materialisation)
        assertEquals("35", t.placettes[0].ug)
        for ((k, p) in t.placettes.withIndex()) {
            val pos = assertNotNull(p.position)
            assertEquals(TourneeDeTest.centre(k).xMetres, pos.xMetres, 1e-6)
            assertEquals(TourneeDeTest.centre(k).yMetres, pos.yMetres, 1e-6)
        }
        assertEquals(emptyList(), t.controles)
    }

    @Test
    fun `une placette de Couchey tombe en France, dans l emprise de la foret, x = Est et y = Nord`() {
        val t = LecteurTournee.lire(TourneeDeTest.couchey(dossier))
        val emprise = assertNotNull(t.empriseForet)
        for (p in t.placettes) {
            val pos = assertNotNull(p.position)
            val c = EmpriseEtFrance.controler(pos, emprise)
            assertTrue(c.enFrance, "placette ${p.code} hors de France")
            assertEquals(true, c.dansEmprise)
            // Couchey (Côte-d'Or) : Est autour de 850 km, Nord autour de 6 660 km en Lambert-93.
            assertTrue(pos.xMetres in 800_000.0..900_000.0, "x (Est) = ${pos.xMetres}")
            assertTrue(pos.yMetres in 6_600_000.0..6_700_000.0, "y (Nord) = ${pos.yMetres}")
        }
    }

    @Test
    fun `le repere est lu dans le fichier - une tournee en 9794 est lue en 9794`() {
        val t = LecteurTournee.lire(TourneeDeTest.couchey(dossier, epsg = 9794))
        assertEquals(CodeEpsg.RGF93_V2B_LAMBERT93, t.epsg)
        assertEquals(9794, t.srsId)
    }

    @Test
    fun `un fichier qui melange 2154 et 9794 est refuse`() {
        val f = TourneeDeTest.couchey(dossier)
        Sqlite.ouvrir(f).use { cnx ->
            cnx.createStatement().use { st ->
                st.executeUpdate("INSERT INTO gpkg_spatial_ref_sys VALUES ('RGF93 v2b (essai)', 9794, 'EPSG', 9794, 'undefined', NULL)")
                st.executeUpdate("UPDATE gpkg_geometry_columns SET srs_id = 9794 WHERE table_name = 'controles'")
            }
        }
        val r = refus { LecteurTournee.lire(f) }
        assertEquals("referentiels_meles", r.motif)
    }

    @Test
    fun `un code EPSG hors Lambert-93 est refuse`() {
        val f = TourneeDeTest.couchey(dossier)
        Sqlite.ouvrir(f).use { cnx -> cnx.createStatement().use { st -> st.executeUpdate("UPDATE gpkg_geometry_columns SET srs_id = 4326") } }
        assertEquals("referentiel_non_pris_en_charge", refus { LecteurTournee.lire(f) }.motif)
    }

    @Test
    fun `un srs_id absent de gpkg_spatial_ref_sys est refuse`() {
        val f = TourneeDeTest.couchey(dossier)
        Sqlite.ouvrir(f).use { cnx -> cnx.createStatement().use { st -> st.executeUpdate("UPDATE gpkg_geometry_columns SET srs_id = 31370") } }
        assertEquals("referentiel_spatial_inconnu", refus { LecteurTournee.lire(f) }.motif)
    }

    @Test
    fun `un fichier qui n est pas un GeoPackage est refuse`() {
        val f = dossier.resolve("texte.gpkg")
        Files.writeString(f, "ceci n'est pas une base SQLite, loin de là, il faut plus de seize octets")
        assertEquals("pas_un_geopackage", refus { LecteurTournee.lire(f) }.motif)
        val b = dossier.resolve("sqlite-simple.gpkg")
        Sqlite.ouvrir(b).use { it.createStatement().use { st -> st.execute("CREATE TABLE t (a)") } }
        assertEquals("pas_un_geopackage", refus { LecteurTournee.lire(b) }.motif)
    }

    @Test
    fun `une couche de sommieR manquante est refusee, avec son nom`() {
        val f = TourneeDeTest.couchey(dossier)
        Sqlite.ouvrir(f).use { cnx -> cnx.createStatement().use { st -> st.executeUpdate("DELETE FROM gpkg_contents WHERE table_name = 'photos'") } }
        val r = refus { LecteurTournee.lire(f) }
        assertEquals(RefusGpkg.CoucheAbsente("photos"), r)
    }

    @Test
    fun `un champ nouveau dans projet est ignore - un lecteur N lit un fichier N+1`() {
        val f = TourneeDeTest.couchey(dossier)
        val sortie = dossier.resolve("export.gpkg")
        ExportTravaux.exporter(f, sortie, Saisies(operateur = "x"), "0.6.0", TourneeDeTest.EXPORTE_LE)
        Sqlite.ouvrir(sortie).use { cnx ->
            cnx.createStatement().use { st ->
                st.executeUpdate("ALTER TABLE projet ADD COLUMN champ_du_futur TEXT")
                st.executeUpdate("UPDATE projet SET version_surculus = '9.9.9', champ_du_futur = 'nouveau'")
            }
        }
        val t = LecteurTournee.lire(sortie)
        assertEquals("9.9.9", t.projet?.versionSurculus)
    }
}
