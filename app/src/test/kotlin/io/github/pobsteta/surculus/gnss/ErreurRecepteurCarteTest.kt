/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.gnss

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Mission 052 : ce que la carte dit quand le récepteur externe ou son service est en erreur (fonction pure, sans Compose). */
class ErreurRecepteurCarteTest {
    @Test fun `reglage inactif - la carte ne dit rien, meme avec une vieille erreur`() {
        assertNull(ErreurRecepteurCarte.pour(reglageActif = false, enMarche = false, erreur = "refus", erreurCorrections = "x"))
    }

    @Test fun `tout va bien ou connexion en cours - rien`() {
        assertNull(ErreurRecepteurCarte.pour(true, enMarche = true, erreur = null, erreurCorrections = null))
        assertNull(ErreurRecepteurCarte.pour(true, enMarche = false, erreur = null, erreurCorrections = null))
    }

    @Test fun `le service n'a pas pu demarrer`() {
        assertEquals(ErreurRecepteurCarte.NON_DEMARRE, ErreurRecepteurCarte.pour(true, enMarche = false, erreur = "non choisi", erreurCorrections = null))
    }

    @Test fun `la liaison avec le recepteur est perdue`() {
        assertEquals(ErreurRecepteurCarte.LIAISON_PERDUE, ErreurRecepteurCarte.pour(true, enMarche = true, erreur = "Connection reset", erreurCorrections = null))
    }

    @Test fun `seules les corrections sont en erreur`() {
        assertEquals(ErreurRecepteurCarte.CORRECTIONS, ErreurRecepteurCarte.pour(true, enMarche = true, erreur = null, erreurCorrections = "401"))
    }

    @Test fun `la liaison perdue passe avant les corrections`() {
        assertEquals(ErreurRecepteurCarte.LIAISON_PERDUE, ErreurRecepteurCarte.pour(true, enMarche = true, erreur = "x", erreurCorrections = "y"))
    }
}
