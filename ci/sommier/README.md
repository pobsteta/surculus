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
     égale le bilan d'export de Surculus. « Déjà présents » de sommieR = **lignes de la
     tournée de départ** (`travaux_*`, `placettes`, `controles`, avec `uuid`) : la
     tournée est tirée du registre. Le `dejaPresentes` de Surculus a un autre sens
     (rejeu d'un export) et n'est **pas comparé**. Un champ absent d'un
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
     `gpkg_data_column_constraints`). Une ligne `gpkg_schema` de `gpkg_extensions`
     n'est admise que si sa `table_name` est l'une des six couches ou l'une de ces
     deux tables ; sur une couche de sommieR, elle est refusée. Les lignes d'une table de
     l'extension Schema créée par l'export (absente du départ) ne sont permises que sur
     les six couches (mission 028 : ce cas n'était pas vérifié).
     `gpkg_ogr_contents.feature_count` n'est pas normalisé : il est vérifié, pour
     chaque couche de départ, égal à son nombre de départ plus les lignes
     ajoutées comptées par F2.
   - **F4** (mission 028, `f4.R`, option `--registre-attendu`) : le registre de sommieR,
     après l'import, porte exactement les valeurs de
     `fixtures/export/registre-attendu-couchey.json`, écrites à la main par le testeur
     (mission 027) : `nb_total`, `nb_vivants`, `nb_abroutis`, `h_moy_cm` et `visite_le`
     de chaque contrôle écrit, retrouvé par le code de sa placette
     (`payload.placette_id` vers `v_placette.code_placette`) ; un contrôle attendu
     absent, en double, ou écrit sans attente est un écart. **Le `point` n'est pas lu
     au registre** : la position mesurée n'y arrive pas dans sommieR 0.31.1
     (`R/registre6.R:150-180`, `R/travaux_terrain.R:156`, `R/travaux_qfield.R:262-277`,
     `inst/sql/003_geometrie.sql:26-31` ; le résumé du job le dit). Il est comparé à la
     couche `positions_controles` du GeoPackage exporté (docs/spec.md l. 259), avec la
     tolérance de 0,5 m (décision de Pascal, mission 027, `tolerance_m` du JSON) ;
     pour P35-02, sans mesure, au piquet, lu dans la couche `placettes` de l'export.
     Source de ce choix : pilotage/journal.md, 2026-10-22, décision de Pascal
     (mission 028). Coordonnées : `lon` puis `lat`, degrés WGS 84 ; les couches de
     l'export (EPSG:2154) sont converties par sf ; chaque point doit tomber dans le
     rectangle de la France du JSON. **Hypothèse** : le fuseau horaire du processus
     R est celui de l'export (`visite_le` des couches est une heure locale lue par
     sommieR) ; en CI, les deux sont en UTC.
   - **F5** (mission 028, `sabotage.R`) : chaque contrôle F1 à F4 est rejoué sur une
     copie sabotée de l'export et **doit passer au rouge** ; le résumé du job donne,
     par sabotage, le contrôle visé et son message. Un sabotage resté vert est un
     écart (un contrôle qui ne contrôle rien) et fait échouer le job.

     | Sabotage | Contrôle visé |
     |---|---|
     | un contrôle retiré de la couche `controles` de l'export | F1 (import : 1 contrôle, export : 2) |
     | une ligne existante modifiée (`codes`) | F2 |
     | un déclencheur ajouté (sur `codes`) | F3 |
     | `nb_total` de P35-01 + 1, puis import par sommieR | F4 (total) |
     | point de `positions_controles` déplacé de 5 m (> 0,5 m) | F4 (point) |

     Le sabotage « total faux » retire d'abord les déclencheurs R-tree d'UPDATE de
     `controles` de la **copie** (RSQLite n'a pas `ST_IsEmpty`) ; F3 n'y est pas rejoué.
   - **Seule exclusion** de F3 : `gpkg_contents.last_change`, qui change à chaque
     écriture (décision de Pascal, 2026-10-21). Rien d'autre n'est toléré.
   - (b) schéma des couches de sommieR de l'export contre `inst/qgis/travaux.gpkg`
     et (c) `sf::st_layers()` liste les six couches : inchangés.
   - **Témoin « sans »**, simple **avertissement** : le même export privé de ses six
     couches s'importe avec le même bilan (`bilan-avec.json`, `bilan-sans.json`). Une
     différence s'affiche dans `resume.md` et dans le résumé du job, sans faire échouer
     le job. Le verdict tient à F1 à F5, (b), (c) et à l'auto-test du retrait. Ce
     n'est plus le critère (a) : il ne mesure presque rien, et il **deviendra rouge
     par construction au lot 2 de sommieR**, quand l'import lira les plants.
   - Le résumé du job est `sortie/resume.md` (écrit même en cas d'écart).
3. **Auto-test du retrait**, toujours : six couches vides (dont trois
   géométriques, donc avec index spatial) et des lignes de `gpkg_data_columns`
   sont posées sur une copie de la tournée de départ, puis retirées ; il ne doit
   rester aucune trace (`sqlite_master`, y compris `rtree_<couche>_*`,
   `gpkg_contents`, `gpkg_geometry_columns`, `gpkg_data_columns`,
   `gpkg_extensions`, `gpkg_metadata_reference`, `gpkg_ogr_contents`).
4. **Sans `--avec`** (usage local seulement ; la CI le fournit toujours) : le retrait
   est éprouvé, F1 à F5 ne sont **pas mesurés**, et le script le dit dans son
   message. **Avec `--avec`**, un import qui n'écrit rien (travaux + placettes +
   contrôles = 0) est un écart : un vert qui ne mesure rien doit le dire.

**Ce qui a été vu rouge en CI, et ce qui ne l'a pas été.** F2 n'a été vu rouge en CI
que sur `codes` (un `UPDATE`) : la tournée de départ n'a aucune ligne de `controles`,
et `ST_IsEmpty` est absent de RSQLite pour `placettes` (déclencheurs R-tree). Le cas
géométrique n'est couvert que par `test-criteres.R`. `feature_count` vaut NULL dans le
modèle de sommieR : en CI, seul « reste NULL » est mesuré, pas « départ + ajouts »
(cette seconde règle n'est couverte que par `test-criteres.R`).

**Limite de F2** : les UUID attendus sont ceux du `bilan-export.json`, calculés par
`uuidDe` de `ExporterScenario`, c'est-à-dire par le code même que F2 contrôle. Un UUID
faux de la même façon des deux côtés passerait. F4 (mission 028) lève en partie ce
cercle : les valeurs du registre sont comparées à des attentes écrites à la main, mais
les UUID eux-mêmes ne le sont pas.

**Ce qui a été vu rouge, mission 028.** En local (R, PostGIS de la mission en conteneur,
export réel du scénario Couchey) : F4 vert sur l'export réel, 12 lignes conformes
(écart de position de P35-01 : 0,018 m) ; F5, cinq sabotages sur cinq détectés, avec
le message de chaque contrôle ; `test-criteres.R` mis en défaut par dix mutations du
code (tolérance ignorée, totaux non comparés, lon et lat inversées, etc.). Ce qui ne se
voit que dans le job de la PR : la même chose sur le runner GitHub (UTC, PostGIS 16-3.4).

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
affiche F1 à F4 avec leur verdict, le registre ligne à ligne, les sabotages de F5, les compteurs de F1 (attendu et mesuré) et les
lignes ajoutées par couche ; le détail reste dans l'artefact `bilans-import-sommier`.
