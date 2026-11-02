# Mission 085 — Ouvrir un contrôle sans position : ADR, spec et formats

> Fiche écrite par l'orchestrateur le 2026-11-02, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> après le changement de cap de Pascal du 2026-11-02, la critique du `contradicteur` et les réponses de Pascal (journal
> du 2026-11-02, branche `pilotage/cycle-17`). Depuis `main`. Une mission = une phrase = une branche = une PR. Le code
> suivra dans la mission 086 (`testeur` puis `dev`), qui part de cette PR fusionnée.

## Mission

**En une phrase (quoi, et pourquoi) :** Écrire dans l'ADR 0003, la spec, les formats et le brief `vers-sommier` la
règle décidée par Pascal pour ouvrir un contrôle sans aucune position GNSS sur le code lu du piquet, pour que la mission
086 et tout successeur codent une règle unique, sourcée et testable.

**Redite par l'agent :** (`archiviste`, 2026-11-02) Écrire, sans la changer, la règle des neuf points de Pascal
(ouvrir un contrôle sans aucun fix sur le code lu d'un piquet connu de la tournée, après 30 s provisoires et une
confirmation, avec la position de sommieR et sans position mesurée, marquée dans l'export et dans un journal v2 qui
relit le v1) dans l'ADR 0003, la spec, les formats du journal et le brief `vers-sommier`, en corrigeant le contexte de
la règle des 15 m, pour que la 086 et tout successeur codent une seule règle, sourcée et testable ; je ne touche ni au
code, ni aux tests, ni aux fixtures.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui (critique du contradicteur, décisions de Pascal ci-dessous) | |
| A-t-il les moyens ? | oui | |

**Temps de la main :** tenir : la règle touche la justesse des données et le format du journal.

## Ma réponse d'abord

_Rempli par Pascal seul._

## L'Homme

- **Agent :** `archiviste`.
- **Modèle :** opus (justesse des données, formats).
- **Ce qui ne lui est pas demandé :** écrire du code, des tests ou des fixtures (mission 086).

## La règle décidée par Pascal (2026-11-02)

