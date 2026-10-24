# Mission 038 — Thème commun par `terrain-style`

> Fiche écrite par l'orchestrateur le 2026-10-24, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Décidée par Pascal : ADR 0008
> (acceptée le 2026-10-23, amendée le 2026-10-24 : verrou des 7 jours levé, « fais sauter
> le verrou des 7 jours et continue »). **Lancement : après l'essai au soleil de Couchey**
> (ADR 0008, « Trois points confirmés ensuite »), sauf décision contraire de Pascal.

## Mission

**En une phrase (quoi, et pourquoi) :** Donner à Surculus le schéma clair de `terrain-style` (épinglé à `d840329`), recopié 1:1 dans un `ColorScheme` en thème clair forcé, écrire la légende de la carte en noir à côté de sa pastille et faire passer chaque texte de la palette d'état au seuil de WCAG 2.2, 1.4.6 (7:1, 4,5:1 pour le grand texte), pour que les trois applications de nemeton aient le même style sans jamais perdre la lisibilité au soleil.

**Redite par l'agent :** _à remplir par l'agent avant d'agir._

## Vérifié à la source

Constats vérifiés par l'orchestrateur sur `main` (5b4e3dc) et sur `terrain` (`d840329`), le 2026-10-24.

| Constat | Lu à | Source du constat |
|---|---|---|
| `terrain-style` v0.6.0 : `StyleTerrain.clair` / `.sombre` (`Schema`, tous les rôles en ARGB `Long`), `Contraste` (rapport WCAG 2.2), couleurs sémantiques `reussi` et `alerte` (`Schema.kt` l. 65-66, type `Semantique`) ; rôles interdits en texte listés en tête de `StyleTerrain.kt` | `terrain/terrain-style/src/main/kotlin/fr/nemeton/terrain/style/` (`StyleTerrain.kt` l. 1-22, `Semantique.kt`, `Contraste.kt`, `Schema.kt`) | orchestrateur |
| Coordonnée JitPack : `com.github.pobsteta.terrain:terrain-style:<sha court>` ; dépôt `https://jitpack.io` filtré par `includeGroup("com.github.pobsteta.terrain")` | `terrain/README.md` l. 27-46 (dépôt, l. 34) et 163-166 (SHA court) | orchestrateur |
| JitPack a construit `d840329` (`build.log`, `terrain-style-d840329.pom` : HTTP 200) | `https://jitpack.io/com/github/pobsteta/terrain/d840329/build.log` | orchestrateur |
| Surculus n'a pas de thème : `MaterialTheme { … }` sans `colorScheme` | `app/src/main/kotlin/io/github/pobsteta/surculus/MainActivity.kt:42` | orchestrateur |
| La palette d'état : `Palette` (vert `#0B6B2E`, orange `#B45300`, rouge `#B00020`, noir, blanc), recopiée dans l'écran d'essai | `app/src/main/kotlin/io/github/pobsteta/surculus/carte/Palette.kt:15-38` ; `app/src/main/kotlin/io/github/pobsteta/surculus/essai/EcranEssaiGnss.kt:73-77` | orchestrateur |
| Textes en couleur d'état : légende de la carte, anomalies, refus, hors France, badge du fix (blanc sur la teinte), guidage, ortho | `carte/EcranCarte.kt:86, 196, 198, 279-287, 298` ; `carte/Commun.kt:71-74` ; `carte/EcranGuidage.kt:70, 76, 150-162, 204, 206, 216-218` ; `tournee/EcranTournees.kt:125, 159-176` ; écran d'essai : `essai/EcranEssaiGnss.kt:162-163, 170, 180, 267-279` (blanc sur la teinte du fix) (sous `app/src/main/kotlin/io/github/pobsteta/surculus/`) | orchestrateur |
| Contrastes déjà mesurés : orange 4,78:1 en texte, blanc sur orange 5,03:1 | `docs/adr/0008-bibliotheques-communes.md`, « La palette d'état » | orchestrateur (ADR) |
| **Valeur provisoire** : `reussi` (`#0F8558`) peut changer en v0.6.1 après l'essai au soleil de `terrain` du 2026-10-30 (repli `#00843D`) | `terrain/README.md` l. 169-171 ; `terrain/docs/adr/0002-couleurs-semantiques.md` | orchestrateur (relecture de la PR #74) |
| Dépôts des dépendances du projet : `google()` et `mavenCentral()` seulement | `settings.gradle.kts:15-21` | orchestrateur |

