package io.github.pobsteta.surculus.core.reperes

import io.github.pobsteta.surculus.core.geo.EmpriseLambert93
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.geo.UniteAzimut
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PointDeriveTest {

    /** Piquet de Couchey (fixture C14), Lambert-93 x = E, y = N, mètres. */
    private val piquetCouchey = PointLambert93(xMetres = 850469.39, yMetres = 6686306.33)
    private val jour = LocalDate.of(2026, 10, 15)

    private fun az(v: Double, u: UniteAzimut = UniteAzimut.DEGRES) = AzimutSaisi.valider(v, u).valeurOuEchec()

    @Test
    fun `le piquet de Couchey revient en latitude 47,26 puis longitude 4,99`() {
        // pyproj 3.6.1 : Transformer.from_crs(2154, 4326, always_xy=True) → (4.99000006, 47.26000002)
        val p = PointGeographique.depuisLambert93(piquetCouchey)
        assertEquals(47.26, p.latitudeDeg, 1e-6)
        assertEquals(4.99, p.longitudeDeg, 1e-6)
    }

    @Test
    fun `un plant derive a Couchey tombe en France et garde tous les termes`() {
        val plant = PointDerive.deriver(piquetCouchey, CodeEpsg.RGF93_V1_LAMBERT93, 25.0, az(123.0), jour).valeurOuEchec()
        assertTrue(EmpriseLambert93.FRANCE_METROPOLITAINE.contient(plant.position))
        assertEquals(CodeEpsg.RGF93_V1_LAMBERT93, plant.epsg)
        assertEquals(123.0, plant.azimut.valeur)
        assertEquals(jour, plant.date)
        assertEquals(25.0, kotlin.math.hypot(plant.dxM, plant.dyM), 1e-9)
        val o = plant.orientation
        assertEquals(o.azimutMagnetiqueDeg + o.declinaisonDeg - o.convergenceDeg, o.azimutGrilleDeg, 1e-9)
    }

    @Test
    fun `axes inverses, le piquet est refuse`() {
        val inverse = PointLambert93(xMetres = piquetCouchey.yMetres, yMetres = piquetCouchey.xMetres)
        val r = PointDerive.deriver(inverse, CodeEpsg.RGF93_V1_LAMBERT93, 10.0, az(0.0), jour)
        assertEquals("piquet_hors_france", (r as Resultat.Refuse).refus.motif)
    }

    @Test
    fun `date hors validite, aucun plant derive`() {
        val r = PointDerive.deriver(piquetCouchey, CodeEpsg.RGF93_V1_LAMBERT93, 10.0, az(0.0), LocalDate.of(2030, 1, 1))
        assertEquals("date_hors_validite", (r as Resultat.Refuse).refus.motif)
    }

    @Test
    fun `distance negative refusee`() {
        val r = PointDerive.deriver(piquetCouchey, CodeEpsg.RGF93_V1_LAMBERT93, -1.0, az(0.0), jour)
        assertEquals("distance_invalide", (r as Resultat.Refuse).refus.motif)
    }

    @Test
    fun `200 grades vers le sud, le plant est au sud du piquet`() {
        val plant = PointDerive.deriver(piquetCouchey, CodeEpsg.RGF93_V1_LAMBERT93, 10.0, az(200.0, UniteAzimut.GRADES), jour).valeurOuEchec()
        assertTrue(plant.dyM < -9.9)
        assertTrue(plant.position.yMetres < piquetCouchey.yMetres)
    }

    @Test
    fun `en 9794 la position est la meme qu'en 2154 et garde son code`() {
        val a = PointDerive.deriver(piquetCouchey, CodeEpsg.RGF93_V1_LAMBERT93, 10.0, az(45.0), jour).valeurOuEchec()
        val b = PointDerive.deriver(piquetCouchey, CodeEpsg.RGF93_V2B_LAMBERT93, 10.0, az(45.0), jour).valeurOuEchec()
        assertEquals(a.position, b.position)
        assertEquals(CodeEpsg.RGF93_V2B_LAMBERT93, b.epsg)
    }

    @Test
    fun `point geographique hors limites refuse`() {
        assertTrue(PointGeographique.de(91.0, 0.0) is Resultat.Refuse)
        assertTrue(PointGeographique.de(0.0, 181.0) is Resultat.Refuse)
        assertTrue(PointGeographique.de(Double.NaN, 0.0) is Resultat.Refuse)
    }

    @Test
    fun `un fichier qui mele 2154 et 9794 est refuse`() {
        assertEquals(CodeEpsg.RGF93_V1_LAMBERT93, ReferentielTournee.verifier(listOf(2154, 2154)).valeurOuEchec())
        assertEquals(CodeEpsg.RGF93_V2B_LAMBERT93, ReferentielTournee.verifier(listOf(9794)).valeurOuEchec())
        val mele = (ReferentielTournee.verifier(listOf(2154, 9794, 2154)) as Resultat.Refuse).refus
        assertEquals("referentiels_meles", mele.motif)
        assertEquals(setOf(2154, 9794), (mele as RefusRepere.ReferentielsMeles).codes)
        assertEquals("referentiel_non_pris_en_charge", (ReferentielTournee.verifier(listOf(4326)) as Resultat.Refuse).refus.motif)
        assertEquals("referentiel_non_pris_en_charge", (ReferentielTournee.verifier(listOf(2154, 4326)) as Resultat.Refuse).refus.motif)
        assertEquals("aucun_referentiel", (ReferentielTournee.verifier(emptyList()) as Resultat.Refuse).refus.motif)
    }
}
