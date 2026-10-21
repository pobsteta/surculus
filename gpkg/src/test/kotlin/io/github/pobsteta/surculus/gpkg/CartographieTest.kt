package io.github.pobsteta.surculus.gpkg

import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.placette.EmpriseEtFrance
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CartographieTest {
    @TempDir
    lateinit var dossier: Path

    private fun carre(x: Double, y: Double, c: Double) =
        listOf(x to y, x + c to y, x + c to y + c, x to y + c, x to y).map { PointLambert93(it.first, it.second) }

    private val origine = TourneeDeTest.centre(0)

    private val ugAvecTrou = Geometrie.Polygone(
        listOf(carre(origine.xMetres, origine.yMetres, 400.0), carre(origine.xMetres + 100, origine.yMetres + 100, 100.0)),
    )
    private val parcelleDouble = Geometrie.MultiPolygone(
        listOf(
            Geometrie.Polygone(listOf(carre(origine.xMetres + 500, origine.yMetres, 100.0))),
            Geometrie.Polygone(listOf(carre(origine.xMetres + 700, origine.yMetres, 100.0))),
        ),
    )

    private fun vide(srs: Int): ByteArray =
        ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).put('G'.code.toByte()).put('P'.code.toByte()).put(0).put(0x11).putInt(srs).array()

    private fun inserer(f: Path, requete: String, vararg blobs: ByteArray?) {
        Sqlite.ouvrir(f).use { cnx ->
            cnx.prepareStatement(requete).use { ps ->
                blobs.forEachIndexed { i, b -> if (b == null) ps.setNull(i + 1, java.sql.Types.BLOB) else ps.setBytes(i + 1, b) }
                ps.executeUpdate()
            }
        }
    }

    /** Couchey (forêt, UG 35 en carré) + UG 36 à trou, parcelles (dont un multipolygone), et trois géométries défectueuses. */
    private fun coucheyAvecGeometries(): Path {
        val f = TourneeDeTest.couchey(dossier)
        inserer(f, "INSERT INTO ug (geom, numero) VALUES (?, '36')", GeometrieGpkg.encoder(ugAvecTrou, 2154))
        inserer(f, "INSERT INTO parcelles (geom, reference, designation) VALUES (?, '21192000AB0012', 'AB 12')", GeometrieGpkg.encoder(parcelleDouble, 2154))
        inserer(f, "INSERT INTO parcelles (geom, reference, designation) VALUES (?, '21192000AB0013', 'AB 13')", GeometrieGpkg.encoder(ugAvecTrou, 2154))
        return f
    }

    @Test
    fun `les trois couches de Couchey sont lues, avec le bon nombre d objets et leur identifiant`() {
        val c = LecteurTournee.lireCartographie(coucheyAvecGeometries())
        assertEquals(listOf("Forêt de Couchey (démo)"), c.foret.map { it.id })
        assertEquals(listOf("35", "36"), c.ug.map { it.id })
        assertEquals(listOf("21192000AB0012", "21192000AB0013"), c.parcelles.map { it.id })
        assertEquals(listOf("AB 12", "AB 13"), c.parcelles.map { it.designation })
        assertEquals(emptyList(), c.anomalies)
        assertEquals(c.ug.map { it.fid }.distinct().size, c.ug.size)
    }

    @Test
    fun `un polygone a trou garde son trou, un multipolygone garde toutes ses parties`() {
        val c = LecteurTournee.lireCartographie(coucheyAvecGeometries())
        assertEquals(ugAvecTrou, c.ug.single { it.id == "36" }.geometrie)
        assertEquals(2, (c.ug.single { it.id == "36" }.geometrie as Geometrie.Polygone).anneaux.size)
        assertEquals(parcelleDouble, c.parcelles.single { it.id == "21192000AB0012" }.geometrie)
        assertEquals(2, (c.parcelles[0].geometrie as Geometrie.MultiPolygone).polygones.size)
    }

    @Test
    fun `le centre de l emprise des UG de Couchey tombe en France, x = Est et y = Nord en metres`() {
        val c = LecteurTournee.lireCartographie(coucheyAvecGeometries())
        val e = assertNotNull(c.empriseUg)
        val centre = PointLambert93((e.xMinMetres + e.xMaxMetres) / 2, (e.yMinMetres + e.yMaxMetres) / 2)
        assertTrue(EmpriseEtFrance.controler(centre).enFrance)
        assertTrue(centre.xMetres in 800_000.0..900_000.0, "x (Est) = ${centre.xMetres}")
        assertTrue(centre.yMetres in 6_600_000.0..6_700_000.0, "y (Nord) = ${centre.yMetres}")
        assertTrue(e.contient(TourneeDeTest.centre(1)), "la placette P35-02 est dans l'emprise des UG")
    }

    @Test
    fun `une emprise aux axes inverses est refusee (placette en Somalie)`() {
        val f = TourneeDeTest.modeleVide(dossier)
        val inverse = Geometrie.MultiPolygone(
            listOf(Geometrie.Polygone(listOf(carre(origine.yMetres, origine.xMetres, 400.0)))), // x et y permutés
        )
        inserer(f, "INSERT INTO ug (geom, numero) VALUES (?, '35')", GeometrieGpkg.encoder(inverse, 2154))
        val refus = assertFailsWith<GpkgRefuse> { LecteurTournee.lireCartographie(f) }.refus
        assertEquals("hors_france", refus.motif)
        assertTrue((refus as RefusGpkg.PointHorsFrance).axesInverses)
        assertEquals("ug", refus.couche)
    }

    @Test
    fun `une geometrie nulle, vide ou non polygonale est signalee sans faire echouer les autres`() {
        val f = coucheyAvecGeometries()
        inserer(f, "INSERT INTO ug (geom, numero) VALUES (?, 'nulle')", null)
        inserer(f, "INSERT INTO ug (geom, numero) VALUES (?, 'vide')", vide(2154))
        inserer(f, "INSERT INTO ug (geom, numero) VALUES (?, 'point')", GeometrieGpkg.encoder(Geometrie.Point(origine), 2154))
        val c = LecteurTournee.lireCartographie(f)
        assertEquals(listOf("35", "36"), c.ug.map { it.id })
        assertEquals(
            listOf(RaisonAnomalie.NULLE, RaisonAnomalie.VIDE, RaisonAnomalie.TYPE_NON_POLYGONAL),
            c.anomalies.map { it.raison },
        )
        assertTrue(c.anomalies.all { it.couche == "ug" })
        assertEquals(3, c.anomalies.map { it.fid }.distinct().size)
    }

    @Test
    fun `une tournee sans geometrie rend des listes vides et pas d emprise`() {
        val c = LecteurTournee.lireCartographie(TourneeDeTest.modeleVide(dossier))
        assertEquals(emptyList(), c.ug)
        assertNull(c.empriseUg)
    }

    @Test
    fun `lire un polygone Z en gros boutiste redonne ses points`() {
        val b = ByteBuffer.allocate(8 + 5 + 4 + 4 + 5 * 24).order(ByteOrder.BIG_ENDIAN)
        b.put('G'.code.toByte()).put('P'.code.toByte()).put(0).put(0).putInt(2154)
        b.put(0).putInt(1003).putInt(1).putInt(5)
        for (p in carre(origine.xMetres, origine.yMetres, 10.0)) b.putDouble(p.xMetres).putDouble(p.yMetres).putDouble(300.0)
        val g = GeometrieGpkg.lire(b.array())
        assertEquals(Geometrie.Polygone(listOf(carre(origine.xMetres, origine.yMetres, 10.0))), g)
    }

    @Test
    fun `un blob tronque est illisible, il leve une exception claire`() {
        val blob = GeometrieGpkg.encoder(ugAvecTrou, 2154)
        assertFailsWith<IllegalArgumentException> { GeometrieGpkg.lire(blob.copyOf(blob.size - 40)) }
    }
}
