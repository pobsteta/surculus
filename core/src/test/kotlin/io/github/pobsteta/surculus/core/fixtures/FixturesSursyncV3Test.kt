/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
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
import kotlin.test.assertTrue

/**
 * Mission 091, décision de Pascal du 2026-11-04, point 3 : la sauvegarde `.sursync` passe en **v3**, partagée avec les
 * valeurs v3 du journal. Les exemples `fixtures/sursync/exemple-v3*`, refaits par `exemple-v3-fabrication.sh`, sont
 * vérifiés avec gzip, SHA-256 et Gson seuls, sans le code de la sauvegarde : un successeur peut refaire chaque
 * vérification avec `zcat` et `sha256sum`. Comme `FixturesSursyncV2Test` (mission 088).
 *
 * Les tests des valeurs (lecture par `ValeurControle.lire`) sont **rouges (ne compilent pas) jusqu'au `dev`** ; ceux des
 * documents attendent `docs/formats/sursync-v3.md` et `journal-valeurs-v3.md` (orchestrateur, décision 3).
 * Coordonnées : Lambert-93 (EPSG:2154), x = Est, y = Nord, mètres.
 */
class FixturesSursyncV3Test {
    private val dossier = File(Depot.racine, "fixtures/sursync")
    private val formats = File(Depot.racine, "docs/formats")

