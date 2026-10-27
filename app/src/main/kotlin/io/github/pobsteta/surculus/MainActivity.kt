package io.github.pobsteta.surculus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
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
import io.github.pobsteta.surculus.gnss.ConfigRtk
import io.github.pobsteta.surculus.gnss.PilotageRecepteur
import io.github.pobsteta.surculus.gnss.RecepteurService
import io.github.pobsteta.surculus.parametres.EcranParametres
import io.github.pobsteta.surculus.tournee.DepotTournees
import io.github.pobsteta.surculus.tournee.EcranPlacettes
import io.github.pobsteta.surculus.tournee.EcranTournees
import io.github.pobsteta.surculus.ui.theme.SurculusTheme
import io.github.pobsteta.surculus.ui.theme.iconesBarreEtatClaires
import io.github.pobsteta.surculus.sauvegarde.EcranRestauration

/**
 * Accueil (mission 040) : la liste des tournées (mission 011), avec un menu à trois points (Paramètres, À propos) ;
 * Paramètres (mission 046) ouvre l'Essai GNSS (mission 018) et la Restauration (mission 009). Navigation : [EtatNavigation], testée sans Compose.
 * Tournée ouverte : Placettes, puis Carte, puis Guidage (mission 061). Le contrôle des plants (mission 012) viendra après le guidage.
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
            // Thème (mission 051) : Système par défaut, gardé sur le téléphone ; le changement s'applique tout de suite.
            var choixTheme by remember { mutableStateOf(reglagesAffichage.choixTheme) }
            // Thème réellement appliqué (le choix, pas seulement le mode du téléphone), pour les icônes des barres système.
            val sombre = choixTheme.estSombre(isSystemInDarkTheme())
            SurculusTheme(choixTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Survit à une rotation : écran, tournée ouverte (nom de son dossier), placette guidée (uuid).
                    var nav by rememberSaveable(
                        stateSaver = listSaver(save = { it.enListe() }, restore = { EtatNavigation.depuisListe(it) }),
                    ) { mutableStateOf(EtatNavigation()) }
                    // Icônes des barres système : barre d'état claire sous le bandeau vert, présent sur tous les écrans
                    // (carte et guidage compris, mission 055), dans les deux thèmes, comme Marculus ; barre de navigation
                    // selon le thème.
                    val ecranAvecBandeau = true
                    LaunchedEffect(sombre, ecranAvecBandeau) {
                        val controleur = WindowCompat.getInsetsController(window, window.decorView)
                        controleur.isAppearanceLightStatusBars = !iconesBarreEtatClaires(ecranAvecBandeau, sombre)
                        controleur.isAppearanceLightNavigationBars = !sombre
                    }
                    // Récepteur GNSS externe (mission 047, comme Groma) : démarré à l'ouverture de la carte (ou de l'essai)
                    // si le réglage des Paramètres est actif, arrêté à la sortie ; une rotation ne le coupe pas.
                    val pilote = remember { PilotageRecepteur(RecepteurService(applicationContext)) { ConfigRtk.lire(applicationContext).actif } }
                    DisposableEffect(nav.recepteurDemande) {
                        if (nav.recepteurDemande) pilote.entrer()
                        onDispose { if (!isChangingConfigurations) pilote.sortir() }
                    }
                    // À la racine, le retour Android n'est pas intercepté : il quitte l'application.
                    BackHandler(enabled = !nav.estRacine) { nav = nav.retour() ?: nav }
                    // Tous les écrans ont un bandeau compact (liste, Paramètres, Restauration, Essai GNSS : mission 053 ;
                    // carte et guidage : mission 055) et gèrent eux-mêmes leurs marges : le bandeau coloré passe sous la
                    // barre d'état, le corps garde les marges latérales et basse.
                    Column(Modifier.fillMaxSize()) {
                        val tourneeId = nav.tourneeId
                        val placetteId = nav.placetteId
                        if (tourneeId != null && placetteId != null) {
                            EcranGuidage(tourneeId, placetteId, onRetour = { nav = nav.retour() ?: nav })
                        } else if (tourneeId != null && nav.carte) {
                            EcranCarte(tourneeId, centreId = nav.centreId, onRetour = { nav = nav.retour() ?: nav }, onGuider = { nav = nav.guider(it) })
                        } else if (tourneeId != null) {
                            // Mission 061, comme Groma : la tournée s'ouvre sur ses placettes, la carte vient d'un appui.
                            EcranPlacettes(
                                tourneeId,
                                onRetour = { nav = nav.retour() ?: nav },
                                onCarte = { nav = nav.ouvrirCarte() },
                                onPlacette = { nav = nav.ouvrirCarte(it) },
                            )
                        } else when (nav.destination) {
                            Destination.ESSAI -> EcranEssaiGnss(onRetour = { nav = nav.retour() ?: nav })
                            Destination.RESTAURATION -> EcranRestauration(onRetour = { nav = nav.retour() ?: nav })
                            Destination.PARAMETRES -> EcranParametres(
                                pleinEcran = pleinEcran,
                                onPleinEcran = {
                                    pleinEcran = it
                                    reglagesAffichage.pleinEcran = it
                                },
                                choixTheme = choixTheme,
                                onChoixTheme = {
                                    choixTheme = it
                                    reglagesAffichage.choixTheme = it
                                },
                                onEssai = { nav = nav.ouvrirEssai() },
                                onRestauration = { nav = nav.ouvrirRestauration() },
                                onRetour = { nav = nav.retour() ?: nav },
                            )
                            Destination.TOURNEES -> EcranTournees(
                                onOuvrir = { nav = nav.ouvrirTournee(it) },
                                onParametres = { nav = nav.ouvrirParametres() },
                            )
                        }
                    }
                }
            }
        }
    }
}
