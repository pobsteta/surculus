/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.parametres

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.pobsteta.surculus.R
import io.github.pobsteta.surculus.dictee.ModeleVoskImport
import io.github.pobsteta.surculus.dictee.ModeleVoskTelechargement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Taille du modèle installé en Mo (arrondie au supérieur, 0 si absent). */
private fun tailleInstalleeMo(ctx: android.content.Context): Int {
    val octets = ModeleVoskImport.dossier(ctx).walkTopDown().filter { it.isFile }.sumOf { it.length() }
    return ((octets + 1_048_575) / 1_048_576).toInt()
}

/**
 * Section « Dictée vocale » de Paramètres (mission 048) : état du modèle Vosk, téléchargement (comme Marculus),
 * import par fichier, suppression. Un échec ne retire jamais le modèle déjà installé ([ModeleVoskImport]).
 * Boutons d'au moins 48 dp ; styles de texte du thème (aucune taille propre).
 */
@Composable
fun SectionDicteeVocale() {
    val ctx = LocalContext.current
    val portee = rememberCoroutineScope()
    var installeMo by remember { mutableStateOf<Int?>(null) }
    var progression by remember { mutableStateOf<Float?>(null) }
    var enCours by remember { mutableStateOf(false) }
    var travail by remember { mutableStateOf<Job?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    suspend fun actualiser() {
        installeMo = withContext(Dispatchers.IO) { if (ModeleVoskImport.estInstalle(ctx)) tailleInstalleeMo(ctx) else null }
    }
    LaunchedEffect(Unit) { actualiser() }

    val msgOk = stringResource(R.string.dictee_ok)
    val msgSupprime = stringResource(R.string.dictee_supprime)
    val msgEchecConserve = stringResource(R.string.dictee_echec_conserve)
    val msgEchec = stringResource(R.string.dictee_echec)
    val msgAnnule = stringResource(R.string.dictee_annule)

    fun lancer(installation: suspend () -> Result<Unit>) {
        if (enCours) return
        enCours = true
        message = null
        progression = null
        travail = portee.launch {
            val avait = installeMo != null
            // Annulation : l'installateur s'arrête sans toucher au modèle installé ; l'état est remis à plat.
            val resultat = try {
                installation()
            } catch (e: CancellationException) {
                Result.failure(IllegalStateException(msgAnnule))
            }
            progression = null
            enCours = false
            val cause = resultat.exceptionOrNull()?.message ?: "erreur inconnue"
            message = if (resultat.isSuccess) msgOk else (if (avait) msgEchecConserve else msgEchec).format(cause)
            actualiser()
        }
    }

    val selecteur = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            lancer { withContext(Dispatchers.IO) { ModeleVoskImport.importer(ctx, uri) } }
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.dictee_section), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.dictee_desc), style = MaterialTheme.typography.bodySmall)
        Text(
            installeMo?.let { stringResource(R.string.dictee_etat_oui, it) } ?: stringResource(R.string.dictee_etat_non),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (enCours) {
            val p = progression
            if (p != null && p >= 0f) {
                LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.dictee_progression, (p * 100).toInt()), style = MaterialTheme.typography.bodySmall)
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.dictee_attente), style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(
                onClick = { travail?.cancel() },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.dictee_annuler)) }
        } else {
            Button(
                onClick = {
                    lancer {
                        withContext(Dispatchers.IO) {
                            ModeleVoskImport.installer(
                                ctx, ModeleVoskTelechargement.URL_MODELE,
                                progression = { progression = it },
                                continuer = { isActive },
                                source = ModeleVoskTelechargement.source,
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(
                    stringResource(
                        if (installeMo != null) R.string.dictee_retelecharger else R.string.dictee_telecharger,
                        ModeleVoskTelechargement.TAILLE_MO,
                    ),
                )
            }
            OutlinedButton(
                onClick = { selecteur.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.dictee_fichier)) }
            if (installeMo != null) {
                OutlinedButton(
                    onClick = {
                        portee.launch {
                            withContext(Dispatchers.IO) { ModeleVoskImport.supprimer(ctx) }
                            message = msgSupprime
                            actualiser()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.dictee_supprimer)) }
            }
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}
