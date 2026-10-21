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
* **Hors ligne de bout en bout** : carte, ortho, dictée, contrôles, alertes (ortho :
  ADR 0009).
* **Périmètre géographique : France métropolitaine et Corse** (décision de Pascal du
  2026-10-21, ADR 0009). sommieR code EPSG:2154 en dur ; les DOM ne passent pas.

### Repères et unités (règle commune, `CLAUDE.md`)

| Grandeur | Repère, unité |
|---|---|
| Points du GeoPackage | Lambert-93, ordre (x = E, y = N), mètres ; **EPSG:2154** (contrat sommieR 0.31.1) tant que sommieR n'est pas passé à **EPSG:9794** (RGF93 v2b / Lambert-93), cible de toute la tournée (ADR 0003) |
| Position GNSS brute | référentiel **de la source** : **RGF93** (ETRS89) si RTK corrigé par Centipède, dont les bases diffusent des coordonnées RGF93 ; **WGS 84** (EPSG:4326) si autonome. Ordre (latitude, longitude) en degrés décimaux. Convertie en EPSG:2154 avant écriture ; la source et le référentiel sont enregistrés |
| Azimut saisi | lu à la boussole, donc **magnétique** ; unité de la boussole **déclarée** (degrés ou grades), convertie en degrés |
| Azimut géographique | azimut magnétique **+ déclinaison** au point et à la date, par un modèle géomagnétique embarqué (WMM2025, valable jusqu'à fin 2029, puis son successeur, ou IGRF) ; déclinaison enregistrée |
| Azimut dans le quadrillage | azimut géographique **− convergence des méridiens** au point, calculée par la projection Lambert-93 (environ −5° à Brest, +3° à Strasbourg) ; c'est cet azimut qui place le plant en EPSG:2154 ; convergence enregistrée |
| Numérotation des plants | par l'azimut **géographique** depuis le piquet, sens horaire, [0, 360[ |
| Distance | mètres, **horizontale**. Une distance mesurée dans la pente est ramenée à l'horizontale si la pente est saisie |
| Surface | ares pour les vides, hectares pour les interventions (aire calculée en EPSG:2154) |

Des tests vérifient qu'une placette française tombe en France métropolitaine (Corse comprise), qu'une inversion (x, y)
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
  et `placette_uuid`. La position est **moyennée** quelques secondes et comparée à la
  placette la plus proche (règle des 15 m de l'import 0.31.1). Si le GNSS la place hors
  des 15 m mais que l'agent **confirme l'identité** par le code lu sur le piquet, le
  contrôle prend la **position stockée du piquet** et la position mesurée est gardée à
  part, avec le drapeau « rattaché par identité » (ADR 0003).
* **Premier passage** : chaque plant est pointé **relativement au piquet** : distance
  horizontale et azimut magnétique bruts, observation primaire ; ses coordonnées sont
  dérivées (§ 3, ADR 0003). Le RTK sert au piquet, pas aux plants. Il reçoit un UUID et un numéro, donné par
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
  concurrence…) ; sur un plant sain, seul le détail « abroutissement » est permis. Un plant **abrouti** est un plant sain ou atteint dont le détail porte
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
  | volume bas, double appui court | annule la dernière saisie | annule la dernière saisie |

  Les deux touches ensemble sont évitées (raccourci d'accessibilité d'Android). En
  tranche 1, l'écran reste allumé, application au premier plan, en « mode poche » qui
  ignore l'écran tactile (ADR 0004).

  Dictée hors ligne à **grammaire fermée** : « sain », « atteint », « mort », « absent »,
  « non observé », « insectes », « champignon », « animaux », « climat »,
  « indéterminé », « mangé », « hauteur quarante-cinq », « suivant », « remplace »,
  « vide », « fin du vide », « annule ».
  « climat », « mangé » et « remplace » remplacent « abiotique », « abrouti » et « regarni »,
  absents du vocabulaire de `vosk-model-small-fr-0.22` (décision de Pascal du 2026-10-21,
  mission 025). « climat » et « indéterminé » mènent au même groupe DSF ; « mangé » porte
  le détail « abroutissement » sur un plant sain, atteint ou mort. Une phrase porte une
  seule commande ; « hauteur » va de 1 à 400 cm.
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
Pascal) ; il ne remplace pas la réception contractuelle du chantier. Protocole détaillé :
**ADR 0002** (cemagref à deux départs par zone, trois états d'alerte).

| Type de plantation | Méthode |
|---|---|
| **À densité définitive** (peupliers, noyers, hautes tiges) ou **N ≤ 600 plants** | **Comptage en plein** de tous les plants (fascicule 34, guide 2014) : pas de marge |
| **En lignes** | **Cheminement cemagref** (diagonale, un plant sur deux, décalage d'une ligne par segment), taux de sondage **max(grille cemagref, 300/N)**, **deux départs tirés au sort dans chacune de H zones** |
| **En plein, sans lignes lisibles** | **placeaux entiers** comptés comme grappes ; sinon placettes à surface fixe et **densité de vivants à l'hectare** ; à défaut, comptage en plein |

* **Taux** : `taux = Nv × 100 / (Nv + Nm − nm)` (guide 2014), où `nm` (pertes hors
  entreprise) vaut **0 sur le téléphone** : il n'est connu qu'après la décision du
  gestionnaire dans sommieR. Le taux du téléphone est un taux brut, minorant.
* **Marge** : intervalle de confiance à **90 %**, rapport des sommes, variance entre les
  deux départs de chaque zone (environ H degrés de liberté), jamais plus étroite que la
  marge binomiale ; affichée à partir de 6 zones complètes (ADR 0002). Le taux est
  **toujours affiché avec son effectif et sa marge**.
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
  (fascicule 34), source affichée. **Trois états** (ADR 0002) : **« sous le seuil »**
  (borne haute de l'intervalle à 90 % sous le seuil), **« au-dessus »** (borne basse
  au-dessus), **« indéterminé »** sinon, qui propose un **comptage en plein**. Un
  comptage en plein compare directement le taux au seuil.
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

* **Sauvegarde hors de l'appareil** (ADR 0005) : à chaque fin de placette, de constat,
  de vide ou d'intervention, et au moins toutes les N saisies ou M minutes, Surculus
  écrit un `.sursync` dans le stockage partagé et le fait sortir de l'appareil par trois
  voies automatiques : carte SD ou clé USB, téléphone du binôme, envoi quand le réseau
  passe (serveur WebDAV de l'employeur). Les photos sont écrites dès la prise. À
  l'ouverture d'une journée, Surculus signale, sans bloquer, les saisies jamais sorties.
* Vérification complète (§ 6) ; la liste des fautes renvoie à la saisie.
* Fusion des tournées de plusieurs opérateurs : `<code-tournée>-<appareil>-<horodatage>.sursync` (sans nom de propriétaire ni d'agent, ADR 0005),
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
| `plants` | POINT (dérivé) | `uuid`, `placette_uuid`, `numero`, `essence` (code ONF, `essences.csv`), `regarni` (bool), `remplace_uuid`, `pointe_le`, `distance_m` (horizontale), `azimut_mag` et `unite_azimut` (deg, gr) bruts, `declinaison_deg`, `convergence_deg`, `azimut_geo_deg`, `modele_geomagnetique` |
| `positions_controles` | POINT | `controle_uuid`, position GNSS **mesurée** (moyennée), `precision_m`, `source_gnss`, `referentiel_source`, `simulee` (bool), `rattache_par_identite` (bool) |
| `etats_plants` | — | `uuid`, `controle_uuid`, `plant_uuid`, `etat` (sain, atteint, mort, absent, ou `non_observe` : absence de mesure, hors totaux), `cause_groupe` (entomologique, pathologique, animaux, abiotique_complexe_indetermine), `cause_detail`, `hauteur_cm` |
| `constats_reprise` | POLYGON (surface constatée) | `uuid`, `methode` (plein, cemagref, grappes), `pas_lignes`, `departs` (les deux départs tirés), `essence`, `nb_plants_chantier`, `nb_sains`, `nb_atteints`, `nb_morts`, `nb_absents`, `taux_brut_pct`, `ic_bas_pct`, `ic_haut_pct`, `seuil_pct`, `seuil_source`, `alerte` (bool), `visite_le`, `operateur` |
| `vides` | POLYGON | `uuid`, `constat_uuid` ou `placette_uuid`, `surface_are`, `origine` (trace, propose), `visite_le`, `operateur` |
| `projet` | — | `nom`, `foret`, `operateur`, `prepare_le`, `version_sommier`, `version_surculus` |

En aller, la tournée 0.31.1 ne porte ni le contour des plantations à constater, ni leur
nombre de plants, leurs lignes, leur date de réception ou leurs cloisonnements
(`travaux_surf` est en retour seulement) : voir § 4.4 et § 10.

Les totaux de `controles` restent écrits : un import 0.31.1 relit la tournée comme
aujourd'hui.

### Écriture de l'export (mission 006, ADR 0007 et 0011)

Décisions de Pascal du 2026-10-17 (plan de la mission 006, ADR 0011 et relecture de la PR n° 34),
reportées dans `pilotage/briefs/vers-sommier/2026-10-15-travaux-2-plants.md` : les types, `departs`,
`modele_geomagnetique`, `projet`, le zip à plat, la double règle du jour et `visite_le`.
Les choix **techniques du `dev`** (non tranchés par Pascal, à relire) sont signalés « (choix du
`dev`) » : valeurs permises, liste des refus, déterminisme, rejouabilité.

- **Types** des couches ajoutées, par anticipation sur sommieR : `TEXT` (identifiants, codes,
  textes), `INTEGER` (numéros, effectifs, hauteurs), `REAL` (mesures en mètres, degrés,
  pourcents, ares), `BOOLEAN` (0 ou 1), `DATETIME` (ISO 8601 en UTC, millisecondes, « Z », comme
  GDAL). Clé primaire `fid INTEGER PRIMARY KEY AUTOINCREMENT`, géométrie dans `geom`, repère
  du fichier. Tout identifiant est un UUID **en minuscules** (décision de Pascal : l'import de sommieR teste
  `MOTIF_UUID` sur la valeur brute, `R/travaux_qfield.R` l. 350, 382 et 401).
- **`departs`** : un tableau JSON dans un `TEXT` (le contenu des éléments reste à fixer avec le
  protocole du constat de reprise, § 4.4 et ADR 0002).
- **`modele_geomagnetique`** : `WMM2025`, écrit avec chaque plant.
- **`projet`** : une seule ligne, qui porte la **version unique** du fichier. Les champs que
  sommieR doit écrire (`nom`, `foret`, `prepare_le`, `version_sommier`) sont **laissés vides**
  par Surculus tant que la tournée 0.31.1 ne les porte pas, et ne sont jamais écrasés ensuite ;
  Surculus écrit `operateur` et `version_surculus` (la version unique). Un lecteur lit par nom
  de colonne : un fichier plus récent, qui a des champs en plus, se lit en les ignorant.
- **Valeurs permises** (choix du `dev`) (`etat`, `cause_groupe`, `unite_azimut`, `methode`, `origine`) : extension
  Schema de GeoPackage (`gpkg_data_columns`, `gpkg_data_column_constraints`, type `enum`),
  tirées des enums de `:core` et de `docs/referentiels/etats-plants.yaml`, jamais recopiées.
- **Repère** (décision de Pascal, ADR 0003 et 0011) : lu dans `gpkg_spatial_ref_sys` (code EPSG de `organization_coordsys_id`), pour
  `gpkg_contents` comme pour `gpkg_geometry_columns` ; un fichier qui mêle 2154 et 9794 est
  refusé, et un plant dérivé dans l'autre repère que le fichier aussi.
- **Tout ou rien** (choix du `dev`) : un export refusé n'écrit aucun fichier. Les motifs sont des codes
  stables, les mêmes dans le code, la spec et `fixtures/acceptation-export/` (choix du `dev`) : `uuid_invalide`,
  `uuid_en_double`, `melange_reperes`, `hors_france`, `hors_referentiel`, `controle_en_double`, `jour_incoherent`.
  Sont des **décisions de Pascal du 2026-10-17** (journal, PR n° 37) : `uuid_invalide` (UUID refusé tel quel,
  majuscules et espaces compris, sans normalisation), `controle_en_double`, `jour_incoherent` et `hors_referentiel`
  (état, cause ou essence hors référentiel, **et code travaux absent de la couche `codes` de la tournée**). Refus : point hors de France (avec
  signalement d'axes inversés), hors de l'emprise de la forêt, placette inconnue, totaux qui ne
  concordent pas avec les états, UUID en double ou en majuscules, numéro de plant déjà pris,
  essence absente de `essences.csv`, deux contrôles d'une même placette le même jour.
- **Un contrôle par placette et par jour, deux fois** (décision de Pascal) : le jour civil
  Europe/Paris (ADR 0005) **et** le jour UTC, parce que `sommier_importer_qfield_travaux()` lit `visite_le` en UTC et
  refuse la tournée entière si deux contrôles d'une même placette tombent le même jour UTC.
  Entre minuit et 2 h du matin à Paris, les deux jours diffèrent : l'export refuse plutôt que
  de laisser l'import refuser toute la tournée au bureau. La règle UTC tombera quand sommieR
  alignera son import sur le jour civil (demande au brief vers sommieR). Le jour civil est
  **calculé par le journal** (mission 008) et fourni par l'appelant (`ControleSaisi.jour`) :
  l'export le reprend sans le recalculer ; les contrôles déjà présents dans la tournée sont
  confrontés aux deux jours.
- **`visite_le`** (décision de Pascal) : l'instant du journal, soit l'heure GNSS si le fix a au
  plus 5 s, sinon l'heure du téléphone. L'export l'écrit tel quel.
- **Déterministe** (choix du `dev`) : à état égal, deux exports ont le même contenu (lignes rangées par
  identifiant, jamais par ordre de saisie ; horloge injectée dans `gpkg_contents.last_change`).
  Les tests comparent par le contenu, jamais par empreinte du fichier (ADR 0011).
- **Rejouable** (choix du `dev`) : exporter à partir d'un export déjà fait ignore les lignes présentes à
  l'identique ; un même UUID avec un contenu différent est refusé.

### Journal des saisies (sur l'appareil, ADR 0005)

Chaque geste est une ligne ajoutée au journal, jamais modifiée (arbitrage de Pascal du
2026-10-17, plan de la mission 008) :

**Tournée de la ligne** (décision de Pascal, 2026-10-21, mission 031) : chaque ligne porte `code_tournee`, le code de
la tournée à laquelle appartient la saisie (le même code que celui du fichier de la tournée). La valeur `''` désigne
une ligne **non rattachée** : celles qui existaient avant la version 3 de la base. Elles restent au journal, sans
perte, ne sont rattachées à aucune tournée par supposition et n'entrent dans aucune sauvegarde. Une ligne reçue dont
l'`uuid` existe déjà avec une autre tournée est un contenu différent (`uuid_contenu_different`). La projection
ne change pas : sa clé reste (`objet_uuid`, `type`). La sauvegarde et la restauration (§ 4.8) ne portent que sur une
tournée.

| `type` | Objet (`objet_uuid`) | Ce que porte `valeur` |
|---|---|---|
| `controle` | le contrôle | placette, jour civil, opérateur |
| `pointage` | le plant | observation brute (distance horizontale, azimut saisi et unité) et termes dérivés (déclinaison, convergence, position, EPSG) |
| `etat` | la ligne de `etats_plants` | contrôle, plant, état, cause, détail, hauteur |
| `annulation` | l'objet de la ligne annulée | rien ; `remplace_uuid` désigne la ligne annulée |
| `saut_horloge` | l'appareil | écart mesuré, heure avant et après |

- L'état courant d'un (objet, type) suit le lien `remplace_uuid`, pas l'heure : deux têtes
  actives sont **signalées comme conflit**.
- Annuler, c'est défaire : la valeur remplacée redevient courante ; annuler une annulation
  refait.
- L'heure brute n'est jamais réécrite. Un saut d'horloge de plus de 60 s, mesuré sur
  l'horloge monotone dans un même démarrage, est noté. Le jour civil d'un contrôle se
  calcule sur l'heure GNSS quand le fix a au plus 5 s au moment de la saisie (âge mesuré sur
  l'horloge monotone, enregistré avec la ligne), sinon sur l'heure du téléphone.
- Une annulation agit sur la clé (objet, type) de la ligne annulée.
- Un `remplace_uuid` inconnu : la ligne est gardée comme tête, avec une anomalie signalée.
- En conflit, la valeur montrée est la dernière selon l'ordre total, avec le drapeau
  « conflit » et la liste des têtes ; un conflit se résout en annulant l'une des têtes.
- Une ligne reçue deux fois à l'identique est ignorée ; le même `uuid` avec deux contenus
  différents : **les deux versions sont refusées** et l'anomalie est signalée, pour que le
  résultat ne dépende pas de l'ordre d'arrivée.
- Un saut d'horloge est noté au-delà de 60 s **strictement**, dans les deux sens, entre deux
  lignes consécutives (rangées par `sequence`) d'un même appareil et d'un même démarrage ;
  `saut_horloge` note l'heure brute de la ligne précédente et de la ligne courante.
- Un fix GNSS d'âge négatif (postérieur à la saisie) est ignoré : le jour civil se calcule
  alors sur l'heure du téléphone.
- Une saisie dont `remplace_uuid` désigne une ligne d'un **autre objet** ne la remplace pas : la
  ligne désignée garde sa valeur, la saisie devient tête de sa propre clé, et l'anomalie
  `remplace_autre_objet` est rattachée à cette clé (même traitement qu'un remplacement inconnu).
  Une annulation dont l'objet diffère de celui de la ligne qu'elle désigne n'annule rien : la ligne
  désignée garde sa valeur, et l'anomalie `remplace_autre_objet` est rattachée à la clé (objet
  déclaré, `annulation`). Quand l'objet et le type diffèrent tous les deux, seule
  `remplace_autre_objet` est signalée.
- Une saisie de type X dont `remplace_uuid` désigne une ligne de type Y du même objet ne la
  remplace pas : la clé Y garde sa valeur, la saisie est tête de (objet, X), et l'anomalie
  `remplace_autre_type` est rattachée à cette clé.
- Une anomalie de remplacement (`remplace_autre_objet`, `remplace_autre_type`, `remplace_inconnu`)
  disparaît quand la saisie anormale est corrigée par une correction normale : la nouvelle tête
  ne porte plus d'anomalie, et l'erreur reste tracée au journal.
- Annuler une racine, c'est-à-dire la seule saisie d'une clé, ne laisse aucune valeur
  courante (confirmé par Pascal).

(Arbitrages de Pascal du 2026-10-17, mission 019, et décisions de Pascal du 2026-10-17 sur
`remplace_autre_objet`, `remplace_autre_type` et la disparition des anomalies corrigées, relecture de la PR n° 35 ; décisions de format de l'orchestrateur.)

## 6. Règles vérifiées sur le téléphone

Chaque règle a un cas dans `fixtures/regles/`, rejoué en R dans sommieR et en Kotlin
dans Surculus.

| Règle | Origine | Tranche |
|---|---|---|
| Contrôle rattaché à une placette à 15 m au plus, borne incluse (`distances <= 15`, `R/travaux_qfield.R` l. 303) ; code de piquet confirmé mais inconnu de la tournée refusé | sommieR 0.31.0 | 1 |
| Un contrôle par placette et par jour | sommieR 0.28.0 | 1 |
| `nb_vivants` ≤ `nb_total`, `nb_abroutis` ≤ `nb_vivants` | sommieR 0.28.0 | 1 |
| Totaux du contrôle = décompte des états des plants (vivants = sains + atteints ; non observés et plants remplacés exclus) | Surculus | 1 |
| Un regarni remplace exactement un plant mort ou absent de la même placette (`remplace_uuid` obligatoire) ; le plant remplacé compte dans le contrôle où il est vu mort ou absent, puis sort des contrôles suivants | Surculus | 1 |
| Un plant appartient à une seule placette et garde son UUID ; un UUID de plant en double est refusé | Surculus | 1 |
| Numérotation de Groma sans masquage (les plants n'ont pas de diamètre) ; un plant pointé après coup prend le numéro suivant le plus grand déjà donné, sans renuméroter ; pente ≥ 90° refusée ; date hors validité du WMM2025 (après le 31/12/2029) refusée | Surculus | 1 |
| États et causes uniquement ceux de `etats-plants.yaml` ; une cause sur un plant sain ou absent est refusée | Surculus | 1 |
| `h_moy_cm` = moyenne des hauteurs des plants vivants (sains + atteints) mesurés, arrondie à l'entier le plus proche, **demi vers le haut** (42,5 → 43 ; en R, `floor(x + 0.5)`, pas `round()`) (sommieR exige un entier, `R/registre6.R` l. 174) ; absent si aucune hauteur ; `hauteur_cm` entier strictement positif | Surculus | 1 |
| `cause_detail` libre, seule constante reconnue « abroutissement » ; détail refusé sur un plant absent ou non observé, permis seul (sans cause) sur un atteint ou un mort ; sur un plant sain, seul « abroutissement » est permis (arbitrage de Pascal du 2026-10-16, mission 002 ; dictée : décision 4 du 2026-10-21) | Surculus | 1 |
| Un plant « non observé » ne porte aucune mesure (hauteur, cause, détail) ; hauteur refusée sur un plant absent ; un seul état par plant dans un contrôle ; numéro déjà pris dans la placette refusé (comme `sommier_pointer_plant`) | Surculus | 1 |
| Plant pointé au-delà du rayon de sa placette (+ 0,10 m) : alerte et confirmation de l'agent, non refusé | Surculus | 1 |
| Numéros provisoires pendant le premier contrôle, figés à sa clôture ; « un contrôle par jour » = (placette, jour civil Europe/Paris) dans une tournée ; le code lu l'emporte sur le GNSS, « rattaché par identité » seulement au-delà de 15 m | Surculus | 1 |
| Pente acceptée dans ]−90°, 90°[ ; pas de correction du facteur d'échelle Lambert-93 (erreur < 3 cm à 30 m) | Surculus | 1 |
| Unités de la boussole (degrés ou grades) et de la pente (% ou degrés) déclarées à l'ouverture de la tournée ; azimut saisi refusé hors [0, 360[ ou [0, 400[ avant normalisation | ADR 0003 | 1 |
| Tout point écrit tombe dans l'emprise de la forêt et en France ; azimut dans [0, 360[ ; déclinaison et convergence enregistrées | `CLAUDE.md` | 1 |
| L'export refuse (`uuid_invalide`) tout identifiant (contrôle, plant, placette, travail) qui n'est pas un UUID en minuscules, **tel quel** : majuscules ou espaces autour sont refusés, jamais normalisés (l'import de sommieR teste la valeur brute) | décision de Pascal du 2026-10-17 | 1 |
| L'export refuse (`hors_referentiel`) une saisie dont le `code_travaux` est absent de la couche `codes` de la tournée ; il n'embarque aucune liste de codes | décision de Pascal du 2026-10-17 | 1 |
| L'export refuse (`jour_incoherent`) un contrôle dont le jour civil fourni par le journal contredit le jour de Paris de `visite_le` ; il ne corrige pas en silence | décision de Pascal du 2026-10-17 | 1 |
| L'export ne fusionne jamais deux contrôles : deux contrôles d'une même placette qui partagent le jour de Paris **ou** le jour UTC sont refusés (`controle_en_double`) ; la correction d'un contrôle du même jour se fait en amont, dans le journal et le domaine | décision de Pascal du 2026-10-17 | 1 |
| Alerte de reprise quand le seuil (80 % ou 90 % selon l'essence) est au-dessus de l'intervalle de confiance | brief | 2 |
| Vide ≥ 10 ares signalé | brief | 2 |
| Le code admet la forme tracée ; l'unité est l'une de celles du code | sommieR 0.28.0 | 3 |
| `motif_ecart` obligatoire si non prévu | sommieR 0.28.0 | 3 |
| Une placette suit des travaux connus de son UG ; code unique dans l'UG | sommieR 0.28.0 | 4 |

## 7. Tranches et critères d'acceptation

| # | Contenu | Critère d'acceptation |
|---|---|---|
| 0 | Socle : squelette, CI, APK de debug, `.claude/test-command` | CI verte sur un squelette ; un test « placette en France » vert |
| 1 | Ouvrir une tournée, carte, guidage, **contrôle de placette fixe** complet (plants pointés, états et causes, non observé, regarnis, boutons de volume, dictée, totaux, refus), sauvegarde du journal, export `.zip`. Comprend l'extraction du transport Bluetooth et du service RTK de l'application Groma (absents de `groma-core`) | (a) la tournée rendue passe `sommier_importer_qfield_travaux()` (sommieR 0.31.1, démo Couchey) sans faute — **CI** ; (b) les règles du § 6 ont chacune un cas de `fixtures/regles/` vert — **CI**, cas écrits par le **`testeur`** ; (c) **signe du brief** : placette déjà pointée contrôlée en **moins de 10 minutes**, médiane sur au moins 10 placettes, mesurée par l'horodatage de l'application depuis l'entrée dans le cercle de 30 m ; le premier passage est chronométré sans cible — **Pascal** ; (d) **essai terrain, automne 2026** : deux opérateurs, mêmes placettes, même jour, **deux tournées séparées, jamais fusionnées** ; plants pointés qui coïncident **à 0,5 m près** et taux égaux entre eux et au comptage de Pascal — **Pascal**. Risque de **retard** : la tranche 0 n'existe pas encore et la notation d'automne finit en novembre ; Pascal maintient l'automne 2026, avec **repli sur un essai réduit** (une placette, un opérateur) si l'écran de contrôle n'est prêt qu'à la fin de la saison |
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
| Codes des essences | acquis : `essences.csv`, **codes ONF à 3 caractères** (arbitrage de Pascal), correspondance IGN ; seuil à **90 %** pour tous les peupliers et pour toute plantation marquée **« haute tige »** (case de l'agent ou de la tournée), 80 % sinon, noyers compris | — |
| Modèle géomagnétique hors ligne (déclinaison) | acquis : `modele-geomagnetique.md` ; **`WMM.COF` de NCEI** (domaine public selon NCEI), `Declinaison.kt` **copié de Groma** dans `:core` (arbitrage de Pascal du 2026-10-16, mission 005), coefficients identiques à l'archive NCEI, 100 valeurs de test officielles, dates hors validité refusées (arbitrage de Pascal) ; mise à jour avant fin 2029 | — |
| Référentiel des positions Centipède (version de RGF93) | `referentiel-centipede.md` : RGF93, version non écrite (Centipède recommande EPSG:4171, v1) ; **question à poser à Centipède** et contrôle sur une borne géodésique à fiche publiée | Pascal |

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
9. **Emprise de la France** : le test « placette en France » utilise le rectangle du domaine d'usage EPSG:2154 ; un contour officiel (ADMIN-EXPRESS de l'IGN) serait un nouveau référentiel, à décider (Pascal).
