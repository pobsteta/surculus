/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.tournee

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReglagesAffichageTest {
    private class Memoire : MagasinAffichage {
        val booleens = mutableMapOf<String, Boolean>()
        override fun lireBooleen(cle: String, parDefaut: Boolean) = booleens[cle] ?: parDefaut
        override fun ecrireBooleen(cle: String, valeur: Boolean) { booleens[cle] = valeur }
    }

    @Test fun `le lien des licences pointe sur docs-dependances sur la branche main`() {
        assertEquals("https://github.com/pobsteta/surculus/blob/main/docs/dependances.md", URL_LICENCES)
    }

    @Test fun `le lien GitHub pointe sur le depot`() {
        assertEquals("https://github.com/pobsteta/surculus", URL_GITHUB)
    }

    @Test fun `plein ecran est faux par defaut`() {
        assertFalse(ReglagesAffichage(Memoire()).pleinEcran)
    }

    @Test fun `plein ecran est relu apres ecriture, par un autre lecteur du meme magasin`() {
        val magasin = Memoire()
        ReglagesAffichage(magasin).pleinEcran = true
        assertTrue(ReglagesAffichage(magasin).pleinEcran)
        ReglagesAffichage(magasin).pleinEcran = false
        assertFalse(ReglagesAffichage(magasin).pleinEcran)
    }

    @Test fun `texte de version de la release`() {
        assertEquals("Version 0.7.41", texteVersion("0.7.41"))
    }

    @Test fun `texte de version de la fumee garde son suffixe`() {
        assertEquals("Version 0.7.41-fumee", texteVersion("0.7.41-fumee"))
    }

    @Test fun `version absente ou vide n'invente pas de numero`() {
        assertEquals("Version inconnue", texteVersion(null))
        assertEquals("Version inconnue", texteVersion("  "))
    }
}
