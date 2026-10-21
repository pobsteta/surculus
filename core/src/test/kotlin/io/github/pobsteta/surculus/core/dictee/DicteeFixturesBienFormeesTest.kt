package io.github.pobsteta.surculus.core.dictee

import io.github.pobsteta.surculus.core.fixtures.Referentiels
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Actif dès maintenant : les cas de `fixtures/dictee/` sont bien formés, sourcés, fidèles aux
 * référentiels et au vocabulaire du modèle. Aucun code de production n'est lu ni appelé.
 */
class DicteeFixturesBienFormeesTest {

    private val familles = listOf("phrases", "hauteur", "refus", "contexte", "vocabulaire", "modele")
    private val tous = familles.flatMap { f -> DicteeFixtures.cas(f).map { f to it } }

    @Test
    fun `les fichiers de cas attendus existent`() {
        assertEquals(familles.sorted(), DicteeFixtures.fichiers.map { it.famille }.sorted())
    }

    @Test
    fun `les identifiants sont DI-NN, uniques, et chaque cas cite sa source`() {
        tous.forEach { (_, c) ->
            assertTrue(Regex("""DI-\d{2,3}""").matches(c.id), "identifiant ${c.id}")
            assertTrue(c.source.isNotBlank(), "${c.id} : source absente")
            assertTrue(c.description.isNotBlank(), "${c.id} : description absente")
        }
        val doublons = tous.groupBy { it.second.id }.filter { it.value.size > 1 }.keys
        assertTrue(doublons.isEmpty(), "ids en double : $doublons")
    }

    @Test
    fun `tout code produit par une commande appartient aux referentiels`() {
        val etats = Referentiels.etats + "non_observe" // non_observe : docs/spec.md § 5
        val types = setOf("etat", "cause", "detail", "hauteur", "suivant", "regarni", "vide_debut", "vide_fin", "annule")
        val commandes = listOf("phrases", "hauteur", "contexte").flatMap { DicteeFixtures.cas(it) }
            .filter { it.attendu.get("accepte").asBoolean }.map { it.id to it.attendu.getAsJsonObject("commande") }
        assertTrue(commandes.isNotEmpty())
        for ((id, cmd) in commandes) {
            assertTrue(cmd.get("type").asString in types, "$id : type ${cmd.get("type")}")
            cmd.get("etat")?.let { assertTrue(it.asString in etats, "$id : état « ${it.asString} » hors référentiel") }
            cmd.get("cause_groupe")?.let {
                assertTrue(it.asString in Referentiels.groupesCauses, "$id : cause « ${it.asString} » hors référentiel")
            }
            cmd.get("cause_detail")?.let { assertEquals("abroutissement", it.asString, "$id : seul détail reconnu (spec § 6)") }
        }
        assertTrue(Referentiels.groupesCauses.isNotEmpty() && Referentiels.etats.isNotEmpty())
    }

    @Test
    fun `chaque mot de la grammaire de la spec a son cas, et une cause par mot dicte`() {
        val phrases = DicteeFixtures.cas("phrases").map { it.entree.get("phrase").asString }.toSet()
        val spec = setOf(
            "sain", "atteint", "mort", "absent", "non observé", "insectes", "champignon", "animaux", "abiotique",
            "indéterminé", "abrouti", "hauteur quarante-cinq", "suivant", "regarni", "vide", "fin du vide", "annule",
        )
        assertEquals(spec, phrases) // docs/spec.md § 4.3 : 17 phrases
    }

    @Test
    fun `les bornes de hauteur sont 1 et 400 cm (decision de Pascal du 2026-10-21)`() {
        val acceptes = DicteeFixtures.cas("hauteur").filter { it.attendu.get("accepte").asBoolean }
            .map { it.attendu.getAsJsonObject("commande").get("hauteur_cm").asInt }
        assertEquals(1, acceptes.min())
        assertEquals(400, acceptes.max())
        val refuses = DicteeFixtures.cas("hauteur").filterNot { it.attendu.get("accepte").asBoolean }
            .map { it.entree.get("phrase").asString }
        listOf("hauteur zéro", "hauteur quatre cent un", "hauteur", "hauteur quarante-cinq cinquante").forEach {
            assertTrue(it in refuses, "« $it » doit figurer parmi les refus")
        }
    }

    @Test
    fun `une phrase a deux commandes est refusee entiere (decision n 2)`() {
        val refus = DicteeFixtures.cas("refus").map { it.entree.get("phrase").asString }
        listOf("atteint insectes", "sain suivant").forEach { assertTrue(it in refus, it) }
    }

    @Test
    fun `la liste de mots du modele est complete, triee, sans doublon`() {
        val v = DicteeFixtures.vocabulaire
        assertEquals(DicteeFixtures.famille("vocabulaire").json.getAsJsonObject("liste_de_mots").get("nombre_de_mots").asInt, v.size)
        assertEquals(135774, v.size)
        assertEquals(v.size, v.toSet().size, "doublons")
        assertTrue(v.none { it.isBlank() || it.contains(' ') }, "un mot par ligne")
    }

    @Test
    fun `les cas de vocabulaire disent vrai de la liste de mots`() {
        val v = DicteeFixtures.vocabulaire.toHashSet()
        DicteeFixtures.cas("vocabulaire").forEach { c ->
            val mot = c.entree.get("mot").asString
            assertEquals(c.attendu.get("dans_vocabulaire").asBoolean, mot in v, "${c.id} : « $mot »")
        }
    }

    @Test
    fun `trois mots de la grammaire sont absents du modele et signales, jamais contournes`() {
        val v = DicteeFixtures.vocabulaire.toHashSet()
        val mots = DicteeFixtures.cas("phrases").flatMap { it.entree.get("phrase").asString.split(' ') } +
            DicteeFixtures.cas("hauteur").filter { it.attendu.get("accepte").asBoolean }
                .flatMap { it.entree.get("phrase").asString.split(' ') }
        val absents = mots.filter { it !in v }.toSet()
        assertEquals(setOf("abiotique", "abrouti", "regarni"), absents)
        val declares = DicteeFixtures.famille("vocabulaire").json.getAsJsonArray("mots_absents_du_modele").map { it.asString }.toSet()
        assertEquals(declares, absents)
    }

    @Test
    fun `les cas de modele ont un contenu et une empreinte coherents avec l'exemple FIPS 180-4`() {
        val abc = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        val c = DicteeFixtures.cas("modele").first { it.id == "DI-300" }
        assertEquals("abc", c.entree.get("contenu_utf8").asString)
        assertEquals(abc, c.entree.get("sha256_attendu").asString)
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest("abc".toByteArray())
        assertEquals(abc, digest.joinToString("") { "%02x".format(it) })
    }
}
