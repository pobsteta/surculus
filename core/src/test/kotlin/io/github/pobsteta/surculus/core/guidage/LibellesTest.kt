package io.github.pobsteta.surculus.core.guidage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LibellesTest {
    @Test
    fun `distance en metres puis en kilometres`() {
        assertEquals("0 m", Libelles.distance(0.4))
        assertEquals("29 m", Libelles.distance(29.99))
        assertEquals("999 m", Libelles.distance(999.9))
        assertEquals("1,3 km", Libelles.distance(1_250.0))
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
