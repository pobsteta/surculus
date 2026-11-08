# Mission 084 — Les refus de lecture d'une tournée en français

> Fiche écrite par l'orchestrateur le 2026-11-08, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> après la décision de Pascal du 2026-11-02 (PR #146, mission 082 : « mission suivante ») et son choix du 2026-11-08
> pour le cycle 21. Depuis `main`. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Réécrire les quatre refus de la lecture d'une tournée encore techniques à
l'écran (`ReferentielSpatialInconnu`, `Referentiel` et ses trois cas EPSG, `CoucheAbsente`, `PointHorsFrance`) en une
phrase française qui dit ce qui ne va pas et quoi faire, sans nom de table, sans UUID ni coordonnée brute, pour que le
forestier qui ouvre une tournée refusée sache quoi demander à celui qui l'a préparée.

**Redite par l'agent :**

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? (sources, conventions nommées ci-dessous) | oui : la règle de la 082 (phrase française qui dit quoi faire, détail technique hors de l'écran) | |
| A-t-il les moyens ? (outils, données d'essai, tests) | oui | le `testeur` écrit d'abord les tests |

**Temps de la main :** appuyer.

## Ma réponse d'abord

_Rempli par Pascal seul._

## L'Homme

- **Agents :** `testeur` d'abord (tests depuis cette fiche, la fiche 082 et la spec, **sans lire le code qui produit
  les textes**), puis `dev` (rend les tests verts).
- **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** les refus de l'export (`ValidationExport`, `ExportTravaux`, contrôles d'intégrité :
  bac à sable) ; les autres refus de `RefusGpkg` qui ne s'affichent pas à l'ouverture d'une tournée ; changer les
  `motif` (codes stables, lus par les tests et par sommieR) ; changer les préfixes d'écran.

## Ce qui est constaté (vérifié sur `main`, beaa223)

Les refus s'affichent sous les préfixes « Tournée refusée : », « Illisible : » et « Carte impossible : »
(`app/src/main/res/values/strings_tournees.xml:55`, `:56`, `:74`).

| Refus | Où | Texte aujourd'hui |
|---|---|---|
| `ReferentielSpatialInconnu` | `gpkg/.../Refus.kt:38-41` | « Le srs_id N n'est pas décrit dans gpkg_spatial_ref_sys, ou n'est pas un code EPSG. » |
| `Referentiel` → `ReferentielsMeles` | `core/.../reperes/Resultat.kt:112-115` | « Le fichier mêle les référentiels EPSG 2154 et 9794 : une tournée n'en porte qu'un. » |
| `Referentiel` → `ReferentielNonPrisEnCharge` | `Resultat.kt:117-120` | « Référentiel EPSG 4326 non pris en charge : EPSG:2154 ou EPSG:9794 seulement. » |
| `Referentiel` → `AucunReferentiel` | `Resultat.kt:122-125` | « Aucun référentiel déclaré dans le fichier. » (ne dit pas quoi faire) |
| `CoucheAbsente` | `Refus.kt:33-36` | « La couche « X » manque : … » : majuscule après le préfixe (« Tournée refusée : La couche… ») |
| `PointHorsFrance` | `Refus.kt:49-53` | « Point de « placettes » (UUID) hors de France : x = 4.8123456, y = 47.3… (Lambert-93, x = Est, y = Nord). » |

## Harnais

- **Testeur peut :** écrire des tests dans `core/src/test/`, `gpkg/src/test/` et `app/src/test/` qui vérifient, par
  l'API publique des refus ou par le texte affiché, que chacun de ces refus, **composé avec son préfixe d'écran** : ne
  contient ni `_`, ni nom de table GeoPackage, ni UUID, ni nombre à plus de deux décimales ; ne met pas de majuscule
  après « : » ; dit une action (qui contacter ou quoi refaire). Les pousser en premier sur
  `mission/084-refus-de-lecture-en-francais`.
- **Dev peut :** modifier les `message` de ces refus et ce qui les compose à l'écran ; mettre le détail technique au
  journal Android ou dans un champ non affiché (comme la 082). Il ne réécrit pas les tests du `testeur` (s'il en juge un
  faux, il le dit dans la PR).
- **Ne doivent pas :** changer un `motif` ; modifier `fixtures/`, `docs/`, `pilotage/` (sauf la redite) ; toucher à
  `main`.
- **Sources autorisées :** `docs/spec.md`, fiche 082 (règle et décisions du 2026-11-02), cette fiche.

## Valeur provisoire (chiffre, réglage)

Aucune. Les phrases nouvelles sont listées dans la PR (refus, phrase composée telle qu'elle s'affiche, préfixe
compris) ; Pascal les tranche à la relecture. Les deux « : » des phrases composées sont admis (Pascal, 2026-11-02).

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** aucune autre mission ; sur la branche, le `dev` part du commit du `testeur`.
- **Ordre de fusion :** une seule PR.
- **PR en brouillon jusqu'à :** la fin du travail du `dev`.
- **CI rouge attendue :** les tests du `testeur`, entre son push et celui du `dev` (« rouge justifiée »).

## Tests

- Tests du `testeur` vus rouges sur `main`, verts après le `dev` ; `.claude/test-command` au code 0.
- Un test existant qui fige un ancien texte est dit dans la PR, pas affaibli en silence.

## Terrain

- **Échantillon que Pascal vérifiera :** sur la release, l'import d'une tournée refusée (par exemple un GeoPackage en
  EPSG:4326), capturé par l'orchestrateur.

## Qui veille, qui tranche

- **Relecteur :** `relecteur` (chaque texte contre le code, composé avec son préfixe), puis Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Point de suivi :** au push du `testeur`, puis à la PR.
- **Qui tranche :** Pascal (les textes, la fusion).

## Comment saura-t-on que ça marche ?

Aucun des six textes du tableau, composé avec son préfixe, ne montre plus `srs_id`, `gpkg_`, un UUID ou une coordonnée
brute ; chacun dit quoi faire ; la capture de l'import refusé est en français.
