# Mission 042 — Les styles de texte de Marculus et Groma, aux tailles de Surculus

> Fiche écrite par l'orchestrateur le 2026-10-24, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Demandée par Pascal le 2026-10-24 :
> « utilise la meme police que marculus taille, etc », puis, entre trois issues posées par
> l'orchestrateur (à l'identique, styles aux tailles de Surculus, essai au soleil) :
> « 2, les styles de Marculus et groma aux tailles de Surculus ».
> **Lancement :** après la 040 et la 041 (mêmes fichiers) et sous le plafond humain (3 PR en revue).

## Mission

**En une phrase (quoi, et pourquoi) :** Remplacer chaque taille de texte écrite à la main dans Surculus (`fontSize = …sp`) par un style de `MaterialTheme.typography`, comme Marculus et Groma, à travers une `Typography` propre à Surculus qui garde les tailles actuelles, pour que les trois applications partagent la même structure de texte sans que le forestier perde un point de lisibilité au soleil.

**Redite par l'agent :** _à remplir par l'agent avant d'agir._

## Vérifié à la source

Constats vérifiés par l'orchestrateur le 2026-10-24.

| Constat | Lu à | Source du constat |
|---|---|---|
| Police : aucune des trois applications ne déclare de `fontFamily` ni de `Typography` : police système du téléphone | `marculus` v0.65.8 `ui/theme/Theme.kt` ; `groma/android/app/src/main/kotlin` ; Surculus `ui/theme/Theme.kt` (038) | orchestrateur |
| Marculus écrit ses textes en styles : `bodySmall` (61), `titleSmall` (23), `bodyMedium` (18), `titleMedium` (16), `labelSmall` (10), `bodyLarge` (6), `labelMedium` (4), `titleLarge` (3) ; 2 `fontSize` seulement | `marculus/app/src/main/kotlin` (décompte `grep`) | orchestrateur |
| Groma de même : `bodySmall` (23), `bodyMedium` (8), `titleLarge` (6), `titleMedium` (5), `bodyLarge` (3), `labelLarge` (2) ; 5 `fontSize` | `groma/android/app/src/main/kotlin` | orchestrateur |
| Surculus écrit ses tailles à la main, aucun style : 18 sp (28), 20 sp (14), 16 sp (12), 14 sp (10), 22 sp (5), 28 sp (3), 24 sp (2), 44 sp (2), et le cap de guidage en 72 sp | `app/src/main/kotlin` (décompte `grep`, `main` avant la 040) ; `docs/adr/0008-bibliotheques-communes.md` l. 71-79 | orchestrateur |

Règles, avec leur auteur :
- **Les styles de Marculus et Groma, aux tailles de Surculus** : Pascal, 2026-10-24.
- **Aucune taille ne change à l'écran** : chaque texte garde sa taille et sa graisse actuelles ; seule l'écriture change. Si deux textes de même rôle ont des tailles différentes, l'agent ne choisit pas : il le met dans le tableau et le rend.
- **Plier ou tenir** : le compromis ne se paie pas sur la lisibilité du forestier (`CLAUDE.md`).
- **Garde-fou = test vu rouge** et **bilan des écarts avant rendu** : Pascal, bilan du 2026-10-21.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : `Typography` de Material 3, `MaterialTheme(typography = …)` | |
| A-t-il les moyens ? | oui | |

**Temps de la main :** **appuyer** : avant de toucher aux écrans, l'agent rend à l'orchestrateur le tableau rôle → taille (sp, graisse) → textes concernés (fichier:ligne), avec les cas où un rôle aurait deux tailles. Pascal valide le tableau, puis l'agent code.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent. L'orchestrateur ne le remplit pas._

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :**
  - changer une taille, une graisse, une couleur ou une police ;
  - toucher aux couleurs de la 038, à la navigation de la 040 ou à la recherche de la 041 ;
  - ajouter une police embarquée.

## Harnais

- **Peut :** ajouter la `Typography` de Surculus dans `ui/theme/` et la passer à `MaterialTheme` dans `SurculusTheme` ; remplacer les `fontSize` (et les `fontWeight` qui vont avec) de `app/src/main/kotlin` par des `style = MaterialTheme.typography.…` ; écrire les tests.
- **Ne doit pas :** toucher à `main` ; modifier `fixtures/` ; ajouter une dépendance.
- **Sources autorisées :** cette fiche ; `marculus` et `groma` (`~/dev/`, en lecture) ; la doc de Compose Material 3.

## Tests

- Un test JVM vérifie, rôle par rôle, que la `Typography` de Surculus vaut les tailles et graisses du tableau validé (valeurs littérales). **Vu rouge** une fois.
- Un test (ou un `grep` dans la CI) montre qu'il ne reste aucun `fontSize =` dans `app/src/main/kotlin`, sauf les exceptions du tableau validé.
- Commande du projet : `.claude/test-command`, au code 0.
- **Bilan des écarts** : chaque texte dont la taille ou la graisse change (attendu : aucun).

## Terrain

- **Échantillon que Pascal vérifiera :** la variante fumée, à côté de la précédente : les écrans doivent être identiques à l'œil.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours ; au-delà, la décision remonte à Pascal.
- **Point de suivi :** à la remise du tableau, puis à l'ouverture de la PR.

## Comment saura-t-on que ça marche ?

Aucun écran ne change à l'œil, il ne reste aucune taille écrite à la main, et une future convergence des tailles avec Marculus et Groma se fera en changeant une seule `Typography`.
