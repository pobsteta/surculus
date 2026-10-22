# Mission 028 — Registre attendu et auto-test par sabotage dans la CI

> Fiche écrite par l'orchestrateur le 2026-10-21, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Décidée par Pascal le 2026-10-21
> (critères F4 et F5). Lancée après la fusion des missions 026 et 027.

## Mission

**En une phrase (quoi, et pourquoi) :** Faire vérifier par la CI d'import que le registre de sommieR, après import de l'export de Surculus, porte exactement les valeurs attendues de `fixtures/export/registre-attendu-couchey.json` (F4), et que chaque contrôle de la CI (F1 à F4) passe au rouge sur une copie sabotée de l'export (F5 : ligne existante modifiée, déclencheur ajouté, total faux), pour qu'un vert prouve que la donnée du forestier arrive juste.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Vérifié à la source

Constats vérifiés par l'orchestrateur sur `main` (162fb8c), le 2026-10-22.

| Constat | Lu à | Source du constat |
|---|---|---|
| Les attentes F4 existent, écrites par le testeur (mission 027) | `fixtures/export/registre-attendu-couchey.json` ; `fixtures/README.md:158` | orchestrateur |
| Aucune étape de la CI ne lit encore ces attentes | `grep -rn registre-attendu ci .github` : aucun résultat | orchestrateur |
| Le modèle d'auto-test existe : `autotest_retrait`, appelé par le script de la CI | `ci/sommier/fonctions.R:171` ; `ci/sommier/verifier.R:113` | orchestrateur |
| L'avertissement du témoin « sans » passe par `::warning` | `ci/sommier/verifier.R:194` | orchestrateur |
| La tolérance de l'extension Schema porte sur deux tables | `ci/sommier/criteres.R:24` | orchestrateur |
| Le workflow d'import fait 162 lignes | `.github/workflows/import-sommier.yml` | orchestrateur |
| Les tests R de la CI sont dans `ci/sommier/test-criteres.R` | `ls ci/sommier/` | orchestrateur |

Règles, avec leur auteur :
- **F1 à F5 remplacent le critère (a)** ; F4 et F5 vont aux missions 027 (testeur) et 028 (dev). Décision de Pascal, journal du 2026-10-21.
- **Garde-fou = test vu rouge** et **bilan des écarts avant rendu**. Décisions de Pascal au bilan du 2026-10-21.
- **Un écart avec les attentes de la 027 se remonte, il ne se corrige pas.** Fiche 028 d'origine (orchestrateur, 2026-10-21), dans la ligne de la règle du testeur (CLAUDE.md).

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

- **Bilan des écarts** : le rapport liste chaque fichier modifié, et chaque assertion ou fixture changée, avec la raison.

## Terrain

- **Échantillon que Pascal vérifiera :** le résumé du job sur la PR : registre conforme ligne à ligne, puis un sabotage par contrôle (F1 à F4), chacun au rouge avec son message.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.
- **Point de suivi :** à la remise du plan, puis à l'ouverture de la PR.

## Comment saura-t-on que ça marche ?

Le résumé du job sur `main` affiche le registre conforme aux attentes et chaque sabotage détecté.
