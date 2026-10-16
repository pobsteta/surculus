package io.github.pobsteta.surculus.core.placette

import io.github.pobsteta.surculus.core.geo.EmpriseDegres
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.geo.UniteAzimut
import io.github.pobsteta.surculus.core.reperes.AzimutSaisi
import io.github.pobsteta.surculus.core.reperes.CodeEpsg
import io.github.pobsteta.surculus.core.reperes.PointDerive
import io.github.pobsteta.surculus.core.reperes.PointGeographique
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

/** Rayon de placette, rattachement par le code, état du plant remplacé, points en France ; arbitrage de Pascal du 2026-10-16 (spec § 6). */
class PointageEtRattachementTest {

    /** Piquet à Couchey, EPSG:2154 (x = E, y = N, m) : place de dépôt PD-01, voir PlacetteEnFranceTest. */
    private val piquetCouchey = PointLambert93(xMetres = 846_164.76, yMetres = 6_686_123.26)
    private val placette = Placette(UuidPlacette("P-001"), rayonM = 3.99)

    private fun derive(distanceM: Double) = PointDerive.deriver(
        piquetCouchey, CodeEpsg.RGF93_V1_LAMBERT93, distanceM,
        AzimutSaisi.valider(45.0, UniteAzimut.DEGRES).valeurOuEchec(), LocalDate.of(2026, 10, 15),
    ).valeurOuEchec()

    private fun pointer(distanceM: Double, rayon: Placette = placette, confirme: Boolean = false) =
        Pointage.pointer(UuidPlant("u1"), rayon, derive(distanceM), Instant.parse("2026-10-15T09:00:00Z"), confirmeHorsRayon = confirme)

    // Au-delà du rayon + 0,10 m, alerte et confirmation, jamais de refus.

    @Test
    fun `3,99 m pas d'alerte`() {
        assertNull(ControleRayon.verifier(3.99, 3.99))
        assertTrue(pointer(3.99).estAccepte)
    }

    @Test
    fun `4,09 m a la borne exacte pas d'alerte`() {
        assertNull(ControleRayon.verifier(4.09, 3.99))
        assertTrue(pointer(4.09).estAccepte)
    }

    @Test
    fun `4,10 m alerte`() {
        assertNotNull(ControleRayon.verifier(4.10, 3.99))
    }

    @Test
    fun `4,10 m sans confirmation reste a confirmer, plant non enregistre`() {
        val v = pointer(4.10)
        assertTrue(v is Verdict.AConfirmer)
        assertEquals("plant_hors_rayon", (v as Verdict.AConfirmer).alerte.motif)
        assertNull(v.valeurOuNull())
    }

    @Test
    fun `4,10 m avec confirmation le plant est enregistre`() {
        val p = pointer(4.10, confirme = true).valeurOuEchec()
        assertEquals(UuidPlant("u1"), p.uuid)
        assertEquals(4.10, p.derive.distanceHorizontaleM, 1e-12)
        assertFalse(p.regarni)
    }

    @Test
    fun `le rayon est lu sur la placette`() {
        val grande = Placette(UuidPlacette("P-002"), rayonM = 5.64)
        assertTrue(pointer(5.70, grande).estAccepte)
        assertTrue(pointer(5.80, grande) is Verdict.AConfirmer)
    }

    @Test
    fun `le plant pointe a Couchey tombe en France, pas en Somalie`() {
        val p = pointer(3.0).valeurOuEchec()
        assertTrue(EmpriseEtFrance.enFrance(p.derive.position))
        val geo = PointGeographique.depuisLambert93(p.derive.position)
        assertTrue(EmpriseEtFrance.enFrance(geo))
        assertEquals(47.26, geo.latitudeDeg, 0.01)
        assertEquals(4.93, geo.longitudeDeg, 0.01)
    }

    @Test
    fun `le domaine en degres est celui d'EPSG 2154, Couchey dedans, la Somalie dehors`() {
        val france = EmpriseDegres.FRANCE_METROPOLITAINE
        assertTrue(france.contient(latitudeDeg = 47.26, longitudeDeg = 4.93))
        assertFalse(france.contient(latitudeDeg = 4.99, longitudeDeg = 47.26))
    }

    @Test
    fun `le rattachement rend l'UUID de la placette et son code`() {
        val r = Rattachement.rattacher(listOf(p1), p1.position).valeurOuEchec()
        assertEquals(UuidPlacette("uuid-1"), r.piquet.placette)
        assertEquals("P-001", r.piquet.code)
    }

