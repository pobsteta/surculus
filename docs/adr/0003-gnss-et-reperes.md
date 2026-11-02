# ADR 0003 — GNSS et repères : RTK pour le piquet, plants relatifs au piquet, RGF93 v2b

- **Date :** 2026-10-15
- **Statut :** acceptée par Pascal (2026-10-15), après critique du `contradicteur`
- **Proposée par :** Claude (orchestrateur)
- **Tranchée par :** Pascal
- **Amendée le 2026-11-02** (mission 085, décisions de Pascal du 2026-11-02) : ouvrir un contrôle sans position,
  voir « Amendement du 2026-11-02 » plus bas.

## La décision en une phrase

Surculus place le piquet au RTK (récepteur u-blox ZED-F9P sur carte Bluetooth, corrigé
par Centipède, repli par post-traitement ou par le GNSS du téléphone, dit et
enregistré) ; il pointe chaque plant **relativement au piquet** (distance horizontale et
azimut magnétique bruts, observation primaire), dérive ses coordonnées par la chaîne
azimut magnétique + déclinaison − convergence ; il rattache un contrôle au piquet par
son identité quand le GNSS le place hors des 15 m ; et la tournée passera en **EPSG:9794**
(RGF93 v2b / Lambert-93) quand sommieR le fera, d'ici là en EPSG:2154.

## Contexte

But pour le gestionnaire : **retrouver le même plant au passage suivant**, et retrouver
la placette (règle des 15 m de l'import 0.31.1). Spec § 3, § 4.3, § 4.7.

**Erreur corrigée** (l'orchestrateur avait mal cité sa source) : sous couvert, le GNSS
d'un téléphone fait en moyenne 6,1 m d'erreur horizontale, CEP95 13,4 m, jusqu'à 42 m
pour un modèle (Purfürst 2022, tableau 4). Le repli téléphone ne tient donc pas seul la
règle des 15 m.

**Correction du 2026-10-28** (de la phrase « Erreur corrigée » ci-dessus, qui reste telle quelle ; brief de
Ramulus du 2026-10-28, `~/dev/briefs/vers-surculus/2026-10-28-ramulus-citation-purfurst.md`, décision G2 de Ramulus
prise par Pascal). Les chiffres « 6,1 m en moyenne, CEP95 13,4 m » sont ceux de la ligne « All » du tableau 4 de
Purfürst 2022 (x̄ 6,14 m, CEP95 13,38 m, n = 131 338), qui inclut le récepteur géodésique de référence Geo 7X
(n = 12 096) : ils ne décrivent pas les seuls téléphones. Pour les téléphones seuls (10 appareils, n = 119 242) :
moyenne 6,50 m, pondérée ou non ; CEP95 13,38 m en multifréquence (x̄ 5,62 m) et 13,64 m en monofréquence
(x̄ 7,07 m) ; pire téléphone, le Xcover 4, CEP95 41,99 m (x̄ 10,44 m), peut-être porté par des valeurs aberrantes
(CEP99,7 47,25 m, écart-type 10,19 m). Le « 42 m » d'origine tient donc, pour un modèle de téléphone.
**Source relue par l'acquéreur de Surculus le 2026-10-28** (PMC HTML et Europe PMC XML, lignes de données
concordantes), après le calcul de Ramulus (`docs/referentiels/gnss-telephone.yaml` de son dépôt, écart E1).
Le Mi 8 est à 7,20 m (DRMS) dans le tableau, cohérent ; le 7,22 m est une coquille de la Discussion (c'est le DRMS
du P40), présente aussi dans le XML d'Europe PMC.

**Motif corrigé, conclusion gardée** (décisions de Pascal du 2026-10-28 : « Garder, motif corrigé », puis « Corriger, puis fusionner » pour la formulation) : le repli
téléphone ne tient pas seul la règle des 15 m. Par groupe, 95 % des positions tombent sous 13,4 à 13,6 m, ce qui laisse
1,4 à 1,6 m sous 15 m ; mais pris un par un au CEP95, trois téléphones dépassent 15 m (Mi 8 Pro 16,34 m, A7 15,80 m,
S5 16,72 m), et un seul modèle monte à 42 m, le Xcover 4 (CEP95 41,99 m).

**Seuils** : aucun seuil de Surculus ne dépend de ces chiffres (recherche dans la spec, les ADR, `core/` et `app/`).
La règle des 15 m vient de l'import 0.31.1 de sommieR (`R/travaux_qfield.R` l. 303) ; `Controle.DISTANCE_MAX_M` = 15,0
la reprend. Examinés et écartés : les 30 m et 40 m du guidage (arrivée et réarmement), les classes de fix à 0,05, 0,5 et
3 m, le 0,5 m de l'essai terrain. Seul le motif de la conclusion ci-dessus s'appuie sur Purfürst.

**Correction du 2026-11-02** (des mots « règle des 15 m de l'import 0.31.1 » du but ci-dessus et de la phrase « La
règle des 15 m vient de l'import 0.31.1 de sommieR (`R/travaux_qfield.R` l. 303) » des « Seuils », qui restent tels
quels ; relevé du `contradicteur` du 2026-11-02, mission 085). L'import de sommieR n'applique les 15 m **qu'aux
contrôles sans `placette_uuid`** : `rattacher_controles`, `R/travaux_qfield.R` l. 296-305, ne cherche la placette la plus
proche (`st_nearest_feature`, `distances <= RAYON_RATTACHEMENT_M`, `RAYON_RATTACHEMENT_M <- 15` l. 288) que pour les
lignes où `is.na(controles$placette_uuid)` et dont la géométrie n'est pas vide, puis **jette la géométrie**
(`st_drop_geometry`, l. 305). Vérifié par l'`archiviste` le 2026-11-02 dans `~/dev/sommieR`, au tag `v0.31.1` (texte
identique au tag `v0.37.1`, `f0229b9`). Surculus écrit toujours `placette_uuid` : l'import ne lui applique donc **pas** les
15 m, et ne lit pas la position du contrôle. Les 15 m de Surculus (`Controle.DISTANCE_MAX_M`) sont une **règle de
Surculus**, reprise de la valeur de sommieR pour rester cohérente avec une saisie faite dans QField ; ce n'est pas une
condition de l'import. Ce qui change : rien dans la règle des 15 m ; un contrôle ouvert sans position (amendement
du 2026-11-02 plus bas) ne met pas l'import en défaut.

