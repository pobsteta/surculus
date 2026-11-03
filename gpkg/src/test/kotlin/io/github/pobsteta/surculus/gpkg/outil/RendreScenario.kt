package io.github.pobsteta.surculus.gpkg.outil

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.placette.Essences
import io.github.pobsteta.surculus.core.reperes.AzimutSaisi
import io.github.pobsteta.surculus.core.reperes.CodesUniteAzimut
import io.github.pobsteta.surculus.core.reperes.Resultat
import io.github.pobsteta.surculus.core.session.Arrivee
import io.github.pobsteta.surculus.core.session.FixSession
import io.github.pobsteta.surculus.core.session.HorlogeSession
import io.github.pobsteta.surculus.core.session.JournalControle
import io.github.pobsteta.surculus.core.session.LigneCourante
import io.github.pobsteta.surculus.core.session.OuvertureControle
import io.github.pobsteta.surculus.core.session.SessionControle
import io.github.pobsteta.surculus.core.session.SourceAzimut
import io.github.pobsteta.surculus.core.session.SuiteSession
import io.github.pobsteta.surculus.gpkg.ContexteDeTournee
import io.github.pobsteta.surculus.gpkg.ImportTournee
import io.github.pobsteta.surculus.gpkg.LecteurTournee
import io.github.pobsteta.surculus.gpkg.RenduTournee
import io.github.pobsteta.surculus.gpkg.ResultatRendu
import io.github.pobsteta.surculus.gpkg.SaisiesDuJournal
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.UUID
import kotlin.system.exitProcess

/**
 * Outil de la CI (mission 090), jamais embarqué dans l'APK : le **chemin du rendu** de l'application, du journal au
 * `.zip`. Le scénario est joué geste par geste par la session de contrôle ([SessionControle]) sur un journal en mémoire,
 * puis rendu par [RenduTournee.rendre], comme le bouton « Rendre la tournée ». Seuls le journal Room et la publication
 * MediaStore ne passent pas ici.
 *
 * Usage : `./gradlew :gpkg:rendreScenario -Ptournee=<dossier> -Pscenario=<json> -Psortie=<dossier>`. `<sortie>` reçoit
 * `rendu-surculus.zip`, `rendu-surculus/` (le zip décompressé : l'entrée `--avec` de `ci/sommier/verifier.R`) et
 * `bilan-export.json` (critères F1 et F2, même format que [ExporterScenario]).
 * Code de retour : 0 rendu, 1 refusé ou impossible (fautes sur la sortie d'erreur), 2 mauvais usage.
 */
object RendreScenario {
    const val NOM = "rendu-surculus"

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size != 3) {
            System.err.println("usage : RendreScenario <dossier-tournee> <scenario.json> <dossier-sortie>")
            exitProcess(2)
        }
        when (val r = jouer(Paths.get(args[0]), Paths.get(args[1]), Paths.get(args[2]))) {
            is ResultatRendu.Rendue -> println("Rendu Surculus : ${r.bilan.controles} contrôle(s), ${r.bilan.plants} plant(s) dans ${r.zip}")
            is ResultatRendu.Refusee -> { r.fautes.forEach { System.err.println("Faute : ${it.texte}") }; exitProcess(1) }
            is ResultatRendu.Impossible -> { System.err.println("Rendu impossible : ${r.texte}"); exitProcess(1) }
        }
    }

    private class JournalMemoire : JournalControle {
        class L(val uuid: String, val objet: String, val type: String, val valeur: String?, val remplace: String?)
        val lignes = ArrayList<L>()
        private var n = 0
        override fun nouvelUuid(): String = UUID.nameUUIDFromBytes("rendre-scenario:${++n}".toByteArray()).toString()
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

    /** Joue le [scenario] par la session sur une copie de [dossierTournee] et rend la tournée sous [sortie]. */
    fun jouer(dossierTournee: Path, scenario: Path, sortie: Path, essences: Essences = Essences(ExporterScenario.lireEssences())): ResultatRendu {
        val json = JsonParser.parseString(Files.readString(scenario)).asJsonObject
        // La tournée de départ n'est jamais touchée : on travaille sur une copie, comme l'application (spec § 4.1).
        val copie = Files.createDirectories(sortie.resolve("tournee-rendue"))
        Files.copy(dossierTournee.resolve(ImportTournee.NOM_GPKG), copie.resolve(ImportTournee.NOM_GPKG), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        Files.createDirectories(copie.resolve("DCIM"))
        val tournee = LecteurTournee.lire(copie.resolve(ImportTournee.NOM_GPKG))
        val contexte = ContexteDeTournee.de(tournee).copy(operateur = json.texte("operateur")!!)
        val journal = JournalMemoire()
        val horloge = Horloge(0)
        for (c in json.getAsJsonArray("controles").map { it.asJsonObject }) {
            val code = c.get("placette").asString
            val piquet = tournee.placettes.single { it.code == code }.position!!
            horloge.ms = Instant.parse(c.get("visite_le").asString).toEpochMilli()
            val sansPosition = c.get("sans_position_mesuree")?.takeUnless { it.isJsonNull }?.asBoolean == true
            val ouverture = if (sansPosition) {
                SessionControle.ouvrir(contexte, journal, horloge, essences, Arrivee.SansPosition, code)
            } else {
                val m = c.getAsJsonObject("position_mesuree")
                val mesuree = PointLambert93(xMetres = piquet.xMetres + (m?.reel("decalage_est_m") ?: 0.0), yMetres = piquet.yMetres + (m?.reel("decalage_nord_m") ?: 0.0))
                SessionControle.ouvrir(contexte, journal, horloge, essences, Arrivee.Lambert93(mesuree))
            }
            val s = (ouverture as? OuvertureControle.Ouverte)?.session ?: error("$code : ouverture refusée ($ouverture)")
            for (p in c.getAsJsonArray("plants").map { it.asJsonObject }) {
                val unite = (CodesUniteAzimut.lire(p.get("unite_azimut").asString) as Resultat.Accepte).valeur
                val az = (AzimutSaisi.valider(p.get("azimut").asDouble, unite) as Resultat.Accepte).valeur
                check(s.pointer(p.get("distance_m").asDouble, az, SourceAzimut.SAISIE, p.texte("essence")) is SuiteSession.Fait) { "$code : pointage refusé" }
                val dit = s.dire(p.get("etat").asString, p.texte("cause_groupe"), p.texte("cause_detail"), p.get("hauteur_cm")?.takeUnless { it.isJsonNull }?.asInt)
                check(dit is SuiteSession.Fait) { "$code : $dit" }
            }
            check(s.terminer() is SuiteSession.Fait) { "$code : non terminé" }
        }
        val lignes = journal.lignesCourantes()
        val zip = sortie.resolve("$NOM.zip")
        val r = RenduTournee.rendre(copie, lignes, zip, json.get("version_surculus").asString, Instant.parse(json.get("exporte_le").asString), essences)
        if (r is ResultatRendu.Rendue) {
            val dossier = sortie.resolve(NOM)
            dossier.toFile().deleteRecursively()
            Files.newInputStream(zip).use { ImportTournee.extraireZip(it, dossier) }
            val saisies = SaisiesDuJournal.construire(tournee, lignes).saisies
            Files.writeString(sortie.resolve(ExporterScenario.BILAN_JSON), ExporterScenario.bilanJson(r.bilan, saisies).toString() + "\n")
        }
        return r
    }
}
