# Mission 002 — Fixtures sourcées et tests d'acceptation du domaine

> Fiche écrite par l'orchestrateur le 2026-10-15 (`/mission`, porte n° 2), à partir du
> gabarit `imperatrice/skills/mission/fiche-mission.md`. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Écrire, à partir de la spec et des ADR et sans lire le code, les fixtures sourcées et les tests d'acceptation des règles de la tranche 1 (§ 6 : totaux, non observé, regarni, 15 m, rattachement par identité, repères, unités d'azimut), pour que le domaine ne s'auto-évalue pas.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? (sources, conventions nommées ci-dessous) | oui : `docs/spec.md` § 3, 4.3, 6 ; ADR 0002, 0003, 0005 ; `docs/referentiels/` | |
| A-t-il les moyens ? (outils, données d'essai, tests) | oui une fois 001 fusionnée (un module `:core` où poser les tests) ; les valeurs de repères sont tirées de pyproj, de Circé (IGN) et du calculateur NOAA, chacune avec sa source | |

**Temps de la main :** **appuyer** : l'agent rend la liste des cas avant d'écrire les tests.

## L'Homme

- **Agent :** `testeur`
- **Modèle :** sonnet
- **Ce qui ne lui est pas demandé :** aucun code de production ; ne lit pas le code des missions 004 et 005.

## Harnais

- **Peut :** écrire `fixtures/regles/*.json`, `fixtures/reperes/*.json` (sourcés), et les tests JUnit correspondants dans `:core` (en échec tant que 004 et 005 ne sont pas faites, marqués comme tels).
- **Ne doit pas :** sortir du périmètre de cette phrase ; inventer un code (travaux, essence, état) ; toucher à `main` ; modifier sommieR, Marculus ou Groma ; trancher une question de sens (la remonter à l'orchestrateur).
- **Sources autorisées :** `docs/spec.md`, ADR 0002, 0003, 0005, `docs/referentiels/` ; pyproj et calculateur NOAA pour produire les valeurs attendues.

## Tests

- **Comment saura-t-on que c'est juste :** chaque règle du § 6 marquée tranche 1 a au moins un cas ; chaque valeur numérique attendue porte sa source ; cas Brest et Strasbourg (convergence, déclinaison), grades hors [0, 400[, changement d'heure du 25 octobre 2026. Commande du projet : `.claude/test-command`.

## Terrain

- **Échantillon que Pascal vérifiera :** Pascal relit trois cas au hasard et retrouve leur source.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal.
- **Point de suivi :** à la remise du plan (missions tenues) et à l'ouverture de la PR.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

les tests existent, échouent pour la bonne raison, et passent quand 004 et 005 sont faites.
