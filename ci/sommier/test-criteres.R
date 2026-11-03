#!/usr/bin/env Rscript
# Tests de F1, F2 et F3 (mission 026) sur de petits GeoPackage fabriqués ici :
# aucune base PostgreSQL requise. Usage : Rscript ci/sommier/test-criteres.R
# Chaque cas dit ce qu'il attend ; le script s'arrête au premier écart.

dir_script <- dirname(sub("^--file=", "",
  grep("^--file=", commandArgs(FALSE), value = TRUE)[1L]))
source(file.path(dir_script, "fonctions.R"))
source(file.path(dir_script, "criteres.R"))
source(file.path(dir_script, "f4.R"))
source(file.path(dir_script, "sabotage.R"))

travail <- tempfile("test-criteres-")
dir.create(travail)
n_tests <- 0L
verifier <- function(condition, message) {
  n_tests <<- n_tests + 1L
  if (!isTRUE(condition)) stop("ÉCHEC : ", message, call. = FALSE)
}
contient <- function(fautes, motif) any(grepl(motif, fautes, fixed = TRUE))

# Tournée de départ : deux couches de sommieR (index spatial désactivé, car les
# déclencheurs R-tree exigent les fonctions ST_* de GDAL, absentes de RSQLite).
# Coordonnées : x (est), y (nord) en mètres, EPSG:2154.
depart <- file.path(travail, "depart.gpkg")
pts <- function(uuids) sf::st_as_sf(
  data.frame(uuid = uuids, nb_total = seq_along(uuids),
             x = 800000 + seq_along(uuids), y = 6600000 + seq_along(uuids)),
  coords = c("x", "y"), crs = 2154)
sf::st_write(pts(c("aaaa", "bbbb")), depart, layer = "controles", quiet = TRUE,
             layer_options = "SPATIAL_INDEX=NO")
sf::st_write(pts("cccc"), depart, layer = "placettes", quiet = TRUE,
             layer_options = "SPATIAL_INDEX=NO")

