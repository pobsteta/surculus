package io.github.pobsteta.surculus.core.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Mission 012-C1 : le classeur transforme des appuis et relâchements de volume en gestes (spec § 4.3), horloge injectée. */
class ClasseurGestesTest {
    private fun geste(g: Geste) = SortieClasseur.Geste(g)

    @Test fun `volume haut court - sain au relachement`() {
        val c = ClasseurGestes()
        assertEquals(emptyList(), c.appuye(Touche.HAUT, 0))
        assertEquals(listOf(geste(Geste.VOLUME_HAUT_COURT)), c.relache(Touche.HAUT, 120))
    }

    @Test fun `volume haut - le tic a 0,5 s, atteint au relachement avant 3 s`() {
        val c = ClasseurGestes()
        c.appuye(Touche.HAUT, 0)
        assertEquals(emptyList(), c.avancer(499))
        assertEquals(listOf(SortieClasseur.Tic), c.avancer(500))
        assertEquals(listOf(geste(Geste.VOLUME_HAUT_LONG)), c.relache(Touche.HAUT, 2_999))
    }

    @Test fun `a 499 ms c'est encore court, a 500 ms c'est long`() {
        val a = ClasseurGestes().apply { appuye(Touche.HAUT, 0) }
        assertEquals(listOf(geste(Geste.VOLUME_HAUT_COURT)), a.relache(Touche.HAUT, 499))
        val b = ClasseurGestes().apply { appuye(Touche.HAUT, 0) }
        assertEquals(listOf(SortieClasseur.Tic, geste(Geste.VOLUME_HAUT_LONG)), b.relache(Touche.HAUT, 500))
    }

    @Test fun `volume haut 3 s - la sortie, rien d'ecrit, le relachement est mange`() {
        val c = ClasseurGestes()
        c.appuye(Touche.HAUT, 0)
        assertEquals(listOf(SortieClasseur.Tic), c.avancer(500))
        assertEquals(listOf(SortieClasseur.Bourdon), c.avancer(3_000))
        assertEquals(emptyList(), c.relache(Touche.HAUT, 5_000))
    }

    @Test fun `un appui interminable de 30 s ne sort qu'une fois, sans geste`() {
        val c = ClasseurGestes()
        c.appuye(Touche.HAUT, 0)
        val sorties = c.avancer(3_000) + c.avancer(10_000) + c.avancer(30_000) + c.relache(Touche.HAUT, 30_000)
        assertEquals(1, sorties.count { it == SortieClasseur.Bourdon })
        assertTrue(sorties.none { it is SortieClasseur.Geste })
    }

    @Test fun `la sortie a 3 s est prise meme sans tic prealable (relachement tardif)`() {
        val c = ClasseurGestes()
        c.appuye(Touche.HAUT, 0)
        val s = c.relache(Touche.HAUT, 3_200)
        assertEquals(listOf(SortieClasseur.Tic, SortieClasseur.Bourdon), s)
    }

    @Test fun `volume bas court - mort seulement apres 0,35 s, pas avant`() {
        val c = ClasseurGestes()
        c.appuye(Touche.BAS, 0)
        assertEquals(emptyList(), c.relache(Touche.BAS, 100))
        assertEquals(450L, c.echeance())
        assertEquals(emptyList(), c.avancer(449))
        assertEquals(listOf(geste(Geste.VOLUME_BAS_COURT)), c.avancer(450))
        assertNull(c.echeance())
        assertEquals(emptyList(), c.avancer(10_000))
    }

    @Test fun `double appui bas - annule, et n'ecrit jamais mort`() {
        val c = ClasseurGestes()
        c.appuye(Touche.BAS, 0)
        c.relache(Touche.BAS, 100)
        assertEquals(listOf(geste(Geste.VOLUME_BAS_DOUBLE_COURT)), c.appuye(Touche.BAS, 300))
        assertEquals(emptyList(), c.relache(Touche.BAS, 380))
        assertEquals(emptyList(), c.avancer(5_000))
        assertNull(c.echeance())
    }

    @Test fun `deux appuis bas trop espaces font deux morts, pas un double`() {
        val c = ClasseurGestes()
        c.appuye(Touche.BAS, 0)
        c.relache(Touche.BAS, 100)
        // Pas de tic d'horloge entre-temps : le nouvel appui, 600 ms plus tard, libère d'abord le premier mort.
        assertEquals(listOf(geste(Geste.VOLUME_BAS_COURT)), c.appuye(Touche.BAS, 700))
        c.relache(Touche.BAS, 780)
        assertEquals(listOf(geste(Geste.VOLUME_BAS_COURT)), c.avancer(1_200))
    }

    @Test fun `volume bas long - la dictee est demandee a 0,5 s, pas de mort ensuite`() {
        val c = ClasseurGestes()
        c.appuye(Touche.BAS, 0)
        assertEquals(listOf(geste(Geste.VOLUME_BAS_LONG)), c.avancer(500))
        // 012-D2 : le relâchement après le long est signalé (tenir pour parler), sans autre geste.
        assertEquals(listOf(SortieClasseur.BasRelacheApresLong), c.relache(Touche.BAS, 900))
        assertEquals(emptyList(), c.avancer(5_000))
        // Un appui court juste après un long n'est pas un double.
        c.appuye(Touche.BAS, 1_000)
        c.relache(Touche.BAS, 1_100)
        assertEquals(listOf(geste(Geste.VOLUME_BAS_COURT)), c.avancer(1_450))
    }

