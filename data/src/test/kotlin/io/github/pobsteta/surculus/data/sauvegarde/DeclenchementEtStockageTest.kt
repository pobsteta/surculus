package io.github.pobsteta.surculus.data.sauvegarde

import io.github.pobsteta.surculus.data.sauvegarde.Evenement.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** SV-41, SV-42 (écrasement, rotation) et SV-50 à SV-55 (déclenchement, photo), sans Android ; SV-40 (union avec un journal local) est dans `SauvegardeFixturesTest`, qui a besoin de Room. Les tests ne dépendent pas des valeurs de N et M. */
class DeclenchementEtStockageTest {
    private val n = RegleDeclenchement.N_SAISIES
    private val m = RegleDeclenchement.M_MINUTES.toDouble()

    // --- declenchement.json ---

    @Test fun `SV-50 chaque fin d'unite de travail declenche`() {
        for (e in listOf(FIN_PLACETTE, FIN_CONSTAT, FIN_VIDE, FIN_INTERVENTION)) {
            assertTrue(RegleDeclenchement.doitSauvegarder(e, 1, 0.0), e.name)
        }
    }

    @Test fun `SV-51 a la N-ieme saisie, pas a la N-1-ieme`() {
        assertFalse(RegleDeclenchement.doitSauvegarder(SAISIE, n - 1, 0.0))
        assertTrue(RegleDeclenchement.doitSauvegarder(SAISIE, n, 0.0))
    }

    @Test fun `SV-52 apres M minutes monotones avec une saisie nouvelle`() {
        assertFalse(RegleDeclenchement.doitSauvegarder(TIC, 1, m - 0.01))
        assertTrue(RegleDeclenchement.doitSauvegarder(TIC, 1, m))
    }

    @Test fun `SV-53 un saut de l'horloge murale ne change rien - seule la duree monotone compte`() {
        // La règle ne reçoit que la durée monotone : par construction, ni +2 h ni -1 h murales n'y entrent.
        val suivi = SuiviDeclenchement(dernierMonotoneMs = 0)
        suivi.surEvenement(SAISIE, 1_000)
        val avantM = ((m - 1) * 60_000).toLong()
        assertFalse(suivi.surEvenement(TIC, avantM))
        assertTrue(suivi.surEvenement(TIC, (m * 60_000).toLong()))
    }

    @Test fun `SV-54 aucune saisie nouvelle - pas de sauvegarde, meme apres dix fois M`() {
        assertFalse(RegleDeclenchement.doitSauvegarder(TIC, 0, 10 * m))
        assertFalse(RegleDeclenchement.doitSauvegarder(FIN_PLACETTE, 0, 10 * m))
    }

    @Test fun `le suivi repart de zero apres une sauvegarde`() {
        val suivi = SuiviDeclenchement(0)
        repeat(n - 1) { assertFalse(suivi.surEvenement(SAISIE, 1000)) }
        assertTrue(suivi.surEvenement(SAISIE, 1000))
        suivi.sauvegardeFaite(1000)
        assertFalse(suivi.surEvenement(TIC, 1000 + (10 * m * 60_000).toLong()))
    }

    @Test fun `N et M sont ceux de Pascal`() {
        assertEquals(20, FormatSursync.N_SAISIES); assertEquals(5, FormatSursync.M_MINUTES)
    }

    @Test fun `SV-55 photo ecrite des la prise avec son empreinte, dans le dossier de la tournee`() {
        val ecrits = mutableListOf<Triple<String, String, Int>>()
        val stockage = object : StockagePhotos {
            override fun ecrire(dossier: String, nom: String, octets: ByteArray): String {
                ecrits += Triple(dossier, nom, octets.size); return dossier + nom
            }
        }
        val photo = EnregistrementPhoto.enregistrer(stockage, "COUCHEY-2026", "p1.jpg", "abc".toByteArray())
        assertEquals("Pictures/Surculus/COUCHEY-2026/", ecrits.single().first)
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", photo.sha256)
        assertEquals("Pictures/Surculus/COUCHEY-2026/p1.jpg", photo.chemin)
    }

    // --- ecrasement.json ---

    private class Faux : StockageSauvegardes {
        val fichiers = linkedMapOf<String, ByteArray>()
        override fun noms() = fichiers.keys.toSet()
        override fun ecrire(nom: String, octets: ByteArray) { check(nom !in fichiers) { "écrasement de $nom" }; fichiers[nom] = octets }
        override fun supprimer(nom: String) { fichiers.remove(nom) }
    }

    @Test fun `SV-41 un nom pris n'est jamais ecrase - suffixe, ancien intact`() {
        val nom = "COUCHEY-2026-a3f9c2d1-20261014T090507Z.sursync"
        val faux = Faux().apply { fichiers[nom] = byteArrayOf(1, 2, 3) }
        val libre = NomSauvegarde.nomLibre(nom, faux.noms())
        assertTrue(libre != nom && libre.startsWith(nom.removeSuffix(".sursync")) && libre.endsWith(".sursync") && ':' !in libre)
        faux.ecrire(libre, byteArrayOf(9))
        assertEquals(listOf<Byte>(1, 2, 3), faux.fichiers[nom]!!.toList())
        assertTrue(NomSauvegarde.nomLibre(nom, faux.noms()) !in faux.noms())
    }

    @Test fun `SV-42 sixieme sauvegarde - la plus ancienne de cette tournee seulement`() {
        fun noms(code: String) = (1..5).map { NomSauvegarde.nom(code, "a3f9c2d1", 1_791_968_707_000L + it * 60_000L) }
        val coucheyAvant = noms("COUCHEY-2026"); val autre = noms("AUTRE-TOURNEE")
        val prefixe = noms("COUCHEY") // préfixe du code d'une autre tournée : ne doit pas être touché
        val nouvelle = NomSauvegarde.nom("COUCHEY-2026", "a3f9c2d1", 1_791_968_707_000L + 6 * 60_000L)
        val sup = Rotation.asupprimer(coucheyAvant + autre + prefixe + nouvelle, "COUCHEY-2026")
        assertEquals(listOf(coucheyAvant.first()), sup)
    }

    @Test fun `la rotation garde au plus cinq et ne supprime rien en dessous`() {
        val trois = (1..3).map { NomSauvegarde.nom("T", "a3f9c2d1", it * 60_000L) }
        assertTrue(Rotation.asupprimer(trois, "T").isEmpty())
        assertEquals(5, Rotation.asupprimer((1..10).map { NomSauvegarde.nom("T", "a3f9c2d1", it * 60_000L) }, "T").size)
    }
}
