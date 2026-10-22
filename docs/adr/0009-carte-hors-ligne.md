# ADR 0009 — Carte et ortho hors ligne

- **Date :** 2026-10-20
- **Statut :** acceptée par Pascal (2026-10-21), après critique du `contradicteur`
- **Proposée par :** Claude (orchestrateur)
- **Tranchée par :** Pascal

## La décision en une phrase

En tranche 1, la carte de Surculus est servie par **osmdroid en Web Mercator**, avec une
ortho faite de **tuiles WMTS de l'IGN préchargées dans le dossier privé de la tournée** à
son ouverture, avec le code de Groma. Trois garde-fous l'accompagnent :
- l'**état de l'ortho est visible par tournée** (complète, partielle n / N, absente) ;
- un **test vérifie que l'emprise tombe en France métropolitaine** ;
- l'**attribution est datée** du téléchargement.

Un `ortho.mbtiles` ou `ortho.gpkg` en EPSG:3857 présent dans le dossier de tournée est **lu
en priorité**. La **cible** est un brief vers sommieR : un seul fichier tuilé en EPSG:3857
**remplace** `ortho.tif`, lu par QField et par Surculus (option f).

## Décisions de Pascal (2026-10-21)

- Tranche 1 : option (d) avec les trois garde-fous et la lecture prioritaire d'un fichier
  tuilé local.
- Cible : option (f), à demander par un brief vers sommieR. Le choix entre GPKG et
  MBTiles se fait dans sommieR.
- osmdroid est archivé depuis le 2024-11-20 : le constat est noté ici et dans
  `docs/dependances.md`, le réexamen de l'ADR 0004 est au bac à sable, et la 011 part sur
  osmdroid.
- Périmètre : **France métropolitaine et Corse**. sommieR code EPSG:2154 en dur, donc les
  DOM ne passent pas. C'est écrit dans la spec § 1 et dans le test « la placette tombe en
  France ».

## Critique du `contradicteur` (2026-10-20), en bref

- Les faits sont confirmés, sauf le périmètre du code de Groma. Le préchargement vit dans
  `TelechargementTuiles.kt` (WorkManager), `groma-core/…/ZoneTuiles.kt` (zooms 12 à 18) et
  `carte/Fonds.kt` (WMTS `PM_0_19`), en plus de `TuilesHorsLigne.kt`. WorkManager est une
  dépendance de plus.
- Le modèle QField de sommieR affiche déjà la même ortho en ligne (`inst/qgis/travaux.qgs`,
  WMTS `PM_0_19`, EPSG:3857). L'écart entre l'ortho de QField et celle de Surculus est donc
  minime : date du téléchargement, rééchantillonnage, choix de l'IRC.
- Le WMTS de la Géoplateforme n'est pas soumis à limite d'usage (CGU, annexe 3, du
  15 octobre 2024). L'usage abusif reste interdit. La Licence Ouverte 2.0 exige la source
  et une date.
- Groma termine en succès même quand des tuiles manquent (404 et 204 ignorés). Une emprise
  inversée tomberait en Somalie sans alerte. D'où les garde-fous.
- Option (a) : tiff-java lit la structure du TIFF mais pas le JPEG. Le décodage par
  `BitmapFactory` reste une hypothèse non éprouvée.
- PMTiles est écarté : en GDAL, ce format est vectoriel seulement.

## Contexte

La spec (§ 1, § 4.2) exige une carte **hors ligne de bout en bout** : `foret`, `ug`,
`parcelles`, ortho, position et badge du fix. La mission 011 (écrans Tournées, Carte et
Guidage) ne peut pas partir sans savoir d'où vient l'ortho (kanban : « ADR 0009 avant
011 »).

Faits relevés par l'orchestrateur, à vérifier par le `contradicteur` :

