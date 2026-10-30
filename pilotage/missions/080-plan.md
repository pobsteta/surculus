# Mission 080 — Plan (première tâche, « tenir »)

> Rendu par l'`afficheur` le 2026-10-30, sur `mission/080-test-de-rendu-des-ecrans`, **repris (aller-retour 1/3) le même
> jour** selon la section « Reprise du plan » de la fiche (décisions de Pascal et corrections du relecteur). **Aucun code de
> production ni de test n'est écrit** : les essais faits pour ce plan sont jetables et ne sont pas committés. Le code attend
> le « oui » de Pascal.

**Question à laquelle répond le test :** un bandeau, un texte ou une bande cache-t-il un bouton, debout comme couché ?
Pour qui : le forestier en forêt ; le test sert d'abord l'orchestrateur et Pascal, qui n'ont plus à attendre la capture.

## 1. L'outil

**Robolectric 4.17 + `compose-ui-test-junit4`, en JVM, sans émulateur.** Robolectric est déjà dans le catalogue
(`gradle/libs.versions.toml`, utilisé par `:data`) ; la seule nouveauté est l'outil de test Compose.

| Élément | Version | Licence | Source | Où |
|---|---|---|---|---|
| `org.robolectric:robolectric` | 4.17 (déjà au catalogue) | MIT | Maven Central | `testImplementation` de `:app` |
| `androidx.compose.ui:ui-test-junit4` | 1.10.4, tirée par la BOM 2026.02.01 déjà employée (même version que `ui` 1.10.4 du journal, 060) | Apache-2.0 | Google Maven | `testImplementation` |
| `unitTests.isIncludeAndroidResources = true` | — | — | réglage AGP | `app/build.gradle.kts` |

**Tout reste en `testImplementation`** (décision 1 de Pascal) : plus de `ui-test-manifest`, ni de `debugImplementation`, ni de
`fumeeImplementation` ; rien n'entre dans les APK debug et fumée. Aucune dépendance de production ; `docs/dependances.md`
mis à jour (licences, source, mission 080).
Écartés : Paparazzi et Roborazzi (captures d'images de référence : une dépendance de plus, des images à garder à jour,
et ce n'est pas la question posée) ; émulateur en CI (ADR 0011, déjà réservé à l'essai de fumée de la release).

**Premier essai jetable (non committé, défait) :** avec `ui-test-manifest` en debug et fumée, un test Robolectric
(`@Config(sdk = [35], qualifiers = "w823dp-h384dp-land-450dpi")`) qui pose un `Box` de 48 dp cliquable sous un `Text` et lit ses
bornes. Vert par `:app:testFumeeUnitTest` : **135 × 135 px dans une racine de 2314 × 1080 px** (48 dp à 2,8125 px/dp = 450 dpi ;
2 px de moins que 2316 car le qualificatif est en dp entiers).
- Coût mesuré : le test seul 33,7 s la première fois (Robolectric télécharge le jar Android SDK 35, ensuite en cache) ;
  la commande ciblée a pris 2 min 56 s sur une machine où d'autres builds tournaient. **Coût CI annoncé : +1 à 2 min estimées**,
  mesuré avant et après sur la CI elle-même, dans la PR (accepté par Pascal, décision 4).
- Non essayé : `captureToImage` (§ 3), la `MapView` d'osmdroid sous Robolectric.

**Second essai jetable (reprise 1/3, non committé, défait) : la voie sans `ui-test-manifest` existe.** Seulement
`testImplementation` (Robolectric, BOM, `ui-test-junit4`) et `isIncludeAndroidResources`. Le test lance l'activité lui-même,
`Robolectric.buildActivity(ComponentActivity::class.java).setup()`, pose le contenu avec `setContent` de `activity-compose`
(déjà en `implementation`) et lit l'arbre par `createEmptyComposeRule()`. Résultat : **vert**, bouton de 135 × 135 px comme
avant, par `:app:testFumeeUnitTest` ; `app/build.gradle.kts` n'a gagné que des lignes `testImplementation` et le réglage de test
(en 9 s, cache chaud). Le banc commun du code ouvrira donc l'activité ainsi.

