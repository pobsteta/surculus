/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.ui.theme

/** Choix du thème (mission 051), comme Marculus et Groma : suivre le téléphone, ou forcer le clair ou le sombre. */
enum class ChoixTheme {
    SYSTEME, CLAIR, SOMBRE;

    /** Vrai si l'application doit s'afficher en sombre, [systemeSombre] étant l'état du téléphone. */
    fun estSombre(systemeSombre: Boolean): Boolean = when (this) {
        SYSTEME -> systemeSombre
        CLAIR -> false
        SOMBRE -> true
    }

    companion object {
        /** Relit un choix enregistré ([nom], nom de l'énumération). Absent ou inconnu : [SYSTEME], le défaut. */
        fun depuis(nom: String?): ChoixTheme = entries.firstOrNull { it.name == nom } ?: SYSTEME
    }
}
