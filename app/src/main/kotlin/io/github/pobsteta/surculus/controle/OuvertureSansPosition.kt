/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.controle

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.pobsteta.surculus.R
import io.github.pobsteta.surculus.carte.couleurErreur
import io.github.pobsteta.surculus.carte.couleurNeutre
import io.github.pobsteta.surculus.gnss.FixPourSession
import io.github.pobsteta.surculus.gnss.SourcePosition
import io.github.pobsteta.surculus.gpkg.ContexteDeTournee
import io.github.pobsteta.surculus.sauvegarde.SauvegardeApp
import io.github.pobsteta.surculus.tournee.LigneTournee
import io.github.pobsteta.surculus.tournee.TourneeOuverte
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * « Ouvrir sans position », **partagé** par l'écran de contrôle (mission 087) et le guidage (mission 089 ; décision de
 * Pascal du 2026-11-07, « Assouplir, code partagé ») : un seul habillage de la règle de la 087 (ADR 0003, amendement du
 * 2026-11-02, points 1 à 5 ; spec § 4.3 « Arrivée sans position »), appelé par les deux écrans, sans recopie.
 * La règle elle-même reste dans [ArriveeSansPosition] (délai, localisation) et [ArriveeControle.ouvrirSansPosition]
 * (`code_requis`, `code_inconnu`, choix de l'UG, confirmation, écriture).
 */
object OuvertureSansPosition {
    /**
     * La suite de l'attente sans fix, avec ce qui écoute sur ce téléphone. « Récepteur actif » (décision de Pascal du
     * 2026-11-03) : un récepteur RTK branché écoute, même sans la localisation du téléphone.
     */
    fun suite(ctx: Context, msSansFix: Long, fixRecu: Boolean): SuiteSansFix {
        val rtk = SourcePosition.recepteurBranche()
        return ArriveeSansPosition.suite(
            msSansFix = msSansFix,
            fixRecus = if (fixRecu) 1 else 0,
            localisationActive = rtk || SourcePosition.localisationActive(ctx),
            permissionAccordee = rtk || SourcePosition.localisationAutorisee(ctx),
        )
    }

    /**
     * L'appel à [ArriveeControle.ouvrirSansPosition] sur la tournée ouverte, hors du fil principal (journal). Une erreur
     * de lecture ou d'écriture rend [EtatArrivee.SansPosition] : rien n'est ouvert. Ce qu'on garde de l'étape (contrôle
     * ouvert, UG à choisir, confirmation) est l'affaire de l'écran qui appelle.
     */
    suspend fun ouvrir(ctx: Context, ouverte: TourneeOuverte, codeLu: String, ugChoisie: String?, confirmee: Boolean): EtatArrivee {
        val resultat = withContext(Dispatchers.IO) {
            runCatching {
                ArriveeControle.ouvrirSansPosition(
                    contexte = ContexteDeTournee.de(ouverte.tournee),
                    journal = SauvegardeApp.journal(ctx, LigneTournee(ouverte.dossier, ouverte.tournee, null)),
                    horloge = SauvegardeApp.horloge(ctx) { FixPourSession.depuis(SourcePosition.fix.value) },
                    essences = ArriveeControle.essencesDe(ouverte.tournee),
                    codeLu = codeLu,
                    sauvegarde = SauvegardeApp.ecoute(ctx, LigneTournee(ouverte.dossier, ouverte.tournee, null)),
                    ugChoisie = ugChoisie,
                    confirmee = confirmee,
                )
            }
        }
        return resultat.getOrNull() ?: EtatArrivee.SansPosition
    }
}

/**
 * Le verrou de « Ouvrir sans position » (mission 100), **partagé** par le guidage et l'écran de contrôle : pendant une
 * ouverture le bouton ne répond plus, et il répond de nouveau dès qu'elle se termine (ouverte, refusée ou en erreur).
 * Il est pris **au moment de l'appui** (fil principal), avant de lancer la coroutine : deux appuis arrivés dans le même
 * geste ne lancent donc qu'une ouverture. Un seul verrou par écran, pour tous les boutons qui ouvrent sans position.
 */
class VerrouOuverture {
    private val pris = AtomicBoolean(false)

    /** Vrai tant qu'une ouverture est en cours. */
    val enCours: Boolean get() = pris.get()

    /** Lance [bloc] dans [scope] si aucune ouverture n'est en cours ; rend faux (rien lancé) sinon. */
    fun lancer(scope: CoroutineScope, bloc: suspend () -> Unit): Boolean {
        if (!pris.compareAndSet(false, true)) return false
        try {
            scope.launch {
                try { bloc() } finally { pris.set(false) }
            }
        } catch (e: Throwable) {
            pris.set(false)
            throw e
        }
        return true
    }
}

/**
 * L'attente sans fix, posée dans la colonne de l'écran qui l'appelle (contrôle ou guidage) : la phrase de la [suite], le
 * champ « Code lu sur le piquet », puis, à [SuiteSansFix.PROPOSER] seulement, « Ouvrir sans position », grisé tant que le
 * champ est vide, avec `code_requis` dessous. Si rien n'écoute : la demande d'activer la localisation, jamais de bouton.
 */
@Composable
internal fun AttenteSansFix(suite: SuiteSansFix, secondesSansFix: Int, code: String, onCode: (String) -> Unit, onOuvrir: () -> Unit) {
    when (suite) {
        SuiteSansFix.ACTIVER_LOCALISATION ->
            Text(TextesControle.ACTIVER_LOCALISATION, style = MaterialTheme.typography.titleMedium, color = couleurErreur())
        SuiteSansFix.ATTENDRE, SuiteSansFix.POSITION_RECUE ->
            Text(TextesControle.attenteSansFix(secondesSansFix), style = MaterialTheme.typography.bodyLarge, color = couleurNeutre())
        SuiteSansFix.PROPOSER ->
            Text(TextesControle.SANS_FIX_PROPOSE, style = MaterialTheme.typography.bodyLarge, color = couleurNeutre())
    }
    OutlinedTextField(
        value = code,
        onValueChange = onCode,
        label = { Text(stringResource(R.string.guidage_code)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    if (suite == SuiteSansFix.PROPOSER) {
        Button(onClick = onOuvrir, enabled = code.isNotBlank(), modifier = Modifier.fillMaxWidth().height(DimensionsControle.ACTION)) {
            Text(TextesControle.OUVRIR_SANS_POSITION)
        }
        if (code.isBlank()) Text(TextesControle.CODE_REQUIS, style = MaterialTheme.typography.bodyMedium, color = couleurNeutre())
    }
}

/**
 * La placette à confirmer avant d'ouvrir sans position (point 4), posée dans la colonne de l'écran qui l'appelle : rien
 * n'est ouvert tant que l'agent n'a pas confirmé.
 */
@Composable
internal fun ConfirmationSansPosition(etat: EtatArrivee.ConfirmerSansPosition, onOuvrir: () -> Unit, onAnnuler: () -> Unit) {
    Text(TextesControle.confirmationSansPosition(etat.piquet, etat.plantsPointes), style = MaterialTheme.typography.headlineMedium, color = couleurNeutre())
    Button(onClick = onOuvrir, modifier = Modifier.fillMaxWidth().height(DimensionsControle.GRAND_CHOIX)) {
        Text(TextesControle.OUVRIR_SANS_POSITION, style = MaterialTheme.typography.titleLarge)
    }
    OutlinedButton(onClick = onAnnuler, modifier = Modifier.fillMaxWidth().height(DimensionsControle.ACTION)) {
        Text(TextesControle.ANNULER)
    }
}
