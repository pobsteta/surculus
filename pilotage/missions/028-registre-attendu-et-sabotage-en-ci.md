# Mission 028 — Registre attendu et auto-test par sabotage dans la CI

> Fiche écrite par l'orchestrateur le 2026-10-21, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Décidée par Pascal le 2026-10-21
> (critères F4 et F5). Lancée après la fusion des missions 026 et 027.

## Mission

**En une phrase (quoi, et pourquoi) :** Faire vérifier par la CI d'import que le registre de sommieR, après import de l'export de Surculus, porte exactement les valeurs attendues de `fixtures/export/registre-attendu-couchey.json` (F4), et que chaque contrôle de la CI (F1 à F4) passe au rouge sur une copie sabotée de l'export (F5 : ligne existante modifiée, déclencheur ajouté, total faux), pour qu'un vert prouve que la donnée du forestier arrive juste.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : `autotest_retrait` existant comme modèle, critique du `contradicteur` (journal, 2026-10-21) | |
| A-t-il les moyens ? | oui après les missions 026 et 027 | |

**Temps de la main :** **appuyer** : l'agent rend son plan avant d'écrire.

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** modifier les attentes de la 027 (un écart se remonte, il ne se corrige pas) ; modifier sommieR.

## Harnais

- **Peut :** modifier `.github/workflows/import-sommier.yml`, `ci/sommier/` ; compléter l'ADR 0007 (crible).
- **Ne doit pas :** affaiblir un contrôle existant ; toucher à `main`.

## Tests

- Le job est vert sur l'export réel ; chaque sabotage fait rougir le contrôle visé, et l'auto-test le montre dans le résumé du job.
- Renvoyés par la relecture de la 026 (journal, 2026-10-21) : un test de la tolérance `gpkg_schema` sur `gpkg_data_columns` et `gpkg_data_column_constraints` ; un test du chemin de l'avertissement du témoin « sans » (`::warning`, job vert).

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Le résumé du job sur `main` affiche le registre conforme aux attentes et chaque sabotage détecté.
