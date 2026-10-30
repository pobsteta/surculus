package io.github.pobsteta.surculus.core.guidage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LibellesTest {
    @Test
    fun `distance en metres puis en kilometres`() {
        assertEquals("0 m", Libelles.distance(0.4))
        assertEquals("29 m", Libelles.distance(29.4))
        assertEquals("999 m", Libelles.distance(999.4))
        assertEquals("1,3 km", Libelles.distance(1_250.0))
    }

    /** Mission 074 : arrondi (plus de troncature), seuil du kilomètre jugé sur la valeur arrondie, virgule française. */
    @Test
    fun `distance arrondie au metre puis kilometre a une decimale`() {
        assertEquals("0 m", Libelles.distance(0.0))
        assertEquals("1 m", Libelles.distance(0.5))
        assertEquals("30 m", Libelles.distance(29.99))
        assertEquals("999 m", Libelles.distance(998.5))
        assertEquals("1,0 km", Libelles.distance(999.5)) // arrondi à 1 000 m : on ne montre jamais « 1000 m »
        assertEquals("1,0 km", Libelles.distance(1_000.0))
        assertEquals("1,3 km", Libelles.distance(1_250.0))
        assertEquals("10,0 km", Libelles.distance(9_979.54))
        assertEquals("10,0 km", Libelles.distance(10_049.0))
        assertEquals("10,1 km", Libelles.distance(10_050.0))
    }

    @Test
    fun `metres entiers avec espace fine insecable aux milliers`() {
        assertEquals("0 m", Libelles.metres(0.0))
        assertEquals("999 m", Libelles.metres(999.4))
        assertEquals("1\u202F000 m", Libelles.metres(999.5))
        assertEquals("9\u202F980 m", Libelles.metres(9_979.54))
        assertEquals("10\u202F049 m", Libelles.metres(10_049.0))
    }

    @Test
    fun `points cardinaux aux bornes`() {
        assertEquals("N", Libelles.pointCardinal(0.0))
        assertEquals("N", Libelles.pointCardinal(359.9))
        assertEquals("NE", Libelles.pointCardinal(45.0))
        assertEquals("E", Libelles.pointCardinal(90.0))
        assertEquals("S", Libelles.pointCardinal(180.0))
        assertEquals("SO", Libelles.pointCardinal(225.0))
        assertEquals("O", Libelles.pointCardinal(270.0))
        assertEquals("NO", Libelles.pointCardinal(337.4))
    }

    @Test
    fun `cap sur trois chiffres, jamais 360`() {
        assertEquals("047° NE", Libelles.cap(47.0))
        assertEquals("000° N", Libelles.cap(359.6))
        assertEquals("005° N", Libelles.cap(5.0))
    }
}
