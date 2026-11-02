package io.github.pobsteta.surculus.core.placette

import io.github.pobsteta.surculus.core.geo.PointLambert93
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Mission 086, reprise 1/3 (dev) : cas limites de l'ordre des UG, hors ceux de `TriNumeriqueUgTest`. */
class OrdreUgTest {
    private fun p(id: String, ug: String?) = Piquet(UuidPlacette(id), "P01", PointLambert93(850_469.0, 6_686_306.0), ug = ug)

    @Test
    fun `nombres d'abord, puis textes, puis sans UG, egalite departagee par l'UUID`() {
        val l = listOf(p("z", null), p("y", "B"), p("x", "10"), p("w", "A"), p("v", "9"), p("u", "09"))
        assertEquals(listOf("u", "v", "x", "w", "y", "z"), l.sortedWith(OrdreUg).map { it.placette.valeur })
    }
}
