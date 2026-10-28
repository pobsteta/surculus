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
import io.github.pobsteta.surculus.ui.theme.LocalSombre
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.gnss.FixGnss
import io.github.pobsteta.surculus.core.guidage.Guidage
import io.github.pobsteta.surculus.core.guidage.Libelles
import io.github.pobsteta.surculus.core.session.Arrivee
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
fun EcranGuidage(
    id: String,
    placetteUuid: String,
    arriveeVue: Boolean = false,
    /** Arrivée (moins de 30 m) ou bouton « Contrôler » : ouvre le contrôle (mission 012-B1). */
    onArrivee: () -> Unit = {},
    /** L'agent est ressorti à 40 m : l'ouverture automatique est de nouveau permise. */
    onRearme: () -> Unit = {},
    onRetour: () -> Unit,
) {
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
                    Text(stringResource(R.string.carte_refus, chargement.message), color = couleurErreur())
                }
                is Chargement.Pret ->
                    if (cible == null) {
                        Column(Modifier.padding(24.dp)) { Text(stringResource(R.string.carte_refus, "placette $placetteUuid"), color = couleurErreur()) }
                    } else {
                        GuidagePret(cible, chargement.tournee.tournee.placettes, fix, arriveeVue, onArrivee, onRearme)
                    }
            }
        }
    }
}

private sealed interface Confirmation {
    data class EnCours(val n: Int) : Confirmation
    data class Fini(val verdict: Verdict<Rattache>, val codeSaisi: String?) : Confirmation
}

@Composable
private fun GuidagePret(
    cible: PlacetteTournee,
    toutes: List<PlacetteTournee>,
    fix: FixGnss?,
    arriveeVue: Boolean,
    onArrivee: () -> Unit,
    onRearme: () -> Unit,
) {
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
    // Arrivée (mission 012-B1) : un seul bip et une seule ouverture du contrôle par arrivée. Le drapeau est dans l'état de
    // navigation, car cet écran est recomposé à neuf au retour du contrôle : un détecteur local rouvrirait en boucle.
    // Il se baisse à 40 m (`Guidage.SEUIL_REARMEMENT_M`).
    val vue = rememberUpdatedState(arriveeVue)
    LaunchedEffect(cap?.distanceM) {
        val d = cap?.distanceM ?: return@LaunchedEffect
        if (vue.value) {
            if (d >= Guidage.SEUIL_REARMEMENT_M) onRearme()
        } else if (Guidage.estArrive(d)) {
            if (sonActif.value) tonalite?.startTone(ToneGenerator.TONE_PROP_BEEP2, 500)
            onArrivee()
        }
    }

    // Rattachement : moyenne de la position pendant quelques secondes, puis la règle des 15 m (ou le code lu).
    val piquets = remember(toutes) {
        toutes.mapNotNull { p -> if (p.code != null && p.position != null) Piquet(UuidPlacette(p.uuid), p.code!!, p.position!!) else null }
    }
    val positionCourante = rememberUpdatedState(fix?.position)
    val enCours = confirmation is Confirmation.EnCours
    LaunchedEffect(enCours) {
        if (!enCours) return@LaunchedEffect
        val codeLu = code.trim().ifEmpty { null }
        // La moyenne de 5 s est celle du contrôle : écrite une seule fois (`collecterPositionsArrivee`).
        val positions = collecterPositionsArrivee(position = { positionCourante.value }, surSeconde = { i -> confirmation = Confirmation.EnCours(i) })
        val m = (io.github.pobsteta.surculus.controle.ArriveeControle.arrivee(positions) as? Arrivee.Lambert93)?.position
        confirmation = if (m == null) null else Confirmation.Fini(Rattachement.rattacher(piquets, m, codeLu), codeLu)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 1. La seule chose à montrer, en grand : la distance (le titre « Vers <code> » est dans le bandeau).
        if (cap == null && fix != null) {
            // Un fix existe mais tombe hors de France : axes inversés, pas une absence de signal.
            Text(stringResource(R.string.carte_hors_france), color = couleurErreur(), fontWeight = FontWeight.Bold)
        } else if (cap == null) {
            Text(stringResource(R.string.guidage_sans_position), color = couleurErreur(), fontWeight = FontWeight.Bold)
        } else {
            Text(
                Libelles.distance(cap.distanceM),
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = couleurTexteEtat(if (cap.arrive) Palette.Vert else Palette.Noir),
            )
            // 2. Le cap, ou l'arrivée.
            if (cap.arrive) {
                TexteEtat(stringResource(R.string.guidage_arrive), Palette.Vert, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
            // Rouvrir le contrôle à la main (il s'est ouvert tout seul à l'arrivée, mission 012-B1).
            Button(onClick = onArrivee, modifier = Modifier.fillMaxWidth().height(64.dp)) { Text(stringResource(R.string.controle_controler)) }
            when (c) {
                is Confirmation.EnCours -> Text(stringResource(R.string.guidage_moyenne, c.n, SECONDES_DE_MOYENNE_ARRIVEE), style = MaterialTheme.typography.bodyMedium)
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
                TexteEtat(stringResource(R.string.guidage_rattache_identite, r.piquet.code, r.distanceM.roundToInt()), Palette.Orange, fontWeight = FontWeight.Bold)
            } else {
                TexteEtat(stringResource(R.string.guidage_rattache, r.piquet.code, r.distanceM.roundToInt()), Palette.Vert, fontWeight = FontWeight.Bold)
            }
        }
        is Verdict.Refuse -> {
            val refus = v.refus
            val texte = when (refus) {
                is RefusPlacette.HorsQuinzeMetres -> stringResource(R.string.guidage_refus_15m, refus.distanceM.roundToInt())
                is RefusPlacette.CodeInconnu -> stringResource(R.string.guidage_refus_code, refus.code)
                else -> refus.message
            }
            Text(texte, color = couleurErreur(), fontWeight = FontWeight.Bold)
        }
        is Verdict.AConfirmer -> TexteEtat(v.alerte.message, Palette.Orange)
    }
}

/** Radar nord en haut : la placette au centre, le cercle de 30 m, et l'agent là où il est. L'échelle suit la distance. */
@Composable
private fun Radar(agent: PointLambert93?, placette: PointLambert93, modifier: Modifier) {
    // Mission 063 : en sombre, cercle et ligne en onSurface, centre et point d'arrivée cernés d'un filet blanc (pas de plaque).
    val sombre = LocalSombre.current
    val neutre = couleurNeutre()
    val filet = CouleursEtat.filet
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
        val couleur = if (sombre) neutre else if (arrive) Palette.Vert else Palette.Noir
        drawCircle(couleur, radius = (Guidage.SEUIL_ARRIVEE_M * pxParMetre).toFloat(), center = centre, style = Stroke(width = 4.dp.toPx()))
        if (sombre) drawCircle(filet, radius = 8.dp.toPx() + 2.dp.toPx(), center = centre)
        drawCircle(Palette.Rouge, radius = 8.dp.toPx(), center = centre)
        if (agent != null) {
            val p = Offset(centre.x + (dx * pxParMetre).toFloat(), centre.y - (dy * pxParMetre).toFloat())
            drawLine(couleur, p, centre, strokeWidth = 3.dp.toPx())
            if (sombre) drawCircle(filet, radius = 10.dp.toPx() + 2.dp.toPx(), center = p)
            drawCircle(if (sombre && arrive) Palette.Vert else couleur, radius = 10.dp.toPx(), center = p)
        }
    }
}
