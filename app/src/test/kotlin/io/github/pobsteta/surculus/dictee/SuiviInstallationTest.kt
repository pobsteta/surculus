/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.dictee

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Mission 052 : l'état de l'installation du modèle, sans Android (portée immédiate : le travail se fait dans l'appel). */
class SuiviInstallationTest {
    private fun suivi() = SuiviInstallation(CoroutineScope(SupervisorJob() + Dispatchers.Unconfined))

    private val ok: (() -> Boolean, () -> Unit, (Float) -> Unit) -> Result<Unit> = { _, _, _ -> Result.success(Unit) }

    @Test fun `une installation reussie rend l'ecran au repos avec l'issue Ok`() {
        val s = suivi()
        assertTrue(s.demarrer(PhaseInstallation.RESEAU, { false }, ok))
        assertEquals(EtatInstallation(PhaseInstallation.RIEN, null, IssueInstallation.Ok), s.etat.value)
    }

    @Test fun `une exception rend l'ecran au repos avec l'issue Echec et son message`() {
        val s = suivi()
        s.demarrer(PhaseInstallation.FICHIER, { true }) { _, _, _ -> throw IllegalStateException("zip abime") }
        assertEquals(EtatInstallation(PhaseInstallation.RIEN, null, IssueInstallation.Echec("zip abime", true)), s.etat.value)
    }

    @Test fun `une Error rend aussi l'ecran au repos avec l'issue Echec, jamais en cours`() {
        // Une bibliotheque native absente leve une Error, pas une Exception.
        val s = suivi()
        s.demarrer(PhaseInstallation.RESEAU, { false }) { _, _, _ -> throw UnsatisfiedLinkError("libvosk.so") }
        assertEquals(PhaseInstallation.RIEN, s.etat.value.phase)
        assertEquals(IssueInstallation.Echec("libvosk.so", false), s.etat.value.issue)
    }

    @Test fun `apres une Error une nouvelle installation peut etre lancee`() {
        val s = suivi()
        s.demarrer(PhaseInstallation.RESEAU, { false }) { _, _, _ -> throw OutOfMemoryError() }
        assertTrue(s.demarrer(PhaseInstallation.RESEAU, { false }, ok))
        assertEquals(IssueInstallation.Ok, s.etat.value.issue)
    }

    @Test fun `une erreur sans message dit erreur inconnue`() {
        val s = suivi()
        s.demarrer(PhaseInstallation.RESEAU, { false }) { _, _, _ -> throw IllegalStateException() }
        assertEquals(IssueInstallation.Echec("erreur inconnue", false), s.etat.value.issue)
    }

    @Test fun `une annulation demandee donne l'issue Annule, meme si le travail finit en echec`() {
        val s = suivi()
        s.demarrer(PhaseInstallation.RESEAU, { true }) { _, _, _ -> s.annuler(); Result.failure(java.io.IOException("coupe")) }
        assertEquals(IssueInstallation.Annule(true), s.etat.value.issue)
    }

    @Test fun `une seconde installation est refusee pendant la premiere, sans toucher l'etat`() {
        val s = suivi()
        var secondeLancee: Boolean? = null
        s.demarrer(PhaseInstallation.RESEAU, { false }) { _, _, _ ->
            secondeLancee = s.demarrer(PhaseInstallation.FICHIER, { false }, ok)
            assertEquals(PhaseInstallation.RESEAU, s.etat.value.phase)
            Result.success(Unit)
        }
        assertEquals(false, secondeLancee)
        assertEquals(IssueInstallation.Ok, s.etat.value.issue)
    }

    @Test fun `la phase et la progression sont publiees pendant le travail`() {
        val s = suivi()
        val vus = mutableListOf<EtatInstallation>()
        s.demarrer(PhaseInstallation.RESEAU, { false }) { _, nonRetour, progression ->
            progression(0.5f); vus += s.etat.value
            nonRetour(); vus += s.etat.value
            Result.success(Unit)
        }
        assertEquals(PhaseInstallation.RESEAU, vus[0].phase)
        assertEquals(0.5f, vus[0].progression)
        assertEquals(PhaseInstallation.INSTALLATION, vus[1].phase)
    }

    @Test fun `effacerIssue oublie l'issue au repos, pas en cours`() {
        val s = suivi()
        s.demarrer(PhaseInstallation.RESEAU, { false }, ok)
        s.effacerIssue()
        assertNull(s.etat.value.issue)
        s.demarrer(PhaseInstallation.RESEAU, { false }) { _, _, _ ->
            s.effacerIssue()
            assertFalse(s.etat.value.phase == PhaseInstallation.RIEN)
            Result.success(Unit)
        }
    }
}
