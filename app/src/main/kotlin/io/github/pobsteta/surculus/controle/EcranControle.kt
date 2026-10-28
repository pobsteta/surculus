/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.controle

import android.Manifest
import android.os.SystemClock
import androidx.compose.foundation.border
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import io.github.pobsteta.surculus.core.session.SortieClasseur
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.github.pobsteta.surculus.R
import io.github.pobsteta.surculus.carte.BadgeFix
import io.github.pobsteta.surculus.carte.Chargement
import io.github.pobsteta.surculus.carte.PastilleDisque
import io.github.pobsteta.surculus.carte.Palette
import io.github.pobsteta.surculus.carte.chargerTournee
import io.github.pobsteta.surculus.carte.collecterPositionsArrivee
import io.github.pobsteta.surculus.carte.couleurErreur
import io.github.pobsteta.surculus.carte.couleurNeutre
import io.github.pobsteta.surculus.carte.rememberFix
import io.github.pobsteta.surculus.core.placette.EtatDsf
import io.github.pobsteta.surculus.core.placette.GroupeCause
import io.github.pobsteta.surculus.gpkg.ContexteDeTournee
import io.github.pobsteta.surculus.gnss.FixPourSession
import io.github.pobsteta.surculus.gnss.SourcePosition
import io.github.pobsteta.surculus.sauvegarde.SauvegardeApp
import io.github.pobsteta.surculus.tournee.DepotTournees
import io.github.pobsteta.surculus.tournee.LigneTournee
import io.github.pobsteta.surculus.ui.BandeauCompact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Contrôleurs ouverts, par tournée et placette : survivent à la rotation (la session garde en mémoire le plant courant),
 * se libèrent quand on quitte vraiment l'écran. Le journal reste la source : un contrôleur perdu se refait (l'arrivée).
 */
object ControleursOuverts {
    private val ouverts = HashMap<String, ControleurPlacette>()

    fun cle(tournee: String, placette: String) = "$tournee/$placette"

    @Synchronized fun get(cle: String): ControleurPlacette? = ouverts[cle]

    @Synchronized fun poser(cle: String, c: ControleurPlacette) { ouverts[cle] = c }

    @Synchronized fun liberer(cle: String) { ouverts.remove(cle) }
}

/**
 * Écran du contrôle d'une placette (mission 012-B1, spec § 4.3). Une question : « sur quel plant suis-je, dans quel état,
 * combien en reste-t-il ? » En grand : « Plant n° 7 » et son état. Une comparaison : « 7 sur 12 », l'état d'avant en petit.
 * Source et jour dans le bandeau. Tout ce que l'agent dit est écrit au journal de la tournée tout de suite : sortir sans
 * « Terminer » ne perd rien, la placette reste « à faire ».
 */
