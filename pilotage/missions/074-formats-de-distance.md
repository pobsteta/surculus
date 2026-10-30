# Mission 074 — Une seule façon d'écrire une distance

> Fiche écrite par l'orchestrateur le 2026-10-29 (cycle 15), à partir du gabarit
> `imperatrice/skills/mission/fiche-mission.md`, sur la relecture de la 068 (PR #126) et les décisions de
> Pascal du 2026-10-29 (« garde U+202F », choix du cycle 15). Depuis `main` (093edf3). Une mission = une
> phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Écrire toutes les distances montrées au forestier d'une seule façon
française (mètres entiers arrondis, milliers séparés par U+202F, kilomètres à une décimale avec virgule
au-delà d'un seuil), pour qu'il lise la même chose au guidage, sur la carte et au contrôle.

**Redite par l'agent :** (à remplir par l'agent avant d'agir)

## Ce qui est constaté (relecture de la #126)

- `guidage_refus_15m` (`strings_tournees.xml` l. 123) : `%1$d` → « 9980 m », sans séparateur.
- `Libelles.distance` (`core/.../guidage/Libelles.kt`) : « 10,0 km » au-delà de 1 000 m, **tronqué**
  (`toInt`) au lieu d'arrondi en dessous.
- `metresEnFrancais` (`Verdict.kt`, 068) : « 9 980 m » avec U+202F (décision de Pascal : garder U+202F).
- Distances du pointage (`TextesControle.pointe`, « 6,2 m », une décimale, `Locale.FRANCE`) : précision
  voulue (pointage au décimètre), à garder.

## Ce qui est à faire

1. Recenser dans la PR toutes les distances affichées (grep des `m"`, `km`, `%d m`, `distance`) et dire pour
   chacune l'avant et l'après.
2. Une seule fonction (dans `:core`, sans Android) pour les distances de navigation : arrondi au mètre,
   U+202F aux milliers ; au-delà de 1 000 m, garder le passage en km existant de `Libelles.distance` (une
   décimale, virgule, arrondi), **ou** les mètres partout : le seuil et le choix sont une question de sens à
   poser à Pascal si le code actuel ne la tranche pas (ne pas trancher seul ; proposer).
3. Le guidage et le contrôle l'appellent ; la chaîne `guidage_refus_15m` reçoit le texte formaté (`%1$s`).
   Le pointage au décimètre ne change pas.
4. Tests d'abord, vus rouges : 0, 999,5, 1 000, 9 979,54, 10 049 m, et l'arrondi (plus de troncature).

**Temps de la main :** **appuyer**.

## L'Homme

- **Agent :** `dev`.
- **Ce qui ne lui est pas demandé :** les seuils de 15 m et 30 m, les textes autour des distances (sauf le
  `%d` → `%s`), le pointage.

## Harnais

- **Peut :** modifier `Libelles.kt`, `Verdict.kt` (`metresEnFrancais`), `TextesControle`, les écrans du
  guidage et leurs chaînes, leurs tests ; pousser sur `mission/074-formats-de-distance` ; ouvrir la PR.
- **Ne doit pas :** modifier `docs/`, `pilotage/` (sauf la redite), `fixtures/` ; toucher à `main` ; trancher
  une question de sens.

## Tests

- `.claude/test-command` au code 0 ; nouveaux tests vus **rouges** avant la correction.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. Critère d'arrêt : 3 allers-retours. **Fusion : autorisation de
  Pascal.**

## Comment saura-t-on que ça marche ?

Sur la release, à ~10 km de Couchey : guidage et arrivée du contrôle disent la même distance, sous la même
forme ; capture de l'orchestrateur.

## Reprise, aller-retour 1/3 (2026-10-30)

Relecture de la #131 (38f3680) : « prêt », avec un oubli. Décision de Pascal du 2026-10-30 : « Reprise de
la 074 ». Le harnais s'étend à `Verdict.kt` (l'alerte « hors rayon »).

1. L'alerte « hors rayon » (`core/.../placette/Verdict.kt` l. 69) écrit des `Double` bruts (« Plant à
   7.912345678901 m du piquet, au-delà du rayon de la placette (5.64 m + 1.0 m) ») : l'écrire au décimètre,
   virgule française, comme le pointage (« Plant à 7,9 m du piquet, au-delà du rayon de la placette (5,6 m +
   1,0 m) : confirmer le pointage. »), par une fonction de `:core` (pas de `Locale` d'Android). Test vu
   rouge.
2. Remplir la redite de cette fiche.
