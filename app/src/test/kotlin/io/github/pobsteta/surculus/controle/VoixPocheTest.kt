package io.github.pobsteta.surculus.controle

import io.github.pobsteta.surculus.core.gnss.Position
import io.github.pobsteta.surculus.core.session.Geste
import io.github.pobsteta.surculus.core.session.SortieClasseur
import io.github.pobsteta.surculus.core.session.SuiteSession
import io.github.pobsteta.surculus.core.session.Touche
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Mission 012-C2 : la voix et la vibration du mode poche (plan § 7 et § 8). */
class VoixPocheTest {
    private fun annonce(geste: Geste, prepare: (ControleurPlacette) -> Unit = {}): AnnoncePoche = with(BancControle()) {
        val c = controleur()
        prepare(c)
        c.geste(geste)
        assertNotNull(c.derniereAnnonce)
    }

    // --- textes exacts

    @Test fun `sain - le numero dit, l'etat, puis le suivant`() {
        val a = annonce(Geste.VOLUME_HAUT_COURT)
        assertEquals("numéro 1, sain. Suivant, 2.", a.texte)
        assertContentEquals(MotifsVibration.SAIN, a.motif)
        assertFalse(a.erreur)
    }

    @Test fun `mort - le numero dit, l'etat, puis le suivant`() {
        val a = annonce(Geste.VOLUME_BAS_COURT)
        assertEquals("numéro 1, mort. Suivant, 2.", a.texte)
        assertContentEquals(MotifsVibration.MORT, a.motif)
    }

    @Test fun `atteint - pas de suivant, et jamais le mot cause`() {
        val a = annonce(Geste.VOLUME_HAUT_LONG)
        assertEquals("numéro 1, atteint.", a.texte)
        assertContentEquals(MotifsVibration.ATTEINT, a.motif)
        assertFalse("cause" in a.texte.lowercase())
    }

    @Test fun `le dernier plant dit - tous les plants sont dits`() = with(BancControle(nbPlantsAnterieurs = 1)) {
        val c = controleur()
        c.geste(Geste.VOLUME_HAUT_COURT)
        assertEquals("numéro 1, sain. Tous les plants sont dits.", c.derniereAnnonce?.texte)
    }

    @Test fun `plus de plant a dire`() = with(BancControle(nbPlantsAnterieurs = 1)) {
        val c = controleur()
        c.geste(Geste.VOLUME_HAUT_COURT)
        c.geste(Geste.VOLUME_HAUT_COURT)
        assertEquals("plus de plant à dire.", c.derniereAnnonce?.texte)
        assertContentEquals(MotifsVibration.REFUS, c.derniereAnnonce?.motif)
        assertTrue(c.derniereAnnonce!!.erreur)
    }

    @Test fun `annulation - annule, sans inventer le numero du plant remis`() {
        val a = annonce(Geste.VOLUME_BAS_DOUBLE_COURT) { it.geste(Geste.VOLUME_HAUT_COURT) }
        assertEquals("annulé.", a.texte)
        assertContentEquals(MotifsVibration.ANNULE, a.motif)
    }

    @Test fun `rien a annuler`() {
        val a = annonce(Geste.VOLUME_BAS_DOUBLE_COURT)
        assertEquals("rien à annuler.", a.texte)
        assertContentEquals(MotifsVibration.REFUS, a.motif)
    }

    @Test fun `dictee pas encore disponible - pas une faute`() {
        val a = annonce(Geste.VOLUME_BAS_LONG)
        assertEquals("dictée pas encore disponible.", a.texte)
        assertContentEquals(MotifsVibration.REFUS, a.motif)
        assertFalse(a.erreur)
    }

    @Test fun `deux touches ensemble`() = with(BancControle()) {
        val c = controleur()
        c.deuxTouches()
        assertEquals("une touche à la fois.", c.derniereAnnonce?.texte)
        assertContentEquals(MotifsVibration.REFUS, c.derniereAnnonce?.motif)
    }

