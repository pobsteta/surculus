package io.github.pobsteta.surculus.controle

import io.github.pobsteta.surculus.core.geo.UniteAzimut
import io.github.pobsteta.surculus.core.placette.EtatDsf
import io.github.pobsteta.surculus.core.placette.CodesRefus
import io.github.pobsteta.surculus.core.session.FormatValeurs
import io.github.pobsteta.surculus.core.session.ValeurPointage
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Mission 012-B2 : pointer un plant (boussole du téléphone ou clavier), hors rayon, regarni, sur la session de la 012-A. */
class PointageControleTest {
    private fun BancControle.pointages(): List<ValeurPointage> =
        journal.lignesCourantes().filter { it.type == FormatValeurs.TYPE_POINTAGE }.mapNotNull { ValeurPointage.lire(it.valeur) }

    private fun cap(d: Double) = assertNotNull(CapMagnetique.de(d))

    // ------------------------------------------------------------------ cap magnétique pur, déclinaison une seule fois

    @Test fun `un cap de 100 degres magnetiques est ecrit tel quel, la declinaison n'est ajoutee qu'une fois par la session`() = with(BancControle()) {
        val c = controleur()
        val e = c.pointerAvecBoussole("3,2", cap(100.0))
        assertNull(e.message?.takeIf { it.erreur })
        val v = pointages().single()
        assertEquals(100.0, v.azimutMag, 0.0)
        assertEquals("deg", v.uniteAzimut)
        assertEquals("boussole_telephone", v.sourceAzimut)
        assertEquals(3.2, v.distanceHorizontaleM, 1e-9)
        // azimut_geo − azimut_mag = déclinaison, exactement : ni zéro, ni le double.
        assertTrue(abs(v.declinaisonDeg) > 0.1, "la déclinaison de Couchey n'est pas nulle : ${v.declinaisonDeg}")
        assertEquals((100.0 + v.declinaisonDeg + 360.0) % 360.0, v.azimutGeoDeg, 1e-9)
        assertEquals(v.declinaisonDeg, v.azimutGeoDeg - v.azimutMag, 1e-9)
    }

    @Test fun `sans cap, la boussole ne pointe rien et le dit`() = with(BancControle()) {
        val e = controleur().pointerAvecBoussole("3,2", null)
        assertEquals(VueControle.POINTER, e.vue)
        assertEquals(TextesControle.SANS_CAP, e.message?.texte)
        assertTrue(pointages().isEmpty())
    }

    @Test fun `le pointage reussi revient au plant, qui devient le plant courant pret a recevoir son etat`() = with(BancControle(nbPlantsAnterieurs = 0)) {
        val c = controleur()
        c.ouvrirPointer()
        val e = c.pointerAvecBoussole("3,2", cap(143.0))
        assertEquals(VueControle.PLANT, e.vue)
        assertEquals("Plant n° 1 pointé : 143°, 3,2 m", e.message?.texte)
        assertEquals(1, e.plant.numero)
        assertEquals(TextesControle.PAS_ENCORE_DIT, e.plant.libelleEtat)
        assertEquals(1, e.plan.plants.size)
    }

    // ------------------------------------------------------------------ clavier

    @Test fun `au clavier en degres, la source est saisie et l'unite deg`() = with(BancControle()) {
        controleur().pointerAuClavier("3", "143", UniteAzimut.DEGRES)
        val v = pointages().single()
        assertEquals(143.0, v.azimutMag, 0.0)
        assertEquals("deg", v.uniteAzimut)
        assertEquals("saisie", v.sourceAzimut)
    }

    @Test fun `au clavier en grades, la valeur est gardee en grades et convertie en degres par la session`() = with(BancControle()) {
        controleur().pointerAuClavier("3", "100", UniteAzimut.GRADES)
        val v = pointages().single()
        assertEquals(100.0, v.azimutMag, 0.0)
        assertEquals("gr", v.uniteAzimut)
        // 100 gr = 90° : le géographique est 90° plus la déclinaison, une seule fois.
        assertEquals(90.0 + v.declinaisonDeg, v.azimutGeoDeg, 1e-9)
    }

