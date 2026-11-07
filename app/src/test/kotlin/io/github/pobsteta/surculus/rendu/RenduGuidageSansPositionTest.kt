package io.github.pobsteta.surculus.rendu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.github.pobsteta.surculus.carte.GuidagePret
import io.github.pobsteta.surculus.controle.ArriveeControle
import io.github.pobsteta.surculus.controle.ArriveeSansPosition
import io.github.pobsteta.surculus.controle.BancControle
import io.github.pobsteta.surculus.controle.ControleursOuverts
import io.github.pobsteta.surculus.controle.EcouteComptee
import io.github.pobsteta.surculus.controle.EtatArrivee
import io.github.pobsteta.surculus.controle.HorlogeDeTest
import io.github.pobsteta.surculus.controle.JournalEnMemoire
import io.github.pobsteta.surculus.controle.SansPositionEnAttente
import io.github.pobsteta.surculus.controle.TextesControle
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.geo.Projections
import io.github.pobsteta.surculus.core.gnss.FixGnss
import io.github.pobsteta.surculus.core.gnss.QualiteFix
import io.github.pobsteta.surculus.core.guidage.Guidage
import io.github.pobsteta.surculus.core.guidage.Libelles
import io.github.pobsteta.surculus.core.placette.Essences
import io.github.pobsteta.surculus.core.placette.EtatDsf
import io.github.pobsteta.surculus.core.placette.Piquet
import io.github.pobsteta.surculus.core.placette.UuidPlacette
import io.github.pobsteta.surculus.core.reperes.CodeEpsg
import io.github.pobsteta.surculus.core.session.ContexteControle
import io.github.pobsteta.surculus.core.session.PlacetteControlable
import io.github.pobsteta.surculus.core.session.ValeurControle
import io.github.pobsteta.surculus.gpkg.PlacetteTournee
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Mission 089 : « Ouvrir sans position » depuis le guidage, à l'écran (décisions de Pascal du 2026-11-07 : le bloc à la place
 * du radar, le champ du code dès l'absence de fix, le bouton après 30 s, le compte depuis l'ouverture du guidage, remis à zéro
 * par un fix). Écrit par le `dev`, en complément de `GuidageSansPositionTest` (le `testeur` n'a pu lire que la source).
 *
 * Le guidage est rendu tournée chargée (`GuidagePret`) ; le fix, l'horloge monotone, ce qui écoute et l'ouverture sans
 * position (la règle de la 087, sur le banc du contrôle) sont injectés.
 *
 * Coordonnées : Lambert-93 (EPSG:2154), x = Est, y = Nord, mètres ; les fix en (latitude, longitude), degrés WGS 84. P35-01
 * est à Couchey (Côte-d'Or), en France (vérifié par le premier cas).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = BancRendu.DEBOUT_PLEIN)
class RenduGuidageSansPositionTest {
    val regle = BancRendu.nouvelleRegle()

    @get:Rule val chaine: org.junit.rules.RuleChain = org.junit.rules.RuleChain.outerRule(BancRendu.surveillant()).around(regle)

    private val cle = ControleursOuverts.cle("tournee-essai", BancControle.P1.valeur)
    private val t0 = 1_000_000L
    private val sec = 1_000L

    private val p1 = PlacetteTournee(BancControle.P1.valeur, "P35-01", null, 3.99, null, null, BancControle.POSITION_P1)
    private val p2 = PlacetteTournee(BancControle.P2.valeur, "P35-02", null, 3.99, null, null, BancControle.POSITION_P2)

    /** L'écran et ce qu'on lui injecte. */
    private inner class EcranEssai(
        val contexte: ContexteControle = BancControle().contexte,
        var localisationActive: Boolean = true,
        var permissionAccordee: Boolean = true,
    ) {
        val journal = JournalEnMemoire()
        val sauvegarde = EcouteComptee()
        val maintenant: MutableLongState = mutableLongStateOf(t0)
        val fix: MutableState<FixGnss?> = mutableStateOf(null)
        val ouvertures = ArrayList<String?>()

        val contenu: @Composable () -> Unit = {
            GuidagePret(
                cible = p1, toutes = listOf(p1, p2), fix = fix.value, arriveeVue = false,
                onArrivee = { ouvertures += it }, onRearme = {}, cle = cle,
                ouvrirSansPosition = { code, ug, confirmee ->
                    ArriveeControle.ouvrirSansPosition(contexte, journal, HorlogeDeTest(), Essences(emptySet()), code, sauvegarde, ug, confirmee)
                },
                suiteSansFix = { ms, recu -> ArriveeSansPosition.suite(ms, if (recu) 1 else 0, localisationActive, permissionAccordee) },
                maintenant = maintenant,
            )
        }

        fun rendre(): BancRendu.Resultat = BancRendu.rendre(contenu, regle = regle)

        fun aLaSeconde(s: Long) {
            regle.runOnIdle { maintenant.longValue = t0 + s * sec }
            regle.waitForIdle()
        }

        fun poserFix(f: FixGnss?) {
            regle.runOnIdle { fix.value = f }
            regle.waitForIdle()
        }

        fun saisir(code: String) {
            regle.onNode(hasSetTextAction()).performTextInput(code)
            regle.waitForIdle()
        }

        fun appuyer(texte: String) {
            regle.onAllNodes(hasText(texte) and hasClickAction()).onFirst().performClick()
            regle.waitForIdle()
        }
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteractionCollection.onFirst() = this[0]

    private fun present(texte: String) = regle.onAllNodes(hasText(texte)).fetchSemanticsNodes().isNotEmpty()

    private fun boutonPresent(texte: String) = regle.onAllNodes(hasText(texte) and hasClickAction()).fetchSemanticsNodes().isNotEmpty()

    private fun champPresent() = regle.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()

    /** Un fix du téléphone sur un point Lambert-93, (latitude, longitude) en degrés WGS 84. */
    private fun fixSur(p: PointLambert93) = FixGnss(
        position = BancControle.fixSur(p), qualite = QualiteFix.AUTONOME, nbSatellites = 12, hdop = 0.8, altitudeM = 300.0,
        ageCorrectionsS = null, precisionHorizontaleM = 3.0, horodatageMs = 0L,
    )

    private fun aDistance(nordM: Double) = PointLambert93(BancControle.POSITION_P1.xMetres, BancControle.POSITION_P1.yMetres + nordM)

    /** La distance affichée pour un fix à 200 m au nord de P35-01, telle que le guidage la calcule. */
    private val distanceA200 get() = Libelles.distance(Guidage.cap(fixSur(aDistance(200.0)).position, BancControle.POSITION_P1)!!.distanceM)

    @After fun remettre() {
        BancRendu.fermer()
        ControleursOuverts.liberer(cle)
        SansPositionEnAttente.liberer(cle)
    }

    // ------------------------------------------------------------------ 1. avant 30 s

    @Test
    fun `1 - sans fix, a 29 s, le champ et le decompte sont la, pas Ouvrir sans position, ni radar`() {
        // La placette tombe en France (la placette en Somalie) : Lambert-93 → (longitude, latitude) WGS 84.
        val (lon, lat) = Projections.versWgs84(BancControle.POSITION_P1.xMetres, BancControle.POSITION_P1.yMetres)
        assertTrue(lat in 41.0..51.5 && lon in -5.5..10.0, "P35-01 en ($lat, $lon) : hors de France")
        val g = EcranEssai()
        g.rendre()
        assertTrue(champPresent(), "le champ du code dès l'absence de fix (décision 2)")
        assertTrue(present(TextesControle.attenteSansFix(0)))
        g.aLaSeconde(29)
        assertTrue(present(TextesControle.attenteSansFix(29)), "le compte part de l'ouverture du guidage (décision 3)")
        assertTrue(champPresent())
        assertTrue(!boutonPresent(TextesControle.OUVRIR_SANS_POSITION), "pas d'ouverture avant 30 s")
        assertTrue(!present("Nord en haut"), "le radar laisse sa place au bloc (décision 1)")
        assertEquals(emptyList(), g.ouvertures)
    }

    // ------------------------------------------------------------------ 2. 30 s, champ vide

    @Test
    fun `2 - a 30 s, champ vide - Ouvrir sans position grise, code requis dessous, rien d'ecrit`() {
        val g = EcranEssai()
        g.rendre()
        g.aLaSeconde(30)
        assertTrue(present(TextesControle.SANS_FIX_PROPOSE))
        regle.onAllNodes(hasText(TextesControle.OUVRIR_SANS_POSITION) and hasClickAction()).onFirst().assertIsNotEnabled()
        assertTrue(present(TextesControle.CODE_REQUIS))
        assertEquals(0, g.journal.nombreDeLignes)
    }

    // ------------------------------------------------------------------ 3. code de la tournée : confirmation, puis ouverture

    @Test
    fun `3 - code P35-01 - la placette a confirmer, puis le controle ouvert sans position mesuree, repris par l'ecran de controle`() {
        val g = EcranEssai()
        g.rendre()
        g.aLaSeconde(31)
        g.saisir("P35-01")
        regle.onAllNodes(hasText(TextesControle.OUVRIR_SANS_POSITION) and hasClickAction()).onFirst().assertIsEnabled()
        assertTrue(!present(TextesControle.CODE_REQUIS))
        g.appuyer(TextesControle.OUVRIR_SANS_POSITION)
        val attendu = ArriveeControle.ouvrirSansPosition(BancControle().contexte, JournalEnMemoire(), HorlogeDeTest(), Essences(emptySet()), "P35-01", EcouteComptee())
        val texte = TextesControle.confirmationSansPosition(assertIs<EtatArrivee.ConfirmerSansPosition>(attendu).piquet, attendu.plantsPointes)
        assertTrue(present(texte), "la placette à confirmer : « $texte »")
        assertEquals(0, g.journal.nombreDeLignes, "confirmer n'écrit rien")
        assertEquals(emptyList(), g.ouvertures, "rien n'est ouvert avant la confirmation")
        g.appuyer(TextesControle.OUVRIR_SANS_POSITION)
        assertEquals(listOf<String?>("P35-01"), g.ouvertures)
        val c = assertNotNull(ControleursOuverts.get(cle), "le contrôle ouvert est confié à l'écran de contrôle")
        c.dire(EtatDsf.SAIN)
        val v = ValeurControle.lire(g.journal.lignesCourantes().single { it.type == "controle" }.valeur)!!
        assertTrue(v.sansPositionMesuree)
        assertNull(v.positionMesureeX)
        assertEquals(BancControle.POSITION_P1.xMetres, v.positionX, 0.01, "position du contrôle = piquet (x = Est, m)")
        assertEquals(BancControle.POSITION_P1.yMetres, v.positionY, 0.01, "position du contrôle = piquet (y = Nord, m)")
    }

    // ------------------------------------------------------------------ 4. code inconnu

    @Test
    fun `4 - code inconnu de la tournee - refus affiche, rien d'ouvert ni d'ecrit`() {
        val g = EcranEssai()
        g.rendre()
        g.aLaSeconde(30)
        g.saisir("ZZZ")
        g.appuyer(TextesControle.OUVRIR_SANS_POSITION)
        val refus = assertIs<EtatArrivee.Refus>(
            ArriveeControle.ouvrirSansPosition(BancControle().contexte, JournalEnMemoire(), HorlogeDeTest(), Essences(emptySet()), "ZZZ", EcouteComptee()),
        )
        assertEquals("code_inconnu", refus.refus.code)
        assertTrue(present(TextesControle.message(refus.refus)!!.texte), "le refus en clair")
        assertEquals(emptyList(), g.ouvertures)
        assertNull(ControleursOuverts.get(cle))
        assertEquals(0, g.journal.nombreDeLignes)
        assertEquals(0, g.sauvegarde.saisies)
    }

    // ------------------------------------------------------------------ 5. localisation coupée, permission refusée

    @Test
    fun `5 - localisation coupee ou permission refusee - demande de l'activer, jamais le bouton, meme a 1 h`() {
        for ((loc, perm) in listOf(false to true, true to false)) {
            val g = EcranEssai(localisationActive = loc, permissionAccordee = perm)
            g.rendre()
            g.aLaSeconde(3600)
            assertTrue(present(TextesControle.ACTIVER_LOCALISATION), "localisation $loc, permission $perm")
            assertTrue(!boutonPresent(TextesControle.OUVRIR_SANS_POSITION))
            assertEquals(emptyList(), g.ouvertures)
            BancRendu.fermer()
        }
    }

    // ------------------------------------------------------------------ 6. un fix arrive pendant la confirmation

    @Test
    fun `6 - un fix pendant la confirmation - l'etape est abandonnee, la distance revient, le compte repart de zero`() {
        val g = EcranEssai()
        g.rendre()
        g.aLaSeconde(30)
        g.saisir("P35-01")
        g.appuyer(TextesControle.OUVRIR_SANS_POSITION)
        assertTrue(present(TextesControle.ANNULER), "la confirmation est affichée")
        g.poserFix(fixSur(aDistance(200.0)))
        assertTrue(!present(TextesControle.ANNULER), "la confirmation est abandonnée")
        assertTrue(!boutonPresent(TextesControle.OUVRIR_SANS_POSITION))
        assertTrue(!champPresent(), "le champ disparaît au retour d'un fix (décision 2)")
        assertTrue(present(distanceA200), "la distance revient")
        // Le fix se perd de nouveau à 40 s : l'attente repart de zéro (décision 3).
        g.aLaSeconde(40)
        g.poserFix(null)
        assertTrue(present(TextesControle.attenteSansFix(0)))
        g.aLaSeconde(69)
        assertTrue(!boutonPresent(TextesControle.OUVRIR_SANS_POSITION), "29 s depuis la perte du fix")
        g.aLaSeconde(70)
        assertTrue(boutonPresent(TextesControle.OUVRIR_SANS_POSITION))
        assertEquals(emptyList(), g.ouvertures)
        assertEquals(0, g.journal.nombreDeLignes)
    }

    // ------------------------------------------------------------------ 7. avec un fix, rien ne change

    @Test
    fun `7 - avec un fix, comportement d'aujourd'hui - a 10 m l'arrivee et Controler, a 200 m ni champ ni bouton`() {
        val g = EcranEssai()
        g.fix.value = fixSur(aDistance(10.0))
        g.rendre()
        g.aLaSeconde(60)
        assertEquals(listOf<String?>(""), g.ouvertures, "l'arrivée à 30 m ouvre le contrôle, comme avant")
        assertTrue(boutonPresent("Contrôler"))
        assertTrue(champPresent(), "le champ du code de l'arrivée")
        assertTrue(!boutonPresent(TextesControle.OUVRIR_SANS_POSITION))
        assertTrue(!present(TextesControle.SANS_FIX_PROPOSE))
        BancRendu.fermer()

        val loin = EcranEssai()
        loin.fix.value = fixSur(aDistance(200.0))
        loin.rendre()
        loin.aLaSeconde(60)
        assertTrue(present(distanceA200))
        assertTrue(!champPresent())
        assertTrue(!boutonPresent("Contrôler"))
        assertTrue(!boutonPresent(TextesControle.OUVRIR_SANS_POSITION))
        assertEquals(emptyList(), loin.ouvertures)
    }

    // ------------------------------------------------------------------ 8. code dans plusieurs UG

    @Test
    fun `8 - code present dans deux UG - l'UG est demandee par l'ecran de controle de la 087, rien d'ecrit`() {
        val posB = PointLambert93(BancControle.POSITION_P1.xMetres + 100.0, BancControle.POSITION_P1.yMetres)
        val homonymes = ContexteControle(
            operateur = "Agent d'essai",
            placettes = listOf(
                PlacetteControlable(Piquet(UuidPlacette("placette-ug12"), "P01", BancControle.POSITION_P1, ug = "12"), 3.99),
                PlacetteControlable(Piquet(UuidPlacette("placette-ug14"), "P01", posB, ug = "14"), 3.99),
            ),
            epsg = CodeEpsg.RGF93_V1_LAMBERT93,
            empriseForet = null,
            plantsAnterieurs = emptyMap(),
        )
        val g = EcranEssai(contexte = homonymes)
        g.rendre()
        g.aLaSeconde(30)
        g.saisir("P01")
        g.appuyer(TextesControle.OUVRIR_SANS_POSITION)
        val etape = assertIs<EtatArrivee.UgAChoisir>(SansPositionEnAttente.etape(cle), "l'étape est confiée à l'écran de contrôle")
        assertEquals(setOf<String?>("12", "14"), etape.candidats.map { it.ug }.toSet())
        assertEquals(listOf<String?>("P01"), g.ouvertures)
        assertNull(ControleursOuverts.get(cle))
        assertEquals(0, g.journal.nombreDeLignes)
    }

    // ------------------------------------------------------------------ 9. à la taille du téléphone

    private fun taille(cas: String) {
        val defauts = ArrayList<String>()
        val g = EcranEssai()
        val r0 = g.rendre()
        g.aLaSeconde(30)
        val propose = BancRendu.lire(regle, r0.densite)
        defauts += verifier("$cas-propose", propose)
        g.saisir("P35-01")
        g.appuyer(TextesControle.OUVRIR_SANS_POSITION)
        val confirmation = BancRendu.lire(regle, r0.densite)
        defauts += verifier("$cas-confirmation", confirmation, annuler = true)
        assertTrue(defauts.isEmpty(), "Rendu « $cas » :\n" + defauts.joinToString("\n"))
    }

    /** « Ouvrir sans position » (et « Annuler » à la confirmation) vus en entier sans défiler ; aucun cliquable recouvert. */
    private fun verifier(cas: String, r: BancRendu.Resultat, annuler: Boolean = false): List<String> {
        val defauts = ArrayList<String>()
        val attendus = listOf(TextesControle.OUVRIR_SANS_POSITION) + if (annuler) listOf(TextesControle.ANNULER) else emptyList()
        for (nom in attendus) {
            val b = BancRendu.cliquable(r, nom)
            if (b == null) defauts += "bouton « $nom » absent"
            else if (!BancRendu.vu(r, b)) defauts += "bouton « $nom » ${BancRendu.fmt(b.rect)} n'est pas vu en entier dans la racine ${BancRendu.fmt(r.racine)}"
        }
        defauts += BancRendu.recouvrements(r)
        BancRendu.rapport(cas, r, defauts)
        return defauts
    }

    @Test @Config(qualifiers = BancRendu.DEBOUT_PLEIN) fun `9 - debout en plein ecran, bouton vu, rien de recouvert`() = taille("guidage-sans-fix-debout-plein")
    @Test @Config(qualifiers = BancRendu.DEBOUT_SANS_PLEIN) fun `9 - debout sans plein ecran, bouton vu, rien de recouvert`() = taille("guidage-sans-fix-debout-sans-plein")
    @Test @Config(qualifiers = BancRendu.COUCHE_PLEIN) fun `9 - couche en plein ecran, bouton vu, rien de recouvert`() = taille("guidage-sans-fix-couche-plein")
    @Test @Config(qualifiers = BancRendu.COUCHE_SANS_PLEIN) fun `9 - couche sans plein ecran, bouton vu, rien de recouvert`() = taille("guidage-sans-fix-couche-sans-plein")
}
