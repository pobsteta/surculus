# Surculus v1 — spécification

> **Proposée le 15 octobre 2026 par l'orchestrateur, à valider par Pascal** (seconde
> moitié de la porte n° 1). Elle découle de `docs/brief.md` (validé), de
> `docs/adr/0001-application-dediee-plutot-que-qfield.md` (acceptée) et des référentiels
> de `docs/referentiels/`. Elle ne recopie ni le brief ni les référentiels : elle y
> renvoie. Les choix techniques encore ouverts sont signalés **[ADR à venir]** ; les
> propositions de l'orchestrateur, **[proposé]**.
>
> Établie à partir de sommieR 0.31.1 (`main`, `fd92078`), Marculus (`87f5720`), Groma
> (`af551dc`) et nemetonshiny (`9144139`).

## 1. Objet

Surculus est l'application Android de la **tournée travaux** (ADR 0001). Elle suit plant
par plant ce qui a repris sur les placettes fixes, mesure la reprise d'une plantation
par un constat en cheminement, relève les interventions faites et installe les
placettes, puis rend à sommieR un dossier qu'il importe sans changement.

| Application | Geste métier | Ce qu'elle produit | Qui le relit |
|---|---|---|---|
| Marculus | Marteler l'arbre à couper | Feuille de martelage, journal des tiges | nemeton, export CSV |
| Groma | Mesurer la placette d'inventaire par vidéo | Arbres provisoires puis définitifs | nemeton, nemetonshiny |
| **Surculus** | **Suivre ce qui est planté et entretenu** | **Plants, constats de reprise, vides, interventions, placettes** | **sommieR, registre 6** |

## 2. Avec qui elle échange

* **sommieR, destination unique.** Il prépare la tournée
  (`sommier_projet_qfield_travaux()`) et la relit (`sommier_importer_qfield_travaux()`).
  Il tient les **codes travaux** (couche `codes` ; Surculus n'en embarque aucun,
  `CLAUDE.md`) et décide au bureau si une perte est **indépendante de l'entreprise**
  (fascicule 34). Surculus n'écrit jamais dans la base de sommieR.
* **nemetonshiny** (le prévu) : **hors tranche 1** (brief). Ses actions viendront par
  sommieR (brief `vers-nemetonshiny`).
* **nemeton n'est pas sur le chemin.**

## 3. Matériel et conditions

* Smartphone ou tablette Android 8+ (minSdk 26), à une main, gants possibles : boutons
  de volume et dictée pour tout ce qui se répète, vibration et synthèse vocale en retour.
* GNSS : récepteur RTK u-blox ZED-F9P en Bluetooth (SPP), corrections NTRIP Centipède
  quand le réseau passe ; repli sur le GNSS du téléphone, précision affichée et
  enregistrée. **[ADR à venir : GNSS et repères]**
* Pointage d'un plant sans RTK fixe : distance et azimut depuis le piquet. **Matériel de
  pointage à nommer par Pascal** (boussole de visée, télémètre, ruban).
* **Hors ligne de bout en bout** : carte, ortho, dictée, contrôles, alertes.

### Repères et unités (règle commune, `CLAUDE.md`)

