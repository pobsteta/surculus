/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.parametres

import io.github.pobsteta.surculus.gnss.TransportRtk

/** Où en est une autorisation du système (mission 052) : « pas encore demandée » n'est pas « refusée ». */
enum class EtatAutorisation {
    ACCORDEE,

    /** Jamais refusée : aucun refus vu (jamais demandée, ou fenêtre fermée sans réponse). */
    JAMAIS_DEMANDEE,

    /** Refusée, mais le système propose encore de la redemander. */
    REFUSEE,

    /** Refusée et le système ne rouvre plus la fenêtre : il faut passer par les réglages du téléphone. */
    REFUSEE_DEFINITIVEMENT,
}

/**
 * L'état d'une autorisation à partir de ce que le système sait dire (il ne distingue pas « jamais demandée » de
 * « refusée pour de bon » : `shouldShowRequestPermissionRationale` est faux dans les deux cas).
 * @param dejaRefusee Surculus a vu un refus (mémorisé à chaque réponse négative du système).
 * @param rationale `shouldShowRequestPermissionRationale` : vrai quand le système propose encore de redemander,
 *                  donc après un premier refus, même d'une version précédente de l'application.
 */
fun etatAutorisation(accordee: Boolean, dejaRefusee: Boolean, rationale: Boolean): EtatAutorisation = when {
    accordee -> EtatAutorisation.ACCORDEE
    rationale -> EtatAutorisation.REFUSEE
    !dejaRefusee -> EtatAutorisation.JAMAIS_DEMANDEE
    else -> EtatAutorisation.REFUSEE_DEFINITIVEMENT
}

/** Ce que la section « Récepteur GNSS » signale à l'agent (missions 047 et 052) ; Kotlin pur, testé en JVM. */
enum class AvertissementRecepteur {
    BLUETOOTH_A_DEMANDER, BLUETOOTH_REFUSE, BLUETOOTH_REFUSE_DEFINITIVEMENT,
    NOTIFICATIONS_A_DEMANDER, NOTIFICATIONS_REFUSEES, NOTIFICATIONS_REFUSEE_DEFINITIVEMENT,
}

/**
 * Les avertissements selon l'état réel des autorisations, relu à chaque affichage de la section.
 * Réglage inactif : aucun. Bluetooth : seulement si le transport est Bluetooth (en TCP il ne sert à rien).
 * Notifications : quel que soit le transport (sans elles, « Libérer le récepteur » reste caché).
 */
fun avertissementsRecepteur(
    actif: Boolean,
    transport: TransportRtk,
    bluetooth: EtatAutorisation,
    notifications: EtatAutorisation,
): List<AvertissementRecepteur> = buildList {
    if (!actif) return@buildList
    if (transport == TransportRtk.BLUETOOTH) when (bluetooth) {
        EtatAutorisation.ACCORDEE -> Unit
        EtatAutorisation.JAMAIS_DEMANDEE -> add(AvertissementRecepteur.BLUETOOTH_A_DEMANDER)
        EtatAutorisation.REFUSEE -> add(AvertissementRecepteur.BLUETOOTH_REFUSE)
        EtatAutorisation.REFUSEE_DEFINITIVEMENT -> add(AvertissementRecepteur.BLUETOOTH_REFUSE_DEFINITIVEMENT)
    }
    when (notifications) {
        EtatAutorisation.ACCORDEE -> Unit
        EtatAutorisation.JAMAIS_DEMANDEE -> add(AvertissementRecepteur.NOTIFICATIONS_A_DEMANDER)
        EtatAutorisation.REFUSEE -> add(AvertissementRecepteur.NOTIFICATIONS_REFUSEES)
        EtatAutorisation.REFUSEE_DEFINITIVEMENT -> add(AvertissementRecepteur.NOTIFICATIONS_REFUSEE_DEFINITIVEMENT)
    }
}

/** Version à deux états (047) : une autorisation non accordée y compte comme refusée. */
fun avertissementsRecepteur(
    actif: Boolean,
    transport: TransportRtk,
    bluetoothAutorise: Boolean,
    notificationsAutorisees: Boolean,
): List<AvertissementRecepteur> = avertissementsRecepteur(
    actif, transport,
    if (bluetoothAutorise) EtatAutorisation.ACCORDEE else EtatAutorisation.REFUSEE,
    if (notificationsAutorisees) EtatAutorisation.ACCORDEE else EtatAutorisation.REFUSEE,
)
