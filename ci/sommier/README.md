# CI : import sommieR rejoué (mission 007, ADR 0007)

Travail « import sommieR » : `.github/workflows/import-sommier.yml`. Il installe
sommieR **0.31.1** (tag `v0.31.1`, commit `fd92078e9f538aefbf1181cae7e88f64a5f8961d`),
monte un service PostGIS éphémère et lance `verifier.R`.

## Ce que fait `verifier.R`

1. **`--etape preparer`** : base `sommier_modele` (schéma de sommieR, démo Couchey
   par `sommier_demo_couchey()`), puis la tournée de départ telle que sommieR la
   produit (`sommier_projet_qfield_travaux()`) dans `<sortie>/tournee-sans`.
   C'est l'entrée de Surculus (mission 006), pas le témoin.
2. **`--etape comparer`** :
   - L'entrée est l'export décompressé de Surculus (`--avec`). Le **témoin
     « sans »** est une **copie du même export**, privée de ses six couches
     (`plants`, `etats_plants`, `constats_reprise`, `vides`,
     `positions_controles`, `projet`) : tables, et lignes de `gpkg_contents`,
     `gpkg_geometry_columns`, `gpkg_data_columns`, `gpkg_extensions`.
   - Chaque version est importée par `sommier_importer_qfield_travaux()` dans
     son propre clone de `sommier_modele` (la tournée a besoin de la forêt et des
     travaux de la démo pour rattacher ses placettes). Les **bilans** sont écrits
     en JSON trié (`bilan-avec.json`, `bilan-sans.json`) : nombres, ids d'entrées,
     registre sans ce qui dépend de l'horloge.
   - (a) les deux bilans sont identiques ; (b) le schéma des couches de sommieR de
     l'export (type, notnull, pk, géométrie, z/m, `srs_id`) égale
     `inst/qgis/travaux.gpkg` ; (c) `sf::st_layers()` liste les six couches.
3. **Sans `--avec`** (006 absente) : le critère (a) n'est **pas mesuré**. Le script
   éprouve seulement le mécanisme de retrait sur la tournée de départ, qui n'a
   aucune couche ajoutée : le bilan doit être identique. Le travail reste vert et
   l'annonce par un `::warning::` visible sur la PR.

Codes de retour : 0 conforme, 1 écart, 2 mauvais usage (option inconnue,
étape `preparer` manquante). Mot de passe de base : `SOMMIER_CI_PASSWORD`
(celui du service éphémère de la CI, défini une seule fois dans le workflow).

## Limite de la tournée de démo

Elle ne porte aucune saisie nouvelle : ses trois placettes sont déjà au registre.
Le bilan est `0 écrits, 3 déjà présents`. Pour que (a) mesure quelque chose, l'export
de la 006 doit contenir au moins un contrôle ou une placette saisis.

## Ce que la mission 006 doit fournir

- L'export `.zip` **décompressé** dans `ci/sommier/export-surculus/` (variable
  `EXPORT_SURCULUS` du workflow) : un dossier contenant `travaux.gpkg` (et
  `DCIM/` si des photos sont jointes).
- Cet export **part de `ci/sommier/sortie/tournee-sans`** du même job. Chaque
  exécution engendre sa démo, donc ses propres UUID de forêt et de placettes ; un
  export issu d'une autre exécution est refusé par l'import (« travaux suivis
  inconnus »). Surculus y ajoute les six couches, et ne touche à aucune couche ni
  aucun champ de sommieR.
- L'étape de la 006 s'insère dans le workflow entre « Préparer » et « Rejouer ».
- La 006 doit **rendre l'export obligatoire** (supprimer le test `[ -f ... ]` de
  l'étape « Rejouer »), sinon l'absence reste un vert avec avertissement.
