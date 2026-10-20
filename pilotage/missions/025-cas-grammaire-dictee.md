# Mission 025 — Cas d'acceptation de la dictée à grammaire fermée

> Fiche écrite par l'orchestrateur le 2026-10-20, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Décidée par Pascal le 2026-10-20 :
> le `testeur` écrit les cas de la 013 avant que le `dev` ne code.

## Mission

**En une phrase (quoi, et pourquoi) :** Écrire, à partir de la spec (§ 4.3, grammaire fermée) et de l'ADR 0004, sans lire de code, les cas d'acceptation sourcés de l'analyse des phrases dictées (chaque phrase de la grammaire et la commande attendue, nombres de « hauteur » aux bornes, phrases hors grammaire refusées, chaque mot présent dans le vocabulaire de `vosk-model-small-fr-0.22`, modèle à mauvaise empreinte SHA-256 refusé), pour que la mission 013 ait ses tests avant son code.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : format des cas des missions 019 et 020, `fixtures/README.md` ; spec de dictée de Marculus en lecture seule | |
| A-t-il les moyens ? | oui : le modèle se télécharge, son vocabulaire s'en extrait (`graph/words.txt`) | |

**Temps de la main :** **appuyer** : l'agent rend la liste des cas avant de les écrire.

## L'Homme

- **Agent :** `testeur` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** le moteur Vosk, l'interface `MoteurDictee` ; un banc d'essai audio ; aucun code de production.

## Harnais

- **Peut :** créer `fixtures/dictee/` (cas au format JSON, identifiants `DI-NN`, chacun avec sa `source`), compléter `fixtures/README.md`, écrire des tests Kotlin désactivés (`@Disabled("en attente de la mission 013")`) si l'interface attendue se décrit sans lire de code.
- **Ne doit pas :** lire `core/src/main/` ni `app/src/` ; versionner le modèle ; inventer un mot de grammaire ou une commande absente de la spec (il la remonte, avec sa proposition, par exemple la borne haute de « hauteur ») ; toucher à `main`.
- **Sources autorisées :** spec § 4.3 et § 6 ; ADR 0004 ; `docs/referentiels/etats-plants.yaml` (causes) ; Marculus `VoiceGrammar.kt` et sa spec de dictée en lecture seule.

## Tests

- Chaque cas cite sa source. Un mot de la grammaire absent du vocabulaire du modèle est signalé à l'orchestrateur, pas contourné. `.claude/test-command` passe.

## Terrain

- Pascal lit la liste des phrases et dit s'il les prononcerait ainsi en forêt.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Le `dev` de la 013 branche ces cas sans les modifier, et ils passent au vert.
