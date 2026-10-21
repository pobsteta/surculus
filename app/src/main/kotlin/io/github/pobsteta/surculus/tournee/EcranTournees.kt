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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
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
 * La place du bouton « Restaurer une tournée » (mission 009) est tenue par l'accueil, qui garde ce bouton.
 */
/** Message de l'écran ; [refus] (et non un mot du texte) décide de la couleur. */
private data class MessageTournees(val texte: String, val refus: Boolean)

@Composable
fun EcranTournees(onOuvrir: (String) -> Unit, onRetour: () -> Unit) {
    val ctx = LocalContext.current
    val portee = rememberCoroutineScope()
    var lignes by remember { mutableStateOf<List<LigneTournee>?>(null) }
    var version by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<MessageTournees?>(null) }
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

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onRetour, modifier = Modifier.height(56.dp)) { Text(stringResource(R.string.tournees_retour), fontSize = 18.sp) }
            Text(stringResource(R.string.tournees_titre), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
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

        val toutes = lignes
        if (toutes != null && toutes.isEmpty()) Text(stringResource(R.string.tournees_vide), fontSize = 18.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(toutes.orEmpty(), key = { it.id }) { ligne ->
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
