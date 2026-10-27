package io.github.pobsteta.surculus.core.carte

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Les fonds de carte de Surculus sont ceux de Marculus (mission 055) : Plan IGN, Ortho IGN, OpenStreetMap, Ortho chantier.
 * Référence : `marculus` `ui/carte/CarteScreen.kt` l. 141, 185-209, 235-238, 416-430, 655.
 */
class FondsCarteTest {
    @Test
    fun `les quatre fonds ont les libelles de Marculus, dans l'ordre du menu`() {
        assertEquals(
            listOf("Plan IGN", "Ortho IGN", "OpenStreetMap", "Ortho chantier"),
            Fond.entries.map { it.libelle },
        )
    }

    @Test
    fun `sans fichier d'ortho, le menu propose trois fonds, tous en ligne`() {
        val fonds = FondsCarte.proposes(orthoChantier = false)
        assertEquals(listOf(Fond.PLAN_IGN, Fond.IGN, Fond.OSM), fonds)
        assertFalse(Fond.ORTHO in fonds)
    }

    @Test
    fun `avec un fichier d'ortho, l'ortho chantier est proposee en dernier`() {
        assertEquals(listOf(Fond.PLAN_IGN, Fond.IGN, Fond.OSM, Fond.ORTHO), FondsCarte.proposes(orthoChantier = true))
    }

    @Test
    fun `le fond par defaut est le Plan IGN sans fichier d'ortho`() {
        assertEquals(Fond.PLAN_IGN, FondsCarte.PAR_DEFAUT)
        assertEquals(Fond.PLAN_IGN, FondsCarte.parDefaut(orthoChantier = false))
        assertEquals(Fond.PLAN_IGN, FondsCarte.affiche(null, orthoChantier = false))
    }

    @Test
    fun `mission 057 - avec un fichier d'ortho, la carte s'ouvre sur l'Ortho chantier (ecart avec Marculus)`() {
        assertEquals(Fond.ORTHO, FondsCarte.parDefaut(orthoChantier = true))
        assertEquals(Fond.ORTHO, FondsCarte.affiche(null, orthoChantier = true))
    }

    @Test
    fun `le choix memorise est retrouve, l'ortho chantier comprise (memorisee par tournee)`() {
        assertEquals(Fond.OSM, FondsCarte.affiche(FondsCarte.depuisNom("OSM"), orthoChantier = false))
        assertEquals(Fond.IGN, FondsCarte.affiche(FondsCarte.depuisNom("IGN"), orthoChantier = true))
        assertEquals(Fond.PLAN_IGN, FondsCarte.affiche(FondsCarte.depuisNom("PLAN_IGN"), orthoChantier = true))
        assertEquals(Fond.ORTHO, FondsCarte.affiche(FondsCarte.depuisNom("ORTHO"), orthoChantier = true))
    }

    @Test
    fun `mission 057 - le fond est memorise tournee par tournee`() {
        val prefs = mutableMapOf<String, String>()
        fun ouvrir(tournee: String, ortho: Boolean) = FondsCarte.affiche(FondsCarte.depuisNom(prefs[FondsCarte.cle(tournee)]), ortho)
        fun choisir(tournee: String, f: Fond) { prefs[FondsCarte.cle(tournee)] = f.name }

        assertNotEquals(FondsCarte.cle("A"), FondsCarte.cle("B"))
        // Un fond en ligne choisi dans A reste sans effet sur B.
        choisir("A", Fond.OSM)
        assertEquals(Fond.OSM, ouvrir("A", ortho = false))
        // B a une ortho et aucun choix : Ortho chantier.
        assertEquals(Fond.ORTHO, ouvrir("B", ortho = true))
        // Un choix fait dans B est gardé pour B, sans toucher A.
        choisir("B", Fond.IGN)
        assertEquals(Fond.IGN, ouvrir("B", ortho = true))
        assertEquals(Fond.OSM, ouvrir("A", ortho = false))
        // L'ancienne clé globale n'est plus lue.
        assertNotEquals("fond", FondsCarte.cle("A"))
    }