db0 <- DBI::dbConnect(RSQLite::SQLite(), depart)
if (!DBI::dbExistsTable(db0, "gpkg_extensions"))
  invisible(DBI::dbExecute(db0, "CREATE TABLE gpkg_extensions (table_name TEXT, column_name TEXT,
    extension_name TEXT NOT NULL, definition TEXT NOT NULL, scope TEXT NOT NULL)"))
DBI::dbDisconnect(db0)

# Copie de la tournée de départ, modifiée par `sql` ; rend le chemin.
variante <- function(nom, sql = character(0)) {
  f <- file.path(travail, paste0(nom, ".gpkg"))
  file.copy(depart, f, overwrite = TRUE)
  db <- DBI::dbConnect(RSQLite::SQLite(), f)
  for (s in sql) DBI::dbExecute(db, s)
  DBI::dbDisconnect(db)
  f
}
ajout_controle <- "INSERT INTO controles (geom, uuid, nb_total)
                     SELECT geom, 'dddd', 9 FROM controles WHERE uuid = 'aaaa'"
COUCHES <- COUCHES_AJOUTEES

# --- F2 ---------------------------------------------------------------------
x <- variante("identique")
r <- controle_f2(depart, x)
verifier(length(r$fautes) == 0L, "F2 : copie identique sans faute")
verifier(!length(controle_f3(depart, x, COUCHES, r$inserts)), "F3 : copie identique sans faute")

x <- variante("ajout", ajout_controle)
r <- controle_f2(depart, x, list(controles = "DDDD"))
verifier(length(r$fautes) == 0L, "F2 : un ajout attendu (casse ignorée) passe")
verifier(r$inserts[["controles"]] == 1L, "F2 : un ajout compté")
verifier(!length(controle_f3(depart, x, COUCHES, r$inserts)),
         "F3 : un ajout dans controles garde le schéma, feature_count = départ + 1")

r <- controle_f2(depart, x)
verifier(contient(r$fautes, "non attendue"), "F2 : ajout non attendu refusé")
x2 <- variante("manquant")
r <- controle_f2(depart, x2, list(controles = "dddd"))
verifier(contient(r$fautes, "non ajoutée"), "F2 : ajout attendu mais absent refusé")

x <- variante("update", "UPDATE controles SET nb_total = 99 WHERE uuid = 'aaaa'")
r <- controle_f2(depart, x)
verifier(contient(r$fautes, "modifiée"), "F2 : UPDATE d'une ligne existante refusé")
x <- variante("update-geom", "UPDATE controles SET geom = (SELECT geom FROM controles WHERE uuid = 'bbbb') WHERE uuid = 'aaaa'")
verifier(contient(controle_f2(depart, x)$fautes, "modifiée"), "F2 : géométrie modifiée refusée")
x <- variante("delete", "DELETE FROM controles WHERE uuid = 'bbbb'")
verifier(contient(controle_f2(depart, x)$fautes, "retirée"), "F2 : DELETE refusé")
x <- variante("autre-couche", "INSERT INTO placettes (geom, uuid, nb_total) SELECT geom, 'eeee', 1 FROM placettes")
r <- controle_f2(depart, x, list(controles = character(0)))
verifier(contient(r$fautes, "placettes"), "F2 : ajout dans une couche sans UUID attendu refusé")

# --- F3 ---------------------------------------------------------------------
x <- variante("trigger", "CREATE TRIGGER t_x AFTER INSERT ON controles BEGIN SELECT 1; END")
verifier(contient(controle_f3(depart, x, COUCHES), "trigger t_x ajouté"), "F3 : CREATE TRIGGER sur controles refusé")
x <- variante("index", "CREATE INDEX i_x ON controles (nb_total)")
verifier(contient(controle_f3(depart, x, COUCHES), "index i_x ajouté"), "F3 : CREATE INDEX sur controles refusé")
x <- variante("colonne", "ALTER TABLE controles ADD COLUMN extra TEXT")
verifier(length(controle_f3(depart, x, COUCHES)) > 0L, "F3 : colonne ajoutée à controles refusée")
x <- variante("appid", "PRAGMA application_id = 1")
verifier(contient(controle_f3(depart, x, COUCHES), "application_id"), "F3 : application_id changé refusé")
x <- variante("uv", "PRAGMA user_version = 7")
verifier(contient(controle_f3(depart, x, COUCHES), "user_version"), "F3 : user_version changé refusé")
x <- variante("srs", "UPDATE gpkg_geometry_columns SET srs_id = 4326 WHERE table_name = 'controles'")
verifier(contient(controle_f3(depart, x, COUCHES), "gpkg_geometry_columns"), "F3 : srs_id changé refusé")
x <- variante("contents", "UPDATE gpkg_contents SET identifier = 'autre' WHERE table_name = 'controles'")
verifier(contient(controle_f3(depart, x, COUCHES), "gpkg_contents"), "F3 : gpkg_contents modifié refusé")
x <- variante("last_change", "UPDATE gpkg_contents SET last_change = '2000-01-01T00:00:00.000Z'")
verifier(length(controle_f3(depart, x, COUCHES)) == 0L, "F3 : last_change seul est exclu")
x <- variante("fc", "UPDATE gpkg_ogr_contents SET feature_count = 5 WHERE table_name = 'controles'")
verifier(contient(controle_f3(depart, x, COUCHES), "feature_count de controles"), "F3 : feature_count faux refusé")
x <- variante("fc-ajout", ajout_controle)
verifier(contient(controle_f3(depart, x, COUCHES, c(controles = 0L)), "feature_count de controles"),
         "F3 : feature_count > départ + ajouts comptés refusé")
x <- variante("couche-ajoutee")
sf::st_write(pts("zzzz"), x, layer = "plants", quiet = TRUE, layer_options = "SPATIAL_INDEX=NO")
r <- controle_f2(depart, x)
verifier(length(r$fautes) == 0L, "F2 : une couche ajoutée n'est pas comparée")
verifier(length(controle_f3(depart, x, COUCHES, r$inserts)) == 0L, "F3 : une couche ajoutée (plants) est permise")
verifier(length(controle_f3(depart, x, setdiff(COUCHES, "plants"), r$inserts)) > 0L,
         "F3 : la même couche hors de la liste des six est refusée")
x <- variante("couche-etrangere")
sf::st_write(pts("zzzz"), x, layer = "intruse", quiet = TRUE, layer_options = "SPATIAL_INDEX=NO")
verifier(contient(controle_f3(depart, x, COUCHES), "intruse"), "F3 : une couche étrangère aux six est refusée")

# Extension Schema : gpkg_schema admise sur une des six couches, refusée sur une
# couche de sommieR.
ext <- function(couche) sprintf(
  "INSERT INTO gpkg_extensions (table_name, column_name, extension_name, definition, scope)
     VALUES ('%s', NULL, 'gpkg_schema', 'x', 'read-write')", couche)
x <- variante("schema-sur-plants", c(ext("plants")))
verifier(length(controle_f3(depart, x, COUCHES)) == 0L, "F3 : gpkg_schema sur une des six couches admise")
x <- variante("schema-sur-controles", ext("controles"))
verifier(contient(controle_f3(depart, x, COUCHES), "gpkg_extensions"), "F3 : gpkg_schema sur une couche de sommieR refusée")
x <- variante("autre-ext-sur-controles", sub("gpkg_schema", "gpkg_autre", ext("controles")))
verifier(contient(controle_f3(depart, x, COUCHES), "gpkg_extensions"), "F3 : autre extension sur une couche de sommieR refusée")

# feature_count NULL au départ (modèle de sommieR) : doit rester NULL.
x <- variante("fc-null-depart", "UPDATE gpkg_ogr_contents SET feature_count = NULL")
dn <- file.path(travail, "depart-null.gpkg")
invisible(file.copy(x, dn, overwrite = TRUE))
verifier(length(controle_f3(dn, x, COUCHES)) == 0L, "F3 : feature_count NULL des deux côtés accepté")
x2 <- variante("fc-null-rempli", "UPDATE gpkg_ogr_contents SET feature_count = NULL WHERE table_name = 'placettes'")
verifier(contient(controle_f3(dn, depart, COUCHES), "feature_count de controles"),
         "F3 : feature_count NULL au départ puis renseigné dans l'export refusé")

# --- F1 ---------------------------------------------------------------------
bexp <- list(travaux = 0L, placettes = 0L, controles = 2L)
bimp <- list(travaux = 0L, placettes = 0L, controles = 2L, deja_presents = 3L,
             entrees_ecrites = c("a", "b"))
verifier(length(controle_f1(bimp, bexp, 3L)$fautes) == 0L, "F1 : bilans égaux")
bexp2 <- bexp; bexp2$controles <- 3L
verifier(contient(controle_f1(bimp, bexp2, 3L)$fautes, "contrôles"), "F1 : une saisie de moins refusée")
verifier(contient(controle_f1(bimp, bexp, 4L)$fautes, "déjà présents"), "F1 : déjà présents faux refusé")
bimp2 <- bimp; bimp2$entrees_ecrites <- "a"
verifier(contient(controle_f1(bimp2, bexp, 3L)$fautes, "entrées écrites"), "F1 : entrées écrites en moins refusées")
verifier(length(controle_f1(bimp, list(placettes = 0L, controles = 2L), 3L)$fautes) > 0L,
         "F1 : un champ absent du bilan d'export n'est pas une égalité")
verifier(lignes_importables(depart) == 3L, "F1 : lignes de départ comptées (2 contrôles + 1 placette)")

# --- Renvoyés par la relecture de la 026 : extension Schema ------------------
# gpkg_schema et les tables gpkg_data_columns / gpkg_data_column_constraints
# (ADR 0007, règle « Conformité GeoPackage »).
cree_data_columns <- "CREATE TABLE gpkg_data_columns (
  table_name TEXT NOT NULL, column_name TEXT NOT NULL, name TEXT, title TEXT,
  description TEXT, mime_type TEXT, constraint_name TEXT,
  CONSTRAINT pk_gdc PRIMARY KEY (table_name, column_name))"
cree_constraints <- "CREATE TABLE gpkg_data_column_constraints (
  constraint_name TEXT NOT NULL, constraint_type TEXT NOT NULL, value TEXT,
  min NUMERIC, min_is_inclusive BOOLEAN, max NUMERIC, max_is_inclusive BOOLEAN,
  description TEXT)"
