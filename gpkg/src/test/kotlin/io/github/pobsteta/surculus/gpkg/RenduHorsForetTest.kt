package io.github.pobsteta.surculus.gpkg

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.geo.Projections
import io.github.pobsteta.surculus.core.geo.UniteAzimut
import io.github.pobsteta.surculus.core.placette.EmpriseEtFrance
import io.github.pobsteta.surculus.core.placette.Essences
import io.github.pobsteta.surculus.core.reperes.AzimutSaisi
import io.github.pobsteta.surculus.core.reperes.CodeEpsg
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
import kotlin.test.assertTrue

/**
 * Mission 093 : rendre une tournée terminée dont des points tombent hors de l'emprise de la forêt (fiche
 * `pilotage/missions/093-rendre-les-points-hors-foret.md` ; décision de Pascal du 2026-11-06, « garder et signaler » ;
 * spec § 4.8, § 5, § 6). Tests d'acceptation du `testeur`, écrits **sans lire** le code du rendu ni de l'export. Lu pour
 * l'API seulement : `RenduTourneeTest` (banc et `RenduTournee.rendre`), `TourneeDeTest`, `docs/formats/journal-valeurs-v1.md`
 * et `-v3.md` (clés des valeurs `controle` et `pointage`), la signature de `PointDerive.deriver`.
 *
 * Le constat de Pascal (essai de la v0.7.109) : sous couvert, le fix du téléphone erre ; le contrôle est rattaché par son
 * code ; au rendu, la position qui tombe hors de la forêt bloquait toute la tournée. Ici, la journée est jouée par la
 * session comme dans `RenduTourneeTest` (ouverture dans la forêt), puis la valeur courante est **corrigée au journal**
 * pour porter la position errée : l'ouverture d'un fix hors emprise (`point_hors_foret`) est la question 2 de la fiche,
 * que Pascal n'a pas tranchée, et ces tests n'en dépendent pas.
 *
 * **Non exigé, car pas encore décidé (fiche, question 1)** : la forme du signalement (où, avec quelle phrase, trace dans
 * le zip ou non). Ces tests exigent seulement ce qui est sûr : le rendu réussit, le point est écrit tel quel, hors de
 * France le refus reste. API de signalement **proposée** au `dev`, non imposée :
 * `ResultatRendu.Rendue(zip, bilan, signalements: List<SignalementRendu>)`, avec
 * `data class SignalementRendu(val placette: String, val plant: Int?, val texte: String)` (même forme que
 * `FauteDeSaisie`, texte en français, sans UUID ni motif technique).
 *
 * Coordonnées : Lambert-93 (EPSG:2154), x = Est, y = Nord, mètres ; `Projections.versLambert93(lon, lat)`, degrés WGS 84,
 * arguments nommés.
 */
class RenduHorsForetTest {
    @TempDir
    lateinit var dossier: Path

    private val essences = Essences(ExporterScenario.lireEssences())
    private val version = "0.0.0-rendu-093"
    private val exporteLe: Instant = Instant.parse("2026-11-06T12:00:00Z")
    private val operateur = "Agent d'essai"

    // ---------------------------------------------------------------------------------------------------------------
    // Banc (même principe que RenduTourneeTest) : journal en mémoire, horloge sans fix.

    private class JournalMemoire : JournalControle {
        class L(val uuid: String, val objet: String, val type: String, val valeur: String?, val remplace: String?)
        val lignes = ArrayList<L>()
        private var n = 0
        override fun nouvelUuid(): String = UUID.nameUUIDFromBytes("rendu-093:${++n}".toByteArray()).toString()
        override fun saisir(objetUuid: String, type: String, valeur: String, operateur: String, remplaceUuid: String?, fix: FixSession?): String =
            nouvelUuid().also { lignes += L(it, objetUuid, type, valeur, remplaceUuid) }
        override fun annuler(uuidCible: String, operateur: String) {
            lignes += L(nouvelUuid(), lignes.first { it.uuid == uuidCible }.objet, "annulation", null, uuidCible)
        }
        override fun lignesCourantes(): List<LigneCourante> {
            val actives = lignes.filter { it.type != "annulation" && lignes.none { a -> a.type == "annulation" && a.remplace == it.uuid } }
            return actives.filter { l -> actives.none { it.remplace == l.uuid } }.map { LigneCourante(it.uuid, it.objet, it.type, it.valeur, it.remplace) }
        }
        fun tete(objet: String, type: String): LigneCourante = lignesCourantes().single { it.objetUuid == objet && it.type == type }
        /** Correction au journal : même objet, même type, `remplace_uuid` = la tête (format v1). */
        fun corriger(objet: String, type: String, modifier: (JsonObject) -> Unit) {
            val t = tete(objet, type)
            val v = JsonParser.parseString(t.valeur).asJsonObject.also(modifier)
            saisir(objet, type, v.toString(), "Agent d'essai", t.uuid, null)
        }
    }

