/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.core.placette

import io.github.pobsteta.surculus.core.reperes.PlantDerive
import java.time.Instant
import java.time.LocalDate

/** Placette telle que le contrôle la connaît : son identité et son rayon (`placettes.rayon_m`, mètres). */
data class Placette(val uuid: UuidPlacette, val rayonM: Double) {
    init { require(rayonM.isFinite() && rayonM > 0.0) { "Rayon de placette invalide : $rayonM m" } }
}

/**
 * Plant pointé depuis le piquet (couche `plants`, spec § 5). Son numéro n'est pas ici : il est provisoire
 * pendant le premier contrôle et figé à sa clôture ([NumerosPlacette]).
 *
 * [derive] porte l'observation brute (distance horizontale, azimut magnétique saisi et son unité) et les
 * termes dérivés (déclinaison, convergence, azimut géographique, position en Lambert-93, x = E, y = N),
 * calculés par la chaîne des repères (`core.reperes`, mission 005).
 */
data class Plant(
    val uuid: UuidPlant,
    val placette: UuidPlacette,
    val essence: CodeEssence?,
    /** Plant mort ou absent que ce regarni remplace ; `null` si ce n'est pas un regarni. */
    val remplace: UuidPlant?,
    /** Instant du pointage (UTC). */
    val pointeLe: Instant,
    val derive: PlantDerive,
) {
    val regarni: Boolean get() = remplace != null
}

/** Ce que les règles d'identité et de regarni lisent d'un plant numéroté (une ligne de `plants`). */
data class IdentitePlant(
    val uuid: UuidPlant,
    val placette: UuidPlacette,
    val numero: Int,
    val remplace: UuidPlant? = null,
) {
    init { require(numero >= 1) { "Numéro de plant $numero : 1 au moins" } }
}

/** Un plant appartient à une seule placette et garde son UUID (spec § 6). */
object Identite {
    /**
     * Refuse un UUID en double (dans la même placette, arbitrage Q14) ou présent dans deux placettes, et un
     * numéro déjà pris dans la placette (sommieR `sommier_pointer_plant`, brief `travaux-2-plants`).
     */
    fun verifier(plants: List<IdentitePlant>): Verdict<Unit> {
        val vus = HashMap<UuidPlant, IdentitePlant>()
        for (p in plants) {
            val deja = vus[p.uuid]
            if (deja != null) {
                return Verdict.Refuse(
                    if (deja.placette != p.placette) RefusPlacette.PlantDansDeuxPlacettes(p.uuid) else RefusPlacette.UuidEnDouble(p.uuid),
                )
            }
            vus[p.uuid] = p
        }
        plants.groupBy { it.placette to it.numero }.entries.firstOrNull { it.value.size > 1 }?.let { (cle, _) ->
            return Verdict.Refuse(RefusPlacette.NumeroEnDouble(cle.first, cle.second))
        }
        return Verdict.Accepte(Unit)
    }

    /** D'un passage à l'autre, un plant relu garde sa placette et son numéro (spec § 6, tranche 5). */
    fun verifierRelecture(avant: List<IdentitePlant>, apres: List<IdentitePlant>): Verdict<Unit> {
        val parUuid = avant.associateBy { it.uuid }
        for (p in apres) {
            val a = parUuid[p.uuid] ?: continue
            if (a.placette != p.placette) return Verdict.Refuse(RefusPlacette.PlacetteChangee(p.uuid))
            if (a.numero != p.numero) return Verdict.Refuse(RefusPlacette.NumeroChange(p.uuid, a.numero, p.numero))
        }
        return Verdict.Accepte(Unit)
    }
}

/** Observation d'un plant à un contrôle daté (jour civil Europe/Paris) : historique pour les regarnis. */
data class ObservationDatee(val jour: LocalDate, val observation: Observation)

/** Regarni posé lors du contrôle du jour [poseLe], à la place du plant [remplace]. */
data class RegarniPose(val regarni: UuidPlant, val remplace: UuidPlant, val poseLe: LocalDate)

