package io.github.pobsteta.surculus.gpkg.acceptation

import com.google.gson.JsonObject
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.placette.CodeEssence
import io.github.pobsteta.surculus.core.placette.Essences
import io.github.pobsteta.surculus.core.placette.EtatPlant
import io.github.pobsteta.surculus.core.placette.JourCivil
import io.github.pobsteta.surculus.core.placette.Observation
import io.github.pobsteta.surculus.core.placette.Observations
import io.github.pobsteta.surculus.core.placette.Placette
import io.github.pobsteta.surculus.core.placette.Pointage
import io.github.pobsteta.surculus.core.placette.RefusPlacette
import io.github.pobsteta.surculus.core.placette.Totaux
import io.github.pobsteta.surculus.core.placette.UuidControle
import io.github.pobsteta.surculus.core.placette.UuidPlacette
import io.github.pobsteta.surculus.core.placette.UuidPlant
import io.github.pobsteta.surculus.core.placette.Verdict
import io.github.pobsteta.surculus.core.reperes.AzimutSaisi
import io.github.pobsteta.surculus.core.reperes.CodeEpsg
import io.github.pobsteta.surculus.core.reperes.CodesUniteAzimut
import io.github.pobsteta.surculus.core.reperes.PointDerive
import io.github.pobsteta.surculus.core.reperes.Resultat
import io.github.pobsteta.surculus.gpkg.ControleSaisi
import io.github.pobsteta.surculus.gpkg.ExportTravaux
import io.github.pobsteta.surculus.gpkg.ExportZip
import io.github.pobsteta.surculus.gpkg.Geometrie
import io.github.pobsteta.surculus.gpkg.GeometrieGpkg
import io.github.pobsteta.surculus.gpkg.GpkgRefuse
import io.github.pobsteta.surculus.gpkg.LecteurTournee
import io.github.pobsteta.surculus.gpkg.LigneEtat
import io.github.pobsteta.surculus.gpkg.PlantNumerote
import io.github.pobsteta.surculus.gpkg.Saisies
import io.github.pobsteta.surculus.gpkg.Sqlite
import io.github.pobsteta.surculus.gpkg.TourneeDeTest
import io.github.pobsteta.surculus.gpkg.TravauxSaisi
import io.github.pobsteta.surculus.gpkg.outil.ExporterScenario
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

/**
 * Le seul point que la mission 006 modifie : l'export réel de `:gpkg` derrière [ExportTournee].
 *
 * Ce que fait le branchement, et seulement cela :
 * 1. il **construit une vraie tournée gpkg** à partir de `tournee_depart` (le modèle de sommieR 0.31.1, ses placettes,
 *    sa couche `codes`, les plants de la tournée précédente), si bien que le repère passe par le fichier
 *    (`gpkg_spatial_ref_sys`, `gpkg_contents`) et que l'export le lit, au lieu de recevoir `repere_epsg` ;
 * 2. il traduit les saisies en objets du domaine (`:core`), comme l'application : la chaîne des repères dérive la
 *    position des plants, les totaux sont déduits des états ;
 * 3. il appelle [ExportTravaux.exporter] puis [ExportZip.ecrire] ; le motif du refus est celui, stable, de l'export.
 *
 * Seuls deux refus sont rendus par le domaine avant l'export, parce que l'objet ne se construit pas : un état ou une
 * cause hors référentiel (`Observations.valider`, rendu `hors_referentiel`) et deux plants de même UUID dans un
 * contrôle (`Totaux.deduire`, rendu `uuid_en_double`). Tous les autres sont rendus par [ExportTravaux], avec ses codes.
 */
fun exportATester(): ExportTournee = ExportReel

private object ExportReel : ExportTournee {
    private val essences = Essences(ExporterScenario.lireEssences())
    private val horloge = Instant.parse("2026-10-17T12:00:00Z")
    private const val VERSION = "0.1.0-acceptation"
    private const val TRAVAUX_ID = "11111111-1111-4111-8111-111111111111"