@Composable
fun EcranControle(id: String, placetteUuid: String, onRetour: () -> Unit) {
    val ctx = LocalContext.current
    val chargement = chargerTournee(id)
    val (fix, _) = rememberFix()
    val ouverte = (chargement as? Chargement.Pret)?.tournee
    val placette = ouverte?.tournee?.placettes?.firstOrNull { it.uuid == placetteUuid }
    val cle = ControleursOuverts.cle(id, placetteUuid)

    // Permission de stockage, Android 8 et 9 seulement : demandée **une seule fois** (le réglage s'en souvient) ; tant qu'elle
    // manque, une ligne en petit l'explique, et rien n'est bloqué.
    val reglages = remember { DepotTournees.reglagesControle(ctx) }
    var stockageRefuse by remember { mutableStateOf(false) }
    val demanderStockage = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> stockageRefuse = !ok }
    LaunchedEffect(Unit) {
        val accordee = ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        stockageRefuse = PermissionStockage.manqueSurCetAppareil(accordee)
        if (PermissionStockage.aDemanderSurCetAppareil(accordee, reglages.stockageDejaDemande)) {
            reglages.stockageDejaDemande = true
            demanderStockage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
    // On libère le contrôleur en quittant vraiment l'écran, pas à la rotation.
    val activite = ctx as? android.app.Activity
    DisposableEffect(cle) { onDispose { if (activite?.isChangingConfigurations != true) ControleursOuverts.liberer(cle) } }

    var arrivee by remember(cle) { mutableStateOf<EtatArrivee>(ControleursOuverts.get(cle)?.let { EtatArrivee.Ouverte(it) } ?: EtatArrivee.Moyenne(0)) }
    var essai by rememberSaveable(cle) { mutableIntStateOf(0) }
    var code by rememberSaveable(cle) { mutableStateOf("") }
    val fixActuel = rememberUpdatedState(fix)

    // L'arrivée (décisions de Pascal du 2026-10-27) : la moyenne de 5 s part toute seule, avec son décompte, puis la règle des 15 m.
    LaunchedEffect(cle, essai, ouverte) {
        if (ouverte == null || ControleursOuverts.get(cle) != null) return@LaunchedEffect
        // La moyenne de 5 s est celle du guidage : écrite une seule fois (`collecterPositionsArrivee`).
        val positions = collecterPositionsArrivee(position = { fixActuel.value?.position }, surSeconde = { n -> arrivee = EtatArrivee.Moyenne(n) })
        val resultat = withContext(Dispatchers.IO) {
            runCatching {
                ArriveeControle.ouvrir(
                    contexte = ContexteDeTournee.de(ouverte.tournee),
                    journal = SauvegardeApp.journal(ctx, LigneTournee(ouverte.dossier, ouverte.tournee, null)),
                    horloge = SauvegardeApp.horloge(ctx) { FixPourSession.depuis(SourcePosition.fix.value) },
                    essences = ArriveeControle.essencesDe(ouverte.tournee),
                    positions = positions,
                    codeLu = code,
                    sauvegarde = SauvegardeApp.ecoute(ctx, LigneTournee(ouverte.dossier, ouverte.tournee, null)),
                )
            }
        }
        val v = resultat.getOrNull()
        if (v is EtatArrivee.Ouverte) ControleursOuverts.poser(cle, v.controleur)
        arrivee = v ?: EtatArrivee.SansPosition
    }

    Column(Modifier.fillMaxSize()) {
        BandeauCompact(
            titre = placette?.code?.let { stringResource(R.string.controle_titre, it) } ?: stringResource(R.string.controle_titre_sans_code),
            navigationIcon = {
                IconButton(onClick = onRetour, modifier = Modifier.sizeIn(minWidth = DimensionsControle.CIBLE_MIN, minHeight = DimensionsControle.CIBLE_MIN)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.carte_retour))
                }
            },
            actions = { BadgeFix(fix) },
        )
        Column(Modifier.weight(1f).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))) {
            when {
                chargement == Chargement.EnCours -> Texte(stringResource(R.string.carte_ouverture))
                chargement is Chargement.Refus -> Texte(stringResource(R.string.carte_refus, chargement.message), erreur = true)
                placette == null -> Texte(stringResource(R.string.carte_refus, "placette $placetteUuid"), erreur = true)
                else -> when (val a = arrivee) {
                    is EtatArrivee.Moyenne -> VueArrivee(a, code, onCode = { code = it }, onRecommencer = null)
                    is EtatArrivee.Refus -> VueArrivee(a, code, onCode = { code = it }, onRecommencer = { arrivee = EtatArrivee.Moyenne(0); essai++ })
                    EtatArrivee.SansPosition -> VueArrivee(a, code, onCode = { code = it }, onRecommencer = { arrivee = EtatArrivee.Moyenne(0); essai++ })
                    is EtatArrivee.Ouverte -> VueSession(a.controleur, id, reglages, stockageRefuse, onRetour)
                }
            }
        }
    }
}

