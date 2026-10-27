/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.tournee

/** Dépôt du projet et liste complète des dépendances et licences (mission 045), sur la branche `main`. */
const val URL_GITHUB = "https://github.com/pobsteta/surculus"
const val URL_LICENCES = "https://github.com/pobsteta/surculus/blob/main/docs/dependances.md"

/** Plein écran : faux par défaut, comme Marculus (mission 044). */
const val PLEIN_ECRAN_PAR_DEFAUT = false

/**
 * Ligne « Version … » de la boîte « À propos » : [versionName] est celui du paquet installé (release : `0.7.41` ;
 * fumée : `0.7.41-fumee`). Absent ou vide : on le dit, on n'invente pas de numéro.
 */
fun texteVersion(versionName: String?): String =
    versionName?.trim()?.takeIf { it.isNotEmpty() }?.let { "Version $it" } ?: "Version inconnue"

/** Ce que les réglages d'affichage lisent et écrivent sur le téléphone (SharedPreferences en vrai, mémoire dans les tests). */
interface MagasinAffichage {
    fun lireBooleen(cle: String, parDefaut: Boolean): Boolean
    fun ecrireBooleen(cle: String, valeur: Boolean)
}

/** Réglages d'affichage, **gardés sur le téléphone seulement** (comme les réglages Kanban de la mission 043). */
class ReglagesAffichage(private val magasin: MagasinAffichage) {
    var pleinEcran: Boolean
        get() = magasin.lireBooleen(CLE_PLEIN_ECRAN, PLEIN_ECRAN_PAR_DEFAUT)
        set(v) = magasin.ecrireBooleen(CLE_PLEIN_ECRAN, v)

    private companion object {
        const val CLE_PLEIN_ECRAN = "plein_ecran"
    }
}
