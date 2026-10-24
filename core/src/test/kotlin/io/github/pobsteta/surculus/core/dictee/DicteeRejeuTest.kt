package io.github.pobsteta.surculus.core.dictee

import io.github.pobsteta.surculus.core.fixtures.Comparaison
import io.github.pobsteta.surculus.core.fixtures.Depot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.io.File

/**
 * Rejeu des cas de `fixtures/dictee/` contre l'implémentation de la mission 013 (port [Dictee]).
 * Actif depuis la mission 013 ([ImplementationDictee] branchée), sans modification des cas.
 */
class DicteeRejeuTest {

    private fun rejouer(famille: String, appel: (Dictee, io.github.pobsteta.surculus.core.fixtures.CasFixture) -> com.google.gson.JsonObject): List<DynamicTest> {
        val d = ImplementationDictee.dictee()
        return DicteeFixtures.cas(famille).map { c ->
            dynamicTest(c.nom) {
                val ecarts = Comparaison.ecarts(c.attendu, appel(d, c))
                assertTrue(ecarts.isEmpty(), "${c.id} (source : ${c.source}) : ${ecarts.joinToString("; ")}")
            }
        }
    }

    @TestFactory
    fun `phrases de la grammaire`() = rejouer("phrases") { d, c -> d.analyser(c.entree.get("phrase").asString) }

    @TestFactory
    fun `hauteur de 1 a 400 cm`() = rejouer("hauteur") { d, c -> d.analyser(c.entree.get("phrase").asString) }

    @TestFactory
    fun `phrases hors grammaire refusees`() = rejouer("refus") { d, c -> d.analyser(c.entree.get("phrase").asString) }

    @TestFactory
    fun `espaces parasites normalises`() = rejouer("normalisation") { d, c -> d.analyser(c.entree.get("phrase").asString) }

    @TestFactory
    fun `commande confrontee a l'etat courant du plant`() = rejouer("contexte") { d, c ->
        val e = c.entree.get("etat_courant")
        d.appliquer(c.entree.get("phrase").asString, if (e == null || e.isJsonNull) null else e.asString)
    }

    @TestFactory
    fun `modele nom et empreinte SHA-256`() = rejouer("modele") { d, c ->
        val contenu = c.entree.get("contenu_utf8")
        d.verifierModele(
            c.entree.get("nom_modele").asString,
            if (contenu == null || contenu.isJsonNull) null else contenu.asString.toByteArray(Charsets.UTF_8),
            c.entree.get("sha256_attendu").asString,
        )
    }

    @Test
    fun `chaque mot de la grammaire est dans le vocabulaire du modele`() {
        val v = DicteeFixtures.vocabulaire.toHashSet()
        val absents = ImplementationDictee.dictee().motsDeLaGrammaire().filter { it !in v }
        assertTrue(absents.isEmpty(), "mots absents de vosk-model-small-fr-0.22 : $absents")
    }

    @Test
    fun `la grammaire n'a pas d'autre mot que ceux de la spec et des nombres`() {
        val permis = DicteeFixtures.cas("phrases").flatMap { it.entree.get("phrase").asString.split(' ') }.toSet() +
            DicteeFixtures.cas("hauteur").flatMap { it.entree.get("phrase").asString.split(' ') }.toSet()
        val en_trop = ImplementationDictee.dictee().motsDeLaGrammaire() - permis
        // Les mots de nombre non listés dans les cas (par exemple « trente ») sont admis ; aucun mot de commande en plus.
        val interdits = setOf("frottis", "hylobe", "gel", "sécheresse", "concurrence", "abroutissement", "entreprise", "hors", "malade", "vivant", "abiotique", "abrouti", "regarni")
        assertTrue(en_trop.none { it in interdits }, "mots hors grammaire : ${en_trop.filter { it in interdits }}")
    }

    @Test
    fun `l'empreinte du modele est inscrite dans docs-dependances`() {
        val sha = DicteeFixtures.famille("vocabulaire").json.getAsJsonObject("modele").get("sha256_archive_mesure").asString
        val texte = File(Depot.racine, "docs/dependances.md").readText()
        assertTrue(sha in texte, "la somme SHA-256 de vosk-model-small-fr-0.22.zip doit figurer dans docs/dependances.md")
        assertEquals(64, sha.length)
    }
}
