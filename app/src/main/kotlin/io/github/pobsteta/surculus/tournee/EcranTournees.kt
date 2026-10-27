/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.tournee

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import io.github.pobsteta.surculus.ENTREES_MENU
import io.github.pobsteta.surculus.EntreeMenu
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.pobsteta.surculus.R
import io.github.pobsteta.surculus.ui.BandeauCompact
import io.github.pobsteta.surculus.carte.couleurErreur
import io.github.pobsteta.surculus.carte.couleurNeutre
import io.github.pobsteta.surculus.gpkg.GpkgRefuse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Où les sélecteurs s'ouvrent d'abord : `Download/Surculus` (mission 054, comme Groma pour `Download/groma`). */
private val DOSSIER_PAR_DEFAUT: Uri =
    DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:Download/Surculus")

private val TYPES_ZIP = arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")

/** `OpenDocument` qui demande au sélecteur de s'ouvrir d'abord dans [debut] (indice : Android peut l'ignorer). */
private class OuvrirDocumentDans(private val debut: Uri) : ActivityResultContracts.OpenDocument() {
    override fun createIntent(context: android.content.Context, input: Array<String>): Intent =
        super.createIntent(context, input).putExtra(DocumentsContract.EXTRA_INITIAL_URI, debut)
}

/** Message de l'écran ; [refus] (et non un mot du texte) décide de la couleur. */
private data class MessageTournees(val texte: String, val refus: Boolean)

/**
 * Liste des tournées (spec § 4.1) : pour chacune, la forêt en grand, puis qui l'a préparée et quand, et le nombre de
 * contrôles. Un appui ouvre les placettes de la forêt (mission 061). Source : le `travaux.gpkg` de chaque tournée.
 *
 * Accueil de l'application (missions 040 et 054) : barre « Surculus » et menu à trois points (Ouvrir un dossier,
 * Ouvrir un .zip, Paramètres, À propos), disponible aussi en vue colonnes.
 */
