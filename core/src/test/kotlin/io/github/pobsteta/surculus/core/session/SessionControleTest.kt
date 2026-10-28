package io.github.pobsteta.surculus.core.session

import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.placette.EtatDsf
import io.github.pobsteta.surculus.core.placette.Observation
import io.github.pobsteta.surculus.core.placette.UuidPlacette
import io.github.pobsteta.surculus.core.placette.UuidPlant
import io.github.pobsteta.surculus.core.placette.Verdict
import io.github.pobsteta.surculus.core.reperes.PointGeographique
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** La session de contrôle d'une placette sur le journal (mission 012-A, spec § 4.3, § 5 et § 6). */
class SessionControleTest {

    private fun BancSession.controlees() = PlacettesControlees.de(journal.lignesCourantes())

    private fun refus(r: SuiteSession) = assertIs<SuiteSession.Refus>(r)

    // ------------------------------------------------------------ table des gestes (spec § 4.3, « en placette »)

    @Test
    fun `table des gestes en placette`() {
        assertEquals(ActionGeste.Dire(EtatDsf.SAIN, puisSuivant = true), GestesPlacette.action(Geste.VOLUME_HAUT_COURT))
        assertEquals(ActionGeste.Dire(EtatDsf.MORT, puisSuivant = true), GestesPlacette.action(Geste.VOLUME_BAS_COURT))
        assertEquals(ActionGeste.Dire(EtatDsf.ATTEINT, puisSuivant = false), GestesPlacette.action(Geste.VOLUME_HAUT_LONG))
        assertEquals(ActionGeste.OuvrirDictee, GestesPlacette.action(Geste.VOLUME_BAS_LONG))
        assertEquals(ActionGeste.AnnulerDerniereSaisie, GestesPlacette.action(Geste.VOLUME_BAS_DOUBLE_COURT))
    }

    @Test
    fun `les cinq gestes sont dans la table, aucun n'est oublie`() {
        assertEquals(5, Geste.entries.size)
        Geste.entries.forEach { GestesPlacette.action(it) }
    }

    @Test
    fun `volume haut court dit sain puis passe au plant suivant, volume bas court dit mort`() = with(BancSession()) {
        val s = session()
        s.pointe(1.0, 10.0)
        s.pointe(2.0, 100.0)
        s.pointe(3.0, 200.0)
        // Le plant courant est le dernier pointé (n° 3) ; on revient au premier sans état : suivant() repart du début si besoin.
        s.suivant()
        assertNull(s.etat().courant, "après le dernier plant, plus de courant")
        s.suivant()
        assertEquals(1, s.etat().plants.first { it.uuid == s.etat().courant }.numero)
        assertIs<SuiteSession.Fait>(s.appliquer(Geste.VOLUME_HAUT_COURT))
        assertEquals(2, s.etat().plants.first { it.uuid == s.etat().courant }.numero)
        assertIs<SuiteSession.Fait>(s.appliquer(Geste.VOLUME_BAS_COURT))
        assertEquals(3, s.etat().plants.first { it.uuid == s.etat().courant }.numero)
        val e = s.etat().plants
        assertEquals(EtatDsf.SAIN, (e[0].duJour as Observation.Observe).etat)
        assertEquals(EtatDsf.MORT, (e[1].duJour as Observation.Observe).etat)
        assertNull(e[2].duJour)
    }

    @Test
    fun `volume haut long dit atteint et reste sur le plant, la cause vient juste apres`() = with(BancSession()) {
        val s = session()
        s.pointe(1.0, 10.0)
        assertIs<SuiteSession.Fait>(s.appliquer(Geste.VOLUME_HAUT_LONG))
        assertIs<SuiteSession.Fait>(s.preciser(causeGroupe = "animaux", causeDetail = "abroutissement", hauteurCm = 35))
        val o = s.etat().plants.single().duJour as Observation.Observe
        assertEquals(EtatDsf.ATTEINT, o.etat)
        assertEquals("animaux", o.causeGroupe?.code)
        assertEquals(35, o.hauteurCm)
        assertTrue(o.abrouti)
    }