x <- variante("schema-tables", c(cree_data_columns, cree_constraints))
verifier(length(controle_f3(depart, x, COUCHES)) == 0L,
         "F3 : créer gpkg_data_columns et gpkg_data_column_constraints est toléré")
x <- variante("schema-tables-ext", c(cree_data_columns, cree_constraints,
                                     ext("gpkg_data_columns"),
                                     ext("gpkg_data_column_constraints")))
verifier(length(controle_f3(depart, x, COUCHES)) == 0L,
         "F3 : gpkg_schema sur gpkg_data_columns et gpkg_data_column_constraints toléré")
x <- variante("schema-tables-ligne-plants", c(cree_data_columns, cree_constraints,
  "INSERT INTO gpkg_data_columns (table_name, column_name) VALUES ('plants', 'id')",
  "INSERT INTO gpkg_data_column_constraints (constraint_name, constraint_type) VALUES ('c', 'enum')"))
verifier(length(controle_f3(depart, x, COUCHES)) == 0L,
         "F3 : ligne de gpkg_data_columns sur une des six couches, et contrainte, tolérées")
x <- variante("schema-tables-ligne-controles", c(cree_data_columns,
  "INSERT INTO gpkg_data_columns (table_name, column_name) VALUES ('controles', 'nb_total')"))
verifier(contient(controle_f3(depart, x, COUCHES), "gpkg_data_columns"),
         "F3 : ligne de gpkg_data_columns sur une couche de sommieR refusée")
x <- variante("schema-autre-table", c("CREATE TABLE gpkg_autre (a TEXT)"))
verifier(contient(controle_f3(depart, x, COUCHES), "gpkg_autre"),
         "F3 : une autre table gpkg_* ajoutée est refusée")

# --- Renvoyés par la relecture de la 026 : témoin « sans », job vert --------
a <- avertissement_temoin(c("registre", "photos"))
verifier(startsWith(a$annotation, "::warning title=Témoin sans couches::"),
         "témoin : une différence donne une annotation ::warning")
verifier(contient(a$annotation, "registre, photos"), "témoin : l'annotation nomme les champs qui diffèrent")
verifier(startsWith(a$resume, "**Avertissement**") && contient(a$resume, "hors verdict"),
         "témoin : le résumé dit « avertissement, hors verdict »")
a0 <- avertissement_temoin(character(0))
verifier(is.null(a0$annotation), "témoin : aucune différence, aucune annotation")
verifier(contient(a0$resume, "même bilan"), "témoin : aucune différence, le résumé le dit")
# Ni `::warning` ni `::error` ne fait échouer : l'avertissement n'ajoute pas
# d'écart. Le script le garantit par construction (aucun appel à signaler()
# dans le chemin du témoin) : on le vérifie sur le source.
src_verifier <- paste(readLines(file.path(dir_script, "verifier.R"), encoding = "UTF-8"), collapse = "\n")
bloc_temoin <- sub(".*Vérification de fumée \\(plus un critère\\)", "", src_verifier)
bloc_temoin <- sub("# F5 : auto-test.*", "", bloc_temoin)
verifier(!grepl("signaler(", bloc_temoin, fixed = TRUE),
         "témoin : le chemin de l'avertissement n'appelle pas signaler() (job vert)")
verifier(!grepl("::error", bloc_temoin, fixed = TRUE), "témoin : pas d'annotation ::error")

# --- F4 (mission 028) -------------------------------------------------------
fichier_attendu_test <- file.path(dir_script, "..", "..", "fixtures", "export",
                                  "registre-attendu-couchey.json")
attendu <- jsonlite::fromJSON(fichier_attendu_test, simplifyVector = FALSE)
att1 <- attendu$controles[[1L]]; att2 <- attendu$controles[[2L]]
controles_ok <- data.frame(
  id = c("e1", "e2"), code = c("P35-01", "P35-02"),
  visite_le = c(att1$visite_le, att2$visite_le),
  nb_total = c(att1$nb_total, att2$nb_total),
  nb_vivants = c(att1$nb_vivants, att2$nb_vivants),
  nb_abroutis = c(att1$nb_abroutis, att2$nb_abroutis),
  h_moy_cm = c(att1$h_moy_cm, att2$h_moy_cm),
  releve_uuid = c("r1", "r2"), stringsAsFactors = FALSE)
# Mission 087 (décision de Pascal du 2026-11-02, « en faire le cas sans position ») :
# P35-02 est un contrôle ouvert sans position mesurée. API supposée de
# lire_points_export(), à confirmer par le dev :
#   positions : data.frame(controle_uuid, lon, lat, sans_position_mesuree,
#               rattache_par_identite, precision_m) ; lon et lat NA quand la
#               géométrie est NULL ;
#   controles : data.frame(uuid, lon, lat), la couche `controles` de l'export
#               (le point du contrôle ; pour P35-02, le piquet) ;
#   piquets   : inchangé.
verifier(isTRUE(att2$point$sans_position_mesuree) && !isTRUE(att1$point$sans_position_mesuree),
         "F4 : le JSON attendu marque P35-02, et lui seul, sans position mesurée (mission 087)")
