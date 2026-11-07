/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.core.reperes

import io.github.pobsteta.surculus.core.geo.UniteAzimut
import io.github.pobsteta.surculus.core.guidage.Libelles
import java.time.LocalDate

/**
 * Issue d'une étape de la chaîne des repères : une valeur acceptée, ou un refus motivé.
 * Un refus n'est jamais remplacé par une valeur par défaut, ni par une valeur normalisée.
 */
sealed interface Resultat<out T> {
    data class Accepte<out T>(val valeur: T) : Resultat<T>
    data class Refuse(val refus: RefusRepere) : Resultat<Nothing>

    /** Valeur acceptée, ou `null` si refus. */
    fun valeurOuNull(): T? = (this as? Accepte<T>)?.valeur

    /** Valeur acceptée ; lève [IllegalStateException] avec le motif si refus (pour les appelants qui ont déjà validé). */
    fun valeurOuEchec(): T = when (this) {
        is Accepte -> valeur
        is Refuse -> throw IllegalStateException("${refus.motif} : ${refus.message}")
    }
}

/** Enchaîne une étape si la précédente est acceptée. */
inline fun <T, R> Resultat<T>.puis(etape: (T) -> Resultat<R>): Resultat<R> = when (this) {
    is Resultat.Accepte -> etape(valeur)
    is Resultat.Refuse -> this
}

/**
 * Motifs de refus de la chaîne des repères. [motif] est un identifiant stable (snake_case), lu par les
 * fixtures et par les écrans ; [message] l'explique en français à l'agent.
 */
sealed interface RefusRepere {
    val motif: String
    val message: String

    companion object {
        /** Un nombre montré au forestier : une décimale, virgule française ; jamais « NaN » ni « Infinity » (mission 081). */
        internal fun dit(v: Double, feminin: Boolean = false): String = when {
            v.isNaN() -> "non numérique"
            v.isInfinite() -> (if (feminin) "infinie" else "infini") + if (v > 0) "" else " négati" + if (feminin) "ve" else "f"
            else -> Libelles.decimetres(v)
        }

        /** Des mètres Lambert-93 entiers, milliers séparés par l'espace fine : « 6 686 123 m » (mission 081). */
        internal fun ditMetres(v: Double): String = if (v.isFinite()) Libelles.metres(v) else dit(v)
    }

    /** Azimut saisi hors de [0, 360[ (degrés) ou [0, 400[ (grades) : refusé **avant** normalisation, valeur gardée telle quelle. */
    data class AzimutHorsPlage(val valeurSaisie: Double, val unite: UniteAzimut) : RefusRepere {
        override val motif = "azimut_hors_plage"
        override val message = "Azimut ${dit(valeurSaisie)} hors de [0, ${unite.tourComplet.toInt()}[ en ${if (unite == UniteAzimut.GRADES) "grades" else "degrés"} : ressaisir la lecture."
    }

    data object UniteAzimutNonDeclaree : RefusRepere {
        override val motif = "unite_azimut_non_declaree"
        override val message = "Unité de la boussole (degrés ou grades) non déclarée à l'ouverture de la tournée."
    }

    data class UniteAzimutInconnue(val code: String) : RefusRepere {
        override val motif = "unite_azimut_inconnue"
        override val message = "Unité de boussole « $code » inconnue : degrés (deg) ou grades (gr)."
    }

    data object UnitePenteNonDeclaree : RefusRepere {
        override val motif = "unite_pente_non_declaree"
        override val message = "Unité de la pente (% ou degrés) non déclarée à l'ouverture de la tournée."
    }

    data class UnitePenteInconnue(val code: String) : RefusRepere {
        override val motif = "unite_pente_inconnue"
        override val message = "Unité de pente « $code » inconnue : pourcent (pct) ou degrés (deg)."
    }

    /** Pente dont l'angle n'est pas dans ]-90°, 90°[ (ou non finie). */
    data class PenteHorsPlage(val pente: Double, val unite: UnitePente) : RefusRepere {
        override val motif = "pente_hors_plage"
        override val message = "Pente ${dit(pente, feminin = true)}${if (!pente.isFinite()) "" else if (unite == UnitePente.POURCENT) " %" else "°"} refusée : l'angle doit être dans ]-90°, 90°[."
    }

    data class DistanceInvalide(val distanceM: Double) : RefusRepere {
        override val motif = "distance_invalide"
        override val message = "Distance ${dit(distanceM, feminin = true)}${if (distanceM.isFinite()) " m" else ""} refusée : elle doit être un nombre positif ou nul."
    }

    /** Date hors de la validité du modèle géomagnétique : aucune extrapolation. */
    data class DateHorsValidite(val date: LocalDate, val premierJour: LocalDate, val dernierJour: LocalDate) : RefusRepere {
        override val motif = "date_hors_validite"
        override val message = "Date $date hors de la validité du WMM2025 ($premierJour au $dernierJour) : pas de déclinaison extrapolée."
    }

    data class PointHorsLimites(val latitudeDeg: Double, val longitudeDeg: Double) : RefusRepere {
        override val motif = "point_hors_limites"
        override val message = "Point : latitude ${dit(latitudeDeg)}, longitude ${dit(longitudeDeg)} (degrés), hors des limites géographiques."
    }

    /** Piquet hors de l'emprise de la France métropolitaine : axes inversés ou point non reprojeté. */
    data class PiquetHorsFrance(val xMetres: Double, val yMetres: Double) : RefusRepere {
        override val motif = "piquet_hors_france"
        override val message = "Piquet (x = ${ditMetres(xMetres)}, y = ${ditMetres(yMetres)}, Lambert-93) hors de France : axes inversés ou point non reprojeté ?"
    }

    data class ReferentielsMeles(val codes: Set<Int>) : RefusRepere {
        override val motif = "referentiels_meles"
        override val message = "Le fichier mêle les référentiels EPSG ${codes.sorted().joinToString(" et ")} : une tournée n'en porte qu'un."
    }

    data class ReferentielNonPrisEnCharge(val codes: Set<Int>) : RefusRepere {
        override val motif = "referentiel_non_pris_en_charge"
        override val message = "Référentiel EPSG ${codes.sorted().joinToString(", ")} non pris en charge : EPSG:2154 ou EPSG:9794 seulement."
    }

    data object AucunReferentiel : RefusRepere {
        override val motif = "aucun_referentiel"
        override val message = "Aucun référentiel déclaré dans le fichier."
    }
}
