/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.core.session

import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Mission 091, décisions de Pascal du 2026-11-04, points 2, 3 et 4 : les valeurs du journal passent en **v3** ; le
 * contrôle porte le besoin, la concurrence et la qualité du fix (`precision_m`, `source_gnss`, `simulee`) ; le journal
 * relit v1, v2 et v3 sans perte ; un contrôle ancien (v1, v2) a une qualité du fix **NULL, jamais « faux »**.
 *
 * Écrit par le `testeur` depuis la fiche 091, la spec § 5 (`positions_controles`), `docs/formats/journal-valeurs-v1.md`
 * et `-v2.md`, **sans lire** `Valeurs.kt` ni `SessionControle.kt`. `docs/formats/journal-valeurs-v3.md` n'est pas encore
 * écrit (orchestrateur) : les noms de clé suivent les colonnes de la spec § 5 et de sommieR (`besoin`, `concurrence`,
 * `precision_m`, `source_gnss`, `simulee`) ; **aucun test n'impose leur ordre** dans la valeur (la comparaison se fait
 * clé à clé). L'exemple v3 est `fixtures/sursync/exemple-v3.jsonl` (`exemple-v3-fabrication.sh`).
 *
 * **Rouge (ne compile pas) jusqu'au `dev`.** API supposée (reprise de la fiche : `QualiteFix` dans `core.session`,
 * `precisionM: Double?`), que le `dev` confirme ou remplace :
 * ```kotlin
 * data class QualiteFix(val precisionM: Double?, val sourceGnss: String, val simulee: Boolean)
 * // ValeurControle : trois champs en plus, en fin de constructeur, avec null par défaut (les appels positionnels de
 * // ValeursEtCodesTest restent valides) :
 * //   val besoin: String? = null, val concurrence: String? = null, val qualite: QualiteFix? = null
 * // FormatValeurs.VERSION = 3 ; Arrivee.Lambert93(position, qualite: QualiteFix? = null)
 * ```
 * `qualite == null` se lit « qualité du fix inconnue » : les trois clés sont écrites à `null` (sans position mesurée,
 * contrôle ancien). Jamais `simulee: false` par défaut.
 *
 * Coordonnées : Lambert-93 (EPSG:2154), x = Est, y = Nord, mètres ; Couchey (Côte-d'Or).
 */
class JournalValeursV3Test {
    private val racine: Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }.first { Files.exists(it.resolve("settings.gradle.kts")) }

    private fun exemples(doc: String): List<String> =
        Regex("```json\\s*\\n(.*?)\\n```", RegexOption.DOT_MATCHES_ALL).findAll(Files.readString(racine.resolve("docs/formats/$doc")))
            .map { it.groupValues[1].trim() }.toList()

    private val exempleV1 by lazy { exemples("journal-valeurs-v1.md").single { "\"placette_uuid\"" in it && "\"v\":1" in it } }
    private val exempleV2 by lazy { exemples("journal-valeurs-v2.md").single { "\"v\":2" in it } }

    /** Les `valeur` des saisies `controle` de l'exemple v3 de la sauvegarde, par code de placette (lues avec Gson). */
    private val fixtureV3: Map<String, String> by lazy {
        Files.readAllLines(racine.resolve("fixtures/sursync/exemple-v3.jsonl")).drop(1).dropLast(1)
            .map { JsonParser.parseString(it).asJsonObject.get("valeur").asString }
            .associateBy { JsonParser.parseString(it).asJsonObject.get("code_placette").asString }
    }

    private fun json(s: String): JsonObject = JsonParser.parseString(s).asJsonObject

    private fun lireTrace(texte: String): Pair<ValeurControle?, List<String>> {
        val traces = ArrayList<String>()
        return ValeurControle.lire(texte) { traces += it } to traces
    }

    /** P35-01, avec une position mesurée à 0,72 m du piquet, terminé (Lambert-93, x = E, y = N, m). */
    private val rtk = ValeurControle(
        placetteUuid = "11111111-1111-4111-8111-111111111111", codePlacette = "P35-01", jourCivil = "2026-11-03",
        operateur = "Agent d'essai", termine = true, visiteLeUtcMs = 1_793_706_400_000,
        positionX = 846164.76, positionY = 6686123.26, positionMesureeX = 846165.36, positionMesureeY = 6686122.86,
        rattacheParIdentite = false, repereEpsg = 2154, numeros = mapOf("019...a" to 1, "019...b" to 2), sansPositionMesuree = false,
        besoin = "aucun", concurrence = "moyenne",
        qualite = QualiteFix(precisionM = 0.02, sourceGnss = "recepteur_rtk_fixe", simulee = false, referentielSource = "RGF93"),
    )

    /** P35-02, ouvert sans position mesurée : qualité du fix inconnue ; besoin « RG » (couche `codes` de la tournée). */
    private val sansPosition = ValeurControle(
        placetteUuid = "33333333-3333-4333-8333-333333333333", codePlacette = "P35-02", jourCivil = "2026-11-03",
        operateur = "Agent d'essai", termine = true, visiteLeUtcMs = 1_793_706_700_000,
        positionX = 846200.0, positionY = 6686150.0, positionMesureeX = null, positionMesureeY = null,
        rattacheParIdentite = true, repereEpsg = 2154, numeros = mapOf("019...c" to 1), sansPositionMesuree = true,
        besoin = "RG", concurrence = "forte", qualite = null,
    )

    private val nulle: JsonElement = JsonNull.INSTANCE

    // ------------------------------------------------------------------ écriture v3

    @Test
    fun `un controle termine s'ecrit en v3 avec besoin, concurrence, precision_m, source_gnss et simulee, et se relit`() {
        val t = rtk.versJson()
        assertTrue(t.startsWith("{\"v\":3,"), t)
        val o = json(t)
        assertEquals("aucun", o.get("besoin")?.asString, t)
        assertEquals("moyenne", o.get("concurrence")?.asString, t)
        assertEquals(0.02, assertNotNull(o.get("precision_m"), t).asDouble, 0.0)
        assertEquals("recepteur_rtk_fixe", o.get("source_gnss")?.asString, t)
        assertEquals(false, assertNotNull(o.get("simulee"), t).asBoolean)
        val (lu, traces) = lireTrace(t)
        assertEquals(rtk, lu)
        assertEquals(emptyList(), traces)
    }

    @Test
    fun `sans position mesuree - precision_m, source_gnss et simulee ecrits a null explicite, jamais faux`() {
        val t = sansPosition.versJson()
        assertTrue(t.startsWith("{\"v\":3,"), t)
        val o = json(t)
        for (k in listOf("precision_m", "source_gnss", "simulee", "referentiel_source")) {
            assertTrue(o.has(k), "clé $k absente : $t (null explicite, comme position_mesuree_x en v2)")
            assertEquals(nulle, o.get(k), "clé $k : $t")
        }
        assertEquals("RG", o.get("besoin").asString)
        assertEquals("forte", o.get("concurrence").asString)
        assertEquals(sansPosition, ValeurControle.lire(t))
    }

    @Test
    fun `un fix qui ne donne pas sa precision - precision_m null, jamais 0, la source et simulee restent`() {
        val v = rtk.copy(qualite = QualiteFix(precisionM = null, sourceGnss = "recepteur_autonome", simulee = false))
        val o = json(v.versJson())
        assertEquals(nulle, o.get("precision_m"), v.versJson())
        assertEquals("recepteur_autonome", o.get("source_gnss").asString)
        assertEquals(false, o.get("simulee").asBoolean)
        assertEquals(v, ValeurControle.lire(v.versJson()))
    }

    @Test
    fun `une position simulee s'ecrit simulee vrai et se relit`() {
        val v = rtk.copy(qualite = QualiteFix(precisionM = 3.8, sourceGnss = "telephone", simulee = true))
        assertEquals(true, json(v.versJson()).get("simulee").asBoolean)
        assertEquals(v, ValeurControle.lire(v.versJson()))
    }

    @Test
    fun `un controle ouvert, pas encore termine - besoin et concurrence null, la valeur v3 se relit`() {
        val ouvert = rtk.copy(termine = false, numeros = null, besoin = null, concurrence = null)
        val o = json(ouvert.versJson())
        assertEquals(nulle, o.get("besoin") ?: nulle)
        assertEquals(nulle, o.get("concurrence") ?: nulle)
        assertEquals(ouvert, ValeurControle.lire(ouvert.versJson()))
    }

    // ------------------------------------------------------------------ v1 et v2 relues sans perte

    @Test
    fun `l'exemple v1 se relit - besoin, concurrence et qualite du fix null, jamais faux`() {
        val (lu, traces) = lireTrace(exempleV1)
        val c = assertNotNull(lu, exempleV1)
        assertEquals(emptyList(), traces)
        assertNull(c.besoin); assertNull(c.concurrence)
        assertNull(c.qualite, "contrôle ancien : qualité du fix inconnue (décision 2 du 2026-11-04)")
        assertTrue(c.termine)
        assertEquals(846164.76, c.positionMesureeX!!, 0.0)
        // Réécrit en v3 : la qualité reste inconnue (null), jamais « simulee: false ».
        val o = json(c.versJson())
        assertEquals(nulle, o.get("simulee"), c.versJson())
        assertEquals(nulle, o.get("precision_m"), c.versJson())
    }

    @Test
    fun `l'exemple v2 se relit - meme controle, besoin, concurrence et qualite null`() {
        val (lu, traces) = lireTrace(exempleV2)
        val c = assertNotNull(lu, exempleV2)
        assertEquals(emptyList(), traces)
        assertTrue(c.sansPositionMesuree); assertNull(c.positionMesureeX)
        assertEquals("P35-01", c.codePlacette); assertEquals(mapOf("019...a" to 1, "019...b" to 2), c.numeros)
        assertNull(c.besoin); assertNull(c.concurrence); assertNull(c.qualite)
        // Chaque clé de la v2, hors `v`, est réécrite à l'identique : rien n'est perdu.
        val o = json(c.versJson())
        for ((k, x) in json(exempleV2).entrySet()) if (k != "v") assertEquals(x, o.get(k), "clé $k")
    }

    // ------------------------------------------------------------------ l'exemple v3 (fixture de la sauvegarde)

    @Test
    fun `chaque valeur de l'exemple v3 se relit sans trace et dit ce qu'elle porte`() {
        assertEquals(setOf("P35-01", "P35-02", "P35-03", "P35-04"), fixtureV3.keys)
        val lu = fixtureV3.mapValues { (code, v) ->
            val (c, traces) = lireTrace(v)
            assertEquals(emptyList(), traces, code)
            assertNotNull(c, "$code illisible : $v")
        }
        assertEquals(rtk, lu.getValue("P35-01"))
        assertEquals(sansPosition, lu.getValue("P35-02"))
        val ancien = lu.getValue("P35-03")
        assertTrue(fixtureV3.getValue("P35-03").startsWith("{\"v\":2,"), "P35-03 est une valeur v2 dans une sauvegarde v3")
        assertTrue(ancien.termine); assertNull(ancien.besoin); assertNull(ancien.concurrence); assertNull(ancien.qualite)
        assertEquals(QualiteFix(precisionM = 3.8, sourceGnss = "telephone", simulee = true, referentielSource = "WGS84"), lu.getValue("P35-04").qualite)
    }

    @Test
    fun `une valeur v3 reecrite par Surculus redonne chaque cle de l'exemple, ni plus ni moins`() {
        for (code in listOf("P35-01", "P35-02", "P35-04")) {
            val v = fixtureV3.getValue(code)
            val reecrit = assertNotNull(ValeurControle.lire(v), v).versJson()
            assertEquals(json(v), json(reecrit), "$code : mêmes clés, mêmes valeurs (l'ordre relève de journal-valeurs-v3.md)")
        }
    }

    // ------------------------------------------------------------------ valeurs hors référentiel ou incohérentes

    /**
     * Proposé par le `testeur` (règle de la v2 : « une valeur incohérente est illisible et ignorée, jamais complétée ») ;
     * `journal-valeurs-v3.md` le confirme ou le remplace.
     */
    @Test
    fun `v3 hors domaine - concurrence, source_gnss inconnues ou precision_m a 0, valeur ignoree et tracee`() {
        val base = fixtureV3.getValue("P35-01")
        val fautives = listOf(
            base.replace("\"concurrence\":\"moyenne\"", "\"concurrence\":\"élevée\""),
            base.replace("\"source_gnss\":\"recepteur_rtk_fixe\"", "\"source_gnss\":\"gps\""),
            base.replace("\"precision_m\":0.02", "\"precision_m\":0"),
            base.replace("\"precision_m\":0.02", "\"precision_m\":-1.0"),
        )
        for (t in fautives) {
            assertTrue(t != base, "le remplacement doit porter : $t")
            val (lu, traces) = lireTrace(t)
            assertNull(lu, t)
            assertEquals(1, traces.size, t)
        }
    }

    @Test
    fun `v3 incoherente - sans position mesuree mais une qualite du fix, valeur ignoree`() {
        val t = fixtureV3.getValue("P35-02").replace("\"source_gnss\":null", "\"source_gnss\":\"telephone\"")
        assertTrue(t != fixtureV3.getValue("P35-02"))
        assertNull(lireTrace(t).first)
    }

    // ------------------------------------------------------------------ mission 094 : referentiel_source, ajout compatible en v3

    /*
     * Décisions de Pascal du 2026-11-06, point 3 : clé `referentiel_source` (« RGF93 », « WGS84 » ou null) **à la fin** de la
     * valeur `controle`, après `concurrence` ; le journal reste en v3 ; une valeur v1, v2 ou v3 sans la clé se relit null ;
     * une valeur hors domaine rend la ligne illisible ; `sans_position_mesuree` vrai impose null.
     */

    @Test
    fun `094 - referentiel_source s'ecrit en v3, derniere cle apres concurrence, et se relit`() {
        for (r in listOf("RGF93", "WGS84", null)) {
            val v = rtk.copy(qualite = rtk.qualite!!.copy(referentielSource = r))
            val t = v.versJson()
            assertTrue(t.startsWith("{\"v\":3,"), "le journal reste en v3 : $t")
            val cles = json(t).keySet().toList()
            assertEquals("referentiel_source", cles.last(), t)
            assertEquals("concurrence", cles[cles.size - 2], t)
            assertEquals(if (r == null) nulle else com.google.gson.JsonPrimitive(r), json(t).get("referentiel_source"), t)
            val (lu, traces) = lireTrace(t)
            assertEquals(v, lu); assertEquals(emptyList(), traces)
            assertEquals(r, lu!!.qualite!!.referentielSource)
        }
    }

    @Test
    fun `094 - sans position mesuree, referentiel_source ecrit null explicite, derniere cle`() {
        val t = sansPosition.versJson()
        val o = json(t)
        assertTrue(o.has("referentiel_source"), t)
        assertEquals(nulle, o.get("referentiel_source"), t)
        assertEquals("referentiel_source", o.keySet().last(), t)
    }

    @Test
    fun `094 - une valeur v3 ecrite avant la 094, sans la cle, se relit avec un referentiel null, jamais devine`() {
        for (code in listOf("P35-01", "P35-04")) {
            val avec = fixtureV3.getValue(code)
            val sans = Regex(",\"referentiel_source\":(\"[A-Z0-9]+\"|null)").replace(avec, "")
            assertTrue(sans != avec && "referentiel_source" !in sans, "banc : la clé est retirée ($code)")
            val (lu, traces) = lireTrace(sans)
            val c = assertNotNull(lu, "$code sans la clé illisible : $sans")
            assertEquals(emptyList(), traces, code)
            val q = assertNotNull(c.qualite, "$code : la qualité du fix de la 091 reste lue")
            assertNull(q.referentielSource, "$code : référentiel inconnu, jamais déduit de source_gnss")
            assertEquals(json(avec).get("source_gnss").asString, q.sourceGnss)
        }
        val p3502 = Regex(",\"referentiel_source\":null").replace(fixtureV3.getValue("P35-02"), "")
        assertEquals(sansPosition, ValeurControle.lire(p3502), "P35-02 sans la clé")
    }

    @Test
    fun `094 - v1 et v2 relues - referentiel null, et reecrites en v3 avec referentiel_source null`() {
        for (ancien in listOf(exempleV1, exempleV2, fixtureV3.getValue("P35-03"))) {
            val c = assertNotNull(ValeurControle.lire(ancien), ancien)
            assertNull(c.qualite?.referentielSource)
            val o = json(c.versJson())
            assertTrue(o.has("referentiel_source"), "null explicite, comme simulee : ${c.versJson()}")
            assertEquals(nulle, o.get("referentiel_source"), c.versJson())
        }
    }

    @Test
    fun `094 - referentiel_source hors domaine, valeur illisible et tracee`() {
        val base = fixtureV3.getValue("P35-01")
        val cle = "\"referentiel_source\":\"RGF93\""
        assertTrue(cle in base, "banc : $base")
        for (faux in listOf("\"rgf93\"", "\"RGF 93\"", "\"ETRS89\"", "\"WGS 84\"", "\"EPSG:4326\"", "\"\"", "2154", "4326", "true")) {
            val t = base.replace(cle, "\"referentiel_source\":$faux")
            val (lu, traces) = lireTrace(t)
            assertNull(lu, "referentiel_source = $faux : $t")
            assertEquals(1, traces.size, t)
        }
    }

    @Test
    fun `094 - sans position mesuree mais un referentiel dit, valeur incoherente ignoree`() {
        val base = fixtureV3.getValue("P35-02")
        for (r in listOf("RGF93", "WGS84")) {
            val t = base.replace("\"referentiel_source\":null", "\"referentiel_source\":\"$r\"")
            assertTrue(t != base, "banc : $base")
            val (lu, traces) = lireTrace(t)
            assertNull(lu, t)
            assertEquals(1, traces.size, t)
        }
    }

    @Test
    fun `094 - la session ecrit au journal le referentiel dit a l'arrivee`() {
        for (r in listOf("RGF93", "WGS84", null)) with(BancSession()) {
            val q = QualiteFix(precisionM = 0.03, sourceGnss = "recepteur_rtk_fixe", simulee = false, referentielSource = r)
            val o = SessionControle.ouvrir(contexte, journal, horloge, essences, Arrivee.Lambert93(BancSession.POSITION_P1, qualite = q), null, ecoute)
            val s = assertIs<OuvertureControle.Ouverte>(o).session
            s.pointe(1.0, 10.0); s.dire("sain")
            s.direBesoin("aucun"); s.direConcurrence("faible")
            assertIs<SuiteSession.Fait>(s.terminer())
            val brut = journal.deType(FormatValeurs.TYPE_CONTROLE).last().valeur!!
            assertEquals(r, dernierControle().qualite?.referentielSource, brut)
            assertEquals("referentiel_source", json(brut).keySet().last(), brut)
        }
    }

    // ------------------------------------------------------------------ par la session : le journal écrit la v3

    private fun BancSession.dernierControle(): ValeurControle =
        assertNotNull(ValeurControle.lire(journal.deType(FormatValeurs.TYPE_CONTROLE).last().valeur))

    @Test
    fun `la session ecrit au journal le besoin, la concurrence et la qualite du fix dite a l'arrivee`() = with(BancSession()) {
        val q = QualiteFix(precisionM = 1.5, sourceGnss = "recepteur_dgps", simulee = false)
        val o = SessionControle.ouvrir(contexte, journal, horloge, essences, Arrivee.Lambert93(BancSession.POSITION_P1, qualite = q), null, ecoute)
        val s = assertIs<OuvertureControle.Ouverte>(o).session
        s.pointe(1.0, 10.0); s.dire("sain")
        assertIs<SuiteSession.Fait>(s.direBesoin("aucun"))
        assertIs<SuiteSession.Fait>(s.direConcurrence("moyenne"))
        assertIs<SuiteSession.Fait>(s.terminer())
        val brut = journal.deType(FormatValeurs.TYPE_CONTROLE).last().valeur!!
        assertTrue(brut.startsWith("{\"v\":3,"), brut)
        val c = dernierControle()
        assertTrue(c.termine)
        assertEquals("aucun", c.besoin); assertEquals("moyenne", c.concurrence)
        assertEquals(q, c.qualite)
    }

    @Test
    fun `la session ouverte sans position ecrit une qualite du fix inconnue, jamais simulee faux`() = with(BancSession()) {
        val o = SessionControle.ouvrir(contexte, journal, horloge, essences, Arrivee.SansPosition, "P35-01", ecoute)
        val s = assertIs<OuvertureControle.Ouverte>(o).session
        s.pointe(1.0, 10.0); s.dire("mort")
        s.direBesoin("aucun"); s.direConcurrence("forte")
        assertIs<SuiteSession.Fait>(s.terminer())
        val c = dernierControle()
        assertTrue(c.sansPositionMesuree)
        assertNull(c.qualite)
        assertEquals("forte", c.concurrence)
        assertEquals(nulle, json(journal.deType(FormatValeurs.TYPE_CONTROLE).last().valeur!!).get("simulee"))
    }

    @Test
    fun `sans qualite dite a l'arrivee, la position mesuree garde une qualite inconnue - jamais simulee faux invente`() = with(BancSession()) {
        val s = session()
        s.pointe(1.0, 10.0); s.dire("sain")
        s.direBesoin("aucun"); s.direConcurrence("faible")
        assertIs<SuiteSession.Fait>(s.terminer())
        assertNull(dernierControle().qualite)
    }
}
