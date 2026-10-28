package io.github.pobsteta.surculus.carte

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Mission 064 : les six boutons de la carte tiennent dans la hauteur visible, en portrait (une colonne, rien ne change)
 * comme en paysage (deux colonnes). Téléphone de Pascal : 1080 x 2316 px à 450 dpi, soit 2,8125 px par dp.
 */
class DispositionBoutonsTest {
    private val dp = 450f / 160f

    // Hauteur de la zone de carte mesurée sur la capture v0.7.67 en paysage : environ 740 px.
    private val carteCoucheePascal = 740f / dp
    // Portrait : environ 1400 px de carte visible.
    private val cartePortraitPascal = 1400f / dp
    private val petitEcranCouche = 200f

    @Test
    fun `en portrait les six boutons restent en une colonne`() {
        assertEquals(1, DispositionBoutons.colonnes(cartePortraitPascal, 6))
        assertEquals(1, DispositionBoutons.colonnes(cartePortraitPascal, 5))
    }

    @Test
    fun `en paysage sur le telephone de Pascal, deux colonnes`() {
        assertEquals(2, DispositionBoutons.colonnes(carteCoucheePascal, 6))
        assertEquals(3, DispositionBoutons.boutonsParColonne(carteCoucheePascal, 6))
    }

    @Test
    fun `sur un petit ecran couche, chaque colonne tient dans la hauteur`() {
        val par = DispositionBoutons.boutonsParColonne(petitEcranCouche, 6)
        assertTrue(DispositionBoutons.hauteurColonneDp(par) <= petitEcranCouche)
        assertEquals(3, par)
        assertEquals(2, DispositionBoutons.colonnes(petitEcranCouche, 6))
    }

    @Test
    fun `les colonnes tiennent toujours dans la hauteur quand c'est possible, et la cible fait 48 dp`() {
        assertEquals(48, DispositionBoutons.CIBLE_DP)
        for (h in listOf(carteCoucheePascal, cartePortraitPascal, petitEcranCouche, 120f)) {
            val par = DispositionBoutons.boutonsParColonne(h, 6)
            assertTrue(par == 1 || DispositionBoutons.hauteurColonneDp(par) <= h, "h=$h par=$par")
        }
    }

    @Test
    fun `la carte utilise la disposition en colonnes`() {
        val racine = File("src/main/kotlin/io/github/pobsteta/surculus").takeIf { it.isDirectory }
            ?: File("app/src/main/kotlin/io/github/pobsteta/surculus")
        val src = File(racine, "carte/EcranCarte.kt").readText()
        assertTrue("DispositionBoutons.boutonsParColonne" in src)
        assertTrue("FlowColumn" in src)
    }
}
