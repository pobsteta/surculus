/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.ui.theme

/**
 * Icônes de la barre d'état claires (blanches) ? (mission 051)
 * - Écran à bandeau : le bandeau `primary` (vert) passe sous la barre d'état dans les deux thèmes, donc icônes claires
 *   toujours (5,17:1, comme Marculus). Depuis la mission 055, **tous** les écrans en ont un : liste, Paramètres, Essai GNSS,
 *   Restauration (mission 053), carte et guidage (mission 055). `MainActivity` passe donc `true`.
 * - Écran sans bandeau (aucun aujourd'hui) : le fond du thème passerait sous la barre, donc icônes claires en sombre seulement.
 */
fun iconesBarreEtatClaires(ecranAvecBandeau: Boolean, sombre: Boolean): Boolean = ecranAvecBandeau || sombre
