# ADR 0008 — Style commun avec Marculus et Groma, par la bibliothèque `terrain`

- **Date :** 2026-10-22 (réécrite le même jour après la critique du `contradicteur`)
- **Statut :** proposée
- **Proposée par :** Claude (orchestrateur)
- **Tranchée par :** Pascal

## La décision en une phrase

Le style commun des trois applications est un **schéma de couleurs Material 3 complet**
(tous les rôles, en thème clair), tenu dans un module **Kotlin pur `terrain-style`** de la
bibliothèque `terrain` avec un test de contraste sur **chaque couple texte / fond** ;
Surculus le reprend tel quel, thème clair forcé, et garde à part sa palette d'état.

## Périmètre

Cette ADR ne traite **que du style** (décision de Pascal du 2026-10-22, sur la proposition
de l'orchestrateur après la critique). Le remplacement des copies de `groma-core` et de
l'application Groma par `terrain` (GNSS, géodésie, journal, GeoPackage, voix, carte) est
**renvoyé à une ADR ultérieure**, après l'essai d'automne 2026 et une table des écarts
module par module : `terrain` n'a ni la politique du fix de l'ADR 0003, ni le refus des
azimuts avant normalisation, ni les grades, ni la convergence, ni le modèle WMM, ni
EPSG:9794, et `terrain-journal` fusionne en « dernière écriture gagne » avec des UUID v4,
contre le journal en ajout seul et les UUID v7 de Surculus (ADR 0005) (critique du
`contradicteur`, journal du 2026-10-22).

## Contexte

**Pourquoi maintenant.** Pascal veut un style commun pour la cohérence du projet nemeton
(`docs/brief.md`, « Ma réponse d'abord »), qui ne passe jamais devant la lisibilité au
soleil (brief, « Style commun » ; spec § 3, « Affichage »), et qui vient de `terrain`, pas
d'une copie (journal du 2026-10-22, arbitrage 4).

**Ce qu'est aujourd'hui le style de Marculus.** Cinq rôles seulement sont fixés :
`primary` = `VertVif` #3E7A40, `onPrimary` = blanc, `primaryContainer` = `VertMarculus`
#374742, `onPrimaryContainer` = crème #F5EBD8, `secondary` = `VertMarculus` (en sombre :
crème) (`marculus/app/src/main/kotlin/io/github/pobsteta/marculus/ui/theme/Theme.kt`
l. 13-27, `87c3cf4`). **Tous les autres rôles sont ceux de Material 3 par défaut**, dont le
fond `surface` #FEF7FF et les violets de `tertiary` et de `secondaryContainer`. Le thème suit
le mode sombre du téléphone (`MainActivity.kt` l. 63). Groma en est une copie
(`groma/android/app/src/main/kotlin/groma/app/ui/Theme.kt` l. 10 et 32).

**Ce qu'est aujourd'hui l'affichage de Surculus.** `MaterialTheme` par défaut
(`app/.../MainActivity.kt` l. 42), plus une palette d'état en dur, en deux copies
(`app/.../carte/Palette.kt` l. 16-20 ; `app/.../essai/EcranEssaiGnss.kt` l. 73-77).

**`terrain`** v0.5.0 (`ee98731`) : cinq modules, tous Kotlin pur, JVM 17 ; les parties
Android restent dans chaque application (`terrain/README.md` l. 12-22). JitPack la construit
**sans ses tests** (`terrain/jitpack.yml`, `publishToMavenLocal -x test`). Construction
locale : `./gradlew publishToMavenLocal` (`terrain/README.md` l. 84). Groma ne la déclare
pas ; Marculus oui (`marculus/gradle/libs.versions.toml` l. 18 et 29-33).

### Inventaire des textes de Surculus (main après la PR #65)

Contrastes de luminance relative WCAG 2.2, calculés par l'orchestrateur (vérifiés par le
`contradicteur` sur fond blanc), contre le **fond réellement peint** : `surface` #FEF7FF de
Material 3, sauf mention. « Grand » = au moins 24 sp, ou 18,7 sp en gras (18 pt et 14 pt de
WCAG, en supposant 1 sp ≈ 1 px CSS : hypothèse).

