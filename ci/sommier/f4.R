# Critère F4 de la CI d'import sommieR (mission 028, ADR 0007, « Critère de CI »).
#
# F4 : après l'import de l'export, le registre de sommieR porte exactement les
# valeurs de fixtures/export/registre-attendu-couchey.json (écrites à la main
# par le testeur, mission 027, sans lire le code qu'elles contrôlent).
#
# Décision de Pascal (pilotage/journal.md, 2026-10-22, décision de Pascal,
# mission 028) : F4 compare au REGISTRE les totaux (nb_total, nb_vivants,
# nb_abroutis, h_moy_cm) et visite_le, le contrôle étant retrouvé par le code de
# sa placette (payload.placette_id -> v_placette.code_placette). Le `point`
# attendu est comparé à la couche `positions_controles` du GeoPackage EXPORTÉ
# par Surculus (docs/spec.md l. 259), sans passer par le registre, avec la
# tolérance de 0,5 m (décision de Pascal, mission 027, lue dans le JSON ;
# `tolerance_m`). P35-02 est ouvert sans position mesurée (mission 087 ; ADR
# 0003, amendement du 2026-11-02, point 7 ; décision de Pascal du 2026-11-02,
# « en faire le cas sans position ») : sa ligne de `positions_controles` est
# vérifiée (une seule, géométrie NULL, sans_position_mesuree et
# rattache_par_identite vrais, precision_m vide), et son point attendu, le
# piquet, est lu dans la couche `controles` de l'export.
# Pourquoi hors registre : sommieR 0.31.1 (fd92078) n'inscrit aucune position
# sur un contrôle : R/registre6.R:150-180, R/travaux_terrain.R:156,
# R/travaux_qfield.R:262-277, inst/sql/003_geometrie.sql:26-31 (voir
# LIMITE_POSITION_SOMMIER, reprise dans le résumé du job).
#
# Coordonnées : toujours (lon, lat) en degrés décimaux WGS 84 (EPSG:4326), noms
# explicites ; les couches de l'export sont en EPSG:2154, converties par sf.
# Chaque fonction rend la liste des fautes (vide : conforme).

LIMITE_POSITION_SOMMIER <- paste0(
  "**La position mesurée n'arrive pas dans le registre de sommieR 0.31.1** ",
  "(R/registre6.R:150-180, R/travaux_terrain.R:156, R/travaux_qfield.R:262-277, ",
  "inst/sql/003_geometrie.sql:26-31) : F4 la compare à la couche ",
  "`positions_controles` de l'export (pilotage/journal.md, 2026-10-22, décision ",
  "de Pascal, mission 028).")

# Ligne du résumé sur le point de P35-02 (contrôle sans position mesurée).
LIGNE_PIQUET <- paste0(
  "P35-02 est un contrôle ouvert sans position mesurée (mission 087) : sa ligne de ",
  "`positions_controles` a une géométrie NULL, et son point, lu dans la couche ",
  "`controles` de l'export, est le piquet de sommieR.")

# Distance en mètres entre deux points (lon, lat) en degrés WGS 84, au plan
# tangent à (lon1, lat1) : dlon * N cos(lat), dlat * M (ellipsoïde WGS 84,
# comme `methode_decalage` de registre-attendu-couchey.json).
distance_wgs84_m <- function(lon1, lat1, lon2, lat2) {
  a <- 6378137; f <- 1 / 298.257223563; e2 <- f * (2 - f)
  phi <- lat1 * pi / 180
  w <- 1 - e2 * sin(phi)^2
  n <- a / sqrt(w)
  m <- a * (1 - e2) / w^1.5
  dx <- (lon2 - lon1) * pi / 180 * n * cos(phi)
  dy <- (lat2 - lat1) * pi / 180 * m
  sqrt(dx^2 + dy^2)
}

# Le point (lon, lat) tombe dans le rectangle de la France métropolitaine du
# JSON attendu (piège de la Somalie : latitude et longitude inversées).
dans_france <- function(lon, lat, rect)
  lat >= rect$lat_min & lat <= rect$lat_max &
    lon >= rect$lon_min & lon <= rect$lon_max

