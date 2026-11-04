package io.github.pobsteta.surculus.core.fixtures

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.placette.EmpriseEtFrance
import io.github.pobsteta.surculus.core.session.ValeurControle
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.EOFException
import java.io.File
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Mission 088 : les exemples du format `.sursync` v2 (`fixtures/sursync/exemple-v2*`, refaits par
 * `fixtures/sursync/exemple-v2-fabrication.sh`) et les documents du format (`docs/formats/sursync-v2.md`, son schéma,
 * le renvoi depuis `sursync-v1.md`). Vérifie les fichiers avec gzip, SHA-256 et Gson seuls, sans le code de la
 * sauvegarde : un successeur peut refaire chaque vérification avec `zcat` et `sha256sum`.
 *
 * Rouge jusqu'au `dev` : les tests des documents (le document v2, son schéma et le renvoi n'existent pas encore).
 * Coordonnées : Lambert-93 (EPSG:2154), x = Est, y = Nord, mètres.
 */
class FixturesSursyncV2Test {
    private val dossier = File(Depot.racine, "fixtures/sursync")
    private val formats = File(Depot.racine, "docs/formats")

    private fun octets(nom: String) = File(dossier, nom).readBytes()
    private fun degzip(b: ByteArray) = GZIPInputStream(ByteArrayInputStream(b)).readBytes()
    private fun sha256(b: ByteArray) = MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }
    private fun lignes(texte: String) = texte.trimEnd('\n').split("\n")
    private fun json(s: String): JsonObject = JsonParser.parseString(s).asJsonObject

    private val contenu: ByteArray by lazy { degzip(octets("exemple-v2.sursync")) }
    private val texte: List<String> by lazy { lignes(contenu.toString(Charsets.UTF_8)) }

    /** Les octets de tout ce qui précède le pied, fins de ligne comprises (`sursync-v1.md`, « Structure », 3). */
    private fun avantPied(b: ByteArray): ByteArray {
        val t = b.toString(Charsets.UTF_8)
        val i = t.trimEnd('\n').lastIndexOf('\n')
        return t.substring(0, i + 1).toByteArray(Charsets.UTF_8)
    }

    private fun schema(nom: String): JsonObject = json(File(formats, nom).readText()).getAsJsonObject("\$defs")
    private fun requis(s: JsonObject, def: String) = s.getAsJsonObject(def).getAsJsonArray("required").map { it.asString }.toSet()

    // ------------------------------------------------------------------ l'exemple v2

    @Test
    fun `exemple v2 - gzip lisible, contenu identique au jsonl publie, empreinte juste`() {
        assertEquals(File(dossier, "exemple-v2.jsonl").readText(), contenu.toString(Charsets.UTF_8))
        val pied = json(texte.last())
        assertEquals(setOf("sha256"), pied.keySet())
        assertEquals(sha256(avantPied(contenu)), pied.get("sha256").asString)
        assertTrue(Regex("^[0-9a-f]{64}$").matches(pied.get("sha256").asString))
    }

    @Test
    fun `exemple v2 - en-tete en version 2, une seule tournee, nb_lignes juste, memes champs que la v1`() {
        val e = json(texte.first())
        assertEquals("sursync", e.get("format").asString)
        assertEquals(2, e.get("version").asInt)
        assertEquals("COUCHEY-2026", e.get("code_tournee").asString)
        assertEquals(texte.size - 2, e.get("nb_lignes").asInt)
        val v1 = schema("sursync-v1.schema.json")
        assertEquals(requis(v1, "entete"), e.keySet(), "structure inchangée : seul le numéro de version monte")
        texte.drop(1).dropLast(1).forEach { assertEquals(requis(v1, "saisie"), json(it).keySet(), it) }
    }

    @Test
    fun `exemple v2 - saisies dans l'ordre total du journal, uuid uniques`() {
        val s = texte.drop(1).dropLast(1).map(::json)
        val cles = s.map { listOf(it.get("saisi_le_utc_ms").asLong.toString().padStart(20, '0'), it.get("appareil").asString, it.get("sequence").asLong.toString().padStart(20, '0'), it.get("uuid").asString) }
        assertEquals(cles.sortedWith { a, b -> a.joinToString("|").compareTo(b.joinToString("|")) }, cles)
        assertEquals(s.size, s.map { it.get("uuid").asString }.toSet().size)
    }

    @Test
    fun `exemple v2 - un controle sans position et un avec, valeurs v2 lisibles et reecrites au caractere pres`() {
        val valeurs = texte.drop(1).dropLast(1).map(::json).filter { it.get("type").asString == "controle" }.map { it.get("valeur").asString }
        assertEquals(2, valeurs.size)
        val lues = valeurs.map { v ->
            val traces = ArrayList<String>()
            val c = assertNotNull(ValeurControle.lire(v) { traces += it }, "valeur illisible : $v")
            assertEquals(emptyList(), traces)
            // Mission 091 : Surculus écrit désormais la v3 ; une valeur v2 relue garde chaque clé, hors `v`, à l'identique.
            assertMemesClesSaufVersion(v, c.versJson())
            assertTrue(v.startsWith("{\"v\":2,"))
            c
        }
        val v2Sans = valeurs.single { "\"sans_position_mesuree\":true" in it }
        val sans = lues.single { it.sansPositionMesuree }
        assertNull(sans.positionMesureeX); assertNull(sans.positionMesureeY)
        assertTrue(sans.rattacheParIdentite)
        val avec = lues.single { !it.sansPositionMesuree }
        assertNotNull(avec.positionMesureeX); assertNotNull(avec.positionMesureeY)
        // Le contrôle sans position est l'exemple du format des valeurs v2, recopié tel quel.
        val exemple = Regex("```json\\s*\\n(.*?)\\n```", RegexOption.DOT_MATCHES_ALL)
            .findAll(File(formats, "journal-valeurs-v2.md").readText()).map { it.groupValues[1].trim() }.single { "\"v\":2" in it }
        assertEquals(exemple, v2Sans, "recopié tel quel dans la fixture")
        assertMemesClesSaufVersion(exemple, sans.versJson())
    }

    /** Mission 091 (v3) : chaque clé de [v2], hors `v`, se retrouve avec la même valeur (null explicites compris) dans [ecrit]. */
    private fun assertMemesClesSaufVersion(v2: String, ecrit: String) {
        val a = json(v2); val b = json(ecrit)
        for ((k, x) in a.entrySet()) if (k != "v") assertEquals(x, b.get(k), "clé $k : $ecrit")
    }

    @Test
    fun `exemple v2 - les positions tombent en France, et pas une fois x et y inverses (la placette en Somalie)`() {
        val lues = texte.drop(1).dropLast(1).map(::json).map { ValeurControle.lire(it.get("valeur").asString)!! }
        for (c in lues) {
            assertEquals(2154, c.repereEpsg)
            val p = PointLambert93(c.positionX, c.positionY)
            assertTrue(EmpriseEtFrance.enFrance(p), "position du contrôle hors de France : $p")
            assertFalse(EmpriseEtFrance.enFrance(PointLambert93(c.positionY, c.positionX)), "x et y inversés ne doivent pas tomber en France")
            if (!c.sansPositionMesuree) assertTrue(EmpriseEtFrance.enFrance(PointLambert93(c.positionMesureeX!!, c.positionMesureeY!!)))
        }
    }

    // ------------------------------------------------------------------ les exemples abîmés

    @Test
    fun `exemple v2 a empreinte fausse - pied de l'original, une valeur changee, le controle sans position intact`() {
        val faux = degzip(octets("exemple-v2-empreinte-fausse.sursync"))
        val l = lignes(faux.toString(Charsets.UTF_8))
        assertEquals(texte.last(), l.last(), "pied inchangé")
        assertNotEquals(json(l.last()).get("sha256").asString, sha256(avantPied(faux)), "l'empreinte doit être fausse")
        assertEquals(texte.size, l.size)
        assertEquals(texte.dropLast(2), l.dropLast(2), "seule la ligne du contrôle avec position change")
        assertNotEquals(texte[texte.size - 2], l[l.size - 2])
    }

    @Test
    fun `exemple v2 tronque - les 10 derniers octets de l'exemple en moins, gzip coupe`() {
        val entier = octets("exemple-v2.sursync")
        val coupe = octets("exemple-v2-tronque.sursync")
        assertEquals(entier.size - 10, coupe.size)
        assertTrue(entier.copyOf(coupe.size).contentEquals(coupe))
        assertFailsWith<EOFException> { degzip(coupe) }
    }

    @Test
    fun `le script de fabrication est publie avec les exemples`() {
        val s = File(dossier, "exemple-v2-fabrication.sh").readText()
        listOf("gzip -n", "sha256sum", "exemple-v2-empreinte-fausse.sursync", "exemple-v2-tronque.sursync").forEach { assertTrue(it in s, it) }
    }

    // ------------------------------------------------------------------ les documents du format (écrits par le dev)

    @Test
    fun `sursync-v2 md existe, renvoie a la v1 et dit les refus`() {
        val doc = File(formats, "sursync-v2.md")
        assertTrue(doc.isFile, "docs/formats/sursync-v2.md absent (spec § 4.8 : écrit par la mission de code)")
        val t = doc.readText()
        listOf("sursync-v1.md", "sursync-v2.schema.json", "journal-valeurs-v2.md", "version_future", "version_illisible", "exemple-v2")
            .forEach { assertTrue(it in t, "sursync-v2.md ne cite pas $it") }
        assertTrue("application à jour" in t, "message « mettre l'application à jour »")
    }

    @Test
    fun `sursync-v1 md renvoie a la v2 par un lien`() {
        // Le nom y figure déjà en texte (« La mission 086 crée la v2 (`sursync-v2.md`… ») : on exige un lien qui mène au document.
        assertTrue(Regex("\\]\\(sursync-v2\\.md\\)").containsMatchIn(File(formats, "sursync-v1.md").readText()), "pas de lien [..](sursync-v2.md)")
    }

    @Test
    fun `le schema v2 a la structure de la v1 et reserve le type photo`() {
        val v1 = schema("sursync-v1.schema.json")
        val f = File(formats, "sursync-v2.schema.json")
        assertTrue(f.isFile, "docs/formats/sursync-v2.schema.json absent")
        val v2 = schema("sursync-v2.schema.json")
        for (def in listOf("entete", "saisie", "pied")) assertEquals(requis(v1, def), requis(v2, def), def)
        fun types(s: JsonObject) = s.getAsJsonObject("saisie").getAsJsonObject("properties").getAsJsonObject("type").getAsJsonArray("enum").map { it.asString }.toSet()
        assertEquals(types(v1) + "photo", types(v2), "spec § 4.8 : la v2 ajoute le type de ligne photo (réservé, non écrit)")
        // L'exemple v2 respecte les clés du schéma v2.
        assertEquals(requis(v2, "entete"), json(texte.first()).keySet())
    }
}
