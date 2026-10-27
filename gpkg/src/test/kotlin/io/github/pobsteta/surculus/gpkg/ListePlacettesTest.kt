package io.github.pobsteta.surculus.gpkg

import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.placette.EmpriseEtFrance
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Mission 061 : titre de la forêt, tri « à faire d'abord », prochaine placette à faire. */
class ListePlacettesTest {
    @TempDir
    lateinit var dossier: Path

    private fun placette(code: String?, x: Double = 868_000.0, y: Double = 6_700_000.0, uuid: String = "u-$code") =
        PlacetteTournee(uuid, code, null, null, null, null, PointLambert93(x, y))

    @Test
    fun `titre - le projet d'abord, puis la couche foret, puis le dossier`() {
        assertEquals("Forêt communale de Couchey", titreForet("Forêt communale de Couchey", "Autre", "tournee-1"))
        assertEquals("Forêt de Couchey (démo)", titreForet(null, "Forêt de Couchey (démo)", "tournee-1"))
        assertEquals("Forêt de Couchey (démo)", titreForet("", "Forêt de Couchey (démo)", "tournee-1"), "projet.foret vide (sommieR)")
        assertEquals("Forêt de Couchey (démo)", titreForet("  ", "Forêt de Couchey (démo)", "tournee-1"))
        assertEquals("tournee-1", titreForet(null, null, "tournee-1"))
        assertEquals("tournee-1", titreForet("", " ", "tournee-1"))
    }

    @Test
    fun `la couche foret de Couchey donne le nom de la foret`() {
        val t = LecteurTournee.lire(TourneeDeTest.couchey(dossier))
        assertEquals("Forêt de Couchey (démo)", t.nomForet)
        assertEquals("Forêt de Couchey (démo)", titreForet(t.projet?.foret, t.nomForet, "tournee-x"))
    }

    @Test
    fun `une couche foret vide ne donne pas de nom`() {
        assertNull(LecteurTournee.lire(TourneeDeTest.modeleVide(dossier)).nomForet)
    }

    @Test
    fun `a faire d'abord, puis les autres, chacun dans l'ordre du code, et le compte n et k`() {
        val p = listOf(placette("P3"), placette("P1"), placette("P2"), placette("P4"))
        val o = ordonnerPlacettes(p, controlees = setOf("u-P1", "u-P3"))
        assertEquals(listOf("P2", "P4"), o.aFaire.map { it.code })
        assertEquals(listOf("P1", "P3"), o.autres.map { it.code })
        assertEquals(listOf("P2", "P4", "P1", "P3"), o.toutes.map { it.code })
        assertEquals(4, o.nombre)
        assertEquals(2, o.aFaire.size)
    }

    @Test
    fun `sans saisie, toutes les placettes de la tournee sont a faire`() {
        val o = ordonnerPlacettes(listOf(placette("P2"), placette("P1")), emptySet())
        assertEquals(listOf("P1", "P2"), o.aFaire.map { it.code })
        assertEquals(emptyList(), o.autres)
    }

    @Test
    fun `une placette controlee le matin passe en bas, meme si son etat de suivi dit jamais`() {
        val jamais = placette("P1").copy(etatSuivi = EtatSuivi.JAMAIS)
        assertTrue(!estAFaire(jamais, setOf(jamais.uuid)))
        assertTrue(estAFaire(jamais, emptySet()))
    }

    @Test
    fun `prochaine a faire - la plus proche de la position, sinon la premiere`() {
        val proche = placette("P2", x = 868_010.0)
        val loin = placette("P1", x = 868_500.0)
        val depuis = PointLambert93(868_000.0, 6_700_000.0)
        assertEquals("P2", prochaineAFaire(listOf(loin, proche), emptySet(), depuis)?.code)
        assertEquals("P1", prochaineAFaire(listOf(proche, loin), emptySet(), null)?.code)
    }

    @Test
    fun `prochaine a faire - ignore les placettes saisies, rien si aucune a faire`() {
        val a = placette("P1")
        val b = placette("P2")
        assertEquals("P2", prochaineAFaire(listOf(a, b), setOf(a.uuid), null)?.code)
        assertNull(prochaineAFaire(listOf(a, b), setOf(a.uuid, b.uuid), null))
        assertNull(prochaineAFaire(emptyList(), emptySet(), null))
    }

    @Test
    fun `prochaine a faire - une placette sans position ou sans code n'est pas atteignable`() {
        val sansPosition = placette("P1").copy(position = null)
        val sansCode = placette(null)
        assertNull(prochaineAFaire(listOf(sansPosition, sansCode), emptySet(), null))
    }

    @Test
    fun `la prochaine placette de Couchey tombe en France, x = Est et y = Nord`() {
        val t = LecteurTournee.lire(TourneeDeTest.couchey(dossier))
        val p = assertNotNull(prochaineAFaire(t.placettes, emptySet(), null))
        assertEquals("P35-01", p.code)
        assertTrue(EmpriseEtFrance.controler(p.position!!).enFrance)
        // Placette de la Somalie : x et y inversés, hors de France, jamais cadrée comme une placette de Bourgogne.
        assertTrue(!EmpriseEtFrance.controler(PointLambert93(p.position!!.yMetres, p.position!!.xMetres)).enFrance)
    }
}

/** Mission 061 : « naviguer vers » et centrage de la carte. */
class CibleNavigationTest {
    private fun placette(code: String, x: Double, y: Double) =
        PlacetteTournee("u-$code", code, null, null, null, null, PointLambert93(x, y))

    @Test
    fun `naviguer vers la placette choisie, sinon la prochaine a faire, sinon rien`() {
        val a = placette("P1", 868_000.0, 6_700_000.0)
        val b = placette("P2", 868_100.0, 6_700_000.0)
        assertEquals("P2", cibleNavigation(b, listOf(a, b), emptySet(), null)?.code, "la choisie d'abord, même si P1 est la prochaine")
        assertEquals("P1", cibleNavigation(null, listOf(a, b), emptySet(), null)?.code)
        assertEquals("P2", cibleNavigation(null, listOf(a, b), setOf(a.uuid), null)?.code)
        assertNull(cibleNavigation(null, listOf(a, b), setOf(a.uuid, b.uuid), null), "aucune à faire : bouton inactif")
        assertNull(cibleNavigation(null, emptyList(), emptySet(), null))
    }

    @Test
    fun `la carte se centre sur une placette de Bourgogne, pas sur une placette en Somalie`() {
        val couchey = placette("P35-01", 868_000.0, 6_700_000.0)
        assertEquals(PointLambert93(868_000.0, 6_700_000.0), centrageInitial(couchey))
        // Axes inversés (x = Nord, y = Est) : la placette tombe en Somalie, on ne centre pas dessus.
        val somalie = placette("P-SOM", 6_700_000.0, 868_000.0)
        assertNull(centrageInitial(somalie))
        assertNull(centrageInitial(null))
        assertNull(centrageInitial(couchey.copy(position = null)))
    }
}
