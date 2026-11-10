# Mission 104 — Audit des gardes de rendu et de forme

> Fiche écrite par l'orchestrateur le 2026-11-09, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> après la décision de Pascal au bilan du 2026-11-09 (cause n° 3 du Pareto, « Oui, l'audit »). Depuis `main`.

## Mission

**En une phrase (quoi, et pourquoi) :** Muter chaque garde de rendu et de forme existante de `app/src/test/` (tests qui
lisent un rendu, une forme, une dimension ou une disposition d'écran) et la voir rouge, ou la signaler muette, pour
qu'aucune garde ne protège plus rien sans qu'on le sache (garde tautologique de la 102, quatre tests muets de la 096).

**Redite par l'agent :**
- *Testeur :* Je dois, pour chaque garde de rendu et de forme de `app/src/test/`, casser à la main la valeur qu'elle garde dans le code de production (sur une copie locale jamais poussée), voir le test rouge puis annuler, et signaler toute garde qui reste verte (muette), sans corriger l'application.

## Les trois questions

| Question | Réponse | Conséquence |
|---|---|---|
| Redire en une phrase ? | oui | |
| Sait-il comment ? | oui : mutation à la main (changer la valeur gardée dans le code de production, sur une copie locale non poussée), voir le test rouge, annuler | |
| Les moyens ? | oui : `.claude/test-command`, Robolectric | |

**Temps de la main :** appuyer.

## Ma réponse d'abord

_Rempli par Pascal seul._

## L'Homme

- **Agent :** `testeur`, sonnet. Ici, le `testeur` lit le code de production, puisqu'il doit le muter ; il n'en pousse
  aucune modification.
- **Ce qui ne lui est pas demandé :** corriger le code de l'application ; réécrire les tests de logique (hors rendu et
  forme) ; renforcer une garde muette sans le dire (il la signale ; il peut proposer le correctif dans un commit à part,
  nommé).

## Harnais

- **Peut :** lister les gardes (par exemple `BancRendu`, `RenduLibellesCodesTerminerTest`, `BoussoleTest`, tests
  `Rendu*`, tests de dimensions de `DimensionsControle`, `DispositionBoutons`) ; pour chacune, une mutation du code de
  production qu'elle doit voir, le résultat (rouge / reste vert), la sortie de test ; remplir la rubrique « Résultat »
  de cette fiche ; corriger une garde muette dans un commit à part.
- **Ne doit pas :** pousser une mutation du code de production ; toucher à `main`, `fixtures/`, `docs/`.
- **Sources :** `CLAUDE.md`, fiches 080, 096 et 102, journal (« une garde n'est prouvée que vue rouge »).

## Valeur provisoire

Aucune.

## Dépend de / rouge attendu jusqu'à

Aucune dépendance. Une seule PR, en brouillon jusqu'à la fin. CI verte attendue (aucune mutation poussée).

## Tests

`.claude/test-command` au code 0 sur la branche poussée.

## Terrain

Sans objet (aucun écran ne change).

## Qui veille, qui tranche

- **Relecteur :** `relecteur` (rejoue deux mutations au hasard), puis Pascal.
- **Critère d'arrêt :** 3 allers-retours. **Qui tranche :** Pascal (garder, corriger ou retirer une garde muette).

## Comment saura-t-on que ça marche ?

Un tableau garde / mutation / rouge ou muette, chaque ligne avec sa sortie de test ; aucune garde muette non signalée.

## Résultat

**Bilan : 57 gardes auditées, 52 rouges, 5 muettes (les cinq corrigées dans des commits à part, vues rouges sous la même mutation).**
En plus, deux mutations sont restées vertes sans que la garde soit muette (M8, M11, voir plus bas). 71 lignes de mutation jouées.

### Méthode

- Mutation à la main d'une valeur du code de production (ou, pour les gardes de source, d'un texte que la garde lit), sur le
  fichier du worktree, **annulée après chaque essai** (le fichier est remis depuis une copie) : aucune mutation n'est dans un commit.
  Une mutation qui ne compile pas ne compte pas (K2, K3, K4, H3, J1, O1 ont été refaites autrement).
- Commande de chaque essai : `ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:testFumeeUnitTest --tests '<classe>'`. Les extraits ci-dessous sont
  le nom du test en échec et la ligne de l'assertion, tels que Gradle les affiche.
