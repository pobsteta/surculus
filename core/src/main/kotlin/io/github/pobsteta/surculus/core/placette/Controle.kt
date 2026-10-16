/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.core.placette

import io.github.pobsteta.surculus.core.geo.EmpriseDegres
import io.github.pobsteta.surculus.core.geo.EmpriseLambert93
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.geo.Projections
import io.github.pobsteta.surculus.core.reperes.PointGeographique
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.hypot

/**
 * Piquet d'une placette de la tournée : UUID de la placette (`placettes.uuid`), code lu sur le piquet
 * (`placettes.code_placette`) et position stockée (EPSG:2154, x = E, y = N, mètres).
 */
data class Piquet(val placette: UuidPlacette, val code: String, val position: PointLambert93)

/** Contrôle rattaché à une placette (ADR 0003, spec § 4.3). */
data class Rattache(
    val piquet: Piquet,
    /** Distance horizontale, en mètres de Lambert-93, entre la position mesurée et le piquet. */
    val distanceM: Double,
    /** Vrai seulement si le code lu a rattaché le contrôle à un piquet à plus de 15 m (le code lu l'emporte sur le GNSS ; arbitrage de Pascal du 2026-10-16 (spec § 6)). */
    val parIdentite: Boolean,
    /** Position portée par le contrôle : celle du piquet si rattaché par identité, la position mesurée sinon. */
    val positionControle: PointLambert93,
    /** Position GNSS mesurée (moyennée), toujours gardée à part (`positions_controles`). */
    val positionMesuree: PointLambert93,
)

/**
 * Règle des 15 m de l'import sommieR 0.31.1 (`distances <= 15`, `R/travaux_qfield.R` l. 303), borne incluse.
 * Distances planes en Lambert-93 (mètres) ; la position mesurée est déjà convertie en EPSG:2154.
 */
object Rattachement {
    const val DISTANCE_MAX_M = 15.0

    fun distanceM(a: PointLambert93, b: PointLambert93): Double = hypot(a.xMetres - b.xMetres, a.yMetres - b.yMetres)

    /**
     * [codeLu] : code du piquet confirmé par l'agent, `null` s'il n'a rien confirmé. Le code lu l'emporte sur
     * le GNSS ; sans code, la placette la plus proche, une égalité de distance départagée par le code.
     */
    fun rattacher(piquets: List<Piquet>, mesuree: PointLambert93, codeLu: String? = null): Verdict<Rattache> {
        if (piquets.isEmpty()) return Verdict.Refuse(RefusPlacette.AucunePlacette)
        if (codeLu != null) {
            val p = piquets.firstOrNull { it.code == codeLu } ?: return Verdict.Refuse(RefusPlacette.CodeInconnu(codeLu))
            val d = distanceM(p.position, mesuree)
            val parIdentite = d > DISTANCE_MAX_M
            return Verdict.Accepte(Rattache(p, d, parIdentite, if (parIdentite) p.position else mesuree, mesuree))
        }
        val plusProche = piquets.minWith(compareBy<Piquet>({ distanceM(it.position, mesuree) }, { it.code }))
        val d = distanceM(plusProche.position, mesuree)
        if (d > DISTANCE_MAX_M) return Verdict.Refuse(RefusPlacette.HorsQuinzeMetres(d))
        return Verdict.Accepte(Rattache(plusProche, d, false, mesuree, mesuree))
    }
}

/** Jour civil des contrôles : fuseau Europe/Paris (ADR 0005), jamais UTC. */
object JourCivil {
    val FUSEAU: ZoneId = ZoneId.of("Europe/Paris")

    fun de(instant: Instant): LocalDate = instant.atZone(FUSEAU).toLocalDate()
}

/** Contrôle déjà écrit dans la tournée. */
data class ControleEcrit(val uuid: UuidControle, val placette: UuidPlacette, val jour: LocalDate)

/** Effet d'une saisie de contrôle. */
sealed interface EcritureControle {
    /** Premier contrôle de cette placette ce jour-là. */
    data class Nouveau(val controle: ControleEcrit) : EcritureControle

    /** La placette a déjà un contrôle ce jour-là : la saisie le corrige (tant que la tournée n'est pas rendue). */
    data class Corrige(val premier: ControleEcrit) : EcritureControle
}

/** Un contrôle par placette et par jour civil Europe/Paris, dans une tournée (clé : placette et jour civil Europe/Paris ; arbitrage de Pascal du 2026-10-16 (spec § 6)). */
object UnControleParJour {
    fun ecrire(existants: List<ControleEcrit>, uuid: UuidControle, placette: UuidPlacette, saisiLe: Instant): EcritureControle {
        val jour = JourCivil.de(saisiLe)
        val premier = existants.firstOrNull { it.placette == placette && it.jour == jour }
        return if (premier != null) EcritureControle.Corrige(premier) else EcritureControle.Nouveau(ControleEcrit(uuid, placette, jour))
    }
}

/** Contrôle d'un point écrit : en France, dans l'emprise de la forêt, inversion d'axes détectée. */
data class ControlePoint(
    val enFrance: Boolean,
    /** `null` si aucune emprise de forêt n'est donnée. */
    val dansEmprise: Boolean?,
    /** Point hors de France qui y tombe une fois ses axes permutés : (lat, lon) ou (x, y) inversés. */
    val inversionDetectee: Boolean,
) {
    val refuse: Boolean get() = !enFrance || dansEmprise == false
}

/** Tout point écrit tombe dans l'emprise de la forêt et en France (spec § 6, `CLAUDE.md`). */
object EmpriseEtFrance {
    /** Domaine d'usage d'EPSG:2154 en degrés (`core.geo`). */
    private fun dansDomaine(latDeg: Double, lonDeg: Double) = EmpriseDegres.FRANCE_METROPOLITAINE.contient(latDeg, lonDeg)

    fun enFrance(p: PointLambert93): Boolean = EmpriseLambert93.FRANCE_METROPOLITAINE.contient(p)

    fun enFrance(g: PointGeographique): Boolean = dansDomaine(g.latitudeDeg, g.longitudeDeg)

    /** Point Lambert-93 (x = E, y = N, mètres). */
    fun controler(p: PointLambert93, empriseForet: EmpriseLambert93? = null): ControlePoint {
        val fr = enFrance(p)
        val inverse = runCatching { PointLambert93(p.yMetres, p.xMetres) }.getOrNull()
        return ControlePoint(fr, empriseForet?.contient(p), !fr && inverse != null && enFrance(inverse))
    }

    /** Point GNSS en (latitude, longitude), degrés décimaux ; testé en degrés, avant toute projection. */
    fun controler(g: PointGeographique, empriseForet: EmpriseLambert93? = null): ControlePoint {
        val fr = enFrance(g)
        return ControlePoint(
            enFrance = fr,
            dansEmprise = if (fr) empriseForet?.contient(versLambert93(g)) else empriseForet?.let { false },
            inversionDetectee = !fr && dansDomaine(g.longitudeDeg, g.latitudeDeg),
        )
    }

    /** (latitude, longitude) vers Lambert-93. Attention : `Projections.versLambert93` prend **(lon, lat)**. */
    fun versLambert93(g: PointGeographique): PointLambert93 {
        val (x, y) = Projections.versLambert93(lon = g.longitudeDeg, lat = g.latitudeDeg)
        return PointLambert93(xMetres = x, yMetres = y)
    }
}