    private class Horloge(var ms: Long) : HorlogeSession {
        override fun maintenantUtcMs() = ms
        override fun monotoneMs() = 0L
        override fun fix(): FixSession? = null
    }

    private class Journee(val journal: JournalMemoire, val controles: Map<String, String>, val plants: Map<String, String>)

    private fun dossierTournee(nom: String): Path {
        val d = Files.createDirectories(dossier.resolve(nom))
        TourneeDeTest.couchey(d, nom = ImportTournee.NOM_GPKG)
        Files.createDirectories(d.resolve("DCIM"))
        return d
    }

    /** P35-01 ouvert avec une position mesurée à 0,6 m du piquet (2 plants), P35-02 ouvert sans position (1 plant) ; tous terminés. */
    private fun jouer(d: Path): Journee {
        val tournee = LecteurTournee.lire(d.resolve(ImportTournee.NOM_GPKG))
        val contexte = ContexteDeTournee.de(tournee).copy(operateur = operateur)
        val journal = JournalMemoire()
        val horloge = Horloge(0)
        val controles = LinkedHashMap<String, String>()
        val plants = LinkedHashMap<String, String>()
        fun dernier(type: String) = journal.lignes.last { it.type == type }

        fun controler(code: String, visiteLe: String, sansPosition: Boolean, pointes: List<Triple<Double, Double, Int>>) {
            val piquet = tournee.placettes.single { it.code == code }.position!!
            horloge.ms = Instant.parse(visiteLe).toEpochMilli()
            val ouverture = if (sansPosition) {
                SessionControle.ouvrir(contexte, journal, horloge, essences, Arrivee.SansPosition, code)
            } else {
                SessionControle.ouvrir(contexte, journal, horloge, essences, Arrivee.Lambert93(PointLambert93(xMetres = piquet.xMetres + 0.6, yMetres = piquet.yMetres - 0.4)))
            }
            val s = assertNotNull(ouverture as? OuvertureControle.Ouverte, "$code : $ouverture").session
            pointes.forEachIndexed { i, (distance, azimut, hauteur) ->
                val az = (AzimutSaisi.valider(azimut, UniteAzimut.DEGRES) as Resultat.Accepte).valeur
                assertTrue(s.pointer(distance, az, SourceAzimut.SAISIE, "CHS") is SuiteSession.Fait, "$code : pointage $i")
                plants["$code/$i"] = dernier(FormatValeurs.TYPE_POINTAGE).objet
                assertTrue(s.dire("sain", null, null, hauteur) is SuiteSession.Fait, "$code : état $i")
            }
            s.direBesoin("aucun"); s.direConcurrence("faible")
            assertTrue(s.terminer() is SuiteSession.Fait, "$code terminé")
            controles[code] = dernier(FormatValeurs.TYPE_CONTROLE).objet
        }

        controler("P35-01", "2026-10-20T09:30:00Z", false, listOf(Triple(1.2, 10.0, 35), Triple(2.0, 100.0, 40)))
        controler("P35-02", "2026-10-20T10:15:00Z", true, listOf(Triple(1.5, 45.0, 50)))
        return Journee(journal, controles, plants)
    }

    private fun rendre(d: Path, j: Journee, zip: Path): ResultatRendu =
        RenduTournee.rendre(d, j.journal.lignesCourantes(), zip, version, exporteLe, essences)

    private fun rendue(r: ResultatRendu): ResultatRendu.Rendue =
        assertNotNull(r as? ResultatRendu.Rendue, "rendu refusé alors que le point est en France (décision de Pascal du 2026-11-06, « garder et signaler ») : $r")

    private fun decompresser(zip: Path, nom: String): Path {
        val sortie = Files.createDirectories(dossier.resolve(nom))
        ZipInputStream(Files.newInputStream(zip)).use { z ->
            while (true) {
                val e = z.nextEntry ?: break
                val cible = sortie.resolve(e.name).normalize()
                assertTrue(cible.startsWith(sortie), "entrée hors du dossier : ${e.name}")
                if (e.isDirectory) Files.createDirectories(cible) else { Files.createDirectories(cible.parent); Files.copy(z, cible) }
            }
        }
        return sortie
    }

    private fun entrees(zip: Path): List<String> = ZipInputStream(Files.newInputStream(zip)).use { z ->
        generateSequence { z.nextEntry }.map { it.name }.toList()
    }