| Texte | Où | Taille | Contraste | 7:1 | 1.4.6 tel qu'écrit |
|---|---|---|---|---|---|
| noir (`Palette.Noir`) | messages, cap de guidage, légende « inconnu » | 14-72 sp | 19,96 | oui | oui |
| texte par défaut (`onSurface` #1D1B20) | tous les écrans | — | 16,23 | oui | oui |
| rouge #B00020 | refus, hors France, sans position, légende « jamais » (`EcranCarte.kt` l. 86, 196, 198, 279 ; `EcranGuidage.kt` l. 70, 76, 150, 152, 216 ; `EcranTournees.kt` l. 125, 159) | 14-22 sp | **6,96** | **non** | non sous 18,7 sp gras, oui au-dessus |
| vert #0B6B2E | « arrivé », cap arrivé, rattaché, légende « à jour », ortho complète (`EcranGuidage.kt` l. 158, 162, 206 ; `EcranCarte.kt` l. 281 ; `EcranTournees.kt` l. 176) | 14-72 sp | **6,32** | **non** | oui pour l. 158 (72 sp) et 162 (24 sp gras) ; non pour la légende (14 sp) |
| orange #B45300 | rattachement par identité, à confirmer, anomalies, légende « travaux à prévoir », ortho partielle (`EcranGuidage.kt` l. 204, 218 ; `EcranCarte.kt` l. 280, 287 ; `EcranTournees.kt` l. 176) | 14-18 sp | **4,78** | **non** | non |
| blanc sur vert / orange / rouge | badge du fix (`carte/Commun.kt` l. 69-75, 18 sp gras), panneau de l'écran d'essai GNSS (`EcranEssaiGnss.kt` l. 267-279) | 18-44 sp | 6,65 / **5,03** / 7,33 | non / **non** / oui | oui / non (badge 18 sp) / oui |
| blanc sur `primary` par défaut #6750A4 | boutons (`MainActivity.kt` l. 76-84, `EcranGuidage.kt` l. 180-184) | — | **6,44** | **non** | non |
| blanc sur vert #0B6B2E | bouton de `EcranCarte.kt` l. 298 | — | 6,65 | non | non |
| texte d'un bouton désactivé (`onSurface` à 38 %) | rôle implicite de Material 3 | — | ≈ 2,3 (`contradicteur`) | non | exempté |

Pour comparaison, les couleurs de Marculus sur `surface` : `VertMarculus` 9,32 ; `VertVif`
4,91 ; blanc sur `VertMarculus` 9,80 ; blanc sur `VertVif` 5,17 (bouton principal de
Marculus et de Groma).

Les **pastilles** des placettes (mission 033) ne sont pas du texte : WCAG 2.2, 1.4.11, leur
demande 3:1 **contre les couleurs adjacentes**, ici l'ortho et le Plan IGN, pas du blanc. Ce
contraste n'est mesuré contre aucun fond réel.

## Options

| Option | Ce que c'est | Pour | Contre |
|---|---|---|---|
| A | Module **Android Compose** `terrain-theme` (un `MaterialTheme` partagé) | Une seule source prête à l'emploi | Contraire à la règle « Kotlin pur » de `terrain` (README l. 20) ; construction Android sur JitPack ; impose Compose et sa version aux trois applications |
| **B étendue (proposée)** | Module Kotlin pur `terrain-style` : **tous les rôles** du schéma Material 3 en ARGB (`Long`), en clair (et en sombre si Marculus le garde) ; un test parcourt **tous les couples** rôle « on » / rôle de fond, plus les couleurs d'état ; chaque application recopie les rôles 1:1 dans son `ColorScheme`, et un test de l'application vérifie l'égalité avec les jetons | Style commun réel (les rôles, pas seulement trois couleurs) ; testable en JVM ; respecte la règle de `terrain` | Une adaptation de quelques dizaines de lignes par application, tenue par son test d'égalité |
| **E (avec B)** | Les valeurs des rôles sont **générées** une fois par Material Color Utilities (Google, Apache-2.0) depuis la couleur de départ `VertMarculus`, au niveau de contraste 0,5 (« medium »), puis figées dans `terrain-style` | Convention publiée plutôt qu'invention : à ce niveau, `onPrimary`, `onSurface` et `onError` visent 11:1, `onSurfaceVariant` et `onPrimaryContainer` 7:1 (`ColorSpec2021.java`, `ContrastCurve.java`) | Publication Maven de la bibliothèque **non vérifiée** ; teintes qui s'écartent des trois couleurs actuelles |
| C | Troisième copie du `Theme.kt` de Marculus | Immédiat | Écartée par Pascal le 2026-10-22 |
| D | Cohérence par le nom, l'icône, la typographie et les formes seulement | Aucun risque au soleil | Ne donne pas les couleurs communes demandées |

**La palette d'état** (rouge, orange, vert, noir des pastilles, du badge et des alertes)
reste propre à Surculus (spec § 3). Là où elle porte du **texte**, la règle de contraste
s'y applique comme au reste : la mission devra foncer les variantes « texte » de ces
couleurs, ou écrire en noir à côté d'une pastille de couleur, **sans changer les pastilles**
de la mission 033.

## Le crible FRANCE

| Lettre | Question | Réponse (une ligne) | Ce que ça coûte | Prêts à payer ? |
|---|---|---|---|---|
| **F**rugale | La plus simple qui tienne ? | Des jetons en `Long` et un test JVM ; pas d'Android dans `terrain` | Quelques dizaines de lignes d'adaptation par application | à trancher par Pascal |
| **R**esponsable | Qui fait quoi ? | Pascal maintient `terrain` ; l'orchestrateur écrit le brief vers `terrain` ; un `dev` de Surculus fait l'adaptation, le `testeur` le test d'égalité depuis cette ADR | `terrain` n'a ni `testeur` séparé ni `relecteur` (règles de ce dépôt seulement) ; aucun successeur désigné pour `terrain` | question 3 à Pascal |
| **A**nticiper | Si l'imprévu arrive ? | Version épinglée par **SHA de commit** (JitPack accepte un commit comme version), pas par tag ; repli par `publishToMavenLocal` sans réseau ; comme JitPack ne joue pas les tests, le test de contraste est **rejoué dans Surculus** sur les jetons reçus | Une étape de plus à chaque montée de version | à trancher par Pascal |
| **N**ovateur | Toutes les idées ? | Cinq options, dont la génération par Material Color Utilities | — | — |
| **C**oût | Que coûte-t-elle à l'ensemble ? | Une mission dans `terrain` (`terrain-style`), puis une mission Surculus en « appuyer » après le terrain de Pascal ; Marculus et Groma suivent s'ils le veulent | Aucun délai sur la 012 ; rien avant le retour de Couchey | à trancher par Pascal |
| **É**cologique | Que réutilise-t-on, qui réutilisera ? | `terrain` déjà publiée ; une convention de Google pour les rôles ; Marculus et Groma peuvent prendre les mêmes jetons | Groma ne consomme pas encore `terrain` : « commune » à deux applications aujourd'hui | à trancher par Pascal |

## Valeurs sacrifiées

- **Le mode sombre** dans Surculus : thème clair forcé (Pascal, 2026-10-22).
- **L'identité exacte avec Marculus, si Marculus ne change pas** : blanc sur `VertVif`
  (5,17:1) ne tient pas la règle de contraste ; soit Marculus et Groma prennent les mêmes
  jetons, soit les boutons de Surculus sont d'un autre vert (question 1).
- **La correction unique des autres briques**, d'ici l'ADR ultérieure : les copies restent.

## Ce qui reste à trancher par Pascal

1. **Les couleurs de Marculus et Groma** : si `VertVif` ne porte plus de texte blanc,
   Marculus et Groma prennent-ils les mêmes jetons (briefs vers eux), ou acceptes-tu que
   les boutons de Surculus soient d'un autre vert ?
2. **La règle de contraste** : 7:1 pour **tout** texte, grand texte, badge et éléments
   désactivés compris (règle propre au projet, plus stricte que la norme) ; ou **WCAG 2.2,
   1.4.6 tel qu'écrit** (7:1, mais 4,5:1 pour le grand texte, éléments désactivés exemptés).
3. **Le successeur de `terrain`** : qui, à part toi, peut la maintenir et la reconstruire
   si JitPack échoue ou si tu passes la main ?
4. **L'option** : B étendue seule, ou B avec E (rôles générés par Material Color
   Utilities), sous réserve que sa publication soit vérifiée (`acquereur`).

