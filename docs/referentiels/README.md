# Référentiels

> Une seule source de vérité pour les codes et conventions métier. **On n'invente jamais
> un code quand une convention existe.** Chaque fichier indique sa source, sa date et
> son propriétaire.

## À constituer (agent `acquereur`)

| Référentiel | Source | Propriétaire | Statut |
|---|---|---|---|
| Codes travaux | **Hors Surculus** : lus dans la couche `codes` de la tournée ; nomenclature tenue par sommieR (table de correspondance possible : fascicule 34 du CCTG, nomenclature de l'aide, actions de nemetonshiny) | Pascal | décidé le 2026-10-15 |
| États des plants et causes | DSF (sain, atteint, mort, absent ; causes en cinq groupes) ; constat de reprise INRAE RENFOR | Pascal | à acquérir |
| Protocole de constat de reprise en cheminement | Méthode cemagref (INRAE, ONF) ; protocole DSF ; guide « Réussir la plantation forestière » (2014) ; fascicule 34 du CCTG | Pascal | à acquérir |
| Seuils de reprise et densités minimales | DSF (80 %), ONF (70 % à N+2), densités minimales de l'aide au renouvellement | Pascal | à acquérir |
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
| `ONF - ITTS_Arc-jurassien.pdf` | ONF | _URL à compléter_ |
| `ONF - ITTS_Chenes Sud-Ouest.pdf` | ONF | _URL à compléter_ |
| `ONF_ITTS_memento_alpesSud_2019.pdf` | ONF | _URL à compléter_ |
| `ONF_ITTS_plateau landais.pdf` | ONF | _URL à compléter_ |
| `ONF - Memento travaux_hetraie_nord atl_futaie reg_2020.pdf` | ONF | _URL à compléter_ |
| `ONF - Guide-sylvicultures-chenaies continentales.pdf` | ONF | _URL à compléter_ |

### Fiches itinéraires techniques par essence du CNPF

Page : https://www.cnpf.fr/gestion-durable-des-forets/mise-en-oeuvre/fiches-itineraires-techniques-par-essence
(consultée et téléchargée le 15 octobre 2026).

| Essence | Fichier local | URL |
|---|---|---|
| Équipes projet (2021-07-26), hors itinéraires, à confirmer | `cnpf/2021_07_26_equipes_projet_vfb.pdf` | https://www.cnpf.fr/sites/socle/files/cnpf-old/2021_07_26_equipes_projet_vfb.pdf |
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
