/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.controle

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * La boussole du téléphone (mission 012-B2) : le cap du **bord haut** de l'appareil tenu à plat, par rapport au nord
 * **magnétique**, rendu tel quel en [CapMagnetique]. Cette classe ne connaît ni la position ni le modèle géomagnétique du
 * système : la déclinaison est ajoutée **une seule fois**, par la session de contrôle (plan § 4, quatre garde-fous ; un test
 * de source interdit ici la classe du système qui la calcule).
 *
 * Capteur : vecteur de rotation (accéléromètre, magnétomètre, gyroscope) ; à défaut, accéléromètre et champ magnétique.
 * Ni l'un ni l'autre : [disponible] est faux, l'écran propose le clavier.
 */
class BoussoleTelephone(private val capteurs: SensorManager) : SensorEventListener {
    private val vecteurRotation: Sensor? = capteurs.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometre: Sensor? = capteurs.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val champ: Sensor? = capteurs.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    /** Vrai si le téléphone a de quoi lire un cap. */
    val disponible: Boolean = vecteurRotation != null || (accelerometre != null && champ != null)

    private val fenetre = FenetreCap()
    private val _lecture = MutableStateFlow(LectureBoussole(null, null, ClasseBoussole.INUTILISABLE, false))
    val lecture: StateFlow<LectureBoussole> = _lecture

    private var statut: StatutCapteur? = null
    private var incertitudeRad: Double? = null
    private var inclinaisonDeg: Double? = null
    private var normeChampUt: Double? = null
    private var derniereMiseAJourMs = 0L
    private val gravite = FloatArray(3)
    private val magnetique = FloatArray(3)
    private var aGravite = false
    private var aMagnetique = false
    private val matrice = FloatArray(9)
    private val orientation = FloatArray(3)

    fun demarrer() {
        fenetre.vider()
        vecteurRotation?.let { capteurs.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        if (vecteurRotation == null) accelerometre?.let { capteurs.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        champ?.let { capteurs.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun arreter() {
        capteurs.unregisterListener(this)
    }

    /** Cap moyenné sur la dernière seconde : celui que « Pointer » prend à l'appui. */
    fun capDeLaDerniereSeconde(): CapMagnetique? = synchronized(fenetre) { fenetre.moyenne(SystemClock.elapsedRealtime()) }

    override fun onAccuracyChanged(capteur: Sensor, precision: Int) {
        if (capteur.type == (if (vecteurRotation != null) Sensor.TYPE_ROTATION_VECTOR else Sensor.TYPE_MAGNETIC_FIELD)) {
            statut = StatutCapteur.de(precision)
            publier(force = true)
        }
    }

    override fun onSensorChanged(e: SensorEvent) {
        when (e.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(matrice, e.values)
                incertitudeRad = e.values.getOrNull(4)?.toDouble()
                lireOrientation()
            }
            Sensor.TYPE_ACCELEROMETER -> {
                e.values.copyInto(gravite, endIndex = 3)
                aGravite = true
                if (vecteurRotation == null) lireOrientationAccelMag()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                e.values.copyInto(magnetique, endIndex = 3)
                aMagnetique = true
                normeChampUt = sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2]).toDouble()
                if (vecteurRotation == null) lireOrientationAccelMag()
            }
        }
    }

    private fun lireOrientationAccelMag() {
        if (aGravite && aMagnetique && SensorManager.getRotationMatrix(matrice, null, gravite, magnetique)) lireOrientation()
    }

    /** `orientation[0]` : azimut du bord haut par rapport au nord magnétique (documentation de `SensorManager.getOrientation`). */
    private fun lireOrientation() {
        SensorManager.getOrientation(matrice, orientation)
        val maintenant = SystemClock.elapsedRealtime()
        // matrice[8] = cosinus de l'angle entre la normale de l'écran et la verticale : 1 quand le téléphone est à plat.
        inclinaisonDeg = Math.toDegrees(acos(matrice[8].toDouble().coerceIn(-1.0, 1.0)))
        synchronized(fenetre) { fenetre.ajouter(maintenant, Math.toDegrees(orientation[0].toDouble())) }
        publier(force = false)
    }

    private fun publier(force: Boolean) {
        val maintenant = SystemClock.elapsedRealtime()
        if (!force && maintenant - derniereMiseAJourMs < PERIODE_AFFICHAGE_MS) return
        derniereMiseAJourMs = maintenant
        val cap = capDeLaDerniereSeconde()
        _lecture.value = LectureBoussole(
            cap = cap,
            incertitudeDeg = incertitudeRad?.takeIf { it >= 0.0 }?.let { Math.toDegrees(it) },
            classe = if (cap == null) ClasseBoussole.INUTILISABLE else ClasseBoussole.depuis(statut, incertitudeRad, inclinaisonDeg),
            champPerturbe = ChampMagnetique.perturbe(normeChampUt),
        )
    }

    private companion object {
        const val PERIODE_AFFICHAGE_MS = 100L
    }
}

/** La boussole du téléphone, écoutée seulement tant que [actif] (jamais en arrière-plan) ; `null` si le capteur manque. */
@Composable
fun rememberBoussole(actif: Boolean): Pair<BoussoleTelephone?, State<LectureBoussole>> {
    val ctx = LocalContext.current
    val boussole = remember {
        (ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager)?.let(::BoussoleTelephone)?.takeIf { it.disponible }
    }
    DisposableEffect(boussole, actif) {
        if (actif) boussole?.demarrer()
        onDispose { boussole?.arreter() }
    }
    val lecture = (boussole?.lecture ?: remember { MutableStateFlow(LectureBoussole(null, null, ClasseBoussole.INUTILISABLE, false)) }).collectAsState()
    return boussole to lecture
}