    @Test
    fun `volume bas long ouvre la dictee et n'ecrit rien`() = with(BancSession()) {
        val s = session()
        s.pointe(1.0, 10.0)
        val avant = journal.lignes.size
        assertEquals(SuiteSession.DicteeDemandee, s.appliquer(Geste.VOLUME_BAS_LONG))
        assertEquals(avant, journal.lignes.size)
    }

    @Test
    fun `volume bas double appui court annule la derniere saisie`() = with(BancSession()) {
        val s = session()
        s.pointe(1.0, 10.0)
        s.dire("sain")
        assertEquals(1, s.etat().nbDits)
        assertIs<SuiteSession.Fait>(s.appliquer(Geste.VOLUME_BAS_DOUBLE_COURT))
        assertEquals(0, s.etat().nbDits, "l'état annulé ne compte plus")
        assertEquals(1, s.etat().nbPlants, "le pointage reste")
    }

    // ------------------------------------------------------------ pointer, dire, totaux

    @Test
    fun `pointer ecrit le controle puis le pointage avec l'observation brute et les termes derives`() = with(BancSession()) {
        val s = session()
        assertIs<SuiteSession.Fait>(s.pointe(2.0, 100.0))
        assertEquals(listOf("controle", "pointage"), journal.lignes.map { it.type })
        val c = ValeurControle.lire(journal.deType("controle").single().valeur)!!
        assertEquals("2026-10-27", c.jourCivil)
        assertEquals("Agent d'essai", c.operateur)
        assertFalse(c.termine)
        assertEquals(2154, c.repereEpsg)
        val p = ValeurPointage.lire(journal.deType("pointage").single().valeur)!!
        assertEquals(2.0, p.distanceHorizontaleM)
        assertEquals(100.0, p.azimutMag)
        assertEquals("deg", p.uniteAzimut)
        assertEquals("boussole_telephone", p.sourceAzimut)
        assertEquals("CHS", p.essence)
        // Le plant est à 2 m du piquet, en Lambert-93 (x = E, y = N), dans le rayon.
        val d = Math.hypot(p.x - BancSession.POSITION_P1.xMetres, p.y - BancSession.POSITION_P1.yMetres)
        assertEquals(2.0, d, 1e-6)
        assertEquals(c.positionX, BancSession.POSITION_P1.xMetres)
        // Toutes les lignes portent l'opérateur de la tournée.
        assertTrue(journal.lignes.all { it.operateur == "Agent d'essai" })
    }

    @Test
    fun `les totaux et 7 sur 12 viennent des plants dits`() = with(BancSession()) {
        val s = session()
        repeat(4) { s.pointe(1.0 + it * 0.5, 10.0 + it * 90.0) }
        // Du plus ancien au plus récent : sain 35 cm, atteint abrouti 40 cm, mort, absent.
        val uuids = s.etat().plants.map { it.uuid }
        fun dit(i: Int, etat: String, g: String? = null, d: String? = null, h: Int? = null) {
            assertTrue(s.choisir(uuids[i]))
            assertIs<SuiteSession.Fait>(s.dire(etat, g, d, h))
        }
        dit(0, "sain", h = 35)
        dit(1, "atteint", d = "abroutissement", h = 40)
        dit(2, "mort", g = "animaux")
        assertEquals(3, s.etat().nbDits)
        assertEquals(4, s.etat().nbPlants)
        val t = (s.etat().totaux as Verdict.Accepte).valeur
        assertEquals(3, t.nbTotal)
        assertEquals(2, t.nbVivants)
        assertEquals(1, t.nbAbroutis)
        assertEquals(38, t.hMoyCm) // (35 + 40) / 2 = 37,5, demi vers le haut
    }

    @Test
    fun `un non observe sort des totaux`() = with(BancSession()) {
        val s = session()
        s.pointe(1.0, 10.0)
        assertIs<SuiteSession.Fait>(s.nonObserve())
        val t = (s.etat().totaux as Verdict.Accepte).valeur
        assertEquals(0, t.nbTotal)
        assertEquals(1, s.etat().nbDits)
    }