    @Test
    fun `Couchey saisi en (lon, lat) au lieu de (lat, lon) est detecte`() {
        val inverse = PointGeographique.de(latitudeDeg = 4.93, longitudeDeg = 47.26).valeurOuEchec()
        val c = EmpriseEtFrance.controler(inverse)
        assertFalse(c.enFrance)
        assertTrue(c.inversionDetectee)
        assertTrue(c.refuse)
    }

    // Le code lu l'emporte sur le GNSS ; par identité seulement au-delà de 15 m ; égalité par le code.

    private val p1 = Piquet(UuidPlacette("uuid-1"), "P-001", PointLambert93(850_469.0, 6_686_306.0))
    private val p2 = Piquet(UuidPlacette("uuid-2"), "P-002", PointLambert93(850_479.0, 6_686_306.0))

    @Test
    fun `code lu P-002 a 10 m alors que P-001 est a 5 m`() {
        val nord = Piquet(UuidPlacette("uuid-2"), "P-002", PointLambert93(850_469.0, 6_686_321.0)) // 15 m au nord de P-001
        val mesuree = PointLambert93(850_469.0, 6_686_311.0) // 5 m de P-001, 10 m de P-002
        assertEquals("P-001", Rattachement.rattacher(listOf(p1, nord), mesuree).valeurOuEchec().piquet.code)
        val r = Rattachement.rattacher(listOf(p1, nord), mesuree, codeLu = "P-002").valeurOuEchec()
        assertEquals("P-002", r.piquet.code)
        assertEquals(10.0, r.distanceM, 1e-9)
        assertFalse(r.parIdentite)
        assertEquals(mesuree, r.positionControle)
    }

    @Test
    fun `egalite de distance sans code, le plus petit code`() {
        val milieu = PointLambert93(850_474.0, 6_686_306.0)
        assertEquals("P-001", Rattachement.rattacher(listOf(p2, p1), milieu).valeurOuEchec().piquet.code)
    }

    @Test
    fun `code lu a plus de 15 m rattache par identite a la position stockee`() {
        val loin = PointLambert93(850_469.0, 6_686_346.0)
        val r = Rattachement.rattacher(listOf(p1), loin, codeLu = "P-001").valeurOuEchec()
        assertTrue(r.parIdentite)
        assertEquals(p1.position, r.positionControle)
        assertEquals(loin, r.positionMesuree)
    }

    // Dernier état observé au plus tard le jour de la pose, non observés ignorés.

    private val j1 = LocalDate.of(2026, 10, 1)
    private val j2 = LocalDate.of(2026, 10, 2)
    private val m1 = IdentitePlant(UuidPlant("m1"), UuidPlacette("P-001"), 1)
    private val r1 = IdentitePlant(UuidPlant("r1"), UuidPlacette("P-001"), 2, remplace = UuidPlant("m1"))

    private fun verifier(historique: List<ObservationDatee>, pose: LocalDate) =
        Regarnis.verifier(listOf(m1), { Regarnis.dernierEtatObserve(historique, pose) }, r1)

    @Test
    fun `sain au controle 1, non observe au controle 2, regarni pose au controle 2 refuse`() {
        val h = listOf(ObservationDatee(j1, Observation.Observe(EtatDsf.SAIN)), ObservationDatee(j2, Observation.NonObserve))
        assertEquals("plant_remplace_ni_mort_ni_absent", (verifier(h, j2) as Verdict.Refuse).refus.motif)
    }

    @Test
    fun `mort au controle 1, non observe au controle 2, regarni pose au controle 2 accepte`() {
        val h = listOf(ObservationDatee(j1, Observation.Observe(EtatDsf.MORT)), ObservationDatee(j2, Observation.NonObserve))
        assertTrue(verifier(h, j2).estAccepte)
    }

    @Test
    fun `un etat posterieur a la pose n'est pas lu, un plant jamais vu est refuse`() {
        val h = listOf(ObservationDatee(j1, Observation.Observe(EtatDsf.SAIN)), ObservationDatee(j2, Observation.Observe(EtatDsf.MORT)))
        assertEquals(EtatDsf.SAIN, Regarnis.dernierEtatObserve(h, j1))
        assertFalse(verifier(h, j1).estAccepte)
        assertFalse(verifier(emptyList(), j1).estAccepte)
    }

    @Test
    fun `jour civil Europe-Paris et non UTC`() {
        assertEquals(LocalDate.of(2026, 10, 15), JourCivil.de(Instant.parse("2026-10-14T22:30:00Z")))
        val premier = ControleEcrit(UuidControle("c1"), UuidPlacette("P-001"), LocalDate.of(2026, 10, 15))
        val e = UnControleParJour.ecrire(listOf(premier), UuidControle("c2"), UuidPlacette("P-001"), Instant.parse("2026-10-15T08:00:00Z"))
        assertEquals(EcritureControle.Corrige(premier), e)
    }
}
