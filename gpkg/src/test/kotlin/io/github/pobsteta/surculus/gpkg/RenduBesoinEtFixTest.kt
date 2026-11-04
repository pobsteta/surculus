/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.gpkg

import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.geo.UniteAzimut
import io.github.pobsteta.surculus.core.placette.Essences
import io.github.pobsteta.surculus.core.placette.Verdict
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
import io.github.pobsteta.surculus.gpkg.outil.ExporterScenario
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import java.util.zip.ZipInputStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Mission 091 : besoin, concurrence et qualité du fix, du geste de l'agent au `.zip` rendu (fiche
 * `pilotage/missions/091-besoin-concurrence-et-qualite-du-fix.md`, sections « Tests » et « Repris de la relecture de
 * la 090 » ; spec § 4.3, § 4.8, § 5 ; ADR 0003, ligne « Fix »). Écrit par le `testeur` **sans lire** le code du
 * contrôle, du journal ni du rendu.
 *
 * Lu pour l'API seulement : `RenduTourneeTest` (banc : journal en mémoire, horloge, `RenduTournee.rendre`,
 * `ResultatRendu`, `FauteDeSaisie`), `SessionEtExportCoucheyTest`, `SessionControleTest` (enchaînement d'un regarni :
 * `dire("mort")`, `remplace()`, `pointer`), les signatures publiques de `SessionControle` (`ouvrir`, `pointer`,
 * `dire`, `remplace`, `terminer`, `etat`), `Arrivee`, `SuiteSession`, `Ports.kt`, `Tournee.kt` (`codesTravaux`,
 * `PlantLu`), `Totaux`, `TourneeDeTest`.
 *
 * Domaines (sommieR 0.31.1, lecture seule) : `besoin` = un code de la couche `codes` de la tournée, ou « aucun »
 * (`R/registre6.R` l. 137-138 ; `R/travaux_qfield.R` l. 61-64) ; `concurrence` ∈ faible, moyenne, forte (l. 64). Aucune
 * liste n'est embarquée : les codes sont lus dans la couche `codes` de la tournée d'essai.
 *
 * **Rouge (ne compile pas) jusqu'au `dev`.** API **supposée**, la plus simple possible, que le `dev` confirme ou
 * remplace dans son plan (les noms comptent moins que ce qui est vérifié) :
 *
 * ```kotlin
 * // core.session
 * /** Ce que le récepteur dit de son fix à l'arrivée (spec § 5, positions_controles ; ADR 0003, ligne « Fix »). */
 * data class QualiteFix(val precisionM: Double, val sourceGnss: String, val simulee: Boolean)
 * // Arrivee.Lambert93(position, qualite: QualiteFix? = null) ; même paramètre pour Arrivee.Gnss.
 *
 * class SessionControle {
 *     /** Le travail qui s'impose : un code de la couche `codes` de la tournée, ou « aucun » ;
 *      *  sinon Refus(code = "hors_referentiel"). Dit de nouveau, il remplace le précédent. */
 *     fun direBesoin(code: String): SuiteSession
 *     /** faible, moyenne ou forte ; sinon Refus(code = "hors_referentiel"). */
 *     fun direConcurrence(niveau: String): SuiteSession
 * }
 * ```
 * `ContexteDeTournee.de(tournee)` porte les codes de la tournée jusqu'à la session (le champ n'est pas nommé ici).
 *
 * **Non supposé, car à trancher par Pascal (fiche, questions 1 à 4)** : le moment et le geste de la saisie (le test dit
 * le besoin et la concurrence juste avant « Terminer », ce qui vaut pour toutes les réponses) ; s'ils sont
 * obligatoires (aucun test ne termine un contrôle sans eux, aucun ne vérifie un refus de « Terminer ») ; les contrôles
 * anciens sans besoin ; le format du journal et de la sauvegarde ; d'où vient la source du fix et ses valeurs (le test
 * vérifie seulement que ce que l'arrivée a dit ressort tel quel).
 *
 * Coordonnées : Lambert-93 (EPSG:2154), x = Est, y = Nord, mètres.
 */
class RenduBesoinEtFixTest {
    @TempDir
    lateinit var dossier: Path

    private val essences = Essences(ExporterScenario.lireEssences())
    private val version = "0.0.0-rendu-091"
    private val exporteLe: Instant = Instant.parse("2026-10-20T12:00:00Z")