points_ok <- list(
  positions = data.frame(controle_uuid = c("r1", "r2"), lon = c(att1$point$lon, NA),
                         lat = c(att1$point$lat, NA),
                         sans_position_mesuree = c(FALSE, TRUE),
                         rattache_par_identite = c(FALSE, TRUE),
                         precision_m = c(0.4, NA), stringsAsFactors = FALSE),
  controles = data.frame(uuid = c("r1", "r2"), lon = c(att1$point$lon, att2$point$lon),
                         lat = c(att1$point$lat, att2$point$lat), stringsAsFactors = FALSE),
  piquets = data.frame(code_placette = c("P35-01", "P35-02"),
                       lon = c(4.9325, att2$point$lon),
                       lat = c(47.2591, att2$point$lat), stringsAsFactors = FALSE))
f4 <- controle_f4(attendu, controles_ok, points_ok)
verifier(length(f4$fautes) == 0L, "F4 : registre et points conformes, sans faute")
verifier(nrow(f4$lignes) >= 12L && all(f4$lignes$verdict == "conforme"),
         "F4 : au moins 12 lignes comparées (2 x (visite_le, 4 totaux, point)), toutes conformes")
verifier(any(f4$lignes$placette == "P35-02" & grepl("positions_controles", f4$lignes$champ)),
         "F4 : le résumé porte une ligne sur la ligne de positions_controles de P35-02 (mission 087)")
verifier(att1$point$tolerance_m == 0.5, "F4 : tolérance du JSON = 0,5 m (décision de Pascal, mission 027)")

change <- function(df, i, k, v) { df[[k]][i] <- v; df }
for (k in c("nb_total", "nb_vivants", "nb_abroutis", "h_moy_cm")) {
  f <- controle_f4(attendu, change(controles_ok, 1L, k, controles_ok[[k]][1L] + 1), points_ok)
  verifier(contient(f$fautes, paste0("P35-01, ", k)), paste("F4 :", k, "faux refusé"))
}
f <- controle_f4(attendu, change(controles_ok, 2L, "h_moy_cm", 41), points_ok)
verifier(contient(f$fautes, "P35-02, h_moy_cm"), "F4 : h_moy_cm faux de P35-02 refusé")
f <- controle_f4(attendu, change(controles_ok, 1L, "nb_total", NA), points_ok)
verifier(contient(f$fautes, "nb_total"), "F4 : total absent (NA) refusé")
f <- controle_f4(attendu, change(controles_ok, 1L, "visite_le", "2026-10-20T11:30:00Z"), points_ok)
verifier(contient(f$fautes, "P35-01, visite_le : attendu 2026-10-20T09:30:00Z, lu au registre 2026-10-20T11:30:00Z"),
         "F4 : visite_le faux de 2 h refusé, avec la valeur lue au registre")
f <- controle_f4(attendu, change(controles_ok, 1L, "visite_le", "2026-10-19T22:00:00Z"), points_ok)
verifier(contient(f$fautes, "P35-01, visite_le"), "F4 : visite_le un jour trop tôt (perte de l'heure) refusé")
f <- controle_f4(attendu, change(controles_ok, 1L, "visite_le", "2026-10-20T09:30:01Z"), points_ok)
verifier(contient(f$fautes, "P35-01, visite_le"), "F4 : visite_le faux d'une seconde refusé (à la seconde)")
f <- controle_f4(attendu, change(controles_ok, 1L, "visite_le", "2026-10-20 09:30:00"), points_ok)
verifier(contient(f$fautes, "P35-01, visite_le"), "F4 : visite_le sans « Z » (pas un instant UTC) refusé")
f <- controle_f4(attendu, change(controles_ok, 1L, "visite_le", "2026-10-20T09:30:00.000Z"), points_ok)
verifier(length(f$fautes) == 0L, "F4 : même instant avec millisecondes accepté")
f <- controle_f4(attendu, change(controles_ok, 1L, "visite_le", "2026-10-20T11:30:00Z"), points_ok)
verifier("2026-10-20T11:30:00Z" %in% f$lignes$mesure && "ÉCART" %in% f$lignes$verdict,
         "F4 : le résumé porte la valeur lue au registre, et la ligne visite_le n'est pas conforme d'office")
# Le fuseau du processus R ne change pas la comparaison.
tz_avant <- Sys.getenv("TZ", NA)
Sys.setenv(TZ = "Europe/Paris")
verifier(length(controle_f4(attendu, controles_ok, points_ok)$fautes) == 0L,
         "F4 : conforme aussi sous TZ=Europe/Paris")
verifier(contient(controle_f4(attendu, change(controles_ok, 1L, "visite_le", "2026-10-20T11:30:00Z"),
                              points_ok)$fautes, "visite_le"),
         "F4 : une heure fausse reste refusée sous TZ=Europe/Paris")
if (is.na(tz_avant)) Sys.unsetenv("TZ") else Sys.setenv(TZ = tz_avant)
f <- controle_f4(attendu, controles_ok[2L, ], points_ok)
verifier(contient(f$fautes, "P35-01, contrôle du"), "F4 : contrôle attendu absent du registre refusé")
f <- controle_f4(attendu, rbind(controles_ok, change(controles_ok, 1L, "id", "e3")[1L, ]), points_ok)
verifier(contient(f$fautes, "2 entrée(s)"), "F4 : contrôle en double refusé")
extra <- change(change(controles_ok[1L, ], 1L, "id", "e9"), 1L, "visite_le", "2026-10-21T09:30:00Z")
f <- controle_f4(attendu, rbind(controles_ok, extra), points_ok)
verifier(contient(f$fautes, "sans attente"), "F4 : contrôle écrit sans attente refusé")