# Positions de l'export de Surculus, en (lon, lat) WGS 84, lues avec sf.
# Une géométrie NULL ou vide (contrôle sans position mesurée, mission 087)
# donne lon et lat NA, jamais (0, 0) ; la ligne n'est pas perdue.
# Rend list(positions = data.frame(controle_uuid, lon, lat,
#             sans_position_mesuree, rattache_par_identite, precision_m),
#           controles = data.frame(uuid, lon, lat)   (couche `controles`),
#           piquets = data.frame(code_placette, lon, lat)).
lire_points_export <- function(export_gpkg) {
  en_wgs84 <- function(couche) {
    x <- sf::st_read(export_gpkg, couche, quiet = TRUE)
    g <- sf::st_geometry(x)
    vide <- sf::st_is_empty(g)
    lon <- rep(NA_real_, length(g)); lat <- rep(NA_real_, length(g))
    if (any(!vide)) {
      xy <- sf::st_coordinates(sf::st_transform(g[!vide], 4326))  # X = lon, Y = lat
      lon[!vide] <- unname(xy[, "X"]); lat[!vide] <- unname(xy[, "Y"])
    }
    list(x = sf::st_drop_geometry(x), lon = lon, lat = lat)
  }
  # Colonne absente (export d'avant la 087) : NA, jamais une valeur devinée.
  colonne <- function(df, k, vide) if (k %in% names(df)) df[[k]] else rep(vide, nrow(df))
  couches <- sf::st_layers(export_gpkg)$name
  positions <- data.frame(controle_uuid = character(0), lon = numeric(0),
                          lat = numeric(0), sans_position_mesuree = logical(0),
                          rattache_par_identite = logical(0),
                          precision_m = numeric(0), stringsAsFactors = FALSE)
  if ("positions_controles" %in% couches) {
    p <- en_wgs84("positions_controles")
    positions <- data.frame(controle_uuid = tolower(as.character(p$x$controle_uuid)),
                            lon = p$lon, lat = p$lat,
                            sans_position_mesuree = as.logical(colonne(p$x, "sans_position_mesuree", NA)),
                            rattache_par_identite = as.logical(colonne(p$x, "rattache_par_identite", NA)),
                            precision_m = as.numeric(colonne(p$x, "precision_m", NA_real_)),
                            stringsAsFactors = FALSE)
  }
  controles <- data.frame(uuid = character(0), lon = numeric(0),
                          lat = numeric(0), stringsAsFactors = FALSE)
  if ("controles" %in% couches) {
    k <- en_wgs84("controles")
    controles <- data.frame(uuid = tolower(as.character(k$x$uuid)),
                            lon = k$lon, lat = k$lat, stringsAsFactors = FALSE)
  }
  piquets <- data.frame(code_placette = character(0), lon = numeric(0),
                        lat = numeric(0), stringsAsFactors = FALSE)
  if ("placettes" %in% couches) {
    p <- en_wgs84("placettes")
    piquets <- data.frame(code_placette = as.character(p$x$code_placette),
                          lon = p$lon, lat = p$lat, stringsAsFactors = FALSE)
  }
  list(positions = positions, controles = controles, piquets = piquets)
}

# Instant ISO 8601 en UTC (« 2026-10-20T09:30:00Z », millisecondes admises) en
# secondes depuis 1970, indépendamment du fuseau du processus (tz = "UTC"
# explicite). NA si la chaîne n'est pas un instant UTC.
# Mesuré le 2026-10-22 (mission 028) : l'export écrit visite_le en UTC avec « Z »
# (gpkg/.../ValidationExport.kt:237-241, docs/spec.md:282) ; sf::read_sf rend un
# POSIXct, donc as.POSIXct de sommieR (R/travaux_qfield.R:264) ne change rien, et
# le registre porte le même instant sous TZ=UTC et sous TZ=Europe/Paris.
instant_utc <- function(x) {
  x <- sub("\\.[0-9]+Z$", "Z", as.character(x))
  as.numeric(as.POSIXct(x, format = "%Y-%m-%dT%H:%M:%SZ", tz = "UTC"))
}

