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
import io.github.pobsteta.surculus.core.dictee.EchecInstallation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/** Ce que l'écran dit d'un échec d'installation : une phrase en français, jamais le message de l'exception (mission 081). */
const val TEXTE_PAS_DE_RESEAU = "pas de réseau : réessayer au bureau, ou importer le fichier du modèle"
const val TEXTE_DISQUE_PLEIN = "plus de place sur le téléphone : libérer de la place puis réessayer"
const val TEXTE_ECHEC = "le téléchargement ou l'installation du modèle de voix n'a pas abouti"

/**
 * La phrase que l'écran montre après « Échec : » (toujours en minuscule) : celle d'un échec connu ([EchecInstallation]),
 * « pas de réseau » (hôte introuvable, délai dépassé, connexion refusée), « plus de place » (disque plein), sinon la
 * phrase générique. Jamais le message brut de l'exception (mission 081).
 */
internal fun causeAffichee(e: Throwable?): String {
    var t: Throwable? = e
    var profondeur = 0
    while (t != null && profondeur++ < 8) {
        when {
            t is EchecInstallation -> return t.message ?: TEXTE_ECHEC
            t is java.net.UnknownHostException || t is java.net.SocketTimeoutException || t is java.net.ConnectException -> return TEXTE_PAS_DE_RESEAU
            t.message?.let { "ENOSPC" in it || "No space left on device" in it } == true -> return TEXTE_DISQUE_PLEIN
        }
        t = t.cause
    }
    return TEXTE_ECHEC
}

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
 *
 * Mission 052 : la mécanique est ici, sans Android, pour être testée en JVM ; [SuiviInstallationVosk] la branche sur
 * le contexte. Toute fin du travail, **Error comprise** (une bibliothèque native absente lève une Error), rend l'écran
 * au repos avec une issue : jamais « en cours » pour toujours.
 */
class SuiviInstallation(
    private val portee: CoroutineScope,
    /** Journal technique : reçoit l'exception d'un échec. L'écran, lui, n'en montre jamais le message (mission 081). */
    private val journaliser: (Throwable) -> Unit = {},
) {
    private val _etat = MutableStateFlow(EtatInstallation())
    private val annulation = AtomicBoolean(false)

    val etat: StateFlow<EtatInstallation> = _etat.asStateFlow()

    /** Oublie l'issue de la dernière installation (après une suppression, pour ne pas afficher un message périmé). */
    fun effacerIssue() = _etat.update { if (it.phase == PhaseInstallation.RIEN) it.copy(issue = null) else it }

    /** Demande l'arrêt de la réception ; sans effet une fois l'empreinte vérifiée. */
    fun annuler() = annulation.set(true)

    /**
     * Lance [travail] (reçoit : « continuer ? », « point de non-retour passé », « progression »). Faux (rien lancé) si une
     * installation tourne déjà. [avait] : un modèle est-il déjà installé (lu avant le travail).
     */
    fun demarrer(
        debut: PhaseInstallation,
        avait: () -> Boolean,
        travail: (() -> Boolean, () -> Unit, (Float) -> Unit) -> Result<Unit>,
    ): Boolean {
        synchronized(this) {
            if (_etat.value.phase != PhaseInstallation.RIEN) return false
            annulation.set(false)
            _etat.value = EtatInstallation(phase = debut)
        }
        portee.launch {
            val modeleAvant = try { avait() } catch (e: Throwable) { false }
            val r = try {
                travail(
                    { !annulation.get() },
                    { _etat.update { it.copy(phase = PhaseInstallation.INSTALLATION) } },
                    { p -> _etat.update { it.copy(progression = p) } },
                )
            } catch (e: CancellationException) {
                throw e // l'annulation de la portée n'est pas un échec d'installation
            } catch (e: Throwable) {
                Result.failure(e)
            }
            val issue = when {
                r.isSuccess -> IssueInstallation.Ok
                annulation.get() -> IssueInstallation.Annule(modeleAvant)
                else -> {
                    val e = r.exceptionOrNull()
                    // Un échec connu garde sa phrase française ; tout autre dit la phrase générique (mission 081).
                    val cause = causeAffichee(e)
                    _etat.value = EtatInstallation(PhaseInstallation.RIEN, null, IssueInstallation.Echec(cause, modeleAvant))
                    // Le journal technique passe après le retour au repos : il ne peut pas bloquer l'écran.
                    if (e != null) runCatching { journaliser(e) }
                    return@launch
                }
            }
            _etat.value = EtatInstallation(PhaseInstallation.RIEN, null, issue)
        }
        return true
    }
}

/** Le suivi du processus : portée du processus, entrées et sorties d'Android. */
object SuiviInstallationVosk {
    private val suivi = SuiviInstallation(CoroutineScope(SupervisorJob() + Dispatchers.IO)) { android.util.Log.w("SuiviInstallation", "échec de l'installation de Vosk", it) }

    val etat: StateFlow<EtatInstallation> get() = suivi.etat

    fun effacerIssue() = suivi.effacerIssue()

    fun annuler() = suivi.annuler()

    /** Télécharge puis installe. Faux (rien lancé) si une installation tourne déjà. [messageRefus] : empreinte mauvaise. */
    fun telecharger(context: Context, messageRefus: String): Boolean {
        val ctx = context.applicationContext
        return suivi.demarrer(PhaseInstallation.RESEAU, { ModeleVoskImport.estInstalle(ctx) }) { continuer, nonRetour, progression ->
            ModeleVoskImport.installer(
                ctx, ModeleVoskTelechargement.URL_MODELE, progression, continuer, messageRefus, nonRetour,
                ModeleVoskTelechargement.source,
            )
        }
    }

    /** Installe depuis le fichier choisi. Faux (rien lancé) si une installation tourne déjà. */
    fun importer(context: Context, fichier: Uri): Boolean {
        val ctx = context.applicationContext
        return suivi.demarrer(PhaseInstallation.FICHIER, { ModeleVoskImport.estInstalle(ctx) }) { continuer, nonRetour, _ ->
            ModeleVoskImport.importer(ctx, fichier, continuer, nonRetour)
        }
    }
}
