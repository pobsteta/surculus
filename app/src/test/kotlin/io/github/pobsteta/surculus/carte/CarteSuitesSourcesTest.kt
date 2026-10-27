package io.github.pobsteta.surculus.carte

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Mission 057 : plus de bouton « Ma position » ; le marqueur passe par `MarqueurPosition`. Lecture des sources. */
class CarteSuitesSourcesTest {
    private val main = File("src/main").takeIf { it.isDirectory } ?: File("app/src/main")
    private val ecran by lazy { File(main, "kotlin/io/github/pobsteta/surculus/carte/EcranCarte.kt").readText() }

    @Test
    fun `plus de bouton Ma position ni de chaine associee`() {
        assertTrue(ecran.isNotEmpty())
        assertFalse(Regex("carte_ma_position|centrerSurMoi|Ma position").containsMatchIn(ecran))
        val chaines = File(main, "res/values/strings_tournees.xml").readText()
        assertFalse("carte_ma_position" in chaines)
    }

    @Test
    fun `le bouton recentrer reste`() {
        assertTrue("MyLocation" in ecran)
    }

    @Test
    fun `le marqueur n'est active que par MarqueurPosition`() {
        assertTrue("MarqueurPosition.aDessiner" in ecran)
        assertFalse(Regex("l\\.position\\.isEnabled\\s*=\\s*fix\\s*!=\\s*null").containsMatchIn(ecran))
    }

    @Test
    fun `le fond est memorise par tournee, plus par la cle globale`() {
        assertTrue("FondsCarte.cle(" in ecran)
        assertFalse("CLE_FOND" in ecran)
    }

    @Test
    fun `une seule fonction decide du fond, et l'ecran l'appelle`() {
        assertTrue("FondsCarte.affiche(" in ecran)
        assertFalse("FondsCarte.parDefaut(" in ecran)
        assertFalse("FondsCarte.retenu(" in ecran)
    }

    @Test
    fun `la carte n'est creee qu'une fois la presence de l'ortho verifiee`() {
        assertTrue("produceState<Boolean?>(null" in ecran)
        assertFalse(Regex("produceState\\(false").containsMatchIn(ecran))
        val debut = ecran.indexOf("fun EcranCarte(")
        val pret = ecran.indexOf("is Chargement.Pret", debut)
        assertTrue(pret > 0 && "R.string.carte_ouverture" in ecran.substring(pret, ecran.indexOf("private fun GeoPoint", pret)), "etat d'ouverture tant que l'ortho n'est pas verifiee")
    }

    @Test
    fun `la carte creee sur l'ortho chantier recoit son fichier des la creation`() {
        val fabrique = ecran.substringAfter("factory = { contexte ->").substringBefore("l.carte = carte")
        assertTrue("FichierOrtho.ouvrir(ouverte.dossier)" in fabrique)
    }
}
