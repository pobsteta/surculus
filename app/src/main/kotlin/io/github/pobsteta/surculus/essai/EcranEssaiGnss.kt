/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.essai

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.pobsteta.surculus.R
import io.github.pobsteta.surculus.core.essai.AffichageEssai
import io.github.pobsteta.surculus.core.essai.Drapeau
import io.github.pobsteta.surculus.core.essai.EssaiGnss
import io.github.pobsteta.surculus.core.essai.Ton
import io.github.pobsteta.surculus.core.gnss.PointMontage
import io.github.pobsteta.surculus.core.gnss.QualiteFix
import io.github.pobsteta.surculus.gnss.ClientNtrip
import io.github.pobsteta.surculus.gnss.ConfigRtk
import io.github.pobsteta.surculus.gnss.ServiceGnssRtk
import io.github.pobsteta.surculus.gnss.SourcePosition
import io.github.pobsteta.surculus.gnss.TransportRtk
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Quatre couleurs, contrastées pour le plein soleil : noir sur blanc, et trois teintes d'état.
private val Vert = Color(0xFF0B6B2E)
private val Orange = Color(0xFFB45300)
private val Rouge = Color(0xFFB00020)
private val Noir = Color(0xFF000000)
private val Blanc = Color(0xFFFFFFFF)

private fun Ton.couleur() = when (this) {
    Ton.BON -> Vert
    Ton.A_SURVEILLER -> Orange
    Ton.MAUVAIS -> Rouge
}

/**
 * Écran d'essai GNSS (mission 018) : le fix en grand en haut, les réglages du récepteur dessous.
 * Source unique du fix : [SourcePosition.fix] ; état du service : [ServiceGnssRtk.etat].
 */
