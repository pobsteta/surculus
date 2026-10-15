# PLAN — Surculus

Source unique de l'état d'avancement. Mis à jour à chaque étape.

## Décisions

| Date | Décision | Par |
|---|---|---|
| 2026-09-23 | Nom : **Surculus**, la jeune pousse, en paire avec Marculus | Pascal |
| 2026-10-15 | **Application séparée** de Marculus, qui reprend les bibliothèques communes (GNSS/NTRIP, géodésie, GeoPackage, carte, dictée, journal) au lieu de les copier | Pascal |
| 2026-10-15 | **Plants comptés et localisés** : chaque plant d'une placette a un point et un UUID stable d'un passage à l'autre ; les totaux du contrôle s'en déduisent | Pascal |
| 2026-10-15 | Licence : **GPL-3.0-or-later**, comme Groma | Pascal |
| 2026-10-15 | **sommieR, destination unique** des saisies ; nemetonshiny fournit le prévu (plan d'action) ; nemeton n'est pas sur le chemin | Claude, à valider |
| 2026-10-15 | Contrat = `travaux.gpkg` de sommieR 0.31.1, sans renommage ; couches ajoutées (`plants`, `etats_plants`, `projet`) ignorées par l'import actuel | Claude, à valider |
| 2026-10-15 | Saisies dans un journal Room append-only ; le GeoPackage n'est écrit qu'à l'export | Claude, à valider |
| 2026-10-15 | Plant sans RTK fixe : pointé en distance et azimut depuis le piquet ; numéro par azimut depuis le nord, sens horaire | Claude, à valider |

## Avancement

| # | Étape | État |
|---|---|---|
| 0 | Socle : README, LICENSE, CLAUDE.md, spécification v1, architecture, dépendances, briefs | fait (cette PR) ; CI et release-please à faire |
| 0b | Briefs déposés : sommieR (plants), nemetonshiny + sommieR (prévu), Marculus (bibliothèques communes) | écrits dans `briefs/`, à déposer |
| 1 | Tournée, carte, contrôle de placette (plants pointés, compteur, dictée, totaux, refus), export `.zip` | à faire |
| 2 | Relevé des interventions (tracé RTK, quantité, prévu/motif) | à faire |
| 3 | Installation de placette, fusion `.sursync` | à faire |
| 4 | Plants au registre, après le brief sommieR `travaux-2` | à faire |

## Points ouverts

1. Bibliothèques communes : forme (module Gradle publié, composite build ou
   sous-module) et licence de Marculus.
2. Rattachement du prévu de nemetonshiny (UGF) aux UG de sommieR.
3. Pointer tous les plants ou seulement certaines placettes : temps de
   terrain à mesurer.
4. Identifiant d'application : `io.github.pobsteta.surculus` ou
   `fr.nemeton.surculus`.

## Prochaine étape

Déposer les trois briefs, puis ouvrir la tranche 1 : squelette Gradle
(`:core`, `:data`, `:app`), CI, et les premiers cas de `fixtures/regles/`.