    @Test fun `un azimut tape hors de 0 a 360 degres ou de 0 a 400 grades est refuse avec son code, rien n'est ecrit`() = with(BancControle()) {
        val c = controleur()
        listOf("360" to UniteAzimut.DEGRES, "400" to UniteAzimut.GRADES, "-1" to UniteAzimut.DEGRES, "361,5" to UniteAzimut.DEGRES).forEach { (texte, unite) ->
            val m = assertNotNull(c.pointerAuClavier("3", texte, unite).message, "$texte ${unite.name}")
            assertTrue(m.erreur)
            assertTrue(m.texte.startsWith("azimut_hors_plage : "), m.texte)
        }
        assertTrue(pointages().isEmpty())
        assertEquals(0, journal.nombreDeLignes)
    }

    @Test fun `les bornes incluses - 0 et 359,9 degres, 0 et 399,9 grades - sont acceptees`() {
        listOf("0" to UniteAzimut.DEGRES, "359,9" to UniteAzimut.DEGRES, "0" to UniteAzimut.GRADES, "399,9" to UniteAzimut.GRADES).forEach { (texte, unite) ->
            with(BancControle()) {
                val e = controleur().pointerAuClavier("3", texte, unite)
                assertEquals(VueControle.PLANT, e.vue, "$texte ${unite.name}")
                assertEquals(1, pointages().size)
            }
        }
    }

    @Test fun `un cap ou une distance illisible est dit en clair, sans code, rien n'est ecrit`() = with(BancControle()) {
        val c = controleur()
        assertEquals(SaisiePointage.AZIMUT_ILLISIBLE, c.pointerAuClavier("3", "abc", UniteAzimut.DEGRES).message?.texte)
        assertEquals(SaisiePointage.DISTANCE_ILLISIBLE, c.pointerAuClavier("", "90", UniteAzimut.DEGRES).message?.texte)
        assertEquals(SaisiePointage.DISTANCE_ILLISIBLE, c.pointerAvecBoussole("0", cap(10.0)).message?.texte)
        assertEquals(SaisiePointage.DISTANCE_ILLISIBLE, c.pointerAvecBoussole("-2", cap(10.0)).message?.texte)
        assertTrue(pointages().isEmpty())
    }

    @Test fun `la virgule et le point sont deux ecritures de la meme distance`() {
        assertEquals(6.2, SaisiePointage.nombre("6,2"))
        assertEquals(6.2, SaisiePointage.nombre(" 6.2 "))
        assertNull(SaisiePointage.nombre("6,2,1"))
    }

    // ------------------------------------------------------------------ unité mémorisée par tournée

    private class Memoire : io.github.pobsteta.surculus.tournee.MagasinAffichage {
        val textes = mutableMapOf<String, String>()
        val booleens = mutableMapOf<String, Boolean>()
        override fun lireBooleen(cle: String, parDefaut: Boolean) = booleens[cle] ?: parDefaut
        override fun ecrireBooleen(cle: String, valeur: Boolean) { booleens[cle] = valeur }
        override fun lireTexte(cle: String) = textes[cle]
        override fun ecrireTexte(cle: String, valeur: String) { textes[cle] = valeur }
    }

    @Test fun `l'unite de l'azimut tape est memorisee par tournee, degres par defaut`() {
        val magasin = Memoire()
        val r = ReglagesControle(magasin)
        assertEquals(UniteAzimut.DEGRES, r.uniteAzimut("t1"))
        r.choisirUniteAzimut("t1", UniteAzimut.GRADES)
        assertEquals(UniteAzimut.GRADES, r.uniteAzimut("t1"))
        assertEquals(UniteAzimut.DEGRES, r.uniteAzimut("t2"), "une autre tournée garde ses degrés")
        assertEquals(UniteAzimut.GRADES, ReglagesControle(magasin).uniteAzimut("t1"), "relue par un autre lecteur du même magasin")
    }

    @Test fun `une unite illisible dans le magasin retombe sur les degres`() {
        val magasin = Memoire().also { it.textes["unite_azimut/t1"] = "toise" }
        assertEquals(UniteAzimut.DEGRES, ReglagesControle(magasin).uniteAzimut("t1"))
    }

    @Test fun `la permission de stockage une fois demandee est retenue`() {
        val magasin = Memoire()
        assertFalse(ReglagesControle(magasin).stockageDejaDemande)
        ReglagesControle(magasin).stockageDejaDemande = true
        assertTrue(ReglagesControle(magasin).stockageDejaDemande)
    }

    // ------------------------------------------------------------------ hors rayon : une confirmation, pas un refus

