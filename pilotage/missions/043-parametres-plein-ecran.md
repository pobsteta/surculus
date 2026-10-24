# Mission 043 — Un écran Paramètres, avec le plein écran de Marculus

> Fiche écrite par l'orchestrateur le 2026-10-24, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Demandée par Pascal le 2026-10-24 :
> « il n'y a pas l'affichage plein écran dans les paramètres de surculus ? », puis « oui »
> à la proposition de l'orchestrateur (une entrée « Paramètres » dans le menu « ⋮ », un
> premier réglage « Plein écran » repris de Marculus).
> **Lancement :** après la 040 et la 041 (même menu) et sous le plafond humain. Le récepteur
> GNSS dans Paramètres est la mission 044, qui part de celle-ci.

## Mission

**En une phrase (quoi, et pourquoi) :** Ajouter au menu « ⋮ » un écran Paramètres dont le premier réglage, « Plein écran », masque les barres de statut et de navigation comme dans Marculus et reste choisi d'une ouverture à l'autre, pour que la carte et le guidage gagnent la place des barres quand le forestier le veut.

**Redite par l'agent :** _à remplir par l'agent avant d'agir._

## Vérifié à la source

| Constat | Lu à | Source du constat |
|---|---|---|
| Marculus : `LaunchedEffect(reglages.pleinEcran)` ; `WindowInsetsControllerCompat.hide(WindowInsetsCompat.Type.systemBars())`, `systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`, sinon `show(...)` | `marculus`, `app/src/main/kotlin/io/github/pobsteta/marculus/MainActivity.kt` l. 51-59 | orchestrateur |
| Marculus : réglage « Plein écran », « Masque les barres de statut et de navigation. » (`LigneReglage`) | `ui/parametres/ParametresScreen.kt` l. 178 ; `res/values/strings.xml` l. 235-236 | orchestrateur |
| Surculus n'a pas d'écran Paramètres ; la navigation est `EtatNavigation` (040), le menu « ⋮ » est dans `tournee/EcranTournees.kt` (040) | branche `mission/041-recherche-tournees` | orchestrateur |
| Surculus garde déjà des réglages en `SharedPreferences` (« Wi-Fi seulement », `ReglagesOrtho` ; `ConfigRtk`, fichier « surculus ») | `tournee/EcranTournees.kt` ; `gnss/ConfigRtk.kt` | orchestrateur |

Règles, avec leur auteur :
- **Paramètres dans le menu « ⋮ », premier réglage « Plein écran » comme Marculus** : Pascal, 2026-10-24.
- **Désactivé par défaut** : l'heure et la batterie restent visibles tant que le forestier ne choisit pas autrement (proposition de l'orchestrateur, à confirmer à l'essai).
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
- **Ce qui ne lui est pas demandé :** d'autres réglages (anti-veille, vibration, thème, etc.) ; le récepteur GNSS (044) ; toucher à la recherche (041) ou au thème (038).

## Harnais

- **Peut :** ajouter `parametres/EcranParametres.kt`, l'entrée « Paramètres » du menu et sa destination dans `EtatNavigation` (retour vers Tournées) ; un petit stockage des réglages (`SharedPreferences`, comme `ReglagesOrtho`) ; appliquer le plein écran dans `MainActivity` ; les chaînes ; les tests.
- **Ne doit pas :** toucher à `main` ; modifier `fixtures/` ; ajouter une dépendance.
- **Sources autorisées :** cette fiche ; `marculus` (`~/dev/marculus`, en lecture).

## Tests

- `NavigationTest` : Paramètres s'ouvre depuis Tournées, le retour y ramène. Réglage : valeur par défaut faux, lu après écriture. **Vu rouge** une fois.
- Commande du projet : `.claude/test-command`, au code 0.
- **Bilan des écarts.**

## Terrain

- **Échantillon que Pascal vérifiera :** la fumée : « ⋮ » → Paramètres → Plein écran ; les barres disparaissent sur tous les écrans, reviennent au glissement depuis le bord, et le choix tient après avoir fermé l'application.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal. Critère d'arrêt : 3 allers-retours. Point de suivi : à l'ouverture de la PR.

## Comment saura-t-on que ça marche ?

Pascal coche « Plein écran », la carte occupe tout l'écran, et elle s'ouvre encore ainsi le lendemain.
