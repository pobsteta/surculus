# ADR 0004 — Stack : Kotlin, Compose, Room, module GeoPackage JVM, Vosk embarqué

- **Date :** 2026-10-15
- **Statut :** acceptée par Pascal (2026-10-15), après critique du `contradicteur`
- **Proposée par :** Claude (orchestrateur)
- **Tranchée par :** Pascal

## La décision en une phrase

Surculus est écrit en Kotlin pour Android (minSdk 26, identifiant
`io.github.pobsteta.surculus`), avec la pile de Marculus (Compose, Room, osmdroid) ; la
lecture et l'écriture du `travaux.gpkg` vivent dans un module JVM `:gpkg`
(geopackage-java) testé sans appareil ; la dictée passe par une interface `MoteurDictee`
implémentée avec Vosk 0.3.75 et le modèle `vosk-model-small-fr-0.22` ; la commande de
tests du projet est `./gradlew :core:test :gpkg:test :app:assembleDebug :app:lintDebug` (lint ajouté le 2026-10-16, arbitrage de Pascal).

## Contexte

L'agent de terrain doit pouvoir, gants aux mains et sans regarder l'écran, rendre une
tournée que sommieR importe ; Pascal, seul mainteneur de trois applications (ou son
successeur), doit pouvoir la publier encore en 2027. L'ADR 0001 reprend le code de
Marculus et de Groma.

Options envisagées :

1. **Kotlin natif, pile de Marculus** : retenue.
2. **Kotlin Multiplatform** : pas de besoin iOS ; Bluetooth classique absent d'iOS.
3. **Flutter ou React Native** : tout le code repris serait à réécrire.
4. **Application web** : écartée dans l'ADR 0001.

Dictée hors ligne :

| Moteur | Licence | Grammaire | Motif |
|---|---|---|---|
| **Vosk** | Apache-2.0 | **fermée stricte** (liste de mots) | retenu : seul masque strict hors ligne, déjà éprouvé dans Marculus |
| whisper.cpp | MIT | grammaire GBNF par **pénalité**, pas un masque | lourd en calcul sur téléphone |
| sherpa-onnx | Apache-2.0 | « hotwords » = **bonus de score** (modèles transducteurs) | pas de grammaire fermée ; modèle français existant |
| Reconnaissance d'Android | propriétaire (services Google) | biais `EXTRA_BIASING_STRINGS` à partir de l'API 33 | minSdk 26 ; dépend de services non libres |

Tests : le premier critère de la tranche 1 (l'export passe l'import de sommieR) est vérifié
par la CI. Écrire le GeoPackage dans un module Android ne se teste que sur appareil ;
d'où le module **`:gpkg` en JVM** (arbitrage de Pascal), plutôt que Robolectric ou un
émulateur en CI.

## Règles

| Sujet | Règle |
|---|---|
| Modules | `:core` (Kotlin pur), `:gpkg` (JVM : geopackage-java MIT + sqlite-jdbc), `:data` (Room, journal), `:app` (Compose, services GNSS et dictée) |
| Tests | JUnit Jupiter, aligné sur `groma-core` ; la CI joue `./gradlew :core:test :gpkg:test :app:assembleDebug :app:lintDebug` (lint ajouté le 2026-10-16, arbitrage de Pascal) et rejoue l'import R de sommieR (`sommier_importer_qfield_travaux()`) sur l'export de la démo Couchey |
| Vosk | `vosk-android` **0.3.75** ; contrôle d'alignement des bibliothèques natives sur **16 Ko** (`zipalign -P 16`) en CI |
| Modèle | `vosk-model-small-fr-0.22` (41 Mo, Apache-2.0), **nom exact et somme SHA-256** inscrits dans `docs/dependances.md` ; les modèles français NC (`small-fr-pguyot`) et AGPL (`fr-linto`) sont exclus ; modèle **republié** dans les releases de Surculus (NOTICE conservé) et **importable par fichier**, sans réseau |
| Grammaire | un test `:core` vérifie que chaque mot de la grammaire figure dans la liste de mots du modèle (fixture versionnée) ; la grammaire est neutre vis-à-vis du moteur, ce que le test rend vérifiable |
| Boutons de volume | **tranche 1 : écran maintenu allumé, application au premier plan, « mode poche »** qui ignore l'écran tactile (arbitrage de Pascal) ; la mission inclut un essai MediaSession écran éteint sur deux appareils (Android 14 et 16) |
| Geste « annuler » | pas les deux touches ensemble (raccourci d'accessibilité d'Android) : double appui court sur volume bas, à confirmer à l'essai |
| Identifiant | `io.github.pobsteta.surculus` (arbitrage de Pascal) ; distribution, clé de signature et vérification des développeurs : ADR 0010 |

