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
    /** Un enregistrement et la ligne physique (à partir de 1) où il commence ; `champs` est vide pour une ligne vide. */
    data class Enregistrement(val ligne: Int, val champs: List<String>)

    private class GuillemetNonFerme(val ligne: Int) : Exception()

    /** Valeurs de la colonne `niveau_verification` (en-têtes des CSV, mission 030). */
    val niveauxVerification = setOf("verifie", "extrait", "non_trouve", "renvoi")

    /**
     * Colonne `zone` de couts-plantation.csv : vide, `toutes`, ou une lettre de zone du barème 2024 (A à E,
     * écart 1 du fichier) éventuellement suivie d'un libellé : `A (…)`, `A, B, C, D`, `C et D`.
     */
    private val zoneBareme = Regex("""^[A-E]([\s,].*)?$""")

    private fun lire(texte: String): List<Enregistrement> {
        val sortie = mutableListOf<Enregistrement>()
        var ligne = 1
        var debut = 1
        var champs = mutableListOf<String>()
        val courant = StringBuilder()
        var vide = true
        var entreGuillemets = false
        var i = 0
        fun clore() {
            if (vide) {
                sortie += Enregistrement(debut, emptyList())
            } else {
                champs += courant.toString()
                sortie += Enregistrement(debut, champs)
            }
            champs = mutableListOf()
            courant.clear()
            vide = true
        }
        while (i < texte.length) {
            val c = texte[i]
            if (entreGuillemets) {
                if (c == '"') {
                    if (i + 1 < texte.length && texte[i + 1] == '"') {
                        courant.append('"')
                        i++
                    } else {
                        entreGuillemets = false
                    }
                } else {
                    if (c == '\n') ligne++
                    courant.append(c)
                }
            } else {
                when (c) {
                    '"' -> { entreGuillemets = true; vide = false }
                    ',' -> { champs += courant.toString(); courant.clear(); vide = false }
                    '\r' -> {}
                    '\n' -> { clore(); ligne++; debut = ligne }
                    else -> { courant.append(c); vide = false }
                }
            }
            i++
        }
        if (entreGuillemets) throw GuillemetNonFerme(debut)
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
        } catch (e: GuillemetNonFerme) {
            return listOf("$nom ligne ${e.ligne + decalage} : guillemet ouvert et jamais refermé")
        }
        if (enregistrements.isEmpty() || enregistrements[0].champs.isEmpty()) return listOf("$nom : en-tête absent")
        val entete = enregistrements[0]
        val colNiveau = entete.champs.indexOf("niveau_verification")
        val colZone = if (nom == "couts-plantation.csv") entete.champs.indexOf("zone") else -1
        val defauts = mutableListOf<String>()
        val dernier = enregistrements.lastIndex
        enregistrements.drop(1).forEachIndexed { k, e ->
            val estDernier = k + 1 == dernier
            if (e.champs.isEmpty()) {
                if (!estDernier) defauts += "$nom ligne ${e.ligne} : ligne vide au milieu du fichier"
                return@forEachIndexed
            }
            if (e.champs.size != entete.champs.size) {
                defauts += "$nom ligne ${e.ligne} : ${e.champs.size} champs au lieu de ${entete.champs.size} (en-tête ligne ${entete.ligne})"
                return@forEachIndexed
            }
            if (colNiveau >= 0 && e.champs[colNiveau] !in niveauxVerification) {
                defauts += "$nom ligne ${e.ligne} : niveau_verification « ${e.champs[colNiveau]} » hors de $niveauxVerification"
            }
            if (colZone >= 0) {
                val zone = e.champs[colZone]
                if (zone.isNotEmpty() && zone != "toutes" && !zoneBareme.matches(zone)) {
                    defauts += "$nom ligne ${e.ligne} : zone « $zone » ni vide, ni « toutes », ni une lettre A à E du barème"
                }
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

    @Test
    fun `niveau_verification hors des valeurs de la 030 refuse`() {
        val t = "a,niveau_verification\n1,verifie\n2,extrait\n3,non_trouve\n4,renvoi\n5,probable\n"
        val defauts = FormeCsv.verifier("t.csv", t)
        assertEquals(1, defauts.size)
        assertTrue(defauts[0].startsWith("t.csv ligne 6 : niveau_verification « probable »"), defauts[0])
    }

    @Test
    fun `zone de couts-plantation, lettres du bareme acceptees, quantites refusees`() {
        val tete = "poste,zone,niveau_verification\n"
        val bons = listOf("", "toutes", "A (SER F21 Landes de Gascogne)", "A, B, C, D", "C et D", "C, D, E", "E")
        for (z in bons) {
            val champ = if (',' in z) "\"$z\"" else z
            assertEquals(emptyList<String>(), FormeCsv.verifier("couts-plantation.csv", tete + "p,$champ,verifie\n"), z)
        }
        // lignes 45 et 46 d'avant 22ec874 : « 1,20 m » et « 0,50 m » avaient décalé la colonne zone
        for (z in listOf("20 m", "50 m")) {
            val d = FormeCsv.verifier("couts-plantation.csv", tete + "p,$z,verifie\n")
            assertEquals(1, d.size, z)
            assertTrue(d[0].startsWith("couts-plantation.csv ligne 2 : zone « $z »"), d[0])
        }
        // la règle de zone ne vaut que pour couts-plantation.csv
        assertEquals(emptyList<String>(), FormeCsv.verifier("autre.csv", tete + "p,20 m,verifie\n"))
    }

    private val csv: List<File> by lazy {
        val dossier = File(Depot.racine, "docs/referentiels")
        checkNotNull(dossier.takeIf { it.isDirectory }) { "dossier introuvable : $dossier" }
        dossier.listFiles { f -> f.extension == "csv" }!!.sortedBy { it.name }
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
