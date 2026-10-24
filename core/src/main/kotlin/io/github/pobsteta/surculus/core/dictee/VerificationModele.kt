/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 *
 * Écrit pour Surculus (mission 013, ADR 0004) ; aucun code repris de Marculus ni de terrain.
 */
package io.github.pobsteta.surculus.core.dictee

import fr.nemeton.terrain.voix.InstallationModele
import java.io.IOException
import java.io.InputStream

/** Nom et empreinte du modèle de dictée (ADR 0004). Seul `vosk-model-small-fr-0.22` est accepté. */
object VerificationModele {
    const val NOM_MODELE = "vosk-model-small-fr-0.22"

    /** Somme SHA-256 de l'archive officielle, inscrite dans `docs/dependances.md` (un test les confronte). */
    const val SHA256_ARCHIVE = "cabf6180e177eb9b3a9a9d43a437bd5e549f3a7d09525e5d69a3fed787be12ad"

    private val hex64 = Regex("[0-9a-fA-F]{64}")

    /**
     * Vrai si [nom] est le nom exact du modèle, [attendu] une empreinte SHA-256 bien formée (64 caractères
     * hexadécimaux, casse ignorée : décision 13) et [contenu] non vide, d'empreinte égale. [contenu] null
     * (fichier absent) ou illisible : refus net, sans exception. Le flux est lu par blocs (le modèle fait
     * 42 Mo) puis fermé.
     */
    fun verifier(nom: String, contenu: InputStream?, attendu: String): Boolean {
        if (contenu == null) return false
        return try {
            contenu.use { flux ->
                if (nom != NOM_MODELE || !hex64.matches(attendu)) return false
                val compte = Compteur(flux)
                // Empreinte de terrain-voix (hexadécimal minuscule) ; nom, forme et vide restent contrôlés ici.
                val obtenu = InstallationModele.empreinte(compte)
                compte.octets > 0 && obtenu.equals(attendu, ignoreCase = true)
            }
        } catch (_: IOException) {
            false
        }
    }

    private class Compteur(private val flux: InputStream) : InputStream() {
        var octets = 0L
            private set

        override fun read(): Int = flux.read().also { if (it >= 0) octets++ }

        override fun read(b: ByteArray, off: Int, len: Int): Int = flux.read(b, off, len).also { if (it > 0) octets += it }
    }
}