@Composable
private fun Texte(texte: String, erreur: Boolean = false) {
    Text(texte, color = if (erreur) couleurErreur() else couleurNeutre(), modifier = Modifier.padding(24.dp))
}

/** Arrivée : le décompte de la moyenne, ou le refus avec son code et le champ « code du piquet ». */
@Composable
private fun VueArrivee(etat: EtatArrivee, code: String, onCode: (String) -> Unit, onRecommencer: (() -> Unit)?) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (etat) {
            is EtatArrivee.Moyenne -> {
                Text(stringResource(R.string.controle_moyenne, etat.n, etat.total), style = MaterialTheme.typography.headlineMedium, color = couleurNeutre())
                Text(stringResource(R.string.controle_moyenne_aide), style = MaterialTheme.typography.bodyMedium, color = couleurNeutre())
            }
            is EtatArrivee.Refus -> Text(TextesControle.message(etat.refus)!!.texte, style = MaterialTheme.typography.titleMedium, color = couleurErreur())
            EtatArrivee.SansPosition -> Text(stringResource(R.string.controle_sans_position), style = MaterialTheme.typography.titleMedium, color = couleurErreur())
            is EtatArrivee.Ouverte -> {}
        }
        OutlinedTextField(
            value = code,
            onValueChange = onCode,
            label = { Text(stringResource(R.string.guidage_code)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        onRecommencer?.let {
            Button(onClick = it, modifier = Modifier.fillMaxWidth().height(DimensionsControle.ACTION)) { Text(stringResource(R.string.controle_recommencer)) }
        }
    }
}

/** La session ouverte : la vue Plant, la confirmation de fin, ou « Contrôlée ». */
@Composable
private fun VueSession(controleur: ControleurPlacette, tournee: String, reglages: ReglagesControle, stockageRefuse: Boolean, onRetour: () -> Unit) {
    var ecran by remember(controleur) { mutableStateOf<EcranControle?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(controleur) { ecran = withContext(Dispatchers.IO) { controleur.instantane() } }
    fun agir(commande: ControleurPlacette.() -> EcranControle) {
        scope.launch { ecran = withContext(Dispatchers.IO) { controleur.commande() } }
    }
    // Mode poche (mission 012-C1) : l'état vit hors de Compose (PocheApp) et traverse la rotation.
    val mode = PocheApp.mode
    val activite = LocalContext.current as? android.app.Activity
    var poche by remember { mutableStateOf(mode.actif) }
    val verrou = remember { Mutex() }
    // Voix et vibration (012-C2) : sans voix française, l'annonce s'écrit dans la bande ; le bandeau le dit à l'entrée.
    val sorties = PocheApp.sorties
    // L'état vit dans `sorties` (hors de Compose) ; ce compteur ne sert qu'à redessiner la bande.
    var rafraichir by remember { mutableStateOf(0) }
    DisposableEffect(controleur) {
        poche = mode.actif
        val desabonner = mode.abonner { actif, raison ->
            poche = actif
            if (!actif) { sorties?.effacer(); rafraichir++ }
            if (!actif && raison != RaisonSortie.VUE_QUITTEE) sorties?.annoncer(AnnoncesPoche.sortie())
            val annonce = raison?.let { TextesControle.sortiePoche(it) }
            if (!actif && annonce != null) scope.launch { verrou.withLock { ecran = withContext(Dispatchers.IO) { controleur.annoncer(annonce) } } }
        }
        // Les gestes de volume arrivent dans l'ordre et s'appliquent un par un.
        sorties?.surTexte = { rafraichir++ }
        PocheApp.pont.surRappel = { sorties?.annoncer(AnnoncesPoche.rappel()) }
        PocheApp.pont.ecoute = { sortie ->
            sorties?.signal(sortie)
            when (sortie) {
                is SortieClasseur.Geste -> scope.launch {
                    verrou.withLock {
                        var annonce: AnnoncePoche? = null
                        ecran = withContext(Dispatchers.IO) { controleur.geste(sortie.geste).also { annonce = controleur.derniereAnnonce } }
                        annonce?.let { sorties?.annoncer(it) }
                    }
                }
                SortieClasseur.DeuxTouches -> scope.launch {
                    verrou.withLock {
                        ecran = withContext(Dispatchers.IO) { controleur.deuxTouches() }
                        controleur.derniereAnnonce?.let { sorties?.annoncer(it) }
                    }
                }
                SortieClasseur.Tic, SortieClasseur.Bourdon -> {}
            }
        }
        onDispose {
            desabonner()
            PocheApp.pont.ecoute = null
            PocheApp.pont.surRappel = null
            sorties?.surTexte = null
            if (activite?.isChangingConfigurations != true) mode.sortir(RaisonSortie.VUE_QUITTEE)
        }
    }
    // « Terminer » ou toute autre vue que le plant : le mode poche se lève (le regarni et les totaux se font à l'écran).
    LaunchedEffect(ecran?.vue) {
        val vue = ecran?.vue
        if (vue != null && vue != VueControle.PLANT) mode.sortir(if (vue == VueControle.TERMINEE) RaisonSortie.TERMINER else RaisonSortie.VUE_QUITTEE)
    }
    val e = ecran ?: return
    // Le geste « retour » depuis Pointer ou Terminer revient au plant ; depuis le plant, il quitte l'écran (plan § 3).
    BackHandler(enabled = e.vue == VueControle.TERMINER || e.vue == VueControle.POINTER) { agir { reprendre() } }
    // Enregistré après le précédent, donc prioritaire : en poche, le retour système est ignoré (une poche appuie sur « retour »).
    BackHandler(enabled = !ModePoche.retourPris(poche)) { }

    // La bande du mode poche est hors de la zone qui défile (mission 067) : entière et lisible quel que soit le défilement.
    Column(Modifier.fillMaxSize()) {
    if (poche) {
        rafraichir // lu pour que la bande se redessine à chaque annonce écrite
        Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            BandePoche(ligneVoix = if (sorties?.sansVoixFrancaise != false) AnnoncesPoche.BANDEAU_SANS_VOIX else null, annonce = sorties?.annonceEcrite)
        }
    }
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.controle_source, e.codePlacette, e.jour), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (e.plant.alerteTerminee) {
            Text(stringResource(R.string.controle_alerte_terminee), style = MaterialTheme.typography.titleSmall, color = couleurErreur())
        }
        when (e.vue) {
            VueControle.PLANT -> VuePlantEcran(e, ::agir, poche = poche, onPoche = {
                mode.entrer(SystemClock.uptimeMillis())
                sorties?.annoncer(AnnoncesPoche.entree(e.plant.numero))
            })
            VueControle.TERMINER -> VueTerminer(e, ::agir)
            VueControle.TERMINEE -> VueTerminee(e, ::agir, onRetour)
            VueControle.POINTER -> VuePointerEcran(e, tournee, reglages, ::agir)
        }
        e.message?.let { Text(it.texte, style = MaterialTheme.typography.bodyLarge, color = if (it.erreur) couleurErreur() else couleurNeutre()) }
        if (stockageRefuse) Text(stringResource(R.string.controle_stockage_refuse), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    }
}

@Composable
private fun VuePlantEcran(e: EcranControle, agir: (ControleurPlacette.() -> EcranControle) -> Unit, poche: Boolean, onPoche: () -> Unit) {
    val p = e.plant
    // 1. La seule chose en grand : le numéro du plant ; 2. son état ; 3. l'état d'avant, en petit.
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { agir { plantPrecedent() } }, enabled = p.aPrecedent, modifier = Modifier.size(DimensionsControle.FLECHE)) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.controle_plant_precedent))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(p.titre, style = MaterialTheme.typography.displaySmall, color = couleurNeutre())
            Text(p.compteur, style = MaterialTheme.typography.titleLarge, color = couleurNeutre())
        }
        IconButton(onClick = { agir { plantSuivant() } }, enabled = p.aSuivant, modifier = Modifier.size(DimensionsControle.FLECHE)) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.controle_plant_suivant))
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        p.etatDuJour?.let { PastilleForme(it, 32.dp); Spacer(Modifier.width(8.dp)) }
        Text(p.libelleEtat, style = MaterialTheme.typography.headlineMedium, color = couleurNeutre())
    }
    p.precedent?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
    }
    if (p.aucunPlant) {
        Text(stringResource(R.string.controle_aucun_plant), style = MaterialTheme.typography.bodyLarge, color = couleurNeutre())
    }
    // 4. Les quatre états, gros boutons : forme et mot.
    val actif = p.numero != null
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BoutonEtat(EtatDsf.SAIN, actif, Modifier.weight(1f)) { agir { dire(EtatDsf.SAIN) } }
        BoutonEtat(EtatDsf.ATTEINT, actif, Modifier.weight(1f)) { agir { dire(EtatDsf.ATTEINT) } }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BoutonEtat(EtatDsf.MORT, actif, Modifier.weight(1f)) { agir { dire(EtatDsf.MORT) } }
        BoutonEtat(EtatDsf.ABSENT, actif, Modifier.weight(1f)) { agir { dire(EtatDsf.ABSENT) } }
    }
    // Cause : seulement sur un plant atteint ou mort.
    if (p.causesVisibles) {
        Text(stringResource(R.string.controle_cause), style = MaterialTheme.typography.titleMedium, color = couleurNeutre())
        GroupeCause.entries.chunked(2).forEach { ligne ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ligne.forEach { g ->
                    val choisi = p.groupeCause == g
                    val m = Modifier.weight(1f).height(DimensionsControle.BOUTON_CAUSE)
                    if (choisi) {
                        FilledTonalButton(onClick = { agir { cause(g) } }, modifier = m) { Text(TextesControle.libelle(g), color = couleurNeutre()) }
                    } else {
                        OutlinedButton(onClick = { agir { cause(g) } }, modifier = m) { Text(TextesControle.libelle(g), color = couleurNeutre()) }
                    }
                }
            }
        }
        OutlinedButton(onClick = { agir { sansCause() } }, modifier = Modifier.fillMaxWidth().height(DimensionsControle.BOUTON_CAUSE)) {
            Text(stringResource(R.string.controle_sans_cause), color = couleurNeutre())
        }
    }
    // Regarni : seulement sur un plant mort ou absent (la session refuse les autres).
    if (p.regarniPossible) {
        OutlinedButton(onClick = { agir { regarni() } }, modifier = Modifier.fillMaxWidth().height(DimensionsControle.ACTION)) {
            Text(stringResource(R.string.controle_remplacer), color = couleurNeutre())
        }
    }
    // 5. Le plan à l'échelle : toucher un plant le choisit.
    PlanCanvas(e.plan, onChoisir = { id -> agir { choisir(id) } })
    // 6. Trois actions.
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { agir { ouvrirPointer() } }, modifier = Modifier.weight(1f).height(DimensionsControle.ACTION), contentPadding = PaddingValues(horizontal = 4.dp)) {
            Text(stringResource(R.string.controle_pointer), color = couleurNeutre())
        }
        OutlinedButton(onClick = { agir { annuler() } }, modifier = Modifier.weight(1f).height(DimensionsControle.ACTION), contentPadding = PaddingValues(horizontal = 4.dp)) {
            Text(stringResource(R.string.controle_annuler), color = couleurNeutre())
        }
        Button(onClick = { agir { demanderTerminer() } }, modifier = Modifier.weight(1f).height(DimensionsControle.ACTION), contentPadding = PaddingValues(horizontal = 4.dp)) {
            Text(stringResource(R.string.controle_terminer))
        }
    }
    // 7. Le mode poche : le téléphone au fond de la poche, les états au volume (voix et vibration : mission 012-C2).
    if (!poche) {
        OutlinedButton(onClick = onPoche, modifier = Modifier.fillMaxWidth().height(DimensionsControle.ACTION)) {
            Text(stringResource(R.string.controle_poche), color = couleurNeutre())
        }
    }
}

