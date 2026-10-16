#!/usr/bin/env Rscript
# Vérification « import sommieR » (mission 007, ADR 0007 « Critère de CI »).
#
# Usage :
#   Rscript ci/sommier/verifier.R --sortie <dossier> \
#       [--modele <travaux.gpkg>] [--avec <export Surculus décompressé>]
# --modele : par défaut inst/qgis/travaux.gpkg du sommieR installé.
#
# Sans --avec : sommieR engendre lui-même la tournée de la démo Couchey
# (« sans » couches ajoutées) ; on l'importe, on écrit le bilan et on compare
# son schéma au modèle. Avec --avec : on importe aussi l'export décompressé de
# Surculus, et on compare ses bilan et schéma à ceux de « sans ».
#
# --etape preparer : crée la base de démo et la tournée « sans » dans <sortie>,
# puis s'arrête (<sortie>/tournee-sans, <sortie>/foret_id.txt) ; --etape
# comparer : reprend là, importe et compare ; défaut « tout ».
#
# Connexion : SOMMIER_CI_HOST (localhost), SOMMIER_CI_PORT (5432),
# SOMMIER_CI_USER (postgres), SOMMIER_CI_PASSWORD (obligatoire).
# Code de retour : 0 tout est identique ; 1 un écart ; 2 mauvais usage.

args <- commandArgs(trailingOnly = TRUE)
option <- function(nom, defaut = NULL) {
  i <- match(nom, args)
  if (is.na(i)) return(defaut)
  if (i == length(args)) {
    message("Valeur absente pour ", nom)
    quit(status = 2)
  }
  args[[i + 1L]]
}
dir_script <- dirname(sub("^--file=", "",
  grep("^--file=", commandArgs(FALSE), value = TRUE)[1L]))
source(file.path(dir_script, "fonctions.R"))

modele <- option("--modele", system.file("qgis", "travaux.gpkg",
                                         package = "sommieR"))
sortie <- option("--sortie")
avec <- option("--avec")
etape <- option("--etape", "tout")
if (modele == "" || is.null(sortie) ||
    !etape %in% c("tout", "preparer", "comparer")) {
  message("Usage : verifier.R --sortie <dossier> [--modele <travaux.gpkg>] ",
          "[--avec <export décompressé>] ",
          "[--etape tout|preparer|comparer]")
  quit(status = 2)
}
dir.create(sortie, showWarnings = FALSE, recursive = TRUE)
ecarts <- character(0)
signaler <- function(...) {
  ecarts <<- c(ecarts, paste0(...))
  message("ÉCART : ", paste0(...))
}

# 1. Base modèle : schéma de sommieR + démo Couchey, puis tournée « sans ».
# Étape « preparer » : à part, pour que Surculus (mission 006) puisse partir de
# CETTE tournée -- chaque démo a ses propres UUID de forêt et de placettes.
fichier_foret <- file.path(sortie, "foret_id.txt")
sans <- file.path(sortie, "tournee-sans")
if (etape %in% c("tout", "preparer")) {
  admin <- connecter("postgres")
  DBI::dbExecute(admin, 'DROP DATABASE IF EXISTS "sommier_modele"')
  DBI::dbExecute(admin, 'CREATE DATABASE "sommier_modele"')
  DBI::dbDisconnect(admin)
  con <- connecter("sommier_modele")
  DBI::dbExecute(con, "CREATE EXTENSION IF NOT EXISTS postgis")
  sommieR::sommier_init_schema(con)
  demo <- sommieR::sommier_demo_couchey(con)
  unlink(sans, recursive = TRUE)
  sommieR::sommier_projet_qfield_travaux(con, demo$foret_id, sans,
                                         operateur = "ci-import-sommier")
  DBI::dbDisconnect(con)
  writeLines(demo$foret_id, fichier_foret)
  message("sommieR ", as.character(utils::packageVersion("sommieR")),
          " ; GDAL ", sf::sf_extSoftVersion()[["GDAL"]])
  message("Tournée sans : ", sans)
  if (etape == "preparer") quit(status = 0)
} else if (!file.exists(fichier_foret) ||
           !file.exists(file.path(sans, "travaux.gpkg"))) {
  stop("Étape « preparer » non faite dans ", sortie, call. = FALSE)
}
demo <- list(foret_id = readLines(fichier_foret, n = 1L))

# 2. Import de « sans » dans une base clonée du modèle, et bilan.
bilan_sans <- importer_et_bilanter(sans, "sommier_sans", "sommier_modele",
                                   demo$foret_id)
ecrire_bilan(bilan_sans, file.path(sortie, "bilan-sans.json"))
message("Bilan sans : ", bilan_sans$travaux, " travaux, ",
        bilan_sans$placettes, " placettes, ", bilan_sans$controles,
        " contrôles, ", bilan_sans$deja_presents, " déjà présents.")

# 3. (b) Schéma des couches de sommieR contre le modèle.
for (x in comparer_schema(file.path(sans, "travaux.gpkg"), modele))
  signaler("schéma (sans) : ", x)

# 4. « avec » : l'export décompressé de Surculus.
if (!is.null(avec)) {
  if (!file.exists(file.path(avec, "travaux.gpkg")))
    stop("Pas de travaux.gpkg dans ", avec, call. = FALSE)
  bilan_avec <- importer_et_bilanter(avec, "sommier_avec", "sommier_modele",
                                     demo$foret_id)
  ecrire_bilan(bilan_avec, file.path(sortie, "bilan-avec.json"))
  # (a) bilans identiques
  for (k in comparer_bilans(bilan_sans, bilan_avec))
    signaler("bilan : « ", k, " » diffère entre sans et avec")
  # (b) schéma inchangé
  for (x in comparer_schema(file.path(avec, "travaux.gpkg"), modele))
    signaler("schéma (avec) : ", x)
  # (c) couches ajoutées listées
  for (x in couches_manquantes(file.path(avec, "travaux.gpkg")))
    signaler("couche ajoutée absente de st_layers : ", x)
} else {
  message("Pas d'export Surculus (--avec) : comparaison avec/sans non faite.")
}

if (length(ecarts) > 0L) {
  message(length(ecarts), " écart(s).")
  quit(status = 1)
}
message("Tout est identique.")
