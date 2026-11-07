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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import io.github.pobsteta.surculus.core.session.Geste
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
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
import io.github.pobsteta.surculus.carte.uneSecondeArrivee
import io.github.pobsteta.surculus.carte.couleurErreur
import io.github.pobsteta.surculus.carte.couleurNeutre
import io.github.pobsteta.surculus.carte.rememberFix
import io.github.pobsteta.surculus.core.placette.EtatDsf
import io.github.pobsteta.surculus.core.placette.GroupeCause
import io.github.pobsteta.surculus.gpkg.ContexteDeTournee
import io.github.pobsteta.surculus.gpkg.RenduTournee
import io.github.pobsteta.surculus.gnss.FixPourSession
import io.github.pobsteta.surculus.tournee.TourneeOuverte
import io.github.pobsteta.surculus.gnss.SourcePosition
import io.github.pobsteta.surculus.core.gnss.FixGnss
import io.github.pobsteta.surculus.core.session.QualiteFix
import io.github.pobsteta.surculus.core.session.Besoins
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
 * Choix de l'UG en attente, par tournée et placette (mission 086, reprise 1/3) : survit à la rotation avec les positions de
 * la moyenne, pour que tourner l'écran pendant le choix ne relance pas la moyenne de 5 s. Libéré en quittant vraiment l'écran,
 * au choix et à « Refaire la mesure ».
 */
object ChoixUgEnAttente {
    private val attente = HashMap<String, Pair<EtatArrivee.UgAChoisir, List<FixGnss>>>()

    @Synchronized fun get(cle: String): Pair<EtatArrivee.UgAChoisir, List<FixGnss>>? = attente[cle]

    @Synchronized fun poser(cle: String, etat: EtatArrivee.UgAChoisir, fixes: List<FixGnss>) { attente[cle] = etat to fixes }

    @Synchronized fun liberer(cle: String) { attente.remove(cle) }
}

/**
 * Arrivée sans position en attente, par tournée et placette (mission 087) : début de l'attente sans fix (horloge monotone),
 * et, une fois « Ouvrir sans position » demandé, l'étape en cours (choix de l'UG, puis confirmation) et l'UG choisie.
 * Survit à la rotation, comme [ChoixUgEnAttente] ; libéré en quittant vraiment l'écran, à l'ouverture et quand un fix arrive.
 */
object SansPositionEnAttente {
    class Etat(var debutMs: Long? = null, var etape: EtatArrivee? = null, var ugChoisie: String? = null)

    private val attente = HashMap<String, Etat>()

    @Synchronized fun de(cle: String): Etat = attente.getOrPut(cle) { Etat() }

    @Synchronized fun etape(cle: String): EtatArrivee? = attente[cle]?.etape

    @Synchronized fun liberer(cle: String) { attente.remove(cle) }
}

/**
 * Écran du contrôle d'une placette (mission 012-B1, spec § 4.3). Une question : « sur quel plant suis-je, dans quel état,
 * combien en reste-t-il ? » En grand : « Plant n° 7 » et son état. Une comparaison : « 7 sur 12 », l'état d'avant en petit.
 * Source et jour dans le bandeau. Tout ce que l'agent dit est écrit au journal de la tournée tout de suite : sortir sans
 * « Terminer » ne perd rien, la placette reste « à faire ».
 */
