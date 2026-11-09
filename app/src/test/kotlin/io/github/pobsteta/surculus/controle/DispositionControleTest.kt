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
        // Garde muette de la 104 : la ligne ci-dessus suit la constante, elle ne voit pas une réserve ramenée à 0. La barre
        // d'application (BandeauCompact, 48 dp) doit rester déduite, quelle que soit la valeur de la réserve.
        assertTrue(c <= 360f - 48f, "côté $c : le plan couché doit laisser au moins 48 dp (barre d'application)")
        assertTrue(c > 0f)
    }

    @Test fun `le cote n'est jamais negatif ni plus grand que la largeur`() {
        listOf(0f, 50f, 300f, 800f).forEach { l -> listOf(0f, 90f, 360f, 900f).forEach { h ->
            assertTrue(cotePlanDp(l, h) in 0f..l, "l=$l h=$h")
        } }
    }
}
