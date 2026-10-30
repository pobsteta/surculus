/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 *
 * Origine : dépôt Groma (https://github.com/pobsteta/groma), fichier android/app/src/main/kotlin/groma/app/ui/SectionRtk.kt.
 * Modifications : paquet ; ni bouton « Tester » ni diagnostic (la mesure est dans l'Essai GNSS) ; lignes d'au moins 48 dp ;
 *                 styles de texte du thème (mission 042).
 */
package io.github.pobsteta.surculus.parametres

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.pobsteta.surculus.R
import io.github.pobsteta.surculus.core.essai.EssaiGnss
import io.github.pobsteta.surculus.core.gnss.PointMontage
import io.github.pobsteta.surculus.gnss.ClientNtrip
import io.github.pobsteta.surculus.gnss.ConfigRtk
import io.github.pobsteta.surculus.gnss.SourcePosition
import io.github.pobsteta.surculus.gnss.TransportRtk
import kotlinx.coroutines.launch

/**
 * Paramètres → « Récepteur GNSS » (mission 047), repris de Groma : interrupteur, transport (Bluetooth ou TCP), appareil,
 * corrections NTRIP. Les réglages s'enregistrent à la saisie ; le récepteur démarre à l'ouverture de la carte et s'arrête
 * à la sortie de la tournée (voir `PilotageRecepteur`), pas ici : la mesure est dans l'Essai GNSS.
 */
@Composable
fun SectionRecepteur() {
    val ctx = LocalContext.current
    var rtk by remember { mutableStateOf(ConfigRtk.lire(ctx)) }
    fun maj(c: ConfigRtk) { rtk = c; ConfigRtk.memoriser(ctx, c) }
    val portee = rememberCoroutineScope()
    var choixAppareil by remember { mutableStateOf(false) }
    var points by remember { mutableStateOf<List<PointMontage>>(emptyList()) }
    var menuPoints by remember { mutableStateOf(false) }
    var chargement by remember { mutableStateOf(false) }
    // Les autorisations se relisent à chaque affichage et au retour des réglages du système : jamais d'état périmé.
    var relecture by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        relecture++
        onPauseOrDispose {}
    }

    // Rationale lue juste avant chaque demande : avec celle d'après, elle dit si le refus est définitif (mission 052).
    val rationalesAvant = remember { mutableMapOf<String, Boolean>() }

    // Android 13+ : sans la notification, « Libérer le récepteur » reste caché ; Android 12+ : Bluetooth demande une autorisation.
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { reponses ->
        // Un refus est mémorisé : c'est lui qui distingue « refusée » de « pas encore demandée » (mission 052).
        // Android ne dit pas une fenêtre fermée sans réponse d'un refus définitif déjà acquis : dans le doute, réglages du téléphone.
        reponses.forEach { (permission, accordee) -> memoriserSiRefus(ctx, permission, accordee, rationalesAvant[permission] == true) }
        relecture++
    }
    val permissionBt = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        memoriserSiRefus(ctx, Manifest.permission.BLUETOOTH_CONNECT, ok, rationalesAvant[Manifest.permission.BLUETOOTH_CONNECT] == true)
        relecture++
        if (ok) choixAppareil = true
    }

    Text(stringResource(R.string.recepteur_section), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
    Text(stringResource(R.string.recepteur_section_desc), style = MaterialTheme.typography.bodySmall)
    Interrupteur(stringResource(R.string.recepteur_actif), stringResource(R.string.recepteur_actif_desc), rtk.actif) { actif ->
        maj(rtk.copy(actif = actif))
        if (actif) { val demandes = permissionsAUtiliser(); noterRationales(ctx, demandes, rationalesAvant); permissions.launch(demandes.toTypedArray()) }
    }
    if (!rtk.actif) return
    // `relecture` est lu ici pour recalculer après chaque réponse du système.
    val avertissements = if (relecture < 0) emptyList() else avertissementsRecepteur(
        rtk.actif, rtk.transport, etatBluetooth(ctx), etatNotifications(ctx),
    )
    avertissements.forEach {
        val texte = when (it) {
            AvertissementRecepteur.BLUETOOTH_A_DEMANDER -> R.string.recepteur_bt_a_demander
            AvertissementRecepteur.BLUETOOTH_REFUSE -> R.string.recepteur_bt_refuse
            AvertissementRecepteur.BLUETOOTH_REFUSE_DEFINITIVEMENT -> R.string.recepteur_bt_refuse_definitif
            AvertissementRecepteur.NOTIFICATIONS_A_DEMANDER -> R.string.recepteur_notif_a_demander
            AvertissementRecepteur.NOTIFICATIONS_REFUSEES -> R.string.recepteur_notif_refusee
            AvertissementRecepteur.NOTIFICATIONS_REFUSEE_DEFINITIVEMENT -> R.string.recepteur_notif_refusee_definitif
        }
        // « Pas encore demandée » n'est pas une erreur : texte normal ; un refus est en couleur d'erreur.
        val pasDemandee = it == AvertissementRecepteur.BLUETOOTH_A_DEMANDER || it == AvertissementRecepteur.NOTIFICATIONS_A_DEMANDER
        Text(
            stringResource(texte), style = MaterialTheme.typography.bodyMedium,
            color = if (pasDemandee) androidx.compose.ui.graphics.Color.Unspecified else MaterialTheme.colorScheme.error,
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Choix(stringResource(R.string.recepteur_bluetooth), rtk.transport == TransportRtk.BLUETOOTH) { maj(rtk.copy(transport = TransportRtk.BLUETOOTH)) }
        Choix(stringResource(R.string.recepteur_tcp), rtk.transport == TransportRtk.TCP) { maj(rtk.copy(transport = TransportRtk.TCP)) }
    }
    when (rtk.transport) {
        TransportRtk.BLUETOOTH -> Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                rtk.appareilBtNom ?: stringResource(R.string.recepteur_aucun_appareil),
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
            )
            OutlinedButton(modifier = Modifier.heightIn(min = 48.dp), onClick = {
                if (autorisationBluetooth(ctx)) choixAppareil = true else { noterRationales(ctx, listOf(Manifest.permission.BLUETOOTH_CONNECT), rationalesAvant); permissionBt.launch(Manifest.permission.BLUETOOTH_CONNECT) }
            }) { Text(stringResource(R.string.recepteur_choisir)) }
        }
        TransportRtk.TCP -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Champ(rtk.hoteTcp, { maj(rtk.copy(hoteTcp = it.trim())) }, stringResource(R.string.recepteur_hote), Modifier.weight(2f))
            Champ(
                rtk.portTcp.toString(), { v -> v.toIntOrNull()?.let { maj(rtk.copy(portTcp = it)) } },
                stringResource(R.string.recepteur_port), Modifier.weight(1f), KeyboardType.Number,
            )
        }
    }

    Interrupteur(stringResource(R.string.recepteur_ntrip), stringResource(R.string.recepteur_ntrip_desc), rtk.ntrip) { maj(rtk.copy(ntrip = it)) }
    if (rtk.ntrip) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Champ(rtk.casterHote, { maj(rtk.copy(casterHote = it.trim())) }, stringResource(R.string.recepteur_caster), Modifier.weight(2f))
            Champ(
                rtk.casterPort.toString(), { v -> v.toIntOrNull()?.let { maj(rtk.copy(casterPort = it)) } },
                stringResource(R.string.recepteur_port), Modifier.weight(1f), KeyboardType.Number,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Champ(rtk.pointMontage, { maj(rtk.copy(pointMontage = it.trim())) }, stringResource(R.string.recepteur_point_montage), Modifier.weight(1f))
            Box {
                OutlinedButton(enabled = !chargement, modifier = Modifier.heightIn(min = 48.dp), onClick = {
                    chargement = true
                    portee.launch {
                        val recus = ClientNtrip.pointsMontage(rtk.casterHote, rtk.casterPort)
                        points = EssaiGnss.trierPoints(recus, SourcePosition.positionApprochee(ctx))
                        chargement = false
                        menuPoints = true
                    }
                }) { Text(stringResource(R.string.recepteur_charger)) }
                DropdownMenu(expanded = menuPoints, onDismissRequest = { menuPoints = false }) {
                    if (points.isEmpty()) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.recepteur_aucun_point)) }, onClick = { menuPoints = false })
                    }
                    points.take(50).forEach { p ->
                        DropdownMenuItem(
                            text = { Text("${p.nom} · ${p.identifiant} · ${p.format}") },
                            onClick = { maj(rtk.copy(pointMontage = p.nom)); menuPoints = false },
                        )
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Champ(rtk.utilisateur, { maj(rtk.copy(utilisateur = it.trim())) }, stringResource(R.string.recepteur_utilisateur), Modifier.weight(1f))
            Champ(rtk.motDePasse, { maj(rtk.copy(motDePasse = it)) }, stringResource(R.string.recepteur_mot_de_passe), Modifier.weight(1f), KeyboardType.Password)
        }
        Text(stringResource(R.string.recepteur_compte_aide), style = MaterialTheme.typography.bodySmall)
    }

    if (choixAppareil) {
        val appareils = remember { appareilsAppaires(ctx) }
        AlertDialog(
            onDismissRequest = { choixAppareil = false },
            confirmButton = {},
            dismissButton = {
                TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = { choixAppareil = false }) { Text(stringResource(R.string.recepteur_annuler)) }
            },
            title = { Text(stringResource(R.string.recepteur_appareils)) },
            text = {
                Column {
                    if (appareils.isEmpty()) Text(stringResource(R.string.recepteur_aucun_appaire))
                    appareils.forEach { (nom, adresse) ->
                        TextButton(
                            modifier = Modifier.heightIn(min = 48.dp),
                            onClick = { maj(rtk.copy(appareilBt = adresse, appareilBtNom = nom)); choixAppareil = false },
                        ) { Text(nom) }
                    }
                }
            },
        )
    }
}