**SDK 36 essayé d'abord (relecteur) : échec.** Robolectric 4.17 avec la JDK 21 du dépôt s'arrête sur
`Failed to interact with raw FileDescriptor internals` (`jdk.internal.access` non exporté hors de `java.base`) ; **SDK 35 : vert**.
Piste pour 36, à tenter au premier pas du code : `--add-exports java.base/jdk.internal.access=ALL-UNNAMED` dans les `jvmArgs`
de la tâche de test ; sinon SDK 35, dit dans la PR (décision de la relecture, § 6).

## 2. Les écrans et les tailles

Source des tailles : capture de l'orchestrateur du 2026-10-30 (v0.7.95), journal.

| Écran rendu | Cas |
|---|---|
| Carte (`CartePrete`) | couché 823 × 384 dp en plein écran ; couché **sans** plein écran, hauteur utile ≈ 306 dp ; debout 384 × 823 dp en plein écran **et debout sans plein écran** (hauteur utile à mesurer, la barre de navigation étant alors en bas : valeur provisoire, voir § 6). Chacun avec la bande « Récepteur GNSS non démarré » (`carte_recepteur_non_demarre`) et avec une placette choisie (6 boutons) puis sans (5 boutons). |
| Contrôle (`VueSession`) | les mêmes quatre tailles (debout et couché, **avec et sans plein écran**) en mode normal ; debout et couché en mode poche (bande « MODE POCHE / 3 s sur volume haut… ») ; avec le bloc d'écoute affiché (« J'ÉCOUTE », « ÉCOUTE FERMÉE ») puis après son effacement. |

Densité 450 dpi partout. **Jeu de données de la carte :** une `TourneeOuverte(dossier, tournee, cartographie)` **construite en mémoire dans
`app/src/test`**, avec les trois placettes de Couchey (P35-01, P35-02, P35-03, positions en France ; un test vérifie qu'elles
tombent en France). Écartés : le modèle `travaux-sommier-0.31.1.gpkg` (toutes ses tables sont vides) et `TourneeDeTest`
(dans les tests de `:gpkg`, invisibles de `:app`) ; pas de `testFixtures`, donc le harnais ne bouge pas. Le contrôleur du contrôle vient de `BancControle` (déjà dans `app/src/test`).

**Ce qu'il faudra toucher en production (rendu inchangé, permis par la fiche) :** `CartePrete` et `VueSession` sont `private` et
lisent le disque, le GNSS et les services : les rendre `internal` et leur passer l'état (tournée ouverte, fix, état du
récepteur, mode poche) en paramètres avec la valeur d'aujourd'hui par défaut ; poser des `testTag` sur les boutons de la carte,
le bandeau, les lignes de texte, le bouton « Mode poche », la bande, le bloc d'écoute. **Risque nommé :** si la `MapView`
d'osmdroid ne se construit pas sous Robolectric, un paramètre `fondDeCarte` remplace **la fabrique de la `View`** à l'intérieur du
même `AndroidView(… .clipToBounds())` et vaut le vrai par défaut. L'`AndroidView` passe alors en `View` avec un **cast vers
`MapView` dans `update`** (gardé par un `as?`), sans changer le rendu : le `clipToBounds()` reste dans le rendu testé. `CarteClipTest`, `BandeauCarteGuidageTest` et `DispositionControleTest` restent intacts.

## 3. Les assertions

1. **Boutons entiers** : les six (cinq sans placette choisie) existent, chacun est `assertIsDisplayed()` et ses bornes
   sont **entièrement dans la racine** (rien de coupé, ni en haut ni en bas : le cas des boutons écrasés de la 078).
