package io.github.pobsteta.surculus.controle

import io.github.pobsteta.surculus.core.placette.EtatDsf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Mission 100, constat au niveau de la règle (sans écran) : deux ouvertures confirmées de suite de la même placette, sur le même
 * journal, c'est ce que produit un double appui à la confirmation, au guidage comme à l'écran de contrôle (spec § 4 et § 6 : un
 * contrôle par placette et par jour civil Europe/Paris dans une tournée). Écrit par le `testeur` sans lire `SessionControle.kt`.
 *
 * Mission 101 (recadrage) : la règle reprend déjà le contrôle du jour ; la « ligne en trop » (3 lignes au lieu de 2) est un état dit
 * sur le contrôle repris, une correction d'état légitime (spec § 4.3), pas une seconde ouverture. Le constat reste écrit tel que la 100
 * l'a vu, mais ne dit pas un défaut ; la règle « une ligne `controle` par placette et par jour » est jugée par
 * `SecondeOuvertureSansPositionTest`.
 *
 * Ces cas **disent** ce que la règle fait quand l'écran ne protège pas ; ils ne jugent pas la règle. Le verrou, lui, est jugé par
 * `RenduDoubleAppuiSansPositionTest` (guidage). Coordonnées : P35-01 à Couchey (Côte-d'Or), Lambert-93 (EPSG:2154), en France.
 */
class DoubleOuvertureSansPositionTest {
    @Test
    fun `constat - deux ouvertures confirmees de suite puis un etat dit sur chacune - ce qu'ecrit le journal`() = with(BancControle()) {
        val premier = assertIs<EtatArrivee.Ouverte>(ArriveeControle.ouvrirSansPosition(contexte, journal, horloge, essences, "P35-01", sauvegarde, confirmee = true))
        val second = ArriveeControle.ouvrirSansPosition(contexte, journal, horloge, essences, "P35-01", sauvegarde, confirmee = true)
        val apresOuvertures = journal.nombreDeLignes
        val r1 = runCatching { premier.controleur.dire(EtatDsf.SAIN) }
        val r2 = runCatching { (second as? EtatArrivee.Ouverte)?.controleur?.dire(EtatDsf.SAIN) }
        println(
            "CONSTAT 100 règle : second appel = ${second::class.simpleName}, lignes après les deux ouvertures = $apresOuvertures, " +
                "premier dire = ${r1.fold({ "ok" }, { "${it::class.simpleName} ${it.message}" })}, " +
                "second dire = ${r2.fold({ "ok" }, { "${it::class.simpleName} ${it.message}" })}, " +
                "lignes controle = ${journal.lignesCourantes().count { it.type == "controle" }}, lignes totales = ${journal.nombreDeLignes}, " +
                "sauvegardes = ${sauvegarde.saisies}",
        )
    }

    @Test
    fun `deux ouvertures confirmees de suite, un etat dit sur les deux - jamais deux controles pour la placette ce jour-la`() = with(BancControle()) {
        val premier = ArriveeControle.ouvrirSansPosition(contexte, journal, horloge, essences, "P35-01", sauvegarde, confirmee = true)
        val second = ArriveeControle.ouvrirSansPosition(contexte, journal, horloge, essences, "P35-01", sauvegarde, confirmee = true)
        for (e in listOf(premier, second)) (e as? EtatArrivee.Ouverte)?.let { runCatching { it.controleur.dire(EtatDsf.SAIN) } }
        assertEquals(1, journal.lignesCourantes().count { it.type == "controle" }, "un contrôle par placette et par jour")
    }
}
