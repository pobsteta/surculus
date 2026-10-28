package io.github.pobsteta.surculus.controle

import io.github.pobsteta.surculus.core.geo.EmpriseLambert93
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.gnss.Position
import io.github.pobsteta.surculus.core.placette.RefusPlacette
import io.github.pobsteta.surculus.core.session.Arrivee
import io.github.pobsteta.surculus.core.session.SuiteSession
import io.github.pobsteta.surculus.carte.SECONDES_DE_MOYENNE_ARRIVEE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Mission 012-B1 : l'arrivée (moyenne de 5 s, règle des 15 m, code du piquet) et ses refus montrés avec leur code. */
class ArriveeControleTest {
    private fun refus(e: EtatArrivee) = assertIs<EtatArrivee.Refus>(e).refus

    @Test fun `la moyenne dure 5 secondes et part toute seule`() {
        assertEquals(5, SECONDES_DE_MOYENNE_ARRIVEE)
        assertEquals(EtatArrivee.Moyenne(0, 5), EtatArrivee.Moyenne(0))
    }

    @Test fun `au piquet, la session s'ouvre sur la placette`() = with(BancControle()) {
        assertIs<EtatArrivee.Ouverte>(ouvrir())
        assertEquals(0, journal.nombreDeLignes, "ouvrir n'ecrit rien : la premiere saisie ecrit le controle")
    }

    @Test fun `la position moyennee est la moyenne des fix en Lambert-93`() {
        val a = BancControle.fixSur(PointLambert93(846_160.0, 6_686_120.0))
        val b = BancControle.fixSur(PointLambert93(846_170.0, 6_686_130.0))
        val arrivee = assertIs<Arrivee.Lambert93>(assertNotNull(ArriveeControle.arrivee(listOf(a, b))))
        assertEquals(846_165.0, arrivee.position.xMetres, 0.01)
        assertEquals(6_686_125.0, arrivee.position.yMetres, 0.01)
    }

    @Test fun `aucun fix pendant la moyenne - pas de position, rien n'est ouvert`() = with(BancControle()) {
        assertEquals(EtatArrivee.SansPosition, ouvrir(positions = emptyList()))
        assertEquals(0, journal.nombreDeLignes)
    }

    /** Placette de la Somalie (chapitre 15) : un fix hors de France ne pointe rien, et le refus porte son code. */
    @Test fun `un fix en Somalie est refuse hors_france, aucune session, aucune ligne`() = with(BancControle()) {
        val somalie = Position(latitude = 2.0469, longitude = 45.3182)
        val r = refus(ouvrir(positions = listOf(somalie)))
        assertEquals("hors_france", r.code)
        assertEquals("hors_france : ${r.message}", TextesControle.message(r)!!.texte)
        assertEquals(0, journal.nombreDeLignes)
        assertEquals(0, sauvegarde.saisies)
    }

    @Test fun `latitude et longitude inversees sont refusees hors_france, pas prises pour une absence de signal`() = with(BancControle()) {
        val bon = BancControle.fixSur(BancControle.POSITION_P1)
        val inverse = Position(latitude = bon.longitude, longitude = bon.latitude)
        assertEquals("hors_france", refus(ouvrir(positions = listOf(inverse))).code)
    }

    @Test fun `a plus de 15 m du piquet - hors_15_m avec son code, puis le code lu rattache par identite`() = with(BancControle()) {
        val loin = List(5) { BancControle.fixSur(PointLambert93(846_164.76 + 22.0, 6_686_123.26)) }
        val r = refus(ouvrir(positions = loin))
        assertEquals("hors_15_m", r.code)
        assertTrue(TextesControle.message(r)!!.texte.startsWith("hors_15_m : "))
        assertTrue(TextesControle.message(r)!!.erreur)
        assertIs<EtatArrivee.Ouverte>(ouvrir(positions = loin, codeLu = "P35-01"))
        Unit
    }

    @Test fun `un code de piquet inconnu est refuse code_inconnu`() = with(BancControle()) {
        assertEquals("code_inconnu", refus(ouvrir(codeLu = "ZZZ")).code)
    }

