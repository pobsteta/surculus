# Coûts de référence : plantation et regarni (`couts-plantation.csv`)

- **Propriétaire** : Pascal Obstétar.
- **Date de consultation** : 2026-10-15. Acquis par l'agent `acquereur`.
- **Décision éclairée** : chiffrer le « coût complet » d'une plantation et d'un regarni dans le brief de Surculus (et fixer l'ordre de grandeur annuel pour ~4 ha/an).
- **Niveau de vérification : tout est « extrait ».** `WebFetch` a échoué (ENOTFOUND) sur tous les sites ; les chiffres viennent d'extraits renvoyés par `WebSearch`, aucun PDF lu en entier. **Aucun chiffre n'est confirmé sur texte intégral.** À valider sur les documents d'origine avant tout usage contractuel.

## Ce qu'il faut savoir avant de lire les chiffres

1. **Le barème retenu par l'administration est un forfait d'aide, pas une facture.** C'est la valeur « retenue » la plus officielle trouvée (barème France Nation Verte / Fonds Pérenne 2024, annexe E publiée par la DRAAF Bretagne). Il sert à calculer une subvention.
2. **Le régime d'aide est en mouvement.** Selon un blog juridique (landot-avocats, 2026-10-11, non vérifié sur le texte officiel) : le décret ayant créé France Nation Verte a été annulé par le Conseil d'État le 15 juillet 2026 ; le décret n° 2026-937 du 8 octobre 2026 et un arrêté NOR TECT2624357A du même jour le rétablissent ; taux de base de 40 à 50 % des travaux et frais de maîtrise d'œuvre ; plafond 2 M€ par bénéficiaire ; dépôt de nouveaux dossiers signalé comme suspendu. **Le barème 2024 ci-dessous peut avoir changé dans l'arrêté d'octobre 2026, que je n'ai pas pu lire.** Source à ouvrir en priorité : Légifrance, NOR TECT2624357A.
3. **Pas de barème officiel BFC 2024-2026 trouvé.** Seul repère régional : mémento BFC 2020 (aide plafonnée à 1 500 €/ha, 40 %, projet de plus de 2 ha). Le barème utilisé ici est national ; la colonne de zone (plaine / montagne) n'a pas pu être confirmée dans l'extrait.
4. **Deux lacunes nettes** : (a) aucun prix de regarni publié trouvé ; (b) aucun prix de dégagements autonome (ils sont inclus dans le forfait « avec 3 entretiens »). Aucun prix de plant forestier de pépinière 2024-2026 non plus.

## Fourchettes retenues par type (€/ha, forfaits d'aide 2024, hors protection gibier et hors maîtrise d'œuvre)

| Type | < 4 ha | 4-10 ha | > 10 ha |
|---|---|---|---|
| Résineux (CS3) | 5 200 | 4 952 | 4 704 |
| Feuillus nobles (CS6) | 5 658 | 5 389 | 5 120 |
| Chênes, châtaignier (CS7) | 6 445 | non lu | non lu |

Ajouts (même barème, même niveau d'extrait) : protections individuelles ≥ 1,20 m 3 003 / 2 856 / 2 693 €/ha ; répulsif 504 / 475 / 452 €/ha ; clôture ≥ 1,8 m 4 380 / 3 070 / 2 020 €/ha ; maîtrise d'œuvre 1 500 € + 18 % (< 4 ha) ou + 16 % (4-10 ha).

Pas de ligne « plantation par placeaux / enrichissement » : **aucun chiffre trouvé** (la fiche Fibois Grand Est indique seulement que l'enrichissement est une dépense éligible, sans montant). Ne pas extrapoler au prorata.

## Écarts relevés entre sources

| Sujet | Source A | Source B | Écart / cause |
|---|---|---|---|
| Protections individuelles ≥ 1,20 m, < 4 ha | 3 003 €/ha (barème 2024, Bretagne) | 2 760 €/ha (Fransylva, « AURA 2023 ») | +8,8 %, rapport constant sur les trois tranches (1,089 ; 1,089 ; 1,084) : même gabarit national, revalorisé. **Pas deux sources indépendantes.** |
| Protection chevreuil, prix unitaire | 2,10 € posée (Fransylva, non datée) | 1,50-1,70 €/arbre feuillus (Ambert 2018) | Ambert 19-29 % plus bas : 2018 contre étude non datée ; prix de 2018 probablement obsolètes. |
| Forfait vs unitaire | 3 003 €/ha | 2,10 × 1 430 plants = 3 003 | Égalité exacte : le forfait semble construit sur le prix Fransylva et une densité implicite de ~1 430 plants/ha. **Concordance non indépendante.** |
| Coût total | Barème reconstitué : ~9 400 à 11 500 €/ha (voir calcul ci-dessous) | CNPF BFC : 11 828 €/ha (5,5 ha, 2024-2028) | Même ordre de grandeur (BFC entre 3 et 26 % plus haut) : **seule vraie concordance entre sources d'origines différentes** (forfait national, projet réel régional), mais postes du projet CNPF non lus. |
| Coût total bas | Fransylva LBC : 4 500-7 000 €/ha (2024) ; Hérault 4 000-5 000 | Barème reconstitué ~9 400+ | Écart d'un facteur ~2. Cause probable : périmètres différents (LBC peut exclure protections ou MOE ; Hérault oral). **Non établie.** |
| Plant | 1,25 € posé (Fransylva, chêne 30/50) | 1,70 € TTC (Granville Terre et Mer 2024) | Non comparables : plant de haie, TTC, petite quantité. Aucun prix de pépinière forestière 2024-2026 trouvé. |
| Maîtrise d'œuvre | 1 500 € + 18/16 % (Bretagne 2024) | idem (AURA 2023) | Concordance non indépendante ; incohérence interne dans AURA (16 % pour les plus gros projets, 14 % au-delà de 20 ha). |
| 2 310 €/ha | Barème 2024, ligne CS1 pin maritime | Étude Fransylva (plants + protection, pin maritime) | Valeur identique dans deux documents, et 2 310 = 1 100 × 2,10. **Soit coïncidence, soit extraits confondus par l'outil de recherche** : à vérifier sur les documents. Non utilisée. |

## Ordre de grandeur pour 4 ha/an

Hypothèses (à valider par Pascal) : une opération de 4 ha dans l'année tombe dans la tranche « 4-10 ha » (le seuil exact à 4 ha est à vérifier) ; plantation en plein, protections individuelles sur tous les plants (pas de clôture) ; maîtrise d'œuvre = 1 500 € + 16 % appliqués à la somme travaux + protections (assiette non confirmée) ; HT ; valeurs 2024 sans indexation ; coûts réels pouvant dépasser le forfait.

| Scénario | Travaux + protections | MOE | Total pour 4 ha | Par ha |
|---|---|---|---|---|
| Résineux, sans protection | 4 952 × 4 = 19 808 | 4 669 | **~24 500 €** | ~6 100 |
| Résineux protégé | (4 952 + 2 856) × 4 = 31 232 | 6 497 | **~37 700 €** | ~9 400 |
| Feuillus nobles protégés | (5 389 + 2 856) × 4 = 32 980 | 6 777 | **~39 800 €** | ~9 900 |
| Chênes protégés (tranche < 4 ha, par défaut) | (6 445 + 3 003) × 4 = 37 792 | 8 303 | **~46 100 €** | ~11 500 |

Si clôture à la place des protections individuelles (tranche 4-10 ha, 3 070 €/ha) : +~860 €/an par rapport aux protections individuelles, mais le forfait ne dépend pas de la forme réelle de la parcelle. Fourchette de travail : **~24 000 à 46 000 € par an, ordre de grandeur central ~38 000-40 000 € pour des feuillus protégés**, soit environ 9 500-11 500 €/ha (cohérent avec 11 828 €/ha du CNPF BFC). Avant subvention. Un taux de 40-50 % (non vérifié) laisserait à charge environ 50-60 % de ce montant.

## Regarni de 20 % des plants : calcul (aucun prix publié)

Hypothèses : densité 1 500 plants/ha (minimum cité pour hêtre et chênes dans l'arrêté régional 2025 de Bretagne, selon un extrait) soit 300 plants/ha à remplacer, 1 200 plants pour 4 ha ; prix unitaires de l'étude Fransylva (non datée, probablement sous-estimés en 2026) : plant 0,85 + pose 0,40 = 1,25 € ; protection chevreuil 2,10 €.

- Sans protection : 1,25 € × 300 = **~375 €/ha**, soit **~1 500 € pour 4 ha**.
- Avec protection chevreuil : 3,35 € × 300 = **~1 005 €/ha**, soit **~4 000 € pour 4 ha**.
- Variante avec les prix Ambert 2018 pour la pose et la protection feuillus : ~2,65-2,90 € par plant, soit ~800-870 €/ha.

Ce calcul n'inclut ni le déplacement, ni la préparation de poches, ni un surcoût de petite quantité (le regarni est probablement plus cher par plant qu'une plantation en plein). **Calcul de l'acquéreur, pas une donnée sourcée.** Repères de taux : 12 % de mortalité la première année (INRAE/CNPF) ; remplacement par le reboiseur au-delà de 20 % (Univ. Lorraine) ; mortalité due à la sécheresse ou au gibier généralement exclue de la garantie.

## Trois points au sol que Pascal peut vérifier

1. **Densité réelle et nombre de protections** sur une parcelle plantée : compter les plants sur une placette de 100 m² et extrapoler. Le forfait de 3 003 €/ha suppose ~1 430 plants/ha protégés ; si la densité réelle ou le choix des protections diffère, le poste le plus lourd du budget bouge.
2. **Taux de reprise à un an** sur les plantations existantes de Marsannay : compter les plants morts sur 2-3 placettes. 12 % (INRAE) et 20 % (seuil de garantie) sont les deux repères ; c'est le taux qui fixe le coût du regarni.
3. **Périmètre d'une parcelle à clôturer** : mesurer le linéaire réel (en ml) d'une parcelle type et le multiplier par 9,20 €/ml (Fransylva) pour comparer au forfait de clôture (4 380 €/ha sous 4 ha) et au forfait de protections individuelles ; le choix clôture contre protections dépend de la forme et de la taille de la parcelle.

## À acquérir ensuite (non fait, car non trouvé en extrait)

Texte intégral de l'arrêté NOR TECT2624357A et de son barème ; annexe E complète (lignes dégagements, regarni, colonnes de zones) ; un devis local 2026 de pépiniériste et d'entrepreneur forestier (Côte-d'Or) ; le détail du projet CNPF BFC (11 828 €/ha).

## Sources (toutes en extrait de recherche)

- DRAAF Bretagne, barème Fonds Pérenne 2024 : https://draaf.bretagne.agriculture.gouv.fr/IMG/pdf/fnv_annexe_e_-_bareme_2024-11-04.pdf
- Fransylva, AG 19 mai 2023 : https://cf.appdrag.com/fransylva-38ec2b/uploads/files/37f98d93-f1bd-411c-93ad-df321d462611.pdf
- Fransylva, étude sur les coûts de plantation : https://www.fransylva.fr/uploads/files/06ac02e4-4fb0-449e-ab42-7ca428214d20.docx
- Fransylva, présentation Label Bas Carbone, 2025 : https://cf.appdrag.com/fransylva-38ec2b/uploads/files/Presentation-LBC---AG-Loiret-05-04-2025-Q3fe.pdf
- CNPF BFC, présentation : https://www.bourgogne-franche-comte.developpement-durable.gouv.fr/IMG/pdf/foret_presentaiton_cnpf.pptx.pdf
- Mémento BFC 2020 : https://www.bourgognefranchecomte.fr/sites/default/files/2020-03/Guide%20des%20aides%20forets-2020_0.pdf
- Réunion forestière d'Ambert, 2018 : https://www.ambertlivradoisforez.fr/wp-content/uploads/2019/03/R%C3%A9union-foresti%C3%A8re-de-7-d%C3%A9cembre-2018.pdf
- INRAE RENFOR : https://renfor.hub.inrae.fr/content/download/3896/36455?version=1 et https://renfor.hub.inrae.fr/content/download/4569/41851?version=3
- Univ. Lorraine (HAL) : https://hal.univ-lorraine.fr/hal-03998594v1/document
- Landot avocats, 2026-10-11 (régime d'aide, non vérifié) : https://blog.landot-avocats.net/2026/10/11/renaissance-des-regimes-daides-au-renouvellement-forestier/
- Peupliers de France, Granville Terre et Mer, Europe 1 : URL dans le CSV.
