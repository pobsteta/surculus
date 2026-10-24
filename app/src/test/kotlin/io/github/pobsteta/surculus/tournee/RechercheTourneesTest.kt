/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.tournee

import io.github.pobsteta.surculus.carte.OrthoTournee
import io.github.pobsteta.surculus.core.carte.EtatOrtho
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RechercheTourneesTest {
    private val couchey = ChampsTournee("Forêt de Couchey", "Marie Dupont", "2026-10-12 08:30:00", "tournee-2026-10-12")

    @Test fun `sans accents ni casse`() {
        assertTrue(correspond(couchey, "couchéy"))
        assertTrue(correspond(couchey, "COUCHEY"))
        assertTrue(correspond(couchey, "foret"))
        assertTrue(correspond(couchey, "  couchey  "))
    }

    @Test fun `chaque champ est cherche`() {
        assertTrue(correspond(couchey, "dupont"), "opérateur")
        assertTrue(correspond(couchey, "marie"), "opérateur, casse")
        assertTrue(correspond(couchey, "2026-10-12 08"), "date telle que le fichier la porte")
        assertTrue(correspond(couchey, "12/10/2026"), "date à la française")
        assertTrue(correspond(couchey, "tournee-2026"), "nom du dossier")
    }

    @Test fun `rien ne correspond`() {
        assertFalse(correspond(couchey, "Vauclair"))
        assertFalse(correspond(couchey, "13/10/2026"))
    }

    @Test fun `tournee refusee trouvee par le nom du dossier`() {
        val refusee = ChampsTournee(null, null, null, "import-42")
        assertTrue(correspond(refusee, "import-42"))
        assertFalse(correspond(refusee, "couchey"))
    }

    @Test fun `recherche vide ou blanche = tout`() {
        assertTrue(correspond(couchey, ""))
        assertTrue(correspond(couchey, "   "))
        assertTrue(correspond(ChampsTournee(null, null, null, "x"), ""))
    }

    @Test fun `partage ouvertes et rendues`() {
        val (rendues, ouvertes) = partager(listOf(1, 2, 3, 4)) { it % 2 == 0 }
        assertEquals(listOf(2, 4), rendues)
        assertEquals(listOf(1, 3), ouvertes)
    }

    @Test fun `aucune tournee n'est rendue tant que l'export n'existe pas`() {
        val l = LigneTournee(File("x"), null, "refus", OrthoTournee(EtatOrtho.Absente, null, null))
        assertFalse(l.rendue)
        assertEquals("x", l.champs.dossier)
    }
}
