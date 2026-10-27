/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.carte

import androidx.compose.ui.graphics.Color
import io.github.pobsteta.surculus.ui.theme.SchemaClair
import io.github.pobsteta.surculus.ui.theme.SchemaSombre
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Contraste des couleurs d'état (mission 051, puis 063) : en clair, rien ne change ; en sombre, plus de plaque blanche,
 * texte en onSurface ou en error du thème, pastilles cernées de blanc. Texte >= 4,5:1, pastille >= 3:1 (WCAG 1.4.11),
 * sur le fond réel de chaque thème.
 */
class ContrasteTest {
    private val teintes = mapOf("Vert" to Palette.Vert, "Orange" to Palette.Orange, "Rouge" to Palette.Rouge, "Noir" to Palette.Noir)
    private val filet = CouleursEtat.filet

    @Test fun `la formule donne 21 pour noir sur blanc et 1 pour deux couleurs egales`() {
        assertEquals(21f, contraste(Color.Black, Color.White), 0.01f)
        assertEquals(1f, contraste(Color.White, Color.White), 0.001f)
    }

    @Test fun `en clair, chaque teinte d'etat se lit sur le fond (4,5 au moins)`() {
        teintes.forEach { (nom, c) ->
            assertTrue(contraste(c, SchemaClair.background) >= CONTRASTE_MINIMAL, "$nom sur fond clair : ${contraste(c, SchemaClair.background)}")
        }
    }

    /** Mission 063 : en sombre, le texte d'état est onSurface, l'erreur est `error` du thème, sur le fond réel (>= 4,5). */
    @Test fun `en sombre, les textes d'etat se lisent sur le fond sans plaque (4,5 au moins)`() {
        val fond = CouleursEtat.fond(true)
        teintes.forEach { (nom, c) ->
            val texte = CouleursEtat.texte(c, true)
            assertTrue(contraste(texte, fond) >= CONTRASTE_MINIMAL, "$nom : texte ${contraste(texte, fond)}")
        }
        assertTrue(contraste(CouleursEtat.erreur(true), fond) >= CONTRASTE_MINIMAL, "erreur : ${contraste(CouleursEtat.erreur(true), fond)}")
        assertTrue(contraste(CouleursEtat.neutre(true), fond) >= CONTRASTE_MINIMAL)
    }

    /** Sur les fonds des cartes de liste et des boutons (surfaceContainer, surfaceContainerHigh), pas seulement sur surface. */
    @Test fun `en sombre, erreur et onSurface se lisent aussi sur les fonds des conteneurs`() {
        listOf(SchemaSombre.surfaceContainer, SchemaSombre.surfaceContainerHigh, SchemaSombre.background).forEach { fond ->
            assertTrue(contraste(SchemaSombre.onSurface, fond) >= CONTRASTE_MINIMAL, "onSurface sur $fond")
            assertTrue(contraste(SchemaSombre.error, fond) >= CONTRASTE_MINIMAL, "error sur $fond")
        }
    }

    /** En clair, rien ne change : textes dans la teinte d'état, erreur en rouge, neutre en noir, lisibles sur le fond. */
    @Test fun `en clair, les textes d'etat sont inchanges et se lisent sur le fond (4,5 au moins)`() {
        val fond = CouleursEtat.fond(false)
        teintes.forEach { (nom, c) ->
            assertEquals(c, CouleursEtat.texte(c, false), nom)
            assertTrue(contraste(c, fond) >= CONTRASTE_MINIMAL, "$nom : ${contraste(c, fond)}")
        }
        assertEquals(Palette.Rouge, CouleursEtat.erreur(false))
        assertEquals(Palette.Noir, CouleursEtat.neutre(false))
    }

    /** WCAG 1.4.11 : en sombre, le filet blanc contre le fond ET chaque teinte (disque « inconnu » noir compris) contre le filet. */
    @Test fun `en sombre, chaque pastille cernee de blanc se detache du fond et de son filet (3 au moins)`() {
        val fond = CouleursEtat.fond(true)
        assertTrue(contraste(filet, fond) >= CONTRASTE_PASTILLE, "filet sur fond : ${contraste(filet, fond)}")
        teintes.forEach { (nom, c) ->
            assertTrue(contraste(c, filet) >= CONTRASTE_PASTILLE, "$nom contre le filet : ${contraste(c, filet)}")
        }
    }

    @Test fun `en clair, chaque pastille se detache du fond (3 au moins)`() {
        val fond = CouleursEtat.fond(false)
        teintes.forEach { (nom, c) ->
            assertTrue(contraste(c, fond) >= CONTRASTE_PASTILLE, "$nom : ${contraste(c, fond)}")
        }
    }

    /** Badge du fix et bloc de l'essai : blanc sur teinte, modèle inchangé (>= 4,5). */
    @Test fun `le blanc sur chaque teinte d'etat se lit (4,5 au moins)`() {
        listOf(Palette.Vert, Palette.Orange, Palette.Rouge).forEach { c ->
            assertTrue(contraste(Palette.Blanc, c) >= CONTRASTE_MINIMAL, "$c : ${contraste(Palette.Blanc, c)}")
        }
    }

    /** Mission 062 : les textes neutres prennent onSurface du thème, posés sur le fond réel (Surface de MainActivity = surface). */
    @Test fun `le texte neutre (onSurface) se lit sur le fond, en clair comme en sombre`() {
        assertTrue(contraste(SchemaClair.onSurface, SchemaClair.surface) >= CONTRASTE_MINIMAL, "clair : ${contraste(SchemaClair.onSurface, SchemaClair.surface)}")
        assertTrue(contraste(SchemaSombre.onSurface, SchemaSombre.surface) >= CONTRASTE_MINIMAL, "sombre : ${contraste(SchemaSombre.onSurface, SchemaSombre.surface)}")
    }
}
