# Mission 043 — Vue Kanban des tournées, colonnes de Marculus

> Fiche écrite par l'orchestrateur le 2026-10-27, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Demandée par Pascal le 2026-10-27,
> après le constat que Marculus a une vue Kanban et Groma non : « idem de marculus pour les
> colonnes kanban ».
> **Lancement :** après la fusion de la #83 (041, mêmes fichiers) et la réponse de Pascal
> aux questions ouvertes ci-dessous. Sous le plafond humain (3 PR en revue).

## Mission

**En une phrase (quoi, et pourquoi) :** Ajouter à la liste des tournées une vue Kanban à cinq colonnes, celles de Marculus (Proposée, Validée, Planifiée, Réalisée, Abandonnée), avec glisser-déposer des cartes et un statut minimal imposé par l'avancement, pour que le forestier voie d'un coup d'œil où en est chacune de ses tournées, comme dans Marculus.

**Redite par l'agent :** _à remplir par l'agent avant d'agir._

## Vérifié à la source

Constats vérifiés par l'orchestrateur le 2026-10-27.

| Constat | Lu à | Source du constat |
|---|---|---|
| Marculus : `enum class EtatKanban { PROPOSEE, VALIDEE, PLANIFIEE, REALISEE, ABANDONNEE }` | `marculus` `core/src/main/kotlin/fr/marculus/core/model/Modeles.kt:33` (33a254a) | orchestrateur |
| Marculus : libellés « Proposée », « Validée », « Planifiée », « Réalisée », « Abandonnée » ; réglage « Vue Kanban des contextes » dans les Paramètres, désactivé par défaut ; bouton de la barre pour passer de la liste aux colonnes | `marculus` `app/src/main/res/values/strings.xml` l. 263-274 ; `ui/contextes/ListeContextesScreen.kt` l. 109, 130, 181-185 | orchestrateur |
| Marculus : glisser-déposer d'une carte entre colonnes | `ListeContextesScreen.kt` l. 131, 320-344 | orchestrateur |
| Marculus : statut minimal, « tiges → Planifiée ; export → Réalisée » ; colonne affichée = au moins ce minimum, même si le statut stocké est inférieur | `ListeContextesScreen.kt` l. 591-600 ; `data/.../MartelageRepository.kt` l. 85-91, 298 | orchestrateur |
| Marculus : le statut est stocké dans sa base locale (Room, `Entities.kt:27`, `statut = "PROPOSEE"`) | `marculus` `data/src/main/kotlin/io/github/pobsteta/marculus/data/db/Entities.kt:27` | orchestrateur |
| Groma n'a pas de vue Kanban (le mot n'apparaît que dans son `pilotage/`) | `groma`, recherche de « kanban » dans `*.kt` et `*.xml` | orchestrateur |
| Surculus n'a ni base locale de ce genre ni écran Paramètres ; sa mémoire est le journal en ajout seul (ADR 0005, spec « Journal des saisies ») ; l'état « rendue » n'existe pas encore (`LigneTournee.rendue` faux, 041) | `docs/spec.md` l. 350 et suiv. ; `tournee/DepotTournees.kt` (041) | orchestrateur |

Règles, avec leur auteur :
- **Les colonnes de Marculus** : Pascal, 2026-10-27.
- **On n'invente pas la marque « rendue »** (041) : le minimum « Réalisée » viendra avec l'export.
- **L'écran sert le forestier en forêt** : cibles d'au moins 48 dp, texte lisible au soleil, une main, des gants (`CLAUDE.md`).
- **Garde-fou = test vu rouge** et **bilan des écarts avant rendu** : Pascal, bilan du 2026-10-21.

## Questions ouvertes (à Pascal, avant le lancement)

1. **Où vit le statut choisi à la main ?** (a) sur le téléphone seulement, comme Marculus (perdu à la restauration d'un `.sursync` sur un autre téléphone) ; (b) dans le journal, par un type de ligne nouveau (voyage avec la sauvegarde, mais change le format du journal : spec et ADR 0005).
2. **Minimum imposé par l'avancement** : transposer Marculus en « au moins une saisie → Planifiée ; rendue → Réalisée » ?
3. **Activation** : Surculus n'a pas d'écran Paramètres. Bouton de bascule dans la barre, toujours visible, ou entrée « Vue Kanban » dans le menu « ⋮ » ?

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : modèle à recopier dans Marculus | |
| A-t-il les moyens ? | oui, une fois les trois questions tranchées ; les icônes de Marculus (`ViewColumn`, `ViewList`) demanderaient une dépendance d'icônes : il le dit sans l'ajouter | |

**Temps de la main :** **appuyer**.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent. L'orchestrateur ne le remplit pas._

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :**
  - écrire la marque « rendue » ou l'export ;
  - toucher à la recherche et à la puce de la 041, sauf pour les masquer en vue Kanban comme Marculus ;
  - toucher à la navigation de la 040 ou au thème de la 038.

## Harnais

- **Peut :** modifier `tournee/EcranTournees.kt` et `tournee/DepotTournees.kt` ; ajouter les chaînes à `strings_tournees.xml` ; écrire la logique des colonnes (statut, minimum, colonne affichée) en Kotlin pur, testable sans Android.
- **Ne doit pas :** toucher à `main` ; modifier `fixtures/` ; ajouter une dépendance ; changer le format du journal sans la réponse (b) de Pascal à la question 1.
- **Sources autorisées :** cette fiche ; `docs/spec.md` § 4.1 ; `marculus` (`~/dev/`, en lecture).

## Tests

- Tests JVM : colonne affichée = max(statut stocké, minimum) ; une carte déposée dans une colonne inférieure au minimum reste au minimum ; libellés des cinq colonnes ; recherche vide = toutes les tournées réparties. **Vu rouge** une fois.
- Commande du projet : `.claude/test-command`, au code 0.
- **Bilan des écarts** : ce qui change à l'écran, et tout écart avec Marculus.

## Terrain

- **Échantillon que Pascal vérifiera :** la variante fumée, avec deux tournées au moins : passer en vue Kanban, glisser une carte de Proposée à Validée avec des gants si possible, quitter et rouvrir l'application, la carte est restée.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours ; au-delà, la décision remonte à Pascal.
- **Point de suivi :** à l'ouverture de la PR.

## Comment saura-t-on que ça marche ?

Pascal ouvre la vue Kanban et retrouve ses tournées rangées comme ses contextes dans Marculus ; une carte glissée reste où il l'a mise, et ne peut pas descendre sous ce que l'avancement impose.
