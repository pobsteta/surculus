package io.github.pobsteta.surculus.gpkg

import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.placette.EmpriseEtFrance
import io.github.pobsteta.surculus.core.placette.UuidPlacette
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Mission 087 : export d'un contrôle ouvert sans position mesurée (ADR 0003, amendement du 2026-11-02, point 7 ; spec § 5,
 * `positions_controles` ; sources : Darwin Core `coordinateUncertaintyInMeters` (« Leave the value empty […] Zero is not
 * a valid value »), QField `@position_horizontal_accuracy` NULL, GeoPackage 1.3 exigence 152). Écrit par le testeur depuis
 * la spec et l'ADR, sans lire `ExportTravaux.kt`, `Couches.kt` ni `ValidationExport.kt`.
 *
 * Rouge (ne compile pas) jusqu'au `dev`. API supposée, à confirmer par le dev :
 * `PositionMesuree(position: PointLambert93?, ..., rattacheParIdentite, sansPositionMesuree: Boolean = false)`, appelée
 * ici par noms. Colonne `sans_position_mesuree` (BOOLEAN) dans `positions_controles`.
 *
 * Choix de format écrits comme tels (proposés par le `relecteur` de la PR #150, non tranchés par Pascal) : sans position
 * mesurée, `source_gnss`, `referentiel_source` et `simulee` sont NULL.
 *
 * Coordonnées : Lambert-93 (EPSG:2154), x = Est, y = Nord, mètres ; les placettes de [TourneeDeTest] sont à Couchey.
 */
class ExportSansPositionTest {
    @TempDir
    lateinit var dossier: Path

    private val v = TourneeDeTest

    private val sansMesure = PositionMesuree(position = null, rattacheParIdentite = true, sansPositionMesuree = true)

    /** La journée de [TourneeDeTest], où P35-02 (contrôle `uuid(3002)`) est ouvert sans position, à la position de son piquet. */
    private fun saisies(): Saisies {
        val base = v.saisiesDeLaJournee()
        val c2 = base.controles[1].copy(position = v.centre(1), positionMesuree = sansMesure)
        return base.copy(controles = listOf(base.controles[0], c2))
    }

    private fun exporter(s: Saisies = saisies(), source: Path = v.couchey(dossier), nom: String = "export.gpkg"): Path {
        val sortie = dossier.resolve(nom)
        ExportTravaux.exporter(source, sortie, s, v.VERSION, v.EXPORTE_LE)
        return sortie
    }

    private fun ligne(f: Path, controle: String) = Sqlite.ouvrir(f, lectureSeule = true).use {
        it.lignes(
            "SELECT geom IS NULL AS geom_nulle, length(geom) AS taille, precision_m, source_gnss, referentiel_source, simulee, " +
                "rattache_par_identite, sans_position_mesuree FROM positions_controles WHERE controle_uuid = '$controle'",
        )
    }

    @Test
    fun `une ligne de positions_controles par controle, dont celui sans position`() {
        val f = exporter()
        assertEquals(2, Contenu.compter(f, "positions_controles"), "une ligne par contrôle (spec § 5)")
        assertEquals(1, ligne(f, v.uuid(3002)).size)
    }

    @Test
    fun `sans position mesuree - geometrie NULL, pas un POINT EMPTY`() {
        val l = ligne(exporter(), v.uuid(3002)).single()
        assertEquals(1, l.entier("geom_nulle"), "géométrie NULL (GeoPackage 1.3, exigence 152 : un point vide serait un blob NaN)")
        assertNull(l["taille"], "aucun blob, même vide")
    }

    @Test
    fun `sans position mesuree - precision_m NULL, jamais 0 (Darwin Core)`() {
        val l = ligne(exporter(), v.uuid(3002)).single()
        assertNull(l["precision_m"])
    }

    @Test
    fun `sans position mesuree - les deux drapeaux vrais, source, referentiel et simulee NULL`() {
        val l = ligne(exporter(), v.uuid(3002)).single()
        assertEquals(1, l.entier("sans_position_mesuree"))
        assertEquals(1, l.entier("rattache_par_identite"), "définition élargie : le code lu a désigné la placette")
        assertNull(l["source_gnss"])
        assertNull(l["referentiel_source"])
        assertNull(l["simulee"], "choix de format : NULL, pas faux")
    }

    @Test
    fun `avec position mesuree - sans_position_mesuree faux, geometrie presente, inchange`() {
        val l = ligne(exporter(), v.uuid(3001)).single()
        assertEquals(0, l.entier("sans_position_mesuree"), "écrit 0, pas NULL")
        assertEquals(0, l.entier("geom_nulle"))
        assertEquals(0.4, l.reel("precision_m"))
        assertEquals("telephone", l.texte("source_gnss"))
        assertEquals(true, l.booleen("simulee"))
        assertEquals(false, l.booleen("rattache_par_identite"))
    }

    @Test
    fun `controles porte la position du piquet, en France - l'import 0_31_1 lit placette_uuid`() {
        val f = exporter()
        val c = Sqlite.ouvrir(f, lectureSeule = true).use { it.lignes("SELECT placette_uuid, geom FROM controles WHERE uuid = '${v.uuid(3002)}'").single() }
        assertEquals(v.placetteUuid(1), c.texte("placette_uuid"))
        val p = assertNotNull(GeometrieGpkg.point(c.blob("geom")!!))
        assertEquals(v.centre(1).xMetres, p.xMetres, 1e-6, "x (Est) du piquet")
        assertEquals(v.centre(1).yMetres, p.yMetres, 1e-6, "y (Nord) du piquet")
        assertTrue(EmpriseEtFrance.enFrance(p), "la placette tombe en France (pas en Somalie)")
    }

    @Test
    fun `l'index spatial ne porte que les geometries presentes, le fichier est coherent`() {
        val f = exporter()
        Sqlite.ouvrir(f, lectureSeule = true).use { cnx ->
            val pleines = cnx.lignes("SELECT count(*) AS n FROM positions_controles WHERE geom IS NOT NULL").first().entier("n")
            assertEquals(1, pleines)
            assertEquals(pleines, cnx.lignes("SELECT count(*) AS n FROM rtree_positions_controles_geom").first().entier("n"))
            assertEquals(2, cnx.lignes("SELECT feature_count AS n FROM gpkg_ogr_contents WHERE table_name = 'positions_controles'").single().entier("n"))
            assertEquals("ok", cnx.lignes("PRAGMA integrity_check").single().texte("integrity_check"))
        }
    }

    @Test
    fun `l'export se relit, et le rejouer ne double pas la ligne sans position`() {
        val source = v.couchey(dossier)
        val premier = exporter(source = source, nom = "a.gpkg")
        LecteurTournee.lire(premier)
        val sortie = dossier.resolve("b.gpkg")
        val bilan = ExportTravaux.exporter(premier, sortie, saisies(), v.VERSION, v.EXPORTE_LE)
        assertEquals(Contenu.de(premier), Contenu.de(sortie), "une ligne à géométrie NULL est « déjà présente à l'identique »")
        assertEquals(0, bilan.positions)
        assertEquals(2, Contenu.compter(sortie, "positions_controles"))
    }

    @Test
    fun `deux exports du meme etat ont le meme contenu, avec une ligne sans position`() {
        val source = v.couchey(dossier)
        assertEquals(Contenu.de(exporter(source = source, nom = "a.gpkg")), Contenu.de(exporter(source = source, nom = "b.gpkg")))
    }

    @Test
    fun `une ligne qui mentirait est refusee - geometrie absente sans le drapeau, ou drapeau avec une position`() {
        val base = v.saisiesDeLaJournee()
        fun avec(pm: () -> PositionMesuree) = base.copy(controles = listOf(base.controles[0], base.controles[1].copy(positionMesuree = pm())))
        assertFails("position absente sans sans_position_mesuree") {
            exporter(avec { PositionMesuree(position = null, rattacheParIdentite = true, sansPositionMesuree = false) }, nom = "x.gpkg")
        }
        assertFails("sans_position_mesuree avec une position") {
            exporter(avec { PositionMesuree(position = v.centre(1), rattacheParIdentite = true, sansPositionMesuree = true) }, nom = "y.gpkg")
        }
    }

    /** ADR 0003, amendement du 2026-11-02 : « Que GDAL et `sf` lisent une géométrie NULL de GeoPackage […] : non vérifié. » */
    @Test
    fun `GDAL relit positions_controles avec la ligne sans position (si ogrinfo est installe)`() {
        val ogr = ProcessBuilder("ogrinfo", "--version").redirectErrorStream(true).runCatching { start().also { it.waitFor() }.exitValue() }.getOrNull()
        assumeTrue(ogr == 0, "ogrinfo absent : vérification GDAL sautée")
        val f = exporter()
        val p = ProcessBuilder("ogrinfo", "-ro", "-al", f.toString(), "positions_controles").redirectErrorStream(true).start()
        val texte = p.inputStream.bufferedReader().readText()
        assertEquals(0, p.waitFor(), texte)
        assertTrue("ERROR" !in texte, texte)
        assertTrue(Regex("Feature Count: 2").containsMatchIn(texte), texte)
        assertTrue(Regex("sans_position_mesuree \\(.*\\) = 1").containsMatchIn(texte), texte)
    }

    /** Point 9 : l'installation d'une placette exige toujours une position (garde-fou de type : ne compile pas sinon). */
    @Test
    fun `une placette installee porte toujours une position`() {
        val i = PlacetteInstallee(UuidPlacette(v.uuid(9001)), "P35-09", v.TRAVAUX_ID, 3.99, position = v.centre(2))
        val position: PointLambert93 = i.position
        assertTrue(EmpriseEtFrance.enFrance(position))
    }
}
