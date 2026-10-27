# Mission 046 — Un écran Paramètres, comme Marculus

> Fiche écrite par l'orchestrateur le 2026-10-24 (numérotée 043 sur la branche non fusionnée
> `mission/043-parametres`, f947899), renumérotée et réécrite le 2026-10-27 : les numéros 043
> à 045 sont pris par les missions fusionnées ce jour-là. Demandée par Pascal le 2026-10-24
> (« il n'y a pas l'affichage plein écran dans les paramètres de surculus ? », puis « oui »),
> puis le 2026-10-27 : « Ajoute un menu Paramètres, comme dans marculus qui regroupe essai
> GNSS et plein écran », et « 3. oui » à la proposition d'y ranger aussi « Vue Kanban » et
> « Restaurer » comme Marculus.
> **Lancement :** après la 042 (mêmes écrans) et sous le plafond humain (3 PR en revue). Le
> récepteur GNSS dans Paramètres est la mission 047, la voix Vosk la 048 ; elles partent de
> celle-ci.

## Mission

**En une phrase (quoi, et pourquoi) :** Ajouter au menu « ⋮ » un écran Paramètres, comme Marculus, qui regroupe « Plein écran », « Vue Kanban », l'accès à l'Essai GNSS et « Restaurer », et ne laisser au menu « ⋮ » que « Paramètres » et « À propos », pour que les réglages soient au même endroit que dans Marculus et que le menu reste court.

**Redite par l'agent :** _à remplir par l'agent avant d'agir._

## Vérifié à la source

| Constat | Lu à | Source du constat |
|---|---|---|
| Marculus : `LaunchedEffect(reglages.pleinEcran)` ; `WindowInsetsControllerCompat.hide(WindowInsetsCompat.Type.systemBars())`, `systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`, sinon `show(...)` | `marculus`, `app/src/main/kotlin/io/github/pobsteta/marculus/MainActivity.kt` l. 51-59 | orchestrateur |
| Marculus : réglage « Plein écran », « Masque les barres de statut et de navigation. » (`LigneReglage`) | `ui/parametres/ParametresScreen.kt` l. 178 ; `res/values/strings.xml` l. 235-236 | orchestrateur |
| Surculus n'a pas d'écran Paramètres ; la navigation est `EtatNavigation` (040), le menu « ⋮ » est dans `tournee/EcranTournees.kt` et porte, au 2026-10-27, « Vue Kanban » (043), « Plein écran » (044), « Essai GNSS », « Restaurer » et « À propos » (044, 045) | `main` à a0186c7 | orchestrateur |
| Marculus range dans ses Paramètres, entre autres, « Plein écran » (l. 178), « Vue Kanban des contextes » (l. 227) et « Restaurer » (l. 395) | `marculus` `ui/parametres/ParametresScreen.kt` (33a254a) | orchestrateur |
| Le plein écran et la vue Kanban existent déjà, gardés en SharedPreferences (« affichage », « kanban ») ; la mission les **déplace** dans Paramètres sans changer leur logique ni leur défaut | `tournee/ReglagesAffichage.kt`, `tournee/EtatKanban.kt`, `tournee/DepotTournees.kt` | orchestrateur |
| Surculus garde déjà des réglages en `SharedPreferences` (« Wi-Fi seulement », `ReglagesOrtho` ; `ConfigRtk`, fichier « surculus ») | `tournee/EcranTournees.kt` ; `gnss/ConfigRtk.kt` | orchestrateur |

Règles, avec leur auteur :
- **Paramètres dans le menu « ⋮ », comme Marculus** : Pascal, 2026-10-24 et 2026-10-27.
- **Paramètres regroupe Plein écran, Vue Kanban, Essai GNSS et Restaurer ; le menu « ⋮ » ne garde que Paramètres et À propos** : Pascal, 2026-10-27 (« 3. oui »).
- **Aucun réglage ne change de défaut ni de stockage** : plein écran et vue Kanban restent faux par défaut, dans leurs SharedPreferences actuelles.
- **Styles de texte de la 042** (tailles de Marculus) pour le nouvel écran.
- **L'écran sert le forestier en forêt** : lignes de réglage d'au moins 48 dp, une case par ligne, retour par le bouton « Retour » et le geste système (`CLAUDE.md`).
- **Garde-fou = test vu rouge** et **bilan des écarts avant rendu** : Pascal, bilan du 2026-10-21.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : modèle Marculus | |
| A-t-il les moyens ? | oui ; `androidx.core` est déjà là (à vérifier, sinon il le dit) | |

**Temps de la main :** **appuyer**.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent. L'orchestrateur ne le remplit pas._

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** d'autres réglages (anti-veille, vibration, thème, etc.) ; le récepteur GNSS (047) ; la voix Vosk (048) ; changer la logique de la vue Kanban (043) ou du plein écran (044) ; toucher à la recherche (041) ou au thème (038).

## Harnais

- **Peut :** ajouter `parametres/EcranParametres.kt`, l'entrée « Paramètres » du menu et sa destination dans `EtatNavigation` (retour vers Tournées) ; y déplacer les cases « Plein écran » et « Vue Kanban » et les entrées « Essai GNSS » et « Restaurer » (qui ouvrent leurs écrans actuels) ; les retirer du menu « ⋮ » ; les chaînes ; les tests.
- **Ne doit pas :** toucher à `main` ; modifier `fixtures/` ; ajouter une dépendance.
- **Sources autorisées :** cette fiche ; `marculus` (`~/dev/marculus`, en lecture).

## Tests

- `NavigationTest` : Paramètres s'ouvre depuis Tournées, le retour y ramène. Paramètres ouvre l'Essai GNSS et la Restauration, le retour revient à Paramètres. Menu « ⋮ » : seulement Paramètres et À propos. **Vu rouge** une fois.
- Commande du projet : `.claude/test-command`, au code 0.
- **Bilan des écarts.**

## Terrain

- **Échantillon que Pascal vérifiera :** la release qui suit la fusion (plus de variante fumée) : « ⋮ » ne montre que Paramètres et À propos ; dans Paramètres, Plein écran et Vue Kanban gardent leur état d'avant la mise à jour ; Essai GNSS et Restaurer s'ouvrent et le retour ramène à Paramètres.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal. Critère d'arrêt : 3 allers-retours. Point de suivi : à l'ouverture de la PR.

## Comment saura-t-on que ça marche ?

Pascal retrouve tous les réglages dans Paramètres, à la même place que dans Marculus, et le menu « ⋮ » n'a plus que deux entrées.