## Le crible FRANCE

| Lettre | Question | Réponse (une ligne) | Ce que ça coûte |
|---|---|---|---|
| **F**rugale | La plus simple qui tienne ? | Même pile que Marculus, un seul langage. | Compose et Room sont lourds ; bibliothèques natives Vosk (environ +10 Mo d'APK) et modèle de 41 Mo. |
| **R**esponsable | Qui fait quoi ? | Le `dev` code, le `testeur` écrit les tests depuis la spec, la CI vérifie l'export ; Pascal valide les PR. | Pascal garde les essais sur appareil (boutons, dictée, GNSS) : deux appareils, à chaque tranche. |
| **A**nticipateur | Si l'imprévu arrive ? | `:core` et `:gpkg` testés sans téléphone ; modèle republié et importable sans réseau ; moteur de dictée remplaçable. | Cible SDK annuelle ; Vosk porté par un petit projet ; écran allumé (batterie). |
| **N**ovateur | Toutes les idées ? | Quatre piles et quatre moteurs de dictée lus. | Un banc d'essai audio de terrain (vent, gants) à constituer pour comparer les moteurs. |
| **C**oût | Coût pour l'ensemble ? | Réutilisation maximale de Marculus et Groma. | Mise à jour annuelle de la cible SDK pour trois applications ; module `:gpkg` à écrire. |
| **É**cologique | Que réutilise-t-on, qui réutilisera ? | Mêmes briques que Marculus et Groma ; geopackage-java réutilisable hors Android. | Android seul. |

## Valeurs sacrifiées

- **Frugalité** : taille de l'APK et du modèle ; pile Compose/Room lourde.
- **Responsabilité partagée** : les essais sur appareil restent à Pascal.
- **Geste « téléphone en poche »** : écran allumé en tranche 1, jusqu'à l'essai MediaSession.
- **Portée** : Android seul.

## Conséquences

- `.claude/test-command` : `./gradlew :core:test :gpkg:test :app:assembleDebug :app:lintDebug` (lint ajouté le 2026-10-16, arbitrage de Pascal).
- `docs/architecture.md` et `docs/dependances.md` mis à jour (module `:gpkg`,
  geopackage-java, sqlite-jdbc, Vosk 0.3.75, proj4j retiré).

## Sources

- Marculus : `gradle/libs.versions.toml`, `core/build.gradle.kts`, `data/build.gradle.kts`,
  `.github/workflows/ci.yml`, `docs/specs/2026-08-21-tranche-dictee-vocale.md`
  (l. 138-157, 168-169, 209-218, 233), `ModeleVosk.kt`, `VoiceGrammar.kt`.
- `groma-core/build.gradle.kts` (JUnit Jupiter).
- Modèles Vosk : https://alphacephei.com/vosk/models ; versions : https://repo1.maven.org/maven2/com/alphacephei/vosk-android/
- Pages de 16 Ko : https://developer.android.com/guide/practices/page-sizes
- NGA geopackage-java (MIT) : https://github.com/ngageoint/geopackage-java
- Kotlin et JUnit : https://kotlinlang.org/docs/jvm-test-using-junit.html
- Boutons média : https://developer.android.com/media/legacy/media-buttons ;
  politique d'accessibilité Google Play : https://support.google.com/googleplay/android-developer/answer/10964491 ;
  raccourci d'accessibilité : https://support.google.com/accessibility/android/answer/7650693
- whisper.cpp, exemple `command` : https://huggingface.co/datasets/echodict/whisper.cpp/blob/main/examples/command/command.cpp
- sherpa-onnx, hotwords : https://k2-fsa.github.io/sherpa/onnx/hotwords/index.html
- `RecognizerIntent`, API 33 : https://developer.android.com/sdk/api_diff/33/changes/android.speech.RecognizerIntent
- Google Play, niveau d'API cible : https://developer.android.com/google/play/requirements/target-sdk ;
  vérification des développeurs : https://developer.android.com/developer-verification

## Question de contrôle

Cette décision tient-elle nos six valeurs, et que sommes-nous prêts à payer pour cela ?
