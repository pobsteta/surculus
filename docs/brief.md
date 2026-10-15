# Surculus — brief

> **Brief amendé le 15 octobre 2026** à partir des arbitrages de Pascal sur la critique du
> `contradicteur` (12 objections) et sur les référentiels acquis par l'`acquereur`
> (`docs/referentiels/`). Les chiffres marqués *extrait* viennent de résumés de recherche :
> les textes intégraux (ministère, DRAAF, DSF) étaient injoignables depuis la session.
> Les passages encore ouverts sont marqués **[ouvert]**. **Validé par Pascal le 15 octobre
> 2026 (porte n° 1, brief).**

## Le problème, et pour qui (Abstraction)

- **Problème, dans l'ordre** :
  1. **le geste** : contrôler une plantation plant par plant sur le terrain, au papier ou
     dans QField, est lent et source d'erreurs, si bien qu'on le fait peu et tard ;
  2. **la preuve** : on ne sait pas montrer, à temps pour un regarni, qu'une plantation
     tient la densité de plants vivants exigée (arrêté régional MFR, à la réception puis
     à 5 ans) ; ce second volet vient après le premier.
- **Bénéficiaires** : d'abord **l'agent ou le technicien de terrain** qui contrôle les
  plants ; ensuite **le propriétaire**, qui paie les travaux et s'engage envers l'État
  quand la plantation est aidée.
- **Coût complet aujourd'hui** (ordre de grandeur, pour 4 ha plantés par an) :
  - environ **40 000 € HT investis par an** (24 000 à 46 000 € selon les essences et les
    protections ; barème forfaitaire des aides 2024, *extrait* —
    `docs/referentiels/couts-plantation.md`) ;
  - environ **20 % des plantations en échec** (reprise < 80 %) en moyenne nationale depuis
    2007 selon le DSF, de 15 % à 38 % selon les années (*extrait* —
    `docs/referentiels/echecs-plantations.csv`), soit environ **0,8 ha et 8 000 € par an**
    exposés à un échec découvert trop tard (calcul de l'orchestrateur) ;
  - le temps passé aujourd'hui à contrôler et à ressaisir : **à mesurer lors du premier
    passage à Marsannay-la-Côte**.

## Ce que fait Surculus

- **En une phrase** : application Android de terrain, hors ligne sauf pour les corrections
  RTK, qui relève les travaux faits dans une plantation et suit, placette par placette et
  plant par plant, ce qui a repris, pour le rendre au sommier forestier.
- **Les gestes sur le terrain** : l'agent ouvre sur son téléphone la tournée préparée par
  sommieR ; il trace au GNSS l'intervention faite (code, surface, prévue ou non), pose ou
  retrouve le piquet de chaque placette, puis pointe chaque plant par rapport au piquet et
  le dit vivant, mort ou non retrouvé, avec ses dégâts, à la voix ou aux boutons, sans
  réseau.
- **Le dispositif de contrôle**, fixé par la tournée et constant pour une même placette :
  - **C1, les k plants les plus proches du piquet**, quand aucune ligne n'est repérable
    (régénération naturelle, plantation sans lignes, enrichissement par placeaux) ;
  - **C2, une portion de ligne de longueur fixe** à partir du piquet, quand des lignes sont
    repérables (y compris une plantation en plein faite en lignes).
- **Les états d'un plant** : vivant, mort, **non retrouvé** (réversible : il peut redevenir
  vivant ou mort au passage suivant).
- **Les dégâts**, un seul vocabulaire : abroutissement, frottis, rongeurs, sanglier, hylobe
  ou insectes, sécheresse, gel, travaux, concurrence, inconnue. Sur un plant vivant, ce sont
  les dégâts observés (plusieurs possibles) ; sur un plant mort, la cause probable (une
  seule ; « inconnue » seulement pour un mort).
- **Ce qu'elle produit** : la tournée complétée (interventions localisées, placettes,
  plants, états, dégâts, photos), avec, calculés sur place, le **taux de reprise** et la
  **densité de plants vivants toutes essences confondues**, et une alerte sous le seuil de
  reprise fixé par la tournée (80 % par défaut, seuil de regarni des fiches CNPF).
  sommieR l'importe dans son registre des travaux.
- **Phase B** (après la v1) : la densité de plants vivants **d'essences-objectif**, à
  comparer aux seuils de l'arrêté MFR de la région.

## Ce qu'elle ne fait pas (hors périmètre)

- La programmation des travaux et le chantier (fiche de chantier, déclaration,
  facturation).
