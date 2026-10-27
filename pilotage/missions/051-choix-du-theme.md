# Mission 051 — Le choix du thème dans Paramètres : Système, Clair, Sombre

> Fiche écrite par l'orchestrateur le 2026-10-27, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Demandée par Pascal le 2026-10-27 :
> « Ajoute dans paramètres le choix du thème comme dans groma et Marculus », puis « Oui je
> confirme ».
> **Lancement :** après la fusion des PR #91 (047) et #92 (048), qui touchent le même écran
> Paramètres ; sous le plafond humain (3 PR en revue).

## Mission

**En une phrase (quoi, et pourquoi) :** Ajouter dans Paramètres le choix du thème, Système, Clair ou Sombre, comme Marculus et Groma, gardé sur le téléphone, pour que Surculus suive le mode du téléphone comme les deux autres applications, ou que le forestier force le clair au soleil.

**Redite par l'agent :** _à remplir par l'agent avant d'agir._

## Vérifié à la source

Constats vérifiés par l'orchestrateur le 2026-10-27.

| Constat | Lu à | Source du constat |
|---|---|---|
| Marculus : `enum class ChoixTheme { SYSTEME, CLAIR, SOMBRE }`, `estSombre(systemeSombre)` ; défaut SYSTEME | `marculus` `core/src/main/kotlin/fr/marculus/core/model/Reglages.kt` l. 29-46 (33a254a) | orchestrateur |
| Marculus : section « Thème », « Suivre le téléphone, ou forcer le thème clair ou sombre. », choix Système / Clair / Sombre | `marculus` `res/values/strings.xml` l. 227-232 ; `ui/parametres/ParametresScreen.kt` l. 451 | orchestrateur |
| Groma : `ChoixTheme` dans `groma.core`, appliqué par `appliquerTheme`, choix dans son écran Paramètres, gardé sous la clé « theme » | `groma` `android/app/src/main/kotlin/groma/app/GromaApp.kt` l. 9, 23 ; `MainActivity.kt` l. 336 ; `ui/EcranParametres.kt` l. 46 (0458303) | orchestrateur |
| Surculus : `SurculusTheme` force le clair (« Thème clair forcé, quel que soit le mode du téléphone (lisibilité au soleil) ») ; ses couleurs sombres existent déjà, recopiées de Marculus | `app/src/main/kotlin/io/github/pobsteta/surculus/ui/theme/Theme.kt` l. 29-38 | orchestrateur |
| Décision du 2026-10-22 « thème clair forcé », inscrite dans l'ADR 0008 | `docs/adr/0008-bibliotheques-communes.md` l. 16, 129, 172, 250 | orchestrateur |

Règles, avec leur auteur :
- **Choix Système / Clair / Sombre dans Paramètres, comme Marculus et Groma** : Pascal, 2026-10-27. Cette décision remplace « thème clair forcé » (2026-10-22) ; l'orchestrateur ajoute un amendement daté à l'ADR 0008 dans la même PR.
- **Défaut « Système », comme Marculus** : lecture par l'orchestrateur du « Oui je confirme » de Pascal (2026-10-27) à sa proposition (a) ; à corriger par Pascal avant le lancement s'il voulait « Clair ».
- **Les couleurs ne changent pas** : clair et sombre restent ceux de Marculus v0.65.0 ; `ThemeTest` (038) reste vert, étendu au schéma sombre s'il ne le couvre pas.
- **La palette d'état et la légende de la carte restent lisibles en sombre** : l'agent vérifie le contraste des pastilles et de la légende sur fond sombre et rend ce qui ne passe pas, sans changer les couleurs d'état lui-même.
- **Styles de texte de Marculus** (042) ; lignes d'au moins 48 dp (`CLAUDE.md`).
- **Garde-fou = test vu rouge** et **bilan des écarts avant rendu** : Pascal, bilan du 2026-10-21.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : modèle Marculus (`ChoixTheme`) | |
| A-t-il les moyens ? | oui | |

**Temps de la main :** **appuyer**.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent. L'orchestrateur ne le remplit pas._

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** changer une couleur du thème ou de la palette d'état ; la légende en noir (bac à sable) ; toucher au plein écran ou à la vue Kanban.

## Harnais

- **Peut :** ajouter `ChoixTheme` (Kotlin pur, testable), son stockage (SharedPreferences, comme « affichage »), la ligne de choix dans `parametres/EcranParametres.kt`, l'application dans `ui/theme/Theme.kt` et `MainActivity.kt`, les chaînes, les tests.
- **Ne doit pas :** toucher à `main` ; modifier `fixtures/` ou `pilotage/journal.md` ; ajouter une dépendance ; modifier l'ADR 0008 (l'orchestrateur l'amende).
- **Sources autorisées :** cette fiche ; `CLAUDE.md` ; `marculus` et `groma` (`~/dev/`, en lecture).

## Tests

- Tests JVM : `ChoixTheme` (Système suit le téléphone, Clair et Sombre forcent) ; défaut Système ; choix relu après écriture ; `ThemeTest` vert pour les deux schémas. **Vu rouge** une fois.
- Commande du projet : `.claude/test-command`, au code 0.
- **Bilan des écarts** : ce qui change à l'écran, contrastes de la palette d'état en sombre.

## Terrain

- **Échantillon que Pascal vérifiera :** la release qui suit la fusion, téléphone en mode sombre : Surculus s'ouvre en sombre comme Marculus ; Paramètres → Thème → Clair le repasse en clair et le choix tient après réouverture ; carte et pastilles lisibles dans les deux thèmes, au soleil.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal. Critère d'arrêt : 3 allers-retours. Point de suivi : à l'ouverture de la PR.

## Comment saura-t-on que ça marche ?

Posé à côté de Marculus, téléphone en sombre, Surculus a le même vert sur le même fond ; et au soleil, un appui dans Paramètres le repasse en clair.
