# Auto-test par sabotage (mission 028, critère F5 ; décision de Pascal du
# 2026-10-21, « garde-fou = test vu rouge »). Chaque contrôle de la CI (F1 à
# F4) est rejoué sur une COPIE sabotée de l'export de Surculus et doit passer
# au rouge. Un sabotage qui reste vert est un écart : le contrôle ne contrôle
# rien. Même esprit qu'autotest_retrait() (fonctions.R).
#
#   F1  un contrôle retiré de la couche `controles` : l'import en écrit un de
#       moins que le bilan d'export n'en annonce ;
#   F2  une ligne existante modifiée (couche `codes` de la tournée de départ) ;
#   F3  un déclencheur ajouté (sur `codes`, couche de sommieR) ;
#   F4  un total faux (nb_total de P35-01 + 1 dans `controles`, importé par
#       sommieR : le registre porte le faux total) ;
#   F4  un point déplacé de 5 m (> 0,5 m, décision de Pascal, mission 027) dans
#       `positions_controles`.
#
# Coordonnées : les couches sont en EPSG:2154, x (est) puis y (nord), en
# mètres ; le déplacement est de +5 m en x.

DEPLACEMENT_POINT_M <- 5

copier_export <- function(export_dir, destination) {
  unlink(destination, recursive = TRUE)
  dir.create(destination, recursive = TRUE)
  file.copy(list.files(export_dir, full.names = TRUE, all.files = TRUE,
                       no.. = TRUE), destination, recursive = TRUE)
  invisible(file.path(destination, "travaux.gpkg"))
}

sql_sur <- function(gpkg, requetes) {
  db <- DBI::dbConnect(RSQLite::SQLite(), gpkg)
  on.exit(DBI::dbDisconnect(db))
  for (q in requetes) DBI::dbExecute(db, q)
  invisible(gpkg)
}

sans_declencheurs_rtree <- function(gpkg, table) {
  db <- DBI::dbConnect(RSQLite::SQLite(), gpkg)
  on.exit(DBI::dbDisconnect(db))
  t <- DBI::dbGetQuery(db, "SELECT name FROM sqlite_master WHERE type = 'trigger'")$name
  t <- t[startsWith(t, paste0("rtree_", table, "_"))]
  for (x in t) DBI::dbExecute(db, paste0("DROP TRIGGER ", q_ident(x)))
  invisible(t)
}

# Les sabotages : `controle` est le contrôle qui doit rougir ; `modifier` reçoit
# le chemin du GeoPackage copié ; `import` dit si le contrôle a besoin d'un
# import par sommieR dans une base (F1 et F4 « total »).
liste_sabotages <- function(controle_autre, controle_p35_01) {
  list(
    list(nom = "contrôle retiré de l'export", controle = "F1", import = TRUE,
         modifier = function(gpkg) sql_sur(gpkg, sprintf(
           "DELETE FROM controles WHERE uuid = '%s'", controle_autre))),
    list(nom = "ligne existante modifiée (codes)", controle = "F2",
         import = FALSE,
         modifier = function(gpkg) sql_sur(gpkg,
           "UPDATE codes SET libelle = 'sabotage' WHERE code = 'PL'")),
    list(nom = "déclencheur ajouté (codes)", controle = "F3", import = FALSE,
         modifier = function(gpkg) sql_sur(gpkg,
           "CREATE TRIGGER sabotage_declencheur AFTER INSERT ON codes BEGIN SELECT 1; END")),
    list(nom = "total faux (nb_total de P35-01 + 1)", controle = "F4",
         import = TRUE,
         modifier = function(gpkg) {
           # RSQLite n'a pas ST_IsEmpty, appelée par les déclencheurs R-tree
           # d'UPDATE de `controles` : on les retire de la COPIE (F3 n'est pas
           # rejoué sur ce sabotage).
           sans_declencheurs_rtree(gpkg, "controles")
           sql_sur(gpkg, sprintf(
             "UPDATE controles SET nb_total = nb_total + 1 WHERE uuid = '%s'",
             controle_p35_01))
         }),
    list(nom = sprintf("point déplacé de %s m (positions_controles)",
                       DEPLACEMENT_POINT_M), controle = "F4", import = FALSE,
         modifier = function(gpkg) {
           p <- sf::st_read(gpkg, "positions_controles", quiet = TRUE)
           crs <- sf::st_crs(p)              # EPSG:2154, conservé
           sf::st_geometry(p) <- sf::st_geometry(p) +
             c(DEPLACEMENT_POINT_M, 0)       # x (est), y (nord), mètres
           sf::st_crs(p) <- crs
           sf::st_write(p, gpkg, layer = "positions_controles",
                        delete_layer = TRUE, quiet = TRUE)
         })
  )
}

# Rejoue le contrôle visé sur le GeoPackage saboté. `ctx` : depart_gpkg,
# bilan_export, attendu, foret_id, bilan_propre (import de l'export non saboté),
# nom_base. Rend list(rouge, message).
mesurer_sabotage <- function(s, gpkg, dossier, ctx) {
  uuid_ecrits <- lapply(ctx$bilan_export$uuid_ecrits, unlist)
  bilan <- if (s$import)
    importer_et_bilanter(dossier, ctx$nom_base, "sommier_modele", ctx$foret_id)
  else ctx$bilan_propre
  fautes <- switch(
    s$controle,
    F1 = controle_f1(bilan, ctx$bilan_export,
                     lignes_importables(ctx$depart_gpkg))$fautes,
    F2 = controle_f2(ctx$depart_gpkg, gpkg, uuid_ecrits)$fautes,
    F3 = controle_f3(ctx$depart_gpkg, gpkg, COUCHES_AJOUTEES,
                     controle_f2(ctx$depart_gpkg, gpkg, uuid_ecrits)$inserts),
    F4 = controle_f4(ctx$attendu,
                     controles_ecrits(bilan$registre, bilan$entrees_ecrites,
                                      bilan$codes_placettes),
                     lire_points_export(gpkg))$fautes)
  list(rouge = length(fautes) > 0L,
       message = if (length(fautes)) fautes[[1L]] else "AUCUN ÉCART : le contrôle est resté vert")
}

# Fait tous les sabotages. Rend un data.frame (sabotage, controle, resultat,
# message) ; une ligne « resultat = vert » est un écart du script lui-même.
autotest_sabotage <- function(export_dir, travail, ctx, controle_autre,
                              controle_p35_01) {
  sab <- liste_sabotages(controle_autre, controle_p35_01)
  lignes <- lapply(seq_along(sab), function(i) {
    s <- sab[[i]]
    dossier <- file.path(travail, paste0("sabotage-", i))
    gpkg <- copier_export(export_dir, dossier)
    s$modifier(gpkg)
    r <- mesurer_sabotage(s, gpkg, dossier, ctx)
    data.frame(sabotage = s$nom, controle = s$controle,
               resultat = if (r$rouge) "rouge (détecté)" else "VERT (non détecté)",
               message = r$message, stringsAsFactors = FALSE)
  })
  do.call(rbind, lignes)
}
