# Mission 093 — Rendre une tournée dont des points tombent hors de la forêt

> Fiche écrite par l'orchestrateur le 2026-11-06, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> après l'essai de Pascal sur la v0.7.109 et sa décision du même jour. Depuis `main` (7e82a8a). Une mission = une
> phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Faire que le rendu d'une tournée terminée écrive tels quels, et signale sans
bloquer, les positions de contrôles et de plants qui tombent hors de l'emprise de la forêt (mais en France), pour
qu'une tournée faite sous couvert, où le fix du téléphone erre, puisse toujours être rendue et importée dans sommieR
sans perdre aucune mesure.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Le constat (essai de Pascal, 2026-11-06)

- Pascal : « Quand la tournée est terminée, si des positions sont hors de forêt, impossible à rendre, alors que ça
  doit l'être. »
- Le rendu refuse tout point hors de l'emprise : `ValidationExport.point()`
  (`gpkg/src/main/kotlin/io/github/pobsteta/surculus/gpkg/ValidationExport.kt` l. 43-47) lève
  `RefusGpkg.PointHorsForet`, traduit en faute « la position du contrôle tombe hors de la forêt »
  (`FautesDuRendu.kt` l. 41-46). Une seule faute bloque toute la tournée (export tout ou rien).
- La spec § 6 (l. 649) l'exige aujourd'hui, avec une seule exception : les points de `photos_terrain`, « gardés et
  signalés » (amendement du 2026-10-28, ADR 0003 : le fix du téléphone peut errer de plusieurs dizaines de mètres sous
  couvert).
- sommieR 0.31.1 accepte un point hors des UG : le contrôle est rattaché par `placette_uuid`, et l'UG du point est
  laissée vide (`R/travaux_qfield.R` l. 467-468, tag v0.31.1). Le refus est donc du côté de Surculus seul.

## Décision de Pascal (2026-11-06)

**« Garder et signaler »** : l'exception des photos s'étend aux contrôles et aux plants. Un point hors de l'emprise de
la forêt, mais en France, est écrit tel qu'il a été mesuré, et le rendu le signale, sans bloquer. **Hors de France, le
refus reste** (la placette en Somalie, `CLAUDE.md`).

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | à moitié : la règle est décidée, mais la forme du signalement (où, avec quelle phrase) et le sort du refus `point_hors_foret` à l'**ouverture** (`Controle.kt` l. 153, `SessionControle.kt` l. 724) sont à proposer | **appuyer** : le `dev` rend d'abord un plan court (une page), puis code sans attendre si le plan ne pose aucune question de sens |
| A-t-il les moyens ? | oui | le `testeur` écrit d'abord les tests |

**Temps de la main :** appuyer.

## Ma réponse d'abord

_Rempli par Pascal seul._

## L'Homme

- **Agents :** `testeur` d'abord (tests depuis la spec, cette fiche et la décision ci-dessus, **sans lire** le code du
  rendu et de l'export), puis `dev`.
- **Modèle :** opus (justesse des données).
- **Ce qui ne lui est pas demandé :** changer le format du GeoPackage ou du journal (pas de nouveau champ sans question
  à Pascal) ; la 089 ; toucher aux photos.

## Questions que le plan du `dev` doit poser à Pascal (s'il y en a)

1. **Le signalement** : où (écran « Rendre la tournée », avant et après le rendu ?) et avec quelle phrase (proposer
   par exemple « P35-01 : position du contrôle hors de la forêt, gardée telle quelle ») ; faut-il une trace dans le
   zip rendu (fichier texte, champ) ou l'UG vide de sommieR suffit-elle ?
2. **L'ouverture** : un fix hors de l'emprise refuse aujourd'hui l'ouverture (`point_hors_foret`). Le plan dit si ce
   refus est encore atteint après le rattachement par code, et s'il faut le garder (il sert au guidage) ou le changer.
   Pascal tranche : la décision du 2026-11-06 porte sur le **rendu**.

Si le `dev` constate qu'un test du `testeur` contredit la décision ou un format, **il s'arrête et le dit tout de suite
à l'orchestrateur ; il ne reste pas muet.**

## Tests existants qui figent l'ancienne règle (relevés par l'orchestrateur)

- `gpkg/src/test/.../gpkg/ExportTravauxTest.kt` l. 400-404 (« un point en France mais hors de l emprise de la foret
  est refuse », Paris) : contredit la décision ; le `testeur` le réécrit (point écrit et signalé).
- `gpkg/src/test/.../gpkg/RenduTourneeDevTest.kt` l. 232-233 (test du `dev`) : traduction des fautes
  `PointHorsForet` ; le `dev` l'adapte si la faute disparaît.
- Refus à l'**ouverture** (`ArriveeControleTest` l. 115, `RefusOuvertureLisiblesTest` l. 50, `SessionControleTest`
  l. 536, `RejeuViaSessionTest` l. 154) : **inchangés** tant que Pascal n'a pas tranché la question 2.

## Harnais

- **Testeur peut :** tests dans `core/`, `gpkg/`, `app/src/test/` ; fixtures ; CI d'import (`ci/sommier/`), par
  exemple une tournée dont un contrôle tombe hors de l'emprise, importée sans faute ; vus rouges.
- **Dev peut :** `gpkg/` (validation et fautes du rendu), le signalement dans `app/`, les tests existants qui
  exigeaient le refus au rendu (en le signalant à l'orchestrateur, qui les confie au `testeur` s'ils sont les siens).
- **Ne doivent pas :** toucher à `docs/` (signaler : l'orchestrateur amende la spec § 6 l. 649 et l'ADR 0003) ;
  toucher à `main` ; lever le refus hors de France.
- **Sources autorisées :** spec § 4.8, § 5, § 6 ; ADR 0003, 0007 ; `docs/formats/` ; sommieR (`~/dev/sommieR`, lecture,
  tag v0.31.1) ; fiches 085, 087, 090, 091.

## Valeur provisoire (chiffre, réglage)

- Aucune.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** 091 (#156, 7e82a8a).
- **PR en brouillon jusqu'à :** la fin du travail du `dev`.
- **CI rouge attendue :** les tests du `testeur`, entre son push et celui du `dev`.

## Tests

- Une tournée terminée dont la position d'un contrôle tombe hors de l'emprise de la forêt, en France, est rendue :
  le point est écrit tel qu'il a été mesuré (mêmes coordonnées, même précision, même source) et le rendu le signale.
- Même chose pour un plant hors de l'emprise.
- Un point hors de France refuse toujours le rendu, avec la faute actuelle (et l'indice d'axes inversés).
- Une tournée sans point hors de l'emprise est rendue sans aucun signalement.
- Après import dans sommieR (CI, deux passes), le contrôle hors de l'emprise est importé et rattaché à sa placette ;
  F5 reste à 8 sur 8.
- `.claude/test-command` au code 0 ; CI verte.

## Terrain

- **Échantillon que Pascal vérifiera :** sur la release, la tournée de Couchey contrôlée à la maison (positions hors de
  la forêt) : « Rendre la tournée » réussit, le signalement est lisible, et l'import dans sommieR fait passer P35-01
  « à programmer ».

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Qui tranche :** Pascal (questions du plan, phrase du signalement, fusion).

## Comment saura-t-on que ça marche ?

La tournée que Pascal n'a pas pu rendre le 2026-11-06 se rend, dit quelles positions tombent hors de la forêt, et
s'importe dans sommieR.