Règles, avec leur auteur :
- **ADR 0008**, décisions 1, 2, 4, 5 et 6, « Trois points confirmés ensuite » et amendements du 2026-10-24 : Pascal.
- **Thème clair forcé**, pas de mode sombre, pas de couleur dynamique : Pascal, 2026-10-22 (ADR 0008, valeurs sacrifiées).
- **Une couleur qui échoue à 7:1 ne se retouche jamais à la main** : la mission rend la liste, Pascal choisit : Pascal, 2026-10-23.
- **Garde-fou = test vu rouge** et **bilan des écarts avant rendu** : Pascal, bilan du 2026-10-21.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : Compose Material 3, `ColorScheme`, dépendance JitPack ; la manière pour la palette d'état est à **proposer**, pas à trancher | il rend sa proposition avant de la coder |
| A-t-il les moyens ? | oui : `terrain-style` publié, JitPack construit ; l'essai au soleil est à Pascal | |

**Temps de la main :** **appuyer** : avant de toucher aux textes de la palette d'état, l'agent rend à l'orchestrateur un tableau : chaque texte (fichier:ligne), sa couleur et son fond, son contraste, la manière proposée (couleur foncée, texte noir à côté d'une pastille, rôle de `terrain-style`, couleur sémantique) et le contraste obtenu. Pascal valide avant le code.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent. L'orchestrateur ne le remplit pas._

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :**
  - changer la couleur des pastilles de la carte (question 6) ou le marqueur de la position ;
  - retoucher une valeur de `terrain-style` (s'il en faut une autre, il le dit) ;
  - ajouter un mode sombre, la couleur dynamique ou `verification-metadata.xml` ;
  - toucher aux briques GNSS, géodésie, journal, GeoPackage, voix ou carte (ADR 0012).

## Harnais

- **Peut :** ajouter le dépôt JitPack à `settings.gradle.kts`, **filtré** au groupe `com.github.pobsteta.terrain` ; déclarer `terrain-style` à `d840329` dans `gradle/libs.versions.toml` (avec sa source en commentaire, comme les autres lignes) ; écrire le thème de Surculus (`ui/theme/` ou équivalent) et le brancher dans `MainActivity.kt` ; remplacer les couleurs d'état des textes selon le tableau validé ; supprimer la copie de la palette dans `EcranEssaiGnss.kt` au profit de `Palette` ; mettre à jour `docs/dependances.md` et `NOTICE`.
- **Ne doit pas :** toucher à `main` ; modifier `fixtures/` ; ajouter une autre dépendance.
- **Sources autorisées :** `docs/adr/0008-bibliotheques-communes.md` ; `docs/spec.md` § 3 ; le dépôt `terrain` (`~/dev/terrain`, en lecture) ; la doc de Compose Material 3.

## Tests

- **Égalité** : un test JVM vérifie, rôle par rôle, que le `ColorScheme` de Surculus vaut des **valeurs littérales** écrites dans Surculus (dans le thème ou dans le test), recopiées de `StyleTerrain.clair` à `d840329`, et que `StyleTerrain.clair` les vaut aussi : c'est la seule garde contre un artefact reconstruit (verrou des 7 jours levé, ADR 0008 amendée) ; **vu rouge** une fois (un rôle changé, non commité).
- **Contraste rejoué** : un test parcourt les couples texte / fond que l'application emploie (rôles « on » sur leurs fonds, et chaque texte de la palette d'état sur son fond) avec `Contraste` de `terrain-style` : 7:1, 4,5:1 pour le grand texte (au moins 24 sp, ou 18,7 sp en gras) ; **vu rouge** une fois (l'orange actuel en texte).
- La légende de la carte : texte en noir, pastille en couleur, vérifié par un test ou une capture.
- Commande du projet : `.claude/test-command`.
- **Bilan des écarts** : le rapport liste chaque fichier modifié et chaque couple en échec, sans retouche.

## Terrain

- **Échantillon que Pascal vérifiera :** l'APK de la branche, au soleil, sur les écrans Tournées, Carte (légende, badge du fix, pastilles sur ortho claire), Guidage et Essai GNSS ; résultat noté au journal **avant la fusion** (spec § 3, ADR 0008).

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours ; au-delà, la décision remonte à Pascal.
- **Point de suivi :** à la remise du tableau de la palette d'état, puis à l'ouverture de la PR.

## Comment saura-t-on que ça marche ?

Au soleil, Pascal lit chaque texte de la carte et du guidage d'un coup d'œil, et les tests d'égalité et de contraste sont verts dans la CI de Surculus.