2. **Taille** : chaque bouton ≥ 32 dp (plancher décidé le 2026-10-30, `DispositionBoutons.MIN_DP`, lu dans le code) et, quand la
   hauteur le permet, 48 dp.
3. **Rien ne recouvre un cliquable** : la règle porte sur **tous** les nœuds cliquables et **tous** les textes de l'arbre
   sémantique (étiquetés ou non ; le `testTag` ne sert qu'à nommer dans le rapport) : aucun texte ni autre nœud visible ne coupe
   un cliquable, sauf ses propres descendants. Une **liste d'exceptions explicite et courte**, chacune justifiée dans le code :
   la carte (`AndroidView`) sous tout, et le bloc d'écoute sur « Mode poche » (point 4). Toute exception ajoutée est lue par le relecteur.
4. **Contrôle** : la bande du mode poche et le bouton « Mode poche » sont dans la racine après défilement de la zone ; le bloc
   d'écoute **peut** recouvrir « Mode poche » pendant l'écoute et jusqu'à `DUREE_BANDEAU_MS` (6 s) après (décision 2 de Pascal) ;
   **à l'échéance, plus du tout** (horloge de test avancée de `DUREE_BANDEAU_MS`, puis d'une milliseconde de plus, comme la 076).
   La constante est lue dans le code, pas recopiée.
5. **Le rognage de la carte (cause de la 055/060)** : un débordement ne se voit **pas** dans les bornes sémantiques, puisque
   `AndroidView` dessinait hors de ses bornes sans changer sa taille. Il faut un pixel : `captureToImage()` avec
   `@GraphicsMode(NATIVE)` de Robolectric, une vue factice qui peint un rectangle plus grand qu'elle-même, et la lecture d'un
   pixel du bandeau. **À essayer au premier pas du code ; si ça ne marche pas, le dire (décision 5), sans autre outil** : ce
   défaut reste sur la liste « ce que le test ne voit pas ».

Sortie pour Pascal : un fichier texte par cas (nom du cas, liste des nœuds avec leurs bornes en dp, verdict) dans
`app/build/reports/rendu/`, publié comme artefact de la CI ; une image (`captureToImage`) si l'étape 5 marche. Échantillon à
comparer : carte couchée, face à la capture du 2026-10-30.

## 4. La preuve qu'il aurait vu rouge

Méthode (décision 3 de Pascal) : **défaire la correction sur le code actuel**, pas rejouer d'anciens commits (ils n'ont pas le
banc de test). Pour chaque défaut : un patch, donné **en entier dans le corps de la PR** ; un commit nommé sur une **branche de
preuve à part** (`preuve/080-<défaut>`, partant de la tête de la PR, jamais fusionnée) ; la CI y est rouge, puis verte une fois le
patch retiré ; le relecteur peut rejouer le patch. **Au moins deux défauts passés.**