@Composable
fun EcranControle(id: String, placetteUuid: String, codeInitial: String = "", onRetour: () -> Unit, onParametres: () -> Unit = {}) {
    val ctx = LocalContext.current
    val chargement = chargerTournee(id)
    val (fix, _) = rememberFix()
    val ouverte = (chargement as? Chargement.Pret)?.tournee
    val placette = ouverte?.tournee?.placettes?.firstOrNull { it.uuid == placetteUuid }
    val cle = ControleursOuverts.cle(id, placetteUuid)
    // Mission 090 : une tournée rendue est en lecture seule (décision de Pascal du 2026-11-03) ; aucun contrôle ne s'ouvre,
    // rien n'est écrit au journal. On la rouvre depuis la liste de ses placettes.
    val rendue = remember(ouverte) { ouverte != null && RenduTournee.estRendue(ouverte.dossier.toPath()) }

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
    DisposableEffect(cle) { onDispose { if (activite?.isChangingConfigurations != true) { ControleursOuverts.liberer(cle); ChoixUgEnAttente.liberer(cle); SansPositionEnAttente.liberer(cle) } } }

    var arrivee by remember(cle) { mutableStateOf<EtatArrivee>(ControleursOuverts.get(cle)?.let { EtatArrivee.Ouverte(it) } ?: ChoixUgEnAttente.get(cle)?.first ?: SansPositionEnAttente.etape(cle) ?: EtatArrivee.Moyenne(0)) }
    var essai by rememberSaveable(cle) { mutableIntStateOf(0) }
    var code by rememberSaveable(cle) { mutableStateOf(codeInitial) }
    val fixActuel = rememberUpdatedState(fix)
    // Les fix de l'arrivée sont gardés : le choix de l'UG rouvre avec la même position, sans refaire la moyenne.
    // Les fix entiers (mission 091) : leur position, et ce qu'ils disent d'eux-mêmes (précision, source, simulée).
    var fixesArrivee by remember(cle) { mutableStateOf(ChoixUgEnAttente.get(cle)?.second ?: emptyList()) }
    val scope = rememberCoroutineScope()
    suspend fun ouvrirArrivee(ouverte: TourneeOuverte, fixes: List<FixGnss>, ugChoisie: String?): EtatArrivee {
        val resultat = withContext(Dispatchers.IO) {
            runCatching {
                ArriveeControle.ouvrir(
                    contexte = ContexteDeTournee.de(ouverte.tournee),
                    journal = SauvegardeApp.journal(ctx, LigneTournee(ouverte.dossier, ouverte.tournee, null)),
                    horloge = SauvegardeApp.horloge(ctx) { FixPourSession.depuis(SourcePosition.fix.value) },
                    essences = ArriveeControle.essencesDe(ouverte.tournee),
                    positions = fixes.map { it.position },
                    codeLu = code,
                    sauvegarde = SauvegardeApp.ecoute(ctx, LigneTournee(ouverte.dossier, ouverte.tournee, null)),
                    ugChoisie = ugChoisie,
                    qualite = QualiteFix.deFix(fixes),
                )
            }
        }
        val v = resultat.getOrNull()
        if (v is EtatArrivee.Ouverte) ControleursOuverts.poser(cle, v.controleur)
        if (v is EtatArrivee.UgAChoisir) ChoixUgEnAttente.poser(cle, v, fixes) else ChoixUgEnAttente.liberer(cle)
        return v ?: EtatArrivee.SansPosition
    }

    /** « Ouvrir sans position » (mission 087) : choix de l'UG si le code se répète, puis confirmation, puis ouverture. */
    suspend fun ouvrirSansPosition(ouverte: TourneeOuverte, ugChoisie: String?, confirmee: Boolean): EtatArrivee {
        // L'habillage de l'appel est partagé avec le guidage (mission 089) ; l'écran ne garde ici que l'étape en cours.
        val v = OuvertureSansPosition.ouvrir(ctx, ouverte, code, ugChoisie, confirmee)
        val attente = SansPositionEnAttente.de(cle)
        when (v) {
            is EtatArrivee.Ouverte -> { ControleursOuverts.poser(cle, v.controleur); SansPositionEnAttente.liberer(cle) }
            is EtatArrivee.UgAChoisir, is EtatArrivee.ConfirmerSansPosition -> { attente.etape = v; attente.ugChoisie = ugChoisie }
            else -> { attente.etape = null; attente.ugChoisie = null }
        }
        return v
    }

    // Un fix reçu pendant le choix de l'UG ou la confirmation sans position : on abandonne et on refait la moyenne (ADR 0003,
    // amendement du 2026-11-02, point 6 ; le point 10 ne vaut qu'après l'ouverture ; décision de Pascal du 2026-11-03).
    val enSansPosition = SansPositionEnAttente.etape(cle) != null && (arrivee is EtatArrivee.ConfirmerSansPosition || arrivee is EtatArrivee.UgAChoisir)
    LaunchedEffect(cle, enSansPosition, fix != null) {
        if (enSansPosition && fix != null) {
            SansPositionEnAttente.liberer(cle)
            arrivee = EtatArrivee.Moyenne(0)
            essai++
        }
    }

    // L'arrivée (décisions de Pascal du 2026-10-27) : la moyenne de 5 s part toute seule, avec son décompte, puis la règle des 15 m.
    LaunchedEffect(cle, essai, ouverte) {
        if (ouverte == null || rendue || ControleursOuverts.get(cle) != null || ChoixUgEnAttente.get(cle) != null || SansPositionEnAttente.etape(cle) != null) return@LaunchedEffect
        // L'attente sans fix (30 s, mission 087) court dès la première moyenne sans position.
        SansPositionEnAttente.de(cle).let { if (it.debutMs == null) it.debutMs = SystemClock.elapsedRealtime() }
        // La moyenne de 5 s est celle du guidage : écrite une seule fois (`collecterPositionsArrivee`).
        val fixes = ArrayList<FixGnss>()
        collecterPositionsArrivee(position = { fixActuel.value?.also { fixes += it }?.position }, surSeconde = { n -> arrivee = EtatArrivee.Moyenne(n) })
        fixesArrivee = fixes
        // Un fix reçu : l'attente sans fix repart de zéro la prochaine fois.
        if (fixes.isNotEmpty()) SansPositionEnAttente.de(cle).debutMs = null
        arrivee = ouvrirArrivee(ouverte, fixes, null)
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
                rendue -> Texte(stringResource(R.string.controle_tournee_rendue))
                else -> when (val a = arrivee) {
                    is EtatArrivee.Moyenne -> VueArrivee(a, code, onCode = { code = it }, onRecommencer = null)
                    is EtatArrivee.Refus -> VueArrivee(a, code, onCode = { code = it }, onRecommencer = { arrivee = EtatArrivee.Moyenne(0); essai++ })
                    EtatArrivee.SansPosition -> VueSansFix(
                        debutMs = SansPositionEnAttente.de(cle).debutMs ?: SystemClock.elapsedRealtime(),
                        fixRecu = fix != null,
                        code = code, onCode = { code = it },
                        onOuvrir = { scope.launch { arrivee = ouvrirSansPosition(ouverte!!, null, confirmee = false) } },
                        onRecommencer = { arrivee = EtatArrivee.Moyenne(0); essai++ },
                    )
                    is EtatArrivee.UgAChoisir -> if (SansPositionEnAttente.etape(cle) != null) {
                        VueChoixUg(a, onChoix = { ug -> scope.launch { arrivee = ouvrirSansPosition(ouverte!!, ug, confirmee = false) } }, onRecommencer = { SansPositionEnAttente.de(cle).etape = null; arrivee = EtatArrivee.SansPosition })
                    } else {
                        VueChoixUg(a, onChoix = { ug -> scope.launch { arrivee = ouvrirArrivee(ouverte!!, fixesArrivee, ug) } }, onRecommencer = { ChoixUgEnAttente.liberer(cle); arrivee = EtatArrivee.Moyenne(0); essai++ })
                    }
                    is EtatArrivee.ConfirmerSansPosition -> VueConfirmerSansPosition(
                        a,
                        onOuvrir = { scope.launch { arrivee = ouvrirSansPosition(ouverte!!, SansPositionEnAttente.de(cle).ugChoisie, confirmee = true) } },
                        onAnnuler = { SansPositionEnAttente.de(cle).let { it.etape = null; it.ugChoisie = null }; arrivee = EtatArrivee.SansPosition },
                    )
                    is EtatArrivee.Ouverte -> VueSession(a.controleur, id, reglages, stockageRefuse, onRetour, onParametres)
                }
            }
        }
    }
}

