package io.github.pobsteta.surculus.core.fixtures

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

/**
 * Rejeu des fixtures de `fixtures/regles/` (rattachement à 15 m, un contrôle par jour, emprise, plants)
 * contre l'implémentation de la mission 004. Un cas = un test nommé par son id.
 * Branché sur [AdaptateurReglesPlacette] (mission 004).
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

    @TestFactory
    fun `rattachement a 15 m`() = rejouer(Fixtures.tous().filter { it.famille == "rattachement-15m" })

    @TestFactory
    fun `un controle par placette et par jour`() = rejouer(Fixtures.tous().filter { it.famille == "un-controle-par-jour" })

    @TestFactory
    fun `emprise de la foret et France`() = rejouer(Fixtures.tous().filter { it.famille == "emprise-et-france" })

    @TestFactory
    fun `totaux du controle`() = rejouer(Fixtures.tous().filter { it.famille == "totaux-controle" })

    @TestFactory
    fun `regarni`() = rejouer(Fixtures.tous().filter { it.famille == "regarni" })

    @TestFactory
    fun `identite des plants`() = rejouer(Fixtures.tous().filter { it.famille == "identite" })

    @TestFactory
    fun `etats et causes`() = rejouer(Fixtures.tous().filter { it.famille == "etats-causes" })

    @TestFactory
    fun `numerotation des plants (cas de Groma)`() = rejouer(Fixtures.tous().filter { it.famille == "numerotation" })
}