    @Test fun `un plant hors rayon demande confirmation, rien n'est ecrit, ce n'est pas un refus`() = with(BancControle()) {
        val c = controleur()
        val e = c.pointerAvecBoussole("9", cap(90.0))
        assertEquals(VueControle.POINTER, e.vue)
        assertNotNull(e.pointage.aConfirmer)
        assertNull(e.message, "pas de refus ni de code : une question")
        assertTrue(pointages().isEmpty())
        assertEquals(0, journal.nombreDeLignes)
    }

    @Test fun `confirmer ecrit le plant, marque hors rayon confirme`() = with(BancControle()) {
        val c = controleur()
        c.pointerAvecBoussole("9", cap(90.0))
        val e = c.confirmerHorsRayon()
        assertEquals(VueControle.PLANT, e.vue)
        assertNull(e.pointage.aConfirmer)
        val v = pointages().single()
        assertTrue(v.horsRayonConfirme)
        assertEquals(9.0, v.distanceHorizontaleM, 1e-9)
    }

    @Test fun `refaire efface la question, rien n'est ecrit`() = with(BancControle()) {
        val c = controleur()
        c.pointerAvecBoussole("9", cap(90.0))
        val e = c.refaire()
        assertNull(e.pointage.aConfirmer)
        assertTrue(pointages().isEmpty())
        // Un pointage dans le rayon, ensuite, passe sans confirmation.
        assertEquals(VueControle.PLANT, c.pointerAvecBoussole("3", cap(90.0)).vue)
        assertFalse(pointages().single().horsRayonConfirme)
    }

    @Test fun `un plant dans le rayon n'est pas marque hors rayon`() = with(BancControle()) {
        controleur().pointerAvecBoussole("3,5", cap(10.0))
        assertFalse(pointages().single().horsRayonConfirme)
    }

    // ------------------------------------------------------------------ regarnis

    @Test fun `le regarni d'un plant mort est lie par remplace_uuid et garde son essence`() = with(BancControle(essenceAncien = "CHS", essencesConnues = setOf("CHS"))) {
        val c = controleur()
        c.dire(EtatDsf.MORT)
        assertTrue(c.instantane().plant.regarniPossible)
        val e = c.regarni()
        assertEquals(VueControle.POINTER, e.vue)
        assertEquals(1, e.pointage.regarniDe)
        c.pointerAvecBoussole("3", cap(45.0))
        val v = pointages().single()
        assertEquals("ancien-1", v.remplaceUuid)
        assertEquals("CHS", v.essence)
        assertNull(c.instantane().pointage.regarniDe, "le regarni n'est plus en attente")
    }

    @Test fun `le regarni d'un plant absent est permis`() = with(BancControle()) {
        val c = controleur()
        c.dire(EtatDsf.ABSENT)
        c.plantPrecedent()
        assertTrue(c.instantane().plant.regarniPossible)
        assertEquals(VueControle.POINTER, c.regarni().vue)
    }

    @Test fun `le regarni d'un plant sain est refuse par la session avec son code`() = with(BancControle()) {
        val c = controleur()
        c.dire(EtatDsf.SAIN)
        c.plantPrecedent()
        assertFalse(c.instantane().plant.regarniPossible)
        val e = c.regarni()
        assertEquals(VueControle.PLANT, e.vue)
        val m = assertNotNull(e.message)
        assertTrue(m.erreur)
        assertTrue(m.texte.contains(" : "), m.texte)
        assertNull(e.pointage.regarniDe)
    }

    @Test fun `un plant sans etat dit n'est pas remplacable`() = with(BancControle()) {
        val c = controleur()
        assertFalse(c.instantane().plant.regarniPossible)
        assertTrue(assertNotNull(c.regarni().message).erreur)
    }

    @Test fun `une essence etrangere a la tournee est refusee hors_referentiel`() = with(BancControle(essenceAncien = "ZZZ", essencesConnues = setOf("CHS"))) {
        val c = controleur()
        c.dire(EtatDsf.MORT)
        c.regarni()
        val m = assertNotNull(c.pointerAvecBoussole("3", cap(45.0)).message)
        assertTrue(m.texte.startsWith("hors_referentiel : "), m.texte)
        assertTrue(pointages().isEmpty())
    }

    @Test fun `Annuler defait le pointage`() = with(BancControle(nbPlantsAnterieurs = 0)) {
        val c = controleur()
        c.pointerAvecBoussole("3", cap(10.0))
        assertEquals(1, c.instantane().plan.plants.size)
        c.annuler()
        assertEquals(0, c.instantane().plan.plants.size)
    }

