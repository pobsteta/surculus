package io.github.pobsteta.surculus.core.session

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.github.pobsteta.surculus.core.fixtures.CasFixture
import io.github.pobsteta.surculus.core.fixtures.Comparaison
import io.github.pobsteta.surculus.core.fixtures.Fixtures
import io.github.pobsteta.surculus.core.fixtures.Referentiels
import io.github.pobsteta.surculus.core.geo.EmpriseLambert93
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.placette.EmpriseEtFrance
import io.github.pobsteta.surculus.core.placette.Essences
import io.github.pobsteta.surculus.core.placette.Piquet
import io.github.pobsteta.surculus.core.placette.UuidPlacette
import io.github.pobsteta.surculus.core.placette.Verdict
import io.github.pobsteta.surculus.core.reperes.CodeEpsg
import io.github.pobsteta.surculus.core.reperes.PointGeographique
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.time.Instant

/**
 * Les cas de `fixtures/regles/` rejoués **à travers la session de contrôle** (mission 012-A), et non contre les règles
 * du domaine prises une à une : ouverture à l'arrivée (France, 15 m, code lu), saisies sur le journal, totaux de la
 * session. Les cas que la session ne peut pas rejouer par construction sont listés avec leur raison dans [EXCLUS] :
 * un test vérifie que la liste est exacte (un cas exclu à tort ne passe pas inaperçu).
 *
 * Familles non rejouées ici : `identite` (la session crée elle-même ses UUID de plant : un doublon ne peut pas naître
 * par elle) et `numerotation` (cas de Groma en coordonnées locales, rejoués contre `Numerotation` ; la session s'en
 * sert, testée dans `SessionControleTest`).
 */
class RejeuViaSessionTest {

    companion object {
        /** id du cas -> pourquoi la session ne le rejoue pas. */
        val EXCLUS = mapOf(
            "T15" to "deux états du même UUID dans un contrôle : la session crée un UUID par plant, le cas est hors d'atteinte ; refus rejoué par RejeuReglesPlacetteTest",
            "T2" to "regarnis posés à des contrôles numérotés (jours successifs) : rejoués par RejeuReglesPlacetteTest ; la session le prouve dans SessionControleTest",
            "T3" to "idem T2", "T3b" to "idem T2", "T3c" to "idem T2", "T5" to "idem T2", "T12" to "idem T2", "T14" to "idem T2",
            "G5" to "remplace d'une autre placette : « remplace » ne désigne que le plant courant de la placette, le cas est hors d'atteinte",
            "G6" to "plant remplacé inconnu : idem G5",
            "G7" to "second regarni du même plant : SessionControleTest, « un plant ne peut être remplacé deux fois »",
            "G9" to "remplace_uuid absent : la session lie toujours le regarni à son plant, le cas est hors d'atteinte",
            "G10" to "regarnis posés à des contrôles numérotés : SessionControleTest, « le plant remplacé avant aujourd'hui sort des totaux »",
        )
    }

    private val essences by lazy { Essences(Referentiels.essences) }

    private fun JsonObject.texte(cle: String): String? = get(cle)?.takeUnless { it.isJsonNull }?.asString
    private fun JsonObject.entier(cle: String): Int? = get(cle)?.takeUnless { it.isJsonNull }?.asInt
    private fun JsonObject.objets(cle: String): List<JsonObject> = getAsJsonArray(cle)?.map { it.asJsonObject } ?: emptyList()
    private fun xy(o: JsonObject) = PointLambert93(xMetres = o["x"].asDouble, yMetres = o["y"].asDouble)

    private fun banc(piquets: List<Piquet>, empriseForet: EmpriseLambert93? = null, heure: String = "2026-10-14T09:00:00Z") = BancSession(heure, essences = essences).let { b ->
        val ctx = b.contexte.copy(placettes = piquets.map { PlacetteControlable(it, 3.99) }, empriseForet = empriseForet)
        Triple(b, ctx, b.journal)
    }

    private fun ouvrir(b: BancSession, ctx: ContexteControle, arrivee: Arrivee, codeLu: String? = null) =
        SessionControle.ouvrir(ctx, b.journal, b.horloge, essences, arrivee, codeLu, b.ecoute)

    // ---------------------------------------------------------------- familles

