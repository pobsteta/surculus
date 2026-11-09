# Mission 103 — « Placette introuvable » en français, sans UUID

> Fiche écrite par l'orchestrateur le 2026-11-09, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> après le choix de Pascal du 2026-11-09 pour le cycle 22 (« UUID de la carte »). Entrée du bac à sable issue de la
> relecture de la 082. Depuis `main`. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Remplacer le refus « Carte impossible : placette <uuid> » du guidage et du
contrôle par une phrase française qui dit ce qui ne va pas et quoi faire, sans UUID, pour que le forestier qui tombe
dessus en forêt sache revenir à la liste et quoi demander, comme les refus de la 082 et de la 084.

**Redite par l'agent :**
- *Testeur :* écrire, sans lire le code des écrans, des tests Robolectric qui ouvrent le guidage (placette absente, puis placette sans position) et le contrôle (placette absente) sur une vraie tournée d'essai, et qui échouent tant que le refus affiché, préfixe compris, montre un UUID, met une majuscule après « : » ou ne dit pas quoi faire.
- *Dev :* _à remplir_

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? (sources, conventions nommées ci-dessous) | oui : la règle de la 082 et de la 084 (phrase française en minuscule après « : », qui dit quoi faire ; détail technique hors de l'écran) | |
| A-t-il les moyens ? (outils, données d'essai, tests) | oui : Robolectric, tournée d'essai des tests existants avec un `placetteUuid` absent | le `testeur` écrit d'abord les tests |

**Temps de la main :** appuyer.

## Ma réponse d'abord

_Rempli par Pascal seul._

## L'Homme

- **Agents :** `testeur` d'abord (tests depuis cette fiche et la spec, **sans lire le code qui produit le texte**),
  puis `dev` (rend les tests verts).
- **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** les autres refus (`Chargement.Refus`, déjà traités par la 084) ; les textes
  `carte_placette_sans_position` et `carte_placette_hors_france` (ils montrent le code de placette, pas l'UUID) ;
  changer le préfixe « Carte impossible : » ; changer la navigation.

## Ce qui est constaté (vérifié sur `main`, a0d6f66)

| Écran | Où | Quand | Texte aujourd'hui |
|---|---|---|---|
| Guidage | `app/.../carte/EcranGuidage.kt:127` | tournée chargée, mais aucune placette de cet UUID **avec une position** (`cible`, l. 105) | « Carte impossible : placette 3f2a…-… » |
| Contrôle | `app/.../controle/EcranControle.kt:282` | tournée chargée, mais aucune placette de cet UUID (l. 175) | « Carte impossible : placette 3f2a…-… » |

Le préfixe vient de `carte_refus` (`app/src/main/res/values/strings_tournees.xml:74`). Au guidage, deux causes
distinctes donnent le même texte : placette absente de la tournée, ou placette sans position. Le `dev` dit dans la PR
s'il les distingue et pourquoi.

## Harnais

- **Testeur peut :** écrire des tests dans `app/src/test/` qui ouvrent l'écran de guidage et l'écran de contrôle sur
  une tournée d'essai avec un `placetteUuid` absent (et, au guidage, une placette sans position), et vérifient que le
  texte affiché, préfixe compris : ne contient pas l'UUID ni aucun UUID ; ne met pas de majuscule après « : » ; dit une
  action (revenir, choisir une autre placette, ou demander à celui qui a préparé la tournée). Les pousser en premier sur
  `mission/103-placette-introuvable-en-francais`.
- **Dev peut :** modifier ces deux textes et ce qui les compose ; mettre l'UUID au journal Android (comme la 084). Il ne
  réécrit pas les tests du `testeur` (s'il en juge un faux, il le dit dans la PR).
- **Ne doivent pas :** modifier `fixtures/`, `docs/`, `pilotage/` (sauf la redite) ; toucher à `main`.
- **Sources autorisées :** `docs/spec.md`, fiches 082 et 084, cette fiche.

## Valeur provisoire (chiffre, réglage)

Aucune. Les phrases nouvelles sont listées dans la PR, telles qu'elles s'affichent, préfixe compris ; Pascal les
tranche à la relecture.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** aucune autre mission ; sur la branche, le `dev` part du commit du `testeur`.
- **Ordre de fusion :** une seule PR.
- **PR en brouillon jusqu'à :** la fin du travail du `dev`.
- **CI rouge attendue :** les tests du `testeur`, entre son push et celui du `dev` (« rouge justifiée »).

## Tests

- Tests du `testeur` vus rouges sur `main`, verts après le `dev` ; `.claude/test-command` au code 0.
- Un test existant qui fige l'ancien texte est dit dans la PR, pas affaibli en silence.

## Terrain

- **Échantillon que Pascal vérifiera :** le cas est difficile à provoquer sur le téléphone (placette disparue entre la
  liste et l'écran). La preuve est le rendu Robolectric joint à la PR (rapport `rendu-des-ecrans` de la CI) ; pas de
  capture téléphone exigée, sauf si l'orchestrateur trouve un chemin simple.

## Qui veille, qui tranche

- **Relecteur :** `relecteur` (chaque texte contre le code, composé avec son préfixe), puis Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Point de suivi :** au push du `testeur`, puis à la PR.
- **Qui tranche :** Pascal (les textes, la fusion).

## Comment saura-t-on que ça marche ?

Aucun écran du guidage ni du contrôle ne montre plus un UUID ; le texte dit quoi faire ; le rendu est joint à la PR.