    @Test
    fun `corriger un etat reecrit la ligne du meme objet, sans doublon dans les totaux`() = with(BancSession()) {
        val s = session()
        s.pointe(1.0, 10.0)
        s.dire("sain")
        s.dire("mort")
        assertEquals(2, journal.deType("etat").size)
        assertEquals(1, journal.deType("etat").map { it.objetUuid }.toSet().size, "un seul objet : la correction remplace")
        assertEquals(journal.deType("etat")[0].uuid, journal.deType("etat")[1].remplaceUuid)
        assertEquals(EtatDsf.MORT, (s.etat().plants.single().duJour as Observation.Observe).etat)
        assertEquals(1, (s.etat().totaux as Verdict.Accepte).valeur.nbTotal)
    }

    @Test
    fun `les refus portent leur code de la spec 6`() = with(BancSession()) {
        val s = session()
        s.pointe(1.0, 10.0)
        assertEquals("hors_referentiel", refus(s.dire("vivant")).code)
        assertEquals("hors_referentiel", refus(s.dire("atteint", causeGroupe = "volcan")).code)
        assertEquals("cause_sur_etat", refus(s.dire("sain", causeGroupe = "animaux")).code)
        assertEquals("hauteur_sur_absent", refus(s.dire("absent", hauteurCm = 30)).code)
        assertEquals("hauteur_invalide", refus(s.dire("sain", hauteurCm = 0)).code)
        assertEquals("mesure_sur_non_observe", refus(s.dire("non_observe", hauteurCm = 30)).code)
        assertEquals("hors_referentiel", refus(s.pointe(1.0, 20.0, essence = "ZZZ")).code)
        assertEquals(0, journal.deType("etat").size, "un refus n'écrit rien")
    }

    @Test
    fun `hauteur sans etat est refusee, l'etat d'abord, mais acceptee sur un plant mort`() = with(BancSession()) {
        val s = session()
        s.pointe(1.0, 10.0)
        assertEquals(SuiteSession.EtatDAbord, s.preciser(hauteurCm = 40))
        s.dire("mort")
        assertIs<SuiteSession.Fait>(s.preciser(hauteurCm = 40))
    }

    @Test
    fun `choisir un plant le rend courant, un plant inconnu est refuse`() = with(BancSession()) {
        val s = session()
        s.pointe(1.0, 10.0)
        s.pointe(2.0, 100.0)
        val premier = s.etat().plants.first().uuid
        assertTrue(s.choisir(premier))
        assertEquals(premier, s.etat().courant)
        assertFalse(s.choisir(UuidPlant("inconnu")))
        assertEquals(premier, s.etat().courant)
    }

    @Test
    fun `sans plant courant, dire est refuse sans rien ecrire`() = with(BancSession()) {
        val s = session()
        assertEquals(SuiteSession.AucunPlantCourant, s.dire("sain"))
        assertEquals(0, journal.lignes.size)
    }

    // ------------------------------------------------------------ pointage hors rayon

    @Test
    fun `plant hors rayon plus 0,10 m demande confirmation et n'ecrit rien, confirme il est ecrit`() = with(BancSession()) {
        val s = session()
        assertIs<SuiteSession.AConfirmer>(s.pointe(4.20, 10.0))
        assertEquals(0, journal.lignes.size, "ni contrôle ni pointage tant que l'agent n'a pas confirmé")
        assertIs<SuiteSession.Fait>(s.pointe(4.20, 10.0, confirme = true))
        assertTrue(ValeurPointage.lire(journal.deType("pointage").single().valeur)!!.horsRayonConfirme)
        // À la borne (3,99 + 0,10), pas d'alerte.
        assertIs<SuiteSession.Fait>(s.pointe(4.09, 190.0))
    }

    // ------------------------------------------------------------ Terminer, « Contrôlée »