    @Test fun `autre refus - on renvoie a l'ecran, le code n'est jamais dit`() {
        val a = AnnoncesPoche.pourSuite(Geste.VOLUME_HAUT_COURT, SuiteSession.Refus("R-12", "m", "motif long"), numeroDit = 1, numeroApres = 1, memePlant = true, termine = false)
        assertEquals("refusé. Regardez l'écran.", a.texte)
        assertTrue(a.erreur)
    }

    @Test fun `placette deja terminee - l'alerte avant le numero, a chaque geste`() = with(BancControle()) {
        val c = controleur()
        c.geste(Geste.VOLUME_HAUT_COURT)
        c.demanderTerminer(); c.terminer(); c.reprendre()
        c.geste(Geste.VOLUME_HAUT_COURT)
        assertEquals("placette déjà terminée. Numéro 2, sain. Suivant, 3.", c.derniereAnnonce?.texte)
        assertContentEquals(MotifsVibration.DEJA_TERMINEE, c.derniereAnnonce?.motif)
        c.geste(Geste.VOLUME_BAS_COURT)
        assertTrue(c.derniereAnnonce!!.texte.startsWith("placette déjà terminée. Numéro 3, mort."), c.derniereAnnonce!!.texte)
    }

    @Test fun `double appui bas apres Terminer - la placette est rouverte, pas d'alerte de placette terminee`() = with(BancControle()) {
        val c = controleur()
        c.geste(Geste.VOLUME_HAUT_COURT)
        c.demanderTerminer(); c.terminer(); c.reprendre()
        c.geste(Geste.VOLUME_BAS_DOUBLE_COURT)
        assertEquals("annulé.", c.derniereAnnonce?.texte)
        assertContentEquals(MotifsVibration.ANNULE, c.derniereAnnonce?.motif)
    }

    @Test fun `entree, sortie, retour, rappel`() {
        assertEquals("mode poche actif. Numéro 7.", AnnoncesPoche.entree(7).texte)
        assertEquals("mode poche actif.", AnnoncesPoche.entree(null).texte)
        assertContentEquals(MotifsVibration.ENTREE, AnnoncesPoche.entree(7).motif)
        assertEquals("mode poche terminé.", AnnoncesPoche.sortie().texte)
        assertEquals("mode poche actif.", AnnoncesPoche.retour().texte)
        assertEquals("mode poche actif, 3 secondes sur volume haut pour sortir", AnnoncesPoche.rappel().texte)
    }

    // --- motifs

    @Test fun `les motifs sont ceux du plan, tous distincts et de moins d'une seconde`() {
        assertContentEquals(longArrayOf(0, 60), MotifsVibration.SAIN)
        assertContentEquals(longArrayOf(0, 60, 80, 60), MotifsVibration.ATTEINT)
        assertContentEquals(longArrayOf(0, 400), MotifsVibration.MORT)
        assertContentEquals(longArrayOf(0, 60, 80, 400), MotifsVibration.ANNULE)
        assertContentEquals(longArrayOf(0, 40, 40, 40, 40, 40), MotifsVibration.REFUS)
        assertContentEquals(longArrayOf(0, 30), MotifsVibration.TIC)
        assertContentEquals(longArrayOf(0, 600), MotifsVibration.BOURDON)
        assertContentEquals(longArrayOf(0, 100, 100, 100), MotifsVibration.ENTREE)
        assertContentEquals(longArrayOf(0, 400, 100, 60), MotifsVibration.DEJA_TERMINEE)
        val tous = MotifsVibration.tous
        assertEquals(9, tous.size)
        assertEquals(tous.size, tous.map { it.toList() }.toSet().size, "deux motifs identiques")
        tous.forEach { assertTrue(it.sum() < 1_000, "motif trop long : ${it.toList()}") }
    }

    // --- sorties : voix, repli, vibration

    private class MoteurFaux(var francais: Boolean = true, var echec: Boolean = false) : MoteurVoix {
        val dits = ArrayList<String>()
        override val francaisDisponible get() = francais
        override fun dire(texte: String): Boolean { if (echec) return false; dits += texte; return true }
    }