    @Test
    fun `mission 057 - les quatre cas d'ouverture, le choix a la main l'emporte`() {
        // 1. choix mémorisé : il l'emporte, même si la tournée a une ortho.
        assertEquals(Fond.OSM, FondsCarte.affiche(FondsCarte.depuisNom("OSM"), orthoChantier = true))
        // 2. rien de mémorisé, ortho présente : Ortho chantier.
        assertEquals(Fond.ORTHO, FondsCarte.affiche(null, orthoChantier = true))
        // 3. rien de mémorisé, pas d'ortho : Plan IGN.
        assertEquals(Fond.PLAN_IGN, FondsCarte.affiche(null, orthoChantier = false))
        // 4. Ortho chantier mémorisée, tournée sans ortho : Plan IGN.
        assertEquals(Fond.PLAN_IGN, FondsCarte.affiche(FondsCarte.depuisNom("ORTHO"), orthoChantier = false))
    }

    @Test
    fun `mission 057 - choisir l'ortho chantier a la main est memorise pour la tournee`() {
        val prefs = mutableMapOf<String, String>()
        prefs[FondsCarte.cle("A")] = Fond.OSM.name
        prefs[FondsCarte.cle("A")] = Fond.ORTHO.name // le dernier choix remplace l'ancien
        assertEquals(Fond.ORTHO, FondsCarte.affiche(FondsCarte.depuisNom(prefs[FondsCarte.cle("A")]), orthoChantier = true))
    }

    @Test
    fun `un nom memorise inconnu donne le fond par defaut`() {
        assertEquals(Fond.PLAN_IGN, FondsCarte.affiche(FondsCarte.depuisNom("n'importe quoi"), orthoChantier = false))
    }

    @Test
    fun `attributions IGN et OSM, aucune pour l'ortho chantier`() {
        assertEquals("© IGN Géoplateforme, Licence Ouverte Etalab 2.0", Fond.PLAN_IGN.attribution)
        assertEquals(Fond.PLAN_IGN.attribution, Fond.IGN.attribution)
        assertEquals("© contributeurs OpenStreetMap (ODbL)", Fond.OSM.attribution)
        assertNull(Fond.ORTHO.attribution)
    }

    @Test
    fun `l'URL du cadastre est celle de Marculus (Parcellaire Express, PNG transparent)`() {
        val u = Geoplateforme.CADASTRE.url(z = 17, x = 67540, y = 45160)
        assertTrue("LAYER=CADASTRALPARCELS.PARCELLAIRE_EXPRESS" in u, u)
        assertTrue("STYLE=PCI%20vecteur" in u && "FORMAT=image/png" in u, u)
        assertTrue(u.endsWith("&TILEMATRIX=17&TILEROW=45160&TILECOL=67540"), u)
    }

    @Test
    fun `le cadastre est masque par defaut`() {
        assertFalse(FondsCarte.CADASTRE_PAR_DEFAUT)
    }

    @Test
    fun `la mention du cadastre s'ajoute seulement hors des fonds IGN`() {
        assertEquals(listOf(Attributions.OSM, "cadastre © IGN"), FondsCarte.mentions(Fond.OSM, cadastre = true))
        assertEquals(listOf(Attributions.IGN), FondsCarte.mentions(Fond.PLAN_IGN, cadastre = true))
        assertEquals(listOf(Attributions.IGN), FondsCarte.mentions(Fond.IGN, cadastre = true))
        assertEquals(listOf("cadastre © IGN"), FondsCarte.mentions(Fond.ORTHO, cadastre = true))
        assertEquals(emptyList<String>(), FondsCarte.mentions(Fond.ORTHO, cadastre = false))
    }

    @Test
    fun `les URL de la Geoplateforme sont celles de Marculus (WMTS PM_0_19, TILEROW est Y)`() {
        assertEquals(
            "https://data.geopf.fr/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0" +
                "&LAYER=GEOGRAPHICALGRIDSYSTEMS.PLANIGNV2&STYLE=normal&FORMAT=image/png" +
                "&TILEMATRIXSET=PM_0_19&TILEMATRIX=16&TILEROW=22580&TILECOL=33770",
            Geoplateforme.PLAN_IGN.url(z = 16, x = 33770, y = 22580),
        )
        val ortho = Geoplateforme.ORTHO_IGN.url(z = 18, x = 135082, y = 90321)
        assertTrue("LAYER=ORTHOIMAGERY.ORTHOPHOTOS" in ortho && "FORMAT=image/jpeg" in ortho, ortho)
        assertTrue(ortho.endsWith("&TILEMATRIX=18&TILEROW=90321&TILECOL=135082"), ortho)
        assertEquals(".png", Geoplateforme.PLAN_IGN.extension)
        assertEquals(".jpg", Geoplateforme.ORTHO_IGN.extension)
    }
}
