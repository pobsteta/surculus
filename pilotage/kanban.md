# Kanban de Surculus

> Cinq colonnes (fiche n° 8). Chaque carte porte l'Homme (qui), la Mission (quoi, quand)
> et les Moyens (comment, avec quoi), et renvoie à sa fiche dans `pilotage/missions/`.
> **Une carte qui ne bouge pas est souvent la mauvaise nouvelle à dire tout de suite.**
> Tenu à jour par l'orchestrateur à chaque fin de cycle.

## Bac à sable

_Idées, à trier au prochain `/mission`._

- **`terrain-style` v0.6.1** : à sa publication, remplacer la copie littérale du thème
  (038) par la dépendance ; le test d'égalité doit rester vert.
- **Choix Système / Clair / Sombre** (Marculus v0.65.0), proposé par la session `marculus`.

- **Orchestrateur + `contradicteur`** : ADR 0006 (fusion, avant la tranche 4), 0010
  (voir la carte « clé de signature » au Planifié), 0012 (briques communes vers `terrain`,
  réservée, après l'essai d'automne et une table des écarts ; entrée : réponse de Marculus
  du 2026-10-17, `~/dev/briefs/traites/2026-10-23-traite-terrain-gnss-publie.md`) ;
  réexamen de l'ADR 0004 (osmdroid archivé depuis le 2024-11-20, ADR 0009) : la 034 n'y
  ajoute qu'un renvoi daté.
- **Dettes de la 009** : demander `WRITE_EXTERNAL_STORAGE` à l'exécution (Android 8-9)
  et brancher le déclenchement de la sauvegarde sur les saisies (011, 012) ; essai de
  restauration sur deux téléphones après la 011 et la 012.
- **Pascal (spec § 5)** : l'empreinte des photos au journal exige un type de ligne qui
  n'existe pas ; à trancher avant la mission photo.
- **Pascal (dans sommieR)** : brief vers sommieR, un seul fichier d'ortho tuilé en
  EPSG:3857 à la place d'`ortho.tif` (ADR 0009, cible f).
- **Dettes de la 011** : état « rendue » et nombre de saisies dans la liste des tournées
  (export, 032) ; l'essai sur téléphone est sur la carte « terrain avant la 012 ».
- **Référentiels** : lire le JORF du 10/10/2026 (textes 7 et 8) avant de chiffrer avec le
  barème 2024 (« en vigueur NON vérifié »).
- **Tests (relecture de la 027)** : une seule emprise France, celle de la spec § 10.9
  (domaine d'usage d'EPSG:2154) ; `FixturesBienFormeesTest` (l. 138) a sa propre boîte.
  `fixtures/README.md` (l. 79) : ajouter `remplace_autre_tournee` aux codes d'anomalie
  (`testeur`).
- **Emprise France** : y min vaut 6 005 294,71 dans le code (PROJ, densify 50) et
  6 005 281,2 dans `fixtures/acceptation-export/repere-et-france.json` (epsg.io), soit
  13,5 m d'écart, sans effet sur le test de la Corse ; laquelle fait foi (Pascal) ?
- **Carte, après la 012** : l'état d'une placette contrôlée pendant la tournée, « mission
  à part, après 012 » (Pascal, 2026-10-22 ; l'option proposait de faire primer l'état local
  sur `etat_suivi`) ; travaux et vides au bac à sable (Pascal, même jour), à dessiner quand
  l'app les saisira (la tournée de sommieR n'en porte aucun).
- **Pascal (ADR 0008)** : note de Marculus du 2026-10-24 (`~/dev/briefs/vers-surculus/`) :
  `terrain-style` v0.6.0 fait disparaître le vert, Marculus l'a annulé ; le thème actuel de
  Marculus (5,17:1) a été jugé « parfait » au soleil. L'ADR 0008 (mêmes jetons, 7:1)
  tient-elle ? Proposition de l'orchestrateur : trancher au retour de Couchey, avant la 038.
  Marculus a aussi envoyé un brief à `terrain` pour une palette qui reste verte, et ajouté
  un choix Système / Clair / Sombre (« utile peut-être pour Surculus »).
