/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.tournee

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pobsteta.surculus.R
import io.github.pobsteta.surculus.carte.Palette
import io.github.pobsteta.surculus.carte.ReglagesOrtho
import io.github.pobsteta.surculus.core.carte.EtatOrtho
import io.github.pobsteta.surculus.core.carte.ZoneTuiles
import io.github.pobsteta.surculus.gpkg.GpkgRefuse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Liste des tournées (spec § 4.1) : pour chacune, la forêt en grand, puis qui l'a préparée et quand, le nombre de
 * contrôles, et l'état de l'ortho (ADR 0009). Un appui ouvre la carte. Source : le `travaux.gpkg` de chaque tournée.
 *
 * Accueil de l'application (mission 040) : barre « Surculus » et menu à trois points (Essai GNSS, Restaurer une tournée).
 */
/** Message de l'écran ; [refus] (et non un mot du texte) décide de la couleur. */
private data class MessageTournees(val texte: String, val refus: Boolean)

@Composable
fun EcranTournees(onOuvrir: (String) -> Unit, onEssai: () -> Unit, onRestauration: () -> Unit) {
    val ctx = LocalContext.current
    val portee = rememberCoroutineScope()
    var lignes by remember { mutableStateOf<List<LigneTournee>?>(null) }
    var version by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<MessageTournees?>(null) }
    var recherche by rememberSaveable { mutableStateOf("") }
    var voirRendues by rememberSaveable { mutableStateOf(false) }
    var occupe by remember { mutableStateOf(false) }
    var wifi by remember { mutableStateOf(ReglagesOrtho.wifiSeulement(ctx)) }
    val ressources = LocalResources.current

    // Recharge la liste ; tant qu'une ortho n'est pas complète, on recompte toutes les 5 s (le préchargement avance).
    LaunchedEffect(version) {
        val l = withContext(Dispatchers.IO) { DepotTournees.lister(ctx) }
        lignes = l
        if (l.any { it.tournee != null && it.ortho.etat != EtatOrtho.Complete }) {
            delay(5000)
            version++
        }
    }

    fun importer(travail: suspend () -> java.io.File) {
        occupe = true
        message = MessageTournees(ressources.getString(R.string.tournees_import_en_cours), refus = false)
        portee.launch {
            message = try {
                val dossier = withContext(Dispatchers.IO) { travail() }
                MessageTournees(ressources.getString(R.string.tournees_import_ok, dossier.name), refus = false)
            } catch (e: GpkgRefuse) {
                MessageTournees(ressources.getString(R.string.tournees_import_refus, e.refus.message), refus = true)
            } catch (e: Exception) {
                MessageTournees(ressources.getString(R.string.tournees_import_refus, e.message ?: e.javaClass.simpleName), refus = true)
            }
            occupe = false
            version++
        }
    }

    val choisirZip = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) importer { DepotTournees.importerZip(ctx, uri) }
    }
    val choisirDossier = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) importer { DepotTournees.importerDossier(ctx, uri) }
    }

    Column(Modifier.fillMaxSize()) {
        BandeauAccueil(onEssai = onEssai, onRestauration = onRestauration)
        Column(Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { choisirDossier.launch(null) }, enabled = !occupe, modifier = Modifier.weight(1f).height(64.dp)) {
                    Text(stringResource(R.string.tournees_importer_dossier), fontSize = 18.sp)
                }
                Button(onClick = { choisirZip.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) }, enabled = !occupe, modifier = Modifier.weight(1f).height(64.dp)) {
                    Text(stringResource(R.string.tournees_importer_zip), fontSize = 18.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.tournees_wifi), fontSize = 18.sp)
                Switch(checked = wifi, onCheckedChange = { wifi = it; ReglagesOrtho.definirWifiSeulement(ctx, it) })
            }
            message?.let { Text(it.texte, fontSize = 16.sp, color = if (it.refus) Palette.Rouge else Palette.Noir) }

            val toutes = lignes.orEmpty()
            val trouvees = if (recherche.isBlank()) toutes else toutes.filter { correspond(it.champs, recherche) }
            val (rendues, ouvertes) = partager(trouvees) { it.rendue }
            val affichees = if (voirRendues) rendues else ouvertes
            val autres = if (voirRendues) ouvertes else rendues
            if (lignes != null && toutes.isEmpty()) Text(stringResource(R.string.tournees_vide), fontSize = 18.sp)
            if (toutes.isNotEmpty()) {
                // Zone de recherche et puce « Rendues (n) » sur une même ligne, comme Groma et Marculus (mission 041).
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = recherche,
                        onValueChange = { recherche = it },
                        label = { Text(stringResource(R.string.tournees_recherche)) },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 18.sp),
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = voirRendues,
                        onClick = { voirRendues = !voirRendues },
                        label = { Text(stringResource(R.string.tournees_rendues, toutes.count { it.rendue }), fontSize = 16.sp) },
                        leadingIcon = if (voirRendues) ({ Text("✓", fontSize = 16.sp, fontWeight = FontWeight.Bold) }) else null,
                    )
                }
                if (recherche.isNotBlank() && autres.isNotEmpty()) {
                    Text(
                        pluralStringResource(
                            if (voirRendues) R.plurals.tournees_autres_ouvertes else R.plurals.tournees_autres_rendues,
                            autres.size,
                            autres.size,
                        ),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (affichees.isEmpty()) {
                    Text(
                        stringResource(
                            when (messageVide(recherche, voirRendues)) {
                                MessageVide.AUCUNE_TROUVEE -> R.string.tournees_aucune_trouvee
                                MessageVide.AUCUNE_RENDUE -> R.string.tournees_aucune_rendue
                                MessageVide.AUCUNE_OUVERTE -> R.string.tournees_aucune_ouverte
                            },
                        ),
                        fontSize = 18.sp,
                    )
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(affichees, key = { it.id }) { ligne ->
                    LigneDeTournee(ligne, onOuvrir = { onOuvrir(ligne.id) }, onOrtho = {
                        // La lecture du gpkg (emprise) se fait hors du fil principal.
                        portee.launch {
                            val refus = withContext(Dispatchers.IO) { DepotTournees.planifierOrtho(ctx, ligne.dossier, remplacer = true) }
                            message = when (refus) {
                                is ZoneTuiles.Resultat.HorsFrance -> MessageTournees(ressources.getString(R.string.ortho_refus_hors_france), refus = true)
                                is ZoneTuiles.Resultat.TropGrande -> MessageTournees(ressources.getString(R.string.ortho_refus_trop_grande, refus.nombre.toInt()), refus = true)
                                else -> MessageTournees(ressources.getString(R.string.ortho_planifiee), refus = false)
                            }
                            version++
                        }
                    })
                    HorizontalDivider()
                }
            }
        }
    }
}

