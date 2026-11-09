# Mission 102 — Les libellés des codes lisibles dans la grille de « Terminer »

> Fiche écrite par l'orchestrateur le 2026-11-07, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> après la décision de Pascal du même jour (cycle 20, « Libellés rognés à « Terminer » »). Depuis `main` (4cdff95).
> Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Faire que le début du libellé sous chaque code de la grille du besoin, à
« Terminer », se lise en entier, sans mot coupé ni texte rogné par le bord du bouton, pour que l'agent reconnaisse le
code au soleil et avec des gants.

**Redite par l'agent :** _(testeur, 2026-11-07)_ Je dois écrire les tests qui échouent tant que, à « Terminer », le début du libellé
sous chacun des codes de la grille ne se lit pas en entier (aucun mot coupé, rien de rogné ni de débordant du bouton, à chaque
taille debout et couché, avec et sans plein écran), le code restant gros et la grille à 4 par ligne debout, sans figer la façon
d'y arriver. _(L'`afficheur` redira la sienne avant d'agir.)_

## Le constat (vérifié par l'orchestrateur)

- Capture du 2026-11-07, v0.7.111, P35-01 de la Colombière, téléphone debout : sous les codes, « Préparation / du… »,
  « Plantation / en… », « Cloisonneme / nt… », « Entretien / des… » ; la seconde ligne est coupée au milieu d'un mot et
  rognée par le bord rond du bouton. Déjà noté au bac à sable depuis la v0.7.109 (091).
- Code (`main`, 4cdff95) : `EcranControle.kt` l. 873-877 (`ChoixBesoinEtConcurrence`) : 4 codes par ligne, bouton de
  `DimensionsControle.BOUTON_CODE`, code en `titleLarge`, libellé court en `labelSmall`, `maxLines = 2` ;
  `ControleurPlacette.libelleCourt` (l. 486-498) coupe au dernier mot entier dans 18 caractères
  (`LIBELLE_COURT_MAX`), puis ajoute « … ». Un mot plus large que le bouton (« Cloisonnement ») est coupé par Compose.
- Les libellés viennent de la couche `codes` du `travaux.gpkg` (sommieR) ; Surculus n'en invente aucun.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | plusieurs façons (une ligne, coupe aux mots, taille, forme du bouton) : choix d'écran | **tenir** : l'`afficheur` rend un plan avec des options |
| A-t-il les moyens ? | oui : `BancRendu` (tailles debout et couché), les 15 codes de la tournée d'essai | le `testeur` écrit d'abord les tests |

**Temps de la main :** tenir (écran de terrain).

## Ma réponse d'abord

_Rempli par Pascal seul._

## L'Homme

- **Agents :** `testeur` d'abord (tests depuis cette fiche et la spec, **sans lire** `EcranControle.kt` ni
  `ControleurPlacette.kt` ; il peut lire les tests de rendu existants, dont ceux de la 091), puis `afficheur` (plan
  d'abord, puis code).
- **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** changer les codes ou leurs libellés (sommieR) ; changer la taille des boutons sous
  le minimum ganté ; la concurrence ou « Aucun ».

## Décision de Pascal sur le plan de l'`afficheur` (2026-11-07)

- **« Coins arrondis, 2 lignes »** (option C) : les boutons de la grille des codes passent d'une forme pleinement
  arrondie (debout 82 × 80 dp, quasi-cercle de rayon 40 dp) à un rectangle aux coins de 12 dp (`COIN_CODE`), même
  encombrement (80 dp), code en `titleLarge` inchangé ; « Aucun » et la concurrence gardent leur forme. Le libellé
  (préfixe « CODE - » retiré) est coupé aux mots selon la largeur mesurée, sur deux lignes, « … » seulement s'il est
  coupé ; libellé à 11 sp, 10 sp au plus petit, jamais un mot coupé.
- `LIBELLE_COURT_MAX` (18 caractères, non mesuré) disparaît ; `BesoinTerminerDevTest` l. 47-52, qui figeait ce détail,
  est adapté par l'`afficheur` (choix technique de l'orchestrateur, conséquence de la décision) et dit dans la PR.
- Le `testeur` remplace son coin provisoire de 16 dp par `COIN_CODE_DP = 12` (décision) et ajoute un test qui garde la
  forme.
- À juger sur la capture : « Dégagement / (manuel… » (mot entre parenthèses).

## Harnais

- **Testeur peut :** tests dans `app/src/test/` ; vus rouges.
- **Afficheur peut :** la grille de `ChoixBesoinEtConcurrence`, `libelleCourt` et ses constantes, `DimensionsControle`
  si besoin ; ses tests.
- **Ne doivent pas :** toucher à `docs/`, à `main` ; réécrire les tests de l'autre.

## Valeur provisoire (chiffre, réglage)

- **18 caractères** (`LIBELLE_COURT_MAX`) : réglage actuel, non mesuré ; la mission le remplace par une règle vérifiée
  au rendu, aux tailles debout et couché.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** 091. **PR en brouillon jusqu'à :** la fin de l'`afficheur`. **CI rouge attendue :** les tests du
  `testeur`.

## Tests

- Avec les 15 codes de la tournée d'essai (libellés sommieR), à chaque taille (debout, couché, avec et sans plein
  écran) : aucun libellé coupé au milieu d'un mot ; aucun texte qui déborde du bouton.
- Le code reste lisible en gros ; la grille reste à au moins 4 par ligne debout, sinon le dire.
- `.claude/test-command` au code 0 ; CI verte.

## Terrain

- **Échantillon :** sur la release, « Terminer » sur P35-01 de la Colombière : capture de la grille, debout et couché ;
  Pascal juge au soleil.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Arrêt de la relecture :** 3 allers-retours. **Qui tranche :** Pascal
  (option d'affichage, fusion).

## Comment saura-t-on que ça marche ?

Au soleil, l'agent lit sous chaque code un début de libellé entier.
