package io.github.pobsteta.surculus.core.placette

import io.github.pobsteta.surculus.core.geo.PointLambert93
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Mission 086 : un même code de piquet dans plusieurs UG (ADR 0003, amendement du 2026-11-02, point 3 ; spec § 4.3 et § 6).
 *
 * Cas de test (données d'essai, pas une tournée réelle). API supposée, à confirmer par le dev :
 * - `Piquet(placette, code, position, ug: String? = null)` : l'UG de la placette ;
 * - `Verdict.AChoisirUg(candidats: List<Piquet>)` : nouveau cas de [Verdict], porte les placettes homonymes ;
 * - `Rattachement.rattacher(piquets, mesuree, codeLu, ugChoisie: String? = null)` : l'UG choisie par l'agent.
 */
class RattachementHomonymesTest {
    private val origine = PointLambert93(850_469.0, 6_686_306.0)

    /** Deux placettes « P01 » : UG 12 en [origine], UG 14 à 100 m à l'est. */
    private val ug12 = Piquet(UuidPlacette("uuid-ug12"), "P01", origine, ug = "12")
    private val ug14 = Piquet(UuidPlacette("uuid-ug14"), "P01", PointLambert93(850_569.0, 6_686_306.0), ug = "14")
    private val autre = Piquet(UuidPlacette("uuid-p02"), "P02", PointLambert93(850_469.0, 6_686_406.0), ug = "12")

    private fun ordres(vararg p: Piquet) = listOf(p.toList(), p.toList().reversed())

    private fun mesureeAuNordDe12(m: Double) = PointLambert93(origine.xMetres, origine.yMetres + m)

    private fun candidatsUg(v: Verdict<Rattache>): Set<String?> {
        val c = v as? Verdict.AChoisirUg ?: error("attendu : AChoisirUg, obtenu : $v")
        assertEquals(2, c.candidats.size)
        assertTrue(c.candidats.all { it.code == "P01" })
        return c.candidats.map { it.ug }.toSet()
    }

    @Test
    fun `code unique, avec une UG, inchange - 15 m puis identite au-dela`() {
        for (tournee in ordres(ug12, autre)) {
            val r = Rattachement.rattacher(tournee, mesureeAuNordDe12(5.0), codeLu = "P01").valeurOuEchec()
            assertEquals(ug12, r.piquet)
            assertFalse(r.parIdentite)
            val loin = Rattachement.rattacher(tournee, mesureeAuNordDe12(40.0), codeLu = "P01").valeurOuEchec()
            assertTrue(loin.parIdentite)
            assertEquals(ug12.position, loin.positionControle)
        }
    }

    @Test
    fun `deux homonymes, une seule a 14,9 m - rattachee sans question`() {
        for (tournee in ordres(ug12, ug14)) {
            val r = Rattachement.rattacher(tournee, mesureeAuNordDe12(14.9), codeLu = "P01").valeurOuEchec()
            assertEquals("uuid-ug12", r.piquet.placette.valeur)
            assertFalse(r.parIdentite)
        }
    }

    @Test
    fun `deux homonymes, une seule a 15,0 m exactement - borne incluse, rattachee sans question`() {
        for (tournee in ordres(ug12, ug14)) {
            val r = Rattachement.rattacher(tournee, mesureeAuNordDe12(15.0), codeLu = "P01").valeurOuEchec()
            assertEquals("uuid-ug12", r.piquet.placette.valeur)
            assertEquals(15.0, r.distanceM, 1e-9)
        }
    }

    @Test
    fun `deux homonymes, la plus proche a 15,1 m - aucune a 15 m, l'UG est demandee`() {
        for (tournee in ordres(ug12, ug14)) {
            val v = Rattachement.rattacher(tournee, mesureeAuNordDe12(15.1), codeLu = "P01")
            assertEquals(setOf<String?>("12", "14"), candidatsUg(v))
        }
    }

    @Test
    fun `deux homonymes, aucune a 15 m - le choix de l'UG ouvre la placette choisie, par identite`() {
        val mesuree = PointLambert93(850_519.0, 6_686_356.0) // environ 70 m de chacune
        for (tournee in ordres(ug12, ug14)) {
            assertEquals(setOf<String?>("12", "14"), candidatsUg(Rattachement.rattacher(tournee, mesuree, codeLu = "P01")))
            for (choisie in listOf(ug12, ug14)) {
                val r = Rattachement.rattacher(tournee, mesuree, codeLu = "P01", ugChoisie = choisie.ug).valeurOuEchec()
                assertEquals(choisie, r.piquet)
                assertTrue(r.parIdentite)
                assertEquals(choisie.position, r.positionControle)
                assertEquals(mesuree, r.positionMesuree)
            }
        }
    }

    @Test
    fun `deux homonymes, les deux a 15 m au plus - l'UG est demandee, puis le choix rattache sans identite`() {
        val proche = Piquet(UuidPlacette("uuid-ug14"), "P01", PointLambert93(origine.xMetres + 10.0, origine.yMetres), ug = "14")
        val mesuree = PointLambert93(origine.xMetres + 5.0, origine.yMetres) // 5 m de chacune
        for (tournee in ordres(ug12, proche)) {
            assertEquals(setOf<String?>("12", "14"), candidatsUg(Rattachement.rattacher(tournee, mesuree, codeLu = "P01")))
            val r = Rattachement.rattacher(tournee, mesuree, codeLu = "P01", ugChoisie = "14").valeurOuEchec()
            assertEquals("uuid-ug14", r.piquet.placette.valeur)
            assertFalse(r.parIdentite)
            assertEquals(mesuree, r.positionControle)
        }
    }

    @Test
    fun `jamais la premiere trouvee - l'ordre de la tournee ne change pas le resultat`() {
        val mesuree = PointLambert93(850_519.0, 6_686_356.0)
        val a = Rattachement.rattacher(listOf(ug12, ug14, autre), mesuree, codeLu = "P01")
        val b = Rattachement.rattacher(listOf(autre, ug14, ug12), mesuree, codeLu = "P01")
        assertEquals(candidatsUg(a), candidatsUg(b))
    }

    @Test
    fun `sans code lu - inchange, la plus proche a 15 m au plus`() {
        for (tournee in ordres(ug12, ug14, autre)) {
            val r = Rattachement.rattacher(tournee, mesureeAuNordDe12(5.0)).valeurOuEchec()
            assertEquals(ug12, r.piquet)
        }
    }
}
