/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.controle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.pobsteta.surculus.R
import io.github.pobsteta.surculus.carte.Palette
import io.github.pobsteta.surculus.carte.couleurErreur
import io.github.pobsteta.surculus.carte.couleurNeutre
import io.github.pobsteta.surculus.carte.couleurTexteEtat
import io.github.pobsteta.surculus.core.placette.UuidPlant
import io.github.pobsteta.surculus.core.geo.UniteAzimut
import io.github.pobsteta.surculus.ui.theme.LocalSombre
import kotlin.math.roundToInt

/**
 * La vue Pointer (mission 012-B2) : une seule question, « où est ce plant ? ». En grand : le cap lu à la boussole du
 * téléphone (magnétique), vivant. Dessous : la distance tapée, et « Pointer le plant ». Une ligne d'aide, remplacée par
 * l'avertissement quand la boussole est peu fiable (on avertit, on ne bloque pas). Le clavier est le secours.
 */
@Composable
fun VuePointerEcran(
    e: EcranControle,
    tournee: String,
    reglages: ReglagesControle,
    agir: (ControleurPlacette.() -> EcranControle) -> Unit,
) {
    var clavier by rememberSaveable { mutableStateOf(false) }
    var distance by rememberSaveable { mutableStateOf("") }
    var capTape by rememberSaveable { mutableStateOf("") }
    var unite by remember(tournee) { mutableStateOf(reglages.uniteAzimut(tournee)) }
    val (boussole, lecture) = rememberBoussole(actif = true)
    // Sans capteur, le clavier n'est pas un choix : c'est la seule voie.
    val auClavier = clavier || boussole == null
    val l = lecture.value
    val p = e.pointage

    p.regarniDe?.let { Text(stringResource(R.string.controle_regarni_de, it), style = MaterialTheme.typography.titleMedium, color = couleurNeutre()) }
    if (p.regarniDe != null && p.essencesTournee.isNotEmpty()) {
        // Une seule touche change l'essence ; l'essence du plant remplacé est déjà choisie. Pas de clavier.
        Text(stringResource(R.string.controle_regarni_essence), style = MaterialTheme.typography.titleSmall, color = couleurNeutre())
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            p.essencesTournee.forEach { code ->
                val m = Modifier.widthIn(min = DimensionsControle.ACTION).height(DimensionsControle.ACTION)
                val action = { agir { choisirEssence(code) } }
                if (code == p.essenceChoisie) FilledTonalButton(onClick = action, modifier = m) { Text(code, color = couleurNeutre(), style = MaterialTheme.typography.titleMedium) }
                else OutlinedButton(onClick = action, modifier = m) { Text(code, color = couleurNeutre(), style = MaterialTheme.typography.titleMedium) }
            }
        }
    }

    if (auClavier) {
        OutlinedTextField(
            value = capTape,
            onValueChange = { capTape = it },
            label = { Text(stringResource(if (unite == UniteAzimut.GRADES) R.string.controle_cap_clavier_gr else R.string.controle_cap_clavier_deg)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UniteAzimut.entries.forEach { u ->
                val m = Modifier.weight(1f).height(DimensionsControle.CIBLE_MIN)
                val action = { unite = u; reglages.choisirUniteAzimut(tournee, u) }
                val libelle = stringResource(if (u == UniteAzimut.GRADES) R.string.controle_unite_gr else R.string.controle_unite_deg)
                if (u == unite) FilledTonalButton(onClick = action, modifier = m) { Text(libelle, color = couleurNeutre()) }
                else OutlinedButton(onClick = action, modifier = m) { Text(libelle, color = couleurNeutre()) }
            }
        }
        if (boussole == null) Text(stringResource(R.string.controle_boussole_absente), style = MaterialTheme.typography.bodyMedium, color = couleurErreur())
    } else {
        Text(stringResource(R.string.controle_cap_magnetique), style = MaterialTheme.typography.titleMedium, color = couleurNeutre())
        // En grand : le cap ; en erreur quand il est douteux (on avertit, « Pointer » reste actif).
        val couleurCap = if (l.classe == ClasseBoussole.BONNE) couleurNeutre() else couleurErreur()
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                l.cap?.let { stringResource(R.string.controle_cap_valeur, it.degres.roundToInt() % 360) } ?: stringResource(R.string.controle_cap_absent),
                style = MaterialTheme.typography.displayMedium,
                color = couleurCap,
            )
            l.incertitudeDeg?.let { Text(stringResource(R.string.controle_cap_incertitude, it.roundToInt()), style = MaterialTheme.typography.titleLarge, color = couleurCap) }
        }
        if (l.classe != ClasseBoussole.BONNE) Text(stringResource(R.string.controle_cap_douteux), style = MaterialTheme.typography.titleSmall, color = couleurErreur())
    }

    OutlinedTextField(
        value = distance,
        onValueChange = { distance = it },
        label = { Text(stringResource(R.string.controle_distance)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )

    if (p.aConfirmer != null) {
        // Hors rayon : ce n'est pas un refus, c'est une question. Rien n'est écrit avant la réponse.
        Text(p.aConfirmer, style = MaterialTheme.typography.titleMedium, color = couleurTexteEtat(Palette.Orange))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { agir { confirmerHorsRayon() } }, modifier = Modifier.weight(1f).height(DimensionsControle.ACTION)) {
                Text(stringResource(R.string.controle_confirmer))
            }
            OutlinedButton(onClick = { agir { refaire() } }, modifier = Modifier.weight(1f).height(DimensionsControle.ACTION)) {
                Text(stringResource(R.string.controle_refaire), color = couleurNeutre())
            }
        }
    } else {
        Button(
            onClick = {
                if (auClavier) agir { pointerAuClavier(distance, capTape, unite) }
                else {
                    // Le cap est pris à l'appui : moyenne de la dernière seconde.
                    val cap = boussole?.capDeLaDerniereSeconde()
                    agir { pointerAvecBoussole(distance, cap) }
                }
            },
            modifier = Modifier.fillMaxWidth().height(DimensionsControle.BOUTON_ETAT),
        ) {
            Text(stringResource(if (p.regarniDe != null) R.string.controle_pointer_regarni else R.string.controle_pointer_plant), style = MaterialTheme.typography.titleLarge)
        }
    }

    // Une ligne d'aide, remplacée par l'avertissement quand il le faut.
    when {
        !auClavier && l.classe != ClasseBoussole.BONNE -> Text(stringResource(R.string.controle_boussole_faible), style = MaterialTheme.typography.bodyMedium, color = couleurErreur())
        !auClavier && l.champPerturbe -> Text(stringResource(R.string.controle_champ_perturbe), style = MaterialTheme.typography.bodyMedium, color = couleurErreur())
        !auClavier -> Text(stringResource(R.string.controle_aide_pointer), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (boussole != null) {
        TextButton(onClick = { clavier = !clavier }, modifier = Modifier.heightIn(min = DimensionsControle.CIBLE_MIN)) {
            Text(stringResource(if (auClavier) R.string.controle_lire_boussole else R.string.controle_saisir_clavier))
        }
    }
    OutlinedButton(onClick = { agir { reprendre() } }, modifier = Modifier.fillMaxWidth().height(DimensionsControle.ACTION)) {
        Text(stringResource(R.string.controle_retour_plant), color = couleurNeutre())
    }
}

/**
 * Le plan des plants autour du piquet, à l'échelle, nord en haut : le cercle de la placette, le piquet au centre, chaque
 * plant par la forme et la couleur de son état (pas encore dit : petit disque neutre), le plant courant cerné. Toucher un
 * plant le choisit. Les plants d'un passage précédent ne sont pas dessinés : la mention en petit le dit.
 */
@Composable
fun PlanCanvas(plan: PlanPlants, onChoisir: (UuidPlant) -> Unit, modifier: Modifier = Modifier) {
    val sombre = LocalSombre.current
    val neutre = couleurNeutre()
    val mesure = rememberTextMeasurer()
    val styleNumero = MaterialTheme.typography.labelSmall.copy(color = neutre)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(
            Modifier.widthIn(max = COTE_PLAN_MAX_DP.dp).fillMaxWidth().aspectRatio(1f).align(Alignment.CenterHorizontally)
                .pointerInput(plan) {
                    detectTapGestures { o ->
                        plan.plantTouche(o.x, o.y, size.width.toFloat(), tolerancePx = DimensionsControle.CIBLE_MIN.toPx() / 2f)?.let { onChoisir(it.uuid) }
                    }
                },
        ) {
            val cote = size.minDimension
            val c = Offset(cote / 2f, cote / 2f)
            drawCircle(neutre, radius = plan.rayonM.toFloat() * plan.echelle(cote), center = c, style = Stroke(width = 2.dp.toPx()))
            // Le piquet : une croix.
            val q = 6.dp.toPx()
            drawLine(neutre, Offset(c.x - q, c.y), Offset(c.x + q, c.y), strokeWidth = 2.dp.toPx())
            drawLine(neutre, Offset(c.x, c.y - q), Offset(c.x, c.y + q), strokeWidth = 2.dp.toPx())
            val pastille = 22.dp.toPx()
            plan.plants.forEach { plant ->
                val px = plan.versPixels(plant, cote)
                val centre = Offset(px.x, px.y)
                if (plant.etat != null) dessinerEtat(plant.etat, centre, pastille, sombre)
                else drawCircle(neutre, radius = pastille / 5f, center = centre)
                if (plant.courant) drawCircle(neutre, radius = pastille * 0.8f, center = centre, style = Stroke(width = 3.dp.toPx()))
                drawText(mesure, plant.numero.toString(), Offset(centre.x + pastille * 0.5f, centre.y - pastille * 0.9f), style = styleNumero)
            }
        }
        Text(stringResource(R.string.controle_plan), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (plan.nonDessines > 0) {
            Text(stringResource(R.string.controle_plants_anciens, plan.nonDessines), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
