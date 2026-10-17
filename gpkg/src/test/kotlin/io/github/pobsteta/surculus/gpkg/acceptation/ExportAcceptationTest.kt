package io.github.pobsteta.surculus.gpkg.acceptation

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.time.Instant
import java.util.zip.ZipFile

private val DOSSIER: Path = run {
    var d: Path? = Path.of(System.getProperty("user.dir")).toAbsolutePath()
    while (d != null && !Files.isDirectory(d.resolve("fixtures/acceptation-export"))) d = d.parent
    requireNotNull(d) { "fixtures/acceptation-export introuvable depuis ${System.getProperty("user.dir")}" }
    d
}
private val FIXTURES = DOSSIER.resolve("fixtures/acceptation-export")
private val MOTIFS = setOf("melange_reperes", "controle_en_double", "hors_france", "hors_referentiel", "uuid_en_double")

private class CasExport(val fichier: String, val json: JsonObject) {
    val id = json.get("id").asString
    val entree = json.getAsJsonObject("entree")
    val attendu = json.getAsJsonObject("attendu")
}

private fun lire(nom: String): JsonObject = JsonParser.parseString(Files.readString(FIXTURES.resolve(nom))).asJsonObject

private fun cas(): List<CasExport> = Files.list(FIXTURES).use { s -> s.map { it.fileName.toString() }.toList() }
    .filter { it.endsWith(".json") && lire(it).has("cas") }.sorted()
    .flatMap { f -> lire(f).getAsJsonArray("cas").map { CasExport(f, it.asJsonObject) } }

/** Cas bien formés : actif dès maintenant, il ne dépend d'aucun export. */
class FixturesExportBienFormeesTest {
    private val tous = cas()

    @Test
    fun `les identifiants sont uniques et les cas sourcés`() {
        assertTrue(tous.isNotEmpty())
        assertEquals(tous.size, tous.map { it.id }.toSet().size, "ids en double")
        tous.forEach { assertTrue(it.json.get("source")?.asString?.isNotBlank() == true, "${it.id} : source absente") }
        tous.forEach { assertTrue(it.json.get("description")?.asString?.isNotBlank() == true, "${it.id} : description absente") }
    }

    @Test
    fun `un cas refuse porte un motif connu`() {
        tous.filter { it.attendu.get("accepte")?.asBoolean == false }.forEach {
            assertTrue(it.attendu.get("motif").asString in MOTIFS, "${it.id} : motif inconnu")
        }
    }
}

/** Cas d'acceptation de l'export (mission 020). Branchés par la mission 006 : voir [BranchementExport]. */
@Disabled("Mission 020 : actif quand la mission 006 branche l'export dans BranchementExport.kt")
class ExportAcceptationTest {

    private val export: ExportTournee by lazy { exportATester() }

    @TestFactory
    fun `cas d acceptation de l export`(): List<DynamicTest> = cas().map { c ->
        dynamicTest("${c.id} : ${c.json.get("description").asString.take(80)}") { jouer(c) }
    }

    private fun jouer(c: CasExport) {
        if (c.entree.get("operation")?.asString == "lecture") return jouerLecture(c)
        val res = export.exporter(c.entree, Files.createTempDirectory("export-${c.id}"))
        if (c.attendu.get("accepte").asBoolean) {
            val accepte = res as? ResultatExport.Accepte ?: error("${c.id} : refusé (${res}) alors qu'il devait être accepté")
            Fichier(accepte.zip).use { verifier(c, it) }
        } else {
            val refus = res as? ResultatExport.Refuse ?: error("${c.id} : accepté alors qu'il devait être refusé")
            assertEquals(c.attendu.get("motif").asString, refus.motif, c.id)
        }
    }

