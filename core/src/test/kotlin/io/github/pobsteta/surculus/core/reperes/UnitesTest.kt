package io.github.pobsteta.surculus.core.reperes

import io.github.pobsteta.surculus.core.geo.UniteAzimut
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UnitesTest {

    private fun refus(r: Resultat<*>) = (r as Resultat.Refuse).refus

    @Test
    fun `un azimut hors plage est refuse avec sa valeur brute, jamais normalisee`() {
        for ((v, u) in listOf(360.0 to UniteAzimut.DEGRES, 720.0 to UniteAzimut.DEGRES, -0.001 to UniteAzimut.DEGRES,
            400.0 to UniteAzimut.GRADES, 800.0 to UniteAzimut.GRADES, -1.0 to UniteAzimut.GRADES)) {
            val f = refus(AzimutSaisi.valider(v, u)) as RefusRepere.AzimutHorsPlage
            assertEquals(v, f.valeurSaisie, "valeur gardée telle quelle")
            assertEquals(u, f.unite)
        }
    }

    @Test
    fun `valeurs non finies refusees`() {
        for (v in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            for (u in UniteAzimut.entries) assertTrue(AzimutSaisi.valider(v, u) is Resultat.Refuse, "$v $u")
        }
    }

    @Test
    fun `les grades sont convertis sans normalisation et la saisie est gardee`() {
        val a = AzimutSaisi.valider(399.999, UniteAzimut.GRADES).valeurOuEchec()
        assertEquals(399.999, a.valeur)
        assertEquals(UniteAzimut.GRADES, a.unite)
        assertEquals(359.9991, a.degres, 1e-9)
        assertEquals(90.0, AzimutSaisi.valider(100.0, UniteAzimut.GRADES).valeurOuEchec().degres, 1e-12)
    }

    @Test
    fun `codes d'unite de boussole lus strictement`() {
        assertEquals(UniteAzimut.DEGRES, CodesUniteAzimut.lire("deg").valeurOuEchec())
        assertEquals(UniteAzimut.GRADES, CodesUniteAzimut.lire("gr").valeurOuEchec())
        assertEquals("unite_azimut_non_declaree", refus(CodesUniteAzimut.lire(null)).motif)
        assertEquals("unite_azimut_non_declaree", refus(CodesUniteAzimut.lire(" ")).motif)
        for (c in listOf("mil", "DEG", "grad", "°")) assertEquals("unite_azimut_inconnue", refus(CodesUniteAzimut.lire(c)).motif, c)
        assertEquals("gr", CodesUniteAzimut.code(UniteAzimut.GRADES))
    }

    @Test
    fun `ouverture de tournee, boussole et pente toutes deux declarees`() {
        val t = UnitesTournee.declarer("gr", "pct").valeurOuEchec()
        assertEquals(UniteAzimut.GRADES, t.uniteAzimut)
        assertEquals(UnitePente.POURCENT, t.unitePente)
        assertEquals("unite_azimut_non_declaree", refus(UnitesTournee.declarer(null, "pct")).motif)
        assertEquals("unite_pente_non_declaree", refus(UnitesTournee.declarer("deg", null)).motif)
        assertEquals("unite_pente_inconnue", refus(UnitesTournee.declarer("deg", "grade")).motif)
        assertEquals("unite_azimut_inconnue", refus(UnitesTournee.declarer("mil", "pct")).motif)
    }

    @Test
    fun `pente negative acceptee entre -90 et 90 exclus, symetrique`() {
        val desc = DistanceHorizontale.calculer(10.0, -20.0, UnitePente.DEGRES).valeurOuEchec()
        val mont = DistanceHorizontale.calculer(10.0, 20.0, UnitePente.DEGRES).valeurOuEchec()
        assertEquals(mont, desc, 1e-12)
        assertEquals(9.8058, DistanceHorizontale.calculer(10.0, -20.0, UnitePente.POURCENT).valeurOuEchec(), 1e-4)
        assertTrue(DistanceHorizontale.calculer(10.0, -89.9, UnitePente.DEGRES) is Resultat.Accepte)
        assertTrue(DistanceHorizontale.calculer(10.0, -90.0, UnitePente.DEGRES) is Resultat.Refuse)
        assertTrue(DistanceHorizontale.calculer(10.0, 90.0, UnitePente.DEGRES) is Resultat.Refuse)
    }

    @Test
    fun `distance et pente non valides refusees`() {
        assertEquals("distance_invalide", refus(DistanceHorizontale.calculer(-1.0, null, UnitePente.DEGRES)).motif)
        assertEquals("distance_invalide", refus(DistanceHorizontale.calculer(Double.NaN, null, UnitePente.DEGRES)).motif)
        assertEquals("pente_hors_plage", refus(DistanceHorizontale.calculer(10.0, Double.NaN, UnitePente.POURCENT)).motif)
        assertEquals(10.0, DistanceHorizontale.calculer(10.0, null, UnitePente.POURCENT).valeurOuEchec())
    }
}
