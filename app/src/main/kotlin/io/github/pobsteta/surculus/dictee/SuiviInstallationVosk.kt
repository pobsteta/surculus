/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 *
 * Écrit pour Surculus (mission 048, ADR 0004) ; aucun code repris de Marculus ni de terrain.
 */
package io.github.pobsteta.surculus.dictee

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/** Ce que l'écran montre pendant une installation. */
enum class PhaseInstallation { RIEN, RESEAU, FICHIER, INSTALLATION }

/** Issue de la dernière installation, gardée jusqu'à la suivante. [avait] : un modèle était installé avant. */
sealed interface IssueInstallation {
    data object Ok : IssueInstallation
    data class Annule(val avait: Boolean) : IssueInstallation
    data class Echec(val cause: String, val avait: Boolean) : IssueInstallation
}

data class EtatInstallation(
    val phase: PhaseInstallation = PhaseInstallation.RIEN,
    val progression: Float? = null,
    val issue: IssueInstallation? = null,
)

/**
 * État de l'installation du modèle **hors de la composition** (mission 048) : l'installation tourne dans une portée
 * du processus, pas dans celle de l'écran ; en quittant puis en revenant dans Paramètres, l'écran retrouve la phase
 * et la progression au lieu de proposer une seconde installation. Le verrou du processus, lui, est dans
 * [io.github.pobsteta.surculus.core.dictee.InstallateurModele].
 */
object SuiviInstallationVosk {
    private val portee = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _etat = MutableStateFlow(EtatInstallation())
    private val annulation = AtomicBoolean(false)

    val etat: StateFlow<EtatInstallation> = _etat.asStateFlow()

    /** Oublie l'issue de la dernière installation (après une suppression, pour ne pas afficher un message périmé). */
    fun effacerIssue() = _etat.update { if (it.phase == PhaseInstallation.RIEN) it.copy(issue = null) else it }

    /** Demande l'arrêt de la réception ; sans effet une fois l'empreinte vérifiée. */
    fun annuler() = annulation.set(true)

    /** Télécharge puis installe. Faux (rien lancé) si une installation tourne déjà. [messageRefus] : empreinte mauvaise. */
    fun telecharger(context: Context, messageRefus: String): Boolean =
        demarrer(context, PhaseInstallation.RESEAU) { ctx, continuer, nonRetour, progression ->
            ModeleVoskImport.installer(
                ctx, ModeleVoskTelechargement.URL_MODELE, progression, continuer, messageRefus, nonRetour,
                ModeleVoskTelechargement.source,
            )
        }

    /** Installe depuis le fichier choisi. Faux (rien lancé) si une installation tourne déjà. */
    fun importer(context: Context, fichier: Uri): Boolean =
        demarrer(context, PhaseInstallation.FICHIER) { ctx, continuer, nonRetour, _ ->
            ModeleVoskImport.importer(ctx, fichier, continuer, nonRetour)
        }

    private fun demarrer(
        context: Context,
        debut: PhaseInstallation,
        travail: (Context, () -> Boolean, () -> Unit, (Float) -> Unit) -> Result<Unit>,
    ): Boolean {
        val ctx = context.applicationContext
        synchronized(this) {
            if (_etat.value.phase != PhaseInstallation.RIEN) return false
            annulation.set(false)
            _etat.value = EtatInstallation(phase = debut)
        }
        portee.launch {
            val avait = ModeleVoskImport.estInstalle(ctx)
            val r = try {
                travail(
                    ctx,
                    { !annulation.get() },
                    { _etat.update { it.copy(phase = PhaseInstallation.INSTALLATION) } },
                    { p -> _etat.update { it.copy(progression = p) } },
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
            val issue = when {
                r.isSuccess -> IssueInstallation.Ok
                annulation.get() -> IssueInstallation.Annule(avait)
                else -> IssueInstallation.Echec(r.exceptionOrNull()?.message ?: "erreur inconnue", avait)
            }
            _etat.value = EtatInstallation(PhaseInstallation.RIEN, null, issue)
        }
        return true
    }
}
