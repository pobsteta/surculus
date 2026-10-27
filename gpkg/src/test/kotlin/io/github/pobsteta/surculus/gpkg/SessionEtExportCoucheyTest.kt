package io.github.pobsteta.surculus.gpkg

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.placette.CodeEssence
import io.github.pobsteta.surculus.core.placette.EmpriseEtFrance
import io.github.pobsteta.surculus.core.placette.Essences
import io.github.pobsteta.surculus.core.placette.EtatPlant
import io.github.pobsteta.surculus.core.placette.Observation
import io.github.pobsteta.surculus.core.placette.Observations
import io.github.pobsteta.surculus.core.placette.Placette
import io.github.pobsteta.surculus.core.placette.Plant
import io.github.pobsteta.surculus.core.placette.Pointage
import io.github.pobsteta.surculus.core.placette.Totaux
import io.github.pobsteta.surculus.core.placette.UuidControle
import io.github.pobsteta.surculus.core.placette.UuidPlacette
import io.github.pobsteta.surculus.core.placette.UuidPlant
import io.github.pobsteta.surculus.core.placette.Verdict
import io.github.pobsteta.surculus.core.reperes.AzimutSaisi
import io.github.pobsteta.surculus.core.reperes.CodesUniteAzimut
import io.github.pobsteta.surculus.core.reperes.PointDerive
import io.github.pobsteta.surculus.core.reperes.Resultat
import io.github.pobsteta.surculus.core.session.Arrivee
import io.github.pobsteta.surculus.core.session.ContexteControle
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
import io.github.pobsteta.surculus.core.session.ValeurEtat
import io.github.pobsteta.surculus.core.session.ValeurPointage
import io.github.pobsteta.surculus.gpkg.outil.ExporterScenario
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Mission 012-A : le scénario de Couchey (`fixtures/export/scenario-couchey.json`) est **joué par la session de contrôle**
 * sur la tournée de départ, puis l'export relit le journal de la session : mêmes totaux que le registre attendu écrit
 * à la main (`registre-attendu-couchey.json`), mêmes plants, mêmes numéros et mêmes positions que l'export direct du
 * scénario (outil de la CI), à moins d'un micromètre. Le format de `valeur` (docs/formats/journal-valeurs-v1.md) porte
 * donc tout ce que l'export lui demande.
 */
class SessionEtExportCoucheyTest {
    @TempDir
    lateinit var dossier: Path

