/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.gpkg

import com.google.gson.JsonParser
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.geo.UniteAzimut
import io.github.pobsteta.surculus.core.placette.Essences
import io.github.pobsteta.surculus.core.reperes.AzimutSaisi
import io.github.pobsteta.surculus.core.reperes.Resultat
import io.github.pobsteta.surculus.core.session.Arrivee
import io.github.pobsteta.surculus.core.session.FixSession
import io.github.pobsteta.surculus.core.session.FormatValeurs
import io.github.pobsteta.surculus.core.session.HorlogeSession
import io.github.pobsteta.surculus.core.session.JournalControle
import io.github.pobsteta.surculus.core.session.LigneCourante
import io.github.pobsteta.surculus.core.session.OuvertureControle
import io.github.pobsteta.surculus.core.session.QualiteFix
import io.github.pobsteta.surculus.core.session.SessionControle
import io.github.pobsteta.surculus.core.session.SourceAzimut
import io.github.pobsteta.surculus.core.session.SuiteSession
import io.github.pobsteta.surculus.core.session.ValeurControle
import io.github.pobsteta.surculus.gpkg.outil.ExporterScenario
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Mission 091, décision de Pascal du 2026-11-04, point 2 : un contrôle **ancien**, terminé au journal sans besoin (valeur
 * v1 ou v2, écrite avant la 091), est **refusé au rendu**, faute « P35-01 : contrôle sans besoin ni concurrence :
 * l'annuler et le refaire » ; sa qualité du fix est **NULL, jamais « faux »**.
 *
 * Écrit par le `testeur` sans lire le code du rendu ni du journal. Banc et API : ceux de `RenduBesoinEtFixTest` (journal
 * en mémoire, `RenduTournee.rendre`, `ResultatRendu.Refusee`, `FauteDeSaisie(placette, plant, texte)`) et l'API supposée
 * de `JournalValeursV3Test` (`ValeurControle.besoin`, `concurrence`, `qualite`).
 *
 * Le contrôle ancien est fabriqué ainsi : un contrôle complet est joué par la session (besoin et concurrence dits), puis
 * sa ligne `controle` courante est **ramenée en v2** dans le journal (les cinq clés de la v3 retirées, `"v":2`), comme si
 * elle avait été écrite par un Surculus d'avant la 091. Les lignes `pointage` et `etat` restent celles de la session.
 *
 * **Phrase** : celle de la fiche, au caractère près ; un point final est toléré (la fiche n'en met pas, la phrase de la
 * tournée vide en a un). **Rouge (ne compile pas) jusqu'au `dev`.**
 *
 * Coordonnées : Lambert-93 (EPSG:2154), x = Est, y = Nord, mètres.
 */
class RenduControleAncienTest {
    @TempDir
    lateinit var dossier: Path

    private val essences = Essences(ExporterScenario.lireEssences())
    private val exporteLe: Instant = Instant.parse("2026-10-20T12:00:00Z")

    /** Décision 2 de Pascal (2026-11-04), fiche 091. */
    private fun phrase(code: String) = "$code : contrôle sans besoin ni concurrence : l'annuler et le refaire"

    private val clesV3 = listOf("besoin", "concurrence", "precision_m", "source_gnss", "simulee")

    private class JournalMemoire : JournalControle {
        class L(val uuid: String, val objet: String, val type: String, var valeur: String?, val remplace: String?)
        val lignes = ArrayList<L>()
        private var n = 0
        override fun nouvelUuid(): String = UUID.nameUUIDFromBytes("ancien-091:${++n}".toByteArray()).toString()
        override fun saisir(objetUuid: String, type: String, valeur: String, operateur: String, remplaceUuid: String?, fix: FixSession?): String =
            nouvelUuid().also { lignes += L(it, objetUuid, type, valeur, remplaceUuid) }
        override fun annuler(uuidCible: String, operateur: String) {
            lignes += L(nouvelUuid(), lignes.first { it.uuid == uuidCible }.objet, "annulation", null, uuidCible)
        }
        override fun lignesCourantes(): List<LigneCourante> {
            val actives = lignes.filter { it.type != "annulation" && lignes.none { a -> a.type == "annulation" && a.remplace == it.uuid } }
            return actives.filter { l -> actives.none { it.remplace == l.uuid } }.map { LigneCourante(it.uuid, it.objet, it.type, it.valeur, it.remplace) }
        }
    }

    private class Horloge(var ms: Long) : HorlogeSession {
        override fun maintenantUtcMs() = ms
        override fun monotoneMs() = 0L
        override fun fix(): FixSession? = null
    }

    private val dossierTournee: Path by lazy {
        val d = Files.createDirectories(dossier.resolve("tournee"))
        TourneeDeTest.couchey(d, nom = ImportTournee.NOM_GPKG)
        Files.createDirectories(d.resolve("DCIM"))
        d
    }
    private val tournee: Tournee by lazy { LecteurTournee.lire(dossierTournee.resolve(ImportTournee.NOM_GPKG)) }
    private val journal = JournalMemoire()
    private val horloge = Horloge(0)

