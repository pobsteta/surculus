# Dépendances et licences

Licence du dépôt : **GPL-3.0-or-later**, comme Groma et nemeton.

## Prévues (tranche 1)

| Composant | Où | Licence | Remarque |
|---|---|---|---|
| Kotlin, kotlinx-coroutines, kotlinx-serialization | partout | Apache-2.0 | |
| AndroidX, Jetpack Compose, Material 3, Room, DataStore | `:app`, `:data` | Apache-2.0 | |
| osmdroid | carte | Apache-2.0 | |
| NGA geopackage-android | `:data` | MIT | lecture et écriture du `travaux.gpkg` |
| proj4j | `:core` | Apache-2.0 | RGF93 / WGS 84 ↔ Lambert-93, convergence des méridiens |
| Modèle géomagnétique WMM2025 (ou IGRF-14) | `:core` | domaine public (U.S. Government Work, par déduction, à confirmer) | déclinaison hors ligne ; WMM2025 valable jusqu'à fin 2029 |
| Vosk (bibliothèque et modèle français) | dictée | Apache-2.0 | modèle téléchargé une fois avant le terrain (comme Marculus), puis hors ligne ; licence du modèle à vérifier chez alphacephei |
| JUnit 5 | tests | EPL-2.0 | tests seulement |
| Bibliothèques communes Marculus / Groma | GNSS, géodésie, GeoPackage, carte, dictée, journal | GPL-3.0-or-later | Marculus (`792b5aa`) et Groma sont sous GPL-3.0-or-later ; tranche 1 : `groma-core` provisoire (brief `vers-marculus`) |
| Tuiles IGN Géoplateforme, OpenStreetMap | carte | Etalab 2.0, ODbL | attribution affichée, comme dans Groma |

Aucune dépendance non commerciale ni AGPL n'est prévue.
