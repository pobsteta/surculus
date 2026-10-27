/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.tournee

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

    @Test fun `message de liste vide selon la puce et la recherche`() {
        assertEquals(MessageVide.AUCUNE_TROUVEE, messageVide("couchey", voirRendues = false))
        assertEquals(MessageVide.AUCUNE_TROUVEE, messageVide("couchey", voirRendues = true))
        assertEquals(MessageVide.AUCUNE_RENDUE, messageVide("", voirRendues = true))
        // Puce éteinte, recherche vide, liste vide : toutes les tournées sont rendues, on ne dit pas « aucune rendue ».
        assertEquals(MessageVide.AUCUNE_OUVERTE, messageVide("  ", voirRendues = false))
    }

    @Test fun `champs de la ligne - foret, operateur, date et dossier a leur place`() {
        val t = io.github.pobsteta.surculus.gpkg.Tournee(
            epsg = io.github.pobsteta.surculus.core.reperes.CodeEpsg.RGF93_V1_LAMBERT93, srsId = 2154, couches = emptyList(), placettes = emptyList(),
            controles = emptyList(), empriseForet = null,
            projet = io.github.pobsteta.surculus.gpkg.ProjetLu("Nom", "Foret X", "Operateur Y", "2026-10-12", null, null),
            plants = emptyList(), etats = emptyList(),
        )
        val l = LigneTournee(File("dossier-z"), t, null)
        assertEquals(ChampsTournee("Foret X", "Operateur Y", "2026-10-12", "dossier-z", "Nom"), l.champs)
    }

    @Test fun `le nom du projet est cherche meme quand la foret existe`() {
        val c = ChampsTournee("Foret X", "Operateur Y", "2026-10-12", "dossier-z", "Plantation Printemps")
        assertTrue(correspond(c, "printemps"))
        assertTrue(correspond(c, "foret x"))
        assertFalse(correspond(c.copy(nomProjet = null), "printemps"))
    }

    @Test fun `oe et ae ligatures valent oe et ae des deux cotes`() {
        val c = ChampsTournee("Forêt du Bœuf", null, null, "d", "Cæsar")
        assertTrue(correspond(c, "boeuf"))
        assertTrue(correspond(c, "BŒUF"))
        assertTrue(correspond(c, "bœuf"))
        assertTrue(correspond(c, "caesar"))
        assertTrue(correspond(c, "cæsar"))
        assertTrue(correspond(ChampsTournee("Forêt du Boeuf", null, null, "d"), "bœuf"))
        assertFalse(correspond(ChampsTournee("Strasse", null, null, "d"), "straße"))
    }

    @Test fun `aucune tournee n'est rendue tant que l'export n'existe pas`() {
        val l = LigneTournee(File("x"), null, "refus")
        assertFalse(l.rendue)
        assertEquals("x", l.champs.dossier)
    }
}
