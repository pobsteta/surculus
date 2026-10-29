package io.github.pobsteta.surculus.controle

import io.github.pobsteta.surculus.carte.CONTRASTE_MINIMAL
import io.github.pobsteta.surculus.carte.CONTRASTE_PASTILLE
import io.github.pobsteta.surculus.carte.contraste
import io.github.pobsteta.surculus.ui.theme.SchemaClair
import io.github.pobsteta.surculus.ui.theme.SchemaSombre
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Mission 075 : le bouton micro comme celui de Marculus. Sans Compose : la place (fonction pure de l'écran, jamais de l'écoute),
 * l'aspect (fonction pure de l'écoute et de « prêt ») et les contrastes. « Il ne bouge pas sous le doigt » et « visible sans défiler »
 * ne se prouvent qu'à la capture.
 */
class MicroControleTest {
    @Test fun `couche, le micro est dans une colonne fixe de 128 dp a droite`() {
        assertEquals(EmplacementMicro.COLONNE, DispositionMicro.emplacement(largeurDp = 823f, hauteurDp = 384f))
        assertEquals(128f, DimensionsMicro.ZONE.value)
    }

    @Test fun `debout, le micro est dans un pied fixe sous la zone qui defile`() {
        assertEquals(EmplacementMicro.PIED, DispositionMicro.emplacement(largeurDp = 360f, hauteurDp = 800f))
        assertEquals(EmplacementMicro.PIED, DispositionMicro.emplacement(largeurDp = 500f, hauteurDp = 500f))
    }

    @Test fun `la place du micro ne depend que de l'ecran, et le bloc J'ECOUTE est en surimpression`() {
        // emplacement() n'a que la largeur et la hauteur : l'apparition du bloc ne peut pas déplacer le micro.
        assertTrue(DispositionMicro.ECOUTE_EN_SURIMPRESSION, "le bloc J'ÉCOUTE couvre la zone qui défile, il ne la pousse pas")
    }

    @Test fun `le micro fait 112 dp, son icone 48 dp, et la zone le contient`() {
        assertEquals(112f, DimensionsMicro.BOUTON.value)
        assertEquals(48f, DimensionsMicro.ICONE.value)
        assertTrue(DimensionsMicro.ZONE.value >= DimensionsMicro.BOUTON.value)
        assertTrue(DimensionsMicro.BOUTON.value >= DimensionsControle.CIBLE_MIN.value)
    }

    @Test fun `l'aspect - pret, ecoute, barre`() {
        assertEquals(EtatMicro.PRET, AspectMicro.pour(AffichageEcoute.RIEN, pret = true))
        assertEquals(EtatMicro.PRET, AspectMicro.pour(AffichageEcoute.FERMEE, pret = true))
        assertEquals(EtatMicro.ECOUTE, AspectMicro.pour(AffichageEcoute.OUVERTURE, pret = true))
        assertEquals(EtatMicro.ECOUTE, AspectMicro.pour(AffichageEcoute.ECOUTE, pret = true))
        AffichageEcoute.entries.forEach { assertEquals(EtatMicro.BARRE, AspectMicro.pour(it, pret = false), "$it") }
    }

    @Test fun `pret seulement si le micro est accorde et le modele installe`() {
        assertTrue(AspectMicro.pret(EtatPermissionMicro.ACCORDEE, modeleInstalle = true))
        assertFalse(AspectMicro.pret(EtatPermissionMicro.ACCORDEE, modeleInstalle = false))
        EtatPermissionMicro.entries.filter { it != EtatPermissionMicro.ACCORDEE }.forEach {
            assertFalse(AspectMicro.pret(it, modeleInstalle = true), "$it")
        }
    }

    @Test fun `les trois etats se distinguent par la couleur, l'ecoute est en rouge d'erreur`() {
        listOf(SchemaClair, SchemaSombre).forEach { s ->
            assertEquals(s.error, CouleursMicro.fond(EtatMicro.ECOUTE, s))
            assertEquals(s.onError, CouleursMicro.icone(EtatMicro.ECOUTE, s))
            assertEquals(s.primaryContainer, CouleursMicro.fond(EtatMicro.PRET, s))
            assertEquals(s.surfaceVariant, CouleursMicro.fond(EtatMicro.BARRE, s))
            assertNotEquals(CouleursMicro.fond(EtatMicro.PRET, s), CouleursMicro.fond(EtatMicro.ECOUTE, s))
            assertNotEquals(CouleursMicro.fond(EtatMicro.PRET, s), CouleursMicro.fond(EtatMicro.BARRE, s))
        }
    }

    @Test fun `l'icone se lit sur son fond dans les trois etats, en clair et en sombre`() {
        listOf(SchemaClair, SchemaSombre).forEach { s ->
            EtatMicro.entries.forEach { e ->
                val c = contraste(CouleursMicro.icone(e, s), CouleursMicro.fond(e, s))
                assertTrue(c >= CONTRASTE_MINIMAL, "$e, sombre=${s === SchemaSombre} : $c")
            }
        }
    }

    @Test fun `le filet du bouton se voit sur le fond de l'ecran, en clair et en sombre`() {
        listOf(SchemaClair, SchemaSombre).forEach { s ->
            val c = contraste(CouleursMicro.filet(s), s.surface)
            assertTrue(c >= CONTRASTE_PASTILLE, "filet : $c")
        }
    }
}
