package io.github.pobsteta.surculus.data.sauvegarde

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.pobsteta.surculus.data.journal.db.HorlogeFixe
import io.github.pobsteta.surculus.data.journal.db.JournalDatabase
import io.github.pobsteta.surculus.data.journal.db.JournalRepository
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Mission 088, tests du `dev` : le type de ligne `photo`, réservé par la v2 (spec § 4.8), n'est ni écrit ni restauré
 * par cette version. Décision de Pascal du 2026-11-03 (option A) : une ligne `photo` reçue dans une v2 est écartée et
 * signalée `ligne_illisible`, les autres lignes sont restaurées (`docs/formats/sursync-v2.md`, « Le type `photo` »).
 * Ce test fige ce comportement : la mission des photos le changera en connaissance de cause.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SursyncV2DevTest {
    private val contexte: Context get() = ApplicationProvider.getApplicationContext()
    private val bases = mutableListOf<JournalDatabase>()

    @After fun fermer() { bases.forEach { runCatching { it.close() } } }

    private val racine: File by lazy {
        var d: File? = File(System.getProperty("user.dir")).absoluteFile
        while (d != null && !File(d, "fixtures/sursync").isDirectory) d = d.parentFile
        checkNotNull(d)
    }

    /** Les deux saisies de `exemple-v2.sursync`, en texte (sans en-tête ni pied). */
    private val saisies: List<String> by lazy {
        OutilsSursync.lignesTexte(File(racine, "fixtures/sursync/exemple-v2.sursync").readBytes()).drop(1).dropLast(1)
    }

    /** Une ligne `photo` aux treize champs bien typés, comme la v2 la réserve (sa valeur n'est pas encore définie). */
    private val photo: String by lazy {
        OutilsSursync.json(saisies[1]).apply {
            addProperty("uuid", "019a4f00-0000-7000-8000-0000000000f1")
            addProperty("type", "photo")
            addProperty("valeur", "{\"chemin\":\"Pictures/Surculus/COUCHEY-2026/p.jpg\"}")
            addProperty("sequence", 3)
        }.toString()
    }

    private fun fichierV2(lignes: List<String>) = OutilsSursync.fichier(listOf(OutilsSursync.entete(2, lignes.size).toString()) + lignes)

    private fun depot(): JournalRepository {
        val db = JournalDatabase.construire(contexte, null).allowMainThreadQueries().build().also { bases += it }
        return JournalRepository(db, "B", HorlogeFixe(), tourneeObligatoire = false)
    }

    @Test fun `une ligne photo dans une v2 est ecartee et signalee ligne_illisible, les autres lues`() {
        val r = LectureSursync.lire(fichierV2(listOf(saisies[0], photo, saisies[1])))
        assertTrue(r.accepte, "le fichier n'est pas refusé en entier : ${r.motif}")
        assertEquals(setOf(Signalement.LIGNE_ILLISIBLE), r.signalements)
        assertEquals(1, r.lignesRefusees)
        assertEquals(listOf("019a4f00-0000-7000-8000-0000000000f1"), r.uuidRefuses)
        assertEquals(listOf("controle", "controle"), r.lignes.map { it.type })
    }

    @Test fun `restaurer une v2 avec une ligne photo - la photo n'entre pas au journal, les controles oui`() {
        val local = depot()
        val cr = Restauration.restaurer(fichierV2(listOf(saisies[0], photo, saisies[1])), local)
        assertTrue(cr.accepte)
        assertEquals(setOf(Signalement.LIGNE_ILLISIBLE), cr.signalements)
        assertEquals(2, cr.ajoutees); assertEquals(1, cr.refusees)
        assertEquals(setOf("controle"), local.lignes().map { it.type }.toSet())
        assertEquals(2, local.lignes().size)
    }

    @Test fun `une sauvegarde n'ecrit jamais de ligne photo, en version courante (3 depuis la mission 091)`() {
        val local = depot()
        Restauration.restaurer(fichierV2(saisies), local)
        val octets = OutilsSursync.ecrire(local.lignes())
        val types = OutilsSursync.lignesTexte(octets).drop(1).dropLast(1).map { OutilsSursync.json(it).get("type").asString }
        assertEquals(FormatSursync.VERSION_COURANTE, OutilsSursync.json(OutilsSursync.lignesTexte(octets).first()).get("version").asInt)
        assertEquals(3, FormatSursync.VERSION_COURANTE)
        assertTrue("photo" !in types, "$types")
    }
}
