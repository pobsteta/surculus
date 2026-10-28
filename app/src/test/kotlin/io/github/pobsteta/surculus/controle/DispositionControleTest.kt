package io.github.pobsteta.surculus.controle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Mission 067 : le plan est un carré qui ne dépasse jamais son côté maximal, donc ne déborde pas sur les boutons couché. */
class DispositionControleTest {
    @Test fun `couche, le plan reste un carre de 320 dp au plus`() {
        assertEquals(320f, cotePlanDp(780f))
    }

    @Test fun `debout, le plan prend la largeur disponible`() {
        assertEquals(300f, cotePlanDp(300f))
    }

    @Test fun `le cote n'est jamais negatif ni superieur au maximum`() {
        listOf(0f, 100f, 320f, 640f, 2000f).forEach { assertTrue(cotePlanDp(it) in 0f..COTE_PLAN_MAX_DP) }
    }
}
