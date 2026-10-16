# CI : import sommieR rejoué (mission 007, ADR 0007)

Travail « import sommieR » : `.github/workflows/import-sommier.yml`. Il installe
sommieR **0.31.1** (tag `v0.31.1`, commit `fd92078e9f538aefbf1181cae7e88f64a5f8961d`),
monte un service PostGIS éphémère et lance `verifier.R`.

## Ce que fait `verifier.R`

1. Base `sommier_modele` : schéma de sommieR, démo Couchey
   (`sommier_demo_couchey()`), puis la tournée « **sans** » telle que sommieR la
   produit (`sommier_projet_qfield_travaux()`), sans couche ajoutée.
2. Pour chaque tournée, une base clonée de `sommier_modele` (la tournée a besoin
   de la forêt et des travaux de la démo pour rattacher ses placettes) reçoit
   `sommier_importer_qfield_travaux()`. Le **bilan** est écrit en JSON trié
   (`bilan-sans.json`, `bilan-avec.json`) : nombres écrits, identifiants des
   entrées, et contenu du registre sans ce qui dépend de l'horloge.
3. (b) Le schéma des couches de la tournée (champs, types, géométrie, `srs_id`)
   est comparé à `inst/qgis/travaux.gpkg` de sommieR.
4. Avec `--avec <dossier>` : (a) les deux bilans doivent être identiques ;
   (b) le schéma de l'export est comparé au modèle ; (c) `sf::st_layers()` doit
   lister les six couches ajoutées (`COUCHES_AJOUTEES` dans `fonctions.R`).

`--etape preparer` s'arrête après la tournée « sans » ; `--etape comparer`
reprend là. Code de retour : 0 identique, 1 écart, 2 mauvais usage. Mot de passe de base :
variable `SOMMIER_CI_PASSWORD` (celui du service éphémère de la CI).

## Limite de l'entrée provisoire

La tournée de la démo ne porte aucune saisie nouvelle : ses trois placettes sont
déjà au registre de la base clonée. Le bilan « sans » est donc
`0 écrits, 3 déjà présents`. Il prouve que l'import lit le gabarit sans faute,
pas qu'il écrit la même chose : cela ne viendra qu'avec l'export de la 006.

## Ce que la mission 006 doit fournir

- L'export `.zip` **décompressé** dans `ci/sommier/export-surculus/` (variable
  `EXPORT_SURCULUS` du workflow), c'est-à-dire un dossier contenant
  `travaux.gpkg` (et `DCIM/` si des photos sont jointes), comme celui qu'écrit
  `sommier_projet_qfield_travaux()`.
- Cet export doit **partir de la tournée « sans » du même job** :
  `ci/sommier/sortie/tournee-sans` (écrite par l'étape `--etape preparer`).
  Chaque exécution engendre sa démo, donc ses propres UUID de forêt et de
  placettes ; un export issu d'une autre exécution est refusé par l'import
  (« travaux suivis inconnus »), ce que l'essai local a montré. Surculus lit
  cette tournée, y ajoute les six couches, et ne touche à aucune couche ni
  aucun champ de sommieR.
- L'étape de la 006 s'insère dans le workflow entre « Préparer » et « Rejouer ».
- Tant que le dossier est absent, le workflow ne rejoue que « sans » et le dit.
  La 006 doit **rendre l'export obligatoire** (supprimer le test
  `[ -f ... ]` dans l'étape « Rejouer »), sinon l'absence passerait pour un vert.
