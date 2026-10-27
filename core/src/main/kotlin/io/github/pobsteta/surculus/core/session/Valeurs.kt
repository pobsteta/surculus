/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.core.session

/**
 * Format de la `valeur` des lignes du journal écrites par la session de contrôle : **unique endroit du code** qui
 * connaît les clés. Le même format est décrit dans `docs/formats/journal-valeurs-v1.md`, que la mission d'export
 * relira. Les clés suivent les noms de colonnes de la spec § 5 quand ils existent (`placette_uuid`, `jour_civil`,
 * `etat`, `cause_groupe`, `cause_detail`, `hauteur_cm`, `azimut_mag`, `unite_azimut`, `remplace_uuid`...).
 *
 * Unités : distances en mètres (distance **horizontale**), azimuts en degrés [0, 360[ sauf `azimut_mag` qui est
 * dans `unite_azimut` (`deg` ou `gr`), positions en Lambert-93 (`x` = Est, `y` = Nord, mètres), instants en
 * millisecondes Unix UTC.
 */
object FormatValeurs {
    /** Version du format de `valeur`, écrite dans chaque ligne sous la clé `v`. */
    const val VERSION = 1L

    const val TYPE_CONTROLE = "controle"
    const val TYPE_POINTAGE = "pointage"
    const val TYPE_ETAT = "etat"
    const val TYPE_ANNULATION = "annulation"

    internal fun Map<String, Any?>.texte(cle: String): String? = this[cle] as? String
    internal fun Map<String, Any?>.reel(cle: String): Double? = (this[cle] as? Number)?.toDouble()
    internal fun Map<String, Any?>.entier(cle: String): Long? = (this[cle] as? Number)?.takeIf { it.toDouble() == it.toLong().toDouble() }?.toLong()
    internal fun Map<String, Any?>.booleen(cle: String): Boolean? = this[cle] as? Boolean
}

/** Valeur d'une ligne `controle` (objet : l'UUID du contrôle). « Terminer » la placette réécrit la ligne avec `termine = true`. */
data class ValeurControle(
    val placetteUuid: String,
    val codePlacette: String,
    /** Jour civil Europe/Paris, `AAAA-MM-JJ`, calculé sur l'heure GNSS si le fix a au plus 5 s, sinon sur l'heure du téléphone. */
    val jourCivil: String,
    val operateur: String,
    val termine: Boolean,
    /** L'instant qui fonde [jourCivil] (heure GNSS ou du téléphone) : il ne peut pas contredire le jour. */
    val visiteLeUtcMs: Long,
    val positionX: Double,
    val positionY: Double,
    val positionMesureeX: Double,
    val positionMesureeY: Double,
    val rattacheParIdentite: Boolean,
    val repereEpsg: Int,
    /** Numéros figés des plants de la placette (UUID de plant vers numéro), seulement quand [termine]. */
    val numeros: Map<String, Int>? = null,
) {
    fun versJson(): String = Json.ecrire(
        linkedMapOf<String, Any?>(
            "v" to FormatValeurs.VERSION, "placette_uuid" to placetteUuid, "code_placette" to codePlacette, "jour_civil" to jourCivil,
            "operateur" to operateur, "termine" to termine, "visite_le_utc_ms" to visiteLeUtcMs,
            "position_x" to positionX, "position_y" to positionY,
            "position_mesuree_x" to positionMesureeX, "position_mesuree_y" to positionMesureeY,
            "rattache_par_identite" to rattacheParIdentite, "repere_epsg" to repereEpsg,
        ).also { m -> numeros?.let { m["numeros"] = LinkedHashMap<String, Any?>(it) } },
    )

    companion object {
        fun lire(texte: String?): ValeurControle? = with(FormatValeurs) {
            val m = Json.lire(texte) ?: return null
            @Suppress("UNCHECKED_CAST")
            val numeros = (m["numeros"] as? Map<String, Any?>)?.mapValues { (it.value as? Number)?.toInt() ?: return null }
            ValeurControle(
                m.texte("placette_uuid") ?: return null, m.texte("code_placette") ?: return null, m.texte("jour_civil") ?: return null,
                m.texte("operateur") ?: return null, m.booleen("termine") ?: return null, m.entier("visite_le_utc_ms") ?: return null,
                m.reel("position_x") ?: return null, m.reel("position_y") ?: return null,
                m.reel("position_mesuree_x") ?: return null, m.reel("position_mesuree_y") ?: return null,
                m.booleen("rattache_par_identite") ?: return null, m.entier("repere_epsg")?.toInt() ?: return null, numeros,
            )
        }
    }
}