- Les calculs du bilan : ils restent dans sommieR.
- Le martelage (Marculus) et l'inventaire des peuplements (Groma).
- Écrire dans une base de données : Surculus remplit un fichier, l'import de sommieR
  décide.
- En v1 : l'essence de chaque plant et la densité d'essences-objectif (phase B).

## Contraintes connues

- **Terrain** : pas de réseau en forêt ; tout fonctionne hors ligne **sauf les corrections
  RTK** (NTRIP Centipède), disponibles seulement quand le réseau passe. Récepteur GNSS
  RTK externe (u-blox ZED-F9P en Bluetooth), repli sur le GNSS du téléphone. Saisie vocale
  à grammaire fermée et boutons de volume, comme Marculus.
- **Position** : **seul le piquet est positionné au GNSS**, avec la meilleure position
  disponible et sa précision enregistrée (RTK fixe, RTK flottant ou GNSS seul),
  améliorable au passage suivant. Les plants sont repérés par rapport au piquet (distance
  et azimut en C1, rang le long de la ligne en C2).
- **Coordonnées** : stockage en **Lambert-93 (EPSG:2154)**, X vers l'est et Y vers le
  nord, en mètres ; conversion sur le téléphone ; position brute du piquet conservée
  (latitude et longitude ETRS89, précision). Un test vérifie qu'une placette française
  tombe en France.
- **Référentiels** : les codes travaux sont ceux de sommieR
  (`docs/referentiels/codes-travaux-sommier.csv`), **nomenclature interne assumée** :
  aucune source ONF ni CNPF ne codifie les travaux (RNTSF, CNPTSF et fiches CNPF lus).
  Chaque code est rattaché à un libellé du RNTSF §1.1 (`correspondance-sommier.md`).
- **Lien avec l'existant** : sommieR prépare la tournée et la relit, il est la seule
  destination des saisies ; nemetonshiny fournit le prévu (son plan d'action) ; Surculus
  reprend les bibliothèques communes avec Marculus et Groma (GNSS, GeoPackage, carte,
  dictée, journal) sans les recopier ; nemeton n'est pas sur le chemin (confirmé par Pascal le
  15 octobre 2026).
- **Contrat avec sommieR** : le `travaux.gpkg` de sommieR, sans renommer ses couches ni
  ses champs, porte un **numéro de version** (`projet.version_contrat`) que Surculus
  vérifie à l'ouverture de la tournée : une version inconnue est refusée clairement,
  avant le départ en forêt. Une tournée rendue reste **lisible sans sommieR**, par exemple
  dans QGIS (couches et champs documentés avec le contrat).
- **Données personnelles** : l'audio n'est jamais enregistré (seul le texte reconnu est
  gardé) ; l'opérateur est noté par un identifiant court ; l'horodatage est à la minute et
  aucune trace GNSS continue n'est conservée ; les métadonnées EXIF des photos sont
  conservées, et **on ne photographie que le plant, jamais une personne**. Le responsable
  du traitement est le gestionnaire qui emploie les agents.
- **Formats et licences** : GeoPackage (norme ouverte de l'OGC) ; logiciel libre sous
  **GPL-3.0-or-later**, applications et bibliothèques communes comprises. Préalable :
  Pascal, seul auteur de Marculus, lui ajoute la même licence.
- **Sources documentaires** : les PDF de l'ONF et du CNPF ne portent aucune licence de
  rediffusion ; ils restent hors dépôt et seuls nos extraits sourcés à la page sont
  publiés (`docs/referentiels/SOURCES.md`).

## Comment saura-t-on que ça marche ?

- **Signe concret** : un agent contrôle une placette **en moins de 10 minutes, recherche
  du piquet comprise**, sans papier, et la tournée rendue s'importe dans sommieR sans une
  faute.
  - *Risque accepté par Pascal* : ce signe ne compare pas Surculus à QField ; le gain du
    « geste » par rapport à QField ne sera donc pas démontré.
- **Terrain** : la **forêt communale de Marsannay-la-Côte (Côte-d'Or)**, avec au moins
  trois placettes contrôlées deux fois, où Pascal vérifie sur place que les plants pointés
  sont retrouvés au même endroit et que le taux de reprise de Surculus égale son propre
  comptage. Idéalement une parcelle en lignes (C2) et une sans lignes (C1). **[ouvert :
  parcelles, premier utilisateur et échéance à préciser par Pascal]**

## Qui tranche

Pascal Obstétar.
