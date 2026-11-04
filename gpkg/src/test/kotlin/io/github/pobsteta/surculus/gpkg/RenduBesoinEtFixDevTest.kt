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
import io.github.pobsteta.surculus.core.placette.EmpriseEtFrance
import io.github.pobsteta.surculus.core.placette.Essences
import io.github.pobsteta.surculus.core.reperes.AzimutSaisi
import io.github.pobsteta.surculus.core.reperes.PointDerive
import io.github.pobsteta.surculus.core.reperes.Resultat
import io.github.pobsteta.surculus.core.session.Arrivee
import io.github.pobsteta.surculus.core.session.FixSession
import io.github.pobsteta.surculus.core.session.FormatValeurs
import io.github.pobsteta.surculus.core.session.HorlogeSession
import io.github.pobsteta.surculus.core.session.JournalControle
import io.github.pobsteta.surculus.core.session.LigneCourante
import io.github.pobsteta.surculus.core.session.OuvertureControle
import io.github.pobsteta.surculus.core.session.SessionControle
import io.github.pobsteta.surculus.core.session.SourceAzimut
import io.github.pobsteta.surculus.core.session.SuiteSession
import io.github.pobsteta.surculus.core.session.ValeurControle
import io.github.pobsteta.surculus.core.session.ValeurPointage
import io.github.pobsteta.surculus.gpkg.outil.ExporterScenario
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.nio.file.AccessDeniedException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Mission 091, tests du `dev` (décisions de Pascal du 2026-11-04) : contrôle terminé sans besoin refusé au rendu (question
 * 2), qualité inconnue écrite NULL (jamais « faux »), recalcul des plants avec la déclinaison du journal (point 6), cause
 * vraie d'un rendu impossible (repris de la 090).
 *
 * Coordonnées : Lambert-93 (EPSG:2154), x = Est, y = Nord, mètres ; la tournée d'essai est à Couchey (Côte-d'Or).
 */
class RenduBesoinEtFixDevTest {
    @TempDir
    lateinit var dossier: Path

    private val essences = Essences(ExporterScenario.lireEssences())

    private class Journal : JournalControle {
        class L(val uuid: String, val objet: String, val type: String, var valeur: String?, val remplace: String?)
        val lignes = ArrayList<L>()
        private var n = 0
        override fun nouvelUuid(): String = UUID.nameUUIDFromBytes("rendu-dev-091:${++n}".toByteArray()).toString()
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

    private class Horloge(val ms: Long) : HorlogeSession {
        override fun maintenantUtcMs() = ms
        override fun monotoneMs() = 0L
        override fun fix(): FixSession? = null
    }

    private class Banc(val dossier: Path, val tournee: Tournee, val journal: Journal)

    /** Un contrôle de P35-01 terminé, mesuré à (+0,6 m E, -0,4 m N) du piquet, sans qualité du fix, besoin et concurrence dits. */
    private fun banc(): Banc {
        val d = Files.createDirectories(dossier.resolve("tournee"))
        TourneeDeTest.couchey(d, nom = ImportTournee.NOM_GPKG)
        Files.createDirectories(d.resolve("DCIM"))
        val t = LecteurTournee.lire(d.resolve(ImportTournee.NOM_GPKG))
        val j = Journal()
        val piquet = t.placettes.single { it.code == "P35-01" }.position!!
        assertTrue(EmpriseEtFrance.controler(piquet).enFrance, "le piquet de Couchey tombe en France")
        val o = SessionControle.ouvrir(
            ContexteDeTournee.de(t).copy(operateur = "Agent d'essai"), j, Horloge(Instant.parse("2026-10-20T09:30:00Z").toEpochMilli()), essences,
            Arrivee.Lambert93(PointLambert93(xMetres = piquet.xMetres + 0.6, yMetres = piquet.yMetres - 0.4)),
        )
        val s = assertIs<OuvertureControle.Ouverte>(o).session
        val az = (AzimutSaisi.valider(143.0, UniteAzimut.DEGRES) as Resultat.Accepte).valeur
        assertIs<SuiteSession.Fait>(s.pointer(2.0, az, SourceAzimut.SAISIE, "CHS"))
        assertIs<SuiteSession.Fait>(s.dire("sain", hauteurCm = 35))
        assertIs<SuiteSession.Fait>(s.direBesoin("aucun"))
        assertIs<SuiteSession.Fait>(s.direConcurrence("moyenne"))
        assertIs<SuiteSession.Fait>(s.terminer())
        return Banc(d, t, j)
    }

    private fun convertir(b: Banc) = SaisiesDuJournal.construire(b.tournee, b.journal.lignesCourantes())