    /** La phrase décidée par Pascal le 2026-11-03 (fiche 091, « Le constat »). */
    private val phraseTourneeVide = "Aucun contrôle terminé dans cette tournée : rien à rendre."

    // ---------------------------------------------------------------------------------------------------------------
    // Banc : journal en mémoire (règles de projection de la spec § 5), comme RenduTourneeTest.

    private class JournalMemoire : JournalControle {
        class L(val uuid: String, val objet: String, val type: String, val valeur: String?, val remplace: String?)
        val lignes = ArrayList<L>()
        private var n = 0
        override fun nouvelUuid(): String = UUID.nameUUIDFromBytes("rendu-091:${++n}".toByteArray()).toString()
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

    private class Banc(val dossierTournee: Path, val tournee: Tournee, val journal: JournalMemoire, val horloge: Horloge, essences: Essences) {
        val contexte = ContexteDeTournee.de(tournee).copy(operateur = "Agent d'essai")
        private val ess = essences

        fun ouvrir(code: String, visiteLe: String, arrivee: Arrivee): SessionControle {
            horloge.ms = Instant.parse(visiteLe).toEpochMilli()
            val o = if (arrivee == Arrivee.SansPosition) {
                SessionControle.ouvrir(contexte, journal, horloge, ess, arrivee, code)
            } else {
                SessionControle.ouvrir(contexte, journal, horloge, ess, arrivee)
            }
            return assertNotNull(o as? OuvertureControle.Ouverte, "$code : $o").session
        }

        fun piquet(code: String): PointLambert93 = tournee.placettes.single { it.code == code }.position!!

        /** Arrivée mesurée à (+0,6 m est, -0,4 m nord) du piquet, comme le scénario Couchey. */
        fun mesuree(code: String, qualite: QualiteFix?): Arrivee {
            val p = piquet(code)
            return Arrivee.Lambert93(PointLambert93(xMetres = p.xMetres + 0.6, yMetres = p.yMetres - 0.4), qualite = qualite)
        }

        fun dernier(type: String) = journal.lignes.last { it.type == type }.objet
    }

    private fun banc(): Banc {
        val d = Files.createDirectories(dossier.resolve("tournee"))
        TourneeDeTest.couchey(d, nom = ImportTournee.NOM_GPKG)
        Files.createDirectories(d.resolve("DCIM"))
        return Banc(d, LecteurTournee.lire(d.resolve(ImportTournee.NOM_GPKG)), JournalMemoire(), Horloge(0), essences)
    }

    /** Un code de la couche `codes` de la tournée d'essai, autre que « aucun » (lu, jamais écrit dans le test). */
    private fun unCodeDeLaTournee(b: Banc): String {
        val codes = b.tournee.codesTravaux.filter { it != "aucun" }.sorted()
        assertTrue(codes.isNotEmpty(), "banc : la couche codes de la tournée d'essai porte au moins un code")
        return codes.first()
    }

    private fun pointe(s: SessionControle, distanceM: Double, azimutDeg: Double) {
        val az = (AzimutSaisi.valider(azimutDeg, UniteAzimut.DEGRES) as Resultat.Accepte).valeur
        assertIs<SuiteSession.Fait>(s.pointer(distanceM, az, SourceAzimut.SAISIE, "CHS"))
    }

    private fun ok(r: SuiteSession, quoi: String) = assertTrue(r is SuiteSession.Fait, "$quoi : $r")

    /** Contrôle complet : deux plants sains, besoin et concurrence dits, terminé. Rend l'UUID du contrôle. */
    private fun controler(b: Banc, code: String, visiteLe: String, arrivee: Arrivee, besoin: String, concurrence: String): String {
        val s = b.ouvrir(code, visiteLe, arrivee)
        pointe(s, 1.2, 10.0); ok(s.dire("sain", hauteurCm = 35), "sain")
        pointe(s, 2.0, 100.0); ok(s.dire("sain", hauteurCm = 40), "sain")
        ok(s.direBesoin(besoin), "besoin $besoin")
        ok(s.direConcurrence(concurrence), "concurrence $concurrence")
        ok(s.terminer(), "$code terminé")
        return b.dernier(FormatValeurs.TYPE_CONTROLE)
    }

    private fun rendre(b: Banc, zip: Path): ResultatRendu =
        RenduTournee.rendre(b.dossierTournee, b.journal.lignesCourantes(), zip, version, exporteLe, essences)

    private fun rendue(r: ResultatRendu): ResultatRendu.Rendue = assertNotNull(r as? ResultatRendu.Rendue, "rendu refusé : $r")