- **sommieR** joint l'ortho au projet de tournée comme un fichier `ortho.tif`
  (`joindre_ortho`, `R/qfield.R:360`), facultatif (`sommier_projet_qfield_travaux(…,
  ortho = NULL)`, `R/travaux_qfield.R:41`). Il l'écrit avec `sommier_ortho_ign()`
  (`R/qfield.R:469`) : WMS de la Géoplateforme (`ORTHOIMAGERY.ORTHOPHOTOS`), **EPSG:2154**,
  GeoTIFF tuilé, compression JPEG en YCbCr, 0,5 m par défaut, « une dizaine de mégaoctets »
  pour une forêt de trois kilomètres de côté.
- **osmdroid** (Apache-2.0, ADR 0004, `docs/dependances.md`) n'affiche que des tuiles en
  grille Web Mercator.
- **Marculus** lit une ortho stockée en tuiles GeoPackage et la reprojette en Web Mercator
  avec geopackage-java (`GpkgRepository.kt`, `OrthoSource`). L'ADR 0011 exclut
  geopackage-java de Surculus.
- **Groma** précharge des tuiles XYZ dans le dossier du projet,
  `tuiles/<source>/<z>/<x>/<y>.<ext>`, et les sert avant le cache d'osmdroid et le réseau
  (`android/app/src/main/kotlin/groma/app/carte/TuilesHorsLigne.kt`). La carte marche en
  mode avion.

Options envisagées :

| | Option | Avis |
|---|---|---|
| a | Décoder `ortho.tif` sur l'appareil (TIFF tuilé JPEG/YCbCr, géoréférencement GeoTIFF), le reprojeter en tuiles Web Mercator à l'ouverture de la tournée | Fidèle au contrat de sommieR, mais il faut un lecteur GeoTIFF sans GDAL et une reprojection Lambert-93 vers Web Mercator, à écrire et à tester ; c'est la plus coûteuse |
| b | Carte entière en Lambert-93, l'ortho posée telle quelle | osmdroid ne le permet pas ; il faudrait changer de bibliothèque de carte (ADR 0004 rouverte) |
| c | sommieR livre en plus une ortho tuilée en Web Mercator (MBTiles ou dossier XYZ, avec le pilote MBTiles de GDAL ou `gdal2tiles`) | Le plus simple pour Surculus ; dépend d'une évolution de sommieR, décidée par Pascal dans sommieR |
| d | Surculus précharge les tuiles WMTS de l'IGN (grille PM, Web Mercator) sur l'emprise de la tournée, à son ouverture, avec le code de Groma | Le code est déjà écrit et éprouvé, mais il faut le réseau au bureau, et l'ortho peut différer de celle de QField |
| e | Pas d'ortho en tranche 1, couches vectorielles seules | Frugal, mais l'agent perd son meilleur repère en forêt |

| f | sommieR **remplace** `ortho.tif` par un seul fichier tuilé en EPSG:3857 (GPKG avec `TILING_SCHEME=GoogleMapsCompatible`, ou MBTiles), lu par QField et par Surculus (sqlite-jdbc, ADR 0011) | Ajoutée par le `contradicteur` : la source unique de l'ortho est tenue. **Cible retenue** |
| g | Convention de bureau en attendant sommieR : `gdal_translate -of MBTILES` puis `gdaladdo`, et Surculus lit le fichier s'il est présent | Ajoutée par le `contradicteur` : couvre la tournée ouverte sans réseau |

Proposition initiale de l'orchestrateur : (d) en tranche 1, (c) comme cible. Après la
critique, la cible est passée de (c) à (f), et la lecture d'un fichier tuilé local (g) a été
ajoutée.

## Le crible FRANCE

| Lettre | Question | Réponse (une ligne) | Ce que ça coûte |
|---|---|---|---|
| **F**rugale | La plus simple qui tienne ? | Reprendre le code de Groma, sans décodeur GeoTIFF ni reprojection raster sur l'appareil | Deux sources d'ortho possibles (QField et Surculus) tant que (c) n'existe pas |
| **R**esponsable | Qui fait quoi ? | Surculus précharge et sert les tuiles ; sommieR, à terme, les livre ; Pascal décide dans sommieR | Un brief de plus vers sommieR |
| **A**nticipateur | Et si l'imprévu arrive ? | Pas de réseau à l'ouverture : un fichier tuilé local est lu s'il existe, sinon l'état « absente » est affiché. Tuiles manquantes : état « partielle n / N ». Service en panne : on retente avec recul (WorkManager). osmdroid est archivé depuis le 2024-11-20, mais un fichier en EPSG:3857 reste portable | Une tournée ouverte sans réseau ni fichier n'a pas d'ortho |
| **N**ovateur | Toutes les idées écoutées ? | Cinq options, dont décoder le GeoTIFF et changer de bibliothèque de carte | — |
| **C**oût | Coût pour l'ensemble ? | Quelques dizaines de Mo de tuiles par tournée, à mesurer sur Couchey | Stockage du téléphone |
| **É**cologique | Que réutilisons-nous ? | Le code de Groma, osmdroid, la Géoplateforme (Licence Ouverte) | — |