    /** Relecture : un fichier N+1 (colonne et table en plus) se lit en ignorant le nouveau. */
    private fun jouerLecture(c: CasExport) {
        val res = export.exporter(c.entree, Files.createTempDirectory("export-${c.id}")) as? ResultatExport.Accepte
            ?: error("${c.id} : l'export de départ est refusé")
        val dir = Files.createTempDirectory("lecture-${c.id}")
        val gpkg = dir.resolve("travaux.gpkg")
        ZipFile(res.zip.toFile()).use { z -> z.getInputStream(z.getEntry("travaux.gpkg")).use { Files.copy(it, gpkg) } }
        DriverManager.getConnection("jdbc:sqlite:$gpkg").use { cnx ->
            cnx.createStatement().use { st ->
                st.executeUpdate("ALTER TABLE projet ADD COLUMN nouveau TEXT")
                st.executeUpdate("CREATE TABLE inconnue (id INTEGER PRIMARY KEY, v TEXT)")
                st.executeUpdate("INSERT INTO gpkg_contents (table_name, data_type, identifier) VALUES ('inconnue', 'attributes', 'inconnue')")
            }
        }
        val lue = export.relire(gpkg)
        assertTrue("inconnue" !in lue.champsParCouche, "couche inconnue retenue")
        assertTrue("nouveau" !in (lue.champsParCouche["projet"] ?: emptySet()), "champ inconnu retenu")
        val ref = lire("schema-couches-ajoutees.json").getAsJsonObject("couches")
        ref.entrySet().forEach { (nom, v) ->
            val champs = v.asJsonObject.getAsJsonArray("champs").map { it.asJsonObject.get("nom").asString }.toSet()
            assertTrue(lue.champsParCouche[nom].orEmpty().containsAll(champs), "$nom : champs connus non relus")
        }
    }

    private fun travaux(f: Fichier, attendus: JsonArray) {
        attendus.map { it.asJsonObject }.forEach { t ->
            val couche = t.get("couche").asString
            val u = t.get("uuid").asString
            assertEquals(t.get("code_travaux").asString, f.texte("SELECT code_travaux FROM $couche WHERE uuid='$u'"), "$u : code_travaux")
        }
    }

    private fun verifier(c: CasExport, f: Fichier) {
        val a = c.attendu
        a.get("zip")?.asJsonObject?.let { z ->
            z.get("contient")?.asJsonArray?.forEach { assertTrue(it.asString in f.entrees, "zip sans ${it.asString}") }
            if (z.get("dcim_present")?.asBoolean == true) assertTrue(f.entrees.any { it.startsWith("DCIM/") }, "DCIM/ absent")
            z.get("entrees_autorisees")?.asJsonArray?.map { it.asString }?.let { ok ->
                f.entrees.forEach { e ->
                    assertTrue(ok.any { p -> if (p.endsWith("*")) e.startsWith(p.dropLast(1)) else e == p }, "entrée hors zip à plat : $e")
                }
            }
        }
        a.get("couches_presentes")?.asJsonArray?.forEach { assertTrue(it.asString in f.contenus(), "couche absente : ${it.asString}") }
        a.get("schema_sommier_identique")?.let { schemaSommier(f, lire(it.asString)) }
        a.get("couches_ajoutees_conformes")?.let { schemaAjoute(f, lire(it.asString)) }
        a.get("conformite_geopackage")?.let { conformite(f) }
        a.get("valeurs_permises")?.let { valeursPermises(f, lire(it.asString)) }
        a.get("geometries")?.asJsonObject?.let { g -> geometries(f, g) }
        a.get("srs_id_couches_a_geometrie")?.let { s ->
            assertEquals(setOf(s.asInt), f.lignes("SELECT DISTINCT srs_id FROM gpkg_geometry_columns").map { it[0].toInt() }.toSet())
        }
        a.get("projet")?.asJsonObject?.let { projet(f, it) }
        a.get("controles_ecrits")?.asJsonArray?.let { controles(f, it) }
        a.get("totaux_controles_non_nuls")?.asJsonArray?.forEach { col ->
            assertEquals(0, f.nombre("SELECT count(*) FROM controles WHERE ${col.asString} IS NULL"), "total nul : ${col.asString}")
        }
        a.get("plants_ecrits")?.asJsonArray?.let { plants(f, it) }
        a.get("travaux_ecrits")?.asJsonArray?.let { travaux(f, it) }
        a.get("rtree")?.asJsonObject?.let { r ->
            assertEquals(r.get("lignes").asInt, f.nombre("SELECT count(*) FROM ${r.get("table").asString}"))
        }
        a.get("couches_vides")?.asJsonArray?.forEach { assertEquals(0, f.nombre("SELECT count(*) FROM ${it.asString}"), "non vide : ${it.asString}") }
        a.get("emprise_l93_m")?.let { pointsEnFrance(f, it.asJsonObject) }
        if (a.get("contenu_identique_apres_reexport")?.asBoolean == true) {
            val bis = export.exporter(c.entree, Files.createTempDirectory("export-bis-${c.id}")) as ResultatExport.Accepte
            Fichier(bis.zip).use { assertEquals(f.empreinteDuContenu(), it.empreinteDuContenu(), "contenu différent au second export") }
        }
        if (a.get("contenu_identique_a_l_ordre_direct")?.asBoolean == true) {
            val inverse = export.exporter(ordreInverse(c.entree), Files.createTempDirectory("export-inv-${c.id}")) as ResultatExport.Accepte
            Fichier(inverse.zip).use { assertEquals(f.empreinteDuContenu(), it.empreinteDuContenu(), "contenu dépendant de l'ordre d'arrivée") }
        }
    }

