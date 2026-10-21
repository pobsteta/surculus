#!/usr/bin/env Rscript
# Vérification « import sommieR » (mission 007, ADR 0007 « Critère de CI »).
#
# Usage :
#   Rscript ci/sommier/verifier.R --sortie <dossier> [--etape tout|preparer|comparer]
#       [--modele <travaux.gpkg>] [--avec <export Surculus décompressé>]
#
# --etape preparer : crée la base de démo et la tournée de départ de Surculus
#   (<sortie>/tournee-depart, <sortie>/foret_id.txt), puis s'arrête. Surculus
#   (mission 006) part de cette tournée pour écrire son export.
# --etape comparer : reprend là. Défaut : « tout ».
# --modele : par défaut inst/qgis/travaux.gpkg du sommieR installé.
#
# Avec --avec (l'export de Surculus décompressé) :
#   (a) le témoin « sans » est une copie du MÊME export, privée de ses six
#       couches ajoutées ; les deux versions sont importées dans deux clones
#       du modèle et leurs bilans doivent être identiques ;
#   (b) le schéma des couches de sommieR de l'export égale le modèle ;
#   (c) sf::st_layers() liste toutes les couches ajoutées.
#   Si l'import n'écrit rien (travaux + placettes + contrôles = 0), c'est un
#   écart : un vert qui ne mesure rien doit le dire.
# Toujours : auto-test du retrait (six couches vides posées sur une copie de
#   la tournée de départ, retirées, plus aucune trace).
# Sans --avec : le critère (a) n'est PAS mesuré, et le script le dit.
#
# Connexion : SOMMIER_CI_HOST (localhost), SOMMIER_CI_PORT (5432),
# SOMMIER_CI_USER (postgres), SOMMIER_CI_PASSWORD (obligatoire).
# Code de retour : 0 conforme ; 1 écart ; 2 mauvais usage ou étape manquante.

usage <- function(motif) {
  message(motif)
  message("Usage : verifier.R --sortie <dossier> [--etape ",
          "tout|preparer|comparer] [--modele <travaux.gpkg>] ",
          "[--avec <export décompressé>]")
  quit(status = 2)
}

args <- commandArgs(trailingOnly = TRUE)
connues <- c("--sortie", "--etape", "--modele", "--avec")
noms <- args[seq_along(args) %% 2L == 1L]
if (length(args) %% 2L != 0L || !all(noms %in% connues))
  usage(paste0("Option inconnue ou valeur absente : ",
               paste(args, collapse = " ")))
if (anyDuplicated(noms))
  usage(paste0("Option donnée deux fois : ",
               paste(unique(noms[duplicated(noms)]), collapse = ", ")))
option <- function(nom, defaut = NULL) {
  i <- match(nom, noms)
  if (is.na(i)) defaut else args[[2L * i]]
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
    !etape %in% c("tout", "preparer", "comparer"))
  usage("Arguments invalides.")
dir.create(sortie, showWarnings = FALSE, recursive = TRUE)
ecarts <- character(0)
signaler <- function(...) {
  ecarts <<- c(ecarts, paste0(...))
  message("ÉCART : ", paste0(...))
}

