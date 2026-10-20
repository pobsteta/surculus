# ADR 0007 — Couches ajoutées au contrat `travaux.gpkg` : schéma possédé par sommieR, écrit par anticipation par Surculus

- **Date :** 2026-10-15
- **Statut :** acceptée par Pascal (2026-10-15), après critique du `contradicteur`
- **Proposée par :** Claude (orchestrateur)
- **Tranchée par :** Pascal

## La décision en une phrase

Le schéma des couches de plants (`plants`, `etats_plants`, `constats_reprise`, `vides`,
`positions_controles`, `projet`) appartient à sommieR, qui les définira dans son modèle
sans préfixe (brief `vers-sommier` lot 2) ; Surculus les écrit à l'identique, par
anticipation, sans toucher aux couches ni aux champs existants de sommieR, conformes au
standard GeoPackage, avec une version unique au niveau du fichier ; et, tant que sommieR
ne verse pas les plants à l'aller, Surculus reprend ceux de la tournée rendue précédente.

## Contexte

Le contrat appartient à sommieR (brief, ADR 0001). L'import 0.31.1 lit une **liste fixe**
de couches (`travaux_surf`, `travaux_lin`, `travaux_pt`, `placettes`, `controles`,
`photos` : `R/travaux_qfield.R` l. 167-177 et 309-310) et en ignore toute autre, sans
erreur ni avertissement.

**Erreur corrigée** (l'orchestrateur s'était trompé) : la première version proposait des
couches préfixées `surculus_`, possédées par Surculus, à l'inverse du brief lot 2 déjà
écrit, qui demande à sommieR de créer `plants`, `etats_plants` et `projet` dans son
modèle. Le brief lot 2 avait aussi pris du retard sur la spec (états du DSF, non observé,
regarnis, azimut brut) : il est réaligné.

Options envisagées :

1. **Schéma possédé par sommieR, sans préfixe, écrit par anticipation par Surculus**
   (retenue par Pascal) : une seule source du schéma.
2. **Schéma possédé par Surculus, préfixe `surculus_`** : plus autonome, deux sources du
   schéma des plants.
3. **Fichier séparé** (`surculus.gpkg`) : deux fichiers à garder ensemble.
4. **Champs ajoutés aux couches de sommieR** : interdit par le brief.

## Règles

| Sujet | Règle |
|---|---|
| Couches de sommieR | lues et écrites comme 0.31.1, sans champ ajouté ni renommé ; totaux de `controles` toujours écrits |
| Couches de plants | noms et champs de `docs/spec.md` § 5, identiques au brief lot 2 ; tant que sommieR ne les a pas dans son modèle, Surculus les crée lui-même ; dès qu'il les a, c'est sa définition qui fait foi |
| Conformité GeoPackage | chaque table enregistrée dans `gpkg_contents` (`features` ou `attributes`, OGC 12-128 Req. 118) ; clé primaire INTEGER alias de `rowid` (Req. 119) ; `srs_id` repris de `gpkg_spatial_ref_sys` déjà présent ; valeurs permises (`etat`, `cause_groupe`, `unite_azimut`…) par l'extension **Schema** (`gpkg_data_columns`, `gpkg_data_column_constraints`), qui voyage avec le fichier |
| Version | **une seule**, au niveau du fichier, dans `projet` ; les champs écrits par sommieR (`nom`, `foret`, `prepare_le`, `version_sommier`, version du schéma) sont séparés de ceux de Surculus (`operateur`, `version_surculus`) ; un lecteur de version N lit un fichier N+1 en ignorant les champs nouveaux |
| Repère | lu dans `gpkg_spatial_ref_sys`, jamais codé en dur ; EPSG:2154 tant que sommieR n'est pas passé à EPSG:9794 par une version de son modèle (modèle QGIS, base, import et ortho ensemble, ADR 0003) ; un fichier qui mêle 2154 et 9794 est refusé |
| Plants à l'aller | tant que sommieR ne les verse pas : Surculus reprend les plants de la **tournée rendue précédente** (son `.zip` exporté ou sa copie locale), rattachés par `placette_uuid` ; dès que sommieR les verse, sa version fait foi (arbitrage de Pascal) |
| Export | un `.zip` ; décompressé, c'est le dossier que lit l'import de sommieR |
| Critère de CI | (a) le bilan de `sommier_importer_qfield_travaux()` est **identique** avec et sans les couches ajoutées ; (b) le schéma des couches de sommieR est inchangé (champs, types, géométrie, `srs_id`), comparé au modèle `inst/qgis/travaux.gpkg` ; (c) `sf::st_layers()` liste toutes les couches ajoutées ; l'import tourne sur le `.zip` décompressé, avec un service PostgreSQL/PostGIS en CI |

## Le crible FRANCE

| Lettre | Question | Réponse (une ligne) | Ce que ça coûte |
|---|---|---|---|
| **F**rugale | La plus simple qui tienne ? | Oui : un seul fichier, un seul schéma, aucune modification des couches existantes. | Écrire des tables conformes au standard (extension Schema comprise). |
| **R**esponsable | Qui fait quoi ? | sommieR possède le schéma ; Surculus l'écrit ; Pascal porte le brief dans sommieR. | Pascal, seul mainteneur des deux, porte la décision côté sommieR. |
| **A**nticipateur | Si l'imprévu arrive ? | Si sommieR tarde, Surculus écrit les couches et reprend les plants de la tournée précédente : l'essai du printemps 2027 ne dépend pas de sommieR. | Risque de **retard** côté registre : les plants n'y entrent que quand sommieR les lit. |
| **N**ovateur | Toutes les idées ? | Quatre options ; extension Schema et Metadata du standard examinées. | — |
| **C**oût | Coût pour l'ensemble ? | Un critère de CI qui mesure vraiment quelque chose. | Un service PostGIS en CI ; R dans la CI de Surculus. |
| **É**cologique | Que réutilise-t-on, qui réutilisera ? | Standard OGC, extension Schema lue par GDAL (≥ 3.3), QGIS, QField. | — |

## Valeurs sacrifiées

- **Autonomie de Surculus** : il attend la spec de sommieR pour figer le schéma, et doit
  suivre ses changements. Accepté pour une seule source du schéma.

## Sources

- sommieR : `R/travaux_qfield.R` (l. 59-102, 162-177, 216, 299, 309-314, 465, 472-474),
  `data-raw/qfield_modele.py` (l. 58, 139, 164-169, 825-832, 864, 869),
  `tests/testthat/test-travaux-qfield-db.R`.
- `pilotage/briefs/vers-sommier/2026-10-15-travaux-2-plants.md` (réaligné le 2026-10-15).
- `docs/spec.md` § 4.3, 4.4, 5, 10 ; ADR 0001, 0002, 0003, 0004.
- OGC GeoPackage 1.4 (OGC 12-128r19), Req. 118 et 119, annexes F.8 et F.9 :
  https://www.geopackage.org/spec/ ; extension Schema :
  https://www.geopackage.org/guidance/extensions/schema.html
- GDAL, pilote GPKG : https://gdal.org/en/stable/drivers/vector/gpkg.html

## Question de contrôle

Cette décision tient-elle nos six valeurs, et que sommes-nous prêts à payer pour cela ?
