# Mission 101 — Une seconde ouverture le même jour reprend le contrôle déjà ouvert

> Fiche écrite par l'orchestrateur le 2026-11-07, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> après la décision de Pascal du même jour (relecture de la #167 : « Oui, mission à part »). Depuis `main` (f9c9924).
> Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Faire que la règle d'ouverture sans position refuse d'ouvrir un second
contrôle pour une placette déjà ouverte ce jour-là et rende le contrôle existant, sans nouvelle ligne au journal, pour
que « un contrôle par placette et par jour » soit tenu par les données et non par le seul écran.

**Redite par l'agent :** _testeur_ : je dois écrire les tests qui diront si, pour une même placette et un même jour, la seconde ouverture sans position reprend le contrôle déjà ouvert au lieu d'en ouvrir un autre, sans ligne de plus au journal, afin que « un contrôle par placette et par jour » soit tenu par les données et non par le seul écran.

## Le constat

- Mission 100 (#167, f9c9924) : le `testeur` a montré, au niveau de la règle (`DoubleOuvertureSansPositionTest`), que deux
  appels de suite à l'ouverture sans position confirmée rendent tous deux « ouvert » et laissent une ligne en trop au
  journal (3 au lieu de 2), sans exception ; un seul contrôle reste courant pour la placette ce jour-là. Seul le
  `VerrouOuverture` de l'écran l'empêche aujourd'hui.
- Règle (spec § 4, § 6 l. 648) : un contrôle par (placette, jour civil Europe/Paris) dans une tournée.

## Décision de Pascal (2026-11-07)

**« Oui, mission à part »** : la seconde ouverture le même jour reprend le contrôle déjà ouvert, sans nouvelle ligne au
journal.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | en partie : où vit la règle, et le cas avec position | **tenir** : le `dev` rend un plan d'abord |
| A-t-il les moyens ? | oui : `BancControle`, `DoubleOuvertureSansPositionTest` | le `testeur` écrit d'abord les tests |

**Temps de la main :** tenir (justesse des données du journal).

## Ma réponse d'abord

_Rempli par Pascal seul._

## L'Homme

- **Agents :** `testeur` d'abord (sans lire `SessionControle.kt`, `ArriveeControle.kt`, `ArriveeSansPosition.kt` ni
  `OuvertureSansPosition.kt` ; il peut lire les tests existants pour leurs bancs), puis `dev` (opus), plan d'abord.
- **Ce qui ne lui est pas demandé :** l'écran (le verrou de la 100 reste) ; « Annuler » pendant une ouverture
  (Pascal : « Laisser ainsi », 2026-11-07).

## Questions au plan du `dev` (l'orchestrateur les posera à Pascal)

1. L'ouverture **avec** position (règle des 15 m) a-t-elle le même défaut ? Si oui, est-elle dans la mission ?
2. Que rend la règle à la seconde ouverture : le contrôle existant, ou un refus nommé que l'écran traduit ?
3. Un contrôle déjà **terminé** ce jour-là : la seconde ouverture le reprend-elle (spec : « Placette déjà terminée : ce
   que vous dites s'ajoute ») ?

## Harnais

- **Testeur peut :** tests dans `core/src/test/` et `app/src/test/` ; vus rouges.
- **Dev peut :** la règle d'ouverture (`core/` et `app/.../controle/`), ses tests.
- **Ne doivent pas :** toucher à `docs/`, à `main`, au format du journal ; réécrire les tests de l'autre (le constat
  de `DoubleOuvertureSansPositionTest` peut devenir faux : le `testeur` le met à jour lui-même).

## Valeur provisoire (chiffre, réglage)

- Aucune.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** 100 (#167, f9c9924). **PR en brouillon jusqu'à :** la fin du `dev`. **CI rouge attendue :** les tests
  du `testeur`, entre son push et celui du `dev`.

## Tests

- Deux ouvertures sans position confirmées de suite, même placette, même jour : un seul contrôle, une seule écriture ;
  la seconde rend le contrôle existant.
- Une autre placette, ou le lendemain : une nouvelle ouverture reste permise.
- Après redémarrage (journal relu) : la seconde ouverture reprend toujours le contrôle du jour.
- `.claude/test-command` au code 0 ; CI verte.

## Terrain

- Aucun nouvel écran ; l'essai du double appui de la 100 vaut aussi pour celle-ci.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Arrêt de la relecture :** 3 allers-retours. **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Quoi que fasse l'écran, le journal ne porte qu'une ouverture par placette et par jour.
