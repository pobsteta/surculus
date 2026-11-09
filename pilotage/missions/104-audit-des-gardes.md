# Mission 104 — Audit des gardes de rendu et de forme

> Fiche écrite par l'orchestrateur le 2026-11-09, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> après la décision de Pascal au bilan du 2026-11-09 (cause n° 3 du Pareto, « Oui, l'audit »). Depuis `main`.

## Mission

**En une phrase (quoi, et pourquoi) :** Muter chaque garde de rendu et de forme existante de `app/src/test/` (tests qui
lisent un rendu, une forme, une dimension ou une disposition d'écran) et la voir rouge, ou la signaler muette, pour
qu'aucune garde ne protège plus rien sans qu'on le sache (garde tautologique de la 102, quatre tests muets de la 096).

**Redite par l'agent :**
- *Testeur :* _à remplir_

## Les trois questions

| Question | Réponse | Conséquence |
|---|---|---|
| Redire en une phrase ? | oui | |
| Sait-il comment ? | oui : mutation à la main (changer la valeur gardée dans le code de production, sur une copie locale non poussée), voir le test rouge, annuler | |
| Les moyens ? | oui : `.claude/test-command`, Robolectric | |

**Temps de la main :** appuyer.

## Ma réponse d'abord

_Rempli par Pascal seul._

## L'Homme

- **Agent :** `testeur`, sonnet. Ici, le `testeur` lit le code de production, puisqu'il doit le muter ; il n'en pousse
  aucune modification.
- **Ce qui ne lui est pas demandé :** corriger le code de l'application ; réécrire les tests de logique (hors rendu et
  forme) ; renforcer une garde muette sans le dire (il la signale ; il peut proposer le correctif dans un commit à part,
  nommé).

## Harnais

- **Peut :** lister les gardes (par exemple `BancRendu`, `RenduLibellesCodesTerminerTest`, `BoussoleTest`, tests
  `Rendu*`, tests de dimensions de `DimensionsControle`, `DispositionBoutons`) ; pour chacune, une mutation du code de
  production qu'elle doit voir, le résultat (rouge / reste vert), la sortie de test ; remplir la rubrique « Résultat »
  de cette fiche ; corriger une garde muette dans un commit à part.
- **Ne doit pas :** pousser une mutation du code de production ; toucher à `main`, `fixtures/`, `docs/`.
- **Sources :** `CLAUDE.md`, fiches 080, 096 et 102, journal (« une garde n'est prouvée que vue rouge »).

## Valeur provisoire

Aucune.

## Dépend de / rouge attendu jusqu'à

Aucune dépendance. Une seule PR, en brouillon jusqu'à la fin. CI verte attendue (aucune mutation poussée).

## Tests

`.claude/test-command` au code 0 sur la branche poussée.

## Terrain

Sans objet (aucun écran ne change).

## Qui veille, qui tranche

- **Relecteur :** `relecteur` (rejoue deux mutations au hasard), puis Pascal.
- **Critère d'arrêt :** 3 allers-retours. **Qui tranche :** Pascal (garder, corriger ou retirer une garde muette).

## Comment saura-t-on que ça marche ?

Un tableau garde / mutation / rouge ou muette, chaque ligne avec sa sortie de test ; aucune garde muette non signalée.

## Résultat

_À remplir par le testeur._
