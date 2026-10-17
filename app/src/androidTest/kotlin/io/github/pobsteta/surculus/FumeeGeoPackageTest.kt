package io.github.pobsteta.surculus

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.geo.Projections
import io.github.pobsteta.surculus.core.geo.UniteAzimut
import io.github.pobsteta.surculus.core.placette.CodeEssence
import io.github.pobsteta.surculus.core.placette.EtatDsf
import io.github.pobsteta.surculus.core.placette.EtatPlant
import io.github.pobsteta.surculus.core.placette.Observation
import io.github.pobsteta.surculus.core.placette.Plant
import io.github.pobsteta.surculus.core.placette.Totaux
import io.github.pobsteta.surculus.core.placette.UuidControle
import io.github.pobsteta.surculus.core.placette.UuidPlacette
import io.github.pobsteta.surculus.core.placette.UuidPlant
import io.github.pobsteta.surculus.core.placette.Verdict
import io.github.pobsteta.surculus.core.reperes.AzimutSaisi
import io.github.pobsteta.surculus.core.reperes.CodeEpsg
import io.github.pobsteta.surculus.core.reperes.PointDerive
import io.github.pobsteta.surculus.core.reperes.Resultat
import io.github.pobsteta.surculus.gpkg.ControleSaisi
import io.github.pobsteta.surculus.gpkg.ExportTravaux
import io.github.pobsteta.surculus.gpkg.Geometrie
import io.github.pobsteta.surculus.gpkg.GeometrieGpkg
import io.github.pobsteta.surculus.gpkg.LecteurTournee
import io.github.pobsteta.surculus.gpkg.LigneEtat
import io.github.pobsteta.surculus.gpkg.PlantNumerote
import io.github.pobsteta.surculus.gpkg.Saisies
import io.github.pobsteta.surculus.gpkg.Sqlite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant
import java.time.LocalDate

/**
 * Essai de fumée de l'APK release (ADR 0011), joué sur émulateur x86_64 par le workflow `release.yml` : la release
 * ne part pas s'il échoue. Il écrit une tournée de démonstration avec le code de l'application (sqlite-jdbc et ses
 * natifs Android, les fonctions ST_* déclarées en Kotlin, l'index rtree) puis la relit. Ce que R8, les natifs
 * `libsqlitejdbc.so` ou le désucrage casseraient apparaît ici, pas chez l'agent de terrain.
 *
 * Limite assumée : x86_64 seulement. Le chemin ARM (téléphones) n'est jamais exécuté en CI ; Pascal l'essaie
 * sur son téléphone.
 */
@RunWith(AndroidJUnit4::class)
class FumeeGeoPackageTest {
    private val travauxId = "11111111-1111-4111-8111-111111111111"
    private val placetteId = "00000000-0000-4000-8000-000000000101"

