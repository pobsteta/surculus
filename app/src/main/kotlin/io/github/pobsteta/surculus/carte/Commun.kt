/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.carte

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.pobsteta.surculus.R
import io.github.pobsteta.surculus.core.gnss.FixGnss
import io.github.pobsteta.surculus.gnss.SourcePosition
import io.github.pobsteta.surculus.gnss.libelleFix
import io.github.pobsteta.surculus.tournee.DepotTournees
import io.github.pobsteta.surculus.tournee.TourneeOuverte
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * La position de l'application ([SourcePosition.fix] : récepteur RTK s'il y en a un, sinon GNSS du téléphone). Demande la
 * permission de localisation une fois, et n'écoute le téléphone que tant que l'écran est visible (jamais en arrière-plan).
 * `permissionRefusee` vaut vrai si l'agent a refusé.
 */
@Composable
fun rememberFix(): Pair<FixGnss?, Boolean> {
    val ctx = LocalContext.current
    val fix by SourcePosition.fix.collectAsStateWithLifecycle()
    var autorisee by remember { mutableStateOf(SourcePosition.localisationAutorisee(ctx)) }
    var refusee by remember { mutableStateOf(false) }
    val demander = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        autorisee = SourcePosition.localisationAutorisee(ctx)
        refusee = !ok
    }
    LaunchedEffect(Unit) { if (!autorisee) demander.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
    LifecycleStartEffect(autorisee) {
        val ecoute = SourcePosition.ecouterTelephone(ctx)
        onStopOrDispose { ecoute.close() }
    }
    return fix to refusee
}

/** Badge du fix (RTK fixe, flottant, autonome, sans position), teinté selon la qualité. */
@Composable
fun BadgeFix(fix: FixGnss?, modifier: Modifier = Modifier) {
    val texte = fix?.let { libelleFix(it) } ?: androidx.compose.ui.res.stringResource(R.string.carte_sans_position)
    Text(
        text = texte,
        color = Palette.Blanc,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(Palette.du(fix?.qualite)).padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/** Résultat du chargement de la tournée : en cours, prête, ou refusée avec son motif. */
sealed interface Chargement {
    data object EnCours : Chargement
    data class Pret(val tournee: TourneeOuverte) : Chargement
    data class Refus(val message: String) : Chargement
}

/** Lit la tournée du dossier privé `files/tournees/<id>` hors du fil principal. */
@Composable
fun chargerTournee(id: String): Chargement {
    val ctx = LocalContext.current
    val etat by produceState<Chargement>(Chargement.EnCours, id) {
        value = withContext(Dispatchers.IO) {
            try {
                Chargement.Pret(DepotTournees.ouvrir(File(DepotTournees.racine(ctx), id)))
            } catch (e: io.github.pobsteta.surculus.gpkg.GpkgRefuse) {
                Chargement.Refus(e.refus.message)
            } catch (e: Exception) {
                Chargement.Refus(e.message ?: e.javaClass.simpleName)
            }
        }
    }
    return etat
}
