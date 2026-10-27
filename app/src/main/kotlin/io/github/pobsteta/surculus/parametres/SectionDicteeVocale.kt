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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/** Taille du modèle installé en Mo (arrondie au supérieur, 0 si absent). */
private fun tailleInstalleeMo(ctx: android.content.Context): Int {
    val octets = ModeleVoskImport.dossier(ctx).walkTopDown().filter { it.isFile }.sumOf { it.length() }
    return ((octets + 1_048_575) / 1_048_576).toInt()
}

/** Ce que l'écran montre pendant une installation. */
private enum class Phase { RIEN, RESEAU, FICHIER, INSTALLATION }

/**
 * Section « Dictée vocale » de Paramètres (mission 048) : état du modèle Vosk, téléchargement (comme Marculus),
 * import par fichier, suppression (avec confirmation). Un échec ou une annulation ne retire jamais le modèle déjà
 * installé ([ModeleVoskImport]). « Annuler » n'est proposé que jusqu'à la vérification de l'empreinte : ensuite
 * l'installation va au bout et l'écran le dit. Boutons d'au moins 48 dp ; styles de texte du thème (aucune taille propre).
 */
@Composable
fun SectionDicteeVocale() {
    val ctx = LocalContext.current
    val portee = rememberCoroutineScope()
    var installeMo by remember { mutableStateOf<Int?>(null) }
    var progression by remember { mutableStateOf<Float?>(null) }
    var phase by remember { mutableStateOf(Phase.RIEN) }
    val annulation = remember { AtomicBoolean(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmerSuppression by remember { mutableStateOf(false) }

    suspend fun actualiser() {
        installeMo = withContext(Dispatchers.IO) { if (ModeleVoskImport.estInstalle(ctx)) tailleInstalleeMo(ctx) else null }
    }
    LaunchedEffect(Unit) { actualiser() }

    val msgOk = stringResource(R.string.dictee_ok)
    val msgSupprime = stringResource(R.string.dictee_supprime)
    val msgEchecConserve = stringResource(R.string.dictee_echec_conserve)
    val msgEchec = stringResource(R.string.dictee_echec)
    val msgAnnuleConserve = stringResource(R.string.dictee_annule_conserve)
    val msgAnnule = stringResource(R.string.dictee_annule)
    val msgRefusReseau = stringResource(R.string.dictee_refus_reseau)

    /** [installation] reçoit « annulé ? » et « point de non-retour atteint » ; elle tourne hors du fil principal. */
    fun lancer(debut: Phase, installation: (continuer: () -> Boolean, nonRetour: () -> Unit) -> Result<Unit>) {
        if (phase != Phase.RIEN) return
        phase = debut
        message = null
        progression = null
        annulation.set(false)
        portee.launch {
            val avait = installeMo != null
            val resultat = withContext(Dispatchers.IO) {
                installation({ !annulation.get() }, { phase = Phase.INSTALLATION })
            }
            val annule = resultat.isFailure && annulation.get()
            message = when {
                resultat.isSuccess -> msgOk
                annule -> if (avait) msgAnnuleConserve else msgAnnule
                else -> {
                    val cause = resultat.exceptionOrNull()?.message ?: "erreur inconnue"
                    (if (avait) msgEchecConserve else msgEchec).format(cause)
                }
            }
            progression = null
            phase = Phase.RIEN
            actualiser()
        }
    }

    val selecteur = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            lancer(Phase.FICHIER) { continuer, nonRetour -> ModeleVoskImport.importer(ctx, uri, continuer, nonRetour) }
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.dictee_section), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.dictee_desc), style = MaterialTheme.typography.bodySmall)
        Text(
            installeMo?.let { stringResource(R.string.dictee_etat_oui, it) } ?: stringResource(R.string.dictee_etat_non),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (phase != Phase.RIEN) {
            val p = progression
            if (phase == Phase.RESEAU && p != null && p >= 0f) {
                LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.dictee_progression, (p * 100).toInt()), style = MaterialTheme.typography.bodySmall)
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    stringResource(
                        when (phase) {
                            Phase.RESEAU -> R.string.dictee_attente
                            Phase.FICHIER -> R.string.dictee_import_attente
                            else -> R.string.dictee_installation
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (phase != Phase.INSTALLATION) {
                OutlinedButton(
                    onClick = { annulation.set(true) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.dictee_annuler)) }
            }
        } else {
            Button(
                onClick = {
                    lancer(Phase.RESEAU) { continuer, nonRetour ->
                        ModeleVoskImport.installer(
                            ctx, ModeleVoskTelechargement.URL_MODELE,
                            progression = { progression = it },
                            continuer = continuer,
                            messageRefus = msgRefusReseau,
                            apresVerification = nonRetour,
                            source = ModeleVoskTelechargement.source,
                        )
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
                            withContext(Dispatchers.IO) { ModeleVoskImport.supprimer(ctx) }
                            message = msgSupprime
                            actualiser()
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