    /** Refus de la saisie avant l'export (traduction de motifs du domaine). */
    private class RefusAvantExport(val motif: String) : Exception(motif)

    override fun exporter(entree: JsonObject, dossier: Path): ResultatExport {
        val depart = entree.getAsJsonObject("tournee_depart")
        val epsg = depart.get("repere_epsg").asInt
        return try {
            val source = construireTournee(depart, dossier.resolve("depart"), epsg)
            val tournee = LecteurTournee.lire(source)
            val saisies = saisies(entree.getAsJsonObject("saisies"), depart, tournee.epsg, tournee.placettes.associate { it.uuid to it.position!! })
            val sortie = dossier.resolve("export-surculus")
            Files.createDirectories(sortie.resolve("DCIM")) // l'aller porte un dossier DCIM/ (photos), même vide
            ExportTravaux.exporter(source, sortie.resolve(ExportZip.GPKG), saisies, VERSION, horloge, essences)
            val zip = dossier.resolve("export-surculus.zip")
            ExportZip.ecrire(sortie, zip)
            ResultatExport.Accepte(zip)
        } catch (e: GpkgRefuse) {
            ResultatExport.Refuse(e.refus.motif)
        } catch (e: RefusAvantExport) {
            ResultatExport.Refuse(e.motif)
        }
    }

    override fun relire(gpkg: Path): TourneeLue {
        val t = LecteurTournee.lire(gpkg)
        val connues = io.github.pobsteta.surculus.gpkg.Couches.TOUTES.associate { c -> c.nom to c.colonnes.map { it.nom }.toSet() }
        // La lecture ne retient que les couches et les champs qu'elle connaît ; le reste du fichier est ignoré.
        Sqlite.ouvrir(gpkg, lectureSeule = true).use { cnx ->
            val retenues = t.nomsCouches.filter { it in connues }
            return TourneeLue(
                retenues.associateWith { couche ->
                    val presentes = HashSet<String>()
                    cnx.createStatement().use { st ->
                        st.executeQuery("PRAGMA table_info(\"$couche\")").use { rs -> while (rs.next()) presentes += rs.getString("name") }
                    }
                    connues.getValue(couche).filter { it in presentes }.toSet()
                },
            )
        }
    }

    // ---- la tournée de départ, en vrai fichier gpkg

    private fun construireTournee(depart: JsonObject, dossier: Path, epsg: Int): Path {
        Files.createDirectories(dossier)
        val f = TourneeDeTest.copierModele(dossier.resolve("travaux.gpkg"))
        Sqlite.ouvrir(f).use { cnx ->
            if (epsg != 2154) basculer(cnx, epsg, null)
            cnx.createStatement().use { st ->
                st.executeUpdate("INSERT INTO suivis (id, libelle, ug) VALUES ('$TRAVAUX_ID', 'Plantation (essai)', '1')")
            }
            depart.getAsJsonArray("codes")?.forEach {
                val c = it.asJsonObject
                cnx.prepareStatement("INSERT INTO codes (code, libelle) VALUES (?, ?)").use { ps ->
                    ps.setString(1, c.get("code").asString); ps.setString(2, c.get("libelle")?.asString)
                    ps.executeUpdate()
                }
            }
            for (p in depart.getAsJsonArray("placettes").map { it.asJsonObject }) {
                cnx.prepareStatement(
                    "INSERT INTO placettes (geom, uuid, code_placette, travaux_id, rayon_m, ug) VALUES (?, ?, ?, ?, 3.99, '1')",
                ).use { ps ->
                    ps.setBytes(1, GeometrieGpkg.encoder(Geometrie.Point(PointLambert93(p.get("x").asDouble, p.get("y").asDouble)), epsg))
                    ps.setString(2, p.get("uuid").asString); ps.setString(3, p.get("code_placette").asString); ps.setString(4, TRAVAUX_ID)
                    ps.executeUpdate()
                }
            }
        }
        val precedents = depart.getAsJsonArray("plants_tournee_precedente").map { it.asJsonObject }
        if (precedents.isNotEmpty()) ajouterPlantsPrecedents(f, precedents, depart, epsg)
        return f
    }

