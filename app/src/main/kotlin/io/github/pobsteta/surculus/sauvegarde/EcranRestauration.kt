/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.sauvegarde

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import io.github.pobsteta.surculus.data.sauvegarde.CompteRendu
import io.github.pobsteta.surculus.data.sauvegarde.MotifRefus
import io.github.pobsteta.surculus.data.sauvegarde.Restauration
import io.github.pobsteta.surculus.data.sauvegarde.Signalement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Restauration d'une tournée depuis un `.sursync` choisi dans le sélecteur de fichier (`ACTION_OPEN_DOCUMENT`). */
@Composable
fun EcranRestauration() {
    val contexte = LocalContext.current
    val portee = rememberCoroutineScope()
    var resultat by remember { mutableStateOf<Resultat?>(null) }

    val choisir = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        portee.launch {
            val compte = withContext(Dispatchers.IO) {
                try {
                    val octets = contexte.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                    Restauration.restaurer(octets, JournalApp.depot(contexte))
                } catch (e: Exception) {
                    null
                }
            }
            resultat = Resultat(compte)
        }
    }

    Column(
        Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(stringResource(R.string.restauration_titre), style = MaterialTheme.typography.titleLarge)
        Button(onClick = { choisir.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth().height(72.dp)) {
            Text(stringResource(R.string.restauration_choisir))
        }
        resultat?.let { Text(texte(it.compte), style = MaterialTheme.typography.bodyMedium) }
    }
}

/** `compte` nul : le fichier n'a pas pu être ouvert ou lu. */
private class Resultat(val compte: CompteRendu?)

@Composable
private fun texte(r: CompteRendu?): String {
    if (r == null) return stringResource(R.string.restauration_lecture_impossible)
    r.refus?.let {
        return when (it) {
            MotifRefus.VERSION_FUTURE -> stringResource(R.string.restauration_version_future)
            MotifRefus.VERSION_ILLISIBLE -> stringResource(R.string.restauration_version_illisible)
        }
    }
    val lignes = mutableListOf(stringResource(R.string.restauration_ok, r.ajoutees, r.dejaPresentes))
    if (r.refusees > 0) lignes += stringResource(R.string.restauration_refusees, r.refusees)
    for (s in r.signalements) lignes += stringResource(
        when (s) {
            Signalement.FICHIER_TRONQUE -> R.string.restauration_tronque
            Signalement.LIGNE_ILLISIBLE -> R.string.restauration_ligne_illisible
            Signalement.EMPREINTE_DIFFERENTE -> R.string.restauration_empreinte
            Signalement.UUID_CONTENU_DIFFERENT -> R.string.restauration_uuid_different
            Signalement.LIGNE_AUTRE_TOURNEE -> R.string.restauration_autre_tournee
        },
    )
    return lignes.joinToString("\n")
}