    @Test
    fun `un controle termine d'avant la 091, sans besoin, est refuse au rendu avec la faute decidee`() {
        val b = banc()
        // Le contrôle terminé tel qu'un Surculus de la 090 l'a écrit : valeur v2, sans besoin ni concurrence.
        val l = b.journal.lignes.last { it.type == FormatValeurs.TYPE_CONTROLE }
        val v = ValeurControle.lire(l.valeur)!!.copy(besoin = null, concurrence = null)
        l.valeur = v.versJson().replace("{\"v\":3,", "{\"v\":2,")
        val c = convertir(b)
        assertEquals(listOf("P35-01 : contrôle sans besoin ni concurrence : l'annuler et le refaire"), c.fautes.map { it.texte })
        assertTrue(c.saisies.controles.isEmpty())
        val r = RenduTournee.rendre(b.dossier, b.journal.lignesCourantes(), dossier.resolve("t.zip"), "0.0.0", Instant.EPOCH, essences)
        assertIs<ResultatRendu.Refusee>(r)
    }

    @Test
    fun `une qualite du fix inconnue sort NULL au rendu, jamais faux`() {
        val b = banc()
        val m = convertir(b).saisies.controles.single().positionMesuree!!
        assertNull(m.precisionM); assertNull(m.sourceGnss); assertNull(m.simulee)
        val zip = dossier.resolve("t.zip")
        assertIs<ResultatRendu.Rendue>(RenduTournee.rendre(b.dossier, b.journal.lignesCourantes(), zip, "0.0.0", Instant.parse("2026-10-20T12:00:00Z"), essences))
        val sortie = Files.createDirectories(dossier.resolve("zip"))
        Files.newInputStream(zip).use { ImportTournee.extraireZip(it, sortie) }
        val l = Sqlite.ouvrir(sortie.resolve("travaux.gpkg"), lectureSeule = true).use { it.lignes("SELECT precision_m, source_gnss, simulee, sans_position_mesuree FROM positions_controles") }.single()
        assertEquals(listOf<Any?>(null, null, null), listOf(l["precision_m"], l["source_gnss"], l["simulee"]))
        assertEquals(0, (l["sans_position_mesuree"] as Number).toInt())
    }

    /** Réécrit le pointage avec une autre déclinaison et la position qu'elle donne, comme l'aurait fait un autre modèle. */
    private fun autreDeclinaison(b: Banc, ecartDeg: Double, decalageXm: Double = 0.0) {
        val l = b.journal.lignes.single { it.type == FormatValeurs.TYPE_POINTAGE }
        val p = ValeurPointage.lire(l.valeur)!!
        val piquet = b.tournee.placettes.single { it.code == "P35-01" }.position!!
        val az = (AzimutSaisi.valider(p.azimutMag, UniteAzimut.DEGRES) as Resultat.Accepte).valeur
        val d = (PointDerive.deriverAvecDeclinaison(piquet, b.tournee.epsg, p.distanceHorizontaleM, az, LocalDate.parse(p.jourDeclinaison), p.declinaisonDeg + ecartDeg) as Resultat.Accepte).valeur
        l.valeur = p.copy(declinaisonDeg = d.orientation.declinaisonDeg, azimutGeoDeg = d.orientation.azimutGeographiqueDeg, azimutGrilleDeg = d.orientation.azimutGrilleDeg, x = d.position.xMetres + decalageXm, y = d.position.yMetres).versJson()
    }

    @Test
    fun `un pointage fait avec un autre modele magnetique se rend - le recalcul prend la declinaison du journal`() {
        val b = banc()
        autreDeclinaison(b, 0.5) // 0,5° à 2 m : environ 1,7 cm, qu'un recalcul par le modèle prendrait pour une faute
        assertEquals(emptyList(), convertir(b).fautes)
    }

    @Test
    fun `une position du journal decalee de plus d'1 mm reste une faute`() {
        val b = banc()
        autreDeclinaison(b, 0.0, decalageXm = 0.002)
        assertEquals(listOf("P35-01, plant n° 1 : la position du plant ne correspond pas à sa distance et à son azimut : annuler ce pointage et le refaire"), convertir(b).fautes.map { it.texte })
    }

    @Test
    fun `un rendu impossible dit sa vraie cause, en francais`() {
        assertEquals(RenduTournee.TEXTE_TOURNEE_ILLISIBLE, RenduTournee.texteDePanne(GpkgRefuse(RefusGpkg.CoucheAbsente("placettes"))))
        assertEquals(RenduTournee.TEXTE_PLACE, RenduTournee.texteDePanne(IOException("write failed: ENOSPC (No space left on device)")))
        assertEquals(RenduTournee.TEXTE_ACCES_REFUSE, RenduTournee.texteDePanne(AccessDeniedException("/x")))
        assertEquals(RenduTournee.TEXTE_ACCES_REFUSE, RenduTournee.texteDePanne(RuntimeException(SecurityException("refus"))))
        assertEquals(RenduTournee.TEXTE_JOURNAL_ILLISIBLE, RenduTournee.texteDePanne(SQLiteFauxException()))
        assertEquals(RenduTournee.TEXTE_ECRITURE, RenduTournee.texteDePanne(IOException("I/O error")))
        assertEquals(RenduTournee.TEXTE_INATTENDU, RenduTournee.texteDePanne(IllegalStateException("?")))
    }

    /** Une exception dont la classe s'appelle comme celles d'Android (`android.database.sqlite.SQLiteException`). */
    private open class SQLiteException : RuntimeException("database disk image is malformed")
    private class SQLiteFauxException : SQLiteException()
}
