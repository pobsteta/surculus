package io.github.pobsteta.surculus.gpkg.outil

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.placette.CodeEssence
import io.github.pobsteta.surculus.core.placette.Essences
import io.github.pobsteta.surculus.core.placette.EtatPlant
import io.github.pobsteta.surculus.core.placette.JourCivil
import io.github.pobsteta.surculus.core.placette.Numerotation
import io.github.pobsteta.surculus.core.placette.NumerosPlacette
import io.github.pobsteta.surculus.core.placette.Observation
import io.github.pobsteta.surculus.core.placette.Observations
import io.github.pobsteta.surculus.core.placette.Placette
import io.github.pobsteta.surculus.core.placette.PlantANumeroter
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
import io.github.pobsteta.surculus.gpkg.BilanExport
import io.github.pobsteta.surculus.gpkg.ControleSaisi
import io.github.pobsteta.surculus.gpkg.ExportTravaux
import io.github.pobsteta.surculus.gpkg.ExportZip
import io.github.pobsteta.surculus.gpkg.Geometrie
import io.github.pobsteta.surculus.gpkg.LecteurTournee
import io.github.pobsteta.surculus.gpkg.LigneEtat
import io.github.pobsteta.surculus.gpkg.PlantNumerote
import io.github.pobsteta.surculus.gpkg.PositionMesuree
import io.github.pobsteta.surculus.gpkg.Saisies
import io.github.pobsteta.surculus.gpkg.Tournee
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlin.system.exitProcess

/**
 * Outil de la CI (mission 007), jamais embarqué dans l'APK : joue un scénario de saisies sur une tournée de
 * départ de sommieR, avec le même code que l'application (`:core` pour les repères, les états et les totaux,
 * `:gpkg` pour l'écriture), et dépose l'export `.zip` et décompressé.
 *
 * Usage : `./gradlew :gpkg:exporterScenario -Ptournee=<dossier> -Pscenario=<json> -Psortie=<dossier>`
 *
 * - `<dossier>` de la tournée : celui qui contient `travaux.gpkg` (`ci/sommier/sortie/tournee-sans`) ;
 * - `<sortie>` : reçoit `export-surculus.zip`, `export-surculus/travaux.gpkg` (le zip décompressé) et `bilan-export.json`.
 *
 * Le scénario désigne les placettes **par leur code** (`P35-01`) : la CI régénère la démo à chaque exécution,
 * donc ses UUID changent, pas ses codes. Les UUID de l'export dérivent du scénario (UUID v3 du nom).
 * Code de retour : 0 réussi, 1 refus (message sur la sortie d'erreur), 2 mauvais usage.
 */
