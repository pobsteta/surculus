package io.github.pobsteta.surculus.data.session

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.geo.UniteAzimut
import io.github.pobsteta.surculus.core.placette.Essences
import io.github.pobsteta.surculus.core.placette.Piquet
import io.github.pobsteta.surculus.core.placette.UuidPlacette
import io.github.pobsteta.surculus.core.reperes.AzimutSaisi
import io.github.pobsteta.surculus.core.reperes.CodeEpsg
import io.github.pobsteta.surculus.core.session.Arrivee
import io.github.pobsteta.surculus.core.session.ContexteControle
import io.github.pobsteta.surculus.core.session.FormatValeurs
import io.github.pobsteta.surculus.core.session.OuvertureControle
import io.github.pobsteta.surculus.core.session.PlacetteControlable
import io.github.pobsteta.surculus.core.session.SessionControle
import io.github.pobsteta.surculus.core.session.SourceAzimut
import io.github.pobsteta.surculus.core.session.SuiteSession
import io.github.pobsteta.surculus.data.journal.TypeLigne
import io.github.pobsteta.surculus.data.journal.db.FixGnss
import io.github.pobsteta.surculus.data.journal.db.HorlogeFixe
import io.github.pobsteta.surculus.data.journal.db.JournalDatabase
import io.github.pobsteta.surculus.data.journal.db.JournalRepository
import io.github.pobsteta.surculus.data.sauvegarde.RegleDeclenchement
import io.github.pobsteta.surculus.data.sauvegarde.ServiceSauvegarde
import io.github.pobsteta.surculus.data.sauvegarde.StockageSauvegardes
import io.github.pobsteta.surculus.data.sauvegarde.SuiviDeclenchement
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Mission 012-A : la session de contrôle sur le **vrai** journal (Room) d'une tournée, comme dans l'application :
 * « Contrôlée » à « Terminer », lecture avec le code de la tournée ouverte, annulation de la fin, sauvegarde.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SessionSurJournalRoomTest {
    private val contexte: Context get() = ApplicationProvider.getApplicationContext()
    private val bases = mutableListOf<JournalDatabase>()
    private val horloge = HorlogeFixe(utcMs = 1_792_000_000_000L) // 2026-10-14, heure d'essai
    private var fix: FixGnss? = null

    @After fun fermer() = bases.forEach { it.close() }

    private fun depot(): JournalRepository {
        val db = JournalDatabase.construire(contexte, null).allowMainThreadQueries().build().also { bases += it }
        return JournalRepository(db, "tel-A", horloge, tourneeObligatoire = true)
    }

    private val p1 = UuidPlacette("11111111-1111-4111-8111-111111111111")
    private val piquet = PointLambert93(846_164.76, 6_686_123.26) // Couchey, EPSG:2154 (x = E, y = N, mètres)
    private val ctx = ContexteControle("Agent d'essai", listOf(PlacetteControlable(Piquet(p1, "P35-01", piquet), 3.99)), CodeEpsg.RGF93_V1_LAMBERT93)

    private fun session(j: JournalDeTournee, ecoute: io.github.pobsteta.surculus.core.session.EcouteSauvegarde = io.github.pobsteta.surculus.core.session.EcouteSauvegarde.AUCUNE): SessionControle {
        val h = HorlogeSessionAppareil(horloge) { fix }
        val r = SessionControle.ouvrir(ctx, j, h, Essences(setOf("CHS")), Arrivee.Lambert93(piquet), sauvegarde = ecoute)
        return (r as OuvertureControle.Ouverte).session
    }

    private fun SessionControle.pointe(d: Double, az: Double) =
        pointer(d, AzimutSaisi.valider(az, UniteAzimut.DEGRES).valeurOuEchec(), SourceAzimut.BOUSSOLE_TELEPHONE, "CHS")

    @Test
    fun `terminer rend la placette Controlee dans le vrai journal, annuler la fin la rend a faire`() {
        val depot = depot()
        val j = JournalDeTournee(depot, "COUCHEY-2026")
        val s = session(j)
        s.pointe(2.0, 100.0)
        s.dire("sain")
        assertEquals(emptySet(), j.placettesControlees(), "commencée, pas terminée : à faire")
        assertIs<SuiteSession.Fait>(s.terminer())
        assertEquals(setOf(p1.valeur), j.placettesControlees())
        // Le lendemain, toujours Contrôlée.
        horloge.avancer(24 * 3_600_000L)
        assertEquals(setOf(p1.valeur), j.placettesControlees())
        // Annuler la fin (le nouveau jour : la session voit le contrôle d'hier terminé, on annule la dernière saisie du jour).
        horloge.avancer(-24 * 3_600_000L)
        assertIs<SuiteSession.Fait>(s.annuler())
        assertEquals(emptySet(), j.placettesControlees(), "fin annulée : à faire")
        assertEquals(false, s.etat().termine)
        // Refaire la fin : Contrôlée de nouveau (le journal garde tout : saisie, annulation, nouvelle saisie).
        assertIs<SuiteSession.Fait>(s.terminer())
        assertEquals(setOf(p1.valeur), j.placettesControlees())
    }

    @Test
    fun `une saisie annulee ne compte pas, comme compterSaisies`() {
        val depot = depot()
        val j = JournalDeTournee(depot, "T")
        val s = session(j)
        s.pointe(2.0, 100.0)
        s.dire("sain")
        s.annuler()
        s.annuler()
        val etat = s.etat()
        assertEquals(0, etat.nbPlants)
        // La projection du journal ne garde que le contrôle ouvert (jamais une valeur annulée).
        val courants = j.lignesCourantes()
        assertEquals(listOf(TypeLigne.CONTROLE), courants.map { it.type })
    }

    @Test
    fun `lecture avec le code de la tournee ouverte, un oubli du code lirait la tournee vide`() {
        val depot = depot()
        val j = JournalDeTournee(depot, "COUCHEY-2026")
        val s = session(j)
        s.pointe(2.0, 100.0)
        val plant = j.lignesCourantes().first { it.type == FormatValeurs.TYPE_POINTAGE }.objetUuid
        // Avec le code de la tournée : la ligne est là.
        assertNotNull(depot.etat(plant, TypeLigne.POINTAGE, "COUCHEY-2026")?.courant)
        // Piège : le défaut `""` de `etat(...)` lit les lignes non rattachées, donc rien (autre état, autre réponse).
        assertNull(depot.etat(plant, TypeLigne.POINTAGE), "sans code, l'état se lit dans les lignes non rattachées")
        assertNull(depot.etat(plant, TypeLigne.POINTAGE, ""))
        // Le journal d'une tournée refuse donc un code vide, au lieu de lire ou d'écrire dans `""`.
        assertFailsWith<IllegalArgumentException> { JournalDeTournee(depot, "") }
        assertFailsWith<IllegalArgumentException> { JournalDeTournee(depot, "  ") }
    }

    @Test
    fun `une autre tournee n'influe pas sur celle-ci, meme placette, meme jour`() {
        val depot = depot()
        val a = JournalDeTournee(depot, "A")
        val b = JournalDeTournee(depot, "B")
        val sa = session(a)
        sa.pointe(2.0, 100.0); sa.dire("sain"); sa.terminer()
        assertEquals(setOf(p1.valeur), a.placettesControlees())
        assertEquals(emptySet(), b.placettesControlees(), "la tournée B n'a rien contrôlé")
        assertEquals(emptyList(), b.lignesCourantes())
        // B contrôle la même placette le même jour : un contrôle à elle, sans toucher à A.
        val sb = session(b)
        sb.pointe(1.0, 50.0)
        assertEquals(1, sb.etat().nbPlants)
        assertEquals(1, session(a).etat().nbPlants)
    }

    @Test
    fun `la session reprend apres fermeture de l'application, le journal Room fait memoire`() {
        val depot = depot()
        val j = JournalDeTournee(depot, "T")
        val s = session(j)
        s.pointe(2.0, 100.0); s.pointe(1.0, 200.0)
        s.suivant(); s.suivant()
        s.dire("mort")
        val rouverte = session(JournalDeTournee(depot, "T"))
        assertEquals(2, rouverte.etat().nbPlants)
        assertEquals(1, rouverte.etat().nbDits)
    }

    @Test
    fun `chaque ligne porte le code de la tournee et l'operateur du projet`() {
        val depot = depot()
        val j = JournalDeTournee(depot, "COUCHEY-2026")
        val s = session(j)
        s.pointe(2.0, 100.0); s.dire("sain"); s.terminer()
        val lignes = depot.lignes()
        assertTrue(lignes.isNotEmpty())
        assertTrue(lignes.all { it.codeTournee == "COUCHEY-2026" && it.operateur == "Agent d'essai" })
        assertEquals(setOf("controle", "pointage", "etat"), lignes.map { it.type }.toSet())
    }

    @Test
    fun `le fix GNSS frais fonde le jour civil dans le vrai journal`() {
        val depot = depot()
        val j = JournalDeTournee(depot, "T")
        // Téléphone déréglé (2026-10-14), fix GNSS de 2 s qui dit le 2026-10-27 à 09 h UTC.
        fix = FixGnss(heureUtcMs = java.time.Instant.parse("2026-10-27T09:00:00Z").toEpochMilli(), monotoneMs = horloge.monoMs - 2_000)
        val s = session(j)
        s.pointe(2.0, 100.0)
        assertEquals(java.time.LocalDate.of(2026, 10, 27), s.etat().jour)
    }

    @Test
    fun `les types de ligne de la session sont ceux de la liste fermee du journal`() {
        assertEquals(TypeLigne.CONTROLE, FormatValeurs.TYPE_CONTROLE)
        assertEquals(TypeLigne.POINTAGE, FormatValeurs.TYPE_POINTAGE)
        assertEquals(TypeLigne.ETAT, FormatValeurs.TYPE_ETAT)
        assertEquals(TypeLigne.ANNULATION, FormatValeurs.TYPE_ANNULATION)
        assertEquals(4, TypeLigne.TOUS.count { it in setOf(FormatValeurs.TYPE_CONTROLE, FormatValeurs.TYPE_POINTAGE, FormatValeurs.TYPE_ETAT, FormatValeurs.TYPE_ANNULATION) })
        assertEquals(5, TypeLigne.TOUS.size, "la liste fermée des cinq types ne change pas")
    }

    // ------------------------------------------------------------ sauvegarde

    private class StockageMemoire : StockageSauvegardes {
        val fichiers = linkedMapOf<String, ByteArray>()
        override fun noms() = fichiers.keys.toSet()
        override fun ecrire(nom: String, octets: ByteArray) { fichiers[nom] = octets }
        override fun supprimer(nom: String) { fichiers.remove(nom) }
    }

    @Test
    fun `la sauvegarde part a terminer et a la vingtieme saisie, jamais sans saisie nouvelle`() {
        val depot = depot()
        val stockage = StockageMemoire()
        val service = ServiceSauvegarde(depot, stockage, "a3f9c2d1", null)
        val suivi = SuiviDeclenchement(horloge.monoMs)
        val declencheur = DeclencheurSauvegarde(suivi, { horloge.monoMs }, { service.sauvegarder("T", horloge.utcMs) })
        val s = session(JournalDeTournee(depot, "T"), declencheur)
        // Contrôle + pointage = 2 saisies, puis 17 états corrigés = 19 : pas encore.
        s.pointe(2.0, 100.0)
        repeat(17) { s.dire(if (it % 2 == 0) "sain" else "mort"); horloge.avancer(1000) }
        assertEquals(0, stockage.fichiers.size, "19 saisies : pas de sauvegarde")
        s.dire("sain") // la 20e
        assertEquals(1, stockage.fichiers.size, "N = ${RegleDeclenchement.N_SAISIES} saisies : une sauvegarde")
        s.terminer() // une saisie de plus + fin de placette
        assertEquals(2, stockage.fichiers.size, "une seule sauvegarde à terminer (la saisie et la fin ne comptent pas deux fois)")
    }

    @Test
    fun `terminer sauvegarde tout de suite, et le sursync ne porte que cette tournee`() {
        val depot = depot()
        val stockage = StockageMemoire()
        val service = ServiceSauvegarde(depot, stockage, "a3f9c2d1", null)
        val declencheur = DeclencheurSauvegarde(SuiviDeclenchement(horloge.monoMs), { horloge.monoMs }, { service.sauvegarder("T", horloge.utcMs) })
        val autre = session(JournalDeTournee(depot, "AUTRE")); autre.pointe(1.0, 10.0)
        val s = session(JournalDeTournee(depot, "T"), declencheur)
        s.pointe(2.0, 100.0); s.dire("sain")
        assertEquals(0, stockage.fichiers.size)
        s.terminer()
        assertEquals(1, stockage.fichiers.size)
        val nom = stockage.fichiers.keys.single()
        assertTrue(nom.startsWith("T-"), "le nom porte le code de la tournée : $nom")
    }

    @Test
    fun `une sauvegarde qui echoue n'est pas comptee faite, la suivante reessaie`() {
        var appels = 0
        var echoue = true
        val suivi = SuiviDeclenchement(0)
        val d = DeclencheurSauvegarde(suivi, { 0L }, { appels++; if (echoue) error("carte absente") })
        d.surSaisie()
        d.surFinPlacette()
        assertEquals(1, appels)
        echoue = false
        d.surFinPlacette()
        assertEquals(2, appels, "échec : les saisies restent comptées, la fin suivante réessaie")
        d.surFinPlacette()
        assertEquals(2, appels, "réussie : plus de saisie nouvelle, pas de sauvegarde")
    }
}