    private fun gpkgDu(zip: Path, nom: String): Path {
        val sortie = Files.createDirectories(dossier.resolve(nom))
        ZipInputStream(Files.newInputStream(zip)).use { z ->
            while (true) {
                val e = z.nextEntry ?: break
                val cible = sortie.resolve(e.name).normalize()
                assertTrue(cible.startsWith(sortie), "entrée hors du dossier : ${e.name}")
                if (e.isDirectory) Files.createDirectories(cible) else { Files.createDirectories(cible.parent); Files.copy(z, cible) }
            }
        }
        return sortie.resolve("travaux.gpkg")
    }

    private fun parCode(gpkg: Path, vararg colonnes: String): Map<String, List<Any?>> {
        val codes = LecteurTournee.lire(gpkg).placettes.associate { it.uuid to it.code!! }
        return Contenu.colonnes(gpkg, "controles", "placette_uuid", *colonnes).associate { codes.getValue(it[0] as String) to it.drop(1) }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Fiche, « Tests », point 1 : un contrôle terminé porte un besoin (code de la couche ou « aucun ») et une
    // concurrence ; un code absent de la couche est refusé (hors_referentiel).

    @Test
    fun `la session accepte un code de la couche codes, aucun, et les trois concurrences`() {
        val b = banc()
        val s = b.ouvrir("P35-01", "2026-10-20T09:30:00Z", b.mesuree("P35-01", null))
        for (code in b.tournee.codesTravaux.filter { it != "aucun" }.sorted() + "aucun") ok(s.direBesoin(code), "besoin $code")
        for (c in listOf("faible", "moyenne", "forte")) ok(s.direConcurrence(c), "concurrence $c")
    }

    @Test
    fun `un besoin absent de la couche codes est refuse par la session - hors_referentiel, rien n'entre au journal`() {
        val b = banc()
        val s = b.ouvrir("P35-01", "2026-10-20T09:30:00Z", b.mesuree("P35-01", null))
        val inconnu = "ZZ-INCONNU"
        assertFalse(inconnu in b.tournee.codesTravaux, "banc : code fabriqué absent de la couche")
        val code = unCodeDeLaTournee(b)
        for (v in listOf(inconnu, code.lowercase().takeIf { it != code }, " $code", "Aucun", "")) {
            if (v == null) continue
            val avant = b.journal.lignes.size
            val r = s.direBesoin(v)
            assertEquals("hors_referentiel", assertIs<SuiteSession.Refus>(r, "besoin « $v »").code, "besoin « $v »")
            assertEquals(avant, b.journal.lignes.size, "un besoin refusé n'écrit rien au journal (« $v »)")
        }
    }

    @Test
    fun `une concurrence hors du domaine est refusee par la session - hors_referentiel, rien n'entre au journal`() {
        val b = banc()
        val s = b.ouvrir("P35-01", "2026-10-20T09:30:00Z", b.mesuree("P35-01", null))
        for (c in listOf("Forte", "élevée", "nulle", "", " faible")) {
            val avant = b.journal.lignes.size
            assertEquals("hors_referentiel", assertIs<SuiteSession.Refus>(s.direConcurrence(c), "concurrence « $c »").code)
            assertEquals(avant, b.journal.lignes.size, "une concurrence refusée n'écrit rien (« $c »)")
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Fiche, « Tests », point 2 : le rendu écrit besoin et concurrence (l'état de suivi après import est F4, ci/sommier).

    @Test
    fun `le rendu ecrit le besoin et la concurrence dits - aucun et moyenne, un code de la couche et forte`() {
        val b = banc()
        val code = unCodeDeLaTournee(b)
        controler(b, "P35-01", "2026-10-20T09:30:00Z", b.mesuree("P35-01", null), "aucun", "moyenne")
        controler(b, "P35-02", "2026-10-20T10:15:00Z", Arrivee.SansPosition, code, "forte")
        val zip = dossier.resolve("besoin.zip")
        rendue(rendre(b, zip))
        val c = parCode(gpkgDu(zip, "besoin"), "besoin", "concurrence")
        assertEquals(listOf<Any?>("aucun", "moyenne"), c["P35-01"], "P35-01 : « aucun » écrit tel quel, pas laissé vide")
        assertEquals(listOf<Any?>(code, "forte"), c["P35-02"], "P35-02 : le code dit, qui fera passer la placette « à programmer »")
    }

    @Test
    fun `un besoin redit remplace le premier - le dernier dit sort au rendu, un refus ne change rien`() {
        val b = banc()
        val code = unCodeDeLaTournee(b)
        val s = b.ouvrir("P35-01", "2026-10-20T09:30:00Z", b.mesuree("P35-01", null))
        pointe(s, 1.2, 10.0); ok(s.dire("mort"), "mort")
        ok(s.direBesoin("aucun"), "aucun"); ok(s.direConcurrence("faible"), "faible")
        ok(s.direBesoin(code), "besoin corrigé"); ok(s.direConcurrence("forte"), "concurrence corrigée")
        assertIs<SuiteSession.Refus>(s.direBesoin("ZZ-INCONNU"))
        ok(s.terminer(), "terminé")
        val zip = dossier.resolve("corrige.zip")
        rendue(rendre(b, zip))
        assertEquals(listOf<Any?>(code, "forte"), parCode(gpkgDu(zip, "corrige"), "besoin", "concurrence")["P35-01"])
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Fiche, « Tests », point 3 : une position mesurée sort avec precision_m, source_gnss et simulee justes ;
    // sans position mesurée, NULL (087).

    private fun positions(gpkg: Path): Map<String, Map<String, Any?>> =
        Sqlite.ouvrir(gpkg, lectureSeule = true).use {
            it.lignes("SELECT controle_uuid, precision_m, source_gnss, referentiel_source, simulee, sans_position_mesuree FROM positions_controles")
        }.associateBy { it["controle_uuid"] as String }

    @Test
    fun `la position mesuree sort avec la precision, la source et le caractere simule dits a l'arrivee`() {
        val b = banc()
        val simulee = QualiteFix(precisionM = 0.4, sourceGnss = "telephone", simulee = true)
        val vraie = QualiteFix(precisionM = 2.7, sourceGnss = "recepteur_dgps", simulee = false)
        val c1 = controler(b, "P35-01", "2026-10-20T09:30:00Z", b.mesuree("P35-01", simulee), "aucun", "moyenne")
        val c3 = controler(b, "P35-03", "2026-10-20T11:00:00Z", b.mesuree("P35-03", vraie), "aucun", "faible")
        val c2 = controler(b, "P35-02", "2026-10-20T10:15:00Z", Arrivee.SansPosition, "aucun", "faible")
        val zip = dossier.resolve("fix.zip")
        rendue(rendre(b, zip))
        val p = positions(gpkgDu(zip, "fix"))

        val l1 = p.getValue(c1)
        assertEquals(0.4, assertNotNull(l1["precision_m"] as Number?, "precision_m de P35-01 vide (le défaut de la PR #155)").toDouble(), 1e-9, "precision_m de P35-01")
        assertEquals("telephone", l1["source_gnss"], "source_gnss de P35-01 : celle dite à l'arrivée")
        assertEquals(1, (l1["simulee"] as Number?)?.toInt(), "P35-01 : position simulée enregistrée (ADR 0003)")

        val l3 = p.getValue(c3)
        assertEquals(2.7, assertNotNull(l3["precision_m"] as Number?, "precision_m de P35-03 vide").toDouble(), 1e-9, "precision_m de P35-03")
        assertEquals("recepteur_dgps", l3["source_gnss"])
        assertEquals(0, (l3["simulee"] as Number?)?.toInt(), "P35-03 : non simulée, dite telle (pas un faux par défaut)")

        val l2 = p.getValue(c2)
        assertEquals(1, (l2["sans_position_mesuree"] as Number).toInt())
        assertEquals(listOf<Any?>(null, null, null, null), listOf(l2["precision_m"], l2["source_gnss"], l2["referentiel_source"], l2["simulee"]),
            "sans position mesurée : precision_m, source_gnss, referentiel_source, simulee NULL (spec § 5)")
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Fiche, « Tests », point 4 (décision de Pascal du 2026-11-03) : tournée sans contrôle terminé, refusée avec la phrase.

    private fun assertRefuseeTourneeVide(b: Banc, nom: String) {
        val dest = Files.createDirectories(dossier.resolve(nom))
        val zip = dest.resolve("tournee.zip")
        val r = assertNotNull(rendre(b, zip) as? ResultatRendu.Refusee, "une tournée sans contrôle terminé est refusée")
        val f = r.fautes.singleOrNull { it.texte == phraseTourneeVide }
        assertNotNull(f, "la phrase décidée par Pascal, au caractère près : ${r.fautes.map { it.texte }}")
        assertNull(f.placette, "la faute porte sur la tournée, pas sur une placette")
        assertNull(f.plant)
        assertFalse(Files.exists(zip), "aucun .zip écrit")
        Files.list(dest).use { s -> assertTrue(s.noneMatch { it.fileName.toString().startsWith("tournee.zip") }, "aucun fichier partiel") }
        assertFalse(RenduTournee.estRendue(b.dossierTournee), "la tournée refusée reste ouverte")
    }

    @Test
    fun `tournee sans aucune saisie - refusee avec la phrase decidee`() {
        val b = banc()
        assertTrue(b.journal.lignesCourantes().isEmpty(), "banc : journal vide")
        assertRefuseeTourneeVide(b, "vide")
    }

    @Test
    fun `tournee dont le seul controle est commence mais non termine - refusee avec la phrase decidee`() {
        val b = banc()
        val s = b.ouvrir("P35-01", "2026-10-20T09:30:00Z", b.mesuree("P35-01", null))
        pointe(s, 1.2, 10.0); ok(s.dire("sain", hauteurCm = 35), "sain")
        assertTrue(b.journal.lignesCourantes().any { it.type == FormatValeurs.TYPE_CONTROLE }, "banc : un contrôle commencé")
        assertRefuseeTourneeVide(b, "commence")
    }

    @Test
    fun `tournee dont le seul controle termine a ete annule - refusee avec la phrase decidee`() {
        val b = banc()
        val c = controler(b, "P35-01", "2026-10-20T09:30:00Z", b.mesuree("P35-01", null), "aucun", "faible")
        b.journal.lignes.filter { it.objet == c && it.type == FormatValeurs.TYPE_CONTROLE }.reversed().forEach { b.journal.annuler(it.uuid, "Agent d'essai") }
        assertTrue(b.journal.lignesCourantes().none { it.objetUuid == c && it.type == FormatValeurs.TYPE_CONTROLE }, "banc : plus de contrôle courant")
        assertRefuseeTourneeVide(b, "annule")
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Repris de la relecture de la 090 : un rendu avec un regarni saisi au journal.

    @Test
    fun `un regarni saisi au journal sort au rendu, lie au plant mort qu'il remplace`() {
        val b = banc()
        val s = b.ouvrir("P35-01", "2026-10-20T09:30:00Z", b.mesuree("P35-01", null))
        pointe(s, 1.2, 10.0); ok(s.dire("sain", hauteurCm = 35), "sain")
        pointe(s, 2.0, 100.0)
        val mort = s.etat().courant!!
        ok(s.dire("mort", causeGroupe = "animaux"), "mort")
        ok(s.remplace(), "remplace")
        pointe(s, 2.1, 110.0)
        val regarni = s.etat().courant!!
        ok(s.dire("sain", hauteurCm = 20), "regarni sain")
        val totaux = (s.etat().totaux as Verdict.Accepte).valeur
        ok(s.direBesoin("aucun"), "besoin"); ok(s.direConcurrence("moyenne"), "concurrence")
        ok(s.terminer(), "terminé")

        val zip = dossier.resolve("regarni.zip")
        rendue(rendre(b, zip))
        val gpkg = gpkgDu(zip, "regarni")
        val t = LecteurTournee.lire(gpkg)
        val r = assertNotNull(t.plants.singleOrNull { it.uuid == regarni.valeur }, "le regarni est exporté")
        assertTrue(r.regarni, "marqué regarni (spec § 5, plants.regarni)")
        assertEquals(mort.valeur, r.remplaceUuid, "lié au plant mort qu'il remplace (spec § 4.3, § 6)")
        assertEquals(r.placetteUuid, t.plants.single { it.uuid == mort.valeur }.placetteUuid, "même placette")
        val etats = t.etats.associateBy { it.plantUuid }
        assertEquals("mort", etats.getValue(mort.valeur).etat, "le plant remplacé compte mort dans ce contrôle")
        assertEquals("sain", etats.getValue(regarni.valeur).etat)
        val c = parCode(gpkg, "nb_total", "nb_vivants", "nb_abroutis", "h_moy_cm").getValue("P35-01")
        assertEquals(listOf(totaux.nbTotal, totaux.nbVivants, totaux.nbAbroutis, totaux.hMoyCm), c.map { (it as Number?)?.toInt() },
            "les totaux du zip sont ceux que la session a montrés à l'agent")
    }
}
