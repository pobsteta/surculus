package io.github.pobsteta.surculus.gpkg

import io.github.pobsteta.surculus.core.geo.EmpriseLambert93
import io.github.pobsteta.surculus.core.geo.PointLambert93
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Mission 061, règle 1a de Pascal (2026-10-27) et les trois cas exigés par la spec § 4.1 avant tout tri « à faire ». */
class RegleAFaireTest {
    @TempDir
    lateinit var dossier: Path

    private fun placette(code: String, etat: EtatSuivi, uuid: String = "u-$code") =
        PlacetteTournee(uuid, code, null, null, null, null, PointLambert93(868_000.0, 6_700_000.0), etat)

    @Test
    fun `a faire = pas a_jour et pas saisie, a_jour en bas avec les saisies`() {
        val p = listOf(
            placette("P1", EtatSuivi.JAMAIS), placette("P2", EtatSuivi.A_JOUR), placette("P3", EtatSuivi.A_PROGRAMMER),
            placette("P4", EtatSuivi.INCONNU), placette("P5", EtatSuivi.JAMAIS),
        )
        val o = ordonnerPlacettes(p, controlees = setOf("u-P5"))
        assertEquals(listOf("P1", "P3", "P4"), o.aFaire.map { it.code })
        assertEquals(listOf("P2", "P5"), o.autres.map { it.code })
    }

    @Test
    fun `une seule placette a faire est la prochaine, plus aucune donne rien`() {
        val p = listOf(placette("P1", EtatSuivi.A_JOUR), placette("P2", EtatSuivi.A_JOUR), placette("P3", EtatSuivi.A_PROGRAMMER))
        assertEquals("P3", prochaineAFaire(p, emptySet(), null)?.code)
        assertNull(prochaineAFaire(p, setOf("u-P3"), null))
    }

    @Test
    fun `deux tournees des memes placettes - une saisie dans l'une ne rend pas la placette faite dans l'autre`() {
        val memes = listOf(placette("P1", EtatSuivi.JAMAIS, "uuid-commun"), placette("P2", EtatSuivi.JAMAIS, "uuid-p2"))
        val saisiesTourneeA = setOf("uuid-commun")
        val saisiesTourneeB = emptySet<String>()
        assertEquals(listOf("P2"), ordonnerPlacettes(memes, saisiesTourneeA).aFaire.map { it.code })
        assertEquals(listOf("P1", "P2"), ordonnerPlacettes(memes, saisiesTourneeB).aFaire.map { it.code })
    }

    @Test
    fun `placette a_jour ancienne - reste a jour, donc en bas, quelle que soit l'anciennete de son dernier controle`() {
        // `dernier_controle` lointain : l'état vient de sommieR, Surculus ne le recalcule pas (règle 1a).
        val ancienne = placette("P1", EtatSuivi.A_JOUR)
        assertTrue(!estAFaire(ancienne, emptySet()))
        assertEquals(listOf("P1"), ordonnerPlacettes(listOf(ancienne), emptySet()).autres.map { it.code })
    }

    @Test
    fun `plantation plus agee que l'itineraire - le tri ne plante pas et chaque placette reste listee`() {
        // Date incohérente : le tri ne lit aucune date (seuls l'état de suivi et les saisies comptent) ; une tournée de
        // Couchey passée en `a_jour` pour toutes ses placettes se lit et se trie sans exception, aucune ne disparaît.
        val f = TourneeDeTest.couchey(dossier)
        Sqlite.ouvrir(f).use { cnx -> cnx.createStatement().use { it.executeUpdate("UPDATE placettes SET etat_suivi = 'a_jour'") } }
        val o = ordonnerPlacettes(LecteurTournee.lire(f).placettes, emptySet())
        assertEquals(3, o.nombre)
        assertEquals(0, o.aFaire.size)
    }

    @Test
    fun `Couchey de la release - P35-03 seule a faire`() {
        val f = TourneeDeTest.couchey(dossier)
        Sqlite.ouvrir(f).use { cnx ->
            cnx.createStatement().use { st ->
                st.executeUpdate("UPDATE placettes SET etat_suivi = 'a_jour' WHERE code_placette IN ('P35-01', 'P35-02')")
                st.executeUpdate("UPDATE placettes SET etat_suivi = 'a_programmer' WHERE code_placette = 'P35-03'")
            }
        }
        val o = ordonnerPlacettes(LecteurTournee.lire(f).placettes, emptySet())
        assertEquals(listOf("P35-03"), o.aFaire.map { it.code })
        assertEquals("P35-03", prochaineAFaire(o.toutes, emptySet(), null)?.code)
    }
}

/** Mission 061 : maison, placettes sans position ou hors de France. */
class CadrageEtProblemesTest {
    private fun placette(code: String, x: Double?, y: Double?) =
        PlacetteTournee("u-$code", code, null, null, null, null, if (x == null || y == null) null else PointLambert93(x, y))

    @Test
    fun `maison - l'emprise de la tournee d'abord, sinon les placettes en France, sinon rien`() {
        val emprise = EmpriseLambert93(860_000.0, 6_690_000.0, 870_000.0, 6_700_000.0)
        assertEquals(emprise, empriseDeCadrage(emprise, listOf(placette("P1", 868_000.0, 6_700_000.0))))
        val a = placette("P1", 868_000.0, 6_700_000.0)
        val b = placette("P2", 868_200.0, 6_700_300.0)
        val cadre = assertNotNull(empriseDeCadrage(null, listOf(a, b)))
        assertEquals(868_000.0 - MARGE_M, cadre.xMinMetres)
        assertEquals(6_700_300.0 + MARGE_M, cadre.yMaxMetres)
        assertTrue(cadre.contient(PointLambert93(868_100.0, 6_700_100.0)))
        assertNotNull(empriseDeCadrage(null, listOf(a)), "une seule placette suffit")
        assertNull(empriseDeCadrage(null, emptyList()), "rien à cadrer : bouton grisé")
        assertNull(empriseDeCadrage(null, listOf(placette("P3", null, null))))
    }

    @Test
    fun `maison - les placettes hors de France (axes inverses, Somalie) ne sont jamais cadrees`() {
        val somalie = placette("P-SOM", 6_700_000.0, 868_000.0)
        assertNull(empriseDeCadrage(null, listOf(somalie)))
        val bourgogne = placette("P1", 868_000.0, 6_700_000.0)
        val cadre = assertNotNull(empriseDeCadrage(null, listOf(somalie, bourgogne)))
        assertTrue(cadre.xMaxMetres < 1_000_000.0)
    }

    @Test
    fun `une placette sans position ou hors de France n'est pas atteignable, et on ne navigue pas vers une autre en silence`() {
        val sans = placette("P1", null, null)
        val somalie = placette("P-SOM", 6_700_000.0, 868_000.0)
        val bonne = placette("P2", 868_000.0, 6_700_000.0)
        assertEquals(ProblemePlacette.SANS_POSITION, probleme(sans))
        assertEquals(ProblemePlacette.HORS_FRANCE, probleme(somalie))
        assertNull(probleme(bonne))
        val toutes = listOf(sans, somalie, bonne)
        assertNull(cibleNavigation(sans, toutes, emptySet(), null))
        assertNull(cibleNavigation(somalie, toutes, emptySet(), null))
        assertEquals("P2", cibleNavigation(null, toutes, emptySet(), null)?.code, "sans choix : la prochaine atteignable")
    }

    @Test
    fun `la prochaine a faire n'est jamais une placette hors de France`() {
        assertNull(prochaineAFaire(listOf(placette("P-SOM", 6_700_000.0, 868_000.0)), emptySet(), null))
    }
}
