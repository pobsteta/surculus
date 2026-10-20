# Kanban de Surculus

> Cinq colonnes (fiche n° 8). Chaque carte porte l'Homme (qui), la Mission (quoi, quand)
> et les Moyens (comment, avec quoi), et renvoie à sa fiche dans `pilotage/missions/`.
> **Une carte qui ne bouge pas est souvent la mauvaise nouvelle à dire tout de suite.**
> Tenu à jour par l'orchestrateur à chaque fin de cycle.

## Bac à sable

_Idées, à trier au prochain `/mission`._

- **Orchestrateur + `contradicteur`** : ADR 0006 (fusion, avant la tranche 4), 0008
  (bibliothèques communes), 0009 (carte hors ligne, avant 011), 0010 (distribution et clé
  de signature, avant la première release).
- **Missions 010 et 014** : en attente de moyens (serveur WebDAV d'essai, appareils
  désignés).
- **Pascal** : écrire à Centipède (version de RGF93) ; informer les employeurs des
  agents (finalité des horodatages et positions) ; matériel de pointage ; plantation
  d'essai d'automne 2026 ; successeur ;
  coût (surfaces, prix d'un regarni, ressaisie) ; revérifier les art. 3-1.1 et 3-2 du
  fascicule 34 ; URL du guide des chênaies continentales.
- **Pascal (dans sommieR)** : mettre à jour les briefs `vers-sommier` (couches
  ajoutées, données des plantations à constater, décision « hors entreprise », table de
  correspondance des codes, `montant_eur` facultatif).

## Validé

Missions 006 à 014 validées par Pascal le 2026-10-15 (`pilotage/equipe.md`, PR #13).
Cycle 3 proposé : **006** module GeoPackage, lecture et export (tenir), **007** CI qui
rejoue l'import de sommieR, **008** journal en ajout seul (tenir).

- **023** la méthode en plugin `imperatrice`, le pilotage dans `pilotage/` — orchestrateur,
  décidée par Pascal le 2026-10-20 ; en revue : PR #1 de `methode-imperatrice` à fusionner
  d'abord, puis la PR de Surculus.

## Planifié

## Terminé

- Cycle 2, 2026-10-16 : **003** GNSS de Groma, fraîcheur du fix, lint dans la commande de
  tests (PR #24) ; **005** chaîne des repères, WMM2025 copié de Groma (PR #25) ; **018**
  écran d'essai GNSS (PR #27) ; **004** domaine du contrôle de placette (PR #28) ; clé de
  signature et premières releases avec APK (v0.4.1, v0.5.0) — Pascal.

- Cycle 1, 2026-10-16 : **001** squelette Gradle et CI (PR #16), **002** fixtures sourcées
  et tests d'acceptation (PR #19), **015** icône « S » (PR #18), **016** badges et
  releases automatiques (PR #20, v0.1.0 sans APK) ; hook garde_main (PR #17) — Pascal.

- ADR 0002 à 0005 et 0007, commande de tests, référentiels essences, WMM, Centipède
  (PR #11, #12) — Pascal, 2026-10-15.
- Porte n° 1 franchie le 2026-10-15 : brief (PR #5), références des sources (PR #6),
  ADR 0001 (PR #7), référentiels de plantation (PR #8), spec v1 (PR #9) — Pascal.

## Abandonné

_Avec la raison, en une ligne._