- Périmètre : les tests de `app/src/test/` qui lisent un rendu (`BancRendu` : `RenduControleTest`, `RenduCarteTest`,
  `RenduLibellesCodesTerminerTest`, `RenduGuidageSansPositionTest` cas 9, `RenduPlacetteIntrouvableTest`, `PlacetteIntrouvableEnFrancaisTest`,
  `BancRenduTest`), une forme, une dimension ou une disposition (`DispositionBoutonsTest`, `DispositionControleTest`, `MicroControleTest`,
  `ContrasteControleTest`, `TypographieTest`, `ThemeEnListeTest`, `EcranPlacettesSourcesTest`, `BandeauCarteGuidageTest`, `BoussoleTest` (gardes de source),
  `RenduAppTest` (bouton de 56 dp), `NavigationTest` (pas de bouton de 64 dp sur l'accueil)).
- **Non audités** (hors périmètre : logique, texte ou scénario, pas un rendu ni une forme) : `RenduDoubleAppui*` (et les cas 1 à 8 de
  `RenduGuidageSansPositionTest`), `AucunTexteTechniqueAppTest`, `PointageControleTest`, `TerminerPlantSansEtatTest`, `RechercheTourneesTest`,
  les tests de logique de `BoussoleTest`, `RenduAppTest` (hors bouton), `NavigationTest` (hors accueil). Dans `TypographieTest`,
  `EcranPlacettesSourcesTest` et `BandeauCarteGuidageTest`, les assertions de simple présence d'un symbole (icône, appel de fonction) ne sont
  pas toutes mutées : une mutation par famille a été jouée, pas une par assertion.
- Suffixe `c` : mutation rejouée avec la **garde corrigée** par un commit « garde muette ». D5b est D5 déplacée sur un bouton de la vue Plant.

### Les gardes (une par propriété vérifiée)

| n° | Garde | Mutation(s) | Verdict |
|---|---|---|---|
| 1-5 | `DispositionBoutonsTest` : plancher 32 dp, plafond 48 dp, écart, marge, une seule colonne via `tailleDp` | A1, A2, A3, A4, A5 | rouges |
| 6 | `DispositionControleTest` : le plan couché tient dans la hauteur | B1 | rouge |
| 7 | `DispositionControleTest` : la réserve de hauteur du plan couché | B2 | **muette** (corrigée, B2c) |
| 8-13 | `MicroControleTest` : place du micro, zone 128 dp, micro 112 dp et icône 48 dp, bloc hors de la colonne qui défile, zone qui garde sa place, état du micro | C4, C1, C2/C3, C5, C6, C7 | rouges |
| 14-17 | `ContrasteControleTest` : contraste des textes, cibles 48 dp (constantes), formes et teintes des états, filet blanc | D4, D1/D2/D6, D3, D7 | rouges |
| 18 | `ContrasteControleTest` : « aucune cible tactile de l'écran n'est sous 48 dp » face à un bouton écrit en dur à 40 dp | D5, D5b | **muette** (corrigée, D5c) |
| 19 | `ThemeEnListeTest` : aucun bouton radio | F3 | rouge |
| 20 | `ThemeEnListeTest` : les cibles du menu du thème font 48 dp | F1, F2 | **muette** (corrigée, F1c, F2c) |
| 21-23 | `EcranPlacettesSourcesTest` : ligne de placette 64 dp, aucun `fontSize`, rognage de la carte | G1, G2, N1 | rouges |
| 24-26 | `BandeauCarteGuidageTest` : aucun autre `fontSize`, six boutons flottants, taille `tailleBouton` | H1, H3, H4 | rouges |
| 27 | `RenduAppTest` : bouton de 56 dp | I1 | rouge |
| 28 | `NavigationTest` : pas de bouton de 64 dp sur l'accueil | J1 | rouge |
| 29-32 | `TypographieTest` : aucune taille écrite à la main, aucune `fontFamily`, bandeau de 48 dp, styles du kanban | E1, E2, E3, E4 | rouges |
| 33-36 | `BoussoleTest` (gardes de source) : pas de `GeomagneticField`, boussole sans `Position`, `delay(1000)` écrit une fois, aucun `fontSize` dans `controle/` | K3, K2, K4, E1 | rouges |
| 37 | `BancRenduTest` : l'effet reprend sur le fil principal | L1 | rouge |
| 38-40 | `RenduCarteTest` : boutons au moins au plancher, position et recouvrement, la carte ne déborde pas du bandeau | N2, N3, N1 | rouges |
| 41-45 | `RenduLibellesCodesTerminerTest` : coins de 12 dp lus sur l'image, 4 codes par ligne, libellés dans un bouton assez haut, code en gros, aucun mot coupé | P1, P2, P3, P4, P6 | rouges |
| 46-53 | `RenduControleTest` : bloc d'écoute effacé après `DUREE_BANDEAU_MS`, appui absorbé, lignes non tronquées, cadre et bornes du bloc, « Mode poche » vu, bloc sur le haut du pied, bande de poche après défilement, recouvrements en mode normal | M1, M2, M3/M4, M5/M6/M7, M9, M10, M12, M13 | rouges |
| 54 | `RenduGuidageSansPositionTest` cas 9 : le bouton « Ouvrir sans position » est vu (poussé hors de l'écran) | O2 | rouge |
| 55 | `RenduGuidageSansPositionTest` cas 9 : le bouton est vu **en entier** (pas rogné en partie) | O1 | **muette** (corrigée, O1c) |
| 56 | `RenduPlacetteIntrouvableTest` et `PlacetteIntrouvableEnFrancaisTest` : le refus est affiché | Q1 | rouge |
| 57 | `RenduLibellesCodesTerminerTest` : libellé non rogné en hauteur (`didOverflowHeight`) | P5 | **muette** (corrigée, P5c) |

### Les gardes muettes (tranchées par Pascal le 2026-11-10, voir « Décisions de Pascal »)

1. **Garde 7, réserve du plan couché** (`DispositionControleTest.kt:16`). La garde compare le côté du plan à `360 - RESERVE_HAUTEUR_PLAN_DP`,
   c'est-à-dire à la constante qu'elle devrait surveiller : mise à 0 (B2), elle reste verte. Correctif proposé et commité : une borne écrite en
   dur, `côté <= 360 - 48` (au moins la barre d'application). La valeur de 88 dp n'a pas de source dans la fiche 067 : Pascal peut en fixer une.
2. **Garde 18, cible de 48 dp** (`ContrasteControleTest.kt:81`). `DimensionsControle.toutes()` ne liste que des constantes ; un bouton
   écrit à 40 dp en dur (D5 sur la vue Arrivée, D5b sur « Pointer » de la vue Plant) n'est vu par aucun test, ni par les rendus. Correctif
   commité : `RenduControleTest.verifier` refuse tout cliquable dont la **taille de mise en page** est sous 48 dp (nouveau champ `Noeud.taille` du banc).
   Aucun cliquable de l'écran du contrôle de l'essai n'est sous 48 dp aujourd'hui (les rendus du contrôle restent verts). La vue Arrivée n'est
   rendue par aucun test : D5 (ligne 344) y reste invisible même après correction.
3. **Garde 20, cibles du menu du thème** (`ThemeEnListeTest.kt:32`). `"heightIn(min = 48.dp)" in source` : l'expression est écrite quatre fois
   dans le fichier, un item du menu (F1, ligne 151) ou la ligne d'ouverture (F2, ligne 141) à 30 ou 20 dp passaient. Correctif commité :
   la garde juge le voisinage de l'item et celui de la ligne.
4. **Garde 55, bouton rogné en partie** (`RenduGuidageSansPositionTest.kt:348`). `BancRendu.vu` ne refuse qu'un nœud d'aire nulle : un bouton
   dont la hauteur est portée à 896 dp (O1) est coupé par l'écran, mais la partie visible a une aire et tient dans la racine, donc « vu en entier ».
   Correctif commité dans `BancRendu` : `vu` compare aussi la boîte rognée à la taille de mise en page (`Noeud.taille`). Les trois classes qui appellent `vu` (`RenduCarteTest.kt:79`, `RenduControleTest.kt:100,130`, `RenduGuidageSansPositionTest.kt:349`)
   restent vertes avec ce durcissement ; à valider par Pascal puisqu'il change la règle de `vu` pour tous les rendus.

5. **Garde 57, libellés rognés en hauteur** (`RenduLibellesCodesTerminerTest.kt`, `defautsDeLibelle`). P5 (`EcranControle.kt:926`, `maxLines = 2` → `1`) fait perdre sa 2e ligne
   à une quinzaine de libellés sans ellipse : ni « ellipsé » ni « hors de sa zone », la garde restait verte. (Classement initial « équivalente » faux, corrigé à la relecture.)
   Correctif commité : la garde vérifie `didOverflowHeight` ; rouge sous P5 (P5c), verte sans mutation.

### Mutations vertes qui ne rendent pas une garde muette

- **M8** (`DispositionMicro.kt:20`, zone du micro 128 → 420 dp) : `RenduControleTest` ne promet pas la place du micro (c'est `MicroControleTest`, rouge sous C1).
- **M11** (`EcranControle.kt:737`, titre de la bande de poche en `displayLarge`) : la bande reste entière, la garde « bande entière » n'a rien à voir.
  La propriété est rouge sous M12 (bande poussée hors de l'écran).

### Tableau garde / mutation / rouge ou muette / sortie

| Id | Garde (test fichier:ligne) | Mutation (fichier:ligne, avant → après) | Résultat | Extrait de sortie (`testFumeeUnitTest`) |
|---|---|---|---|---|
| A1 | `DispositionBoutonsTest.kt:49` — plus court que le plancher, la taille reste 32 dp | `DispositionBoutons.kt:10` : `const val MIN_DP = 32` → `const val MIN_DP = 30` | **rouge** (2) échecs | `DispositionBoutonsTest > plus court que le plancher, la taille reste 32 dp FAILED` → `AssertionError at DispositionBoutonsTest.kt:49` |
| A2 | `DispositionBoutonsTest.kt:24` — debout les six boutons font 48 dp | `DispositionBoutons.kt:9` : `const val CIBLE_DP = 48` → `const val CIBLE_DP = 50` | **rouge** (2) échecs | `DispositionBoutonsTest > debout les six boutons font 48 dp FAILED` → `AssertionError at DispositionBoutonsTest.kt:24` |
| A3 | `DispositionBoutonsTest.kt:36` — couche a 292 dp les six boutons descendent a 38 dp, sous 40 | `DispositionBoutons.kt:11` : `const val ECART_DP = 8` → `const val ECART_DP = 10` | **rouge** (2) échecs | `DispositionBoutonsTest > couche a 292 dp les six boutons descendent a 38 dp, sous 40 FAILED` → `AssertionError at DispositionBoutonsTest.kt:36` |
| A4 | `DispositionBoutonsTest.kt:30` — couche a 336 dp les six boutons font 45 dp | `DispositionBoutons.kt:12` : `const val MARGE_DP = 12` → `const val MARGE_DP = 10` | **rouge** (2) échecs | `DispositionBoutonsTest > couche a 336 dp les six boutons font 45 dp FAILED` → `AssertionError at DispositionBoutonsTest.kt:30` |
| A5 | `DispositionBoutonsTest.kt:68` — la carte utilise une seule colonne et plus les deux colonnes de la 064 | `EcranCarte.kt:396` : `val tailleBouton = DispositionBoutons.tailleDp(maxHeight.va…` → `val tailleBouton = 48` | **rouge** (3) échecs | `DispositionBoutonsTest > la carte utilise une seule colonne et plus les deux colonnes de la 064 FAILED` → `AssertionError at DispositionBoutonsTest.kt:68` |
| B1 | `DispositionControleTest.kt:16` — couche, le plan tient dans la hauteur disponible | `PlanPlants.kt:98` : `minOf(largeurDp, hauteurEcranDp - RESERVE_HAUTEUR_PLAN_DP)` → `largeurDp` | **rouge** (1) échec | `DispositionControleTest > couche, le plan tient dans la hauteur disponible FAILED` → `AssertionError at DispositionControleTest.kt:16` |
| B2 | voir la note | `PlanPlants.kt:89` : `const val RESERVE_HAUTEUR_PLAN_DP = 88f` → `const val RESERVE_HAUTEUR_PLAN_DP = 0f` | **MUETTE** (vert) | `BUILD SUCCESSFUL` (aucun échec) |
| B2c | `DispositionControleTest.kt:19` — couche, le plan tient dans la hauteur disponible | `PlanPlants.kt:89` : `const val RESERVE_HAUTEUR_PLAN_DP = 88f` → `const val RESERVE_HAUTEUR_PLAN_DP = 0f` | **rouge** (1) échec | `DispositionControleTest > couche, le plan tient dans la hauteur disponible FAILED` → `AssertionError at DispositionControleTest.kt:19` |
| C1 | `MicroControleTest.kt:26` — couche, le micro est dans une colonne fixe de 128 dp a droite | `DispositionMicro.kt:20` : `val ZONE = 128.dp` → `val ZONE = 100.dp` | **rouge** (2) échecs | `MicroControleTest > couche, le micro est dans une colonne fixe de 128 dp a droite FAILED` → `AssertionError at MicroControleTest.kt:26` |
| C2 | `MicroControleTest.kt:99` — le micro fait 112 dp, son icone 48 dp, et la zone le contient | `DispositionMicro.kt:16` : `val BOUTON = 112.dp` → `val BOUTON = 100.dp` | **rouge** (1) échec | `MicroControleTest > le micro fait 112 dp, son icone 48 dp, et la zone le contient FAILED` → `AssertionError at MicroControleTest.kt:99` |
| C3 | `MicroControleTest.kt:100` — le micro fait 112 dp, son icone 48 dp, et la zone le contient | `DispositionMicro.kt:17` : `val ICONE = 48.dp` → `val ICONE = 40.dp` | **rouge** (1) échec | `MicroControleTest > le micro fait 112 dp, son icone 48 dp, et la zone le contient FAILED` → `AssertionError at MicroControleTest.kt:100` |
| C4 | `MicroControleTest.kt:25` — couche, le micro est dans une colonne fixe de 128 dp a droite | `DispositionMicro.kt:34` : `if (largeurDp > hauteurDp) EmplacementMicro.COLONNE else Em…` → `if (largeurDp > hauteurDp + 1000f) EmplacementMicro.COLONNE…` | **rouge** (1) échec | `MicroControleTest > couche, le micro est dans une colonne fixe de 128 dp a droite FAILED` → `AssertionError at MicroControleTest.kt:25` |
| C5 | `MicroControleTest.kt:73` — le bloc J'ECOUTE est pose hors de la colonne qui defile | `EcranControle.kt:557` : `val zone: @Composable (Modifier) -> Unit = { m -> ⏎        …` → `val zone: @Composable (Modifier) -> Unit = { m -> ⏎        …` | **rouge** (1) échec | `MicroControleTest > le bloc J'ECOUTE est pose hors de la colonne qui defile FAILED` → `AssertionError at MicroControleTest.kt:73` |
| C6 | `MicroControleTest.kt:95` — la zone qui defile ne change pas de place dans la composition entre Plant et Po… | `EcranControle.kt:537` : `val avecMicro = e.vue == VueControle.PLANT && !poche` → `val avecMicro = e.vue == VueControle.PLANT && !poche ⏎     …` | **rouge** (1) échec | `MicroControleTest > la zone qui defile ne change pas de place dans la composition entre Plant et Pointer FAILED` → `AssertionError at MicroControleTest.kt:95` |
| C7 | `MicroControleTest.kt:62` — l'etat du micro est calcule dans BoutonMicro, apres la lecture de EcouteApp ver… | `EcranControle.kt:537` : `val avecMicro = e.vue == VueControle.PLANT && !poche` → `val avecMicro = e.vue == VueControle.PLANT && !poche ⏎     …` | **rouge** (1) échec | `MicroControleTest > l'etat du micro est calcule dans BoutonMicro, apres la lecture de EcouteApp version FAILED` → `AssertionError at MicroControleTest.kt:62` |
| D1 | `ContrasteControleTest.kt:52` — le plan se dessine en sombre avec le filet blanc des pastilles, et la tolerance… | `FormesEtat.kt:55` : `val CIBLE_MIN = 48.dp` → `val CIBLE_MIN = 40.dp` | **rouge** (1) échec | `ContrasteControleTest > le plan se dessine en sombre avec le filet blanc des pastilles, et la tolerance du to… FAILED` → `AssertionError at ContrasteControleTest.kt:52` |
| D2 | `ContrasteControleTest.kt:83` — aucune cible tactile de l'ecran n'est sous 48 dp, les etats sont plus gros | `FormesEtat.kt:56` : `val BOUTON_ETAT = 64.dp` → `val BOUTON_ETAT = 60.dp` | **rouge** (1) échec | `ContrasteControleTest > aucune cible tactile de l'ecran n'est sous 48 dp, les etats sont plus gros FAILED` → `AssertionError at ContrasteControleTest.kt:83` |
| D3 | `ContrasteControleTest.kt:73` — les quatre etats ont chacun leur forme et leur teinte, celles de la palette, ri… | `FormesEtat.kt:40` : `EtatDsf.ATTEINT -> FormeEtat.TRIANGLE` → `EtatDsf.ATTEINT -> FormeEtat.DISQUE` | **rouge** (1) échec | `ContrasteControleTest > les quatre etats ont chacun leur forme et leur teinte, celles de la palette, rien d'a… FAILED` → `AssertionError at ContrasteControleTest.kt:73` |
| D4 | `ContrasteControleTest.kt:45` — le cap, la distance et l'aide de la vue Pointer se lisent a 4,5 pour 1 | `Palette.kt:63` : `fun neutre(sombre: Boolean): Color = if (sombre) SchemaSomb…` → `fun neutre(sombre: Boolean): Color = if (sombre) SchemaSomb…` | **rouge** (2) échecs | `ContrasteControleTest > le cap, la distance et l'aide de la vue Pointer se lisent a 4,5 pour 1 FAILED` → `AssertionError at ContrasteControleTest.kt:45` |
| D5 | voir la note | `EcranControle.kt:344` : `Button(onClick = it, modifier = Modifier.fillMaxWidth().hei…` → `Button(onClick = it, modifier = Modifier.fillMaxWidth().hei…` | **MUETTE** (vert) | `BUILD SUCCESSFUL` (aucun échec) |
| D5b | voir la note | `EcranControle.kt:705` : `OutlinedButton(onClick = { agir { ouvrirPointer() } }, modi…` → `OutlinedButton(onClick = { agir { ouvrirPointer() } }, modi…` | **MUETTE** (vert) | `BUILD SUCCESSFUL` (aucun échec) |
| D5c | `RenduControleTest.kt:132` — mode poche debout en plein ecran, apres defilement | `EcranControle.kt:705` : `…height(DimensionsControle.ACTION)` → `…height(40.dp)` | **rouge** (2) échecs | `RenduControleTest > mode poche debout en plein ecran, apres defilement FAILED` → `AssertionError at RenduControleTest.kt:132` |
| D6 | `ContrasteControleTest.kt:82` — aucune cible tactile de l'ecran n'est sous 48 dp, les etats sont plus gros | `FormesEtat.kt:57` : `val BOUTON_CAUSE = 56.dp` → `val BOUTON_CAUSE = 40.dp` | **rouge** (1) échec | `ContrasteControleTest > aucune cible tactile de l'ecran n'est sous 48 dp, les etats sont plus gros FAILED` → `AssertionError at ContrasteControleTest.kt:82` |
| D7 | `ContrasteControleTest.kt:51` — le plan se dessine en sombre avec le filet blanc des pastilles, et la tolerance… | `Palette.kt:75` : `val filet: Color = Color(0xFFFFFFFF)` → `val filet: Color = Color(0xFF444444)` | **rouge** (2) échecs | `ContrasteControleTest > le plan se dessine en sombre avec le filet blanc des pastilles, et la tolerance du to… FAILED` → `AssertionError at ContrasteControleTest.kt:51` |
| E1 | `TypographieTest.kt:55` — aucuneTailleEcriteALaMainHorsExceptions | `EcranControle.kt:765` : `import androidx.compose.ui.unit.dp@@@Text(it, style = Mater…` → `import androidx.compose.ui.unit.dp ⏎ import androidx.compos…` | **rouge** (2) échecs | `TypographieTest > aucuneTailleEcriteALaMainHorsExceptions FAILED` → `AssertionError at TypographieTest.kt:55` |
| E2 | `TypographieTest.kt:43` — aucuneFontFamilyPosee | `EcranControle.kt:765` : `Text(it, style = MaterialTheme.typography.bodyMedium, color…` → `Text(it, style = MaterialTheme.typography.bodyMedium, fontF…` | **rouge** (1) échec | `TypographieTest > aucuneFontFamilyPosee FAILED` → `AssertionError at TypographieTest.kt:43` |
| E3 | `TypographieTest.kt:80` — bandeauCompactCommeMarculus | `BandeauCompact.kt:55` : `.height(48.dp)` → `.height(56.dp)` | **rouge** (1) échec | `TypographieTest > bandeauCompactCommeMarculus FAILED` → `AssertionError at TypographieTest.kt:80` |
| E4 | `TypographieTest.kt:64` — vueKanbanAuxStylesDeMarculus | `VueKanban.kt:160` : `Text(carte.ligne.foret ?: carte.ligne.id, style = MaterialT…` → `Text(carte.ligne.foret ?: carte.ligne.id, style = MaterialT…` | **rouge** (1) échec | `TypographieTest > vueKanbanAuxStylesDeMarculus FAILED` → `AssertionError at TypographieTest.kt:64` |
| F1 | voir la note | `EcranParametres.kt:151` : `modifier = Modifier.heightIn(min = 48.dp),` → `modifier = Modifier.heightIn(min = 30.dp),` | **MUETTE** (vert) | `BUILD SUCCESSFUL` (aucun échec) |
| F1c | `ThemeEnListeTest.kt:36` — les cibles du menu font au moins 48 dp et aucune taille de police en dur | `EcranParametres.kt:151` : `modifier = Modifier.heightIn(min = 48.dp),` → `modifier = Modifier.heightIn(min = 30.dp),` | **rouge** (1) échec | `ThemeEnListeTest > les cibles du menu font au moins 48 dp et aucune taille de police en dur FAILED` → `AssertionError at ThemeEnListeTest.kt:36` |
| F2 | voir la note | `EcranParametres.kt:141` : `Modifier.fillMaxWidth().heightIn(min = 48.dp) ⏎            …` → `Modifier.fillMaxWidth().heightIn(min = 20.dp) ⏎            …` | **MUETTE** (vert) | `BUILD SUCCESSFUL` (aucun échec) |
| F2c | `ThemeEnListeTest.kt:39` — les cibles du menu font au moins 48 dp et aucune taille de police en dur | `EcranParametres.kt:141` : `Modifier.fillMaxWidth().heightIn(min = 48.dp) ⏎            …` → `Modifier.fillMaxWidth().heightIn(min = 20.dp) ⏎            …` | **rouge** (1) échec | `ThemeEnListeTest > les cibles du menu font au moins 48 dp et aucune taille de police en dur FAILED` → `AssertionError at ThemeEnListeTest.kt:39` |
| F3 | `ThemeEnListeTest.kt:28` — aucun bouton radio dans Parametres | `EcranParametres.kt:145` : `Text(libelle(valeur), Modifier.weight(1f), style = Material…` → `Text(libelle(valeur) + "RadioButton", Modifier.weight(1f), …` | **rouge** (1) échec | `ThemeEnListeTest > aucun bouton radio dans Parametres FAILED` → `AssertionError at ThemeEnListeTest.kt:28` |
| G1 | `EcranPlacettesSourcesTest.kt:29` — une ligne de placette fait au moins 64 dp, la pastille est celle de la carte | `EcranPlacettes.kt:163` : `heightIn(min = 64.dp)` → `heightIn(min = 40.dp)` | **rouge** (1) échec | `EcranPlacettesSourcesTest > une ligne de placette fait au moins 64 dp, la pastille est celle de la carte FAILED` → `AssertionError at EcranPlacettesSourcesTest.kt:29` |
| G2 | `EcranPlacettesSourcesTest.kt:24` — l'ecran Placettes a le bandeau compact, la fleche de retour, la foret en titre … | `EcranPlacettes.kt:340` : `private val HAUTEUR_BOUTON = 56.dp` → `private val HAUTEUR_BOUTON = 56.dp ⏎ private val k2 = "font…` | **rouge** (1) échec | `EcranPlacettesSourcesTest > l'ecran Placettes a le bandeau compact, la fleche de retour, la foret en titre et… FAILED` → `AssertionError at EcranPlacettesSourcesTest.kt:24` |
| H1 | `BandeauCarteGuidageTest.kt:98` — le guidage garde la distance en 72 sp et aucun autre fontSize n'apparait ailleu… | `EcranCarte.kt:415` : `if (refusee) Text(stringResource(R.string.carte_permission)…` → `if (refusee) Text(stringResource(R.string.carte_permission)…` | **rouge** (1) échec | `BandeauCarteGuidageTest > le guidage garde la distance en 72 sp et aucun autre fontSize n'apparait ailleurs FAILED` → `AssertionError at BandeauCarteGuidageTest.kt:98` |
| H3 | `BandeauCarteGuidageTest.kt:78` — cinq boutons flottants comme Groma plus Controler, de 48 à 32 dp (missions 061,… | `EcranCarte.kt:575` : `SmallFloatingActionButton(onClick = { couchesRef[0]?.carte?…` → `androidx.compose.material3.FloatingActionButton(onClick = {…` | **rouge** (1) échec | `BandeauCarteGuidageTest > cinq boutons flottants comme Groma plus Controler, de 48 à 32 dp (missions 061, 012… FAILED` → `AssertionError at BandeauCarteGuidageTest.kt:78` |
| H4 | `BandeauCarteGuidageTest.kt:81` — cinq boutons flottants comme Groma plus Controler, de 48 à 32 dp (missions 061,… | `EcranCarte.kt:575` : `SmallFloatingActionButton(onClick = { couchesRef[0]?.carte?…` → `SmallFloatingActionButton(onClick = { couchesRef[0]?.carte?…` | **rouge** (9) échecs | `BandeauCarteGuidageTest > cinq boutons flottants comme Groma plus Controler, de 48 à 32 dp (missions 061, 012… FAILED` → `AssertionError at BandeauCarteGuidageTest.kt:81` |
| I1 | `RenduAppTest.kt:54` — le bouton est en bas des placettes, 56 dp, et le rendu tourne hors du fil princ… | `EcranPlacettes.kt:340` : `private val HAUTEUR_BOUTON = 56.dp` → `private val HAUTEUR_BOUTON = 40.dp` | **rouge** (1) échec | `RenduAppTest > le bouton est en bas des placettes, 56 dp, et le rendu tourne hors du fil principal FAILED` → `AssertionError at RenduAppTest.kt:54` |
| J1 | `NavigationTest.kt:55` — l'accueil n'a plus de bouton d'ouverture, les deux ouvertures sont dans le menu | `EcranTournees.kt:82` : `private val DOSSIER_PAR_DEFAUT: Uri =` → `private const val K = "Modifier.weight(1f).height(64.dp)" ⏎…` | **rouge** (1) échec | `NavigationTest > l'accueil n'a plus de bouton d'ouverture, les deux ouvertures sont dans le menu FAILED` → `AssertionError at NavigationTest.kt:55` |
| K2 | `BoussoleTest.kt:119` — la boussole ne connait pas la position, elle rend un CapMagnetique | `BoussoleTelephone.kt:36` : `class BoussoleTelephone(private val capteurs: SensorManager…` → `private val k2: io.github.pobsteta.surculus.core.gnss.Posit…` | **rouge** (1) échec | `BoussoleTest > la boussole ne connait pas la position, elle rend un CapMagnetique FAILED` → `AssertionError at BoussoleTest.kt:119` |
| K3 | `BoussoleTest.kt:109` — aucun fichier de l'application ne cite la classe du systeme qui calcule la decl… | `Ecoute.kt:59` : `const val DUREE_BANDEAU_MS = 6_000L` → `const val DUREE_BANDEAU_MS = 6_000L ⏎ private val k3: andro…` | **rouge** (1) échec | `BoussoleTest > aucun fichier de l'application ne cite la classe du systeme qui calcule la declinaison FAILED` → `AssertionError at BoussoleTest.kt:109` |
| K4 | `BoussoleTest.kt:149` — guidage et controle se servent de la meme moyenne, le decompte d'une seconde n'… | `Ecoute.kt:59` : `const val DUREE_BANDEAU_MS = 6_000L` → `const val DUREE_BANDEAU_MS = 6_000L ⏎ private suspend fun k…` | **rouge** (1) échec | `BoussoleTest > guidage et controle se servent de la meme moyenne, le decompte d'une seconde n'est ecrit qu'un… FAILED` → `AssertionError at BoussoleTest.kt:149` |
| L1 | `BancRenduTest.kt:57` — un effet qui revient d'un travail en entrees-sorties reprend sur le fil princip… | `BancRendu.kt:43` : `createEmptyComposeRule(effectContext = kotlinx.coroutines.t…` → `createEmptyComposeRule()` | **rouge** (1) échec | `BancRenduTest > un effet qui revient d'un travail en entrees-sorties reprend sur le fil principal FAILED` → `AssertionError at BancRenduTest.kt:57` |
| N1 | `RenduCarteTest.kt:130` — la carte ne deborde pas sur le bandeau | `EcranCarte.kt:420` : `BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clipT…` → `BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) { ⏎ …` | **rouge** (2) échecs | `RenduCarteTest > la carte ne deborde pas sur le bandeau FAILED` → `AssertionError at RenduCarteTest.kt:130` |
| N2 | `RenduCarteTest.kt:92` — carte debout sans plein ecran avec placette choisie | `EcranCarte.kt:605` : `modifier = Modifier.size(tailleBouton.dp), ⏎             ) …` → `modifier = Modifier.size(20.dp), ⏎             ) { ⏎       …` | **rouge** (8) échecs | `RenduCarteTest > carte debout sans plein ecran avec placette choisie FAILED` → `AssertionError at RenduCarteTest.kt:92` |
| N3 | `RenduCarteTest.kt:92` — carte debout sans plein ecran avec placette choisie | `EcranCarte.kt:552` : `Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding…` → `Modifier.align(Alignment.CenterStart).fillMaxHeight().paddi…` | **rouge** (8) échecs | `RenduCarteTest > carte debout sans plein ecran avec placette choisie FAILED` → `AssertionError at RenduCarteTest.kt:92` |
| P1 | `RenduLibellesCodesTerminerTest.kt:287` — forme du bouton de code - coins de 12 dp lus sur l'image rendue, pas une pilule | `FormesEtat.kt:68` : `val COIN_CODE = 12.dp` → `val COIN_CODE = 40.dp` | **rouge** (1) échec | `RenduLibellesCodesTerminerTest > forme du bouton de code - coins de 12 dp lus sur l'image rendue, pas une pil… FAILED` → `AssertionError at RenduLibellesCodesTerminerTest.kt:287` |
| P2 | `RenduLibellesCodesTerminerTest.kt:312` — code en gros et 4 par ligne debout en plein ecran | `FormesEtat.kt:65` : `const val CODES_PAR_LIGNE = 4` → `const val CODES_PAR_LIGNE = 3` | **rouge** (2) échecs | `RenduLibellesCodesTerminerTest > code en gros et 4 par ligne debout en plein ecran FAILED` → `AssertionError at RenduLibellesCodesTerminerTest.kt:312` |
| P3 | `RenduLibellesCodesTerminerTest.kt:287` — libelles des 15 codes couche en plein ecran - mots entiers, rien de rogne | `FormesEtat.kt:64` : `val BOUTON_CODE = 80.dp` → `val BOUTON_CODE = 52.dp` | **rouge** (5) échecs | `RenduLibellesCodesTerminerTest > libelles des 15 codes couche en plein ecran - mots entiers, rien de rogne FAILED` → `AssertionError at RenduLibellesCodesTerminerTest.kt:287` |
| P4 | `RenduLibellesCodesTerminerTest.kt:312` — code en gros et 4 par ligne debout en plein ecran | `EcranControle.kt:885` : `Text(c.code, style = MaterialTheme.typography.titleLarge, m…` → `Text(c.code, style = MaterialTheme.typography.titleSmall, m…` | **rouge** (4) échecs | `RenduLibellesCodesTerminerTest > code en gros et 4 par ligne debout en plein ecran FAILED` → `AssertionError at RenduLibellesCodesTerminerTest.kt:312` |
| P5 | `RenduLibellesCodesTerminerTest.kt:257` — libellés non rognés en hauteur (`didOverflowHeight`, aucune assertion avant correction) | `EcranControle.kt:926` : `if (texte.isNotEmpty()) Text(texte, style = base, maxLines …` → `if (texte.isNotEmpty()) Text(texte, style = base, maxLines …` | **MUETTE** (vert) | `BUILD SUCCESSFUL` (aucun échec) |
| P5c | `RenduLibellesCodesTerminerTest.kt:290` — libelles des 15 codes … mots entiers, rien de rogne | `EcranControle.kt:926` : `if (texte.isNotEmpty()) Text(texte, style = base, maxLines …` → `if (texte.isNotEmpty()) Text(texte, style = base, maxLines …` | **rouge** (5) échecs | `RenduLibellesCodesTerminerTest > libelles des 15 codes couche en plein ecran - mots entiers, rien de rogne FAILED` |
| P6 | `RenduLibellesCodesTerminerTest.kt:287` — libelles des 15 codes debout sans plein ecran - mots entiers, rien de rogne | `EcranControle.kt:922` : `fin in 1 until plein.length && plein[fin - 1].isLetterOrDig…` → `false` | **rouge** (3) échecs | `RenduLibellesCodesTerminerTest > libelles des 15 codes debout sans plein ecran - mots entiers, rien de rogne FAILED` → `AssertionError at RenduLibellesCodesTerminerTest.kt:287` |
| Q1 | `PlacetteIntrouvableEnFrancaisTest.kt:139` — guidage - placette absente de la tournee | `EcranGuidage.kt:134` : `Column(Modifier.padding(24.dp)) { Text(stringResource(R.str…` → `Column(Modifier.padding(24.dp)) { }` | **rouge** (5) échecs | `PlacetteIntrouvableEnFrancaisTest > guidage - placette absente de la tournee FAILED` → `AssertionError at PlacetteIntrouvableEnFrancaisTest.kt:139` |
| O1 | voir la note | `OuvertureSansPosition.kt:132` : `Button(onClick = onOuvrir, enabled = code.isNotBlank(), mod…` → `Button(onClick = onOuvrir, enabled = code.isNotBlank(), mod…` | **MUETTE** (vert) | `BUILD SUCCESSFUL` (aucun échec) |
| O1c | `RenduGuidageSansPositionTest.kt:339` — 9 - couche en plein ecran, bouton vu, rien de recouvert | `OuvertureSansPosition.kt:132` : `…height(DimensionsControle.ACTION))` → `…height(DimensionsControle.ACTION * 16))` | **rouge** (4) échecs | `RenduGuidageSansPositionTest > 9 - couche en plein ecran, bouton vu, rien de recouvert FAILED` → `AssertionError at RenduGuidageSansPositionTest.kt:339` |
| O2 | `RenduGuidageSansPositionTest.kt:319` — 8 - code present dans deux UG - l'UG est demandee par l'ecran de controle de la… | `OuvertureSansPosition.kt:131` : `if (suite == SuiteSansFix.PROPOSER) { ⏎         Button(onCl…` → `if (suite == SuiteSansFix.PROPOSER) { ⏎         androidx.co…` | **rouge** (8) échecs | `RenduGuidageSansPositionTest > 8 - code present dans deux UG - l'UG est demandee par l'ecran de controle de l… FAILED` → `AssertionError at RenduGuidageSansPositionTest.kt:319` |
| M1 | `RenduControleTest.kt:196` — bloc d'ecoute couche, il s'efface apres DUREE_BANDEAU_MS | `Ecoute.kt:193` : `return horloge() < fin ⏎` → `return true ⏎` | **rouge** (2) échecs | `RenduControleTest > bloc d'ecoute couche, il s'efface apres DUREE_BANDEAU_MS FAILED` → `AssertionError at RenduControleTest.kt:196` |
| M2 | `RenduControleTest.kt:309` — appui sur le bloc d'ecoute debout, rien ne se declenche dessous | `EcranControle.kt:792` : `.pointerInput(Unit) { awaitPointerEventScope { while (true)…` → `` | **rouge** (2) échecs | `RenduControleTest > appui sur le bloc d'ecoute debout, rien ne se declenche dessous FAILED` → `AssertionError at RenduControleTest.kt:309` |
| M3 | `RenduControleTest.kt:339` — commande la plus longue, debout sans plein ecran, entendu et compris entiers | `EcranControle.kt:765` : `Text(it, style = MaterialTheme.typography.bodyMedium, color…` → `Text(it, style = MaterialTheme.typography.bodyMedium, maxLi…` | **rouge** (6) échecs | `RenduControleTest > commande la plus longue, debout sans plein ecran, entendu et compris entiers FAILED` → `AssertionError at RenduControleTest.kt:339` |
| M4 | `RenduControleTest.kt:339` — commande la plus longue, debout sans plein ecran, entendu et compris entiers | `EcranControle.kt:769` : `Text(TextesEcoute.compris(it, erreur), style = MaterialThem…` → `Text(TextesEcoute.compris(it, erreur), style = MaterialThem…` | **rouge** (9) échecs | `RenduControleTest > commande la plus longue, debout sans plein ecran, entendu et compris entiers FAILED` → `AssertionError at RenduControleTest.kt:339` |
| M5 | `RenduControleTest.kt:446` — zone defilee jusqu'en bas, commande la plus longue, debout sans plein ecran, le… | `EcranControle.kt:793` : `.padding(12.dp), ⏎         verticalArrangement = Arrangemen…` → `.padding(30.dp), ⏎         verticalArrangement = Arrangemen…` | **rouge** (14) échecs | `RenduControleTest > zone defilee jusqu'en bas, commande la plus longue, debout sans plein ecran, le bloc rest… FAILED` → `AssertionError at RenduControleTest.kt:446` |
| M6 | `RenduControleTest.kt:446` — zone defilee jusqu'en bas, commande la plus longue, debout sans plein ecran, le… | `EcranControle.kt:793` : `.padding(12.dp), ⏎         verticalArrangement = Arrangemen…` → `.padding(12.dp), ⏎         verticalArrangement = Arrangemen…` | **rouge** (4) échecs | `RenduControleTest > zone defilee jusqu'en bas, commande la plus longue, debout sans plein ecran, le bloc rest… FAILED` → `AssertionError at RenduControleTest.kt:446` |
| M7 | `RenduControleTest.kt:446` — zone defilee jusqu'en bas, commande la plus longue, debout sans plein ecran, le… | `EcranControle.kt:764` : `Text(it, style = MaterialTheme.typography.headlineMedium, c…` → `Text(it, style = MaterialTheme.typography.displayLarge, col…` | **rouge** (15) échecs | `RenduControleTest > zone defilee jusqu'en bas, commande la plus longue, debout sans plein ecran, le bloc rest… FAILED` → `AssertionError at RenduControleTest.kt:446` |
| M8 | voir la note | `DispositionMicro.kt:20` : `val ZONE = 128.dp` → `val ZONE = 420.dp` | **MUETTE** (vert) | `BUILD SUCCESSFUL` (aucun échec) |
| M9 | `RenduControleTest.kt:276` — apres une phrase compris, couche plein ecran, entendu et compris entiers | `EcranControle.kt:801` : `OutlinedButton(onClick = onPoche, modifier = modifier.heigh…` → `OutlinedButton(onClick = onPoche, modifier = modifier.heigh…` | **rouge** (15) échecs | `RenduControleTest > apres une phrase compris, couche plein ecran, entendu et compris entiers FAILED` → `AssertionError at RenduControleTest.kt:276` |
| M10 | `RenduControleTest.kt:446` — zone defilee jusqu'en bas, commande la plus longue, debout sans plein ecran, le… | `EcranControle.kt:619` : `Modifier.align(Alignment.BottomStart).fillMaxWidth() ⏎     …` → `Modifier.align(Alignment.TopStart).fillMaxWidth() ⏎        …` | **rouge** (15) échecs | `RenduControleTest > zone defilee jusqu'en bas, commande la plus longue, debout sans plein ecran, le bloc rest… FAILED` → `AssertionError at RenduControleTest.kt:446` |
| M11 | voir la note | `EcranControle.kt:737` : `Text(stringResource(R.string.controle_poche_bande), style =…` → `Text(stringResource(R.string.controle_poche_bande), style =…` | **MUETTE** (vert) | `BUILD SUCCESSFUL` (aucun échec) |
| M12 | `RenduControleTest.kt:128` — mode poche debout en plein ecran, apres defilement | `EcranControle.kt:524` : `Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) …` → `Box(Modifier.padding(start = 2000.dp)) { ⏎                 …` | **rouge** (3) échecs | `RenduControleTest > mode poche debout en plein ecran, apres defilement FAILED` → `AssertionError at RenduControleTest.kt:128` |
| M13 | `RenduControleTest.kt:276` — apres une phrase compris, couche plein ecran, entendu et compris entiers | `EcranControle.kt:648` : `import androidx.compose.ui.unit.dp@@@Text(p.libelleEtat, st…` → `import androidx.compose.ui.unit.dp ⏎ import androidx.compos…` | **rouge** (29) échecs | `RenduControleTest > apres une phrase compris, couche plein ecran, entendu et compris entiers FAILED` → `AssertionError at RenduControleTest.kt:276` |

### Décisions de Pascal (2026-11-10)

Les **cinq correctifs sont gardés**.

1. Durcissement de `BancRendu.vu` (un bouton même partiellement rogné est refusé) : **gardé**. Il vaut pour les trois classes qui l'appellent.
2. Plan couché : le plancher de 48 dp est **gardé** (`DispositionControleTest.kt:19`). Les 88 dp de `RESERVE_HAUTEUR_PLAN_DP` restent la valeur
   réglée, sans être figés par un test.
3. `didOverflowHeight` (`RenduLibellesCodesTerminerTest.kt:257`) : **gardé**, le libellé reste entier. Un libellé de code qui ne tient pas
   en 2 lignes rend la CI rouge.
4. Cibles de 48 dp du menu du thème et test du bouton de 40 dp écrit en dur : les deux **gardés**.

### Sortie de `.claude/test-command`

_voir le corps de la PR (code de sortie et dernières lignes)._