    /** Contrôle complet de [code] (deux plants sains), mesuré à 0,6 m E, -0,4 m N du piquet, avec la qualité dite. */
    private fun controler(code: String, visiteLe: String) {
        val contexte = ContexteDeTournee.de(tournee).copy(operateur = "Agent d'essai")
        horloge.ms = Instant.parse(visiteLe).toEpochMilli()
        val p = tournee.placettes.single { it.code == code }.position!!
        val arrivee = Arrivee.Lambert93(PointLambert93(xMetres = p.xMetres + 0.6, yMetres = p.yMetres - 0.4),
            qualite = QualiteFix(precisionM = 0.9, sourceGnss = "recepteur_dgps", simulee = false))
        val s = assertNotNull(SessionControle.ouvrir(contexte, journal, horloge, essences, arrivee) as? OuvertureControle.Ouverte, code).session
        for ((d, az) in listOf(1.2 to 10.0, 2.0 to 100.0)) {
            assertTrue(s.pointer(d, (AzimutSaisi.valider(az, UniteAzimut.DEGRES) as Resultat.Accepte).valeur, SourceAzimut.SAISIE, "CHS") is SuiteSession.Fait)
            assertTrue(s.dire("sain", hauteurCm = 35) is SuiteSession.Fait)
        }
        assertTrue(s.direBesoin("aucun") is SuiteSession.Fait)
        assertTrue(s.direConcurrence("faible") is SuiteSession.Fait)
        assertTrue(s.terminer() is SuiteSession.Fait, "$code terminé")
    }

    /** Ramène en v2 la ligne `controle` courante de [code] : comme écrite par un Surculus d'avant la 091. Rend la valeur v2. */
    private fun rendreAncien(code: String): String {
        val courante = journal.lignesCourantes().single {
            it.type == FormatValeurs.TYPE_CONTROLE && ValeurControle.lire(it.valeur!!)?.codePlacette == code
        }
        val o = JsonParser.parseString(courante.valeur).asJsonObject
        for (k in clesV3) assertNotNull(o.remove(k), "banc : la valeur v3 de $code porte $k")
        o.addProperty("v", 2)
        val v2 = o.toString()
        assertTrue(v2.startsWith("{\"v\":2,"), "banc : $v2")
        journal.lignes.single { it.uuid == courante.uuid }.valeur = v2
        return v2
    }

    private fun rendre(zip: Path): ResultatRendu =
        RenduTournee.rendre(dossierTournee, journal.lignesCourantes(), zip, "0.0.0-rendu-091", exporteLe, essences)

    @Test
    fun `un controle ancien relu du journal - termine, sans besoin ni concurrence, qualite du fix NULL et non fausse`() {
        controler("P35-01", "2026-10-20T09:30:00Z")
        val v2 = rendreAncien("P35-01")
        val c = assertNotNull(ValeurControle.lire(v2), "la valeur v2 se relit (N-1) : $v2")
        assertTrue(c.termine)
        assertNull(c.besoin); assertNull(c.concurrence)
        assertNull(c.qualite, "jamais « simulee: false » inventé pour un contrôle ancien")
    }

    @Test
    fun `un controle ancien termine sans besoin est refuse au rendu, avec la phrase decidee, rien n'est ecrit`() {
        controler("P35-01", "2026-10-20T09:30:00Z")
        controler("P35-02", "2026-10-20T10:15:00Z")
        rendreAncien("P35-01")
        val dest = Files.createDirectories(dossier.resolve("rendu"))
        val zip = dest.resolve("tournee.zip")
        val r = assertNotNull(rendre(zip) as? ResultatRendu.Refusee, "le rendu doit refuser un contrôle sans besoin ni concurrence")
        val f = assertNotNull(r.fautes.singleOrNull { it.placette == "P35-01" }, "une faute sur P35-01 : ${r.fautes}")
        assertEquals(phrase("P35-01"), f.texte.removeSuffix("."), "la phrase décidée par Pascal, au caractère près")
        assertNull(f.plant, "la faute porte sur le contrôle, pas sur un plant")
        assertTrue(r.fautes.none { it.placette == "P35-02" }, "P35-02 a son besoin : aucune faute : ${r.fautes}")
        assertFalse(Files.exists(zip), "aucun .zip écrit")
        Files.list(dest).use { s -> assertTrue(s.noneMatch { it.fileName.toString().startsWith("tournee.zip") }, "aucun fichier partiel") }
        assertFalse(RenduTournee.estRendue(dossierTournee), "la tournée refusée reste ouverte")
    }

    @Test
    fun `chaque controle ancien a sa faute, avec son code de placette`() {
        controler("P35-01", "2026-10-20T09:30:00Z")
        controler("P35-02", "2026-10-20T10:15:00Z")
        rendreAncien("P35-01"); rendreAncien("P35-02")
        val r = assertNotNull(rendre(dossier.resolve("deux.zip")) as? ResultatRendu.Refusee)
        for (code in listOf("P35-01", "P35-02")) {
            val f = assertNotNull(r.fautes.singleOrNull { it.placette == code && it.texte.removeSuffix(".") == phrase(code) }, "$code : ${r.fautes.map { it.texte }}")
            assertNull(f.plant)
        }
    }

    @Test
    fun `temoin - les memes controles avec leur besoin sont rendus`() {
        controler("P35-01", "2026-10-20T09:30:00Z")
        controler("P35-02", "2026-10-20T10:15:00Z")
        val r = rendre(dossier.resolve("temoin.zip"))
        assertTrue(r is ResultatRendu.Rendue, "sans contrôle ancien, le rendu passe : $r")
    }
}
