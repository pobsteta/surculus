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
 * - Écran à bandeau (liste, Paramètres, Essai GNSS, Restauration ; mission 053) : le bandeau `primary` (vert) passe
 *   sous la barre d'état dans les deux thèmes, donc icônes claires toujours (5,17:1, comme Marculus).
 * - Carte et guidage (sans bandeau) : le fond du thème passe sous la barre, donc icônes claires en sombre seulement.
 */
fun iconesBarreEtatClaires(ecranAvecBandeau: Boolean, sombre: Boolean): Boolean = ecranAvecBandeau || sombre