/**
 * Un regarni remplace exactement un plant mort ou absent de la même placette (`remplace_uuid` obligatoire) ;
 * le plant remplacé compte dans le contrôle où il est vu, puis sort des contrôles suivants (spec § 6, Q13).
 * Comme il y a un contrôle par placette et par jour, « contrôle suivant » = contrôle d'un jour postérieur.
 */
object Regarnis {
    /**
     * Dernier état observé d'un plant au plus tard le jour [jusquAu] inclus, les « non observé » ignorés
     * (arbitrage de Pascal 2026-10-16, Q9). `null` si le plant n'a jamais été vu.
     */
    fun dernierEtatObserve(historique: List<ObservationDatee>, jusquAu: LocalDate): EtatDsf? =
        historique.filter { !it.jour.isAfter(jusquAu) && it.observation is Observation.Observe }
            .maxByOrNull { it.jour }
            ?.let { (it.observation as Observation.Observe).etat }

    /**
     * Vérifie un regarni [nouveau] au regard des plants de la tournée [plants] et du dernier état observé du
     * plant remplacé, [dernierEtat] (voir [dernierEtatObserve]).
     */
    fun verifier(plants: List<IdentitePlant>, dernierEtat: (UuidPlant) -> EtatDsf?, nouveau: IdentitePlant): Verdict<Unit> {
        val cible = nouveau.remplace ?: return Verdict.Refuse(RefusPlacette.RemplaceObligatoire)
        val remplace = plants.firstOrNull { it.uuid == cible } ?: return Verdict.Refuse(RefusPlacette.PlantRemplaceInconnu(cible))
        if (remplace.placette != nouveau.placette) return Verdict.Refuse(RefusPlacette.PlantRemplaceAutrePlacette(cible))
        val etat = dernierEtat(cible)
        if (etat != EtatDsf.MORT && etat != EtatDsf.ABSENT) return Verdict.Refuse(RefusPlacette.PlantRemplaceNiMortNiAbsent(cible, etat))
        plants.firstOrNull { it.remplace == cible && it.uuid != nouveau.uuid }?.let {
            return Verdict.Refuse(RefusPlacette.PlantDejaRemplace(cible, it.uuid))
        }
        return Verdict.Accepte(Unit)
    }

    /** Plants remplacés par un regarni posé **avant** le jour du contrôle [jourControle] : ils sortent de ses totaux. */
    fun remplacesAvant(regarnis: List<RegarniPose>, jourControle: LocalDate): Set<UuidPlant> =
        regarnis.filter { it.poseLe.isBefore(jourControle) }.map { it.remplace }.toSet()
}

/**
 * Plant pointé au-delà du rayon de sa placette plus [TOLERANCE_M] : alerte qui exige une confirmation
 * explicite de l'agent, jamais un refus (spec § 6, arbitrage de Pascal 2026-10-16, Q3). La borne est incluse.
 */
object ControleRayon {
    const val TOLERANCE_M = 0.10

    /** Marge de calcul flottant (1 nm) pour que la borne exacte, 3,99 + 0,10, reste incluse. */
    private const val EPS_M = 1e-9

    fun verifier(distanceHorizontaleM: Double, rayonM: Double): AlertePlacette.HorsRayon? =
        if (distanceHorizontaleM > rayonM + TOLERANCE_M + EPS_M) AlertePlacette.HorsRayon(distanceHorizontaleM, rayonM, TOLERANCE_M) else null
}

/** Pointage d'un plant dans sa placette. */
object Pointage {
    /**
     * Crée le plant pointé. Hors du rayon (+ 0,10 m), le plant n'est enregistré que si l'agent l'a confirmé
     * ([confirmeHorsRayon]) ; sinon le verdict est [Verdict.AConfirmer].
     */
    fun pointer(
        uuid: UuidPlant,
        placette: Placette,
        derive: PlantDerive,
        pointeLe: Instant,
        essence: CodeEssence? = null,
        remplace: UuidPlant? = null,
        confirmeHorsRayon: Boolean = false,
    ): Verdict<Plant> {
        val alerte = ControleRayon.verifier(derive.distanceHorizontaleM, placette.rayonM)
        if (alerte != null && !confirmeHorsRayon) return Verdict.AConfirmer(alerte)
        return Verdict.Accepte(Plant(uuid, placette.uuid, essence, remplace, pointeLe, derive))
    }
}