@Composable
private fun Texte(texte: String, erreur: Boolean = false) {
    Text(texte, color = if (erreur) couleurErreur() else couleurNeutre(), modifier = Modifier.padding(24.dp))
}

/** Arrivée : le décompte de la moyenne, ou le refus en clair (sans son motif) avec le champ « Code lu sur le piquet » et le bouton qui suit ce champ. */
@Composable
private fun VueArrivee(etat: EtatArrivee, code: String, onCode: (String) -> Unit, onRecommencer: (() -> Unit)?) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (etat) {
            is EtatArrivee.Moyenne -> {
                Text(stringResource(R.string.controle_moyenne, etat.n, etat.total), style = MaterialTheme.typography.headlineMedium, color = couleurNeutre())
                Text(stringResource(R.string.controle_moyenne_aide), style = MaterialTheme.typography.bodyMedium, color = couleurNeutre())
            }
            is EtatArrivee.Refus -> Text(TextesControle.message(etat.refus)!!.texte, style = MaterialTheme.typography.titleMedium, color = couleurErreur())
            // L'attente sans position a sa propre vue (`VueSansFix`, mission 087) : cette vue n'est jamais appelée pour elle.
            EtatArrivee.SansPosition, is EtatArrivee.UgAChoisir, is EtatArrivee.Ouverte, is EtatArrivee.ConfirmerSansPosition -> {}
        }
        OutlinedTextField(
            value = code,
            onValueChange = onCode,
            label = { Text(stringResource(R.string.guidage_code)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        onRecommencer?.let {
            Button(onClick = it, modifier = Modifier.fillMaxWidth().height(DimensionsControle.ACTION)) { Text(TextesControle.boutonArrivee(code)) }
        }
    }
}

/**
 * Aucune position (mission 087 ; ADR 0003, amendement du 2026-11-02, point 1) : l'attente de 30 s sans fix, puis
 * « Ouvrir sans position », grisé tant que le champ du code est vide. Si rien n'écoute (localisation coupée, permission
 * refusée, aucun récepteur branché) : on demande de l'activer, jamais d'ouverture sans position. Un fix reçu : la moyenne
 * repart (c'est une position, point 6).
 */
@Composable
private fun VueSansFix(debutMs: Long, fixRecu: Boolean, code: String, onCode: (String) -> Unit, onOuvrir: () -> Unit, onRecommencer: () -> Unit) {
    val ctx = LocalContext.current
    var maintenant by remember { mutableStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) {
        while (true) {
            maintenant = SystemClock.elapsedRealtime()
            uneSecondeArrivee()
        }
    }
    // « Récepteur actif » et le délai : la règle de la 087, lue par l'habillage partagé avec le guidage (mission 089).
    val suite = OuvertureSansPosition.suite(ctx, msSansFix = maintenant - debutMs, fixRecu = fixRecu)
    val relancer = rememberUpdatedState(onRecommencer)
    LaunchedEffect(suite) { if (suite == SuiteSansFix.POSITION_RECUE) relancer.value() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Titre « Recherche de la position… » pendant le décompte seulement, en couleur normale (décisions de Pascal du 2026-11-03).
        if (ArriveeSansPosition.titreVisible(suite)) {
            Text(stringResource(R.string.controle_sans_position), style = MaterialTheme.typography.titleMedium, color = couleurNeutre())
        }
        AttenteSansFix(suite, ((maintenant - debutMs) / 1000).toInt(), code, onCode, onOuvrir)
        OutlinedButton(onClick = onRecommencer, modifier = Modifier.fillMaxWidth().height(DimensionsControle.ACTION)) {
            Text(stringResource(R.string.controle_ug_recommencer))
        }
    }
}

/** La placette à confirmer avant d'ouvrir sans position (point 4) : rien n'est ouvert tant que l'agent n'a pas confirmé. */
@Composable
private fun VueConfirmerSansPosition(etat: EtatArrivee.ConfirmerSansPosition, onOuvrir: () -> Unit, onAnnuler: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ConfirmationSansPosition(etat, onOuvrir, onAnnuler)
    }
}

