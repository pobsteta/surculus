/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.carte

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pobsteta.surculus.R
import io.github.pobsteta.surculus.ui.BandeauCompact
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.gnss.FixGnss
import io.github.pobsteta.surculus.core.guidage.DetecteurArrivee
import io.github.pobsteta.surculus.core.guidage.Guidage
import io.github.pobsteta.surculus.core.guidage.Libelles
import io.github.pobsteta.surculus.core.guidage.MoyennePosition
import io.github.pobsteta.surculus.core.placette.Piquet
import io.github.pobsteta.surculus.core.placette.Rattache
import io.github.pobsteta.surculus.core.placette.Rattachement
import io.github.pobsteta.surculus.core.placette.RefusPlacette
import io.github.pobsteta.surculus.core.placette.UuidPlacette
import io.github.pobsteta.surculus.core.placette.Verdict
import io.github.pobsteta.surculus.gpkg.PlacetteTournee
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Guidage vers une placette (spec § 4.2 et 4.3). Une question : à quelle distance, dans quelle direction ?
 * En grand : la distance. Dessous : le cap, et le radar (nord en haut, cercle de 30 m). Bip qui s'accélère à
 * l'approche, un bip d'arrivée à moins de 30 m. Dans le cercle : rattachement à 15 m, ou par le code du piquet.
 * Rien n'est écrit au journal à ce stade (mission 031 pas fusionnée).
 */
@Composable
fun EcranGuidage(id: String, placetteUuid: String, onRetour: () -> Unit) {
    val chargement = chargerTournee(id)
    val cible = (chargement as? Chargement.Pret)?.tournee?.tournee?.placettes?.firstOrNull { it.uuid == placetteUuid && it.position != null }
    val (fix, _) = rememberFix()
    // Bandeau compact (mission 053, étendu au guidage par la 055) : la flèche remplace le bouton « Retour », y compris
    // dans les états d'erreur. Pas de menu « Couches » ni de zoom : le guidage n'a pas de carte.
    Column(Modifier.fillMaxSize()) {
        BandeauCompact(
            titre = if (cible != null) stringResource(R.string.guidage_vers, cible.code.orEmpty()) else stringResource(R.string.guidage_titre),
            navigationIcon = {
                IconButton(onClick = onRetour) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.carte_retour))
                }
            },
            actions = { if (cible != null) BadgeFix(fix) },
        )
        Column(Modifier.weight(1f).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))) {
            when (chargement) {
                Chargement.EnCours -> Column(Modifier.padding(24.dp)) { Text(stringResource(R.string.carte_ouverture)) }
                is Chargement.Refus -> Column(Modifier.padding(24.dp)) {
                    Text(stringResource(R.string.carte_refus, chargement.message), color = Palette.Rouge)
                }
                is Chargement.Pret ->
                    if (cible == null) {
                        Column(Modifier.padding(24.dp)) { Text(stringResource(R.string.carte_refus, "placette $placetteUuid"), color = Palette.Rouge) }
                    } else {
                        GuidagePret(cible, chargement.tournee.tournee.placettes, fix)
                    }
            }
        }
    }
}

private const val SECONDES_DE_MOYENNE = 5

private sealed interface Confirmation {
    data class EnCours(val n: Int) : Confirmation
    data class Fini(val verdict: Verdict<Rattache>, val codeSaisi: String?) : Confirmation
}