    @Test
    fun `une placette commencee et non terminee n'est pas Controlee, terminee elle l'est`() = with(BancSession()) {
        val s = session()
        s.pointe(1.0, 10.0)
        s.dire("sain")
        assertEquals(emptySet(), controlees(), "commencée, pas terminée : à faire")
        assertIs<SuiteSession.Fait>(s.terminer())
        assertEquals(setOf(BancSession.P1.valeur), controlees())
        assertTrue(s.etat().termine)
    }

    @Test
    fun `terminer fige les numeros, ecrit la fin au journal et declenche une fin de placette de la sauvegarde`() = with(BancSession()) {
        val s = session()
        s.pointe(1.0, 100.0)
        s.pointe(1.5, 200.0)
        s.suivant(); s.suivant()
        s.dire("sain"); s.suivant(); s.dire("mort")
        val avant = ecoute.saisies
        assertIs<SuiteSession.Fait>(s.terminer())
        assertEquals(avant + 1, ecoute.saisies)
        assertEquals(1, ecoute.fins)
        val fin = ValeurControle.lire(journal.deType("controle").last().valeur)!!
        assertTrue(fin.termine)
        assertEquals(setOf(1, 2), fin.numeros!!.values.toSet())
        assertTrue(s.etat().plants.all { it.numeroFige })
    }

    @Test
    fun `terminer sans aucun etat dit est refuse, terminer deux fois aussi`() = with(BancSession()) {
        val s = session()
        assertEquals(SuiteSession.RienASaisir, s.terminer())
        s.pointe(1.0, 10.0)
        assertEquals(SuiteSession.RienASaisir, s.terminer(), "un pointage sans état n'est pas un contrôle")
        s.dire("sain")
        assertIs<SuiteSession.Fait>(s.terminer())
        assertEquals(SuiteSession.DejaTerminee, s.terminer())
        assertEquals(1, ecoute.fins)
    }

    @Test
    fun `terminee hier, la placette est encore Controlee aujourd'hui, et un second controle un autre jour reste permis`() {
        val b = BancSession("2026-10-26T09:00:00Z")
        with(b) {
            val s = session()
            s.pointe(1.0, 10.0)
            s.dire("sain")
            s.terminer()
            horloge.a("2026-10-27T09:00:00Z")
            assertEquals(setOf(BancSession.P1.valeur), controlees(), "toujours Contrôlée le lendemain")
            // Le lendemain, la session ne montre rien de dit : un nouveau contrôle, non réclamé.
            val s2 = session()
            assertEquals(0, s2.etat().nbDits)
            assertEquals(LocalDate.of(2026, 10, 27), s2.etat().jour)
            assertNull(s2.etat().controle)
            s2.dire("mort") // le plant d'hier est le plant courant : le premier sans état aujourd'hui
            assertEquals(2, journal.deType("controle").map { it.objetUuid }.toSet().size, "un contrôle par jour : deux jours, deux contrôles")
            assertEquals(setOf(BancSession.P1.valeur), controlees())
        }
    }

    @Test
    fun `annuler la fin rend la placette a faire`() = with(BancSession()) {
        val s = session()
        s.pointe(1.0, 10.0)
        s.dire("sain")
        s.terminer()
        assertEquals(setOf(BancSession.P1.valeur), controlees())
        assertIs<SuiteSession.Fait>(s.annuler())
        assertEquals(emptySet(), controlees(), "la fin annulée : à faire")
        assertFalse(s.etat().termine)
        // Le numéro redevient provisoire.
        assertFalse(s.etat().plants.single().numeroFige)
    }

    @Test
    fun `une placette terminee d'une autre tournee n'influe pas, une autre placette non plus`() {
        val b = BancSession()
        with(b) {
            val s = session()
            s.pointe(1.0, 10.0); s.dire("sain"); s.terminer()
            assertEquals(setOf(BancSession.P1.valeur), controlees())
            assertFalse(BancSession.P2.valeur in controlees())
        }
    }

    // ------------------------------------------------------------ annuler

