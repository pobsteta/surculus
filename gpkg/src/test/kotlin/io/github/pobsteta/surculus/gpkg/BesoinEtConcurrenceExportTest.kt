/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.gpkg

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Mission 091 : les **domaines** du besoin et de la concurrence, vérifiés par l'export avant que l'import de sommieR
 * ne refuse toute la tournée au bureau (spec § 5, « Tout ou rien » ; fiche 091, section « Tests », premier point).
 *
 * Domaines de sommieR 0.31.1 (tag v0.31.1, lus en lecture seule) :
 * - `besoin` = un code de la couche `codes` de la tournée, ou « aucun » (`R/registre6.R` l. 137-138 et 178-179 ;
 *   `R/travaux_qfield.R` l. 61-64, qui ajoute « aucun » à la couche `codes`) ;
 * - `concurrence` ∈ faible, moyenne, forte (`SOMMIER_CONCURRENCES`, `R/registre6.R` l. 64).
 * `valider_choix` compare tel quel (`R/valider.R` l. 79-86) : aucune casse n'est corrigée.
 *
 * **Aucune liste de codes n'est embarquée** (CLAUDE.md) : les codes permis sont lus dans la couche `codes` de la tournée
 * d'essai ([TourneeDeTest.couchey], qui porte PL et RG, et pas « aucun »). Un code « inconnu » est fabriqué et vérifié
 * absent de cette couche.
 *
 * Écrit par le `testeur` sans lire le code de l'export. Lu pour l'API seulement : `ExportTravauxTest` (aides `refus`,
 * `exporter`), `TourneeDeTest.saisiesDeLaJournee`, les classes publiques de `Saisies.kt` (`ControleSaisi.besoin`,
 * `ControleSaisi.concurrence`, `PositionMesuree`) et de `Tournee.kt` (`Tournee.codesTravaux`), `RefusGpkg.motif`.
 *
 * **Hypothèse** : le motif d'une concurrence hors domaine est `hors_referentiel`, comme un état, une cause ou une
 * essence hors référentiel (spec § 5, liste des motifs) ; le `dev` le confirme dans son plan.
 *
 * **Non testé ici, car à trancher par Pascal (fiche 091, questions 1 à 4)** : un contrôle sans besoin ni concurrence
 * (obligatoires ou non, question 1 ; anciens contrôles, question 2) ; le domaine de `source_gnss` (question 4).
 */
class BesoinEtConcurrenceExportTest {
    @TempDir
    lateinit var dossier: Path

    private val v = TourneeDeTest
    private val concurrences = listOf("faible", "moyenne", "forte")

    private fun depart(): Path = v.couchey(Files.createDirectories(dossier.resolve("depart-${System.nanoTime()}")))

    /** Codes de la couche `codes` de la tournée de départ, lus dans le fichier (jamais une liste du test). */
    private fun codesDe(tournee: Path): Set<String> = LecteurTournee.lire(tournee).codesTravaux

    private fun exporter(source: Path, saisies: Saisies, nom: String): Path {
        val sortie = dossier.resolve(nom)
        ExportTravaux.exporter(source, sortie, saisies, v.VERSION, v.EXPORTE_LE)
        return sortie
    }

    private fun refus(source: Path, saisies: Saisies): RefusGpkg {
        val sortie = dossier.resolve("refuse.gpkg")
        val r = assertFailsWith<GpkgRefuse>("l'export devait refuser") { ExportTravaux.exporter(source, sortie, saisies, v.VERSION, v.EXPORTE_LE) }.refus
        assertTrue(Files.notExists(sortie), "un refus ne laisse aucun fichier (tout ou rien, spec § 5)")
        return r
    }

    /** La journée d'essai, P35-01 portant [besoin] et [concurrence]. */
    private fun journee(besoin: String?, concurrence: String?): Saisies {
        val s = v.saisiesDeLaJournee()
        return s.copy(controles = s.controles.mapIndexed { i, c -> if (i == 0) c.copy(besoin = besoin, concurrence = concurrence) else c })
    }

    private fun besoinEtConcurrence(gpkg: Path, uuid: String): List<Any?> =
        Contenu.colonnes(gpkg, "controles", "uuid", "besoin", "concurrence").single { it[0] == uuid }.drop(1)

    @Test
    fun `un code de la couche codes et chaque concurrence du domaine sont ecrits tels quels`() {
        val d = depart()
        val codes = codesDe(d)
        assertTrue(codes.isNotEmpty(), "banc : la tournée d'essai porte des codes")
        val uuid = v.saisiesDeLaJournee().controles[0].uuid.valeur
        for ((i, code) in codes.sorted().withIndex()) {
            for (c in concurrences) {
                val f = exporter(d, journee(code, c), "code-$i-$c.gpkg")
                assertEquals(listOf<Any?>(code, c), besoinEtConcurrence(f, uuid), "besoin $code, concurrence $c")
            }
        }
    }