    private fun rattachement(e: JsonObject): JsonObject {
        val piquets = e.objets("piquets_stockes").map { Piquet(UuidPlacette("uuid-${it.texte("code")}"), it.texte("code")!!, xy(it)) }
        val (b, ctx) = banc(piquets)
        val codeLu = if (e["identite_confirmee"].asBoolean) e.texte("code_lu") else null
        return JsonObject().apply {
            when (val r = ouvrir(b, ctx, Arrivee.Lambert93(xy(e.getAsJsonObject("position_mesuree"))), codeLu)) {
                is OuvertureControle.Ouverte -> {
                    val rat = r.session.rattachement
                    addProperty("issue", "rattache")
                    addProperty("placette", rat.piquet.code)
                    addProperty("distance_m", rat.distanceM)
                    addProperty("rattache_par_identite", rat.parIdentite)
                    add("position_controle", JsonObject().apply { addProperty("x", rat.positionControle.xMetres); addProperty("y", rat.positionControle.yMetres) })
                    add("position_mesuree_conservee", JsonObject().apply { addProperty("x", rat.positionMesuree.xMetres); addProperty("y", rat.positionMesuree.yMetres) })
                }
                is OuvertureControle.Refusee -> { addProperty("issue", "refus"); addProperty("motif", r.refus.motif) }
            }
        }
    }

    private fun unControleParJour(e: JsonObject): JsonObject {
        val saisies: List<Pair<String, Instant>> = if (e.has("controles")) {
            e.objets("controles").map { it.texte("placette")!! to Instant.parse(it.texte("saisi_le_utc")) }
        } else {
            e.getAsJsonArray("controles_saisis_le_utc").map { e.texte("placette")!! to Instant.parse(it.asString) }
        }
        val codes = saisies.map { it.first }.distinct()
        val piquets = codes.mapIndexed { k, c -> Piquet(UuidPlacette("uuid-$c"), c, PointLambert93(846_164.76 + 100.0 * k, 6_686_123.26)) }
        val (b, ctx) = banc(piquets)
        val jours = ArrayList<String>()
        val controles = ArrayList<String>()
        saisies.forEachIndexed { k, (placette, instant) ->
            b.horloge.maintenantMs = instant.toEpochMilli()
            val piquet = piquets.first { it.code == placette }
            val s = (ouvrir(b, ctx, Arrivee.Lambert93(piquet.position)) as OuvertureControle.Ouverte).session
            assertTrue(with(b) { s.pointe(1.0, 10.0 + 20.0 * k) } is SuiteSession.Fait)
            jours += s.etat().jour.toString()
            controles += s.etat().controle!!.valeur
        }
        return JsonObject().apply {
            add("jours_paris", JsonArray().apply { jours.forEach { add(it) } })
            addProperty("meme_jour_civil", jours.toSet().size == 1)
            addProperty("controles_ecrits", controles.toSet().size)
            addProperty("le_second_corrige_le_premier", controles.size > 1 && controles[0] == controles[1])
        }
    }

    private fun emprise(e: JsonObject): JsonObject {
        val arrivee: Arrivee
        val piquetPosition: PointLambert93
        var foret: EmpriseLambert93? = null
        when {
            e.has("lat") -> {
                val g = PointGeographique.de(latitudeDeg = e["lat"].asDouble, longitudeDeg = e["lon"].asDouble).valeurOuEchec()
                arrivee = Arrivee.Gnss(g)
                piquetPosition = if (EmpriseEtFrance.enFrance(g)) EmpriseEtFrance.versLambert93(g) else PointLambert93(846_164.76, 6_686_123.26)
            }
            e.has("point_l93") -> {
                val f = e.getAsJsonObject("foret_emprise_l93")
                foret = EmpriseLambert93(f["xmin"].asDouble, f["ymin"].asDouble, f["xmax"].asDouble, f["ymax"].asDouble)
                piquetPosition = xy(e.getAsJsonObject("point_l93"))
                arrivee = Arrivee.Lambert93(piquetPosition)
            }
            else -> {
                piquetPosition = PointLambert93(846_164.76, 6_686_123.26)
                arrivee = Arrivee.Lambert93(xy(e))
            }
        }
        val (b, ctx) = banc(listOf(Piquet(UuidPlacette("uuid-P"), "P", piquetPosition)), foret)
        return JsonObject().apply {
            when (val r = ouvrir(b, ctx, arrivee)) {
                is OuvertureControle.Ouverte -> {
                    addProperty("en_france", true); addProperty("inversion_detectee", false); addProperty("refuse", false)
                    addProperty("dans_emprise", true)
                    val p = r.session.rattachement.positionMesuree
                    addProperty("x_l93", p.xMetres); addProperty("y_l93", p.yMetres); addProperty("x", p.xMetres); addProperty("y", p.yMetres)
                    addProperty("crs", "EPSG:2154")
                }
                is OuvertureControle.Refusee -> {
                    val horsForet = r.refus.code == "point_hors_foret"
                    addProperty("en_france", horsForet)
                    addProperty("inversion_detectee", r.refus.axesInverses)
                    addProperty("refuse", true)
                    if (horsForet) addProperty("dans_emprise", false)
                }
            }
        }
    }

