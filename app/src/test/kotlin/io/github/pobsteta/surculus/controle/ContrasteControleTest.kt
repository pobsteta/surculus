package io.github.pobsteta.surculus.controle

import io.github.pobsteta.surculus.carte.CONTRASTE_MINIMAL
import io.github.pobsteta.surculus.carte.CONTRASTE_PASTILLE
import io.github.pobsteta.surculus.carte.CouleursEtat
import io.github.pobsteta.surculus.carte.Palette
import io.github.pobsteta.surculus.carte.contraste
import io.github.pobsteta.surculus.core.placette.EtatDsf
import io.github.pobsteta.surculus.ui.theme.SchemaClair
import io.github.pobsteta.surculus.ui.theme.SchemaSombre
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Mission 012-B1 : contraste des textes et pastilles de l'écran du contrôle, en clair et en sombre ; cibles de 48 dp au moins. */
class ContrasteControleTest {
    private fun fond(sombre: Boolean) = CouleursEtat.fond(sombre)

    @Test fun `les textes de l'ecran se lisent a 4,5 pour 1 en clair et en sombre`() {
        listOf(false, true).forEach { sombre ->
            val schema = if (sombre) SchemaSombre else SchemaClair
            val textes = mapOf(
                "neutre" to CouleursEtat.neutre(sombre),
                "erreur" to CouleursEtat.erreur(sombre),
                "gris de l'etat precedent et de la source (onSurfaceVariant)" to schema.onSurfaceVariant,
            )
            textes.forEach { (nom, c) ->
                assertTrue(contraste(c, fond(sombre)) >= CONTRASTE_MINIMAL, "$nom, sombre=$sombre : ${contraste(c, fond(sombre))}")
            }
        }
    }

    @Test fun `le texte du bouton plein (Terminer) se lit sur son fond`() {
        listOf(SchemaClair, SchemaSombre).forEach { s ->
            assertTrue(contraste(s.onPrimary, s.primary) >= CONTRASTE_MINIMAL, "texte du bouton plein")
        }
    }

    @Test fun `en clair, chaque pastille d'etat se distingue du fond a 3 pour 1`() {
        EtatDsf.entries.forEach { e ->
            assertTrue(contraste(ApparenceEtat.teinte(e), fond(false)) >= CONTRASTE_PASTILLE, "${e.code} : ${contraste(ApparenceEtat.teinte(e), fond(false))}")
        }
    }

    @Test fun `en sombre, le filet blanc des pastilles se distingue du fond a 3 pour 1`() {
        assertTrue(contraste(CouleursEtat.filet, fond(true)) >= CONTRASTE_PASTILLE)
    }

    @Test fun `les quatre etats ont chacun leur forme et leur teinte, celles de la palette, rien d'autre`() {
        val formes = EtatDsf.entries.map { ApparenceEtat.forme(it) }
        assertEquals(4, formes.toSet().size)
        assertEquals(listOf("●", "▲", "✖", "○"), formes.map { it.symbole })
        assertEquals(
            listOf(Palette.Vert, Palette.Orange, Palette.Rouge, Palette.Noir),
            listOf(EtatDsf.SAIN, EtatDsf.ATTEINT, EtatDsf.MORT, EtatDsf.ABSENT).map { ApparenceEtat.teinte(it) },
        )
    }

    @Test fun `aucune cible tactile de l'ecran n'est sous 48 dp, les etats sont plus gros`() {
        DimensionsControle.toutes().forEach { assertTrue(it >= DimensionsControle.CIBLE_MIN, "$it") }
        assertTrue(DimensionsControle.BOUTON_ETAT.value >= 64f)
    }
}