    // ---- vérifications

    private fun schemaSommier(f: Fichier, ref: JsonObject) {
        ref.getAsJsonObject("couches").entrySet().forEach { (nom, v) ->
            val o = v.asJsonObject
            assertEquals(o.get("data_type").asString, f.texte("SELECT data_type FROM gpkg_contents WHERE table_name='$nom'"), "$nom : data_type")
            assertEquals(o.get("srs_id").asInt, f.nombre("SELECT srs_id FROM gpkg_contents WHERE table_name='$nom'"), "$nom : srs_id")
            assertEquals(o.get("type_geometrie")?.takeUnless { it.isJsonNull }?.asString,
                f.texte("SELECT geometry_type_name FROM gpkg_geometry_columns WHERE table_name='$nom'"), "$nom : géométrie")
            val champs = o.getAsJsonArray("champs").map { it.asJsonObject.get("nom").asString to it.asJsonObject.get("type").asString }
            assertEquals(champs, f.colonnes(nom).map { it.first to it.second }, "$nom : champs")
        }
    }

    private fun schemaAjoute(f: Fichier, ref: JsonObject) {
        val classes = ref.getAsJsonObject("classes_acceptees")
        ref.getAsJsonObject("couches").entrySet().forEach { (nom, v) ->
            val o = v.asJsonObject
            assertEquals(o.get("data_type").asString, f.texte("SELECT data_type FROM gpkg_contents WHERE table_name='$nom'"), "$nom : data_type")
            val reel = f.colonnes(nom).associate { it.first to it.second.uppercase() }
            o.getAsJsonArray("champs").forEach { ch ->
                val n = ch.asJsonObject.get("nom").asString
                val classe = ch.asJsonObject.get("classe").asString
                assertTrue(n in reel, "$nom.$n absent")
                assertTrue(reel.getValue(n) in classes.getAsJsonArray(classe).map { it.asString }, "$nom.$n : type ${reel[n]} hors classe $classe")
            }
        }
    }

    private fun conformite(f: Fichier) {
        f.tablesUtilisateur().forEach { t ->
            assertEquals(1, f.nombre("SELECT count(*) FROM gpkg_contents WHERE table_name='$t'"), "$t absent de gpkg_contents")
            val pk = f.colonnes(t).filter { it.third }
            assertTrue(pk.size == 1 && pk[0].second.uppercase() == "INTEGER", "$t : clé primaire INTEGER simple attendue")
            assertEquals(1, f.nombre("SELECT count(*) FROM gpkg_contents c JOIN gpkg_spatial_ref_sys s ON s.srs_id=c.srs_id WHERE c.table_name='$t'"),
                "$t : srs_id absent de gpkg_spatial_ref_sys")
        }
    }

    private fun valeursPermises(f: Fichier, ref: JsonObject) {
        ref.getAsJsonArray("colonnes").map { it.asJsonObject }.forEach { col ->
            val t = col.get("couche").asString
            val n = col.get("colonne").asString
            val permises = f.lignes("SELECT k.value FROM gpkg_data_columns d JOIN gpkg_data_column_constraints k " +
                "ON k.constraint_name=d.constraint_name WHERE d.table_name='$t' AND d.column_name='$n' AND k.constraint_type='enum'")
                .map { it[0] }.toSet()
            val attendues = col.getAsJsonArray("valeurs").map { it.asString }.toSet()
            assertEquals(attendues, permises, "$t.$n : valeurs permises")
            col.get("referentiel")?.takeUnless { it.isJsonNull }?.let {
                val horsRef = col.get("valeurs_hors_referentiel")?.asJsonArray?.map { v -> v.asString }?.toSet() ?: emptySet()
                assertEquals(attendues - horsRef, valeursDuYaml(col.get("cle_referentiel").asString), "$t.$n : écart avec le référentiel")
            }
        }
    }

