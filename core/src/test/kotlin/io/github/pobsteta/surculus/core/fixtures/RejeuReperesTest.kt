package io.github.pobsteta.surculus.core.fixtures

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

/**
 * Rejeu des fixtures de `fixtures/reperes/` (convergence, déclinaison WMM2025, chaîne d'azimut,
 * unités d'azimut, distance horizontale) contre l'implémentation de la mission 005.
 * Pour activer : retirer @Disabled et fournir [Implementations.reperes].
 */
class RejeuReperesTest {

    private fun rejouer(famille: String): List<DynamicTest> {
        val reperes = Implementations.reperes()
        return Fixtures.tous().filter { it.famille == famille }.flatMap { f ->
            f.cas.map { c ->
                dynamicTest(c.nom) {
                    val ecarts = Comparaison.ecarts(c.attendu, reperes.appliquer(f.famille, c.entree))
                    assertTrue(ecarts.isEmpty(), "${c.id} (source : ${c.source}) : ${ecarts.joinToString("; ")}")
                }
            }
        }
    }

    @TestFactory
    fun `convergence des meridiens`() = rejouer("convergence")

    @TestFactory
    fun `declinaison WMM2025 et dates hors validite`() = rejouer("declinaison-wmm2025")

    @TestFactory
    fun `chaine d'azimut`() = rejouer("chaine-azimut")

    @TestFactory
    fun `unites d'azimut`() = rejouer("unites-azimut")

    @TestFactory
    fun `distance horizontale et unite de pente`() = rejouer("distance-horizontale")
}
