package io.github.pobsteta.surculus.gpkg

import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.geo.Projections
import io.github.pobsteta.surculus.core.geo.UniteAzimut
import io.github.pobsteta.surculus.core.placette.JourCivil
import io.github.pobsteta.surculus.core.placette.CodeEssence
import io.github.pobsteta.surculus.core.placette.EtatDsf
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
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.LocalDate

/**
 * Tournée de départ des tests : le modèle `inst/qgis/travaux.gpkg` de sommieR 0.31.1 (copié tel quel dans
 * `src/test/resources/modele/`), rempli comme `sommier_projet_qfield_travaux()` remplit la démo Couchey :
 * trois placettes P35-01 à P35-03 de 50 m² (centres de `R/demo.R` l. 366-367, longitude/latitude WGS 84),
 * dans la plantation de l'UG 35.
 */
object TourneeDeTest {
    init {
        // Tests seulement : pas de fsync (voir Sqlite.sansSynchronisationDisque).
        Sqlite.sansSynchronisationDisque = true
    }

    const val MODELE = "/modele/travaux-sommier-0.31.1.gpkg"
    const val TRAVAUX_ID = "11111111-1111-4111-8111-111111111111"

    fun uuid(n: Int) = "00000000-0000-4000-8000-%012x".format(n)

    /** Centres de la démo Couchey : (longitude, latitude) en degrés WGS 84, dans l'ordre de `R/demo.R`. */
    val CENTRES_LON_LAT = listOf(4.93250 to 47.25910, 4.93300 to 47.25940, 4.93350 to 47.25900)

    fun centre(k: Int): PointLambert93 {
        val (x, y) = Projections.versLambert93(lon = CENTRES_LON_LAT[k].first, lat = CENTRES_LON_LAT[k].second)
        return PointLambert93(xMetres = x, yMetres = y)
    }

    fun placetteUuid(k: Int) = uuid(100 + k)
    fun code(k: Int) = "P35-%02d".format(k + 1)

    fun copierModele(dest: Path): Path {
        TourneeDeTest::class.java.getResourceAsStream(MODELE)!!.use { Files.copy(it, dest, StandardCopyOption.REPLACE_EXISTING) }
        return dest
    }

    /** Le modèle de sommieR, vide (aucune forêt, aucune placette). */
    fun modeleVide(dossier: Path): Path = copierModele(dossier.resolve("modele.gpkg"))

    /** La tournée de départ de Couchey : forêt, UG, suivis, codes, trois placettes. `epsg` : 2154 ou 9794. */
    fun couchey(dossier: Path, epsg: Int = 2154, nom: String = "tournee-sans.gpkg"): Path {
        val f = copierModele(dossier.resolve(nom))
        Sqlite.ouvrir(f).use { cnx ->
            if (epsg != 2154) basculerEn(cnx, epsg)
            val c = (0..2).map(::centre)
            val x0 = c.minOf { it.xMetres } - 300
            val x1 = c.maxOf { it.xMetres } + 300
            val y0 = c.minOf { it.yMetres } - 300
            val y1 = c.maxOf { it.yMetres } + 300
            val anneau = listOf(x0 to y0, x1 to y0, x1 to y1, x0 to y1, x0 to y0).map { PointLambert93(it.first, it.second) }
            val multi = GeometrieGpkg.encoder(Geometrie.MultiPolygone(listOf(Geometrie.Polygone(listOf(anneau)))), epsg)
            cnx.prepareStatement("INSERT INTO foret (geom, nom) VALUES (?, 'Forêt de Couchey (démo)')").use { it.setBytes(1, multi); it.executeUpdate() }
            cnx.prepareStatement("INSERT INTO ug (geom, numero) VALUES (?, '35')").use { it.setBytes(1, multi); it.executeUpdate() }
            cnx.createStatement().use { st ->
                st.executeUpdate("INSERT INTO suivis (id, libelle, ug) VALUES ('$TRAVAUX_ID', 'Plantation 2022', '35')")
                st.executeUpdate("INSERT INTO codes (code, libelle, unites, formes) VALUES ('PL', 'Plantation', 'ha', 'surface')")
            }
            for (k in 0..2) {
                cnx.prepareStatement(
                    "INSERT INTO placettes (geom, uuid, code_placette, travaux_id, rayon_m, materialisation, ug) VALUES (?, ?, ?, ?, 3.99, 'piquet bois', '35')",
                ).use { ps ->
                    ps.setBytes(1, GeometrieGpkg.encoder(Geometrie.Point(c[k]), epsg))
                    ps.setString(2, placetteUuid(k)); ps.setString(3, code(k)); ps.setString(4, TRAVAUX_ID)
                    ps.executeUpdate()
                }
            }
        }
        return f
    }

