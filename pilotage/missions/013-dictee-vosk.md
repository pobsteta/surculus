# Mission 013 — Dictée Vosk à grammaire fermée

> Fiche écrite par l'orchestrateur le 2026-10-15 (`/mission`, porte n° 2), à partir du
> gabarit `imperatrice/skills/mission/fiche-mission.md`. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Brancher la dictée hors ligne (interface `MoteurDictee` dans `:core`, implémentation Vosk 0.3.75, modèle `vosk-model-small-fr-0.22` importable par fichier avec SHA-256 vérifié) sur la grammaire de la spec, avec un test qui vérifie que chaque mot est dans le vocabulaire du modèle, pour saisir sans les mains.

**Redite par l'agent :** Je dois écrire l'interface `MoteurDictee` (`:core`) et son implémentation Vosk 0.3.75 (`:app`), avec import par fichier du modèle `vosk-model-small-fr-0.22` accepté seulement si son SHA-256 est le bon (casse ignorée), sur la grammaire fermée de la spec § 4.3, et un test prouvant que chaque mot est dans le vocabulaire du modèle, pour que le forestier saisisse sans les mains, hors ligne (redite du 2026-10-24, `dev`).

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? (sources, conventions nommées ci-dessous) | oui : ADR 0004 ; Marculus (`VoiceGrammar.kt`, `ModeleVosk.kt`, spec dictée) en lecture seule | |
| A-t-il les moyens ? (outils, données d'essai, tests) | oui : le modèle se télécharge ; la liste de mots s'en extrait | |

**Temps de la main :** **appuyer**.

## L'Homme

- **Agent :** `dev`
- **Modèle :** sonnet
- **Ce qui ne lui est pas demandé :** pas de banc d'essai audio (plus tard).

## Harnais

- **Peut :** écrire l'interface et l'implémentation ; inscrire la somme SHA-256 dans `docs/dependances.md`.
- **Ne doit pas :** sortir du périmètre de cette phrase ; inventer un code (travaux, essence, état) ; toucher à `main` ; modifier sommieR, Marculus ou Groma ; trancher une question de sens (la remonter à l'orchestrateur).
- **Sources autorisées :** ADR 0004 ; spec § 4.3 (grammaire) ; Marculus.

## Tests

- **Comment saura-t-on que c'est juste :** vocabulaire couvrant la grammaire ; analyse des phrases de la grammaire (« hauteur quarante-cinq », « non observé ») ; modèle à mauvaise empreinte refusé. Commande du projet : `.claude/test-command`.

## Terrain

- **Échantillon que Pascal vérifiera :** Pascal dicte une placette en forêt, vent compris.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal.
- **Point de suivi :** à la remise du plan (missions tenues) et à l'ouverture de la PR.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

les mots de la grammaire sont reconnus sur le terrain.

## Moyens ajoutés le 2026-10-21

- Cas d'acceptation : `fixtures/dictee/` (mission 025, PR #41), à brancher **sans les modifier** ;
  `DicteeRejeuTest` à réactiver ; port de test `DicteePort.kt`. Mots de la grammaire :
  « climat », « mangé », « remplace » (spec § 4.3 amendée). SHA-256 de l'archive
  `cabf6180…be12ad`, confirmé par deux téléchargements, à inscrire dans
  `docs/dependances.md`.
- **Avant de lancer la 013**, le `testeur` ajoute les cas des décisions 11 et 12 du
  2026-10-21 (même état redit : aucune ligne et confirmation vocale ; « non observé » sur
  un plant qui a un état : correction) et retire l'hypothèse de DI-303 (décision 13).

## Moyens ajoutés le 2026-10-24

- **Préalables levés** : cas des décisions 11 et 12 (mission 036, PR #63) ; DI-303 sans
  hypothèse (mission 037, PR #73). Tous les cas de `fixtures/dictee/` sont à brancher **sans
  les modifier**.
- **Décision 13** écrite dans la spec § 4.3 (Pascal, 2026-10-24 : « oui au 3 ») : empreinte
  comparée sans tenir compte de la casse.
- **À regarder d'abord** : pendant la 037, la commande de tests a échoué deux fois en local
  avant de passer (`:app:stripDebugDebugSymbols` sur `libjnidispatch.so`, et une
  `NoSuchFileException` sur `in-progress-results-generic.bin`) ; la CI ne le reproduit pas.
  La 013 touche la chaîne Vosk/JNA : si l'échec revient, le rapport le dit avec la sortie,
  sans le masquer.
- **Ma réponse d'abord** : _rempli par Pascal seul, avant de lire le travail de l'agent._
- **Critère d'arrêt de la relecture** : 3 allers-retours ; au-delà, la décision remonte à
  Pascal.
