/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.gpkg

import io.github.pobsteta.surculus.core.placette.Essences
import io.github.pobsteta.surculus.core.placette.Observation
import io.github.pobsteta.surculus.core.placette.Observations
import io.github.pobsteta.surculus.core.reperes.CodesUniteAzimut
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.sql.Connection
import java.sql.SQLException
import java.time.Instant

/**
 * Écriture de l'export `travaux.gpkg` (ADR 0007, ADR 0011).
 *
 * L'export part du fichier de la tournée (aller de sommieR 0.31.1, ou tournée rendue précédemment) et lui
 * ajoute, dans une seule transaction :
 * - des lignes dans `controles` et `placettes` de sommieR, **sans toucher à leur schéma** ;
 * - les six couches de Surculus (`plants`, `etats_plants`, `positions_controles`, `constats_reprise`, `vides`,
 *   `projet`), enregistrées dans `gpkg_contents` (`features` ou `attributes`), `gpkg_geometry_columns`, avec
 *   index rtree et extension Schema pour les valeurs permises.
 *
 * Tout ou rien : un refus ([GpkgRefuse]) ne laisse aucun fichier de destination à moitié écrit.
 * Déterministe : à état égal (et même [exporteLe]), deux exports ont le même contenu ; les lignes sont rangées
 * par identifiant, jamais par ordre de saisie. Rejouable : une ligne déjà présente à l'identique est ignorée.
 */