    @Test fun `les deux touches ensemble sont ignorees, rien n'est ecrit`() {
        val c = ClasseurGestes()
        c.appuye(Touche.HAUT, 0)
        assertEquals(listOf(SortieClasseur.DeuxTouches), c.appuye(Touche.BAS, 100))
        val suite = c.avancer(600) + c.avancer(4_000) + c.relache(Touche.HAUT, 4_100) + c.relache(Touche.BAS, 4_200)
        assertTrue(suite.none { it is SortieClasseur.Geste || it == SortieClasseur.Bourdon }, "$suite")
        // Tout relâché : le classeur repart propre.
        c.appuye(Touche.HAUT, 5_000)
        assertEquals(listOf(geste(Geste.VOLUME_HAUT_COURT)), c.relache(Touche.HAUT, 5_100))
    }

    @Test fun `rebond - un second appui moins de 40 ms apres le relachement est ignore`() {
        val c = ClasseurGestes()
        c.appuye(Touche.HAUT, 0)
        assertEquals(listOf(geste(Geste.VOLUME_HAUT_COURT)), c.relache(Touche.HAUT, 100))
        assertEquals(emptyList(), c.appuye(Touche.HAUT, 120))
        assertEquals(emptyList(), c.relache(Touche.HAUT, 130))
        assertEquals(emptyList(), c.avancer(10_000))
    }

    @Test fun `rebond bas - ne fait pas un double appui, un vrai appui a 40 ms en fait un`() {
        val c = ClasseurGestes()
        c.appuye(Touche.BAS, 0)
        c.relache(Touche.BAS, 100)
        assertEquals(emptyList(), c.appuye(Touche.BAS, 120))
        assertEquals(emptyList(), c.relache(Touche.BAS, 130))
        assertEquals(listOf(geste(Geste.VOLUME_BAS_COURT)), c.avancer(450))
        val d = ClasseurGestes()
        d.appuye(Touche.BAS, 0)
        d.relache(Touche.BAS, 100)
        assertEquals(listOf(geste(Geste.VOLUME_BAS_DOUBLE_COURT)), d.appuye(Touche.BAS, 140))
    }

    @Test fun `un appui repete sans relachement n'est pas un nouvel appui`() {
        val c = ClasseurGestes()
        c.appuye(Touche.HAUT, 0)
        assertEquals(emptyList(), c.appuye(Touche.HAUT, 50))
        assertEquals(listOf(geste(Geste.VOLUME_HAUT_COURT)), c.relache(Touche.HAUT, 100))
    }

    @Test fun `remise a zero - un relachement perdu ne laisse rien en attente`() {
        val c = ClasseurGestes()
        c.appuye(Touche.HAUT, 0)
        c.remettreAZero()
        assertEquals(emptyList(), c.avancer(60_000))
        assertNull(c.echeance())
    }

    @Test fun `l'echeance suit l'appui en cours`() {
        val c = ClasseurGestes()
        assertNull(c.echeance())
        c.appuye(Touche.HAUT, 1_000)
        assertEquals(1_500L, c.echeance())
        c.avancer(1_500)
        assertEquals(4_000L, c.echeance())
    }

    // --- 012-D2 : tenir pour parler

    @Test fun `bas relache apres un long - le signal vient une seule fois, au relachement`() {
        val c = ClasseurGestes()
        c.appuye(Touche.BAS, 0)
        assertEquals(listOf(geste(Geste.VOLUME_BAS_LONG)), c.avancer(500))
        assertEquals(emptyList(), c.avancer(4_000)) // tenu : rien de plus
        assertEquals(listOf(SortieClasseur.BasRelacheApresLong), c.relache(Touche.BAS, 4_100))
        assertEquals(emptyList(), c.relache(Touche.BAS, 4_200))
    }

    @Test fun `bas court - aucun signal de relachement, le mort vient apres la fenetre`() {
        val c = ClasseurGestes()
        c.appuye(Touche.BAS, 0)
        assertEquals(emptyList(), c.relache(Touche.BAS, 200))
        assertEquals(listOf(geste(Geste.VOLUME_BAS_COURT)), c.avancer(550))
    }

    @Test fun `haut long relache - pas de signal de micro`() {
        val c = ClasseurGestes()
        c.appuye(Touche.HAUT, 0)
        c.avancer(500)
        assertEquals(listOf(geste(Geste.VOLUME_HAUT_LONG)), c.relache(Touche.HAUT, 900))
    }

    @Test fun `bas long puis deux touches - le micro ouvert se ferme quand meme au relachement du bas`() {
        val c = ClasseurGestes()
        c.appuye(Touche.BAS, 0)
        c.avancer(500)
        assertEquals(listOf(SortieClasseur.DeuxTouches), c.appuye(Touche.HAUT, 600))
        assertEquals(listOf(SortieClasseur.BasRelacheApresLong), c.relache(Touche.BAS, 700))
        assertEquals(emptyList(), c.relache(Touche.HAUT, 800))
    }

    @Test fun `bas long, haut relache avant - le signal vient au relachement du bas`() {
        val c = ClasseurGestes()
        c.appuye(Touche.BAS, 0)
        c.avancer(500)
        c.appuye(Touche.HAUT, 600)
        assertEquals(emptyList(), c.relache(Touche.HAUT, 700))
        assertEquals(listOf(SortieClasseur.BasRelacheApresLong), c.relache(Touche.BAS, 800))
    }

    @Test fun `haut tenu puis bas - le bas ne signale rien, aucun micro n'a ete ouvert`() {
        val c = ClasseurGestes()
        c.appuye(Touche.HAUT, 0)
        c.appuye(Touche.BAS, 100)
        c.avancer(1_000)
        assertEquals(emptyList(), c.relache(Touche.BAS, 1_100))
    }

    @Test fun `les seuils de la C1 sont inchanges`() {
        val s = SeuilsGestes()
        assertEquals(500L, s.longMs); assertEquals(3_000L, s.sortieMs); assertEquals(350L, s.doubleMs); assertEquals(40L, s.rebondMs)
    }
}