/**
 * Valeur d'une ligne `pointage` (objet : l'UUID du plant) : l'observation brute (distance horizontale, azimut
 * magnétique lu et son unité) et les termes dérivés (déclinaison, convergence, azimuts, position, EPSG).
 */
data class ValeurPointage(
    val controleUuid: String,
    val placetteUuid: String,
    val distanceHorizontaleM: Double,
    /** Azimut magnétique tel que lu, dans [uniteAzimut]. */
    val azimutMag: Double,
    /** `deg` ou `gr` (couche `plants.unite_azimut`). */
    val uniteAzimut: String,
    /** D'où vient l'azimut : [SourceAzimut.code]. */
    val sourceAzimut: String,
    val declinaisonDeg: Double,
    val convergenceDeg: Double,
    val azimutGeoDeg: Double,
    val azimutGrilleDeg: Double,
    val jourDeclinaison: String,
    val x: Double,
    val y: Double,
    val repereEpsg: Int,
    val essence: String?,
    /** `remplace_uuid` de la couche `plants` : le plant mort ou absent que ce regarni remplace. */
    val remplaceUuid: String?,
    /** Vrai si le plant est au-delà du rayon + 0,10 m et que l'agent l'a confirmé. */
    val horsRayonConfirme: Boolean,
    val pointeLeUtcMs: Long,
) {
    fun versJson(): String = Json.ecrire(
        linkedMapOf<String, Any?>(
            "v" to FormatValeurs.VERSION, "controle_uuid" to controleUuid, "placette_uuid" to placetteUuid,
            "distance_horizontale_m" to distanceHorizontaleM, "azimut_mag" to azimutMag, "unite_azimut" to uniteAzimut,
            "source_azimut" to sourceAzimut, "declinaison_deg" to declinaisonDeg, "convergence_deg" to convergenceDeg,
            "azimut_geo_deg" to azimutGeoDeg, "azimut_grille_deg" to azimutGrilleDeg, "jour_declinaison" to jourDeclinaison,
            "x" to x, "y" to y, "repere_epsg" to repereEpsg, "essence" to essence, "remplace_uuid" to remplaceUuid,
            "hors_rayon_confirme" to horsRayonConfirme, "pointe_le_utc_ms" to pointeLeUtcMs,
        ),
    )

    companion object {
        fun lire(texte: String?): ValeurPointage? = with(FormatValeurs) {
            val m = Json.lire(texte) ?: return null
            ValeurPointage(
                m.texte("controle_uuid") ?: return null, m.texte("placette_uuid") ?: return null,
                m.reel("distance_horizontale_m") ?: return null, m.reel("azimut_mag") ?: return null,
                m.texte("unite_azimut") ?: return null, m.texte("source_azimut") ?: return null,
                m.reel("declinaison_deg") ?: return null, m.reel("convergence_deg") ?: return null,
                m.reel("azimut_geo_deg") ?: return null, m.reel("azimut_grille_deg") ?: return null,
                m.texte("jour_declinaison") ?: return null, m.reel("x") ?: return null, m.reel("y") ?: return null,
                m.entier("repere_epsg")?.toInt() ?: return null, m.texte("essence"), m.texte("remplace_uuid"),
                m.booleen("hors_rayon_confirme") ?: return null, m.entier("pointe_le_utc_ms") ?: return null,
            )
        }
    }
}

/** Valeur d'une ligne `etat` (objet : l'UUID de la ligne d'`etats_plants`). `etat` vaut un code du DSF ou `non_observe`. */
data class ValeurEtat(
    val controleUuid: String,
    val plantUuid: String,
    val etat: String,
    val causeGroupe: String?,
    val causeDetail: String?,
    val hauteurCm: Int?,
) {
    fun versJson(): String = Json.ecrire(
        linkedMapOf<String, Any?>(
            "v" to FormatValeurs.VERSION, "controle_uuid" to controleUuid, "plant_uuid" to plantUuid, "etat" to etat,
            "cause_groupe" to causeGroupe, "cause_detail" to causeDetail, "hauteur_cm" to hauteurCm,
        ),
    )

    companion object {
        fun lire(texte: String?): ValeurEtat? = with(FormatValeurs) {
            val m = Json.lire(texte) ?: return null
            ValeurEtat(
                m.texte("controle_uuid") ?: return null, m.texte("plant_uuid") ?: return null, m.texte("etat") ?: return null,
                m.texte("cause_groupe"), m.texte("cause_detail"), m.entier("hauteur_cm")?.toInt(),
            )
        }
    }
}
