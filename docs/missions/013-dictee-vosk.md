# Mission 013 — Dictée Vosk à grammaire fermée

> Fiche écrite par l'orchestrateur le 2026-10-15 (`/mission`, porte n° 2), à partir du
> gabarit `docs/modeles/fiche-mission.md`. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Brancher la dictée hors ligne (interface `MoteurDictee` dans `:core`, implémentation Vosk 0.3.75, modèle `vosk-model-small-fr-0.22` importable par fichier avec SHA-256 vérifié) sur la grammaire de la spec, avec un test qui vérifie que chaque mot est dans le vocabulaire du modèle, pour saisir sans les mains.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

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