# Les contrôles ÉCRITS par l'import, lus dans le registre : une ligne par
# contrôle, avec le code de la placette (placette_id -> v_placette.code_placette).
# `registre` : etat_registre() (colonnes id, payload) ; `ecrites` : UUID des
# entrées écrites par l'import ; `codes` : data.frame(id, code_placette).
controles_ecrits <- function(registre, ecrites, codes) {
  vide <- data.frame(id = character(0), code = character(0),
                     visite_le = character(0), nb_total = numeric(0),
                     nb_vivants = numeric(0), nb_abroutis = numeric(0),
                     h_moy_cm = numeric(0), releve_uuid = character(0),
                     stringsAsFactors = FALSE)
  lignes <- registre[tolower(registre$id) %in% tolower(unlist(ecrites)), ,
                     drop = FALSE]
  if (nrow(lignes) == 0L) return(vide)
  p <- lapply(lignes$payload, jsonlite::fromJSON, simplifyVector = FALSE)
  est <- vapply(p, function(x) identical(x$type_entree, "controle"), logical(1))
  p <- p[est]; lignes <- lignes[est, , drop = FALSE]
  if (length(p) == 0L) return(vide)
  champ <- function(k) vapply(p, function(x)
    if (is.null(x[[k]])) NA_character_ else as.character(x[[k]]), character(1))
  num <- function(k) suppressWarnings(as.numeric(champ(k)))
  pid <- tolower(champ("placette_id"))
  data.frame(id = lignes$id,
             code = codes$code_placette[match(pid, tolower(codes$id))],
             visite_le = champ("visite_le"),
             nb_total = num("nb_total"), nb_vivants = num("nb_vivants"),
             nb_abroutis = num("nb_abroutis"), h_moy_cm = num("h_moy_cm"),
             releve_uuid = tolower(champ("releve_uuid")),
             stringsAsFactors = FALSE)
}

