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
import androidx.compose.foundation.border
import io.github.pobsteta.surculus.carte.Palette
import io.github.pobsteta.surculus.carte.couleurErreur
import io.github.pobsteta.surculus.carte.couleurNeutre
import io.github.pobsteta.surculus.ui.theme.LocalSombre

// Mission 063 : plus de copie de la palette ; les teintes viennent de [Palette], texte neutre et erreurs du thème.
private fun Ton.couleur() = when (this) {
    Ton.BON -> Palette.Vert
    Ton.A_SURVEILLER -> Palette.Orange
    Ton.MAUVAIS -> Palette.Rouge
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
                .then(if (LocalSombre.current) Modifier else Modifier.background(Palette.Blanc))
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
                style = MaterialTheme.typography.bodyMedium, color = couleurNeutre(),
            )
            if (!recepteurActif) Text(stringResource(R.string.essai_recepteur_inactif), style = MaterialTheme.typography.bodyMedium, color = couleurNeutre())
            if (localisationRefusee) Text(stringResource(R.string.carte_permission), fontWeight = FontWeight.Bold, color = couleurErreur())
            etat.erreur?.let { Text(it, color = couleurErreur()) }
            etat.ntripErreur?.let { Text(stringResource(R.string.essai_ntrip_erreur, it), color = couleurErreur()) }
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
            .then(if (LocalSombre.current) Modifier.border(2.dp, Palette.Blanc, RoundedCornerShape(16.dp)) else Modifier)
            .background(a.ton.couleur())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(a.qualite, fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Palette.Blanc)
        Text(a.precision, fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Palette.Blanc)
        Text(a.ageFix, color = Palette.Blanc)
        Text(a.ageCorrections, color = Palette.Blanc)
        a.drapeaux.forEach { Text("▲ " + it.texte, fontWeight = FontWeight.Bold, color = Palette.Blanc) }
        a.lambertX?.let { Text("Lambert-93 · $it", color = Palette.Blanc) }
        a.lambertY?.let { Text("Lambert-93 · $it", color = Palette.Blanc) }
        if (a.enFrance == false) {
            Text(stringResource(R.string.essai_hors_france), fontWeight = FontWeight.Bold, color = Palette.Blanc)
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
