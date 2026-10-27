/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.parametres

import io.github.pobsteta.surculus.gnss.TransportRtk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AvertissementsRecepteurTest {
    private val bt = AvertissementRecepteur.BLUETOOTH_REFUSE
    private val notif = AvertissementRecepteur.NOTIFICATIONS_REFUSEES

    @Test fun `bluetooth refuse en transport Bluetooth est signale`() {
        assertEquals(listOf(bt), avertissementsRecepteur(true, TransportRtk.BLUETOOTH, false, true))
    }

    @Test fun `bluetooth refuse en TCP n'est pas signale`() {
        assertTrue(avertissementsRecepteur(true, TransportRtk.TCP, false, true).isEmpty())
    }

    @Test fun `notifications refusees sont signalees, en Bluetooth comme en TCP`() {
        assertEquals(listOf(notif), avertissementsRecepteur(true, TransportRtk.BLUETOOTH, true, false))
        assertEquals(listOf(notif), avertissementsRecepteur(true, TransportRtk.TCP, true, false))
    }

    @Test fun `les deux refus sont signales ensemble`() {
        assertEquals(listOf(bt, notif), avertissementsRecepteur(true, TransportRtk.BLUETOOTH, false, false))
    }

    @Test fun `reglage inactif, aucun avertissement`() {
        assertTrue(avertissementsRecepteur(false, TransportRtk.BLUETOOTH, false, false).isEmpty())
    }

    @Test fun `tout accorde, aucun avertissement`() {
        assertTrue(avertissementsRecepteur(true, TransportRtk.BLUETOOTH, true, true).isEmpty())
    }

    @Test fun `l'autorisation accordee ensuite fait disparaitre l'avertissement`() {
        assertEquals(listOf(bt), avertissementsRecepteur(true, TransportRtk.BLUETOOTH, false, true))
        assertTrue(avertissementsRecepteur(true, TransportRtk.BLUETOOTH, true, true).isEmpty())
    }
}
