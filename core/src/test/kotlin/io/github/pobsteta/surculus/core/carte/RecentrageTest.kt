package io.github.pobsteta.surculus.core.carte

import io.github.pobsteta.surculus.core.geo.EmpriseLambert93
import io.github.pobsteta.surculus.core.gnss.Position
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

/** Le recentrage ne suit jamais un fix hors de France : la placette en Somalie (`CLAUDE.md`, chapitre 15). */
class RecentrageTest {
    private val couchey = EmpriseLambert93(846_000.0, 6_686_000.0, 846_400.0, 6_686_400.0)

    // Position(latitude, longitude), degrés décimaux.
    private val fixCouchey = Position(latitude = 47.26, longitude = 4.93)

    /** Latitude et longitude inversées : (4,93 N ; 47,26 E) est en Somalie. */
    private val fixSomalie = Position(latitude = 4.93, longitude = 47.26)

    @Test
    fun `un fix a Couchey recentre la carte sur lui`() {
        assertEquals(Recentrage.Cible.Fix(fixCouchey), Recentrage.cible(fixCouchey, couchey))
    }

    @Test
    fun `un fix en Somalie ne recentre pas la carte, repli sur l'emprise`() {
        assertEquals(Recentrage.Cible.Emprise, Recentrage.cible(fixSomalie, couchey))
    }

    @Test
    fun `sans fix, repli sur l'emprise`() {
        assertEquals(Recentrage.Cible.Emprise, Recentrage.cible(null, couchey))
    }

    @Test
    fun `fix en Somalie et emprise hors de France, rien`() {
        val somalie = EmpriseLambert93(45.0, 2.0, 46.0, 3.0)
        assertEquals(Recentrage.Cible.Rien, Recentrage.cible(fixSomalie, somalie))
        assertEquals(Recentrage.Cible.Rien, Recentrage.cible(null, null))
    }

    @Test
    fun `fix en France mais emprise hors de France, on suit le fix`() {
        val somalie = EmpriseLambert93(45.0, 2.0, 46.0, 3.0)
        assertInstanceOf(Recentrage.Cible.Fix::class.java, Recentrage.cible(fixCouchey, somalie))
    }
}
