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

import android.content.Context
import android.net.Uri
import fr.nemeton.terrain.voix.InstallationModele
import io.github.pobsteta.surculus.core.dictee.VerificationModele
import java.io.File

/**
 * Import par fichier du modèle `vosk-model-small-fr-0.22` (ADR 0004), sans réseau. L'archive est copiée dans le
 * cache, son empreinte SHA-256 est contrôlée (non vide, casse ignorée) contre
 * [VerificationModele.SHA256_ARCHIVE], puis `terrain-voix` la décompresse dans le stockage interne.
 * Bloquant : à appeler hors du fil principal. Un modèle refusé n'est jamais installé.
 */
object ModeleVoskImport {
    private const val DOSSIER = "vosk-model-fr"

    fun dossier(context: Context): File = File(context.filesDir, DOSSIER)

    // `sha256` jamais nul : terrain-voix ne contrôle rien sinon. L'URL n'est pas utilisée (pas de téléchargement).
    private fun installation(context: Context) =
        InstallationModele(dossier(context), context.cacheDir, "", VerificationModele.SHA256_ARCHIVE)

    fun estInstalle(context: Context): Boolean = installation(context).estInstalle

    fun supprimer(context: Context) = installation(context).supprimer()

    /** Installe le modèle depuis le fichier choisi par l'agent ; l'erreur est rendue telle quelle, en français. */
    fun importer(context: Context, source: Uri): Result<Unit> = runCatching {
        val archive = File(context.cacheDir, "modele-vosk-importe.zip")
        try {
            val flux = context.contentResolver.openInputStream(source) ?: error("Fichier du modèle illisible")
            flux.use { entree -> archive.outputStream().use { entree.copyTo(it) } }
            val accepte = archive.inputStream().use {
                VerificationModele.verifier(VerificationModele.NOM_MODELE, it, VerificationModele.SHA256_ARCHIVE)
            }
            check(accepte) { "Ce fichier n'est pas ${VerificationModele.NOM_MODELE} (empreinte SHA-256 différente ou fichier vide)" }
            installation(context).installer(archive, source.toString()).getOrThrow()
        } finally {
            archive.delete()
        }
    }
}