@Composable
private fun GuidagePret(cible: PlacetteTournee, toutes: List<PlacetteTournee>, fix: FixGnss?) {
    val cap = fix?.let { Guidage.cap(it.position, cible.position!!) }
    val position = fix?.let { Guidage.versLambert93(it.position) }
    var son by remember { mutableStateOf(true) }
    var code by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf<Confirmation?>(null) }

    // Le bip : période selon la distance ; un bip distinct et unique à l'entrée dans le cercle.
    val distance = rememberUpdatedState(cap?.distanceM)
    val sonActif = rememberUpdatedState(son)
    val tonalite = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { tonalite?.release() } }
    LaunchedEffect(Unit) {
        while (true) {
            val periode = distance.value?.let { Guidage.periodeBipMs(it) }
            if (periode != null && sonActif.value) {
                tonalite?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
                delay(periode)
            } else {
                delay(500)
            }
        }
    }
    val detecteur = remember { DetecteurArrivee() }
    LaunchedEffect(cap?.distanceM) {
        val d = cap?.distanceM ?: return@LaunchedEffect
        if (detecteur.entree(d) && sonActif.value) tonalite?.startTone(ToneGenerator.TONE_PROP_BEEP2, 500)
    }

    // Rattachement : moyenne de la position pendant quelques secondes, puis la règle des 15 m (ou le code lu).
    val piquets = remember(toutes) {
        toutes.mapNotNull { p -> if (p.code != null && p.position != null) Piquet(UuidPlacette(p.uuid), p.code!!, p.position!!) else null }
    }
    val positionCourante = rememberUpdatedState(position)
    val enCours = confirmation is Confirmation.EnCours
    LaunchedEffect(enCours) {
        if (!enCours) return@LaunchedEffect
        val codeLu = code.trim().ifEmpty { null }
        val moyenne = MoyennePosition()
        repeat(SECONDES_DE_MOYENNE) { i ->
            confirmation = Confirmation.EnCours(i)
            delay(1000)
            positionCourante.value?.let(moyenne::ajouter)
        }
        val m = moyenne.moyenne()
        confirmation = if (m == null) null else Confirmation.Fini(Rattachement.rattacher(piquets, m, codeLu), codeLu)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 1. La seule chose à montrer, en grand : la distance (le titre « Vers <code> » est dans le bandeau).
        if (cap == null && fix != null) {
            // Un fix existe mais tombe hors de France : axes inversés, pas une absence de signal.
            Text(stringResource(R.string.carte_hors_france), color = Palette.Rouge, fontWeight = FontWeight.Bold)
        } else if (cap == null) {
            Text(stringResource(R.string.guidage_sans_position), color = Palette.Rouge, fontWeight = FontWeight.Bold)
        } else {
            Text(
                Libelles.distance(cap.distanceM),
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = if (cap.arrive) Palette.Vert else Palette.Noir,
            )
            // 2. Le cap, ou l'arrivée.
            if (cap.arrive) {
                Text(stringResource(R.string.guidage_arrive), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Palette.Vert)
            } else {
                Text(stringResource(R.string.guidage_cap, Libelles.cap(cap.azimutDeg)), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
        }
        // 3. La comparaison : où je suis par rapport à la placette et au cercle de 30 m.
        Radar(position, cible.position!!, Modifier.fillMaxWidth().height(260.dp))
        Text(stringResource(R.string.guidage_nord), style = MaterialTheme.typography.bodySmall)

        if (cap?.arrive == true) {
            val c = confirmation
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text(stringResource(R.string.guidage_code)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { confirmation = Confirmation.EnCours(0) },
                enabled = c !is Confirmation.EnCours,
                modifier = Modifier.fillMaxWidth().height(64.dp),
            ) { Text(stringResource(R.string.guidage_rattacher)) }
            when (c) {
                is Confirmation.EnCours -> Text(stringResource(R.string.guidage_moyenne, c.n, SECONDES_DE_MOYENNE), style = MaterialTheme.typography.bodyMedium)
                is Confirmation.Fini -> Resultat(c.verdict)
                null -> {}
            }
        }
        OutlinedButton(onClick = { son = !son }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(stringResource(if (son) R.string.guidage_son_actif else R.string.guidage_son_coupe))
        }
        Text(stringResource(R.string.guidage_rien_enregistre), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun Resultat(v: Verdict<Rattache>) {
    when (v) {
        is Verdict.Accepte -> {
            val r = v.valeur
            if (r.parIdentite) {
                Text(stringResource(R.string.guidage_rattache_identite, r.piquet.code, r.distanceM.roundToInt()), color = Palette.Orange, fontWeight = FontWeight.Bold)
            } else {
                Text(stringResource(R.string.guidage_rattache, r.piquet.code, r.distanceM.roundToInt()), color = Palette.Vert, fontWeight = FontWeight.Bold)
            }
        }
        is Verdict.Refuse -> {
            val refus = v.refus
            val texte = when (refus) {
                is RefusPlacette.HorsQuinzeMetres -> stringResource(R.string.guidage_refus_15m, refus.distanceM.roundToInt())
                is RefusPlacette.CodeInconnu -> stringResource(R.string.guidage_refus_code, refus.code)
                else -> refus.message
            }
            Text(texte, color = Palette.Rouge, fontWeight = FontWeight.Bold)
        }
        is Verdict.AConfirmer -> Text(v.alerte.message, color = Palette.Orange)
    }
}

/** Radar nord en haut : la placette au centre, le cercle de 30 m, et l'agent là où il est. L'échelle suit la distance. */
@Composable
private fun Radar(agent: PointLambert93?, placette: PointLambert93, modifier: Modifier) {
    Canvas(modifier) {
        val centre = Offset(size.width / 2, size.height / 2)
        val rayonMax = minOf(size.width, size.height) / 2 - 8.dp.toPx()
        val dx = agent?.let { it.xMetres - placette.xMetres } ?: 0.0
        val dy = agent?.let { it.yMetres - placette.yMetres } ?: 0.0
        val d = kotlin.math.hypot(dx, dy)
        // Échelle : le cercle de 30 m reste visible, l'agent reste dans le cadre.
        val etendue = maxOf(d * 1.15, Guidage.SEUIL_ARRIVEE_M * 1.5)
        val pxParMetre = (rayonMax / etendue).toFloat()
        val arrive = agent != null && Guidage.estArrive(d)
        val couleur = if (arrive) Palette.Vert else Palette.Noir
        drawCircle(couleur, radius = (Guidage.SEUIL_ARRIVEE_M * pxParMetre).toFloat(), center = centre, style = Stroke(width = 4.dp.toPx()))
        drawCircle(Palette.Rouge, radius = 8.dp.toPx(), center = centre)
        if (agent != null) {
            val p = Offset(centre.x + (dx * pxParMetre).toFloat(), centre.y - (dy * pxParMetre).toFloat())
            drawLine(couleur, p, centre, strokeWidth = 3.dp.toPx())
            drawCircle(couleur, radius = 10.dp.toPx(), center = p)
        }
    }
}
