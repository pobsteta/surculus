/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.controle

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** La synthèse vocale du système, en français de France (mission 012-C2, plan § 7). Chaque annonce coupe la précédente. */
class VoixAndroid(contexte: Context) : MoteurVoix {
    @Volatile private var prete = false
    private var tts: TextToSpeech? = null
    private val principal = Handler(Looper.getMainLooper())

    /** Numéro de la dernière annonce demandée : seule sa fin compte (une annonce coupée par la suivante ne rouvre rien). */
    @Volatile private var dernier = 0

    override var surFin: (() -> Unit)? = null

    init {
        tts = TextToSpeech(contexte.applicationContext) { statut ->
            val moteur = tts
            prete = statut == TextToSpeech.SUCCESS && moteur != null &&
                moteur.isLanguageAvailable(Locale.FRANCE) >= TextToSpeech.LANG_AVAILABLE &&
                moteur.setLanguage(Locale.FRANCE) >= TextToSpeech.LANG_AVAILABLE
            // La fin d'annonce (012-D2, anti-larsen) : le micro se rouvre là, jamais avant.
            moteur?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) = fini(utteranceId)
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) = fini(utteranceId)
                override fun onError(utteranceId: String?, errorCode: Int) = fini(utteranceId)
                override fun onStop(utteranceId: String?, interrupted: Boolean) = fini(utteranceId)
            })
        }
    }

    private fun fini(id: String?) {
        if (id == "poche-$dernier") principal.post { surFin?.invoke() }
    }

    override val francaisDisponible: Boolean get() = prete

    override val parle: Boolean get() = tts?.isSpeaking == true

    override fun dire(texte: String): Boolean {
        val n = ++dernier
        return tts?.speak(texte, TextToSpeech.QUEUE_FLUSH, null, "poche-$n") == TextToSpeech.SUCCESS
    }

    override fun arreter() {
        dernier++ // la fin de l'annonce coupée ne compte pas
        tts?.stop()
    }
}

/** Le vibreur du téléphone (permission normale `VIBRATE`) ; un appareil sans vibreur ne vibre pas, sans erreur. */
class VibreurAndroid(contexte: Context) : Vibreur {
    private val vibreur: Vibrator? = if (android.os.Build.VERSION.SDK_INT >= 31) {
        (contexte.applicationContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        contexte.applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    override fun vibrer(motif: LongArray) {
        val v = vibreur?.takeIf { it.hasVibrator() } ?: return
        v.vibrate(VibrationEffect.createWaveform(motif, -1))
    }
}
