# ADR 0008 — Bibliothèques communes (Marculus, Groma) et style commun

- **Date :** 2026-10-22
- **Statut :** proposée
- **Proposée par :** Claude (orchestrateur)
- **Tranchée par :** Pascal

## La décision en une phrase

Surculus prend ses briques communes dans la bibliothèque **`terrain`** (GPL-3.0-or-later,
JitPack), le **style commun** y entre comme un module **Kotlin pur de jetons de couleur et
de règle de contraste** (`terrain-style`, proposé), et les copies provisoires de
`groma-core` sont remplacées **module par module, après l'essai d'automne 2026**.

## Contexte

Pourquoi maintenant : Pascal a tranché le 2026-10-22 que le style commun avec Marculus et
Groma passe par `terrain` et cette ADR, et qu'il ne passe jamais devant la lisibilité au
soleil (`docs/brief.md`, « Style commun » ; spec § 3, « Affichage » ; journal du
2026-10-22). L'ADR était réservée depuis le 2026-10-15 (spec § 10.5).

Ce qui existe :

- **`terrain` v0.5.0** est publiée : `terrain-gnss`, `-journal`, `-gpkg`, `-voix`,
  `-carte`, toutes en **Kotlin pur, JVM 17**. Restent dans chaque application, « décision
  du 2026-10-17 », l'affichage osmdroid, l'enveloppe Vosk, l'adaptateur SQLite et la
  grammaire métier (`terrain/README.md` l. 12-22). `terrain` n'a **aucun module Android ni
  Compose**.
- **Marculus** la consomme (`marculus/gradle/libs.versions.toml` l. 18 et 29-33, `87c3cf4`).
  **Groma ne la déclare pas** (aucune occurrence dans ses fichiers de build), contrairement
  au README de Marculus (l. 106-115), qui la dit « partagée avec Groma et Surculus ».
- **Surculus** recopie provisoirement `groma-core` : `core/.../gnss/{Gnss,Nmea,Ntrip}.kt`,
  `core/.../geo/{Geodesie,Projections,Azimut}.kt`, `core/.../reperes/wmm/Declinaison.kt`,
  `app/.../gnss/{ConfigRtk,Transports,ServiceGnssRtk,SourcePosition}.kt`, chacun avec un
  en-tête « jusqu'à l'extraction en bibliothèque commune (ADR 0008) » ; `docs/dependances.md`.
- **Le thème** de Marculus tient en trois couleurs et un `MaterialTheme` qui suit le mode
  sombre du téléphone (`marculus/.../ui/theme/Theme.kt` l. 10-12 ;
  `marculus/.../MainActivity.kt` l. 63). Celui de Groma en est une copie (`groma/.../ui/Theme.kt`
  l. 10). Surculus utilise le `MaterialTheme` par défaut (`MainActivity.kt` l. 42) et une
  palette de terrain noir sur blanc (`carte/Palette.kt` l. 13-19).

