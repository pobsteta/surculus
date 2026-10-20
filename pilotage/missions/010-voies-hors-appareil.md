# Mission 010 — Voies de sauvegarde hors appareil

> Fiche écrite par l'orchestrateur le 2026-10-15 (`/mission`, porte n° 2), à partir du
> gabarit `imperatrice/skills/mission/fiche-mission.md`. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Faire sortir automatiquement chaque `.sursync` de l'appareil par trois voies (dossier choisi une fois sur carte SD ou clé USB, téléphone du binôme apparié, envoi WebDAV quand le réseau passe) et signaler sans bloquer, à l'ouverture d'une journée, les saisies jamais sorties, pour tenir la promesse du brief.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? (sources, conventions nommées ci-dessous) | oui : ADR 0005 | |
| A-t-il les moyens ? (outils, données d'essai, tests) | **non** : pas de serveur WebDAV d'essai, pas de second appareil désigné | |

**Temps de la main :** **ne pas lâcher** : moyens à fournir d'abord (serveur WebDAV d'essai, second appareil).

## L'Homme

- **Agent :** `dev`
- **Modèle :** sonnet
- **Ce qui ne lui est pas demandé :** —

## Harnais

- **Peut :** —
- **Ne doit pas :** sortir du périmètre de cette phrase ; inventer un code (travaux, essence, état) ; toucher à `main` ; modifier sommieR, Marculus ou Groma ; trancher une question de sens (la remonter à l'orchestrateur).
- **Sources autorisées :** ADR 0005.

## Tests

- **Comment saura-t-on que c'est juste :** chaque envoi réussi est inscrit au journal ; compteur de saisies non sorties. Commande du projet : `.claude/test-command`.

## Terrain

- **Échantillon que Pascal vérifiera :** Pascal perd volontairement un téléphone (le met de côté) et retrouve la journée sur l'autre.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal.
- **Point de suivi :** à la remise du plan (missions tenues) et à l'ouverture de la PR.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

la journée se retrouve hors du téléphone d'origine.
