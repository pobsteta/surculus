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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.DialogProperties
import io.github.pobsteta.surculus.messages.JournalTechnique
import io.github.pobsteta.surculus.messages.TEXTE_TOURNEE_ILLISIBLE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import io.github.pobsteta.surculus.carte.PastilleEtat
import io.github.pobsteta.surculus.carte.couleurErreur
import io.github.pobsteta.surculus.carte.chargerTournee
import io.github.pobsteta.surculus.gpkg.EtatSuivi
import io.github.pobsteta.surculus.gpkg.FormePastille
import io.github.pobsteta.surculus.gpkg.Pastille
import io.github.pobsteta.surculus.gpkg.PlacetteTournee
import io.github.pobsteta.surculus.gpkg.StatutLigne
import io.github.pobsteta.surculus.gpkg.statutLigne
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
            Corps { Text(stringResource(R.string.carte_refus, c.message), color = couleurErreur()) }
        }
        is Chargement.Pret -> {
            val t = c.tournee.tournee
            val titre = titreForet(t.projet?.foret, t.nomForet, c.tournee.dossier.name)
            val controlees = remember(id) { DepotTournees.placettesControlees(ctx, id) }
            val ordre = remember(t, controlees) { ordonnerPlacettes(t.placettes, controlees) }
            Column(Modifier.fillMaxSize()) {
                BandeauPlacettes(titre, onRetour, onCarte)
                Column(Modifier.weight(1f).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))) {
                    Text(
                        pluralStringResource(R.plurals.placettes_compte, ordre.nombre, ordre.nombre, ordre.aFaire.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    if (ordre.nombre == 0) {
                        Text(stringResource(R.string.placettes_vide), modifier = Modifier.padding(16.dp))
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(ordre.aFaire, key = { it.uuid }) { p -> LignePlacette(p, statutLigne(p, controlees)) { onPlacette(p.uuid) } }
                        items(ordre.autres, key = { it.uuid }) { p -> LignePlacette(p, statutLigne(p, controlees)) { onPlacette(p.uuid) } }
                    }
                    // Mission 090 : « Rendre la tournée », en bas de l'écran des placettes (décision de Pascal du 2026-11-03).
                    BasRendu(id, onPlacetteCode = { code -> t.placettes.firstOrNull { it.code == code }?.let { onPlacette(it.uuid) } })
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
private fun LignePlacette(p: PlacetteTournee, statut: StatutLigne, onClick: () -> Unit) {
    val aFaire = statut == StatutLigne.A_FAIRE
    val couleurTexte = if (aFaire) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    ElevatedCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PastilleEtat(p.etatSuivi, 28.dp, grisee = !aFaire)
            Column(Modifier.weight(1f)) {
                Text(p.code ?: stringResource(R.string.placettes_sans_code), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = couleurTexte)
                Text(
                    when (statut) {
                        StatutLigne.A_FAIRE -> stringResource(R.string.placettes_a_faire) + " · " + stringResource(libelleEtat(p.etatSuivi))
                        StatutLigne.CONTROLEE -> stringResource(R.string.placettes_controlee) + " · " + stringResource(libelleEtat(p.etatSuivi))
                        StatutLigne.ETAT_SEUL -> stringResource(libelleEtat(p.etatSuivi)).replaceFirstChar { it.uppercase() }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = couleurTexte,
                )
            }
        }
    }
}

/** Ce que montre le bas de l'écran pendant un rendu (mission 090). */
private sealed interface EtapeEcranRendu {
    data object Repos : EtapeEcranRendu
    data object Decompte : EtapeEcranRendu
    data class Confirmer(val controles: Int, val plants: Int) : EtapeEcranRendu
    data class EnCours(val etape: EtapeRendu) : EtapeEcranRendu
    data class Fini(val issue: IssueRendu) : EtapeEcranRendu
    data object Rouvrir : EtapeEcranRendu
}

/**
 * Mission 090 (décisions de Pascal du 2026-11-03) : un grand bouton « Rendre la tournée » (56 dp, gants), la confirmation
 * avec le décompte, « Vérification… » puis « Écriture… », la fin (« Tournée rendue : 2 contrôles, 8 plants,
 * Documents/Surculus/….zip » et « Partager »), ou la liste des fautes dont chaque ligne ouvre sa placette. Tournée rendue :
 * lecture seule, « Rouvrir la tournée » avec son avertissement.
 */
@Composable
private fun BasRendu(id: String, onPlacetteCode: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var version by remember(id) { mutableIntStateOf(0) }
    val rendue by produceState<Boolean?>(null, id, version) { value = withContext(Dispatchers.IO) { RenduApp.estRendue(ctx, id) } }
    var etape by remember(id) { mutableStateOf<EtapeEcranRendu>(EtapeEcranRendu.Repos) }

    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        when (rendue) {
            null -> {}
            true -> {
                Text(stringResource(R.string.rendu_lecture_seule), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = { etape = EtapeEcranRendu.Rouvrir }, modifier = Modifier.fillMaxWidth().heightIn(min = HAUTEUR_BOUTON)) {
                    Text(stringResource(R.string.rendu_rouvrir), style = MaterialTheme.typography.titleMedium)
                }
            }
            false -> Button(
                onClick = {
                    etape = EtapeEcranRendu.Decompte
                    scope.launch {
                        etape = withContext(Dispatchers.IO) {
                            runCatching { RenduApp.decompter(ctx, id) }
                                .map { EtapeEcranRendu.Confirmer(it.controles, it.plants) as EtapeEcranRendu }
                                .getOrElse { e ->
                                    JournalTechnique.avertir("Rendu", "décompte impossible", e)
                                    EtapeEcranRendu.Fini(IssueRendu.Impossible(TEXTE_TOURNEE_ILLISIBLE))
                                }
                        }
                    }
                },
                enabled = etape == EtapeEcranRendu.Repos,
                modifier = Modifier.fillMaxWidth().heightIn(min = HAUTEUR_BOUTON),
            ) { Text(stringResource(R.string.rendu_bouton), style = MaterialTheme.typography.titleMedium) }
        }
    }

    when (val e = etape) {
        EtapeEcranRendu.Repos, EtapeEcranRendu.Decompte -> {}
        is EtapeEcranRendu.Confirmer -> AlertDialog(
            onDismissRequest = { etape = EtapeEcranRendu.Repos },
            title = { Text(stringResource(R.string.rendu_confirmer_titre)) },
            text = { Text(stringResource(R.string.rendu_confirmer_texte, compte(e.controles, e.plants))) },
            confirmButton = {
                Button(onClick = {
                    etape = EtapeEcranRendu.EnCours(EtapeRendu.VERIFICATION)
                    scope.launch {
                        val issue = withContext(Dispatchers.IO) {
                            RenduApp.rendre(ctx, id) { s -> scope.launch { if (etape is EtapeEcranRendu.EnCours) etape = EtapeEcranRendu.EnCours(s) } }
                        }
                        etape = EtapeEcranRendu.Fini(issue)
                        version++
                    }
                }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.rendu_confirmer)) }
            },
            dismissButton = {
                TextButton(onClick = { etape = EtapeEcranRendu.Repos }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.rendu_annuler)) }
            },
        )
        is EtapeEcranRendu.EnCours -> AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = { Text(stringResource(R.string.rendu_bouton)) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator(Modifier.size(32.dp))
                    Text(stringResource(if (e.etape == EtapeRendu.VERIFICATION) R.string.rendu_verification else R.string.rendu_ecriture))
                }
            },
            confirmButton = {},
        )
        is EtapeEcranRendu.Fini -> FinRendu(e.issue, onFermer = { etape = EtapeEcranRendu.Repos }, onPlacetteCode = { etape = EtapeEcranRendu.Repos; onPlacetteCode(it) })
        EtapeEcranRendu.Rouvrir -> AlertDialog(
            onDismissRequest = { etape = EtapeEcranRendu.Repos },
            title = { Text(stringResource(R.string.rendu_rouvrir_titre)) },
            text = { Text(stringResource(R.string.rendu_rouvrir_texte)) },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) { runCatching { RenduApp.rouvrir(ctx, id) }.onFailure { JournalTechnique.avertir("Rendu", "réouverture impossible", it) } }
                        etape = EtapeEcranRendu.Repos
                        version++
                    }
                }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.rendu_rouvrir_confirmer)) }
            },
            dismissButton = {
                TextButton(onClick = { etape = EtapeEcranRendu.Repos }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.rendu_annuler)) }
            },
        )
    }
}