    private class VibreurFaux(val casse: Boolean = false) : Vibreur {
        val motifs = ArrayList<List<Long>>()
        override fun vibrer(motif: LongArray) { if (casse) error("pas de vibreur"); motifs += motif.toList() }
    }

    @Test fun `avec la voix - dite et vibree, rien d'ecrit dans la bande`() {
        val m = MoteurFaux(); val v = VibreurFaux(); val ecrits = ArrayList<AnnoncePoche>()
        val s = SortiesPoche(m, v).also { it.surTexte = { a -> ecrits += a } }
        s.annoncer(AnnoncesPoche.entree(3))
        assertEquals(listOf("mode poche actif. Numéro 3."), m.dits)
        assertEquals(listOf(MotifsVibration.ENTREE.toList()), v.motifs)
        assertTrue(ecrits.isEmpty())
    }

    @Test fun `sans voix francaise - le texte va dans la bande, la vibration reste, jamais de silence`() {
        val m = MoteurFaux(francais = false); val v = VibreurFaux(); val ecrits = ArrayList<AnnoncePoche>()
        val s = SortiesPoche(m, v).also { it.surTexte = { a -> ecrits += a } }
        assertTrue(s.sansVoixFrancaise)
        s.annoncer(AnnoncesPoche.pourSuite(Geste.VOLUME_HAUT_COURT, SuiteSession.Fait(), numeroDit = 1, numeroApres = 2, memePlant = false, termine = false))
        assertTrue(m.dits.isEmpty())
        assertEquals("numéro 1, sain. Suivant, 2.", ecrits.single().texte)
        assertEquals(listOf(MotifsVibration.SAIN.toList()), v.motifs)
    }

    @Test fun `l'annonce ecrite et le bandeau sont tenus hors de Compose, ils traversent la rotation`() {
        val s = SortiesPoche(MoteurFaux(francais = false), VibreurFaux())
        assertTrue(s.sansVoixFrancaise)
        s.annoncer(AnnoncesPoche.rappel())
        assertEquals(AnnoncesPoche.rappel(), s.annonceEcrite, "lue dans SortiesPoche, sans surTexte (l'ecran recree)")
        s.effacer()
        assertNull(s.annonceEcrite)
        val e = source("controle/EcranControle.kt")
        assertFalse("sansVoix by remember" in e || "annonceEcrite by remember" in e, "pas d'etat local de Compose")
        assertTrue("sorties?.sansVoixFrancaise" in e)
    }

    @Test fun `initialisation de la voix en echec - meme repli ecrit`() {
        val m = MoteurFaux(echec = true); val v = VibreurFaux(); val ecrits = ArrayList<AnnoncePoche>()
        SortiesPoche(m, v).also { it.surTexte = { a -> ecrits += a } }.annoncer(AnnoncesPoche.rappel())
        assertEquals(1, ecrits.size)
        assertEquals(1, v.motifs.size)
    }

    @Test fun `le bandeau d'entree dit l'absence de voix francaise`() {
        assertEquals("Pas de voix française : regardez l'écran.", AnnoncesPoche.BANDEAU_SANS_VOIX)
    }

    @Test fun `appareil sans vibreur - rien ne casse, la voix reste`() {
        val m = MoteurFaux(); val s = SortiesPoche(m, VibreurFaux(casse = true))
        s.annoncer(AnnoncesPoche.sortie())
        s.signal(SortieClasseur.Tic)
        assertEquals(listOf("mode poche terminé."), m.dits)
    }

    @Test fun `bourdon vibre, et la voix de sortie ne le couvre pas`() {
        val v = VibreurFaux(); val s = SortiesPoche(MoteurFaux(), v)
        s.signal(SortieClasseur.Bourdon)
        s.annoncer(AnnoncesPoche.sortie())
        assertEquals(listOf(MotifsVibration.BOURDON.toList()), v.motifs)
    }

