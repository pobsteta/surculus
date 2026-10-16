package io.github.pobsteta.surculus.core.fixtures

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.github.pobsteta.surculus.core.geo.Azimut
import io.github.pobsteta.surculus.core.geo.EmpriseLambert93
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.placette.ControleEcrit
import io.github.pobsteta.surculus.core.placette.EcritureControle
import io.github.pobsteta.surculus.core.placette.EmpriseEtFrance
import io.github.pobsteta.surculus.core.placette.Essences
import io.github.pobsteta.surculus.core.placette.EtatPlant
import io.github.pobsteta.surculus.core.placette.Identite
import io.github.pobsteta.surculus.core.placette.IdentitePlant
import io.github.pobsteta.surculus.core.placette.JourCivil
import io.github.pobsteta.surculus.core.placette.Numerotation
import io.github.pobsteta.surculus.core.placette.NumerosPlacette
import io.github.pobsteta.surculus.core.placette.Observation
import io.github.pobsteta.surculus.core.placette.ObservationDatee
import io.github.pobsteta.surculus.core.placette.Observations
import io.github.pobsteta.surculus.core.placette.Piquet
import io.github.pobsteta.surculus.core.placette.PlantANumeroter
import io.github.pobsteta.surculus.core.placette.Rattachement
import io.github.pobsteta.surculus.core.placette.RegarniPose
import io.github.pobsteta.surculus.core.placette.Regarnis
import io.github.pobsteta.surculus.core.placette.Totaux
import io.github.pobsteta.surculus.core.placette.UnControleParJour
import io.github.pobsteta.surculus.core.placette.UuidControle
import io.github.pobsteta.surculus.core.placette.UuidPlacette
import io.github.pobsteta.surculus.core.placette.UuidPlant
import io.github.pobsteta.surculus.core.placette.Verdict
import io.github.pobsteta.surculus.core.reperes.CodesUniteAzimut
import io.github.pobsteta.surculus.core.reperes.PointGeographique
import java.time.Instant
import java.time.LocalDate
import kotlin.math.hypot

/**
 * Adaptateur de la mission 004 : traduit chaque cas de `fixtures/regles/` vers `core.placette` et rend les
 * clés de `attendu`. Il ne décide rien lui-même : chaque valeur rendue vient d'une fonction du domaine.
 *
 * Contrôles numérotés des fixtures (`pose_au_controle`, `controle_calcule`) : comme il y a un contrôle par
 * placette et par jour, le contrôle n° k est celui du jour [JOUR_0] + k.
 */
object AdaptateurReglesPlacette : ReglesPlacette {
    private val JOUR_0: LocalDate = LocalDate.of(2026, 10, 1)
    private val essences by lazy { Essences(Referentiels.essences) }

    override fun appliquer(famille: String, entree: JsonObject): JsonObject = when (famille) {
        "rattachement-15m" -> rattachement(entree)
        "un-controle-par-jour" -> unControleParJour(entree)
        "emprise-et-france" -> emprise(entree)
        "totaux-controle" -> totaux(entree)
        "regarni" -> regarni(entree)
        "identite" -> identite(entree)
        "etats-causes" -> etatsCauses(entree)
        "numerotation" -> numerotation(entree)
        else -> error("famille inconnue : $famille")
    }

    // --- outils de lecture ---

    private fun JsonObject.texte(cle: String): String? = get(cle)?.takeUnless { it.isJsonNull }?.asString
    private fun JsonObject.entier(cle: String): Int? = get(cle)?.takeUnless { it.isJsonNull }?.asInt
    private fun JsonObject.objets(cle: String): List<JsonObject> = getAsJsonArray(cle)?.map { it.asJsonObject } ?: emptyList()
    private fun jour(k: Int): LocalDate = JOUR_0.plusDays(k.toLong())
    private fun xy(o: JsonObject) = PointLambert93(xMetres = o["x"].asDouble, yMetres = o["y"].asDouble)
    private fun json(p: PointLambert93) = JsonObject().apply { addProperty("x", p.xMetres); addProperty("y", p.yMetres) }

    private fun observation(o: JsonObject): Observation =
        Observations.valider(o.texte("etat")!!, o.texte("cause_groupe"), o.texte("cause_detail"), o.entier("hauteur_cm")).valeurOuEchec()

    private fun identites(plants: List<JsonObject>): List<IdentitePlant> = plants.mapIndexed { k, p ->
        IdentitePlant(
            uuid = UuidPlant(p.texte("uuid")!!),
            placette = UuidPlacette(p.texte("placette") ?: "P-001"),
            numero = p.entier("numero") ?: (k + 1),
            remplace = p.texte("remplace_uuid")?.let { UuidPlant(it) },
        )
    }

    // --- familles ---

