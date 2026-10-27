# Mission 053 — Le bandeau de Marculus : titre en titleMedium, 48 dp

> Fiche écrite par l'orchestrateur le 2026-10-27, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Signalé par Pascal le 2026-10-27 :
> « ce n'est toujours pas la bonne police ni la bonne couleur dans le bandeau de surculus ».
> Écart à la règle de la 042 (« le style que Marculus donne au même genre de texte »), passé
> au travers du `dev`, du `relecteur` et de l'orchestrateur.
> **Lancement :** le 2026-10-27, depuis `main` (8999f06) ; la #92 (048) est en revue.

## Mission

**En une phrase (quoi, et pourquoi) :** Reprendre dans Surculus le bandeau commun de Marculus (`BandeauCompact` : fond `primary`, titre en `titleMedium`, 48 dp de haut sous la barre d'état) pour la liste des tournées et les écrans à titre (Paramètres, Restauration, Essai GNSS), pour que le haut de l'écran soit le même que dans Marculus.

**Redite par l'agent :** _à remplir par l'agent avant d'agir._

## Vérifié à la source

Constats vérifiés par l'orchestrateur le 2026-10-27.

| Constat | Lu à | Source du constat |
|---|---|---|
| Marculus : `BandeauCompact`, `Surface(color = primary, contentColor = onPrimary)`, `statusBarsPadding()`, `height(48.dp)`, `padding(horizontal = 4.dp)`, titre en `typography.titleMedium` sur une ligne, icône de gauche et actions à droite ; employé sur ses écrans | `marculus` `app/src/main/kotlin/io/github/pobsteta/marculus/ui/BandeauCompact.kt` l. 24-54 (33a254a) | orchestrateur |
| Surculus, liste des tournées : bandeau `height(56.dp)`, titre en `typography.titleLarge` (22 sp) | `app/src/main/kotlin/io/github/pobsteta/surculus/tournee/EcranTournees.kt` l. 311-320 (`main` 8999f06) | orchestrateur |
| Surculus, Paramètres et Restauration : titre d'écran en `titleLarge`, sans bandeau | `parametres/EcranParametres.kt` l. 71 ; `sauvegarde/EcranRestauration.kt` l. 68 | orchestrateur |
| Couleur du bandeau : #3E7A40 dans Surculus, Marculus et Groma, mesurée au pixel sur les captures du téléphone de Pascal (mode sombre) ; l'écart vu tient au fond (Surculus forcé en clair) et relève de la 051 | captures du 2026-10-27, `pilotage/journal.md` | orchestrateur |

Règles, avec leur auteur :
- **Le style que Marculus donne au même genre de texte** (042) : Pascal, 2026-10-24 et 2026-10-27.
- **Le bandeau est un composant commun**, recopié de `BandeauCompact` (nom et forme), utilisé par chaque écran à titre ; les actions existantes (bouton liste/colonnes, menu « ⋮ », flèche de retour) y passent sans changer de comportement.
- **Cibles d'au moins 48 dp** : les icônes du bandeau gardent une cible de 48 dp (`CLAUDE.md`).
- **Pas de changement de couleur** : fond `primary`, texte `onPrimary`, comme aujourd'hui.
- **Garde-fou = test vu rouge** et **bilan des écarts avant rendu** : Pascal, bilan du 2026-10-21.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : recopier `BandeauCompact` | |
| A-t-il les moyens ? | oui | |

**Temps de la main :** **appuyer**.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent. L'orchestrateur ne le remplit pas._

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** le choix du thème (051) ; retirer la case « Vue Kanban » (050) ; toucher aux sections de Paramètres (047, 048) ; changer la carte ou le guidage, qui n'ont pas de bandeau.

## Harnais

- **Peut :** ajouter `ui/BandeauCompact.kt` ; l'employer dans `tournee/EcranTournees.kt`, `parametres/EcranParametres.kt` (seulement le titre et la flèche, pas les sections), `sauvegarde/EcranRestauration.kt`, `essai/EcranEssaiGnss.kt` ; étendre `TypographieTest` ; les tests.
- **Ne doit pas :** toucher à `main` ; modifier `fixtures/` ou `pilotage/journal.md` ; ajouter une dépendance ; changer une couleur.
- **Sources autorisées :** cette fiche ; `CLAUDE.md` ; `marculus` (`~/dev/`, en lecture).

## Tests

- Tests JVM (lecture des sources, comme `TypographieTest`) : un seul bandeau, `titleMedium`, 48 dp ; aucun titre d'écran en `titleLarge` hors bandeau sur les écrans concernés. **Vu rouge** une fois.
- Commande du projet : `.claude/test-command`, au code 0.
- **Bilan des écarts** : ce qui change à l'écran, écarts avec Marculus.

## Terrain

- **Échantillon que Pascal vérifiera :** la release qui suit la fusion, posée à côté de Marculus : même hauteur de bandeau, même taille de titre, sur la liste, Paramètres, Restauration et Essai GNSS.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal. Critère d'arrêt : 3 allers-retours. Point de suivi : à l'ouverture de la PR.

## Comment saura-t-on que ça marche ?

Côte à côte, Surculus et Marculus ont le même bandeau ; Pascal ne voit plus de différence de police en haut de l'écran.
