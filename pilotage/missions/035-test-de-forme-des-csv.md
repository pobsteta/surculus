# Mission 035 — Test de forme des CSV de `docs/referentiels/`

> Fiche écrite par l'orchestrateur le 2026-10-22, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Décidée par Pascal au bilan du
> 2026-10-21 (contre-mesure à la cause 2 du Pareto : « garde-fou = test vu rouge »).

## Mission

**En une phrase (quoi, et pourquoi) :** Ajouter un test qui vérifie que chaque CSV de `docs/referentiels/` est bien formé (même nombre de champs que son en-tête sur chaque ligne de données), et le montrer rouge sur l'état d'avant 22ec874, pour qu'une virgule non protégée ne décale plus jamais une colonne sans que la CI le voie.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Vérifié à la source

Constats vérifiés par l'orchestrateur sur `main` (94f13c5), le 2026-10-22.

| Constat | Lu à | Source du constat |
|---|---|---|
| Huit CSV dans `docs/referentiels/` | `ls docs/referentiels/*.csv` | orchestrateur |
| Les lignes qui commencent par `#` sont des commentaires d'en-tête, pas des données | `docs/referentiels/couts-plantation.csv:1-14` | orchestrateur |
| Avant 22ec874, `couts-plantation.csv` avait 3 lignes de données mal formées | `git show 22ec874^:docs/referentiels/couts-plantation.csv`, lu avec un lecteur CSV | orchestrateur ; journal du 2026-10-21 (contrôle de forme de la 030) |
| `operations-plantation.csv` finit par une ligne vide | `tail -c 200 docs/referentiels/operations-plantation.csv` | orchestrateur |
| Les tests de `:core` lisent déjà `docs/referentiels/`, et Gradle déclare ce dossier comme entrée | `core/build.gradle.kts:33` ; `core/src/test/kotlin/io/github/pobsteta/surculus/core/fixtures/Fixtures.kt:48` (`object Referentiels`) | orchestrateur |

Règles, avec leur auteur :
- **Garde-fou = test vu rouge.** Décision de Pascal au bilan du 2026-10-21.
- **Bilan des écarts avant rendu.** Décision de Pascal au bilan du 2026-10-21.
- **Pas de test sans le lire rouge une fois.** Acquis des missions 022 et 026 (journal).

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : tests de `:core` qui lisent déjà les référentiels (`Fixtures.kt`) | |
| A-t-il les moyens ? | oui : tout est sur `main` | |

**Temps de la main :** **appuyer** : l'agent rend son plan avant d'écrire. Le plan dit :
- comment le test lit un CSV : guillemets, virgules dans les champs, lignes `#`, lignes vides ;
- s'il tolère la ligne vide finale de `operations-plantation.csv` ou s'il la signale ;
- où il range le test.

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :**
  - corriger le contenu d'un référentiel : si le test trouve un défaut sur `main`, il le signale à l'orchestrateur, qui le confie à l'`acquereur` ;
  - contrôler le sens des valeurs.

## Harnais

- **Peut :** ajouter un test dans `core/src/test/` ; ajouter une phrase à `docs/referentiels/README.md` qui renvoie au test.
- **Ne doit pas :**
  - modifier un CSV de `docs/referentiels/` ;
  - ajouter une dépendance sans le dire au plan ;
  - casser un test existant ;
  - toucher à `main` ;
  - trancher une question de sens (la remonter à l'orchestrateur).
- **Sources autorisées :** les CSV de `docs/referentiels/` ; RFC 4180 (format CSV).

## Tests

- Le test est vert sur `main`.
- **Vu rouge :** sur une copie des CSV de `22ec874^` (par exemple `couts-plantation.csv`), il échoue en nommant le fichier et la ligne. Le rapport montre la sortie rouge. Ce sabotage n'est pas commité, ou il est commité puis retiré, comme pour la 026.
- Commande du projet : `.claude/test-command`.
- **Bilan des écarts** : le rapport liste chaque fichier modifié.

## Terrain

- **Échantillon que Pascal vérifiera :** la sortie rouge du test sur `22ec874^`, qui doit nommer `couts-plantation.csv` et ses lignes fautives.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.
- **Point de suivi :** à la remise du plan, puis à l'ouverture de la PR.

## Comment saura-t-on que ça marche ?

Un CSV mal formé ajouté à `docs/referentiels/` fait échouer la CI, et le message nomme le fichier et la ligne.