object ExportTravaux {
    /**
     * @param source tournée de départ (n'est jamais modifiée).
     * @param destination fichier `travaux.gpkg` écrit ; remplacé s'il existe.
     * @param versionSurculus version unique du fichier, écrite dans `projet.version_surculus`.
     * @param exporteLe horloge injectée : écrite dans `gpkg_contents.last_change`, jamais lue ici.
     * @param essences si donné, les codes d'essence des plants sont vérifiés (`docs/referentiels/essences.csv`).
     */
    fun exporter(
        source: Path,
        destination: Path,
        saisies: Saisies,
        versionSurculus: String,
        exporteLe: Instant,
        essences: Essences? = null,
    ): BilanExport {
        val tournee = LecteurTournee.lire(source)
        ValidationExport.verifier(tournee, saisies, essences)
        val tmp = destination.toAbsolutePath().resolveSibling(destination.fileName.toString() + ".tmp")
        Files.createDirectories(tmp.parent)
        try {
            Files.copy(source, tmp, StandardCopyOption.REPLACE_EXISTING)
            val bilan = Sqlite.ouvrir(tmp).use { cnx ->
                cnx.autoCommit = false
                try {
                    val b = Ecrivain(cnx, tournee, exporteLe).ecrire(saisies, versionSurculus)
                    cnx.commit()
                    b
                } catch (e: Throwable) {
                    cnx.rollback()
                    throw e
                }
            }
            try {
                Files.move(tmp, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (e: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(tmp, destination, StandardCopyOption.REPLACE_EXISTING)
            }
            return bilan
        } catch (e: SQLException) {
            throw GpkgRefuse(RefusGpkg.PasUnGeoPackage(e.message ?: "écriture impossible"))
        } finally {
            Files.deleteIfExists(tmp)
        }
    }
}

private class Ecrivain(private val cnx: Connection, private val tournee: Tournee, private val exporteLe: Instant) {
    private val srs = tournee.srsId
    private val horodatage = ValidationExport.datetime(exporteLe)
    private var dejaPresentes = 0

    private val compteurs = HashMap<String, Int>()
    private fun compter(couche: String) {
        compteurs[couche] = (compteurs[couche] ?: 0) + 1
    }

    fun ecrire(saisies: Saisies, versionSurculus: String): BilanExport {
        creerSchema()
        ecrireProjet(saisies.operateur, versionSurculus)
        for (p in saisies.placettesInstallees.sortedBy { it.uuid.valeur }) {
            val ecrit = inserer(
                "placettes", "uuid",
                linkedMapOf(
                    "geom" to blobPoint(p.position),
                    "uuid" to p.uuid.valeur,
                    "code_placette" to p.code,
                    "travaux_id" to p.travauxId,
                    "rayon_m" to p.rayonM,
                    "materialisation" to p.materialisation,
                    "ug" to p.ug,
                ),
            )
            if (ecrit) compter("placettes")
        }
        for (c in saisies.controles.sortedBy { it.uuid.valeur }) ecrireControle(c)
        for (t in saisies.travaux.sortedBy { it.uuid }) {
            val couche = if (t.geometrie is Geometrie.Point) "travaux_pt" else "travaux_surf"
            val ecrit = inserer(
                couche, "uuid",
                linkedMapOf(
                    "geom" to blob(t.geometrie),
                    "uuid" to t.uuid,
                    "code_travaux" to t.codeTravaux,
                    "visite_le" to t.visiteLe?.let(ValidationExport::datetime),
                    "operateur" to t.operateur,
                ),
            )
            if (ecrit) compter(couche)
        }
        for (pn in saisies.plants.sortedWith(compareBy({ it.plant.placette.valeur }, { it.numero }, { it.plant.uuid.valeur }))) {
            val p = pn.plant
            val d = p.derive
            val ecrit = inserer(
                "plants", "uuid",
                linkedMapOf(
                    "geom" to blobPoint(d.position),
                    "uuid" to p.uuid.valeur,
                    "placette_uuid" to p.placette.valeur,
                    "numero" to pn.numero,
                    "essence" to p.essence?.code,
                    "regarni" to p.regarni,
                    "remplace_uuid" to p.remplace?.valeur,
                    "pointe_le" to ValidationExport.datetime(p.pointeLe),
                    "distance_m" to d.distanceHorizontaleM,
                    "azimut_mag" to d.azimut.valeur,
                    "unite_azimut" to CodesUniteAzimut.code(d.azimut.unite),
                    "declinaison_deg" to d.orientation.declinaisonDeg,
                    "convergence_deg" to d.orientation.convergenceDeg,
                    "azimut_geo_deg" to d.orientation.azimutGeographiqueDeg,
                    "modele_geomagnetique" to Couches.MODELE_GEOMAGNETIQUE,
                ),
            )
            if (ecrit) compter("plants")
        }
        for (k in saisies.constats.sortedBy { it.uuid }) {
            val ecrit = inserer(
                "constats_reprise", "uuid",
                linkedMapOf(
                    "geom" to blob(k.surface),
                    "uuid" to k.uuid,
                    "methode" to k.methode.code,
                    "pas_lignes" to k.pasLignes,
                    "departs" to k.departsJson,
                    "essence" to k.essence,
                    "nb_plants_chantier" to k.nbPlantsChantier,
                    "nb_sains" to k.nbSains,
                    "nb_atteints" to k.nbAtteints,
                    "nb_morts" to k.nbMorts,
                    "nb_absents" to k.nbAbsents,
                    "taux_brut_pct" to k.tauxBrutPct,
                    "ic_bas_pct" to k.icBasPct,
                    "ic_haut_pct" to k.icHautPct,
                    "seuil_pct" to k.seuilPct,
                    "seuil_source" to k.seuilSource,
                    "alerte" to k.alerte,
                    "visite_le" to k.visiteLe?.let(ValidationExport::datetime),
                    "operateur" to k.operateur,
                ),
            )
            if (ecrit) compter("constats_reprise")
        }
        for (v in saisies.vides.sortedBy { it.uuid }) {
            val ecrit = inserer(
                "vides", "uuid",
                linkedMapOf(
                    "geom" to blob(v.surface),
                    "uuid" to v.uuid,
                    "constat_uuid" to v.constatUuid,
                    "placette_uuid" to v.placetteUuid,
                    "surface_are" to v.surfaceAre,
                    "origine" to v.origine.code,
                    "visite_le" to v.visiteLe?.let(ValidationExport::datetime),
                    "operateur" to v.operateur,
                ),
            )
            if (ecrit) compter("vides")
        }
        finaliser()
        return BilanExport(
            exporteLe = exporteLe,
            placettesInstallees = compteurs["placettes"] ?: 0,
            controles = compteurs["controles"] ?: 0,
            plants = compteurs["plants"] ?: 0,
            etats = compteurs["etats_plants"] ?: 0,
            positions = compteurs["positions_controles"] ?: 0,
            constats = compteurs["constats_reprise"] ?: 0,
            vides = compteurs["vides"] ?: 0,
            dejaPresentes = dejaPresentes,
        )
    }

    private fun blobPoint(p: io.github.pobsteta.surculus.core.geo.PointLambert93) = blob(Geometrie.Point(p))
    private fun blob(g: Geometrie): ByteArray = GeometrieGpkg.encoder(g, srs)

    private fun ecrireControle(c: ControleSaisi) {
        val t = c.totaux
        val ecrit = inserer(
            "controles", "uuid",
            linkedMapOf(
                "geom" to blobPoint(c.position),
                "uuid" to c.uuid.valeur,
                "placette_uuid" to c.placette.valeur,
                "nb_total" to t.nbTotal,
                "nb_vivants" to t.nbVivants,
                "h_moy_cm" to t.hMoyCm,
                "nb_abroutis" to t.nbAbroutis,
                "concurrence" to c.concurrence,
                "besoin" to c.besoin,
                "visite_le" to ValidationExport.datetime(c.visiteLe),
                "operateur" to c.operateur,
                "observations" to c.observations,
            ),
        )
        if (ecrit) compter("controles")
        c.positionMesuree?.let { m ->
            val ecritP = inserer(
                "positions_controles", "controle_uuid",
                linkedMapOf(
                    "geom" to blobPoint(m.position),
                    "controle_uuid" to c.uuid.valeur,
                    "precision_m" to m.precisionM,
                    "source_gnss" to m.sourceGnss,
                    "referentiel_source" to m.referentielSource,
                    "simulee" to m.simulee,
                    "rattache_par_identite" to m.rattacheParIdentite,
                ),
            )
            if (ecritP) compter("positions_controles")
        }
        for (e in c.etats.sortedWith(compareBy({ it.plant.valeur }, { it.uuid }))) {
            val o = e.observation
            val ecritE = inserer(
                "etats_plants", "uuid",
                linkedMapOf(
                    "uuid" to e.uuid,
                    "controle_uuid" to c.uuid.valeur,
                    "plant_uuid" to e.plant.valeur,
                    "etat" to when (o) {
                        is Observation.Observe -> o.etat.code
                        Observation.NonObserve -> Observations.CODE_NON_OBSERVE
                    },
                    "cause_groupe" to (o as? Observation.Observe)?.causeGroupe?.code,
                    "cause_detail" to (o as? Observation.Observe)?.causeDetail,
                    "hauteur_cm" to (o as? Observation.Observe)?.hauteurCm,
                ),
            )
            if (ecritE) compter("etats_plants")
        }
    }

    /**
     * Insère une ligne, sauf si l'identifiant [cle] est déjà là : identique, elle est ignorée (rejeu) ; différente,
     * c'est un refus. Rend `true` si la ligne a été écrite.
     */
    private fun inserer(table: String, cle: String, valeurs: Map<String, Any?>): Boolean {
        val existante = cnx.prepareStatement("SELECT * FROM ${table.ident()} WHERE ${cle.ident()} = ?").use { ps ->
            ps.setString(1, valeurs[cle] as String)
            ps.executeQuery().use { rs ->
                if (!rs.next()) {
                    null
                } else {
                    val meta = rs.metaData
                    (1..meta.columnCount).associate { meta.getColumnLabel(it) to rs.getObject(it) }
                }
            }
        }
        if (existante != null) {
            val identique = valeurs.all { (n, v) -> memeValeur(existante[n], v) }
            if (!identique) throw GpkgRefuse(RefusGpkg.UuidEnDouble(table, valeurs[cle] as String))
            dejaPresentes++
            return false
        }
        val noms = valeurs.keys.toList()
        cnx.prepareStatement(
            "INSERT INTO ${table.ident()} (${noms.joinToString(", ") { it.ident() }}) VALUES (${noms.joinToString(", ") { "?" }})",
        ).use { ps ->
            noms.forEachIndexed { i, n ->
                when (val v = valeurs[n]) {
                    null -> ps.setObject(i + 1, null)
                    is Boolean -> ps.setInt(i + 1, if (v) 1 else 0)
                    is Int -> ps.setInt(i + 1, v)
                    is Double -> ps.setDouble(i + 1, v)
                    is String -> ps.setString(i + 1, v)
                    is ByteArray -> ps.setBytes(i + 1, v)
                    else -> error("Type non pris en charge pour $table.$n : ${v::class}")
                }
            }
            ps.executeUpdate()
        }
        return true
    }

    private fun memeValeur(dansLeFichier: Any?, voulue: Any?): Boolean = when {
        voulue == null -> dansLeFichier == null
        dansLeFichier == null -> false
        voulue is Boolean -> (dansLeFichier as? Number)?.toInt() == (if (voulue) 1 else 0)
        voulue is Number -> (dansLeFichier as? Number)?.toDouble() == voulue.toDouble()
        voulue is ByteArray -> dansLeFichier is ByteArray && dansLeFichier.contentEquals(voulue)
        else -> dansLeFichier.toString() == voulue.toString()
    }

    // ----------------------------------------------------------------- schéma

    private fun executer(sql: String) {
        cnx.createStatement().use { it.execute(sql) }
    }

    private fun creerSchema() {
        creerExtensionSchema()
        for (c in Couches.TOUTES) {
            val existait = cnx.existeTable(c.nom)
            if (!existait) executer(Couches.ddl(c))
            enregistrer(c)
            if (c.geometrie != null) creerIndexRtree(c.nom)
            declarerColonnes(c)
        }
    }

    private fun creerExtensionSchema() {
        executer(
            "CREATE TABLE IF NOT EXISTS gpkg_data_columns (table_name TEXT NOT NULL, column_name TEXT NOT NULL, name TEXT, title TEXT, " +
                "description TEXT, mime_type TEXT, constraint_name TEXT, CONSTRAINT pk_gdc PRIMARY KEY (table_name, column_name), " +
                "CONSTRAINT gdc_tn UNIQUE (table_name, name), " +
                "CONSTRAINT fk_gdc_tn FOREIGN KEY (table_name) REFERENCES gpkg_contents(table_name))",
        )
        executer(
            "CREATE TABLE IF NOT EXISTS gpkg_data_column_constraints (constraint_name TEXT NOT NULL, constraint_type TEXT NOT NULL, " +
                "value TEXT, min NUMERIC, min_is_inclusive BOOLEAN, max NUMERIC, max_is_inclusive BOOLEAN, description TEXT, " +
                "CONSTRAINT gdcc_ntv UNIQUE (constraint_name, constraint_type, value))",
        )
        for (t in listOf("gpkg_data_columns", "gpkg_data_column_constraints")) {
            extension(t, null, "gpkg_schema", "http://www.geopackage.org/spec/#extension_schema", "read-write")
        }
    }

    private fun extension(table: String, colonne: String?, nom: String, definition: String, portee: String) {
        cnx.prepareStatement(
            "INSERT INTO gpkg_extensions (table_name, column_name, extension_name, definition, scope) " +
                "SELECT ?, ?, ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM gpkg_extensions WHERE table_name = ? AND column_name IS ? AND extension_name = ?)",
        ).use { ps ->
            ps.setString(1, table); ps.setString(2, colonne); ps.setString(3, nom); ps.setString(4, definition); ps.setString(5, portee)
            ps.setString(6, table); ps.setString(7, colonne); ps.setString(8, nom)
            ps.executeUpdate()
        }
    }

    /** `gpkg_contents` (Req. 118), `gpkg_geometry_columns` (Req. 21), `gpkg_ogr_contents` si le fichier la tient. */
    private fun enregistrer(c: CoucheAjoutee) {
        val type = if (c.geometrie != null) "features" else "attributes"
        cnx.prepareStatement(
            "INSERT OR IGNORE INTO gpkg_contents (table_name, data_type, identifier, description, last_change, srs_id) VALUES (?, ?, ?, '', ?, ?)",
        ).use { ps ->
            ps.setString(1, c.nom); ps.setString(2, type); ps.setString(3, c.nom); ps.setString(4, horodatage)
            ps.setInt(5, if (c.geometrie != null) srs else 0)
            ps.executeUpdate()
        }
        if (c.geometrie != null) {
            cnx.prepareStatement("INSERT OR IGNORE INTO gpkg_geometry_columns (table_name, column_name, geometry_type_name, srs_id, z, m) VALUES (?, 'geom', ?, ?, 0, 0)").use { ps ->
                ps.setString(1, c.nom); ps.setString(2, c.geometrie); ps.setInt(3, srs)
                ps.executeUpdate()
            }
        }
    }

    /** Index spatial de l'annexe F.3 d'OGC 12-128, avec les mêmes déclencheurs que ceux de sommieR (GDAL). */
    private fun creerIndexRtree(t: String) {
        val r = "rtree_${t}_geom"
        if (cnx.existeTable(r)) return
        val tq = t.ident()
        val rq = r.ident()
        executer("CREATE VIRTUAL TABLE $rq USING rtree(\"id\", \"minx\", \"maxx\", \"miny\", \"maxy\")")
        val valeurs = "VALUES (NEW.\"fid\",ST_MinX(NEW.\"geom\"), ST_MaxX(NEW.\"geom\"),ST_MinY(NEW.\"geom\"), ST_MaxY(NEW.\"geom\"))"
        executer("CREATE TRIGGER ${"${r}_insert".ident()} AFTER INSERT ON $tq WHEN (new.\"geom\" NOT NULL AND NOT ST_IsEmpty(NEW.\"geom\")) BEGIN INSERT OR REPLACE INTO $rq $valeurs; END")
        executer("CREATE TRIGGER ${"${r}_update1".ident()} AFTER UPDATE OF \"geom\" ON $tq WHEN OLD.\"fid\" = NEW.\"fid\" AND (NEW.\"geom\" NOTNULL AND NOT ST_IsEmpty(NEW.\"geom\")) BEGIN INSERT OR REPLACE INTO $rq $valeurs; END")
        executer("CREATE TRIGGER ${"${r}_update2".ident()} AFTER UPDATE OF \"geom\" ON $tq WHEN OLD.\"fid\" = NEW.\"fid\" AND (NEW.\"geom\" ISNULL OR ST_IsEmpty(NEW.\"geom\")) BEGIN DELETE FROM $rq WHERE id = OLD.\"fid\"; END")
        executer("CREATE TRIGGER ${"${r}_update3".ident()} AFTER UPDATE ON $tq WHEN OLD.\"fid\" != NEW.\"fid\" AND (NEW.\"geom\" NOTNULL AND NOT ST_IsEmpty(NEW.\"geom\")) BEGIN DELETE FROM $rq WHERE id = OLD.\"fid\"; INSERT OR REPLACE INTO $rq $valeurs; END")
        executer("CREATE TRIGGER ${"${r}_update4".ident()} AFTER UPDATE ON $tq WHEN OLD.\"fid\" != NEW.\"fid\" AND (NEW.\"geom\" ISNULL OR ST_IsEmpty(NEW.\"geom\")) BEGIN DELETE FROM $rq WHERE id IN (OLD.\"fid\", NEW.\"fid\"); END")
        executer("CREATE TRIGGER ${"${r}_delete".ident()} AFTER DELETE ON $tq WHEN old.\"geom\" NOT NULL BEGIN DELETE FROM $rq WHERE id = OLD.\"fid\"; END")
        extension(t, "geom", "gpkg_rtree_index", "http://www.geopackage.org/spec120/#extension_rtree", "write-only")
    }

    /** Extension Schema : titre, description et valeurs permises de chaque colonne (voyagent avec le fichier). */
    private fun declarerColonnes(c: CoucheAjoutee) {
        for (col in c.colonnes) {
            col.contrainte?.let { k ->
                for (v in k.valeurs) {
                    cnx.prepareStatement(
                        "INSERT OR IGNORE INTO gpkg_data_column_constraints (constraint_name, constraint_type, value, description) VALUES (?, 'enum', ?, ?)",
                    ).use { ps ->
                        ps.setString(1, k.nom); ps.setString(2, v); ps.setString(3, k.description)
                        ps.executeUpdate()
                    }
                }
            }
            cnx.prepareStatement(
                "INSERT OR IGNORE INTO gpkg_data_columns (table_name, column_name, name, title, description, mime_type, constraint_name) VALUES (?, ?, ?, ?, ?, NULL, ?)",
            ).use { ps ->
                ps.setString(1, c.nom); ps.setString(2, col.nom); ps.setString(3, col.nom); ps.setString(4, col.titre)
                ps.setString(5, col.description); ps.setString(6, col.contrainte?.nom)
                ps.executeUpdate()
            }
        }
    }

    // ----------------------------------------------------------------- projet

    /**
     * Une seule ligne, la **version unique** du fichier. Les champs de sommieR (`nom`, `foret`, `prepare_le`,
     * `version_sommier`) restent vides tant que la tournée de départ ne les porte pas (arbitrage de Pascal du
     * 2026-10-17) et ne sont jamais écrasés ; Surculus écrit les siens (`operateur`, `version_surculus`).
     */
    private fun ecrireProjet(operateur: String?, versionSurculus: String) {
        val n = cnx.lignes("SELECT count(*) AS n FROM \"projet\"").first().entier("n")!!
        if (n == 0) {
            cnx.prepareStatement("INSERT INTO \"projet\" (operateur, version_surculus) VALUES (?, ?)").use { ps ->
                ps.setString(1, operateur); ps.setString(2, versionSurculus)
                ps.executeUpdate()
            }
        } else {
            cnx.prepareStatement("UPDATE \"projet\" SET operateur = ?, version_surculus = ? WHERE fid = (SELECT min(fid) FROM \"projet\")").use { ps ->
                ps.setString(1, operateur); ps.setString(2, versionSurculus)
                ps.executeUpdate()
            }
        }
    }

    // ----------------------------------------------------------------- clôture

    private fun finaliser() {
        val ogr = cnx.existeTable("gpkg_ogr_contents")
        for (c in Couches.TOUTES) {
            val nb = cnx.lignes("SELECT count(*) AS n FROM ${c.nom.ident()}").first().entier("n")!!
            if (ogr) {
                cnx.prepareStatement("INSERT OR REPLACE INTO gpkg_ogr_contents (table_name, feature_count) VALUES (?, ?)").use { ps ->
                    ps.setString(1, c.nom); ps.setInt(2, nb)
                    ps.executeUpdate()
                }
            }
            if (c.geometrie != null) mettreAJourEtendue(c.nom, nb > 0)
            cnx.prepareStatement("UPDATE gpkg_contents SET last_change = ? WHERE table_name = ?").use { ps ->
                ps.setString(1, horodatage); ps.setString(2, c.nom)
                ps.executeUpdate()
            }
        }
        // Les couches de sommieR où l'on a ajouté des lignes : seule leur date de changement bouge.
        for ((couche, n) in compteurs) {
            if (n > 0 && couche in Couches.COUCHES_SOMMIER_ECRITES) {
                cnx.prepareStatement("UPDATE gpkg_contents SET last_change = ? WHERE table_name = ?").use { ps ->
                    ps.setString(1, horodatage); ps.setString(2, couche)
                    ps.executeUpdate()
                }
            }
        }
        verifierIntegrite()
    }

    private fun mettreAJourEtendue(table: String, nonVide: Boolean) {
        if (!nonVide) return
        val r = "rtree_${table}_geom"
        val e = cnx.lignes("SELECT min(minx) AS a, min(miny) AS b, max(maxx) AS c, max(maxy) AS d FROM ${r.ident()}").first()
        if (e["a"] == null) return
        cnx.prepareStatement("UPDATE gpkg_contents SET min_x = ?, min_y = ?, max_x = ?, max_y = ? WHERE table_name = ?").use { ps ->
            ps.setDouble(1, e.reel("a")!!); ps.setDouble(2, e.reel("b")!!); ps.setDouble(3, e.reel("c")!!); ps.setDouble(4, e.reel("d")!!)
            ps.setString(5, table)
            ps.executeUpdate()
        }
    }

    /** Le fichier écrit doit tenir : clés étrangères, index rtree complets, couches enregistrées. */
    private fun verifierIntegrite() {
        val fautes = cnx.lignes("PRAGMA foreign_key_check")
        if (fautes.isNotEmpty()) throw GpkgRefuse(RefusGpkg.PasUnGeoPackage("clés étrangères non respectées : $fautes"))
        for (c in Couches.TOUTES) {
            val inscrite = cnx.lignes("SELECT count(*) AS n FROM gpkg_contents WHERE table_name = '${c.nom}'").first().entier("n")
            if (inscrite != 1) throw GpkgRefuse(RefusGpkg.PasUnGeoPackage("couche ${c.nom} absente de gpkg_contents"))
            if (c.geometrie != null) {
                val lignes = cnx.lignes("SELECT count(*) AS n FROM ${c.nom.ident()} WHERE geom IS NOT NULL AND NOT ST_IsEmpty(geom)").first().entier("n")
                val indexees = cnx.lignes("SELECT count(*) AS n FROM ${"rtree_${c.nom}_geom".ident()}").first().entier("n")
                if (lignes != indexees) throw GpkgRefuse(RefusGpkg.PasUnGeoPackage("index rtree de ${c.nom} incomplet : $indexees sur $lignes"))
            }
        }
    }
}
