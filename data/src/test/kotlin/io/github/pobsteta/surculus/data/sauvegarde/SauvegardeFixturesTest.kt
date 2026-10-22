package io.github.pobsteta.surculus.data.sauvegarde

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.pobsteta.surculus.data.journal.LigneJournal
import io.github.pobsteta.surculus.data.journal.db.HorlogeFixe
import io.github.pobsteta.surculus.data.journal.db.JournalDatabase
import io.github.pobsteta.surculus.data.journal.db.JournalRepository
import io.github.pobsteta.surculus.data.sauvegarde.OutilsSursync.fichier
import io.github.pobsteta.surculus.data.sauvegarde.OutilsSursync.journalDe
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Cas d'acceptation `fixtures/sauvegarde/` (SV-01 à SV-33), rejoués sans les modifier : sauvegarde puis
 * restauration à travers Room (Robolectric), versions, fichiers abîmés, noms.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SauvegardeFixturesTest {
    private val bases = mutableListOf<JournalDatabase>()
    private val contexte: Context get() = ApplicationProvider.getApplicationContext()

    @After fun fermer() { bases.forEach { it.close() } }

    private fun depot(appareil: String, lignes: List<LigneJournal> = emptyList()): JournalRepository {
        val db = JournalDatabase.construire(contexte, null).allowMainThreadQueries().build().also { bases += it }
        return JournalRepository(db, appareil, HorlogeFixe(), tourneeObligatoire = false).also { d -> lignes.forEach(d::recevoir) }
    }

    private fun sauverPuisRestaurer(ref: String, restaurant: String): Triple<JournalRepository, JournalRepository, CompteRendu> {
        val origine = depot("A", journalDe(ref))
        val octets = OutilsSursync.ecrire(origine.lignes())
        val cible = depot(restaurant)
        return Triple(origine, cible, Restauration.restaurer(octets, cible))
    }

    private fun identiques(origine: JournalRepository, cible: JournalRepository, cr: CompteRendu) {
        assertTrue(cr.accepte)
        assertEquals(origine.lignes(), cible.lignes())
        assertEquals(origine.projection(), cible.projection())
        assertEquals(origine.anomalies(), cible.anomalies())
    }

    // --- restauration.json ---

    @Test fun `SV-01 nominal - memes lignes dans le meme ordre champ par champ`() {
        val (o, c, cr) = sauverPuisRestaurer("journal/projection.json#JR-02", "B")
        identiques(o, c, cr)
        assertEquals(2, c.lignes().size)
        // Ordre du fichier lui-même conservé : relecture directe.
        val lues = LectureSursync.lire(OutilsSursync.ecrire(journalDe("journal/projection.json#JR-02"))).lignes
        assertEquals(journalDe("journal/projection.json#JR-02"), lues)
    }

    @Test fun `SV-02 annulation et remplace_uuid restaures comme lignes ordinaires`() {
        val (o, c, cr) = sauverPuisRestaurer("journal/projection.json#JR-03", "B")
        identiques(o, c, cr)
        assertEquals(3, c.lignes().size)
        assertEquals(
            journalDe("journal/projection.json#JR-03").first { it.type == "annulation" }.remplaceUuid,
            c.lignes().first { it.type == "annulation" }.remplaceUuid,
        )
    }

    @Test fun `SV-03 un conflit reste un conflit apres restauration`() {
        val (o, c, cr) = sauverPuisRestaurer("journal/conflits.json#JR-20", "B")
        identiques(o, c, cr)
        assertTrue(c.projection().values.any { it.conflit })
    }

    @Test fun `SV-04 saut d'horloge - heures brutes intactes`() {
        val (o, c, cr) = sauverPuisRestaurer("journal/horloge.json#JR-40", "B")
        identiques(o, c, cr)
        assertEquals(journalDe("journal/horloge.json#JR-40").map { it.saisiLeUtcMs }.sorted(), c.lignes().map { it.saisiLeUtcMs }.sorted())
    }

    @Test fun `SV-05 projection recalculee identique`() {
        val (o, c, cr) = sauverPuisRestaurer("journal/projection.json#JR-03", "B")
        assertTrue(cr.accepte)
        c.reconstruireProjection()
        assertEquals(o.projection(), c.projection())
    }

    @Test fun `SV-06 journal vide puis journal d'une ligne`() {
        val vide = depot("A"); val c0 = depot("B")
        val cr0 = Restauration.restaurer(OutilsSursync.ecrire(vide.lignes()), c0)
        identiques(vide, c0, cr0)
        assertEquals(0, c0.lignes().size)
        val (o, c, cr) = sauverPuisRestaurer("journal/projection.json#JR-01", "B")
        identiques(o, c, cr)
        assertEquals(1, c.lignes().size)
    }

    @Test fun `SV-07 l'appareil restaurant n'est pas reecrit dans les lignes`() {
        val (o, c, cr) = sauverPuisRestaurer("journal/ordre-total.json#JR-30", "Z")
        identiques(o, c, cr)
        assertEquals(journalDe("journal/ordre-total.json#JR-30").map { it.appareil }.toSet(), c.lignes().map { it.appareil }.toSet())
        assertFalse("Z" in c.lignes().map { it.appareil })
    }

    @Test fun `SV-08 restaurer deux fois est idempotent`() {
        val origine = depot("A", journalDe("journal/projection.json#JR-02"))
        val octets = OutilsSursync.ecrire(origine.lignes())
        val cible = depot("B")
        val premier = Restauration.restaurer(octets, cible)
        val second = Restauration.restaurer(octets, cible)
        assertEquals(2, premier.ajoutees)
        assertEquals(0, second.ajoutees)
        assertEquals(2, second.dejaPresentes)
        identiques(origine, cible, second)
    }

    // --- versions.json ---

    @Test fun `SV-10 version courante lue`() {
        val l = journalDe("journal/projection.json#JR-02")
        val r = LectureSursync.lire(OutilsSursync.fichierDe(l, 1), versionCourante = 1)
        assertTrue(r.accepte); assertEquals(l, r.lignes)
    }

    @Test fun `SV-11 version N-1 lue par un lecteur de version 2`() {
        val l = journalDe("journal/projection.json#JR-02")
        val r = LectureSursync.lire(OutilsSursync.fichierDe(l, 1), versionCourante = 2)
        assertTrue(r.accepte); assertEquals(l, r.lignes)
        // N-2 : refusée (version 1 face à un lecteur de version 3).
        assertEquals(MotifRefus.VERSION_ILLISIBLE, LectureSursync.lire(OutilsSursync.fichierDe(l, 1), versionCourante = 3).motif)
    }

    @Test fun `SV-12 version N+1 refusee, journal local inchange`() {
        val l = journalDe("journal/projection.json#JR-02")
        val local = depot("B", l.take(1))
        val avant = local.lignes()
        val cr = Restauration.restaurer(OutilsSursync.fichierDe(l, 2), local, versionCourante = 1)
        assertEquals(MotifRefus.VERSION_FUTURE, cr.refus)
        assertEquals(avant, local.lignes())
    }

    @Test fun `SV-13 version absente, texte ou negative - refusee`() {
        val l = journalDe("journal/projection.json#JR-02")
        for (v in listOf(null, "abc", -1, 0, 1.5)) {
            val local = depot("B", l.take(1))
            val cr = Restauration.restaurer(OutilsSursync.fichierDe(l, v), local)
            assertEquals(MotifRefus.VERSION_ILLISIBLE, cr.refus, "version $v")
            assertEquals(l.take(1), local.lignes())
        }
    }

    // --- fichiers-refuses.json ---

    @Test fun `SV-20 gzip tronque - prefixe lisible restaure, rien d'invente`() {
        val l = journalDe("journal/projection.json#JR-02")
        val octets = OutilsSursync.ecrire(l)
        val r = LectureSursync.lire(octets.copyOf(octets.size - 10))
        assertTrue(r.accepte); assertTrue(Signalement.FICHIER_TRONQUE in r.signalements)
        assertEquals(l.take(r.lignes.size), r.lignes)
    }

    @Test fun `SV-20 gzip coupe au milieu d'un long journal - strictement moins de lignes, dans l'ordre`() {
        val base = journalDe("journal/projection.json#JR-01").single()
        val longues = (1..400).map { base.copy(uuid = "U%04d".format(it), sequence = it.toLong(), valeur = """{"etat":"sain","n":$it}""") }
        val octets = OutilsSursync.ecrire(longues)
        val r = LectureSursync.lire(octets.copyOf(octets.size * 7 / 10))
        assertTrue(r.accepte)
        assertTrue(Signalement.FICHIER_TRONQUE in r.signalements)
        assertTrue(r.lignes.size in 1 until 400, "lignes : ${r.lignes.size}")
        assertEquals(longues.take(r.lignes.size), r.lignes)
        assertEquals(0, r.lignesRefusees)
    }

    @Test fun `SV-21 fichier de 0 octet refuse, journal local inchange`() {
        val local = depot("B", journalDe("journal/projection.json#JR-01"))
        val cr = Restauration.restaurer(ByteArray(0), local)
        assertEquals(MotifRefus.VERSION_ILLISIBLE, cr.refus)
        assertEquals(1, local.lignes().size)
    }

    @Test fun `SV-22 une ligne json coupee - refusee et signalee, les autres restaurees`() {
        val l = journalDe("journal/projection.json#JR-02")
        val texte = OutilsSursync.lignesTexte(OutilsSursync.ecrire(l)).dropLast(1) // sans le pied
        val coupe = texte.toMutableList().also { it[1] = it[1].take(it[1].length / 2) }
        val r = LectureSursync.lire(OutilsSursync.refaire(coupe))
        assertTrue(r.accepte)
        assertEquals(setOf(Signalement.LIGNE_ILLISIBLE), r.signalements)
        assertEquals(1, r.lignesRefusees)
        assertEquals(l.drop(1), r.lignes)
    }

    @Test fun `SV-23 valeur modifiee, empreinte inchangee - avertissement, lignes restaurees`() {
        val l = journalDe("journal/projection.json#JR-02")
        val texte = OutilsSursync.lignesTexte(OutilsSursync.ecrire(l))
        assertTrue(texte[2].contains("mort"))
        val altere = texte.toMutableList().also { it[2] = it[2].replace("mort", "sain") }
        val octets = fichier(altere.dropLast(1), empreinte = OutilsSursync.json(texte.last()).get("sha256").asString)
        val local = depot("B")
        val cr = Restauration.restaurer(octets, local)
        assertTrue(cr.accepte)
        assertEquals(setOf(Signalement.EMPREINTE_DIFFERENTE), cr.signalements)
        assertEquals(2, local.lignes().size)
    }

    @Test fun `SV-24 uuid en double - identique ignore, different refuse et signale`() {
        val ident = LectureSursync.lire(OutilsSursync.fichierDe(journalDe("journal/projection.json#JR-12")))
        assertTrue(ident.accepte); assertEquals(1, ident.lignesIgnorees); assertTrue(ident.signalements.isEmpty())
        assertEquals(1, ident.lignes.size)

        val diff = LectureSursync.lire(OutilsSursync.fichierDe(journalDe("journal/projection.json#JR-13")))
        assertTrue(diff.accepte); assertEquals(listOf("U1"), diff.uuidRefuses)
        assertEquals(setOf(Signalement.UUID_CONTENU_DIFFERENT), diff.signalements); assertEquals(0, diff.lignes.size)

        val entre = LectureSursync.lire(OutilsSursync.fichierDe(journalDe("journal/projection.json#JR-14")))
        assertEquals(listOf("U1"), entre.uuidRefuses)
        assertEquals(setOf(Signalement.UUID_CONTENU_DIFFERENT), entre.signalements)
        assertEquals(listOf("U2"), entre.lignes.map { it.uuid })
    }

    @Test fun `SV-25 derniere ligne coupee - les precedentes entrent dans le journal`() {
        val l = journalDe("journal/projection.json#JR-03")
        val texte = OutilsSursync.lignesTexte(OutilsSursync.ecrire(l)).dropLast(1)
        val coupe = texte.dropLast(1) + texte.last().take(texte.last().length / 2)
        val local = depot("B")
        val cr = Restauration.restaurer(OutilsSursync.refaire(coupe), local)
        assertTrue(cr.accepte)
        assertEquals(setOf(Signalement.LIGNE_ILLISIBLE), cr.signalements)
        assertEquals(1, cr.refusees)
        assertEquals(listOf("U1", "U2"), local.lignes().map { it.uuid }.sorted())
    }

    @Test fun `pied absent - fichier_tronque, lignes lues`() {
        val l = journalDe("journal/projection.json#JR-02")
        val sansPied = fichier(listOf(OutilsSursync.entete(1, 2).toString()) + l.map { EcritureSursync.versJson(it).toString() }, pied = false)
        val r = LectureSursync.lire(sansPied)
        assertTrue(r.accepte); assertEquals(setOf(Signalement.FICHIER_TRONQUE), r.signalements); assertEquals(l, r.lignes)
    }

    @Test fun `fichier non gzip ou en-tete illisible - refuse`() {
        assertEquals(MotifRefus.VERSION_ILLISIBLE, LectureSursync.lire("pas un gzip".toByteArray()).motif)
        assertEquals(MotifRefus.VERSION_ILLISIBLE, LectureSursync.lire(fichier(listOf("{pas du json"))).motif)
        assertNull(LectureSursync.lire(OutilsSursync.ecrire(emptyList())).motif)
    }

    // --- ecrasement.json ---

    @Test fun `SV-40 restaurer sur un journal local non vide - lignes locales gardees, une seule ajoutee`() {
        val locales = journalDe("journal/projection.json#JR-02") // U1, U2
        val local = depot("B", locales)
        val avant = local.lignes()
        // Le fichier a toutes les lignes locales sauf U2, et une ligne de plus.
        val enPlus = locales[0].copy(uuid = "U9", sequence = 9, saisiLeUtcMs = locales[0].saisiLeUtcMs + 1000)
        val fichier = OutilsSursync.fichierDe(listOf(locales[0], enPlus))
        val cr = Restauration.restaurer(fichier, local)
        assertTrue(cr.accepte)
        assertEquals(1, cr.ajoutees)
        assertEquals(1, cr.dejaPresentes)
        val apres = local.lignes()
        assertEquals(3, apres.size)
        assertTrue(apres.containsAll(avant), "une ligne locale a disparu")
        assertEquals(apres.size, apres.map { it.uuid }.toSet().size, "doublon")
        assertEquals(listOf("U9"), apres.map { it.uuid } - avant.map { it.uuid }.toSet())
    }

    // --- noms-fichier.json ---

    @Test fun `SV-30 SV-31 gabarit du nom, sans deux-points`() {
        val nom = NomSauvegarde.nom("COUCHEY-2026", "a3f9c2d1", 1_791_968_707_000L) // 2026-10-14T09:05:07Z
        assertEquals("COUCHEY-2026-a3f9c2d1-20261014T090507Z.sursync", nom)
        assertTrue(Regex("^COUCHEY-2026-[^:\\s]+-[^:\\s]+\\.sursync$").matches(nom))
        assertFalse(':' in nom)
    }

    @Test fun `SV-32 ni proprietaire ni agent dans le nom`() {
        val appareil = NomSauvegarde.nouvelIdentifiantAppareil(java.util.Random(42))
        val nom = NomSauvegarde.nom("COUCHEY-2026", appareil, 1_791_968_707_000L)
        for (interdit in listOf("Jean", "Dupont", "Téléphone")) assertFalse(interdit in nom)
        assertTrue(Regex("[0-9a-f]{8}").matches(appareil))
        // Un nom lisible n'est pas un identifiant opaque : refusé.
        for (lisible in listOf("Téléphone de Jean Dupont", "a:b", "", "a-b")) {
            val e = runCatching { NomSauvegarde.nom("X", lisible, 0) }
            assertTrue(e.isFailure, lisible)
        }
    }

    @Test fun `SV-33 code de tournee avec espace, accent, barre et deux-points`() {
        val nom = NomSauvegarde.nom("Forêt d'Orléans/Lot 3: nord", "a3f9c2d1", 1_791_968_707_000L)
        for (c in listOf(":", "/", "\\", " ")) assertFalse(c in nom, "contient $c : $nom")
        assertTrue(nom.endsWith(".sursync"))
        assertTrue(nom.startsWith("Foret_d_Orleans_Lot_3_nord-"))
        assertEquals("tournee", NomSauvegarde.assainirCode("///"))
    }
}