Et une proposition : pendant le terrain de Couchey, un premier essai au soleil de la
palette actuelle (badge du fix, texte orange, pastilles sur l'ortho), noté au journal, avant
la mission plutôt qu'après.

## Sources

- `terrain/README.md` l. 12-22 et 84 ; `terrain/jitpack.yml` (tag v0.5.0, `ee98731`).
- `marculus/app/src/main/kotlin/io/github/pobsteta/marculus/ui/theme/Theme.kt` l. 10-39 ;
  `marculus/app/src/main/kotlin/io/github/pobsteta/marculus/MainActivity.kt` l. 63 ;
  `marculus/gradle/libs.versions.toml` l. 18 et 29-33 (`87c3cf4`).
- `groma/android/app/src/main/kotlin/groma/app/ui/Theme.kt` l. 10 et 32.
- Surculus (main après la PR #65) : fichiers et lignes du tableau d'inventaire.
- Material 3, jetons par défaut : `ColorLightTokens.kt`, `PaletteTokens.kt` (androidx,
  branche androidx-main ; version de la BOM de Surculus non vérifiée).
- Material Color Utilities : `ColorSpec2021.java`, `ContrastCurve.java`
  (github.com/material-foundation/material-color-utilities).
- [WCAG 2.2](https://www.w3.org/TR/WCAG22/), critères 1.4.3, 1.4.6 et 1.4.11.
- Critique du `contradicteur` sur la première version de cette ADR, et décision de Pascal
  de la réécrire limitée au style : `pilotage/journal.md`, 2026-10-22.

## Question de contrôle

Cette décision tient-elle nos six valeurs pour le **forestier en forêt** (soleil, gants,
une main libre) d'abord, puis pour le **successeur** qui reprendra les trois applications,
et que sommes-nous prêts à payer pour cela ?