    @Test
    fun `annuler defait la derniere saisie puis les precedentes, rien a annuler a la fin`() = with(BancSession()) {
        val s = session()
        s.pointe(1.0, 10.0)
        s.dire("sain")
        s.dire("mort") // correction
        assertIs<SuiteSession.Fait>(s.annuler())
        assertEquals(EtatDsf.SAIN, (s.etat().plants.single().duJour as Observation.Observe).etat, "la valeur remplacée redevient courante")
        assertIs<SuiteSession.Fait>(s.annuler())
        assertNull(s.etat().plants.single().duJour)
        assertIs<SuiteSession.Fait>(s.annuler())
        assertEquals(0, s.etat().plants.size, "le pointage annulé disparaît")
        assertEquals(SuiteSession.RienAAnnuler, s.annuler())
    }

    // ------------------------------------------------------------ reprise (le journal est la seule mémoire)

    @Test
    fun `fermer et rouvrir l'application au milieu d'une placette ne perd rien`() = with(BancSession()) {
        val s = session()
        s.pointe(1.0, 10.0)
        s.pointe(2.0, 200.0)
        s.suivant(); s.suivant()
        s.dire("sain")
        val rouverte = session() // nouvelle instance, même journal
        assertEquals(2, rouverte.etat().nbPlants)
        assertEquals(1, rouverte.etat().nbDits)
        // Le plant courant est le premier sans état.
        assertEquals(2, rouverte.etat().plants.first { it.uuid == rouverte.etat().courant }.numero)
    }

    // ------------------------------------------------------------ numérotation (Groma, provisoire puis figée)

    @Test
    fun `les numeros sont provisoires pendant le premier controle, figes a la fin, puis un plant ajoute prend le numero suivant`() = with(BancSession()) {
        val s = session()
        s.pointe(1.0, 200.0)
        val u200 = s.etat().plants.single().uuid
        assertEquals(1, s.etat().plants.single().numero)
        s.pointe(1.0, 100.0) // un azimut plus petit : il prend le n° 1, le premier passe n° 2 (provisoire)
        assertEquals(2, s.etat().plants.first { it.uuid == u200 }.numero)
        assertFalse(s.etat().plants.any { it.numeroFige })
        s.suivant(); s.suivant()
        s.dire("sain"); s.suivant(); s.dire("sain")
        s.terminer()
        val avant = s.etat().plants.associate { it.uuid to it.numero }
        s.pointe(1.0, 5.0) // azimut plus petit encore, mais après la fin : numéro suivant, sans renuméroter
        val apres = s.etat().plants.associate { it.uuid to it.numero }
        avant.forEach { (u, n) -> assertEquals(n, apres[u], "un numéro figé ne change plus") }
        assertEquals(3, apres.values.max())
        assertEquals(3, s.etat().plants.first { it.uuid == s.etat().courant }.numero)
    }

    // ------------------------------------------------------------ passages suivants : état précédent

    @Test
    fun `un plant d'un passage precedent garde son numero et montre son etat precedent a cote de celui du jour`() {
        val anterieurs = mapOf(
            BancSession.P1 to listOf(
                PlantAnterieur(UuidPlant("ancien-1"), 1, "CHS", null, Observation.Observe(EtatDsf.SAIN)),
                PlantAnterieur(UuidPlant("ancien-2"), 2, "CHS", null, Observation.Observe(EtatDsf.MORT)),
            ),
        )
        with(BancSession(plantsAnterieurs = anterieurs)) {
            val s = session()
            assertEquals(2, s.etat().nbPlants)
            assertEquals(UuidPlant("ancien-1"), s.etat().courant, "on commence par le premier sans état")
            s.dire("atteint")
            val p = s.etat().plants.first { it.uuid == UuidPlant("ancien-1") }
            assertEquals(EtatDsf.SAIN, (p.precedent as Observation.Observe).etat)
            assertEquals(EtatDsf.ATTEINT, (p.duJour as Observation.Observe).etat)
            // Un plant ajouté prend le numéro suivant, sans renuméroter.
            s.pointe(1.0, 5.0)
            assertEquals(3, s.etat().plants.first { it.uuid == s.etat().courant }.numero)
            assertEquals(listOf(1, 2, 3), s.etat().plants.map { it.numero })
        }
    }

