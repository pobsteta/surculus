package io.github.pobsteta.surculus.controle

import io.github.pobsteta.surculus.core.placette.EtatDsf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Mission 101 : une seconde ouverture sans position, le même jour, de la même placette, reprend le contrôle déjà ouvert, sans
 * nouvelle ligne au journal (spec § 4 et § 6 : un contrôle par (placette, jour civil Europe/Paris) dans une tournée ; décision de
 * Pascal du 2026-11-07 : « Oui, mission à part »). Écrit par le `testeur` sans lire le code de la règle.
 *
 * Restent ouverts jusqu'au plan du `dev`, donc **non figés** ici : l'ouverture avec position (règle des 15 m), la forme de ce que
 * rend la règle à la seconde ouverture (contrôle existant ou refus nommé), le cas d'un contrôle déjà terminé. Les tests ne
 * jugent que le journal et, si la règle rend un contrôle, que ce contrôle est le même (aucun second contrôle ne naît de lui).
 */
class SecondeOuvertureSansPositionTest {
    private fun BancControle.ouvrirSansPosition(code: String) =
        ArriveeControle.ouvrirSansPosition(contexte, journal, horloge, essences, code, sauvegarde, confirmee = true)

    private fun BancControle.controles() = journal.lignesCourantes().count { it.type == "controle" }

    /** Ouvre puis dit un état : l'ouverture seule n'écrit rien, la ligne « controle » naît avec la première saisie. */
    private fun BancControle.ouvrirEtDire(code: String) {
        assertIs<EtatArrivee.Ouverte>(ouvrirSansPosition(code)).controleur.dire(EtatDsf.SAIN)
    }

    @Test
    fun `deux ouvertures de suite, meme placette, meme jour - la seconde n'ecrit rien au journal`() = with(BancControle()) {
        ouvrirEtDire("P35-01")
        val lignes = journal.nombreDeLignes
        val saisies = sauvegarde.saisies
        runCatching { ouvrirSansPosition("P35-01") }
        assertEquals(lignes, journal.nombreDeLignes, "aucune ligne en plus au journal")
        assertEquals(saisies, sauvegarde.saisies, "aucune écriture de plus")
    }

    @Test
    fun `la seconde ouverture ne cree jamais un second controle, meme avec un etat dit dessus`() = with(BancControle()) {
        ouvrirEtDire("P35-01")
        val lignes = journal.nombreDeLignes
        val second = runCatching { ouvrirSansPosition("P35-01") }.getOrNull()
        (second as? EtatArrivee.Ouverte)?.let { runCatching { it.controleur.dire(EtatDsf.SAIN) } }
        assertEquals(1, controles(), "un contrôle par placette et par jour")
        assertEquals(1, journal.lignesCourantes().filter { it.type == "controle" }.map { it.objetUuid }.toSet().size)
        assertEquals(lignes, journal.nombreDeLignes, "le journal ne porte qu'une ouverture par placette et par jour")
    }

    @Test
    fun `une autre placette le meme jour - l'ouverture reste permise`() = with(BancControle()) {
        ouvrirEtDire("P35-01")
        // P35-02 n'a aucun plant pointé dans le banc : on juge l'ouverture, pas une saisie.
        assertIs<EtatArrivee.Ouverte>(ouvrirSansPosition("P35-02"))
        assertEquals(1, controles(), "le contrôle de P35-01 reste seul courant")
    }

    @Test
    fun `la meme placette le lendemain - une nouvelle ouverture reste permise`() = with(BancControle()) {
        ouvrirEtDire("P35-01")
        horloge.maintenantMs += 24L * 3600 * 1000
        ouvrirEtDire("P35-01")
        assertEquals(2, controles(), "un contrôle par jour civil")
    }

    @Test
    fun `apres redemarrage, journal relu - la seconde ouverture reprend toujours le controle du jour`() {
        val banc = BancControle()
        banc.ouvrirEtDire("P35-01")
        // Redémarrage : plus aucun objet de la session, seuls un contexte neuf et le journal rejoué tel quel subsistent.
        val apres = BancControle()
        val relu = JournalEnMemoire()
        banc.journal.lignesCourantes().forEach { relu.saisir(it.objetUuid, it.type, it.valeur ?: "", "Agent d'essai", null, null) }
        val lignes = relu.nombreDeLignes
        val second = runCatching {
            ArriveeControle.ouvrirSansPosition(apres.contexte, relu, banc.horloge, apres.essences, "P35-01", apres.sauvegarde, confirmee = true)
        }.getOrNull()
        (second as? EtatArrivee.Ouverte)?.let { runCatching { it.controleur.dire(EtatDsf.SAIN) } }
        assertEquals(lignes, relu.nombreDeLignes, "aucune ligne en plus après redémarrage")
        assertEquals(1, relu.lignesCourantes().count { it.type == "controle" })
    }
}