# Le point : tolérance de 0,5 m, en mètres, lon puis lat.
decale <- function(m_est, m_nord) {   # m_est vers l'est, m_nord vers le nord ; ligne de P35-01 seule
  p <- points_ok
  p$positions$lon[1L] <- att1$point$lon +
    m_est / (distance_wgs84_m(att1$point$lon, att1$point$lat, att1$point$lon + 1, att1$point$lat))
  p$positions$lat[1L] <- att1$point$lat +
    m_nord / (distance_wgs84_m(att1$point$lon, att1$point$lat, att1$point$lon, att1$point$lat + 1))
  p
}
verifier(abs(distance_wgs84_m(4.9325, 47.2591, 4.9325 + 1e-5, 47.2591) - 0.7547) < 0.01,
         "distance : 1e-5 degré de longitude à 47,26 N = 0,755 m")
verifier(abs(distance_wgs84_m(4.9325, 47.2591, 4.9325, 47.2591 + 1e-5) - 1.1119) < 0.01,
         "distance : 1e-5 degré de latitude = 1,112 m")
verifier(length(controle_f4(attendu, controles_ok, decale(0.4, 0))$fautes) == 0L,
         "F4 : point à 0,4 m (est) accepté")
verifier(length(controle_f4(attendu, controles_ok, decale(0, -0.3))$fautes) == 0L,
         "F4 : point à 0,3 m (sud) accepté")
f <- controle_f4(attendu, controles_ok, decale(0.6, 0))
verifier(contient(f$fautes, "point de positions_controles à 0.600 m"), "F4 : point à 0,6 m refusé, avec la distance")
f <- controle_f4(attendu, controles_ok, decale(0, 5))
verifier(contient(f$fautes, "5.000 m"), "F4 : point déplacé de 5 m refusé")
# La position mesurée de P35-01 (+0,6 m est, -0,4 m nord) n'est PAS le piquet
# (decision de Pascal, mission 027) : le piquet seul est à 0,72 m, refusé.
p <- points_ok; p$positions$lon[1L] <- 4.9325; p$positions$lat[1L] <- 47.2591
verifier(contient(controle_f4(attendu, controles_ok, p)$fautes, "0.72"),
         "F4 : le piquet à la place de la position mesurée (0,72 m) est refusé")
# Latitude et longitude inversées : placette en Somalie.
p <- points_ok; p$positions$lon[1L] <- att1$point$lat; p$positions$lat[1L] <- att1$point$lon
f <- controle_f4(attendu, controles_ok, p)
verifier(contient(f$fautes, "P35-01, point de positions_controles"), "F4 : lon et lat inversées refusées")
verifier(!dans_france(47.2591, 4.9325, attendu$france_metropolitaine) &&
           dans_france(4.9325, 47.2591, attendu$france_metropolitaine),
         "F4 : (lon 47,26 ; lat 4,93) tombe hors de France, (lon 4,93 ; lat 47,26) en France")
# Position mesurée attendue mais absente de l'export : écart, pas un repli sur le piquet.
p <- points_ok; p$positions <- p$positions[0L, ]
verifier(contient(controle_f4(attendu, controles_ok, p)$fautes, "aucune ligne de positions_controles"),
         "F4 : positions_controles sans ligne pour P35-01 refusé")
# --- F4, P35-02 sans position mesurée (mission 087 ; ADR 0003, amendement du
# 2026-11-02, point 7 ; décision de Pascal du 2026-11-02) ---------------------
# Le point de P35-02 est lu dans la couche `controles` de l'export (le piquet de
# sommieR), plus dans `placettes`.
p <- points_ok; p$controles$lon[2L] <- p$controles$lon[2L] + 1e-4   # 7,5 m vers l'est
verifier(contient(controle_f4(attendu, controles_ok, p)$fautes, "P35-02, point de controles"),
         "F4 : contrôle de P35-02 déplacé de 7 m du piquet refusé (point lu dans controles)")
p <- points_ok; p$controles <- p$controles[1L, ]
verifier(contient(controle_f4(attendu, controles_ok, p)$fautes, "P35-02"),
         "F4 : contrôle de P35-02 absent de la couche controles de l'export refusé")
# La ligne de positions_controles de P35-02 : présente, une seule, géométrie
# NULL, sans_position_mesuree et rattache_par_identite vrais, precision_m NULL.
f2 <- function(p) controle_f4(attendu, controles_ok, p)$fautes
p <- points_ok; p$positions <- p$positions[1L, ]
f <- f2(p)
verifier(any(grepl("P35-02", f, fixed = TRUE) & grepl("aucune ligne de positions_controles", f, fixed = TRUE)),
         "F4 : ligne sans position de P35-02 retirée refusée (une ligne par contrôle)")
p <- points_ok; p$positions <- p$positions[c(1L, 2L, 2L), ]
verifier(any(grepl("P35-02", f2(p), fixed = TRUE) & grepl("positions_controles", f2(p), fixed = TRUE)),
         "F4 : deux lignes de positions_controles pour P35-02 refusées")
p <- points_ok; p$positions$lon[2L] <- att2$point$lon; p$positions$lat[2L] <- att2$point$lat
f <- f2(p)
verifier(any(grepl("P35-02", f, fixed = TRUE) & grepl("NULL", f, fixed = TRUE)),
         "F4 : géométrie remplie (le piquet) sur la ligne sans position refusée : elle doit être NULL")
p <- points_ok; p$positions$sans_position_mesuree[2L] <- FALSE
f <- f2(p)
verifier(any(grepl("P35-02", f, fixed = TRUE) & grepl("sans_position_mesuree", f, fixed = TRUE)),
         "F4 : sans_position_mesuree faux sur P35-02 refusé")
p <- points_ok; p$positions$sans_position_mesuree[2L] <- NA
verifier(any(grepl("P35-02", f2(p), fixed = TRUE) & grepl("sans_position_mesuree", f2(p), fixed = TRUE)),
         "F4 : sans_position_mesuree absent (NA) sur P35-02 refusé, jamais pris pour vrai")
