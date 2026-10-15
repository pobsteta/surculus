# Référentiels

> Une seule source de vérité pour les codes et conventions métier. **On n'invente jamais
> un code quand une convention existe.** Chaque fichier indique sa source, sa date et
> son propriétaire.

## À constituer (agent `acquereur`)

| Référentiel | Source | Propriétaire | Statut |
|---|---|---|---|
| Codes travaux | **Hors Surculus** : lus dans la couche `codes` de la tournée ; nomenclature tenue par sommieR (table de correspondance possible : fascicule 34 du CCTG, nomenclature de l'aide, actions de nemetonshiny) | Pascal | décidé le 2026-10-15 |
| États des plants et causes | DSF (sain, atteint, mort, absent ; causes en quatre groupes au bilan, cinq lignes à la fiche) ; constat de reprise INRAE RENFOR | Pascal | acquis le 2026-10-15, **à valider** : [`etats-plants.yaml`](etats-plants.yaml) (écarts E1 à E5) |
| Protocole de constat de reprise en cheminement | Méthode cemagref (INRAE, ONF) ; protocole DSF ; guide « Réussir la plantation forestière » (2014) ; fascicule 34 du CCTG | Pascal | acquis le 2026-10-15, **à arbitrer** : [`protocole-constat-reprise.md`](protocole-constat-reprise.md) |
| Seuils de reprise et densités minimales | DSF (80 %), ONF (70 %), F34 (80 % / 90 %), IT DGPE 2025-693 et annexe DRAAF AURA (densités minimales de l'aide) | Pascal | acquis le 2026-10-15, **à arbitrer** : [`seuils-densites.csv`](seuils-densites.csv). Texte national chiffré de France 2030 non trouvé |
| Opérations de plantation et correspondance sommieR | CNPF (fiches I12 et I1), guide 2014, fascicule 34, douze documents de l'ONF (dossier `onf/`) | Pascal | acquis le 2026-10-15, **à arbitrer** : [`operations-plantation.csv`](operations-plantation.csv) ; deux systèmes de codes de tâche ONF, chacun dans un seul document : [`codes-taches-onf.csv`](codes-taches-onf.csv) (plateau landais) et [`codes-teck-onf.csv`](codes-teck-onf.csv) (codes article Teck, hêtraies des Pyrénées) |
| Essences et leurs codes | _à déterminer (cf. codes CHS / CHP dictés en alphabet international dans Marculus)_ | Pascal | à acquérir |

Chaque référentiel est déposé en format ouvert (CSV ou YAML) avec, en tête :
source, date de consultation, propriétaire, et la liste des écarts relevés entre sources.

## Documents sources

> **On garde les références, pas les fichiers** (décision de Pascal, 15 octobre 2026) :
> le dépôt est public et les droits de rediffusion ne sont pas établis. Les PDF sont
> rangés localement dans ce dossier, ignorés par git (`.gitignore`) ; chacun se
> retélécharge depuis son URL.

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
(consultée et téléchargée le 15 octobre 2026).

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
