# Mission 056 — Le choix du thème en liste déroulante, comme Marculus

> Fiche écrite par l'orchestrateur le 2026-10-27, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Demandée par Pascal le 2026-10-27, après
> l'essai de la v0.7.55 : « le thème doit être dans une liste déroulante comme marculus pas par
> check ».
> **Lancement :** le 2026-10-27, depuis `main` (90ee48b, mission 051 fusionnée).

## Mission

**En une phrase (quoi, et pourquoi) :** Remplacer les trois lignes à bouton radio du thème dans Paramètres par la ligne à liste déroulante de Marculus (titre, description, valeur actuelle et flèche ▾, menu Système / Clair / Sombre avec une coche sur le choix actuel), pour que Paramètres ressemble à celui de Marculus.

**Redite par l'agent :** remplacer, dans Paramètres, les trois lignes radio du thème par la liste déroulante de Marculus (titre, description, valeur ▾, menu Système / Clair / Sombre avec coche), pour que Paramètres ressemble à celui de Marculus.

## Vérifié à la source

| Constat | Lu à | Source du constat |
|---|---|---|
| Marculus : `ChoixThemeReglage` : `Column` avec titre `titleMedium`, description `bodySmall`, puis une `Row` cliquable (`Role.DropdownList`, 48 dp) avec le libellé actuel (`bodyLarge`) et `Icons.Default.ArrowDropDown` ; `DropdownMenu` avec un `DropdownMenuItem` par choix (48 dp), `Icons.Default.Check` en `trailingIcon` sur le choix actuel (`param_theme_choix_actuel`) | `marculus` `app/src/main/kotlin/io/github/pobsteta/marculus/ui/parametres/ParametresScreen.kt` l. 440-480 (33a254a) | orchestrateur |
| Surculus : `LigneTheme` en trois lignes radio | `app/src/main/kotlin/io/github/pobsteta/surculus/parametres/EcranParametres.kt` (`main` 90ee48b) | orchestrateur |

Règles, avec leur auteur :
- **Liste déroulante comme Marculus, pas de boutons radio** : Pascal, 2026-10-27.
- **Rien ne change dans le choix lui-même** : `ChoixTheme`, défaut Système, stockage (SharedPreferences « affichage », clé « theme »), application immédiate.
- **Styles de Marculus** (042), cibles d'au moins 48 dp ; `material-icons-extended` est déjà là (043).
- **Garde-fou = test vu rouge** et **bilan des écarts avant rendu** : Pascal, bilan du 2026-10-21.

**Temps de la main :** **lâcher** (recopie d'un composant de Marculus, sans question ouverte) ; fusion soumise à la règle de Pascal (relecture prête, CI verte).

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent. L'orchestrateur ne le remplit pas._

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** les autres lignes de Paramètres ; la carte (055, PR #97 en revue).

## Harnais

- **Peut :** modifier `parametres/EcranParametres.kt` (la ligne du thème seulement), les chaînes du thème, les tests.
- **Ne doit pas :** toucher à `main` ; modifier `fixtures/` ou `pilotage/journal.md` ; ajouter une dépendance.

## Tests

- Lecture des sources : un `DropdownMenu` pour le thème, aucun `RadioButton` ; `ChoixThemeTest` inchangé et vert. **Vu rouge** une fois.
- Commande du projet : `.claude/test-command`, au code 0.

## Terrain

- **Échantillon que Pascal vérifiera :** la release qui suit la fusion : Paramètres → Thème montre « Système ▾ » ; un appui ouvre la liste, une coche est sur le choix actuel ; Clair repasse Surculus en clair.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal. Critère d'arrêt : 3 allers-retours.

## Comment saura-t-on que ça marche ?

Côte à côte, la ligne « Thème » de Surculus et celle de Marculus sont les mêmes.
