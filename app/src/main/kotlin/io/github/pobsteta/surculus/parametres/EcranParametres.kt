/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.parametres

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.pobsteta.surculus.R
import io.github.pobsteta.surculus.ui.BandeauCompact

/**
 * Paramètres (mission 046), comme Marculus : « Plein écran » (mission 044), puis l'accès à l'Essai GNSS et à la
 * Restauration. Le plein écran est tenu par l'activité ([pleinEcran], [onPleinEcran]). Plus de case pour les colonnes
 * de la liste des tournées : leur bouton est toujours dans la barre (mission 050).
 */
@Composable
fun EcranParametres(
    pleinEcran: Boolean,
    onPleinEcran: (Boolean) -> Unit,
    onEssai: () -> Unit,
    onRestauration: () -> Unit,
    onRetour: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        BandeauCompact(
            titre = stringResource(R.string.param_titre),
            navigationIcon = {
                IconButton(onClick = onRetour) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.param_retour))
                }
            },
        )
        Column(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LigneReglage(stringResource(R.string.param_plein_titre), stringResource(R.string.param_plein_desc), pleinEcran, onPleinEcran)
            LigneAcces(stringResource(R.string.essai_ouvrir), stringResource(R.string.param_essai_desc), onEssai)
            LigneAcces(stringResource(R.string.restauration_ouvrir), stringResource(R.string.param_restaurer_desc), onRestauration)
            SectionRecepteur()
            SectionDicteeVocale()
        }
    }
}

/** Case à bascule : toute la ligne est la cible (au moins 48 dp, une main, avec des gants). */
@Composable
private fun LigneReglage(titre: String, description: String, valeur: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .toggleable(value = valeur, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(titre, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = valeur, onCheckedChange = null)
    }
}

/** Entrée qui ouvre un écran ; le retour de cet écran ramène ici. */
@Composable
private fun LigneAcces(titre: String, description: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onClick).padding(vertical = 6.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(titre, style = MaterialTheme.typography.titleMedium)
        Text(description, style = MaterialTheme.typography.bodySmall)
    }
}
