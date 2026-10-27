package io.github.pobsteta.surculus.tournee

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Mission 061 : l'écran « Placettes », le câblage de la navigation et la carte sans boutons du bas. Lecture des sources. */
class EcranPlacettesSourcesTest {
    private val main = File("src/main").takeIf { it.isDirectory } ?: File("app/src/main")
    private fun source(chemin: String) = File(main, "kotlin/io/github/pobsteta/surculus/$chemin").readText()
    private val placettes by lazy { source("tournee/EcranPlacettes.kt") }
    private val carte by lazy { source("carte/EcranCarte.kt") }
    private val chaines by lazy { File(main, "res/values/strings_tournees.xml").readText() }

    @Test
    fun `l'ecran Placettes a le bandeau compact, la fleche de retour, la foret en titre et l'icone carte`() {
        assertTrue("BandeauCompact(" in placettes)
        assertTrue("Icons.AutoMirrored.Filled.ArrowBack" in placettes)
        assertTrue("Icons.Filled.Place" in placettes)
        assertTrue("titreForet(" in placettes)
        assertTrue("ordonnerPlacettes(" in placettes, "à faire d'abord, par la fonction testée")
        assertFalse("fontSize" in placettes)
    }

    @Test
    fun `une ligne de placette fait au moins 64 dp, la pastille est celle de la carte`() {
        assertTrue("heightIn(min = 64.dp)" in placettes)
        assertTrue("PastilleEtat(" in placettes, "la pastille est celle de la carte (Palette.kt, mission 063)")
    }

    @Test
    fun `la navigation de la tournee passe par les placettes, puis la carte, puis le guidage`() {
        val activite = source("MainActivity.kt")
        val guidage = activite.indexOf("EcranGuidage(tourneeId")
        val carteAppel = activite.indexOf("EcranCarte(tourneeId")
        val ecranPlacettes = activite.indexOf("EcranPlacettes(")
        assertTrue(guidage in 0 until carteAppel && carteAppel < ecranPlacettes, "$guidage $carteAppel $ecranPlacettes")
        assertTrue("onCarte = { nav = nav.ouvrirCarte() }" in activite)
        assertTrue("onPlacette = { nav = nav.ouvrirCarte(it) }" in activite)
    }

    @Test
    fun `plus de bouton Placettes ni de liste sous la carte`() {
        assertFalse(Regex("\\bButton\\(|OutlinedButton\\(|ListePlacettes").containsMatchIn(carte))
        for (ancienne in listOf("carte_placettes_liste", "carte_fermer_liste", "carte_guider")) {
            assertFalse(ancienne in carte, ancienne)
            assertFalse(ancienne in chaines, ancienne)
        }
    }

    @Test
    fun `la carte garde son rognage et passe par les fonctions testees pour naviguer et centrer`() {
        assertEquals(1, Regex("Recentrage\\.cible\\(").findAll(carte).count())
        assertTrue("cibleNavigation(" in carte)
        assertTrue("centrageInitial(" in carte)
        assertTrue("clipToBounds()" in carte)
        assertTrue("disabled()" in carte, "naviguer vers est inactif sans placette à faire")
    }

    @Test
    fun `l'accueil donne la foret du fichier, plus le nom du dossier tant qu'une foret existe`() {
        val depot = source("tournee/DepotTournees.kt")
        assertTrue("titreForet(it.projet?.foret, it.nomForet, dossier.name)" in depot)
        assertFalse("projet?.foret ?: tournee?.projet?.nom" in depot)
    }

    @Test
    fun `la ligne dit Controlee par statutLigne, jamais pour toute ligne non a faire`() {
        assertTrue("statutLigne(" in placettes)
        assertFalse("if (aFaire) R.string.placettes_a_faire else R.string.placettes_controlee" in placettes)
    }

    @Test
    fun `les boutons de zoom integres d'osmdroid sont masques, le pincement reste (demande de Pascal)`() {
        assertTrue("zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)" in carte)
        assertTrue("carte.setMultiTouchControls(true)" in carte)
    }

    @Test
    fun `maison cadre l'emprise ou les placettes et se grise sans rien a cadrer, la choisie survit a la rotation`() {
        assertTrue("empriseDeCadrage(" in carte)
        assertTrue("if (cadre == null) disabled()" in carte)
        assertTrue("rememberSaveable(" in carte && "choisieUuid" in carte)
        assertTrue("probleme(p)" in carte, "placette sans position ou hors de France : message, pas de navigation")
        assertTrue("carte_placette_sans_position" in chaines && "carte_placette_hors_france" in chaines)
    }

    /** Mission 062 : texte neutre, sans plaque blanche, dans la couleur du thème. */
    private fun bloc(source: String, depuis: String, jusqua: String): String {
        val i = source.indexOf(depuis)
        assertTrue(i >= 0, depuis)
        return source.substring(i, source.indexOf(jusqua, i))
    }

    @Test
    fun `la ligne du compte des placettes n'a pas de plaque et prend la couleur du theme`() {
        val b = bloc(placettes, "R.plurals.placettes_compte", "if (ordre.nombre == 0)")
        assertFalse("plaqueEtat" in b, b)
        assertTrue("MaterialTheme.colorScheme.onSurface" in b, b)
    }

    @Test
    fun `la ligne de la placette choisie n'a pas de plaque et prend la couleur du theme`() {
        val b = bloc(carte, "R.string.carte_choisie_distance", "// Attributions du fond")
        assertFalse("plaqueEtat" in b, b)
        assertTrue("MaterialTheme.colorScheme.onSurface" in b, b)
        assertFalse("fontSize" in b)
    }
}
