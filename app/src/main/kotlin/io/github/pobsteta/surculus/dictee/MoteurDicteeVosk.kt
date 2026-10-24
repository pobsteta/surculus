/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 *
 * Écrit pour Surculus (mission 013, ADR 0004) ; aucun code repris de Marculus ni de terrain.
 */
package io.github.pobsteta.surculus.dictee

import io.github.pobsteta.surculus.core.dictee.EcouteurDictee
import io.github.pobsteta.surculus.core.dictee.Grammaire
import io.github.pobsteta.surculus.core.dictee.MoteurDictee
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File

/**
 * [MoteurDictee] sur Vosk 0.3.75 : le [Recognizer] reçoit la grammaire fermée de [Grammaire.jsonVosk]
 * (phrases de la spec § 4.3 et `[unk]`). Le modèle est celui que [ModeleVoskImport] a installé dans
 * [dossierModele]. La permission RECORD_AUDIO est demandée par l'appelant avant [demarrer].
 */
class MoteurDicteeVosk(private val dossierModele: File) : MoteurDictee {
    private var modele: Model? = null
    private var service: SpeechService? = null
    private var recognizer: Recognizer? = null

    override fun demarrer(ecouteur: EcouteurDictee) {
        arreter()
        try {
            val m = modele ?: Model(dossierModele.absolutePath).also { modele = it }
            val reco = Recognizer(m, FREQUENCE_HZ, Grammaire.jsonVosk())
            recognizer = reco
            val s = SpeechService(reco, FREQUENCE_HZ)
            service = s
            s.startListening(object : RecognitionListener {
                override fun onResult(hypothesis: String?) = rendre(hypothesis, ecouteur)
                override fun onFinalResult(hypothesis: String?) = rendre(hypothesis, ecouteur)
                override fun onPartialResult(hypothesis: String?) = Unit
                override fun onError(exception: Exception?) {
                    ecouteur.surErreur(exception?.message ?: "erreur de dictée")
                }
                override fun onTimeout() = Unit
            })
        } catch (e: Exception) {
            // Modèle absent ou illisible, micro refusé : message lisible, jamais d'exception muette.
            arreter()
            ecouteur.surErreur(e.message ?: "dictée impossible")
        }
    }

    /** Arrête le service puis ferme le [Recognizer] : `SpeechService` (vosk-android 0.3.75) ne le ferme jamais. */
    override fun arreter() {
        service?.stop()
        service?.shutdown()
        service = null
        recognizer?.close()
        recognizer = null
    }

    /** Libère aussi le modèle (quelques dizaines de Mo en mémoire). */
    fun fermer() {
        arreter()
        modele?.close()
        modele = null
    }

    // Phrase brute au format {"text": "..."} ; le silence (texte vide) n'est pas transmis.
    private fun rendre(json: String?, ecouteur: EcouteurDictee) {
        val texte = json?.let { JSONObject(it).optString("text", "") }.orEmpty()
        if (texte.isNotBlank()) ecouteur.surPhrase(texte)
    }

    private companion object {
        const val FREQUENCE_HZ = 16000.0f
    }
}