    /** Passe toutes les couches à géométrie en [epsg] (9794) : le tout, comme le ferait une version du modèle de sommieR. */
    private fun basculerEn(cnx: java.sql.Connection, epsg: Int) {
        cnx.createStatement().use { st ->
            // Définition factice : le test ne dépend pas du texte WKT, seulement du code.
            st.executeUpdate("INSERT INTO gpkg_spatial_ref_sys VALUES ('RGF93 v2b / Lambert-93 (essai)', $epsg, 'EPSG', $epsg, 'undefined', NULL)")
            st.executeUpdate("UPDATE gpkg_geometry_columns SET srs_id = $epsg")
            st.executeUpdate("UPDATE gpkg_contents SET srs_id = $epsg WHERE data_type = 'features'")
        }
    }

    // ------------------------------------------------------------------ saisies

    val JOUR = LocalDate.of(2026, 10, 20)
    val INSTANT_POINTAGE: Instant = Instant.parse("2026-10-20T08:15:00Z")
    val INSTANT_CONTROLE: Instant = Instant.parse("2026-10-20T09:30:00Z")

    /** Plant pointé depuis le piquet de la placette [k] (core.reperes : chaîne WMM2025, convergence, Lambert-93). */
    fun plant(n: Int, k: Int, distanceM: Double, azimutDeg: Double, essence: String? = "CHS", epsg: CodeEpsg = CodeEpsg.RGF93_V1_LAMBERT93, remplace: UuidPlant? = null): Plant {
        val az = (AzimutSaisi.valider(azimutDeg, UniteAzimut.DEGRES) as io.github.pobsteta.surculus.core.reperes.Resultat.Accepte).valeur
        val derive = (PointDerive.deriver(centre(k), epsg, distanceM, az, JOUR) as io.github.pobsteta.surculus.core.reperes.Resultat.Accepte).valeur
        return Plant(UuidPlant(uuid(1000 + n)), UuidPlacette(placetteUuid(k)), essence?.let(::CodeEssence), remplace, INSTANT_POINTAGE, derive)
    }

    /**
     * Saisies d'une journée : contrôle de P35-01 (4 plants : sain, atteint abrouti de 40 cm, mort, absent) et de
     * P35-02 (2 plants sains), avec leur position mesurée. Totaux déduits des états, comme l'application.
     */
    fun saisiesDeLaJournee(epsg: CodeEpsg = CodeEpsg.RGF93_V1_LAMBERT93): Saisies {
        val p1 = listOf(
            plant(1, 0, 1.2, 10.0, epsg = epsg), plant(2, 0, 2.0, 100.0, epsg = epsg),
            plant(3, 0, 2.5, 200.0, epsg = epsg), plant(4, 0, 3.0, 300.0, epsg = epsg),
        )
        val p2 = listOf(plant(5, 1, 1.5, 45.0, epsg = epsg), plant(6, 1, 2.2, 225.0, epsg = epsg))
        val etats1 = listOf(
            LigneEtat(uuid(2001), p1[0].uuid, Observation.Observe(EtatDsf.SAIN, hauteurCm = 35)),
            LigneEtat(uuid(2002), p1[1].uuid, Observation.Observe(EtatDsf.ATTEINT, causeDetail = "abroutissement", hauteurCm = 40)),
            LigneEtat(uuid(2003), p1[2].uuid, Observation.Observe(EtatDsf.MORT, causeGroupe = io.github.pobsteta.surculus.core.placette.GroupeCause.ANIMAUX, causeDetail = "cervides")),
            LigneEtat(uuid(2004), p1[3].uuid, Observation.Observe(EtatDsf.ABSENT)),
        )
        val etats2 = listOf(
            LigneEtat(uuid(2005), p2[0].uuid, Observation.Observe(EtatDsf.SAIN, hauteurCm = 50)),
            LigneEtat(uuid(2006), p2[1].uuid, Observation.NonObserve),
        )
        fun totaux(e: List<LigneEtat>) =
            (Totaux.deduire(e.map { io.github.pobsteta.surculus.core.placette.EtatPlant(it.plant, it.observation) }) as Verdict.Accepte).valeur
        val c1 = ControleSaisi(
            UuidControle(uuid(3001)), UuidPlacette(placetteUuid(0)), INSTANT_CONTROLE, JourCivil.de(INSTANT_CONTROLE), centre(0),
            PositionMesuree(centre(0), precisionM = 0.4, sourceGnss = "essai", referentielSource = "RGF93", simulee = true),
            etats1, totaux = totaux(etats1), operateur = "Agent d'essai", concurrence = "moyenne", besoin = "aucun",
        )
        val c2 = ControleSaisi(
            UuidControle(uuid(3002)), UuidPlacette(placetteUuid(1)), INSTANT_CONTROLE.plusSeconds(3600), JourCivil.de(INSTANT_CONTROLE.plusSeconds(3600)), centre(1),
            null, etats2, totaux = totaux(etats2), operateur = "Agent d'essai",
        )
        return Saisies(
            operateur = "Agent d'essai",
            controles = listOf(c1, c2),
            plants = (p1 + p2).mapIndexed { i, p -> PlantNumerote(p, if (i < 4) i + 1 else i - 3) },
        )
    }

    val VERSION = "0.6.0"
    val EXPORTE_LE: Instant = Instant.parse("2026-10-20T12:00:00Z")
}
