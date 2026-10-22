#!/usr/bin/env Rscript
# Vérification « import sommieR » (mission 007, ADR 0007 « Critère de CI »).
#
# Usage :
#   Rscript ci/sommier/verifier.R --sortie <dossier> [--etape tout|preparer|comparer]
#       [--modele <travaux.gpkg>] [--avec <export Surculus décompressé>]
#       [--bilan-export <bilan-export.json>] [--registre-attendu <json>]
#
# --etape preparer : crée la base de démo et la tournée de départ de Surculus
#   (<sortie>/tournee-depart, <sortie>/foret_id.txt), puis s'arrête. Surculus
#   (mission 006) part de cette tournée pour écrire son export.
# --etape comparer : reprend là. Défaut : « tout ».
# --modele : par défaut inst/qgis/travaux.gpkg du sommieR installé.
#
# Avec --avec (l'export de Surculus décompressé) et --bilan-export (le
# bilan-export.json écrit à côté, par :gpkg:exporterScenario) :
#   F1 le bilan d'import égale le bilan d'export, « déjà présents » compris ;
#   F2 les couches de la tournée de départ ne diffèrent de l'export que par des
#      ajouts, aux UUID du bilan d'export ;
#   F3 le schéma complet est identique à celui de la tournée de départ, aux six
#      couches ajoutées près (feature_count vérifié, last_change seul exclu) ;
#   F4 (avec --registre-attendu, mission 028) le registre de sommieR porte les
#      valeurs de registre-attendu-couchey.json (totaux et visite_le, par code de
#      placette) ; le point est comparé à `positions_controles` de l'export (voir
#      f4.R : pilotage/journal.md, 2026-10-22, décision de Pascal, mission 028) ;
#   F5 auto-test par sabotage : F1 à F4 passent au rouge sur une copie sabotée
#      de l'export (sabotage.R) ; un sabotage resté vert est un écart ;
#   (b) le schéma des couches de sommieR de l'export égale le modèle ;
#   (c) sf::st_layers() liste toutes les couches ajoutées.
#   Si l'import n'écrit rien (travaux + placettes + contrôles = 0), c'est un
#   écart : un vert qui ne mesure rien doit le dire.
#   Le témoin « sans » (export privé de ses six couches) n'est plus un critère :
#   c'est un avertissement (affiché, sans faire échouer).
#   Le résumé est écrit dans <sortie>/resume.md.
# Toujours : auto-test du retrait (six couches vides posées sur une copie de
#   la tournée de départ, retirées, plus aucune trace).
# Sans --avec : F1, F2 et F3 ne sont PAS mesurés, et le script le dit.
#
# Connexion : SOMMIER_CI_HOST (localhost), SOMMIER_CI_PORT (5432),
# SOMMIER_CI_USER (postgres), SOMMIER_CI_PASSWORD (obligatoire).
# Code de retour : 0 conforme ; 1 écart ; 2 mauvais usage ou étape manquante.

usage <- function(motif) {
  message(motif)
  message("Usage : verifier.R --sortie <dossier> [--etape ",
          "tout|preparer|comparer] [--modele <travaux.gpkg>] ",
          "[--avec <export décompressé>] [--bilan-export <json>] ",
          "[--registre-attendu <json>]")
  quit(status = 2)
}

args <- commandArgs(trailingOnly = TRUE)
connues <- c("--sortie", "--etape", "--modele", "--avec", "--bilan-export",
              "--registre-attendu")
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
source(file.path(dir_script, "criteres.R"))
source(file.path(dir_script, "f4.R"))
source(file.path(dir_script, "sabotage.R"))

modele <- option("--modele", system.file("qgis", "travaux.gpkg",
                                         package = "sommieR"))
sortie <- option("--sortie")
avec <- option("--avec")
fichier_bilan_export <- option("--bilan-export")
etape <- option("--etape", "tout")
fichier_attendu <- option("--registre-attendu")
if (!is.null(fichier_attendu) && !file.exists(fichier_attendu))
  usage(paste0("--registre-attendu : fichier absent : ", fichier_attendu))
if (modele == "" || is.null(sortie) ||
    !etape %in% c("tout", "preparer", "comparer"))
  usage("Arguments invalides.")
if (!is.null(avec) && etape != "preparer" &&
    (is.null(fichier_bilan_export) || !file.exists(fichier_bilan_export)))
  usage("--avec exige --bilan-export <bilan-export.json> existant (F1 et F2).")
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