    @Test
    fun `l'etat du jour precedent dans le journal devient l'etat precedent le lendemain`() {
        val b = BancSession("2026-10-26T09:00:00Z")
        with(b) {
            val s = session()
            s.pointe(1.0, 10.0); s.dire("mort"); s.terminer()
            horloge.a("2026-10-27T09:00:00Z")
            val s2 = session()
            val p = s2.etat().plants.single()
            assertEquals(EtatDsf.MORT, (p.precedent as Observation.Observe).etat)
            assertNull(p.duJour)
        }
    }

    // ------------------------------------------------------------ regarni

    @Test
    fun `remplace lie le prochain plant pointe au plant courant mort`() = with(BancSession()) {
        val s = session()
        s.pointe(1.0, 10.0)
        val mort = s.etat().courant!!
        s.dire("mort")
        assertIs<SuiteSession.Fait>(s.remplace())
        assertEquals(mort, s.etat().regarniDe)
        assertIs<SuiteSession.Fait>(s.pointe(1.1, 12.0))
        val regarni = s.etat().plants.first { it.uuid == s.etat().courant }
        assertEquals(mort, regarni.remplace)
        assertNull(s.etat().regarniDe, "le regarni en attente est consommé")
        assertEquals(mort.valeur, ValeurPointage.lire(journal.deType("pointage").last().valeur)!!.remplaceUuid)
    }

    @Test
    fun `remplace sur un plant absent est accepte, sur un plant sain ou sans etat refuse`() = with(BancSession()) {
        val s = session()
        s.pointe(1.0, 10.0)
        assertEquals("plant_remplace_ni_mort_ni_absent", refus(s.remplace()).code, "sans état : ni mort ni absent")
        s.dire("sain")
        assertEquals("plant_remplace_ni_mort_ni_absent", refus(s.remplace()).code)
        s.dire("absent")
        assertIs<SuiteSession.Fait>(s.remplace())
    }

    @Test
    fun `un plant ne peut etre remplace deux fois`() = with(BancSession()) {
        val s = session()
        s.pointe(1.0, 10.0)
        val mort = s.etat().courant!!
        s.dire("mort")
        s.remplace()
        s.pointe(1.1, 12.0)
        // On revient sur le plant mort et on demande un second regarni.
        assertTrue(s.choisir(mort))
        assertEquals("plant_deja_remplace", refus(s.remplace()).code)
    }

    @Test
    fun `le plant remplace avant aujourd'hui sort des totaux d'aujourd'hui`() {
        val b = BancSession("2026-10-26T09:00:00Z")
        with(b) {
            val s = session()
            s.pointe(1.0, 10.0); val mort = s.etat().courant!!; s.dire("mort")
            s.remplace(); s.pointe(1.1, 12.0); s.dire("sain")
            s.terminer()
            horloge.a("2026-10-27T09:00:00Z")
            val s2 = session()
            assertEquals(1, s2.etat().nbPlants, "le plant remplacé hier n'est plus compté")
            assertTrue(s2.etat().plants.first { it.uuid == mort }.remplaceAvant)
            s2.dire("sain") // le regarni est le premier plant sans état aujourd'hui
            assertEquals(1, (s2.etat().totaux as Verdict.Accepte).valeur.nbTotal)
        }
    }

    // ------------------------------------------------------------ vide

    @Test
    fun `vide et fin du vide sont pas encore disponibles, rien n'est trace, aucun code invente`() = with(BancSession()) {
        val s = session()
        assertEquals(SuiteSession.PasEncoreDisponible("vide"), s.vide())
        assertEquals(SuiteSession.PasEncoreDisponible("fin du vide"), s.finDuVide())
        assertEquals(0, journal.lignes.size)
    }

    // ------------------------------------------------------------ arrivée : France et 15 m

