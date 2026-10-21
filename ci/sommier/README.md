# CI : import sommieR rejoué (mission 007, ADR 0007)

Travail « import sommieR » : `.github/workflows/import-sommier.yml`. Il installe
sommieR **0.31.1** (tag `v0.31.1`, commit `fd92078e9f538aefbf1181cae7e88f64a5f8961d`),
monte un service PostGIS éphémère et lance `verifier.R`.

## Ce que fait `verifier.R`

1. **`--etape preparer`** : base `sommier_modele` (schéma de sommieR, démo Couchey
   par `sommier_demo_couchey()`), puis la tournée de départ telle que sommieR la
   produit (`sommier_projet_qfield_travaux()`) dans `<sortie>/tournee-depart`.
   C'est l'entrée de Surculus (mission 006), pas le témoin.
2. **`--etape comparer`** (mission 026, ADR 0007 amendée). L'entrée est l'export
   décompressé de Surculus (`--avec`) et son `bilan-export.json` (`--bilan-export`,
   écrit à côté du zip par `:gpkg:exporterScenario`). L'export est importé par
   `sommier_importer_qfield_travaux()` dans un clone de `sommier_modele`. Trois
   contrôles, dans `criteres.R`, testés sans base par `test-criteres.R` :
   - **F1** : le bilan d'import (travaux, placettes, contrôles, entrées écrites)
     égale le bilan d'export de Surculus, et « déjà présents » égale le nombre de
     lignes de la tournée de départ que sommieR connaît au registre (travaux,
     placettes, contrôles : la tournée est tirée du registre). Un champ absent d'un
     côté n'est jamais une égalité.
   - **F2** : pour chaque table de données de la tournée de départ, l'export ne
     diffère que par des lignes **ajoutées** (clé primaire absente du départ). Une
     ligne retirée ou modifiée (toutes colonnes, géométrie comprise) est un écart,
     et les UUID ajoutés doivent être exactement ceux de `uuid_ecrits` du bilan
     d'export, en minuscules.
   - **F3** : schéma complet identique à celui de la tournée de départ :
     `application_id`, `user_version`, tout `sqlite_master` (DDL, index,
     déclencheurs, vues), toutes les lignes des tables `gpkg_*`. Seuls ajouts
     permis : les objets et lignes des six couches (dont `rtree_<couche>_*`), et
     le support de l'extension Schema (`gpkg_data_columns`,
     `gpkg_data_column_constraints`, lignes `gpkg_schema` de `gpkg_extensions`).
     `gpkg_ogr_contents.feature_count` n'est pas normalisé : il est vérifié, pour
     chaque couche de départ, égal à son nombre de départ plus les lignes
     ajoutées comptées par F2.
   - **Seule exclusion** de F3 : `gpkg_contents.last_change`, qui change à chaque
     écriture (décision de Pascal, 2026-10-21). Rien d'autre n'est toléré.
   - (b) schéma des couches de sommieR de l'export contre `inst/qgis/travaux.gpkg`
     et (c) `sf::st_layers()` liste les six couches : inchangés.
   - **Vérification de fumée**, hors critères : le même export privé de ses six
     couches s'importe avec le même bilan (`bilan-avec.json`, `bilan-sans.json`).
     Ce n'est plus le critère (a) : elle ne mesure presque rien, et deviendrait
     fausse au lot 2 de sommieR.
   - Le résumé du job est `sortie/resume.md` (écrit même en cas d'écart).
3. **Auto-test du retrait**, toujours : six couches vides (dont trois
   géométriques, donc avec index spatial) et des lignes de `gpkg_data_columns`
   sont posées sur une copie de la tournée de départ, puis retirées ; il ne doit
   rester aucune trace (`sqlite_master`, y compris `rtree_<couche>_*`,
   `gpkg_contents`, `gpkg_geometry_columns`, `gpkg_data_columns`,
   `gpkg_extensions`, `gpkg_metadata_reference`, `gpkg_ogr_contents`).
4. **Sans `--avec`** (usage local seulement ; la CI le fournit toujours) : le retrait
   est éprouvé, F1, F2 et F3 ne sont **pas mesurés**, et le script le dit dans son
   message. **Avec `--avec`**, un import qui n'écrit rien (travaux + placettes +
   contrôles = 0) est un écart : un vert qui ne mesure rien doit le dire.

**Limite de F2 (à lever par la mission 027)** : les UUID attendus sont ceux du
`bilan-export.json`, calculés par `uuidDe` de `ExporterScenario`, c'est-à-dire par le
code même que F2 contrôle. Un UUID faux de la même façon des deux côtés passerait.
Le registre attendu, écrit à la main (mission 027), lèvera ce cercle.

**Critère (b) resserré par rapport à l'ADR 0007** : l'ADR parle de champs, types,
géométrie et `srs_id` ; la comparaison porte aussi sur `notnull`, `pk` et les
drapeaux z/m de `gpkg_geometry_columns`.

Codes de retour : 0 conforme, 1 écart, 2 mauvais usage (option inconnue ou donnée
deux fois, étape `preparer` manquante). Mot de passe de base : `SOMMIER_CI_PASSWORD`
(celui du service éphémère de la CI, défini une seule fois dans le workflow).

## Limite de la tournée de démo

Elle ne porte aucune saisie nouvelle : ses trois placettes sont déjà au registre.
Le bilan de la tournée seule est `0 écrits, 3 déjà présents`. Pour que F1 et F2
mesurent quelque chose, l'export de la 006 contient au moins un contrôle ou une
placette saisis (le scénario Couchey en joue deux).

## Ce que la CI fournit à `verifier.R` (missions 006, 022 et 026)

Le workflow, entre « Préparer » et « Rejouer », installe le JDK 21 et lance
`./gradlew :gpkg:exporterScenario -Ptournee=ci/sommier/sortie/tournee-depart
-Pscenario=fixtures/export/scenario-couchey.json -Psortie=ci/sommier` : l'outil
(classes de test de `:gpkg`, jamais dans l'APK) rejoue deux contrôles sur la tournée
**de ce job** (UUID propres à l'exécution ; placettes désignées par leur code) et
écrit `ci/sommier/export-surculus/travaux.gpkg` (variable `EXPORT_SURCULUS`),
`export-surculus.zip` et `ci/sommier/bilan-export.json`. L'étape « Rejouer »
exige l'export et son bilan : s'ils manquent, le travail échoue. Le résumé du job
affiche F1, F2 et F3 avec leur verdict, les compteurs de F1 (attendu et mesuré) et les
lignes ajoutées par couche ; le détail reste dans l'artefact `bilans-import-sommier`.
