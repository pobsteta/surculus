# Mission 068 — Le message d'arrivée en français, sans code ni décimales

> Fiche écrite par l'orchestrateur le 2026-10-29 (cycle 14), à partir du gabarit
> `imperatrice/skills/mission/fiche-mission.md`, sur l'ordre de Pascal du 2026-10-29 : « change
> h_15_m et les 10 décimales après la virgule quand tu attends un fix ». Depuis `main` (b73c493).
> Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Montrer à l'arrivée au piquet un message en français, sans
code technique et avec une distance arrondie au mètre, pour que le forestier comprenne d'un coup
d'œil, debout dans la parcelle, pourquoi le contrôle ne s'ouvre pas encore et quoi faire.

**Redite par l'agent :** À l'arrivée au piquet, afficher un message en français, sans motif technique ni décimales (distance au mètre, « 9 980 m »), qui dit quoi faire avec le champ « Code lu sur le piquet » et le bouton dessous, pour que le forestier comprenne d'un coup d'œil pourquoi le contrôle ne s'ouvre pas. Reprise 1/3 : remettre dans les tests le refus exact attendu, faire suivre le libellé du bouton au champ (« Rattacher » / « Recommencer »), mettre les commentaires à jour.

## Ce qui est constaté

Capture de l'orchestrateur, v0.7.83, 2026-10-29 (et déjà v0.7.77, journal du 2026-10-28) :

```
hors_15_m : Placette la plus proche à 9979.542810305791 m (plus de 15 m) : confirmer l'identité par le code du piquet.
```

- Le code `hors_15_m` vient de `ControleurPlacette.kt` l. 374 (`TextesControle.message`) :
  `"${suite.code} : ${suite.message}"`. **Tous** les refus de session passent par cette ligne : le motif
  (identifiant stable, lu par les fixtures et le journal) s'affiche devant chaque message.
- Les décimales viennent de `Verdict.kt` l. 186 (`RefusPlacette.HorsQuinzeMetres`) : `$distanceM`
  interpolé tel quel (point décimal anglais, douze décimales).
- Le guidage dit déjà la même chose proprement : `strings_tournees.xml` l. 123, `guidage_refus_15m`
  (« À %1$d m de la placette la plus proche (plus de 15 m). Lisez le code sur le piquet, saisissez-le,
  puis rattachez. »).

## Ce qui est à faire

1. À l'écran du contrôle, ne plus afficher le motif devant le message d'un refus (ligne 374) : le motif
   reste dans les données, le journal et les tests ; seul le texte change. Cela vaut pour tous les refus
   de cette ligne (même cause), pas seulement `hors_15_m`.
2. Distance de `HorsQuinzeMetres` arrondie au mètre entier, en français (espace fine insécable des
   milliers, comme `Locale.FRANCE` : « 9 980 m »). Formulation alignée sur `guidage_refus_15m` ; la
   consigne doit dire quoi faire avec l'écran tel qu'il est (le champ « Code lu sur le piquet » et le
   bouton sous lui).
3. Tests d'abord, vus rouges : le message affiché pour un refus à 9 979,54 m ne contient ni `hors_15_m`,
   ni `_`, ni plus de chiffres que l'entier, et contient « 9 980 m » (ou la forme choisie, dite dans la
   PR) ; un autre refus (par ex. `code_inconnu`) ne montre pas non plus son motif. Adapter
   `ArriveeControleTest` l. 63 (qui exige aujourd'hui le préfixe `hors_15_m : `) en le disant dans la PR.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | |
| A-t-il les moyens ? | oui | |

**Temps de la main :** **appuyer**.

## L'Homme

- **Agent :** `dev`.
- **Ce qui ne lui est pas demandé :** changer les motifs (identifiants stables), les seuils (15 m, 30 m),
  la règle d'arrivée, la moyenne de 5 s, le libellé « Recommencer » ou la disposition de l'écran
  (à signaler s'il gêne la consigne, pas à trancher) ; toucher `fixtures/`.

## Harnais

- **Peut :** modifier `Verdict.kt` (le texte de `HorsQuinzeMetres.message`), `ControleurPlacette.kt`
  (`TextesControle`), les chaînes de l'app et leurs tests ; pousser sur
  `mission/068-message-arrivee-en-francais` ; ouvrir la PR.
- **Ne doit pas :** modifier `fixtures/`, `docs/`, `pilotage/` (sauf la redite de cette fiche) ; toucher
  à `main` ; trancher une question de sens.

## Tests

- `.claude/test-command` au code 0 ; les nouveaux tests vus **rouges** avant la correction.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. Critère d'arrêt : 3 allers-retours.
- **Qui tranche :** Pascal. **Fusion : autorisation à demander à Pascal.**

## Comment saura-t-on que ça marche ?

Sur la release suivante, à l'intérieur (loin de Couchey), ouvrir le contrôle de P35-03 : le message
dit « À 9 980 m … » (ou la forme retenue), sans `hors_15_m` ni décimales ; capture de l'orchestrateur.

## Reprise, aller-retour 1/3 (2026-10-29)

Relecture de la #126 (1b954d1) : « à reprendre ». Décision de Pascal du 2026-10-29, qui lève ici
l'interdiction de toucher au libellé du bouton : « Selon le champ ».

1. **Bloquant (relecteur)** : dans `ControleurPlacetteTest` (l. 133) et `PointageControleTest` (l. 82,
   233, 271), exiger de nouveau *quel* refus est renvoyé : le message exact attendu
   (`assertEquals(RefusPlacette.CauseSurEtat(...).message, m.texte)` ou équivalent), pas seulement
   l'absence du motif.
2. **Pascal** : le bouton sous « Code lu sur le piquet » dit « Rattacher » dès que le champ contient un
   code (non vide après `trim`), « Recommencer » quand il est vide (y compris après « Pas de position
   GNSS ») ; même action qu'aujourd'hui. Le message du refus des 15 m dit « … puis touchez
   « Rattacher » ». Test : le libellé suit le champ.
3. Mineurs : commentaire de `VueArrivee` (`EcranControle.kt` l. 205, « le refus avec son code ») à
   mettre à jour ; commentaire du formatage de la distance sans « comme `Locale.FRANCE` » (U+202F choisi,
   dire pourquoi) ; remplir la redite de cette fiche.

Hors reprise (bac à sable) : formats de distance différents dans l'app (« 9980 m » du guidage,
« 10,0 km » de `Libelles.distance` tronqué, « 9 980 m » ici) ; messages de refus qui montrent encore
`non_observe` ou des UUID.
