package io.github.pobsteta.surculus.gpkg

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.placette.EmpriseEtFrance
import io.github.pobsteta.surculus.core.placette.Totaux
import io.github.pobsteta.surculus.core.placette.UuidControle
import io.github.pobsteta.surculus.core.placette.UuidPlacette
import io.github.pobsteta.surculus.core.placette.Verdict
import io.github.pobsteta.surculus.core.session.ValeurControle
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.util.zip.GZIPInputStream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Mission 088, décision de Pascal du 2026-11-03 (point 3) : test de bout en bout. La sauvegarde v2
 * `fixtures/sursync/exemple-v2.sursync` (un contrôle ouvert sans position mesurée, un contrôle avec) est relue avec gzip
 * et JSON seulement (`gpkg` ne dépend pas de `data`). La `valeur` de chaque contrôle passe par le lecteur du journal
 * ([ValeurControle.lire]), est convertie en [ControleSaisi], puis exportée sur la tournée d'essai ([TourneeDeTest]).
 *
 * Écrit par le `testeur` sans lire le code de la sauvegarde, de la restauration ni de l'export : API tirée de
 * `ExportSansPositionTest`, `SessionEtExportCoucheyTest`, `TourneeDeTest` et des signatures publiques (`Saisies.kt`,
 * `ValeurControle`, `Totaux.deduire`).
 *
 * Concordance : la fixture désigne ses placettes par `11111111-…` (P35-01) et `33333333-…` (P35-02), à des piquets qui
 * ne sont pas ceux de [TourneeDeTest]. La fixture n'est pas modifiée : le test recale la tournée d'essai (UUID et piquet
 * de P35-01 et P35-02) sur ce que porte la sauvegarde, la position du piquet étant `position_x`/`position_y` de la
 * valeur. Coordonnées : Lambert-93 (EPSG:2154), x = Est, y = Nord, mètres.
 */
class SursyncV2ExportBoutEnBoutTest {
    @TempDir
    lateinit var dossier: Path

