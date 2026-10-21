# Critères F1, F2 et F3 de la CI d'import sommieR (mission 026, ADR 0007,
# « Critère de CI »). Ils remplacent la comparaison « avec / sans » (ancien
# critère (a)) par trois mesures de l'export de Surculus.
#
#   F1 le bilan d'import de sommieR égale le bilan d'export de Surculus
#      (bilan-export.json), « déjà présents » compris ;
#   F2 les couches de la tournée de départ ne diffèrent de l'export que par
#      des ajouts, aux UUID attendus (ceux du bilan d'export) ;
#   F3 le schéma complet (sqlite_master, lignes de gpkg_*, application_id,
#      user_version) est identique à celui de la tournée de départ, aux seuls
#      ajouts des six couches près ; feature_count est vérifié, pas normalisé.
#
# Aucune dépendance au-delà de DBI, RSQLite, jsonlite. Chaque fonction rend la
# liste des fautes (vide : conforme), jamais un booléen.

# Couches que sommieR importe (R/travaux_qfield.R : COUCHES_TRAVAUX, placettes,
# controles). Leur nombre de lignes de départ est le « déjà présents » attendu.
COUCHES_IMPORTEES <- c("travaux_surf", "travaux_lin", "travaux_pt",
                       "placettes", "controles")

# Support de l'extension Schema (ADR 0007, règle « Conformité GeoPackage ») :
# deux tables et les lignes qui les déclarent. Seules additions tolérées
# hors des six couches.
TABLES_EXTENSION_SCHEMA <- c("gpkg_data_columns", "gpkg_data_column_constraints")

# La SEULE colonne exclue de la comparaison (décision de Pascal, 2026-10-21) :
# l'horodatage de dernière modification de la couche, qui change à chaque
# écriture. Tout autre écart est une faute.
COLONNES_EXCLUES <- list(gpkg_contents = "last_change")

ouvrir_ro <- function(chemin)
  DBI::dbConnect(RSQLite::SQLite(), chemin, flags = RSQLite::SQLITE_RO)

q_ident <- function(x) paste0('"', gsub('"', '""', x), '"')

