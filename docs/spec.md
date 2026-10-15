# Surculus v1 — spécification

*Rédigée le 15 octobre 2026 à partir du brief validé par Pascal (`docs/brief.md`,
porte n° 1). Elle remplace la spécification du même jour écrite avant le brief
(`docs/specs/2026-10-15-surculus-v1-spec.md`, retirée ; voir l'historique Git et le
journal). Contrat de départ : sommieR 0.31.1 (`main`, `fd92078`). Les propositions
non encore tranchées sont marquées **[proposé]** ; les décisions structurantes passent
en ADR (`docs/adr/`).*

## 1. Objet

Surculus est l'application Android de la **tournée travaux**. Elle relève les
interventions faites, installe et contrôle les placettes de suivi des plantations, et
rend à sommieR un dossier qu'il importe sans changement.

| Application | Geste métier | Ce qu'elle produit | Qui le relit |
|---|---|---|---|
| Marculus | Marteler l'arbre à couper | Feuille de martelage, journal des tiges | nemeton, export CSV |
| Groma | Mesurer la placette d'inventaire par vidéo | Arbres provisoires puis définitifs | nemeton, nemetonshiny |
| **Surculus** | **Suivre ce qui est planté et entretenu** | **Interventions, placettes, plants, contrôles** | **sommieR, registre 6** |