    private fun sessionSurUnePlacette(): Pair<BancSession, SessionControle> {
        val b = BancSession(essences = essences)
        return b to b.session()
    }

    private fun etatsCauses(e: JsonObject): JsonObject {
        val (b, s) = sessionSurUnePlacette()
        val pointe = with(b) { s.pointe(1.0, 10.0, essence = e.texte("essence")) }
        val dit = if (pointe is SuiteSession.Fait) s.dire(e.texte("etat")!!, e.texte("cause_groupe"), e.texte("cause_detail"), e.entier("hauteur_cm")) else pointe
        return JsonObject().apply { addProperty("accepte", pointe is SuiteSession.Fait && dit is SuiteSession.Fait) }
    }

    private fun totaux(e: JsonObject): JsonObject {
        val (b, s) = sessionSurUnePlacette()
        var tousAcceptes = true
        e.objets("plants").forEachIndexed { k, p ->
            check(with(b) { s.pointe(1.0, 5.0 + 30.0 * k, essence = null) } is SuiteSession.Fait)
            val r = s.dire(p.texte("etat")!!, p.texte("cause_groupe"), p.texte("cause_detail"), p.entier("hauteur_cm"))
            if (r !is SuiteSession.Fait) tousAcceptes = false
        }
        val t = (s.etat().totaux as Verdict.Accepte).valeur
        return JsonObject().apply {
            addProperty("refuse", false)
            addProperty("nb_total", t.nbTotal); addProperty("nb_vivants", t.nbVivants); addProperty("nb_abroutis", t.nbAbroutis)
            addProperty("h_moy_cm", t.hMoyCm)
            addProperty("etat_accepte", tousAcceptes)
        }
    }

    private fun regarni(e: JsonObject): JsonObject {
        val (b, s) = sessionSurUnePlacette()
        val p = e.objets("plants").single()
        with(b) { s.pointe(1.0, 10.0, essence = null) }
        s.dire(p.texte("etat")!!)
        return JsonObject().apply { addProperty("accepte", s.remplace() is SuiteSession.Fait) }
    }

    private fun appliquer(famille: String, entree: JsonObject): JsonObject = when (famille) {
        "rattachement-15m" -> rattachement(entree)
        "un-controle-par-jour" -> unControleParJour(entree)
        "emprise-et-france" -> emprise(entree)
        "etats-causes" -> etatsCauses(entree)
        "totaux-controle" -> totaux(entree)
        "regarni" -> regarni(entree)
        else -> error("famille non rejouée par la session : $famille")
    }

    // ---------------------------------------------------------------- tests

    private fun rejouer(famille: String): List<DynamicTest> =
        Fixtures.tous().filter { it.famille == famille }.flatMap { f ->
            f.cas.filter { it.id !in EXCLUS }.map { c: CasFixture ->
                dynamicTest(c.nom) {
                    val ecarts = Comparaison.ecarts(c.attendu, appliquer(famille, c.entree))
                    assertTrue(ecarts.isEmpty(), "${c.id} (source : ${c.source}) : ${ecarts.joinToString("; ")}")
                }
            }
        }

    @TestFactory fun `rattachement a 15 m, par la session`() = rejouer("rattachement-15m")

    @TestFactory fun `un controle par placette et par jour, par la session`() = rejouer("un-controle-par-jour")

    @TestFactory fun `emprise de la foret et France, par la session`() = rejouer("emprise-et-france")

    @TestFactory fun `etats et causes, par la session`() = rejouer("etats-causes")

    @TestFactory fun `totaux du controle, par la session`() = rejouer("totaux-controle")

    @TestFactory fun `regarni, par la session`() = rejouer("regarni")

    @Test
    fun `chaque cas exclu existe dans les fixtures, aucune exclusion n'est un vestige`() {
        val ids = Fixtures.tous().flatMap { f -> f.cas.map { it.id } }.toSet()
        assertTrue(EXCLUS.keys.all { it in ids }, "exclusions inconnues : ${EXCLUS.keys - ids}")
    }

    @Test
    fun `les cas rejoues par la session sont la grande majorite des cas des six familles`() {
        val familles = setOf("rattachement-15m", "un-controle-par-jour", "emprise-et-france", "etats-causes", "totaux-controle", "regarni")
        val cas = Fixtures.tous().filter { it.famille in familles }.flatMap { it.cas }
        val rejoues = cas.count { it.id !in EXCLUS }
        assertTrue(rejoues * 100 >= cas.size * 80, "$rejoues cas rejoués sur ${cas.size}")
    }
}