/**
 * Le code lu existe dans plusieurs UG et la position ne tranche pas : une ligne par candidate, un gros bouton chacune
 * (gants, soleil, une main). Rien n'est ouvert tant que l'agent n'a pas choisi ; aucune n'est présélectionnée.
 */
@Composable
private fun VueChoixUg(etat: EtatArrivee.UgAChoisir, onChoix: (String) -> Unit, onRecommencer: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.controle_ug_question, etat.code), style = MaterialTheme.typography.headlineMedium, color = couleurNeutre())
        Text(stringResource(R.string.controle_ug_aide), style = MaterialTheme.typography.bodyLarge, color = couleurNeutre())
        for (c in etat.candidats) {
            val ug = c.ug
            Button(
                onClick = { onChoix(ug.orEmpty()) },
                enabled = ug != null,
                modifier = Modifier.fillMaxWidth().height(DimensionsControle.GRAND_CHOIX),
            ) { Text(stringResource(R.string.controle_ug_choix, c.code, ug ?: "?"), style = MaterialTheme.typography.titleLarge) }
        }
        OutlinedButton(onClick = onRecommencer, modifier = Modifier.fillMaxWidth().height(DimensionsControle.ACTION)) {
            Text(stringResource(R.string.controle_ug_recommencer))
        }
    }
}