| Grandeur | Repère, unité |
|---|---|
| Points du GeoPackage | Lambert-93, **EPSG:2154** (RGF93), ordre (x = E, y = N), mètres |
| Position GNSS brute | référentiel **de la source** : **RGF93** (ETRS89) si RTK corrigé par Centipède, dont les bases diffusent des coordonnées RGF93 ; **WGS 84** (EPSG:4326) si autonome. Ordre (latitude, longitude) en degrés décimaux. Convertie en EPSG:2154 avant écriture ; la source et le référentiel sont enregistrés |
| Azimut saisi | lu à la boussole, donc **magnétique** ; unité de la boussole **déclarée** (degrés ou grades), convertie en degrés |
| Azimut géographique | azimut magnétique **+ déclinaison** au point et à la date, par un modèle géomagnétique embarqué (WMM2025, valable jusqu'à fin 2029, puis son successeur, ou IGRF) ; déclinaison enregistrée |
| Azimut dans le quadrillage | azimut géographique **− convergence des méridiens** au point, calculée par la projection Lambert-93 (environ −5° à Brest, +3° à Strasbourg) ; c'est cet azimut qui place le plant en EPSG:2154 ; convergence enregistrée |
| Numérotation des plants | par l'azimut **géographique** depuis le piquet, sens horaire, [0, 360[ |
| Distance | mètres, **horizontale**. Une distance mesurée dans la pente est ramenée à l'horizontale si la pente est saisie |
| Surface | ares pour les vides, hectares pour les interventions (aire calculée en EPSG:2154) |

Des tests vérifient qu'une placette française tombe en France, qu'une inversion (x, y)
ou (lat, lon) est détectée, et que la chaîne déclinaison puis convergence donne le bon
point à Brest et à Strasbourg.

## 4. Fonctions, dans l'ordre d'une tournée

### 4.1 Ouvrir une tournée

* Importer le dossier de `sommier_projet_qfield_travaux()` (`travaux.gpkg`, `DCIM/`,
  ortho éventuelle), en dossier ou en `.zip`, depuis Téléchargements ou le partage
  Android.
* Surculus travaille sur **sa propre copie**, dans son stockage privé. La compatibilité
  avec QField est une compatibilité **de format** (ADR 0001 : pas de repli plant par
  plant hors Surculus, risque accepté).
* Liste des tournées : forêt, opérateur, date de préparation, nombre de saisies, état
  (ouverte, rendue). Une tournée n'est jamais écrasée.

### 4.2 Se repérer

* Carte : `foret`, `ug`, `parcelles`, ortho hors ligne, position et badge du fix (RTK
  fixe, flottant, autonome). Couches à cocher avec leur nombre d'objets.
* Placettes colorées selon `etat_suivi` ; travaux tracés selon leur code ; vides relevés.
* **Guidage** : distance et azimut jusqu'à une placette, signal sonore qui s'accélère à
  l'approche, puis le cercle de la placette à moins de 30 m. Dans la placette, guidage
  **de plant en plant** dans l'ordre des numéros.

### 4.3 Contrôler une placette fixe — cœur de l'application

* **Arrivée** : la placette est retrouvée par le guidage ; le contrôle porte sa position
  et `placette_uuid`. Sans placette à 15 m, le contrôle est refusé (règle de l'import
  0.31.1).
* **Premier passage** : chaque plant est pointé à sa place, au RTK fixe à la canne, sinon
  en distance et azimut depuis le piquet (§ 3). Il reçoit un UUID et un numéro, donné par
  l'azimut depuis le nord géographique, sens horaire (numérotation de Groma). Beaucoup de
  plantations ONF jalonnent déjà chaque plant (ITTS, `docs/referentiels/protocole-constat-reprise.md`).
* **Passages suivants** : les plants pointés s'affichent sur le plan de la placette ; on
  ne fait que dire leur état. Un plant pointé que l'agent ne retrouve pas (végétation,
  piquet arraché) est **« non observé »** : ce n'est pas un état, il sort des totaux de
  ce contrôle et reste à chercher au passage suivant (arbitrage de Pascal). *Absent* est
  réservé au plant manquant constaté. Un regarni s'ajoute comme nouveau plant, marqué
  regarni et **lié au plant mort ou absent qu'il remplace** : le plant remplacé sort des
  totaux des contrôles suivants.