| Défaut passé | Correction à défaire | État cassé de référence | Rouge attendu |
|---|---|---|---|
| 075/076, « ÉCOUTE FERMÉE » reste affiché et cache « Mode poche » | **Patch qui garde l'API et ne casse que le comportement** (défaire `69b60af` + `7133d9e` tels quels casserait la compilation du test : horloge injectée, `DUREE_BANDEAU_MS`). Par exemple : `bandeauVisible()` (`EcouteApp`, qui relaie `PilotageEcoute`) reste vrai après la fermeture, la minuterie ne l'éteint plus. Signatures, constante et horloge inchangées. | l'état cassé de référence reste `62ac182` (`main` avant la #132), et `69b60af` + `7133d9e` (écart `62ac182..f5b7e7a`) restent la référence de ce qu'on défait en comportement | assertion 4 : recouvrement toujours là après l'échéance |
| 012-C1/067, bande du mode poche coupée quand l'écran est défilé | même règle : patch qui remet la bande dans la zone qui défile **sans changer de signature** (ex. la bande redevient un enfant de la `Column` défilante), PR #118 (`ba5b13f`) comme référence | `b0db019` (`main` avant la #118) | assertion 4 : bande hors de la racine |
| 078, boutons écrasés | **remonter** `DispositionBoutons.MIN_DP` à 47 ou 48 (au lieu de 32), de sorte que la colonne de 6 boutons ne tienne plus à ≈ 306 dp et que les derniers soient écrasés ou coupés | état actuel de `main` | assertions 1 et 2 |
| 055, bandeau recouvert | `clipToBounds()` de `25153e3` (PR #102, mission 060) | `75e8cfd` | assertion 5, **seulement si le pixel marche** |

Engagement : la 076 et la 078 d'abord (les plus sûres, sans pixel) ; la 067 et la 060 en plus si elles viennent. Un défaut dont
le rouge n'est pas vu est écrit tel quel dans la PR : un test jamais vu rouge ne compte pas.

## 5. Ce que le test ne verra pas

- Le rendu réel de la `MapView` d'osmdroid (tuiles, marqueurs, son propre dessin hors bornes) ; sous Robolectric, c'est une vue vide.
- Les polices et les chasses du téléphone (celles de Robolectric, pas celles de la Samsung) : un texte plus large sur le téléphone passe à la ligne ailleurs ; la taille de police du système n'est pas testée (un cas viendra plus tard, hors de cette mission).
- Les barres système, la découpe de caméra, le plein écran réel : simulés par la hauteur choisie (≈ 306 dp), pas mesurés.
- Le doigt ganté, le soleil, la pluie ; le contraste (déjà `ContrasteTest`, `ContrasteControleTest`).
- Android 16 réel (le test tourne sur SDK 35 faute de mieux, § 1) ; un autre téléphone (la fiche : « un autre téléphone ajoutera ses tailles »).
- Tout ce qu'un geste (défilement fait par la main, rotation en direct) change : le test joue des états, pas des mains.
- L'essai de fumée de la release sur émulateur : hors de cette mission (ADR 0011).

Le test n'enlève donc pas la capture finale du téléphone ; il fait qu'elle ne trouve plus ce qu'il pouvait voir.

## 6. Décisions de Pascal (2026-10-30) et reste à lui poser

Les six questions du premier plan sont tranchées (fiche, « Reprise du plan ») : manifeste en tests seulement, bandeau permis 6 s,
preuve par défaut défait sur le code actuel, CI : coût accepté et étape d'artefact dans `.github/workflows/ci.yml` (seule étape
ajoutée au harnais, qui publie `app/build/reports/rendu/`), 055 dite si le pixel échoue, plancher 32 dp.

**Aucune question nouvelle pour Pascal.** SDK 36 ne démarre pas sous Robolectric 4.17 avec la JDK 21 du dépôt (§ 1) : choix
technique, pas de sens ni de périmètre. Au premier pas du code, essayer `--add-exports` dans les `jvmArgs` de la tâche de test ;
sinon rester sur SDK 35, la différence dite dans la PR.

**Valeur provisoire à inscrire dans la PR :** la hauteur utile « debout sans plein écran » (non mesurée à ce jour), notée
dans la rubrique « Valeur provisoire » de la fiche avec sa source (la sortie du test, puis la capture du téléphone).

## 7. Après le « oui »

Une PR : dépendances et réglage, `internal` et `testTag`, `app/src/test/kotlin/.../rendu/` (un banc commun + `RenduCarteTest`,
`RenduControleTest`), sortie texte des nœuds, `docs/dependances.md`, `.github/workflows/ci.yml` (étape d'artefact seule), preuves
de rouge (patchs dans le corps de la PR, branches `preuve/080-*`), durée de CI annoncée, `.claude/test-command` au code 0.

**Question de contrôle (du futur affichage) :** comprend-on en dix secondes qu'un bouton est caché, et lequel ? Le test des
dix secondes sera fait par Pascal sur la sortie texte de la carte couchée : il doit voir en une ligne « 6 boutons / 6 entiers /
0 recouvrement », ou le nom du bouton coupé.
