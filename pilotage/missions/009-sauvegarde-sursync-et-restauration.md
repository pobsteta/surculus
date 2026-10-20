# Mission 009 — Sauvegarde `.sursync` et restauration

> Fiche écrite par l'orchestrateur le 2026-10-15 (`/mission`, porte n° 2), à partir du
> gabarit `imperatrice/skills/mission/fiche-mission.md`. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Écrire la sauvegarde `.sursync` (format v1 publié avec JSON Schema et fixtures, MediaStore `IS_PENDING`, déclenchée par unité de travail et toutes les N saisies ou M minutes, photos écrites dès la prise) et sa restauration sur un autre appareil, pour qu'une tournée survive à la désinstallation et puisse être rouverte ailleurs.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? (sources, conventions nommées ci-dessous) | oui : ADR 0005 | |
| A-t-il les moyens ? (outils, données d'essai, tests) | oui : fixtures `fixtures/sursync/`, deux appareils pour l'essai | |

**Temps de la main :** **appuyer**.

## L'Homme

- **Agent :** `dev`
- **Modèle :** sonnet
- **Ce qui ne lui est pas demandé :** pas les voies hors appareil (010).

## Harnais

- **Peut :** écrire `docs/formats/sursync-v1.md`, le JSON Schema, les fixtures, le code d'écriture et de restauration ; fixer `allowBackup` et `dataExtractionRules`.
- **Ne doit pas :** sortir du périmètre de cette phrase ; inventer un code (travaux, essence, état) ; toucher à `main` ; modifier sommieR, Marculus ou Groma ; trancher une question de sens (la remonter à l'orchestrateur).
- **Sources autorisées :** ADR 0005 ; Marculus `ExportFichier.kt`.

## Tests

- **Comment saura-t-on que c'est juste :** restauration à la ligne près ; version N-1 lue, N+1 refusée ; noms de fichier sans `:` ni nom de propriétaire. Commande du projet : `.claude/test-command`.

## Terrain

- **Échantillon que Pascal vérifiera :** Pascal restaure une tournée sur un second téléphone.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal.
- **Point de suivi :** à la remise du plan (missions tenues) et à l'ouverture de la PR.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

la tournée restaurée montre les mêmes saisies.
