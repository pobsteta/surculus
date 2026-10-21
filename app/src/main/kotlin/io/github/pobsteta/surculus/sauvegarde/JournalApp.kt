/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.sauvegarde

import android.content.Context
import io.github.pobsteta.surculus.data.journal.db.HorlogeAppareil
import io.github.pobsteta.surculus.data.journal.db.JournalDatabase
import io.github.pobsteta.surculus.data.journal.db.JournalRepository
import io.github.pobsteta.surculus.data.sauvegarde.NomSauvegarde

/**
 * Journal de l'application et identifiant d'appareil **opaque** (8 caractères hexadécimaux tirés une fois par
 * installation, jamais le nom du téléphone ni de son propriétaire : ADR 0005, confidentialité).
 */
object JournalApp {
    @Volatile private var depot: JournalRepository? = null

    fun identifiantAppareil(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences("surculus", Context.MODE_PRIVATE)
        prefs.getString("appareil_opaque", null)?.let { return it }
        val id = NomSauvegarde.nouvelIdentifiantAppareil()
        prefs.edit().putString("appareil_opaque", id).commit()
        return id
    }

    fun depot(context: Context): JournalRepository = depot ?: synchronized(this) {
        depot ?: JournalRepository(
            JournalDatabase.ouvrir(context), identifiantAppareil(context), HorlogeAppareil(context),
        ).also { depot = it }
    }
}
