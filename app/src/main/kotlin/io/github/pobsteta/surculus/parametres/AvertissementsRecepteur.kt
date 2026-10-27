/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.parametres

import io.github.pobsteta.surculus.gnss.TransportRtk

/** Ce que la section « Récepteur GNSS » signale à l'agent (mission 047) ; Kotlin pur, testé en JVM. */
enum class AvertissementRecepteur { BLUETOOTH_REFUSE, NOTIFICATIONS_REFUSEES }

/**
 * Les avertissements selon l'état réel des autorisations, relu à chaque affichage de la section.
 * Réglage inactif : aucun. Bluetooth refusé : seulement si le transport est Bluetooth (en TCP il ne sert à rien).
 * Notifications refusées : quel que soit le transport (sans elles, « Libérer le récepteur » reste caché).
 */
fun avertissementsRecepteur(
    actif: Boolean,
    transport: TransportRtk,
    bluetoothAutorise: Boolean,
    notificationsAutorisees: Boolean,
): List<AvertissementRecepteur> = buildList {
    if (!actif) return@buildList
    if (transport == TransportRtk.BLUETOOTH && !bluetoothAutorise) add(AvertissementRecepteur.BLUETOOTH_REFUSE)
    if (!notificationsAutorisees) add(AvertissementRecepteur.NOTIFICATIONS_REFUSEES)
}