# `attendu` : registre-attendu-couchey.json lu (simplifyVector = FALSE) ;
# `controles` : controles_ecrits() ; `points` : lire_points_export().
# Rend list(fautes, lignes) ; `lignes` : placette, champ, attendu, mesure, verdict.
controle_f4 <- function(attendu, controles, points) {
  fautes <- character(0)
  lignes <- list()
  noter <- function(placette, champ, att, mes, ok, faute = NULL) {
    lignes[[length(lignes) + 1L]] <<- data.frame(
      placette = placette, champ = champ, attendu = as.character(att),
      mesure = as.character(mes), verdict = if (ok) "conforme" else "ÉCART",
      stringsAsFactors = FALSE)
    if (!ok) fautes <<- c(fautes, if (is.null(faute))
      sprintf("F4 : %s, %s : attendu %s, mesuré %s", placette, champ, att, mes)
      else faute)
  }
  rect <- attendu$france_metropolitaine
  utilises <- character(0)
  for (att in attendu$controles) {
    pl <- att$placette
    # Le contrôle de la placette ; s'il y en a plusieurs d'écrits, celui dont
    # l'instant est l'attendu. Un instant faux n'est pas « contrôle absent » :
    # la ligne visite_le le signale, avec la valeur lue au registre.
    cible <- controles[controles$code %in% pl, , drop = FALSE]
    if (nrow(cible) > 1L)
      cible <- cible[instant_utc(cible$visite_le) %in% instant_utc(att$visite_le), ,
                     drop = FALSE]
    if (nrow(cible) != 1L) {
      noter(pl, "contrôle au registre", paste("1 à", att$visite_le),
            paste(nrow(cible), "trouvé(s)"), FALSE,
            sprintf("F4 : %s, contrôle du %s : %d entrée(s) écrite(s) au registre, 1 attendue",
                    pl, att$visite_le, nrow(cible)))
      next
    }
    utilises <- c(utilises, cible$id)
    # L'instant lu au registre (payload.visite_le), comparé à la seconde, en UTC.
    noter(pl, "visite_le (instant UTC, à la seconde)", att$visite_le,
          cible$visite_le,
          isTRUE(instant_utc(cible$visite_le) == instant_utc(att$visite_le)),
          sprintf("F4 : %s, visite_le : attendu %s, lu au registre %s",
                  pl, att$visite_le, cible$visite_le))
    for (k in c("nb_total", "nb_vivants", "nb_abroutis", "h_moy_cm")) {
      mes <- cible[[k]]
      noter(pl, k, att[[k]], mes, !is.na(mes) && mes == att[[k]])
    }
    # Le point : hors registre (voir l'en-tête).
    p <- att$point
    if (!dans_france(p$lon, p$lat, rect))
      noter(pl, "point attendu en France", "oui", "non", FALSE,
            sprintf("F4 : %s, point attendu (lon %s, lat %s) hors de la France métropolitaine",
                    pl, p$lon, p$lat))
    ligne_pos <- points$positions[points$positions$controle_uuid %in%
                                    cible$releve_uuid, , drop = FALSE]
    if (isTRUE(p$sans_position_mesuree)) {
      # Mission 087 : contrôle ouvert sans position mesurée. Sa ligne de
      # positions_controles : une seule, géométrie NULL, drapeaux vrais,
      # precision_m vide (Darwin Core : jamais 0) ; son point : `controles`.
      if (!verifier_sans_position(pl, cible$releve_uuid, ligne_pos, noter)) next
      src <- "controles"
      mes <- if (is.null(points$controles)) data.frame(lon = NA_real_, lat = NA_real_) else
        points$controles[points$controles$uuid %in% cible$releve_uuid, , drop = FALSE]
      if (nrow(mes) != 1L) {
        noter(pl, "point (controles)", "une ligne", paste(nrow(mes), "trouvée(s)"), FALSE,
              sprintf("F4 : %s, contrôle %s : %d ligne(s) dans la couche controles de l'export, 1 attendue",
                      pl, cible$releve_uuid, nrow(mes)))
        next
      }
    } else if (nrow(ligne_pos) > 0L) {
      if ("sans_position_mesuree" %in% names(ligne_pos) && !isFALSE(as.logical(ligne_pos$sans_position_mesuree[1L])))
        noter(pl, "positions_controles, sans_position_mesuree", "faux",
              ligne_pos$sans_position_mesuree[1L], FALSE,
              sprintf("F4 : %s, positions_controles : sans_position_mesuree vaut %s, faux attendu (une position mesurée est attendue)",
                      pl, ligne_pos$sans_position_mesuree[1L]))
      src <- "positions_controles"; mes <- ligne_pos[1L, ]
    } else if (grepl("^position mesur", p$origine)) {
      noter(pl, "point (positions_controles)", "une ligne", "aucune", FALSE,
            sprintf("F4 : %s, aucune ligne de positions_controles pour le contrôle %s alors que le point attendu est une position mesurée",
                    pl, cible$releve_uuid))
      next
    } else {
      # Aucune mesure dans le scénario : le point attendu est le piquet.
      src <- "placettes (piquet)"
      mes <- points$piquets[points$piquets$code_placette %in% pl, , drop = FALSE][1L, ]
    }
    if (is.na(mes$lon)) {
      noter(pl, "point", "présent", "absent", FALSE,
            sprintf("F4 : %s, aucun point lu dans l'export (%s)", pl, src))
      next
    }
    d <- distance_wgs84_m(p$lon, p$lat, mes$lon, mes$lat)
    noter(pl, sprintf("point, %s", src),
          sprintf("lon %.7f, lat %.7f (+/- %s m)", p$lon, p$lat, p$tolerance_m),
          sprintf("lon %.7f, lat %.7f (écart %.3f m)", mes$lon, mes$lat, d),
          is.finite(d) && d <= p$tolerance_m && dans_france(mes$lon, mes$lat, rect),
          sprintf("F4 : %s, point de %s à %.3f m du point attendu (tolérance %s m ; lon %.7f, lat %.7f)",
                  pl, src, d, p$tolerance_m, mes$lon, mes$lat))
  }
  en_trop <- setdiff(controles$id, utilises)
  if (length(en_trop))
    fautes <- c(fautes, sprintf("F4 : %d contrôle(s) écrit(s) au registre sans attente : %s",
                                length(en_trop), paste(utils::head(en_trop, 3), collapse = ", ")))
  list(fautes = fautes,
       lignes = if (length(lignes)) do.call(rbind, lignes) else
         data.frame(placette = character(0), champ = character(0),
                    attendu = character(0), mesure = character(0),
                    verdict = character(0)))
}

