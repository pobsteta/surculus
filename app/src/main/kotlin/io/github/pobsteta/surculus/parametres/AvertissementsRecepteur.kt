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

    /** Aucune réponse « non accordée » vue : l'autorisation n'a jamais été demandée. */
    JAMAIS_DEMANDEE,

    /** Refusée, mais le système propose encore de la redemander. */
    REFUSEE,

    /** Refusée et le système ne rouvre plus la fenêtre : il faut passer par les réglages du téléphone. */
    REFUSEE_DEFINITIVEMENT,
}

/**
 * L'état d'une autorisation à partir de ce que le système sait dire (il ne distingue pas « jamais demandée » de
 * « refusée pour de bon » : `shouldShowRequestPermissionRationale` est faux dans les deux cas).
 * @param dejaRefusee Surculus a vu une réponse « non accordée » du système (mémorisée à chaque réponse négative, y
 *                    compris un refus définitif déjà acquis ou une fenêtre fermée sans réponse : Android ne les distingue pas).
 * @param rationale `shouldShowRequestPermissionRationale` : vrai quand le système propose encore de redemander,
 *                  donc après un premier refus, même d'une version précédente de l'application.
 */
fun etatAutorisation(accordee: Boolean, dejaRefusee: Boolean, rationale: Boolean): EtatAutorisation = when {
    accordee -> EtatAutorisation.ACCORDEE
    rationale -> EtatAutorisation.REFUSEE
    !dejaRefusee -> EtatAutorisation.JAMAIS_DEMANDEE
    else -> EtatAutorisation.REFUSEE_DEFINITIVEMENT
}

/**
 * Faut-il mémoriser un refus après la réponse du système ? Toute réponse « non accordée » est un refus, mais Android ne
 * dit pas tout : [rationaleAvant] et [rationaleApres] sont `shouldShowRequestPermissionRationale` lu juste avant la demande
 * et juste après la réponse.
 *  - faux puis vrai : premier refus, redemandable ;
 *  - vrai puis faux : « ne plus demander », définitif ;
 *  - faux puis faux : refus définitif déjà acquis (fenêtre non ouverte, par exemple sous une version antérieure à la 052)
 *    **ou** fenêtre fermée sans réponse. Android ne les distingue pas ; le premier l'emporte (voir [etatAutorisation]).
 * Les paramètres documentent ces cas ; seul « accordée » change le résultat.
 */
@Suppress("UNUSED_PARAMETER")
fun refusAMemoriser(accordee: Boolean, rationaleAvant: Boolean, rationaleApres: Boolean): Boolean = !accordee

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
