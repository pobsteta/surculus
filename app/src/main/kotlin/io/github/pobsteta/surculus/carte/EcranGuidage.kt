/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.carte

import android.media.AudioManager
import android.os.SystemClock
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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pobsteta.surculus.R
import io.github.pobsteta.surculus.messages.JournalTechnique
import io.github.pobsteta.surculus.controle.AttenteSansFix
import io.github.pobsteta.surculus.controle.ConfirmationSansPosition
import io.github.pobsteta.surculus.controle.ControleursOuverts
import io.github.pobsteta.surculus.controle.EtatArrivee
import io.github.pobsteta.surculus.controle.OuvertureSansPosition
import io.github.pobsteta.surculus.controle.VerrouOuverture
import io.github.pobsteta.surculus.controle.SansPositionEnAttente
import io.github.pobsteta.surculus.controle.SuiteSansFix
import io.github.pobsteta.surculus.controle.TextesControle
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
import kotlinx.coroutines.launch

/**
 * Guidage vers une placette (spec § 4.2 et 4.3). Une question : à quelle distance, dans quelle direction ?
 * En grand : la distance. Dessous : le cap, et le radar (nord en haut, cercle de 30 m). Bip qui s'accélère à
 * l'approche, un bip d'arrivée à moins de 30 m. Dans le cercle : rattachement à 15 m, ou par le code du piquet.
 * Rien n'est écrit au journal à ce stade (mission 031 pas fusionnée).
 *
 * Sans aucun fix (mission 089, décisions de Pascal du 2026-11-07) : à la place du radar, l'attente de la 087, le champ du
 * code dès l'absence de fix et « Ouvrir sans position » après le délai, par le morceau partagé avec l'écran de contrôle
 * ([OuvertureSansPosition], [AttenteSansFix], [ConfirmationSansPosition]).
 */
@Composable
fun EcranGuidage(
    id: String,
    placetteUuid: String,
    arriveeVue: Boolean = false,
    /** Arrivée (moins de 30 m) ou bouton « Contrôler » : ouvre le contrôle (mission 012-B1). */
    onArrivee: (String?) -> Unit = {},
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
                        // Deux causes, deux phrases : placette absente de la tournée, ou présente mais sans position (mission 103).
                        val dansLaTournee = chargement.tournee.tournee.placettes.any { it.uuid == placetteUuid }
                        LaunchedEffect(placetteUuid, dansLaTournee) {
                            JournalTechnique.diagnostic("Guidage", "placette $placetteUuid ${if (dansLaTournee) "sans position" else "absente"} dans la tournée $id")
                        }
                        val raison = if (dansLaTournee) R.string.carte_placette_sans_position_refus else R.string.carte_placette_introuvable
                        Column(Modifier.padding(24.dp)) { Text(stringResource(R.string.carte_refus, stringResource(raison)), color = couleurErreur()) }
                    } else {
                        val ctx = LocalContext.current
                        val ouverte = chargement.tournee
                        GuidagePret(
                            cible, chargement.tournee.tournee.placettes, fix, arriveeVue, onArrivee, onRearme,
                            cle = ControleursOuverts.cle(id, placetteUuid),
                            ouvrirSansPosition = { code, ug, confirmee -> OuvertureSansPosition.ouvrir(ctx, ouverte, code, ug, confirmee) },
                        )
                    }
            }
        }
    }
}

private sealed interface Confirmation {
    data class EnCours(val n: Int) : Confirmation
    data class Fini(val verdict: Verdict<Rattache>, val codeSaisi: String?) : Confirmation
}

/** L'horloge monotone de l'attente sans fix, relue chaque seconde (`uneSecondeArrivee`, la seconde de la 087). */
@Composable
private fun rememberMaintenant(): State<Long> {
    val maintenant = remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) {
        while (true) {
            uneSecondeArrivee()
            maintenant.longValue = SystemClock.elapsedRealtime()
        }
    }
    return maintenant
}

/**
 * Le guidage, tournée chargée. [cle] : la placette au sens de `ControleursOuverts` (contrôle ouvert sans position, ou UG à
 * choisir, repris par l'écran de contrôle). [ouvrirSansPosition], [suiteSansFix] et [maintenant] : la règle de la 087 par
 * l'habillage partagé, et l'horloge monotone ; injectés par le test d'écran.
 */
