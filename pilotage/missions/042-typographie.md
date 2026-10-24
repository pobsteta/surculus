# Mission 042 — Les styles et les tailles de texte de Marculus

> Fiche écrite par l'orchestrateur le 2026-10-24, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Demandée par Pascal le 2026-10-24 :
> « utilise la meme police que marculus taille, etc », puis, entre trois issues posées par
> l'orchestrateur (à l'identique, styles aux tailles de Surculus, essai au soleil) :
> « 2, les styles de Marculus et groma aux tailles de Surculus », **puis, le même jour,
> avant tout lancement : « Je veux dans groma et Surculus les tailles de police de
> Marculus »**. Cette fiche suit la seconde décision.
> **Lancement :** après la 040 et la 041 (mêmes fichiers) et sous le plafond humain (3 PR en revue).

## Mission

**En une phrase (quoi, et pourquoi) :** Remplacer chaque taille de texte écrite à la main dans Surculus (`fontSize = …sp`) par le style de `MaterialTheme.typography` que Marculus donne au même genre de texte, avec les tailles par défaut de Material 3 comme Marculus (aucune `Typography` propre), pour que Surculus, Marculus et Groma aient le même texte.

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
- **Les tailles de police de Marculus** : Pascal, 2026-10-24 (« Je veux dans groma et Surculus les tailles de police de Marculus »). Marculus n'a pas de `Typography` propre : ce sont les valeurs par défaut de Material 3 (BOM 2026.02.01), comme pour les couleurs de la 038.
- **Le texte rapetisse, c'est su et assumé** : la plupart des 18-20 sp passent à 12-16 sp. L'orchestrateur l'a dit à Pascal avant sa décision (compromis payé par le forestier en forêt, `CLAUDE.md`, « plier ou tenir ») ; l'essai au soleil et avec des gants le jugera.
- **Le style suit Marculus** : pour chaque genre de texte (titre d'écran, texte d'un bouton, ligne de liste, message, aide), le style que Marculus emploie pour le même genre. Quand Marculus n'a pas d'équivalent (le cap de guidage en 72 sp, les grands chiffres de l'écran d'essai en 44 sp), l'agent ne choisit pas : il le met dans le tableau et Pascal tranche.
- **Garde-fou = test vu rouge** et **bilan des écarts avant rendu** : Pascal, bilan du 2026-10-21.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : `Typography` de Material 3, `MaterialTheme(typography = …)` | |
| A-t-il les moyens ? | oui | |

**Temps de la main :** **appuyer** : avant de toucher aux écrans, l'agent rend à l'orchestrateur le tableau : chaque texte (fichier:ligne), sa taille actuelle, le style de Marculus retenu (avec le fichier:ligne de Marculus qui l'emploie pour le même genre de texte) et sa taille nouvelle ; à part, les textes sans équivalent dans Marculus. Pascal valide le tableau, puis l'agent code.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent. L'orchestrateur ne le remplit pas._

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :**
  - changer une couleur ou une police ;
  - écrire une `Typography` propre à Surculus ;
  - toucher aux couleurs de la 038, à la navigation de la 040 ou à la recherche de la 041 ;
  - ajouter une police embarquée.

## Harnais

- **Peut :** remplacer les `fontSize` (et les `fontWeight` qui vont avec) de `app/src/main/kotlin` par des `style = MaterialTheme.typography.…` ; écrire les tests.
- **Ne doit pas :** toucher à `main` ; modifier `fixtures/` ; ajouter une dépendance.
- **Sources autorisées :** cette fiche ; `marculus` et `groma` (`~/dev/`, en lecture) ; la doc de Compose Material 3.

## Tests

- Un test JVM vérifie que `SurculusTheme` n'a pas de `Typography` propre (celle de `MaterialTheme` vaut `Typography()` de Material 3), comme le `ThemeTest` de la 038 pour les couleurs. **Vu rouge** une fois.
- Un test (ou un `grep` dans la CI) montre qu'il ne reste aucun `fontSize =` dans `app/src/main/kotlin`, sauf les exceptions du tableau validé.
- Commande du projet : `.claude/test-command`, au code 0.
- **Bilan des écarts** : chaque texte dont la taille change, de combien ; les cibles tactiles qui passeraient sous 48 dp (un bouton ne doit pas rapetisser avec son texte).

## Terrain

- **Échantillon que Pascal vérifiera :** la variante fumée, au soleil et avec des gants, à côté de Marculus : même taille de texte, et chaque bouton encore facile à toucher.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours ; au-delà, la décision remonte à Pascal.
- **Point de suivi :** à la remise du tableau, puis à l'ouverture de la PR.

## Comment saura-t-on que ça marche ?

Posés côte à côte, Surculus et Marculus ont le même texte, il ne reste aucune taille écrite à la main, et Pascal lit encore chaque écran au soleil.