/** La session ouverte : la vue Plant, la confirmation de fin, ou « Contrôlée ». */
@Composable
internal fun VueSession(controleur: ControleurPlacette, tournee: String, reglages: ReglagesControle, stockageRefuse: Boolean, onRetour: () -> Unit, onParametres: () -> Unit) {
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
    // Permission du micro (012-D2, plan § 6) : demandée à l'appui sur « Mode poche » et sur le micro, l'écran sous les yeux,
    // jamais en poche. Refusée, le mode poche entre quand même : la saisie au volume ne dépend jamais du micro.
    var apresPermission by remember { mutableStateOf<(() -> Unit)?>(null) }
    val demanderMicro = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { accordee ->
        EcouteApp.permissionRefusee = if (accordee) null
        else PermissionMicro.apresDemande(false, activite?.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) == true)
        rafraichir++
        apresPermission?.invoke()
        apresPermission = null
    }
    DisposableEffect(controleur) {
        poche = mode.actif
        val desabonner = mode.abonner { actif, raison ->
            poche = actif
            if (!actif) { EcouteApp.arreterEtOublier(); sorties?.effacer(); rafraichir++ }
            if (!actif && raison != RaisonSortie.VUE_QUITTEE) sorties?.annoncer(AnnoncesPoche.sortie())
            val annonce = raison?.let { TextesControle.sortiePoche(it) }
            if (!actif && annonce != null) scope.launch { verrou.withLock { ecran = withContext(Dispatchers.IO) { controleur.annoncer(annonce) } } }
        }
        // Les gestes de volume arrivent dans l'ordre et s'appliquent un par un.
        sorties?.surTexte = { rafraichir++ }
        // Les phrases de la dictée passent par le même verrou que les gestes : l'une après l'autre, dans l'ordre.
        EcouteApp.brancher { phrase ->
            scope.launch {
                verrou.withLock {
                    var annonce: AnnoncePoche? = null
                    ecran = withContext(Dispatchers.IO) { controleur.dicter(phrase).also { annonce = controleur.derniereAnnonce } }
                    annonce?.let { EcouteApp.resultat(it) }
                }
            }
        }
        PocheApp.pont.surRappel = { sorties?.annoncer(AnnoncesPoche.rappel()) }
        PocheApp.pont.ecoute = { sortie ->
            sorties?.signal(sortie)
            when (sortie) {
                // Volume bas tenu 0,5 s : le micro s'ouvre ; il se ferme quand la touche est relâchée (tenir pour parler).
                is SortieClasseur.Geste -> if (sortie.geste == Geste.VOLUME_BAS_LONG) EcouteApp.tenir() else scope.launch {
                    EcouteApp.effacerLignes()
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
                SortieClasseur.BasRelacheApresLong -> EcouteApp.relacher()
                SortieClasseur.Tic, SortieClasseur.Bourdon -> {}
            }
        }
        onDispose {
            desabonner()
            EcouteApp.brancher(null)
            if (activite?.isChangingConfigurations == true) EcouteApp.arreter() else EcouteApp.arreterEtOublier()
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
        val ctx = LocalContext.current
        EcouteApp.version // lu pour que la bande se redessine à chaque changement de l'écoute
        val ligneDictee = TextesEcoute.ligneEtat(EcouteApp.etatPermission(ctx), EcouteApp.modeleInstalle(ctx))
        if (poche) {
            rafraichir // lu pour que la bande se redessine à chaque annonce écrite
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                BandePoche(
                    ligneVoix = if (sorties?.sansVoixFrancaise != false) AnnoncesPoche.BANDEAU_SANS_VOIX else null,
                    // Le résultat de la dictée est déjà dans le bloc d'écoute : pas de doublon avec l'annonce écrite.
                    annonce = sorties?.annonceEcrite.takeIf { EcouteApp.compris == null },
                    ligneDictee = ligneDictee,
                )
            }
        }
        // Mission 075 : le micro est HORS de la zone qui défile (pied debout, colonne couché) ; le bloc J'ÉCOUTE est posé PAR-DESSUS
        // (couché : le haut de la zone, étendu à droite sur la colonne du micro ; debout : du pied, vers le haut, 083). Rien ne pousse rien : le micro et les boutons ne bougent pas quand l'écoute commence.
        val config = LocalConfiguration.current
        val emplacement = DispositionMicro.emplacement(config.screenWidthDp.toFloat(), config.screenHeightDp.toFloat())
        val avecMicro = e.vue == VueControle.PLANT && !poche
        val surPoche: () -> Unit = {
            val entrer: () -> Unit = {
                mode.entrer(SystemClock.uptimeMillis())
                sorties?.annoncer(AnnoncesPoche.entree(e.plant.numero))
            }
            if (EcouteApp.modeleInstalle(ctx) && PermissionMicro.aDemander(EcouteApp.etatPermission(ctx))) {
                apresPermission = entrer
                demanderMicro.launch(Manifest.permission.RECORD_AUDIO)
            } else entrer()
        }
        val surMicro: () -> Unit = {
            if (EcouteApp.modeleInstalle(ctx) && PermissionMicro.aDemander(EcouteApp.etatPermission(ctx))) {
                apresPermission = null
                demanderMicro.launch(Manifest.permission.RECORD_AUDIO)
            } else EcouteApp.tenir()
        }
        val micro: @Composable () -> Unit = {
            BoutonMicro(pret = AspectMicro.pret(EcouteApp.etatPermission(ctx), EcouteApp.modeleInstalle(ctx)), onAppui = surMicro, onRelache = { EcouteApp.relacher() })
        }
        val zone: @Composable (Modifier) -> Unit = { m ->
            Box(m) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Couché, « Mode poche » est en tête de la zone qui défile : la colonne du micro n'a de place que pour le micro.
                    if (avecMicro && emplacement == EmplacementMicro.COLONNE) BoutonPoche(surPoche, Modifier.fillMaxWidth())
                    Text(stringResource(R.string.controle_source, e.codePlacette, e.jour), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (e.plant.alerteTerminee) {
                        Text(stringResource(R.string.controle_alerte_terminee), style = MaterialTheme.typography.titleSmall, color = couleurErreur())
                    }
                    when (e.vue) {
                        VueControle.PLANT -> VuePlantEcran(e, ::agir, poche = poche, ligneDictee = ligneDictee, actionsDictee = ActionsDictee.pour(EcouteApp.etatPermission(ctx), EcouteApp.modeleInstalle(ctx)), onActionDictee = { a ->
                            when (a) {
                                ActionDictee.REGLAGES -> ouvrirReglagesApplication(ctx)
                                ActionDictee.INSTALLER -> onParametres()
                            }
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
        // La disposition ne dépend que de l'écran (couché ou debout), jamais de la vue : la zone garde sa place dans la composition
        // en passant de Plant à Pointer ou Terminer, et le micro n'est qu'un frère conditionnel à côté d'elle.
        if (emplacement == EmplacementMicro.COLONNE) {
            // Couché (083, reprise 2/3, décision « Bloc plus large, à droite ») : le bloc est un frère de la zone et de la colonne du micro,
            // dans un Box qui les contient. Il part du haut de la zone et s'étend vers la droite, par-dessus le haut de la colonne du micro
            // (vide : le micro est en bas), de la largeur de l'écran moins 16 dp de chaque côté ; « entendu » et « compris » y tiennent sur
            // une ligne. Il reste sous « Mode poche » et au-dessus de la flèche et du numéro. Largeur et marges : PROVISOIRES jusqu'à l'essai ganté.
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Row(Modifier.fillMaxSize()) {
                    zone(Modifier.weight(1f).fillMaxHeight())
                    if (avecMicro) {
                        Column(Modifier.width(DimensionsMicro.ZONE).fillMaxHeight().padding(8.dp), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) { micro() }
                    }
                }
                if (!poche && blocEcouteVisible()) {
                    Box(Modifier.align(Alignment.TopStart).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) { BlocEcouteEncadre() }
                }
            }
        } else {
            // Debout (083) : le bloc d'écoute est un frère du pied et de la zone, dans un Box qui les contient : il part du bas du pied, à
            // gauche du micro, et monte sur le bas de la zone qui défile (débord vers le haut). Il ne pousse rien : le Column dessous ne
            // dépend pas de lui.
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.fillMaxSize()) {
                    zone(Modifier.weight(1f).fillMaxWidth())
                    if (avecMicro) {
                        Row(Modifier.fillMaxWidth().height(DimensionsMicro.ZONE).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) { BoutonPoche(surPoche, Modifier.fillMaxWidth()) }
                            Spacer(Modifier.width(12.dp))
                            micro()
                        }
                    }
                }
                // Même cadre que le pied : 16 dp de chaque côté, 8 dp sous, micro (112 dp) et 12 dp d'écart à droite. Largeur et pied
                // : valeurs PROVISOIRES jusqu'à l'essai ganté. La hauteur est celle du texte : le bloc monte de ce qu'il lui faut.
                if (avecMicro && !poche && blocEcouteVisible()) {
                    Box(
                        Modifier.align(Alignment.BottomStart).fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp + DimensionsMicro.BOUTON + 12.dp, bottom = 8.dp),
                    ) { BlocEcouteEncadre(cote = false) }
                }
            }
        }
    }
}

