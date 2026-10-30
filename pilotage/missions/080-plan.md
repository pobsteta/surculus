# Mission 080 — Plan (première tâche, « tenir »)

> Rendu par l'`afficheur` le 2026-10-30, sur `mission/080-test-de-rendu-des-ecrans`. **Aucun code de production ni de test
> n'est écrit** : l'essai fait pour ce plan est jetable et n'est pas committé. Le code attend le « oui » de Pascal.

**Question à laquelle répond le test :** un bandeau, un texte ou une bande cache-t-il un bouton, debout comme couché ?
Pour qui : le forestier en forêt ; le test sert d'abord l'orchestrateur et Pascal, qui n'ont plus à attendre la capture.

## 1. L'outil

**Robolectric 4.17 + `compose-ui-test-junit4`, en JVM, sans émulateur.** Robolectric est déjà dans le catalogue
(`gradle/libs.versions.toml`, utilisé par `:data`) ; la seule nouveauté est l'outil de test Compose.

| Élément | Version | Licence | Source | Où |
|---|---|---|---|---|
| `org.robolectric:robolectric` | 4.17 (déjà au catalogue) | MIT | Maven Central | `testImplementation` de `:app` |
| `androidx.compose.ui:ui-test-junit4` | 1.10.4, tirée par la BOM 2026.02.01 déjà employée (même version que `ui` 1.10.4 du journal, 060) | Apache-2.0 | Google Maven | `testImplementation` |
| `androidx.compose.ui:ui-test-manifest` | idem | Apache-2.0 | Google Maven | `debugImplementation` et `fumeeImplementation` (l'activité vide que `createComposeRule` lance) |
| `unitTests.isIncludeAndroidResources = true` | — | — | réglage AGP | `app/build.gradle.kts` |

Aucune dépendance de production ; `docs/dependances.md` mis à jour (licences, source, mission 080).
Écartés : Paparazzi et Roborazzi (captures d'images de référence : une dépendance de plus, des images à garder à jour,
et ce n'est pas la question posée) ; émulateur en CI (ADR 0011, déjà réservé à l'essai de fumée de la release).

**Essai jetable fait pour ce plan (non committé, défait) :** dans ce dépôt, `app/build.gradle.kts` + un test
`@RunWith(RobolectricTestRunner)` avec `@Config(sdk = [35], qualifiers = "w823dp-h384dp-land-450dpi")` qui pose un `Box` de
48 dp cliquable sous un `Text` et lit ses bornes. Résultat, par la tâche de `test-command` (`:app:testFumeeUnitTest`) :
**vert, le bouton mesure 135 × 135 px dans une racine de 2314 × 1080 px** (48 dp à 2,8125 px/dp = 450 dpi : l'écran du
S23 Ultra est bien reproduit, à 2 px près car le qualificatif est en dp entiers). Les dépendances se résolvent, le
`ComponentActivity` démarre, les bornes se lisent.
- Coût mesuré : le test seul 33,7 s la première fois (Robolectric télécharge le jar Android SDK 35, ensuite en cache) ;
  la commande ciblée a pris 2 min 56 s sur une machine où d'autres builds tournaient. **Coût CI à annoncer dans la PR : +1 à 2 min estimées**, mesuré
  avant et après sur la CI elle-même.
- Non essayé : `captureToImage` (voir § 3), la `MapView` d'osmdroid sous Robolectric, SDK 36.

## 2. Les écrans et les tailles

Source des tailles : capture de l'orchestrateur du 2026-10-30 (v0.7.95), journal.

| Écran rendu | Cas |
|---|---|
| Carte (`CartePrete`) | couché 823 × 384 dp en plein écran ; couché **sans** plein écran, hauteur utile ≈ 306 dp ; debout 384 × 823 dp. Chacun avec la bande « Récepteur GNSS non démarré » (`carte_recepteur_non_demarre`) et avec une placette choisie (6 boutons) puis sans (5 boutons). |
| Contrôle (`VueSession`) | debout et couché, en mode normal ; debout et couché en mode poche (bande « MODE POCHE / 3 s sur volume haut… ») ; avec le bandeau d'écoute affiché (« J'ÉCOUTE », « ÉCOUTE FERMÉE ») puis après son effacement. |