@Composable
fun EcranEssaiGnss() {
    val ctx = LocalContext.current
    val fix by SourcePosition.fix.collectAsStateWithLifecycle()
    val etat by ServiceGnssRtk.etat.collectAsStateWithLifecycle()
    // L'horloge tourne chaque seconde : l'âge du fix doit avancer même quand rien ne change.
    val maintenant by produceState(System.currentTimeMillis()) {
        while (true) { value = System.currentTimeMillis(); delay(1000L) }
    }
    val affichage = EssaiGnss.afficher(fix, etat.fixRefuse, maintenant)

    var config by remember { mutableStateOf(ConfigRtk.lire(ctx)) }
    var points by remember { mutableStateOf<List<PointMontage>>(emptyList()) }
    var menuAppareil by remember { mutableStateOf(false) }
    var menuPoints by remember { mutableStateOf(false) }
    var enregistre by remember { mutableStateOf(false) }
    var manquantes by remember { mutableStateOf<List<String>>(emptyList()) }
    var localisationOk by remember { mutableStateOf(SourcePosition.localisationAutorisee(ctx)) }
    val scope = rememberCoroutineScope()

    // Le GNSS du téléphone sert de repli (et de banc d'essai de la position simulée) seulement tant que
    // l'écran est visible ; l'effet ne dépend que de la permission, pas de l'horloge.
    LifecycleStartEffect(localisationOk) {
        val ecoute = SourcePosition.ecouterTelephone(ctx)
        onStopOrDispose { ecoute.close() }
    }

    fun lancer() {
        // Enregistré seulement ici, sur l'action de Pascal, permissions accordées.
        val c = config.copy(actif = true, transport = TransportRtk.BLUETOOTH, ntrip = true)
        ConfigRtk.memoriser(ctx, c)
        config = c
        enregistre = true
        ServiceGnssRtk.demarrer(ctx)
    }

    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { accordees ->
        localisationOk = SourcePosition.localisationAutorisee(ctx)
        manquantes = accordees.filterValues { !it }.keys.toList()
        if (manquantes.isEmpty()) lancer()
    }

    fun demarrer() {
        val aDemander = permissionsAUtiliser().filter {
            ContextCompat.checkSelfPermission(ctx, it) != PackageManager.PERMISSION_GRANTED
        }
        if (aDemander.isEmpty()) {
            manquantes = emptyList()
            lancer()
        } else {
            // Le service ne démarre que dans le rappel, et seulement si tout est accordé.
            permissions.launch(aDemander.toTypedArray())
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(Blanc)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BlocMesure(affichage)

        Text(
            stringResource(
                R.string.essai_source,
                if (etat.enMarche) stringResource(if (etat.connecte) R.string.essai_lien_ok else R.string.essai_lien_attente)
                else stringResource(R.string.essai_lien_arrete),
            ),
            fontSize = 16.sp, color = Noir,
        )
        etat.erreur?.let { Text(it, fontSize = 16.sp, color = Rouge) }
        etat.ntripErreur?.let { Text(stringResource(R.string.essai_ntrip_erreur, it), fontSize = 16.sp, color = Rouge) }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { demarrer() },
                enabled = config.appareilBt != null && config.pointMontage.isNotBlank(),
                modifier = Modifier.weight(1f).height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Vert, contentColor = Blanc),
            ) { Text(stringResource(R.string.essai_demarrer), fontSize = 20.sp) }
            OutlinedButton(
                onClick = { ServiceGnssRtk.arreter(ctx) },
                modifier = Modifier.weight(1f).height(64.dp),
            ) { Text(stringResource(R.string.essai_arreter), fontSize = 20.sp, color = Noir) }
        }
        if (manquantes.isNotEmpty()) {
            Text(
                stringResource(R.string.essai_permissions_manquantes, manquantes.joinToString(", ") { it.substringAfterLast('.') }),
                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Rouge,
            )
        }
        if (enregistre) Text(stringResource(R.string.essai_enregistre), fontSize = 14.sp, color = Noir)

        Text(stringResource(R.string.essai_reglages), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Noir)

        // Récepteur Bluetooth apparié.
        Box {
            OutlinedButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED ||
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                    ) menuAppareil = true else permissions.launch(permissionsAUtiliser().toTypedArray())
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(config.appareilBtNom ?: stringResource(R.string.essai_choisir_recepteur), fontSize = 18.sp, color = Noir)
            }
            DropdownMenu(expanded = menuAppareil, onDismissRequest = { menuAppareil = false }) {
                val appareils = appareilsAppairesRecepteur(ctx)
                if (appareils.isEmpty()) DropdownMenuItem(text = { Text(stringResource(R.string.essai_aucun_appareil)) }, onClick = { menuAppareil = false })
                appareils.forEach { (nom, adresse) ->
                    DropdownMenuItem(
                        text = { Text(nom, fontSize = 18.sp) },
                        onClick = { config = config.copy(appareilBt = adresse, appareilBtNom = nom); menuAppareil = false },
                    )
                }
            }
        }

        // Point de montage : saisi, ou choisi dans la table des sources du caster.
        OutlinedTextField(
            value = config.pointMontage,
            onValueChange = { config = config.copy(pointMontage = it.trim()) },
            label = { Text(stringResource(R.string.essai_point_montage)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Box {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val recus = ClientNtrip.pointsMontage(config.casterHote, config.casterPort)
                        points = EssaiGnss.trierPoints(recus, SourcePosition.positionApprochee(ctx))
                        menuPoints = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text(stringResource(R.string.essai_charger_points, config.casterHote), fontSize = 16.sp, color = Noir) }
            DropdownMenu(expanded = menuPoints, onDismissRequest = { menuPoints = false }) {
                if (points.isEmpty()) DropdownMenuItem(text = { Text(stringResource(R.string.essai_aucun_point)) }, onClick = { menuPoints = false })
                points.take(50).forEach { p ->
                    DropdownMenuItem(
                        text = { Text(p.nom, fontSize = 18.sp) },
                        onClick = { config = config.copy(pointMontage = p.nom); menuPoints = false },
                    )
                }
            }
        }
        OutlinedTextField(
            value = config.utilisateur,
            onValueChange = { config = config.copy(utilisateur = it.trim()) },
            label = { Text(stringResource(R.string.essai_utilisateur)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = config.motDePasse,
            onValueChange = { config = config.copy(motDePasse = it) },
            label = { Text(stringResource(R.string.essai_mot_de_passe)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(R.string.essai_compte_aide), fontSize = 14.sp, color = Noir)
        Spacer(Modifier.height(24.dp))
    }
}

/** Le bloc vu en premier : la qualité en très grand, la précision, puis quatre lignes. */
@Composable
fun BlocMesure(a: AffichageEssai) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(a.ton.couleur())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(a.qualite, fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Blanc)
        Text(a.precision, fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Blanc)
        Text(a.ageFix, fontSize = 20.sp, color = Blanc)
        Text(a.ageCorrections, fontSize = 20.sp, color = Blanc)
        a.drapeaux.forEach { Text("▲ " + it.texte, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Blanc) }
        a.lambertX?.let { Text("Lambert-93 · $it", fontSize = 18.sp, color = Blanc) }
        a.lambertY?.let { Text("Lambert-93 · $it", fontSize = 18.sp, color = Blanc) }
        if (a.enFrance == false) {
            Text(stringResource(R.string.essai_hors_france), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Blanc)
        }
    }
}

private fun permissionsAUtiliser(): List<String> = buildList {
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(Manifest.permission.BLUETOOTH_CONNECT)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
}

@SuppressLint("MissingPermission")
private fun appareilsAppairesRecepteur(ctx: Context): List<Pair<String, String>> = runCatching {
    ctx.getSystemService(BluetoothManager::class.java)?.adapter?.bondedDevices.orEmpty()
        .map { (it.name ?: it.address) to it.address }
        .sortedBy { it.first }
}.getOrDefault(emptyList())

@Preview(widthDp = 411, heightDp = 500, showBackground = true)
@Composable
private fun ApercuRtkFixe() {
    BlocMesure(
        AffichageEssai(
            qualite = QualiteFix.RTK_FIXE.libelle, ton = Ton.BON, precision = "± 0,02 m",
            ageFix = "Récepteur · fix il y a 1 s", ageCorrections = "Corrections : 3 s", drapeaux = emptyList(),
            lambertX = "X 846 164,76 m", lambertY = "Y 6 686 123,26 m", enFrance = true,
        ),
    )
}

@Preview(widthDp = 411, heightDp = 500, showBackground = true)
@Composable
private fun ApercuRetrograde() {
    BlocMesure(
        AffichageEssai(
            qualite = QualiteFix.AUTONOME.libelle, ton = Ton.A_SURVEILLER, precision = "—",
            ageFix = "Récepteur · fix il y a 1 s", ageCorrections = "Corrections : 34 s",
            drapeaux = listOf(Drapeau.Retrograde(QualiteFix.RTK_FIXE)),
            lambertX = "X 846 164,76 m", lambertY = "Y 6 686 123,26 m", enFrance = true,
        ),
    )
}
