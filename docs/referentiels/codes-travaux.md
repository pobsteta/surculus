# Codes travaux de plantation : notice du référentiel (réécriture sur sources primaires)

- **Source** : PDF déposés dans ce dossier (inventaire et conditions de réutilisation : `SOURCES.md`) : RNTSF (ONF, 2010), CNPTSF (ONF, version 2020), 20 fiches CNPF « Itinéraires techniques par essence » (avril 2020).
- **Date de consultation** : 2026-10-15 (agent `acquereur`, seconde passe ; la première passe reposait sur des extraits de recherche web).
- **Propriétaire** : Pascal Obstétar.
- **Fichiers** : `codes-travaux.csv` (libellés), `essences-itineraires.csv` (densités et critères par essence), `correspondance-sommier.md`.
- **Décision éclairée** : quelle nomenclature de codes travaux Surculus et sommieR adoptent (garder les 15 codes provisoires, les remplacer, les rattacher à une convention), et quel critère de reprise Surculus doit calculer (taux, densité, sondage).

## Résultat principal

1. **Il n'existe aucune nomenclature codifiée de travaux dans les sources ONF fournies.** Le RNTSF et le CNPTSF sont des règlements de prescriptions (environnement, sécurité, responsabilité). Le seul « catalogue » est une liste de libellés de prestations (RNTSF §1.1, imp. p.13), sans code, sans unité, sans prix. Les ITTS (fiches d'itinéraires techniques de l'ONF) ne font pas partie des PDF fournis : le brief les citait comme source de codes, rien ne le confirme.
2. **Les fiches CNPF ne codent pas les travaux.** Elles codent les essences (C, CB, CV, CL, CR, CA, D, EC, ES, FP, H, M, N, P, PA, PM, PN, PS, R, SP) et les itinéraires (I1 à I13, ex. « I9_H »), `introduction_6.pdf` p.8/8. Ce sont des codes réels, mais ce ne sont pas des codes de travaux. Les travaux y sont décrits en trois phases : Préparation, Installation, Premiers entretiens (fiches I12).
3. **Conséquence** : les 15 codes `SOMMIER_CODES_TRAVAUX` ne peuvent être « repris » d'aucune convention. Soit sommieR garde un code interne documenté et le rattache aux libellés ci-dessus (voir `correspondance-sommier.md`), soit on demande à l'ONF s'il existe un bordereau codifié (probablement dans les CCTP et bordereaux de prix, non fournis).

## Ce que disent les sources sur la réception, la garantie, le taux de reprise

| Question | Réponse des sources | Où |
|---|---|---|
| Réception | La réception est une rencontre qui vérifie le respect de toutes les prescriptions ; elle peut être refusée ou faire l'objet de réserves en cas de dommages ; remise en état consignée dans un constat d'achèvement contradictoire | RNTSF §4.3.k, §4.4, §4.5, imp. p.34-35 ; CNPTSF §5.4, §5.5, imp. p.39-40 |
| Garantie de reprise | RNTSF : « Des garanties peuvent être demandées dans les documents remis à l'intervenant » (une phrase). CNPTSF : le mot « garantie » n'apparaît pas | RNTSF §5.1, imp. p.37 |
| Taux de reprise, taux de réussite, comptage, sondage | **Absents** des deux documents ONF | |
| États des plants (vivant, mort, abrouti, disparu), causes de mortalité | **Absents** des deux documents ONF. Seuls mots voisins dans les fiches CNPF : « reprise », « mortalité des plants », « difficultés de reprise » (chêne rouge), « dégâts de gibier », « hylobe » | fiches CNPF |
| Obligation de l'intervenant | « Obligation de résultat » ; choix des matériels laissé à l'intervenant ; le « résultat » d'une plantation n'est pas défini | CNPTSF §5.3.1, imp. p.38 |
| Évaluation du prestataire | Appréciée sur le cahier des charges, les délais, les prescriptions, agrégée au niveau national | CNPTSF §5.5, imp. p.39-40 |
| Où se trouvent les exigences techniques de plantation | Dans les CCTP, cahiers des charges, fiches de chantier, bons de commande : exclus des deux documents | RNTSF Préambule imp. p.12 ; CNPTSF §1.3, imp. p.11 |

Les seuls chiffres de reprise des sources primaires sont dans les fiches CNPF :

| Critère | Valeur | Essences (page du PDF) |
|---|---|---|
| Regarnir après la première année de végétation si la reprise est inférieure à… | 80 % | cèdre I12 p.5 ; douglas I12 p.6 ; mélèze I12 p.6 ; pin sylvestre I12 p.10 |
| Regarnir si la mortalité dépasse… | 20 % (soit 80 % de survie) | mélèze I1 p.2 ; chêne-liège I12 p.12 (sur les 5 premières années) |
| Nombre de tiges visé après regarnis | supérieur à 1 600/ha (pin sylvestre planté à plus de 2 000/ha) | pin sylvestre I12 p.10 |
| Regarnir les vides supérieurs à… (régénération naturelle) | 1 are (sapin) ; 5 ares (cèdre, pins noirs) ; 10 ares (chênes blancs) ; 15 ares (feuillus précieux) ; 20 ares (châtaignier) | fiches I1 |
| Regarnis par durée, sans seuil | jusqu'à 3 ans (noyers A) ; obligatoires jusqu'à 4-5 ans (noyers B) | noyers I1 p.2 |
| Aucun critère de reprise ni de regarni de plantation | hêtre, chênes blancs, chêne vert, chêne rouge, châtaignier, feuillus précieux, épicéas, pin maritime, pin d'Alep, pins noirs, robinier, peuplier, sapin | densité seule |