* **États** (nomenclature du DSF, `docs/referentiels/etats-plants.yaml`) : **sain,
  atteint, mort, absent**. Pour un plant atteint ou mort : **cause** facultative parmi les
  **quatre groupes** du DSF (entomologique, pathologique, animaux, abiotique ou complexe
  ou indéterminé), détail facultatif (abroutissement, frottis, hylobe, gel, sécheresse,
  concurrence…). Un plant **abrouti** est un plant sain ou atteint dont le détail porte
  l'abroutissement ; un plant mort d'abroutissement n'est pas compté dans `nb_abroutis`. Hauteur facultative en cm. L'agent **ne dit jamais** « hors
  entreprise » : c'est sommieR qui le décide au bureau.
* **Saisie sans regarder l'écran** : vibration et synthèse vocale annoncent le numéro du
  plant et l'état enregistré. Boutons de volume **[proposé]** :

  | Geste | En placette | En cheminement |
  |---|---|---|
  | volume haut, appui court | sain, puis plant suivant | sain |
  | volume bas, appui court | mort, puis plant suivant | mort |
  | volume haut, appui long | atteint (la cause se dicte ensuite) | atteint |
  | volume bas, appui long | ouvre la dictée | absent |
  | volume haut et bas ensemble | annule la dernière saisie | annule la dernière saisie |

  Dictée hors ligne à **grammaire fermée** : « sain », « atteint », « mort », « absent »,
  « non observé », « insectes », « champignon », « animaux », « abiotique »,
  « indéterminé », « abrouti », « hauteur quarante-cinq », « suivant », « regarni »,
  « vide », « fin du vide », « annule ».
  **[ADR à venir : stack, dont moteur de dictée]**
* **Totaux déduits des plants** et écrits dans `controles` pour l'import 0.31.1 :
  `nb_total` (sains + atteints + morts + absents, **hors non observés et hors plants
  remplacés**), `nb_vivants` (sains + atteints : un plant atteint compte vivant, arbitrage
  de Pascal), `nb_abroutis` (vivants abroutis), `h_moy_cm`.
* Un contrôle par placette et par jour ; on corrige le premier tant que la tournée n'est
  pas rendue.
* **Pas d'alerte de reprise sur une placette seule** : 10 plants donnent un taux à
  ±25 points (`protocole-constat-reprise.md`). La placette sert le suivi dans le temps et
  la localisation des pertes ; le taux de la plantation vient du § 4.4.

### 4.4 Constat de reprise en cheminement

Le constat sert d'abord **la décision de regarni du gestionnaire** (arbitrage de
Pascal) ; il ne remplace pas la réception contractuelle du chantier. Méthode retenue par
Pascal, sur la critique du `contradicteur` : la plus robuste quand la mortalité se
regroupe (RENFOR, `docs/referentiels/protocole-constat-reprise.md`). Le détail de
l'estimateur sera tranché en ADR **[ADR à venir : protocole de constat]**.

| Type de plantation | Méthode |
|---|---|
| **À densité définitive** (peupliers, noyers, hautes tiges) ou **petit chantier** (le sondage cemagref y dépasserait la moitié des plants) | **Comptage en plein** de tous les plants (fascicule 34, guide 2014) : pas de marge |
| **En lignes** | **Cheminement cemagref** : diagonale, un plant sur deux (séries alternées de 2 et 3 plants), décalage d'une ligne à chaque segment, une ligne sur 2, 3, 5 ou 10 selon la taille du chantier (taux de 50 % sous 400 plants à 5 % au-delà de 2 500). **Deux départs tirés au sort indépendamment**, au pas double, pour une marge sans biais pour le même effort |
| **En plein, sans lignes lisibles** (placeaux, enrichissement) | **Petites grappes** sur une grille systématique à départ aléatoire, sur le modèle du DSF (groupes de 10 plants) ; placeaux entiers là où ils existent ; règle pour les grappes en lisière |

* **Taux** : `taux = Nv × 100 / (Nv + Nm − nm)` (guide 2014), où `nm` (pertes hors
  entreprise) vaut **0 sur le téléphone** : il n'est connu qu'après la décision du
  gestionnaire dans sommieR. Le taux du téléphone est un taux brut, minorant.