1. **Sans aucun fix** reçu pendant **30 s** (valeur provisoire, mesurée à l'essai en forêt), récepteur actif,
   localisation et permission accordées, Surculus propose « Ouvrir sans position ». Localisation coupée ou permission
   refusée : il demande de l'activer, pas d'ouverture sans position.
2. Le contrôle s'ouvre seulement avec un **code lu** sur le piquet qui existe dans la tournée ; **sans code, rien ne
   s'ouvre**.
3. **Code présent dans plusieurs UG** : l'agent **choisit l'UG** (« P01 : UG 12 ou UG 14 ? »), avec ou sans position
   (corrige aussi le cas avec position, aujourd'hui `firstOrNull` dans `core/.../placette/Controle.kt` l. 55).
4. Avant d'ouvrir sans position, Surculus **affiche la placette à confirmer** (« P35-01 · UG 12 · parcelle 4 · 25
   plants pointés : ouvrir sans position ? »).
5. Le contrôle **garde la position de la placette créée par sommieR** (« par défaut si pas de position, on garde cette
   position », Pascal) ; pas de position mesurée.
6. Un fix reçu, **même unique, médiocre ou simulé**, reste une position et suit la règle actuelle (15 m, ou code lu
   qui l'emporte). **Hors de France : refusé**, avec une phrase qui dit quoi faire (« position aberrante : couper puis
   rallumer la localisation »), le refus écrit au journal.
7. **Export** : dans `positions_controles`, une ligne par contrôle, géométrie **NULL** (pas un POINT EMPTY),
   `precision_m` NULL, nouvelle colonne `sans_position_mesuree` (booléen), `rattache_par_identite` vrai (définition
   élargie : le code lu a désigné la placette, la position mesurée ne l'a pas fait) ; `controles` porte la position du
   piquet (l'import 0.31.1 lit `placette_uuid` et jette cette géométrie, `R/travaux_qfield.R` l. 296-305).
8. **Journal des saisies** : **format v2**, qui admet l'absence de position mesurée quand `sans_position_mesuree` est
   vrai ; Surculus relit toujours le v1 (principe N-1, comme `.sursync`).
9. L'**installation d'une placette** exige toujours une position (inchangé).

## Harnais

- **Peut :** amender `docs/adr/0003-gnss-et-reperes.md` (amendement daté, contexte « règle des 15 m de l'import »
  corrigé, crible, valeurs sacrifiées, tests attendus) ; `docs/spec.md` (§ 4.3 arrivée, § 5 `positions_controles`,
  table des tests) ; `docs/formats/journal-valeurs-v1.md` et un nouveau `docs/formats/journal-valeurs-v2.md` ; le brief
  `pilotage/briefs/vers-sommier/2026-10-22-position-mesuree-du-controle.md` (le compléter : certains contrôles n'auront
  pas de position mesurée) ; ajouter à cette fiche la liste des fichiers de code, fixtures et tests que la 086 devra
  toucher (relevé du contradicteur : `Couches.kt` l. 112-120, `fixtures/acceptation-export/schema-couches-ajoutees.json`,
  `fixtures/regles/rattachement-15m.json`, CI F4).
- **Ne doit pas :** changer la règle (toute question de sens remonte à l'orchestrateur) ; écrire du code ; toucher à
  `main`.
- **Sources autorisées :** ADR 0003, spec, formats, brief cité, sommieR dans `~/dev/sommieR` (lecture), conventions
  citées par le contradicteur (Darwin Core `coordinateUncertaintyInMeters`, `georeferenceSources` ; QField
  `@position_horizontal_accuracy` ; GeoPackage 1.3 sur les points vides).

## Valeur provisoire (chiffre, réglage)

- **Valeur provisoire :** 30 s d'attente sans fix avant « Ouvrir sans position ».
- **Mesurée par :** Pascal, à l'essai en forêt sous couvert (premier fix à froid du récepteur), date à fixer.
- **Gel :** avant la publication de la tranche 1 ; marquée « provisoire » dans l'ADR et la spec.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** aucune ; la 086 dépend de celle-ci.
- **Ordre de fusion :** 085 → 086.
- **CI rouge attendue :** aucune (documentation).

## Tests

- Pas de code ; la PR liste les tests d'acceptation que la 086 devra rendre verts (aucun fix + code ; code en double ;
  fix hors de France + code refusé ; localisation coupée ; un seul fix ; confirmation ; journal v2 relu après
  redémarrage ; v1 toujours relu ; export à géométrie NULL).

## Pour la mission 086 : ce qu'elle devra toucher (relevé de l'`archiviste`, 2026-11-02, sur `main` bb2884b)

Relevé par lecture seule, à confirmer par le `dev` ; numéros de ligne à la tête bb2884b.

**Code**

- `core/src/main/kotlin/io/github/pobsteta/surculus/core/placette/Controle.kt` : `Piquet` l. 24 (pas d'UG : il en faut
  une pour le point 3) ; `Rattache.positionMesuree` l. 36 non nullable ; `Rattachement.rattacher` l. 52-64 exige une
  position mesurée ; `firstOrNull` l. 55 (point 3) ; commentaires l. 31 et l. 40 (définition élargie, « règle des 15 m
  de l'import »).
- `core/src/main/kotlin/io/github/pobsteta/surculus/core/session/Valeurs.kt` : `FormatValeurs.VERSION` l. 22 ;
  `ValeurControle` l. 36-62 et `lire` l. 65-76 (v2, `sans_position_mesuree`, positions mesurées à `null`, lecture du v1).
- `core/src/main/kotlin/io/github/pobsteta/surculus/core/session/SessionControle.kt` (ouverture d'un contrôle).
- `app/src/main/kotlin/io/github/pobsteta/surculus/controle/ArriveeControle.kt` : `EtatArrivee.SansPosition` l. 36,
  `arrivee` l. 48, l. 79 (le code lu est ignoré sans position).
- `app/src/main/kotlin/io/github/pobsteta/surculus/carte/MoyenneArrivee.kt` (attente de 30 s, fix unique) ;
  `app/src/main/kotlin/io/github/pobsteta/surculus/carte/EcranGuidage.kt` l. 168 (construction des `Piquet`), l. 185
  (hors de France) ; écrans de confirmation et de choix de l'UG ; `app/src/main/res/values/strings.xml` (phrases).
- `gpkg/src/main/kotlin/io/github/pobsteta/surculus/gpkg/ContexteDeTournee.kt` l. 33 (`Piquet` : UG, parcelle et nombre
  de plants pour la confirmation).
- `gpkg/src/main/kotlin/io/github/pobsteta/surculus/gpkg/Couches.kt` l. 111-121 (`POSITIONS_CONTROLES` : colonne
  `sans_position_mesuree`, description de `rattache_par_identite` l. 119).
- `gpkg/src/main/kotlin/io/github/pobsteta/surculus/gpkg/Saisies.kt` l. 71-78 (`PositionMesuree.position` non nullable)
  et l. 82-96.
- `gpkg/src/main/kotlin/io/github/pobsteta/surculus/gpkg/ExportTravaux.kt` l. 230-243 (aujourd'hui aucune ligne de
  `positions_controles` sans position mesurée ; géométrie NULL à écrire).
- `gpkg/src/main/kotlin/io/github/pobsteta/surculus/gpkg/ValidationExport.kt` l. 119-120.

**Fixtures**

- `fixtures/acceptation-export/schema-couches-ajoutees.json` (`positions_controles`, l. 93 et suivantes) ;
  `fixtures/acceptation-export/couches-ajoutees.json` ; `fixtures/acceptation-export/determinisme-et-referentiels.json`.
- `fixtures/regles/rattachement-15m.json` (cas sans position, code en double dans deux UG, fix hors de France) ;
  `fixtures/export/scenario-couchey.json` ; `fixtures/README.md` si le format des cas change.

**Tests existants à revoir**

- `core/src/test/.../core/placette/PointageEtRattachementTest.kt`, `core/src/test/.../core/session/SessionControleTest.kt`,
  `core/src/test/.../core/session/RejeuViaSessionTest.kt`, `core/src/test/.../core/fixtures/AdaptateurReglesPlacette.kt`,
  `core/src/test/.../core/guidage/GuidageTest.kt`.
- `app/src/test/.../controle/ArriveeControleTest.kt`, `app/src/test/.../gnss/FixPourSessionControleTest.kt`,
  `app/src/androidTest/.../FumeeGeoPackageTest.kt`.
- `gpkg/src/test/.../gpkg/ExportTravauxTest.kt`, `gpkg/src/test/.../gpkg/SessionEtExportCoucheyTest.kt`,
  `gpkg/src/test/.../gpkg/outil/ExporterScenario.kt`.

**CI d'import (F4)**

- `ci/sommier/f4.R` l. 70-71 et 180-191 (une ligne sans géométrie ne doit pas être « aucune ligne » ni une position) ;
  `ci/sommier/fonctions.R` l. 9 et 190 ; `ci/sommier/verifier.R`, `ci/sommier/test-criteres.R`, `ci/sommier/sabotage.R` ;
  `.github/workflows/import-sommier.yml`. Que `sf` lise une géométrie NULL de GeoPackage comme vide : non vérifié.

**Tests d'acceptation attendus** (repris de l'ADR 0003, amendement du 2026-11-02, « Tests attendus ») :

1. aucun fix pendant 30 s + code lu connu : proposé, confirmé, ouvert à la position du piquet, sans position mesurée ;
2. aucun fix, aucun code : rien ne s'ouvre ; moins de 30 s : pas proposé ;
3. code en double dans deux UG : l'agent choisit l'UG, avec et sans position ;
4. fix hors de France + code lu : refusé, phrase qui dit quoi faire, refus au journal (placette en Somalie) ;
5. localisation coupée, permission refusée : demande de l'activer, pas d'ouverture sans position ;
6. un seul fix (médiocre ou simulé) : règle des 15 m, pas d'ouverture sans position ;
7. confirmation de la placette affichée (code, UG, parcelle, nombre de plants pointés) avant d'ouvrir ;
8. journal v2 relu après redémarrage ; v1 toujours relu à l'identique ;
9. export : géométrie NULL (pas un POINT EMPTY), `precision_m` NULL, `sans_position_mesuree` et `rattache_par_identite`
   vrais, `controles` à la position du piquet, import 0.31.1 vert (F4) ;
10. installation d'une placette sans position : toujours refusée.

**Questions ouvertes avant la 086** (rapport de l'`archiviste`) : posées dans la PR #150 ; réponses de Pascal et
questions restantes dans « Reprise, aller-retour 1/3 (2026-11-02) » ci-dessous.

## Reprise, aller-retour 1/3 (2026-11-02)

Après la relecture de la PR #150 (« prête pour Pascal ») et les décisions de Pascal du 2026-11-02. Documentation
seulement.

**Décisions de Pascal** (écrites dans l'ADR 0003, amendement du 2026-11-02, la spec § 4.3, § 4.8, § 5, § 6, et
`docs/formats/journal-valeurs-v2.md`) :

1. **Code en double entre UG** : « la position tranche » : si une seule homonyme est à 15 m au plus, c'est elle ; le
   choix de l'UG n'est demandé que si rien ne tranche (sans position, ou position loin de toutes). Point 3 réécrit.
   Lecture de l'`archiviste`, à confirmer : deux homonymes à 15 m au plus, rien ne tranche, l'agent choisit (confirmé par Pascal à la reprise 2/3).
2. **`.sursync`** : « nouvelle version » : la sauvegarde passe à la version suivante ; un Surculus ancien la refuse
   avec un message (`version_future`) ; un Surculus à jour lit toujours l'ancienne. **Choix de l'`archiviste`** : je
   n'écris pas le document de la version suivante, je la note pour la 086 (ci-dessous), parce que son numéro n'est pas
   tranché : la v2 est déjà promise au type `photo` (spec § 4.8, décision du 2026-10-28, document « à écrire par la
   mission de code », `FormatSursync.VERSION_COURANTE` = 1 sur `main`). Renvoi ajouté en tête de
   `docs/formats/sursync-v1.md`.
3. **Fix reçu après une ouverture sans position** : « non, fixé à l'arrivée » : le contrôle reste sans position
   mesurée jusqu'à la fin ; « Terminer » le reprend tel quel. Point 10 ajouté.
4. **Refus hors de France** : au journal technique (`JournalTechnique`, `app/.../messages/TextesEchec.kt` l. 23 :
   journal Android), pas au journal des saisies ; la liste des types de ligne ne change pas. Point 6 précisé.

**Mineurs du relecteur, corrigés :** (a) « identique au tag `v0.37.1`, `f0229b9` » (et non 0.38.0) ; (b) format v2 :
« toute autre valeur de `v` est illisible » présenté comme règle nouvelle de la v2 ; (c) même numérotation de 1 à 10
dans l'ADR et la spec ; (d) écart « une ligne par contrôle » contre le scénario Couchey (P35-02 sans ligne) écrit dans
l'ADR 0003 et la spec § 5. Le texte de l'amendement, non encore fusionné, a été corrigé dans la PR même.

**Choix de format du relecteur, non tranchés par Pascal, écrits comme tels :** une seule version du format du
journal (`FormatValeurs.VERSION` = 2, lecteur qui accepte 1 et 2) ; `source_gnss`, `referentiel_source` et `simulee`
NULL sans position mesurée ; `null` explicite et ordre des clés de la v2 ; chaque valeur ignorée à la lecture tracée
au journal technique.

**Ajouts à la liste de la 086**

- Couchey et F4 : `ci/sommier/README.md` l. 54-66 ; `fixtures/export/registre-attendu-couchey.json` (P35-02 et
  « decision ») ; le texte F4 de l'ADR 0007 (`docs/adr/0007-couches-ajoutees-au-contrat-sommier.md` l. 52, « sans mesure (P35-02), c'est le piquet ») :
  la 086 fait de P35-02 un contrôle `sans_position_mesuree`, ou dit pourquoi non ; si cela change ce que F4 prouve,
  la question remonte à Pascal.
- Sauvegarde : `data/src/main/kotlin/io/github/pobsteta/surculus/data/sauvegarde/FormatSursync.kt` l. 22
  (`VERSION_COURANTE`) et l. 130 (`lire`) ; `data/.../sauvegarde/Sauvegarde.kt` l. 42 ;
  `app/.../sauvegarde/EcranRestauration.kt` l. 104 (message `version_future`) ; `fixtures/sursync/` (exemple de la
  version suivante, lecture de la v1) ; `fixtures/sauvegarde/` ; `docs/formats/sursync-v1.schema.json` et le document
  `docs/formats/sursync-v2.md` (à créer par la 086). Numéro tranché à la reprise 2/3 : **v2 partagée** avec le type
  `photo` ; la 086 crée la v2, la partie photos reste à coder plus tard dans le même format.
- Journal technique : `app/src/main/kotlin/io/github/pobsteta/surculus/messages/TextesEchec.kt` (`JournalTechnique`)
  pour le refus hors de France et les valeurs ignorées à la lecture.
- Tests d'acceptation en plus : code en double avec une seule homonyme à 15 m au plus (pas de question) ; fix reçu
  après ouverture sans position (ignoré, « Terminer » inchangé) ; refus hors de France absent du journal des saisies ;
  `.sursync` de la version suivante refusé par un lecteur de la version d'avant, version d'avant lue par le lecteur à
  jour ; valeur `"v":3` ignorée et tracée.

**Questions restantes pour Pascal :** ~~numéro de la version suivante du `.sursync`~~ et ~~deux homonymes à 15 m au
plus~~ (tranchées, reprise 2/3 ci-dessous) ; P35-02 de Couchey (en faire un contrôle sans position
mesurée, avec une ligne à géométrie NULL, change ce que F4 vérifie).

## Reprise, aller-retour 2/3 (2026-11-02)

Décisions de Pascal du 2026-11-02, écrites dans l'ADR 0003, la spec § 4.3 et § 4.8, `docs/formats/sursync-v1.md` et
`docs/formats/journal-valeurs-v2.md` :

1. **`.sursync` : « v2 partagée »** : une seule v2 porte à la fois le type `photo` (décision du 2026-10-28) et les
   contrôles sans position. **La 086 crée la v2** (`FormatSursync.VERSION_COURANTE` = 2, lecture de la v1,
   `docs/formats/sursync-v2.md`, schéma, fixtures) ; la partie photos reste à coder plus tard dans le même format.
2. **Deux homonymes à 15 m au plus** : « l'agent choisit l'UG », borne de 15 m incluse : confirmé. La marque
   « lecture de l'`archiviste`, à confirmer » est retirée (ADR 0003 point 3, spec § 4.3 point 3).

Question encore ouverte : P35-02 de Couchey (reprise 1/3, ajouts à la liste de la 086).

## Terrain

- **Échantillon que Pascal vérifiera :** l'amendement de l'ADR, lu en une minute.

## Qui veille, qui tranche

- **Relecteur :** `relecteur` (fidélité aux décisions, sources), puis Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Un successeur qui lit l'ADR 0003 amendé et la spec sait, sans le journal, ce que fait Surculus sans position.