@Composable
private fun VuePlantEcran(
    e: EcranControle, agir: (ControleurPlacette.() -> EcranControle) -> Unit, poche: Boolean, ligneDictee: String?, actionsDictee: List<ActionDictee>, onActionDictee: (ActionDictee) -> Unit,
) {
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
    // Un « remplace » dicté en poche attend ici : à la sortie du mode, la vue propose de pointer le regarni (012-D2, Q8).
    e.pointage.regarniDe?.let { n ->
        if (!poche) {
            Text(stringResource(R.string.controle_regarni_propose, n), style = MaterialTheme.typography.titleMedium, color = couleurNeutre())
            Button(onClick = { agir { ouvrirPointer() } }, modifier = Modifier.fillMaxWidth().height(DimensionsControle.BOUTON_ETAT)) {
                Text(stringResource(R.string.controle_regarni_pointer), style = MaterialTheme.typography.titleLarge)
            }
        }
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
    // 7. Le mode poche et le micro sont hors de cette zone qui défile (mission 075) : pied fixe debout, colonne fixe couché.
    if (!poche) {
        ligneDictee?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        // Sous le message : le bouton qui mène où l'on répare (jamais en poche, le toucher y est ignoré).
        actionsDictee.forEach { a ->
            OutlinedButton(onClick = { onActionDictee(a) }, modifier = Modifier.fillMaxWidth().height(DimensionsControle.ACTION)) {
                Text(stringResource(if (a == ActionDictee.REGLAGES) R.string.controle_dictee_reglages else R.string.controle_dictee_installer), color = couleurNeutre())
            }
        }
    }
}

/**
 * La bande du mode poche : la seule chose à voir en poche, et comment en sortir. Texte `onSurface` cerné d'un filet (063) ;
 * [ligneVoix] garde la place de la ligne « Pas de voix française : regardez l'écran » de la 012-C2.
 */
@Composable
private fun BandePoche(ligneVoix: String? = null, annonce: AnnoncePoche? = null, ligneDictee: String? = null) {
    Column(
        Modifier.fillMaxWidth().border(2.dp, MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.small).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(R.string.controle_poche_bande), style = MaterialTheme.typography.titleMedium, color = couleurNeutre())
        // Une seule idée à la fois : pendant l'écoute, le bloc J'ÉCOUTE prend la place de l'aide de sortie.
        if (!blocEcouteEnPoche()) Text(stringResource(R.string.controle_poche_aide), style = MaterialTheme.typography.bodyMedium, color = couleurNeutre())
        BlocEcoute()
        ligneDictee?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = couleurErreur()) }
        ligneVoix?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = couleurNeutre()) }
        // Sans voix française, l'annonce s'écrit ici (en grand : on la lit d'un coup d'œil) ; un refus en couleur d'erreur.
        annonce?.let { Text(it.texte, style = MaterialTheme.typography.titleLarge, color = if (it.erreur) couleurErreur() else couleurNeutre()) }
    }
}