@Composable
fun EcranTournees(
    onOuvrir: (String) -> Unit,
    onParametres: () -> Unit,
) {
    val ctx = LocalContext.current
    val portee = rememberCoroutineScope()
    var lignes by remember { mutableStateOf<List<LigneTournee>?>(null) }
    var version by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<MessageTournees?>(null) }
    var recherche by rememberSaveable { mutableStateOf("") }
    var voirRendues by rememberSaveable { mutableStateOf(false) }
    var modeColonnes by rememberSaveable { mutableStateOf(false) }
    var saisies by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var statuts by remember { mutableStateOf<Map<String, EtatKanban>>(emptyMap()) }
    var occupe by remember { mutableStateOf(false) }
    val ressources = LocalResources.current
    val reglagesKanban = remember { DepotTournees.reglagesKanban(ctx) }
    var aPropos by rememberSaveable { mutableStateOf(false) }
    if (aPropos) BoiteAPropos(onFermer = { aPropos = false })
    val colonnesAffichees = modeColonnes

    // Mission 058 : nettoyage unique des anciennes tuiles, en tâche de fond (n'attend pas la liste).
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { DepotTournees.nettoyerAnciennesTuiles(ctx) } }

    // Recharge la liste.
    LaunchedEffect(version) {
        val l = withContext(Dispatchers.IO) { DepotTournees.lister(ctx) }
        statuts = withContext(Dispatchers.IO) { l.associate { it.id to reglagesKanban.statut(it.id) } }
        saisies = withContext(Dispatchers.IO) { DepotTournees.saisies(ctx, l) }
        lignes = l
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

    val choisirZip = rememberLauncherForActivityResult(OuvrirDocumentDans(DOSSIER_PAR_DEFAUT)) { uri: Uri? ->
        if (uri != null) importer { DepotTournees.importerZip(ctx, uri) }
    }
    val choisirDossier = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) importer { DepotTournees.importerDossier(ctx, uri) }
    }

    Column(Modifier.fillMaxSize()) {
        BandeauAccueil(
            occupe = occupe,
            onOuvrirDossier = { choisirDossier.launch(DOSSIER_PAR_DEFAUT) },
            onOuvrirZip = { choisirZip.launch(TYPES_ZIP) },
            onParametres = onParametres,
            modeColonnes = modeColonnes,
            onAPropos = { aPropos = true },
            onBasculerVue = { modeColonnes = !modeColonnes },
        )
        Column(Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            message?.let { Text(it.texte, color = if (it.refus) couleurErreur() else couleurNeutre()) }

            val toutes = lignes.orEmpty()
            val trouvees = if (recherche.isBlank()) toutes else toutes.filter { correspond(it.champs, recherche) }
            val (rendues, ouvertes) = partager(trouvees) { it.rendue }
            val affichees = if (voirRendues) rendues else ouvertes
            val autres = if (voirRendues) ouvertes else rendues
            if (lignes != null && toutes.isEmpty()) Text(stringResource(R.string.tournees_vide), style = MaterialTheme.typography.bodyMedium)
            if (toutes.isNotEmpty()) {
                // Zone de recherche et puce « Rendues (n) » sur une même ligne, comme Groma et Marculus (mission 041).
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = recherche,
                        onValueChange = { recherche = it },
                        label = { Text(stringResource(R.string.tournees_recherche)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    // En colonnes, toutes les tournées sont réparties : pas de puce « Rendues », comme Marculus.
                    if (!colonnesAffichees) FilterChip(
                        selected = voirRendues,
                        onClick = { voirRendues = !voirRendues },
                        label = { Text(stringResource(R.string.tournees_rendues, toutes.count { it.rendue }), style = MaterialTheme.typography.labelLarge) },
                        leadingIcon = if (voirRendues) ({ Text("✓", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }) else null,
                    )
                }
                if (!colonnesAffichees && recherche.isNotBlank() && autres.isNotEmpty()) {
                    Text(
                        pluralStringResource(
                            if (voirRendues) R.plurals.tournees_autres_ouvertes else R.plurals.tournees_autres_rendues,
                            autres.size,
                            autres.size,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (colonnesAffichees && trouvees.isEmpty()) {
                    Text(stringResource(R.string.tournees_aucune_trouvee), style = MaterialTheme.typography.bodyMedium)
                } else if (!colonnesAffichees && affichees.isEmpty()) {
                    Text(
                        stringResource(
                            when (messageVide(recherche, voirRendues)) {
                                MessageVide.AUCUNE_TROUVEE -> R.string.tournees_aucune_trouvee
                                MessageVide.AUCUNE_RENDUE -> R.string.tournees_aucune_rendue
                                MessageVide.AUCUNE_OUVERTE -> R.string.tournees_aucune_ouverte
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (colonnesAffichees) {
                if (trouvees.isNotEmpty()) {
                    VueKanban(
                        cartes = trouvees.map { CarteKanban(it, saisies[it.id] ?: 0, statuts[it.id] ?: EtatKanban.PROPOSEE) },
                        onOuvrir = { onOuvrir(it.id) },
                        onDeposer = { l, nouveau ->
                            statuts = statuts + (l.id to nouveau)
                            portee.launch(Dispatchers.IO) { reglagesKanban.definirStatut(l.id, nouveau) }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            } else LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(affichees, key = { it.id }) { ligne ->
                    LigneDeTournee(ligne, onOuvrir = { onOuvrir(ligne.id) })
                    HorizontalDivider()
                }
            }
        }
    }
}

/** « À propos » (mission 044) : version installée lue sur le paquet, licences de `LICENSE` et `docs/dependances.md`. */
@Composable
private fun BoiteAPropos(onFermer: () -> Unit) {
    val ctx = LocalContext.current
    val versionNom = remember {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull()
    }
    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.apropos_titre)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(texteVersion(versionNom), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.apropos_desc), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.apropos_licences), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                TextButton(
                    onClick = {
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_LICENCES))) }
                        onFermer()
                    },
                    modifier = Modifier.height(48.dp),
                ) { Text(stringResource(R.string.apropos_toutes_licences)) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_GITHUB))) }
                    onFermer()
                },
                modifier = Modifier.height(48.dp),
            ) { Text(stringResource(R.string.apropos_github)) }
        },
        dismissButton = {
            TextButton(onClick = onFermer, modifier = Modifier.height(48.dp)) { Text(stringResource(R.string.apropos_fermer)) }
        },
    )
}

