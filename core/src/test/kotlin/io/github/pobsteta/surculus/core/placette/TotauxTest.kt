package io.github.pobsteta.surculus.core.placette

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** Totaux déduits des plants (spec § 4.3 et § 6) ; arrondi de h_moy_cm (demi vers le haut, arbitrage de Pascal du 2026-10-16 (spec § 6)). */
class TotauxTest {

    private var n = 0
    private fun sain(h: Int? = null) = EtatPlant(UuidPlant("p${n++}"), Observation.Observe(EtatDsf.SAIN, hauteurCm = h))

    private fun hMoy(vararg h: Int) = Totaux.deduire(h.map { sain(it) }).valeurOuEchec().hMoyCm

    @Test
    fun `moyenne 42,5 arrondie a 43`() = assertEquals(43, hMoy(40, 45))

    @Test
    fun `moyenne 40,67 arrondie a 41`() = assertEquals(41, hMoy(40, 41, 41))

    @Test
    fun `moyenne 40,33 arrondie a 40`() = assertEquals(40, hMoy(40, 40, 41))

    @Test
    fun `aucune hauteur donne null`() = assertNull(Totaux.deduire(listOf(sain(), sain())).valeurOuEchec().hMoyCm)

    @Test
    fun `la hauteur moyenne est un entier`() {
        val h: Int? = Totaux.deduire(listOf(sain(41), sain(42))).valeurOuEchec().hMoyCm
        assertEquals(42, h) // 41,5 → 42, demi vers le haut
    }

    @Test
    fun `arrondi demi vers le haut`() {
        assertEquals(1, Totaux.arrondiDemiVersLeHaut(0.5))
        assertEquals(0, Totaux.arrondiDemiVersLeHaut(0.49))
        assertEquals(51, Totaux.arrondiDemiVersLeHaut(50.5))
    }

    @Test
    fun `invariants de sommieR 0_28_0 verifies a la construction`() {
        assertThrows<IllegalArgumentException> { Totaux(nbTotal = 2, nbVivants = 3, nbAbroutis = 0, hMoyCm = null) }
        assertThrows<IllegalArgumentException> { Totaux(nbTotal = 3, nbVivants = 2, nbAbroutis = 3, hMoyCm = null) }
    }

    @Test
    fun `un plant remplace a un controle anterieur sort des totaux, le non observe aussi`() {
        val m1 = EtatPlant(UuidPlant("m1"), Observation.Observe(EtatDsf.MORT))
        val r1 = EtatPlant(UuidPlant("r1"), Observation.Observe(EtatDsf.SAIN))
        val n1 = EtatPlant(UuidPlant("n1"), Observation.NonObserve)
        val t = Totaux.deduire(listOf(m1, r1, n1), remplaces = setOf(UuidPlant("m1"))).valeurOuEchec()
        assertEquals(Totaux(nbTotal = 1, nbVivants = 1, nbAbroutis = 0, hMoyCm = null), t)
    }

    @Test
    fun `un meme plant deux fois dans les etats d'un controle est refuse, pas compte deux fois`() {
        val v = Totaux.deduire(
            listOf(
                EtatPlant(UuidPlant("s1"), Observation.Observe(EtatDsf.SAIN)),
                EtatPlant(UuidPlant("s1"), Observation.Observe(EtatDsf.MORT)),
            ),
        )
        assertEquals(Verdict.Refuse(RefusPlacette.PlantEnDoubleDansControle(UuidPlant("s1"))), v)
    }

    @Test
    fun `placette papier de 12 plants`() {
        // Échantillon type pour Pascal : 5 sains (dont 1 abrouti), 3 atteints (dont 1 abrouti), 2 morts (dont 1 abrouti),
        // 1 absent, 1 non observé ; hauteurs 35, 48, 52 (vivants) et 80 (mort, ignorée).
        val etats = listOf(
            Observation.Observe(EtatDsf.SAIN, hauteurCm = 35),
            Observation.Observe(EtatDsf.SAIN, causeDetail = "abroutissement"),
            Observation.Observe(EtatDsf.SAIN),
            Observation.Observe(EtatDsf.SAIN),
            Observation.Observe(EtatDsf.SAIN, hauteurCm = 48),
            Observation.Observe(EtatDsf.ATTEINT, GroupeCause.ANIMAUX, "abroutissement", 52),
            Observation.Observe(EtatDsf.ATTEINT, GroupeCause.ENTOMOLOGIQUE, "hylobe"),
            Observation.Observe(EtatDsf.ATTEINT),
            Observation.Observe(EtatDsf.MORT, GroupeCause.ANIMAUX, "abroutissement", 80),
            Observation.Observe(EtatDsf.MORT, GroupeCause.ABIOTIQUE_COMPLEXE_INDETERMINE, "gel"),
            Observation.Observe(EtatDsf.ABSENT),
            Observation.NonObserve,
        ).mapIndexed { k, o -> EtatPlant(UuidPlant("p$k"), o) }
        // (35 + 48 + 52) / 3 = 45
        assertEquals(Totaux(nbTotal = 11, nbVivants = 8, nbAbroutis = 2, hMoyCm = 45), Totaux.deduire(etats).valeurOuEchec())
    }
}
