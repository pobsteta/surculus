package io.github.pobsteta.surculus.controle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Mission 067 : le côté du plan, calculé par l'écran (`PlanCanvas`) avec [cotePlanDp] ; debout toute la largeur, couché borné par la hauteur. */
class DispositionControleTest {
    @Test fun `debout, le plan garde toute la largeur`() {
        assertEquals(328f, cotePlanDp(largeurDp = 328f, hauteurEcranDp = 800f))
        assertEquals(500f, cotePlanDp(largeurDp = 500f, hauteurEcranDp = 900f))
    }

    @Test fun `couche, le plan tient dans la hauteur disponible`() {
        val c = cotePlanDp(largeurDp = 780f, hauteurEcranDp = 360f)
        assertTrue(c <= 360f - RESERVE_HAUTEUR_PLAN_DP, "côté $c")
        assertTrue(c > 0f)
    }

    @Test fun `le cote n'est jamais negatif ni plus grand que la largeur`() {
        listOf(0f, 50f, 300f, 800f).forEach { l -> listOf(0f, 90f, 360f, 900f).forEach { h ->
            assertTrue(cotePlanDp(l, h) in 0f..l, "l=$l h=$h")
        } }
    }
}
