package io.github.pobsteta.surculus.core.geo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** ADR 0003 : un azimut saisi hors plage est refusé avant toute normalisation. */
class AzimutTest {

    @Test
    fun `degres hors de 0 a 360 exclu sont refuses, jamais normalises`() {
        for (v in listOf(-0.1, -90.0, 360.0, 360.5, 720.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertThrows<IllegalArgumentException>("$v deg") { Azimut.versDegres(v, UniteAzimut.DEGRES) }
            assertThrows<IllegalArgumentException>("$v deg") { Azimut.verifier(v, UniteAzimut.DEGRES) }
        }
    }

    @Test
    fun `grades hors de 0 a 400 exclu sont refuses, jamais normalises`() {
        for (v in listOf(-0.1, -100.0, 400.0, 400.1, 800.0, Double.NaN)) {
            assertThrows<IllegalArgumentException>("$v gr") { Azimut.versDegres(v, UniteAzimut.GRADES) }
        }
    }

    @Test
    fun `des grades pris pour des degres ne passent pas en silence`() {
        // 380 grades est valide en grades (342°) mais refusé comme degrés : la bonne unité est déclarée.
        assertEquals(342.0, Azimut.versDegres(380.0, UniteAzimut.GRADES), 1e-9)
        assertThrows<IllegalArgumentException> { Azimut.versDegres(380.0, UniteAzimut.DEGRES) }
    }

    @Test
    fun `bornes incluses a 0 et exclues au tour complet`() {
        assertEquals(0.0, Azimut.versDegres(0.0, UniteAzimut.DEGRES))
        assertEquals(0.0, Azimut.versDegres(0.0, UniteAzimut.GRADES))
        assertEquals(359.999, Azimut.versDegres(359.999, UniteAzimut.DEGRES), 1e-12)
        assertEquals(359.91, Azimut.versDegres(399.9, UniteAzimut.GRADES), 1e-9)
    }

    @Test
    fun `grades et degres sont coherents dans les deux sens`() {
        assertEquals(90.0, Azimut.versDegres(100.0, UniteAzimut.GRADES), 1e-9)
        assertEquals(100.0, Azimut.depuisDegres(90.0, UniteAzimut.GRADES), 1e-9)
        assertEquals(45.0, Azimut.depuisDegres(45.0, UniteAzimut.DEGRES), 1e-12)
        assertThrows<IllegalArgumentException> { Azimut.depuisDegres(360.0, UniteAzimut.GRADES) }
    }

    @Test
    fun `la chaine magnetique geographique grille refuse un azimut hors plage`() {
        assertThrows<IllegalArgumentException> { Azimut.magnetiqueVersGeographique(361.0, 1.0) }
        assertThrows<IllegalArgumentException> { Azimut.magnetiqueVersGeographique(-1.0, 1.0) }
        assertThrows<IllegalArgumentException> { Azimut.geographiqueVersGrille(360.0, 1.0) }
        // Le résultat, lui, est ramené dans [0, 360[ : 359 + 2 = 1.
        assertEquals(1.0, Azimut.magnetiqueVersGeographique(359.0, 2.0), 1e-9)
        assertEquals(359.0, Azimut.geographiqueVersGrille(1.0, 2.0), 1e-9)
    }

    @Test
    fun `convergence Lambert-93 a Brest et a Strasbourg (valeurs de l ADR 0003)`() {
        // ADR 0003 : γ = −5,43° à Brest (lon −4,486°) et +3,45° à Strasbourg (lon 7,752°).
        assertEquals(-5.43, Azimut.convergenceLambert93(-4.486), 0.01)
        assertEquals(3.45, Azimut.convergenceLambert93(7.752), 0.01)
        assertEquals(0.0, Azimut.convergenceLambert93(3.0), 1e-12)
    }

    @Test
    fun `azimut du vecteur est compte depuis le nord dans le sens horaire`() {
        assertEquals(0.0, Azimut.azimut(dx = 0.0, dy = 1.0), 1e-9)
        assertEquals(90.0, Azimut.azimut(dx = 1.0, dy = 0.0), 1e-9)
        assertEquals(180.0, Azimut.azimut(dx = 0.0, dy = -1.0), 1e-9)
        assertEquals(270.0, Azimut.azimut(dx = -1.0, dy = 0.0), 1e-9)
    }

    @Test
    fun `ecart signe et distance horizontale`() {
        assertEquals(-20.0, Azimut.ecart(350.0, 10.0), 1e-9)
        assertEquals(20.0, Azimut.ecart(10.0, 350.0), 1e-9)
        assertEquals(4.0, Azimut.distanceHorizontale(5.0, 3.0), 1e-12)
        assertThrows<IllegalArgumentException> { Azimut.distanceHorizontale(3.0, 5.0) }
    }
}
