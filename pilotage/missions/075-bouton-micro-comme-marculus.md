# Mission 075 — Le bouton micro comme celui de Marculus

> Fiche écrite par l'orchestrateur le 2026-10-29 (cycle 15), à partir du gabarit
> `imperatrice/skills/mission/fiche-mission.md`, sur la décision de Pascal du 2026-10-29 (PR #123 :
> « Fusionner l'icône seule », la place et l'aspect « partent ensemble dans une mission à part ») et son
> choix du cycle 15. Depuis `main` (093edf3). Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Donner au bouton micro hors poche la place et l'aspect de celui de
Marculus (à portée sans faire défiler, rond, rouge pendant l'écoute, barré quand le micro ou le modèle
manquent) et empêcher l'écran de sauter sous le doigt à l'apparition de « J'ÉCOUTE », pour que le forestier,
une main gantée, tienne le micro du premier coup.

**Redite par l'agent :** (à remplir par l'agent avant d'agir)

## Ce qui est constaté

- Capture v0.7.80 et v0.7.83 : le bouton micro est sous le pli, sous « + Pointer », « Annuler »,
  « Terminer » ; il faut faire défiler le contrôle pour le voir (journal du 2026-10-29).
- Relecture de la #123 : Marculus a un bouton rond flottant de 112 dp, icône de 48 dp, rouge (`error`) à
  l'écoute, `MicOff` quand le micro n'est pas prêt (`~/dev/marculus/app/src/main/kotlin/io/github/pobsteta/
  marculus/ui/feuille/DicteeVocale.kt`, l. 340 à 400) ; Surculus : bouton à contour de 56 dp, icône 32 dp,
  `primary` à l'écoute, pas de `MicOff`.
- Capture v0.7.83 (c07, c08) : quand « J'ÉCOUTE » s'affiche, tout le contrôle descend d'environ 190 px
  pendant l'appui ; le bouton peut glisser hors du doigt (déjà un défaut bloquant de la #121 : « le bouton
  micro change de composable sous le doigt »).

## Ce qui est à faire

**Temps de la main : tenir.** D'abord un **plan sans code** (`pilotage/missions/075-plan.md`) :
1. où placer le bouton, debout et couché, pour qu'il soit visible sans défiler, sans cacher les boutons
   d'état, Pointer, Annuler, Terminer ni le plan (rappel de la 067 : plan couché sous les boutons) ;
2. forme, taille, couleurs (rouge à l'écoute, `MicOff`), mesurées sur Marculus, avec les contrastes de
   l'ADR 0008 ;
3. comment « J'ÉCOUTE » s'affiche sans déplacer le bouton sous le doigt ;
4. les questions de sens pour Pascal, une par une, chacune avec une proposition.
L'orchestrateur soumet le plan à Pascal ; le code ne part qu'après ses réponses, dans la même branche.

## L'Homme

- **Agent :** `afficheur`.
- **Ce qui ne lui est pas demandé :** la machine d'écoute, le mode poche (pas de bouton en poche, décision
  du 2026-10-28), les textes de la dictée.

## Harnais

- **Peut :** lire tout Surculus et Marculus ; écrire le plan ; pousser sur
  `mission/075-bouton-micro-comme-marculus`. Puis, après les réponses de Pascal, modifier `EcranControle.kt`
  et ses tests, ouvrir la PR.
- **Ne doit pas :** écrire du code avant les réponses de Pascal ; modifier `docs/`, `fixtures/`, Marculus ;
  toucher à `main` ; trancher une question de sens.

## Tests

- Au code : `.claude/test-command` au code 0 ; tests vus **rouges** (position du bouton indépendante du
  bandeau, couleurs selon l'état) ; limites des tests sans Compose dites dans la PR.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. Critère d'arrêt : 3 allers-retours. **Fusion : autorisation de
  Pascal.**

## Comment saura-t-on que ça marche ?

Sur la release, capture debout et couchée : le micro visible sans défiler ; pendant l'appui, il ne bouge pas
et passe au rouge ; essai de Pascal avec des gants.

## Reprise, aller-retour 1/3 (2026-10-29)

Relecture de la #130 (2229a8d) : « à reprendre ».

1. **Majeur** : calculer l'état du micro (`AspectMicro.pour(EcouteApp.affichage, …)`) **dans `BoutonMicro`,
   après la lecture de `EcouteApp.version`** (état Compose), comme avant la 075 ; sinon le rouge à l'écoute
   risque de ne jamais s'afficher. Garde de source qui le vérifie.
2. Réindenter le corps de la zone qui défile (`EcranControle.kt`), dans un commit à part.
3. Poser `zone` une seule fois (micro en frère conditionnel) pour ne pas recréer le contenu en passant de
   Plant à Pointer ou Terminer, si c'est simple ; sinon le dire.
4. Remplacer le test tautologique `ECOUTE_EN_SURIMPRESSION` par une garde de source : `BlocEcouteEncadre`
   hors de la `Column` qui défile.
5. PR : Réglages et Installer sont **dans** la zone qui défile, pas dessous ; couché, « J'ÉCOUTE » couvre
   aussi « Mode poche » (suite de Q2, à dire).

## Reprise, aller-retour 2/3 (2026-10-29)

Relecture de la reprise (3edcebd) : « à reprendre », en léger. Le reste est tenu.

1. Le corps de la `Column` qui défile (`EcranControle.kt` l. 375 à 389, de `Text(stringResource(
   R.string.controle_source…` à `if (stockageRefuse)`) est encore aligné sur son accolade fermante : +4
   espaces, commit à part, `git diff -w` vide.
2. Renforcer la garde de source : `BlocEcouteEncadre()` est **dans le corps du `Box` de `zone`, après la
   `Column`, avec `Modifier.align(`** (la garde actuelle ne verrait pas un frère remis au-dessus, le défaut
   d'origine). La voir rouge en remettant le bloc au-dessus de la `Column` (puis revenir).
