package io.github.pobsteta.surculus.core.reperes

import io.github.pobsteta.surculus.core.geo.UniteAzimut
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** Mission 081 (décision de Pascal du 2026-10-30) : azimut, pente et point refusés sont dits sans décimal à point. */
class RefusRepereLisiblesTest {
    private val decimalRe = Regex("\\d\\.\\d")

    private fun sansTechnique(texte: String) {
        assertFalse(decimalRe.containsMatchIn(texte), "« $texte » contient un décimal à point")
        assertFalse('_' in texte, "« $texte » contient un « _ »")
        assertFalse("NaN" in texte || "Infinity" in texte, "« $texte » montre NaN ou Infinity")
    }

    @Test fun `un azimut hors plage est dit avec virgule francaise`() {
        assertEquals(
            "Azimut 400,0 hors de [0, 360[ en degrés : ressaisir la lecture.",
            RefusRepere.AzimutHorsPlage(400.0, UniteAzimut.DEGRES).message,
        )
        assertEquals(
            "Azimut -2,5 hors de [0, 400[ en grades : ressaisir la lecture.",
            RefusRepere.AzimutHorsPlage(-2.5, UniteAzimut.GRADES).message,
        )
        listOf(Double.NaN, Double.POSITIVE_INFINITY, 1e12).forEach {
            sansTechnique(RefusRepere.AzimutHorsPlage(it, UniteAzimut.DEGRES).message)
        }
    }

    @Test fun `une pente hors plage est dite avec virgule francaise`() {
        assertEquals(
            "Pente 120,5 % refusée : l'angle doit être dans ]-90°, 90°[.",
            RefusRepere.PenteHorsPlage(120.5, UnitePente.POURCENT).message,
        )
        assertEquals(
            "Pente 95,0° refusée : l'angle doit être dans ]-90°, 90°[.",
            RefusRepere.PenteHorsPlage(95.0, UnitePente.DEGRES).message,
        )
        listOf(Double.NaN, Double.NEGATIVE_INFINITY).forEach {
            sansTechnique(RefusRepere.PenteHorsPlage(it, UnitePente.DEGRES).message)
        }
    }

    @Test fun `un point hors limites est dit avec virgule francaise`() {
        // Ordre explicite : (latitude, longitude), en degrés.
        assertEquals(
            "Point (latitude 91,5, longitude 2,3) hors des limites géographiques.",
            RefusRepere.PointHorsLimites(latitudeDeg = 91.5, longitudeDeg = 2.3).message,
        )
        sansTechnique(RefusRepere.PointHorsLimites(Double.NaN, Double.POSITIVE_INFINITY).message)
    }
}
