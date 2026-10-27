# Brief — Une seconde démo : la forêt communale de Marsannay-la-Côte

*Établi le 27 octobre 2026 depuis le dépôt `surculus`, à partir de sommieR
0.31.1 (`fd92078`). Décision de Pascal du 2026-10-27 (« 2. a »,
`pilotage/journal.md`). À déposer dans `specs/` de sommieR sous le nom
`brief_demo_marsannay.md`.*

## Le constat

Surculus n'a qu'une tournée de démonstration : celle que produit
`sommier_projet_qfield_travaux()` sur la base de `sommier_demo_couchey()`
(`R/demo.R`). Elle porte 3 placettes, 2 suivis et 3 UG, sur trois parcelles
cadastrales de Couchey (compte fait le 2026-10-27 sur une tournée régénérée par
`ci/sommier/verifier.R --etape preparer` de Surculus).

Pascal veut essayer Surculus sur **deux** tournées, dont une à Marsannay-la-Côte
(Côte-d'Or, INSEE 21390). Il n'existe pour Marsannay aucun chantier de
plantation ni placette dans sommieR, seulement un parcellaire.

## Les données qui existent

Ce sont des données réelles, extraites le 2026-10-15 dans
`~/dev/aigora/aigora-nemeton/.tmp/marsannay/` :

- `onf_F21866Z.gpkg` : 50 parcelles ONF de la forêt `F21866Z`, « Forêt
  communale de Marsannay-La-Cote », non domaniale, 519,8 ha, en EPSG:2154.
  Tirées par `nemeton::load_onf_parcelles_source()` sur l'emprise de la commune
  élargie de 3 km (script `onf.R` du même dossier) ;
- `pc.rds` : parcelles cadastrales de la commune, par
  `nemetonshiny::parcelles_commune("21390")` ;
- `ugf_F21866Z.gpkg` : 113 unités de gestion foncière (cadastre × parcelle ONF),
  en EPSG:4326, issues du projet nemeton `20261015_194200_yzoh` ;
- `~/dev/aigora/aigora-nemeton/.tmp/marsannay-21390/marsannay_parcelles_bois.csv` :
  98 parcelles cadastrales boisées (identifiant, section, numéro, surfaces,
  nature, propriétaire, part BD Forêt).

Ce dossier est un dossier de travail (`.tmp`) : sommieR doit en reprendre la
**source** (cadastre DGFiP par etalab, parcellaire ONF), comme le fait déjà
`SOMMIER_PARCELLES_COUCHEY` (`R/demo.R`, `@source`), et non le dossier lui-même.

## Ce qui est demandé

1. Une fonction `sommier_demo_marsannay(con)` sur le modèle de
   `sommier_demo_couchey()` : quelques parcelles cadastrales **réelles** de la
   forêt communale de Marsannay-la-Côte (contours et références DGFiP), et des
   écritures **inventées** dessus (chantier de plantation, placettes de suivi,
   suivis), en quantité comparable à Couchey.
2. Le même garde-fou que Couchey : le nom de la forêt porte la mention
   « (jeu de demonstration) », pour qu'aucune lecture de la base ne prenne ces
   écritures pour réelles (`NOM_FORET_DEMO`, `R/demo.R` l. 1-7).
3. Les codes travaux de la couche `codes` tirés de la nomenclature de sommieR,
   comme pour Couchey (Surculus n'en embarque aucun : décision de Pascal du
   2026-10-15).
4. Que `sommier_projet_qfield_travaux()` produise une tournée de Marsannay
   lisible par Surculus sans autre changement (même schéma que la tournée de
   Couchey).

## Ce que Surculus en fera

- Une seconde tournée pour l'essai sur le téléphone de Pascal (liste des
  tournées, recherche, vue Kanban, carte, guidage) ;
- plus tard, si Pascal le décide, une seconde tournée dans la CI d'import
  (`.github/workflows/import-sommier.yml`), pour que les critères F1 à F4 ne
  reposent pas sur un seul jeu.

## Ce qui n'est pas demandé

- Aucun changement du schéma de la tournée ni de l'import.
- Aucune donnée réelle de plantation : il n'en existe pas dans ce dossier.
