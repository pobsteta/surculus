package io.github.pobsteta.surculus.core.session

import org.junit.jupiter.api.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Mission 101 (décision de Pascal du 2026-11-07, « Verrou sur la création ») : « un contrôle par placette et par jour »
 * (spec § 6, (placette, jour civil Europe/Paris) dans une tournée) tenu par les données même quand deux sessions de la même
 * placette écrivent **en même temps** : création du contrôle du jour, réécriture (besoin, concurrence) et « Terminer ».
 * Ouverture avec position (règle des 15 m) et sans position. Coordonnées : P35-01 à Couchey (Côte-d'Or), Lambert-93
 * (EPSG:2154, x = Est, y = Nord, mètres), en France.
 */
class ControleDuJourAtomiqueTest {
    private fun BancSession.sansPosition(): SessionControle = assertIs<OuvertureControle.Ouverte>(
        SessionControle.ouvrir(contexte, journalCourse, horloge, essences, Arrivee.SansPosition, "P35-01", ecoute),
    ).session

    private fun BancSession.avecPosition(): SessionControle = assertIs<OuvertureControle.Ouverte>(
        SessionControle.ouvrir(contexte, journalCourse, horloge, essences, Arrivee.Lambert93(BancSession.POSITION_P1), null, ecoute),
    ).session

    /** Le journal des courses, posé sur celui du banc (une instance par banc). */
    private val bancs = HashMap<BancSession, JournalCourse>()
    private val BancSession.journalCourse: JournalCourse get() = bancs.getOrPut(this) { JournalCourse(journal) }

    /** Lignes `controle` courantes de la placette, lues. */
    private fun BancSession.controlesCourants(): List<Pair<LigneCourante, ValeurControle>> =
        journal.lignesCourantes().filter { it.type == FormatValeurs.TYPE_CONTROLE }.map { it to assertNotNull(ValeurControle.lire(it.valeur), it.valeur) }

    /** Lance [actions] en même temps, une par fil ; rend les erreurs. */
    private fun BancSession.enMemeTemps(vararg actions: () -> Unit): List<Throwable> {
        journalCourse.armer(actions.size)
        val erreurs = Collections.synchronizedList(ArrayList<Throwable>())
        actions.map { a -> Thread { runCatching(a).onFailure { erreurs += it } }.also { it.start() } }.forEach { it.join(15_000) }
        return erreurs
    }

    @Test
    fun `avec position - deux sessions pointent en meme temps leur premier plant - une seule ligne controle, les deux pointages dessus`() =
        with(BancSession()) {
            val a = avecPosition()
            val b = avecPosition()
            val erreurs = enMemeTemps({ a.pointe(1.0, 10.0) }, { b.pointe(2.0, 200.0) })
            assertTrue(erreurs.isEmpty(), "aucune erreur : $erreurs")
            val controle = controlesCourants().single().first.objetUuid
            val pointages = journal.deType(FormatValeurs.TYPE_POINTAGE).map { assertNotNull(ValeurPointage.lire(it.valeur)) }
            assertEquals(2, pointages.size)
            assertTrue(pointages.all { it.controleUuid == controle }, "les deux pointages sont sur le même contrôle")
            assertEquals(1, journal.deType(FormatValeurs.TYPE_CONTROLE).size, "une seule ligne controle écrite")
        }

    @Test
    fun `sans position - deux sessions pointent en meme temps leur premier plant - une seule ligne controle`() = with(BancSession()) {
        val a = sansPosition()
        val b = sansPosition()
        val erreurs = enMemeTemps({ a.pointe(1.0, 10.0) }, { b.pointe(2.0, 200.0) })
        assertTrue(erreurs.isEmpty(), "aucune erreur : $erreurs")
        val (_, valeur) = controlesCourants().single()
        assertTrue(valeur.sansPositionMesuree)
        assertEquals(1, journal.deType(FormatValeurs.TYPE_CONTROLE).size)
    }

    @Test
    fun `besoin et concurrence dits en meme temps sur deux sessions - une seule ligne controle courante, qui porte les deux`() =
        with(BancSession()) {
            val a = avecPosition()
            a.pointe(1.0, 10.0)
            val b = avecPosition()
            val erreurs = enMemeTemps({ a.direBesoin("aucun") }, { b.direConcurrence("faible") })
            assertTrue(erreurs.isEmpty(), "aucune erreur : $erreurs")
            val (_, valeur) = controlesCourants().single()
            assertEquals("aucun", valeur.besoin, "la réécriture de l'une n'efface pas celle de l'autre")
            assertEquals("faible", valeur.concurrence)
        }

    @Test
    fun `Terminer et une concurrence redite en meme temps - une seule ligne controle courante, terminee`() = with(BancSession()) {
        val a = avecPosition()
        a.pointe(1.0, 10.0)
        a.dire("sain")
        a.direBesoin("aucun"); a.direConcurrence("faible")
        val b = avecPosition()
        val erreurs = enMemeTemps({ assertIs<SuiteSession.Fait>(a.terminer()) }, { b.direConcurrence("forte") })
        assertTrue(erreurs.isEmpty(), "aucune erreur : $erreurs")
        val (_, valeur) = controlesCourants().single()
        assertTrue(valeur.termine, "la fin n'est pas perdue")
        assertEquals("forte", valeur.concurrence, "la concurrence redite n'est pas perdue")
    }

    @Test
    fun `seconde ouverture sans position apres une premiere saisie - meme controle, la saisie s'y ajoute`() = with(BancSession()) {
        val a = sansPosition()
        a.pointe(1.0, 10.0)
        val controle = controlesCourants().single().first.objetUuid
        val b = sansPosition()
        assertIs<SuiteSession.Fait>(b.pointe(2.0, 200.0))
        assertEquals(controle, b.etat().controle?.valeur, "la seconde ouverture reprend le contrôle du jour")
        assertEquals(1, journal.deType(FormatValeurs.TYPE_CONTROLE).size)
        assertEquals(2, b.etat().nbPlants)
    }
}

/**
 * Journal dont la **première lecture de chaque fil**, une fois armé pour n fils, attend que les n fils soient arrivés à
 * lire (au plus 2 s) : force l'entrelacement « lire, lire, écrire, écrire ». Si la session sérialise la section (verrou
 * pris avant la lecture), un lecteur ne peut pas arriver : l'autre repart à l'échéance.
 */
private class JournalCourse(private val sous: JournalMemoire) : JournalControle {
    @Volatile private var arrives: CountDownLatch? = null
    private val dejaAttendu = ThreadLocal.withInitial { false }

    fun armer(n: Int) { arrives = CountDownLatch(n) }

    override fun nouvelUuid() = synchronized(sous) { sous.nouvelUuid() }

    override fun saisir(objetUuid: String, type: String, valeur: String, operateur: String, remplaceUuid: String?, fix: FixSession?) =
        synchronized(sous) { sous.saisir(objetUuid, type, valeur, operateur, remplaceUuid, fix) }

    override fun annuler(uuidCible: String, operateur: String) = synchronized(sous) { sous.annuler(uuidCible, operateur) }

    override fun lignesCourantes(): List<LigneCourante> {
        val lues = synchronized(sous) { sous.lignesCourantes() }
        val loquet = arrives
        if (loquet != null && !dejaAttendu.get()) {
            dejaAttendu.set(true)
            loquet.countDown()
            loquet.await(2, TimeUnit.SECONDS)
        }
        return lues
    }
}
