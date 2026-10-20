# CI : import sommieR rejoué (mission 007, ADR 0007)

Travail « import sommieR » : `.github/workflows/import-sommier.yml`. Il installe
sommieR **0.31.1** (tag `v0.31.1`, commit `fd92078e9f538aefbf1181cae7e88f64a5f8961d`),
monte un service PostGIS éphémère et lance `verifier.R`.

## Ce que fait `verifier.R`

1. **`--etape preparer`** : base `sommier_modele` (schéma de sommieR, démo Couchey
   par `sommier_demo_couchey()`), puis la tournée de départ telle que sommieR la
   produit (`sommier_projet_qfield_travaux()`) dans `<sortie>/tournee-depart`.
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
3. **Auto-test du retrait**, toujours : six couches vides (dont trois
   géométriques, donc avec index spatial) et des lignes de `gpkg_data_columns`
   sont posées sur une copie de la tournée de départ, puis retirées ; il ne doit
   rester aucune trace (`sqlite_master`, y compris `rtree_<couche>_*`,
   `gpkg_contents`, `gpkg_geometry_columns`, `gpkg_data_columns`,
   `gpkg_extensions`, `gpkg_metadata_reference`, `gpkg_ogr_contents`).
4. **Sans `--avec`** (usage local seulement ; la CI le fournit toujours) : le retrait est éprouvé, le critère (a) n'est
   **pas mesuré**. Le travail reste vert et l'annonce par un `::warning::`
   visible sur la PR. **Avec `--avec`**, un import qui n'écrit rien
   (travaux + placettes + contrôles = 0) est un écart : un vert qui ne mesure
   rien doit le dire.

**Critère (b) resserré par rapport à l'ADR 0007** : l'ADR parle de champs, types,
géométrie et `srs_id` ; la comparaison porte aussi sur `notnull`, `pk` et les
drapeaux z/m de `gpkg_geometry_columns`.

Codes de retour : 0 conforme, 1 écart, 2 mauvais usage (option inconnue ou donnée
deux fois, étape `preparer` manquante). Mot de passe de base : `SOMMIER_CI_PASSWORD`
(celui du service éphémère de la CI, défini une seule fois dans le workflow).

## Limite de la tournée de démo

Elle ne porte aucune saisie nouvelle : ses trois placettes sont déjà au registre.
Le bilan est `0 écrits, 3 déjà présents`. Pour que (a) mesure quelque chose, l'export
de la 006 doit contenir au moins un contrôle ou une placette saisis.

## Ce que la CI fournit à `verifier.R` (missions 006 et 022)

Le workflow, entre « Préparer » et « Rejouer », installe le JDK 21 et lance
`./gradlew :gpkg:exporterScenario -Ptournee=ci/sommier/sortie/tournee-depart
-Pscenario=fixtures/export/scenario-couchey.json -Psortie=ci/sommier` : l'outil
(classes de test de `:gpkg`, jamais dans l'APK) rejoue deux contrôles sur la tournée
**de ce job** (UUID propres à l'exécution ; placettes désignées par leur code) et
écrit `ci/sommier/export-surculus/travaux.gpkg` (variable `EXPORT_SURCULUS`) et
`export-surculus.zip`. L'étape « Rejouer » exige cet export : s'il manque, le travail
échoue. Le résumé du job affiche « Critère (a) mesuré » et le bilan d'entrée.