Les valeurs des autres conventions (placeaux de 16 ou 9 plants de l'aide Renouvellement forestier, taux de sondage du DSF, formule Nv/(Nv+Nm-nm) de l'INRAE/ONF, seuil DSF > 80 % à l'automne) viennent de la première passe, **sans texte source ouvert** ; elles restent à l'état de pistes non vérifiées et ne sont plus reprises dans le CSV.

## Écarts relevés entre sources

1. **Les deux documents ONF et les fiches CNPF ne parlent pas de la même chose.** L'ONF encadre le comportement de l'entreprise sur le chantier (environnement, sécurité, réception) ; le CNPF recommande des itinéraires aux propriétaires (densités, entretiens). Aucun des deux ne définit un protocole de constat de reprise.
2. **Taux ou densité** : pour les résineux cités plus haut, le CNPF donne un déclencheur en taux (reprise < 80 %) ; pour les feuillus, uniquement une densité à l'hectare d'essences objectif (par ex. au moins 1 600 plants/ha pour les chênes blancs, 625 à 1 300 pour les feuillus précieux). Le seuil de 80 % de la spec Surculus (`SOMMIER_SEUIL_REPRISE_PCT`) n'est donc fondé que pour 4 essences sur 20 et seulement comme déclencheur de regarni, un an après plantation.
3. **Mortalité ou reprise, un an ou cinq ans** : même seuil de 80 % de survie mais exprimé de deux façons (mélèze : I12 « reprise < 80 % après la première année », I1 « mortalité > 20 % » sans date ; chêne-liège : 20 % de mortalité sur 5 ans). À trancher : le seuil s'applique à quelle date ?
4. **Regarni dans la plantation ou en travail à part** : le RNTSF range « les regarnis » dans la plantation (§1.1) ; sommieR a un code `RG` distinct (voir correspondance).
5. **Dépressage et nettoiement** : deux libellés distincts dans le RNTSF (glossaire imp. p.42 et 44) ; sommieR associe l'action `depressage` au code `NT`.
6. **Pas de seuil par vide pour la plantation** : les seuils en ares concernent la régénération naturelle ; la spec Surculus (§4.5) ne les connaît pas.
7. **Version du texte** : le RNTSF est la version de septembre 2010 (décision du DG du 23/07/2010, avis JO du 08/09/2010) ; le CNPTSF « version 2020 » (maquette mai 2023) ne dit pas dans les passages lus qu'il remplace le RNTSF. À vérifier sur onf.fr, les deux documents indiquant que la version en vigueur est celle du site.
8. **Pages** : les numéros de page imprimés et ceux du fichier PDF diffèrent (RNTSF : 3 à 5 pages d'écart ; CNPTSF : 1 page). Les références du CSV donnent les deux.

## Conventions de contrôle de plantation : ce qu'on peut affirmer

- **Établi par les sources** : la réception ONF contrôle le chantier (prescriptions, remise en état), pas la survie des plants ; la reprise se juge, côté CNPF, par un taux de 80 % à un an pour quelques résineux, ou par une densité d'essences objectif.
- **Non établi par aucune source fournie** : une placette de 50 m², un taux de sondage, une définition de « abrouti » ou « disparu », une attribution de cause de mortalité, une durée de garantie. Ces éléments, s'ils existent, sont dans les contrats (CCTP, CCAP) ou dans des protocoles non fournis (DSF, INRAE).
- **Arithmétique de la placette de 50 m²** (0,005 ha ; nombre de plants attendus = densité × 0,005) : 600 plants/ha donnent 3 plants ; 1 100 donnent 5,5 ; 1 600 donnent 8 ; 2 000 donnent 10 ; 2 500 donnent 12,5 ; les noyers (50 à 210/ha) donnent moins d'un plant. À 5 plants, un plant mort fait passer le taux de 100 % à 80 %, deux à 60 % : le seuil de 80 % n'a pas de sens à ce grain.

## Limites honnêtes

- Les PDF ont été lus par extraction de texte (`pdftotext -layout`) ; les tableaux des fiches CNPF sont en colonnes imbriquées. Les valeurs ont été relues sur le texte ; trois fiches (hêtre p.28 et p.31, mélèze p.6, pins noirs p.7) ont été contrôlées sur l'image du PDF. Les fiches mélèze et pins noirs ont une extraction bruitée (filigranes) : les valeurs retenues viennent des passages propres.
- Les pages de chaque fiche sont calculées sur l'extraction (une fiche : deux pages) ; à contrôler sur le PDF avant citation officielle.
- Le fichier `guide-version-bfc.pdf` ne concerne pas les travaux de plantation (voir `SOURCES.md`).
- Rien n'a été ajouté qui ne soit dans les PDF.
