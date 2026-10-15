# Codes travaux de plantation : notice du référentiel

Propriétaire : Pascal Obstétar. Consultation : 2026-10-15 (acquereur). Fichier : `codes-travaux.csv`.
Décision éclairée : quelle nomenclature de codes sommieR et Surculus adoptent pour les travaux de plantation (garder les 15 codes provisoires, les remplacer, ou les rattacher à une convention).

## Résultat principal : pas de code à reprendre

- **ITTS de l'ONF** : ce sont des fiches « itinéraire technique de travaux sylvicoles » par essence et contexte (Arc jurassien sapin-épicéa 2015, Alpes du Sud 2019, pin maritime des Landes 2013, chênes du Sud-Ouest 2011, hêtraies pyrénéennes 2017, douglasaies 2013). Producteur : ONF, but : référence technique puis économique, cadrée par DRA/SRA. Pages repérées : https://www.onf.fr/vivre-la-foret/+/2601::itineraires-techniques-de-travaux-sylvicoles-sylvicultures-de-montagne-des-alpes-du-sud.html et https://www.onf.fr/outils/ressources/94876c35-1917-432c-940a-7b1dd65b7f5b/++versions++/1/++paras++/3/++ass++/1/++i18n++data:fr?_=1740129517.049082&download=1 (ITTS 2015 Arc jurassien). **Aucun PDF ITTS n'a été ouvert** ; je n'ai vu aucun code de travaux dans les extraits. Possible qu'un code existe dans les fiches elles-mêmes : à vérifier en ouvrant une ITTS.
- **Nomenclature interne ONF / RNTSF** (Règlement national des travaux et services forestiers) : citée comme cadre de codification unique des prescriptions, mais non trouvée en ligne. À demander à l'ONF si Pascal veut un code officiel.
- **CNPF / CRPF** : aucune « fiche travaux » codée trouvée. Seuls des guides et codes de bonnes pratiques (CRPF PACA, CRPF Hauts-de-France) en libellés.
- **Aides** : le cahier des charges AAP Renouvellement forestier volet 1 (ADEME, France 2030, 2023) parle de placeaux et de barèmes, sans code travaux.
- Seul code réel rencontré : CPV 77200000 (marchés publics, « Services sylvicoles »). Trop grossier pour Surculus.

## Conventions de contrôle de reprise relevées (extraits, à vérifier au texte)

| Point | Valeur relevée | Source |
|---|---|---|
| Placeau d'aide | 16 plants (objectif 12 vivants, 75 %) ou 9 plants (objectif 7, soit 78 %) | CDC volet 1, 2023 |
| Seuil de réussite DSF | taux de reprise > 80 % à la notation d'automne | DSF / instruction DGAL 2017 |
| Formule de taux de reprise | Nv x 100 / (Nv + Nm - nm), nm = morts pour causes indépendantes de l'entreprise | protocoles de constat (INRAE/ONF, Puyal et al., 2021) |
| États minimaux | vivant; mort; mort pour cause extérieure à l'entreprise | idem |
| Taux de sondage | de 50 % (1 à 400 plants) à 10 % (1101 à 2500 plants); recommandation générale ~3 % | DSF ; INRAE/ONF 2021 |
| Échéance | à partir du 1er octobre suivant d'au moins 90 jours la réception ; notation de printemps un mois après débourrement | extrait recherche (source primaire non identifiée) |
| Seuil de regarni | ~80 % (essai INRAE) ; 40 % de mortalité (rapport de remise en état) ; regarni non éligible aux aides sauf force majeure | voir écarts |
| Forme/rayon de placette DSF, échéances 1re/2e saison, état « abrouti » | **NON TROUVÉS** | |

## Écarts relevés entre sources

1. **Seuil de réussite** : 80 % (DSF, essai INRAE) contre 75 % / 78 % (placeaux de l'aide). Cause probable : l'aide raisonne par placeau de petit effectif entier (12/16, 7/9), le DSF par taux global. Le seuil de 80 % de sommieR (`SOMMIER_SEUIL_REPRISE_PCT`) suit le DSF mais pas l'aide : une placette à 12/16 est « sous le seuil » pour Surculus et « conforme » pour l'aide.
2. **Définition du taux** : le constat de reprise exclut les morts hors responsabilité de l'entreprise (nm); le brief Surculus compte vivants / total pointés, sans catégorie « mort pour cause extérieure ». Taux de reprise (technique) et taux de réussite (contractuel/aide) ne sont pas la même chose.
3. **Échelle de sondage** : placettes de 50 m² (spec Surculus, 3,99 m) contre sondage par lignes et taux (INRAE) contre placeaux de 9 ou 16 plants (aide). Trois logiques d'échantillonnage.
4. **Regarni** : seuil technique ~80 %, mais l'aide l'exclut en principe.
5. **Source ITTS** : la mission supposait des codes ONF ; ils n'apparaissent pas publiquement.

## Limites honnêtes

WebFetch a échoué (ENOTFOUND) sur tous les domaines essayés (agriculture.gouv.fr, oise.gouv.fr, renfor, cnpf.fr). Tout provient de résumés de recherche, pas des PDF. Le guide « Réussir la plantation forestière » et le protocole renfor n'ont pas été lus. Aucune deuxième source indépendante n'a pu confirmer les chiffres de sondage et de calendrier.
