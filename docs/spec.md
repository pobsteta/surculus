# Surculus v1 — spécification

> **Brouillon, à revoir après le brief amendé** (`docs/brief.md`, porte n° 1 du
> 15 octobre 2026). Écrite avant la critique du brief : protocole en cheminement, états
> du DSF, sauvegarde du journal, repères de coordonnées et périmètre de la tranche 1
> restent à y reporter. Les choix techniques notés ici sont des propositions, à trancher
> en ADR (`docs/adr/`).

*Établie le 15 octobre 2026, à partir de sommieR 0.31.1 (`main`, `fd92078`),
de Marculus (`87f5720`), de Groma (`af551dc`) et de nemetonshiny
(`9144139`). Décisions de Pascal du même jour : application séparée qui
reprend les bibliothèques communes, plants comptés et localisés, licence
GPL-3.0-or-later.*

## 1. Objet

Surculus est l'application Android de la **tournée travaux**. Elle relève
les interventions faites, installe et contrôle les placettes de suivi des
plantations, et rend à sommieR un dossier qu'il importe sans changement.

| Application | Geste métier | Ce qu'elle produit | Qui le relit |
|---|---|---|---|
| Marculus | Marteler l'arbre à couper | Feuille de martelage, journal des tiges | nemeton, export CSV |
| Groma | Mesurer la placette d'inventaire par vidéo | Arbres provisoires puis définitifs | nemeton, nemetonshiny |
| **Surculus** | **Suivre ce qui est planté et entretenu** | **Interventions, placettes, plants, contrôles** | **sommieR, registre 6** |

## 2. Avec qui elle échange

* **sommieR, destination unique.** Il prépare la tournée
  (`sommier_projet_qfield_travaux()`) et la relit
  (`sommier_importer_qfield_travaux()`). Une saisie de travaux est un fait
  qui doit rester prouvable (UUID, NDP 0, chaîne, photos sous empreinte) :
  c'est le rôle du registre. Un seul chemin d'écriture, une seule vérité.
* **nemetonshiny, source du prévu.** Son plan d'action par UGF
  (`data/action_plan.json`, export `export_action_plan_gpkg()`) porte déjà
  les actions `plantation`, `regeneration`, `protection`, `cloisonnement`,
  `entretien`. Versées dans la tournée par sommieR, elles deviennent des
  propositions sur le terrain (brief `vers-nemetonshiny`). L'action passe à
  *réalisée* d'après ce que sommieR a inscrit, jamais d'après le téléphone.
* **nemeton n'est pas sur le chemin.** Il lit les indicateurs de travaux qui
  sortent de sommieR.

## 3. Matériel et conditions

* Smartphone ou tablette Android 8+ (minSdk 26), utilisé à une main, gants
  possibles : boutons de volume et dictée pour tout ce qui se répète.
* Récepteur GNSS RTK u-blox ZED-F9P en Bluetooth, corrections NTRIP
  Centipède quand le réseau passe ; repli sur le GNSS du téléphone, avec sa
  précision affichée et enregistrée.
* **Hors ligne de bout en bout** : carte, ortho, dictée et contrôles ne
  demandent aucun réseau.

## 4. Fonctions, dans l'ordre d'une tournée

### 4.1 Ouvrir une tournée

* Importer le dossier écrit par `sommier_projet_qfield_travaux()`
  (`travaux.gpkg`, `DCIM/`, ortho éventuelle), en dossier ou en `.zip`,
  depuis Téléchargements ou le partage Android.
* Surculus travaille sur **sa propre copie**, dans son stockage privé. Sous
  Android 11+, une application ne peut pas écrire dans la copie privée de
  QField (Groma, point ouvert 1) : la compatibilité avec QField est une
  compatibilité **de format**, pas un fichier partagé en direct.
* Liste des tournées, façon contextes de Marculus : forêt, opérateur, date
  de préparation, nombre de saisies, état (ouverte, rendue). Une tournée
  n'est jamais écrasée ; une nouvelle préparation de la même forêt est une
  nouvelle tournée.

### 4.2 Se repérer

* Carte osmdroid : `foret`, `ug` (numéro), `parcelles`, ortho hors ligne,
  position GNSS et badge du fix (RTK fixe, flottant, autonome).
* Placettes colorées selon `etat_suivi` (besoin signalé, sans besoin,
  jamais contrôlée) ; travaux de la tournée tracés selon leur code.
* Couches à cocher avec leur nombre d'objets, comme Marculus.
* « Y aller » : distance et azimut jusqu'à une placette, puis le cercle de
  la placette dès qu'on est à moins de 30 m.

### 4.3 Relever une intervention

1. Choisir le code dans `codes` (15 codes de `SOMMIER_CODES_TRAVAUX`). Le
   code impose la forme : surface, ligne ou point.
