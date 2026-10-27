package io.github.pobsteta.surculus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.github.pobsteta.surculus.carte.EcranCarte
import io.github.pobsteta.surculus.carte.EcranGuidage
import io.github.pobsteta.surculus.essai.EcranEssaiGnss
import io.github.pobsteta.surculus.tournee.DepotTournees
import io.github.pobsteta.surculus.tournee.EcranTournees
import io.github.pobsteta.surculus.ui.theme.SurculusTheme
import io.github.pobsteta.surculus.sauvegarde.EcranRestauration

/**
 * Accueil (mission 040) : la liste des tournées (mission 011), avec un menu à trois points (Essai GNSS, mission 018 ;
 * Restaurer une tournée, mission 009). Navigation : [EtatNavigation], testée sans Compose.
 * Le contrôle des plants (mission 012) viendra après le guidage.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val reglagesAffichage = DepotTournees.reglagesAffichage(this)
        setContent {
            // Plein écran (mission 044) : faux par défaut, gardé sur le téléphone ; les barres reviennent au glissement.
            var pleinEcran by remember { mutableStateOf(reglagesAffichage.pleinEcran) }
            LaunchedEffect(pleinEcran) {
                val controleur = WindowCompat.getInsetsController(window, window.decorView)
                if (pleinEcran) {
                    controleur.hide(WindowInsetsCompat.Type.systemBars())
                    controleur.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    controleur.show(WindowInsetsCompat.Type.systemBars())
                }
            }
            SurculusTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Survit à une rotation : écran, tournée ouverte (nom de son dossier), placette guidée (uuid).
                    var nav by rememberSaveable(
                        stateSaver = listSaver(save = { it.enListe() }, restore = { EtatNavigation.depuisListe(it) }),
                    ) { mutableStateOf(EtatNavigation()) }
                    // À la racine, le retour Android n'est pas intercepté : il quitte l'application.
                    BackHandler(enabled = !nav.estRacine) { nav = nav.retour() ?: nav }
                    // La liste des tournées gère elle-même ses marges (sa barre colorée passe sous la barre d'état).
                    val marges = if (nav.estRacine) Modifier else Modifier.safeDrawingPadding()
                    Column(Modifier.fillMaxSize().then(marges)) {
                        val tourneeId = nav.tourneeId
                        val placetteId = nav.placetteId
                        if (tourneeId != null && placetteId != null) {
                            EcranGuidage(tourneeId, placetteId, onRetour = { nav = nav.retour() ?: nav })
                        } else if (tourneeId != null) {
                            EcranCarte(tourneeId, onRetour = { nav = nav.retour() ?: nav }, onGuider = { nav = nav.guider(it) })
                        } else when (nav.destination) {
                            Destination.ESSAI -> EcranEssaiGnss()
                            Destination.RESTAURATION -> EcranRestauration()
                            Destination.TOURNEES -> EcranTournees(
                                onOuvrir = { nav = nav.ouvrirTournee(it) },
                                onEssai = { nav = nav.ouvrirEssai() },
                                onRestauration = { nav = nav.ouvrirRestauration() },
                                pleinEcran = pleinEcran,
                                onPleinEcran = {
                                    pleinEcran = !pleinEcran
                                    reglagesAffichage.pleinEcran = pleinEcran
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
