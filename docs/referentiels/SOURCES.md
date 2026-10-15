# Inventaire des sources primaires déposées dans `docs/referentiels/`

Propriétaire : Pascal Obstétar. Inventaire établi le 2026-10-15 par l'agent `acquereur`, à partir de l'extraction de texte de chaque PDF (`pdftotext -layout`) et de la lecture de pages d'origine pour contrôle.

## Réutilisation et rediffusion : ce que disent les documents

**Aucun des 24 documents ne porte de licence, de mention « © » ni de conditions de réutilisation dans son texte extrait.** Recherche faite sur : licence, droits, reproduction, propriété intellectuelle, ©, crédits. Les seules mentions de ce type : le RNTSF (« Conception et réalisation imprimerie ONF Fontainebleau - Photos ONF - Septembre 2010 - 9200-10-RN-SAM-001 »), le CNPTSF (« Maquette DCOM », logo PEFC), le guide BFC (« Crédit photo et conception : Parc naturel régional du Morvan »). Les fiches CNPF empruntent des cartes (zones GRECO de l'IGN) et des illustrations de la « Flore forestière française » (introduction_6.pdf p.5/8) : contenu de tiers possible.

Absence de licence ne veut pas dire autorisation. L'acquereur n'est pas juriste et n'a pas pu consulter les mentions légales des sites (recherche web infructueuse). Recommandation, **à trancher par Pascal** :

1. Ne pas versionner les PDF dans un dépôt public tant que l'ONF, le CNPF et la sous-préfecture n'ont pas confirmé par écrit (ou que leur site n'indique pas une licence ouverte). La licence GPL-3.0-or-later du code (brief) ne s'applique pas à des documents de tiers.
2. Dans le dépôt public, ne garder que nos CSV et notices : faits chiffrés, libellés courts, références précises (document, article, page). Ce sont des références, pas des reproductions.
3. Les PDF peuvent rester dans un dépôt privé ou hors dépôt (dossier de travail), avec leur URL officielle dans ce fichier.

Piste à vérifier (non établie) : les documents d'un établissement public administratif comme le CNPF sont en principe réutilisables selon le régime des documents administratifs, sous réserve des droits de tiers ; l'ONF (établissement public à caractère industriel et commercial) peut relever d'un régime différent.

## Inventaire

### 1. `ONF_RNTSF.pdf`
- **Titre exact** : « Règlement national des travaux et services forestiers » (RNTSF).
- **Éditeur** : Office national des forêts, Direction générale, 2 avenue de Saint-Mandé, 75570 Paris Cedex 12.
- **Date / édition** : texte arrêté par la décision du directeur général du 23 juillet 2010 (avis de publication au JO n° 0208 du 08/09/2010) ; résolution n° 2010-12 du conseil d'administration du 21 juillet 2010 ; entrée en vigueur au 1er janvier 2011 pour les contrats signés à partir de cette date ; impression septembre 2010. Le texte précise que la version à jour est consultable sur onf.fr (code de recherche +1147).
- **Objet (CQQCOQP)** : *Qui* : l'ONF, pour ses prestataires et ses équipes. *Pourquoi* : rassembler les prescriptions applicables aux travaux et services forestiers (hors exploitation) en forêt domaniale de métropole. *Quoi* : préservation des milieux, sécurité, organisation du chantier, réception, responsabilité, glossaire. *Où / quand* : forêt domaniale de métropole, contrats signés depuis 2011 ; référence pour les collectivités. *Combien* : 45 pages imprimées (41 pages PDF). Il exclut les prescriptions techniques, renvoyées aux CCTP et cahiers des charges.
- **Réutilisation** : aucune mention. Texte opposable aux prestataires et publié par l'ONF.
- **Utile pour Surculus** : §1.1 (typologie des prestations), §4.5 (réception), §5.1 (garanties), glossaire (dégagement, dépressage, nettoiement, taille de formation, élagage).

### 2. `ONF_CNPTSF_2023.pdf`
- **Titre exact** : « Cahier national des prescriptions des travaux et services forestiers (CNPTSF), Version 2020 ». Ce n'est pas une « nomenclature des prestations » : c'est le successeur probable du RNTSF (non dit dans les passages lus).
- **Éditeur** : ONF, Direction générale. Mention « Mai 2023, Maquette DCOM » : le nom de fichier 2023 correspond à la maquette ; le contenu est la version 2020. « Le texte en vigueur est celui consultable sur le site de l'ONF ».
- **Objet (CQQCOQP)** : *Qui* : l'ONF. *Pourquoi* : fixer les prescriptions générales, spécifiques et particulières applicables à toute intervention (vente de bois, travaux, services) en forêt publique. *Quoi* : biodiversité, eau, sol, patrimoine, protection des peuplements (dont engrillagements et protections individuelles de plants §3.3.1), sécurité, déroulement, réception (§5.5), responsabilités, glossaire. *Où* : forêts domaniales ; forêts des collectivités via l'ONF. *Combien* : 58 pages PDF (environ 50 imprimées). Exclut les prescriptions techniques des CCTP.
- **Réutilisation** : aucune mention.

### 3. `introduction_6.pdf`
- **Titre exact** : « Introduction » aux fiches « Itinéraires techniques par essence ».
- **Éditeur** : Centre national de la propriété forestière (CNPF). **Date** : avril 2020. 8 pages (Intro 1/8 à 8/8).
- **Objet** : *Qui* : 80 collaborateurs du CNPF (11 CRPF et IDF). *Pourquoi* : aider les propriétaires à choisir un itinéraire. *Quoi* : 96 fiches, 20 essences, 13 itinéraires codés I1 à I13, essences codées en 1 ou 2 lettres (p.8/8 : correspondance essence/itinéraire/page). *Où* : France métropolitaine (cartes GRECO). *Conditions d'emploi* : fiches valables pour peuplements à plus de 70 % de l'essence dominante, en adéquation avec la station ; à vérifier avec le SRGS pour un document de gestion.
- **Réutilisation** : aucune mention ; cartes et illustrations de tiers (voir plus haut).

### 4. Les 20 fiches CNPF (avril 2020)
Fichiers : `cedre_compressed.pdf`, `chataignier_compressed.pdf`, `chene_liege_compressed.pdf`, `chene_rouge_compressed.pdf`, `chene_vert_compressed.pdf`, `chenes_blancs_compressed.pdf`, `douglas_compressed.pdf`, `epicea_commun_compressed.pdf`, `epicea_de_sitka_compressed.pdf`, `feuillus_precieux_compressed.pdf`, `hetre.pdf`, `meleze_compressed.pdf`, `noyers_compressed.pdf`, `peuplier_compressed.pdf`, `pin_d_alep_compressed.pdf`, `pin_maritime_compressed.pdf`, `pin_sylvestre_1.pdf`, `pins_noirs_compressed_1.pdf`, `robiniers_compressed.pdf`, `sapin_pectine_compressed.pdf` (20 fiches ; la mission en annonçait 22 : il y en a 20 plus l'introduction).
- **Titre** : « Itinéraires techniques de <espèce> », par itinéraire (I1, I2…), chaque fiche sur deux pages (Description ; Recommandations de gestion). Éditeur CNPF, avril 2020.
- **Objet** : *Quoi* : par essence, objectifs, valeurs de référence (densités, hauteurs, diamètres, âges), gestion recommandée, attentions particulières. *Quand* : par phase (installation, éducation, amélioration, récolte). Pour la plantation : fiche I12 « Renouvellement et boisement » (colonnes Préparation, Installation, Premiers entretiens) ou, sans I12, rubrique Installation de la fiche I1. Données : voir `essences-itineraires.csv`.
- **Réutilisation** : aucune mention. Extraction bruitée pour mélèze et pins noirs (filigranes), exploitable.

### 5. `guide-version-bfc.pdf`
- **Titre exact** : « Guide d'usage des chemins ruraux et des voies communales dans le cadre de la mobilisation des bois en Région Bourgogne-Franche-Comté ».
- **Éditeur / date** : Sous-préfecture de Château-Chinon (dir.), 2025 ; conception et crédits photo : Parc naturel régional du Morvan ; logos en couverture : Préfecture de la Nièvre, ONF, PNR du Morvan, ProETF, Fransylva ; contacts : CNPF, ProETF, Union régionale des communes forestières, PNR du Morvan. 24 pages.
- **Objet** : *Pourquoi* : encadrer l'usage des chemins ruraux et de la voirie communale par les chantiers d'exploitation et de transport de bois (arrêtés du maire, contributions spéciales). *Où* : Bourgogne-Franche-Comté (Nièvre, Morvan).
- **Pertinence pour Surculus : nulle** (mobilisation des bois, pas plantation ni suivi de reprise). Il n'a servi à aucune ligne des référentiels. **Proposition** : le sortir de `docs/referentiels/` (à décider par Pascal).
- **Réutilisation** : aucune mention.

## Écarts entre sources

Voir `codes-travaux.md`, section « Écarts relevés entre sources ».
