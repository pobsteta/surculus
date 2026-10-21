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
import androidx.compose.material3.MaterialTheme
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
import io.github.pobsteta.surculus.essai.EcranEssaiGnss
import io.github.pobsteta.surculus.sauvegarde.EcranRestauration

/**
 * Accueil du squelette (mission 001) ; l'écran d'essai GNSS (mission 018) s'ouvre d'un bouton.
 * Les vrais écrans viennent avec les missions 011 et 012.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var essai by rememberSaveable { mutableStateOf(false) }
                    var restauration by rememberSaveable { mutableStateOf(false) }
                    BackHandler(enabled = essai || restauration) { essai = false; restauration = false }
                    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                        if (essai) {
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
