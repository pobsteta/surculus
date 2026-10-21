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
- **Dettes de la 009** : demander `WRITE_EXTERNAL_STORAGE` à l'exécution (Android 8-9)
  et brancher le déclenchement de la sauvegarde sur les saisies (011, 012) ; essai de
  restauration sur deux téléphones après la 011 et la 012.
- **Pascal (spec § 5)** : l'empreinte des photos au journal exige un type de ligne qui
  n'existe pas ; à trancher avant la mission photo.
- **Pascal (dans sommieR)** : brief vers sommieR, un seul fichier d'ortho tuilé en
  EPSG:3857 à la place d'`ortho.tif` (ADR 0009, cible f).
- **Dettes de la 011** : état « rendue » et nombre de saisies dans la liste des tournées
  (export, 032) ; essai sur téléphone et test des dix secondes par Pascal.
- **Référentiels** : lire le JORF du 10/10/2026 (textes 7 et 8) avant de chiffrer avec le
  barème 2024 (« en vigueur NON vérifié ») ; un test de forme des CSV de
  `docs/referentiels/` (même nombre de colonnes sur chaque ligne).
- **Tests (relecture de la 027)** : une seule emprise France, celle de la spec § 10.9
  (domaine d'usage d'EPSG:2154) ; `FixturesBienFormeesTest` (l. 138) a sa propre boîte.
  `fixtures/README.md` (l. 79) : ajouter `remplace_autre_tournee` aux codes d'anomalie
  (`testeur`).
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

- **033** carte : placettes colorées selon `etat_suivi`, travaux tracés selon leur code,
  vides relevés (`afficheur`), décidée par Pascal le 2026-10-21 ; fiche à écrire après
  lecture des valeurs de `etat_suivi` dans sommieR.
- **013** dictée Vosk (`dev`) : part après les cas des décisions 11 et 12 (testeur) et le
  retrait de l'hypothèse DI-303.

## Planifié

_En attente sous le plafond humain (3 PR en revue)._

**Cycle 6, lancé le 2026-10-21 :**

- **032** état par tournée, tournée obligatoire à la saisie (`dev`, appuyer), avant la 012 ;
  plan demandé.
- **027** registre attendu du scénario Couchey (`testeur`, appuyer) ; plan demandé.

**Attendent le plafond :**

- **012** écran du contrôle de placette (`afficheur`, appuyer), après la 032.
- Cas des décisions 11 et 12 de la dictée (`testeur`), avant la 013.
- **028** registre attendu et sabotage en CI (`dev`, appuyer), après 026 et 027.

## Terminé

- Cycle 5 (suite), 2026-10-21 : **011** écrans Tournées, Carte et Guidage (PR #51) ;
  **030** référentiels de la PR #4 revérifiés (PR #52) ; pilotage (PR #50) — Pascal.

- Cycle 5, 2026-10-21 : **009** sauvegarde `.sursync` et restauration (PR #45) ;
  **029** géométries de `foret`, `ug`, `parcelles` (PR #47) ; **026** critères F1, F2, F3
  en CI d'import (PR #46) ; **031** tournée dans chaque ligne du journal (PR #49) ;
  pilotage (PR #44, #48) — Pascal.

- Cycle 4, 2026-10-21 : **022** critère (a) mesuré en CI, deux échecs montrés (PR #39) ;
  **024** cas de la sauvegarde `.sursync` (PR #40) ; **025** cas de la dictée et
  vocabulaire Vosk (PR #41) ; ADR 0009 carte hors ligne, fiches 026 à 028 (PR #42) — Pascal.

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

- 2026-10-21 : PR #4 (ébauche des référentiels, 2026-10-15) fermée : antérieure à la porte
  n° 1, codes travaux contraires à la décision du 2026-10-15 ; ses cinq documents utiles
  passent à la mission 030.