@Composable
private fun compte(controles: Int, plants: Int): String =
    pluralStringResource(R.plurals.rendu_controles, controles, controles) + ", " + pluralStringResource(R.plurals.rendu_plants, plants, plants)

/** La fin d'un rendu : rendue (avec « Partager »), refusée (les fautes, chacune ouvre sa placette) ou impossible. */
@Composable
private fun FinRendu(issue: IssueRendu, onFermer: () -> Unit, onPlacetteCode: (String) -> Unit) {
    val ctx = LocalContext.current
    when (issue) {
        is IssueRendu.Rendue -> AlertDialog(
            onDismissRequest = onFermer,
            title = { Text(stringResource(R.string.rendu_bouton)) },
            text = { Text(stringResource(R.string.rendu_fait, compte(issue.controles, issue.plants), issue.chemin)) },
            confirmButton = {
                Button(onClick = { runCatching { RenduApp.partager(ctx, issue.prive) }.onFailure { JournalTechnique.avertir("Rendu", "partage impossible", it) } },
                    modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.rendu_partager)) }
            },
            dismissButton = { TextButton(onClick = onFermer, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.rendu_fermer)) } },
        )
        is IssueRendu.Refusee -> AlertDialog(
            onDismissRequest = onFermer,
            title = { Text(stringResource(R.string.rendu_fautes_titre)) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(issue.fautes) { f ->
                        val code = f.placette
                        Text(
                            f.texte,
                            color = couleurErreur(),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .then(if (code != null) Modifier.clickable { onPlacetteCode(code) } else Modifier)
                                .padding(vertical = 4.dp),
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = onFermer, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.rendu_fermer)) } },
        )
        is IssueRendu.Impossible -> AlertDialog(
            onDismissRequest = onFermer,
            title = { Text(stringResource(R.string.rendu_bouton)) },
            text = { Text(stringResource(R.string.rendu_impossible, issue.texte), color = couleurErreur()) },
            confirmButton = { TextButton(onClick = onFermer, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.rendu_fermer)) } },
        )
    }
}

/** Le bouton du rendu : 56 dp, au-dessus du minimum de 48 dp (gants ; décision de Pascal du 2026-11-03). */
private val HAUTEUR_BOUTON = 56.dp

private fun libelleEtat(e: EtatSuivi): Int = when (e) {
    EtatSuivi.JAMAIS -> R.string.placettes_etat_jamais
    EtatSuivi.A_PROGRAMMER -> R.string.placettes_etat_a_programmer
    EtatSuivi.A_JOUR -> R.string.placettes_etat_a_jour
    EtatSuivi.INCONNU -> R.string.placettes_etat_inconnu
}
