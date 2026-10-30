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
import io.github.pobsteta.surculus.core.dictee.FluxArchive
import io.github.pobsteta.surculus.core.dictee.InstallateurModele
import io.github.pobsteta.surculus.core.dictee.SourceArchive
import io.github.pobsteta.surculus.core.dictee.VerificationModele
import java.io.File
import io.github.pobsteta.surculus.core.dictee.EchecInstallation

/**
 * Cycle de vie du modèle `vosk-model-small-fr-0.22` (ADR 0004) : import par fichier, sans réseau (mission 013), et
 * installation depuis une source quelconque, dont le téléchargement ([ModeleVoskTelechargement], mission 048).
 * L'archive est copiée dans le cache, son empreinte SHA-256 est contrôlée contre [VerificationModele.SHA256_ARCHIVE],
 * puis décompressée à côté et seulement alors substituée au modèle installé : **un échec ne l'efface jamais**
 * (voir [InstallateurModele]). Bloquant : à appeler hors du fil principal.
 */
object ModeleVoskImport {
    private const val DOSSIER = "vosk-model-fr"

    fun dossier(context: Context): File = File(context.filesDir, DOSSIER)

    private fun installateur(context: Context) = InstallateurModele(dossier(context), context.cacheDir)

    fun estInstalle(context: Context): Boolean = installateur(context).estInstalle

    /** Faux (rien n'est fait) si une installation tourne. */
    fun supprimer(context: Context): Boolean = installateur(context).supprimer()

    /** Installe le modèle depuis le fichier choisi par l'agent ; l'erreur est rendue telle quelle, en français. */
    fun importer(
        context: Context,
        source: Uri,
        continuer: () -> Boolean = { true },
        apresVerification: () -> Unit = {},
    ): Result<Unit> =
        installer(context, source.toString(), {}, continuer, null, apresVerification) {
            FluxArchive(context.contentResolver.openInputStream(source) ?: throw EchecInstallation("fichier du modèle illisible"))
        }

    /**
     * Installe depuis [source] ; [progression] 0..1 (-1 : inconnue), [continuer] faux interrompt la réception ;
     * [apresVerification] : l'archive est bonne, plus d'annulation possible. [messageRefus] : sinon celui de l'import.
     */
    fun installer(
        context: Context,
        origine: String,
        progression: (Float) -> Unit,
        continuer: () -> Boolean,
        messageRefus: String?,
        apresVerification: () -> Unit,
        source: SourceArchive,
    ): Result<Unit> {
        val i = installateur(context)
        return if (messageRefus == null) {
            i.installer(source, origine, progression, continuer, apresVerification = apresVerification)
        } else {
            i.installer(source, origine, progression, continuer, messageRefus, apresVerification)
        }
    }
}
