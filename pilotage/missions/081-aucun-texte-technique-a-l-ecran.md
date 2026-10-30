# Mission 081 — Aucun texte technique à l'écran

> Fiche écrite par l'orchestrateur le 2026-10-30, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> après le bilan du 2026-10-30 (règle : le `testeur` revient sur chaque mission de comportement) et la validation de
> Pascal (porte n° 2). Depuis `main`. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Ne plus montrer au forestier aucun texte technique (identifiant `snake_case`
comme `non_observe`, UUID de plant, nombre décimal brut, message d'exception), pour qu'il lise du français partout où
Surculus lui parle.

**Redite par l'agent :** Testeur : écrire, depuis la fiche et la spec, les tests qui échouent tant qu un texte montré au forestier (refus de placette, distance refusée, pointage, échec d installation de Vosk) contient un `_`, un UUID, un décimal à point ou un nom d exception ; le dev suivra (dev : à compléter).

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? (sources, conventions nommées ci-dessous) | oui (074 : `Libelles` ; 068 : refus en clair) | |
| A-t-il les moyens ? (outils, données d'essai, tests) | oui | le `testeur` écrit d'abord les tests |

**Temps de la main :** appuyer.

## Ma réponse d'abord

_Rempli par Pascal seul._

## L'Homme

- **Agents :** `testeur` d'abord (tests depuis cette fiche et la spec, **sans lire le code qui produit les textes**),
  puis `dev` (rend les tests verts).
- **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** changer les identifiants stables (`motif`, codes d'état) dans les données, le
  journal ou les fixtures : seul ce qui est **affiché** change ; changer un texte déjà en français.

## Ce qui est constaté (bac à sable du kanban)

- Relecture de la 068 : `non_observe` avec un `_` et UUID de plant dans plusieurs messages de `RefusPlacette`.
- Mineurs de la 074 : le pointage (`ControleurPlacette` l. 392) n'appelle pas `Libelles.decimetres` ; `Resultat.kt`
  l. 79 écrit un `Double` brut (« Distance … m refusée »).
- Mineurs de la 052 : `IssueInstallation.Echec` affiche `e.message` brut.

## Harnais

- **Testeur peut :** écrire des tests dans `core/src/test/` et `app/src/test/` qui vérifient, par l'API publique des
  messages, qu'aucun texte montré ne contient `_`, un UUID, un `.` décimal ou un nom d'exception ; les pousser en
  premier sur `mission/081-aucun-texte-technique-a-l-ecran`.
- **Dev peut :** modifier les messages de `core/.../placette/Verdict.kt`, `Resultat.kt`, `ControleurPlacette`,
  `TextesControle`, `SuiviInstallationVosk` et leurs tests existants ; il ne réécrit pas les tests du testeur
  (s'il en juge un faux, il le dit dans la PR).
- **Ne doivent pas :** modifier `fixtures/`, `docs/`, `pilotage/` (sauf la redite) ; inventer un libellé d'état ou de
  cause hors de `docs/referentiels/etats-plants.yaml` ; toucher à `main`.
- **Sources autorisées :** `docs/spec.md`, `docs/referentiels/etats-plants.yaml`, fiches 068 et 074.

## Valeur provisoire (chiffre, réglage)

Aucune. Les textes nouveaux sont proposés dans la PR ; Pascal les tranche à la relecture.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** aucune autre mission ; sur la branche, le `dev` part du commit du `testeur`.
- **Ordre de fusion :** une seule PR.
- **PR en brouillon jusqu'à :** la fin du travail du `dev`.
- **CI rouge attendue :** les tests du `testeur`, entre son push et celui du `dev` (« rouge justifiée »).

## Tests

- Tests du `testeur` vus rouges sur `main`, verts après le `dev` ; `.claude/test-command` au code 0.

## Terrain

- **Échantillon que Pascal vérifiera :** un refus au contrôle (code inconnu, distance refusée) et l'échec d'installation
  de Vosk, sur la release, capturés par l'orchestrateur.

## Qui veille, qui tranche

- **Relecteur :** `relecteur` (contrôle de chaque texte contre le code), puis Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Point de suivi :** au push du `testeur`, puis à la PR.
- **Qui tranche :** Pascal (les textes, la fusion).

## Comment saura-t-on que ça marche ?

Sur la release suivante, aucun écran capturé ne montre `_`, un UUID ou un décimal à point.
