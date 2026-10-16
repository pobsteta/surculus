package io.github.pobsteta.surculus.core.fixtures

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

/**
 * Rejeu des fixtures de `fixtures/regles/` (rattachement à 15 m, un contrôle par jour, emprise, plants)
 * contre l'implémentation de la mission 004. Un cas = un test nommé par son id.
 * Pour activer : retirer @Disabled et fournir [Implementations.reglesPlacette].
 */
class RejeuReglesPlacetteTest {

    private fun rejouer(fichiers: List<FichierFixture>): List<DynamicTest> {
        val regles = Implementations.reglesPlacette()
        return fichiers.flatMap { f ->
            f.cas.map { c ->
                dynamicTest(c.nom) {
                    val ecarts = Comparaison.ecarts(c.attendu, regles.appliquer(f.famille, c.entree))
                    assertTrue(ecarts.isEmpty(), "${c.id} (source : ${c.source}) : ${ecarts.joinToString("; ")}")
                }
            }
        }
    }

    @Disabled("en attente de la mission 004")
    @TestFactory
    fun `rattachement a 15 m`() = rejouer(Fixtures.tous().filter { it.famille == "rattachement-15m" })

    @Disabled("en attente de la mission 004")
    @TestFactory
    fun `un controle par placette et par jour`() = rejouer(Fixtures.tous().filter { it.famille == "un-controle-par-jour" })

    @Disabled("en attente de la mission 004")
    @TestFactory
    fun `emprise de la foret et France`() = rejouer(Fixtures.tous().filter { it.famille == "emprise-et-france" })

    @Disabled("en attente de la mission 004")
    @TestFactory
    fun `totaux du controle`() = rejouer(Fixtures.tous().filter { it.famille == "totaux-controle" })

    @Disabled("en attente de la mission 004")
    @TestFactory
    fun `regarni`() = rejouer(Fixtures.tous().filter { it.famille == "regarni" })

    @Disabled("en attente de la mission 004")
    @TestFactory
    fun `identite des plants`() = rejouer(Fixtures.tous().filter { it.famille == "identite" })

    @Disabled("en attente de la mission 004")
    @TestFactory
    fun `etats et causes`() = rejouer(Fixtures.tous().filter { it.famille == "etats-causes" })

    @Disabled("en attente de la mission 004")
    @TestFactory
    fun `numerotation des plants (cas de Groma)`() = rejouer(Fixtures.tous().filter { it.famille == "numerotation" })
}