/** Barre de l'accueil : le bandeau commun (mission 053), titre « Surculus », bascule liste/colonnes et menu à trois points (cibles de 48 dp). */
@Composable
private fun BandeauAccueil(
    occupe: Boolean,
    onOuvrirDossier: () -> Unit,
    onOuvrirZip: () -> Unit,
    onParametres: () -> Unit,
    modeColonnes: Boolean,
    onAPropos: () -> Unit,
    onBasculerVue: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val menuDesc = stringResource(R.string.tournees_menu)
    BandeauCompact(titre = stringResource(R.string.app_name), actions = {
        // Toujours visible, sans réglage (mission 050). Mêmes icônes que Marculus (décision de Pascal, 2026-10-27) :
        // l'icône montre la vue où l'on va passer.
        IconButton(onClick = onBasculerVue) {
            Icon(
                if (modeColonnes) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.ViewColumn,
                contentDescription = stringResource(if (modeColonnes) R.string.tournees_vue_liste else R.string.tournees_vue_colonnes),
            )
        }
        Box {
            IconButton(onClick = { menu = true }) {
                // « ⋮ » en texte depuis la mission 040 ; material-icons-core est là depuis la 043 (icônes de la bascule), le menu n'a pas été changé.
                Text("⋮", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { contentDescription = menuDesc })
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                for (entree in ENTREES_MENU) {
                    val ouverture = entree == EntreeMenu.OUVRIR_DOSSIER || entree == EntreeMenu.OUVRIR_ZIP
                    DropdownMenuItem(
                        text = {
                            Text(stringResource(when (entree) {
                                EntreeMenu.OUVRIR_DOSSIER -> R.string.tournees_importer_dossier
                                EntreeMenu.OUVRIR_ZIP -> R.string.tournees_importer_zip
                                EntreeMenu.PARAMETRES -> R.string.tournees_menu_parametres
                                EntreeMenu.A_PROPOS -> R.string.tournees_menu_apropos
                            }))
                        },
                        // Pendant un import, les deux ouvertures sont grisées (comme les anciens boutons).
                        enabled = !(ouverture && occupe),
                        modifier = Modifier.heightIn(min = 48.dp),
                        onClick = {
                            menu = false
                            when (entree) {
                                EntreeMenu.OUVRIR_DOSSIER -> onOuvrirDossier()
                                EntreeMenu.OUVRIR_ZIP -> onOuvrirZip()
                                EntreeMenu.PARAMETRES -> onParametres()
                                EntreeMenu.A_PROPOS -> onAPropos()
                            }
                        },
                    )
                }
            }
        }
    })
}

@Composable
private fun LigneDeTournee(ligne: LigneTournee, onOuvrir: () -> Unit) {
    val t = ligne.tournee
    Column(
        Modifier.fillMaxWidth().clickable(enabled = t != null, onClick = onOuvrir).padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // 1. La forêt, en grand.
        Text(ligne.foret ?: ligne.id, style = MaterialTheme.typography.titleLarge)
        if (t == null) {
            Text(stringResource(R.string.tournees_illisible, ligne.refus.orEmpty()), style = MaterialTheme.typography.bodyMedium, color = couleurErreur())
            return@Column
        }
        val inconnu = stringResource(R.string.tournees_inconnu)
        // 2. Qui, quand.
        Text(stringResource(R.string.tournees_prepare, t.projet?.prepareLe ?: inconnu, t.projet?.operateur ?: inconnu), style = MaterialTheme.typography.bodyMedium)
        // 3. Contrôles et état (la tournée est « ouverte » : l'état « rendue » vient avec l'export, mission 006).
        Text(stringResource(R.string.tournees_saisies, t.controles.size), style = MaterialTheme.typography.bodyMedium)
    }
}