    private fun valeursDuYaml(cle: String): Set<String> {
        val lignes = Files.readAllLines(DOSSIER.resolve("docs/referentiels/etats-plants.yaml"))
        val debut = lignes.indexOfFirst { it.startsWith("$cle:") }
        assertTrue(debut >= 0, "clé $cle absente du yaml")
        val section = lignes.drop(debut + 1).takeWhile { it.isBlank() || it.startsWith(" ") || it.startsWith("#") }
        return section.mapNotNull { Regex("""^\s*-\s*(?:etat|groupe):\s*(\w+)""").find(it)?.groupValues?.get(1) }.toSet()
    }

    private fun geometries(f: Fichier, g: JsonObject) {
        g.entrySet().forEach { (t, v) ->
            if (v.isJsonNull) {
                assertEquals("attributes", f.texte("SELECT data_type FROM gpkg_contents WHERE table_name='$t'"), "$t : data_type")
                assertEquals(0, f.nombre("SELECT count(*) FROM gpkg_geometry_columns WHERE table_name='$t'"), "$t : géométrie déclarée")
            } else {
                assertEquals(v.asString, f.texte("SELECT geometry_type_name FROM gpkg_geometry_columns WHERE table_name='$t'"), "$t : géométrie")
            }
        }
    }

    private fun projet(f: Fichier, p: JsonObject) {
        p.get("lignes")?.let { assertEquals(it.asInt, f.nombre("SELECT count(*) FROM projet"), "projet : lignes") }
        if (f.nombre("SELECT count(*) FROM projet") == 0) return
        p.get("vides")?.asJsonArray?.forEach {
            val v = f.texte("SELECT ${it.asString} FROM projet")
            assertTrue(v == null || v.isEmpty(), "projet.${it.asString} doit rester vide")
        }
        p.get("non_vides")?.asJsonArray?.forEach {
            assertTrue(!f.texte("SELECT ${it.asString} FROM projet").isNullOrEmpty(), "projet.${it.asString} doit être rempli")
        }
        if (p.get("aucune_colonne_version_ailleurs")?.asBoolean == true) {
            f.tablesUtilisateur().filter { it != "projet" }.forEach { t ->
                assertFalse(f.colonnes(t).any { it.first.lowercase().startsWith("version") }, "$t porte une colonne de version")
            }
        }
    }

    private fun controles(f: Fichier, attendus: JsonArray) {
        assertEquals(attendus.size(), f.nombre("SELECT count(*) FROM controles"), "nombre de contrôles écrits")
        attendus.map { it.asJsonObject }.forEach { c ->
            val u = c.get("uuid").asString
            c.get("placette_uuid")?.let { assertEquals(it.asString, f.texte("SELECT placette_uuid FROM controles WHERE uuid='$u'"), "$u : placette") }
            listOf("nb_total", "nb_vivants", "nb_abroutis", "h_moy_cm").forEach { col ->
                c.get(col)?.let { assertEquals(it.asInt, f.nombre("SELECT $col FROM controles WHERE uuid='$u'"), "$u : $col") }
            }
            c.get("visite_le_utc")?.let {
                assertEquals(Instant.parse(it.asString), instant(f.texte("SELECT visite_le FROM controles WHERE uuid='$u'")!!), "$u : visite_le")
            }
        }
    }

    private fun plants(f: Fichier, attendus: JsonArray) {
        assertEquals(attendus.size(), f.nombre("SELECT count(*) FROM plants"), "nombre de plants écrits")
        attendus.map { it.asJsonObject }.forEach { p ->
            val u = p.get("uuid").asString
            p.get("placette_uuid")?.let { assertEquals(it.asString, f.texte("SELECT placette_uuid FROM plants WHERE uuid='$u'"), "$u : placette") }
            p.get("numero")?.let { assertEquals(it.asInt, f.nombre("SELECT numero FROM plants WHERE uuid='$u'"), "$u : numéro") }
            p.get("essence")?.let { assertEquals(it.asString, f.texte("SELECT essence FROM plants WHERE uuid='$u'"), "$u : essence") }
        }
    }

    private fun pointsEnFrance(f: Fichier, emprise: JsonObject) {
        val xs = emprise.getAsJsonArray("x").map { it.asDouble }
        val ys = emprise.getAsJsonArray("y").map { it.asDouble }
        var vus = 0
        f.lignes("SELECT table_name, column_name FROM gpkg_geometry_columns WHERE geometry_type_name='POINT'").forEach { (t, col) ->
            f.blobs("SELECT $col FROM $t WHERE $col IS NOT NULL").forEach { b ->
                val xy = pointGpkg(b) ?: return@forEach
                vus++
                assertTrue(xy.first in xs[0]..xs[1] && xy.second in ys[0]..ys[1], "$t : point hors de France $xy (x et y inversés ?)")
            }
        }
        assertTrue(vus > 0, "aucun point à vérifier")
    }

