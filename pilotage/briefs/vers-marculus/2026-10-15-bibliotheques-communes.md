# Brief Marculus — extraire les bibliothèques communes de terrain

> **Statut** : traité le 2026-10-17 par la session `marculus` : licence GPL-3.0-or-later, puis bibliothèque `terrain` (github.com/pobsteta/terrain, JitPack) de v0.1.2 (`terrain-gnss`, Marculus `3377464`) à v0.5.0 (`terrain-journal`, `-gpkg`, `-voix`, `-carte`, Marculus `87c3cf4`). Copie classée : `~/dev/briefs/traites/2026-10-17-traite-bibliotheques-communes.md` ; réponses : `~/dev/briefs/vers-surculus/2026-10-17-terrain-gnss-publie.md` et `~/dev/briefs/vers-groma/2026-10-17-bibliotheques-terrain.md`. Déplacement de ce fichier laissé à la session Surculus.

*Établi le 15 octobre 2026 depuis le dépôt `surculus`, à partir de Marculus
(`87f5720`) et de Groma (`af551dc`). Concerne aussi Groma, qui adoptera les
mêmes bibliothèques dans un second temps.*

## Pourquoi

Trois applications Android de la plateforme Néméton vont sur le terrain
avec le même matériel (smartphone, ZED-F9P en Bluetooth, NTRIP Centipède,
GeoPackage hors ligne) : Marculus, Groma et désormais **Surculus** (suivi
des travaux et des plantations). Décision de Pascal du 15 octobre 2026 :
Surculus est une application séparée **qui reprend les bibliothèques
communes**, sans recopier le code.

Aujourd'hui, ces briques existent en deux copies :

| Brique | Marculus (`fr.marculus.core`, `:data`) | Groma (`groma.core`) |
|---|---|---|
| NMEA | `NmeaParser`, `NmeaDecoupeur`, `FixGnss` | `Nmea`, `Gnss` |
| NTRIP | `Ntrip` | `Ntrip` |
| Géodésie, projections | `Geodesie`, `GrilleWebMercator` | `Geodesie`, `Projections` |
| GeoPackage | `CouchesGpkg`, `GpkgRepository` | `GeoPackage` |
| Carte | écran Carte (osmdroid, couches à cocher) | `Carte`, écran Carte |
| Dictée | `voice/` (grammaire fermée Vosk, push-to-talk) | — |
| Journal, fusion | journal `tige`, `MergeDao`, `.marsync` | — |

`groma/docs/dependances.md` note que le code GNSS de Groma est « repris de
Marculus » et que **Marculus n'a pas de fichier `LICENSE`**. Une troisième
copie dans Surculus multiplierait les corrections à faire en trois endroits
(une trame NMEA mal découpée, un mot de passe NTRIP mal encodé).

## Demandé

1. **Donner une licence à Marculus** : ajouter `LICENSE`
   (GPL-3.0-or-later, comme Groma, nemeton et Surculus), puisque son code
   est déjà repris sous GPL par Groma.
2. **Extraire, sans changer le comportement de Marculus**, des modules
   publiables, dans l'ordre de leur utilité pour Surculus :
   - `terrain-gnss` (Kotlin pur, JVM) : NMEA (découpe, analyse, fix,
     précision), client NTRIP, géodésie (distances, aires, azimuts),
     projections Lambert-93 / WGS 84 / Web Mercator ;
   - `terrain-gpkg` (Android) : lecture des couches par nom, écriture
     d'entités, reprojection ;
   - `terrain-journal` (Kotlin pur) : saisie à UUID, horodatage `modifie`,
     fusion par union sur l'UUID (règle « dernière écriture gagne ») ;
   - `terrain-voix` (Android) : moteur Vosk à grammaire fermée, déclencheurs
     push-to-talk (micro, volume bas maintenu), rejet du hors grammaire ;
     la grammaire elle-même reste dans chaque application ;
   - `terrain-carte` (Android, en dernier) : fonds IGN / OSM, couches à
     cocher avec leur nombre d'objets, badge du fix.
3. **Les rendre consommables** par les autres dépôts. Proposition : un dépôt
   `pobsteta/terrain` (nom à choisir), publié par tag via JitPack ou GitHub
   Packages ; Marculus le consomme en premier, à iso-fonctionnalité, ses
   tests JVM existants faisant foi.
4. **Garder les tests** : les tests `:core` de Marculus sur ces briques
   déménagent avec elles ; Groma ajoute ses cas (NMEA, NTRIP) au même jeu.

## Critères d'acceptation

- Marculus compile et passe ses tests en consommant les modules, sans
  changement visible sur le terrain (RTK, dictée, carte, synchronisation).
- Un projet vide déclare `terrain-gnss` et lit une trame GGA RTK fixe.
- Aucune dépendance nouvelle non commerciale ni AGPL.

## Calendrier proposé

`terrain-gnss` et `terrain-journal` d'abord : ce sont eux dont la tranche 1
de Surculus a besoin (contrôle de placette au RTK, saisies à UUID). En
attendant, Surculus peut démarrer sur une reprise provisoire signalée dans
`docs/dependances.md`, à remplacer dès la première publication.