/** Barre de l'accueil : titre « Surculus » et menu à trois points (cible de 48 dp, comme le IconButton de Material 3). */
@Composable
private fun BandeauAccueil(onEssai: () -> Unit, onRestauration: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val menuDesc = stringResource(R.string.tournees_menu)
    Surface(color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
        Row(
            Modifier.fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))
                .height(56.dp).padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Box {
                IconButton(onClick = { menu = true }) {
                    // « ⋮ » plutôt que material-icons-core : pas de dépendance de plus (mission 040).
                    Text("⋮", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { contentDescription = menuDesc })
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.essai_ouvrir), fontSize = 18.sp) },
                        onClick = { menu = false; onEssai() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.restauration_ouvrir), fontSize = 18.sp) },
                        onClick = { menu = false; onRestauration() },
                    )
                }
            }
        }
    }
}

@Composable
private fun LigneDeTournee(ligne: LigneTournee, onOuvrir: () -> Unit, onOrtho: () -> Unit) {
    val t = ligne.tournee
    Column(
        Modifier.fillMaxWidth().clickable(enabled = t != null, onClick = onOuvrir).padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // 1. La forêt, en grand.
        Text(ligne.foret ?: ligne.id, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        if (t == null) {
            Text(stringResource(R.string.tournees_illisible, ligne.refus.orEmpty()), fontSize = 16.sp, color = Palette.Rouge)
            return@Column
        }
        val inconnu = stringResource(R.string.tournees_inconnu)
        // 2. Qui, quand.
        Text(stringResource(R.string.tournees_prepare, t.projet?.prepareLe ?: inconnu, t.projet?.operateur ?: inconnu), fontSize = 16.sp)
        // 3. Contrôles et état (la tournée est « ouverte » : l'état « rendue » vient avec l'export, mission 006).
        Text(stringResource(R.string.tournees_saisies, t.controles.size), fontSize = 16.sp)
        // 4. L'ortho, en couleur.
        val o = ligne.ortho
        val (texte, couleur) = when (val e = o.etat) {
            EtatOrtho.Complete ->
                (if (o.fichierLocal != null) stringResource(R.string.ortho_complete_fichier, o.fichierLocal) else stringResource(R.string.ortho_complete)) to Palette.Vert
            is EtatOrtho.Partielle -> stringResource(R.string.ortho_partielle, e.presentes.toInt(), e.attendues.toInt()) to Palette.Orange
            EtatOrtho.Absente -> stringResource(R.string.ortho_absente) to Palette.Rouge
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(texte, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = couleur)
            if (o.etat != EtatOrtho.Complete) {
                OutlinedButton(onClick = onOrtho, modifier = Modifier.height(48.dp)) { Text(stringResource(R.string.ortho_relancer), fontSize = 14.sp) }
            }
        }
    }
}
