# Fonctions de la vérification « import sommieR » (mission 007, ADR 0007).
# Aucune dépendance au-delà de sommieR, DBI, RPostgres, sf, RSQLite, jsonlite.
#
# Coordonnées : les couches de sommieR sont en EPSG:2154 (x, y en mètres) ;
# ce script ne convertit rien, il compare les `srs_id` écrits dans le fichier.

# Couches ajoutées par Surculus (ADR 0007, règles « Couches de plants »).
COUCHES_AJOUTEES <- c("plants", "etats_plants", "constats_reprise", "vides",
                      "positions_controles", "projet")

env_obligatoire <- function(nom) {
  v <- Sys.getenv(nom, "")
  if (v == "") stop("Variable d'environnement absente : ", nom, call. = FALSE)
  v
}

connecter <- function(dbname) {
  DBI::dbConnect(
    RPostgres::Postgres(), dbname = dbname,
    host = Sys.getenv("SOMMIER_CI_HOST", "localhost"),
    port = as.integer(Sys.getenv("SOMMIER_CI_PORT", "5432")),
    user = Sys.getenv("SOMMIER_CI_USER", "postgres"),
    password = env_obligatoire("SOMMIER_CI_PASSWORD"))
}

# ---------------------------------------------------------------- schéma ----

# Description d'un GeoPackage : par table, champs (type SQL, notnull, pk),
# type de géométrie, drapeaux z et m, srs_id. Les champs gardent l'ordre de
# la table.
decrire_gpkg <- function(chemin) {
  db <- DBI::dbConnect(RSQLite::SQLite(), chemin, flags = RSQLite::SQLITE_RO)
  on.exit(DBI::dbDisconnect(db))
  tables <- DBI::dbGetQuery(
    db, "SELECT table_name, data_type FROM gpkg_contents ORDER BY table_name")
  geom <- DBI::dbGetQuery(
    db, "SELECT table_name, geometry_type_name AS geometrie, srs_id, z, m
           FROM gpkg_geometry_columns")
  res <- lapply(tables$table_name, function(t) {
    champs <- DBI::dbGetQuery(db, sprintf("PRAGMA table_info(\"%s\")", t))
    g <- geom[geom$table_name == t, ]
    list(
      data_type = tables$data_type[tables$table_name == t],
      geometrie = if (nrow(g)) g$geometrie else NA_character_,
      srs_id = if (nrow(g)) as.integer(g$srs_id) else NA_integer_,
      z = if (nrow(g)) as.integer(g$z) else NA_integer_,
      m = if (nrow(g)) as.integer(g$m) else NA_integer_,
      champs = stats::setNames(
        as.list(paste0(toupper(champs$type), " notnull=", champs$notnull,
                       " pk=", champs$pk)),
        champs$name)
    )
  })
  stats::setNames(res, tables$table_name)
}

# Écarts entre les couches d'une tournée et le modèle de sommieR. Seules les
# couches du modèle sont comparées : une couche en plus n'est pas un écart
# (c'est le point (c) de l'ADR 0007). Rend un vecteur de fautes, vide si tout
# est identique.
comparer_schema <- function(tournee_gpkg, modele_gpkg) {
  t <- decrire_gpkg(tournee_gpkg)
  m <- decrire_gpkg(modele_gpkg)
  fautes <- character(0)
  for (couche in names(m)) {
    if (!couche %in% names(t)) {
      fautes <- c(fautes, paste0(couche, " : couche absente de la tournée"))
      next
    }
    a <- m[[couche]]
    b <- t[[couche]]
    if (!identical(a$data_type, b$data_type))
      fautes <- c(fautes, paste0(couche, " : data_type ", a$data_type,
                                 " -> ", b$data_type))
    if (!identical(a$geometrie, b$geometrie))
      fautes <- c(fautes, paste0(couche, " : géométrie ", a$geometrie,
                                 " -> ", b$geometrie))
    if (!identical(a$z, b$z) || !identical(a$m, b$m))
      fautes <- c(fautes, paste0(couche, " : drapeaux z/m ", a$z, "/", a$m,
                                 " -> ", b$z, "/", b$m))
    if (!identical(a$srs_id, b$srs_id))
      fautes <- c(fautes, paste0(couche, " : srs_id ", a$srs_id,
                                 " -> ", b$srs_id))
    if (!identical(a$champs, b$champs)) {
      manquants <- setdiff(names(a$champs), names(b$champs))
      ajoutes <- setdiff(names(b$champs), names(a$champs))
      communs <- intersect(names(a$champs), names(b$champs))
      changes <- communs[unlist(a$champs[communs]) != unlist(b$champs[communs])]
      fautes <- c(
        fautes,
        if (length(manquants)) paste0(couche, " : champs retirés ",
                                      paste(manquants, collapse = ", ")),
        if (length(ajoutes)) paste0(couche, " : champs ajoutés ",
                                    paste(ajoutes, collapse = ", ")),
        if (length(changes)) paste0(couche, " : type, notnull ou pk changés ",
                                    paste(changes, collapse = ", ")),
        if (!length(manquants) && !length(ajoutes) && !length(changes))
          paste0(couche, " : ordre des champs différent"))
    }
  }
  fautes
}

# Copie `dossier` dans `destination` privée des `couches` données : tables,
# lignes de gpkg_contents, gpkg_geometry_columns, gpkg_data_columns et
# gpkg_extensions. Une couche absente est ignorée. C'est le témoin « sans »
# du critère (a) : le même export, sans ce que Surculus a ajouté.
#
# `sf::st_delete()` passe par GDAL, qui retire lui-même la table, l'index
# spatial (`rtree_<couche>_<geom>` et ses triggers) et la ligne de
# `gpkg_ogr_contents` ; le DELETE ci-dessous ne fait que balayer ce que GDAL
# aurait laissé dans les tables du standard. Puis `traces_restantes()` vérifie
# qu'il ne reste plus rien, et la fonction s'arrête sinon.
retirer_couches <- function(dossier, destination, couches = COUCHES_AJOUTEES) {
  unlink(destination, recursive = TRUE)
  dir.create(destination, recursive = TRUE)
  file.copy(list.files(dossier, full.names = TRUE, all.files = TRUE,
                       no.. = TRUE), destination, recursive = TRUE)
  gpkg <- file.path(destination, "travaux.gpkg")
  for (couche in intersect(couches, sf::st_layers(gpkg)$name))
    sf::st_delete(gpkg, layer = couche, quiet = TRUE)
  db <- DBI::dbConnect(RSQLite::SQLite(), gpkg)
  on.exit(DBI::dbDisconnect(db))
  for (couche in couches) {
    for (t in TABLES_STANDARD_PAR_COUCHE) {
      if (DBI::dbExistsTable(db, t))
        DBI::dbExecute(db, sprintf("DELETE FROM %s WHERE table_name = ?", t),
                       params = list(couche))
    }
  }
  restes <- traces_restantes(db, couches)
  if (length(restes))
    stop("Traces non retirées : ", paste(restes, collapse = " ; "),
         call. = FALSE)
  invisible(destination)
}

# Tables du standard GeoPackage (et de GDAL) qui portent une ligne par couche.
TABLES_STANDARD_PAR_COUCHE <- c(
  "gpkg_contents", "gpkg_geometry_columns", "gpkg_data_columns",
  "gpkg_extensions", "gpkg_metadata_reference", "gpkg_ogr_contents")

# Ce qu'il reste des `couches` dans le GeoPackage ouvert `db` : objets de
# sqlite_master (tables, vues, triggers, index) dont le nom est celui de la
# couche ou `rtree_<couche>_*`, et lignes des tables du standard. Vecteur
# vide : plus rien.
traces_restantes <- function(db, couches) {
  restes <- character(0)
  maitre <- DBI::dbGetQuery(db, "SELECT type, name FROM sqlite_master")
  for (couche in couches) {
    noms <- maitre$name[maitre$name == couche |
                          startsWith(maitre$name, paste0("rtree_", couche, "_"))]
    if (length(noms))
      restes <- c(restes, paste0("sqlite_master : ", paste(noms, collapse = ", ")))
    for (t in TABLES_STANDARD_PAR_COUCHE) {
      if (!DBI::dbExistsTable(db, t)) next
      n <- DBI::dbGetQuery(
        db, sprintf("SELECT count(*) AS n FROM %s WHERE table_name = ?", t),
        params = list(couche))$n
      if (n > 0L) restes <- c(restes, paste0(t, " : ", couche))
    }
  }
  restes
}

# Auto-test du retrait (décision de Pascal) : pose les six couches, vides, sur
# une copie de `dossier` -- dont trois géométriques, donc avec index spatial,
# et des lignes dans gpkg_data_columns --, les retire, et vérifie que le
# résultat ne contient plus rien d'elles et garde les couches de départ.
# Rend un vecteur de fautes, vide si tout va bien.
autotest_retrait <- function(dossier, travail) {
  copie <- file.path(travail, "autotest-pose")
  unlink(copie, recursive = TRUE)
  dir.create(copie, recursive = TRUE)
  file.copy(list.files(dossier, full.names = TRUE, all.files = TRUE,
                       no.. = TRUE), copie, recursive = TRUE)
  gpkg <- file.path(copie, "travaux.gpkg")
  avant <- sf::st_layers(gpkg)$name
  # sf calcule l'étendue d'une couche à zéro ligne avec min()/max() et en
  # avertit ("no non-missing arguments to min; returning Inf") : on ne tait que
  # ces avertissements-là, pas les autres.
  vide_pt <- withCallingHandlers(
    sf::st_as_sf(data.frame(id = integer(0), x = numeric(0), y = numeric(0)),
                 coords = c("x", "y"), crs = 2154),
    warning = function(w) {
      if (grepl("no non-missing arguments to (min|max)", conditionMessage(w)))
        invokeRestart("muffleWarning")
    })
  for (couche in COUCHES_AJOUTEES) {
    if (couche %in% c("plants", "vides", "positions_controles")) {
      sf::st_write(vide_pt, gpkg, layer = couche, quiet = TRUE)
    } else {
      sf::st_write(data.frame(id = integer(0)), gpkg, layer = couche,
                   quiet = TRUE)
    }
  }
  db <- DBI::dbConnect(RSQLite::SQLite(), gpkg)
  if (!DBI::dbExistsTable(db, "gpkg_data_columns"))
    DBI::dbExecute(db, "CREATE TABLE gpkg_data_columns (
      table_name TEXT NOT NULL, column_name TEXT NOT NULL, name TEXT,
      title TEXT, description TEXT, mime_type TEXT, constraint_name TEXT,
      CONSTRAINT pk_gdc PRIMARY KEY (table_name, column_name))")
  for (couche in COUCHES_AJOUTEES)
    DBI::dbExecute(db, "INSERT INTO gpkg_data_columns (table_name, column_name)
                         VALUES (?, 'id')", params = list(couche))
  poses <- traces_restantes(db, COUCHES_AJOUTEES)
  DBI::dbDisconnect(db)
  fautes <- character(0)
  if (!any(grepl("^sqlite_master : .*rtree_", poses)))
    fautes <- c(fautes, "autotest : la pose n'a créé aucun index spatial")
  if (!all(COUCHES_AJOUTEES %in% sf::st_layers(gpkg)$name))
    fautes <- c(fautes, "autotest : les six couches ne sont pas toutes posées")
  retire <- retirer_couches(copie, file.path(travail, "autotest-retire"))
  db <- DBI::dbConnect(RSQLite::SQLite(), file.path(retire, "travaux.gpkg"))
  on.exit(DBI::dbDisconnect(db))
  fautes <- c(fautes, traces_restantes(db, COUCHES_AJOUTEES))
  apres <- sf::st_layers(file.path(retire, "travaux.gpkg"))$name
  if (!setequal(apres, avant))
    fautes <- c(fautes, paste0("autotest : couches de départ ", length(avant),
                               " -> ", length(apres), " après retrait"))
  fautes
}

# (c) Toutes les couches ajoutées sont listées par sf::st_layers().
couches_manquantes <- function(tournee_gpkg, attendues = COUCHES_AJOUTEES) {
  setdiff(attendues, sf::st_layers(tournee_gpkg)$name)
}

# ----------------------------------------------------------------- bilan ----

# Contenu du registre pour une forêt, sans ce qui dépend de l'horloge
# (date_saisie, hash, hash_prev, seq).
etat_registre <- function(con, foret_id) {
  x <- DBI::dbGetQuery(
    con,
    "SELECT id::text AS id, registre, ug_uuid::text AS ug_uuid,
            date_evenement::text AS date_evenement, ndp, auteur,
            corrige_id::text AS corrige_id, schema_version,
            ST_AsText(geom) AS geom, payload::jsonb::text AS payload
       FROM entree_sommier WHERE foret_id = $1 ORDER BY id",
    params = list(foret_id))
  rownames(x) <- NULL
  x
}

# Importe `dossier` dans une base clonée de la base modèle (qui porte la forêt
# de la démo, dont la tournée a besoin pour rattacher ses placettes) ; rend le
# bilan, trié, prêt à être écrit en JSON.
importer_et_bilanter <- function(dossier, nom_base, base_modele, foret_id) {
  admin <- connecter("postgres")
  DBI::dbExecute(admin, sprintf('DROP DATABASE IF EXISTS "%s"', nom_base))
  DBI::dbExecute(admin, sprintf('CREATE DATABASE "%s" TEMPLATE "%s"',
                                nom_base, base_modele))
  DBI::dbDisconnect(admin)
  con <- connecter(nom_base)
  on.exit(DBI::dbDisconnect(con))
  depot <- tempfile("depot-photos-")
  dir.create(depot)
  avant <- nrow(etat_registre(con, foret_id))
  bilan <- sommieR::sommier_importer_qfield_travaux(
    con, foret_id, dossier, depot, auteur = "ci-import-sommier")
  registre <- etat_registre(con, foret_id)
  list(
    dossier_importe = basename(normalizePath(dossier)),
    travaux = bilan$travaux, placettes = bilan$placettes,
    controles = bilan$controles, deja_presents = bilan$deja_presents,
    photos = bilan$photos,
    entrees_ecrites = sort(vapply(bilan$entrees, function(e) e$id,
                                  character(1))),
    entrees_avant = avant, entrees_apres = nrow(registre),
    registre = registre
  )
}

ecrire_bilan <- function(bilan, chemin) {
  jsonlite::write_json(bilan, chemin, auto_unbox = TRUE, pretty = TRUE,
                       null = "null", na = "null")
}

# Deux bilans doivent être identiques, à l'exception du nom du dossier.
# Rend les noms des champs qui diffèrent.
comparer_bilans <- function(a, b) {
  a$dossier_importe <- NULL
  b$dossier_importe <- NULL
  champs <- union(names(a), names(b))
  champs[!vapply(champs, function(k) identical(a[[k]], b[[k]]), logical(1))]
}