    @Test
    fun `une placette francaise tombe en France, la placette en Somalie ne pointe rien`() = with(BancSession()) {
        // Brest, (latitude, longitude) en degrés : accepté, mais loin de tout piquet de la tournée (hors 15 m).
        val brest = PointGeographique.de(latitudeDeg = 48.39, longitudeDeg = -4.49).valeurOuEchec()
        val enFrance = SessionControle.ouvrir(contexte, journal, horloge, essences, Arrivee.Gnss(brest))
        assertEquals("hors_15_m", assertIs<OuvertureControle.Refusee>(enFrance).refus.code, "en France, mais loin des piquets")
        // La placette en Somalie : (latitude 5,15, longitude 46,2).
        val somalie = PointGeographique.de(latitudeDeg = 5.15, longitudeDeg = 46.2).valeurOuEchec()
        val r = assertIs<OuvertureControle.Refusee>(SessionControle.ouvrir(contexte, journal, horloge, essences, Arrivee.Gnss(somalie)))
        assertEquals("hors_france", r.refus.code)
        // (5,15 N ; 46,2 E) est la position française (46,2 N ; 5,15 E) aux axes inversés : on le dit à l'agent.
        assertTrue(r.refus.axesInverses)
        assertEquals(0, journal.lignes.size, "un fix hors de France ne pointe rien")
    }

    @Test
    fun `latitude et longitude inversees sont detectees`() = with(BancSession()) {
        val inverse = PointGeographique.de(latitudeDeg = 4.49, longitudeDeg = 48.39).valeurOuEchec() // (lat, lon) de Brest permutés
        val r = assertIs<OuvertureControle.Refusee>(SessionControle.ouvrir(contexte, journal, horloge, essences, Arrivee.Gnss(inverse)))
        assertEquals("hors_france", r.refus.code)
        assertTrue(r.refus.axesInverses, "(4,49 N ; 48,39 E) tombe en France une fois permuté (48,39 N ; 4,49 E)")
    }

    @Test
    fun `un point Lambert-93 aux axes inverses ne tombe pas en France`() = with(BancSession()) {
        val r = assertIs<OuvertureControle.Refusee>(ouvrir(PointLambert93(6_686_123.26, 846_164.76)))
        assertEquals("hors_france", r.refus.code)
        assertTrue(r.refus.axesInverses)
    }

    @Test
    fun `a 14,9 m du piquet la placette est rattachee, a 15,1 m refusee, avec le code du piquet elle l'est par identite`() = with(BancSession()) {
        val a14 = PointLambert93(BancSession.POSITION_P1.xMetres, BancSession.POSITION_P1.yMetres + 14.9)
        assertIs<OuvertureControle.Ouverte>(ouvrir(a14))
        val a15 = PointLambert93(BancSession.POSITION_P1.xMetres, BancSession.POSITION_P1.yMetres + 15.1)
        assertEquals("hors_15_m", assertIs<OuvertureControle.Refusee>(ouvrir(a15)).refus.code)
        assertEquals("code_inconnu", assertIs<OuvertureControle.Refusee>(ouvrir(a15, codeLu = "XXX")).refus.code)
        val s = (ouvrir(a15, codeLu = "P35-01") as OuvertureControle.Ouverte).session
        s.pointe(1.0, 10.0)
        val c = ValeurControle.lire(journal.deType("controle").single().valeur)!!
        assertTrue(c.rattacheParIdentite)
        assertEquals(BancSession.POSITION_P1.yMetres, c.positionY, "le contrôle prend la position du piquet")
        assertEquals(BancSession.POSITION_P1.yMetres + 15.1, c.positionMesureeY, 1e-9, "la position mesurée est gardée à part")
    }

    @Test
    fun `hors de l'emprise de la foret est refuse avec son code`() {
        val b = BancSession()
        val ctx = b.contexte.copy(
            empriseForet = io.github.pobsteta.surculus.core.geo.EmpriseLambert93(0.0, 0.0, 100_000.0, 100_000.0),
        )
        val r = SessionControle.ouvrir(ctx, b.journal, b.horloge, b.essences, Arrivee.Lambert93(BancSession.POSITION_P1))
        assertEquals("point_hors_foret", assertIs<OuvertureControle.Refusee>(r).refus.code)
    }

    // ------------------------------------------------------------ date : jour civil GNSS, sinon téléphone

