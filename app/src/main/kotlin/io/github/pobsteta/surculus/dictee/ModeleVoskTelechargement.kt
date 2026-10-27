/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 *
 * Écrit pour Surculus (mission 048, ADR 0004) ; l'URL est celle de Marculus, aucun code repris.
 */
package io.github.pobsteta.surculus.dictee

import io.github.pobsteta.surculus.core.dictee.FluxArchive
import io.github.pobsteta.surculus.core.dictee.SourceArchive
import java.io.FilterInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Source réseau du modèle de dictée : la même archive que Marculus (`ModeleVosk.URL_MODELE`, marculus
 * `app/src/main/kotlin/io/github/pobsteta/marculus/voice/ModeleVosk.kt`, ~42 Mo, Apache 2.0). L'empreinte attendue
 * est celle de [io.github.pobsteta.surculus.core.dictee.VerificationModele] : une archive autre est refusée.
 */
object ModeleVoskTelechargement {
    const val URL_MODELE = "https://alphacephei.com/vosk/models/vosk-model-small-fr-0.22.zip"
    const val TAILLE_MO = 42

    val source = SourceArchive {
        val connexion = (URL(URL_MODELE).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            instanceFollowRedirects = true
        }
        try {
            if (connexion.responseCode != HttpURLConnection.HTTP_OK) throw IOException("serveur : code ${connexion.responseCode}")
            val flux = object : FilterInputStream(connexion.inputStream) {
                override fun close() {
                    try {
                        super.close()
                    } finally {
                        connexion.disconnect()
                    }
                }
            }
            FluxArchive(flux, connexion.contentLengthLong)
        } catch (e: Exception) {
            connexion.disconnect()
            throw e
        }
    }
}
