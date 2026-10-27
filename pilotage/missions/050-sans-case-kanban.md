# Mission 050 — Le bouton liste/colonnes toujours visible, sans case « Vue Kanban »

> Fiche écrite par l'orchestrateur le 2026-10-27, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Demandée par Pascal le 2026-10-27 :
> « Tu peux supprimer le menu Kanban car il y a le bouton », puis « Oui je confirme ».
> **Lancement :** après la fusion des PR #91 (047) et #92 (048), qui touchent le même écran
> Paramètres ; sous le plafond humain (3 PR en revue).

## Mission

**En une phrase (quoi, et pourquoi) :** Retirer de Paramètres la case « Vue Kanban » et rendre le bouton de bascule liste/colonnes toujours visible dans la barre de la liste des tournées, pour que le forestier passe d'une vue à l'autre en un appui, sans réglage préalable.

**Redite par l'agent :** _à remplir par l'agent avant d'agir._

## Vérifié à la source

Constats vérifiés par l'orchestrateur le 2026-10-27.

| Constat | Lu à | Source du constat |
|---|---|---|
| La case « Vue Kanban » est dans Paramètres et lit/écrit `reglagesKanban.vueActive` (SharedPreferences « kanban ») | `parametres/EcranParametres.kt` l. 64, 78-80 (046) | orchestrateur |
| La liste des tournées n'affiche le bouton de bascule que si `vueActive` est vrai | `tournee/EcranTournees.kt` l. 112 (043) | orchestrateur |
| Défaut actuel : `VUE_KANBAN_ACTIVE_PAR_DEFAUT = false` | `tournee/EtatKanban.kt` l. 34, 84-85 | orchestrateur |
| Marculus garde un réglage qui active le bouton (choix de Pascal pour la 043, « menu ⋮ et bouton comme marculus ») | `pilotage/journal.md`, 2026-10-27 | orchestrateur |

Règles, avec leur auteur :
- **Plus de case « Vue Kanban » ; le bouton de bascule est toujours visible** : Pascal, 2026-10-27. C'est un écart volontaire avec Marculus.
- **Les statuts des cartes ne bougent pas** : les colonnes choisies à la main (SharedPreferences « kanban ») restent ; seul le réglage d'activation disparaît. La clé `vueActive` devenue inutile n'est plus lue ; l'agent dit s'il la laisse dans le fichier ou la retire, sans toucher aux statuts.
- **La liste reste la vue par défaut à l'ouverture**, comme aujourd'hui (la bascule est gardée le temps de l'écran, `rememberSaveable`) ; la garder d'une ouverture à l'autre est hors mission (voir 049).
- **Styles de texte de Marculus** (042) et cibles d'au moins 48 dp (`CLAUDE.md`).
- **Garde-fou = test vu rouge** et **bilan des écarts avant rendu** : Pascal, bilan du 2026-10-21.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : retirer une ligne et une condition | |
| A-t-il les moyens ? | oui | |

**Temps de la main :** **lâcher** (petit retrait, sans question de sens ouverte) ; la fusion reste soumise à la règle de Pascal (relecture prête, CI verte).

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent. L'orchestrateur ne le remplit pas._

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** changer la vue Kanban elle-même, ses colonnes ou ses statuts ; garder la vue choisie d'une ouverture à l'autre (049) ; toucher au thème (051).

## Harnais

- **Peut :** modifier `parametres/EcranParametres.kt`, `tournee/EcranTournees.kt`, `tournee/EtatKanban.kt`, les chaînes, les tests.
- **Ne doit pas :** toucher à `main` ; modifier `fixtures/` ou `pilotage/journal.md` ; ajouter une dépendance.
- **Sources autorisées :** cette fiche ; `CLAUDE.md`.

## Tests

- Tests JVM : le bouton de bascule ne dépend plus d'aucun réglage ; la liste est la vue d'ouverture ; les statuts gardés restent lus. **Vu rouge** une fois.
- Commande du projet : `.claude/test-command`, au code 0.
- **Bilan des écarts.**

## Terrain

- **Échantillon que Pascal vérifiera :** la release qui suit la fusion : Paramètres n'a plus de case « Vue Kanban » ; la liste des tournées montre l'icône des colonnes dans la barre ; les cartes rangées avant la mise à jour sont restées dans leurs colonnes.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal. Critère d'arrêt : 3 allers-retours. Point de suivi : à l'ouverture de la PR.

## Comment saura-t-on que ça marche ?

Pascal passe de la liste aux colonnes d'un appui dès l'ouverture, sans être passé par Paramètres.