Priorités (brief) : **d'abord le geste** de l'agent de terrain (contrôler vite et sans
faute), **ensuite la preuve** pour le propriétaire (densité d'essences-objectif, phase B).

## 2. Avec qui elle échange

- **sommieR, destination unique.** Il prépare la tournée
  (`sommier_projet_qfield_travaux()`) et la relit (`sommier_importer_qfield_travaux()`).
- **nemetonshiny, source du prévu.** Son plan d'action (`export_action_plan_gpkg()`),
  versé dans la tournée par sommieR, devient des propositions sur le terrain. L'action
  passe à *réalisée* d'après ce que sommieR a inscrit, jamais d'après le téléphone.
- **nemeton n'est pas sur le chemin.** Il lit les indicateurs qui sortent de sommieR.
- **Bibliothèques communes** avec Marculus et Groma (GNSS/NTRIP, géodésie, GeoPackage,
  carte, dictée, journal), reprises sans copie (brief `vers-marculus`).

## 3. Matériel et conditions

- Smartphone ou tablette Android 8+ (minSdk 26), utilisé à une main, gants possibles :
  boutons de volume et dictée pour tout ce qui se répète.
- Récepteur GNSS RTK u-blox ZED-F9P en Bluetooth, corrections NTRIP Centipède **quand le
  réseau passe** ; repli sur le GNSS du téléphone. La précision est toujours affichée et
  enregistrée.
- **Hors ligne, sauf les corrections RTK** : carte, ortho, modèle de dictée et contrôles
  sont présents sur le téléphone **avant le départ** ; rien d'autre ne demande de réseau.

## 4. Parcours sur le terrain, dans l'ordre d'une tournée

### 4.1 Préparer et ouvrir une tournée (au bureau, avec réseau)

- Importer le dossier écrit par `sommier_projet_qfield_travaux()` (`travaux.gpkg`,
  `DCIM/`, ortho éventuelle), en dossier ou en `.zip`.
- **Vérifier la version du contrat** (`projet.version_contrat`, §5) : une version
  inconnue est refusée avec un message clair, avant le départ en forêt.
- Vérifier que la tournée est **prête pour le hors-ligne** : tuiles et ortho de l'emprise,
  modèle de dictée français ; sinon, l'application le dit et propose de les charger.
- Surculus travaille sur **sa propre copie**, dans son stockage privé. La compatibilité
  avec QField est une compatibilité de format, pas un fichier partagé.
- Liste des tournées : forêt, opérateur, date de préparation, nombre de saisies, état
  (ouverte, rendue). Une tournée n'est jamais écrasée.

### 4.2 Se repérer

- Carte : `foret`, `ug`, `parcelles`, ortho hors ligne, position GNSS et badge du fix
  (RTK fixe, RTK flottant, GNSS seul) avec la précision en mètres.
- Placettes colorées selon `etat_suivi` ; travaux de la tournée tracés selon leur code.
- **« Y aller »** : distance et azimut jusqu'au piquet d'une placette ; à moins de 30 m,
  le piquet et le dispositif (cercle C1 ou segment de ligne C2) s'affichent à l'échelle.

### 4.3 Relever une intervention

1. Choisir le code dans `codes` : les 15 codes de sommieR
   (`docs/referentiels/codes-travaux-sommier.csv`), **nomenclature interne** rattachée
   aux libellés du RNTSF §1.1 (`docs/referentiels/correspondance-sommier.md`). Le code
   impose la forme (surface, ligne, point) et les unités.
2. Tracer : marche du contour au GNSS (sommet tous les *n* mètres ou au toucher), ou
   dessin sur la carte ; point pour un regarni localisé.
3. La quantité est proposée depuis la géométrie (aire en ha, longueur en m, calculées en
   Lambert-93) ; l'unité par défaut est la première du code.
4. Renseigner : prévu / reporté / non prévu (et `motif_ecart` hors prévu), exécution,
   intervenant, nombre de plants, date de réception, observations, photos. Si la tournée
   porte un programme, l'action prévue la plus proche est proposée.
5. Enregistrer dans `travaux_surf`, `travaux_lin` ou `travaux_pt`.

### 4.4 Installer une placette

- **Poser le piquet et le positionner** avec la meilleure position disponible : RTK
  fixe si possible, sinon RTK flottant ou GNSS seul. La source et la précision sont
  enregistrées ; l'installation n'est **jamais bloquée** par l'absence de RTK. La
  position pourra être améliorée au passage suivant (nouvelle mesure, la meilleure
  précision l'emporte, l'ancienne reste au journal).
- La position brute (latitude, longitude ETRS89, précision) est conservée ; la position
  stockée est convertie en Lambert-93 sur le téléphone.
- **Dispositif**, proposé par la tournée et constant pour la placette :
  - **C1 — k plants les plus proches du piquet**, quand aucune ligne n'est repérable
    (régénération naturelle, plantation sans lignes, enrichissement par placeaux) ;
  - **C2 — portion de ligne de longueur L** à partir du piquet, dans un sens donné,
    quand des lignes sont repérables (y compris une plantation en plein en lignes) ;
    l'écartement entre lignes *e* est connu (tournée) ou mesuré.
  - Valeurs par défaut **[proposé]** : k = 10 ; L = 20 m. À ajuster après le premier
    passage à Marsannay.
- Rattachement à un suivi de son UG (`suivis`), code proposé (« P35-04 »),
  matérialisation. Ajout dans `placettes`.

### 4.5 Contrôler une placette — cœur de l'application

- **Arrivée** : le piquet est retrouvé par « Y aller ». Le contrôle porte
  `placette_uuid` ; sans placette à 15 m, il est refusé (règle de l'import 0.31.1).
- **Premier passage — pointer les plants par rapport au piquet**, sans exigence de
  précision GNSS au plant :
  - C1 : distance (m, horizontale) et azimut (grades ou degrés, **nord géographique**,
    sens horaire **[proposé]**) depuis le piquet ; on pointe les k plus proches ;
  - C2 : rang le long de la ligne depuis le piquet (1, 2, 3…), et distance au piquet
    facultative ; on pointe tous les plants de la portion de longueur L.
  - Chaque plant reçoit un UUID stable et un numéro dans la placette (C1 : par azimut
    depuis le nord, sens horaire, comme Groma ; C2 : par rang).
- **Passages suivants** : les plants déjà pointés s'affichent sur le plan de la
  placette ; on ne fait que changer leur état et leurs dégâts. Un regarni s'ajoute comme
  nouveau plant.
- **États** (un seul par plant et par contrôle) :
  - `vivant` ;
  - `mort` — définitif ;
  - `non_retrouve` — **réversible** : au passage suivant, le plant peut redevenir
    `vivant` ou `mort`.
- **Dégâts**, un seul vocabulaire (`abroutissement`, `frottis`, `rongeurs`, `sanglier`,
  `hylobe_insectes`, `secheresse`, `gel`, `travaux`, `concurrence`, `inconnue`) :
  - sur un plant `vivant` : zéro, un ou plusieurs dégâts observés (`inconnue` interdit) ;
  - sur un plant `mort` : une cause probable, une seule, `inconnue` permis ;
  - sur un plant `non_retrouve` : aucun.
- Hauteur facultative en cm.
- **Saisie sans regarder l'écran** : boutons de volume (plant suivant / vivant / mort),
  vibration et synthèse vocale ; dictée à grammaire fermée, locale : « vivant », « mort »,
  « pas trouvé », « abrouti », « frotté », « hylobe »…, « hauteur quarante-cinq »,
  « suivant », « annule ». **L'audio n'est jamais enregistré** : seul le texte reconnu
  est gardé.
- **Indicateurs calculés sur place, à titre indicatif** (seul sommieR fait foi) :
  - **taux de reprise** = vivants / (vivants + morts) **[proposé : les `non_retrouve`
    sont exclus du calcul et comptés à part]** ;
  - **densité de plants vivants toutes essences confondues**, en plants/ha :
    - C2 : vivants × 10 000 / (L × e) ;
    - C1 : estimateur par distance au k-ième plant, **[proposé]** (k − 1) × 10 000 /
      (π × r_k²), avec r_k la distance horizontale du k-ième plant pointé ; le biais de
      cet estimateur sur les plantations régulières ou agrégées est à documenter (ADR
      et référentiel, §9).
  - Sous le **seuil de reprise fourni par la tournée** (80 % par défaut, seuil de
    regarni des fiches CNPF pour plusieurs résineux), l'application le signale et
    propose un besoin.
- Concurrence, besoin, observations, photos. **On ne photographie que le plant, jamais
  une personne** (rappel à l'écran de prise de vue). Les métadonnées EXIF sont
  conservées.
- Un contrôle par placette et par jour : un second est refusé ; on corrige le premier
  tant que la tournée n'est pas rendue.

### 4.6 Rendre la tournée

- Vérification complète (§6) ; la liste des fautes renvoie à la saisie.
- **Copie de sauvegarde hors appareil pendant la tournée [proposé]** : export
  incrémental du journal vers la carte SD ou un support externe à chaque fin de
  placette, pour qu'un appareil perdu n'emporte pas la journée (forme à fixer par ADR).
- Fusion des tournées de plusieurs opérateurs : `.sursync` fusionnés par **union sur
  l'UUID**, idempotente, comme les `.marsync` de Marculus.
- Export `<tournée>.zip` : `travaux.gpkg` complété, `DCIM/`, et la **notice du
  contrat** (couches et champs) pour qu'il reste lisible dans QGIS sans sommieR.

## 5. Données manipulées

Le contrat est le `travaux.gpkg` de sommieR 0.31.1, en **Lambert-93 (EPSG:2154), X vers
l'est, Y vers le nord, en mètres**. Surculus n'en renomme rien. Les distances sont
horizontales, en mètres ; les surfaces en hectares ; l'horodatage est à la minute, en
heure locale avec son décalage (ISO 8601).

| Couche | Sens | Champs utilisés |
|---|---|---|
| `foret`, `ug`, `parcelles` | aller | `nom` ; `numero` ; `reference`, `designation` |
| `codes` | aller | `code`, `libelle`, `unites`, `formes` |
| `suivis` | aller | `id`, `libelle`, `ug` |
| `placettes` | aller, retour en ajout | `uuid`, `code_placette`, `travaux_id`, `rayon_m`, `materialisation`, `ug`, `etat_suivi`, `dernier_controle`, `dernier_besoin` |
| `travaux_surf`, `travaux_lin`, `travaux_pt` | retour | champs de 0.31.1, dont `operateur`, `visite_le`, `precision_m`, `source_gnss` |
| `controles` | retour | `uuid`, `placette_uuid`, `nb_total`, `nb_vivants`, `h_moy_cm`, `nb_abroutis`, `concurrence`, `besoin`, `visite_le`, `operateur`, `observations` |
| `photos` + `DCIM/` | retour | `uuid`, `constat_uuid`, `fichier`, `prise_le` |

**Compatibilité 0.31.1** : les totaux de `controles` restent écrits (`nb_abroutis` =
vivants portant le dégât `abroutissement`) ; `rayon_m` est rempli pour C1 avec r_k du
premier passage, et laissé vide pour C2. Un import 0.31.1 relit donc la tournée comme
aujourd'hui.

**Ajouts demandés à sommieR** (brief `vers-sommier/travaux-2`, à réviser selon cette
spec), écrits par Surculus et ignorés par l'import actuel tant qu'ils ne sont pas livrés :

| Couche | Géométrie | Champs |
|---|---|---|
| `projet` | — | `nom`, `foret`, `operateur`, `prepare_le`, `version_sommier`, **`version_contrat`**, **`seuil_reprise_pct`** |
| `placettes` (champs ajoutés) | — | **`dispositif`** (`C1`, `C2`), **`k`**, **`longueur_m`**, **`ecartement_m`**, **`azimut_ligne`**, **`source_gnss`**, **`precision_m`**, **`lat_etrs89`**, **`lon_etrs89`** |
| `plants` | POINT (EPSG:2154) | `uuid`, `placette_uuid`, `numero`, `distance_m`, `azimut`, `rang`, `pointe_le`, `methode` (`distance_azimut`, `rang_ligne`, `gnss`), `essence` (facultatif, phase B) |
| `etats_plants` | — | `uuid`, `controle_uuid`, `plant_uuid`, `etat` (`vivant`, `mort`, `non_retrouve`), `hauteur_cm` |
| `degats_plants` | — | `uuid`, `etat_plant_uuid`, `degat` (vocabulaire du §4.5) |

**Opérateur** : identifiant court (initiales ou code), jamais le nom complet. **Aucune
trace GNSS continue** n'est conservée : seules les positions des piquets et des
interventions.

## 6. Règles vérifiées sur le téléphone

Les mêmes qu'à l'import (`controler_tournee_travaux()`, `registre6_*()`), plus celles
des plants. Chaque règle a un cas dans `fixtures/regles/`.

| Règle | Origine |
|---|---|
| Le code admet la forme tracée (surface, ligne, point) | sommieR 0.28.0 |
| L'unité est l'une de celles du code | sommieR 0.28.0 |
| `motif_ecart` obligatoire hors prévu | sommieR 0.28.0 |
| Une placette suit des travaux connus de son UG | sommieR 0.28.0 |
| Code de placette unique dans l'UG | sommieR 0.28.0 |
| `nb_vivants` ≤ `nb_total`, `nb_abroutis` ≤ `nb_vivants` | sommieR 0.28.0 |
| Un contrôle par placette et par jour | sommieR 0.28.0 |
| Contrôle rattaché à une placette à 15 m au plus | sommieR 0.31.0 |
| Version du contrat connue à l'ouverture | brief (2026-10-15) |
| Totaux du contrôle = décompte des états des plants | brief |
| Un plant appartient à une seule placette et garde son UUID | brief |
| Un plant `mort` ne redevient pas `vivant` ; un `non_retrouve` peut redevenir `vivant` ou `mort` | brief |
| Dégâts : plusieurs sur un vivant, un seul sur un mort, aucun sur un non retrouvé ; `inconnue` seulement sur un mort | brief |
| Le dispositif d'une placette ne change pas d'un passage à l'autre | brief |
| C1 : au plus k plants au premier passage ; C2 : rangs consécutifs depuis 1 | brief |
| Coordonnées en EPSG:2154 ; une placette française tombe en France (emprise métropole) | `CLAUDE.md` |

## 7. Critères d'acceptation

1. **Signe concret (brief)** : sur le terrain de la forêt communale de
   Marsannay-la-Côte, un agent contrôle une placette **en moins de 10 minutes, recherche
   du piquet comprise**, sans papier ; mesure faite sur au moins trois placettes.
2. **Import sans faute** : la tournée rendue passe `sommier_importer_qfield_travaux()`
   (sommieR 0.31.1, puis la version livrant `travaux-2`) sans une faute.
3. **Fidélité** : au moins trois placettes contrôlées deux fois ; Pascal retrouve sur
   place les plants pointés à leur numéro, et le taux de reprise de Surculus égale son
   propre comptage.
4. **Hors ligne** : une tournée complète se fait en mode avion, RTK excepté ; une
   installation sans RTK fixe aboutit avec sa précision enregistrée.
5. **États réversibles** : un plant `non_retrouve` au passage n redevient `vivant` au
   passage n+1 sans perdre son UUID ni son numéro.
6. **Données personnelles** : aucun fichier audio dans le stockage de l'application ni
   dans l'export ; aucun nom complet d'opérateur ; aucune trace continue.
7. **Lisibilité** : l'export s'ouvre dans QGIS, couches et champs conformes à la notice.
8. **Coordonnées** : un test automatisé vérifie qu'un piquet saisi en ETRS89 près de
   Marsannay-la-Côte tombe en Côte-d'Or en EPSG:2154.

Mesure de base à prendre au premier passage : le temps actuel de contrôle et de
ressaisie (papier ou QField), pour chiffrer le coût complet (brief).

## 8. Hors périmètre

La programmation des travaux et le chantier (fiche, déclaration, facturation) ; les
calculs du bilan (dans sommieR) ; le martelage (Marculus) ; l'inventaire (Groma) ;
l'écriture dans une base de données ; **en v1**, l'essence de chaque plant et la densité
d'essences-objectif (phase B) ; la comparaison chronométrée avec QField (risque accepté,
brief).

## 9. Besoins en référentiels (à confier à l'`acquereur`)

| Besoin | Pour | État |
|---|---|---|
| Arrêté MFR Bourgogne-Franche-Comté 2026 (SREAF-2026-09) : densités à la réception et à 5 ans, essences-objectif | phase B, seuils | référence repérée, texte non lu (`mfr-bfc.md`) |
| Estimateur de densité par distance au k-ième plant et son biais (Prodan ; Kleinn et Vilčko 2006) | §4.5, C1 | à acquérir |
| Protocole de suivi des plantations du DSF : définition de la reprise, traitement des plants non retrouvés, causes notées | §4.5, taux et dégâts | extraits seulement |
| Bilan DSF de reprise BFC (CRFB du 24/11/2025) | coût complet, seuil | référence repérée, non lue |
| Arrêté du 8/10/2026 (NOR TECT2624357A) : barème des aides | coût complet | à vérifier |
| Liste des essences et codes (IFN `espar` + TAXREF ou liste MFR) | phase B | à acquérir |

Les sites du ministère, des DRAAF et du CNPF sont injoignables depuis la session : Pascal
dépose les PDF dans `docs/referentiels/` sur son poste (non versionnés).

## 10. Décisions structurantes (ADR à proposer)

| ADR | Décision |
|---|---|
| 0001 | Stack de l'application (langage, interface, modules) et commande de tests |
| 0002 | Stockage sur le téléphone : journal en ajout seul, GeoPackage écrit à l'export |
| 0003 | Hors-ligne : ce qui est préchargé, sauvegarde hors appareil pendant la tournée |
| 0004 | GNSS : piquet seul positionné, sources et précision, repli sans RTK |
| 0005 | Export et contrat avec sommieR : version, couches ajoutées, notice QGIS |
| 0006 | Licence et bibliothèques communes : GPL-3.0-or-later, forme du partage avec Marculus et Groma |

## 11. Tranches

| # | Contenu | Critère d'acceptation |
|---|---|---|
| 0 | Socle : squelette, CI, commande de tests | CI verte sur un squelette |
| 1 | Ouvrir une tournée (version du contrat), carte, **contrôle de placette** C1 et C2 (guidage, plants pointés, états, dégâts, dictée, indicateurs, refus immédiats, photos), export `.zip` | critères 2, 5, 6, 8 sur la démo Couchey ; essai sur appareil avec le ZED-F9P |
| 2 | Installer une placette (piquet sans RTK obligatoire), sauvegarde hors appareil | critère 4 |
| 3 | Relever les interventions (tracé, quantité, prévu/motif), programme proposé | une plantation tracée s'importe avec sa surface ; un écart sans motif est refusé |
| 4 | Fusion multi-opérateurs `.sursync`, notice QGIS | fusion idempotente, import sans doublon ; critère 7 |
| 5 | Terrain à Marsannay-la-Côte | critères 1 et 3 |
| B | Essence par plant, densité d'essences-objectif, seuils MFR | à spécifier après la v1 |

## 12. Questions ouvertes

1. **Taux de reprise** : les plants `non_retrouve` sont-ils exclus du calcul (proposé),
   comptés comme morts, ou comme vivants ?
2. **Valeurs par défaut** : k = 10 (C1) et L = 20 m (C2) ?
3. **Azimut** : degrés ou grades, nord géographique ou magnétique (avec déclinaison) ?
4. **Parcelles d'essai, premier utilisateur, échéance** à Marsannay-la-Côte (brief).
5. **Bibliothèques communes** : forme du partage avec Marculus et Groma (ADR 0006).
6. **Rattacher le prévu de nemetonshiny aux UG de sommieR** : rattachement géométrique
   proposé par le brief `vers-nemetonshiny`, à valider.
7. **Identifiant d'application** : `io.github.pobsteta.surculus` ou `fr.nemeton.surculus`.