/**
 * La bande du mode poche : la seule chose à voir en poche, et comment en sortir. Texte `onSurface` cerné d'un filet (063) ;
 * [ligneVoix] garde la place de la ligne « Pas de voix française : regardez l'écran » de la 012-C2.
 */
@Composable
private fun BandePoche(ligneVoix: String? = null, annonce: AnnoncePoche? = null) {
    Column(
        Modifier.fillMaxWidth().border(2.dp, MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.small).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(R.string.controle_poche_bande), style = MaterialTheme.typography.titleMedium, color = couleurNeutre())
        Text(stringResource(R.string.controle_poche_aide), style = MaterialTheme.typography.bodyMedium, color = couleurNeutre())
        ligneVoix?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = couleurNeutre()) }
        // Sans voix française, l'annonce s'écrit ici (en grand : on la lit d'un coup d'œil) ; un refus en couleur d'erreur.
        annonce?.let { Text(it.texte, style = MaterialTheme.typography.titleLarge, color = if (it.erreur) couleurErreur() else couleurNeutre()) }
    }
}

@Composable
private fun BoutonEtat(etat: EtatDsf, actif: Boolean, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = actif, modifier = modifier.height(DimensionsControle.BOUTON_ETAT)) {
        PastilleForme(etat)
        Spacer(Modifier.width(8.dp))
        Text(TextesControle.libelle(etat), style = MaterialTheme.typography.titleLarge, color = couleurNeutre())
    }
}