# Tables de données du GeoPackage (hors gpkg_*, rtree_*, sqlite_*).
tables_donnees <- function(db) {
  t <- DBI::dbGetQuery(
    db, "SELECT name FROM sqlite_master WHERE type = 'table'
            AND name NOT LIKE 'gpkg\\_%' ESCAPE '\\'
            AND name NOT LIKE 'rtree\\_%' ESCAPE '\\'
            AND name NOT LIKE 'sqlite\\_%' ESCAPE '\\' ORDER BY name")$name
  t
}

# ------------------------------------------------------------------ F1 -----

# Lignes de la tournée de départ que sommieR trouvera déjà au registre : la
# tournée est tirée du registre, toutes ses lignes (travaux, placettes,
# contrôles) y sont, donc comptées « déjà présentes ».
lignes_importables <- function(depart_gpkg) {
  db <- ouvrir_ro(depart_gpkg)
  on.exit(DBI::dbDisconnect(db))
  n <- 0L
  for (t in intersect(COUCHES_IMPORTEES, tables_donnees(db)))
    n <- n + DBI::dbGetQuery(db, sprintf(
      "SELECT count(*) AS n FROM %s WHERE uuid IS NOT NULL", q_ident(t)))$n
  as.integer(n)
}

# `bilan_import` : bilan de sommier_importer_qfield_travaux() (liste de
# importer_et_bilanter) ; `bilan_export` : liste lue de bilan-export.json ;
# `deja_attendus` : lignes_importables() de la tournée de départ.
# Rend list(fautes, lignes) ; `lignes` : tableau compteur / attendu / mesuré.
controle_f1 <- function(bilan_import, bilan_export, deja_attendus) {
  champ <- function(x, k) {
    v <- x[[k]]
    if (is.null(v) || length(v) != 1L || is.na(v)) NA_integer_ else as.integer(v)
  }
  ecrites <- length(unlist(bilan_import$entrees_ecrites))
  lignes <- data.frame(
    compteur = c("travaux", "placettes", "contrôles", "déjà présents",
                 "entrées écrites"),
    attendu = c(champ(bilan_export, "travaux"),
                champ(bilan_export, "placettes"),
                champ(bilan_export, "controles"),
                as.integer(deja_attendus),
                sum(champ(bilan_export, "travaux"),
                    champ(bilan_export, "placettes"),
                    champ(bilan_export, "controles"))),
    mesure = c(champ(bilan_import, "travaux"),
               champ(bilan_import, "placettes"),
               champ(bilan_import, "controles"),
               champ(bilan_import, "deja_presents"),
               as.integer(ecrites)),
    stringsAsFactors = FALSE)
  # Une valeur absente d'un côté n'est jamais une égalité.
  faux <- is.na(lignes$attendu) | is.na(lignes$mesure) |
    lignes$attendu != lignes$mesure
  fautes <- if (any(faux))
    sprintf("F1 : %s, export %s, import %s", lignes$compteur[faux],
            lignes$attendu[faux], lignes$mesure[faux]) else character(0)
  list(fautes = fautes, lignes = lignes)
}

# ------------------------------------------------------------------ F2 -----

# Compare chaque table de données de la tournée de départ à l'export.
# `attendus` : liste nommée par table des UUID qu'on attend voir AJOUTÉS
# (minuscules). Rend list(fautes, inserts) ; `inserts` : entier nommé, nombre
# de lignes ajoutées par table (sert à F3 pour feature_count).
# Une ligne est ajoutée si sa clé primaire est absente du départ ; retirée si
# sa clé a disparu ; modifiée si la clé existe des deux côtés mais que la ligne
# diffère (comparaison de toutes les colonnes, géométrie comprise).
controle_f2 <- function(depart_gpkg, export_gpkg, attendus = list()) {
  fautes <- character(0)
  inserts <- integer(0)
  db <- ouvrir_ro(export_gpkg)
  on.exit(DBI::dbDisconnect(db))
  DBI::dbExecute(db, sprintf("ATTACH DATABASE %s AS depart",
                             DBI::dbQuoteString(db, normalizePath(depart_gpkg))))
  tables_depart <- DBI::dbGetQuery(
    db, "SELECT name FROM depart.sqlite_master WHERE type = 'table'
            AND name NOT LIKE 'gpkg\\_%' ESCAPE '\\'
            AND name NOT LIKE 'rtree\\_%' ESCAPE '\\'
            AND name NOT LIKE 'sqlite\\_%' ESCAPE '\\' ORDER BY name")$name
  tables_export <- tables_donnees(db)
  for (t in tables_depart) {
    if (!t %in% tables_export) {
      fautes <- c(fautes, paste0("F2 : ", t, " absente de l'export"))
      next
    }
    qt <- q_ident(t)
    cd <- DBI::dbGetQuery(db, sprintf("PRAGMA depart.table_info(%s)", qt))
    ce <- DBI::dbGetQuery(db, sprintf("PRAGMA main.table_info(%s)", qt))
    if (!identical(cd$name, ce$name)) {
      fautes <- c(fautes, paste0("F2 : ", t, " : colonnes différentes"))
      next
    }
    cle <- if (any(cd$pk == 1L) && sum(cd$pk > 0L) == 1L)
      q_ident(cd$name[cd$pk == 1L]) else "rowid"
    ins <- DBI::dbGetQuery(db, sprintf(
      "SELECT %s AS k FROM main.%s WHERE %s NOT IN (SELECT %s FROM depart.%s)",
      cle, qt, cle, cle, qt))$k
    sup <- DBI::dbGetQuery(db, sprintf(
      "SELECT %s AS k FROM depart.%s WHERE %s NOT IN (SELECT %s FROM main.%s)",
      cle, qt, cle, cle, qt))$k
    mod <- DBI::dbGetQuery(db, sprintf(
      "SELECT %s AS k FROM (SELECT * FROM depart.%s EXCEPT SELECT * FROM main.%s)
         WHERE %s IN (SELECT %s FROM main.%s)", cle, qt, qt, cle, cle, qt))$k
    inserts[t] <- length(ins)
    if (length(sup))
      fautes <- c(fautes, sprintf("F2 : %s : %d ligne(s) retirée(s) (clé %s)",
                                  t, length(sup), paste(utils::head(sup, 5), collapse = ", ")))
    if (length(mod))
      fautes <- c(fautes, sprintf("F2 : %s : %d ligne(s) modifiée(s) (clé %s)",
                                  t, length(mod), paste(utils::head(mod, 5), collapse = ", ")))
    # UUID des lignes ajoutées (à défaut de colonne uuid : la clé).
    ajoutes <- if (length(ins) == 0L) character(0) else if ("uuid" %in% cd$name)
      tolower(DBI::dbGetQuery(db, sprintf(
        "SELECT uuid FROM main.%s WHERE %s IN (%s)", qt, cle,
        paste(ins, collapse = ",")))$uuid) else paste0(cle, "=", ins)
    voulus <- tolower(attendus[[t]])
    en_trop <- setdiff(ajoutes, voulus)
    manquants <- setdiff(voulus, ajoutes)
    if (length(en_trop))
      fautes <- c(fautes, sprintf("F2 : %s : ligne(s) ajoutée(s) non attendue(s) : %s",
                                  t, paste(utils::head(en_trop, 5), collapse = ", ")))
    if (length(manquants))
      fautes <- c(fautes, sprintf("F2 : %s : ligne(s) attendue(s) non ajoutée(s) : %s",
                                  t, paste(utils::head(manquants, 5), collapse = ", ")))
  }
  inconnues <- setdiff(names(attendus)[lengths(attendus) > 0L], tables_depart)
  for (t in inconnues)
    fautes <- c(fautes, paste0("F2 : UUID attendus dans ", t,
                               ", couche absente de la tournée de départ"))
  list(fautes = fautes, inserts = inserts)
}

# ------------------------------------------------------------------ F3 -----

# Clé textuelle d'une ligne, pour comparer des tables sans dépendre de l'ordre.
cles_lignes <- function(df) {
  if (nrow(df) == 0L) return(character(0))
  do.call(paste, c(lapply(df, function(c) ifelse(is.na(c), "<NA>", as.character(c))),
                   sep = "\u001f"))
}

table_complete <- function(db, t, exclues = character(0)) {
  df <- DBI::dbGetQuery(db, sprintf("SELECT * FROM %s", q_ident(t)))
  df[, setdiff(names(df), exclues), drop = FALSE]
}

# Une ligne ajoutée à une table gpkg_* est-elle un ajout permis ? Permis :
# celles des six couches (colonne table_name), et celles de l'extension Schema.
ajout_permis <- function(t, df, couches) {
  if (nrow(df) == 0L) return(logical(0))
  if (t == "gpkg_data_column_constraints") return(rep(TRUE, nrow(df)))
  if (t == "gpkg_extensions")
    # Extension Schema (gpkg_schema) : seulement sur une des six couches ou sur
    # les deux tables qui la portent. Une ligne gpkg_schema sur une couche de
    # sommieR est refusée ; toute autre extension, seulement sur les six couches.
    return(ifelse(df$extension_name %in% "gpkg_schema",
                  df$table_name %in% c(couches, TABLES_EXTENSION_SCHEMA),
                  df$table_name %in% couches))
  if ("table_name" %in% names(df)) return(df$table_name %in% couches)
  rep(FALSE, nrow(df))
}

# `inserts` : sortie de controle_f2()$inserts. Rend les fautes.
controle_f3 <- function(depart_gpkg, export_gpkg, couches_ajoutees,
                        inserts = integer(0)) {
  fautes <- character(0)
  d <- ouvrir_ro(depart_gpkg)
  e <- ouvrir_ro(export_gpkg)
  on.exit({ DBI::dbDisconnect(d); DBI::dbDisconnect(e) })
  for (p in c("application_id", "user_version")) {
    a <- DBI::dbGetQuery(d, sprintf("PRAGMA %s", p))[[1L]]
    b <- DBI::dbGetQuery(e, sprintf("PRAGMA %s", p))[[1L]]
    if (!identical(a, b))
      fautes <- c(fautes, sprintf("F3 : %s %s -> %s", p, a, b))
  }
  # sqlite_master : tables, index, déclencheurs, vues, DDL compris.
  lire_maitre <- function(db)
    DBI::dbGetQuery(db, "SELECT type, name, tbl_name, sql FROM sqlite_master")
  md <- lire_maitre(d)
  me <- lire_maitre(e)
  kd <- cles_lignes(md)
  ke <- cles_lignes(me)
  for (i in which(!kd %in% ke))
    fautes <- c(fautes, sprintf("F3 : sqlite_master : %s %s retiré ou modifié",
                                md$type[i], md$name[i]))
  extra <- me[!ke %in% kd, , drop = FALSE]
  permis_objet <- function(x) {
    x$tbl_name %in% c(couches_ajoutees, TABLES_EXTENSION_SCHEMA) |
      x$name %in% c(couches_ajoutees, TABLES_EXTENSION_SCHEMA) |
      Reduce(`|`, lapply(couches_ajoutees, function(c)
        startsWith(x$name, paste0("rtree_", c, "_"))), FALSE)
  }
  if (nrow(extra)) {
    ok <- permis_objet(extra)
    for (i in which(!ok))
      fautes <- c(fautes, sprintf("F3 : sqlite_master : %s %s ajouté (%s)",
                                  extra$type[i], extra$name[i], extra$tbl_name[i]))
  }
  # Tables gpkg_* : lignes du départ inchangées, ajouts permis seulement.
  gd <- DBI::dbGetQuery(d, "SELECT name FROM sqlite_master WHERE type = 'table'
                              AND name LIKE 'gpkg\\_%' ESCAPE '\\'")$name
  ge <- DBI::dbGetQuery(e, "SELECT name FROM sqlite_master WHERE type = 'table'
                              AND name LIKE 'gpkg\\_%' ESCAPE '\\'")$name
  for (t in setdiff(ge, gd))
    if (!t %in% TABLES_EXTENSION_SCHEMA)
      fautes <- c(fautes, paste0("F3 : table ", t, " ajoutée"))
  for (t in gd) {
    if (!t %in% ge) { fautes <- c(fautes, paste0("F3 : table ", t, " retirée")); next }
    # feature_count : vérifié à part (départ + ajouts comptés par F2).
    exclues <- c(COLONNES_EXCLUES[[t]], if (t == "gpkg_ogr_contents") "feature_count")
    a <- table_complete(d, t, exclues)
    b <- table_complete(e, t, exclues)
    if (!identical(names(a), names(b))) {
      fautes <- c(fautes, paste0("F3 : ", t, " : colonnes différentes"))
      next
    }
    ka <- cles_lignes(a)
    kb <- cles_lignes(b)
    n <- sum(!ka %in% kb)
    if (n) fautes <- c(fautes, sprintf("F3 : %s : %d ligne(s) retirée(s) ou modifiée(s)", t, n))
    sup <- b[!kb %in% ka, , drop = FALSE]
    ok <- ajout_permis(t, sup, couches_ajoutees)
    if (any(!ok))
      fautes <- c(fautes, sprintf("F3 : %s : %d ajout(s) non permis (%s)", t, sum(!ok),
                                  paste(utils::head(cles_lignes(sup[!ok, , drop = FALSE]), 3),
                                        collapse = " | ")))
  }
  # feature_count de chaque couche de départ = départ + lignes ajoutées (F2).
  if ("gpkg_ogr_contents" %in% gd && "gpkg_ogr_contents" %in% ge) {
    fd <- DBI::dbGetQuery(d, "SELECT table_name, feature_count FROM gpkg_ogr_contents")
    fe <- DBI::dbGetQuery(e, "SELECT table_name, feature_count FROM gpkg_ogr_contents")
    for (i in seq_len(nrow(fd))) {
      t <- fd$table_name[i]
      ajout <- if (t %in% names(inserts)) inserts[[t]] else 0L
      mesure <- fe$feature_count[fe$table_name == t]
      depart_fc <- fd$feature_count[i]
      if (length(mesure) != 1L) {
        fautes <- c(fautes, sprintf(
          "F3 : gpkg_ogr_contents : ligne de %s absente de l'export", t))
        next
      }
      # feature_count NULL au départ (cas du modèle de sommieR : GDAL le
      # recalcule à l'ouverture) : l'export doit le laisser NULL, inchangé.
      attendu <- if (is.na(depart_fc)) NA_integer_ else depart_fc + ajout
      if (!identical(is.na(mesure), is.na(attendu)) ||
          (!is.na(attendu) && mesure != attendu))
        fautes <- c(fautes, sprintf(
          "F3 : gpkg_ogr_contents : feature_count de %s, attendu %s (départ %s + %d ajoutées), mesuré %s",
          t, attendu, depart_fc, ajout, mesure))
    }
  }
  fautes
}
