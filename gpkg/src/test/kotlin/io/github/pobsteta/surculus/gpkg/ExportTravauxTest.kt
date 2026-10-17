package io.github.pobsteta.surculus.gpkg

import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.geo.Projections
import io.github.pobsteta.surculus.core.placette.CodeEssence
import io.github.pobsteta.surculus.core.placette.EtatDsf
import io.github.pobsteta.surculus.core.placette.Essences
import io.github.pobsteta.surculus.core.placette.GroupeCause
import io.github.pobsteta.surculus.core.placette.Observation
import io.github.pobsteta.surculus.core.placette.Observations
import io.github.pobsteta.surculus.core.placette.Totaux
import io.github.pobsteta.surculus.core.placette.UuidControle
import io.github.pobsteta.surculus.core.placette.UuidPlacette
import io.github.pobsteta.surculus.core.reperes.CodeEpsg
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExportTravauxTest {
    @TempDir
    lateinit var dossier: Path

    private val v = TourneeDeTest

    private fun depart(epsg: Int = 2154) = v.couchey(dossier, epsg)

    private fun exporter(source: Path, saisies: Saisies, nom: String = "export.gpkg", exporteLe: Instant = v.EXPORTE_LE): Path {
        val sortie = dossier.resolve(nom)
        ExportTravaux.exporter(source, sortie, saisies, v.VERSION, exporteLe)
        return sortie
    }

    private fun refus(source: Path, saisies: Saisies): RefusGpkg {
        val sortie = dossier.resolve("refuse.gpkg")
        val r = assertFailsWith<GpkgRefuse> { ExportTravaux.exporter(source, sortie, saisies, v.VERSION, v.EXPORTE_LE) }.refus
        assertTrue(Files.notExists(sortie), "un refus ne laisse aucun fichier de destination")
        assertTrue(Files.notExists(dossier.resolve("refuse.gpkg.tmp")), "ni fichier temporaire")
        return r
    }

    // -------------------------------------------------------------- contenu de l'export

    @Test
    fun `l export ajoute les six couches de Surculus et garde les onze de sommieR`() {
        val t = LecteurTournee.lire(exporter(depart(), v.saisiesDeLaJournee()))
        assertEquals(Couches.COUCHES_SOMMIER.toSet() + Couches.NOMS, t.nomsCouches)
        assertEquals(listOf("plants", "positions_controles", "etats_plants", "constats_reprise", "vides", "projet"), t.couchesAjoutees)
    }

    @Test
    fun `gpkg_contents liste toutes les couches ajoutees, avec le bon type et le meme srs_id`() {
        val f = exporter(depart(), v.saisiesDeLaJournee())
        Sqlite.ouvrir(f, lectureSeule = true).use { cnx ->
            val contenus = cnx.lignes("SELECT table_name, data_type, identifier, srs_id FROM gpkg_contents").associateBy { it.texte("table_name") }
            for (c in Couches.TOUTES) {
                val l = contenus[c.nom] ?: error("${c.nom} absente de gpkg_contents")
                assertEquals(if (c.geometrie != null) "features" else "attributes", l.texte("data_type"), c.nom)
                assertEquals(c.nom, l.texte("identifier"))
                assertEquals(if (c.geometrie != null) 2154 else 0, l.entier("srs_id"), c.nom)
            }
            // Toute table utilisateur est enregistrée dans gpkg_contents (OGC 12-128 Req. 118).
            val tables = cnx.lignes("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'gpkg\\_%' ESCAPE '\\' AND name NOT LIKE 'rtree\\_%' ESCAPE '\\' AND name NOT LIKE 'sqlite\\_%' ESCAPE '\\'")
                .map { it.texte("name") }
            assertEquals(emptyList(), tables.filter { it !in contenus }, "tables sans ligne dans gpkg_contents")
            val geom = cnx.lignes("SELECT table_name, column_name, geometry_type_name, srs_id, z, m FROM gpkg_geometry_columns").associateBy { it.texte("table_name") }
            for (c in Couches.TOUTES) {
                if (c.geometrie == null) {
                    assertTrue(c.nom !in geom, "${c.nom} (attributs) n'a pas de géométrie")
                } else {
                    assertEquals(c.geometrie, geom[c.nom]?.texte("geometry_type_name"), c.nom)
                    assertEquals(2154, geom[c.nom]?.entier("srs_id"))
                    assertEquals(0, geom[c.nom]?.entier("z")); assertEquals(0, geom[c.nom]?.entier("m"))
                }
            }
        }
    }

    @Test
    fun `chaque table a une cle primaire entiere alias de rowid (Req 119) et gpkg_contents sait compter`() {
        val f = exporter(depart(), v.saisiesDeLaJournee())
        Sqlite.ouvrir(f, lectureSeule = true).use { cnx ->
            for (c in Couches.TOUTES) {
                val pk = cnx.lignes("PRAGMA table_info(${c.nom.ident()})").filter { it.entier("pk") == 1 }
                assertEquals(listOf("fid" to "INTEGER"), pk.map { it.texte("name") to it.texte("type") }, c.nom)
            }
            for (c in Couches.TOUTES) {
                val n = cnx.lignes("SELECT feature_count AS n FROM gpkg_ogr_contents WHERE table_name = '${c.nom}'").single().entier("n")
                assertEquals(cnx.lignes("SELECT count(*) AS n FROM ${c.nom.ident()}").first().entier("n"), n, "gpkg_ogr_contents.${c.nom}")
            }
        }
    }

    @Test
    fun `le schema des couches de sommieR est inchange - champs, types, cles, geometrie, srs_id`() {
        val modele = v.modeleVide(dossier)
        val f = exporter(depart(), v.saisiesDeLaJournee())
        fun schema(chemin: Path, couche: String) = Sqlite.ouvrir(chemin, lectureSeule = true).use { cnx ->
            listOf(
                cnx.lignes("PRAGMA table_info(${couche.ident()})").map { listOf(it["cid"], it["name"], it["type"], it["notnull"], it["dflt_value"], it["pk"]) },
                cnx.lignes("SELECT geometry_type_name, srs_id, z, m, column_name FROM gpkg_geometry_columns WHERE table_name = '$couche'").map { it.toSortedMap().toString() },
                cnx.lignes("SELECT data_type, srs_id FROM gpkg_contents WHERE table_name = '$couche'").map { it.toSortedMap().toString() },
                cnx.lignes("SELECT sql FROM sqlite_master WHERE tbl_name = '$couche' ORDER BY name").map { it["sql"] },
            )
        }
        for (couche in Couches.COUCHES_SOMMIER) {
            assertEquals(schema(modele, couche), schema(f, couche), "schéma de $couche")
        }
    }

    @Test
    fun `la source n est jamais modifiee`() {
        val source = depart()
        val avant = Files.readAllBytes(source)
        exporter(source, v.saisiesDeLaJournee())
        assertTrue(avant.contentEquals(Files.readAllBytes(source)))
    }

    @Test
    fun `les controles sont ecrits dans la couche controles de sommieR avec leurs totaux`() {
        val f = exporter(depart(), v.saisiesDeLaJournee())
        val lignes = Sqlite.ouvrir(f, lectureSeule = true).use { it.lignes("SELECT * FROM controles ORDER BY fid") }
        assertEquals(2, lignes.size)
        val c1 = lignes[0]
        assertEquals(v.uuid(3001), c1.texte("uuid"))
        assertEquals(v.placetteUuid(0), c1.texte("placette_uuid"))
        // 4 plants (sain, atteint abrouti, mort, absent) : total 4, vivants 2, abroutis 1, hauteur moyenne (35+40)/2 = 37,5 -> 38.
        assertEquals(listOf(4, 2, 1, 38), listOf("nb_total", "nb_vivants", "nb_abroutis", "h_moy_cm").map { c1.entier(it) })
        assertEquals("2026-10-20T09:30:00.000Z", c1.texte("visite_le"))
        assertEquals("Agent d'essai", c1.texte("operateur"))
        assertEquals("moyenne", c1.texte("concurrence"))
        // Deuxième contrôle : un sain (50 cm) et un non observé, hors totaux.
        assertEquals(listOf(1, 1, 0, 50), listOf("nb_total", "nb_vivants", "nb_abroutis", "h_moy_cm").map { lignes[1].entier(it) })
        assertNull(lignes[1].texte("besoin"))
        // La position du contrôle est un point en Lambert-93 en France.
        val pos = GeometrieGpkg.point(c1.blob("geom")!!)!!
        assertEquals(v.centre(0).xMetres, pos.xMetres, 1e-6)
        assertEquals(v.centre(0).yMetres, pos.yMetres, 1e-6)
    }

    @Test
    fun `les plants gardent leur uuid, leur numero, leur observation brute et leur position derivee`() {
        val saisies = v.saisiesDeLaJournee()
        val f = exporter(depart(), saisies)
        val t = LecteurTournee.lire(f)
        assertEquals(6, t.plants.size)
        val p1 = saisies.plants.first { it.plant.uuid.valeur == v.uuid(1001) }
        val lu = t.plants.single { it.uuid == v.uuid(1001) }
        assertEquals(v.placetteUuid(0), lu.placetteUuid)
        assertEquals(1, lu.numero)
        assertEquals("CHS", lu.essence)
        assertEquals(false, lu.regarni)
        assertEquals(p1.plant.derive.position.xMetres, lu.position!!.xMetres, 1e-9)
        assertEquals(p1.plant.derive.position.yMetres, lu.position!!.yMetres, 1e-9)
        val ligne = Sqlite.ouvrir(f, lectureSeule = true).use { it.lignes("SELECT * FROM plants WHERE uuid = '${v.uuid(1001)}'").single() }
        assertEquals(1.2, ligne.reel("distance_m"))
        assertEquals(10.0, ligne.reel("azimut_mag"))
        assertEquals("deg", ligne.texte("unite_azimut"))
        assertEquals("WMM2025", ligne.texte("modele_geomagnetique"))
        assertEquals(p1.plant.derive.orientation.declinaisonDeg, ligne.reel("declinaison_deg"))
        assertEquals(p1.plant.derive.orientation.convergenceDeg, ligne.reel("convergence_deg"))
        assertEquals(p1.plant.derive.orientation.azimutGeographiqueDeg, ligne.reel("azimut_geo_deg"))
        assertEquals("2026-10-20T08:15:00.000Z", ligne.texte("pointe_le"))
        // Les numéros sont par placette : P35-02 a ses plants 1 et 2.
        assertEquals(listOf(1, 2), t.plants.filter { it.placetteUuid == v.placetteUuid(1) }.map { it.numero })
    }

    @Test
    fun `un regarni garde le lien vers le plant qu il remplace`() {
        val base = v.saisiesDeLaJournee()
        val mort = base.plants[2].plant // mort au contrôle
        val regarni = v.plant(50, 0, 2.6, 205.0, essence = "CHS", remplace = mort.uuid)
        val f = exporter(depart(), base.copy(plants = base.plants + PlantNumerote(regarni, 5)))
        val lu = LecteurTournee.lire(f).plants.single { it.uuid == regarni.uuid.valeur }
        assertTrue(lu.regarni)
        assertEquals(mort.uuid.valeur, lu.remplaceUuid)
        assertEquals(5, lu.numero)
    }

    @Test
    fun `les etats lus ne perdent ni cause ni detail ni hauteur - dont le non observe`() {
        val f = exporter(depart(), v.saisiesDeLaJournee())
        val etats = LecteurTournee.lire(f).etats.associateBy { it.uuid }
        assertEquals(6, etats.size)
        assertEquals(EtatLu(v.uuid(2002), v.uuid(3001), v.uuid(1002), "atteint", null, "abroutissement", 40), etats[v.uuid(2002)])
        assertEquals(EtatLu(v.uuid(2003), v.uuid(3001), v.uuid(1003), "mort", "animaux", "cervides", null), etats[v.uuid(2003)])
        assertEquals(EtatLu(v.uuid(2004), v.uuid(3001), v.uuid(1004), "absent", null, null, null), etats[v.uuid(2004)])
        assertEquals(Observations.CODE_NON_OBSERVE, etats[v.uuid(2006)]!!.etat)
    }

    @Test
    fun `la position GNSS mesuree va dans positions_controles, le drapeau d identite est ecrit`() {
        val f = exporter(depart(), v.saisiesDeLaJournee())
        val l = Sqlite.ouvrir(f, lectureSeule = true).use { it.lignes("SELECT * FROM positions_controles").single() }
        assertEquals(v.uuid(3001), l.texte("controle_uuid"))
        assertEquals(0.4, l.reel("precision_m"))
        assertEquals("essai", l.texte("source_gnss"))
        assertEquals("RGF93", l.texte("referentiel_source"))
        assertEquals(true, l.booleen("simulee"))
        assertEquals(false, l.booleen("rattache_par_identite"))
    }

    // -------------------------------------------------------------- extensions du standard

    @Test
    fun `l extension Schema declare les valeurs permises, tirees des enums de core`() {
        val f = exporter(depart(), v.saisiesDeLaJournee())
        Sqlite.ouvrir(f, lectureSeule = true).use { cnx ->
            fun valeurs(nom: String) = cnx.lignes("SELECT value FROM gpkg_data_column_constraints WHERE constraint_name = '$nom' AND constraint_type = 'enum'").map { it.texte("value")!! }.toSet()
            assertEquals(setOf("sain", "atteint", "mort", "absent", "non_observe"), valeurs("etat_plant"))
            assertEquals(GroupeCause.entries.map { it.code }.toSet(), valeurs("cause_groupe"))
            assertEquals(setOf("deg", "gr"), valeurs("unite_azimut"))
            assertEquals(setOf("plein", "cemagref", "grappes"), valeurs("methode_constat"))
            assertEquals(setOf("trace", "propose"), valeurs("origine_vide"))
            // Chaque colonne de chaque couche ajoutée est décrite ; celles qui ont des valeurs permises pointent leur contrainte.
            for (c in Couches.TOUTES) {
                val decrites = cnx.lignes("SELECT column_name, constraint_name FROM gpkg_data_columns WHERE table_name = '${c.nom}'").associate { it.texte("column_name") to it.texte("constraint_name") }
                assertEquals(c.colonnes.map { it.nom }.toSet(), decrites.keys, c.nom)
                for (col in c.colonnes) assertEquals(col.contrainte?.nom, decrites[col.nom], "${c.nom}.${col.nom}")
            }
            // Les valeurs écrites respectent les contraintes que le fichier déclare.
            val permis = valeurs("etat_plant")
            cnx.lignes("SELECT DISTINCT etat FROM etats_plants").forEach { assertTrue(it.texte("etat") in permis) }
            // Les deux tables de l'extension sont déclarées dans gpkg_extensions.
            val ext = cnx.lignes("SELECT table_name, extension_name, scope FROM gpkg_extensions WHERE extension_name = 'gpkg_schema'").map { it.texte("table_name") to it.texte("scope") }
            assertEquals(setOf("gpkg_data_columns" to "read-write", "gpkg_data_column_constraints" to "read-write"), ext.toSet())
        }
    }

    @Test
    fun `l index rtree des couches ajoutees est complet et declare, le fichier est coherent`() {
        val saisies = v.saisiesDeLaJournee()
        val f = exporter(depart(), saisies.copy(constats = listOf(constat(1)), vides = listOf(vide(1, constatUuid = v.uuid(5001)))))
        Sqlite.ouvrir(f, lectureSeule = true).use { cnx ->
            for (c in Couches.TOUTES.filter { it.geometrie != null }) {
                val n = cnx.lignes("SELECT count(*) AS n FROM ${c.nom.ident()}").first().entier("n")
                assertTrue(n!! > 0, "couche ${c.nom} vide : le test ne prouve rien")
                assertEquals(n, cnx.lignes("SELECT count(*) AS n FROM ${"rtree_${c.nom}_geom".ident()}").first().entier("n"), "rtree ${c.nom}")
                val ext = cnx.lignes("SELECT scope FROM gpkg_extensions WHERE table_name = '${c.nom}' AND extension_name = 'gpkg_rtree_index'").map { it.texte("scope") }
                assertEquals(listOf("write-only"), ext, "extension rtree de ${c.nom}")
            }
            assertEquals("ok", cnx.lignes("PRAGMA integrity_check").single().texte("integrity_check"))
            assertEquals(emptyList(), cnx.lignes("PRAGMA foreign_key_check"))
            // L'étendue de gpkg_contents contient les points.
            val e = cnx.lignes("SELECT min_x, max_x, min_y, max_y FROM gpkg_contents WHERE table_name = 'plants'").single()
            assertTrue(e.reel("min_x")!! < v.centre(0).xMetres && v.centre(0).xMetres < e.reel("max_x")!! + 5)
        }
    }

    // -------------------------------------------------------------- déterminisme, rejeu

    @Test
    fun `deux exports du meme etat ont le meme contenu`() {
        val source = depart()
        val a = exporter(source, v.saisiesDeLaJournee(), "a.gpkg")
        val b = exporter(source, v.saisiesDeLaJournee(), "b.gpkg")
        assertEquals(Contenu.de(a), Contenu.de(b))
    }

    @Test
    fun `l ordre de saisie ne change pas le contenu de l export`() {
        val source = depart()
        val s = v.saisiesDeLaJournee()
        val renverse = s.copy(
            controles = s.controles.reversed().map { it.copy(etats = it.etats.reversed()) },
            plants = s.plants.reversed(),
        )
        assertEquals(Contenu.de(exporter(source, s, "a.gpkg")), Contenu.de(exporter(source, renverse, "b.gpkg")))
    }

    @Test
    fun `seule l horloge injectee change le contenu`() {
        val source = depart()
        val a = Contenu.de(exporter(source, v.saisiesDeLaJournee(), "a.gpkg"))
        val b = Contenu.de(exporter(source, v.saisiesDeLaJournee(), "b.gpkg", exporteLe = v.EXPORTE_LE.plusSeconds(60)))
        assertNotEquals(a["gpkg_contents"], b["gpkg_contents"])
        assertEquals(a - "gpkg_contents", b - "gpkg_contents")
    }

    @Test
    fun `rejouer l export sur l export ne change rien et compte les lignes deja presentes`() {
        val source = depart()
        val s = v.saisiesDeLaJournee()
        val premier = exporter(source, s, "a.gpkg")
        val sortie = dossier.resolve("b.gpkg")
        val bilan = ExportTravaux.exporter(premier, sortie, s, v.VERSION, v.EXPORTE_LE)
        assertEquals(Contenu.de(premier), Contenu.de(sortie))
        assertEquals(0, bilan.controles + bilan.plants + bilan.etats + bilan.positions)
        assertEquals(2 + 6 + 6 + 1, bilan.dejaPresentes)
    }

    @Test
    fun `le bilan compte ce qui est ecrit`() {
        val sortie = dossier.resolve("e.gpkg")
        val bilan = ExportTravaux.exporter(depart(), sortie, v.saisiesDeLaJournee(), v.VERSION, v.EXPORTE_LE)
        assertEquals(BilanExport(v.EXPORTE_LE, 0, 2, 6, 6, 1, 0, 0, 0), bilan)
    }

    @Test
    fun `un meme uuid avec un contenu different est refuse - jamais ecrase en silence`() {
        val s = v.saisiesDeLaJournee()
        val premier = exporter(depart(), s, "a.gpkg")
        val autre = s.copy(controles = listOf(s.controles[0].copy(concurrence = "forte"), s.controles[1]))
        val r = assertFailsWith<GpkgRefuse> { ExportTravaux.exporter(premier, dossier.resolve("b.gpkg"), autre, v.VERSION, v.EXPORTE_LE) }.refus
        assertEquals(RefusGpkg.UuidEnDouble("controles", v.uuid(3001)), r)
    }

    // -------------------------------------------------------------- version et projet

    @Test
    fun `la version est unique, au niveau du fichier, dans projet - les champs de sommieR restent vides`() {
        val f = exporter(depart(), v.saisiesDeLaJournee())
        val projet = LecteurTournee.lire(f).projet!!
        assertEquals(ProjetLu(null, null, "Agent d'essai", null, null, "0.6.0"), projet)
        assertEquals(1, Contenu.compter(f, "projet"))
        // Un second export du fichier rendu remet à jour la version, sans ajouter de ligne.
        val sortie = dossier.resolve("b.gpkg")
        ExportTravaux.exporter(f, sortie, v.saisiesDeLaJournee(), "0.7.0", v.EXPORTE_LE)
        assertEquals("0.7.0", LecteurTournee.lire(sortie).projet!!.versionSurculus)
        assertEquals(1, Contenu.compter(sortie, "projet"))
    }

    @Test
    fun `les champs de sommieR dans projet ne sont jamais ecrases`() {
        val f = exporter(depart(), v.saisiesDeLaJournee())
        Sqlite.ouvrir(f).use { it.createStatement().use { st -> st.executeUpdate("UPDATE projet SET nom = 'Couchey', foret = 'F1', prepare_le = '2026-10-01', version_sommier = '0.31.1'") } }
        val sortie = dossier.resolve("b.gpkg")
        ExportTravaux.exporter(f, sortie, v.saisiesDeLaJournee(), "0.7.0", v.EXPORTE_LE)
        assertEquals(ProjetLu("Couchey", "F1", "Agent d'essai", "2026-10-01", "0.31.1", "0.7.0"), LecteurTournee.lire(sortie).projet)
    }

    // -------------------------------------------------------------- repère

    @Test
    fun `un plant en 9794 dans une tournee en 2154 est refuse - les deux ne se melent pas`() {
        val s = v.saisiesDeLaJournee(epsg = CodeEpsg.RGF93_V2B_LAMBERT93)
        val r = refus(depart(), s)
        assertEquals("referentiels_meles", r.motif)
        assertTrue(r is RefusGpkg.ReferentielDuPlantDifferent)
    }

    @Test
    fun `une tournee en 9794 s exporte en 9794 - le srs_id est lu dans le fichier`() {
        val f = exporter(depart(epsg = 9794), v.saisiesDeLaJournee(epsg = CodeEpsg.RGF93_V2B_LAMBERT93))
        val t = LecteurTournee.lire(f)
        assertEquals(CodeEpsg.RGF93_V2B_LAMBERT93, t.epsg)
        Sqlite.ouvrir(f, lectureSeule = true).use { cnx ->
            assertEquals(setOf(9794), cnx.lignes("SELECT srs_id FROM gpkg_geometry_columns").map { it.entier("srs_id") }.toSet())
            assertEquals(9794, GeometrieGpkg.decoder(cnx.lignes("SELECT geom FROM plants LIMIT 1").single().blob("geom")!!).srsId)
        }
    }

    @Test
    fun `une tournee qui melange les deux reperes est refusee avant toute ecriture`() {
        val source = depart()
        Sqlite.ouvrir(source).use { cnx ->
            cnx.createStatement().use { st ->
                st.executeUpdate("INSERT INTO gpkg_spatial_ref_sys VALUES ('essai', 9794, 'EPSG', 9794, 'undefined', NULL)")
                st.executeUpdate("UPDATE gpkg_geometry_columns SET srs_id = 9794 WHERE table_name = 'placettes'")
            }
        }
        assertEquals("referentiels_meles", refus(source, v.saisiesDeLaJournee()).motif)
    }

    // -------------------------------------------------------------- France, forêt

    private fun controleA(position: PointLambert93) = v.saisiesDeLaJournee().let { s ->
        s.copy(controles = listOf(s.controles[0].copy(position = position, positionMesuree = null)), plants = s.plants.take(4))
    }

    @Test
    fun `un point en Somalie est refuse - hors de France`() {
        val (x, y) = Projections.versLambert93(lon = 45.34, lat = 2.04) // Mogadiscio
        val r = refus(depart(), controleA(PointLambert93(x, y)))
        assertTrue(r is RefusGpkg.PointHorsFrance, r.toString())
        assertEquals("point_hors_france", r.motif)
    }

    @Test
    fun `des axes inverses (y, x) sont refuses et signales`() {
        val c = v.centre(0)
        val r = refus(depart(), controleA(PointLambert93(c.yMetres, c.xMetres)))
        assertTrue(r is RefusGpkg.PointHorsFrance && r.axesInverses, r.toString())
    }

    @Test
    fun `un point en France mais hors de l emprise de la foret est refuse`() {
        val r = refus(depart(), controleA(PointLambert93(652_000.0, 6_862_000.0))) // Paris
        assertTrue(r is RefusGpkg.PointHorsForet, r.toString())
    }

    @Test
    fun `un plant pointe hors de France est refuse`() {
        val s = v.saisiesDeLaJournee()
        val mauvais = s.plants[0].plant.let { p -> p.copy(derive = p.derive.copy(position = PointLambert93(1.0, 2.0))) }
        val r = refus(depart(), s.copy(plants = listOf(PlantNumerote(mauvais, 1)) + s.plants.drop(1)))
        assertEquals("point_hors_france", r.motif)
    }

    // -------------------------------------------------------------- règles de l'import

    @Test
    fun `des totaux qui ne concordent pas avec les plants sont refuses`() {
        val s = v.saisiesDeLaJournee()
        val r = refus(depart(), s.copy(controles = listOf(s.controles[0].copy(totaux = Totaux(4, 3, 1, 38)), s.controles[1])))
        assertEquals("totaux_discordants", r.motif)
    }

    private fun sansEtats(n: Int, placette: Int, quand: Instant) = ControleSaisi(
        UuidControle(v.uuid(4000 + n)), UuidPlacette(v.placetteUuid(placette)), quand, v.centre(placette), totaux = Totaux(4, 3, 1, 40),
    )

    @Test
    fun `deux controles de la meme placette le meme jour civil sont refuses`() {
        val a = sansEtats(1, 0, Instant.parse("2026-10-20T08:00:00Z"))
        val b = sansEtats(2, 0, Instant.parse("2026-10-20T14:00:00Z"))
        assertEquals("deux_controles_meme_jour", refus(depart(), Saisies("x", controles = listOf(a, b))).motif)
        // Deux placettes différentes le même jour : accepté.
        exporter(depart(), Saisies("x", controles = listOf(a, sansEtats(3, 1, Instant.parse("2026-10-20T08:00:00Z")))), "ok.gpkg")
    }

    @Test
    fun `jour de Paris et jour UTC - on refuse ce que l import de sommieR refuserait, dans les deux cas`() {
        // Même jour UTC, jours de Paris différents (22:30Z = 00:30 le 21 à Paris) : sommieR refuserait la tournée entière.
        val utc = refus(depart(), Saisies("x", controles = listOf(sansEtats(1, 0, Instant.parse("2026-10-20T18:00:00Z")), sansEtats(2, 0, Instant.parse("2026-10-20T22:30:00Z")))))
        assertEquals(RefusGpkg.DeuxControlesMemeJour(v.placetteUuid(0), java.time.LocalDate.of(2026, 10, 20), "UTC"), utc)
        // Même jour de Paris, jours UTC différents : c'est la règle de Surculus (un contrôle par placette et par jour civil).
        val paris = refus(depart(), Saisies("x", controles = listOf(sansEtats(1, 0, Instant.parse("2026-10-20T22:30:00Z")), sansEtats(2, 0, Instant.parse("2026-10-21T00:30:00Z")))))
        assertEquals(RefusGpkg.DeuxControlesMemeJour(v.placetteUuid(0), java.time.LocalDate.of(2026, 10, 21), "Europe/Paris"), paris)
    }

    @Test
    fun `un controle le meme jour qu un controle deja dans le fichier est refuse`() {
        val premier = exporter(depart(), Saisies("x", controles = listOf(sansEtats(1, 0, Instant.parse("2026-10-20T08:00:00Z")))))
        val r = assertFailsWith<GpkgRefuse> {
            ExportTravaux.exporter(premier, dossier.resolve("b.gpkg"), Saisies("x", controles = listOf(sansEtats(2, 0, Instant.parse("2026-10-20T15:00:00Z")))), v.VERSION, v.EXPORTE_LE)
        }
        assertEquals("deux_controles_meme_jour", r.refus.motif)
    }

    @Test
    fun `un identifiant en majuscules est refuse - l import de sommieR ne lit que des minuscules`() {
        val c = sansEtats(1, 0, Instant.parse("2026-10-20T08:00:00Z")).copy(uuid = UuidControle("AAAAAAAA-0000-4000-8000-000000000001"))
        assertEquals("champ_invalide", refus(depart(), Saisies("x", controles = listOf(c))).motif)
    }

    @Test
    fun `un controle d une placette inconnue est refuse`() {
        val c = sansEtats(1, 0, Instant.parse("2026-10-20T08:00:00Z")).copy(placette = UuidPlacette(v.uuid(999)))
        assertEquals("placette_inconnue", refus(depart(), Saisies("x", controles = listOf(c))).motif)
    }

    @Test
    fun `un numero de plant deja pris dans la placette est refuse`() {
        val s = v.saisiesDeLaJournee()
        val doublon = PlantNumerote(v.plant(60, 0, 2.0, 50.0), 1)
        assertEquals("numero_en_double", refus(depart(), s.copy(plants = s.plants + doublon)).motif)
    }

    @Test
    fun `une essence absente de la liste connue est refusee`() {
        val s = v.saisiesDeLaJournee()
        val connues = Essences(setOf("CHS", "CHP"))
        val inconnu = PlantNumerote(v.plant(61, 2, 1.0, 20.0, essence = "XYZ"), 1)
        val r = assertFailsWith<GpkgRefuse> {
            ExportTravaux.exporter(depart(), dossier.resolve("r.gpkg"), s.copy(plants = s.plants + inconnu), v.VERSION, v.EXPORTE_LE, connues)
        }.refus
        assertEquals(RefusGpkg.EssenceInconnue(v.uuid(1061), "XYZ"), r)
        assertEquals(CodeEssence("CHS").code, "CHS")
    }

    @Test
    fun `un etat d un plant d une autre placette ou inconnu est refuse`() {
        val s = v.saisiesDeLaJournee()
        val c = s.controles[0]
        val autre = c.etats[0].copy(plant = s.plants[4].plant.uuid) // plant de P35-02 dans le contrôle de P35-01
        assertEquals("champ_invalide", refus(depart(), s.copy(controles = listOf(c.copy(etats = listOf(autre) + c.etats.drop(1), totaux = c.totaux), s.controles[1]))).motif)
        val inconnu = c.etats[0].copy(plant = io.github.pobsteta.surculus.core.placette.UuidPlant(v.uuid(9999)))
        assertEquals("etat_sur_objet_inconnu", refus(depart(), s.copy(controles = listOf(c.copy(etats = listOf(inconnu) + c.etats.drop(1)), s.controles[1]))).motif)
    }

    // -------------------------------------------------------------- placettes installées, constats, vides

    private fun carreAutour(c: PointLambert93, demi: Double = 5.0) = Geometrie.Polygone(
        listOf(
            listOf(
                PointLambert93(c.xMetres - demi, c.yMetres - demi), PointLambert93(c.xMetres + demi, c.yMetres - demi),
                PointLambert93(c.xMetres + demi, c.yMetres + demi), PointLambert93(c.xMetres - demi, c.yMetres + demi),
                PointLambert93(c.xMetres - demi, c.yMetres - demi),
            ),
        ),
    )

    private fun constat(n: Int) = ConstatReprise(
        v.uuid(5000 + n), carreAutour(v.centre(0), 20.0), MethodeConstat.CEMAGREF, pasLignes = 2.5,
        departsJson = "[[3,1],[7,2]]", essence = "CHS", nbPlantsChantier = 1050, nbSains = 200, nbAtteints = 40, nbMorts = 30, nbAbsents = 30,
        tauxBrutPct = 90.0, icBasPct = 87.0, icHautPct = 93.0, seuilPct = 80.0, seuilSource = "fascicule 34", alerte = false,
        visiteLe = v.INSTANT_CONTROLE, operateur = "Agent d'essai",
    )

    private fun vide(n: Int, constatUuid: String? = null, placetteUuid: String? = null) = Vide(
        v.uuid(6000 + n), carreAutour(v.centre(1), 2.0), constatUuid = constatUuid, placetteUuid = placetteUuid,
        surfaceAre = 0.16, origine = OrigineVide.TRACE, visiteLe = v.INSTANT_CONTROLE, operateur = "Agent d'essai",
    )

    @Test
    fun `constats et vides sont ecrits comme polygones, departs en tableau JSON`() {
        val f = exporter(depart(), Saisies("x", constats = listOf(constat(1)), vides = listOf(vide(1, constatUuid = v.uuid(5001)), vide(2, placetteUuid = v.placetteUuid(1)))))
        Sqlite.ouvrir(f, lectureSeule = true).use { cnx ->
            val k = cnx.lignes("SELECT * FROM constats_reprise").single()
            assertEquals("cemagref", k.texte("methode"))
            assertEquals("[[3,1],[7,2]]", k.texte("departs"))
            assertEquals(1050, k.entier("nb_plants_chantier"))
            assertEquals(false, k.booleen("alerte"))
            val env = GeometrieGpkg.decoder(k.blob("geom")!!)
            assertEquals(2154, env.srsId)
            assertEquals(v.centre(0).xMetres - 20.0, env.enveloppe!!.xMin, 1e-9)
            assertEquals(2, cnx.lignes("SELECT * FROM vides").size)
            assertEquals("trace", cnx.lignes("SELECT origine FROM vides LIMIT 1").single().texte("origine"))
        }
    }

    @Test
    fun `departs qui n est pas un tableau JSON est refuse, et un vide sans rattachement aussi`() {
        assertEquals("champ_invalide", refus(depart(), Saisies("x", constats = listOf(constat(1).copy(departsJson = "{\"a\": 1}")))).motif)
        assertEquals("champ_invalide", refus(depart(), Saisies("x", vides = listOf(vide(1)))).motif)
        assertEquals("champ_invalide", refus(depart(), Saisies("x", vides = listOf(vide(1, constatUuid = v.uuid(5001), placetteUuid = v.placetteUuid(0))))).motif)
    }

    @Test
    fun `une placette installee s ajoute a la couche placettes, lisible par le lecteur`() {
        val neuve = PlacetteInstallee(
            UuidPlacette(v.uuid(7001)), "P35-04", v.TRAVAUX_ID, 3.99, "piquet bois", "35",
            PointLambert93(v.centre(0).xMetres + 20, v.centre(0).yMetres + 5),
        )
        val f = exporter(depart(), Saisies("x", placettesInstallees = listOf(neuve)))
        val t = LecteurTournee.lire(f)
        assertEquals(listOf("P35-01", "P35-02", "P35-03", "P35-04"), t.placettes.map { it.code })
        assertEquals(v.TRAVAUX_ID, t.placettes.last().travauxId)
        Sqlite.ouvrir(f, lectureSeule = true).use { cnx ->
            assertEquals(4, cnx.lignes("SELECT count(*) AS n FROM rtree_placettes_geom").first().entier("n"))
            // Le modèle de sommieR laisse feature_count à NULL (« inconnu » pour GDAL) : on n'invente pas de compte.
            val compte = cnx.lignes("SELECT feature_count AS n FROM gpkg_ogr_contents WHERE table_name = 'placettes'").single().entier("n")
            assertTrue(compte == null || compte == 4, "feature_count = $compte")
        }
        assertEquals("code_placette_en_double", refus(depart(), Saisies("x", placettesInstallees = listOf(neuve.copy(code = "P35-01")))).motif)
    }

    // -------------------------------------------------------------- GDAL relit l'export

    @Test
    fun `GDAL relit l export - les couches y sont, avec leurs lignes (si ogrinfo est installe)`() {
        val ogr = ProcessBuilder("ogrinfo", "--version").redirectErrorStream(true).runCatching { start().also { it.waitFor() }.exitValue() }.getOrNull()
        assumeTrue(ogr == 0, "ogrinfo absent : vérification GDAL sautée")
        val f = exporter(depart(), v.saisiesDeLaJournee().let { it.copy(constats = listOf(constat(1))) })
        val p = ProcessBuilder("ogrinfo", "-ro", "-al", "-so", f.toString()).redirectErrorStream(true).start()
        val texte = p.inputStream.bufferedReader().readText()
        assertEquals(0, p.waitFor(), texte)
        assertTrue("ERROR" !in texte, texte)
        for (couche in Couches.NOMS + Couches.COUCHES_SOMMIER) assertTrue("Layer name: $couche" in texte, "couche $couche absente pour GDAL")
        assertTrue(Regex("Layer name: plants[\\s\\S]*?Feature Count: 6").containsMatchIn(texte), texte)
        assertTrue("EPSG\",\"2154" in texte || "ID[\"EPSG\",2154]" in texte, "repère EPSG:2154 absent")
    }
}