@Composable
private fun VueTerminer(e: EcranControle, agir: (ControleurPlacette.() -> EcranControle) -> Unit) {
    Text(stringResource(R.string.controle_terminer_titre, e.codePlacette), style = MaterialTheme.typography.headlineMedium, color = couleurNeutre())
    Totaux(e)
    Button(onClick = { agir { terminer() } }, modifier = Modifier.fillMaxWidth().height(DimensionsControle.BOUTON_ETAT)) {
        Text(stringResource(R.string.controle_terminer), style = MaterialTheme.typography.titleLarge)
    }
    OutlinedButton(onClick = { agir { reprendre() } }, modifier = Modifier.fillMaxWidth().height(DimensionsControle.ACTION)) {
        Text(stringResource(R.string.controle_reprendre), color = couleurNeutre())
    }
}

@Composable
private fun VueTerminee(e: EcranControle, agir: (ControleurPlacette.() -> EcranControle) -> Unit, onRetour: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        PastilleDisque(Palette.Vert, 28.dp)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.controle_controlee, e.codePlacette), style = MaterialTheme.typography.headlineMedium, color = couleurNeutre())
    }
    Totaux(e)
    Button(onClick = onRetour, modifier = Modifier.fillMaxWidth().height(DimensionsControle.BOUTON_ETAT)) {
        Text(stringResource(R.string.controle_fermer), style = MaterialTheme.typography.titleLarge)
    }
    OutlinedButton(onClick = { agir { reprendre() } }, modifier = Modifier.fillMaxWidth().height(DimensionsControle.ACTION)) {
        Text(stringResource(R.string.controle_reprendre), color = couleurNeutre())
    }
}

@Composable
private fun Totaux(e: EcranControle) {
    val t = e.totaux
    if (t.refus != null) Text(t.refus, color = couleurErreur()) else Text(t.texte, style = MaterialTheme.typography.titleMedium, color = couleurNeutre())
}
