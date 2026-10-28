/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.controle

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Cap **magnétique pur**, en degrés dans [0, 360[, tel que le capteur du téléphone le donne (bord haut de l'appareil, nord
 * magnétique). Jamais un `Double` nu : le type dit qu'aucune déclinaison n'y est ajoutée. Elle est ajoutée **une seule
 * fois**, par la session de contrôle (`PointDerive`, WMM2025), qui l'écrit au journal (mission 012-B2, plan § 4).
 */
@JvmInline
value class CapMagnetique private constructor(val degres: Double) {
    companion object {
        /** Ramène un angle quelconque dans [0, 360[ ; `null` s'il n'est pas fini. */
        fun de(degres: Double): CapMagnetique? =
            if (!degres.isFinite()) null else CapMagnetique(((degres % 360.0) + 360.0) % 360.0)
    }
}

/** Moyenne circulaire : 359° et 1° donnent 0°, jamais 180°. Même principe que le lissage de Groma (cercle unité). */
object LissageCap {
    fun moyenne(degres: List<Double>): CapMagnetique? {
        if (degres.isEmpty()) return null
        var s = 0.0
        var c = 0.0
        for (d in degres) {
            val r = Math.toRadians(d)
            s += sin(r)
            c += cos(r)
        }
        // Les lectures s'annulent (par exemple 0° et 180°) : pas de moyenne, on ne devine pas.
        if (abs(s) < 1e-9 && abs(c) < 1e-9) return null
        return CapMagnetique.de(Math.toDegrees(atan2(s, c)))
    }
}

/** Les lectures de la dernière seconde (fenêtre glissante) : « Pointer » prend leur moyenne, pas un tremblement. */
class FenetreCap(private val dureeMs: Long = DUREE_MS) {
    private val lectures = ArrayDeque<Pair<Long, Double>>()

    fun ajouter(instantMs: Long, degres: Double) {
        lectures.addLast(instantMs to degres)
        purger(instantMs)
    }

    /** Moyenne des lectures des [dureeMs] dernières millisecondes avant [instantMs] ; `null` sans lecture. */
    fun moyenne(instantMs: Long): CapMagnetique? {
        purger(instantMs)
        return LissageCap.moyenne(lectures.map { it.second })
    }

    fun vider() = lectures.clear()

    private fun purger(instantMs: Long) {
        while (lectures.isNotEmpty() && instantMs - lectures.first().first > dureeMs) lectures.removeFirst()
    }

    companion object {
        const val DUREE_MS = 1000L
    }
}

/** Précision que le capteur dit de lui-même (niveaux de `SensorManager.SENSOR_STATUS_*`, recopiés pour rester en Kotlin pur). */
enum class StatutCapteur(val niveau: Int) {
    INUTILISABLE(0), FAIBLE(1), MOYEN(2), ELEVE(3);

    companion object {
        fun de(niveau: Int): StatutCapteur = entries.firstOrNull { it.niveau == niveau } ?: INUTILISABLE
    }
}

/**
 * Classement de la lecture : **bonne**, **douteuse** (« dessinez un 8 », ou « tenez-le à plat »), **inutilisable**.
 * Les seuils sont **provisoires** : ils se fixent à l'essai en forêt, pas devinés ici (décision de Pascal du 2026-10-27).
 */
enum class ClasseBoussole {
    BONNE, DOUTEUSE, INUTILISABLE;

    companion object {
        /** Au-delà, le téléphone n'est plus « à plat » : la lecture du bord haut se trompe. Provisoire. */
        const val INCLINAISON_MAX_DEG = 25.0

        /** Incertitude de cap annoncée par le capteur au-delà de laquelle le cap est douteux. Provisoire. */
        const val INCERTITUDE_MAX_DEG = 15.0

        fun depuis(statut: StatutCapteur?, incertitudeRad: Double?, inclinaisonDeg: Double?): ClasseBoussole {
            if (statut == StatutCapteur.INUTILISABLE) return INUTILISABLE
            if (statut == StatutCapteur.FAIBLE) return DOUTEUSE
            if (inclinaisonDeg != null && inclinaisonDeg > INCLINAISON_MAX_DEG) return DOUTEUSE
            // Le capteur donne -1 quand il n'annonce pas d'incertitude : on ne la devine pas.
            if (incertitudeRad != null && incertitudeRad >= 0.0 && Math.toDegrees(incertitudeRad) > INCERTITUDE_MAX_DEG) return DOUTEUSE
            return BONNE
        }
    }
}

/** Champ magnétique perturbé (canne métallique, récepteur RTK, coque aimantée) : on avertit, on ne corrige rien. */
object ChampMagnetique {
    /** Bornes de la norme du champ en µT autour du champ terrestre de la France (de l'ordre de 45 µT). **Provisoires**, à fixer à l'essai. */
    const val NORME_MIN_UT = 30.0
    const val NORME_MAX_UT = 60.0

    fun perturbe(normeMicroTesla: Double?): Boolean = normeMicroTesla != null && (normeMicroTesla < NORME_MIN_UT || normeMicroTesla > NORME_MAX_UT)
}

/** Ce que la boussole montre à l'écran à un instant : cap lissé, précision et champ. */
data class LectureBoussole(
    val cap: CapMagnetique?,
    /** « ± 8° » quand le capteur annonce une incertitude. */
    val incertitudeDeg: Double?,
    val classe: ClasseBoussole,
    val champPerturbe: Boolean,
) {
    /** À avertir (sans bloquer) : « Boussole peu fiable. Dessinez un 8 avec le téléphone. » */
    val avertir: Boolean get() = classe != ClasseBoussole.BONNE || champPerturbe
}