- **Maintenance** : le commentaire du test « ECART terrain-voix »
  (`core/src/test/.../dictee/InstallationModeleTest.kt` l. 86) dit « brief à venir » ; le
  faire pointer vers pobsteta/terrain#23 à la prochaine mission de code (Pascal, 2026-10-24).
- **Maintenance** : la doc d'`ExporterScenario.kt` (l. 51) cite `tournee-sans` au lieu
  de `tournee-depart`.
- **Missions 010 et 014** : en attente de moyens (serveur WebDAV d'essai, appareils
  désignés).
- **Pascal** : écrire à Centipède (version de RGF93) ; informer les employeurs des
  agents (finalité des horodatages et positions) ; matériel de pointage ; plantation
  d'essai d'automne 2026 ; successeur ;
  coût (surfaces, prix d'un regarni, ressaisie) ; revérifier les art. 3-1.1 et 3-2 du
  fascicule 34 ; URL du guide des chênaies continentales.
- **Briefs envoyés par issue et par note dans `~/dev/briefs/vers-*/` le 2026-10-23**
  (ordre de Pascal ; désormais les deux canaux) : style commun vers
  `terrain` (pobsteta/terrain#1, `terrain-style`, à faire en premier), Marculus
  (pobsteta/marculus#2) et Groma (pobsteta/groma#57) ; temps de la main vers la méthode
  (pobsteta/methode-imperatrice#18 ; Pascal y tranche les points 3 à 5, et corrige le
  livre). La mission dans `terrain` compte dans le plafond humain (Pascal, 2026-10-23).
  Surculus migre `equipe.md` et ses fiches après la 1.0.0 du plugin.
- **Pascal (dans sommieR)** : déposer le brief
  `vers-sommier/2026-10-22-position-mesuree-du-controle.md` (le registre ne garde pas la
  position mesurée du contrôle, décision du 2026-10-22).
- **Pascal (dans sommieR)** : mettre à jour les briefs `vers-sommier` (couches
  ajoutées, données des plantations à constater, décision « hors entreprise », table de
  correspondance des codes, `montant_eur` facultatif).

## Validé

- **038** thème commun, le vert de Marculus (`dev`, appuyer), en cours depuis le
  2026-10-24 : valeurs de Marculus v0.65.0 en littéral, clair et sombre, thème clair
  forcé, test d'égalité rôle par rôle (ADR 0008, amendement du soir). Essai au soleil de
  l'APK de la branche par Pascal avant fusion. Fiche :
  `pilotage/missions/038-theme-commun.md`. PR #81 relue, prête ; attend l'essai avec la 040.
- **040** accueil sur la liste des tournées, menu à trois points comme Marculus (`dev`,
  appuyer), en cours depuis le 2026-10-24, sur la branche de la 038. Fiche :
  `pilotage/missions/040-accueil-tournees.md`. PR #82, prête après reprise.
- **041** recherche et puce « Rendues (n) » (`dev`, appuyer), sur la branche de la 040.
  PR #83, en relecture. Fiche : `pilotage/missions/041-recherche-tournees.md`.

## Planifié

_En attente sous le plafond humain (3 PR en revue)._

**Attendent le plafond :**

- **042** styles et tailles de texte de Marculus (décision du 2026-10-24, seconde) (`dev`, appuyer : tableau
  des styles validé par Pascal avant le code). Après la 040 et la 041. Fiche :
  `pilotage/missions/042-typographie.md`.

- **Pascal, terrain avant la 012** (bilan du 2026-10-21) : APK de la 011 sur Couchey
  (import, ortho, mode avion, guidage, test des dix secondes) ; restauration de
  `exemple-v1.sursync` ; comptes de Couchey contre QGIS (029) ; **essai au soleil** de la
  palette actuelle (ADR 0008 : badge du fix, texte orange, pastilles sur ortho claire,
  marqueur noir de la position, bouton « Retour »). Chaque essai au journal.
- **Pascal, clé de signature** : dire au journal où est la copie hors du poste et qui
  détient les mots de passe, puis l'ADR 0010.
- **012** écran du contrôle de placette (`afficheur`, appuyer), après la 032 et le terrain.
- **Palette d'état et légende en noir** (sorties de la 038 le 2026-10-24) : après l'essai
  au soleil de Couchey.

## Terminé

- Cycle 9, 2026-10-23 et 24 : **037** DI-303 suit la décision 13 (PR #73) ; **013** dictée
  Vosk à grammaire fermée, `terrain-voix` épinglé (PR #75) ; **039** cas de « hauteur »
  (PR #78) ; ADR 0008 amendée, fiche 038 (PR #74) ; décisions (PR #76) ; brief vers
  `terrain`, pobsteta/terrain#23 (PR #77) — Pascal.

- Cycle 8, 2026-10-22 et 23 : **028** registre attendu (F4) et sabotages (F5) dans la CI
  d'import (PR #64) ; **036** cas des décisions 11 et 12 de la dictée (PR #63) ; **033**
  placettes colorées selon `etat_suivi`, légende avec décomptes (PR #65) ; brief et spec
  amendés, style commun et liste du départ (PR #68) ; **ADR 0008** style commun acceptée,
  briefs vers `terrain`, Marculus et Groma (PR #69) ; pilotage (PR #62, #66, #67) — Pascal.

- Cycle 7, 2026-10-22 : **034** trous de mémoire du bilan, README, spec, index des ADR
  (PR #58) ; **035** test de forme des CSV de `docs/referentiels/`, vu rouge 9 sur 9
  (PR #59) ; pilotage (PR #60) — Pascal.

- Cycle 6, 2026-10-22 : **027** registre attendu du scénario Couchey (PR #55) ; **032** état
  par tournée, tournée obligatoire (PR #56) ; pilotage et bilan (PR #53, #54) ; outillage
  `ANDROID_HOME` (PR #57) — Pascal.

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

## Livraisons

_Une ligne par cycle, ajoutée sous la précédente à la fin de chaque `/boucle`, et une
par `/bilan` : la livraison que Pascal a retenue, pour qui, et ce qu'elle apporte de plus
que la précédente du même rythme. Un cycle sans livraison neuve livre sa mauvaise
nouvelle, car
« une mauvaise nouvelle est une livraison sur laquelle on apprend » (Pascal) : on
l'inscrit avec ce qu'elle nous apprend. Rubrique ouverte le 2026-10-22 (plugin
imperatrice 0.3.0) : les cycles 1 à 8 ne sont pas inscrits rétroactivement._

```
- cycle N · livraison · pour qui · ce qu'elle apporte de plus que la précédente
- cycle N · mauvaise nouvelle : … · pour qui · ce qu'elle nous apprend
- bilan du AAAA-MM-JJ · livraison · pour qui · ce qu'elle apporte de plus que la précédente
- bilan du AAAA-MM-JJ · mauvaise nouvelle : … · pour qui · ce qu'elle nous apprend
```

- cycle 8 · la carte colore les placettes selon `etat_suivi` (jamais, à jour, travaux à
  prévoir), avec une légende qui les compte (033) · pour le forestier en forêt · avant, toutes
  les placettes avaient la même couleur : il voit d'un coup d'œil celles à contrôler
  d'abord. *(Retenue par Pascal le 2026-10-23.)*
- cycle 9 · mauvaise nouvelle : le moteur de dictée (013) et ses 167 cas sont prêts, mais
  le forestier ne peut pas encore dicter en forêt, aucun écran ne l'appelle avant la 012 · pour
  le forestier en forêt · découper une fonction sans son écran ne livre rien sur le terrain.
  *(Proposée par l'orchestrateur, close par Pascal le 2026-10-24 : « fusionne et clos ».)*