    private val racine: Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }.first { Files.exists(it.resolve("settings.gradle.kts")) }
    private val scenario = JsonParser.parseString(Files.readString(racine.resolve("fixtures/export/scenario-couchey.json"))).asJsonObject
    private val registre = JsonParser.parseString(Files.readString(racine.resolve("fixtures/export/registre-attendu-couchey.json"))).asJsonObject
    private val essences = Essences(ExporterScenario.lireEssences())

    /** Journal en mémoire aux UUID valides (v4 déterministes), règles de projection de la spec § 5. */
    private class JournalMemoire : JournalControle {
        class L(val uuid: String, val objet: String, val type: String, val valeur: String?, val remplace: String?)
        val lignes = ArrayList<L>()
        private var n = 0
        override fun nouvelUuid(): String = UUID.nameUUIDFromBytes("session-couchey:${++n}".toByteArray()).toString()
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

    private fun JsonObject.texte(cle: String) = get(cle)?.takeUnless { it.isJsonNull }?.asString
    private fun JsonObject.reel(cle: String) = get(cle)?.takeUnless { it.isJsonNull }?.asDouble

    private fun rejouer(source: Path): JournalMemoire {
        val tournee = LecteurTournee.lire(source)
        val contexte: ContexteControle = ContexteDeTournee.de(tournee).copy(operateur = scenario.texte("operateur")!!)
        val journal = JournalMemoire()
        val horloge = Horloge(0)
        for (c in scenario.getAsJsonArray("controles").map { it.asJsonObject }) {
            val code = c.get("placette").asString
            val piquet = tournee.placettes.single { it.code == code }.position!!
            horloge.ms = Instant.parse(c.get("visite_le").asString).toEpochMilli()
            val m = c.getAsJsonObject("position_mesuree")
            val mesuree = PointLambert93(piquet.xMetres + (m?.reel("decalage_est_m") ?: 0.0), piquet.yMetres + (m?.reel("decalage_nord_m") ?: 0.0))
            val s = (SessionControle.ouvrir(contexte, journal, horloge, essences, Arrivee.Lambert93(mesuree)) as OuvertureControle.Ouverte).session
            for (p in c.getAsJsonArray("plants").map { it.asJsonObject }) {
                val unite = (CodesUniteAzimut.lire(p.get("unite_azimut").asString) as Resultat.Accepte).valeur
                val az = (AzimutSaisi.valider(p.get("azimut").asDouble, unite) as Resultat.Accepte).valeur
                assertTrue(s.pointer(p.get("distance_m").asDouble, az, SourceAzimut.SAISIE, p.texte("essence")) is SuiteSession.Fait)
                val dit = s.dire(p.get("etat").asString, p.texte("cause_groupe"), p.texte("cause_detail"), p.get("hauteur_cm")?.asInt)
                assertTrue(dit is SuiteSession.Fait, "$code : $dit")
            }
            assertTrue(s.terminer() is SuiteSession.Fait)
        }
        return journal
    }

    /** Ce que fera la mission d'export : relire les valeurs du journal et reconstruire les saisies du domaine. */
    private fun saisiesDuJournal(j: JournalMemoire, tournee: Tournee): Saisies {
        val courantes = j.lignesCourantes()
        val piquets = tournee.placettes.associate { it.uuid to it.position!! }
        val controles = ArrayList<ControleSaisi>()
        val plants = ArrayList<PlantNumerote>()
        for (lc in courantes.filter { it.type == FormatValeurs.TYPE_CONTROLE }) {
            val c = ValeurControle.lire(lc.valeur)!!
            check(c.termine)
            val piquet = piquets.getValue(c.placetteUuid)
            val domaine = Placette(UuidPlacette(c.placetteUuid), 3.99)
            val deCeControle = courantes.filter { it.type == FormatValeurs.TYPE_POINTAGE }.mapNotNull { l -> ValeurPointage.lire(l.valeur)?.let { l to it } }
                .filter { it.second.controleUuid == lc.objetUuid }
            val plantsDuControle = deCeControle.map { (l, v) ->
                val unite = (CodesUniteAzimut.lire(v.uniteAzimut) as Resultat.Accepte).valeur
                val az = (AzimutSaisi.valider(v.azimutMag, unite) as Resultat.Accepte).valeur
                val derive = (PointDerive.deriver(piquet, tournee.epsg, v.distanceHorizontaleM, az, LocalDate.parse(v.jourDeclinaison)) as Resultat.Accepte).valeur
                // Le journal garde les termes dérivés : la recomposition par la chaîne des repères les retrouve à l'identique.
                assertEquals(v.x, derive.position.xMetres, 1e-9)
                assertEquals(v.y, derive.position.yMetres, 1e-9)
                assertEquals(v.declinaisonDeg, derive.orientation.declinaisonDeg, 1e-12)
                val plant: Plant = (
                    Pointage.pointer(UuidPlant(l.objetUuid), domaine, derive, Instant.ofEpochMilli(v.pointeLeUtcMs), v.essence?.let(::CodeEssence), null, v.horsRayonConfirme) as Verdict.Accepte
                    ).valeur
                PlantNumerote(plant, c.numeros!!.getValue(l.objetUuid))
            }
            val etats = courantes.filter { it.type == FormatValeurs.TYPE_ETAT }.mapNotNull { l -> ValeurEtat.lire(l.valeur)?.let { l to it } }
                .filter { it.second.controleUuid == lc.objetUuid }
                .map { (l, v) ->
                    val o: Observation = (Observations.valider(v.etat, v.causeGroupe, v.causeDetail, v.hauteurCm) as Verdict.Accepte).valeur
                    LigneEtat(l.objetUuid, UuidPlant(v.plantUuid), o)
                }
            val totaux = (Totaux.deduire(etats.map { EtatPlant(it.plant, it.observation) }) as Verdict.Accepte).valeur
            controles += ControleSaisi(
                UuidControle(lc.objetUuid), UuidPlacette(c.placetteUuid), Instant.ofEpochMilli(c.visiteLeUtcMs), LocalDate.parse(c.jourCivil),
                PointLambert93(c.positionX, c.positionY), PositionMesuree(PointLambert93(c.positionMesureeX, c.positionMesureeY)),
                etats, totaux = totaux, operateur = c.operateur,
            )
            plants += plantsDuControle
        }
        return Saisies(scenario.texte("operateur"), controles = controles, plants = plants)
    }

    private fun tourneeDeDepart(): Path {
        val d = Files.createDirectories(dossier.resolve("depart"))
        return TourneeDeTest.couchey(d, nom = "travaux.gpkg")
    }

    @Test
    fun `le scenario de Couchey joue par la session puis relu par l'export donne le registre attendu et les memes plants que l'export direct`() {
        val source = tourneeDeDepart()
        val journal = rejouer(source)
        val tournee = LecteurTournee.lire(source)
        val saisies = saisiesDuJournal(journal, tournee)
        val sortie = dossier.resolve("par-la-session.gpkg")
        ExportTravaux.exporter(source, sortie, saisies, "0.0.0-session", Instant.parse(scenario.texte("exporte_le")!!), essences)
        val relu = LecteurTournee.lire(sortie)
        assertEquals(2, relu.controles.size)
        assertEquals(8, relu.plants.size)
        assertEquals(8, relu.etats.size)

        // Totaux du registre attendu, écrit à la main par le testeur (mission 027), sans passer par le code de l'export.
        val codes = relu.placettes.associate { it.uuid to it.code }
        val colonnes = Contenu.colonnes(sortie, "controles", "placette_uuid", "nb_total", "nb_vivants", "nb_abroutis", "h_moy_cm")
        for (attendu in registre.getAsJsonArray("controles").map { it.asJsonObject }) {
            val ligne = colonnes.single { codes[it[0]] == attendu.get("placette").asString }
            assertEquals(
                listOf(attendu.get("nb_total").asInt, attendu.get("nb_vivants").asInt, attendu.get("nb_abroutis").asInt, attendu.get("h_moy_cm").asInt),
                ligne.drop(1).map { (it as Number).toInt() }, "totaux de ${attendu.get("placette").asString}",
            )
        }

        // Mêmes plants, mêmes numéros, mêmes positions que l'export direct du scénario (l'outil de la CI).
        val direct = dossier.resolve("direct")
        ExporterScenario.jouer(Files.createDirectories(dossier.resolve("depart-direct")).also { TourneeDeTest.couchey(it, nom = "travaux.gpkg") }, racine.resolve("fixtures/export/scenario-couchey.json"), direct, essences)
        val ref = LecteurTournee.lire(direct.resolve("export-surculus/travaux.gpkg"))
        fun cle(p: PlantLu) = "${p.placetteUuid}#${p.numero}"
        val parCle = ref.plants.associateBy(::cle)
        assertEquals(parCle.keys, relu.plants.map(::cle).toSet(), "mêmes numéros dans les mêmes placettes")
        for (p in relu.plants) {
            val r = parCle.getValue(cle(p))
            assertTrue(hypot(p.position!!.xMetres - r.position!!.xMetres, p.position!!.yMetres - r.position!!.yMetres) < 1e-6, "position du plant ${cle(p)}")
            assertEquals(r.essence, p.essence)
        }
        // Chaque plant pointé est en France et dans le rayon (+ tolérance) de sa placette.
        val piquets = relu.placettes.associate { it.uuid to it.position!! }
        for (p in relu.plants) {
            assertTrue(EmpriseEtFrance.enFrance(p.position!!))
            val q = piquets.getValue(p.placetteUuid)
            assertTrue(abs(hypot(p.position!!.xMetres - q.xMetres, p.position!!.yMetres - q.yMetres)) <= 3.99 + 0.1)
        }
    }
}
