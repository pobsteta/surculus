package io.github.pobsteta.surculus.core.dictee

import fr.nemeton.terrain.voix.NombresFrancais
import io.github.pobsteta.surculus.core.fixtures.Depot
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tests unitaires de la mission 013 (en plus du rejeu des cas de `fixtures/dictee/`). */
class DicteeUnitairesTest {

    @Test
    fun `chaque nombre de 1 a 400 fait l'aller-retour en lettres`() {
        for (n in 1..400) assertEquals(n, LectureNombres.analyser(NombresFrancais.jetons(n).joinToString(" ")), "n=$n")
    }

    @Test
    fun `les formes a traits d'union donnent la meme valeur que les formes a espaces`() {
        for (n in 1..400) assertEquals(n, LectureNombres.analyser(NombresFrancais.jetons(n).joinToString("-")), "n=$n")
    }

    @Test
    fun `hors de 1 a 400 ou mal forme, pas de nombre`() {
        for (t in listOf("", "zéro", "quatre cent un", "cinq cents", "mille", "deux cents vingt", "vingt dix", "-", "vingt-", "quatre vingts un", "un cent")) {
            assertNull(LectureNombres.analyser(t), t)
        }
    }

    @Test
    fun `la grammaire donnee au moteur est analysee en une commande chacune`() {
        for (p in Grammaire.phrases) assertTrue(AnalyseurDictee.analyser(p) is ResultatDictee.Accepte, p)
        assertEquals(16 + 400, Grammaire.phrases.size)
        assertEquals(Grammaire.phrases.size, Grammaire.phrases.toSet().size)
    }

    @Test
    fun `le JSON de Vosk est une liste de phrases entre guillemets terminee par unk`() {
        val j = Grammaire.jsonVosk()
        assertTrue(j.startsWith("[\"sain\", \"atteint\"") && j.endsWith(", \"[unk]\"]"), j.take(60))
        assertTrue("\"hauteur quarante cinq\"" in j && "\"fin du vide\"" in j)
    }

    @Test
    fun `la grammaire ne contient ni zero ni mille ni les anciens mots`() {
        assertTrue("hauteur" in Grammaire.mots && "quatre" in Grammaire.mots)
        assertFalse("zéro" in Grammaire.mots || "mille" in Grammaire.mots || "abiotique" in Grammaire.mots)
    }

    @Test
    fun `un etat inconnu n'est pas un etat`() {
        assertNull(EtatDicte.deCode("vivant"))
        assertEquals(EtatDicte.NON_OBSERVE, EtatDicte.deCode("non_observe"))
    }

    @Test
    fun `fichier du modele illisible refuse sans exception`() {
        val casse = object : InputStream() {
            override fun read(): Int = throw IOException("lecture impossible")
        }
        assertFalse(VerificationModele.verifier(VerificationModele.NOM_MODELE, casse, "a".repeat(64)))
    }

    @Test
    fun `gros fichier lu par blocs et refuse si l'empreinte differe`() {
        val octets = ByteArray(5_000_000) { (it % 251).toByte() }
        val attendu = MessageDigest.getInstance("SHA-256").digest(octets).joinToString("") { "%02x".format(it) }
        assertTrue(VerificationModele.verifier(VerificationModele.NOM_MODELE, octets.inputStream(), attendu))
        assertTrue(VerificationModele.verifier(VerificationModele.NOM_MODELE, octets.inputStream(), attendu.uppercase()))
        octets[4_999_999] = 0
        assertFalse(VerificationModele.verifier(VerificationModele.NOM_MODELE, octets.inputStream(), attendu))
    }

    @Test
    fun `la somme du modele dans le code est celle des fixtures et de docs-dependances`() {
        val sha = DicteeFixtures.famille("vocabulaire").json.getAsJsonObject("modele").get("sha256_archive_mesure").asString
        assertEquals(sha, VerificationModele.SHA256_ARCHIVE)
        assertTrue(sha in File(Depot.racine, "docs/dependances.md").readText())
    }

    @Test
    fun `hauteur sur un plant sans etat est refusee`() {
        // Décision de Pascal, 2026-10-24 : refus, et la voix demande d'abord l'état.
        assertTrue(AnalyseurDictee.appliquer("hauteur dix", null) is ResultatDictee.Refus)
    }

    @Test
    fun `hauteur sur un plant mort et commandes de navigation`() {
        // Hauteur sur mort : non refusée par la spec § 5 (refusée sur absent et non observé seulement).
        assertTrue(AnalyseurDictee.appliquer("hauteur dix", EtatDicte.MORT) is ResultatDictee.Accepte)
        // Les commandes de navigation ne dépendent pas de l'état.
        for (e in listOf(null) + EtatDicte.entries) {
            for (p in listOf("suivant", "annule", "vide", "fin du vide")) {
                assertTrue(AnalyseurDictee.appliquer(p, e) is ResultatDictee.Accepte, "$p / $e")
            }
        }
    }
}
