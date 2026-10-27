# Mission 044 — « À propos » et « Plein écran » dans le menu « ⋮ », comme Marculus

> Fiche écrite par l'orchestrateur le 2026-10-27, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Demandée par Pascal le 2026-10-27 :
> « Ajoute un A propos dans le menu pour connaitre la version de surculus installée », puis
> « Ajoute un check plein écran comme groma et marculus ».
> **Lancement :** le 2026-10-27, sur la branche de la 043 (`mission/043-kanban-tournees`, même
> menu « ⋮ ») : la PR de la 044 se fusionne après la #84.

## Mission

**En une phrase (quoi, et pourquoi) :** Ajouter au menu « ⋮ » de la liste des tournées une entrée « À propos » qui affiche la version installée de Surculus, et une case « Plein écran » qui masque les barres système, comme Marculus, pour que Pascal sache quelle version il essaie et que le forestier gagne de la place à l'écran.

**Redite par l'agent :** Ajouter au menu ⋮ des tournées « À propos » (version installée, licences du projet) et une case « Plein écran » (barres système masquées, réglage gardé), pour que Pascal sache quelle version il essaie et que le forestier gagne de la place à l'écran. _(Recopiée par l'orchestrateur depuis le compte rendu du `dev`.)_

## Vérifié à la source

Constats vérifiés par l'orchestrateur le 2026-10-27.

| Constat | Lu à | Source du constat |
|---|---|---|
| Marculus : entrée « À propos » du menu de l'application ; boîte de dialogue « À propos de Marculus » : « Version %1$s » lue par `packageManager.getPackageInfo(...).versionName`, une phrase de description, les licences (Vosk, cartographie, GPL v3), un bouton « Ouvrir sur GitHub » | `marculus` `ui/contextes/ListeContextesScreen.kt` l. 125, 210-211, 420-440 ; `res/values/strings.xml` l. 422-429 (33a254a) | orchestrateur |
| Marculus : réglage « Plein écran » (« Masque les barres de statut et de navigation »), faux par défaut, appliqué dans `MainActivity` par `WindowInsetsControllerCompat.hide(systemBars())` et `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE` (les barres réapparaissent au glissement) ; il est dans l'écran Paramètres de Marculus | `marculus` `MainActivity.kt` l. 51-61 ; `core/.../Reglages.kt:51` ; `ui/parametres/ParametresScreen.kt` l. 178 ; `strings.xml` l. 235-236 | orchestrateur |
| Groma n'a pas de réglage plein écran : seul son écran de capture caméra gère les barres (`CaptureActivity.kt` l. 72-77) | `groma/android/app/src/main/kotlin/groma/app/CaptureActivity.kt` | orchestrateur |
| Surculus : `versionName` vient de la propriété Gradle `appVersionName` (release : par exemple 0.7.41 ; fumée : suffixe `-fumee`) | `app/build.gradle.kts` l. 69-70, 124 | orchestrateur |
| Surculus n'a pas d'écran Paramètres ; le menu « ⋮ » de la liste des tournées porte déjà « Essai GNSS », « Restaurer » et, avec la 043, « Vue Kanban » (case, gardée dans des SharedPreferences) | `tournee/EcranTournees.kt` (branche de la 043) | orchestrateur |
| Surculus est sous GPL v3 | `LICENSE` | orchestrateur |

Règles, avec leur auteur :
- **« À propos » montre la version installée** : Pascal, 2026-10-27.
- **Une case « Plein écran » dans le menu, comme Marculus** : Pascal, 2026-10-27. Surculus n'ayant pas d'écran Paramètres, la case va dans le menu « ⋮ », comme « Vue Kanban » (décision de Pascal pour la 043). Faux par défaut, comme Marculus ; gardée sur le téléphone.
- **Les licences affichées sont celles du projet** : lues dans `LICENSE` et `docs/dependances.md`, jamais inventées. Si une licence n'y est pas écrite, l'agent le dit au lieu de la deviner.
- **L'écran sert le forestier en forêt** : cibles d'au moins 48 dp, texte lisible au soleil (`CLAUDE.md`).
- **Garde-fou = test vu rouge** et **bilan des écarts avant rendu** : Pascal, bilan du 2026-10-21.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : modèle à recopier dans Marculus | |
| A-t-il les moyens ? | oui ; `androidx.core` (`WindowCompat`) est-il déjà là ? s'il manque, il le dit avant de l'ajouter | |

**Temps de la main :** **appuyer**.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent. L'orchestrateur ne le remplit pas._

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :**
  - un écran Paramètres ;
  - toucher à la vue Kanban (043), à la recherche (041), à la navigation (040) ou au thème (038) ;
  - le choix Système / Clair / Sombre (bac à sable).

## Harnais

- **Peut :** modifier `tournee/EcranTournees.kt`, `MainActivity.kt`, `tournee/DepotTournees.kt` (ou un petit dépôt de réglages à côté), les chaînes `strings*.xml` ; écrire en Kotlin pur ce qui se teste sans Android (par exemple le texte de version, le réglage et son défaut).
- **Ne doit pas :** toucher à `main` ; modifier `fixtures/` ou `pilotage/journal.md` ; ajouter une dépendance sans le dire.
- **Sources autorisées :** cette fiche ; `CLAUDE.md` ; `LICENSE` ; `docs/dependances.md` ; `marculus` (`~/dev/`, en lecture).

## Tests

- Tests JVM : plein écran faux par défaut, réglage relu après écriture ; texte de version (« Version 0.7.41 », et la forme de la fumée). **Vu rouge** une fois.
- Commande du projet : `.claude/test-command`, au code 0.
- **Bilan des écarts** : ce qui change à l'écran, écarts avec Marculus.

## Terrain

- **Échantillon que Pascal vérifiera :** sur la release qui suit la fusion (plus de variante fumée : décision de Pascal du 2026-10-27), menu « ⋮ » → « À propos » affiche la version qu'il a installée ; cocher « Plein écran » masque les barres, un glissement depuis le bord les fait réapparaître, la case reste cochée après avoir quitté et rouvert l'application.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours ; au-delà, la décision remonte à Pascal.
- **Point de suivi :** à l'ouverture de la PR.

## Comment saura-t-on que ça marche ?

Pascal ouvre « À propos » et lit la même version que la release qu'il vient d'installer ; en plein écran, la carte et la liste occupent tout l'écran.
