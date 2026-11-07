package io.github.pobsteta.surculus.gpkg

import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.geo.Projections
import io.github.pobsteta.surculus.core.placette.EmpriseEtFrance
import io.github.pobsteta.surculus.core.placette.Essences
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
import io.github.pobsteta.surculus.core.session.ValeurControle
import io.github.pobsteta.surculus.gpkg.outil.ExporterScenario
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.UUID
import java.util.zip.ZipInputStream
import kotlin.math.hypot
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Mission 090 : rendre la tournée depuis le téléphone (spec § 4.1, § 4.8, § 5, § 6 ; ADR 0005 et 0007 ;
 * `ci/sommier/README.md`). Tests d'acceptation écrits par le `testeur` depuis la fiche, **sans lire** le code de l'export,
 * du journal ni des écrans de tournée. Lu pour l'API seulement : `SessionEtExportCoucheyTest`,
 * `SursyncV2ExportBoutEnBoutTest`, `ExportSansPositionTest`, `TourneeDeTest`, `Contenu`, la signature de
 * `ExportTravaux.exporter` (et, en la lisant, ses trois premières lignes : lecture, vérification, fichier temporaire),
 * `ExporterScenario.jouer` (outil de test de la CI), `Saisies.kt`, `Tournee.kt` (classes publiques), `Ports.kt`
 * (`JournalControle`, `LigneCourante`, `PlacettesControlees`), les signatures publiques de `SessionControle`,
 * `ImportTournee.NOM_GPKG`, `Projections.versLambert93`, `EmpriseEtFrance.enFrance`, les signatures de
 * `DepotTournees.kt` (dont `LigneTournee.rendue`, aujourd'hui toujours faux) et `docs/formats/journal-valeurs-v1.md`.
 *
 * Éprouvé avant le push contre une implémentation jetable (non commitée) : journal → saisies comme
 * `SessionEtExportCoucheyTest`, puis `ExportTravaux`, zip à plat. Les huit tests de contenu passent avec elle ; les deux
 * tests de fautes restent rouges pour la bonne raison (faute sans placette ni plant, texte technique avec UUID). Le banc
 * (journée jouée, correction, annulation, Somalie, essence) est donc juste ; le rouge tient à l'API absente.
 *
 * **Rouge (ne compile pas) jusqu'au `dev`.** API **supposée**, la plus simple possible, que le `dev` confirme ou remplace
 * dans son plan :
 *
 * ```kotlin
 * object RenduTournee {
 *     /** Du journal au `.zip` : saisies du journal → vérification complète (§ 6) → `travaux.gpkg` complété + `DCIM/`. */
 *     fun rendre(
 *         dossierTournee: Path,          // la copie privée de la tournée : travaux.gpkg et DCIM/ (spec § 4.1)
 *         lignes: List<LigneCourante>,   // lignes courantes du journal de CETTE tournée (JournalControle lié à son code)
 *         destination: Path,             // le <tournée>.zip ; son emplacement est la question 1 de la fiche
 *         versionSurculus: String,
 *         exporteLe: Instant,            // horloge injectée (export déterministe, § 5)
 *         essences: Essences? = null,
 *     ): ResultatRendu
 *
 *     /** La tournée est-elle rendue (état « rendue » de la liste des tournées, § 4.1) ? */
 *     fun estRendue(dossierTournee: Path): Boolean
 * }
 * sealed interface ResultatRendu {
 *     data class Rendue(val zip: Path, val bilan: BilanExport) : ResultatRendu
 *     data class Refusee(val fautes: List<FauteDeSaisie>) : ResultatRendu
 * }
 * /** Une faute qui renvoie à la saisie : code de la placette (`P35-01`), numéro du plant, texte en français. */
 * data class FauteDeSaisie(val placette: String?, val plant: Int?, val texte: String)
 * ```
 *
 * Le journal est passé en liste : l'export ne peut pas y écrire (« l'export ne touche pas au journal », § 5). Le filtre
 * « cette tournée seulement » est celui du port [JournalControle], lié au code de la tournée (mission 032) ; il n'est
 * pas rejoué ici.
 *
 * **Non testé, car à trancher par Pascal (fiche, questions 1 à 4)** : où le `.zip` est écrit et comment il sort du
 * téléphone ; les saisies après le rendu et le sort d'un `.zip` déjà écrit ; la place du bouton et ce que l'agent voit ;
 * un contrôle commencé mais non terminé (aucun test n'en contient). Aussi laissé ouvert : les **plants** pointés
 * pendant un contrôle annulé (exportés ou non) ; seules ses lignes de `controles`, `positions_controles` et
 * `etats_plants` sont exigées absentes.
 *
 * Coordonnées : Lambert-93 (EPSG:2154), x = Est, y = Nord, mètres ; conversions par `Projections.versLambert93(lon, lat)`,
 * degrés décimaux WGS 84, arguments toujours nommés.
 */
class RenduTourneeTest {
    @TempDir
    lateinit var dossier: Path

    private val racine: Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }.first { Files.exists(it.resolve("settings.gradle.kts")) }
    private val scenario = JsonParser.parseString(Files.readString(racine.resolve("fixtures/export/scenario-couchey.json"))).asJsonObject
    private val registre = JsonParser.parseString(Files.readString(racine.resolve("fixtures/export/registre-attendu-couchey.json"))).asJsonObject
    private val essences = Essences(ExporterScenario.lireEssences())

    private val version = "0.0.0-rendu"
    private val exporteLe: Instant = Instant.parse("2026-10-20T12:00:00Z")

    /** Octets d'une photo livrée par la tournée dans `DCIM/` : elle doit ressortir à l'identique (fiche : DCIM tel que livré). */
    private val photoAller = "photo d'aller, octets quelconques".toByteArray(Charsets.UTF_8)

    // ---------------------------------------------------------------------------------------------------------------
    // Banc : journal en mémoire (règles de projection de la spec § 5 : annuler défait, annuler la racine ne laisse rien)

    private class JournalMemoire : JournalControle {
        class L(val uuid: String, val objet: String, val type: String, val valeur: String?, val remplace: String?)
        val lignes = ArrayList<L>()
        private var n = 0
        override fun nouvelUuid(): String = UUID.nameUUIDFromBytes("rendu-090:${++n}".toByteArray()).toString()
        override fun saisir(objetUuid: String, type: String, valeur: String, operateur: String, remplaceUuid: String?, fix: FixSession?): String =
            nouvelUuid().also { lignes += L(it, objetUuid, type, valeur, remplaceUuid) }
        override fun annuler(uuidCible: String, operateur: String) {
            lignes += L(nouvelUuid(), lignes.first { it.uuid == uuidCible }.objet, "annulation", null, uuidCible)
        }
        override fun lignesCourantes(): List<LigneCourante> {
            val actives = lignes.filter { it.type != "annulation" && lignes.none { a -> a.type == "annulation" && a.remplace == it.uuid } }
            return actives.filter { l -> actives.none { it.remplace == l.uuid } }.map { LigneCourante(it.uuid, it.objet, it.type, it.valeur, it.remplace) }
        }

        /** Tête courante de (objet, type). */
        fun tete(objet: String, type: String): LigneCourante = lignesCourantes().single { it.objetUuid == objet && it.type == type }

        /** Corrige une ligne : nouvelle saisie du même objet et du même type, `remplace_uuid` = la tête (format v1, § `etat`). */
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

    /** Ce qu'a produit la journée jouée : le journal et les UUID qu'on désigne ensuite. */
    private class Journee(val journal: JournalMemoire, val controles: Map<String, String>, val etats: Map<String, String>, val plants: Map<String, String>)

    private fun JsonObject.texte(cle: String) = get(cle)?.takeUnless { it.isJsonNull }?.asString
    private fun JsonObject.reel(cle: String) = get(cle)?.takeUnless { it.isJsonNull }?.asDouble

    /** La tournée telle que l'application la garde : `travaux.gpkg` de Couchey (sommieR 0.31.1) et `DCIM/`. */
    private fun dossierTournee(nom: String = "tournee", avecPhoto: Boolean = true): Path {
        val d = Files.createDirectories(dossier.resolve(nom))
        TourneeDeTest.couchey(d, nom = ImportTournee.NOM_GPKG)
        val dcim = Files.createDirectories(d.resolve("DCIM"))
        if (avecPhoto) Files.write(dcim.resolve("aller-P35-01.jpg"), photoAller)
        return d
    }

    /**
     * Joue, par la session de contrôle, sur la tournée [d] :
     * - P35-01 (avec position mesurée) et P35-02 (ouvert sans position) comme le scénario Couchey de la CI, sauf le plant
     *   « e » de P35-01, dit d'abord **mort** (animaux) puis **corrigé** au journal en sain, 42 cm : la valeur du
     *   scénario. Si la correction n'était pas appliquée, P35-01 aurait 2 vivants et une hauteur moyenne de 38 cm ;
     * - P35-03, contrôlé (un plant sain de 30 cm) et terminé, puis **annulé** (toutes ses lignes `controle`, de la tête
     *   à la racine : spec § 5, « annuler une racine ne laisse aucune valeur courante »).
     * Tous les contrôles restants sont **terminés** (question 4 de la fiche non supposée).
     */
    private fun jouer(d: Path): Journee {
        val tournee = LecteurTournee.lire(d.resolve(ImportTournee.NOM_GPKG))
        val contexte = ContexteDeTournee.de(tournee).copy(operateur = scenario.texte("operateur")!!)
        val journal = JournalMemoire()
        val horloge = Horloge(0)
        val controles = LinkedHashMap<String, String>()
        val etats = LinkedHashMap<String, String>()
        val plants = LinkedHashMap<String, String>()

        fun dernier(type: String) = journal.lignes.last { it.type == type }

        fun controler(code: String, visiteLe: String, sansPosition: Boolean, decalageEst: Double, decalageNord: Double, liste: List<JsonObject>, besoin: String = "aucun", concurrence: String = "faible") {
            val piquet = tournee.placettes.single { it.code == code }.position!!
            horloge.ms = Instant.parse(visiteLe).toEpochMilli()
            val ouverture = if (sansPosition) {
                SessionControle.ouvrir(contexte, journal, horloge, essences, Arrivee.SansPosition, code)
            } else {
                val mesuree = PointLambert93(xMetres = piquet.xMetres + decalageEst, yMetres = piquet.yMetres + decalageNord)
                SessionControle.ouvrir(contexte, journal, horloge, essences, Arrivee.Lambert93(mesuree))
            }
            val s = assertNotNull(ouverture as? OuvertureControle.Ouverte, "$code : $ouverture").session
            for (p in liste) {
                val unite = (CodesUniteAzimut.lire(p.get("unite_azimut").asString) as Resultat.Accepte).valeur
                val az = (AzimutSaisi.valider(p.get("azimut").asDouble, unite) as Resultat.Accepte).valeur
                assertTrue(s.pointer(p.get("distance_m").asDouble, az, SourceAzimut.SAISIE, p.texte("essence")) is SuiteSession.Fait)
                plants["$code/${p.get("cle").asString}"] = dernier(FormatValeurs.TYPE_POINTAGE).objet
                val dit = s.dire(p.get("etat").asString, p.texte("cause_groupe"), p.texte("cause_detail"), p.get("hauteur_cm")?.takeUnless { it.isJsonNull }?.asInt)
                assertTrue(dit is SuiteSession.Fait, "$code : $dit")
                etats["$code/${p.get("cle").asString}"] = dernier(FormatValeurs.TYPE_ETAT).objet
            }
            s.direBesoin(besoin); s.direConcurrence(concurrence)
            assertTrue(s.terminer() is SuiteSession.Fait, "$code terminé")
            controles[code] = dernier(FormatValeurs.TYPE_CONTROLE).objet
        }

        val c = scenario.getAsJsonArray("controles").map { it.asJsonObject }
        // P35-01 : le plant « e » dit mort (animaux), corrigé plus bas.
        val p1 = c[0].getAsJsonArray("plants").map { it.asJsonObject.deepCopy() }
        p1.single { it.get("cle").asString == "e" }.apply {
            addProperty("etat", "mort"); addProperty("cause_groupe", "animaux"); add("hauteur_cm", JsonNull.INSTANCE)
        }
        val m = c[0].getAsJsonObject("position_mesuree")
        controler("P35-01", c[0].texte("visite_le")!!, false, m.reel("decalage_est_m")!!, m.reel("decalage_nord_m")!!, p1, c[0].texte("besoin")!!, c[0].texte("concurrence")!!)
        controler("P35-02", c[1].texte("visite_le")!!, true, 0.0, 0.0, c[1].getAsJsonArray("plants").map { it.asJsonObject }, c[1].texte("besoin")!!, c[1].texte("concurrence")!!)
        val p3 = JsonParser.parseString("""{"cle":"a","distance_m":1.4,"azimut":120.0,"unite_azimut":"deg","essence":"CHS","etat":"sain","hauteur_cm":30}""").asJsonObject
        controler("P35-03", "2026-10-20T11:00:00Z", false, 0.3, 0.2, listOf(p3))

        // Correction : l'état du plant « e » de P35-01 redevient sain, 42 cm (valeur du scénario et du registre attendu).
        journal.corriger(etats.getValue("P35-01/e"), FormatValeurs.TYPE_ETAT) {
            it.addProperty("etat", "sain"); it.add("cause_groupe", JsonNull.INSTANCE); it.add("cause_detail", JsonNull.INSTANCE); it.addProperty("hauteur_cm", 42)
        }
        // Annulation : P35-03, de la tête à la racine.
        val c3 = controles.getValue("P35-03")
        journal.lignes.filter { it.objet == c3 && it.type == FormatValeurs.TYPE_CONTROLE }.reversed().forEach { journal.annuler(it.uuid, "Agent d'essai") }
        assertTrue(journal.lignesCourantes().none { it.objetUuid == c3 && it.type == FormatValeurs.TYPE_CONTROLE }, "banc : P35-03 n'a plus de valeur courante")
        return Journee(journal, controles, etats, plants)
    }

    private fun rendre(d: Path, lignes: List<LigneCourante>, zip: Path): ResultatRendu =
        RenduTournee.rendre(d, lignes, zip, version, exporteLe, essences)

    /** Décompresse [zip] dans un dossier neuf, en refusant tout chemin qui sortirait du dossier. */
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

    private fun rendue(r: ResultatRendu): ResultatRendu.Rendue = assertNotNull(r as? ResultatRendu.Rendue, "rendu refusé : $r")

    private fun refusee(r: ResultatRendu): ResultatRendu.Refusee = assertNotNull(r as? ResultatRendu.Refusee, "rendu accepté alors qu'une faute est saisie : $r")

    private fun codes(gpkg: Path): Map<String, String> = LecteurTournee.lire(gpkg).placettes.associate { it.uuid to it.code!! }

    // ---------------------------------------------------------------------------------------------------------------
    // Critère 1 : un sans position, un corrigé, un annulé → un .zip dont le travaux.gpkg passe et contient exactement
    // les lignes attendues.

    @Test
    fun `le zip est a plat - travaux gpkg et DCIM a la racine, la photo d'aller identique`() {
        val d = dossierTournee()
        val j = jouer(d)
        val zip = dossier.resolve("rendu").resolve("tournee.zip")
        Files.createDirectories(zip.parent)
        val r = rendue(rendre(d, j.journal.lignesCourantes(), zip))
        assertEquals(zip, r.zip)
        assertTrue(Files.isRegularFile(zip), "le .zip est écrit")

        val noms = entrees(zip)
        assertTrue("travaux.gpkg" in noms, "travaux.gpkg à la racine du zip (zip à plat, décision de Pascal du 2026-10-17) : $noms")
        assertTrue(noms.all { it == "travaux.gpkg" || it.startsWith("DCIM/") || '/' !in it.trimEnd('/') }, "rien n'est rangé sous un dossier englobant : $noms")
        val sortie = decompresser(zip, "decompresse")
        assertTrue(Files.isRegularFile(sortie.resolve("travaux.gpkg")), "décompressé, c'est le dossier que lit l'import de sommieR (ADR 0007)")
        assertTrue(Files.isDirectory(sortie.resolve("DCIM")))
        assertTrue(photoAller.contentEquals(Files.readAllBytes(sortie.resolve("DCIM/aller-P35-01.jpg"))), "DCIM/ tel que la tournée l'a livré")
    }

    @Test
    fun `un DCIM vide livre par sommieR reste un dossier DCIM dans le zip`() {
        val d = dossierTournee(avecPhoto = false)
        val j = jouer(d)
        val zip = dossier.resolve("vide.zip")
        rendue(rendre(d, j.journal.lignesCourantes(), zip))
        assertTrue(Files.isDirectory(decompresser(zip, "decompresse-vide").resolve("DCIM")), "DCIM/ présent même vide (spec § 4.8 : « travaux.gpkg complété, DCIM/ »)")
    }

    @Test
    fun `les lignes ajoutees sont exactement celles du journal - corrige applique, annule absent`() {
        val d = dossierTournee()
        val j = jouer(d)
        val zip = dossier.resolve("tournee.zip")
        val r = rendue(rendre(d, j.journal.lignesCourantes(), zip))
        val gpkg = decompresser(zip, "lignes").resolve("travaux.gpkg")
        val codes = codes(gpkg)
        val c1 = j.controles.getValue("P35-01")
        val c2 = j.controles.getValue("P35-02")
        val c3 = j.controles.getValue("P35-03")

        // controles : P35-01 et P35-02, aux UUID du journal ; P35-03 annulé n'y est pas.
        val controles = Contenu.colonnes(gpkg, "controles", "uuid", "placette_uuid")
        assertEquals(setOf(c1, c2), controles.map { it[0] }.toSet(), "contrôles exportés")
        assertEquals(listOf("P35-01", "P35-02"), controles.map { codes[it[1]] }.sortedBy { it })
        assertEquals(2, r.bilan.controles, "bilan : deux contrôles")

        // Totaux : ceux du registre attendu écrit à la main (mission 027) ; P35-01 n'a ces totaux que si la correction
        // du plant « e » (mort → sain, 42 cm) est appliquée (sinon 2 vivants, h_moy 38).
        val totaux = Contenu.colonnes(gpkg, "controles", "placette_uuid", "nb_total", "nb_vivants", "nb_abroutis", "h_moy_cm")
        for (a in registre.getAsJsonArray("controles").map { it.asJsonObject }) {
            val l = totaux.single { codes[it[0]] == a.get("placette").asString }
            assertEquals(
                listOf(a.get("nb_total").asInt, a.get("nb_vivants").asInt, a.get("nb_abroutis").asInt, a.get("h_moy_cm").asInt),
                l.drop(1).map { (it as Number).toInt() }, "totaux de ${a.get("placette").asString}",
            )
        }

        // etats_plants : 5 + 3 lignes, une par (contrôle, plant), aucune du contrôle annulé ; l'état courant de « e ».
        val etats = Contenu.colonnes(gpkg, "etats_plants", "uuid", "controle_uuid", "etat", "cause_groupe", "hauteur_cm")
        assertEquals(8, etats.size, "états de P35-01 (5) et P35-02 (3)")
        assertTrue(etats.none { it[1] == c3 }, "aucun état du contrôle annulé (pas de ligne orpheline)")
        val e = etats.single { it[0] == j.etats.getValue("P35-01/e") }
        assertEquals(listOf<Any?>("sain", null, 42), listOf(e[2], e[3], (e[4] as Number?)?.toInt()), "la correction est appliquée")

        // plants : ceux de P35-01 (5) et P35-02 (3) sont tous là. Ceux du contrôle annulé : question ouverte, non testée.
        val plants = Contenu.colonnes(gpkg, "plants", "uuid").map { it[0] }.toSet()
        val attendus = j.plants.filterKeys { !it.startsWith("P35-03/") }.values.toSet()
        assertTrue(plants.containsAll(attendus), "plants de P35-01 et P35-02 exportés : manquent ${attendus - plants}")

        // positions_controles : une ligne par contrôle exporté, aucune pour l'annulé ; P35-02 à géométrie NULL.
        val pos = Sqlite.ouvrir(gpkg, lectureSeule = true).use {
            it.lignes("SELECT controle_uuid, geom IS NULL AS nulle, precision_m, sans_position_mesuree, rattache_par_identite FROM positions_controles")
        }
        assertEquals(setOf(c1, c2), pos.map { it.texte("controle_uuid") }.toSet(), "une ligne par contrôle (spec § 5)")
        assertEquals(2, pos.size)
        val p2 = pos.single { it.texte("controle_uuid") == c2 }
        assertEquals(listOf(1, null, 1, 1), listOf(p2.entier("nulle"), p2["precision_m"], p2.entier("sans_position_mesuree"), p2.entier("rattache_par_identite")))
        assertEquals(0, pos.single { it.texte("controle_uuid") == c1 }.entier("nulle"))
    }

    @Test
    fun `placette francaise en France - chaque point ecrit tombe en France, P35-01 a sa position mesuree`() {
        val d = dossierTournee()
        val j = jouer(d)
        val zip = dossier.resolve("france.zip")
        rendue(rendre(d, j.journal.lignesCourantes(), zip))
        val gpkg = decompresser(zip, "france").resolve("travaux.gpkg")
        Sqlite.ouvrir(gpkg, lectureSeule = true).use { cnx ->
            for (table in listOf("controles", "plants", "positions_controles")) {
                for (l in cnx.lignes("SELECT geom FROM $table WHERE geom IS NOT NULL")) {
                    val p = assertNotNull(GeometrieGpkg.point(l.blob("geom")!!), "point de $table")
                    assertTrue(EmpriseEtFrance.enFrance(p), "$table : ($p) hors de France")
                }
            }
            // Point attendu de P35-01 : (lon, lat) WGS 84 du registre, converti en Lambert-93 (x = E, y = N), à 0,5 m.
            val a = registre.getAsJsonArray("controles").map { it.asJsonObject }.single { it.get("placette").asString == "P35-01" }.getAsJsonObject("point")
            val (x, y) = Projections.versLambert93(lon = a.get("lon").asDouble, lat = a.get("lat").asDouble)
            val g = cnx.lignes("SELECT geom FROM positions_controles WHERE controle_uuid = '${j.controles.getValue("P35-01")}'").single()
            val m = assertNotNull(GeometrieGpkg.point(g.blob("geom")!!))
            assertTrue(hypot(m.xMetres - x, m.yMetres - y) <= a.get("tolerance_m").asDouble, "P35-01 mesuré à (x=${m.xMetres}, y=${m.yMetres}), attendu (x=$x, y=$y)")
        }
    }

    @Test
    fun `le travaux gpkg rendu passe la verification - rendu de nouveau depuis lui-meme, il ne change pas`() {
        // Spec § 5, « Rejouable » : exporter à partir d'un export déjà fait ignore les lignes présentes à l'identique.
        // Si le premier rendu ne passait pas la vérification complète (§ 6), ce second rendu serait refusé.
        val d = dossierTournee()
        val j = jouer(d)
        val zip = dossier.resolve("premier.zip")
        rendue(rendre(d, j.journal.lignesCourantes(), zip))
        val relu = decompresser(zip, "relu")
        val zip2 = dossier.resolve("second.zip")
        rendue(rendre(relu, j.journal.lignesCourantes(), zip2))
        assertEquals(Contenu.de(relu.resolve("travaux.gpkg")), Contenu.de(decompresser(zip2, "relu2").resolve("travaux.gpkg")))
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Critère 2 : une faute bloque l'export ; la liste dit quelle placette, quel plant, en français, sans motif technique.

    private val motifsTechniques = listOf(
        "uuid_invalide", "uuid_en_double", "melange_reperes", "hors_france", "hors_referentiel", "controle_en_double",
        "jour_incoherent", "totaux_discordants", "Exception", "GpkgRefuse", "RefusGpkg", "null",
    )
    private val motifUuid = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

    private fun assertFautePropre(f: FauteDeSaisie) {
        assertTrue(f.texte.isNotBlank(), "texte vide")
        for (m in motifsTechniques) assertFalse(f.texte.contains(m), "motif technique « $m » dans « ${f.texte} » (règle des missions 068 et 082)")
        assertNull(motifUuid.find(f.texte), "UUID dans « ${f.texte} » : l'agent ne lit pas un UUID")
    }

    /** Tout ou rien (§ 5) : un rendu refusé n'écrit aucun fichier, et la tournée ne passe pas « rendue ». */
    private fun assertRienEcrit(d: Path, zip: Path) {
        assertFalse(Files.exists(zip), "un rendu refusé n'écrit pas de .zip")
        Files.list(zip.parent).use { s -> assertTrue(s.noneMatch { it.fileName.toString().startsWith(zip.fileName.toString()) }, "aucun fichier partiel") }
        assertFalse(RenduTournee.estRendue(d), "la tournée refusée reste ouverte")
    }

    @Test
    fun `placette en Somalie - un controle a latitude et longitude inversees bloque le rendu et la faute nomme P35-01`() {
        val d = dossierTournee()
        val j = jouer(d)
        // Le piège de la Somalie : le piquet de P35-01 (lon 4,93250 ; lat 47,25910) lu à l'envers, lon 47,25910 ; lat 4,93250.
        val (sx, sy) = Projections.versLambert93(lon = 47.25910, lat = 4.93250)
        j.journal.corriger(j.controles.getValue("P35-01"), FormatValeurs.TYPE_CONTROLE) {
            it.addProperty("position_x", sx); it.addProperty("position_y", sy)
            it.addProperty("position_mesuree_x", sx); it.addProperty("position_mesuree_y", sy)
        }
        assertNotNull(ValeurControle.lire(j.journal.tete(j.controles.getValue("P35-01"), FormatValeurs.TYPE_CONTROLE).valeur), "banc : valeur lisible")
        val dest = Files.createDirectories(dossier.resolve("somalie"))
        val zip = dest.resolve("tournee.zip")
        val r = refusee(rendre(d, j.journal.lignesCourantes(), zip))
        val f = r.fautes.singleOrNull { it.placette == "P35-01" }
        assertNotNull(f, "la liste renvoie à P35-01 : ${r.fautes}")
        assertTrue(f.texte.contains("P35-01"), "le texte dit quelle placette : « ${f.texte} »")
        r.fautes.forEach(::assertFautePropre)
        assertRienEcrit(d, zip)
    }

    @Test
    fun `essence hors referentiel - la faute nomme la placette et le numero du plant`() {
        val d = dossierTournee()
        val j = jouer(d)
        val plantB = j.plants.getValue("P35-02/b")
        j.journal.corriger(plantB, FormatValeurs.TYPE_POINTAGE) { it.addProperty("essence", "ZZZ") }
        val numero = ValeurControle.lire(j.journal.tete(j.controles.getValue("P35-02"), FormatValeurs.TYPE_CONTROLE).valeur)!!.numeros!!.getValue(plantB)
        val dest = Files.createDirectories(dossier.resolve("essence"))
        val zip = dest.resolve("tournee.zip")
        val r = refusee(rendre(d, j.journal.lignesCourantes(), zip))
        val f = r.fautes.singleOrNull { it.placette == "P35-02" && it.plant == numero }
        assertNotNull(f, "la liste renvoie à P35-02, plant n° $numero : ${r.fautes}")
        assertTrue(f.texte.contains("P35-02"), "le texte dit quelle placette : « ${f.texte} »")
        r.fautes.forEach(::assertFautePropre)
        assertRienEcrit(d, zip)
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Critère 3 : rendre deux fois donne le même contenu ; la tournée est marquée rendue et reste consultable.

    @Test
    fun `rendre deux fois donne le meme contenu, compare par le contenu et non par l'empreinte`() {
        val d = dossierTournee()
        val j = jouer(d)
        val lignes = j.journal.lignesCourantes()
        val a = dossier.resolve("a.zip")
        val b = dossier.resolve("b.zip")
        rendue(rendre(d, lignes, a))
        rendue(rendre(d, lignes, b))
        assertEquals(entrees(a).sorted(), entrees(b).sorted(), "mêmes entrées")
        val da = decompresser(a, "a")
        val db = decompresser(b, "b")
        assertEquals(Contenu.de(da.resolve("travaux.gpkg")), Contenu.de(db.resolve("travaux.gpkg")), "même contenu (ADR 0011 : jamais par empreinte)")
        assertTrue(Files.readAllBytes(da.resolve("DCIM/aller-P35-01.jpg")).contentEquals(Files.readAllBytes(db.resolve("DCIM/aller-P35-01.jpg"))))
        assertEquals(lignes, j.journal.lignesCourantes(), "le journal est inchangé")
    }

    @Test
    fun `la tournee passe rendue, reste consultable, et sa copie n'est jamais ecrasee`() {
        val d = dossierTournee()
        val j = jouer(d)
        val gpkg = d.resolve(ImportTournee.NOM_GPKG)
        val avant = Contenu.de(gpkg)
        val photo = Files.readAllBytes(d.resolve("DCIM/aller-P35-01.jpg"))
        assertFalse(RenduTournee.estRendue(d), "ouverte avant le rendu")

        rendue(rendre(d, j.journal.lignesCourantes(), dossier.resolve("tournee.zip")))

        assertTrue(RenduTournee.estRendue(d), "rendue après le rendu (spec § 4.1 et § 4.8)")
        val t = LecteurTournee.lire(gpkg)
        assertEquals(listOf("P35-01", "P35-02", "P35-03"), t.placettes.map { it.code }.sortedBy { it }, "la tournée reste consultable : ses placettes")
        assertEquals(avant, Contenu.de(gpkg), "la copie de la tournée n'est pas modifiée par le rendu (une tournée n'est jamais écrasée)")
        assertTrue(photo.contentEquals(Files.readAllBytes(d.resolve("DCIM/aller-P35-01.jpg"))))
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Critère 4 : le .zip du chemin de l'application équivaut à l'export que la CI d'import (F1 à F5) éprouve.

    @Test
    fun `le zip rendu porte les memes controles et les memes plants que l'export de la CI d'import`() {
        val d = dossierTournee()
        val j = jouer(d)
        val zip = dossier.resolve("app.zip")
        rendue(rendre(d, j.journal.lignesCourantes(), zip))
        val app = decompresser(zip, "app").resolve("travaux.gpkg")

        val depart = Files.createDirectories(dossier.resolve("depart-ci"))
        TourneeDeTest.couchey(depart, nom = "travaux.gpkg")
        val sortieCi = dossier.resolve("ci")
        ExporterScenario.jouer(depart, racine.resolve("fixtures/export/scenario-couchey.json"), sortieCi, essences)
        val ci = sortieCi.resolve("export-surculus/travaux.gpkg")

        // Les UUID diffèrent (journal d'un côté, scénario de l'autre) : on compare par code de placette et numéro de plant.
        val codesApp = codes(app)
        val codesCi = codes(ci)
        fun totaux(f: Path, codes: Map<String, String>) =
            Contenu.colonnes(f, "controles", "placette_uuid", "nb_total", "nb_vivants", "nb_abroutis", "h_moy_cm", "visite_le")
                .associate { codes.getValue(it[0] as String) to it.drop(1).map { v -> v?.toString() } }
        assertEquals(totaux(ci, codesCi), totaux(app, codesApp), "mêmes contrôles, mêmes totaux, même visite_le")

        val ref = LecteurTournee.lire(ci).plants.associateBy { "${codesCi[it.placetteUuid]}#${it.numero}" }
        val rendus = LecteurTournee.lire(app).plants.filter { codesApp[it.placetteUuid] != "P35-03" }.associateBy { "${codesApp[it.placetteUuid]}#${it.numero}" }
        assertEquals(ref.keys, rendus.keys, "mêmes numéros dans les mêmes placettes")
        for ((k, p) in rendus) {
            val r = ref.getValue(k)
            assertTrue(hypot(p.position!!.xMetres - r.position!!.xMetres, p.position!!.yMetres - r.position!!.yMetres) < 1e-6, "position du plant $k")
            assertEquals(r.essence, p.essence, "essence du plant $k")
        }
    }
}