Densité 450 dpi partout. Le jeu de données vient de `fixtures/` (tournée d'essai de Couchey) pour la carte ; le contrôleur
vient de `BancControle` (déjà dans `app/src/test`) pour le contrôle.

**Ce qu'il faudra toucher en production (rendu inchangé, permis par la fiche) :** `CartePrete` et `VueSession` sont `private` et lisent le
disque, le GNSS et les services : les rendre `internal` et leur passer l'état (tournée ouverte, fix, état du récepteur,
mode poche) en paramètres avec la valeur d'aujourd'hui par défaut ; poser des `testTag` sur les six boutons de la carte, le
bandeau, les lignes de texte, le bouton « Mode poche », la bande, le bandeau d'écoute. **Risque nommé :** si la
`MapView` d'osmdroid ne se construit pas sous Robolectric, on la remplace en test par une `View` vide de même taille (un
paramètre `fondDeCarte` qui vaut le vrai par défaut) ; le rendu de production ne change pas.

## 3. Les assertions

1. **Boutons entiers** : les six (cinq sans placette choisie) existent, chacun est `assertIsDisplayed()` et ses bornes
   sont **entièrement dans la racine** (rien de coupé, ni en haut ni en bas : le cas des 5 et 6 boutons écrasés de la 078).
2. **Taille** : chaque bouton ≥ le plancher (voir question 3) et, quand la hauteur le permet, 48 dp.
3. **Rien ne recouvre un cliquable** : pour chaque nœud cliquable étiqueté, aucun autre nœud étiqueté visible (texte, bandeau,
   bande) dont les bornes le coupent, sauf ses propres enfants. Liste explicite des paires permises (la carte sous tout) ;
   on n'applique pas la règle à tous les nœuds de l'arbre pour ne pas fabriquer de faux rouges.
4. **Contrôle** : la bande du mode poche et le bouton « Mode poche » sont dans la racine après défilement de la zone ;
   le bandeau d'écoute ne couvre pas « Mode poche » une fois la minuterie échue (horloge de test, comme la 076).
5. **Le rognage de la carte (cause de la 055/060)** : une bordure qui déborde ne se voit **pas** dans les bornes sémantiques,
   puisque `AndroidView` dessinait hors de ses bornes sans changer sa taille. Il faut un pixel :
   `captureToImage()` avec `@GraphicsMode(NATIVE)` de Robolectric, une vue factice qui peint un rectangle plus grand
   qu'elle-même, et la lecture d'un pixel du bandeau. **À essayer au premier pas du code ; si ça ne marche pas, je le dis
   et ce défaut reste sur la liste « ce que le test ne voit pas ».**

Sortie pour Pascal : un fichier texte par cas (nom du cas, liste des nœuds étiquetés avec leurs bornes en dp, verdict), écrit dans `app/build/reports/rendu/`,
et publié comme artefact de la CI ; une image (`captureToImage`) si l'étape 5 marche. Échantillon à comparer : carte couchée, face à la
capture du 2026-10-30.

## 4. La preuve qu'il aurait vu rouge