    /** Passe les couches à géométrie en [epsg] (ou seulement [couche] si elle est donnée). */
    private fun basculer(cnx: java.sql.Connection, epsg: Int, couche: String?) {
        val filtre = if (couche == null) "" else " WHERE table_name = '$couche'"
        cnx.createStatement().use { st ->
            st.executeUpdate("INSERT OR IGNORE INTO gpkg_spatial_ref_sys VALUES ('RGF93 v2b / Lambert-93 (essai)', $epsg, 'EPSG', $epsg, 'undefined', NULL)")
            st.executeUpdate("UPDATE gpkg_geometry_columns SET srs_id = $epsg$filtre")
            st.executeUpdate("UPDATE gpkg_contents SET srs_id = $epsg WHERE data_type = 'features'" + if (couche == null) "" else " AND table_name = '$couche'")
        }
    }

    /**
     * La tournée précédente : ses plants sont déjà dans la couche `plants` du fichier de départ (spec § 5, « plants à
     * l'aller »). On les y écrit avec l'export réel, puis, si leur repère diffère de celui de la tournée (EX-14b), on
     * déclare la couche `plants` dans cet autre repère : un fichier qui mêle deux repères, que la lecture doit refuser.
     */
    private fun ajouterPlantsPrecedents(f: Path, precedents: List<JsonObject>, depart: JsonObject, epsg: Int) {
        val tournee = LecteurTournee.lire(f)
        val placettes = tournee.placettes.associateBy { it.uuid }
        val plants = precedents.map { p ->
            val placette = placettes.getValue(p.get("placette_uuid").asString)
            val derive = deriver(placette.position!!, tournee.epsg, 1.0, 0.0, LocalDate.of(2026, 10, 1))
            val plant = io.github.pobsteta.surculus.core.placette.Plant(
                UuidPlant(p.get("uuid").asString), UuidPlacette(placette.uuid), p.get("essence")?.asString?.let(::CodeEssence),
                null, Instant.parse("2026-10-01T08:00:00Z"), derive,
            )
            PlantNumerote(plant, p.get("numero").asInt)
        }
        val tmp = f.resolveSibling("precedent.gpkg")
        ExportTravaux.exporter(f, tmp, Saisies(operateur = "tournée précédente", plants = plants), "precedente", horloge)
        Files.move(tmp, f, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        precedents.firstNotNullOfOrNull { it.get("repere_epsg")?.asInt }?.takeIf { it != epsg }?.let { autre ->
            Sqlite.ouvrir(f).use { cnx -> basculer(cnx, autre, "plants") }
        }
    }

    // ---- les saisies, en objets du domaine

    private fun deriver(piquet: PointLambert93, epsg: CodeEpsg, distanceM: Double, azimutDeg: Double, jour: LocalDate) =
        (PointDerive.deriver(piquet, epsg, distanceM, ((AzimutSaisi.valider(azimutDeg, io.github.pobsteta.surculus.core.geo.UniteAzimut.DEGRES)) as Resultat.Accepte).valeur, jour) as? Resultat.Accepte)?.valeur
            ?: throw RefusAvantExport("hors_france")

    private fun uuidDe(nom: String): String = UUID.nameUUIDFromBytes(("acceptation-export:$nom").toByteArray(Charsets.UTF_8)).toString()

    private fun JsonObject.texte(cle: String): String? = if (has(cle) && !get(cle).isJsonNull) get(cle).asString else null
    private fun JsonObject.entier(cle: String): Int? = if (has(cle) && !get(cle).isJsonNull) get(cle).asInt else null

    private fun saisies(s: JsonObject, depart: JsonObject, epsg: CodeEpsg, piquets: Map<String, PointLambert93>): Saisies {
        val operateur = s.texte("operateur")
        val rayons = 3.99
        val controles = ArrayList<ControleSaisi>()
        val plants = ArrayList<PlantNumerote>()
        for (c in s.getAsJsonArray("controles").map { it.asJsonObject }) {
            val uuidControle = c.get("uuid").asString
            val placetteUuid = c.get("placette_uuid").asString
            val piquet = piquets[placetteUuid] ?: throw RefusAvantExport("placette_inconnue")
            val visite = Instant.parse(c.get("visite_le").asString)
            val pos = c.getAsJsonObject("position")
            val position = PointLambert93(pos.get("x").asDouble, pos.get("y").asDouble)
            val repere = pos.entier("repere_epsg")?.let { code -> CodeEpsg.entries.first { it.code == code } }
            val domaine = Placette(UuidPlacette(placetteUuid), rayons)
            val lignes = ArrayList<LigneEtat>()
            val remplaces = HashSet<UuidPlant>()
            for (p in c.getAsJsonArray("plants").map { it.asJsonObject }) {
                val id = p.get("uuid").asString
                val unite = (CodesUniteAzimut.lire(p.get("unite_azimut").asString) as Resultat.Accepte).valeur
                val azimut = (AzimutSaisi.valider(p.get("azimut_mag").asDouble, unite) as Resultat.Accepte).valeur
                val derive = (PointDerive.deriver(piquet, epsg, p.get("distance_m").asDouble, azimut, LocalDate.ofInstant(visite, ZoneOffset.UTC)) as? Resultat.Accepte)?.valeur
                    ?: throw RefusAvantExport("hors_france")
                val remplace = p.texte("remplace_uuid")?.let(::UuidPlant)
                val essence = p.texte("essence")?.let(::CodeEssence)
                val plant = (Pointage.pointer(UuidPlant(id), domaine, derive, visite.minusSeconds(1800), essence, remplace, confirmeHorsRayon = true) as Verdict.Accepte).valeur
                val observation = when (val v = Observations.valider(p.get("etat").asString, p.texte("cause_groupe"), p.texte("cause_detail"), p.entier("hauteur_cm"))) {
                    is Verdict.Accepte -> v.valeur
                    is Verdict.Refuse -> throw RefusAvantExport(
                        when (v.refus) {
                            is RefusPlacette.EtatInconnu, is RefusPlacette.GroupeCauseInconnu -> "hors_referentiel"
                            else -> v.refus.motif
                        },
                    )
                    else -> error("Verdict inattendu : $v")
                }
                if (remplace != null) remplaces += remplace
                plants += PlantNumerote(plant, p.get("numero").asInt)
                lignes += LigneEtat(uuidDe("etat:$uuidControle:$id"), plant.uuid, observation)
            }
            val totaux = when (val v = Totaux.deduire(lignes.map { EtatPlant(it.plant, it.observation) }, remplaces)) {
                is Verdict.Accepte -> v.valeur
                // Deux plants de même UUID dans un contrôle : le domaine le refuse avant l'export (EX-30), c'est un UUID en double.
                is Verdict.Refuse -> throw RefusAvantExport(if (v.refus is RefusPlacette.PlantEnDoubleDansControle) "uuid_en_double" else v.refus.motif)
                else -> error("Verdict inattendu : $v")
            }
            controles += ControleSaisi(
                UuidControle(uuidControle), UuidPlacette(placetteUuid), visite, LocalDate.parse(c.get("jour_civil").asString), position,
                etats = lignes, remplaces = remplaces, totaux = totaux, operateur = operateur, repere = repere,
            )
        }
        val travaux = s.getAsJsonArray("travaux")?.map { it.asJsonObject }.orEmpty().map { t ->
            require(t.get("couche").asString == "travaux_pt") { "branchement : seul travaux_pt est joué" }
            TravauxSaisi(
                t.get("uuid").asString, t.get("code_travaux").asString, Geometrie.Point(PointLambert93(t.get("x").asDouble, t.get("y").asDouble)),
                t.texte("visite_le")?.let(Instant::parse), t.texte("operateur"),
            )
        }
        return Saisies(operateur, controles = controles, plants = plants, travaux = travaux)
    }
}
