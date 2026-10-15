# Mission 014 — Essai : boutons de volume écran éteint

> Fiche écrite par l'orchestrateur le 2026-10-15 (`/mission`, porte n° 2), à partir du
> gabarit `docs/modeles/fiche-mission.md`. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Mesurer sur deux appareils (Android 14 et 16) si une MediaSession reçoit les boutons de volume écran éteint ou verrouillé, et rendre un rapport chiffré, pour décider si la tranche suivante peut se passer de l'écran allumé.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? (sources, conventions nommées ci-dessous) | oui : ADR 0004 ; documentation Android des boutons média | |
| A-t-il les moyens ? (outils, données d'essai, tests) | **non** : deux appareils Android 14 et 16 non désignés | |

**Temps de la main :** **ne pas lâcher** : Pascal désigne les deux appareils d'abord ; ensuite **tenir** (rapport avant tout code de production).

## L'Homme

- **Agent :** `dev`
- **Modèle :** sonnet
- **Ce qui ne lui est pas demandé :** aucun code de production.

## Harnais

- **Peut :** un prototype jetable sur une branche d'essai.
- **Ne doit pas :** sortir du périmètre de cette phrase ; inventer un code (travaux, essence, état) ; toucher à `main` ; modifier sommieR, Marculus ou Groma ; trancher une question de sens (la remonter à l'orchestrateur).
- **Sources autorisées :** ADR 0004 ; https://developer.android.com/media/legacy/media-buttons

## Tests

- **Comment saura-t-on que c'est juste :** rapport : appareil, version, écran éteint ou verrouillé, touches reçues ou non. Commande du projet : `.claude/test-command`.

## Terrain

- **Échantillon que Pascal vérifiera :** Pascal reproduit l'essai sur un appareil.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal.
- **Point de suivi :** à la remise du plan (missions tenues) et à l'ouverture de la PR.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

un tableau appareil × état de l'écran × touche reçue, que Pascal tranche.
