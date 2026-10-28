/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.gnss

import io.github.pobsteta.surculus.core.gnss.FixGnss
import io.github.pobsteta.surculus.data.journal.db.FixGnss as FixDeSession

/**
 * Le fix que la session de contrôle voit (mission 065, spec § 5) : l'heure UTC du fix et l'instant où il a été reçu sur
 * l'horloge monotone. La session en tire l'âge du fix (au plus 5 s : le jour civil vient de l'heure GNSS) ; un âge négatif
 * ou trop grand est écarté par la règle de `JourCivil`, pas ici.
 */
object FixPourSession {
    /**
     * `null` sans fix, sans heure UTC complète, sans instant monotone, ou pour une position simulée (son heure n'est pas
     * celle des satellites) : le jour civil retombe alors sur l'heure du téléphone.
     */
    fun depuis(fix: FixGnss?): FixDeSession? {
        if (fix == null || fix.simule) return null
        val heure = fix.heureUtcMs ?: return null
        val monotone = fix.recuMonotoneMs ?: return null
        return FixDeSession(heureUtcMs = heure, monotoneMs = monotone)
    }
}
