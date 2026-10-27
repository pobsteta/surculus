package io.github.pobsteta.surculus.core.carte

import io.github.pobsteta.surculus.core.gnss.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Mission 057 : le marqueur de position n'est pas dessiné quand le fix tombe hors de France (la Somalie, `CLAUDE.md`). */
class MarqueurPositionTest {
    // Position(latitude, longitude), degrés décimaux.
    private val fixCouchey = Position(latitude = 47.26, longitude = 4.93)

    /** Latitude et longitude inversées : (4,93 N ; 47,26 E) est en Somalie. */
    private val fixSomalie = Position(latitude = 4.93, longitude = 47.26)

    @Test
    fun `un fix en France est dessine`() {
        assertEquals(fixCouchey, MarqueurPosition.aDessiner(fixCouchey))
    }

    @Test
    fun `un fix en Somalie n'est pas dessine`() {
        assertNull(MarqueurPosition.aDessiner(fixSomalie))
    }

    @Test
    fun `pas de fix, pas de marqueur`() {
        assertNull(MarqueurPosition.aDessiner(null))
    }
}
