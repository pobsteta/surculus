/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.data.sauvegarde

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.pobsteta.surculus.core.session.QualiteFix
import io.github.pobsteta.surculus.core.session.ValeurControle
import io.github.pobsteta.surculus.data.journal.LigneJournal
import io.github.pobsteta.surculus.data.journal.db.HorlogeFixe
import io.github.pobsteta.surculus.data.journal.db.JournalDatabase
import io.github.pobsteta.surculus.data.journal.db.JournalRepository
import io.github.pobsteta.surculus.data.sauvegarde.OutilsSursync.journalDe
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Mission 091, décision de Pascal du 2026-11-04, point 3 : la sauvegarde `.sursync` passe en **v3**, partagée avec les
 * valeurs v3 du journal ; elle suit **N-1** (un lecteur v3 lit v2 et v3) ; un Surculus ancien (lecteur v2) refuse une v3
 * avec `version_future` et ne touche à rien (règle de la 085 : refuser, jamais perdre sans le dire).
 *
 * Écrit par le `testeur` sans lire le code de la sauvegarde, de la restauration ni du journal. API : celle de
 * `SursyncV2Test` (mission 088 : `LectureSursync.lire(octets, versionCourante)`, `Restauration.restaurer(octets, depot,
 * versionCourante)`, `EcritureSursync.ecrire`, `EnTeteSursync`, `ServiceSauvegarde`, `OutilsSursync`) ; pour les valeurs,
 * l'API supposée de `JournalValeursV3Test` (`ValeurControle.besoin`, `concurrence`, `qualite: QualiteFix?`).
 *
 * Le « Surculus ancien » est le lecteur paramétré `versionCourante = 2`, comme le lecteur v1 de la 088 : cela prouve la
 * règle telle que le code la porte, pas le comportement de l'APK installé avant la 091.
 *
 * Fixtures : `fixtures/sursync/exemple-v3*`, refaites par `fixtures/sursync/exemple-v3-fabrication.sh`.
 * Rouge jusqu'au `dev` : la version courante reste 2 (un lecteur à jour refuse la v3) et `QualiteFix` n'existe pas.
 * Coordonnées : Lambert-93 (EPSG:2154), x = Est, y = Nord, mètres.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SursyncV3Test {
    private val contexte: Context get() = ApplicationProvider.getApplicationContext()
    private val bases = mutableListOf<JournalDatabase>()

    @After fun fermer() { bases.forEach { runCatching { it.close() } } }

    private val racine: File by lazy {
        var d: File? = File(System.getProperty("user.dir")).absoluteFile
        while (d != null && !File(d, "fixtures/sursync").isDirectory) d = d.parentFile
        checkNotNull(d)
    }
    private fun exemple(nom: String) = File(racine, "fixtures/sursync/$nom").readBytes()
    private val v3 get() = exemple("exemple-v3.sursync")
    private val v2 get() = exemple("exemple-v2.sursync")

    private val tournee = "COUCHEY-2026"

    /** Les saisies de `exemple-v3.jsonl`, lues ici sans le lecteur testé (Gson), avec la tournée de l'en-tête. */
    private val attendues: List<LigneJournal> by lazy {
        val lignes = File(racine, "fixtures/sursync/exemple-v3.jsonl").readText().trimEnd('\n').split("\n")
        lignes.drop(1).dropLast(1).map { JsonParser.parseString(it).asJsonObject.versLigne() }
    }

    private fun JsonObject.versLigne(): LigneJournal {
        fun s(k: String) = get(k).takeUnless { it.isJsonNull }?.asString
        fun l(k: String) = get(k).takeUnless { it.isJsonNull }?.asLong
        return LigneJournal(
            uuid = s("uuid")!!, objetUuid = s("objet_uuid")!!, type = s("type")!!, valeur = s("valeur"),
            remplaceUuid = s("remplace_uuid"), operateur = s("operateur")!!, appareil = s("appareil")!!,
            sequence = l("sequence")!!, saisiLeUtcMs = l("saisi_le_utc_ms")!!, heureGnssMs = l("heure_gnss_ms"),
            monotoneMs = l("monotone_ms")!!, demarrage = l("demarrage")!!, ageFixMs = l("age_fix_ms"), codeTournee = tournee,
        )
    }

    private fun depot(appareil: String, lignes: List<LigneJournal> = emptyList(), nom: String? = null): JournalRepository {
        val db = JournalDatabase.construire(contexte, nom).allowMainThreadQueries().build().also { bases += it }
        return JournalRepository(db, appareil, HorlogeFixe(), tourneeObligatoire = false).also { d -> lignes.forEach(d::recevoir) }
    }

    private fun versionEnTete(octets: ByteArray): Int =
        OutilsSursync.json(OutilsSursync.lignesTexte(octets).first()).get("version").asInt

    private fun stockage() = object : StockageSauvegardes {
        val fichiers = linkedMapOf<String, ByteArray>()
        override fun noms() = fichiers.keys.toSet()
        override fun ecrire(nom: String, octets: ByteArray) { check(nom !in fichiers); fichiers[nom] = octets }
        override fun supprimer(nom: String) { fichiers.remove(nom) }
    }

    /** Les contrôles relus du journal, par code de placette. */
    private fun controles(lignes: List<LigneJournal>): Map<String, ValeurControle> =
        lignes.filter { it.type == "controle" }.map { assertNotNull(ValeurControle.lire(it.valeur!!), "valeur illisible : ${it.valeur}") }
            .associateBy { it.codePlacette }

    // ------------------------------------------------------------------ la sauvegarde s'écrit en v3

    @Test fun `l'ecriture d'une v3 redonne exactement le contenu de l'exemple publie`() {
        val octets = EcritureSursync.ecrire(EnTeteSursync(3, tournee, "0.31.1", "a3f9c2d1", 1_793_707_900_000L, 4), attendues)
        assertEquals(String(exemple("exemple-v3.jsonl")), OutilsSursync.decompresser(octets))
    }

    @Test fun `le service ecrit en v3 un journal aux valeurs v2 et v3, relu a la ligne pres`() {
        val st = stockage()
        val service = ServiceSauvegarde(depot("A", attendues), st, "a3f9c2d1", "0.31.1")
        val octets = st.fichiers.getValue(service.sauvegarder(tournee, 1_793_707_900_000L).nom)
        assertEquals(3, versionEnTete(octets))
        val r = LectureSursync.lire(octets)
        assertTrue(r.accepte, "motif : ${r.motif}"); assertEquals(emptySet(), r.signalements)
        assertEquals(attendues, r.lignes, "valeurs recopiées au caractère près, null explicites compris")
    }

    // ------------------------------------------------------------------ un Surculus à jour lit la v3 et la v2 (N-1)

    @Test fun `l'exemple v3 publie se lit par un Surculus a jour, sans signalement`() {
        val r = LectureSursync.lire(v3)
        assertTrue(r.accepte, "motif : ${r.motif}"); assertEquals(emptySet(), r.signalements)
        assertEquals(tournee, r.entete?.codeTournee)
        assertEquals(attendues, r.lignes)
    }

    @Test fun `une v2 se relit par un Surculus a jour, et par un lecteur v3 explicite`() {
        for (r in listOf(LectureSursync.lire(v2), LectureSursync.lire(v2, versionCourante = 3))) {
            assertTrue(r.accepte, "motif : ${r.motif}"); assertEquals(emptySet(), r.signalements)
            assertEquals(2, r.lignes.size)
        }
    }

    @Test fun `une v2 se restaure dans un journal a jour, valeurs v2 intactes`() {
        val cible = depot("B")
        val cr = Restauration.restaurer(v2, cible)
        assertTrue(cr.accepte, "refus : ${cr.refus}")
        assertEquals(2, cr.ajoutees)
        val c = controles(cible.lignes())
        assertTrue(c.getValue("P35-01").sansPositionMesuree)
        assertNull(c.getValue("P35-01").qualite, "contrôle v2 : qualité du fix inconnue, jamais « faux »")
        assertNull(c.getValue("P35-02").besoin)
    }

    // ------------------------------------------------------------------ restaurer une v3 : rien n'est perdu

    @Test fun `une v3 se restaure a l'identique, relue apres redemarrage - besoin, concurrence et qualite du fix intacts`() {
        val nom = "journal-091-redemarrage.db"
        contexte.deleteDatabase(nom)
        val avant = depot("B", nom = nom)
        val cr = Restauration.restaurer(v3, avant)
        assertTrue(cr.accepte, "refus : ${cr.refus}")
        assertEquals(emptySet(), cr.signalements)
        assertEquals(4, cr.ajoutees)
        bases.removeAt(bases.lastIndex).close() // redémarrage : la base est fermée, puis rouverte depuis le disque

        val apres = depot("B", nom = nom)
        assertEquals(attendues, apres.lignes().sortedBy { it.uuid }, "mêmes lignes, valeurs au caractère près")
        val c = controles(apres.lignes())
        val p1 = c.getValue("P35-01")
        assertEquals("aucun", p1.besoin); assertEquals("moyenne", p1.concurrence)
        assertEquals(QualiteFix(precisionM = 0.02, sourceGnss = "recepteur_rtk_fixe", simulee = false), p1.qualite)
        val p2 = c.getValue("P35-02")
        assertEquals("RG", p2.besoin); assertEquals("forte", p2.concurrence)
        assertTrue(p2.sansPositionMesuree); assertNull(p2.qualite, "sans position mesurée : qualité NULL")
        val p3 = c.getValue("P35-03")
        assertTrue(p3.termine); assertNull(p3.besoin); assertNull(p3.concurrence); assertNull(p3.qualite)
        assertEquals(QualiteFix(precisionM = 3.8, sourceGnss = "telephone", simulee = true), c.getValue("P35-04").qualite)
    }

    @Test fun `restaurer une v3 sur un journal local non vide - union sur l'uuid, idempotent`() {
        val locale = journalDe("journal/projection.json#JR-02")
        val cible = depot("B", locale)
        val premier = Restauration.restaurer(v3, cible)
        val second = Restauration.restaurer(v3, cible)
        assertTrue(premier.accepte, "refus : ${premier.refus}")
        assertEquals(4, premier.ajoutees)
        assertEquals(0, second.ajoutees); assertEquals(4, second.dejaPresentes)
        assertEquals(locale.size + 4, cible.lignes().size, "ni perte ni doublon")
    }

    // ------------------------------------------------------------------ un Surculus ancien refuse la v3

    @Test fun `un lecteur v2 refuse la v3 en entier - version_future, journal local inchange`() {
        assertEquals(MotifRefus.VERSION_FUTURE, LectureSursync.lire(v3, versionCourante = 2).motif)
        val local = depot("B", journalDe("journal/projection.json#JR-01"))
        val avant = local.lignes()
        val cr = Restauration.restaurer(v3, local, versionCourante = 2)
        assertEquals(MotifRefus.VERSION_FUTURE, cr.refus)
        assertEquals(avant, local.lignes(), "rien n'a été changé, pas même les contrôles v2 que la v3 porte")
    }

    @Test fun `une sauvegarde ecrite par un Surculus a jour est refusee par un lecteur v2`() {
        val octets = OutilsSursync.ecrire(attendues) // version courante
        assertEquals(MotifRefus.VERSION_FUTURE, LectureSursync.lire(octets, versionCourante = 2).motif)
    }

    // ------------------------------------------------------------------ fichiers abîmés en v3 : mêmes règles

    @Test fun `v3 a empreinte fausse - avertissement, toutes les lignes restaurees`() {
        val local = depot("B")
        val cr = Restauration.restaurer(exemple("exemple-v3-empreinte-fausse.sursync"), local)
        assertTrue(cr.accepte, "refus : ${cr.refus}")
        assertEquals(setOf(Signalement.EMPREINTE_DIFFERENTE), cr.signalements)
        assertEquals(4, local.lignes().size)
        assertEquals("RG", controles(local.lignes()).getValue("P35-02").besoin, "le contrôle non touché reste intact")
    }

    @Test fun `v3 tronquee - fichier_tronque, lignes lisibles restaurees, rien d'invente`() {
        val r = LectureSursync.lire(exemple("exemple-v3-tronque.sursync"))
        assertTrue(r.accepte, "motif : ${r.motif}")
        assertTrue(Signalement.FICHIER_TRONQUE in r.signalements)
        assertEquals(attendues.take(r.lignes.size), r.lignes)
    }
}
