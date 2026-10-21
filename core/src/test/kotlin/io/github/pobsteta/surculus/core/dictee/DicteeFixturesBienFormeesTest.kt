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

    private val familles = listOf("phrases", "hauteur", "refus", "contexte", "normalisation", "vocabulaire", "modele")
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
        val commandes = listOf("phrases", "hauteur", "contexte", "normalisation").flatMap { DicteeFixtures.cas(it) }
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
            "sain", "atteint", "mort", "absent", "non observé", "insectes", "champignon", "animaux", "climat",
            "indéterminé", "mangé", "hauteur quarante-cinq", "suivant", "remplace", "vide", "fin du vide", "annule",
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
    fun `chaque mot de la grammaire est dans le vocabulaire du modele, aucune absence consignee`() {
        val v = DicteeFixtures.vocabulaire.toHashSet()
        val mots = (DicteeFixtures.cas("phrases") + DicteeFixtures.cas("hauteur").filter { it.attendu.get("accepte").asBoolean })
            .flatMap { it.entree.get("phrase").asString.split(' ') }.toSet()
        assertTrue(mots.isNotEmpty())
        assertEquals(emptySet<String>(), mots.filter { it !in v }.toSet(), "mots de la grammaire absents du modèle")
        val declares = DicteeFixtures.famille("vocabulaire").json.getAsJsonArray("mots_absents_du_modele")
        assertEquals(0, declares.size(), "plus aucune absence à consigner (décision n° 5)")
    }

    @Test
    fun `les trois mots remplaces sont dans le modele et les anciens n'y sont pas`() {
        val v = DicteeFixtures.vocabulaire.toHashSet()
        listOf("climat", "mangé", "remplace").forEach { assertTrue(it in v, it) }
        listOf("abiotique", "abrouti", "regarni").forEach { assertTrue(it !in v, it) }
        val phrases = DicteeFixtures.cas("phrases").map { it.entree.get("phrase").asString }
        listOf("abiotique", "abrouti", "regarni").forEach { assertTrue(it !in phrases, "$it ne doit plus être une phrase") }
        val refus = DicteeFixtures.cas("refus").map { it.entree.get("phrase").asString }
        listOf("abiotique", "abrouti", "regarni").forEach { assertTrue(it in refus, "$it doit être refusé") }
    }

    @Test
    fun `les nombres existent sous les deux formes, traits d'union et espaces`() {
        val acceptes = DicteeFixtures.cas("hauteur").filter { it.attendu.get("accepte").asBoolean }
            .associate { it.entree.get("phrase").asString to it.attendu.getAsJsonObject("commande").get("hauteur_cm").asInt }
        assertEquals(45, acceptes["hauteur quarante-cinq"])
        assertEquals(45, acceptes["hauteur quarante cinq"])
        assertEquals(99, acceptes["hauteur quatre-vingt-dix-neuf"])
        assertEquals(99, acceptes["hauteur quatre vingt dix neuf"])
        val v = DicteeFixtures.vocabulaire.toHashSet()
        acceptes.keys.flatMap { it.split(' ') }.forEach { assertTrue(it in v, "« $it » absent du modèle") }
    }

    @Test
    fun `l'abroutissement est accepte sur sain, atteint et mort, refuse sur absent, non observe et sans etat`() {
        val m = DicteeFixtures.cas("contexte").filter { it.entree.get("phrase").asString == "mangé" }
            .associate { (it.entree.get("etat_courant").let { e -> if (e.isJsonNull) null else e.asString }) to it.attendu.get("accepte").asBoolean }
        assertEquals(mapOf("sain" to true, "atteint" to true, "mort" to true, "absent" to false, "non_observe" to false, null to false), m)
        assertTrue(DicteeFixtures.cas("contexte").none { it.entree.get("phrase").asString == "abrouti" })
    }

    @Test
    fun `les espaces parasites sont normalises, sauf phrase d'espaces seuls`() {
        val n = DicteeFixtures.cas("normalisation")
        assertTrue(n.any { it.entree.get("phrase").asString.startsWith(" ") && it.attendu.get("accepte").asBoolean })
        assertTrue(n.any { it.entree.get("phrase").asString.endsWith(" ") && it.attendu.get("accepte").asBoolean })
        assertTrue(n.any { it.entree.get("phrase").asString.contains("  ") && it.attendu.get("accepte").asBoolean })
        val refus = DicteeFixtures.cas("refus").first { it.entree.get("phrase").asString == "   " }
        assertTrue(!refus.attendu.get("accepte").asBoolean)
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

    private fun etat(c: io.github.pobsteta.surculus.core.fixtures.CasFixture): String? =
        c.entree.get("etat_courant").let { e -> if (e.isJsonNull) null else e.asString }

    @Test
    fun `remplace n'est accepte que sur un plant mort ou absent (decision n 9)`() {
        val m = DicteeFixtures.cas("contexte").filter { it.entree.get("phrase").asString == "remplace" }
            .associate { etat(it) to it.attendu.get("accepte").asBoolean }
        assertEquals(
            mapOf("mort" to true, "absent" to true, "sain" to false, "atteint" to false, "non_observe" to false, null to false),
            m,
        )
    }

    @Test
    fun `un nouvel etat sur un plant qui en a deja un est une correction annoncee par la voix (decision n 10)`() {
        val etats = setOf("sain", "atteint", "mort", "absent")
        val cas = DicteeFixtures.cas("contexte").filter { it.attendu.has("correction") }
        assertTrue(cas.isNotEmpty())
        cas.forEach { c ->
            val attendu = c.attendu
            val avait = etat(c) != null
            assertEquals(avait, attendu.get("correction").asBoolean, "${c.id} : correction seulement si un état existe déjà")
            assertEquals(avait, attendu.get("annonce_vocale").asBoolean, "${c.id} : annonce vocale de la correction")
            assertEquals("etat", attendu.getAsJsonObject("commande").get("type").asString)
        }
        assertTrue(cas.any { etat(it) == "atteint" && it.entree.get("phrase").asString == "sain" })
        assertTrue(cas.any { etat(it) == null && !it.attendu.get("correction").asBoolean })
        assertTrue(cas.filter { etat(it) != null }.all { etat(it) in etats })
    }

    @Test
    fun `le champ hypothese est un texte, jamais un booleen`() {
        tous.forEach { (_, c) ->
            c.json.get("hypothese")?.let {
                assertTrue(it.isJsonPrimitive && it.asJsonPrimitive.isString && it.asString.isNotBlank(), "${c.id} : hypothese doit être un texte")
            }
        }
    }

    @Test
    fun `plus aucune source ne laisse une proposition du testeur a confirmer, hors champ hypothese`() {
        tous.forEach { (_, c) ->
            assertTrue(!c.source.contains("proposée par le testeur") && !c.source.contains("à confirmer"), "${c.id} : source périmée")
            assertTrue(!c.source.contains("seul « abrouti »"), "${c.id} : source périmée (abrouti)")
        }
    }

    @Test
    fun `les cas de normalisation et de refus de forme citent l'inscription du journal`() {
        val ids = listOf("DI-61", "DI-62", "DI-68", "DI-69", "DI-70", "DI-71")
        val refus = DicteeFixtures.cas("refus").associateBy { it.id }
        ids.forEach {
            assertTrue(refus.getValue(it).source.contains("pilotage/journal.md, 2026-10-21") && refus.getValue(it).source.contains("Choix technique"), "$it : source")
        }
    }

    @Test
    fun `les cas d'abroutissement citent la spec 6 alignee et la decision 4`() {
        DicteeFixtures.cas("contexte").filter { it.entree.get("phrase").asString == "mangé" }.forEach {
            assertTrue(it.source.contains("§ 6") && it.source.contains("décision n° 4"), "${it.id} : source")
        }
    }

    @Test
    fun `les symboles de la liste qui ne sont pas des mots sont declares, et aucun mot de la grammaire n'en est un`() {
        val j = DicteeFixtures.famille("vocabulaire").json.getAsJsonObject("liste_de_mots")
        val declares = j.getAsJsonArray("symboles_non_mots").map { it.asString }.toSet()
        assertEquals(setOf("<eps>", "#0", "<s>", "</s>", "!SIL", "[unk]"), declares)
        val v = DicteeFixtures.vocabulaire.toHashSet()
        declares.forEach { assertTrue(it in v, it) }
        assertEquals(135774 - 6, j.get("nombre_de_mots_reels").asInt)
    }

    @Test
    fun `le script d'extraction de la liste de mots est versionne et documente`() {
        val script = java.io.File(DicteeFixtures.dossier, "outils/extraire_vocabulaire.py")
        assertTrue(script.isFile, "fixtures/dictee/outils/extraire_vocabulaire.py")
        val readme = java.io.File(DicteeFixtures.dossier.parentFile, "README.md").readText()
        assertTrue(readme.contains("extraire_vocabulaire.py"), "commande d'extraction dans fixtures/README.md")
        assertTrue(readme.contains("<eps>") && readme.contains("[unk]"), "symboles non mots dans fixtures/README.md")
        assertTrue(!readme.contains("proposée par le `testeur`, à confirmer par Pascal"), "README périmé")
    }
}
