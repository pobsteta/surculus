# Mission 095 — « Terminer » refusé tant qu'un plant pointé n'a pas d'état

> Fiche écrite par l'orchestrateur le 2026-11-06, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> après l'essai de bout en bout de Pascal sur la tournée de la Colombière et sa décision du même jour. Depuis `main`
> (7e82a8a). Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Griser « Terminer » tant qu'un plant pointé du contrôle n'a reçu ni état ni
« non observé », en nommant le plant qui manque, pour qu'aucun plant ne sorte des totaux sans que l'agent l'ait
décidé.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Le constat (essai du 2026-11-06)

- Contrôle de P35-03 à la maison : trois plants pointés ; le plant n° 2 (2,0 m) n'a reçu aucun état. « Terminer » l'a
  accepté sans rien dire ; le rendu porte `nb_total` = 2 pour 3 plants (`tournee-20261106-184318.zip`).
- C'est conforme au calcul des totaux (spec § 4, l. 303-305 : sains + atteints + morts + absents, hors non observés),
  mais la spec ne disait pas si un plant pointé pouvait rester sans état à la fin.
- « Terminer » est déjà grisé tant que le besoin ou la concurrence manque, avec la phrase de ce qui manque (091,
  `SessionControle.terminer()`, `SuiteSession.ChoixManquant`, vue « Terminer » de `EcranControle.kt`).

## Décision de Pascal (2026-11-06)

**« Bloquer et nommer »** : « Terminer » grisé tant qu'un plant pointé du contrôle n'a ni état (sain, atteint, mort,
absent) ni « non observé », avec une phrase qui nomme le plant, par exemple « Plant n° 2 sans état : dire son état ou
« non observé ». » (plusieurs plants : les nommer tous, ou « Plants n° 2 et 5 sans état… »). Les plants remplacés par
un regarni ne sont pas concernés (ils sortent des totaux, spec § 4).

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : même mécanisme que le besoin et la concurrence (091) | **appuyer** |
| A-t-il les moyens ? | oui | le `testeur` écrit d'abord les tests |

**Temps de la main :** appuyer.

## Ma réponse d'abord

_Rempli par Pascal seul._

## L'Homme

- **Agents :** `testeur` d'abord (tests depuis la spec, cette fiche et la décision, **sans lire** le code de la
  session et de l'écran de contrôle), puis `dev`.
- **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** changer le calcul des totaux ; changer le rendu ; les contrôles déjà terminés au
  journal (ils restent tels quels).

## Tests existants à surveiller (relevés par l'orchestrateur)

- Les tests qui appellent `terminer()` après avoir pointé des plants sans leur dire d'état (environ 40 appels dans 13
  fichiers depuis la 091) : le `testeur` les liste et ajoute l'état manquant, sans toucher aux assertions. Si un test
  exige qu'un plant sans état soit accepté à « Terminer », il le signale à l'orchestrateur.

## Harnais

- **Testeur peut :** tests dans `core/`, `app/src/test/` ; vus rouges.
- **Dev peut :** `core/.../session/` (la règle), la vue « Terminer » de `app/` (la phrase), les tests du `dev`.
- **Ne doivent pas :** toucher à `docs/` (l'orchestrateur amende la spec § 4.3 et § 6) ; toucher à `main`.
- **Sources autorisées :** spec § 4, § 4.3, § 6 ; fiche 091.

Si un test du `testeur` contredit la décision, **le `dev` s'arrête et le dit tout de suite ; il ne reste pas muet.**

## Valeur provisoire (chiffre, réglage)

- Aucune.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** 091 (#156, 7e82a8a).
- **PR en brouillon jusqu'à :** la fin du travail du `dev`.
- **CI rouge attendue :** les tests du `testeur`, entre son push et celui du `dev`.

## Tests

- Un plant pointé sans état : « Terminer » refusé (rien n'est écrit), la phrase nomme le plant par son numéro.
- Le même plant dit « non observé » : « Terminer » accepté ; il sort des totaux.
- Plusieurs plants sans état : tous nommés.
- Un plant remplacé par un regarni, sans état ce jour : n'empêche pas « Terminer ».
- Besoin, concurrence et plant manquants ensemble : la phrase dit tout ce qui manque.
- `.claude/test-command` au code 0 ; CI verte.

## Terrain

- **Échantillon que Pascal vérifiera :** sur la release, un contrôle de la Colombière avec un plant pointé laissé sans
  état : « Terminer » grisé, phrase lisible au soleil.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Qui tranche :** Pascal (phrase, fusion).

## Comment saura-t-on que ça marche ?

On ne peut plus terminer un contrôle en oubliant un plant : « Terminer » dit lequel.