# La ligne de positions_controles d'un contrôle ouvert sans position mesurée
# (mission 087 ; ADR 0003, amendement du 2026-11-02, point 7). Note chaque
# vérification ; rend FALSE si la ligne manque ou est en double (le point ne
# se compare pas), TRUE sinon.
verifier_sans_position <- function(pl, releve, ligne_pos, noter) {
  champ <- "positions_controles (contrôle sans position mesurée)"
  if (nrow(ligne_pos) == 0L) {
    noter(pl, champ, "une ligne", "aucune", FALSE,
          sprintf("F4 : %s, aucune ligne de positions_controles pour le contrôle %s, ouvert sans position mesurée (une ligne par contrôle)",
                  pl, releve))
    return(FALSE)
  }
  if (nrow(ligne_pos) > 1L) {
    noter(pl, champ, "une ligne", paste(nrow(ligne_pos), "lignes"), FALSE,
          sprintf("F4 : %s, %d lignes de positions_controles pour le contrôle %s, une attendue",
                  pl, nrow(ligne_pos), releve))
    return(FALSE)
  }
  l <- ligne_pos[1L, ]
  noter(pl, "positions_controles, géométrie", "NULL",
        if (is.na(l$lon)) "NULL" else sprintf("lon %.7f, lat %.7f", l$lon, l$lat),
        is.na(l$lon) && is.na(l$lat),
        sprintf("F4 : %s, positions_controles : géométrie présente (lon %s, lat %s), NULL attendue (contrôle sans position mesurée)",
                pl, l$lon, l$lat))
  noter(pl, "positions_controles, sans_position_mesuree", "vrai",
        l$sans_position_mesuree, isTRUE(as.logical(l$sans_position_mesuree)),
        sprintf("F4 : %s, positions_controles : sans_position_mesuree vaut %s, vrai attendu",
                pl, l$sans_position_mesuree))
  noter(pl, "positions_controles, rattache_par_identite", "vrai",
        l$rattache_par_identite, isTRUE(as.logical(l$rattache_par_identite)),
        sprintf("F4 : %s, positions_controles : rattache_par_identite vaut %s, vrai attendu (définition élargie)",
                pl, l$rattache_par_identite))
  noter(pl, "positions_controles, precision_m", "vide",
        if (is.na(l$precision_m)) "vide" else l$precision_m, is.na(l$precision_m),
        sprintf("F4 : %s, positions_controles : precision_m vaut %s, vide attendu (jamais 0 : Darwin Core)",
                pl, l$precision_m))
  TRUE
}

# Avertissement du témoin « sans couches » (mission 026) : pas un écart.
# Rend list(annotation, resume) ; `annotation` est la ligne `::warning` que
# GitHub affiche (NULL si `diff` est vide), `resume` la phrase du résumé.
avertissement_temoin <- function(diff) {
  if (!length(diff))
    return(list(annotation = NULL,
                resume = "Vérification de fumée (hors verdict) : témoin sans couches ajoutées, même bilan."))
  list(annotation = paste0("::warning title=Témoin sans couches::bilan différent : ",
                           paste(diff, collapse = ", ")),
       resume = paste0("**Avertissement** (fumée, hors verdict) : le témoin sans couches ajoutées donne un bilan différent (",
                       paste(diff, collapse = ", "), ")."))
}