## Valeurs sacrifiées

- **Le hors-ligne dès l'import** : sans fichier tuilé local, il faut le réseau au moment
  d'ouvrir la tournée.
- **La sobriété des données mobiles** de l'agent : le préchargement peut passer par le
  réseau mobile (option « Wi-Fi seulement » à prévoir dans la 011).
- **Une dépendance de plus à un service en ligne** au moment de préparer la tournée.
- La source unique de l'ortho n'est presque pas sacrifiée, puisque QField affiche déjà ce
  même WMTS. Elle sera tenue par la cible (f).

## Sources

- sommieR : `R/qfield.R` (`sommier_ortho_ign`, `joindre_ortho`), `R/travaux_qfield.R`.
- Groma : `android/app/src/main/kotlin/groma/app/carte/TuilesHorsLigne.kt`.
- Marculus : `data/src/main/kotlin/io/github/pobsteta/marculus/data/GpkgRepository.kt`.
- Spec § 1, § 4.1, § 4.2 ; ADR 0004 ; ADR 0011.
- Groma : `TelechargementTuiles.kt`, `carte/Fonds.kt`, `groma-core/…/ZoneTuiles.kt` ;
  sommieR : `inst/qgis/travaux.qgs` (couche « Orthophotographie IGN (réseau) »).
- Limites d'usage de la Géoplateforme :
  https://cartes.gouv.fr/aide/fr/guides-utilisateur/utiliser-les-services-de-la-geoplateforme/limites-d-usage/ ;
  CGU : https://cartes.gouv.fr/cgu/ (annexe 3) ; Licence Ouverte 2.0 :
  https://www.etalab.gouv.fr/licence-ouverte-open-licence/
- osmdroid, dépôt archivé : https://github.com/osmdroid/osmdroid ; `MBTilesFileArchive.java`.
- GDAL, pilotes MBTiles et GPKG raster : https://gdal.org/en/stable/drivers/raster/mbtiles.html,
  https://gdal.org/en/stable/drivers/raster/gpkg.html ; PMTiles (vectoriel seulement) :
  https://gdal.org/en/stable/drivers/vector/pmtiles.html
- QField, formats : https://docs.qfield.org/reference/data-format/
- tiff-java : https://github.com/ngageoint/tiff-java ; EPSG:2154 : https://epsg.io/2154
- Non vérifié : couverture DOM de `ORTHOIMAGERY.ORTHOPHOTOS` ; lecture par QField d'un
  GPKG raster en 3857 sur Android ; volume réel sur Couchey (estimation du `contradicteur` :
  25 à 40 Mo).

## Question de contrôle

Cette décision tient-elle nos six valeurs, et que sommes-nous prêts à payer pour cela ?

## Après les décisions de Pascal du 2026-10-21 (ajout, rien n'est effacé plus haut)

La mention de la section « Valeurs sacrifiées » (« option "Wi-Fi seulement" à prévoir dans la
011 ») est **dépassée** : la mission 011, fusionnée (PR #51), la livre, et Pascal a décidé que
**« Wi-Fi seulement » est activé par défaut**. Les décisions (a) à (d) de la mission 011 sont au
journal du 2026-10-21 (`pilotage/journal.md`, entrée « mission 011 · (a) … ») et dans
`pilotage/missions/011-ecrans-tournees-carte-guidage.md`. Elles ne sont pas recopiées ici.