@Composable
private fun Champ(valeur: String, onChange: (String) -> Unit, libelle: String, modifier: Modifier, clavier: KeyboardType = KeyboardType.Text) {
    // Suit la valeur enregistrée quand elle change de l'extérieur (point de montage choisi avec « Charger »).
    var texte by remember(valeur) { mutableStateOf(valeur) }
    OutlinedTextField(
        value = texte,
        onValueChange = { texte = it; onChange(it) },
        label = { Text(libelle) },
        singleLine = true,
        visualTransformation = if (clavier == KeyboardType.Password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = clavier),
        modifier = modifier,
    )
}

@Composable
private fun Choix(libelle: String, choisi: Boolean, onClick: () -> Unit) {
    val m = Modifier.heightIn(min = 48.dp)
    if (choisi) Button(onClick = onClick, modifier = m) { Text(libelle) } else OutlinedButton(onClick = onClick, modifier = m) { Text(libelle) }
}

/** Case à bascule : toute la ligne est la cible (au moins 48 dp, une main, avec des gants). */
@Composable
private fun Interrupteur(titre: String, description: String, valeur: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .toggleable(value = valeur, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(titre, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = valeur, onCheckedChange = null)
    }
}

private fun autorisationBluetooth(ctx: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

private fun autorisationNotifications(ctx: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private const val PREFS_AUTORISATIONS = "autorisations"

/** Mémorise qu'une autorisation a été refusée : le système ne distingue pas « jamais demandée » de « refusée pour de bon ». */
private fun memoriserRefus(ctx: Context, permission: String) {
    ctx.getSharedPreferences(PREFS_AUTORISATIONS, Context.MODE_PRIVATE).edit().putBoolean(permission, true).apply()
}

/** Mémorise un refus d'après la réponse du système et la rationale lue avant la demande. */
private fun memoriserSiRefus(ctx: Context, permission: String, accordee: Boolean, rationaleAvant: Boolean) {
    if (refusAMemoriser(accordee, rationaleAvant, rationale(ctx, permission))) memoriserRefus(ctx, permission)
}

/** Relève `shouldShowRequestPermissionRationale` juste avant une demande. */
private fun noterRationales(ctx: Context, permissions: List<String>, vers: MutableMap<String, Boolean>) {
    permissions.forEach { vers[it] = rationale(ctx, it) }
}

private fun rationale(ctx: Context, permission: String): Boolean =
    activite(ctx)?.shouldShowRequestPermissionRationale(permission) == true

/** L'activité sous les enveloppes de contexte (le contexte de la composition n'en est pas toujours une). */
private tailrec fun activite(ctx: Context): android.app.Activity? = when (ctx) {
    is android.app.Activity -> ctx
    is android.content.ContextWrapper -> activite(ctx.baseContext)
    else -> null
}

private fun etat(ctx: Context, permission: String, accordee: Boolean): EtatAutorisation = etatAutorisation(
    accordee = accordee,
    dejaRefusee = ctx.getSharedPreferences(PREFS_AUTORISATIONS, Context.MODE_PRIVATE).getBoolean(permission, false),
    rationale = rationale(ctx, permission),
)

private fun etatBluetooth(ctx: Context) = etat(ctx, Manifest.permission.BLUETOOTH_CONNECT, autorisationBluetooth(ctx))

private fun etatNotifications(ctx: Context) = etat(ctx, Manifest.permission.POST_NOTIFICATIONS, autorisationNotifications(ctx))

private fun permissionsAUtiliser(): List<String> = buildList {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(Manifest.permission.BLUETOOTH_CONNECT)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
}

/** Appareils Bluetooth appairés (nom, adresse), triés par nom ; vide sans autorisation. */
@SuppressLint("MissingPermission")
private fun appareilsAppaires(ctx: Context): List<Pair<String, String>> = runCatching {
    ctx.getSystemService(BluetoothManager::class.java)?.adapter?.bondedDevices.orEmpty()
        .map { (it.name ?: it.address) to it.address }
        .sortedBy { it.first }
}.getOrDefault(emptyList())
