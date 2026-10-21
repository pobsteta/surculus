#!/usr/bin/env Rscript
# Tests de F1, F2 et F3 (mission 026) sur de petits GeoPackage fabriqués ici :
# aucune base PostgreSQL requise. Usage : Rscript ci/sommier/test-criteres.R
# Chaque cas dit ce qu'il attend ; le script s'arrête au premier écart.

dir_script <- dirname(sub("^--file=", "",
  grep("^--file=", commandArgs(FALSE), value = TRUE)[1L]))
source(file.path(dir_script, "fonctions.R"))
source(file.path(dir_script, "criteres.R"))

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

cat(n_tests, "vérifications réussies.\n")