    @Test
    fun `aucun est permis meme quand la couche codes ne le porte pas`() {
        val d = depart()
        assertFalse("aucun" in codesDe(d), "banc : la couche codes d'essai ne porte pas « aucun »")
        val f = exporter(d, journee("aucun", "faible"), "aucun.gpkg")
        assertEquals(listOf<Any?>("aucun", "faible"), besoinEtConcurrence(f, v.saisiesDeLaJournee().controles[0].uuid.valeur))
    }

    @Test
    fun `un besoin absent de la couche codes est refuse - hors_referentiel, rien n'est ecrit`() {
        val d = depart()
        val inconnu = "ZZ-INCONNU"
        assertFalse(inconnu in codesDe(d), "banc : le code fabriqué est bien absent de la couche")
        assertEquals("hors_referentiel", refus(d, journee(inconnu, "moyenne")).motif)
    }

    @Test
    fun `un code de la couche dans une autre casse, ou entoure d'espaces, est refuse - jamais normalise`() {
        val d = depart()
        val code = codesDe(d).sorted().first()
        for (variante in listOf(code.lowercase(), " $code", "$code ", "Aucun", "AUCUN")) {
            if (variante == code || variante in codesDe(d)) continue
            assertEquals("hors_referentiel", refus(d, journee(variante, "moyenne")).motif, "besoin « $variante »")
        }
    }

    @Test
    fun `une concurrence hors du domaine faible, moyenne, forte est refusee`() {
        val d = depart()
        for (c in listOf("Forte", "FAIBLE", "élevée", "nulle", "", " moyenne")) {
            assertEquals("hors_referentiel", refus(d, journee("aucun", c)).motif, "concurrence « $c »")
        }
    }

    @Test
    fun `le contrôle sans position mesuree de la journee d'essai garde precision, source, referentiel et simulee NULL`() {
        // Spec § 5, `positions_controles` : sans position mesurée, `precision_m`, `source_gnss`, `referentiel_source`
        // et `simulee` sont NULL ; `sans_position_mesuree` porte seul l'information (087).
        val f = exporter(depart(), v.saisiesDeLaJournee(), "sans-position.gpkg")
        val c2 = v.saisiesDeLaJournee().controles[1].uuid.valeur
        val l = Sqlite.ouvrir(f, lectureSeule = true).use {
            it.lignes("SELECT precision_m, source_gnss, referentiel_source, simulee FROM positions_controles WHERE controle_uuid = '$c2'")
        }.single()
        assertEquals(listOf<Any?>(null, null, null, null), listOf(l["precision_m"], l["source_gnss"], l["referentiel_source"], l["simulee"]))
    }

    @Test
    fun `une position mesuree ecrit sa precision, sa source et simulee tels que saisis`() {
        // Ce que l'export reçoit, il l'écrit : 0,4 m, la source et « simulée » (TourneeDeTest : « telephone », RGF93, vrai).
        val f = exporter(depart(), v.saisiesDeLaJournee(), "avec-position.gpkg")
        val c1 = v.saisiesDeLaJournee().controles[0]
        val attendu = c1.positionMesuree!!
        val l = Sqlite.ouvrir(f, lectureSeule = true).use {
            it.lignes("SELECT precision_m, source_gnss, referentiel_source, simulee FROM positions_controles WHERE controle_uuid = '${c1.uuid.valeur}'")
        }.single()
        assertEquals(attendu.precisionM!!, (l["precision_m"] as Number).toDouble(), 1e-12)
        assertEquals(attendu.sourceGnss, l["source_gnss"])
        assertEquals(attendu.referentielSource, l["referentiel_source"])
        assertEquals(1, (l["simulee"] as Number).toInt(), "simulée vraie → 1")
        // Une position non simulée, dite telle : 0, et la précision n'est jamais 0.
        val s = v.saisiesDeLaJournee()
        val nonSimulee = s.copy(controles = s.controles.mapIndexed { i, c -> if (i == 0) c.copy(positionMesuree = attendu.copy(simulee = false)) else c })
        val g = exporter(depart(), nonSimulee, "non-simulee.gpkg")
        val m = Sqlite.ouvrir(g, lectureSeule = true).use {
            it.lignes("SELECT precision_m, simulee FROM positions_controles WHERE controle_uuid = '${c1.uuid.valeur}'")
        }.single()
        assertEquals(0, (m["simulee"] as Number).toInt())
        assertTrue((m["precision_m"] as Number).toDouble() > 0.0, "precision_m jamais 0 (Darwin Core, ADR 0003)")
    }
}