* **Marge** : intervalle de confiance à 95 % par l'estimateur par le quotient, variance
  tirée des deux départs indépendants (ou, à défaut, par différences successives), avec
  correction de population finie et quantile de Student. Les morts se regroupent : la
  formule binomiale serait trop optimiste et n'est pas utilisée. Le taux est **toujours
  affiché avec son effectif et sa marge** ; sous un nombre minimal de segments comptés
  (à fixer en ADR), pas de marge affichée.
* **Données nécessaires** : contour de la plantation, nombre de plants, écartement et
  orientation des lignes, essence par ligne si mélange, date de réception,
  cloisonnements. La tournée de sommieR 0.31.1 ne les porte pas : en tranche 2, l'agent
  les saisit (contour tracé, lignes numérotées depuis une lisière) ; à terme, sommieR
  les met dans la tournée (§ 10).
* **Calendrier** : Surculus propose le constat à partir du 1er octobre qui suit d'au
  moins 90 jours la réception (guide 2014) si la date est connue, et le signale sinon,
  sans bloquer.
* **Geste** : le cheminement est guidé à la voix (segment, ligne, plant) ; l'agent dit
  l'état de chaque plant compté aux boutons ou à la voix, sans pointer chaque plant.

### 4.5 Alertes et vides

Règles du brief (arbitrages de Pascal ; sources dans `docs/referentiels/seuils-densites.csv`) :

* **Reprise** : seuil de **80 %**, **90 %** pour les peupliers et hautes tiges
  (fascicule 34), source affichée. Alerte **quand le seuil est au-dessus de
  l'intervalle de confiance** du taux (arbitrage de Pascal) : un comptage en plein, sans
  marge, alerte dès que le taux est sous le seuil ; un sondage alerte dès que sa marge
  le permet. Surculus vise **au moins 300 plants** dans un sondage pour que la marge soit
  petite, et dit « marge trop large, compléter » quand le seuil tombe dans l'intervalle.
* **Vides** : alerte si des plants morts ou absents **groupés couvrent 10 ares ou plus**,
  même si le taux moyen est bon (ITTS de l'ONF). Un sondage en lignes ne localise pas un
  vide de 10 ares (deux lignes comptées peuvent être à 30-40 m) : le vide est donc
  **tracé par l'agent** quand il le voit (« vide » ouvre le contour au GNSS, « fin du
  vide » le ferme), en cheminement comme en placette. Sur les placettes, Surculus peut
  aussi le **proposer** à partir des plants pointés morts ou absents. L'aire est calculée
  en EPSG:2154. Écrit dans la couche `vides`.
* Une alerte propose un besoin (regarni, dégagement, protection) ; elle ne décide rien.
* **Densité de vivants par essence objectif** : sortie dérivée, à l'hectare, comparable
  aux densités minimales de l'aide (`seuils-densites.csv`) ; calculée sur la surface
  plantée, hors cloisonnements si la tournée les porte.

### 4.6 Relever une intervention

1. Choisir le code dans la couche `codes` de la tournée. Le code impose la forme :
   surface, ligne ou point.
2. Tracer : marche du contour au RTK, ou dessin sur la carte ; point pour un regarni
   localisé.
3. La quantité est proposée depuis la géométrie (aire en ha, longueur en m) ; l'unité
   par défaut est la première du code.
4. Renseigner : prévu / reporté / non prévu (et `motif_ecart` hors prévu), exécution,
   intervenant, essence objectif, nombre de plants, date de réception, montant
   **facultatif** s'il est connu sur le terrain (Surculus ne calcule rien, arbitrage de
   Pascal), observations, photos.
5. Enregistrer dans `travaux_surf`, `travaux_lin` ou `travaux_pt`.

### 4.7 Installer une placette

* Point au piquet, exigé en **RTK fixe** (sinon l'application le dit et enregistre la
  précision obtenue).
