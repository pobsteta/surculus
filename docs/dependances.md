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
| Bibliothèques communes Marculus / Groma | GNSS, géodésie, GeoPackage, carte, dictée, journal | GPL-3.0-or-later | Marculus (`792b5aa`) et Groma sont sous GPL-3.0-or-later ; tranche 1 : `groma-core` provisoire (brief `vers-marculus`) |
| Tuiles IGN Géoplateforme, OpenStreetMap | carte | Etalab 2.0, ODbL | attribution affichée, comme dans Groma |

Aucune dépendance non commerciale ni AGPL n'est prévue.