    // ------------------------------------------------------------------ le plan

    @Test fun `un plant a 2 m et 90 degres est a l'est du piquet`() = with(BancControle(nbPlantsAnterieurs = 0)) {
        val c = controleur()
        c.pointerAuClavier("2", "90", UniteAzimut.DEGRES)
        val p = c.instantane().plan
        val plant = p.plants.single()
        // 90° magnétiques plus la déclinaison (de l'ordre du degré) : l'est, à quelques centimètres de nord près.
        assertTrue(plant.estM > 1.9, "est ${plant.estM}")
        assertTrue(abs(plant.nordM) < 0.3, "nord ${plant.nordM}")
        assertEquals(2.0, Math.hypot(plant.estM, plant.nordM), 0.05)
    }

    @Test fun `nord en haut, le piquet au centre, meme echelle sur les deux axes`() {
        val plan = PlanPlants(emptyList(), rayonM = 4.0, nonDessines = 0)
        val centre = plan.versPixels(0.0, 0.0, 400f)
        assertEquals(200f, centre.x, 0.01f)
        assertEquals(200f, centre.y, 0.01f)
        val nord = plan.versPixels(0.0, 2.0, 400f)
        val est = plan.versPixels(2.0, 0.0, 400f)
        assertTrue(nord.y < centre.y, "le nord est en haut")
        assertEquals(centre.x, nord.x, 0.01f)
        assertTrue(est.x > centre.x)
        assertEquals(centre.y - nord.y, est.x - centre.x, 0.01f, "même échelle")
        // Le cercle de la placette tient dans le carré, avec un peu d'air.
        assertTrue(plan.echelle(400f) * 4f < 200f)
    }

    @Test fun `un plant confirme hors rayon reste dans le cadre du plan`() = with(BancControle(nbPlantsAnterieurs = 0)) {
        val c = controleur()
        c.pointerAvecBoussole("9", cap(0.0))
        c.confirmerHorsRayon()
        val p = c.instantane().plan
        assertTrue(p.etendueM > 9.0)
        val px = p.versPixels(p.plants.single(), 300f)
        assertTrue(px.x in 0f..300f && px.y in 0f..300f, "$px")
    }

    @Test fun `toucher un plant du plan le choisit, le vide ne choisit rien`() = with(BancControle(nbPlantsAnterieurs = 0)) {
        val c = controleur()
        c.pointerAuClavier("2", "90", UniteAzimut.DEGRES)
        c.pointerAuClavier("2", "270", UniteAzimut.DEGRES)
        val plan = c.instantane().plan
        val premier = plan.plants.first { it.numero == 1 }
        val px = plan.versPixels(premier, 300f)
        assertEquals(premier.uuid, plan.plantTouche(px.x + 3f, px.y - 3f, 300f, tolerancePx = 24f)?.uuid)
        assertNull(plan.plantTouche(5f, 5f, 300f, tolerancePx = 24f))
        val e = c.choisir(premier.uuid)
        assertEquals(1, e.plant.numero)
        assertTrue(e.plan.plants.first { it.numero == 1 }.courant)
    }

    @Test fun `les plants du passage precedent ne sont pas dessines, la mention les compte`() = with(BancControle()) {
        val c = controleur()
        val plan = c.instantane().plan
        assertTrue(plan.plants.isEmpty())
        assertEquals(12, plan.nonDessines)
        c.pointerAvecBoussole("3", cap(10.0))
        val apres = c.instantane().plan
        assertEquals(1, apres.plants.size)
        assertEquals(12, apres.nonDessines)
    }

    @Test fun `l'etat dit se voit sur le plan`() = with(BancControle(nbPlantsAnterieurs = 0)) {
        val c = controleur()
        c.pointerAvecBoussole("3", cap(10.0))
        c.dire(EtatDsf.MORT)
        assertEquals(EtatDsf.MORT, c.instantane().plan.plants.single().etat)
    }

    @Test fun `un refus de rayon du code porte son code de la spec`() {
        // Garde-fou : le code du refus d'azimut est bien celui de CodesRefus, pas un texte inventé ici.
        val refus = io.github.pobsteta.surculus.core.reperes.RefusRepere.AzimutHorsPlage(360.0, UniteAzimut.DEGRES)
        assertEquals("azimut_hors_plage", CodesRefus.code(refus))
    }
}
