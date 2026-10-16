package io.github.pobsteta.surculus.core.fixtures

import com.google.gson.JsonObject
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.reperes.AzimutSaisi
import io.github.pobsteta.surculus.core.reperes.ChaineAzimut
import io.github.pobsteta.surculus.core.reperes.CodeEpsg
import io.github.pobsteta.surculus.core.reperes.CodesUniteAzimut
import io.github.pobsteta.surculus.core.reperes.Convergence
import io.github.pobsteta.surculus.core.reperes.DeclinaisonWmm2025
import io.github.pobsteta.surculus.core.reperes.DistanceHorizontale
import io.github.pobsteta.surculus.core.reperes.PointDerive
import io.github.pobsteta.surculus.core.reperes.PointGeographique
import io.github.pobsteta.surculus.core.reperes.Resultat
import io.github.pobsteta.surculus.core.reperes.UnitePente
import io.github.pobsteta.surculus.core.reperes.UnitesTournee
import java.time.LocalDate

/**
 * Adaptateur de la mission 005 : traduit chaque cas de `fixtures/reperes/` vers `core.reperes` et rend
 * les clés de `attendu`. Il ne calcule rien lui-même.
 *
 * `normalise_en_silence`, `extrapolation_silencieuse` et `positive_vers_l_est` sont des propriétés de
 * conception (refus sans normalisation, refus sans extrapolation, convention de signe du WMM) : rendues
 * comme constantes ici, elles sont démontrées par les tests unitaires de `core.reperes`.
 */
object AdaptateurReperes : Reperes {
    override fun appliquer(famille: String, entree: JsonObject): JsonObject = when (famille) {
        "convergence" -> convergence(entree)
        "declinaison-wmm2025" -> declinaison(entree)
        "chaine-azimut" -> if (entree.has("piquet_l93")) plantDerive(entree) else chaine(entree)
        "unites-azimut" -> if (entree.has("tournee")) ouverture(entree) else unite(entree)
        "distance-horizontale" -> if (entree.has("distance_mesuree_m")) distance(entree) else ouverture(entree)
        else -> error("famille inconnue : $famille")
    }

    private fun JsonObject.texte(cle: String): String? = get(cle)?.takeUnless { it.isJsonNull }?.asString
    private fun point(e: JsonObject) = PointGeographique.de(e["lat"].asDouble, e["lon"].asDouble).valeurOuEchec()
    private fun date(e: JsonObject) = LocalDate.parse(e["date"].asString)
    private fun azimut(e: JsonObject) =
        AzimutSaisi.valider(e["azimut_mag"].asDouble, CodesUniteAzimut.lire(e.texte("unite_azimut")).valeurOuEchec()).valeurOuEchec()

    private fun convergence(e: JsonObject) = JsonObject().apply { addProperty("convergence_deg", Convergence.lambert93(point(e))) }

    private fun declinaison(e: JsonObject): JsonObject {
        val altitudeM = (e.get("altitude_km")?.asDouble ?: 0.0) * 1000.0
        val r = DeclinaisonWmm2025.calculer(point(e), date(e), altitudeM)
        return JsonObject().apply {
            addProperty("hors_validite", r is Resultat.Refuse)
            addProperty("refuse", r is Resultat.Refuse)
            addProperty("extrapolation_silencieuse", false)
            if (r is Resultat.Accepte) {
                addProperty("declinaison_deg", r.valeur)
                addProperty("positive_vers_l_est", true)
            }
        }
    }

    private fun chaine(e: JsonObject): JsonObject {
        val o = ChaineAzimut.orienter(azimut(e), point(e), date(e)).valeurOuEchec()
        return JsonObject().apply {
            addProperty("declinaison_deg", o.declinaisonDeg)
            addProperty("convergence_deg", o.convergenceDeg)
            addProperty("azimut_geo_deg", o.azimutGeographiqueDeg)
            addProperty("azimut_grille_deg", o.azimutGrilleDeg)
        }
    }

    /** C14 : le piquet est donné en Lambert-93 sans code ; contrat sommieR 0.31.1, donc EPSG:2154. */
    private fun plantDerive(e: JsonObject): JsonObject {
        val p = e.getAsJsonObject("piquet_l93")
        val plant = PointDerive.deriver(
            PointLambert93(p["x"].asDouble, p["y"].asDouble), CodeEpsg.RGF93_V1_LAMBERT93,
            e["distance_m"].asDouble, azimut(e), date(e),
        ).valeurOuEchec()
        return JsonObject().apply {
            addProperty("azimut_grille_deg", plant.orientation.azimutGrilleDeg)
            addProperty("dx_m", plant.dxM)
            addProperty("dy_m", plant.dyM)
            addProperty("x", plant.position.xMetres)
            addProperty("y", plant.position.yMetres)
        }
    }

    private fun unite(e: JsonObject): JsonObject {
        val u = CodesUniteAzimut.lire(e.texte("unite_azimut")).valeurOuEchec()
        val r = AzimutSaisi.valider(e["azimut_mag"].asDouble, u)
        return JsonObject().apply {
            addProperty("accepte", r is Resultat.Accepte)
            addProperty("normalise_en_silence", false)
            if (r is Resultat.Accepte) addProperty("azimut_deg", r.valeur.degres)
        }
    }

    private fun ouverture(e: JsonObject): JsonObject {
        val t = e.getAsJsonObject("tournee")
        val r = UnitesTournee.declarer(t.texte("unite_azimut"), t.texte("unite_pente"))
        return JsonObject().apply {
            addProperty("ouverture_refusee", r is Resultat.Refuse)
            if (r is Resultat.Refuse) addProperty("motif", r.refus.motif)
        }
    }

    /** Cas de distance : seule l'unité de pente de la tournée sert (D7 et D9 ne déclarent pas la boussole). */
    private fun distance(e: JsonObject): JsonObject {
        val unite = UnitePente.lire(e.getAsJsonObject("tournee").texte("unite_pente")).valeurOuEchec()
        val pente = e.get("pente")?.takeUnless { it.isJsonNull }?.asDouble
        val r = DistanceHorizontale.calculer(e["distance_mesuree_m"].asDouble, pente, unite)
        return JsonObject().apply {
            addProperty("refuse", r is Resultat.Refuse)
            if (r is Resultat.Accepte) addProperty("distance_horizontale_m", r.valeur)
        }
    }
}
