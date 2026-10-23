# ADR 0008 — Style commun avec Marculus et Groma, par la bibliothèque `terrain`

- **Date :** 2026-10-22 (réécrite le même jour après la critique du `contradicteur`)
- **Statut :** acceptée (Pascal, 2026-10-23 : « Je valide les 6 propositions »), sans successeur de `terrain`
  pour l'instant (risque assumé, question 3) ; coûts du crible F, A, C et É à confirmer par
  Pascal
- **Proposée par :** Claude (orchestrateur)
- **Tranchée par :** Pascal

## La décision en une phrase

Le style commun des trois applications est un **schéma de couleurs Material 3 complet**
(tous les rôles, en thème clair), **généré une fois par Material Color Utilities depuis
`VertMarculus` puis figé**, tenu dans un module **Kotlin pur `terrain-style`** de la
bibliothèque `terrain` avec un test de contraste **WCAG 2.2, 1.4.6** sur **chaque couple
texte / fond** ; Surculus le reprend tel quel, thème clair forcé, et garde à part sa
palette d'état ; Marculus et Groma prennent les mêmes jetons.

## Périmètre

Cette ADR ne traite **que du style** (décision de Pascal du 2026-10-22, sur la proposition
de l'orchestrateur après la critique). Le remplacement des copies de `groma-core` et de
l'application Groma par `terrain` (GNSS, géodésie, journal, GeoPackage, voix, carte) est
**renvoyé à une ADR ultérieure**, après l'essai d'automne 2026 et une table des écarts
module par module : `terrain` n'a ni la politique du fix de l'ADR 0003, ni le refus des
azimuts avant normalisation, ni les grades, ni la convergence, ni le modèle WMM, ni
EPSG:9794, et `terrain-journal` fusionne en « dernière écriture gagne » avec des UUID v4,
contre le journal en ajout seul et les UUID v7 de Surculus (ADR 0005) (critique du
`contradicteur`, journal du 2026-10-22). Numéro réservé : **ADR 0012** (index des ADR). Les
en-têtes des copies qui renvoient encore à « ADR 0008 » pour l'extraction
(`core/.../gnss/*.kt`, `core/.../geo/*.kt`, `Declinaison.kt`, `app/.../gnss/*.kt` et leurs
tests, `NOTICE` l. 38, ADR 0003 l. 69, fiche de la mission 003 l. 26) seront renvoyés à
l'ADR 0012 par sa première mission ; d'ici là, ce paragraphe fait le lien.

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

Contrastes de luminance relative WCAG 2.2, calculés par l'orchestrateur et recalculés par le
`contradicteur`, contre le **fond réellement peint** : `surface` #FEF7FF de
Material 3, sauf mention. « Grand » = au moins 24 sp, ou 18,7 sp en gras (18 pt et 14 pt de
WCAG, en supposant 1 sp ≈ 1 px CSS, police du téléphone à 100 % : hypothèse prudente selon le
`contradicteur`, non sourcée ici ; le sp suit la taille de police choisie dans le téléphone).

| Texte | Où | Taille | Contraste | 7:1 | 1.4.6 tel qu'écrit |
|---|---|---|---|---|---|
| noir (`Palette.Noir`) | messages, cap de guidage, légende « inconnu » | 14-72 sp | 19,96 | oui | oui |
| texte par défaut (`onSurface` #1D1B20) | tous les écrans | — | 16,23 | oui | oui |
| rouge #B00020 | refus, hors France, sans position, légende « jamais », ortho absente, erreurs de l'écran d'essai (`EcranCarte.kt` l. 86, 196, 198, 279 ; `EcranGuidage.kt` l. 70, 76, 150, 152, 216 ; `EcranTournees.kt` l. 125, 159, 176 ; `EcranEssaiGnss.kt` l. 162, 163, 180) | 14-22 sp | **6,96** | **non** | oui pour `EcranGuidage.kt` l. 150 et 152 (22 sp gras) ; non ailleurs |
| vert #0B6B2E | « arrivé », cap arrivé, rattaché, légende « à jour », ortho complète (`EcranGuidage.kt` l. 158, 162, 206 ; `EcranCarte.kt` l. 281 ; `EcranTournees.kt` l. 176) | 14-72 sp | **6,32** | **non** | oui pour `EcranGuidage.kt` l. 158 (72 sp) et 162 (24 sp gras) ; non pour l. 206 et `EcranTournees.kt` l. 176 (18 sp gras) et pour la légende (14 sp) |
| orange #B45300 | rattachement par identité, à confirmer, anomalies, légende « travaux à prévoir », ortho partielle (`EcranGuidage.kt` l. 204, 218 ; `EcranCarte.kt` l. 280, 287 ; `EcranTournees.kt` l. 176) | 14-18 sp | **4,78** | **non** | non |
| blanc sur vert / orange / rouge | badge du fix (`carte/Commun.kt` l. 69-75, 18 sp gras), panneau de l'écran d'essai GNSS (`EcranEssaiGnss.kt` l. 267-279) | 18-44 sp | 6,65 / **5,03** / 7,33 | non / **non** / oui | badge (18 sp gras, texte normal) : non / non / oui ; lignes de 18-20 sp de l'écran d'essai : non / non / oui ; lignes de 44 sp : oui |
| blanc sur `primary` par défaut #6750A4 | boutons pleins (`MainActivity.kt` l. 76-84, `EcranGuidage.kt` l. 180-184, `EcranTournees.kt` l. 114-117, `EcranCarte.kt` l. 307, `sauvegarde/EcranRestauration.kt` l. 69) | 18-20 sp | **6,44** | **non** | non |
| `primary` #6750A4 **en texte** sur `surface` | boutons à contour : « Retour » de chaque écran (`EcranCarte.kt` l. 87, 192 ; `EcranGuidage.kt` l. 71, 143 ; `EcranTournees.kt` l. 110), « Ma position » (`EcranCarte.kt` l. 293), « Fermer la liste » (l. 336), son (`EcranGuidage.kt` l. 191), « Relancer l'ortho » (`EcranTournees.kt` l. 178) ; libellé et curseur des champs actifs : code du piquet (`EcranGuidage.kt` l. 173), réglages de l'écran d'essai (`EcranEssaiGnss.kt` l. 212, 240, 247) | 14-18 sp | **6,12** | **non** | non |
| blanc sur vert #0B6B2E | boutons de `EcranCarte.kt` l. 298 et « Démarrer » de `EcranEssaiGnss.kt` l. 170-171 | 18-20 sp | 6,65 | non | non |
| texte d'un bouton désactivé (`onSurface` à 38 %) | rôle implicite de Material 3 | — | ≈ 2,3 (`contradicteur`) | non | exempté |

Pour comparaison, les couleurs de Marculus sur `surface` : `VertMarculus` 9,32 ; `VertVif`
4,91 ; blanc sur `VertMarculus` 9,80 ; blanc sur `VertVif` 5,17 (bouton principal de
Marculus et de Groma).

Les **pastilles** des placettes (mission 033) ne sont pas du texte : WCAG 2.2, 1.4.11, leur
demande 3:1 **contre les couleurs adjacentes**. Chaque pastille est cerclée d'un **filet
blanc** de 3 dp (`EcranCarte.kt` l. 104-137) : contre ce blanc, vert 6,65, orange 5,03, rouge
7,33, noir 21, toutes au-dessus de 3:1. Restent à regarder sur l'ortho réelle, sans mesure à
ce jour : le filet blanc sur une ortho claire (prairie, coupe rase, sol nu) ; le **marqueur de
la position de l'agent**, un disque noir **sans filet** (`EcranCarte.kt` l. 231), sur la
canopée sombre ; les contours des polygones (l. 217-219).

## Options

| Option | Ce que c'est | Pour | Contre |
|---|---|---|---|
| A | Module **Android Compose** `terrain-theme` (un `MaterialTheme` partagé) | Une seule source prête à l'emploi | Contraire à la règle « Kotlin pur » de `terrain` (README l. 20) ; construction Android sur JitPack ; impose Compose et sa version aux trois applications |
| **B étendue (retenue)** | Module Kotlin pur `terrain-style` : **tous les rôles** du schéma Material 3 en ARGB (`Long`), en clair (et en sombre si Marculus le garde) ; un test parcourt **tous les couples** rôle « on » / rôle de fond, **et** `primary`, `secondary`, `tertiary`, `error` employés en texte sur `surface`, `surfaceContainer*` et `surfaceDim` (cas des boutons à contour et des champs), plus les couleurs d'état ; chaque application recopie les rôles 1:1 dans son `ColorScheme`, et un test de l'application vérifie l'égalité avec les jetons | Style commun réel (les rôles, pas seulement trois couleurs) ; testable en JVM ; respecte la règle de `terrain` | Une adaptation de quelques dizaines de lignes par application, tenue par son test d'égalité |
| **E (retenue, avec B)** | Les valeurs des rôles sont **générées** une fois par Material Color Utilities (Google, Apache-2.0) depuis la couleur de départ `VertMarculus`, au niveau de contraste 0,5 (« medium »), puis **figées** dans `terrain-style` ; y sont inscrits le script de génération, le commit de la bibliothèque, la version du schéma (`SPEC_2021`), le niveau de contraste et la couleur de départ ; le test de contraste reste juge | Convention publiée plutôt qu'invention : dans `ColorSpec2021`, à ce niveau, `onPrimary`, `onSurface` et `onError` visent 11:1, `onSurfaceVariant` et `onPrimaryContainer` 7:1, et `primary`, `secondary`, `tertiary`, `error` 7:1 contre la surface (`ColorSpec2021.java`, `ContrastCurve.java`) | **Pas d'artefact Java officiel** (le README renvoie à MDC-Android) : valeurs figées depuis les sources, aucune dépendance à l'exécution ; les courbes changent d'une version du schéma à l'autre (`ColorSpec2025` : `onPrimary` 6, `onSurfaceVariant` 4,5 ; `MaterialDynamicColors` est passé à `ColorSpec2026`) ; teintes qui s'écartent des trois couleurs actuelles |
| C | Troisième copie du `Theme.kt` de Marculus | Immédiat | Écartée par Pascal le 2026-10-22 |
| D | Cohérence par le nom, l'icône, la typographie et les formes seulement | Aucun risque au soleil | Ne donne pas les couleurs communes demandées |

**La palette d'état** (rouge, orange, vert, noir des pastilles, du badge et des alertes)
reste propre à Surculus (spec § 3). Là où elle porte du **texte**
(alertes, badge, et la légende de la carte, `EcranCarte.kt` l. 279-281, qui reprend les
couleurs des pastilles), elle échoue à la règle de contraste : l'orange (4,78:1) échoue
quelle que soit la réponse à la question 2. Décision de Pascal (question 6, 2026-10-23) :
« couleurs de la 033 gardées » vise **les pastilles seules** ; la légende de la carte est
écrite **en noir à côté de sa pastille de couleur**. Les autres textes de la palette d'état
(alertes, badge du fix, écran d'essai) devront tenir la même règle de contraste : **la
manière n'est pas décidée** (couleur foncée, texte noir à côté d'une pastille, autre fond
pour le badge blanc sur orange à 5,03:1). Elle sera proposée par la mission du thème et
soumise à l'essai au soleil, puis à Pascal.

## Le crible FRANCE

| Lettre | Question | Réponse (une ligne) | Ce que ça coûte | Prêts à payer ? |
|---|---|---|---|---|
| **F**rugale | La plus simple qui tienne ? | Des jetons en `Long` et un test JVM ; pas d'Android dans `terrain` | Quelques dizaines de lignes d'adaptation par application | **à confirmer par Pascal** (déduit de son acceptation, pas d'une réponse explicite) |
| **R**esponsable | Qui fait quoi ? | Pascal maintient `terrain` ; l'orchestrateur écrit le brief vers `terrain` ; un `dev` de Surculus fait l'adaptation, le `testeur` le test d'égalité depuis cette ADR | `terrain` n'a ni `testeur` séparé ni `relecteur` (règles de ce dépôt seulement) ; aucun successeur désigné pour `terrain` | oui : aucun successeur pour l'instant, risque assumé par Pascal (question 3), à revoir à chaque bilan |
| **A**nticiper | Si l'imprévu arrive ? | Version épinglée par **SHA de commit** (JitPack accepte « release tag, commit hash or `master-SNAPSHOT` », docs.jitpack.io), d'un commit **publié depuis plus de 7 jours** (avant, un artefact public peut être reconstruit) et déjà construit par JitPack (il construit à la demande, là où il a déjà échoué : `terrain/README.md` l. 50-51) ; repli par `publishToMavenLocal` sans réseau ; comme JitPack ne joue pas les tests, le test de contraste est **rejoué dans Surculus** sur les jetons reçus | Une étape de plus à chaque montée de version ; Gradle ne vérifie pas les dépendances (pas de `verification-metadata.xml`) ; `docs/dependances.md` l. 20 et `NOTICE` à mettre à jour par la mission | **à confirmer par Pascal** (déduit de son acceptation, pas d'une réponse explicite) |
| **N**ovateur | Toutes les idées ? | Cinq options, dont la génération par Material Color Utilities | — | — |
| **C**oût | Que coûte-t-elle à l'ensemble ? | Une mission dans `terrain` (`terrain-style`), puis une mission Surculus en « appuyer » après le terrain de Pascal ; Marculus et Groma prennent les mêmes jetons, demandés par un brief dans chaque dépôt (question 1) | Aucun délai sur la 012 ; rien avant le retour de Couchey. Le bouton principal de Marculus et de Groma change de vert ; la mise en œuvre revient à leurs dépôts. La mission dans `terrain` se fait hors de ce dépôt ; à compter dans le plafond humain de `pilotage/equipe.md` (proposition de l'orchestrateur) | mêmes jetons : oui (Pascal, question 1) ; le reste **à confirmer par Pascal** (déduit) |
| **É**cologique | Que réutilise-t-on, qui réutilisera ? | `terrain` déjà publiée ; une convention de Google pour les rôles ; Marculus et Groma peuvent prendre les mêmes jetons | Groma ne consomme pas encore `terrain` : « commune » à deux applications aujourd'hui | **à confirmer par Pascal** (déduit de son acceptation, pas d'une réponse explicite) |

## Valeurs sacrifiées

- **Le mode sombre** dans Surculus : thème clair forcé (Pascal, 2026-10-22).
- **Les couleurs actuelles de Marculus et Groma** : blanc sur `VertVif` (5,17:1) ne tient
  pas la règle de contraste ; les deux applications prennent les jetons générés, et leur
  bouton principal change de vert (question 1).
- **Les teintes exactes choisies à la main** : les rôles viennent de Material Color
  Utilities et peuvent s'écarter des trois couleurs actuelles (option E).
- **La couleur du texte de la légende** : la légende est écrite en noir, seule la pastille
  porte la couleur (question 6).
- **La correction unique des autres briques**, d'ici l'ADR ultérieure : les copies restent.

## Décision de Pascal (2026-10-23)

Pascal a validé les propositions de l'orchestrateur sur les six questions (« Je valide les
6 propositions »), telles que l'orchestrateur les lui a posées dans la session :

1. **Les couleurs de Marculus et Groma** : les trois applications prennent **les mêmes
   jetons**, demandés par un brief dans chaque dépôt
   (`pilotage/briefs/vers-marculus/2026-10-22-readme-terrain-et-style-commun.md`,
   `pilotage/briefs/vers-groma/2026-10-23-style-commun.md`).
2. **La règle de contraste** : **WCAG 2.2, 1.4.6 tel qu'écrit** : 7:1 pour le texte,
   4,5:1 pour le grand texte (au moins 24 sp, ou 18,7 sp en gras), éléments désactivés
   exemptés. *Rappel, hors de la décision : 1.4.11 demande déjà 3:1 pour ce qui n'est pas
   du texte ; les seuils en sp reposent sur l'hypothèse non sourcée 1 sp ≈ 1 px CSS
   (inventaire).*
3. **Le successeur de `terrain`** : **aucun pour l'instant**, risque assumé par Pascal
   (2026-10-23 : « Pour l'instant personne repre[nd] »). Pascal maintient `terrain` ; le
   dépôt est public sous GPL-3.0 (vérifié le 2026-10-23) et se reconstruit sans réseau
   (`publishToMavenLocal`) : un tiers peut reprendre le code, mais personne ne s'y est
   engagé. À revoir à chaque bilan.
4. **L'option** : **B étendue avec E** : rôles générés une fois par Material Color
   Utilities depuis `VertMarculus`, niveau de contraste 0,5, `SPEC_2021`, valeurs figées
   dans `terrain-style` avec le script, le commit de la bibliothèque et ces réglages ; le
   test de contraste reste juge.
5. **Le grand texte** : jugé avec la police du téléphone **à 100 %** (même hypothèse, 1 sp ≈ 1 px CSS, à vérifier).
   Un agent qui agrandit la police agrandit le texte sans en baisser le contraste.
6. **« Couleurs de la 033 gardées »** : **les pastilles seules**. La légende est écrite en
   noir à côté de sa pastille de couleur ; l'orange ne porte plus de texte dans la légende.

### Suite

- Brief vers `terrain` : module `terrain-style`
  (`pilotage/briefs/vers-terrain/2026-10-23-terrain-style.md`).
- Mission Surculus du thème, en « appuyer », **après le retour de Couchey** : reprise des
  jetons, thème clair forcé, légende en noir, palette d'état passée au test, test d'égalité
  avec les jetons et test de contraste rejoué dans Surculus, `docs/dependances.md` et
  `NOTICE` ; essai au soleil par Pascal avant la fusion (spec § 3).

Et une proposition : pendant le terrain de Couchey, un premier essai au soleil de la
palette actuelle (badge du fix, texte orange, pastilles sur une ortho claire, **marqueur
noir de la position** sur la canopée, bouton « Retour » violet), noté au journal, avant la
mission plutôt qu'après.

## Sources

- `terrain/README.md` l. 12-22 et 84 ; `terrain/jitpack.yml` (tag v0.5.0, `ee98731`).
- `marculus/app/src/main/kotlin/io/github/pobsteta/marculus/ui/theme/Theme.kt` l. 10-39 ;
  `marculus/app/src/main/kotlin/io/github/pobsteta/marculus/MainActivity.kt` l. 63 ;
  `marculus/gradle/libs.versions.toml` l. 18 et 29-33 (`87c3cf4`).
- `groma/android/app/src/main/kotlin/groma/app/ui/Theme.kt` l. 10 et 32.
- Surculus (main après la PR #65) : fichiers et lignes du tableau d'inventaire.
- Material 3, jetons par défaut : `ColorLightTokens.kt`, `PaletteTokens.kt` (androidx,
  branche androidx-main ; version de la BOM de Surculus non vérifiée).
- Material Color Utilities : README, `ColorSpec2021.java`, `ColorSpec2025.java`,
  `ContrastCurve.java`, `MaterialDynamicColors.java`
  (github.com/material-foundation/material-color-utilities).
- JitPack : https://docs.jitpack.io/ (versions par commit, immutabilité après 7 jours).
- [WCAG 2.2](https://www.w3.org/TR/WCAG22/), critères 1.4.3, 1.4.6 et 1.4.11.
- Critiques du `contradicteur` sur la première version de cette ADR et sur sa réécriture, et décision de Pascal
  de la réécrire limitée au style : `pilotage/journal.md`, 2026-10-22.

## Question de contrôle

Cette décision tient-elle nos six valeurs pour le **forestier en forêt** (soleil, gants,
une main libre) d'abord, puis pour le **successeur** qui reprendra les trois applications,
et que sommes-nous prêts à payer pour cela ?
