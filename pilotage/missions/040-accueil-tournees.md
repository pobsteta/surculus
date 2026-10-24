# Mission 040 — L'accueil sur la liste des tournées, menu à trois points

> Fiche écrite par l'orchestrateur le 2026-10-24, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Demandée par Pascal à l'essai de la
> 038 sur le téléphone (2026-10-24) : « Surculus doit s'ouvrir comme marculus et surculus
> sur la liste des tournées à faire. Essai GNSS et restaurer une tournée doit se trouver dans
> un menu hamburger comme dans marculus (3 petits points) ». Mission à part, sur la branche de
> la 038 (Pascal, 2026-10-24 : « Mission 040 à part »).

## Mission

**En une phrase (quoi, et pourquoi) :** Faire s'ouvrir Surculus directement sur la liste des tournées, avec une barre de titre « Surculus » et un menu à trois points (Essai GNSS, Restaurer une sauvegarde) comme l'accueil de Marculus, pour que le forestier arrive tout de suite à son travail et que les trois applications s'ouvrent de la même façon.

**Redite par l'agent :** Faire de la liste des tournées l'écran d'accueil de Surculus, avec une barre « Surculus » et un menu à trois points qui ouvre Essai GNSS et Restaurer une tournée, pour que le forestier arrive tout de suite à son travail comme dans Marculus, sans que le retour Android ne le perde.

## Vérifié à la source

Constats vérifiés par l'orchestrateur le 2026-10-24.

| Constat | Lu à | Source du constat |
|---|---|---|
| Accueil actuel : un titre et trois gros boutons (Tournées, Essai GNSS, Restauration) ; navigation par booléens `rememberSaveable` et `BackHandler` | `app/src/main/kotlin/io/github/pobsteta/surculus/MainActivity.kt` l. 42-85 | orchestrateur |
| L'écran Tournées a un bouton « Retour » et le titre « Tournées » en tête ; signature `EcranTournees(onOuvrir, onRetour)` | `tournee/EcranTournees.kt` l. 64, 108-111 | orchestrateur |
| Marculus : `Scaffold` avec `topBar = BandeauCompact(titre = "Marculus", actions = { … })`, un `IconButton` `Icons.Filled.MoreVert` (description « Menu ») qui ouvre un `DropdownMenu` de `DropdownMenuItem` | `marculus`, `app/src/main/kotlin/io/github/pobsteta/marculus/ui/contextes/ListeContextesScreen.kt` l. 176-215 (tag `v0.65.8`) ; `ui/BandeauCompact.kt` | orchestrateur |
| Surculus ne déclare pas `material-icons-core` (seulement `compose-material3`) | `app/build.gradle.kts` l. 197 | orchestrateur |

Règles, avec leur auteur :
- **L'accueil est la liste des tournées ; Essai GNSS et Restauration passent dans le menu à trois points** : Pascal, 2026-10-24.
- **L'écran sert le forestier en forêt** (gants, soleil, une main libre) : `CLAUDE.md`. Le bouton à trois points garde une cible d'au moins 48 dp, comme le `IconButton` de Material 3.
- **Garde-fou = test vu rouge** et **bilan des écarts avant rendu** : Pascal, bilan du 2026-10-21.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : Compose Material 3, `Scaffold`, `TopAppBar`, `DropdownMenu` ; modèle à lire dans Marculus | |
| A-t-il les moyens ? | oui ; pour l'icône `MoreVert`, soit `material-icons-core` (Apache-2.0, comme Marculus), soit le caractère « ⋮ » : il choisit et le dit | |

**Temps de la main :** **appuyer** : l'agent code puis rend ; Pascal juge sur le téléphone avant la fusion.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent. L'orchestrateur ne le remplit pas._

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :**
  - changer le contenu de la liste des tournées (tri, filtre « à faire », état « rendue » : dette de la 011 au bac à sable) ;
  - ajouter d'autres entrées au menu (Paramètres, À propos, choix du thème) ;
  - toucher aux écrans Carte, Guidage, Essai GNSS ou Restauration autrement que pour leur retour vers la liste ;
  - toucher au thème de la 038.

## Harnais

- **Peut :** réécrire la navigation de `MainActivity.kt` (la liste des tournées à la racine ; le retour Android sur la liste quitte l'application) ; retirer le bouton « Retour » de l'écran Tournées et mettre en tête une barre « Surculus » avec le menu à trois points ; ajouter les chaînes nécessaires à `strings.xml` ; ajouter `material-icons-core` (version portée par la BOM Compose) à `gradle/libs.versions.toml` et à `app/build.gradle.kts`, et la ligne de `docs/dependances.md` et de `NOTICE` qui va avec ; écrire les tests.
- **Ne doit pas :** toucher à `main` ; modifier `fixtures/` ; ajouter une autre dépendance.
- **Sources autorisées :** cette fiche ; `docs/spec.md` (écrans) ; le dépôt `marculus` (`~/dev/marculus`, en lecture) ; la doc de Compose Material 3.

## Tests

- Un test vérifie la navigation, de préférence sans Compose ni Robolectric (par exemple un état de navigation en Kotlin pur : racine = Tournées ; Essai et Restauration ouverts depuis le menu ; retour d'Essai ou de Restauration vers Tournées ; retour de Guidage vers Carte, de Carte vers Tournées ; retour sur Tournées = quitter). **Vu rouge** une fois.
- Commande du projet : `.claude/test-command`, au code 0.
- **Bilan des écarts** : chaque écran dont l'aspect ou le retour change.

## Terrain

- **Échantillon que Pascal vérifiera :** la variante fumée de la branche, installée à côté de Surculus 0.7.25 : l'application s'ouvre sur la liste des tournées ; les trois points ouvrent Essai GNSS et Restaurer ; le retour ramène toujours à la liste ; avec des gants si possible.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours ; au-delà, la décision remonte à Pascal.
- **Point de suivi :** à l'ouverture de la PR.

## Comment saura-t-on que ça marche ?

Pascal ouvre Surculus et voit tout de suite ses tournées, comme dans Marculus ; il trouve Essai GNSS et Restaurer derrière les trois points, et le retour ne le perd jamais.

## Bilan des écarts (agent, 2026-10-24)

| Écran | Aspect | Retour |
|---|---|---|
| Accueil (3 gros boutons) | supprimé | sans objet |
| Tournées | devient l'accueil : barre « Surculus » (fond primaire, 56 dp) + menu « ⋮ » ; titre « Tournées » et bouton « Retour » retirés ; contenu inchangé | retour Android à la racine : quitte l'application |
| Essai GNSS | inchangé (aucune barre) | retour Android vers Tournées |
| Restauration | inchangé (aucune barre) | retour Android vers Tournées |
| Carte, Guidage | inchangés | Guidage vers Carte, Carte vers Tournées (inchangé) |

Icône : caractère « ⋮ » dans un `IconButton` (cible 48 dp), pas de `material-icons-core` : aucune dépendance de plus, ni ligne `dependances.md`/`NOTICE`. Ressources retirées : `tournees_ouvrir`, `tournees_titre`.
