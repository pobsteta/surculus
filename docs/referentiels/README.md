# Référentiels

> Une seule source de vérité pour les codes et conventions métier. **On n'invente jamais
> un code quand une convention existe.** Chaque fichier indique sa source, sa date et
> son propriétaire.

La forme des fichiers CSV (autant de champs que l'en-tête sur chaque ligne) est vérifiée par le test
[`ReferentielsCsvBienFormesTest`](../../core/src/test/kotlin/io/github/pobsteta/surculus/core/fixtures/ReferentielsCsvBienFormesTest.kt).

## À constituer (agent `acquereur`)

| Référentiel | Source | Propriétaire | Statut |
|---|---|---|---|
| Codes travaux | **Hors Surculus** : lus dans la couche `codes` de la tournée ; nomenclature tenue par sommieR (table de correspondance possible : fascicule 34 du CCTG, nomenclature de l'aide, actions de nemetonshiny) | Pascal | décidé le 2026-10-15 |
| États des plants et causes | DSF (sain, atteint, mort, absent ; causes en quatre groupes au bilan, cinq lignes à la fiche) ; constat de reprise INRAE RENFOR | Pascal | acquis le 2026-10-15, **à valider** : [`etats-plants.yaml`](etats-plants.yaml) (écarts E1 à E5) |
| Protocole de constat de reprise en cheminement | Méthode cemagref (INRAE, ONF) ; protocole DSF ; guide « Réussir la plantation forestière » (2014) ; fascicule 34 du CCTG | Pascal | acquis le 2026-10-15, **à arbitrer** : [`protocole-constat-reprise.md`](protocole-constat-reprise.md) |
| Seuils de reprise et densités minimales | DSF (80 %), ONF (70 %), F34 (80 % / 90 %), IT DGPE 2025-693 et annexe DRAAF AURA (densités minimales de l'aide) | Pascal | acquis le 2026-10-15, **à arbitrer** : [`seuils-densites.csv`](seuils-densites.csv). Texte national chiffré de France 2030 non trouvé |
| Opérations de plantation et correspondance sommieR | CNPF (fiches I12 et I1), guide 2014, fascicule 34, douze documents de l'ONF (dossier `onf/`) | Pascal | acquis le 2026-10-15, **à arbitrer** : [`operations-plantation.csv`](operations-plantation.csv) ; deux systèmes de codes de tâche ONF, chacun dans un seul document : [`codes-taches-onf.csv`](codes-taches-onf.csv) (plateau landais) et [`codes-teck-onf.csv`](codes-teck-onf.csv) (codes article Teck, hêtraies des Pyrénées) |
| Essences et leurs codes | Codes ONF à trois caractères (nomenclature ONF des essences, DT Méditerranée, 2021 ; compléments de la table gftools de Marculus), correspondance avec les codes IGN (IFN) ; famille de seuil de reprise (F34 art. 3-2, p. 25) | Pascal | acquis le 2026-10-15, **à arbitrer** : [`essences.csv`](essences.csv) (écarts E1 à E8 ; code DSF non trouvé) |
| Modèle géomagnétique hors ligne (déclinaison) | WMM2025 (NOAA NCEI et BGS), IGRF-14 (IAGA) ; calculateurs officiels NOAA et BGS | Pascal | acquis le 2026-10-15, **à arbitrer** : [`modele-geomagnetique.md`](modele-geomagnetique.md) (licence BGS non réglée ; pas de bibliothèque Java ou Kotlin établie ; déclinaisons de test au 2026-10-15) |
| Référentiel des positions RTK Centipède | docs.centipede.fr (« Systèmes de référence », « Paramétrage du positionnement dans la base »), CRAIG, EPSG, IGN | Pascal | acquis le 2026-10-15, **à arbitrer** : [`referentiel-centipede.md`](referentiel-centipede.md) (RGF93 ; version v1 ou v2b non établie ; écart avec WGS 84 environ 1 m) |
| Coûts de plantation et de regarni | Barème Fonds Pérenne 2024 (annexe E publiée par la DRAAF Bretagne), Fransylva (Label Bas Carbone, 2025), DSF et CNPF pour les seuils de regarni | Pascal | revérifié le 2026-10-21 (mission 030), **à arbitrer** : [`couts-plantation.csv`](couts-plantation.csv). Barème 2024 lu en entier ; régime d'aide remis en cause en octobre 2026 (montants du nouvel arrêté non lus) ; aucun prix de regarni trouvé ; prix unitaires encore « extrait » |
| Échecs de plantations (observation, pas un code) | DSF : bilans nationaux 2023, 2024 (Lettre du DSF n°61) et 2025 ; bilan Normandie 2025 ; CRFB BFC du 24/11/2025 | Pascal | revérifié le 2026-10-21 (mission 030), **à arbitrer** : [`echecs-plantations.csv`](echecs-plantations.csv). Taux d'échec régional BFC non trouvé |
| Itinéraires techniques CNPF par essence | CNPF, fiches « Itinéraires techniques par essence » (avril 2020) et introduction | Pascal | revérifié le 2026-10-21 (mission 030), **à arbitrer** : [`essences-itineraires.csv`](essences-itineraires.csv). Mélèze non relu (PDF trop gros) ; éléments non relus listés par ligne |
| Densités minimales MFR en Bourgogne-Franche-Comté | Arrêté DRAAF/SREAF-2026-09 du 06/07/2026 (annexe 2) | Pascal | revérifié le 2026-10-21 (mission 030), **à arbitrer** : valeurs en lignes B1 à B6 de [`seuils-densites.csv`](seuils-densites.csv) (source unique) ; notice [`mfr-bfc.md`](mfr-bfc.md). Instruction DGPE 2025-693 abrogée par la 782, elle-même abrogée par la 2026-462 : mêmes valeurs |

Chaque référentiel est déposé en format ouvert (CSV ou YAML) avec, en tête :
source, date de consultation, propriétaire, et la liste des écarts relevés entre sources.

## Documents sources

> **On garde les références, pas les fichiers** (décision de Pascal, 15 octobre 2026) :
> le dépôt est public et les droits de rediffusion ne sont pas établis. Les PDF sont
> rangés localement dans ce dossier, ignorés par git (`.gitignore`) ; chacun se
> retélécharge depuis son URL.

### Réutilisation et rediffusion : ce que disent les sources

*(Repris de l'inventaire `SOURCES.md` de la PR #4, fusionné ici le 2026-10-21 ; l'absence de mention dans les 24 PDF déposés le 15 octobre est un constat de l'ébauche, non refait.)*

- **Les PDF déposés ne portent ni licence ni conditions de réutilisation dans leur texte** (recherche de « licence », « droits », « reproduction », « © » dans l'extraction du 15/10). Mentions relevées : RNTSF (conception et impression ONF Fontainebleau, septembre 2010), CNPTSF (maquette DCOM, logo PEFC). Les fiches CNPF empruntent des cartes de l'IGN et des illustrations de la « Flore forestière française » : contenu de tiers possible.
- **Mentions légales relues le 2026-10-21.** CNPF (https://www.cnpf.fr/mentions-legales) : tous droits réservés, autorisation écrite préalable pour toute reproduction ou diffusion, usage strictement personnel, privé et non commercial toléré, mention « Copyright CNPF, tous droits réservés » avec lien vers www.cnpf.fr pour toute copie autorisée. Ministère de l'Agriculture (https://agriculture.gouv.fr/mentions-legales) : textes sous licence Etalab 2.0 sauf mention contraire ; documents officiels (arrêtés, communiqués, circulaires) non protégés ; photos et vidéos réservées ; citation explicite du site demandée. ONF : mentions légales non relues.
- **Conséquence pour ce dépôt public** (à confirmer par Pascal) : ne pas versionner les PDF ; ne garder que nos CSV et notices (valeurs chiffrées, résumés courts, référence document et page), avec la source citée. Pour les fiches CNPF, ajouter la mention de droits si le fichier est rediffusé ; en cas de doute, écrire à webmaster@cnpf.fr. La licence GPL-3.0-or-later du code ne s'applique pas à des documents de tiers.
- L'ONF (établissement public industriel et commercial) peut relever d'un régime différent de celui du CNPF : non établi.
- **Hors sujet, à sortir du dossier** (proposition de l'ébauche) : `guide-version-bfc.pdf` (« Guide d'usage des chemins ruraux et des voies communales... », Sous-préfecture de Château-Chinon, 2025, 24 pages) : mobilisation des bois, pas plantation ni suivi de reprise ; n'a servi à aucune ligne.

### Textes nationaux de l'ONF

| Fichier local | Éditeur | Source |
|---|---|---|
| `ONF_RNTSF.pdf` | ONF, Direction générale : « Règlement national des travaux et services forestiers » (RNTSF), décision du directeur général du 23 juillet 2010, avis au JO n° 0208 du 08/09/2010, en vigueur au 1er janvier 2011 pour les contrats signés à partir de cette date (version à jour sur onf.fr, code +1147) | _URL à compléter_ |
| `ONF_CNPTSF_2023.pdf` | ONF, Direction générale : « Cahier national des prescriptions des travaux et services forestiers » (CNPTSF), version 2020, maquette de mai 2023 (le nom du fichier suit la maquette, le contenu est de 2020) ; le texte en vigueur est celui du site de l'ONF | _URL à compléter_ |

Ces deux textes fixent des prescriptions générales et excluent les prescriptions techniques renvoyées aux CCTP (inventaire du 15/10, non revérifié).

### ITTS et mémentos de l'ONF

Déposés par Pascal le 15 octobre 2026.

| Fichier local | Éditeur | Source |
|---|---|---|
| `onf/ONF - ITTS_DT Ile-de-France chataignier (codification).pdf` | ONF, DT Île-de-France Nord-Ouest, octobre 2004, « Guide de sylviculture du châtaignier dans le Nord-Ouest » | https://www.onf.fr/vivre-la-foret/+/1f63::guide-de-sylviculture-du-chataignier-dans-le-nord-ouest.html |
| `onf/ONF - ITTS_Chene rouge domaine atlantique.pdf` | ONF, Directions territoriales Auvergne-Limousin, Bourgogne-Champagne-Ardennes, Centre-Ouest, Île-de-France-Nord-Ouest, Sud-Ouest, validé le 10 novembre 2004, « Le chêne rouge du domaine atlantique » | https://www.onf.fr/vivre-la-foret/+/1f6b::guide-de-sylviculture-et-itineraires-techniques-de-travaux-sylvicoles-le-chene-rouge-du-domaine-atlantique.html |
| `onf/ONF - ITTS_Douglasaies francaises.pdf` | ONF, T. Sardin (coord.), janvier 2013 (réf. 9200-13-GUI-SAM-017), « Douglasaies françaises » | https://www.onf.fr/vivre-la-foret/+/2664::travaux-sylvicoles-et-itineraires-techniques-de-travaux-sylvicoles-douglasaies-francaises.html |
| `onf/ONF - ITTS_Travaux sylvicoles hetraies Pyrenees.pdf` | ONF, T. Sardin (coord.), octobre 2013 (réf. 9200-13-GUI-SAM-015), « Hêtraies et hêtraies sapinières des Pyrénées » | https://www.onf.fr/vivre-la-foret/+/2764::travaux-sylvicoles-et-itineraires-techniques-de-travaux-sylvicoles-hetraies-et-hetraies-sapinieres-des-pyrenees.html |
| `onf/ONF - ITTS_Memento hetraies Pyrenees.pdf` | ONF, T. Sardin et T. Villiers, mars 2017 (réf. 9200-17-GUI-SAM-061), mémento « Hêtraies et hêtraies sapinières des Pyrénées » (contient la nomenclature Teck) | https://www.onf.fr/vivre-la-foret/+/26a1::itineraires-techniques-de-travaux-sylvicoles-memento-pratique-hetraies-et-hetraies-sapinieres-des-pyrenees.html |
| `onf/ONF - ITTS_Chenaies Ile-de-France futaie irreguliere.pdf` | ONF, T. Sardin (coord.), validé le 8 décembre 2017, édition 2019, mémento « La futaie irrégulière des chênaies d'Île-de-France » | https://www.onf.fr/vivre-la-foret/+/2666::memento-pratique-et-itineraires-techniques-de-travaux-sylvicoles-la-futaie-irreguliere-des-chenaies-dile-de-france.html |
| `onf/ONF - ITTS_Arc-jurassien.pdf` | ONF, Direction générale, mars 2015 (réf. 9200-15-GUI-SAM-053), « Arc jurassien, sapin et épicéa » | https://www.onf.fr/outils/ressources/94876c35-1917-432c-940a-7b1dd65b7f5b/++versions++/1/++paras++/3/++ass++/1/++i18n++data:fr?download=1 |
| `onf/ONF - ITTS_Chenes Sud-Ouest.pdf` | ONF, DT Sud-Ouest, novembre 2011, « Les chênes du Sud-Ouest (Aquitaine, Midi-Pyrénées) » | https://www.onf.fr/vivre-la-foret/+/268a::itineraires-techniques-de-travaux-sylvicoles-itts-les-chenes-du-sud-ouest-aquitaine-midi-pyrenees.html |
| `onf/ONF_ITTS_memento_alpesSud_2019.pdf` | ONF, mémento ITTS Alpes du Sud, 2019 (année d'après le nom du fichier) | https://www.onf.fr/vivre-la-foret/+/2601::itineraires-techniques-de-travaux-sylvicoles-sylvicultures-de-montagne-des-alpes-du-sud.html |
| `onf/ONF_ITTS_plateau landais.pdf` | ONF, DT Sud-Ouest, agence Landes Nord-Aquitaine, « Le plateau landais », révision post-Klaus 2013 | https://www.onf.fr/vivre-la-foret/+/2766::itineraires-techniques-de-travaux-sylvicoles-itts-plateau-landais-pin-maritime-des-landes.html |
| `onf/ONF - Memento travaux_hetraie_nord atl_futaie reg_2020.pdf` | ONF, mémento ITTS « Hêtraie nord-atlantique en futaie régulière », validé le 14 novembre 2019, édition 2020 | https://www.onf.fr/vivre-la-foret/+/2691::memento-pratique-travaux-hetraie-nord-atlantique-en-futaie-reguliere.html |
| `onf/ONF - Guide-sylvicultures-chenaies continentales.pdf` | ONF, T. Sardin, « Guide des sylvicultures : chênaies continentales », validé le 12 décembre 2007, édition 2008 (ISBN 978-2-84207-321-3) | _URL à compléter (le PDF n'en porte pas)_ |

### Fiches itinéraires techniques par essence du CNPF

Page : https://www.cnpf.fr/gestion-durable-des-forets/mise-en-oeuvre/fiches-itineraires-techniques-par-essence
(consultée et téléchargée le 15 octobre 2026 ; fiches relues pour la mission 030 le 21 octobre 2026, voir [`essences-itineraires.csv`](essences-itineraires.csv)). Les fiches sont datées d'avril 2020 ; l'introduction (8 pages) est `cnpf/introduction_6.pdf`. Il y a 20 fiches par essence plus l'introduction (la mission d'origine en annonçait 22).

| Essence | Fichier local | URL |
|---|---|---|
| Équipes projet (2021-07-26), hors itinéraires (confirmé le 2026-10-15 : présentation des 23 équipes du projet, sans contenu technique) | `cnpf/2021_07_26_equipes_projet_vfb.pdf` | https://www.cnpf.fr/sites/socle/files/cnpf-old/2021_07_26_equipes_projet_vfb.pdf |
| Cèdres | `cnpf/cedre_compressed.pdf` | https://www.cnpf.fr/sites/socle/files/cnpf-old/cedre_compressed.pdf |
| Châtaignier | `cnpf/chataignier_compressed.pdf` | https://www.cnpf.fr/sites/socle/files/cnpf-old/chataignier_compressed.pdf |
| Chêne-liège | `cnpf/chene_liege_compressed.pdf` | https://www.cnpf.fr/sites/socle/files/cnpf-old/chene_liege_compressed.pdf |
| Chêne rouge | `cnpf/chene_rouge_compressed.pdf` | https://www.cnpf.fr/sites/socle/files/cnpf-old/chene_rouge_compressed.pdf |
| Chêne vert | `cnpf/chene_vert_compressed.pdf` | https://www.cnpf.fr/sites/socle/files/cnpf-old/chene_vert_compressed.pdf |
| Chênes blancs | `cnpf/chenes_blancs_compressed.pdf` | https://www.cnpf.fr/sites/socle/files/cnpf-old/chenes_blancs_compressed.pdf |
| Douglas | `cnpf/douglas_compressed.pdf` | https://www.cnpf.fr/sites/socle/files/cnpf-old/douglas_compressed.pdf |
| Épicéa commun | `cnpf/epicea_commun_compressed.pdf` | https://www.cnpf.fr/sites/socle/files/cnpf-old/epicea_commun_compressed.pdf |
| Épicéa de Sitka | `cnpf/epicea_de_sitka_compressed.pdf` | https://www.cnpf.fr/sites/socle/files/cnpf-old/epicea_de_sitka_compressed.pdf |
| Feuillus précieux | `cnpf/feuillus_precieux_compressed.pdf` | https://www.cnpf.fr/sites/socle/files/cnpf-old/feuillus_precieux_compressed.pdf |
| Hêtre | `cnpf/hetre.pdf` | https://www.cnpf.fr/sites/socle/files/cnpf-old/hetre.pdf |
| Introduction | `cnpf/introduction_6.pdf` | https://www.cnpf.fr/sites/socle/files/cnpf-old/introduction_6.pdf |
| Mélèzes | `cnpf/meleze_compressed.pdf` | https://www.cnpf.fr/sites/socle/files/cnpf-old/meleze_compressed.pdf |
| Noyers | `cnpf/noyers_compressed.pdf` | https://www.cnpf.fr/sites/socle/files/cnpf-old/noyers_compressed.pdf |
| Peupliers | `cnpf/peuplier_compressed.pdf` | https://www.cnpf.fr/sites/socle/files/cnpf-old/peuplier_compressed.pdf |
| Pin d'Alep | `cnpf/pin_d_alep_compressed.pdf` | https://www.cnpf.fr/sites/socle/files/cnpf-old/pin_d_alep_compressed.pdf |
| Pin maritime | `cnpf/pin_maritime_compressed.pdf` | https://www.cnpf.fr/sites/socle/files/cnpf-old/pin_maritime_compressed.pdf |
| Pin sylvestre | `cnpf/pin_sylvestre_1.pdf` | https://www.cnpf.fr/sites/socle/files/cnpf-old/pin_sylvestre_1.pdf |
| Pins noirs | `cnpf/pins_noirs_compressed_1.pdf` | https://www.cnpf.fr/sites/socle/files/cnpf-old/pins_noirs_compressed_1.pdf |
| Robinier | `cnpf/robiniers_compressed.pdf` | https://www.cnpf.fr/sites/socle/files/cnpf-old/robiniers_compressed.pdf |
| Sapin pectiné | `cnpf/sapin_pectine_compressed.pdf` | https://www.cnpf.fr/sites/socle/files/cnpf-old/sapin_pectine_compressed.pdf |