# 3. Entrée, bilans.
if (!is.null(avec)) {
  if (!file.exists(file.path(avec, "travaux.gpkg")))
    usage(paste0("Pas de travaux.gpkg dans ", avec))
  entree <- avec
} else {
  entree <- depart
}
gpkg_depart <- file.path(depart, "travaux.gpkg")
gpkg_entree <- file.path(entree, "travaux.gpkg")

bilan_entree <- importer_et_bilanter(entree, "sommier_entree", "sommier_modele",
                                     foret_id)
ecrire_bilan(bilan_entree, file.path(sortie, "bilan-avec.json"))
message("Bilan (entrée) : ", bilan_entree$travaux, " travaux, ",
        bilan_entree$placettes, " placettes, ", bilan_entree$controles,
        " contrôles, ", bilan_entree$deja_presents, " déjà présents.")

# (b) schéma des couches de sommieR contre le modèle.
for (x in comparer_schema(gpkg_entree, modele))
  signaler("schéma : ", x)

resume <- c("## Import sommieR de l'export Surculus", "")
if (!is.null(avec)) {
  # Un vert qui ne mesure rien doit le dire.
  if (bilan_entree$travaux + bilan_entree$placettes +
      bilan_entree$controles == 0L)
    signaler("l'import n'a rien écrit (travaux + placettes + contrôles = 0) : ",
             "F1 ne mesure rien")
  # (c) couches ajoutées listées.
  for (x in couches_manquantes(gpkg_entree))
    signaler("couche ajoutée absente de st_layers : ", x)

  bilan_export <- jsonlite::fromJSON(fichier_bilan_export,
                                     simplifyVector = FALSE)
  # F1 : bilan d'import = bilan d'export.
  f1 <- controle_f1(bilan_entree, bilan_export, lignes_importables(gpkg_depart))
  # F2 : ajouts seuls, aux UUID du bilan d'export.
  f2 <- controle_f2(gpkg_depart, gpkg_entree,
                    lapply(bilan_export$uuid_ecrits, unlist))
  # F3 : schéma complet identique à celui de la tournée de départ.
  f3 <- list(fautes = controle_f3(gpkg_depart, gpkg_entree, COUCHES_AJOUTEES,
                                  f2$inserts))
  # F4 : le registre de sommieR porte les valeurs attendues (mission 028).
  attendu <- if (!is.null(fichier_attendu))
    jsonlite::fromJSON(fichier_attendu, simplifyVector = FALSE)
  f4 <- NULL
  if (!is.null(attendu)) {
    controles_registre <- controles_ecrits(
      bilan_entree$registre, bilan_entree$entrees_ecrites,
      bilan_entree$codes_placettes)
    f4 <- controle_f4(attendu, controles_registre,
                      lire_points_export(gpkg_entree))
  }
  tous_fautes <- c(f1$fautes, f2$fautes, f3$fautes, f4$fautes)
  for (x in tous_fautes) signaler(x)
  verdict <- function(fautes) if (length(fautes)) "ÉCHEC" else "vert"
  message("F1 bilan d'import = bilan d'export : ", verdict(f1$fautes))
  message("F2 ajouts seuls, aux UUID attendus : ", verdict(f2$fautes))
  message("F3 schéma identique à la tournée de départ : ", verdict(f3$fautes))
  if (!is.null(f4)) {
    message("F4 registre = valeurs attendues (027) : ", verdict(f4$fautes))
    message(gsub("[*`]", "", LIMITE_POSITION_SOMMIER))
    cat("::notice title=Position mesurée hors registre::",
        gsub("[*`]", "", LIMITE_POSITION_SOMMIER), "\n", sep = "")
  } else {
    message("F4 NON MESURÉ : pas de --registre-attendu.")
  }

  resume <- c(resume,
    sprintf("| Contrôle | Verdict |"), "|---|---|",
    sprintf("| F1 : le bilan d'import égale le bilan d'export, déjà présents compris | %s |", verdict(f1$fautes)),
    sprintf("| F2 : ajouts seuls dans les couches de la tournée de départ, aux UUID attendus | %s |", verdict(f2$fautes)),
    sprintf("| F3 : schéma complet identique à la tournée de départ (feature_count vérifié) | %s |", verdict(f3$fautes)),
    if (!is.null(f4))
      sprintf("| F4 : le registre de sommieR porte les valeurs attendues (mission 027), ligne à ligne | %s |", verdict(f4$fautes))
    else "| F4 : registre attendu | NON MESURÉ (pas de --registre-attendu) |",
    "", "F1, compteurs :", "",
    "| Compteur | Export Surculus (attendu) | Import sommieR (mesuré) |", "|---|---|---|",
    sprintf("| %s | %s | %s |", f1$lignes$compteur, f1$lignes$attendu, f1$lignes$mesure),
    "", "F2, lignes ajoutées par couche de la tournée de départ :", "",
    "| Couche | Lignes ajoutées |", "|---|---|",
    if (length(f2$inserts)) sprintf("| %s | %d |", names(f2$inserts), f2$inserts))
  if (!is.null(f4))
    resume <- c(resume, "", "F4, registre contre attentes (écrites à la main, mission 027) :", "",
      LIMITE_POSITION_SOMMIER, "",
      "| Placette | Champ | Attendu | Mesuré | Verdict |", "|---|---|---|---|---|",
      if (nrow(f4$lignes))
        sprintf("| %s | %s | %s | %s | %s |", f4$lignes$placette, f4$lignes$champ,
                f4$lignes$attendu, f4$lignes$mesure, f4$lignes$verdict),
      "", LIGNE_PIQUET)
  if (length(tous_fautes))
    resume <- c(resume, "", "Écarts :", "", paste0("- ", tous_fautes))

  # Vérification de fumée (plus un critère) : le même export privé de ses six
  # couches s'importe avec le même bilan.
  temoin <- retirer_couches(entree, file.path(sortie, "temoin-sans"))
  bilan_temoin <- importer_et_bilanter(temoin, "sommier_temoin", "sommier_modele",
                                       foret_id)
  ecrire_bilan(bilan_temoin, file.path(sortie, "bilan-sans.json"))
  # Un avertissement, pas un écart : il n'entre pas dans le verdict.
  diff_fumee <- comparer_bilans(bilan_entree, bilan_temoin)
  avert <- avertissement_temoin(diff_fumee)
  if (length(diff_fumee))
    message("AVERTISSEMENT (fumée) : bilan différent entre l'export et son ",
            "témoin sans couches ajoutées : ", paste(diff_fumee, collapse = ", "))
  if (!is.null(avert$annotation)) cat(avert$annotation, "\n", sep = "")
  resume <- c(resume, "", avert$resume)

  # F5 : auto-test par sabotage (mission 028). Chaque contrôle doit rougir.
  if (!is.null(f4)) {
    f5 <- tryCatch({
      ce <- controles_registre
      p1 <- ce$releve_uuid[ce$code %in% "P35-01"]
      autre <- setdiff(ce$releve_uuid, p1)[1L]
      if (length(p1) != 1L || is.na(autre))
        stop("P35-01 et un second contrôle sont introuvables au registre")
      autotest_sabotage(
        entree, file.path(sortie, "sabotage"),
        list(depart_gpkg = gpkg_depart, bilan_export = bilan_export,
             attendu = attendu, foret_id = foret_id,
             bilan_propre = bilan_entree, nom_base = "sommier_sabotage"),
        controle_autre = autre, controle_p35_01 = p1)
    }, error = function(e) {
      signaler("F5 : auto-test par sabotage interrompu : ", conditionMessage(e))
      NULL })
    if (!is.null(f5)) {
      for (i in which(!f5$resultat == RESULTAT_OK))
        signaler("F5 : sabotage « ", f5$sabotage[i], " » : ", f5$controle[i],
                 " : ", f5$resultat[i], " (", f5$message[i], ")")
      message("F5 sabotages : ", sum(f5$resultat == RESULTAT_OK), " sur ",
              nrow(f5), " détectés.")
      resume <- c(resume, "",
        "F5, auto-test par sabotage (chaque contrôle doit passer au rouge, pour le motif visé, sur une copie sabotée de l'export) :", "",
        "| Sabotage | Contrôle visé | Résultat | Message du contrôle |", "|---|---|---|---|",
        sprintf("| %s | %s | %s | %s |", f5$sabotage, f5$controle, f5$resultat,
                gsub("\\|", "/", f5$message)))
    }
  } else {
    message("F5 NON MESURÉ : pas de --registre-attendu.")
  }
} else {
  message("F1 À F5 NON MESURÉS : pas d'export Surculus (--avec). ",
          "Le retrait des couches est éprouvé (auto-test), pas l'export.")
  resume <- c(resume, "F1 à F5 non mesurés : pas d'export Surculus.")
}
writeLines(c(resume, "", "Le détail est dans l'artefact bilans-import-sommier."),
           file.path(sortie, "resume.md"))

if (length(ecarts) > 0L) {
  message(length(ecarts), " écart(s).")
  quit(status = 1)
}
message("Conforme.")