    private fun BancSession.jourEcrit() = ValeurControle.lire(journal.deType("controle").first().valeur)!!

    @Test
    fun `telephone dereglé, un fix GNSS frais donne le bon jour civil`() = with(BancSession("2025-01-01T10:00:00Z")) {
        // L'heure GNSS dit le 27 octobre 2026 ; le téléphone, déréglé, le 1er janvier 2025. Fix reçu 2 s plus tôt.
        horloge.monotoneMs = 5_000_000
        horloge.fixCourant = FixSession(heureUtcMs = java.time.Instant.parse("2026-10-27T09:00:00Z").toEpochMilli(), monotoneMs = 4_998_000)
        val s = session()
        s.pointe(1.0, 10.0)
        assertEquals("2026-10-27", jourEcrit().jourCivil)
        assertEquals(java.time.Instant.parse("2026-10-27T09:00:00Z").toEpochMilli(), jourEcrit().visiteLeUtcMs, "l'instant ne contredit pas le jour")
    }

    @Test
    fun `fix trop vieux ou posterieur a la saisie, on prend l'heure du telephone`() {
        for ((age, attendu) in listOf(5_001L to "2026-10-27", -1L to "2026-10-27", 5_000L to "2026-10-15")) {
            with(BancSession("2026-10-27T09:00:00Z")) {
                horloge.monotoneMs = 5_000_000
                // Le fix dit le 15 octobre (valeur d'essai) : il n'est retenu qu'avec un âge de 0 à 5 000 ms inclus.
                horloge.fixCourant = FixSession(java.time.Instant.parse("2026-10-15T09:00:00Z").toEpochMilli(), 5_000_000 - age)
                session().pointe(1.0, 10.0)
                assertEquals(attendu, jourEcrit().jourCivil, "âge du fix : $age ms")
            }
        }
    }

    @Test
    fun `a minuit, le jour civil est celui de Paris, pas celui d'UTC`() = with(BancSession("2026-10-27T23:30:00Z")) {
        // 23 h 30 UTC = 0 h 30 le 28 octobre à Paris (heure d'hiver, UTC+1 depuis le 25 octobre).
        session().pointe(1.0, 10.0)
        assertEquals("2026-10-28", jourEcrit().jourCivil)
    }

    @Test
    fun `deux saisies le meme jour civil de Paris corrigent le meme controle, le lendemain en ouvre un autre`() = with(BancSession("2026-10-27T08:00:00Z")) {
        val s = session()
        s.pointe(1.0, 10.0)
        horloge.a("2026-10-27T14:00:00Z")
        s.pointe(1.5, 100.0)
        assertEquals(1, journal.deType("controle").size)
        horloge.a("2026-10-28T14:00:00Z")
        s.pointe(2.0, 200.0)
        assertEquals(2, journal.deType("controle").size)
    }

    @Test
    fun `une date hors de la validite du WMM2025 est refusee et n'ecrit rien`() = with(BancSession("2031-03-01T09:00:00Z")) {
        val r = refus(session().pointe(1.0, 10.0))
        assertEquals("date_hors_validite", r.code)
        assertEquals(0, journal.lignes.size)
    }

    // ------------------------------------------------------------ sauvegarde

    @Test
    fun `chaque ligne ecrite est une saisie pour la sauvegarde, une annulation aussi`() = with(BancSession()) {
        val s = session()
        s.pointe(1.0, 10.0) // contrôle + pointage = 2 lignes
        s.dire("sain") // 1 ligne
        s.annuler() // 1 annulation
        assertEquals(journal.lignes.size, ecoute.saisies)
        assertEquals(0, ecoute.fins)
    }

    @Test
    fun `le contexte pose l'operateur de la tournee sur chaque ligne`() = with(BancSession()) {
        val s = session()
        s.pointe(1.0, 10.0); s.dire("sain"); s.terminer(); s.annuler()
        assertTrue(journal.lignes.all { it.operateur == "Agent d'essai" })
        assertNotNull(journal.lignes.firstOrNull { it.type == "annulation" })
    }
}
