package io.github.pobsteta.surculus.carte

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Mission 078 : les boutons de la carte sont toujours en une seule colonne pleine hauteur, de 48 dp (quand elle tient) à
 * 32 dp (plancher absolu, décision de Pascal du 2026-10-30). Téléphone de Pascal : 2316 x 1080 px à 450 dpi, soit
 * 2,8125 px par dp (823 x 384 dp).
 */
class DispositionBoutonsTest {
    private val dp = 450f / 160f

    // Couché sans plein écran : 384 dp moins la barre de titre (~48 dp) = 336 dp. Avec barres (état, gestes) : ~292 dp.
    private val coucheSansBarres = 336f
    private val coucheAvecBarres = 292f
    private val debout = 2316f / dp - 48f

    @Test
    fun `debout les six boutons font 48 dp`() {
        assertEquals(48, DispositionBoutons.tailleDp(debout, 6))
        assertEquals(48, DispositionBoutons.tailleDp(debout, 5))
    }

    @Test
    fun `couche a 336 dp les six boutons font 45 dp`() {
        assertEquals(45, DispositionBoutons.tailleDp(coucheSansBarres, 6))
        assertEquals(334, DispositionBoutons.hauteurColonneDp(6, 45))
    }

    @Test
    fun `couche a 292 dp les six boutons descendent a 38 dp, sous 40`() {
        assertEquals(38, DispositionBoutons.tailleDp(coucheAvecBarres, 6))
        assertTrue(DispositionBoutons.hauteurColonneDp(6, 38) <= coucheAvecBarres)
        assertTrue(DispositionBoutons.hauteurColonneDp(6, 39) > coucheAvecBarres)
    }

    @Test
    fun `a 260 dp la taille est le plancher de 32 dp`() {
        assertEquals(32, DispositionBoutons.MIN_DP)
        assertEquals(32, DispositionBoutons.tailleDp(260f, 6))
    }

    @Test
    fun `plus court que le plancher, la taille reste 32 dp`() {
        for (h in listOf(255f, 200f, 120f, 0f)) assertEquals(32, DispositionBoutons.tailleDp(h, 6), "h=$h")
    }

    @Test
    fun `la taille ne depasse jamais 48 dp`() {
        assertEquals(48, DispositionBoutons.tailleDp(5000f, 6))
    }

    @Test
    fun `la place reservee aux textes est la largeur de la colonne`() {
        assertEquals(48 + 2 * DispositionBoutons.MARGE_DP, DispositionBoutons.largeurReserveeDp(48))
        assertEquals(32 + 2 * DispositionBoutons.MARGE_DP, DispositionBoutons.largeurReserveeDp(32))
    }

    @Test
    fun `la carte utilise une seule colonne et plus les deux colonnes de la 064`() {
        val racine = File("src/main/kotlin/io/github/pobsteta/surculus").takeIf { it.isDirectory }
            ?: File("app/src/main/kotlin/io/github/pobsteta/surculus")
        val src = File(racine, "carte/EcranCarte.kt").readText()
        assertTrue("DispositionBoutons.tailleDp" in src)
        assertFalse("FlowColumn" in src)
        assertFalse("boutonsParColonne" in src)
    }
}
