# Mission 007 — CI : import sommieR rejoué sur l'export

> Fiche écrite par l'orchestrateur le 2026-10-15 (`/mission`, porte n° 2), à partir du
> gabarit `docs/modeles/fiche-mission.md`. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Ajouter à la CI un travail qui rejoue `sommier_importer_qfield_travaux()` (sommieR 0.31.1, PostGIS) sur l'export décompressé de la démo Couchey, avec et sans les couches ajoutées, pour vérifier automatiquement le premier critère de la tranche 1.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? (sources, conventions nommées ci-dessous) | oui : ADR 0007 (critère de CI) ; sommieR public (`pobsteta/sommieR`), `tests/testthat/test-travaux-qfield-db.R` | |
| A-t-il les moyens ? (outils, données d'essai, tests) | oui une fois 006 fusionnée | |

**Temps de la main :** **appuyer**.

## L'Homme

- **Agent :** `dev`
- **Modèle :** sonnet
- **Ce qui ne lui est pas demandé :** ne modifie pas sommieR.

## Harnais

- **Peut :** ajouter un service PostgreSQL/PostGIS et R à la CI, installer sommieR 0.31.1 depuis GitHub, écrire un script R de comparaison des bilans.
- **Ne doit pas :** sortir du périmètre de cette phrase ; inventer un code (travaux, essence, état) ; toucher à `main` ; modifier sommieR, Marculus ou Groma ; trancher une question de sens (la remonter à l'orchestrateur).
- **Sources autorisées :** ADR 0007 ; sommieR en lecture seule.

## Tests

- **Comment saura-t-on que c'est juste :** le travail CI échoue si le bilan diffère avec et sans couches ajoutées, ou si le schéma de sommieR change. Commande du projet : `.claude/test-command`.

## Terrain

- **Échantillon que Pascal vérifiera :** —

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal.
- **Point de suivi :** à la remise du plan (missions tenues) et à l'ouverture de la PR.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

la CI de la PR montre le travail « import sommieR » vert.