private fun blocEcouteVisible(): Boolean = EcouteApp.bandeauVisible()

/** En mode poche, la bande est à part : elle ne s'efface pas (mission 076). */
private fun blocEcouteEnPoche(): Boolean = EcouteApp.affichage != AffichageEcoute.RIEN || EcouteApp.compris != null

/**
 * L'écoute : en grand « J'ÉCOUTE » / « ÉCOUTE FERMÉE », puis la comparaison entendu / compris (ou refusé, en couleur d'erreur) : le
 * forestier compare ce qu'il a dit à ce que la machine a retenu (plan § 2). Avec [cote], « entendu » et « compris » partagent une
 * ligne (couché : large mais bas, pour ne pas descendre sur « Plant précédent », 083) ; sinon l'un sous l'autre.
 */
@Composable
private fun BlocEcoute(cote: Boolean = false) {
    EcouteApp.version // lu pour se redessiner à chaque changement
    val grand = TextesEcoute.grand(EcouteApp.affichage)
    val entendu = EcouteApp.entendu
    val compris = EcouteApp.compris
    val titre: @Composable () -> Unit = { grand?.let { Text(it, style = MaterialTheme.typography.headlineMedium, color = couleurNeutre()) } }
    val ligneEntendu: @Composable (Modifier) -> Unit = { m -> entendu?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = couleurNeutre(), modifier = m) } }
    val ligneCompris: @Composable (Modifier) -> Unit = { m ->
        compris?.let {
            val erreur = EcouteApp.comprisErreur
            Text(TextesEcoute.compris(it, erreur), style = MaterialTheme.typography.titleMedium, color = if (erreur) couleurErreur() else couleurNeutre(), modifier = m)
        }
    }
    if (cote && entendu != null && compris != null) {
        // Deux colonnes : à gauche le titre et « entendu » (une ligne), à droite « compris » ou le refus, libre de prendre 2 ou 3 lignes
        // sans descendre plus bas que la colonne de gauche (083, reprise 2/3).
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(4.dp)) { titre(); ligneEntendu(Modifier) }
            ligneCompris(Modifier.weight(1f))
        }
    } else { titre(); ligneEntendu(Modifier); ligneCompris(Modifier) }
}

/**
 * Hors du mode poche, le même bloc dans son cadre, sur fond opaque, posé par-dessus ce qu'il couvre (075, 083) : debout, du pied
 * (à gauche du micro) vers le haut ; couché, en tête de la zone et étendu à droite sur la colonne du micro. Il **absorbe le toucher** (décision de Pascal du 2026-11-02) : pendant
 * l'écoute et les 6 s, un appui sur lui ne déclenche aucun bouton caché dessous. [cote] : voir [BlocEcoute].
 */
@Composable
private fun BlocEcouteEncadre(cote: Boolean = true) {
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
            .border(2.dp, MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.small)
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) { BlocEcoute(cote) }
}

/** « Mode poche », hors de la zone qui défile (debout : à gauche du micro, dans le pied ; couché : en tête de la zone). */
@Composable
private fun BoutonPoche(onPoche: () -> Unit, modifier: Modifier) {
    OutlinedButton(onClick = onPoche, modifier = modifier.height(DimensionsControle.ACTION), contentPadding = PaddingValues(horizontal = 4.dp)) {
        Text(stringResource(R.string.controle_poche), color = couleurNeutre())
    }
}

/**
 * Le micro hors du mode poche, comme celui de Marculus : rond de 112 dp, icône de 48 dp, rouge pendant l'écoute, barré quand le micro
 * ou le modèle manquent (il reste actif : le toucher demande la permission). On le **tient** pour parler, on le lâche pour finir ;
 * piloté par les interactions du bouton, un doigt qui glisse hors du bouton (Cancel) ferme comme un relâchement, et le bouton qui
 * disparaît aussi.
 */