    private val racine: Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }.first { Files.exists(it.resolve("settings.gradle.kts")) }

    private val sansPosition = "22222222-2222-4222-8222-222222222201"
    private val avecPosition = "22222222-2222-4222-8222-222222222202"

    /** Lignes JSON décompressées de la sauvegarde, pied compris ; l'empreinte du pied est vérifiée au passage. */
    private fun lireSauvegarde(): List<JsonObject> {
        val octets = GZIPInputStream(Files.newInputStream(racine.resolve("fixtures/sursync/exemple-v2.sursync"))).use { it.readBytes() }
        val texte = String(octets, Charsets.UTF_8)
        val lignes = texte.split("\n").filter { it.isNotEmpty() }
        val pied = JsonParser.parseString(lignes.last()).asJsonObject
        val contenu = texte.substring(0, texte.length - lignes.last().length - 1)
        val empreinte = MessageDigest.getInstance("SHA-256").digest(contenu.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        assertEquals(pied.get("sha256").asString, empreinte, "la fixture est intègre")
        return lignes.map { JsonParser.parseString(it).asJsonObject }
    }

    /** Ce qu'une restauration rend au journal, converti vers ce que l'export attend (comme `SessionEtExportCoucheyTest`). */
    private fun versControle(objetUuid: String, c: ValeurControle): ControleSaisi = ControleSaisi(
        UuidControle(objetUuid), UuidPlacette(c.placetteUuid), Instant.ofEpochMilli(c.visiteLeUtcMs), LocalDate.parse(c.jourCivil),
        PointLambert93(c.positionX, c.positionY),
        if (c.sansPositionMesuree) {
            PositionMesuree(position = null, rattacheParIdentite = c.rattacheParIdentite, sansPositionMesuree = true)
        } else {
            PositionMesuree(PointLambert93(c.positionMesureeX!!, c.positionMesureeY!!), rattacheParIdentite = c.rattacheParIdentite)
        },
        emptyList(), totaux = (Totaux.deduire(emptyList()) as Verdict.Accepte).valeur, operateur = c.operateur,
    )

    /** La tournée d'essai, P35-01 et P35-02 recalées sur les UUID et les piquets de la sauvegarde. */
    private fun tournee(controles: List<Pair<String, ValeurControle>>): Path {
        val f = TourneeDeTest.couchey(dossier)
        Sqlite.ouvrir(f).use { cnx ->
            for ((_, c) in controles) {
                cnx.prepareStatement("UPDATE placettes SET uuid = ?, geom = ? WHERE code_placette = ?").use { ps ->
                    ps.setString(1, c.placetteUuid)
                    ps.setBytes(2, GeometrieGpkg.encoder(Geometrie.Point(PointLambert93(c.positionX, c.positionY)), 2154))
                    ps.setString(3, c.codePlacette)
                    assertEquals(1, ps.executeUpdate(), "placette ${c.codePlacette} dans la tournée d'essai")
                }
            }
        }
        return f
    }

    @Test
    fun `une sauvegarde v2 restauree avec un controle sans position s'exporte avec une geometrie NULL`() {
        val lignes = lireSauvegarde()
        val entete = lignes.first()
        assertEquals("sursync", entete.get("format").asString)
        assertEquals(2, entete.get("version").asInt)
        val saisies = lignes.drop(1).dropLast(1)
        assertEquals(entete.get("nb_lignes").asInt, saisies.size)

        // Le lecteur du journal relit chaque valeur v2.
        val controles = saisies.filter { it.get("type").asString == "controle" }.map { l ->
            l.get("objet_uuid").asString to assertNotNull(ValeurControle.lire(l.get("valeur").asString), "valeur v2 lisible par le journal")
        }
        assertEquals(2, controles.size)
        val lu = controles.toMap()
        assertTrue(lu.getValue(sansPosition).sansPositionMesuree)
        assertNull(lu.getValue(sansPosition).positionMesureeX)

        val source = tournee(controles)
        val sortie = dossier.resolve("export.gpkg")
        ExportTravaux.exporter(source, sortie, Saisies(operateur = "Agent d'essai", controles = controles.map { (u, c) -> versControle(u, c) }), TourneeDeTest.VERSION, TourneeDeTest.EXPORTE_LE)

        Sqlite.ouvrir(sortie, lectureSeule = true).use { cnx ->
            fun positions(controle: String) = cnx.lignes(
                "SELECT geom IS NULL AS geom_nulle, length(geom) AS taille, precision_m, sans_position_mesuree, rattache_par_identite " +
                    "FROM positions_controles WHERE controle_uuid = '$controle'",
            )

            // Contrôle sans position : une seule ligne, géométrie NULL (pas un POINT EMPTY), drapeaux vrais, précision NULL.
            val sp = positions(sansPosition).single()
            assertEquals(1, sp.entier("geom_nulle"), "géométrie NULL (GeoPackage 1.3, exigence 152)")
            assertNull(sp["taille"], "aucun blob, même un point vide")
            assertEquals(1, sp.entier("sans_position_mesuree"))
            assertEquals(1, sp.entier("rattache_par_identite"))
            assertNull(sp["precision_m"], "précision NULL, jamais 0 (Darwin Core)")

            // Son point de `controles` est le piquet de la placette, en France (pas en Somalie).
            val c = cnx.lignes("SELECT placette_uuid, geom FROM controles WHERE uuid = '$sansPosition'").single()
            assertEquals(lu.getValue(sansPosition).placetteUuid, c.texte("placette_uuid"))
            val p = assertNotNull(GeometrieGpkg.point(c.blob("geom")!!))
            assertEquals(846164.76, p.xMetres, 1e-6, "x (Est) du piquet de P35-01")
            assertEquals(6686123.26, p.yMetres, 1e-6, "y (Nord) du piquet de P35-01")
            assertTrue(EmpriseEtFrance.enFrance(p), "le contrôle tombe en France")

            // Contrôle avec position : sa géométrie mesurée est gardée.
            val ap = positions(avecPosition).single()
            assertEquals(0, ap.entier("geom_nulle"))
            assertEquals(0, ap.entier("sans_position_mesuree"))
            val g = cnx.lignes("SELECT geom FROM positions_controles WHERE controle_uuid = '$avecPosition'").single()
            val m = assertNotNull(GeometrieGpkg.point(g.blob("geom")!!))
            assertEquals(846201.5, m.xMetres, 1e-6, "x (Est) mesuré")
            assertEquals(6686152.0, m.yMetres, 1e-6, "y (Nord) mesuré")
            assertTrue(EmpriseEtFrance.enFrance(m))
        }
        assertEquals(2, Contenu.compter(sortie, "positions_controles"), "une ligne par contrôle")
    }
}