    /** Point GeoPackage (OGC 12-128 § 2.1.3) : en-tête, enveloppe facultative, puis WKB. Rend (x, y), ou null si vide. */
    private fun pointGpkg(b: ByteArray): Pair<Double, Double>? {
        val flags = b[3].toInt()
        if (flags and 0b0001_0000 != 0) return null
        val enveloppe = when ((flags shr 1) and 0b111) { 0 -> 0; 1 -> 32; 2, 3 -> 48; else -> 64 }
        val wkb = ByteBuffer.wrap(b, 8 + enveloppe, b.size - 8 - enveloppe)
        wkb.order(if (wkb.get().toInt() == 1) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
        wkb.getInt()
        return wkb.getDouble() to wkb.getDouble()
    }

    private fun instant(s: String): Instant {
        val t = s.trim().replace(' ', 'T')
        return Instant.parse(if (t.endsWith("Z") || t.contains('+')) t else t + "Z")
    }

    private fun ordreInverse(entree: JsonObject): JsonObject {
        val copie = entree.deepCopy()
        val controles = copie.getAsJsonObject("saisies").getAsJsonArray("controles")
        val inverses = controles.reversed().map { it.deepCopy().asJsonObject.also { c -> c.add("plants", c.getAsJsonArray("plants").reversed().toJsonArray()) } }
        copie.getAsJsonObject("saisies").add("controles", inverses.toJsonArray())
        return copie
    }

    private fun List<JsonElement>.toJsonArray() = JsonArray().also { a -> forEach { a.add(it) } }
}

/** Un .zip exporté, travaux.gpkg extrait et ouvert en lecture. */
private class Fichier(zip: Path) : AutoCloseable {
    val entrees: List<String>
    private val cnx: Connection

    init {
        val dir = Files.createTempDirectory("gpkg-lu")
        ZipFile(zip.toFile()).use { z ->
            entrees = z.entries().toList().map { it.name }
            z.getEntry("travaux.gpkg")?.let { e -> z.getInputStream(e).use { Files.copy(it, dir.resolve("travaux.gpkg")) } }
        }
        require(Files.exists(dir.resolve("travaux.gpkg"))) { "travaux.gpkg absent du zip" }
        cnx = DriverManager.getConnection("jdbc:sqlite:${dir.resolve("travaux.gpkg")}")
    }

    fun lignes(sql: String): List<List<String>> = cnx.createStatement().use { st ->
        st.executeQuery(sql).use { rs ->
            val n = rs.metaData.columnCount
            buildList { while (rs.next()) add((1..n).map { rs.getString(it) ?: "" }) }
        }
    }

    fun texte(sql: String): String? = cnx.createStatement().use { st -> st.executeQuery(sql).use { if (it.next()) it.getString(1) else null } }
    fun nombre(sql: String): Int = texte(sql)?.toDouble()?.toInt() ?: -1
    fun blobs(sql: String): List<ByteArray> = cnx.createStatement().use { st ->
        st.executeQuery(sql).use { rs -> buildList { while (rs.next()) add(rs.getBytes(1)) } }
    }

    fun contenus(): Set<String> = lignes("SELECT table_name FROM gpkg_contents").map { it[0] }.toSet()
    fun tablesUtilisateur(): List<String> = contenus().sorted()

    /** (nom, type, clé primaire) dans l'ordre des colonnes. */
    fun colonnes(t: String): List<Triple<String, String, Boolean>> =
        lignes("PRAGMA table_info($t)").map { Triple(it[1], it[2], it[5] != "0" && it[5].isNotEmpty()) }

    /** Contenu de toutes les tables, sans la clé fid, sans l'ordre des lignes : jamais une empreinte du fichier (ADR 0011). */
    fun empreinteDuContenu(): Map<String, List<String>> {
        val tables = lignes("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'rtree_%'").map { it[0] }.sorted()
        return tables.associateWith { t ->
            val cols = colonnes(t).map { it.first }.filter { it != "fid" && it != "last_change" }
            if (cols.isEmpty()) emptyList()
            else lignes("SELECT ${cols.joinToString(",") { "quote($it)" }} FROM $t").map { it.joinToString("|") }.sorted()
        }
    }

    override fun close() = cnx.close()
}
