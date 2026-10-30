package io.github.pobsteta.surculus.carte

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Mission 078 : les boutons de la carte sont toujours en une seule colonne pleine hauteur, à 48 dp quand elle tient,
 * jamais sous 40 dp. Téléphone de Pascal : 2316 x 1080 px à 450 dpi, soit 2,8125 px par dp (823 x 384 dp).
 */
class DispositionBoutonsTest {
    private val dp = 450f / 160f

    // Couché : 1080 px = 384 dp, moins la barre de titre (~48 dp) : environ 336 dp.
    private val ecranCouchePascal = 336f
    // Debout : 2316 px = 823 dp, moins la barre de titre : environ 775 dp.
    private val ecranDeboutPascal = 2316f / dp - 48f
    private val ecranTropCourt = 250f

    @Test
    fun `debout les six boutons font 48 dp`() {
        assertEquals(48, DispositionBoutons.tailleDp(ecranDeboutPascal, 6))
        assertEquals(48, DispositionBoutons.tailleDp(ecranDeboutPascal, 5))
    }

    @Test
    fun `couche sur le telephone de Pascal, la colonne tient entre 40 et 48 dp`() {
        val t = DispositionBoutons.tailleDp(ecranCouchePascal, 6)
        assertTrue(t in 40..48, "t=$t")
        assertTrue(DispositionBoutons.hauteurColonneDp(6, t) <= ecranCouchePascal)
        // Juste ce qu'il faut : un dp de plus ne tiendrait plus.
        if (t < 48) assertTrue(DispositionBoutons.hauteurColonneDp(6, t + 1) > ecranCouchePascal)
    }

    @Test
    fun `a 40 dp les six boutons tiennent dans 304 dp`() {
        assertEquals(304, DispositionBoutons.hauteurColonneDp(6, 40))
        assertEquals(40, DispositionBoutons.tailleDp(304f, 6))
    }

    @Test
    fun `la taille ne descend jamais sous 40 dp, meme sur un ecran trop court`() {
        assertEquals(40, DispositionBoutons.MIN_DP)
        for (h in listOf(ecranTropCourt, 120f, 0f)) {
            assertEquals(40, DispositionBoutons.tailleDp(h, 6), "h=$h")
        }
        assertFalse(DispositionBoutons.tient(ecranTropCourt, 6))
        assertTrue(DispositionBoutons.tient(ecranCouchePascal, 6))
    }

    @Test
    fun `avec cinq boutons la taille est au moins celle de six`() {
        assertTrue(DispositionBoutons.tailleDp(ecranCouchePascal, 5) >= DispositionBoutons.tailleDp(ecranCouchePascal, 6))
    }

    @Test
    fun `la place reservee aux textes est la largeur de la colonne`() {
        assertEquals(48 + 2 * DispositionBoutons.MARGE_DP, DispositionBoutons.largeurReserveeDp(48))
        assertEquals(40 + 2 * DispositionBoutons.MARGE_DP, DispositionBoutons.largeurReserveeDp(40))
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