On ne rejoue pas les anciens commits (ils n'ont pas le banc de test). On **défait la correction sur le nouveau test** et on montre le rouge, puis le vert une fois rétablie :

| Défaut passé | Correction à défaire | État cassé de référence | Rouge attendu |
|---|---|---|---|
| 075/076, « ÉCOUTE FERMÉE » reste affiché et cache « Mode poche » | diff de la 076 (`7133d9e`, PR #132 : `EcouteApp.kt`, `EcranControle.kt`) | `62ac182` (`main` avant la #132) | assertion 4 (bandeau ne couvre pas « Mode poche » après échéance) |
| 012-C1, bande du mode poche coupée quand l'écran est défilé | PR #118 (`ba5b13f`, mission 067) | `b0db019` (`main` avant la #118) | assertion 4 (bande dans la racine) |
| 055, bandeau de la carte recouvert | `clipToBounds()` de `25153e3` (PR #102, mission 060) | `75e8cfd` | assertion 5, **seulement si le pixel marche** |

**Engagement :** au moins une des trois (la 076, la plus sûre), si possible deux, avec le numéro d'exécution ou la sortie collée dans la PR.
Pour la 078 (boutons écrasés) : on baisse le plancher à un chiffre absurde dans `DispositionBoutons` et le test des six boutons entiers doit rougir.
Si je ne réussis à faire rougir qu'un défaut sur les trois, la PR le dira : un test jamais vu rouge ne compte pas.

## 5. Ce que le test ne verra pas

- Le rendu réel de la `MapView` d'osmdroid (tuiles, marqueurs, son propre dessin hors bornes) ; sous Robolectric, c'est une vue vide.
- Les polices et les chasses du téléphone (Roboto de Robolectric, pas celle de la Samsung One UI) : un texte plus large sur le téléphone passe à la ligne ailleurs ; réglage « taille de police » du système non testé (un cas à police agrandie viendra plus tard, hors de cette mission).
- Les barres système, la découpe de caméra, le plein écran réel : simulés par la hauteur choisie (≈ 306 dp), pas mesurés.
- Le doigt ganté, le soleil, la pluie ; le contraste (déjà `ContrasteTest`, `ContrasteControleTest`).
- Android 16 réel (le test tourne sur SDK 35, voir question 5) ; un autre téléphone (la fiche : « un autre téléphone ajoutera ses tailles »).
- Tout ce qu'un geste (défilement fait par la main, rotation en direct) change : le test joue des états, pas des mains.
- L'essai de fumée de la release sur émulateur : hors de cette mission (ADR 0011).

Le test n'enlève donc pas la capture finale du téléphone ; il fait qu'elle ne trouve plus ce qu'il pouvait voir.

## 6. Questions pour Pascal (une par une)

1. **L'outil.** Robolectric 4.17 + `compose-ui-test-junit4` 1.10.4 (via la BOM) + `ui-test-manifest`, en tests de `:app` seulement : oui ?
2. **Toucher la production sans changer le rendu.** Rendre `CartePrete` et `VueSession` `internal` avec l'état en paramètres, poser des `testTag` et, s'il le faut, un paramètre pour remplacer la `MapView` en test : oui ?
3. **Le plancher des boutons.** La fiche dit « jamais sous 40 dp » (journal, décision du 2026-10-30) ; le code de `main` (`DispositionBoutons.MIN_DP`) dit **32 dp**. Le test doit-il exiger 40 (il rougira alors dès le premier jour sur `main`, ce qui contredit « le test passe sur main ») ou 32 ? Le journal dit « lecture de l'orchestrateur à confirmer » : c'est cette confirmation qui manque.
4. **Le coût en CI.** +1 à 2 min environ (mesure réelle donnée dans la PR) : accepté ?
5. **Android 35 ou 36.** Robolectric 4.17 : le téléphone est sous Android 16 (SDK 36). Je propose SDK 35 si 36 n'est pas pris en charge, et je le dis dans la PR : d'accord ?
6. **Le pixel de la carte (assertion 5).** Si `captureToImage` ne marche pas sous Robolectric, on garde la 055 hors du filet (dite dans « ce que le test ne voit pas ») plutôt que d'ajouter un autre outil : d'accord ?

## 7. Après le « oui »

Une PR : dépendances et réglage, `internal` et `testTag`, `app/src/test/kotlin/.../rendu/` (un banc commun + `RenduCarteTest`,
`RenduControleTest`), sortie texte des nœuds, `docs/dependances.md`, preuve de rouge dans le corps de la PR, durée de CI annoncée,
`.claude/test-command` au code 0.

**Question de contrôle (du futur affichage) :** comprend-on en dix secondes qu'un bouton est caché, et lequel ? Le test des
dix secondes sera fait par Pascal sur la sortie texte de la carte couchée : il doit voir en une ligne « 6 boutons / 6 entiers /
0 recouvrement », ou le nom du bouton coupé.
