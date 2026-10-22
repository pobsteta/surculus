package io.github.pobsteta.surculus.core.fixtures

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.io.File

/**
 * Forme des CSV de `docs/referentiels/` (mission 035) : sur chaque ligne de données, autant de champs
 * que l'en-tête. Une virgule non protégée par des guillemets décale une colonne sans rien casser d'autre.
 * Lecture selon la RFC 4180 ; les lignes `#` avant l'en-tête sont des commentaires ; une ligne vide
 * n'est tolérée qu'en fin de fichier.
 */
object FormeCsv {
    /** Un enregistrement et la ligne physique (à partir de 1) où il commence ; `champs` vaut 0 pour une ligne vide. */
    data class Enregistrement(val ligne: Int, val champs: Int)

    private class GuilletNonFerme(val ligne: Int) : Exception()

    private fun lire(texte: String): List<Enregistrement> {
        val sortie = mutableListOf<Enregistrement>()
        var ligne = 1
        var debut = 1
        var champs = 1
        var vide = true
        var entreGuillemets = false
        var i = 0
        fun clore() {
            sortie += Enregistrement(debut, if (vide) 0 else champs)
            champs = 1
            vide = true
        }
        while (i < texte.length) {
            val c = texte[i]
            if (entreGuillemets) {
                if (c == '"') {
                    if (i + 1 < texte.length && texte[i + 1] == '"') i++ else entreGuillemets = false
                } else if (c == '\n') {
                    ligne++
                }
            } else {
                when (c) {
                    '"' -> { entreGuillemets = true; vide = false }
                    ',' -> { champs++; vide = false }
                    '\r' -> {}
                    '\n' -> { clore(); ligne++; debut = ligne }
                    else -> vide = false
                }
            }
            i++
        }
        if (entreGuillemets) throw GuilletNonFerme(debut)
        if (!vide) clore()
        return sortie
    }

    /** Défauts de forme, « fichier ligne N : ... » ; liste vide si le fichier est bien formé. */
    fun verifier(nom: String, texte: String): List<String> {
        val brut = texte.lines()
        var decalage = 0
        while (decalage < brut.size && brut[decalage].startsWith("#")) decalage++
        val corps = brut.drop(decalage).joinToString("\n")
        val enregistrements = try {
            lire(corps).map { it.copy(ligne = it.ligne + decalage) }
        } catch (e: GuilletNonFerme) {
            return listOf("$nom ligne ${e.ligne + decalage} : guillemet ouvert et jamais refermé")
        }
        if (enregistrements.isEmpty() || enregistrements[0].champs == 0) return listOf("$nom : en-tête absent")
        val entete = enregistrements[0]
        val defauts = mutableListOf<String>()
        val dernier = enregistrements.lastIndex
        enregistrements.drop(1).forEachIndexed { k, e ->
            val estDernier = k + 1 == dernier
            when {
                e.champs == 0 && estDernier -> {}
                e.champs == 0 -> defauts += "$nom ligne ${e.ligne} : ligne vide au milieu du fichier"
                e.champs != entete.champs ->
                    defauts += "$nom ligne ${e.ligne} : ${e.champs} champs au lieu de ${entete.champs} (en-tête ligne ${entete.ligne})"
            }
        }
        return defauts
    }
}

class ReferentielsCsvBienFormesTest {

    private val entete = "a,b,c\n"

    @Test
    fun `virgules protegees par des guillemets, guillemet double et fin de ligne dans un champ`() {
        val t = entete + "1,\"x, y\",3\n4,\"dit \"\"oui\"\"\",6\n7,\"deux\nlignes\",9\n10,11,12\n"
        assertEquals(emptyList<String>(), FormeCsv.verifier("t.csv", t))
        val fautif = entete + "7,\"deux\nlignes\",9\n10,11\n"
        assertEquals(listOf("t.csv ligne 4 : 2 champs au lieu de 3 (en-tête ligne 1)"), FormeCsv.verifier("t.csv", fautif))
    }

    @Test
    fun `commentaires d'en-tete ignores et lignes comptees depuis le debut du fichier`() {
        val t = "# commentaire, avec virgule\n# autre \"guillemet\n$entete" + "1,2,3\n1,2\n"
        assertEquals(listOf("t.csv ligne 5 : 2 champs au lieu de 3 (en-tête ligne 3)"), FormeCsv.verifier("t.csv", t))
    }

    @Test
    fun `virgule non protegee, ligne trop courte ou trop longue`() {
        // cas type de couts-plantation.csv avant 22ec874 : "A, B, C, D" sans guillemets
        val t = entete + "x,A, B, C,D\n1,2\n1,2,3\n1,2,3,4\n"
        assertEquals(
            listOf(
                "t.csv ligne 2 : 5 champs au lieu de 3 (en-tête ligne 1)",
                "t.csv ligne 3 : 2 champs au lieu de 3 (en-tête ligne 1)",
                "t.csv ligne 5 : 4 champs au lieu de 3 (en-tête ligne 1)",
            ),
            FormeCsv.verifier("t.csv", t),
        )
    }

    @Test
    fun `ligne vide finale toleree, ligne vide au milieu refusee`() {
        assertEquals(emptyList<String>(), FormeCsv.verifier("t.csv", entete + "1,2,3\n\n"))
        assertEquals(emptyList<String>(), FormeCsv.verifier("t.csv", entete + "1,2,3"))
        assertEquals(
            listOf("t.csv ligne 3 : ligne vide au milieu du fichier"),
            FormeCsv.verifier("t.csv", entete + "1,2,3\n\n4,5,6\n"),
        )
    }

    @Test
    fun `fins de ligne CRLF acceptees`() {
        assertEquals(emptyList<String>(), FormeCsv.verifier("t.csv", "a,b\r\n1,2\r\n"))
    }

    @Test
    fun `guillemet jamais referme et en-tete absent signales`() {
        assertEquals(
            listOf("t.csv ligne 2 : guillemet ouvert et jamais refermé"),
            FormeCsv.verifier("t.csv", entete + "1,\"2,3\n"),
        )
        assertEquals(listOf("t.csv : en-tête absent"), FormeCsv.verifier("t.csv", "# seulement un commentaire\n"))
    }

    private val csv: List<File> by lazy {
        File(Depot.racine, "docs/referentiels").listFiles { f -> f.extension == "csv" }!!.sortedBy { it.name }
    }

    @Test
    fun `le dossier des referentiels contient bien des CSV`() {
        assertTrue(csv.size >= 8, "CSV trouvés : ${csv.map { it.name }}")
    }

    @TestFactory
    fun `chaque CSV de docs-referentiels est bien forme`(): List<DynamicTest> = csv.map { f ->
        dynamicTest(f.name) {
            val defauts = FormeCsv.verifier(f.name, f.readText())
            assertTrue(defauts.isEmpty(), "CSV mal formé :\n" + defauts.joinToString("\n"))
        }
    }
}