p <- points_ok; p$positions$rattache_par_identite[2L] <- FALSE
f <- f2(p)
verifier(any(grepl("P35-02", f, fixed = TRUE) & grepl("rattache_par_identite", f, fixed = TRUE)),
         "F4 : rattache_par_identite faux sur P35-02 refusé (définition élargie)")
p <- points_ok; p$positions$precision_m[2L] <- 0
f <- f2(p)
verifier(any(grepl("P35-02", f, fixed = TRUE) & grepl("precision_m", f, fixed = TRUE)),
         "F4 : precision_m à 0 sur P35-02 refusé (NULL, jamais 0 : Darwin Core)")
# P35-01 a une position mesurée : sa ligne ne peut pas se dire sans position.
p <- points_ok; p$positions$sans_position_mesuree[1L] <- TRUE
f <- f2(p)
verifier(any(grepl("P35-01", f, fixed = TRUE) & grepl("sans_position_mesuree", f, fixed = TRUE)),
         "F4 : P35-01 marqué sans position alors qu'une position est attendue refusé")
# Le piquet de P35-02 dans `placettes` ne sert plus de point : le déplacer seul
# ne change rien (le point est celui du contrôle).
p <- points_ok; p$piquets$lon[2L] <- p$piquets$lon[2L] + 1e-4
verifier(!any(grepl("P35-02, point de controles", f2(p), fixed = TRUE)),
         "F4 : le point de P35-02 est celui de la couche controles, pas de placettes")
# Point attendu lui-même hors de France : le JSON serait faux.
att_faux <- attendu; att_faux$controles[[1L]]$point$lon <- 47.2591; att_faux$controles[[1L]]$point$lat <- 4.9325
verifier(contient(controle_f4(att_faux, controles_ok, points_ok)$fautes, "hors de la France métropolitaine"),
         "F4 : un point attendu hors de France est signalé")

# lire_points_export : EPSG:2154 -> (lon, lat) WGS 84. Un point du terrain
# (Couchey, x 846125,6 ; y 6686098 en Lambert-93) tombe à lon 4,9325, lat 47,259.
export_test <- file.path(travail, "export-test.gpkg")
sf::st_write(sf::st_as_sf(
  data.frame(controle_uuid = "R1", x = 846125.6, y = 6686098),
  coords = c("x", "y"), crs = 2154), export_test, layer = "positions_controles",
  quiet = TRUE, layer_options = "SPATIAL_INDEX=NO")
sf::st_write(sf::st_as_sf(
  data.frame(code_placette = "P35-02", x = 846162, y = 6686133),
  coords = c("x", "y"), crs = 2154), export_test, layer = "placettes",
  quiet = TRUE, layer_options = "SPATIAL_INDEX=NO")
pts_export <- lire_points_export(export_test)
verifier(abs(pts_export$positions$lon - 4.93250) < 1e-4 && abs(pts_export$positions$lat - 47.25910) < 1e-4,
         "lire_points_export : x 846125,6 ; y 6686098 (EPSG:2154) = lon 4,9325 ; lat 47,2591")
verifier(dans_france(pts_export$positions$lon, pts_export$positions$lat, attendu$france_metropolitaine),
         "lire_points_export : la placette tombe en France (pas en Somalie)")
verifier(identical(pts_export$positions$controle_uuid, "r1"), "lire_points_export : UUID en minuscules")
verifier(abs(pts_export$piquets$lon - 4.9330) < 1e-4 && abs(pts_export$piquets$lat - 47.2594) < 1e-4,
         "lire_points_export : piquet de P35-02 en (lon, lat)")
# Un export sans positions_controles : aucune ligne, pas d'erreur.
sans_pos <- file.path(travail, "sans-positions.gpkg")
sf::st_write(sf::st_as_sf(data.frame(code_placette = "P35-02", x = 846162, y = 6686133),
                          coords = c("x", "y"), crs = 2154), sans_pos,
             layer = "placettes", quiet = TRUE, layer_options = "SPATIAL_INDEX=NO")
verifier(nrow(lire_points_export(sans_pos)$positions) == 0L, "lire_points_export : sans couche positions_controles, aucune ligne")

# --- Mission 087 : un export de fabrication avec une ligne sans position -----
# Points posés en (lon, lat) WGS 84, convertis en EPSG:2154 (x = est, y = nord,
# mètres) comme l'export de Surculus. r1 : P35-01, position mesurée attendue ;
# r2 : P35-02, contrôle sans position mesurée (géométrie NULL dans
# positions_controles, piquet dans controles). UUID en minuscules.
en_l93 <- function(df) sf::st_transform(sf::st_as_sf(df, coords = c("lon", "lat"), crs = 4326), 2154)
export_sp <- file.path(travail, "export-sans-position.gpkg")
sf::st_write(en_l93(data.frame(controle_uuid = c("r1", "r2"),
                               precision_m = c(0.4, NA), sans_position_mesuree = c(FALSE, TRUE),
                               rattache_par_identite = c(FALSE, TRUE),
                               lon = c(att1$point$lon, att2$point$lon), lat = c(att1$point$lat, att2$point$lat))),
             export_sp, layer = "positions_controles", quiet = TRUE, layer_options = "SPATIAL_INDEX=NO")
sf::st_write(en_l93(data.frame(uuid = c("r1", "r2"),
                               lon = c(att1$point$lon, att2$point$lon), lat = c(att1$point$lat, att2$point$lat))),
             export_sp, layer = "controles", quiet = TRUE, layer_options = "SPATIAL_INDEX=NO")
sf::st_write(en_l93(data.frame(code_placette = c("P35-01", "P35-02"),
                               lon = c(4.9325, att2$point$lon), lat = c(47.2591, att2$point$lat))),
             export_sp, layer = "placettes", quiet = TRUE, layer_options = "SPATIAL_INDEX=NO")