2. Tracer : marche du contour au RTK (sommet tous les *n* mètres ou au
   toucher), ou dessin sur la carte ; point pour un regarni localisé.
3. La quantité est proposée depuis la géométrie (aire géodésique en ha,
   longueur en m) ; l'unité par défaut est la première du code.
4. Renseigner : prévu / reporté / non prévu (et `motif_ecart` hors prévu),
   exécution, intervenant, essence objectif, nombre de plants, date de
   réception, observations, photos. Si la tournée porte un programme (brief
   nemetonshiny), l'action prévue la plus proche est proposée et préremplit
   le formulaire.
5. Enregistrer dans `travaux_surf`, `travaux_lin` ou `travaux_pt`.

### 4.4 Installer une placette

* Point au piquet, exigé en **RTK fixe** (sinon l'application le dit et
  enregistre la précision obtenue).
* Rattachement à un suivi de son UG (`suivis`), code proposé
  (« P35-04 »), rayon 3,99 m (50 m²) par défaut, matérialisation.
* Ajout dans `placettes` (seule couche d'aller où Surculus écrit).

### 4.5 Contrôler une placette — cœur de l'application

* **Arrivée** : la placette est retrouvée par « Y aller » ; le contrôle
  porte sa position et `placette_uuid`. Sans placette à 15 m, le contrôle
  est refusé (règle de l'import 0.31.1).
* **Plants pointés et comptés.**
  * Premier passage : chaque plant est pointé à sa place. Au RTK fixe, le
    point est pris à la canne ; sinon, il se saisit en **distance et azimut
    depuis le piquet** (plus précis qu'un GNSS autonome à 1 m près). Chaque
    plant reçoit un UUID et un numéro dans la placette, donné par l'azimut
    depuis le nord, dans le sens horaire, comme la numérotation de Groma.
  * Passages suivants : les plants déjà pointés s'affichent sur le plan de
    la placette ; on ne fait que changer leur état. Un plant non retrouvé
    passe *disparu* ; un regarni s'ajoute comme nouveau plant.
  * États : vivant, mort, disparu ; *abrouti* est une marque sur un plant
    vivant ; hauteur facultative en cm.
* **Saisie sans regarder l'écran** : boutons de volume (plant suivant /
  vivant / mort), vibration et synthèse vocale ; dictée Vosk à grammaire
  fermée : « vivant », « mort », « abrouti », « hauteur quarante-cinq »,
  « suivant », « concurrence forte », « besoin dégagement », « annule ».
* **Totaux déduits des plants** : `nb_total`, `nb_vivants`, `nb_abroutis`,
  `h_moy_cm` (moyenne des hauteurs dites), puis taux de reprise et densité
  à l'hectare (`nb_vivants × 10 000 / πr²`). Sous 80 %
  (`SOMMIER_SEUIL_REPRISE_PCT`), l'application le signale et propose un
  besoin.
* Concurrence, besoin, observations, photos ; enregistrement dans
  `controles`, et les plants dans la couche `plants` (§ 5).
* Un contrôle par placette et par jour : un second est refusé ; on corrige
  le premier tant que la tournée n'est pas rendue.

### 4.6 Rendre la tournée

* Vérification complète (§ 6) ; la liste des fautes renvoie à la saisie.
* Fusion des tournées de plusieurs opérateurs : chacun partage un
  `<tournée>-<opérateur>-<horodatage>.sursync`, fusionné par **union sur
  l'UUID**, idempotente, comme les `.marsync` de Marculus.
* Export `<tournée>.zip` : `travaux.gpkg` complété, `DCIM/`, par le partage
  Android ou vers Téléchargements. La tournée passe *rendue* et reste
  consultable.

## 5. Modèle de données

Le contrat est le `travaux.gpkg` de sommieR 0.31.1, en Lambert-93
(EPSG:2154). Surculus n'en renomme rien.

| Couche | Sens | Champs utilisés |
|---|---|---|
| `foret`, `ug`, `parcelles` | aller | `nom` ; `numero` ; `reference`, `designation` |
| `codes` | aller | `code`, `libelle`, `unites`, `formes` |
| `suivis` | aller | `id`, `libelle`, `ug` |
| `placettes` | aller, retour en ajout | `uuid`, `code_placette`, `travaux_id`, `rayon_m`, `materialisation`, `ug`, `etat_suivi`, `dernier_controle`, `dernier_besoin` |
| `travaux_surf`, `travaux_lin`, `travaux_pt` | retour | `uuid`, `code_travaux`, `nature_travaux`, `annee`, `quantite`, `unite`, `nb_plants`, `montant_eur`, `modalite`, `essence_objectif`, `execution`, `intervenant`, `prevu`, `motif_ecart`, `date_reception`, `visite_le`, `operateur`, `precision_m`, `source_gnss`, `observations` |
| `controles` | retour | `uuid`, `placette_uuid`, `nb_total`, `nb_vivants`, `h_moy_cm`, `nb_abroutis`, `concurrence`, `besoin`, `visite_le`, `operateur`, `observations` |
| `photos` + `DCIM/` | retour | `uuid`, `constat_uuid`, `fichier`, `prise_le` |

**Ajouts demandés à sommieR** (brief `vers-sommier`), écrits par Surculus
dès la tranche 1 et ignorés par l'import actuel tant que le brief n'est pas
livré :

| Couche | Géométrie | Champs |
|---|---|---|
| `plants` | POINT | `uuid`, `placette_uuid`, `numero`, `essence`, `pointe_le`, `methode` (rtk, distance_azimut, gnss), `precision_m` |
| `etats_plants` | — | `uuid`, `controle_uuid`, `plant_uuid`, `etat` (vivant, mort, disparu), `abrouti`, `hauteur_cm` |
| `projet` | — | `nom`, `foret`, `operateur`, `prepare_le`, `version_sommier` |

Les totaux de `controles` restent écrits : un import 0.31.1 relit donc
la tournée comme aujourd'hui, et les plants attendent le brief.

## 6. Règles vérifiées sur le téléphone

Les mêmes qu'à l'import (`controler_tournee_travaux()`,
`registre6_*()`), plus celles des plants. Chaque règle a un cas dans
`fixtures/regles/`.

| Règle | Origine | Tranche |
|---|---|---|
| Le code admet la forme tracée (surface, ligne, point) | sommieR 0.28.0 | 2 |
| L'unité est l'une de celles du code | sommieR 0.28.0 | 2 |
| `motif_ecart` obligatoire si `prevu` ≠ `prevu` | sommieR 0.28.0 | 2 |
| Une placette suit des travaux connus de son UG | sommieR 0.28.0 | 3 |
| Code de placette unique dans l'UG | sommieR 0.28.0 | 3 |
| `nb_vivants` ≤ `nb_total`, `nb_abroutis` ≤ `nb_vivants` | sommieR 0.28.0 | 1 |
| Un contrôle par placette et par jour | sommieR 0.28.0 | 1 |
| Contrôle rattaché à une placette à 15 m au plus | sommieR 0.31.0 | 1 |
| Totaux du contrôle = décompte des états des plants | Surculus (brief) | 1 |
| Un plant appartient à une seule placette et garde son UUID | Surculus (brief) | 1 |

## 7. Tranches

| # | Contenu | Critère d'acceptation |
|---|---|---|
| 0 | Socle : dépôt, spécification, briefs, CI (Kotlin, APK de debug), release-please | ce dépôt ; CI verte sur un squelette |
| 1 | Ouvrir une tournée, carte, **contrôle de placette** complet (guidage, plants pointés, compteur, dictée, totaux, refus immédiats, photos), export `.zip` | la tournée rendue passe `sommier_importer_qfield_travaux()` sur la démo Couchey (sommieR 0.31.1) sans faute ; essai sur appareil avec le ZED-F9P |
| 2 | Relever les interventions (tracé RTK, quantité, prévu/motif), programme proposé si présent | une plantation tracée s'importe avec sa surface ; un écart sans motif est refusé sur le téléphone |
| 3 | Installer une placette, fusion multi-opérateurs `.sursync` | deux opérateurs sur la même tournée : fusion idempotente, import sans doublon |
| 4 | Plants inscrits au registre (après le brief sommieR), suivi plant par plant d'un passage à l'autre | les plants d'une placette contrôlée deux fois gardent leur UUID et leur numéro |

## 8. Hors périmètre

La programmation des travaux et le chantier (fiche, déclaration,
facturation), les calculs du bilan (dans sommieR), le martelage (Marculus),
l'inventaire (Groma). Surculus n'écrit jamais dans la base de sommieR : il
remplit un GeoPackage, et l'import décide.

## 9. Questions ouvertes

1. **Bibliothèques communes** : forme et calendrier de l'extraction depuis
   Marculus et Groma (brief `vers-marculus`). D'ici là, la tranche 1
   reprend-elle provisoirement le code GNSS de Groma, déjà séparé dans
   `groma-core` ?
2. **Rattacher le prévu de nemetonshiny aux UG de sommieR** : les
   identifiants d'UGF de nemetonshiny ne sont pas ceux de sommieR ; le
   brief propose un rattachement géométrique, à valider.
3. **Pointer chaque plant de toutes les placettes** dès le premier passage,
   ou seulement sur une partie (placettes « fines ») ? Le temps de terrain
   est à mesurer en tranche 1.
4. **Identifiant d'application** : `io.github.pobsteta.surculus` (comme
   Marculus) ou `fr.nemeton.surculus` (comme Groma) ?
