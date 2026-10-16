# Dépendances et licences

Licence du dépôt : **GPL-3.0-or-later**, comme Groma et nemeton.

## Prévues (tranche 1)

| Composant | Où | Licence | Remarque |
|---|---|---|---|
| Kotlin, kotlinx-coroutines, kotlinx-serialization | partout | Apache-2.0 | |
| AndroidX, Jetpack Compose, Material 3, Room, DataStore | `:app`, `:data` | Apache-2.0 | |
| osmdroid | carte | Apache-2.0 | |
| NGA geopackage-java + sqlite-jdbc | `:gpkg` (JVM) | MIT ; Apache-2.0 | lecture et écriture du `travaux.gpkg`, testées sans appareil (ADR 0004) |
| Modèle géomagnétique WMM2025 (`WMM.COF`, NCEI) | `:core` | domaine public selon NCEI (compatibilité GPL à confirmer) | déclinaison hors ligne, valable jusqu'à fin 2029 ; pas de bibliothèque Java établie (`docs/referentiels/modele-geomagnetique.md`) |
| Vosk `vosk-android` 0.3.75 et modèle `vosk-model-small-fr-0.22` (41 Mo) | dictée | Apache-2.0 | modèle republié dans les releases de Surculus et importable par fichier ; SHA-256 à inscrire ; modèles fr NC et AGPL exclus (ADR 0004) |
| JUnit Jupiter (aligné sur `groma-core`) | tests | EPL-2.0 | tests seulement |
| JNA 5.18.1 (`aar`) | `:app` (requise par Vosk) | LGPL-2.1-or-later ou Apache-2.0, au choix | embarquée dans l'APK (bibliothèque native `libjnidispatch.so`) |
| Bibliothèques communes Marculus / Groma | GNSS, géodésie, GeoPackage, carte, dictée, journal | GPL-3.0-or-later | Marculus (`792b5aa`) et Groma sont sous GPL-3.0-or-later ; tranche 1 : `groma-core` provisoire (brief `vers-marculus`) |
| Tuiles IGN Géoplateforme, OpenStreetMap | carte | Etalab 2.0, ODbL | attribution affichée, comme dans Groma |

Aucune dépendance non commerciale ni AGPL n'est prévue.

## Versions posées (mission 001, 2026-10-16)

| Composant | Version | Licence | Source de la version |
|---|---|---|---|
| Gradle (wrapper) | 9.8.1, `distributionSha256Sum` inscrit | Apache-2.0 | AGP 9.4.1 exige Gradle ≥ 9.6.0 ; 9.8.1 est celle de Groma (`groma/.github/workflows/ci.yml` l. 47) ; somme : `https://services.gradle.org/distributions/gradle-9.8.1-bin.zip.sha256` |
| Android Gradle Plugin | 9.4.1 | Apache-2.0 | `groma/android/build.gradle.kts` l. 12 (arbitrage de Pascal) |
| Kotlin et plugin compose-compiler | 2.4.21 | Apache-2.0 | `groma/android/build.gradle.kts` l. 18-19 (arbitrage de Pascal) |
| KSP | 2.3.12 | Apache-2.0 | Maven Central, `com/google/devtools/ksp/symbol-processing-gradle-plugin/maven-metadata.xml` (dernière version, publiée le 2026-09-09) |
| vosk-android | 0.3.75 | Apache-2.0 | ADR 0004 ; Maven Central, `com/alphacephei/vosk-android/maven-metadata.xml` |
| JNA (`aar`) | 5.18.1 | LGPL-2.1-or-later ou Apache-2.0 | dépendance déclarée dans le POM de vosk-android 0.3.75 ; licences : POM de `net.java.dev.jna:jna:5.18.1` |
| sqlite-jdbc | 3.53.4.0 | Apache-2.0 | `groma/android/groma-core/build.gradle.kts` l. 24 |
| JUnit Jupiter | 6.1.3 | EPL-2.0 | `groma/android/groma-core/build.gradle.kts` l. 19 |

Le catalogue complet, avec les versions reprises de Marculus, est `gradle/libs.versions.toml`.