    private fun rattachement(e: JsonObject): JsonObject {
        val mesuree = e.getAsJsonObject("position_mesuree")
        check(mesuree.texte("crs") == "EPSG:2154") { "position mesurée attendue en EPSG:2154" }
        val piquets = e.objets("piquets_stockes").map { Piquet(it.texte("code")!!, xy(it)) }
        val codeLu = if (e["identite_confirmee"].asBoolean) e.texte("code_lu") else null
        return JsonObject().apply {
            when (val r = Rattachement.rattacher(piquets, xy(mesuree), codeLu)) {
                is Verdict.Accepte -> {
                    addProperty("issue", "rattache")
                    addProperty("placette", r.valeur.piquet.code)
                    addProperty("distance_m", r.valeur.distanceM)
                    addProperty("rattache_par_identite", r.valeur.parIdentite)
                    add("position_controle", json(r.valeur.positionControle))
                    add("position_mesuree_conservee", json(r.valeur.positionMesuree))
                }
                is Verdict.Refuse -> { addProperty("issue", "refus"); addProperty("motif", r.refus.motif) }
                is Verdict.AConfirmer -> error("pas d'alerte attendue au rattachement")
            }
        }
    }

    private fun unControleParJour(e: JsonObject): JsonObject {
        val saisies: List<Pair<String, Instant>> = if (e.has("controles")) {
            e.objets("controles").map { it.texte("placette")!! to Instant.parse(it.texte("saisi_le_utc")) }
        } else {
            e.getAsJsonArray("controles_saisis_le_utc").map { e.texte("placette")!! to Instant.parse(it.asString) }
        }
        val ecrits = mutableListOf<ControleEcrit>()
        val effets = saisies.mapIndexed { k, (placette, instant) ->
            UnControleParJour.ecrire(ecrits, UuidControle("c$k"), UuidPlacette(placette), instant).also {
                if (it is EcritureControle.Nouveau) ecrits += it.controle
            }
        }
        val jours = saisies.map { JourCivil.de(it.second) }
        return JsonObject().apply {
            add("jours_paris", JsonArray().apply { jours.forEach { add(it.toString()) } })
            addProperty("meme_jour_civil", jours.toSet().size == 1)
            addProperty("controles_ecrits", ecrits.size)
            addProperty("le_second_corrige_le_premier", effets.getOrNull(1) is EcritureControle.Corrige)
        }
    }

    private fun emprise(e: JsonObject): JsonObject = JsonObject().apply {
        when {
            e.has("lat") -> {
                check(e.texte("ordre") == null || e.texte("ordre") == "lat,lon") { "ordre (lat, lon) attendu" }
                val g = PointGeographique.de(latitudeDeg = e["lat"].asDouble, longitudeDeg = e["lon"].asDouble).valeurOuEchec()
                val c = EmpriseEtFrance.controler(g)
                addProperty("en_france", c.enFrance)
                addProperty("inversion_detectee", c.inversionDetectee)
                addProperty("refuse", c.refuse)
                if (c.enFrance) {
                    val p = EmpriseEtFrance.versLambert93(g)
                    addProperty("x_l93", p.xMetres); addProperty("y_l93", p.yMetres)
                    addProperty("x", p.xMetres); addProperty("y", p.yMetres)
                    addProperty("crs", "EPSG:2154")
                }
            }
            e.has("point_l93") -> {
                val b = e.getAsJsonObject("foret_emprise_l93")
                val foret = EmpriseLambert93(b["xmin"].asDouble, b["ymin"].asDouble, b["xmax"].asDouble, b["ymax"].asDouble)
                val c = EmpriseEtFrance.controler(xy(e.getAsJsonObject("point_l93")), foret)
                addProperty("en_france", c.enFrance)
                addProperty("dans_emprise", c.dansEmprise)
                addProperty("refuse", c.refuse)
            }
            else -> {
                check(e.texte("crs") == "EPSG:2154" && e.texte("ordre") == "x_est,y_nord") { "EPSG:2154 (x = E, y = N) attendu" }
                val c = EmpriseEtFrance.controler(xy(e))
                addProperty("en_france", c.enFrance)
                addProperty("inversion_detectee", c.inversionDetectee)
                addProperty("refuse", c.refuse)
            }
        }
    }

    private fun totaux(e: JsonObject): JsonObject {
        // Un « non observé » est une absence de mesure : la fixture T9b lui donne une hauteur, que le domaine
        // refuserait à la saisie (Observations.valider) ; ici seule la déduction des totaux est testée.
        val etats = e.objets("plants").map { p ->
            val obs = if (p.texte("etat") == Observations.CODE_NON_OBSERVE) Observation.NonObserve else observation(p)
            EtatPlant(UuidPlant(p.texte("uuid")!!), obs)
        }
        val regarnis = e.objets("regarnis").map {
            RegarniPose(UuidPlant(it.texte("uuid")!!), UuidPlant(it.texte("remplace_uuid")!!), jour(it.entier("pose_au_controle")!!))
        }
        val t = Totaux.deduire(etats, Regarnis.remplacesAvant(regarnis, jour(e.entier("controle_calcule")!!)))
        return JsonObject().apply {
            addProperty("nb_total", t.nbTotal)
            addProperty("nb_vivants", t.nbVivants)
            addProperty("nb_abroutis", t.nbAbroutis)
            addProperty("h_moy_cm", t.hMoyCm)
            addProperty("etat_accepte", e.objets("plants").all { Observations.valider(it.texte("etat")!!).estAccepte })
        }
    }

