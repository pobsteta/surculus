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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import io.github.pobsteta.surculus.dictee.IssueInstallation
import io.github.pobsteta.surculus.dictee.ModeleVoskTelechargement
import io.github.pobsteta.surculus.dictee.PhaseInstallation
import io.github.pobsteta.surculus.dictee.SuiviInstallationVosk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Taille du modèle installé en Mo (arrondie au supérieur, 0 si absent). */
private fun tailleInstalleeMo(ctx: android.content.Context): Int {
    val octets = ModeleVoskImport.dossier(ctx).walkTopDown().filter { it.isFile }.sumOf { it.length() }
    return ((octets + 1_048_575) / 1_048_576).toInt()
}

/**
 * Section « Dictée vocale » de Paramètres (mission 048) : état du modèle Vosk, téléchargement (comme Marculus),
 * import par fichier, suppression (avec confirmation). Un échec ou une annulation ne retire jamais le modèle déjà
 * installé ([ModeleVoskImport]). « Annuler » n'est proposé que jusqu'à la vérification de l'empreinte : ensuite
 * l'installation va au bout et l'écran le dit. L'installation et son état vivent hors de la composition
 * ([SuiviInstallationVosk]) : en revenant dans Paramètres, l'écran retrouve la phase et la progression.
 * Boutons d'au moins 48 dp ; styles de texte du thème (aucune taille propre).
 */
@Composable
fun SectionDicteeVocale() {
    val ctx = LocalContext.current
    val portee = rememberCoroutineScope()
    var installeMo by remember { mutableStateOf<Int?>(null) }
    val etat by SuiviInstallationVosk.etat.collectAsState()
    val phase = etat.phase
    var messageSuppression by remember { mutableStateOf<String?>(null) }
    var confirmerSuppression by remember { mutableStateOf(false) }

    // Relu au retour dans l'écran et à chaque fin d'installation ; ne lève jamais d'exception.
    LaunchedEffect(phase) {
        installeMo = withContext(Dispatchers.IO) {
            runCatching { if (ModeleVoskImport.estInstalle(ctx)) tailleInstalleeMo(ctx) else null }.getOrNull()
        }
    }

    val msgOk = stringResource(R.string.dictee_ok)
    val msgSupprime = stringResource(R.string.dictee_supprime)
    val msgSuppressionRefusee = stringResource(R.string.dictee_supprimer_refuse)
    val msgEchecConserve = stringResource(R.string.dictee_echec_conserve)
    val msgEchec = stringResource(R.string.dictee_echec)
    val msgAnnuleConserve = stringResource(R.string.dictee_annule_conserve)
    val msgAnnule = stringResource(R.string.dictee_annule)
    val msgRefusReseau = stringResource(R.string.dictee_refus_reseau)
    val message = messageSuppression ?: when (val issue = etat.issue) {
        null -> null
        IssueInstallation.Ok -> msgOk
        is IssueInstallation.Annule -> if (issue.avait) msgAnnuleConserve else msgAnnule
        is IssueInstallation.Echec -> (if (issue.avait) msgEchecConserve else msgEchec).format(issue.cause)
    }

    val selecteur = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            messageSuppression = null
            SuiviInstallationVosk.importer(ctx, uri)
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.dictee_section), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.dictee_desc), style = MaterialTheme.typography.bodySmall)
        Text(
            installeMo?.let { stringResource(R.string.dictee_etat_oui, it) } ?: stringResource(R.string.dictee_etat_non),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (phase != PhaseInstallation.RIEN) {
            val p = etat.progression
            if (phase == PhaseInstallation.RESEAU && p != null && p >= 0f) {
                LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.dictee_progression, (p * 100).toInt()), style = MaterialTheme.typography.bodySmall)
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    stringResource(
                        when (phase) {
                            PhaseInstallation.RESEAU -> R.string.dictee_attente
                            PhaseInstallation.FICHIER -> R.string.dictee_import_attente
                            else -> R.string.dictee_installation
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (phase != PhaseInstallation.INSTALLATION) {
                OutlinedButton(
                    onClick = { SuiviInstallationVosk.annuler() },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.dictee_annuler)) }
            }
        } else {
            Button(
                onClick = {
                    messageSuppression = null
                    SuiviInstallationVosk.telecharger(ctx, msgRefusReseau)
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
                    onClick = { confirmerSuppression = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.dictee_supprimer)) }
            }
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }

    if (confirmerSuppression) {
        AlertDialog(
            onDismissRequest = { confirmerSuppression = false },
            title = { Text(stringResource(R.string.dictee_supprimer_titre)) },
            text = { Text(stringResource(R.string.dictee_supprimer_texte)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmerSuppression = false
                        portee.launch {
                            val faite = withContext(Dispatchers.IO) { ModeleVoskImport.supprimer(ctx) }
                            SuiviInstallationVosk.effacerIssue()
                            messageSuppression = if (faite) msgSupprime else msgSuppressionRefusee
                            installeMo = withContext(Dispatchers.IO) {
                                runCatching { if (ModeleVoskImport.estInstalle(ctx)) tailleInstalleeMo(ctx) else null }.getOrNull()
                            }
                        }
                    },
                    modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp),
                ) { Text(stringResource(R.string.dictee_confirmer)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmerSuppression = false },
                    modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp),
                ) { Text(stringResource(R.string.dictee_annuler)) }
            },
        )
    }
}
