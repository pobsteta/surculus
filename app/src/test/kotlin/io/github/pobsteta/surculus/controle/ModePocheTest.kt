package io.github.pobsteta.surculus.controle

import io.github.pobsteta.surculus.core.session.Geste
import io.github.pobsteta.surculus.core.session.SortieClasseur
import io.github.pobsteta.surculus.core.session.Touche
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Mission 012-C1 : le mode poche ignore le toucher et le retour, garde le volume, et se lève par ses sorties. */
class ModePocheTest {
    @Test fun `le toucher est pris hors du mode poche, ignore dedans`() {
        assertTrue(ModePoche.toucherPris(actif = false))
        assertFalse(ModePoche.toucherPris(actif = true))
    }

    @Test fun `le retour systeme est ignore en poche seulement`() {
        assertTrue(ModePoche.retourPris(actif = false))
        assertFalse(ModePoche.retourPris(actif = true))
    }

    @Test fun `entrer puis sortir dit la raison`() {
        val m = ModePoche()
        val vus = ArrayList<Pair<Boolean, RaisonSortie?>>()
        m.abonner { a, r -> vus += a to r }
        m.entrer(0)
        assertTrue(m.actif)
        m.sortir(RaisonSortie.TROIS_SECONDES)
        assertFalse(m.actif)
        assertEquals(listOf(true to null, false to RaisonSortie.TROIS_SECONDES), vus)
        m.sortir(RaisonSortie.TERMINER)
        assertEquals(2, vus.size, "sortir hors du mode ne dit rien")
    }

    @Test fun `quinze minutes sans geste levent le mode, un geste repousse l'echeance`() {
        val m = ModePoche()
        m.entrer(0)
        assertEquals(15 * 60_000L, m.echeanceInactiviteMs())
        m.activite(5 * 60_000L)
        m.verifier(5 * 60_000L + 14 * 60_000L + 59_000L)
        assertTrue(m.actif)
        m.verifier(20 * 60_000L)
        assertFalse(m.actif)
        assertNull(m.echeanceInactiviteMs())
    }

    @Test fun `les textes de sortie disent pourquoi`() {
        assertEquals("Mode poche terminé.", TextesControle.sortiePoche(RaisonSortie.TROIS_SECONDES))
        assertEquals("Mode poche terminé : 15 minutes sans geste.", TextesControle.sortiePoche(RaisonSortie.INACTIVITE))
        assertEquals("Mode poche terminé : l'application a quitté l'écran.", TextesControle.sortiePoche(RaisonSortie.ARRIERE_PLAN))
        assertNull(TextesControle.sortiePoche(RaisonSortie.TERMINER))
        assertNull(TextesControle.sortiePoche(RaisonSortie.VUE_QUITTEE))
    }

    // --- le pont : touches -> classeur -> mode et écoute

    private fun pont(): Triple<PontPoche, ModePoche, MutableList<SortieClasseur>> {
        val m = ModePoche()
        val p = PontPoche(m)
        val vus = ArrayList<SortieClasseur>()
        p.ecoute = { vus += it }
        return Triple(p, m, vus)
    }

    @Test fun `hors du mode poche les touches de volume ne sont pas prises et ne font aucun geste`() {
        val (p, _, vus) = pont()
        assertFalse(p.touche(Touche.HAUT, appuyee = true, instantMs = 0))
        assertFalse(p.touche(Touche.HAUT, appuyee = false, instantMs = 100))
        assertTrue(vus.isEmpty())
    }

    @Test fun `en poche, un appui court sur volume haut est un geste et repousse l'inactivite`() {
        val (p, m, vus) = pont()
        m.entrer(0)
        assertTrue(p.touche(Touche.HAUT, true, 1_000))
        assertTrue(p.touche(Touche.HAUT, false, 1_100))
        assertEquals(listOf<SortieClasseur>(SortieClasseur.Geste(Geste.VOLUME_HAUT_COURT)), vus)
        assertEquals(1_100L + 15 * 60_000L, m.echeanceInactiviteMs())
    }

    @Test fun `en poche, 3 s sur volume haut sortent du mode sans aucun geste`() {
        val (p, m, vus) = pont()
        m.entrer(0)
        p.touche(Touche.HAUT, true, 0)
        p.avancer(3_000)
        assertFalse(m.actif)
        p.touche(Touche.HAUT, false, 4_000)
        assertTrue(vus.none { it is SortieClasseur.Geste }, "$vus")
    }

    @Test fun `le pont donne la prochaine echeance, classeur ou inactivite`() {
        val (p, m, _) = pont()
        m.entrer(0)
        assertEquals(15 * 60_000L, p.echeance())
        p.touche(Touche.BAS, true, 10)
        p.touche(Touche.BAS, false, 60)
        assertEquals(410L, p.echeance())
    }

    @Test fun `sortir du mode remet le classeur a zero`() {
        val (p, m, vus) = pont()
        m.entrer(0)
        p.touche(Touche.HAUT, true, 0)
        m.sortir(RaisonSortie.ARRIERE_PLAN)
        m.entrer(10_000)
        p.avancer(20_000)
        assertTrue(vus.isEmpty(), "$vus")
    }

    // --- reprise de la relecture de la PR #113

    @Test fun `une rotation ne leve pas le mode poche, un vrai passage en arriere-plan le leve`() {
        val m = ModePoche().apply { entrer(0) }
        m.surArret(changementDeConfiguration = true)
        assertTrue(m.actif, "tourner le telephone en mode poche : le mode reste actif")
        m.surArret(changementDeConfiguration = false)
        assertFalse(m.actif)
    }

    private fun source(chemin: String) = java.io.File("src/main/kotlin/io/github/pobsteta/surculus/$chemin").readText()

    @Test fun `l'Activity passe par la garde de rotation et l'ecran par retourPris`() {
        val activite = source("MainActivity.kt")
        assertTrue("PocheApp.mode.surArret(isChangingConfigurations)" in activite)
        assertFalse("sortir(RaisonSortie.ARRIERE_PLAN)" in activite, "pas de sortie sans garde dans l'Activity")
        assertTrue("BackHandler(enabled = !ModePoche.retourPris(poche))" in source("controle/EcranControle.kt"))
    }

    @Test fun `apres une rotation la nouvelle Activity rearme la minuterie des 15 minutes`() {
        val activite = source("MainActivity.kt")
        val onCreate = activite.substringAfter("override fun onCreate").substringBefore("setContent")
        assertTrue("if (PocheApp.mode.actif) {" in onCreate && "programmer()" in onCreate.substringAfter("if (PocheApp.mode.actif) {"),
            "onCreate doit rearmer programmer() quand le mode poche est actif")
    }

    @Test fun `les repetitions d'une touche prise, apres la sortie a 3 s, ne repartent pas au systeme`() {
        val (p, m, _) = pont()
        m.entrer(0)
        assertTrue(p.touche(Touche.HAUT, true, 0))
        p.avancer(3_000)
        assertFalse(m.actif)
        assertTrue(p.touche(Touche.HAUT, true, 3_100, repetition = true), "repetition prise")
        assertTrue(p.touche(Touche.HAUT, true, 3_200, repetition = true))
        assertTrue(p.touche(Touche.HAUT, false, 3_300), "le relachement est pris aussi")
        assertFalse(p.touche(Touche.HAUT, true, 4_000), "hors du mode, une nouvelle touche est au systeme")
        assertFalse(p.touche(Touche.HAUT, true, 4_100, repetition = true))
        assertFalse(p.touche(Touche.HAUT, false, 4_200))
    }
}
