# Mission 068 — Le message d'arrivée en français, sans code ni décimales

> Fiche écrite par l'orchestrateur le 2026-10-29 (cycle 14), à partir du gabarit
> `imperatrice/skills/mission/fiche-mission.md`, sur l'ordre de Pascal du 2026-10-29 : « change
> h_15_m et les 10 décimales après la virgule quand tu attends un fix ». Depuis `main` (b73c493).
> Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Montrer à l'arrivée au piquet un message en français, sans
code technique et avec une distance arrondie au mètre, pour que le forestier comprenne d'un coup
d'œil, debout dans la parcelle, pourquoi le contrôle ne s'ouvre pas encore et quoi faire.

**Redite par l'agent :** (à remplir par l'agent avant d'agir)

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