object ExporterScenario {
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size != 3) {
            System.err.println("usage : ExporterScenario <dossier-tournee> <scenario.json> <dossier-sortie>")
            exitProcess(2)
        }
        try {
            val bilan = jouer(Paths.get(args[0]), Paths.get(args[1]), Paths.get(args[2]))
            println(bilan)
        } catch (e: ScenarioInvalide) {
            System.err.println("Scénario refusé : ${e.message}")
            exitProcess(1)
        } catch (e: io.github.pobsteta.surculus.gpkg.GpkgRefuse) {
            System.err.println("Export refusé (${e.refus.motif}) : ${e.message}")
            exitProcess(1)
        }
    }

    class ScenarioInvalide(message: String) : Exception(message)

    /** Joue le [scenario] sur la tournée de [dossierTournee] ; écrit sous [sortie]. Rend un résumé d'une ligne. */
    fun jouer(dossierTournee: Path, scenario: Path, sortie: Path, essences: Essences = Essences(lireEssences())): String {
        val source = dossierTournee.resolve("travaux.gpkg")
        if (!Files.isRegularFile(source)) throw ScenarioInvalide("pas de travaux.gpkg dans $dossierTournee")
        val json = JsonParser.parseString(Files.readString(scenario)).asJsonObject
        val tournee = LecteurTournee.lire(source)
        val saisies = construire(json, tournee, essences)
        val dossierExport = sortie.resolve("export-surculus")
        Files.createDirectories(dossierExport)
        val bilan = ExportTravaux.exporter(
            source, dossierExport.resolve(ExportZip.GPKG), saisies,
            json.get("version_surculus").asString, Instant.parse(json.get("exporte_le").asString), essences,
        )
        ExportZip.ecrire(dossierExport, sortie.resolve("export-surculus.zip"))
        Files.writeString(sortie.resolve(BILAN_JSON), bilanJson(bilan, saisies).toString() + "\n")
        return "Export Surculus : ${bilan.controles} contrôle(s), ${bilan.plants} plant(s), ${bilan.etats} état(s) écrits " +
            "dans ${dossierExport.resolve(ExportZip.GPKG)} (EPSG:${tournee.epsg.code}) et ${sortie.resolve("export-surculus.zip")}"
    }

    /** Nom du bilan d'export, écrit à côté du zip (mission 026, critère F1 de l'ADR 0007). */
    const val BILAN_JSON = "bilan-export.json"

    /**
     * Bilan d'export en JSON, lu par la CI d'import (`ci/sommier/verifier.R`, critères F1 et F2). Les compteurs sont
     * ceux de [BilanExport] ; `travaux` et `uuid_ecrits` viennent des [saisies] (UUID en minuscules, triés), que
     * l'export écrit dans les couches de sommieR. Limite : ces UUID viennent du même code que l'export
     * (`uuidDe`), le registre écrit à la main (mission 027) lèvera ce cercle.
     */
    fun bilanJson(bilan: BilanExport, saisies: Saisies): JsonObject {
        fun uuids(l: List<String>) = JsonArray().also { a -> l.map { it.lowercase() }.sorted().forEach { a.add(it) } }
        return JsonObject().apply {
            addProperty("exporte_le", bilan.exporteLe.toString())
            addProperty("placettes", bilan.placettesInstallees)
            addProperty("controles", bilan.controles)
            addProperty("travaux", saisies.travaux.size)
            addProperty("plants", bilan.plants)
            addProperty("etats_plants", bilan.etats)
            addProperty("positions_controles", bilan.positions)
            addProperty("constats_reprise", bilan.constats)
            addProperty("vides", bilan.vides)
            addProperty("deja_presentes", bilan.dejaPresentes)
            add(
                "uuid_ecrits",
                JsonObject().apply {
                    add("placettes", uuids(saisies.placettesInstallees.map { it.uuid.valeur }))
                    add("controles", uuids(saisies.controles.map { it.uuid.valeur }))
                    add("travaux_pt", uuids(saisies.travaux.filter { it.geometrie is Geometrie.Point }.map { it.uuid }))
                    add("travaux_surf", uuids(saisies.travaux.filter { it.geometrie !is Geometrie.Point }.map { it.uuid }))
                },
            )
        }
    }

    /** Codes d'essence de `docs/referentiels/essences.csv`, lus à la racine du dépôt (jamais recopiés). */
    fun lireEssences(): Set<String> {
        var d: Path? = Paths.get("").toAbsolutePath()
        while (d != null && !Files.exists(d.resolve("docs/referentiels/essences.csv"))) d = d.parent
        val csv = d?.resolve("docs/referentiels/essences.csv") ?: throw ScenarioInvalide("docs/referentiels/essences.csv introuvable")
        return Files.readAllLines(csv).filter { !it.startsWith("#") && !it.startsWith("code_onf") && it.isNotBlank() }
            .map { it.substringBefore(',').trim('"') }.toSet()
    }

    private fun uuidDe(nom: String): String = UUID.nameUUIDFromBytes(("surculus-scenario:$nom").toByteArray(Charsets.UTF_8)).toString()

    private fun JsonObject.texte(cle: String): String? = if (has(cle) && !get(cle).isJsonNull) get(cle).asString else null
    private fun JsonObject.reel(cle: String): Double? = if (has(cle) && !get(cle).isJsonNull) get(cle).asDouble else null

    fun construire(json: JsonObject, tournee: Tournee, essences: Essences): Saisies {
        val operateur = json.texte("operateur")
        val controles = ArrayList<ControleSaisi>()
        val plants = ArrayList<PlantNumerote>()
        for (c in json.getAsJsonArray("controles").map { it.asJsonObject }) {
            val code = c.get("placette").asString
            val candidates = tournee.placettes.filter { it.code == code }
            val placette = candidates.singleOrNull() ?: throw ScenarioInvalide("placette « $code » : ${candidates.size} trouvée(s) dans la tournée, une attendue")
            val piquet = placette.position ?: throw ScenarioInvalide("placette « $code » sans position")
            val rayon = placette.rayonM ?: throw ScenarioInvalide("placette « $code » sans rayon")
            val visite = Instant.parse(c.get("visite_le").asString)
            val jour = LocalDate.ofInstant(visite, ZoneOffset.UTC)
            val domaine = Placette(UuidPlacette(placette.uuid.lowercase()), rayon)

            val pointes = ArrayList<Triple<String, io.github.pobsteta.surculus.core.placette.Plant, Observation>>()
            for (p in c.getAsJsonArray("plants").map { it.asJsonObject }) {
                val cle = p.get("cle").asString
                val unite = (CodesUniteAzimut.lire(p.get("unite_azimut").asString) as? Resultat.Accepte)?.valeur
                    ?: throw ScenarioInvalide("plant $code/$cle : unité d'azimut inconnue")
                val azimut = (AzimutSaisi.valider(p.get("azimut").asDouble, unite) as? Resultat.Accepte)?.valeur
                    ?: throw ScenarioInvalide("plant $code/$cle : azimut hors plage")
                val derive = (PointDerive.deriver(piquet, tournee.epsg, p.get("distance_m").asDouble, azimut, jour) as? Resultat.Accepte)?.valeur
                    ?: throw ScenarioInvalide("plant $code/$cle : position non dérivable")
                val essence = p.texte("essence")?.let { (essences.valider(it) as? Verdict.Accepte)?.valeur ?: throw ScenarioInvalide("plant $code/$cle : essence « $it » absente de essences.csv") }
                val plant = (Pointage.pointer(UuidPlant(uuidDe("plant:${placette.uuid}:$cle")), domaine, derive, visite.minusSeconds(1800), essence) as? Verdict.Accepte)?.valeur
                    ?: throw ScenarioInvalide("plant $code/$cle : pointage refusé (hors rayon ?)")
                val observation = (
                    Observations.valider(
                        p.get("etat").asString, p.texte("cause_groupe"), p.texte("cause_detail"), if (p.has("hauteur_cm")) p.get("hauteur_cm").asInt else null,
                    ) as? Verdict.Accepte
                    )?.valeur ?: throw ScenarioInvalide("plant $code/$cle : observation refusée")
                pointes += Triple(cle, plant, observation)
            }
            val numeros = (
                Numerotation.figer(Numerotation.provisoires(pointes.map { PlantANumeroter.de(it.second.uuid, it.second.derive) })) as Verdict.Accepte<NumerosPlacette.Figes>
                ).valeur.numeros
            val etats = pointes.map { (cle, plant, obs) -> LigneEtat(uuidDe("etat:${placette.uuid}:${visite}:$cle"), plant.uuid, obs) }
            val totaux = (Totaux.deduire(etats.map { EtatPlant(it.plant, it.observation) }) as Verdict.Accepte).valeur
            val mesuree = c.getAsJsonObject("position_mesuree")?.let { m ->
                PositionMesuree(
                    PointLambert93(piquet.xMetres + (m.reel("decalage_est_m") ?: 0.0), piquet.yMetres + (m.reel("decalage_nord_m") ?: 0.0)),
                    m.reel("precision_m"), m.texte("source_gnss"), m.texte("referentiel_source"), m.get("simulee")?.asBoolean ?: false,
                )
            }
            controles += ControleSaisi(
                UuidControle(uuidDe("controle:${placette.uuid}:$visite")), domaine.uuid, visite, JourCivil.de(visite), mesuree?.position ?: piquet, mesuree,
                etats, totaux = totaux, operateur = operateur, concurrence = c.texte("concurrence"), besoin = c.texte("besoin"),
                observations = c.texte("observations"),
            )
            plants += pointes.map { PlantNumerote(it.second, numeros.getValue(it.second.uuid)) }
        }
        return Saisies(operateur, controles = controles, plants = plants)
    }
}
