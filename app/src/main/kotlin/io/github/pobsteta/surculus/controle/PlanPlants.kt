/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.controle

import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.placette.EtatDsf
import io.github.pobsteta.surculus.core.placette.Observation
import io.github.pobsteta.surculus.core.placette.UuidPlant
import io.github.pobsteta.surculus.core.session.EtatSession
import kotlin.math.hypot
import kotlin.math.max

/** Un plant sur le plan : sa place par rapport au piquet, en mètres (x = Est, y = Nord), et son état du jour. */
data class PlantSurPlan(
    val uuid: UuidPlant,
    val numero: Int,
    /** Mètres à l'Est du piquet. */
    val estM: Double,
    /** Mètres au Nord du piquet. */
    val nordM: Double,
    /** État dit aujourd'hui ; `null` : pas encore dit (ou « non observé »). */
    val etat: EtatDsf?,
    val courant: Boolean,
)

/** Un point à dessiner, en pixels (origine en haut à gauche, y vers le bas). */
data class PointPixel(val x: Float, val y: Float)

/**
 * Le plan des plants autour du piquet, **à l'échelle**, nord en haut (spec § 4.3). Fonction pure de l'état de la session :
 * le dessin ne fait que tracer ce qu'il reçoit. Les plants d'un passage précédent n'ont pas de position (la session ne la
 * porte pas) : ils ne sont **pas dessinés**, seulement comptés pour la mention en petit (décision de Pascal du 2026-10-27).
 */
data class PlanPlants(
    val plants: List<PlantSurPlan>,
    /** Rayon de la placette, en mètres : le cercle tracé. */
    val rayonM: Double,
    /** Plants de la liste sans position, donc absents du plan. */
    val nonDessines: Int,
) {
    /** Demi-largeur du plan en mètres : le rayon, ou le plant le plus loin si un pointage hors rayon a été confirmé. */
    val etendueM: Double
        get() = max(rayonM, plants.maxOfOrNull { hypot(it.estM, it.nordM) } ?: 0.0) * MARGE

    /** Mètres vers pixels dans un carré de [cote] pixels : le piquet au centre, le nord en haut, la même échelle sur les deux axes. */
    fun versPixels(estM: Double, nordM: Double, cote: Float): PointPixel {
        val echelle = cote / 2f / etendueM.toFloat()
        return PointPixel(cote / 2f + estM.toFloat() * echelle, cote / 2f - nordM.toFloat() * echelle)
    }

    fun versPixels(plant: PlantSurPlan, cote: Float) = versPixels(plant.estM, plant.nordM, cote)

    /** Pixels par mètre dans un carré de [cote] pixels. */
    fun echelle(cote: Float): Float = cote / 2f / etendueM.toFloat()

    /** Le plant touché en ([x], [y]) : le plus proche, à [tolerancePx] au plus ; `null` si le doigt est dans le vide. */
    fun plantTouche(x: Float, y: Float, cote: Float, tolerancePx: Float): PlantSurPlan? =
        plants.map { it to versPixels(it, cote) }
            .map { (p, px) -> p to hypot(px.x - x, px.y - y) }
            .filter { it.second <= tolerancePx }
            .minByOrNull { it.second }?.first

    companion object {
        /** Un peu d'air autour du cercle de la placette. */
        const val MARGE = 1.15

        fun depuis(etat: EtatSession, piquet: PointLambert93, rayonM: Double): PlanPlants {
            val dessines = etat.plants.filter { it.position != null && !it.remplaceAvant }.map { p ->
                PlantSurPlan(
                    uuid = p.uuid,
                    numero = p.numero,
                    estM = p.position!!.xMetres - piquet.xMetres,
                    nordM = p.position!!.yMetres - piquet.yMetres,
                    etat = (p.duJour as? Observation.Observe)?.etat,
                    courant = p.uuid == etat.courant,
                )
            }
            return PlanPlants(dessines, rayonM, nonDessines = etat.plants.count { it.position == null && !it.remplaceAvant })
        }
    }
}

/** Côté maximal du plan, en dp : au-delà, le plan ne grandit pas (téléphone couché, tablette). */
const val COTE_PLAN_MAX_DP = 320f

/**
 * Côté du carré du plan, en dp, pour une largeur disponible donnée (mission 067) : toute la largeur, sans dépasser [COTE_PLAN_MAX_DP].
 * Le plan est un carré posé dans le flux, sous les boutons : il ne les recouvre jamais, debout ou couché.
 */
fun cotePlanDp(largeurDisponibleDp: Float): Float = largeurDisponibleDp.coerceIn(0f, COTE_PLAN_MAX_DP)