    /** Banc : le point est en France et hors du rectangle de la forêt de [TourneeDeTest.couchey] (trois centres ± 300 m). */
    private fun horsEmpriseEnFrance(p: PointLambert93) {
        assertTrue(EmpriseEtFrance.enFrance(p), "banc : ($p) doit tomber en France")
        val c = (0..2).map(TourneeDeTest::centre)
        val dedans = p.xMetres in (c.minOf { it.xMetres } - 300)..(c.maxOf { it.xMetres } + 300) &&
            p.yMetres in (c.minOf { it.yMetres } - 300)..(c.maxOf { it.yMetres } + 300)
        assertFalse(dedans, "banc : ($p) doit tomber hors de l'emprise de la forêt")
    }

    /**
     * Le fix de P35-01 a erré jusqu'en [erre] : corrige la valeur `controle` courante (clés v3, `journal-valeurs-v3.md`) ;
     * le piquet (`position_x`, `position_y`) ne bouge pas, le contrôle est rattaché par son code.
     */
    private fun fixErre(j: Journee, erre: PointLambert93, precisionM: Double = 25.0, source: String = "telephone") {
        val c1 = j.controles.getValue("P35-01")
        j.journal.corriger(c1, FormatValeurs.TYPE_CONTROLE) {
            it.addProperty("position_mesuree_x", erre.xMetres); it.addProperty("position_mesuree_y", erre.yMetres)
            it.addProperty("rattache_par_identite", true); it.addProperty("sans_position_mesuree", false)
            it.addProperty("precision_m", precisionM); it.addProperty("source_gnss", source); it.addProperty("simulee", false)
        }
        assertNotNull(ValeurControle.lire(j.journal.tete(c1, FormatValeurs.TYPE_CONTROLE).valeur), "banc : valeur de contrôle lisible")
    }

    private fun piquetP3501(): PointLambert93 = TourneeDeTest.centre(0)

    // ---------------------------------------------------------------------------------------------------------------

    @Test
    fun `controle dont la position mesuree tombe hors de la foret, en France - la tournee est rendue, le point ecrit tel quel`() {
        val d = dossierTournee("controle-hors-emprise")
        val j = jouer(d)
        val p = piquetP3501()
        val erre = PointLambert93(xMetres = p.xMetres - 350.0, yMetres = p.yMetres + 40.0) // ~50 m à l'ouest de la forêt
        horsEmpriseEnFrance(erre)
        fixErre(j, erre, precisionM = 25.0, source = "telephone")

        val zip = dossier.resolve("controle-hors-emprise.zip")
        val r = rendue(rendre(d, j, zip))
        assertEquals(2, r.bilan.controles, "les deux contrôles sont rendus, aucune mesure perdue")
        assertTrue(RenduTournee.estRendue(d), "la tournée passe « rendue »")

        val gpkg = decompresser(zip, "controle-hors-emprise").resolve("travaux.gpkg")
        Sqlite.ouvrir(gpkg, lectureSeule = true).use { cnx ->
            val l = cnx.lignes("SELECT * FROM positions_controles WHERE controle_uuid = '${j.controles.getValue("P35-01")}'").single()
            val m = assertNotNull(GeometrieGpkg.point(l.blob("geom")!!), "la position mesurée est écrite (pas de géométrie NULL)")
            assertEquals(erre.xMetres, m.xMetres, 1e-6, "x (Est) tel que mesuré")
            assertEquals(erre.yMetres, m.yMetres, 1e-6, "y (Nord) tel que mesuré")
            assertEquals(25.0, l.reel("precision_m"), "même précision")
            assertEquals("telephone", l.texte("source_gnss"), "même source")
            assertEquals(false, l.booleen("simulee"))
            assertEquals(true, l.booleen("rattache_par_identite"), "rattaché par son code")
            // Le contrôle reste rattaché à sa placette (sommieR rattache par placette_uuid, R/travaux_qfield.R l. 467-468).
            val k = cnx.lignes("SELECT placette_uuid FROM controles WHERE uuid = '${j.controles.getValue("P35-01")}'").single()
            assertEquals(TourneeDeTest.placetteUuid(0), k.texte("placette_uuid"))
        }
    }

    @Test
    fun `tournee controlee a la maison - position mesuree a des kilometres, en France - rendue quand meme`() {
        // L'échantillon de Pascal : la tournée de Couchey contrôlée à la maison. Point pris à 5 km à l'est de la forêt (lon 5,0003 ; lat 47,2617).
        val d = dossierTournee("maison")
        val j = jouer(d)
        val (x, y) = Projections.versLambert93(lon = 5.0003, lat = 47.2617)
        val maison = PointLambert93(xMetres = x, yMetres = y)
        horsEmpriseEnFrance(maison)
        fixErre(j, maison, precisionM = 8.0, source = "telephone")
        val zip = dossier.resolve("maison.zip")
        rendue(rendre(d, j, zip))
        val gpkg = decompresser(zip, "maison").resolve("travaux.gpkg")
        val g = Sqlite.ouvrir(gpkg, lectureSeule = true).use {
            it.lignes("SELECT geom FROM positions_controles WHERE controle_uuid = '${j.controles.getValue("P35-01")}'").single()
        }
        val m = assertNotNull(GeometrieGpkg.point(g.blob("geom")!!))
        assertEquals(maison.xMetres, m.xMetres, 1e-6); assertEquals(maison.yMetres, m.yMetres, 1e-6)
    }

