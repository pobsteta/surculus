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
    fun `le recentrage passe par la decision testee, jamais par le fix brut`() {
        assertEquals(1, Regex("Recentrage\\.cible\\(").findAll(carte).count(), "le bouton recentrer seul (« Ma position » retiré, mission 057)")
        assertFalse("animateTo(GeoPoint(f.position" in carte)
        assertFalse("animateTo(GeoPoint(fix.position" in carte)
    }

    @Test
    fun `les etats ouverture et refus gardent les marges du corps`() {
        val debut = carte.indexOf("fun EcranCarte(")
        val fin = carte.indexOf("is Chargement.Pret", debut)
        val etats = carte.substring(debut, fin)
        assertEquals(2, Regex("windowInsetsPadding\\(WindowInsets\\.safeDrawing\\.only\\(WindowInsetsSides\\.Horizontal \\+ WindowInsetsSides\\.Bottom\\)\\)").findAll(etats).count())
    }

    @Test
    fun `le fournisseur du cadastre est libere a la sortie, couche cochee ou non`() {
        assertTrue("cadastre?.onDetach(" in carte)
    }

    @Test
    fun `la chaine tournees_retour n'existe plus`() {
        val chaines = File(racine.parentFile.parentFile.parentFile.parentFile.parentFile, "res/values/strings_tournees.xml").readText()
        assertFalse("tournees_retour" in chaines)
    }

    @Test
    fun `les cases de couches du bas de l'ecran sont retirees`() {
        assertFalse("CaseCouche" in carte)
    }

    @Test
    fun `cinq boutons flottants comme Groma plus Controler, de 48 à 32 dp (missions 061, 012-B1 et 078)`() {
        assertEquals(6, Regex("SmallFloatingActionButton\\(").findAll(carte).count())
        for (icone in listOf("Icons.Filled.Check", "Icons.Filled.Navigation", "Icons.Filled.Add", "Icons.Filled.Remove", "Icons.Filled.Home", "Icons.Filled.MyLocation")) assertTrue(icone in carte, icone)
        for (libelle in listOf("controle_controler", "carte_naviguer", "carte_zoom_avant", "carte_zoom_arriere", "carte_cadrer_tournee", "carte_recentrer")) assertTrue("R.string.$libelle" in carte, libelle)
        assertEquals(6, Regex("Modifier.size\\(tailleBouton.dp\\)").findAll(carte).count())
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
