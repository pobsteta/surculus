package io.github.pobsteta.surculus.core.placette

import io.github.pobsteta.surculus.core.geo.PointLambert93
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Mission 086, reprise 1/3 (mineur du relecteur) : les UG candidates sont triées comme des nombres
 * (« UG 9 » avant « UG 12 »), pas comme des textes (« 12 » avant « 9 »), quel que soit l'ordre de la tournée.
 * Aucune API nouvelle : `Verdict.AChoisirUg.candidats` doit sortir triée.
 */
class TriNumeriqueUgTest {
    private val origine = PointLambert93(850_469.0, 6_686_306.0)
    private fun p(ug: String, dx: Double) = Piquet(UuidPlacette("uuid-ug$ug"), "P01", PointLambert93(origine.xMetres + dx, origine.yMetres), ug = ug)

    private val ug9 = p("9", 100.0)
    private val ug10 = p("10", 200.0)
    private val ug12 = p("12", 300.0)
    private val ug100 = p("100", 400.0)

    @Test
    fun `les candidates sortent en ordre numerique, quel que soit l'ordre de la tournee`() {
        val mesuree = PointLambert93(origine.xMetres + 1000.0, origine.yMetres) // loin de toutes : l'UG est demandee
        val ordres = listOf(
            listOf(ug12, ug9, ug100, ug10),
            listOf(ug100, ug12, ug10, ug9),
            listOf(ug9, ug10, ug12, ug100),
        )
        for (tournee in ordres) {
            val v = Rattachement.rattacher(tournee, mesuree, codeLu = "P01")
            val c = v as? Verdict.AChoisirUg ?: error("attendu : AChoisirUg, obtenu : $v")
            assertEquals(listOf("9", "10", "12", "100"), c.candidats.map { it.ug })
        }
    }
}
