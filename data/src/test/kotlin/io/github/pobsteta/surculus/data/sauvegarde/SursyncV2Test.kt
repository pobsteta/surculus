package io.github.pobsteta.surculus.data.sauvegarde

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonObject
import com.google.gson.JsonParser
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
 * Mission 088 : la sauvegarde `.sursync` passe à sa version 2 (spec § 4.8 ; `docs/formats/sursync-v1.md`, en-tête
 * « Version suivante décidée » et « Version » ; `docs/formats/journal-valeurs-v2.md`, « Sauvegarde `.sursync` »).
 *
 * Écrit par le `testeur` depuis la fiche, la spec et les formats, **sans lire** le code de la sauvegarde ni de la
 * restauration. L'API appelée est celle des tests existants (`OutilsSursync`, `SauvegardeFixturesTest`,
 * `FormatEtServiceTest`) ; seules les signatures de `LigneJournal` et de `JournalDatabase.construire` ont été lues.
 *
 * **Le lecteur v1** (« un Surculus ancien ») est le lecteur paramétré `versionCourante = 1`, comme dans SV-12 :
 * la règle écrite du format (« un lecteur de version N refuse N+1 : `version_future` ») est appliquée avec N = 1. Cela
 * ne prouve pas que l'APK installé avant la 087 se comporte ainsi : seulement que la règle, telle que le code la porte
 * aujourd'hui, refuse la v2 quand N = 1 (voir la PR).
 *
 * Fixtures : `fixtures/sursync/exemple-v2*`, refaites par `fixtures/sursync/exemple-v2-fabrication.sh`.
 * Rouge jusqu'au `dev` : la version courante reste 1, un lecteur à jour refuse donc la v2 (`version_future`).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SursyncV2Test {
    private val contexte: Context get() = ApplicationProvider.getApplicationContext()
    private val bases = mutableListOf<JournalDatabase>()

    @After fun fermer() { bases.forEach { runCatching { it.close() } } }

    private val racine: File by lazy {
        var d: File? = File(System.getProperty("user.dir")).absoluteFile
        while (d != null && !File(d, "fixtures/sursync").isDirectory) d = d.parentFile
        checkNotNull(d)
    }
    private fun exemple(nom: String) = File(racine, "fixtures/sursync/$nom").readBytes()
    private val v2 get() = exemple("exemple-v2.sursync")

    private val tournee = "COUCHEY-2026"
    private val uuidSansPosition = "019a4f00-0000-7000-8000-000000000001"
    private val uuidAvecPosition = "019a4f00-0000-7000-8000-000000000002"

    /** Les saisies de `exemple-v2.jsonl`, lues ici sans le lecteur testé (Gson), avec la tournée de l'en-tête. */
    private val attendues: List<LigneJournal> by lazy {
        val lignes = File(racine, "fixtures/sursync/exemple-v2.jsonl").readText().trimEnd('\n').split("\n")
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

    private fun controleSansPosition(lignes: List<LigneJournal>): ValeurControle {
        val l = lignes.single { it.uuid == uuidSansPosition }
        return assertNotNull(ValeurControle.lire(l.valeur!!), "valeur v2 illisible : ${l.valeur}")
    }

    /** Fichier v2 bâti à la main à partir de lignes de saisie déjà sérialisées (pied recalculé). */
    private fun fichierV2(saisies: List<String>): ByteArray =
        OutilsSursync.fichier(listOf(OutilsSursync.entete(2, saisies.size).toString()) + saisies)

    private val saisiesTexte: List<String> by lazy { OutilsSursync.lignesTexte(v2).drop(1).dropLast(1) }

    // ------------------------------------------------------------------ une sauvegarde écrite après la 087 est en v2

    // Mission 091, décision 3 de Pascal (2026-11-04) : la sauvegarde passe en v3 (SursyncV3Test). Les tests de ce fichier
    // qui parlaient du « Surculus à jour » parlent désormais d'un lecteur v2 (`versionCourante = 2`) ; la v3 suit N-1.
    @Test fun `la version courante du format est 3 depuis la mission 091`() {
        assertEquals(3, FormatSursync.VERSION_COURANTE)
    }

    @Test fun `le service ecrit une sauvegarde en version 3, meme pour un journal aux seules valeurs v1, nom inchange`() {
        val fichiers = linkedMapOf<String, ByteArray>()
        val stockage = object : StockageSauvegardes {
            override fun noms() = fichiers.keys.toSet()
            override fun ecrire(nom: String, octets: ByteArray) { check(nom !in fichiers); fichiers[nom] = octets }
            override fun supprimer(nom: String) { fichiers.remove(nom) }
        }
        // Journal aux valeurs v1 (fixtures du journal) : depuis la 087 le journal écrit en v2, la sauvegarde aussi.
        val service = ServiceSauvegarde(depot("A", journalDe("journal/projection.json#JR-02")), stockage, "a3f9c2d1", "0.31.1")
        val r = service.sauvegarder(tournee, 1_793_620_600_000L)
        assertEquals("COUCHEY-2026-a3f9c2d1-20261102T115640Z.sursync", r.nom, "nom : décisions de la 024 et de la 009, inchangées")
        assertEquals(3, versionEnTete(fichiers.getValue(r.nom)))
        assertTrue(LectureSursync.lire(fichiers.getValue(r.nom)).accepte)
    }

    @Test fun `un journal avec un controle sans position se sauvegarde en v3 et se relit a la ligne pres`() {
        val fichiers = linkedMapOf<String, ByteArray>()
        val stockage = object : StockageSauvegardes {
            override fun noms() = fichiers.keys.toSet()
            override fun ecrire(nom: String, octets: ByteArray) { check(nom !in fichiers); fichiers[nom] = octets }
            override fun supprimer(nom: String) { fichiers.remove(nom) }
        }
        val service = ServiceSauvegarde(depot("A", attendues), stockage, "a3f9c2d1", "0.31.1")
        val octets = fichiers.getValue(service.sauvegarder(tournee, 1_793_620_600_000L).nom)
        assertEquals(3, versionEnTete(octets))
        val r = LectureSursync.lire(octets)
        assertTrue(r.accepte); assertEquals(emptySet(), r.signalements)
        assertEquals(attendues, r.lignes, "valeur recopiée au caractère près, null explicites compris")
    }

    @Test fun `l'ecriture d'une v2 redonne exactement le contenu de l'exemple publie`() {
        val octets = EcritureSursync.ecrire(EnTeteSursync(2, tournee, "0.31.1", "a3f9c2d1", 1_793_620_600_000L, 2), attendues)
        assertEquals(String(exemple("exemple-v2.jsonl")), OutilsSursync.decompresser(octets))
    }

    // ------------------------------------------------------------------ un Surculus à jour lit la v2 et la v1

    @Test fun `l'exemple v2 publie se lit par un Surculus a jour, sans signalement`() {
        val r = LectureSursync.lire(v2)
        assertTrue(r.accepte, "motif : ${r.motif}"); assertEquals(emptySet(), r.signalements)
        assertEquals(tournee, r.entete?.codeTournee)
        assertEquals(attendues, r.lignes)
    }

    @Test fun `une v1 relue par un lecteur v2 donne exactement ce qu'elle donnait avant`() {
        val jr02 = journalDe("journal/projection.json#JR-02").sortedWith(compareBy({ it.saisiLeUtcMs }, { it.appareil }, { it.sequence }, { it.uuid }))
        val r = LectureSursync.lire(exemple("exemple-v1.sursync"), versionCourante = 2)
        assertTrue(r.accepte, "motif : ${r.motif}"); assertEquals(emptySet(), r.signalements)
        assertEquals(jr02, r.lignes)
        assertTrue(Signalement.FICHIER_TRONQUE in LectureSursync.lire(exemple("exemple-v1-tronque.sursync"), versionCourante = 2).signalements)
        val faux = LectureSursync.lire(exemple("exemple-v1-empreinte-fausse.sursync"), versionCourante = 2)
        assertEquals(setOf(Signalement.EMPREINTE_DIFFERENTE), faux.signalements); assertEquals(2, faux.lignes.size)
    }

    @Test fun `une v1 se restaure par un lecteur v2 comme avant`() {
        val origine = depot("A", journalDe("journal/projection.json#JR-03"))
        val v1 = OutilsSursync.fichierDe(origine.lignes(), 1)
        val cible = depot("B")
        val cr = Restauration.restaurer(v1, cible, versionCourante = 2)
        assertTrue(cr.accepte, "refus : ${cr.refus}")
        assertEquals(origine.lignes(), cible.lignes())
        assertEquals(origine.projection(), cible.projection())
        assertEquals(origine.anomalies(), cible.anomalies())
    }

    @Test fun `un lecteur de version 3 lit encore la v2 et refuse la v1 (N-2)`() {
        assertTrue(LectureSursync.lire(v2, versionCourante = 3).accepte)
        assertEquals(MotifRefus.VERSION_ILLISIBLE, LectureSursync.lire(exemple("exemple-v1.sursync"), versionCourante = 3).motif)
    }

    // ------------------------------------------------------------------ restaurer un contrôle sans position

    @Test fun `une v2 avec un controle sans position se restaure a l'identique, relue apres redemarrage`() {
        val nom = "journal-088-redemarrage.db"
        contexte.deleteDatabase(nom)
        val avant = depot("B", nom = nom)
        val cr = Restauration.restaurer(v2, avant)
        assertTrue(cr.accepte, "refus : ${cr.refus}")
        assertEquals(emptySet(), cr.signalements)
        assertEquals(2, cr.ajoutees)
        bases.removeAt(bases.lastIndex).close() // redémarrage : la base est fermée, puis rouverte depuis le disque

        val apres = depot("B", nom = nom)
        assertEquals(attendues, apres.lignes().sortedBy { it.uuid }, "mêmes lignes, appareil et séquence d'origine, tournée de l'en-tête")
        val c = controleSansPosition(apres.lignes())
        assertTrue(c.sansPositionMesuree, "le drapeau sans_position_mesuree revient")
        assertNull(c.positionMesureeX); assertNull(c.positionMesureeY)
        assertTrue(c.rattacheParIdentite, "sans position mesurée, toujours rattaché par identité (journal-valeurs-v2)")
        assertEquals(846164.76, c.positionX, 0.0); assertEquals(6686123.26, c.positionY, 0.0) // Lambert-93, x = E, y = N, m
        val avecPosition = assertNotNull(ValeurControle.lire(apres.lignes().single { it.uuid == uuidAvecPosition }.valeur!!))
        assertEquals(false, avecPosition.sansPositionMesuree)
        assertEquals(846201.5, avecPosition.positionMesureeX!!, 0.0)
    }

    @Test fun `restaurer une v2 sur un journal local non vide - union sur l'uuid, idempotent`() {
        val locale = journalDe("journal/projection.json#JR-02")
        val cible = depot("B", locale)
        val premier = Restauration.restaurer(v2, cible)
        val second = Restauration.restaurer(v2, cible)
        assertTrue(premier.accepte, "refus : ${premier.refus}")
        assertEquals(2, premier.ajoutees)
        assertEquals(0, second.ajoutees); assertEquals(2, second.dejaPresentes)
        assertEquals((locale + attendues).map { it.uuid }.toSet(), cible.lignes().map { it.uuid }.toSet())
        assertEquals(4, cible.lignes().size, "ni perte ni doublon")
    }

    // ------------------------------------------------------------------ un Surculus ancien refuse la v2

    @Test fun `un lecteur v1 refuse la v2 en entier - version_future, journal local inchange`() {
        assertEquals(MotifRefus.VERSION_FUTURE, LectureSursync.lire(v2, versionCourante = 1).motif)
        val local = depot("B", journalDe("journal/projection.json#JR-01"))
        val avant = local.lignes()
        val cr = Restauration.restaurer(v2, local, versionCourante = 1)
        assertEquals(MotifRefus.VERSION_FUTURE, cr.refus)
        assertEquals(avant, local.lignes(), "rien n'a été changé, en particulier aucune ligne du contrôle avec position")
    }

    @Test fun `une sauvegarde ecrite par un Surculus a jour est refusee par un lecteur v1`() {
        val octets = OutilsSursync.ecrire(attendues) // version courante
        assertEquals(MotifRefus.VERSION_FUTURE, LectureSursync.lire(octets, versionCourante = 1).motif)
    }

    // ------------------------------------------------------------------ versions refusées par un Surculus à jour

    @Test fun `une v4 est refusee en entier - version_future, journal local inchange`() {
        val local = depot("B", journalDe("journal/projection.json#JR-01"))
        val avant = local.lignes()
        val v4 = OutilsSursync.fichier(listOf(OutilsSursync.entete(4, 2).toString()) + saisiesTexte)
        assertEquals(MotifRefus.VERSION_FUTURE, LectureSursync.lire(v4).motif)
        assertEquals(MotifRefus.VERSION_FUTURE, Restauration.restaurer(v4, local).refus)
        assertEquals(avant, local.lignes())
    }

    @Test fun `une version illisible reste version_illisible`() {
        for (v in listOf(null, "abc", "2", -1, 0, 1.5, 2.5)) {
            val local = depot("B", journalDe("journal/projection.json#JR-01"))
            val f = OutilsSursync.fichier(listOf(OutilsSursync.entete(v, 2).toString()) + saisiesTexte)
            assertEquals(MotifRefus.VERSION_ILLISIBLE, Restauration.restaurer(f, local).refus, "version $v")
            assertEquals(1, local.lignes().size, "version $v : journal local inchangé")
        }
        assertEquals(MotifRefus.VERSION_ILLISIBLE, LectureSursync.lire(ByteArray(0)).motif, "0 octet")
    }

    // ------------------------------------------------------------------ fichiers abîmés en v2 : mêmes règles qu'en v1

    @Test fun `v2 a empreinte fausse - avertissement, toutes les lignes restaurees, le controle sans position intact`() {
        val local = depot("B")
        val cr = Restauration.restaurer(exemple("exemple-v2-empreinte-fausse.sursync"), local)
        assertTrue(cr.accepte, "refus : ${cr.refus}")
        assertEquals(setOf(Signalement.EMPREINTE_DIFFERENTE), cr.signalements)
        assertEquals(2, local.lignes().size)
        assertTrue(controleSansPosition(local.lignes()).sansPositionMesuree)
    }

    @Test fun `v2 tronquee - fichier_tronque, lignes lisibles restaurees, rien d'invente`() {
        val r = LectureSursync.lire(exemple("exemple-v2-tronque.sursync"))
        assertTrue(r.accepte, "motif : ${r.motif}")
        assertTrue(Signalement.FICHIER_TRONQUE in r.signalements)
        assertEquals(attendues.take(r.lignes.size), r.lignes)
    }

    @Test fun `v2 coupee au milieu de la ligne du controle avec position - le controle sans position reste`() {
        val coupe = listOf(saisiesTexte[0], saisiesTexte[1].take(saisiesTexte[1].length / 2))
        val local = depot("B")
        val cr = Restauration.restaurer(fichierV2(coupe), local)
        assertTrue(cr.accepte)
        assertEquals(setOf(Signalement.LIGNE_ILLISIBLE), cr.signalements)
        assertEquals(listOf(uuidSansPosition), local.lignes().map { it.uuid })
    }

    @Test fun `v2 - saisie incomplete ou type hors liste refusee et signalee, les autres restaurees`() {
        val incomplete = OutilsSursync.json(saisiesTexte[1]).apply { remove("age_fix_ms") }.toString()
        val horsListe = OutilsSursync.json(saisiesTexte[1]).apply { addProperty("type", "inventaire") }.toString()
        for (fautive in listOf(incomplete, horsListe)) {
            val r = LectureSursync.lire(fichierV2(listOf(saisiesTexte[0], fautive)))
            assertTrue(r.accepte)
            assertEquals(setOf(Signalement.LIGNE_ILLISIBLE), r.signalements, fautive)
            assertEquals(listOf(uuidSansPosition), r.lignes.map { it.uuid })
        }
    }

    @Test fun `v2 - meme uuid deux fois, identique ignore, different refuse`() {
        val ident = LectureSursync.lire(fichierV2(listOf(saisiesTexte[0], saisiesTexte[0], saisiesTexte[1])))
        assertTrue(ident.accepte); assertEquals(emptySet(), ident.signalements); assertEquals(1, ident.lignesIgnorees)
        assertEquals(attendues, ident.lignes)

        // Même uuid, contenu différent : le contrôle « sans position » repris avec une position mesurée.
        val autre = saisiesTexte[0].replace(
            "\\\"position_mesuree_x\\\":null,\\\"position_mesuree_y\\\":null,\\\"rattache_par_identite\\\":true,\\\"sans_position_mesuree\\\":true",
            "\\\"position_mesuree_x\\\":846165.0,\\\"position_mesuree_y\\\":6686124.0,\\\"rattache_par_identite\\\":false,\\\"sans_position_mesuree\\\":false",
        )
        check(autre != saisiesTexte[0])
        val diff = LectureSursync.lire(fichierV2(listOf(saisiesTexte[0], autre, saisiesTexte[1])))
        assertTrue(diff.accepte)
        assertEquals(listOf(uuidSansPosition), diff.uuidRefuses)
        assertEquals(setOf(Signalement.UUID_CONTENU_DIFFERENT), diff.signalements)
        assertEquals(listOf(uuidAvecPosition), diff.lignes.map { it.uuid })
    }

    @Test fun `v2 - une ligne d'une autre tournee est refusee`() {
        val autre = OutilsSursync.json(saisiesTexte[1]).apply { addProperty("code_tournee", "AUTRE-2026") }.toString()
        val r = LectureSursync.lire(fichierV2(listOf(saisiesTexte[0], autre)))
        assertTrue(r.accepte)
        assertEquals(setOf(Signalement.LIGNE_AUTRE_TOURNEE), r.signalements)
        assertEquals(listOf(uuidSansPosition), r.lignes.map { it.uuid })
    }
}
