/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.tournee

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.pobsteta.surculus.R
import io.github.pobsteta.surculus.carte.Palette
import kotlin.math.roundToInt

/** Une carte du Kanban : la tournée, la colonne où elle s'affiche et ce que l'avancement impose au moins. */
data class CarteKanban(val ligne: LigneTournee, val saisies: Int, val stocke: EtatKanban) {
    val minimal: EtatKanban get() = statutMinimal(saisies, ligne.rendue)
    val colonne: EtatKanban get() = colonneAffichee(stocke, minimal)
}

/**
 * Vue Kanban des tournées (mission 043), sur le modèle de Marculus : une colonne par état, appui long puis glissé pour
 * changer de colonne, appui court pour ouvrir la carte de la tournée. [onDeposer] reçoit le nouveau statut à garder ;
 * un dépôt sous le minimum imposé par l'avancement n'est pas transmis.
 */
@Composable
fun VueKanban(
    cartes: List<CarteKanban>,
    onOuvrir: (LigneTournee) -> Unit,
    onDeposer: (LigneTournee, EtatKanban) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tiree by remember { mutableStateOf<CarteKanban?>(null) }
    var doigt by remember { mutableStateOf(Offset.Zero) } // coordonnées de la racine
    var origine by remember { mutableStateOf(Offset.Zero) } // coin de cette vue, coordonnées de la racine
    val rectsColonnes = remember { mutableStateMapOf<EtatKanban, Rect>() }
    val colonnes = repartir(cartes) { it.colonne }

    Box(modifier.fillMaxSize().onGloballyPositioned { origine = it.boundsInRoot().topLeft }) {
        Row(
            Modifier.fillMaxSize().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EtatKanban.entries.forEach { etat ->
                val cartesDeLaColonne = colonnes.getValue(etat)
                Column(
                    Modifier.width(260.dp).fillMaxHeight().onGloballyPositioned { rectsColonnes[etat] = it.boundsInRoot() },
                ) {
                    Text(
                        "${etat.libelle} (${cartesDeLaColonne.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(8.dp),
                    )
                    LazyColumn(contentPadding = PaddingValues(bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(cartesDeLaColonne, key = { it.ligne.id }) { carte ->
                            CarteDeKanban(
                                carte = carte,
                                estTiree = tiree?.ligne?.id == carte.ligne.id,
                                onOuvrir = { onOuvrir(carte.ligne) },
                                onDebut = { p -> tiree = carte; doigt = p },
                                onGlisse = { p -> doigt = p },
                                onFin = {
                                    val cible = rectsColonnes.entries.firstOrNull { it.value.contains(doigt) }?.key
                                    cible?.let { deposer(carte.stocke, it, carte.minimal) }?.let { onDeposer(carte.ligne, it) }
                                    tiree = null
                                },
                                onAnnule = { tiree = null },
                            )
                        }
                    }
                }
            }
        }
        tiree?.let { c ->
            ElevatedCard(
                Modifier
                    .offset { IntOffset((doigt.x - origine.x - 110.dp.toPx()).roundToInt(), (doigt.y - origine.y - 24.dp.toPx()).roundToInt()) }
                    .width(220.dp),
            ) {
                Text(
                    c.ligne.foret ?: c.ligne.id,
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CarteDeKanban(
    carte: CarteKanban,
    estTiree: Boolean,
    onOuvrir: () -> Unit,
    onDebut: (Offset) -> Unit,
    onGlisse: (Offset) -> Unit,
    onFin: () -> Unit,
    onAnnule: () -> Unit,
) {
    var coin by remember { mutableStateOf(Offset.Zero) }
    val t = carte.ligne.tournee
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coin = it.boundsInRoot().topLeft }
            .graphicsLayer { alpha = if (estTiree) 0.3f else 1f }
            .pointerInput(carte.ligne.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset -> onDebut(coin + offset) },
                    onDrag = { change, _ -> change.consume(); onGlisse(coin + change.position) },
                    onDragEnd = { onFin() },
                    onDragCancel = { onAnnule() },
                )
            }
            .clickable(enabled = t != null, onClick = onOuvrir),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(carte.ligne.foret ?: carte.ligne.id, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (t == null) {
                Text(stringResource(R.string.tournees_illisible, carte.ligne.refus.orEmpty()), style = MaterialTheme.typography.bodyMedium, color = Palette.Rouge)
            } else {
                t.projet?.prepareLe?.let { Text(dateFrancaise(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text(
                    stringResource(R.string.kanban_controles, t.controles.size) + " · " +
                        pluralStringResource(R.plurals.kanban_saisies, carte.saisies, carte.saisies),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