    private fun octets(nom: String) = File(dossier, nom).readBytes()
    private fun degzip(b: ByteArray) = GZIPInputStream(ByteArrayInputStream(b)).readBytes()
    private fun sha256(b: ByteArray) = MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }
    private fun lignes(texte: String) = texte.trimEnd('\n').split("\n")
    private fun json(s: String): JsonObject = JsonParser.parseString(s).asJsonObject

    private val contenu: ByteArray by lazy { degzip(octets("exemple-v3.sursync")) }
    private val texte: List<String> by lazy { lignes(contenu.toString(Charsets.UTF_8)) }
    private val saisies: List<JsonObject> by lazy { texte.drop(1).dropLast(1).map(::json) }

    private fun avantPied(b: ByteArray): ByteArray {
        val t = b.toString(Charsets.UTF_8)
        val i = t.trimEnd('\n').lastIndexOf('\n')
        return t.substring(0, i + 1).toByteArray(Charsets.UTF_8)
    }

    private fun requis(nom: String, def: String) =
        json(File(formats, nom).readText()).getAsJsonObject("\$defs").getAsJsonObject(def).getAsJsonArray("required").map { it.asString }.toSet()

    // ------------------------------------------------------------------ l'exemple v3

    @Test
    fun `exemple v3 - gzip lisible, contenu identique au jsonl publie, empreinte juste`() {
        assertEquals(File(dossier, "exemple-v3.jsonl").readText(), contenu.toString(Charsets.UTF_8))
        val pied = json(texte.last())
        assertEquals(setOf("sha256"), pied.keySet())
        assertEquals(sha256(avantPied(contenu)), pied.get("sha256").asString)
    }

    @Test
    fun `exemple v3 - en-tete en version 3, une seule tournee, nb_lignes juste, structure de la v2 inchangee`() {
        val e = json(texte.first())
        assertEquals("sursync", e.get("format").asString)
        assertEquals(3, e.get("version").asInt)
        assertEquals("COUCHEY-2026", e.get("code_tournee").asString)
        assertEquals(saisies.size, e.get("nb_lignes").asInt)
        assertEquals(requis("sursync-v2.schema.json", "entete"), e.keySet(), "seul le numéro de version monte")
        saisies.forEach { assertEquals(requis("sursync-v2.schema.json", "saisie"), it.keySet(), it.toString()) }
    }

    @Test
    fun `exemple v3 - saisies dans l'ordre total du journal, uuid uniques`() {
        val cles = saisies.map { listOf(it.get("saisi_le_utc_ms").asLong.toString().padStart(20, '0'), it.get("appareil").asString, it.get("sequence").asLong.toString().padStart(20, '0'), it.get("uuid").asString) }
        assertEquals(cles.sortedWith { a, b -> a.joinToString("|").compareTo(b.joinToString("|")) }, cles)
        assertEquals(saisies.size, saisies.map { it.get("uuid").asString }.toSet().size)
    }

    @Test
    fun `exemple v3 - une valeur v2 ancienne et trois valeurs v3, besoin et concurrence dans leurs domaines`() {
        val valeurs = saisies.map { json(it.get("valeur").asString) }
        assertEquals(listOf(2, 3, 3, 3), valeurs.map { it.get("v").asInt })
        val v2 = valeurs.first()
        for (k in listOf("besoin", "concurrence", "precision_m", "source_gnss", "simulee")) assertFalse(v2.has(k), "la valeur v2 n'a pas $k")
        for (v in valeurs.drop(1)) {
            for (k in listOf("besoin", "concurrence", "precision_m", "source_gnss", "simulee")) assertTrue(v.has(k), "v3 sans $k : $v")
            // besoin : la couche `codes` de la tournée d'essai porte RG (sommieR 0.31.1, R/registre6.R l. 17-45), ou « aucun ».
            assertTrue(v.get("besoin").asString in setOf("aucun", "RG"), v.toString())
            assertTrue(v.get("concurrence").asString in setOf("faible", "moyenne", "forte"), v.toString())
            val s = v.get("source_gnss")
            if (!s.isJsonNull) assertTrue(s.asString in setOf("telephone", "recepteur_autonome", "recepteur_dgps", "recepteur_rtk_flottant", "recepteur_rtk_fixe"))
            val p = v.get("precision_m")
            if (!p.isJsonNull) assertTrue(p.asDouble > 0.0, "precision_m jamais 0 : $v")
        }
        val sans = valeurs.single { it.get("sans_position_mesuree").asBoolean }
        for (k in listOf("precision_m", "source_gnss", "simulee")) assertTrue(sans.get(k).isJsonNull, "sans position mesurée : $k null")
    }

    @Test
    fun `exemple v3 - chaque valeur se lit par le journal sans trace`() {
        for (s in saisies) {
            val v = s.get("valeur").asString
            val traces = ArrayList<String>()
            assertNotNull(ValeurControle.lire(v) { traces += it }, "valeur illisible : $v")
            assertEquals(emptyList(), traces, v)
        }
    }

    @Test
    fun `exemple v3 - les positions tombent en France, et pas une fois x et y inverses (la placette en Somalie)`() {
        for (s in saisies) {
            val c = assertNotNull(ValeurControle.lire(s.get("valeur").asString))
            assertEquals(2154, c.repereEpsg)
            val p = PointLambert93(c.positionX, c.positionY)
            assertTrue(EmpriseEtFrance.enFrance(p), "position du contrôle hors de France : $p")
            assertFalse(EmpriseEtFrance.enFrance(PointLambert93(c.positionY, c.positionX)), "x et y inversés ne doivent pas tomber en France")
            if (!c.sansPositionMesuree) assertTrue(EmpriseEtFrance.enFrance(PointLambert93(c.positionMesureeX!!, c.positionMesureeY!!)))
        }
    }

    // ------------------------------------------------------------------ les exemples abîmés

    @Test
    fun `exemple v3 a empreinte fausse - pied de l'original, seule la valeur de P35-01 changee`() {
        val faux = degzip(octets("exemple-v3-empreinte-fausse.sursync"))
        val l = lignes(faux.toString(Charsets.UTF_8))
        assertEquals(texte.last(), l.last(), "pied inchangé")
        assertNotEquals(json(l.last()).get("sha256").asString, sha256(avantPied(faux)), "l'empreinte doit être fausse")
        assertEquals(texte.size, l.size)
        val differentes = texte.indices.filter { texte[it] != l[it] }
        assertEquals(1, differentes.size)
        assertTrue("\\\"code_placette\\\":\\\"P35-01\\\"" in texte[differentes.single()], "seule la ligne de P35-01 change")
    }

    @Test
    fun `exemple v3 tronque - les 10 derniers octets de l'exemple en moins, gzip coupe`() {
        val entier = octets("exemple-v3.sursync")
        val coupe = octets("exemple-v3-tronque.sursync")
        assertEquals(entier.size - 10, coupe.size)
        assertTrue(entier.copyOf(coupe.size).contentEquals(coupe))
        assertFailsWith<EOFException> { degzip(coupe) }
    }

    @Test
    fun `le script de fabrication v3 est publie avec les exemples`() {
        val s = File(dossier, "exemple-v3-fabrication.sh").readText()
        listOf("gzip -n", "sha256sum", "exemple-v3-empreinte-fausse.sursync", "exemple-v3-tronque.sursync").forEach { assertTrue(it in s, it) }
    }

    // ------------------------------------------------------------------ les documents du format (orchestrateur, décision 3)

    @Test
    fun `sursync-v3 md et journal-valeurs-v3 md existent et se citent`() {
        val sursync = File(formats, "sursync-v3.md")
        val valeurs = File(formats, "journal-valeurs-v3.md")
        assertTrue(sursync.isFile, "docs/formats/sursync-v3.md absent (décision 3 du 2026-11-04 : rédigé par l'orchestrateur)")
        assertTrue(valeurs.isFile, "docs/formats/journal-valeurs-v3.md absent")
        val t = sursync.readText()
        listOf("journal-valeurs-v3.md", "version_future", "exemple-v3").forEach { assertTrue(it in t, "sursync-v3.md ne cite pas $it") }
        val v = valeurs.readText()
        listOf("besoin", "concurrence", "precision_m", "source_gnss", "simulee", "journal-valeurs-v2.md").forEach { assertTrue(it in v, "journal-valeurs-v3.md ne cite pas $it") }
    }
}