Ce qui est vérifié : az_grille = az_mag + D − γ, avec γ = n·(λ − 3°), n = 0,7256 ; γ
vaut −5,43° à Brest et +3,45° à Strasbourg ; c'est la formule de `groma-core`
(`Azimut.geographiqueVersGrille`, `Projections.kt`, algorithmes IGN ALG0003/ALG0004),
testée par Groma contre pyproj.

Options envisagées :

1. **Carte ZED-F9P en Bluetooth classique (SPP) + NTRIP dans Surculus**, interface
   `Transport` de Groma, ouverte au **Bluetooth basse consommation** (le micrologiciel
   SparkFun RTK Everywhere fournit le NMEA complet en BLE) : retenue.
2. **Position simulée** par une application tierce : perd le statut du fix et la
   précision ; écartée, et **détectée** si elle est active.
3. **GNSS du téléphone seul** : insuffisant (ci-dessus).
4. **Plants au RTK à la canne** : sous couvert fermé à plus de 50-65 %, le RTK bas coût
   dépasse 50 cm (Chalmers) ; deux repères dans une même placette. Écartée par Pascal au
   profit du pointage relatif.

## Règles

| Sujet | Règle |
|---|---|
| Piquet (installation) | RTK fixe exigé ; sans 4G, **journal UBX brut et post-traitement** (RTKLIB, RINEX de la base), comme Groma (`docs/terrain.md`) ; sinon précision obtenue enregistrée et dite |
| Plant | observation primaire **relative au piquet** : distance horizontale (m), azimut magnétique brut (boussole du téléphone en degrés, ou saisi au clavier en secours dans l'unité choisie, degrés ou grades), sa source, déclinaison et convergence appliquées, date ; EPSG dérivé |
| Unités d'azimut | refus **avant normalisation** hors [0, 360[ en degrés et hors [0, 400[ en grades (`groma-core` normalise : à ne pas reprendre tel quel) |
| Déclinaison | modèle géomagnétique embarqué (`docs/referentiels/modele-geomagnetique.md`), au point et à la date ; WMM2025 copié de Groma, bornes 2025-01-01 à 2029-12-31 (mission 005, arbitrage de Pascal du 2026-10-16) ; altitude prise à 0 m (écart < 0,005° pour 500 m, testé) ; ajoutée **une seule fois**, par la session de contrôle : le capteur du téléphone fournit un cap magnétique pur, jamais la déclinaison du système Android (deux modèles ne se cumulent pas ; un test de source l'interdit) |
| Convergence | γ = n·(λ − 3°), formule de `groma-core` ; **une seule** implémentation (proj4j retiré) |
| Règle des 15 m | position **moyennée** quelques secondes, précision enregistrée ; placette la plus proche ; si le fix est hors des 15 m mais que l'agent **confirme l'identité** (code lu sur le piquet), le contrôle prend la **position stockée du piquet** et garde à part la position GNSS mesurée, avec le drapeau « rattaché par identité » (arbitrage de Pascal) |
| Fix | horodaté ; un fix plus vieux que quelques secondes est refusé ; un RTK dont les corrections sont trop vieilles est rétrogradé ; une **position simulée** (`Location.isMock()`) est détectée et enregistrée |
| Référentiel de source | enregistré tel quel (« RGF93, base Centipède X », « WGS 84 non transformé ») |
| Code EPSG | **EPSG:9794** pour toute la tournée **quand sommieR le fera** (brief `vers-sommier`) ; d'ici là EPSG:2154, contrat 0.31.1 (arbitrage de Pascal) |
| Perturbation magnétique | visée inverse proposée ; téléphone tenu à plat, loin de la canne, du récepteur RTK et de toute masse métallique ; précision du capteur et champ magnétique affichés ; étalonnage demandé (« dessinez un 8 ») quand le capteur se dit peu fiable ; on avertit, on ne bloque pas ; seuils fixés à l'essai en forêt |

### Amendement du 2026-10-28 (mission 012-B2, décisions de Pascal du 2026-10-27)

La boussole du téléphone remplace la boussole de visée pour les plants, **sous réserve de l'essai en forêt**
(spec § 10, question 1). Le cap est lu au capteur d'orientation du téléphone, tenu à plat, bord haut vers le
plant, par moyenne de la dernière seconde à l'appui sur « Pointer ». Un clavier de secours permet de taper l'azimut,
dans l'unité choisie à côté du champ (degrés ou grades, mémorisée par tournée). La précision du capteur n'est pas
gardée au journal (format de valeurs v1 inchangé). Les deux pièges de la boussole d'un téléphone sont traités : la
déclinaison n'est ajoutée qu'une fois, et un capteur peu fiable ou un champ perturbé avertit l'agent sans le bloquer.

La ligne « Règle des 15 m » du tableau ci-dessus suppose une position mesurée. Sans aucune position, voir
**« Amendement du 2026-11-02 : ouvrir un contrôle sans position »**, plus bas.

## Le crible FRANCE

| Lettre | Question | Réponse (une ligne) | Ce que ça coûte |
|---|---|---|---|
| **F**rugale | La plus simple qui tienne ? | Oui : RTK au seul piquet, boussole du téléphone et ruban pour les plants, formules de `groma-core`. | Extraction du transport Bluetooth et du service RTK de l'application Groma (tranche 1). |
| **R**esponsable | Qui fait quoi ? | Surculus calcule et enregistre chaque terme ; l'agent déclare l'unité de sa boussole et confirme l'identité du piquet. | Un oubli de déclaration des grades fausse les azimuts : demandée à l'ouverture de la tournée. |
| **A**nticipateur | Si l'imprévu arrive ? | Sans 4G : post-traitement ; GNSS faible : rattachement par identité ; position simulée détectée ; modèle géomagnétique daté. | Dépendance à Centipède, réseau bénévole sans garantie de service ; mise à jour du modèle en 2029 ; boussole du téléphone perturbée par le métal : précision et champ affichés, clavier en secours. |
| **N**ovateur | Toutes les idées ? | Quatre options, BLE gardé ouvert, post-traitement repris de Groma. | Un essai terrain pour pointer quelques plants des deux façons. |
| **C**oût | Coût pour l'ensemble ? | Un récepteur RTK et son antenne par agent (prix à chiffrer par Pascal) ; Centipède gratuit. | Matériel par agent (aucun matériel de plus pour le pointage des plants : le téléphone fait boussole) ; temps de la main ajouté à chaque plant pointé. |
| **É**cologique | Que réutilise-t-on, qui réutilisera ? | `groma-core` (GPL), conventions IGN, Centipède ouvert. | Copie provisoire du transport GNSS de Groma jusqu'à l'ADR 0008. |

## Valeurs sacrifiées

- **Frugalité du matériel** : un récepteur RTK par agent.
- **Rapidité** : chaque plant pointé à la boussole et au ruban au premier passage.
- **Indépendance** : 4G et réseau Centipède, sans garantie de service.
- **Portée** : Android seul (le SPP n'existe pas sur iOS ; le BLE reste possible).
- **Écologie du code** : copie provisoire du transport GNSS.

## Tests exigés

- Fixtures **sourcées** : convergence tirée de pyproj (`meridian_convergence`) et de
  l'outil Circé de l'IGN ; déclinaison tirée du calculateur NOAA ; Brest et Strasbourg.
- Une placette française tombe en France ; une inversion (x, y) ou (lat, lon) est
  détectée.
- Grades et degrés hors plage refusés avant normalisation.
- Position simulée détectée ; fix trop vieux refusé ; corrections trop vieilles
  rétrogradées.
- Sur le terrain : au moins un plant pointé des deux façons (RTK à découvert et
  boussole), pour voir une erreur systématique commune aux deux opérateurs que le
  critère de 0,5 m ne voit pas.

## Amendement du 2026-11-02 : ouvrir un contrôle sans position

- **Décidé par :** Pascal, le 2026-11-02 (journal de pilotage, branche `pilotage/cycle-17`, lignes « cap », « 085 » et
  « ADR 0003 » du 2026-11-02), après la critique du `contradicteur` ; points 3 et 6 précisés et point 10 ajouté le même
  jour, après la relecture de la PR #150 (reprise 1/3 de la mission 085, fiche, « Reprise, aller-retour 1/3 »). Écrit par l'`archiviste` (mission 085) ; codé
  par la mission 086.
- **Ce qui change :** la règle « toujours une position mesurée à l'arrivée » tombe. Sous couvert dense, le téléphone
  peut ne donner aucune position au pied du piquet, et le forestier ne pouvait plus travailler (`ArriveeControle.arrivee`
  rendait `EtatArrivee.SansPosition` et ignorait le code lu).

### La règle (texte de Pascal, repris de la fiche 085 ; points 3, 6 et 10 : reprise 1/3 du 2026-11-02)

La spec § 4.3 reprend la même numérotation, de 1 à 10.

1. **Sans aucun fix** reçu pendant **30 s** (valeur **provisoire**, mesurée à l'essai en forêt), récepteur actif,
   localisation et permission accordées, Surculus propose « Ouvrir sans position ». Localisation coupée ou permission
   refusée : il demande de l'activer, pas d'ouverture sans position.
2. Le contrôle s'ouvre seulement avec un **code lu** sur le piquet qui existe dans la tournée ; **sans code, rien ne
   s'ouvre**.
3. **Code présent dans plusieurs UG** : **la position tranche** : si une seule des placettes de ce code est à 15 m au
   plus de la position mesurée, c'est elle. Le **choix de l'UG** (« P01 : UG 12 ou UG 14 ? ») n'est demandé que si rien
   ne tranche : sans position, ou position loin de toutes (aucune à 15 m au plus). Deux placettes de ce code à 15 m au
   plus : rien ne tranche non plus, l'agent choisit l'UG ; borne de 15 m incluse (décision de Pascal du 2026-11-02, reprise 2/3). Corrige aussi le cas
   avec position, aujourd'hui `firstOrNull` dans `core/.../placette/Controle.kt` l. 55. (Texte du 2026-11-02 matin :
   « l'agent choisit l'UG, avec ou sans position » ; précisé par Pascal le 2026-11-02, « la position tranche ».)
4. Avant d'ouvrir sans position, Surculus **affiche la placette à confirmer** (« P35-01 · UG 12 · parcelle 4 · 25
   plants pointés : ouvrir sans position ? »).
5. Le contrôle **garde la position de la placette créée par sommieR** (« par défaut si pas de position, on garde cette
   position », Pascal) ; pas de position mesurée.
6. Un fix reçu, **même unique, médiocre ou simulé**, reste une position et suit la règle actuelle (15 m, ou code lu
   qui l'emporte). **Hors de France : refusé**, avec une phrase qui dit quoi faire (« position aberrante : couper puis
   rallumer la localisation »), le refus écrit au **journal technique** (`JournalTechnique`, diagnostic), **pas au
   journal des saisies**, dont la liste des types de ligne ne change pas (précisé par Pascal le 2026-11-02).
7. **Export** : dans `positions_controles`, une ligne par contrôle, géométrie **NULL** (pas un POINT EMPTY),
   `precision_m` NULL, nouvelle colonne `sans_position_mesuree` (booléen), `rattache_par_identite` vrai (définition
   élargie : le code lu a désigné la placette, la position mesurée ne l'a pas fait) ; `controles` porte la position du
   piquet (l'import 0.31.1 lit `placette_uuid` et jette cette géométrie, `R/travaux_qfield.R` l. 296-305).
8. **Journal des saisies** : **format v2**, qui admet l'absence de position mesurée quand `sans_position_mesuree` est
   vrai ; Surculus relit toujours le v1 (principe N-1, comme `.sursync`).
9. L'**installation d'une placette** exige toujours une position (inchangé).
10. **Fix reçu après une ouverture sans position** : rien ne change, la position est « fixée à l'arrivée ». Le contrôle
    reste « sans position mesurée » jusqu'à la fin ; « Terminer » le reprend tel quel (décision de Pascal du
    2026-11-02, reprise 1/3).

**Sauvegarde `.sursync`** (décision de Pascal du 2026-11-02, reprise 1/3, « nouvelle version ») : la sauvegarde passe à
la version suivante de son format ; un Surculus ancien la refuse avec un message (`version_future`, « mettre
l'application à jour », `docs/formats/sursync-v1.md`) au lieu de perdre sans le dire les contrôles sans position ; un
Surculus à jour lit toujours l'ancienne. **Une seule v2 du `.sursync`**, partagée (décision de Pascal du 2026-11-02, reprise 2/3, « v2 partagée ») : elle porte à
la fois le type `photo` (décision du 2026-10-28, spec § 4.8) et les contrôles sans position. La 086 crée la v2 ; la
partie photos reste à coder plus tard, dans le même format.

**Écart avec l'existant, signalé et non tranché ici.** « Une ligne par contrôle » dans `positions_controles` (point 7)
contredit le scénario Couchey de la CI d'import : P35-02, « sans mesure », n'a aujourd'hui **aucune** ligne
(`gpkg/.../ExportTravaux.kt` l. 230, ligne écrite seulement s'il y a une position mesurée) ; F4 prend alors le piquet
dans `placettes` (ADR 0007, critère F4 ; `ci/sommier/README.md` l. 54-66 ; `fixtures/export/registre-attendu-couchey.json`,
« decision »). La 086 en fera un contrôle `sans_position_mesuree`, ou dira pourquoi non ; si cela touche au sens (ce que
F4 prouve), la question remonte à Pascal.

**Valeur provisoire :** les **30 s** d'attente sans fix. Mesurées par Pascal à l'essai en forêt sous couvert (premier
fix à froid du récepteur), date à fixer ; gelées avant la publication de la tranche 1.

Où la règle est écrite pour le code : spec § 4.3 (« Arrivée sans position »), § 5 (`positions_controles`), § 6 (règles
vérifiées) ; `docs/formats/journal-valeurs-v2.md` ; brief `pilotage/briefs/vers-sommier/2026-10-22-position-mesuree-du-controle.md`
(complément du 2026-11-02).

### Pourquoi une géométrie NULL et un `precision_m` NULL

Sources vérifiées par l'`archiviste` le 2026-11-02 (pages téléchargées et lues) :

- **Darwin Core**, `dwc:coordinateUncertaintyInMeters` (https://dwc.tdwg.org/terms/#dwc:coordinateUncertaintyInMeters) :
  « Leave the value empty if the uncertainty is unknown, cannot be estimated, or is not applicable (because there are
  no coordinates). Zero is not a valid value for this term. » D'où `precision_m` **NULL**, jamais 0.
  `dwc:georeferenceSources` (même page) : la liste des ressources qui ont servi à géoréférencer le lieu, « described
  specifically enough to allow anyone in the future to use the same resources » : c'est le rôle de
  `sans_position_mesuree` (la position vient de sommieR, pas d'un récepteur).
- **QField**, variables d'expression (https://docs.qfield.org/reference/expression_variables/) : quand le réticule est
  déplacé à la main, « @position_horizontal_accuracy > The value is NULL » et `@position_source_name` vaut
  `manual`. L'outil que sommieR utilise déjà dit « pas de précision » par NULL, pas par 0.
- **GeoPackage 1.3** (https://www.geopackage.org/spec130/, exigence 152, § 2.1.3.1.1) : un point vide s'encode avec des
  coordonnées NaN et le drapeau « vide » ; la norme note que les GeoPackage 1.2.1 et antérieurs étaient
  « underspecified » sur les géométries vides et que des lecteurs peuvent s'y tromper. `ST_IsEmpty` rend NULL pour
  une géométrie NULL (même page, extension SQL). Une géométrie **NULL** dit « pas de valeur », sans dépendre de
  l'encodage des NaN. Que GDAL et `sf` lisent une géométrie NULL de GeoPackage comme une géométrie vide : **non
  vérifié** (à couvrir par le test F4 de la 086).

**Choix de format** (proposés par le `relecteur` de la PR #150, non tranchés par Pascal, écrits comme tels) : sans
position mesurée, `source_gnss`, `referentiel_source` et `simulee` sont aussi **NULL** ; `sans_position_mesuree` porte
seul l'information (spec § 5).

### Le crible FRANCE de l'amendement

| Lettre | Question | Réponse (une ligne) | Ce que ça coûte |
|---|---|---|---|
| **F**rugale | La plus simple qui tienne ? | Oui : le code lu, déjà saisi pour le rattachement par identité, désigne la placette ; la position est celle que sommieR a créée. | Un écran de confirmation et un choix de l'UG ; un format de journal v2. |
| **R**esponsable | Qui fait quoi ? | L'agent lit le code et confirme la placette ; Surculus dit qu'il n'a pas de position et le marque à l'export ; sommieR décide de ce qu'il garde. | L'identité repose sur l'agent : un code mal lu rattache le contrôle à la mauvaise placette, sans GNSS pour le voir. |
| **A**nticipateur | Si l'imprévu arrive ? | Plus de forestier bloqué sous couvert ; localisation coupée ou permission refusée : on demande de l'activer ; fix hors de France refusé avec une phrase qui dit quoi faire. | Un fix simulé ou médiocre reste une position (point 6) ; 30 s non mesurées ; un ancien Surculus refuse la sauvegarde de version suivante (message « mettre l'application à jour ») plutôt que de perdre un contrôle sans position. |
| **N**ovateur | Toutes les idées ? | Examinées par le `contradicteur` : code sans confirmation (écarté), attente de 5 s (trop courte, écartée), POINT EMPTY (écarté pour NULL), position de sommieR (retenue). | Aucune. |
| **C**oût | Coût pour l'ensemble ? | Pas de matériel ; une mission de code (086) et un complément au brief `vers-sommier`. | Le registre de sommieR aura des contrôles sans position mesurée, à traiter à part. |
| **É**cologique | Que réutilise-t-on, qui réutilisera ? | Conventions ouvertes : Darwin Core, comportement de QField, GeoPackage 1.3. | Aucune. |

### Valeurs sacrifiées

- **Justesse de la position** : un contrôle sans position mesurée ne prouve pas où il a été fait ; seul le code lu
  le rattache.
- **Contrôle croisé** : sans GNSS, rien ne signale un code mal lu ou une placette confondue ; la confirmation (point
  4) est le seul garde-fou.
- **Rapidité** : 30 s d'attente et une confirmation de plus avant d'ouvrir ; un choix d'UG quand le code se répète et
  que la position ne tranche pas.
- **Position tardive** : un fix reçu après l'ouverture sans position n'est pas gardé (point 10).
- **Simplicité du format** : un journal v2 à côté du v1, et une version suivante du `.sursync`, que Surculus doit lire
  avec la précédente.

### Tests attendus (mission 086)

- Aucun fix pendant 30 s, récepteur actif, localisation et permission accordées, code lu connu : « Ouvrir sans
  position » proposé, placette affichée à confirmer, contrôle ouvert à la position du piquet, sans position mesurée.
- Aucun fix et aucun code : rien ne s'ouvre.
- Moins de 30 s sans fix : « Ouvrir sans position » n'est pas proposé.
- Localisation coupée, ou permission refusée : Surculus demande de l'activer ; « Ouvrir sans position » n'est pas
  proposé.
- Code présent dans deux UG : une seule à 15 m au plus de la position mesurée, c'est elle, sans question ; sans
  position, ou aucune à 15 m au plus, l'agent choisit l'UG ; jamais le premier trouvé.
- Un seul fix reçu (même médiocre ou simulé) : règle des 15 m ou du code lu, pas d'ouverture sans position.
- Fix hors de France avec un code lu connu : refus, phrase « position aberrante : couper puis rallumer la
  localisation », refus écrit au journal technique (`JournalTechnique`), aucune ligne ajoutée au journal des saisies,
  pas d'ouverture sans position (placette en Somalie).
- Fix reçu après une ouverture sans position : le contrôle reste sans position mesurée ; « Terminer » le reprend tel
  quel.
- Sauvegarde : écrite dans la version suivante du `.sursync` ; refusée par un lecteur de la version d'avant
  (`version_future`) ; la version d'avant toujours lue par le lecteur à jour.
- Journal v2 : un contrôle sans position mesurée écrit, l'application redémarrée, le contrôle relu ; une valeur v1
  toujours relue à l'identique.
- Export : une ligne de `positions_controles` par contrôle sans position, géométrie NULL (pas un POINT EMPTY),
  `precision_m` NULL, `sans_position_mesuree` vrai, `rattache_par_identite` vrai ; `controles` à la position du
  piquet ; la tournée passe toujours l'import 0.31.1 (CI d'import, F4 adapté).
- Installation d'une placette sans position : toujours refusée.

## Sources

- Purfürst, *Sensors* 22(3):1289, 2022, tableau 4 : https://pmc.ncbi.nlm.nih.gov/articles/PMC8838512/
- Chalmers ODR, RTK bas coût et professionnel en forêt : https://odr.chalmers.se/items/47010366-ab19-419e-a491-b484197cd571
- Centipède, positionnement des bases et systèmes de référence :
  https://docs.centipede.fr/docs/base/positionnement-france.html ,
  https://docs.centipede.fr/docs/centipede/4_Systeme2reference.html
- EPSG:9794, RGF93 v2b / Lambert-93 : https://spatialreference.org/ref/epsg/9794
- SparkFun RTK Everywhere, Bluetooth : https://docs.sparkfun.com/SparkFun_RTK_Everywhere_Firmware/connecting_bluetooth/
- Android, `Location.isMock()` : https://developer.android.com/reference/android/location/Location#isMock()
- Suunto KB-14 (1/3°) : https://us.suunto.com/products/suunto-kb-14-360r-g-compass
- `groma-core` : `Azimut.kt`, `Projections.kt`, `Gnss.kt` ; Groma : `gnss/Transports.kt`,
  `gnss/ServiceGnssRtk.kt`, `gnss/SourcePosition.kt`, `docs/terrain.md` ;
  `core/tests/test_azimut.py`.
- sommieR, `R/travaux_qfield.R` l. 89 et 94 (lecture forcée en EPSG:2154).
- Ajoutées le 2026-11-02 (amendement « sans position ») : sommieR, `R/travaux_qfield.R` l. 284-306
  (`rattacher_controles`), tag `v0.31.1` ; Darwin Core, https://dwc.tdwg.org/terms/ (`coordinateUncertaintyInMeters`,
  `georeferenceSources`) ; QField, https://docs.qfield.org/reference/expression_variables/ ; GeoPackage 1.3,
  https://www.geopackage.org/spec130/ (exigence 152).

## Question de contrôle

Cette décision tient-elle nos six valeurs, et que sommes-nous prêts à payer pour cela ?