    @Test fun `un code lu vide ou fait d'espaces est ignore`() = with(BancControle()) {
        assertIs<EtatArrivee.Ouverte>(ouvrir(codeLu = "   "))
        Unit
    }

    @Test fun `hors de l'emprise de la foret - point_hors_foret`() {
        val b = BancControle(emprise = EmpriseLambert93(0.0, 0.0, 1.0, 1.0))
        assertEquals("point_hors_foret", refus(b.ouvrir()).code)
    }

    /** Un refus par code : le texte montré commence toujours par le code de la spec § 6. */
    @Test fun `chaque refus de la session est montre avec son code`() {
        val refus = listOf(
            RefusPlacette.HorsQuinzeMetres(22.0),
            RefusPlacette.CodeInconnu("X"),
            RefusPlacette.CauseSurEtat(io.github.pobsteta.surculus.core.placette.EtatDsf.SAIN),
            RefusPlacette.EtatInconnu("vivant"),
            RefusPlacette.GroupeCauseInconnu("x"),
            RefusPlacette.HauteurSurAbsent(10),
            RefusPlacette.AucunePlacette,
        ).map { SuiteSession.Refus.de(it) }
        refus.forEach { r ->
            assertTrue(r.code.isNotBlank())
            assertEquals("${r.code} : ${r.message}", TextesControle.message(r)!!.texte)
        }
        assertEquals("hors_referentiel", refus[3].code)
        assertEquals("hors_referentiel", refus[4].code)
    }

    @Test fun `la permission de stockage n'est demandee que sur Android 8 et 9`() {
        listOf(26, 27, 28).forEach { assertTrue(PermissionStockage.aDemander(it, accordee = false), "API $it") }
        listOf(29, 30, 34, 35).forEach { assertTrue(!PermissionStockage.aDemander(it, accordee = false), "API $it") }
        assertTrue(!PermissionStockage.aDemander(27, accordee = true))
        // Une seule fois : déjà demandée, on ne redemande pas, mais la ligne d'explication reste tant qu'elle manque.
        assertTrue(!PermissionStockage.aDemander(27, accordee = false, dejaDemandee = true))
        assertTrue(PermissionStockage.manque(27, accordee = false))
        assertTrue(!PermissionStockage.manque(34, accordee = false))
    }

    @Test fun `les essences de la session sont celles des plants de la tournee, sans liste de l'application`() {
        fun plant(u: String, e: String?) = io.github.pobsteta.surculus.gpkg.PlantLu(u, "placette-1", 1, e, false, null, null)
        val t = io.github.pobsteta.surculus.gpkg.Tournee(
            epsg = io.github.pobsteta.surculus.core.reperes.CodeEpsg.RGF93_V1_LAMBERT93, srsId = 2154, couches = emptyList(), placettes = emptyList(),
            controles = emptyList(), empriseForet = null, projet = null,
            plants = listOf(plant("a", "CHS"), plant("b", "CHS"), plant("c", null), plant("d", " ")), etats = emptyList(),
        )
        val e = ArriveeControle.essencesDe(t)
        assertTrue(e.valider("CHS") is io.github.pobsteta.surculus.core.placette.Verdict.Accepte)
        assertTrue(e.valider("ZZZ") is io.github.pobsteta.surculus.core.placette.Verdict.Refuse)
        assertTrue(ArriveeControle.essencesDe(t.copy(plants = emptyList())).valider("CHS") is io.github.pobsteta.surculus.core.placette.Verdict.Refuse)
    }

    @Test fun `un refus de stockage ne bloque pas la saisie - la session n'en depend pas`() = with(BancControle()) {
        // La session n'a aucun accès au stockage : l'écoute de sauvegarde est un port, qui échoue sans effet sur la saisie.
        val c = controleur()
        c.dire(io.github.pobsteta.surculus.core.placette.EtatDsf.SAIN)
        assertEquals("1 sur 12", c.instantane().plant.compteur)
    }
}
