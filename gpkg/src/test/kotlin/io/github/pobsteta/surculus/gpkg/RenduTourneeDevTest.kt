package io.github.pobsteta.surculus.gpkg

import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.placette.Essences
import io.github.pobsteta.surculus.core.placette.RefusPlacette
import io.github.pobsteta.surculus.core.placette.UuidPlacette
import io.github.pobsteta.surculus.core.placette.UuidPlant
import io.github.pobsteta.surculus.core.reperes.AzimutSaisi
import io.github.pobsteta.surculus.core.reperes.CodesUniteAzimut
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
import io.github.pobsteta.surculus.gpkg.outil.ExporterScenario
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.zip.ZipInputStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Mission 090, tests du `dev` (en plus de ceux du testeur, `RenduTourneeTest`) : décisions de Pascal du 2026-11-03 sur le
 * plan (contrôle non terminé refusé, plants d'un contrôle annulé non exportés, zip à liste fixe, « Rouvrir »), une faute
 * par placette, textes des fautes sans motif technique ni UUID, échec d'écriture sans reste.
 * Coordonnées : Lambert-93 (EPSG:2154), x = Est, y = Nord, mètres.
 */
class RenduTourneeDevTest {
    @TempDir
    lateinit var dossier: Path

    private val essences = Essences(ExporterScenario.lireEssences())
    private val exporteLe = Instant.parse("2026-10-20T12:00:00Z")

    private class JournalMemoire : JournalControle {
        class L(val uuid: String, val objet: String, val type: String, val valeur: String?, val remplace: String?)
        val lignes = ArrayList<L>()
        private var n = 0
        override fun nouvelUuid(): String = UUID.nameUUIDFromBytes("rendu-dev-090:${++n}".toByteArray()).toString()
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

    private fun tournee(nom: String = "t"): Path {
        val d = Files.createDirectories(dossier.resolve(nom))
        TourneeDeTest.couchey(d, nom = ImportTournee.NOM_GPKG)
        Files.createDirectories(d.resolve("DCIM"))
        return d
    }

    /**
     * Contrôle de [code] le [visiteLe], deux plants sains (1,5 m à 30° et 2,0 m à 200°, degrés), mesuré à 0,5 m E du
     * piquet ; [terminer] faux laisse le contrôle commencé. Rend l'UUID du contrôle (`null` s'il n'est pas écrit).
     */
    private fun controler(d: Path, journal: JournalMemoire, code: String, visiteLe: String, terminer: Boolean = true, essence: String = "CHS"): String {
        val t = LecteurTournee.lire(d.resolve(ImportTournee.NOM_GPKG))
        val piquet = t.placettes.single { it.code == code }.position!!
        val horloge = Horloge(Instant.parse(visiteLe).toEpochMilli())
        val o = SessionControle.ouvrir(ContexteDeTournee.de(t).copy(operateur = "Agent"), journal, horloge, essences,
            Arrivee.Lambert93(PointLambert93(xMetres = piquet.xMetres + 0.5, yMetres = piquet.yMetres)))
        val s = assertNotNull(o as? OuvertureControle.Ouverte, "$o").session
        val deg = (CodesUniteAzimut.lire("deg") as Resultat.Accepte).valeur
        for ((dist, az) in listOf(1.5 to 30.0, 2.0 to 200.0)) {
            assertTrue(s.pointer(dist, (AzimutSaisi.valider(az, deg) as Resultat.Accepte).valeur, SourceAzimut.SAISIE, essence) is SuiteSession.Fait)
            assertTrue(s.dire("sain", null, null, 40) is SuiteSession.Fait)
        }
        if (terminer) {
            s.direBesoin("aucun"); s.direConcurrence("faible")
            assertTrue(s.terminer() is SuiteSession.Fait)
        }
        return journal.lignes.last { it.type == FormatValeurs.TYPE_CONTROLE }.objet
    }

    private fun entrees(zip: Path): List<String> = ZipInputStream(Files.newInputStream(zip)).use { z -> generateSequence { z.nextEntry }.map { it.name }.toList() }

    private fun rendre(d: Path, j: JournalMemoire, zip: Path) = RenduTournee.rendre(d, j.lignesCourantes(), zip, "0.0.0-dev", exporteLe, essences)

    @Test
    fun `un controle non termine est refuse - le terminer ou l'annuler - et rien n'est ecrit`() {
        val d = tournee()
        val j = JournalMemoire()
        controler(d, j, "P35-01", "2026-10-20T08:00:00Z")
        controler(d, j, "P35-02", "2026-10-20T09:00:00Z", terminer = false)
        val dest = Files.createDirectories(dossier.resolve("sortie"))
        val r = assertNotNull(rendre(d, j, dest.resolve("t.zip")) as? ResultatRendu.Refusee)
        assertEquals(listOf(FauteDeSaisie("P35-02", null, "P35-02 : contrôle non terminé : le terminer ou l'annuler")), r.fautes)
        Files.list(dest).use { assertEquals(0L, it.count(), "rien n'est écrit") }
        assertFalse(RenduTournee.estRendue(d))
    }

    @Test
    fun `les plants d'un controle annule ne sont pas exportes, ils restent au journal`() {
        val d = tournee()
        val j = JournalMemoire()
        controler(d, j, "P35-01", "2026-10-20T08:00:00Z")
        val c2 = controler(d, j, "P35-02", "2026-10-20T09:00:00Z")
        j.lignes.filter { it.objet == c2 && it.type == FormatValeurs.TYPE_CONTROLE }.reversed().forEach { j.annuler(it.uuid, "Agent") }
        val zip = dossier.resolve("t.zip")
        val r = assertNotNull(rendre(d, j, zip) as? ResultatRendu.Rendue)
        assertEquals(1, r.bilan.controles)
        assertEquals(2, r.bilan.plants, "seuls les deux plants de P35-01")
        assertTrue(j.lignesCourantes().count { it.type == FormatValeurs.TYPE_POINTAGE } == 4, "les pointages de P35-02 restent au journal")
    }

    @Test
    fun `le zip ne porte que travaux gpkg et DCIM - ni ortho, ni projet QGIS, ni la marque`() {
        val d = tournee()
        Files.writeString(d.resolve("ortho.tif"), "ortho")
        Files.writeString(d.resolve("ortho.mbtiles"), "ortho")
        Files.writeString(d.resolve("travaux.qgs"), "<qgis/>")
        Files.writeString(d.resolve("travaux_attachments.zip"), "pj")
        Files.createDirectories(d.resolve("DCIM/sous"))
        Files.writeString(d.resolve("DCIM/sous/a.jpg"), "a")
        val j = JournalMemoire()
        controler(d, j, "P35-01", "2026-10-20T08:00:00Z")
        val zip = dossier.resolve("t.zip")
        assertNotNull(rendre(d, j, zip) as? ResultatRendu.Rendue)
        assertTrue(RenduTournee.estRendue(d))
        val zip2 = dossier.resolve("t2.zip")
        assertNotNull(rendre(d, j, zip2) as? ResultatRendu.Rendue, "rendre une tournée déjà rendue est permis (nouveau zip)")
        assertEquals(listOf("travaux.gpkg", "DCIM/", "DCIM/sous/", "DCIM/sous/a.jpg"), entrees(zip2))
    }

    @Test
    fun `une faute par placette - deux placettes fautives donnent deux lignes, en francais, sans UUID`() {
        val d = tournee()
        val j = JournalMemoire()
        controler(d, j, "P35-01", "2026-10-20T08:00:00Z")
        controler(d, j, "P35-02", "2026-10-20T09:00:00Z")
        // Un plant de chaque placette corrigé au journal avec une essence hors référentiel.
        val premiers = j.lignesCourantes().filter { it.type == FormatValeurs.TYPE_POINTAGE }.groupBy { it.valeur!!.substringAfter("\"placette_uuid\":\"").take(36) }.values.map { it.first() }
        assertEquals(2, premiers.size)
        for (l in premiers) j.saisir(l.objetUuid, l.type, l.valeur!!.replace("\"essence\":\"CHS\"", "\"essence\":\"ZZZ\""), "Agent", l.uuid, null)
        val r = assertNotNull(rendre(d, j, dossier.resolve("t.zip")) as? ResultatRendu.Refusee)
        assertEquals(listOf("P35-01", "P35-02"), r.fautes.map { it.placette }.sortedBy { it }, "${r.fautes}")
        r.fautes.forEach { assertNotNull(it.plant); assertPropre(it.texte) }
    }

    @Test
    fun `rouvrir retire la marque - la tournee redevient ouverte, la copie n'est pas touchee`() {
        val d = tournee()
        val j = JournalMemoire()
        controler(d, j, "P35-01", "2026-10-20T08:00:00Z")
        val avant = Contenu.de(d.resolve(ImportTournee.NOM_GPKG))
        assertNotNull(rendre(d, j, dossier.resolve("Couchey.zip")) as? ResultatRendu.Rendue)
        val marque = Files.readString(d.resolve(RenduTournee.MARQUE))
        assertTrue("\"zip\":\"Couchey.zip\"" in marque && "\"controles\":1" in marque, marque)
        RenduTournee.rouvrir(d)
        assertFalse(RenduTournee.estRendue(d))
        assertEquals(avant, Contenu.de(d.resolve(ImportTournee.NOM_GPKG)))
    }

    @Test
    fun `ecriture impossible - rien n'est laisse et la tournee reste ouverte`() {
        val d = tournee()
        val j = JournalMemoire()
        controler(d, j, "P35-01", "2026-10-20T08:00:00Z")
        val fichier = Files.writeString(dossier.resolve("pas-un-dossier"), "x")
        val r = rendre(d, j, fichier.resolve("t.zip"))
        val i = assertNotNull(r as? ResultatRendu.Impossible, "$r")
        assertPropre(i.texte, forme = false)
        assertFalse(RenduTournee.estRendue(d))
        Files.list(d).use { s -> assertTrue(s.noneMatch { it.fileName.toString().endsWith(".part") }) }
    }

    @Test
    fun `decompter donne les controles et les plants a rendre, sans rien ecrire`() {
        val d = tournee()
        val j = JournalMemoire()
        controler(d, j, "P35-01", "2026-10-20T08:00:00Z")
        controler(d, j, "P35-02", "2026-10-20T09:00:00Z")
        val n = RenduTournee.decompter(d, j.lignesCourantes())
        assertEquals(2, n.controles)
        assertEquals(4, n.plants)
        assertEquals(Instant.parse("2026-10-20T09:00:00Z"), n.derniereSaisie)
        assertFalse(RenduTournee.estRendue(d))
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Textes des fautes : chaque refus de la vérification donne « placette[, plant n° N] : constat : consigne ».

    private val motifs = listOf(
        "uuid_invalide", "uuid_en_double", "melange_reperes", "hors_france", "hors_referentiel", "controle_en_double",
        "jour_incoherent", "totaux_discordants", "Exception", "GpkgRefuse", "RefusGpkg", "null", "EPSG", "x =", "srs",
    )
    private val motifUuid = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

    private fun assertPropre(texte: String, forme: Boolean = true) {
        assertTrue(texte.isNotBlank())
        for (m in motifs) assertFalse(texte.contains(m), "« $m » dans « $texte »")
        assertNull(motifUuid.find(texte), "UUID dans « $texte »")
        if (forme) assertEquals(2, texte.windowed(3).count { it == " : " }, "forme « qui : constat : consigne » : « $texte »")
    }

    @Test
    fun `chaque refus de la verification donne un texte propre qui nomme la placette et le plant`() {
        val ctl = "11111111-1111-4111-8111-111111111111"
        val plant = "22222222-2222-4222-8222-222222222222"
        val etat = "33333333-3333-4333-8333-333333333333"
        val plc = "44444444-4444-4444-8444-444444444444"
        val r = ReperesSaisies(mapOf(ctl to "P35-01"), mapOf(plant to "P35-01"), mapOf(plant to 3), mapOf(etat to plant), mapOf(plc to "P35-01"))
        val refus = listOf(
            RefusGpkg.PointHorsFrance("controles", ctl, 1.0, 2.0, true),
            RefusGpkg.PointHorsFrance("plants", plant, 1.0, 2.0, false),
            RefusGpkg.PointHorsForet("positions_controles", ctl, 1.0, 2.0),
            RefusGpkg.PointHorsForet("plants", plant, 1.0, 2.0),
            RefusGpkg.EssenceInconnue(plant, "ZZZ"),
            RefusGpkg.PlacetteInconnue("controles", ctl, plc),
            RefusGpkg.DeuxControlesMemeJour(plc, LocalDate.of(2026, 10, 20), "UTC"),
            RefusGpkg.TotauxDiscordants(ctl, "a", "b"),
            RefusGpkg.JourIncoherent(ctl, LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 21)),
            RefusGpkg.ReferentielDuControleDifferent(ctl, 9794, 2154),
            RefusGpkg.ReferentielDuPlantDifferent(plant, 9794, 2154),
            RefusGpkg.EtatSurObjetInconnu(etat, "le plant $plant"),
            RefusGpkg.Identite(RefusPlacette.NumeroEnDouble(UuidPlacette(plc), 3)),
            RefusGpkg.Identite(RefusPlacette.PlantRemplaceInconnu(UuidPlant(plant))),
            RefusGpkg.Identite(RefusPlacette.RemplaceObligatoire),
            RefusGpkg.ChampInvalide("positions_controles", "geom", "contrôle $ctl : x"),
            RefusGpkg.ChampInvalide("etats_plants", "plant_uuid", "le plant $plant"),
            RefusGpkg.UuidEnDouble("controles", ctl),
            RefusGpkg.UuidInvalide("controles", "uuid", "ABC"),
            RefusGpkg.PasUnGeoPackage("x"),
        )
        for (x in refus) {
            val f = FautesDuRendu.de(x, r, "P35-01")
            assertPropre(f.texte)
            if (x !is RefusGpkg.PasUnGeoPackage) {
                assertEquals("P35-01", f.placette, "$x")
                assertTrue(f.texte.startsWith("P35-01"), f.texte)
            }
        }
        val essence = FautesDuRendu.de(RefusGpkg.EssenceInconnue(plant, "ZZZ"), r, null)
        assertEquals(FauteDeSaisie("P35-01", 3, "P35-01, plant n° 3 : essence « ZZZ » absente de la liste des essences : corriger l'essence"), essence)
        val somalie = FautesDuRendu.de(RefusGpkg.PointHorsFrance("controles", ctl, 1.0, 2.0, true), r, null)
        assertEquals("P35-01 : la position du contrôle tombe hors de France (latitude et longitude inversées ?) : annuler ce contrôle et le refaire", somalie.texte)
    }
}
