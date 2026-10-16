/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.core.reperes

import io.github.pobsteta.surculus.core.geo.Azimut
import io.github.pobsteta.surculus.core.geo.UniteAzimut
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos

/**
 * Codes d'unité de boussole tels qu'écrits dans la couche `plants` (`unite_azimut`, spec § 5) et dans
 * les fixtures : `deg` (degrés) et `gr` (grades). L'unité elle-même est [UniteAzimut] de `core.geo`.
 */
object CodesUniteAzimut {
    private val parCode = mapOf("deg" to UniteAzimut.DEGRES, "gr" to UniteAzimut.GRADES)

    /** Lecture stricte : `null` ou vide → non déclarée ; tout code hors {deg, gr} → inconnue. */
    fun lire(code: String?): Resultat<UniteAzimut> = when {
        code.isNullOrBlank() -> Resultat.Refuse(RefusRepere.UniteAzimutNonDeclaree)
        else -> parCode[code]?.let { Resultat.Accepte(it) } ?: Resultat.Refuse(RefusRepere.UniteAzimutInconnue(code))
    }

    fun code(unite: UniteAzimut): String = parCode.entries.first { it.value == unite }.key
}

/**
 * Azimut magnétique **saisi** (lu sur la boussole), gardé tel quel avec son unité : c'est l'observation
 * primaire enregistrée (ADR 0003). On ne l'obtient que par [valider], qui refuse une valeur hors de
 * [0, 360[ en degrés ou de [0, 400[ en grades **avant toute normalisation** (règle de `core.geo.Azimut.verifier`).
 *
 * API stable, réutilisée par la mission 004.
 */
class AzimutSaisi private constructor(
    /** Valeur lue, dans l'unité de la boussole, jamais modifiée. */
    val valeur: Double,
    val unite: UniteAzimut,
) {
    /** Azimut magnétique en degrés décimaux, dans [0, 360[ (1 gr = 0,9°), sans normalisation. */
    val degres: Double get() = Azimut.versDegres(valeur, unite)

    override fun equals(other: Any?) = other is AzimutSaisi && other.valeur == valeur && other.unite == unite
    override fun hashCode() = 31 * valeur.hashCode() + unite.hashCode()
    override fun toString() = "AzimutSaisi($valeur ${CodesUniteAzimut.code(unite)})"

    companion object {
        fun valider(valeur: Double, unite: UniteAzimut): Resultat<AzimutSaisi> = try {
            Azimut.verifier(valeur, unite)
            Resultat.Accepte(AzimutSaisi(valeur, unite))
        } catch (e: IllegalArgumentException) {
            Resultat.Refuse(RefusRepere.AzimutHorsPlage(valeur, unite))
        }
    }
}

/** Unité de la pente, déclarée une fois par tournée (arbitrage de Pascal 2026-10-16, Q9). */
enum class UnitePente(
    /** Code écrit dans les données et les fixtures. */
    val code: String,
) {
    POURCENT("pct"),
    DEGRES("deg"),
    ;

    companion object {
        fun lire(code: String?): Resultat<UnitePente> = when {
            code.isNullOrBlank() -> Resultat.Refuse(RefusRepere.UnitePenteNonDeclaree)
            else -> entries.firstOrNull { it.code == code }?.let { Resultat.Accepte(it) }
                ?: Resultat.Refuse(RefusRepere.UnitePenteInconnue(code))
        }
    }
}

/**
 * Unités de la boussole et de la pente, déclarées à l'ouverture de la tournée (spec § 6, ADR 0003).
 * Une tournée ne s'ouvre pas sans les deux. API stable, réutilisée par la mission 004.
 */
data class UnitesTournee private constructor(val uniteAzimut: UniteAzimut, val unitePente: UnitePente) {
    companion object {
        /** Contrôle l'unité de boussole d'abord, puis celle de la pente ; codes `deg`/`gr` et `pct`/`deg`. */
        fun declarer(codeAzimut: String?, codePente: String?): Resultat<UnitesTournee> =
            CodesUniteAzimut.lire(codeAzimut).puis { az ->
                UnitePente.lire(codePente).puis { pente -> Resultat.Accepte(UnitesTournee(az, pente)) }
            }

        fun declarer(uniteAzimut: UniteAzimut, unitePente: UnitePente) = UnitesTournee(uniteAzimut, unitePente)
    }
}

/** Distance mesurée suivant la pente ramenée à l'horizontale (spec § 3). */
object DistanceHorizontale {
    /**
     * d_h = d·cos(α), avec α = atan(p / 100) en pourcent, α = p en degrés. [pente] `null` (non saisie) :
     * distance inchangée. Une pente négative (lecture en descente) est acceptée : l'angle doit être dans
     * ]-90°, 90°[ (arbitrage de Pascal 2026-10-16) ; 90° et au-delà sont refusés (spec § 6).
     */
    fun calculer(distanceMesureeM: Double, pente: Double?, unite: UnitePente): Resultat<Double> {
        if (!distanceMesureeM.isFinite() || distanceMesureeM < 0.0) return Resultat.Refuse(RefusRepere.DistanceInvalide(distanceMesureeM))
        if (pente == null) return Resultat.Accepte(distanceMesureeM)
        if (!pente.isFinite()) return Resultat.Refuse(RefusRepere.PenteHorsPlage(pente, unite))
        val alphaRad = when (unite) {
            UnitePente.POURCENT -> atan(pente / 100.0)
            UnitePente.DEGRES -> {
                if (abs(pente) >= 90.0) return Resultat.Refuse(RefusRepere.PenteHorsPlage(pente, unite))
                pente * PI / 180.0
            }
        }
        return Resultat.Accepte(distanceMesureeM * cos(alphaRad))
    }
}
