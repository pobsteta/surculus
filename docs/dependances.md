# Dépendances et licences

Licence du dépôt : **GPL-3.0-or-later**, comme Groma et nemeton.

## Prévues (tranche 1)

| Composant | Où | Licence | Remarque |
|---|---|---|---|
| Kotlin, kotlinx-coroutines, kotlinx-serialization | partout | Apache-2.0 | |
| AndroidX, Jetpack Compose, Material 3, Room, DataStore | `:app`, `:data` | Apache-2.0 | |
| osmdroid | carte | Apache-2.0 | |
| NGA geopackage-android | `:data` | MIT | lecture et écriture du `travaux.gpkg` |
| proj4j | `:core` | Apache-2.0 | Lambert-93 ↔ WGS 84 |
| Vosk (bibliothèque et modèle français) | dictée | Apache-2.0 | modèle téléchargé à la demande, comme Marculus |
| JUnit 5 | tests | EPL-2.0 | tests seulement |
| Bibliothèques communes Marculus / Groma | GNSS, géodésie, GeoPackage, carte, dictée, journal | à fixer | Marculus n'a pas de fichier `LICENSE` : à préciser avant toute reprise (brief `vers-marculus`) |
| Tuiles IGN Géoplateforme, OpenStreetMap | carte | Etalab 2.0, ODbL | attribution affichée, comme dans Groma |

Aucune dépendance non commerciale ni AGPL n'est prévue.