# Géométrie NULL (pas un POINT EMPTY), comme l'écrit l'export (ADR 0003, point 7).
sql_sur(export_sp, "UPDATE positions_controles SET geom = NULL WHERE controle_uuid = 'r2'")
db_sp <- DBI::dbConnect(RSQLite::SQLite(), export_sp)
verifier(DBI::dbGetQuery(db_sp, "SELECT geom IS NULL AS n FROM positions_controles WHERE controle_uuid = 'r2'")$n == 1L,
         "fabrication : la ligne r2 a bien une géométrie NULL")
DBI::dbDisconnect(db_sp)
# Ce que l'ADR 0003 laisse « non vérifié » : sf lit une géométrie NULL de
# GeoPackage sans erreur, comme une géométrie vide.
lu_sf <- sf::st_read(export_sp, "positions_controles", quiet = TRUE)
verifier(nrow(lu_sf) == 2L && sf::st_is_empty(sf::st_geometry(lu_sf))[lu_sf$controle_uuid == "r2"],
         "sf lit la géométrie NULL de positions_controles comme vide, sans perdre la ligne")

pts_sp <- lire_points_export(export_sp)
verifier(nrow(pts_sp$positions) == 2L, "lire_points_export : la ligne sans position n'est pas perdue")
r2 <- pts_sp$positions[pts_sp$positions$controle_uuid == "r2", ]
verifier(nrow(r2) == 1L && is.na(r2$lon) && is.na(r2$lat),
         "lire_points_export : géométrie NULL -> lon et lat NA, jamais (0, 0)")
verifier(isTRUE(as.logical(r2$sans_position_mesuree)) && isTRUE(as.logical(r2$rattache_par_identite)) && is.na(r2$precision_m),
         "lire_points_export : sans_position_mesuree, rattache_par_identite et precision_m lus")
r1 <- pts_sp$positions[pts_sp$positions$controle_uuid == "r1", ]
verifier(isFALSE(as.logical(r1$sans_position_mesuree)) && abs(r1$precision_m - 0.4) < 1e-9,
         "lire_points_export : P35-01 avec position, sans_position_mesuree faux")
c2 <- pts_sp$controles[pts_sp$controles$uuid == "r2", ]
verifier(nrow(c2) == 1L && abs(c2$lon - 4.9330) < 1e-6 && abs(c2$lat - 47.2594) < 1e-6,
         "lire_points_export : point du contrôle de P35-02 lu dans controles, en (lon, lat)")
verifier(dans_france(c2$lon, c2$lat, attendu$france_metropolitaine),
         "lire_points_export : le contrôle sans position tombe en France (pas en Somalie)")
verifier(length(controle_f4(attendu, controles_ok, pts_sp)$fautes) == 0L,
         "F4 de bout en bout sur l'export de fabrication : conforme")

# controles_ecrits : lecture du registre (payload JSON) et du code de placette.
reg <- data.frame(
  id = c("E1", "e2", "e3"),
  payload = c(
    '{"type_entree":"controle","placette_id":"P-A","nb_total":5,"nb_vivants":3,"nb_abroutis":1,"h_moy_cm":39,"visite_le":"2026-10-20T09:30:00Z","releve_uuid":"R1"}',
    '{"type_entree":"placette","code_placette":"P35-01"}',
    '{"type_entree":"controle","placette_id":"p-b","nb_total":2,"nb_vivants":2,"visite_le":"2026-10-20T10:15:00Z"}'),
  stringsAsFactors = FALSE)
codes_reg <- data.frame(id = c("p-a", "p-b"), code_placette = c("P35-01", "P35-02"))
ce <- controles_ecrits(reg, c("E1", "e2", "e3"), codes_reg)
verifier(nrow(ce) == 2L && identical(ce$code, c("P35-01", "P35-02")),
         "controles_ecrits : les placettes ne sont pas des contrôles ; code par placette_id (casse ignorée)")
verifier(ce$nb_total[1L] == 5 && is.na(ce$h_moy_cm[2L]) && ce$releve_uuid[1L] == "r1",
         "controles_ecrits : totaux lus, champ absent = NA")
verifier(nrow(controles_ecrits(reg, "e3", codes_reg)) == 1L, "controles_ecrits : seules les entrées écrites par l'import")

# --- F5 : mesurer_sabotage (sans base : F2 et F3) ----------------------------
uuid_ecrits_test <- list(controles = character(0))
ctx_test <- list(depart_gpkg = depart, bilan_export = list(uuid_ecrits = uuid_ecrits_test))
# Contrôle « sabotage nul » : une copie intacte reste verte, le script le dit.
x <- variante("f5-intact")
r <- mesurer_sabotage(list(controle = "F2", import = FALSE), x, dirname(x), ctx_test)
verifier(!r$rouge && contient(r$message, "AUCUN ÉCART"), "F5 : une copie non sabotée reste verte, et le dit")
# Le motif visé : un contrôle rouge pour une AUTRE faute n'est pas un succès.
x <- variante("f5-motif", "UPDATE controles SET nb_total = 99 WHERE uuid = 'aaaa'")
r <- mesurer_sabotage(list(controle = "F2", import = FALSE, motif = "F2 : controles : 1 ligne\\(s\\) retirée"),
                      x, dirname(x), ctx_test)
verifier(r$rouge && !r$motif_vu && contient(r$message, "MOTIF INATTENDU"),
         "F5 : F2 rouge pour « modifiée » alors que « retirée » était visé : motif inattendu")
verifier(resultat_sabotage(r) == "ROUGE, MOTIF INATTENDU" && resultat_sabotage(r) != RESULTAT_OK,
         "F5 : un motif inattendu n'est pas un résultat conforme")
r <- mesurer_sabotage(list(controle = "F2", import = FALSE, motif = "F2 : controles : 1 ligne\\(s\\) modifiée"),
                      x, dirname(x), ctx_test)
