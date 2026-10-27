/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.essai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.pobsteta.surculus.R
import io.github.pobsteta.surculus.ui.BandeauCompact
import io.github.pobsteta.surculus.carte.rememberFix
import io.github.pobsteta.surculus.core.essai.AffichageEssai
import io.github.pobsteta.surculus.core.essai.Drapeau
import io.github.pobsteta.surculus.core.essai.EssaiGnss
import io.github.pobsteta.surculus.core.essai.Ton
import io.github.pobsteta.surculus.core.gnss.QualiteFix
import io.github.pobsteta.surculus.gnss.ConfigRtk
import io.github.pobsteta.surculus.gnss.ServiceGnssRtk
import io.github.pobsteta.surculus.gnss.SourcePosition
import kotlinx.coroutines.delay

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
 * Écran d'essai GNSS (mission 018) : la mesure seule, le fix en grand (mission 047 : les réglages du récepteur sont dans
 * Paramètres → Récepteur GNSS ; le récepteur démarre à l'ouverture de cet écran et s'arrête à la sortie, comme sur la carte).
 * Source unique du fix : [SourcePosition.fix] ; état du service : [ServiceGnssRtk.etat].
 */
@Composable
fun EcranEssaiGnss(onRetour: () -> Unit) {
    val ctx = LocalContext.current
    val (fix, localisationRefusee) = rememberFix()
    val etat by ServiceGnssRtk.etat.collectAsStateWithLifecycle()
    // L'horloge tourne chaque seconde : l'âge du fix doit avancer même quand rien ne change.
    val maintenant by produceState(System.currentTimeMillis()) {
        while (true) { value = System.currentTimeMillis(); delay(1000L) }
    }
    val affichage = EssaiGnss.afficher(fix, etat.fixRefuse, maintenant)
    val recepteurActif = remember { ConfigRtk.lire(ctx).actif }

    Column {
        BandeauCompact(
            titre = stringResource(R.string.essai_ouvrir),
            navigationIcon = {
                IconButton(onClick = onRetour) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.param_retour))
                }
            },
        )
        Column(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
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
                style = MaterialTheme.typography.bodyMedium, color = Noir,
            )
            if (!recepteurActif) Text(stringResource(R.string.essai_recepteur_inactif), style = MaterialTheme.typography.bodyMedium, color = Noir)
            if (localisationRefusee) Text(stringResource(R.string.carte_permission), fontWeight = FontWeight.Bold, color = Rouge)
            etat.erreur?.let { Text(it, color = Rouge) }
            etat.ntripErreur?.let { Text(stringResource(R.string.essai_ntrip_erreur, it), color = Rouge) }
            Spacer(Modifier.height(24.dp))
        }
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
        Text(a.ageFix, color = Blanc)
        Text(a.ageCorrections, color = Blanc)
        a.drapeaux.forEach { Text("▲ " + it.texte, fontWeight = FontWeight.Bold, color = Blanc) }
        a.lambertX?.let { Text("Lambert-93 · $it", color = Blanc) }
        a.lambertY?.let { Text("Lambert-93 · $it", color = Blanc) }
        if (a.enFrance == false) {
            Text(stringResource(R.string.essai_hors_france), fontWeight = FontWeight.Bold, color = Blanc)
        }
    }
}

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
