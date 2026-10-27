/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.tournee

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.pobsteta.surculus.R
import io.github.pobsteta.surculus.carte.Chargement
import io.github.pobsteta.surculus.carte.Palette
import io.github.pobsteta.surculus.carte.chargerTournee
import io.github.pobsteta.surculus.carte.plaqueEtat
import io.github.pobsteta.surculus.gpkg.EtatSuivi
import io.github.pobsteta.surculus.gpkg.FormePastille
import io.github.pobsteta.surculus.gpkg.Pastille
import io.github.pobsteta.surculus.gpkg.PlacetteTournee
import io.github.pobsteta.surculus.gpkg.ordonnerPlacettes
import io.github.pobsteta.surculus.gpkg.titreForet
import io.github.pobsteta.surculus.ui.BandeauCompact

/**
 * Écran « Placettes » (mission 061), comme celui de Groma (`EcranPlacettes.kt` l. 60-133) : bandeau avec flèche de retour,
 * la forêt en titre et l'icône carte ; sous le bandeau « n placettes · k à faire » ; les placettes à faire d'abord, puis les
 * autres grisées. Un appui sur une placette ouvre la carte centrée dessus ([onPlacette]) ; l'icône carte l'ouvre cadrée sur
 * toute la tournée ([onCarte]).
 */
@Composable
fun EcranPlacettes(id: String, onRetour: () -> Unit, onCarte: () -> Unit, onPlacette: (String) -> Unit) {
    val ctx = LocalContext.current
    when (val c = chargerTournee(id)) {
        Chargement.EnCours -> Column {
            BandeauPlacettes(stringResource(R.string.placettes_titre), onRetour, onCarte = null)
            Corps { Text(stringResource(R.string.carte_ouverture)) }
        }
        is Chargement.Refus -> Column {
            BandeauPlacettes(stringResource(R.string.placettes_titre), onRetour, onCarte = null)
            Corps { Text(stringResource(R.string.carte_refus, c.message), color = Palette.Rouge) }
        }
        is Chargement.Pret -> {
            val t = c.tournee.tournee
            val titre = titreForet(t.projet?.foret, t.nomForet, c.tournee.dossier.name)
            val controlees = remember(id) { DepotTournees.placettesControlees(ctx, id) }
            val ordre = remember(t, controlees) { ordonnerPlacettes(t.placettes, controlees) }
            Column(Modifier.fillMaxSize()) {
                BandeauPlacettes(titre, onRetour, onCarte)
                Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))) {
                    Text(
                        pluralStringResource(R.plurals.placettes_compte, ordre.nombre, ordre.nombre, ordre.aFaire.size),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).plaqueEtat(),
                    )
                    if (ordre.nombre == 0) {
                        Text(stringResource(R.string.placettes_vide), modifier = Modifier.padding(16.dp))
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(ordre.aFaire, key = { it.uuid }) { p -> LignePlacette(p, aFaire = true) { onPlacette(p.uuid) } }
                        items(ordre.autres, key = { it.uuid }) { p -> LignePlacette(p, aFaire = false) { onPlacette(p.uuid) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun Corps(contenu: @Composable () -> Unit) {
    Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)).padding(24.dp)) { contenu() }
}

/** Bandeau compact : flèche de retour à gauche, icône carte à droite (cibles de 48 dp). */
@Composable
private fun BandeauPlacettes(titre: String, onRetour: () -> Unit, onCarte: (() -> Unit)?) {
    BandeauCompact(
        titre = titre,
        navigationIcon = {
            IconButton(onClick = onRetour) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.carte_retour))
            }
        },
        actions = {
            if (onCarte != null) IconButton(onClick = onCarte) {
                Icon(Icons.Filled.Place, contentDescription = stringResource(R.string.placettes_carte))
            }
        },
    )
}

/** Une placette : pastille d'état comme sur la carte (mission 033), code, état dit en toutes lettres ; grisée si elle n'est plus à faire. */
@Composable
private fun LignePlacette(p: PlacetteTournee, aFaire: Boolean, onClick: () -> Unit) {
    val couleurTexte = if (aFaire) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    ElevatedCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PastilleEtat(p.etatSuivi, grisee = !aFaire)
            Column(Modifier.weight(1f)) {
                Text(p.code ?: stringResource(R.string.placettes_sans_code), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = couleurTexte)
                Text(
                    stringResource(if (aFaire) R.string.placettes_a_faire else R.string.placettes_controlee) + " · " + stringResource(libelleEtat(p.etatSuivi)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = couleurTexte,
                )
            }
        }
    }
}

private fun libelleEtat(e: EtatSuivi): Int = when (e) {
    EtatSuivi.JAMAIS -> R.string.placettes_etat_jamais
    EtatSuivi.A_PROGRAMMER -> R.string.placettes_etat_a_programmer
    EtatSuivi.A_JOUR -> R.string.placettes_etat_a_jour
    EtatSuivi.INCONNU -> R.string.placettes_etat_inconnu
}

/** Même pastille que sur la carte (forme et teinte de [Pastille.de]) : disque plein, anneau, ou disque noir marqué d'un « ? ». */
@Composable
private fun PastilleEtat(etat: EtatSuivi, grisee: Boolean) {
    val p = Pastille.de(etat)
    val teinte = Palette.de(p.teinte)
    val couleur = if (grisee) teinte.copy(alpha = 0.55f) else teinte
    when (p.forme) {
        FormePastille.ANNEAU -> Box(Modifier.size(28.dp).border(BorderStroke(5.dp, couleur), CircleShape))
        FormePastille.DISQUE_PLEIN -> Box(Modifier.size(28.dp).background(couleur, CircleShape))
        FormePastille.DISQUE_AVEC_POINT_INTERROGATION -> Box(Modifier.size(28.dp).background(couleur, CircleShape), contentAlignment = Alignment.Center) {
            Text("?", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}