verifier(r$rouge && r$motif_vu && resultat_sabotage(r) == RESULTAT_OK, "F5 : le bon motif est conforme")
verifier(resultat_sabotage(list(rouge = FALSE, motif_vu = FALSE)) == "VERT (non détecté)", "F5 : contrôle vert = écart")
verifier(all(vapply(liste_sabotages("a", "b"), function(s) !is.null(s$motif), logical(1))),
         "F5 : chaque sabotage déclare son motif")
x <- variante("f5-f2", "UPDATE controles SET nb_total = 99 WHERE uuid = 'aaaa'")
verifier(mesurer_sabotage(list(controle = "F2", import = FALSE), x, dirname(x), ctx_test)$rouge,
         "F5 : le sabotage F2 (ligne modifiée) rougit F2")
x <- variante("f5-f3", "CREATE TRIGGER sabotage_declencheur AFTER INSERT ON controles BEGIN SELECT 1; END")
verifier(mesurer_sabotage(list(controle = "F3", import = FALSE), x, dirname(x), ctx_test)$rouge,
         "F5 : le sabotage F3 (déclencheur) rougit F3")
verifier(!mesurer_sabotage(list(controle = "F3", import = FALSE), variante("f5-f3-intact"),
                           travail, ctx_test)$rouge, "F5 : F3 reste vert sans sabotage")
# Le déplacement de 5 m est supérieur à la tolérance de 0,5 m du JSON.
verifier(DEPLACEMENT_POINT_M > att1$point$tolerance_m, "F5 : le point est déplacé de plus que la tolérance")
# Sabotage du point sur un export de fabrication : décalage de 5 m en x, F4 rouge.
dossier_s <- file.path(travail, "export-saboter"); dir.create(dossier_s)
invisible(file.copy(export_test, file.path(dossier_s, "travaux.gpkg")))
sab <- liste_sabotages("autre", "r1")
s_point <- sab[[5L]]
verifier(identical(s_point$controle, "F4") && grepl("point", s_point$nom), "F5 : le 5e sabotage est le point déplacé")
s_point$modifier(file.path(dossier_s, "travaux.gpkg"))
apres <- lire_points_export(file.path(dossier_s, "travaux.gpkg"))
verifier(abs(distance_wgs84_m(pts_export$positions$lon, pts_export$positions$lat,
                              apres$positions$lon, apres$positions$lat) - DEPLACEMENT_POINT_M) < 0.05,
         "F5 : le point sabotage est à 5 m de l'original, en Lambert-93 conservé")
verifier(length(sab) == 7L && setequal(vapply(sab, function(s) s$controle, ""), c("F1", "F2", "F3", "F4")),
         "F5 : un sabotage au moins par contrôle F1 à F4, plus les deux de la ligne sans position (mission 087)")

# --- F5, mission 087 : la ligne sans position de P35-02 (sabotages 6 et 7) ---
# `controle_autre` est le contrôle de P35-02 (verifier.R), ici « r2 ».
sab_sp <- liste_sabotages("r2", "r1")
rejouer_f4 <- function(s) {
  d <- file.path(travail, paste0("export-sp-sabotage-", gsub("[^a-z0-9]", "-", tolower(s$nom))))
  dir.create(d, showWarnings = FALSE)
  g <- file.path(d, "travaux.gpkg")
  invisible(file.copy(export_sp, g, overwrite = TRUE))
  s$modifier(g)
  f <- controle_f4(attendu, controles_ok, lire_points_export(g))$fautes
  list(fautes = f, vues = grep(s$motif, f, value = TRUE))
}
s6 <- sab_sp[[6L]]
verifier(identical(s6$controle, "F4") && !isTRUE(s6$import) && grepl("retirée", s6$nom) && grepl("sans position", s6$nom),
         "F5 : le 6e sabotage retire la ligne sans position de positions_controles (F4, sans import)")
r6 <- rejouer_f4(s6)
verifier(length(r6$vues) > 0L, paste("F5 : ligne sans position retirée -> F4 rouge pour son motif ; fautes :",
                                     paste(r6$fautes, collapse = " | ")))
s7 <- sab_sp[[7L]]
verifier(identical(s7$controle, "F4") && !isTRUE(s7$import) && grepl("remplie", s7$nom),
         "F5 : le 7e sabotage remplit la géométrie de la ligne sans position (F4, sans import)")
r7 <- rejouer_f4(s7)
verifier(length(r7$vues) > 0L, paste("F5 : géométrie remplie -> F4 rouge pour son motif ; fautes :",
                                     paste(r7$fautes, collapse = " | ")))
verifier(!grepl("P35-01", s6$motif) && !grepl("P35-01", s7$motif) && grepl("P35-02", s6$motif) && grepl("P35-02", s7$motif),
         "F5 : les deux sabotages visent P35-02, pas P35-01")
# Sans sabotage, la copie reste verte : les deux motifs ne sont pas vus.
r0 <- rejouer_f4(list(nom = "intacte", modifier = function(g) invisible(g), motif = s6$motif))
verifier(length(r0$fautes) == 0L, "F5 : l'export de fabrication non saboté reste vert")
# Le sabotage du point (5e) ne touche pas la ligne sans position.
d5 <- file.path(travail, "export-sp-sabotage-5"); dir.create(d5, showWarnings = FALSE)
g5 <- file.path(d5, "travaux.gpkg"); invisible(file.copy(export_sp, g5, overwrite = TRUE))
sab_sp[[5L]]$modifier(g5)
apres5 <- lire_points_export(g5)
verifier(nrow(apres5$positions) == 2L && all(is.na(apres5$positions$lon[apres5$positions$controle_uuid == "r2"])),
         "F5 : déplacer le point de P35-01 laisse la ligne sans position sans géométrie")
verifier(contient(controle_f4(attendu, controles_ok, apres5)$fautes, "P35-01, point de positions_controles"),
         "F5 : le 5e sabotage rougit toujours F4 sur P35-01 quand une ligne sans position existe")

cat(n_tests, "vérifications réussies.\n")
