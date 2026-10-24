package io.github.pobsteta.surculus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.pobsteta.surculus.carte.EcranCarte
import io.github.pobsteta.surculus.carte.EcranGuidage
import io.github.pobsteta.surculus.essai.EcranEssaiGnss
import io.github.pobsteta.surculus.tournee.EcranTournees
import io.github.pobsteta.surculus.ui.theme.SurculusTheme
import io.github.pobsteta.surculus.sauvegarde.EcranRestauration

/**
 * Accueil : Tournées (mission 011) en premier, puis la restauration (mission 009) et l'essai GNSS (mission 018).
 * Navigation : accueil, tournées, carte d'une tournée, guidage vers une de ses placettes (écrans de la 011).
 * Le contrôle des plants (mission 012) viendra après le guidage.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SurculusTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var essai by rememberSaveable { mutableStateOf(false) }
                    var restauration by rememberSaveable { mutableStateOf(false) }
                    // Tournée ouverte (nom de son dossier) et placette guidée (uuid) : survivent à une rotation.
                    var tournees by rememberSaveable { mutableStateOf(false) }
                    var tourneeId by rememberSaveable { mutableStateOf<String?>(null) }
                    var placetteId by rememberSaveable { mutableStateOf<String?>(null) }
                    val retour = {
                        when {
                            placetteId != null -> placetteId = null
                            tourneeId != null -> tourneeId = null
                            else -> { tournees = false; essai = false; restauration = false }
                        }
                    }
                    BackHandler(enabled = essai || restauration || tournees) { retour() }
                    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                        if (tournees && tourneeId != null && placetteId != null) {
                            EcranGuidage(tourneeId!!, placetteId!!, onRetour = { retour() })
                        } else if (tournees && tourneeId != null) {
                            EcranCarte(tourneeId!!, onRetour = { retour() }, onGuider = { placetteId = it })
                        } else if (tournees) {
                            EcranTournees(onOuvrir = { tourneeId = it }, onRetour = { retour() })
                        } else if (essai) {
                            EcranEssaiGnss()
                        } else if (restauration) {
                            EcranRestauration()
                        } else {
                            Column(
                                Modifier.fillMaxSize().padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(text = stringResource(R.string.app_name), fontSize = 32.sp)
                                Button(onClick = { tournees = true }, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                                    Text(stringResource(R.string.tournees_ouvrir), fontSize = 20.sp)
                                }
                                Button(onClick = { essai = true }, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                                    Text(stringResource(R.string.essai_ouvrir), fontSize = 20.sp)
                                }
                                Button(onClick = { restauration = true }, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                                    Text(stringResource(R.string.restauration_ouvrir), fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
