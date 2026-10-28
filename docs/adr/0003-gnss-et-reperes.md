# ADR 0003 — GNSS et repères : RTK pour le piquet, plants relatifs au piquet, RGF93 v2b

- **Date :** 2026-10-15
- **Statut :** acceptée par Pascal (2026-10-15), après critique du `contradicteur`
- **Proposée par :** Claude (orchestrateur)
- **Tranchée par :** Pascal

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
du P40), lue sur le seul HTML.

**Motif corrigé, conclusion gardée** (décision de Pascal du 2026-10-28, « Garder, motif corrigé ») : le repli
téléphone ne tient pas seul la règle des 15 m, parce qu'un CEP95 de 13,4 à 13,6 m laisse 5 % des positions au-delà
et ne laisse aucune marge sous 15 m, et que certains modèles montent à 42 m.

**Seuils** : aucun seuil de Surculus ne dépend de ces chiffres (recherche dans la spec, les ADR, `core/` et `app/`).
La règle des 15 m vient de l'import 0.31.1 de sommieR (`R/travaux_qfield.R` l. 303) ; `Controle.DISTANCE_MAX_M` = 15,0
la reprend. Examinés et écartés : les 30 m et 40 m du guidage (arrivée et réarmement), les classes de fix à 0,05, 0,5 et
3 m, le 0,5 m de l'essai terrain. Seul le motif de la conclusion ci-dessus s'appuie sur Purfürst.

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

## Question de contrôle

Cette décision tient-elle nos six valeurs, et que sommes-nous prêts à payer pour cela ?