    @Test
    fun ecrireUneTourneeDeDemonstrationPuisLaRelire() {
        val instr = InstrumentationRegistry.getInstrumentation()
        val dossier = File(instr.targetContext.cacheDir, "fumee").apply { deleteRecursively(); mkdirs() }
        val depart = File(dossier, "travaux.gpkg")
        instr.context.assets.open("travaux-sommier-0.31.1.gpkg").use { e -> depart.outputStream().use { e.copyTo(it) } }

        // Les natifs sont là, SQLite a R*Tree : sans lui, les déclencheurs de sommieR ne tiendraient pas.
        Sqlite.ouvrir(depart.toPath()).use { cnx ->
            cnx.createStatement().use { st ->
                st.executeQuery("PRAGMA compile_options").use { rs ->
                    val options = generateSequence { if (rs.next()) rs.getString(1) else null }.toList()
                    assertTrue("SQLite sans R*Tree : $options", options.any { it.contains("ENABLE_RTREE") })
                }
            }
        }

        // Couchey, placette P35-01 (R/demo.R de sommieR : longitude 4,93250, latitude 47,25910).
        val (x, y) = Projections.versLambert93(lon = 4.93250, lat = 47.25910)
        val piquet = PointLambert93(xMetres = x, yMetres = y)
        val anneau = listOf(
            PointLambert93(x - 300, y - 300), PointLambert93(x + 300, y - 300), PointLambert93(x + 300, y + 300),
            PointLambert93(x - 300, y + 300), PointLambert93(x - 300, y - 300),
        )
        Sqlite.ouvrir(depart.toPath()).use { cnx ->
            val foret = GeometrieGpkg.encoder(Geometrie.MultiPolygone(listOf(Geometrie.Polygone(listOf(anneau)))), 2154)
            cnx.prepareStatement("INSERT INTO foret (geom, nom) VALUES (?, 'Couchey (fumée)')").use { it.setBytes(1, foret); it.executeUpdate() }
            cnx.prepareStatement(
                "INSERT INTO placettes (geom, uuid, code_placette, travaux_id, rayon_m, ug) VALUES (?, ?, 'P35-01', ?, 3.99, '35')",
            ).use {
                it.setBytes(1, GeometrieGpkg.encoder(Geometrie.Point(piquet), 2154))
                it.setString(2, placetteId)
                it.setString(3, travauxId)
                it.executeUpdate()
            }
        }

        // Un contrôle de deux plants, pointés depuis le piquet par la chaîne des repères de :core.
        fun plant(n: Int, distanceM: Double, azimutDeg: Double): Plant {
            val az = (AzimutSaisi.valider(azimutDeg, UniteAzimut.DEGRES) as Resultat.Accepte).valeur
            val derive = (PointDerive.deriver(piquet, CodeEpsg.RGF93_V1_LAMBERT93, distanceM, az, LocalDate.of(2026, 10, 20)) as Resultat.Accepte).valeur
            return Plant(UuidPlant("00000000-0000-4000-8000-00000000100$n"), UuidPlacette(placetteId), CodeEssence("CHS"), null, Instant.parse("2026-10-20T08:00:00Z"), derive)
        }
        val p1 = plant(1, 1.5, 30.0)
        val p2 = plant(2, 2.5, 200.0)
        val etats = listOf(
            LigneEtat("00000000-0000-4000-8000-000000002001", p1.uuid, Observation.Observe(EtatDsf.SAIN, hauteurCm = 40)),
            LigneEtat("00000000-0000-4000-8000-000000002002", p2.uuid, Observation.Observe(EtatDsf.MORT)),
        )
        val totaux = (Totaux.deduire(etats.map { EtatPlant(it.plant, it.observation) }) as Verdict.Accepte).valeur
        val saisies = Saisies(
            operateur = "Fumée",
            controles = listOf(
                ControleSaisi(
                    UuidControle("00000000-0000-4000-8000-000000003001"), UuidPlacette(placetteId),
                    Instant.parse("2026-10-20T09:00:00Z"), piquet, etats = etats, totaux = totaux,
                ),
            ),
            plants = listOf(PlantNumerote(p1, 1), PlantNumerote(p2, 2)),
        )

        val export = File(dossier, "export.gpkg")
        val bilan = ExportTravaux.exporter(depart.toPath(), export.toPath(), saisies, "fumee", Instant.parse("2026-10-20T12:00:00Z"))
        assertEquals(1, bilan.controles)
        assertEquals(2, bilan.plants)
        assertEquals(2, bilan.etats)

        // Relecture : la tournée rendue se lit, avec ses couches, ses plants et son repère.
        val relue = LecteurTournee.lire(export.toPath())
        assertEquals(2154, relue.epsg.code)
        assertEquals(1, relue.controles.size)
        assertEquals(listOf(1, 2), relue.plants.map { it.numero })
        assertEquals(setOf("plants", "etats_plants", "positions_controles", "constats_reprise", "vides", "projet"), relue.couchesAjoutees.toSet())
        assertEquals("fumee", relue.projet?.versionSurculus)
    }
}
