# Dépendances et licences

Licence du dépôt : **GPL-3.0-or-later**, comme Groma et nemeton.

## Prévues (tranche 1)

| Composant | Où | Licence | Remarque |
|---|---|---|---|
| Kotlin, kotlinx-coroutines, kotlinx-serialization | partout | Apache-2.0 | |
| AndroidX, Jetpack Compose, Material 3, Room, DataStore | `:app`, `:data` | Apache-2.0 | |
| material-icons-core et material-icons-extended (version de la BOM Compose) | `:app` | Apache-2.0 | icônes `ViewColumn` et `ViewList` de la bascule liste/colonnes, comme Marculus ; décision de Pascal, 2026-10-27 (mission 043), qui lève pour ce seul point l'interdiction d'ajouter une dépendance ; R8 retire les icônes non utilisées de l'APK |
| osmdroid 6.1.20 | carte | Apache-2.0 | dépôt **archivé depuis le 2024-11-20** (ADR 0009) ; réexamen de l'ADR 0004 au bac à sable |
| sqlite-jdbc 3.53.4.0 (jar et classifieur `natives-android`) | `:gpkg` (JVM) et `:app` (natifs Android) | Apache-2.0 | lecture et écriture du `travaux.gpkg`, sur la JVM et sur l'appareil (ADR 0011) ; écrivain GeoPackage maison, sans geopackage-java ni proj4j |
| desugar_jdk_libs 2.1.5 | `:app` | GPL-2.0 avec exception Classpath (OpenJDK) | désucrage des API Java récentes, exigé par sqlite-jdbc sur Android (USAGE.md du projet) ; taille mesurée dans le rendu de la mission 006 |
| Modèle géomagnétique WMM2025 (`WMM.COF`, NCEI) | `:core` | domaine public selon NCEI (compatibilité GPL à confirmer) | déclinaison hors ligne, valable jusqu'à fin 2029 ; pas de bibliothèque Java établie (`docs/referentiels/modele-geomagnetique.md`) |
| Vosk `vosk-android` 0.3.75 et modèle `vosk-model-small-fr-0.22` (41 Mo) | dictée | Apache-2.0 | modèle republié dans les releases de Surculus et importable par fichier ; modèles fr NC et AGPL exclus (ADR 0004) ; archive `vosk-model-small-fr-0.22.zip` (42 233 323 octets, `https://alphacephei.com/vosk/models/vosk-model-small-fr-0.22.zip`), **SHA-256 `cabf6180e177eb9b3a9a9d43a437bd5e549f3a7d09525e5d69a3fed787be12ad`** (téléchargée le 2026-10-21 par la mission 025, confirmée par un second téléchargement), comparée sans tenir compte de la casse (spec § 4.3, décision 13) |
| JUnit Jupiter (aligné sur `groma-core`) | tests | EPL-2.0 | tests seulement |
| JUnit 4.13.2, Robolectric 4.17, `androidx.test:core` 1.7.0, `room-testing` 2.8.1 | tests de `:data` | EPL-1.0 ; MIT ; Apache-2.0 ; Apache-2.0 | journal Room sous Robolectric (mission 008, ADR 0004) ; versions de Marculus (`gradle/libs.versions.toml`) ; aucune dépendance de production |
| Robolectric 4.17 (déjà au catalogue) et `androidx.compose.ui:ui-test-junit4` 1.10.4 (version de la BOM Compose 2026.02.01, Google Maven), `kotlinx-coroutines-test` 1.10.2 (version des coroutines du catalogue ; `StandardTestDispatcher` du banc, déjà tirée par `ui-test-junit4`) | tests de `:app` (`testImplementation` seulement) | MIT ; Apache-2.0 ; Apache-2.0 | test de rendu des écrans en JVM, sans émulateur (mission 080, décision de Pascal du 2026-10-30) ; rien dans les APK ; `ui-test-manifest` écarté |
| `terrain-voix` `d840329` (tag v0.6.0 de `pobsteta/terrain`, via JitPack, groupe `com.github.pobsteta.terrain`) | `:core`, `:app` | GPL-3.0-or-later (même auteur) | `GrammaireFermee` (JSON de la grammaire Vosk), `NombresFrancais.jetons` (nombres en lettres), `InstallationModele.installer` et `empreinte` (import par fichier, SHA-256) ; ni `telecharger`, ni `VocabulaireVosk`, ni `NoyauPtt` ; dépôt JitPack limité à ce groupe (`settings.gradle.kts`) ; décision de Pascal du 2026-10-24 (mission 013) ; aucun code recopié |
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
| Gson (tests de `:core` seulement) | 2.14.0 | Apache-2.0 | Maven Central, `com/google/code/gson/gson/maven-metadata.xml` (release) ; licence : POM de `gson-2.14.0.pom` ; lit les `fixtures/` JSON (mission 002) ; aucune dépendance de production |
| New Rocker Regular (police de l'icône) | – | OFL-1.1 | `google/fonts`, `ofl/newrocker` ; téléchargée par `app/icone/generer_icone.py`, non versionnée (`.gitignore`), comme Groma. Outil de génération (avec Pillow), absent de l'APK |

Le catalogue complet, avec les versions reprises de Marculus, est `gradle/libs.versions.toml`.

## Actions GitHub (mission 016, 2026-10-16)

Licences lues sur l'API GitHub (`/repos/<action>/license`) le 2026-10-16.

| Action | Version | Licence | Où |
|---|---|---|---|
| `actions/checkout` | v4 | MIT | tous les workflows |
| `actions/setup-java` | v4 | MIT | tous les workflows (Temurin 21) |
| `android-actions/setup-android` | v3 | MIT | tous les workflows |
| `gradle/actions/setup-gradle` | v4 | **non déclarée par l'API (NOASSERTION)** : à confirmer, voir ci-dessous | tous les workflows |
| `actions/upload-artifact` | v4 | MIT | `ci.yml`, `badges.yml`, `release.yml` |
| `actions/download-artifact` | v4.3.0, épinglée par SHA | MIT | `badges.yml`, `release.yml` (jobs qui écrivent) |
| `mathieudutour/github-tag-action` | v6.2 | MIT | `release.yml` (calcul de la version, préfixe `v`) |
| `softprops/action-gh-release` | v2.6.2, épinglée par SHA | MIT | `release.yml` (tag et release) |
| `cicirello/jacoco-badge-generator` | v2 | MIT | `badges.yml` (badge de couverture) |
| `reactivecircus/android-emulator-runner` | v2.38.0, épinglée par SHA (commit du tag annoté) | Apache-2.0 | `release.yml` (essai de fumée de l'APK release sur émulateur x86_64, ADR 0011) |

`gradle/actions` est utilisée depuis la mission 001 : sa licence n'est pas déclarée de façon
univoque par l'API. À confirmer par Pascal. C'est un outil de CI, non embarqué dans l'APK ;
la retirer des workflows reste possible sans toucher au code de l'application.