**Contrastes** (luminance relative de WCAG 2.2, calculés par l'orchestrateur le 2026-10-22 ;
seuil de Pascal : 7:1 pour le texte, [WCAG 2.2, 1.4.6](https://www.w3.org/TR/WCAG22/#contrast-enhanced)) :

| Texte / fond | Contraste | ≥ 7:1 ? |
|---|---|---|
| blanc sur `VertMarculus` #374742 | 9,80 | oui |
| crème #F5EBD8 sur `VertMarculus` | 8,29 | oui |
| noir sur crème | 17,76 | oui |
| blanc sur `VertVif` #3E7A40 (boutons de Marculus) | 5,17 | **non** |
| `VertVif` sur crème | 4,37 | **non** |
| `Palette.Vert` #0B6B2E de Surculus sur blanc (texte du cap « arrivé », `EcranGuidage.kt` l. 158 et 235) | 6,65 | **non** |
| `Palette.Rouge` #B00020 sur blanc (messages de refus, `EcranTournees.kt` l. 125) | 7,33 | oui |
| `Palette.Orange` #B45300 sur blanc | 5,03 | **non** (pastille, pas texte) |

Les pastilles des placettes sont des objets graphiques, pas du texte : la norme leur
demande 3:1 ([WCAG 2.2, 1.4.11](https://www.w3.org/TR/WCAG22/#non-text-contrast)).

Options envisagées pour le style :

| Option | Ce que c'est | Pour | Contre |
|---|---|---|---|
| A | Module **Android Compose** `terrain-theme` (un `MaterialTheme` partagé) | Une seule source, prête à l'emploi | Contraire à la règle « Kotlin pur » de `terrain` (décision du 2026-10-17) ; construction Android sur JitPack ; impose Compose et sa version aux trois applications |
| **B (proposée)** | Module **Kotlin pur** `terrain-style` : les couleurs en ARGB (`Long`), leurs rôles (fond, texte, action, alerte), une fonction de contraste WCAG et un test qui exige 7:1 pour chaque couple texte/fond déclaré | Respecte la règle de `terrain` ; testable en JVM ; chaque application en tire son `ColorScheme` en une vingtaine de lignes ; Marculus et Groma y gagnent la règle de contraste | Une vingtaine de lignes Compose restent dans chaque application ; `VertVif` ne peut plus porter de texte blanc |
| C | Troisième copie provisoire du `Theme.kt` de Marculus | Immédiat | Écarté par Pascal le 2026-10-22 (question 4) |
| D | Cohérence par le nom, l'icône et la typographie seulement | Aucun risque au soleil | Ne donne pas le style commun demandé |

Options pour les autres briques (GNSS, géodésie, journal, GeoPackage, voix, carte) :

| Option | Pour | Contre |
|---|---|---|
| 1. Remplacer les copies de `groma-core` **maintenant** | Une seule correction pour trois applications | Touche le GNSS et le journal à la veille de l'essai d'automne (spec § 7, critère d) ; `terrain-gpkg` en SQL simple ne remplace pas l'écrivain de l'ADR 0011 sans essai |
| **2 (proposée). Après l'essai d'automne 2026**, module par module, chacun en mission avec les tests existants de Surculus comme juges | L'essai porte sur le code qui a été testé ; chaque remplacement est petit | Deux copies vivent jusqu'à l'hiver ; une correction faite dans `terrain` d'ici là est à reporter à la main |
| 3. Ne jamais migrer | Aucun travail | Contraire à la décision du 2026-10-15 (brief `vers-marculus`, « sans recopier le code ») |

## Le crible FRANCE

| Lettre | Question | Réponse (une ligne) | Ce que ça coûte | Prêts à payer ? |
|---|---|---|---|---|
| **F**rugale | La plus simple qui tienne ? | Des jetons et une fonction de contraste, sans Android dans `terrain` | Une vingtaine de lignes Compose par application | à trancher par Pascal |
| **R**esponsable | Qui fait quoi ? | Pascal mainteneur de `terrain` (seul) ; l'orchestrateur rédige le brief vers `terrain` ; un `dev` de Surculus consomme | Une mission dans un autre dépôt, hors de l'équipe de Surculus (`pilotage/equipe.md`) | à trancher par Pascal |
| **A**nticiper | Si l'imprévu arrive ? | JitPack indisponible : version épinglée par tag, construction locale possible (`terrain/README.md` l. 50-55) ; le thème clair forcé tient même si le téléphone est en sombre | Une dépendance de plus au réseau de JitPack à la construction | à trancher par Pascal |
| **N**ovateur | Toutes les idées ? | Quatre options pour le style, trois pour les briques | — | — |
| **C**oût | Que coûte-t-elle à l'ensemble ? | Une mission `terrain-style`, une mission Surculus après le terrain de Pascal, puis une mission par brique après l'automne | Délai : rien avant le retour de Couchey | à trancher par Pascal |
| **É**cologique | Que réutilise-t-on, qui réutilisera ? | `terrain` déjà publiée ; Marculus et Groma peuvent adopter `terrain-style` et sa règle de contraste | Groma n'a pas encore adopté `terrain` | à trancher par Pascal |

## Valeurs sacrifiées

- **L'uniformité parfaite avec Marculus** : `VertVif` ne porte plus de texte blanc dans
  Surculus (5,17:1). Les actions prennent `VertMarculus` (9,80:1), ou `VertVif` en simple
  surface sans texte. Raison : la lisibilité au soleil passe devant le style (Pascal,
  2026-10-22).
- **Le mode sombre** dans Surculus : thème clair forcé (Pascal, 2026-10-22).
- **La correction unique, d'ici l'hiver** : les copies de `groma-core` restent jusqu'après
  l'essai d'automne (option 2).

## Ce qui reste à trancher par Pascal

1. Le module `terrain-style` en **Kotlin pur** (option B), ou un module Android (option A).
2. Le **texte vert** du cap « arrivé » (`Palette.Vert`, 6,65:1) : le foncer pour tenir
   7:1, ou dire que la règle des 7:1 porte sur le texte courant et pas sur ce mot.
   La palette des pastilles (mission 033) n'est pas en cause (3:1 suffit).
3. Le **calendrier des autres briques** : option 1, 2 ou 3.

## Sources

- `terrain/README.md` l. 1-22 et 30-55 (`ee98731`, tag v0.5.0).
- `marculus/gradle/libs.versions.toml` l. 18, 29-33 ; `marculus/README.md` l. 106-115 ;
  `marculus/app/src/main/kotlin/io/github/pobsteta/marculus/ui/theme/Theme.kt` l. 10-12 ;
  `marculus/app/src/main/kotlin/io/github/pobsteta/marculus/MainActivity.kt` l. 63 (`87c3cf4`).
- `groma/android/app/src/main/kotlin/groma/app/ui/Theme.kt` l. 10 et 32.
- Surculus : `app/.../MainActivity.kt` l. 42, `app/.../carte/Palette.kt` l. 13-19,
  `app/.../carte/EcranGuidage.kt` l. 158 et 235, `app/.../tournee/EcranTournees.kt` l. 125 ;
  `docs/dependances.md` ; ADR 0003 l. 69, ADR 0011.
- Brief `pilotage/briefs/vers-marculus/2026-10-15-bibliotheques-communes.md`.
- [WCAG 2.2](https://www.w3.org/TR/WCAG22/), critères 1.4.3, 1.4.6 et 1.4.11.
- Critique du `contradicteur` sur l'amendement du brief et arbitrages de Pascal :
  `pilotage/journal.md`, 2026-10-22.

## Question de contrôle

Cette décision tient-elle nos six valeurs, et que sommes-nous prêts à payer pour cela ?
