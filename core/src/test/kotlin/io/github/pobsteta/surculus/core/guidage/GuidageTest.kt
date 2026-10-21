package io.github.pobsteta.surculus.core.guidage

import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.geo.Projections
import io.github.pobsteta.surculus.core.gnss.Position
import io.github.pobsteta.surculus.core.placette.Piquet
import io.github.pobsteta.surculus.core.placette.Rattachement
import io.github.pobsteta.surculus.core.placette.RefusPlacette
import io.github.pobsteta.surculus.core.placette.UuidPlacette
import io.github.pobsteta.surculus.core.placette.Verdict
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Guidage (spec § 4.2) : distance, azimut, seuil d'arrivée strict à 30 m, bip ; puis rattachement à 15 m (§ 4.3). */
class GuidageTest {
    /** Piquet à Couchey, EPSG:2154 : place de dépôt PD-01 (PlacetteEnFranceTest). */
    private val piquet = PointLambert93(846_164.76, 6_686_123.26)

    private fun position(estM: Double, nordM: Double): Position {
        val (lon, lat) = Projections.versWgs84(piquet.xMetres + estM, piquet.yMetres + nordM)
        return Position(latitude = lat, longitude = lon)
    }

    @Test
    fun `29,99 m est dans le cercle, 30 m pile n'y est pas`() {
        assertTrue(Guidage.estArrive(29.99))
        assertFalse(Guidage.estArrive(30.0))
        assertFalse(Guidage.estArrive(30.01))
        assertTrue(Guidage.estArrive(0.0))
    }

    @Test
    fun `au sud de la placette, cap au nord`() {
        val cap = Guidage.cap(position(0.0, -100.0), piquet)!!
        assertEquals(100.0, cap.distanceM, 0.5)
        // Azimut géographique : le nord de la grille s'écarte du nord vrai de la convergence (environ 1,4° à Couchey).
        assertTrue(cap.azimutDeg < 3.0 || cap.azimutDeg > 357.0, "azimut = ${cap.azimutDeg}")
        assertFalse(cap.arrive)
    }

    @Test
    fun `a l'ouest de la placette, cap a l'est`() {
        val cap = Guidage.cap(position(-50.0, 0.0), piquet)!!
        assertEquals(50.0, cap.distanceM, 0.5)
        assertEquals(90.0, cap.azimutDeg, 2.0)
    }

    @Test
    fun `a 29 m le cap dit arrive, a 31 m non`() {
        assertTrue(Guidage.cap(position(29.0, 0.0), piquet)!!.arrive)
        assertFalse(Guidage.cap(position(31.0, 0.0), piquet)!!.arrive)
    }

    @Test
    fun `une position hors de France ne guide pas`() {
        // Latitude et longitude inversées : (4,97 N ; 47,2 E) est en Somalie.
        assertNull(Guidage.cap(Position(latitude = 4.97, longitude = 47.2), piquet))
        assertNull(Guidage.versLambert93(Position(0.0, 0.0)))
    }

    @Test
    fun `le bip accelere a l'approche et se tait dans le cercle`() {
        val loin = Guidage.periodeBipMs(500.0)!!
        val milieu = Guidage.periodeBipMs(100.0)!!
        val pres = Guidage.periodeBipMs(30.0)!!
        assertTrue(loin > milieu && milieu > pres, "$loin $milieu $pres")
        assertEquals(2000L, loin)
        assertEquals(500L, pres)
        assertNull(Guidage.periodeBipMs(29.99))
    }

    @Test
    fun `un seul bip d'arrivee par entree, rearme au-dela de 40 m`() {
        val d = DetecteurArrivee()
        assertFalse(d.entree(80.0))
        assertTrue(d.entree(29.0))
        assertFalse(d.entree(28.0))
        assertFalse(d.entree(31.0)) // oscillation du GNSS autour de 30 m
        assertFalse(d.entree(29.0)) // pas de deuxième bip
        assertFalse(d.entree(45.0)) // sorti, ré-armé
        assertTrue(d.entree(25.0))
    }

    @Test
    fun `la moyenne de positions`() {
        val m = MoyennePosition()
        assertNull(m.moyenne())
        m.ajouter(PointLambert93(100.0, 200.0))
        m.ajouter(PointLambert93(102.0, 204.0))
        assertEquals(PointLambert93(101.0, 202.0), m.moyenne())
        assertEquals(2, m.nombre)
    }

    private val piquets = listOf(
        Piquet(UuidPlacette("u-01"), "P35-01", piquet),
        Piquet(UuidPlacette("u-02"), "P35-02", PointLambert93(piquet.xMetres + 80.0, piquet.yMetres)),
    )

    @Test
    fun `a 14 m le piquet est rattache par le GNSS`() {
        val v = Rattachement.rattacher(piquets, PointLambert93(piquet.xMetres + 14.0, piquet.yMetres))
        val r = (v as Verdict.Accepte).valeur
        assertEquals("P35-01", r.piquet.code)
        assertFalse(r.parIdentite)
    }

    @Test
    fun `a 16 m sans code refus, avec le code lu rattache par identite sur la position du piquet`() {
        val mesuree = PointLambert93(piquet.xMetres - 16.0, piquet.yMetres)
        val refus = assertInstanceOf(Verdict.Refuse::class.java, Rattachement.rattacher(piquets, mesuree))
        assertInstanceOf(RefusPlacette.HorsQuinzeMetres::class.java, refus.refus)
        val r = (Rattachement.rattacher(piquets, mesuree, codeLu = "P35-01") as Verdict.Accepte).valeur
        assertTrue(r.parIdentite)
        assertEquals(piquet, r.positionControle)
        assertEquals(mesuree, r.positionMesuree)
    }

    @Test
    fun `un code inconnu est refuse`() {
        val v = Rattachement.rattacher(piquets, piquet, codeLu = "X99")
        assertInstanceOf(RefusPlacette.CodeInconnu::class.java, (v as Verdict.Refuse).refus)
    }
}
