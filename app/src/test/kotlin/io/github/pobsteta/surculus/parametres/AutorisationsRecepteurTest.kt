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

/** Mission 052 : « pas encore demandée » n'est pas « refusée ». */
class AutorisationsRecepteurTest {
    @Test fun `accordee l'emporte sur tout`() {
        assertEquals(EtatAutorisation.ACCORDEE, etatAutorisation(accordee = true, dejaRefusee = true, rationale = false))
    }

    @Test fun `jamais refusee et non accordee - pas encore demandee`() {
        assertEquals(EtatAutorisation.JAMAIS_DEMANDEE, etatAutorisation(accordee = false, dejaRefusee = false, rationale = false))
    }

    @Test fun `refusee une fois, le systeme propose encore de la redemander - refusee`() {
        assertEquals(EtatAutorisation.REFUSEE, etatAutorisation(accordee = false, dejaRefusee = true, rationale = true))
    }

    @Test fun `refusee et le systeme ne propose plus - refusee definitivement`() {
        assertEquals(EtatAutorisation.REFUSEE_DEFINITIVEMENT, etatAutorisation(accordee = false, dejaRefusee = true, rationale = false))
    }

    @Test fun `si le systeme propose de redemander, c'est qu'elle a deja ete refusee`() {
        assertEquals(EtatAutorisation.REFUSEE, etatAutorisation(accordee = false, dejaRefusee = false, rationale = true))
    }

    @Test fun `telephone neuf - Bluetooth et notifications pas encore demandes, chacun son avertissement`() {
        val n = EtatAutorisation.JAMAIS_DEMANDEE
        assertEquals(
            listOf(AvertissementRecepteur.BLUETOOTH_A_DEMANDER, AvertissementRecepteur.NOTIFICATIONS_A_DEMANDER),
            avertissementsRecepteur(true, TransportRtk.BLUETOOTH, n, n),
        )
    }

    @Test fun `refus definitif - avertissement propre, different du refus simple`() {
        val d = EtatAutorisation.REFUSEE_DEFINITIVEMENT
        val r = EtatAutorisation.REFUSEE
        val a = EtatAutorisation.ACCORDEE
        assertEquals(listOf(AvertissementRecepteur.BLUETOOTH_REFUSE_DEFINITIVEMENT), avertissementsRecepteur(true, TransportRtk.BLUETOOTH, d, a))
        assertEquals(listOf(AvertissementRecepteur.NOTIFICATIONS_REFUSEE_DEFINITIVEMENT), avertissementsRecepteur(true, TransportRtk.TCP, a, d))
        assertEquals(listOf(AvertissementRecepteur.BLUETOOTH_REFUSE), avertissementsRecepteur(true, TransportRtk.BLUETOOTH, r, a))
        assertEquals(listOf(AvertissementRecepteur.NOTIFICATIONS_REFUSEES), avertissementsRecepteur(true, TransportRtk.TCP, a, r))
    }

    @Test fun `en TCP le Bluetooth non demande ne dit rien, reglage inactif rien non plus`() {
        val n = EtatAutorisation.JAMAIS_DEMANDEE
        assertEquals(listOf(AvertissementRecepteur.NOTIFICATIONS_A_DEMANDER), avertissementsRecepteur(true, TransportRtk.TCP, n, n))
        assertTrue(avertissementsRecepteur(false, TransportRtk.BLUETOOTH, n, n).isEmpty())
    }

    @Test fun `tout accorde, aucun avertissement`() {
        val a = EtatAutorisation.ACCORDEE
        assertTrue(avertissementsRecepteur(true, TransportRtk.BLUETOOTH, a, a).isEmpty())
    }
}
