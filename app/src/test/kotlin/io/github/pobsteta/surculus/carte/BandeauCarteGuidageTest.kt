package io.github.pobsteta.surculus.carte

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Mission 055 : la carte et le guidage prennent le `BandeauCompact` avec la flèche de retour ; la carte a le menu
 * « Couches » (icône `Layers`, deux sections) et les trois boutons de Marculus (zoom avant, zoom arrière, recentrer).
 * Le guidage n'a ni menu de couches ni zoom (il n'a pas de carte). Le test lit les sources.
 */
class BandeauCarteGuidageTest {
    private val racine = File("src/main/kotlin/io/github/pobsteta/surculus").takeIf { it.isDirectory }
        ?: File("app/src/main/kotlin/io/github/pobsteta/surculus")

    private fun source(nom: String) = File(racine, nom).readText()

    private val carte get() = source("carte/EcranCarte.kt")
    private val guidage get() = source("carte/EcranGuidage.kt")

    @Test
    fun `la carte a le bandeau compact, la fleche de retour et l'icone Couches`() {
        assertTrue("BandeauCompact(" in carte)
        assertTrue("Icons.AutoMirrored.Filled.ArrowBack" in carte)
        assertTrue("Icons.Filled.Layers" in carte)
        assertFalse("R.string.tournees_retour" in carte, "le bouton « Retour » de la carte a disparu")
    }

    @Test
    fun `le menu a deux sections, Fond de carte puis Couches affichees`() {
        val menu = carte
        val fond = menu.indexOf("TitreSection(stringResource(R.string.carte_fond_de_carte))")
        val couches = menu.indexOf("TitreSection(stringResource(R.string.carte_couches))")
        assertTrue(fond in 0 until couches, "fond=$fond couches=$couches")
        assertTrue("FondsCarte.proposes(orthoChantier)" in menu)
        // Cadastre d'abord, puis forêt, UG, parcelles, placettes (ce que la carte dessine déjà).
        val ordre = listOf("carte_couche_cadastre", "carte_couche_foret", "carte_couche_ug", "carte_couche_parcelles", "carte_couche_placettes")
            .map { menu.indexOf("LigneCouche(stringResource(R.string.$it") }
        assertTrue(ordre.all { it > couches }, "$ordre")
        assertEquals(ordre.sorted(), ordre)
    }

    @Test
    fun `les cases de couches du bas de l'ecran sont retirees`() {
        assertFalse("CaseCouche" in carte)
    }

    @Test
    fun `trois boutons flottants comme Marculus, d'au moins 48 dp`() {
        assertEquals(3, Regex("SmallFloatingActionButton\\(").findAll(carte).count())
        for (icone in listOf("Icons.Filled.Add", "Icons.Filled.Remove", "Icons.Filled.MyLocation")) assertTrue(icone in carte, icone)
        for (libelle in listOf("carte_zoom_avant", "carte_zoom_arriere", "carte_recentrer")) assertTrue("R.string.$libelle" in carte, libelle)
        assertEquals(3, Regex("sizeIn\\(minWidth = 48.dp, minHeight = 48.dp\\)").findAll(carte).count())
    }

    @Test
    fun `le guidage a le bandeau et la fleche, sans Retour, ni couches, ni zoom`() {
        assertTrue("BandeauCompact(" in guidage)
        assertTrue("Icons.AutoMirrored.Filled.ArrowBack" in guidage)
        assertTrue("R.string.guidage_vers" in guidage)
        assertFalse("R.string.tournees_retour" in guidage, "plus de bouton « Retour » dans le corps")
        for (interdit in listOf("Icons.Filled.Layers", "SmallFloatingActionButton", "carte_couches", "carte_zoom")) {
            assertFalse(interdit in guidage, interdit)
        }
    }

    @Test
    fun `le guidage garde la distance en 72 sp et aucun autre fontSize n'apparait ailleurs`() {
        assertEquals(1, Regex("fontSize").findAll(guidage).count())
        assertFalse("fontSize" in carte)
    }

    @Test
    fun `MainActivity ne pose plus de marges autour de la carte et du guidage`() {
        assertFalse("safeDrawingPadding" in source("MainActivity.kt"))
    }
}