# 1. Base modèle : schéma de sommieR + démo Couchey, puis la tournée de départ.
# Étape « preparer » à part : chaque démo a ses propres UUID de forêt et de
# placettes, et l'export de Surculus doit partir de CETTE tournée.
fichier_foret <- file.path(sortie, "foret_id.txt")
depart <- file.path(sortie, "tournee-depart")
if (etape %in% c("tout", "preparer")) {
  admin <- connecter("postgres")
  DBI::dbExecute(admin, 'DROP DATABASE IF EXISTS "sommier_modele"')
  DBI::dbExecute(admin, 'CREATE DATABASE "sommier_modele"')
  DBI::dbDisconnect(admin)
  con <- connecter("sommier_modele")
  DBI::dbExecute(con, "CREATE EXTENSION IF NOT EXISTS postgis")
  sommieR::sommier_init_schema(con)
  demo <- sommieR::sommier_demo_couchey(con)
  unlink(depart, recursive = TRUE)
  sommieR::sommier_projet_qfield_travaux(con, demo$foret_id, depart,
                                         operateur = "ci-import-sommier")
  DBI::dbDisconnect(con)
  writeLines(demo$foret_id, fichier_foret)
  message("sommieR ", as.character(utils::packageVersion("sommieR")),
          " ; GDAL ", sf::sf_extSoftVersion()[["GDAL"]])
  message("Tournée de départ : ", depart)
  if (etape == "preparer") quit(status = 0)
} else if (!file.exists(fichier_foret) ||
           !file.exists(file.path(depart, "travaux.gpkg"))) {
  usage(paste0("Étape « preparer » non faite dans ", sortie))
}
foret_id <- readLines(fichier_foret, n = 1L)

# 2. Auto-test du retrait : six couches vides posées sur une copie de la
# tournée de départ, retirées, rien ne doit rester.
for (x in autotest_retrait(depart, file.path(sortie, "autotest")))
  signaler("retrait : ", x)
message("Auto-test du retrait : ",
        if (length(ecarts)) "ÉCHEC" else "réussi",
        " (six couches posées puis retirées).")

# 3. Entrée, témoin, bilans.
if (!is.null(avec)) {
  if (!file.exists(file.path(avec, "travaux.gpkg")))
    usage(paste0("Pas de travaux.gpkg dans ", avec))
  entree <- avec
} else {
  entree <- depart
}
temoin <- retirer_couches(entree, file.path(sortie, "temoin-sans"))
# TEMPORAIRE (démonstration ii, mission 022) : simule une couche ajoutée dont le
# retrait entraîne une couche de sommieR (ici : les lignes de « controles »).
local({
  db <- DBI::dbConnect(RSQLite::SQLite(), file.path(temoin, "travaux.gpkg"))
  on.exit(DBI::dbDisconnect(db))
  DBI::dbExecute(db, "DELETE FROM controles")
})

bilan_entree <- importer_et_bilanter(entree, "sommier_entree", "sommier_modele",
                                     foret_id)
bilan_temoin <- importer_et_bilanter(temoin, "sommier_temoin", "sommier_modele",
                                     foret_id)
ecrire_bilan(bilan_entree, file.path(sortie, "bilan-avec.json"))
ecrire_bilan(bilan_temoin, file.path(sortie, "bilan-sans.json"))
message("Bilan (entrée) : ", bilan_entree$travaux, " travaux, ",
        bilan_entree$placettes, " placettes, ", bilan_entree$controles,
        " contrôles, ", bilan_entree$deja_presents, " déjà présents.")

# (a) bilans identiques, entrée et témoin privé de ses couches ajoutées.
for (k in comparer_bilans(bilan_entree, bilan_temoin))
  signaler("bilan : « ", k, " » diffère entre avec et sans couches ajoutées")

# (b) schéma des couches de sommieR contre le modèle.
for (x in comparer_schema(file.path(entree, "travaux.gpkg"), modele))
  signaler("schéma : ", x)

if (!is.null(avec)) {
  # Un vert qui ne mesure rien doit le dire.
  if (bilan_entree$travaux + bilan_entree$placettes +
      bilan_entree$controles == 0L)
    signaler("l'import n'a rien écrit (travaux + placettes + contrôles = 0) : ",
             "le critère (a) ne mesure rien")
  # (c) couches ajoutées listées.
  for (x in couches_manquantes(file.path(avec, "travaux.gpkg")))
    signaler("couche ajoutée absente de st_layers : ", x)
} else {
  message("CRITÈRE (a) NON MESURÉ : pas d'export Surculus (--avec). ",
          "Le retrait des couches est éprouvé (auto-test), pas la ",
          "comparaison des bilans.")
}

if (length(ecarts) > 0L) {
  message(length(ecarts), " écart(s).")
  quit(status = 1)
}
message("Conforme.")