    private fun regarni(e: JsonObject): JsonObject {
        val plantsJson = e.objets("plants")
        val plants = identites(plantsJson)
        val etats = plantsJson.associate { UuidPlant(it.texte("uuid")!!) to observation(it) }
        return JsonObject().apply {
            e.getAsJsonObject("regarni")?.let { r ->
                val jourPose = jour(1)
                val nouveau = IdentitePlant(
                    UuidPlant(r.texte("uuid")!!), UuidPlacette(r.texte("placette")!!), plants.size + 1,
                    r.texte("remplace_uuid")?.let { UuidPlant(it) },
                )
                val dernierEtat = { u: UuidPlant ->
                    Regarnis.dernierEtatObserve(listOfNotNull(etats[u]?.let { ObservationDatee(jourPose, it) }), jourPose)
                }
                addProperty("accepte", Regarnis.verifier(plants, dernierEtat, nouveau).estAccepte)
            }
            if (e.has("regarnis")) {
                val poses = e.objets("regarnis").map {
                    RegarniPose(UuidPlant(it.texte("uuid")!!), UuidPlant(it.texte("remplace_uuid")!!), jour(it.entier("pose_au_controle")!!))
                }
                for (pose in poses) for (k in 1..2) {
                    addProperty("${pose.remplace}_compte_au_controle_$k", pose.remplace !in Regarnis.remplacesAvant(poses, jour(k)))
                }
            }
        }
    }

    private fun identite(e: JsonObject): JsonObject = JsonObject().apply {
        if (e.has("passage_1")) {
            val avant = identites(e.objets("passage_1"))
            val apres = identites(e.objets("passage_2"))
            val v = Identite.verifier(avant).estAccepte && Identite.verifier(apres).estAccepte &&
                Identite.verifierRelecture(avant, apres).estAccepte
            addProperty("accepte", v)
            addProperty("uuid_conserve", apres.map { it.uuid }.containsAll(avant.map { it.uuid }))
            addProperty("numero_conserve", Identite.verifierRelecture(avant, apres).estAccepte)
        } else {
            val plants = identites(e.objets("plants"))
            addProperty("accepte", Identite.verifier(plants).estAccepte)
            e.getAsJsonObject("ajout")?.let { a ->
                val figes = NumerosPlacette.Figes(plants.associate { it.uuid to it.numero })
                val uuid = UuidPlant(a.texte("uuid")!!)
                val apres = Numerotation.ajouter(figes, uuid).valeurOuEchec()
                addProperty("numero_ajoute", apres.numeros.getValue(uuid))
                add("numeros_existants_inchanges", JsonObject().apply { plants.forEach { addProperty(it.uuid.valeur, apres.numeros.getValue(it.uuid)) } })
            }
        }
    }

    private fun etatsCauses(e: JsonObject): JsonObject {
        val obs = Observations.valider(e.texte("etat")!!, e.texte("cause_groupe"), e.texte("cause_detail"), e.entier("hauteur_cm"))
        val essence: Verdict<*> = e.texte("essence")?.let { essences.valider(it) } ?: Verdict.Accepte(Unit)
        return JsonObject().apply {
            addProperty("accepte", obs.estAccepte && essence.estAccepte)
            (obs as? Verdict.Refuse)?.let { addProperty("motif", it.refus.motif) }
            (essence as? Verdict.Refuse)?.let { addProperty("motif_essence", it.refus.motif) }
        }
    }

    private fun numerotation(e: JsonObject): JsonObject {
        val p = e.getAsJsonObject("parametres")
        val unite = CodesUniteAzimut.lire(p.texte("unite_azimut")).valeurOuEchec()
        val tolerance = p.get("tolerance_deg")?.asDouble ?: Numerotation.TOLERANCE_DEG
        // Convention des fixtures : x vers l'est, y vers le nord géographique, mètres depuis le piquet.
        val plants = e.objets("plants").map {
            val x = it["x"].asDouble
            val y = it["y"].asDouble
            PlantANumeroter(it.texte("id")!!, Azimut.azimut(x, y), hypot(x, y))
        }
        val numeros = Numerotation.provisoires(plants, tolerance).numeros
        return JsonObject().apply {
            add("numeros", JsonObject().apply { numeros.forEach { (u, n) -> addProperty(u.valeur, n.toString()) } })
            add("exclus", JsonObject())
            add("azimuts", JsonObject().apply { plants.forEach { addProperty(it.id, Azimut.depuisDegres(it.azimutGeoDeg, unite)) } })
            add("visibles", JsonArray().apply { plants.forEach { add(it.id) } })
        }
    }
}
