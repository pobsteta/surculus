# Kanban de Surculus

> Cinq colonnes (fiche n° 8). Chaque carte porte l'Homme (qui), la Mission (quoi, quand)
> et les Moyens (comment, avec quoi), et renvoie à sa fiche dans `pilotage/missions/`.
> **Une carte qui ne bouge pas est souvent la mauvaise nouvelle à dire tout de suite.**
> Tenu à jour par l'orchestrateur à chaque fin de cycle.

## Bac à sable

_Idées, à trier au prochain `/mission`._

- **Orchestrateur + `contradicteur`** : ADR 0006 (fusion, avant la tranche 4), 0008
  (bibliothèques communes), 0010 (distribution et clé de signature) ; réexamen de l'ADR
  0004 (osmdroid archivé depuis le 2024-11-20, ADR 0009).
- **Pascal (dans sommieR)** : brief vers sommieR, un seul fichier d'ortho tuilé en
  EPSG:3857 à la place d'`ortho.tif` (ADR 0009, cible f).
- **Maintenance** : la doc d'`ExporterScenario.kt` (l. 51) cite `tournee-sans` au lieu
  de `tournee-depart`.
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

- **009** sauvegarde `.sursync` (`dev`) : moyens prêts dès la fusion de la PR #40 (cas de la 024).
- **013** dictée Vosk (`dev`) : moyens prêts dès la fusion de la PR #41 (cas de la 025) et
  l'ajout des cas des décisions 11 et 12 par le `testeur`.
- **011** écrans tournées, carte, guidage (`afficheur`) : ADR 0009 acceptée, prête dès la
  fusion de la PR de pilotage.

## Planifié

_En attente sous le plafond humain (3 PR en revue)._

- **026** CI d'import, F1 + F2 + F3 (`dev`, appuyer), après la fusion de la #39.
- **027** registre attendu du scénario Couchey (`testeur`, appuyer).
- **028** registre attendu et sabotage en CI (`dev`, appuyer), après 026 et 027.

**En revue chez Pascal :**

- **022** critère (a) mesuré en CI, deux échecs montrés (PR #39) — relecteur : prêt.
- **024** cas de la sauvegarde `.sursync` (PR #40) — relecteur : prêt.
- **025** cas de la dictée (PR #41) — relecteur : prêt, après la PR de pilotage.
- **PR de pilotage** — à fusionner en premier (au-delà du plafond, décision de Pascal).

## Terminé

- Cycle 3, 2026-10-17 : **006** module GeoPackage (PR #34), **007** CI import sommieR
  (PR #33), **008** journal en ajout seul (PR #35), **019** et **021** scénarios du journal
  (PR #31, #35), **020** cas de l'export (PR #36), arbitrages (PR #37) — Pascal.

- 2026-10-20 : **023** la méthode passe au plugin `imperatrice` v0.1.0 (GPL-3.0, CI et
  releases automatiques, pobsteta/methode-imperatrice#1), le pilotage dans `pilotage/`,
  README centré sur l'application (PR #38) — Pascal.

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