@Composable
internal fun GuidagePret(
    cible: PlacetteTournee,
    toutes: List<PlacetteTournee>,
    fix: FixGnss?,
    arriveeVue: Boolean,
    onArrivee: (String?) -> Unit,
    onRearme: () -> Unit,
    cle: String,
    ouvrirSansPosition: suspend (code: String, ugChoisie: String?, confirmee: Boolean) -> EtatArrivee,
    suiteSansFix: (msSansFix: Long, fixRecu: Boolean) -> SuiteSansFix = LocalContext.current.let { ctx -> { ms, recu -> OuvertureSansPosition.suite(ctx, ms, recu) } },
    maintenant: State<Long> = rememberMaintenant(),
) {
    val cap = fix?.let { Guidage.cap(it.position, cible.position!!) }
    val position = fix?.let { Guidage.versLambert93(it.position) }
    var son by remember { mutableStateOf(true) }
    var code by rememberSaveable { mutableStateOf("") }
    var confirmation by remember { mutableStateOf<Confirmation?>(null) }

    // L'attente sans fix (mission 089, décision 3 de Pascal) : depuis l'ouverture du guidage, remise à zéro par chaque fix
    // reçu (même hors de France : c'est une position, point 6), gardée à la rotation (horloge monotone).
    var debutSansFix by rememberSaveable { mutableStateOf<Long?>(null) }
    if (fix != null) debutSansFix = null else if (debutSansFix == null) debutSansFix = maintenant.value
    // L'étape de « Ouvrir sans position » montrée ici : un refus (code inconnu) ou la placette à confirmer.
    var etapeSansPosition by remember { mutableStateOf<EtatArrivee?>(null) }
    val scope = rememberCoroutineScope()
    // Mission 100 : un seul appui à la fois sur « Ouvrir sans position » (verrou partagé avec l'écran de contrôle).
    val verrou = remember { VerrouOuverture() }
    // Un fix reçu pendant l'étape : on l'abandonne, la distance et la règle des 15 m reviennent (point 6).
    LaunchedEffect(fix != null) { if (fix != null) etapeSansPosition = null }
    val codeOuvert = rememberUpdatedState(code)
    val arrivee = rememberUpdatedState(onArrivee)
    fun suiteOuverture(v: EtatArrivee) {
        when (v) {
            // Le contrôle ouvert sans position est repris par l'écran de contrôle, qui le libère en le quittant.
            is EtatArrivee.Ouverte -> {
                etapeSansPosition = null
                ControleursOuverts.poser(cle, v.controleur)
                arrivee.value(codeOuvert.value)
            }
            // Le code se répète dans plusieurs UG : l'écran de contrôle de la 087 pose la question, puis la confirmation.
            is EtatArrivee.UgAChoisir -> {
                etapeSansPosition = null
                SansPositionEnAttente.de(cle).let { it.etape = v; it.ugChoisie = null }
                arrivee.value(codeOuvert.value)
            }
            is EtatArrivee.ConfirmerSansPosition, is EtatArrivee.Refus -> etapeSansPosition = v
            else -> etapeSansPosition = null
        }
    }

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
    val codeRef = rememberUpdatedState(code)
    LaunchedEffect(cap?.distanceM) {
        val d = cap?.distanceM ?: return@LaunchedEffect
        if (vue.value) {
            if (d >= Guidage.SEUIL_REARMEMENT_M) onRearme()
        } else if (Guidage.estArrive(d)) {
            if (sonActif.value) tonalite?.startTone(ToneGenerator.TONE_PROP_BEEP2, 500)
            onArrivee(codeRef.value)
        }
    }

    // Rattachement : moyenne de la position pendant quelques secondes, puis la règle des 15 m (ou le code lu).
    val piquets = remember(toutes) {
        toutes.mapNotNull { p -> if (p.code != null && p.position != null) Piquet(UuidPlacette(p.uuid), p.code!!, p.position!!, p.ug) else null }
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
        if (fix == null) {
            // 3 bis. Aucun fix (mission 089, décisions 1 et 2 de Pascal) : à la place du radar, qui n'aurait rien à montrer,
            // l'attente de la 087 et le champ du code, puis « Ouvrir sans position » après le délai.
            val ms = maintenant.value - (debutSansFix ?: maintenant.value)
            val suite = suiteSansFix(ms, false)
            when (val etape = etapeSansPosition) {
                is EtatArrivee.ConfirmerSansPosition -> ConfirmationSansPosition(
                    etape,
                    onOuvrir = { verrou.lancer(scope) { suiteOuverture(ouvrirSansPosition(code, null, true)) } },
                    onAnnuler = { etapeSansPosition = null },
                )
                else -> {
                    AttenteSansFix(suite, (ms / 1000).toInt(), code, onCode = { code = it; etapeSansPosition = null }, onOuvrir = {
                        verrou.lancer(scope) { suiteOuverture(ouvrirSansPosition(code, null, false)) }
                    })
                    if (etape is EtatArrivee.Refus && suite == SuiteSansFix.PROPOSER) {
                        TextesControle.message(etape.refus)?.let { Text(it.texte, color = couleurErreur(), fontWeight = FontWeight.Bold) }
                    }
                }
            }
        } else {
            // 3. La comparaison : où je suis par rapport à la placette et au cercle de 30 m.
            Radar(position, cible.position!!, Modifier.fillMaxWidth().height(260.dp))
            Text(stringResource(R.string.guidage_nord), style = MaterialTheme.typography.bodySmall)
        }

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
            Button(onClick = { onArrivee(code) }, modifier = Modifier.fillMaxWidth().height(64.dp)) { Text(stringResource(R.string.controle_controler)) }
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
                TexteEtat(stringResource(R.string.guidage_rattache_identite, r.piquet.code, Libelles.distance(checkNotNull(r.distanceM) { "guidage : rattachement toujours mesuré" })), Palette.Orange, fontWeight = FontWeight.Bold)
            } else {
                TexteEtat(stringResource(R.string.guidage_rattache, r.piquet.code, Libelles.distance(checkNotNull(r.distanceM) { "guidage : rattachement toujours mesuré" })), Palette.Vert, fontWeight = FontWeight.Bold)
            }
        }
        is Verdict.Refuse -> {
            val refus = v.refus
            val texte = when (refus) {
                is RefusPlacette.HorsQuinzeMetres -> stringResource(R.string.guidage_refus_15m, Libelles.distance(refus.distanceM))
                is RefusPlacette.CodeInconnu -> stringResource(R.string.guidage_refus_code, refus.code)
                else -> refus.message
            }
            Text(texte, color = couleurErreur(), fontWeight = FontWeight.Bold)
        }
        is Verdict.AConfirmer -> TexteEtat(v.alerte.message, Palette.Orange)
        is Verdict.AChoisirUg -> TexteEtat(
            stringResource(R.string.guidage_ug_a_choisir, v.candidats.first().code, v.candidats.mapNotNull { it.ug }.joinToString(", ")),
            Palette.Orange, fontWeight = FontWeight.Bold,
        )
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
