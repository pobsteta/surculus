package io.github.pobsteta.surculus.controle

import io.github.pobsteta.surculus.core.placette.EtatDsf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

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
    fun `la seconde ouverture ne cree jamais un second controle, meme avec un etat dit sur le controle repris`() = with(BancControle()) {
        ouvrirEtDire("P35-01")
        val objet = journal.lignesCourantes().single { it.type == "controle" }.objetUuid
        val second = runCatching { ouvrirSansPosition("P35-01") }.getOrNull()
        // Un état dit sur le contrôle repris est une saisie légitime (spec § 4.3) : on compte les lignes « controle », pas toutes.
        (second as? EtatArrivee.Ouverte)?.let { runCatching { it.controleur.dire(EtatDsf.SAIN) } }
        val controlesLus = journal.lignesCourantes().filter { it.type == "controle" }
        assertEquals(1, controlesLus.size, "une seule ligne controle pour la placette ce jour-là")
        assertEquals(objet, controlesLus.single().objetUuid, "c'est le même contrôle")
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
        val objet = relu.lignesCourantes().single { it.type == "controle" }.objetUuid
        val second = runCatching {
            ArriveeControle.ouvrirSansPosition(apres.contexte, relu, banc.horloge, apres.essences, "P35-01", apres.sauvegarde, confirmee = true)
        }.getOrNull()
        (second as? EtatArrivee.Ouverte)?.let { runCatching { it.controleur.dire(EtatDsf.SAIN) } }
        val controlesLus = relu.lignesCourantes().filter { it.type == "controle" }
        assertEquals(1, controlesLus.size, "une seule ligne controle après redémarrage")
        assertEquals(objet, controlesLus.single().objetUuid, "le contrôle du jour est repris")
    }

    @Test
    fun `course - deux sessions de la meme placette, meme jour, premiere saisie en meme temps - une seule ligne controle`() {
        val banc = BancControle()
        val journal = JournalQuiAttendLesDeuxLecteurs(banc.journal)
        fun session() = assertIs<EtatArrivee.Ouverte>(
            ArriveeControle.ouvrirSansPosition(banc.contexte, journal, banc.horloge, banc.essences, "P35-01", EcouteComptee(), confirmee = true),
        ).controleur
        val a = session()
        val b = session()
        journal.armer()
        val erreurs = java.util.Collections.synchronizedList(ArrayList<Throwable>())
        val fils = listOf(a, b).map { c ->
            Thread { runCatching { c.dire(EtatDsf.SAIN) }.onFailure { erreurs += it } }.also { it.start() }
        }
        fils.forEach { it.join(15_000) }
        assertTrue(erreurs.isEmpty(), "aucune erreur : $erreurs")
        val controlesLus = journal.lignesCourantes().filter { it.type == "controle" }
        assertEquals(1, controlesLus.size, "deux sessions vivantes, un seul contrôle de la placette ce jour-là")
    }
}

/**
 * Journal dont la **première lecture de chaque fil**, une fois armé, attend que les deux fils soient arrivés à lire (au plus
 * 2 s : si la règle sérialise la création, le second lecteur ne peut pas arriver, le premier repart à l'échéance). Force
 * l'entrelacement « lire, lire, écrire, écrire » sans lire le code de la session.
 */
private class JournalQuiAttendLesDeuxLecteurs(private val sous: JournalEnMemoire) : io.github.pobsteta.surculus.core.session.JournalControle {
    @Volatile private var arme = false
    private val arrives = java.util.concurrent.CountDownLatch(2)
    private val dejaAttendu = ThreadLocal.withInitial { false }

    fun armer() { arme = true }

    override fun nouvelUuid() = synchronized(sous) { sous.nouvelUuid() }

    override fun saisir(
        objetUuid: String, type: String, valeur: String, operateur: String, remplaceUuid: String?,
        fix: io.github.pobsteta.surculus.core.session.FixSession?,
    ) = synchronized(sous) { sous.saisir(objetUuid, type, valeur, operateur, remplaceUuid, fix) }

    override fun annuler(uuidCible: String, operateur: String) = synchronized(sous) { sous.annuler(uuidCible, operateur) }

    override fun lignesCourantes(): List<io.github.pobsteta.surculus.core.session.LigneCourante> {
        val lues = synchronized(sous) { sous.lignesCourantes() }
        if (arme && !dejaAttendu.get()) {
            dejaAttendu.set(true)
            arrives.countDown()
            arrives.await(2, java.util.concurrent.TimeUnit.SECONDS)
        }
        return lues
    }
}
