/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 *
 * Origine : dépôt Groma (https://github.com/pobsteta/groma), fichier android/groma-core/src/main/kotlin/groma/core/Gnss.kt,
 *           état au commit f12b644 (2026-10-16). Reprise provisoire (ADR 0003, mission 003)
 *           jusqu'à l'extraction en bibliothèque commune (ADR 0008).
 * Modifications : paquet ; horodatage et drapeau « simulé » dans FixGnss ; évaluation de l'âge du fix et des corrections (PolitiqueFix).
 */
package io.github.pobsteta.surculus.core.gnss

/**
 * GNSS model (brief 2026-10-14 carte RTK, § 6). Taken from Marculus (same
 * author), adapted to Groma: pure Kotlin, no I/O.
 */

/** Position in WGS 84 degrees. */
data class Position(val latitude: Double, val longitude: Double)

/**
 * Fix quality from the GGA quality indicator. RTK_FIXE = ambiguities solved
 * (centimetres); RTK_FLOTTANT = float solution (decimetres).
 */
enum class QualiteFix(val codeNmea: Int, val libelle: String) {
    AUCUN(0, "Sans position"),
    AUTONOME(1, "Autonome"),
    DGPS(2, "DGPS"),
    PPS(3, "PPS"),
    RTK_FIXE(4, "RTK fixe"),
    RTK_FLOTTANT(5, "RTK flottant"),
    ESTIME(6, "Estimé"),
    MANUEL(7, "Manuel"),
    SIMULATION(8, "Simulation"),
    ;

    /** RTK-corrected solution (fixed or float). */
    val estRtk: Boolean get() = this == RTK_FIXE || this == RTK_FLOTTANT

    companion object {
        /** Quality of a GGA code; AUCUN for any unknown code. */
        fun depuisCode(code: Int): QualiteFix = entries.firstOrNull { it.codeNmea == code } ?: AUCUN

        /** Approximate quality from the horizontal accuracy (m), for the phone's own GNSS. */
        fun depuisPrecision(precisionM: Double?): QualiteFix = when {
            precisionM == null -> AUCUN
            precisionM <= 0.05 -> RTK_FIXE
            precisionM <= 0.5 -> RTK_FLOTTANT
            precisionM <= 3.0 -> DGPS
            else -> AUTONOME
        }
    }
}

/** Origin of a fix: external receiver (NMEA, RTK possible) or the phone's GNSS (never RTK). */
enum class OrigineFix { TELEPHONE, RECEPTEUR }

/** Satellite in view (GSV): number, elevation and azimuth (°), signal to noise (dB-Hz), talker. */
data class SatelliteVu(val prn: Int, val elevation: Int?, val azimut: Int?, val snr: Int?, val systeme: String)

/** One GNSS fix, whatever its origin; absent fields are null. */
data class FixGnss(
    val position: Position,
    val qualite: QualiteFix,
    val nbSatellites: Int,
    val hdop: Double?,
    val altitudeM: Double?,
    val ageCorrectionsS: Double?,
    val precisionHorizontaleM: Double?,
    /**
     * Instant où Surculus a **reçu** le fix (millisecondes depuis 1970-01-01T00:00:00Z, horloge du
     * téléphone), et non l'instant de la mesure par le récepteur : il sert à refuser un fix trop vieux
     * (ADR 0003). Obligatoire : pas de valeur par défaut. L'heure de la mesure, quand le récepteur la
     * donne, est dans [heureUtc].
     */
    val horodatageMs: Long,
    val pdop: Double? = null,
    val vdop: Double? = null,
    /** Reference station of the corrections (GGA field 14). */
    val stationRef: String? = null,
    /** Course over ground (°, 0 = north, clockwise), from RMC; absent when still. */
    val capDeg: Double? = null,
    /** Speed over ground (m/s), from RMC. */
    val vitesseMs: Double? = null,
    val satellites: List<SatelliteVu> = emptyList(),
    val origine: OrigineFix = OrigineFix.RECEPTEUR,
    /** Heure UTC de la mesure telle que le récepteur l'écrit dans la GGA (`hhmmss.ss`), sinon null. */
    val heureUtc: String? = null,
    /**
     * Qualité annoncée par le récepteur quand [PolitiqueFix] l'a rétrogradée (corrections trop vieilles
     * ou d'âge inconnu) ; [qualite] porte alors la qualité retenue. Null si le fix n'a pas été rétrogradé.
     */
    val qualiteAnnoncee: QualiteFix? = null,
    /** Vrai si Android signale une position simulée (`Location.isMock()`) : détectée et enregistrée, ADR 0003. */
    val simule: Boolean = false,
)

/** Pourquoi un fix est refusé par [PolitiqueFix.evaluer]. */
enum class RaisonRefus { SANS_POSITION, TROP_VIEUX, HORLOGE_INCOHERENTE }

/** Verdict de [PolitiqueFix.evaluer]. */
sealed interface EvaluationFix {
    /**
     * Fix utilisable. S'il a été rétrogradé, [FixGnss.qualiteAnnoncee] garde la qualité que le
     * récepteur annonçait et [fix] porte la qualité retenue.
     */
    data class Accepte(val fix: FixGnss) : EvaluationFix {
        val qualiteAnnoncee: QualiteFix? get() = fix.qualiteAnnoncee
        val retrograde: Boolean get() = fix.qualiteAnnoncee != null
    }

    data class Refuse(val raison: RaisonRefus) : EvaluationFix
}

/**
 * Règles de fraîcheur de l'ADR 0003 : un fix plus vieux que [ageMaxFixMs] est refusé ; un RTK (fixe ou
 * flottant) dont l'âge des corrections dépasse [ageMaxCorrectionsS], **ou est absent** (champ GGA 13
 * vide, arbitrage de Pascal du 2026-10-16), est rétrogradé en autonome : il ne revendique plus de
 * centimètres. Le fix rétrogradé garde la qualité annoncée dans [FixGnss.qualiteAnnoncee].
 *
 * **Seuils (5 s, 30 s, 1 s) : valeurs retenues par Pascal le 2026-10-16, à mesurer à l'essai
 * terrain** ; ils ne sont pas tirés d'une source.
 */
data class PolitiqueFix(
    val ageMaxFixMs: Long = 5_000,
    val ageMaxCorrectionsS: Double = 30.0,
    /** Écart d'horloge toléré vers le futur (ms) avant de juger l'horloge incohérente. */
    val toleranceFuturMs: Long = 1_000,
) {
    init {
        require(ageMaxFixMs > 0 && ageMaxCorrectionsS > 0 && toleranceFuturMs >= 0) { "Seuils invalides" }
    }

    fun evaluer(fix: FixGnss, maintenantMs: Long): EvaluationFix {
        if (fix.qualite == QualiteFix.AUCUN) return EvaluationFix.Refuse(RaisonRefus.SANS_POSITION)
        val age = maintenantMs - fix.horodatageMs
        if (age < -toleranceFuturMs) return EvaluationFix.Refuse(RaisonRefus.HORLOGE_INCOHERENTE)
        if (age > ageMaxFixMs) return EvaluationFix.Refuse(RaisonRefus.TROP_VIEUX)
        val corrections = fix.ageCorrectionsS
        if (fix.qualite.estRtk && (corrections == null || corrections > ageMaxCorrectionsS)) {
            return EvaluationFix.Accepte(fix.copy(qualite = QualiteFix.AUTONOME, qualiteAnnoncee = fix.qualite))
        }
        return EvaluationFix.Accepte(fix)
    }
}
