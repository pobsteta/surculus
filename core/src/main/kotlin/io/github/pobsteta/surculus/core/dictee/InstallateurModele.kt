/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 *
 * Écrit pour Surculus (mission 048, ADR 0004) ; aucun code repris de Marculus ni de terrain.
 */
package io.github.pobsteta.surculus.core.dictee

import fr.nemeton.terrain.voix.InstallationModele
import java.io.File
import java.io.IOException
import java.io.InputStream

/** Ce que renvoie l'ouverture d'une source : le flux de l'archive et sa taille annoncée (≤ 0 : inconnue). */
class FluxArchive(val flux: InputStream, val taille: Long = -1)

/** Où se trouve l'archive : réseau, fichier choisi sur le téléphone, ou (en test) la mémoire. */
fun interface SourceArchive {
    @Throws(IOException::class)
    fun ouvrir(): FluxArchive
}

/**
 * Installe le modèle Vosk **sans jamais effacer celui qui est déjà là tant que le nouveau n'est pas complet**
 * (mission 048). `terrain-voix` (`InstallationModele.installer`/`telecharger`, d840329) vide le dossier cible avant
 * de décompresser et sur tout échec : on ne lui donne donc jamais le vrai dossier.
 *
 * Déroulé : (1) l'archive est copiée dans un fichier temporaire du [cache] ; (2) son empreinte SHA-256 est
 * contrôlée contre [sha256] ; (3) `terrain-voix` la décompresse dans un dossier voisin « .nouveau » ; (4) seulement
 * alors le dossier installé est remplacé par le nouveau (renommage). Tout échec avant (4) supprime les
 * fichiers temporaires et laisse l'ancien modèle intact. Bloquant : à appeler hors du fil principal.
 */
class InstallateurModele(
    val dossier: File,
    private val cache: File,
    private val sha256: String = VerificationModele.SHA256_ARCHIVE,
) {
    private val nouveau = File(dossier.parentFile, dossier.name + ".nouveau")
    private val ancien = File(dossier.parentFile, dossier.name + ".ancien")
    private val partiel = File(cache, "modele-vosk-recu.part")

    // `terrain-voix` détient le nom du témoin : on lui demande si le dossier est complet.
    val estInstalle: Boolean
        get() {
            reparer()
            return complet(dossier)
        }

    private fun complet(d: File) = InstallationModele(d, cache, "", sha256).estInstalle

    /**
     * Répare l'état laissé par un arrêt brutal entre les deux renommages de [remplacer] : si le dossier installé
     * manque (ou est incomplet) et que « .ancien » (à défaut « .nouveau ») est complet, il est remis en place ;
     * puis les « .nouveau » et « .ancien » orphelins sont effacés. À ne pas appeler pendant une installation.
     */
    fun reparer() {
        if (!complet(dossier)) {
            val reste = listOf(ancien, nouveau).firstOrNull { it.exists() && complet(it) }
            if (reste != null) {
                dossier.deleteRecursively()
                check(reste.renameTo(dossier)) { "Réparation impossible : ${reste.name} non déplaçable" }
            }
        }
        ancien.deleteRecursively()
        nouveau.deleteRecursively()
    }

    fun supprimer() {
        dossier.deleteRecursively()
        nouveau.deleteRecursively()
        ancien.deleteRecursively()
    }

    /**
     * Installe l'archive de [source]. [progression] reçoit 0..1 (-1 tant que la taille est inconnue) ; [continuer]
     * faux interrompt la réception (jusqu'à la vérification de l'empreinte, [apresVerification] marque ensuite le
     * point de non-retour). [messageRefus] : message si l'empreinte est mauvaise. L'échec porte un message en français ; le modèle précédent reste en place.
     */
    fun installer(
        source: SourceArchive,
        origine: String,
        progression: (Float) -> Unit = {},
        continuer: () -> Boolean = { true },
        messageRefus: String = "Ce fichier n'est pas ${VerificationModele.NOM_MODELE} (empreinte SHA-256 différente ou fichier vide)",
        apresVerification: () -> Unit = {},
    ): Result<Unit> {
        val resultat = runCatching {
            reparer()
            cache.mkdirs()
            partiel.delete()
            nouveau.deleteRecursively()
            copier(source, progression, continuer)
            val accepte = partiel.inputStream().use { VerificationModele.verifier(VerificationModele.NOM_MODELE, it, sha256) }
            check(accepte) { messageRefus }
            // Point de non-retour : archive complète et bonne, [continuer] n'est plus consulté ; l'appelant retire « Annuler ».
            apresVerification()
            InstallationModele(nouveau, cache, "", sha256).installer(partiel, origine).getOrThrow()
            remplacer()
            progression(1f)
        }
        partiel.delete()
        if (resultat.isFailure) nouveau.deleteRecursively()
        return resultat
    }

    private fun copier(source: SourceArchive, progression: (Float) -> Unit, continuer: () -> Boolean) {
        val ouvert = source.ouvrir()
        ouvert.flux.use { entree ->
            partiel.outputStream().use { sortie ->
                val tampon = ByteArray(64 * 1024)
                var cumul = 0L
                while (true) {
                    check(continuer()) { "Téléchargement interrompu" }
                    val lu = entree.read(tampon)
                    if (lu < 0) break
                    sortie.write(tampon, 0, lu)
                    cumul += lu
                    // 90 % de la barre pour la réception, le reste pour le contrôle et la décompression.
                    progression(if (ouvert.taille > 0) (cumul.toFloat() / ouvert.taille).coerceAtMost(1f) * 0.9f else -1f)
                }
                check(ouvert.taille <= 0 || cumul == ouvert.taille) {
                    "Téléchargement incomplet ($cumul octets reçus sur ${ouvert.taille})"
                }
            }
        }
    }

    /** Le nouveau dossier est complet : il prend la place de l'ancien ; au moindre souci, l'ancien est remis. */
    private fun remplacer() {
        ancien.deleteRecursively()
        val avaitUnAncien = dossier.exists()
        if (avaitUnAncien) check(dossier.renameTo(ancien)) { "Remplacement impossible (ancien modèle non déplaçable)" }
        if (!nouveau.renameTo(dossier)) {
            val remis = !avaitUnAncien || ancien.renameTo(dossier)
            error(
                if (remis) "Remplacement impossible (nouveau modèle non déplaçable)"
                else "Remplacement impossible, l'ancien modèle sera remis en place au prochain démarrage",
            )
        }
        ancien.deleteRecursively()
    }
}