@Composable
private fun BoutonMicro(pret: Boolean, onAppui: () -> Unit, onRelache: () -> Unit) {
    EcouteApp.version // lu pour changer d'aspect pendant l'écoute
    // Calculé APRÈS cette lecture, dans ce composable : c'est lui qui se redessine quand l'écoute change (rouge à l'écoute).
    val etat = AspectMicro.pour(EcouteApp.affichage, pret)
    val interactions = remember { MutableInteractionSource() }
    val appui by rememberUpdatedState(onAppui)
    val relache by rememberUpdatedState(onRelache)
    LaunchedEffect(interactions) {
        interactions.interactions.collect { i ->
            when (i) {
                is PressInteraction.Press -> appui()
                is PressInteraction.Release, is PressInteraction.Cancel -> relache()
                else -> Unit
            }
        }
    }
    DisposableEffect(Unit) { onDispose { relache() } }
    val libelle = stringResource(if (etat == EtatMicro.ECOUTE) R.string.controle_micro_ecoute else R.string.controle_micro)
    // UN SEUL bouton, jamais deux composables selon l'état : changer de composable sous le doigt enverrait un Cancel et
    // fermerait l'écoute aussitôt. Seuls les couleurs, l'icône (Mic / MicOff) et la description (lue par TalkBack) changent.
    val s = MaterialTheme.colorScheme
    Button(
        onClick = {},
        interactionSource = interactions,
        modifier = Modifier.size(DimensionsMicro.BOUTON),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        border = BorderStroke(2.dp, CouleursMicro.filet(s)),
        colors = ButtonDefaults.buttonColors(containerColor = CouleursMicro.fond(etat, s), contentColor = CouleursMicro.icone(etat, s)),
    ) { Icon(if (etat == EtatMicro.BARRE) Icons.Filled.MicOff else Icons.Filled.Mic, contentDescription = libelle, modifier = Modifier.size(DimensionsMicro.ICONE)) }
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
    ChoixBesoinEtConcurrence(e.besoin, agir)
    // Grisé tant qu'il manque un choix (décision de Pascal du 2026-11-04, question 1), avec la phrase de ce qui manque.
    Button(onClick = { agir { terminer() } }, enabled = e.besoin.terminerPermis, modifier = Modifier.fillMaxWidth().height(DimensionsControle.BOUTON_ETAT)) {
        Text(stringResource(R.string.controle_terminer), style = MaterialTheme.typography.titleLarge)
    }
    e.besoin.manque?.let { Text(it, style = MaterialTheme.typography.titleMedium, color = couleurNeutre()) }
    OutlinedButton(onClick = { agir { reprendre() } }, modifier = Modifier.fillMaxWidth().height(DimensionsControle.ACTION)) {
        Text(stringResource(R.string.controle_reprendre), color = couleurNeutre())
    }
}

/**
 * Le besoin et la concurrence, à l'écran de « Terminer » (mission 091) : un grand bouton « Aucun », la grille des codes de
 * la couche `codes` (4 par ligne, code en gros et début du libellé, environ 80 dp : faisable avec des gants), puis
 * faible, moyenne, forte. Le choix fait est plein, les autres cernés ; aucun n'est choisi d'avance.
 */
@Composable
private fun ChoixBesoinEtConcurrence(b: VueBesoin, agir: (ControleurPlacette.() -> EcranControle) -> Unit) {
    Text(stringResource(R.string.controle_besoin_titre), style = MaterialTheme.typography.titleLarge, color = couleurNeutre())
    BoutonChoix(b.aucunChoisi, Modifier.fillMaxWidth().height(DimensionsControle.BOUTON_ETAT), { agir { direBesoin(Besoins.AUCUN) } }) {
        Text(stringResource(R.string.controle_besoin_aucun), style = MaterialTheme.typography.titleLarge)
    }
    for (ligne in b.codes.chunked(DimensionsControle.CODES_PAR_LIGNE)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (c in ligne) {
                BoutonChoix(c.choisi, Modifier.weight(1f).height(DimensionsControle.BOUTON_CODE), { agir { direBesoin(c.code) } }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(c.code, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                        if (c.libelleCourt.isNotEmpty()) Text(c.libelleCourt, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                    }
                }
            }
            repeat(DimensionsControle.CODES_PAR_LIGNE - ligne.size) { Spacer(Modifier.weight(1f)) }
        }
    }
    Text(stringResource(R.string.controle_concurrence_titre), style = MaterialTheme.typography.titleLarge, color = couleurNeutre())
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (c in b.concurrences) {
            BoutonChoix(c.choisi, Modifier.weight(1f).height(DimensionsControle.BOUTON_ETAT), { agir { direConcurrence(c.niveau) } }) {
                Text(c.libelle, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            }
        }
    }
}

/** Un bouton de choix : plein s'il est choisi, cerné sinon (le choix se voit au soleil sans dépendre de la seule couleur). */
@Composable
private fun BoutonChoix(choisi: Boolean, modifier: Modifier, onClick: () -> Unit, contenu: @Composable () -> Unit) {
    val padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 4.dp)
    if (choisi) {
        Button(onClick = onClick, modifier = modifier, contentPadding = padding) { contenu() }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier, contentPadding = padding) { contenu() }
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

/** Ouvre les réglages Android de Surculus (autorisations), quand le micro est refusé pour de bon. */
private fun ouvrirReglagesApplication(ctx: android.content.Context) {
    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", ctx.packageName, null))
        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    try { ctx.startActivity(intent) } catch (_: Exception) { /* pas d'écran de réglages : le message reste */ }
}