    @Test fun `tic vibre, les gestes se disent ailleurs`() {
        val v = VibreurFaux(); val s = SortiesPoche(MoteurFaux(), v)
        s.signal(SortieClasseur.Tic)
        s.signal(SortieClasseur.Geste(Geste.VOLUME_HAUT_COURT))
        s.signal(SortieClasseur.DeuxTouches)
        assertEquals(listOf(MotifsVibration.TIC.toList()), v.motifs)
    }

    // --- rappel à 5 minutes

    @Test fun `rappel a 5 minutes sans geste - une seule fois par periode`() {
        val m = ModePoche().apply { entrer(0) }
        val p = PontPoche(m)
        var rappels = 0
        p.surRappel = { rappels++ }
        assertEquals(5 * 60_000L, p.echeance())
        p.avancer(5 * 60_000L - 1); assertEquals(0, rappels)
        p.avancer(5 * 60_000L); assertEquals(1, rappels)
        p.avancer(9 * 60_000L); assertEquals(1, rappels, "une seule fois")
        assertEquals(15 * 60_000L, p.echeance())
        // Un geste ouvre une nouvelle période.
        p.touche(Touche.HAUT, true, 10 * 60_000L); p.touche(Touche.HAUT, false, 10 * 60_000L + 100)
        p.avancer(15 * 60_000L + 100); assertEquals(2, rappels)
    }

    @Test fun `aucun rappel apres la sortie`() {
        val m = ModePoche().apply { entrer(0) }
        val p = PontPoche(m)
        var rappels = 0
        p.surRappel = { rappels++ }
        m.sortir(RaisonSortie.ARRIERE_PLAN)
        p.avancer(6 * 60_000L)
        assertEquals(0, rappels)
        assertNull(p.echeance())
        assertFalse(m.rappelDu(6 * 60_000L), "le mode est levé : aucun rappel")
        assertNull(m.echeanceRappelMs())
    }

    // --- Somalie

    /** Placette de la Somalie (chapitre 15) : hors de France, rien n'est pointé, donc rien n'est annoncé comme écrit. */
    @Test fun `un fix en Somalie ne pointe rien et rien n'est annonce comme ecrit`() = with(BancControle()) {
        val somalie = Position(latitude = 2.0469, longitude = 45.3182)
        assertIs<EtatArrivee.Refus>(ouvrir(positions = listOf(somalie)))
        val voix = MoteurFaux(); val s = SortiesPoche(voix, VibreurFaux())
        val controleur = (ouvrir(positions = listOf(somalie)) as? EtatArrivee.Ouverte)?.controleur
        val pont = PontPoche(ModePoche().apply { entrer(0) })
        pont.ecoute = { sortie ->
            if (sortie is SortieClasseur.Geste && controleur != null) { controleur.geste(sortie.geste); controleur.derniereAnnonce?.let(s::annoncer) }
        }
        pont.touche(Touche.HAUT, true, 0); pont.touche(Touche.HAUT, false, 100); pont.avancer(1_000)
        assertNull(controleur)
        assertTrue(voix.dits.none { "numéro" in it }, "${voix.dits}")
        assertEquals(0, journal.nombreDeLignes)
    }

    // --- câblage

    private fun source(chemin: String) = java.io.File("src/main/kotlin/io/github/pobsteta/surculus/$chemin").readText()

    @Test fun `le manifeste declare VIBRATE`() {
        assertTrue("\"android.permission.VIBRATE\"" in java.io.File("src/main/AndroidManifest.xml").readText())
    }

    @Test fun `l'adaptateur vocal est francais et interrompt la precedente`() {
        val a = source("controle/VoixAndroid.kt")
        assertTrue("Locale.FRANCE" in a)
        assertTrue("QUEUE_FLUSH" in a)
        assertFalse("vosk" in a.lowercase(), "la voix n'a aucun lien avec Vosk")
    }

    @Test fun `l'ecran branche la voix, le rappel et la bande`() {
        val e = source("controle/EcranControle.kt")
        assertTrue("PocheApp.sorties" in e)
        assertTrue("surRappel" in e)
        assertTrue("BANDEAU_SANS_VOIX" in e)
    }
}