    @Test
    fun `plant pointe hors de la foret, en France - la tournee est rendue, le plant ecrit a sa position derivee`() {
        val d = dossierTournee("plant-hors-emprise")
        val j = jouer(d)
        // Le plant 1 de P35-01 corrigé au journal : 320 m plein ouest du piquet (20 m hors de la forêt), confirmé hors rayon.
        // Termes dérivés recalculés par core (PointDerive), pour que la valeur reste cohérente (ADR 0003).
        val plant = j.plants.getValue("P35-01/1")
        val tete = JsonParser.parseString(j.journal.tete(plant, FormatValeurs.TYPE_POINTAGE).valeur).asJsonObject
        val jour = LocalDate.parse(tete.get("jour_declinaison").asString)
        val az = (AzimutSaisi.valider(270.0, UniteAzimut.DEGRES) as Resultat.Accepte).valeur
        val derive = (PointDerive.deriver(piquetP3501(), CodeEpsg.RGF93_V1_LAMBERT93, 320.0, az, jour) as Resultat.Accepte).valeur
        horsEmpriseEnFrance(derive.position)
        j.journal.corriger(plant, FormatValeurs.TYPE_POINTAGE) {
            it.addProperty("distance_horizontale_m", 320.0); it.addProperty("azimut_mag", 270.0); it.addProperty("unite_azimut", "deg")
            it.addProperty("declinaison_deg", derive.orientation.declinaisonDeg)
            it.addProperty("convergence_deg", derive.orientation.convergenceDeg)
            it.addProperty("azimut_geo_deg", derive.orientation.azimutGeographiqueDeg)
            it.addProperty("azimut_grille_deg", derive.orientation.azimutGrilleDeg)
            it.addProperty("x", derive.position.xMetres); it.addProperty("y", derive.position.yMetres)
            it.addProperty("hors_rayon_confirme", true)
        }

        val zip = dossier.resolve("plant-hors-emprise.zip")
        val r = rendue(rendre(d, j, zip))
        assertEquals(3, r.bilan.plants, "les trois plants sont rendus, aucune mesure perdue")
        val gpkg = decompresser(zip, "plant-hors-emprise").resolve("travaux.gpkg")
        val lu = LecteurTournee.lire(gpkg).plants.single { it.uuid == plant }
        assertEquals(derive.position.xMetres, lu.position!!.xMetres, 1e-6, "x (Est) tel que dérivé")
        assertEquals(derive.position.yMetres, lu.position!!.yMetres, 1e-6, "y (Nord) tel que dérivé")
        assertEquals(TourneeDeTest.placetteUuid(0), lu.placetteUuid, "le plant reste dans sa placette")
    }

    @Test
    fun `position mesuree en Somalie - le rendu reste refuse et la faute nomme P35-01`() {
        val d = dossierTournee("somalie")
        val j = jouer(d)
        // Le piège de la Somalie, sur la seule position mesurée : le piquet de P35-01 (lon 4,93250 ; lat 47,25910) lu à l'envers.
        val (x, y) = Projections.versLambert93(lon = 47.25910, lat = 4.93250)
        fixErre(j, PointLambert93(xMetres = x, yMetres = y))
        val dest = Files.createDirectories(dossier.resolve("somalie-sortie"))
        val zip = dest.resolve("tournee.zip")
        val r = assertNotNull(rendre(d, j, zip) as? ResultatRendu.Refusee, "un point hors de France bloque toujours le rendu")
        assertNotNull(r.fautes.singleOrNull { it.placette == "P35-01" }, "la faute renvoie à P35-01 : ${r.fautes}")
        assertFalse(Files.exists(zip), "tout ou rien : aucun .zip")
        assertFalse(RenduTournee.estRendue(d), "la tournée refusée reste ouverte")
    }

    @Test
    fun `tournee sans point hors de la foret - rendue, le zip ne porte que travaux gpkg et DCIM`() {
        // Aucun signalement : quelle que soit sa forme (question 1 de la fiche), une tournée sans point hors emprise n'en
        // porte aucune trace. Seule chose sûre ici : rien de plus dans le zip que travaux.gpkg et DCIM/.
        val d = dossierTournee("propre")
        val j = jouer(d)
        val zip = dossier.resolve("propre.zip")
        rendue(rendre(d, j, zip))
        val noms = entrees(zip)
        assertTrue(noms.all { it == "travaux.gpkg" || it.startsWith("DCIM/") }, "rien d'autre dans le zip : $noms")
    }
}