* Rattachement à un suivi de son UG (`suivis`), code proposé (« P35-04 »), rayon 3,99 m
  (50 m²) par défaut, matérialisation. Ajout dans `placettes`.

### 4.8 Sauvegarder et rendre la tournée

* **Sauvegarde hors de l'appareil** : à chaque fin de placette, de constat ou
  d'intervention, Surculus écrit une sauvegarde du journal dans un fichier local
  partageable sans réseau (stockage partagé ou carte SD), restaurable sur un autre
  appareil. **[ADR à venir : stockage et sauvegarde]**
* Vérification complète (§ 6) ; la liste des fautes renvoie à la saisie.
* Fusion des tournées de plusieurs opérateurs : `<tournée>-<opérateur>-<horodatage>.sursync`,
  **union sur l'UUID**, idempotente, comme les `.marsync` de Marculus.
* Export `<tournée>.zip` : `travaux.gpkg` complété, `DCIM/`. La tournée passe *rendue*
  et reste consultable. **[ADR à venir : format d'échange et fusion]**

## 5. Modèle de données

Le contrat est le `travaux.gpkg` de sommieR 0.31.1, en EPSG:2154. Surculus n'en renomme
rien.

| Couche | Sens | Champs utilisés |
|---|---|---|
| `foret`, `ug`, `parcelles` | aller | `nom` ; `numero` ; `reference`, `designation` |
| `codes` | aller | `code`, `libelle`, `unites`, `formes` |
| `suivis` | aller | `id`, `libelle`, `ug` |
| `placettes` | aller, retour en ajout | `uuid`, `code_placette`, `travaux_id`, `rayon_m`, `materialisation`, `ug`, `etat_suivi`, `dernier_controle`, `dernier_besoin` |
| `travaux_surf`, `travaux_lin`, `travaux_pt` | retour | `uuid`, `code_travaux`, `nature_travaux`, `annee`, `quantite`, `unite`, `nb_plants`, `montant_eur`, `modalite`, `essence_objectif`, `execution`, `intervenant`, `prevu`, `motif_ecart`, `date_reception`, `visite_le`, `operateur`, `precision_m`, `source_gnss`, `observations` |
| `controles` | retour | `uuid`, `placette_uuid`, `nb_total`, `nb_vivants`, `h_moy_cm`, `nb_abroutis`, `concurrence`, `besoin`, `visite_le`, `operateur`, `observations` |
| `photos` + `DCIM/` | retour | `uuid`, `constat_uuid`, `fichier`, `prise_le` |

**Couches ajoutées par Surculus**, écrites dès la tranche 1 et **ignorées par l'import
0.31.1** tant que sommieR ne les lit pas (briefs `vers-sommier` à mettre à jour) :

| Couche | Géométrie | Champs |
|---|---|---|
| `plants` | POINT | `uuid`, `placette_uuid`, `numero`, `essence`, `regarni` (bool), `remplace_uuid`, `pointe_le`, `methode` (rtk, distance_azimut, gnss), `precision_m`, `referentiel_source` (rgf93, wgs84), `distance_m`, `azimut_mag_deg`, `declinaison_deg`, `convergence_deg`, `azimut_geo_deg` |
| `etats_plants` | — | `uuid`, `controle_uuid`, `plant_uuid`, `etat` (sain, atteint, mort, absent, ou `non_observe` : absence de mesure, hors totaux), `cause_groupe` (entomologique, pathologique, animaux, abiotique_complexe_indetermine), `cause_detail`, `hauteur_cm` |
| `constats_reprise` | POLYGON (surface constatée) | `uuid`, `methode` (plein, cemagref, grappes), `pas_lignes`, `departs` (les deux départs tirés), `essence`, `nb_plants_chantier`, `nb_sains`, `nb_atteints`, `nb_morts`, `nb_absents`, `taux_brut_pct`, `ic_bas_pct`, `ic_haut_pct`, `seuil_pct`, `seuil_source`, `alerte` (bool), `visite_le`, `operateur` |
| `vides` | POLYGON | `uuid`, `constat_uuid` ou `placette_uuid`, `surface_are`, `origine` (trace, propose), `visite_le`, `operateur` |

En aller, la tournée 0.31.1 ne porte ni le contour des plantations à constater, ni leur
nombre de plants, leurs lignes, leur date de réception ou leurs cloisonnements
(`travaux_surf` est en retour seulement) : voir § 4.4 et § 10.
| `projet` | — | `nom`, `foret`, `operateur`, `prepare_le`, `version_sommier`, `version_surculus` |

Les totaux de `controles` restent écrits : un import 0.31.1 relit la tournée comme
aujourd'hui.

## 6. Règles vérifiées sur le téléphone

Chaque règle a un cas dans `fixtures/regles/`, rejoué en R dans sommieR et en Kotlin
dans Surculus.

| Règle | Origine | Tranche |
|---|---|---|
| Contrôle rattaché à une placette à 15 m au plus | sommieR 0.31.0 | 1 |
| Un contrôle par placette et par jour | sommieR 0.28.0 | 1 |
| `nb_vivants` ≤ `nb_total`, `nb_abroutis` ≤ `nb_vivants` | sommieR 0.28.0 | 1 |
| Totaux du contrôle = décompte des états des plants (vivants = sains + atteints ; non observés et plants remplacés exclus) | Surculus | 1 |
| Un regarni remplace au plus un plant mort ou absent de la même placette | Surculus | 1 |
| Un plant appartient à une seule placette et garde son UUID | Surculus | 1 |
| États et causes uniquement ceux de `etats-plants.yaml` | Surculus | 1 |
| Tout point écrit tombe dans l'emprise de la forêt et en France ; azimut dans [0, 360[ ; déclinaison et convergence enregistrées | `CLAUDE.md` | 1 |
| Alerte de reprise quand le seuil (80 % ou 90 % selon l'essence) est au-dessus de l'intervalle de confiance | brief | 2 |
| Vide ≥ 10 ares signalé | brief | 2 |
| Le code admet la forme tracée ; l'unité est l'une de celles du code | sommieR 0.28.0 | 3 |
| `motif_ecart` obligatoire si non prévu | sommieR 0.28.0 | 3 |
| Une placette suit des travaux connus de son UG ; code unique dans l'UG | sommieR 0.28.0 | 4 |

## 7. Tranches et critères d'acceptation

| # | Contenu | Critère d'acceptation |
|---|---|---|
| 0 | Socle : squelette, CI, APK de debug, `.claude/test-command` | CI verte sur un squelette ; un test « placette en France » vert |
| 1 | Ouvrir une tournée, carte, guidage, **contrôle de placette fixe** complet (plants pointés, états et causes, non observé, regarnis, boutons de volume, dictée, totaux, refus), sauvegarde du journal, export `.zip`. Comprend l'extraction du transport Bluetooth et du service RTK de l'application Groma (absents de `groma-core`) | (a) la tournée rendue passe `sommier_importer_qfield_travaux()` (sommieR 0.31.1, démo Couchey) sans faute — **CI** ; (b) les règles du § 6 ont chacune un cas de `fixtures/regles/` vert — **CI**, cas écrits par le **`testeur`** ; (c) **signe du brief** : placette déjà pointée contrôlée en **moins de 10 minutes**, médiane sur au moins 10 placettes, mesurée par l'horodatage de l'application depuis l'entrée dans le cercle de 30 m ; le premier passage est chronométré sans cible — **Pascal** ; (d) **essai terrain, automne 2026** : deux opérateurs, mêmes placettes, même jour, **deux tournées séparées, jamais fusionnées** ; plants pointés qui coïncident **à 0,5 m près** et taux égaux entre eux et au comptage de Pascal — **Pascal**. Risque de **retard** : la tranche 0 n'existe pas encore et la notation d'automne finit en novembre |
| 2 | **Constat en cheminement** (plein, cemagref, grappes), alertes de reprise et de vides, densité par essence | sur une plantation réelle, le taux de Surculus **± sa marge** contient le taux d'un **comptage en plein** de la même plantation — **Pascal** ; un vide de 10 ares tracé est signalé — **`testeur`** ; dépend des **codes des essences** (§ 9) |
| 3 | Relevé des interventions (tracé RTK, quantité, prévu / motif) | une plantation tracée s'importe avec sa surface ; un écart sans motif est refusé sur le téléphone |
| 4 | Installation de placette, fusion multi-opérateurs `.sursync` | deux opérateurs sur la même tournée : fusion idempotente, import sans doublon |
| 5 | Plants, constats et vides lus par sommieR (après ses briefs) ; suivi d'un passage à l'autre | printemps 2027 : les plants d'une placette contrôlée deux fois gardent leur UUID et leur numéro |

## 8. Hors périmètre

La programmation des travaux et le chantier (fiche, déclaration, facturation), les
calculs du bilan (dans sommieR), la décision « hors entreprise » (dans sommieR), le
choix de la nomenclature des codes travaux (dans sommieR), le martelage (Marculus),
l'inventaire (Groma), le prévu de nemetonshiny (après la tranche 1). Surculus n'écrit
jamais dans la base de sommieR.

## 9. Besoins en référentiels

| Besoin | État | Qui |
|---|---|---|
| États et causes des plants | acquis : `etats-plants.yaml` | — |
| Protocoles de constat de reprise | acquis : `protocole-constat-reprise.md` | — |
| Seuils et densités minimales | acquis : `seuils-densites.csv` ; **pages du fascicule 34 à revérifier** | Pascal |
| Table de correspondance des codes travaux | proposée : `operations-plantation.csv`, `codes-taches-onf.csv`, `codes-teck-onf.csv` ; **à porter dans sommieR** | Pascal |
| **Codes des essences** (essence d'un plant, essence objectif, peupliers et hautes tiges pour le seuil à 90 %) | **à acquérir** : convention à trouver (codes de l'IGN, du DSF ou de sommieR), comparée à Marculus | `acquereur` |
| Modèle géomagnétique hors ligne (déclinaison) | **à acquérir** : WMM2025 (NOAA et BGS, valable jusqu'à fin 2029 ; licence non déclarée, domaine public par déduction) ou IGRF-14 ; mise à jour à prévoir | `acquereur` |
| Référentiel des positions Centipède (version de RGF93) | **à vérifier** sur la documentation Centipède | `acquereur` |

## 10. Questions ouvertes

1. **Matériel de pointage** des plants sans RTK fixe (Pascal).
2. **Plantation d'essai** pour l'automne 2026 (Pascal) et **successeur** (Pascal).
3. **Coût** : surfaces plantées par an, prix d'un regarni, temps de ressaisie (Pascal).
4. **Plantations à constater** : sommieR met-il dans la tournée leur contour, leur nombre
   de plants, leurs lignes (écartement, orientation, essence), leur date de réception et
   leurs cloisonnements ? À ajouter au brief `vers-sommier`.
5. **Bibliothèques communes** : forme et calendrier de l'extraction (Marculus et Groma
   sont sous GPL-3.0-or-later). **[ADR à venir]**
6. **Identifiant d'application** : `io.github.pobsteta.surculus` ou `fr.nemeton.surculus`.
7. **Briefs vers sommieR** à mettre à jour : nouvelles couches (`etats_plants` avec
   états du DSF et `non_observe`, `constats_reprise`, `vides`), décision « hors
   entreprise », table de correspondance des codes, `montant_eur` facultatif.
8. **Unité des boussoles** (degrés ou grades) et **temps réel de pointage** d'un plant :
   à mesurer au premier essai.
