# Modèle géomagnétique hors ligne : WMM2025 et IGRF-14

- **Propriétaire** : Pascal Obstétar
- **Date de consultation** : 2026-10-15
- **Constitué par** : agent `acquereur`
- **Décision éclairée** : le modèle de déclinaison embarqué dans Surculus, hors ligne (spec § 3, ligne « Azimut géographique » ; règle § 6 « déclinaison enregistrée »). Rien n'est tranché ici : chaque option est sourcée, la décision (et l'ADR « GNSS et repères ») revient à Pascal.

## Sources

| Sigle | Document | Lecture |
|---|---|---|
| NCEI-WMM | NOAA NCEI, « World Magnetic Model (WMM) », https://ncei.noaa.gov/products/world-magnetic-model | page web, lue par un outil de résumé (citations courtes reprises mot à mot par lui) |
| NCEI-META | NOAA NCEI, fiche de métadonnées « World Magnetic Model 2025 », https://www.ncei.noaa.gov/metadata/geoportal/rest/metadata/item/gov.noaa.ngdc:WMM2025/html, DOI 10.25921/aqfd-sd83 | idem |
| NCEI-ERR | NOAA NCEI, « Accuracy, limitations and error model », lu à l'adresse https://www.ncei.noaa.gov/node/7141 (la réponse de l'API NOAA renvoie vers https://www.ncei.noaa.gov/products/world-magnetic-model/accuracy-limitations-error-model) | idem ; le rapport technique WMM2025 n'est pas lu |
| BGS-WMM | British Geological Survey, https://geomag.bgs.ac.uk/research/modelling/WorldMagneticModel.html (l'ancienne adresse eap.bgs.ac.uk renvoie une erreur 404) ; fiche data.gov.uk https://www.data.gov.uk/dataset/world-magnetic-model | idem |
| NCEI-IGRF | NOAA NCEI, « IGRF 14th generation », https://www.ncei.noaa.gov/products/international-geomagnetic-reference-field ; GFZ « IGRF-14 released » https://www.gfz.de/en/section/geomagnetism/overview/detailpage-section-news/igrf-14-released | idem (GFZ : vu seulement dans les résultats de recherche) |
| API-NOAA | calculateur NOAA (interface programmable du calculateur de déclinaison), https://www.ngdc.noaa.gov/geomag-web/calculators/calculateDeclination?...&model=WMM (ou IGRF) | appels du 2026-10-15, réponses JSON lues |
| API-BGS | service web BGS, https://geomag.bgs.ac.uk/web_service/GMModels/wmm/2025?latitude=..&longitude=..&altitude=0&date=2026-10-15&format=json | appels du 2026-10-15 |
| GEOLIB | GeographicLib, https://geographiclib.sourceforge.io/C++/doc/magnetic.html ; licence https://geographiclib.sourceforge.io/LICENSE.txt ; classes Java https://geographiclib.sourceforge.io/Java/doc/net/sf/geographiclib/package-summary.html | pages web |

## Les deux modèles (Qui, Quoi, Quand, Combien)

| | WMM2025 | IGRF-14 |
|---|---|---|
| **Qui** | NGA (États-Unis) et DGC (Royaume-Uni) ; développé par NCEI et BGS ; NCEI distribue pour la NGA, BGS pour la DGC (NCEI-WMM, data.gov.uk) | Groupe de travail de l'IAGA ; coefficients arrêtés en novembre 2024 (NCEI-IGRF) |
| **But** | navigation et cap (usage militaire et civil) | modèle de référence scientifique du champ interne |
| **Quoi** | degré 12, variation séculaire linéaire (BGS-WMM : « degree 12 spherical harmonic model of the main field at 2025.0, plus a secular variation estimate for 2025.0 to 2030.0 ») | champ principal jusqu'au degré 13, variation séculaire jusqu'au degré 8 pour 2025-2030 (résumé de recherche, GFZ et WHU) |
| **Fichier** | `WMM.COF` (« WMM2025 Coefficient File », décembre 2024), avec valeurs de test et instructions de mise à jour ; logiciels C Windows et Linux, février 2025 | `igrf14coeffs.txt` (https://www.ngdc.noaa.gov/IAGA/vmod/coeffs/igrf14coeffs.txt) et `IGRF14coeffs.xlsx` ; Geomag 7.0 ; Fortran `igrf14.f` du BGS ; paquet Python `pyIGRF14` (NCEI-IGRF) |
| **Validité** | publié le 2024-12-17. NCEI : « expires on December 31, 2029 », dates acceptées 2025.0 à 2030.0, calculateur en ligne jusqu'au 2029-12-31. BGS : « valid for the period 2025.0 to 2030.0 » (écart de un an, voir écarts) | 1900.0 à 2030.0 (GeographicLib : igrf14 1900-2030). La page NCEI ne donne pas de période |
| **Combien (précision)** | **aucune valeur chiffrée sur la page d'accueil NCEI.** NCEI-ERR : erreur de déclinaison à un écart-type, D = racine(0,26² + (5417 / H)²) degrés (H = intensité horizontale en nT), « Table 3 ». Le calculateur NOAA rend cette incertitude par point : **0,34 à 0,37 degré en France** (valeurs ci-dessous). Anomalies locales : « Local declination anomalies can exceed 10°, and 3-4° anomalies are not uncommon » (NCEI-ERR) | aucune précision chiffrée sur la page NCEI ; renvoie à l'avertissement « Model Health Warning ». À 2025-2030, l'IGRF est une prévision comme le WMM |
| **Fréquence de renouvellement** | tous les 5 ans (NCEI-META : « produced at 5-year intervals ») | tous les 5 ans (génération suivante attendue vers 2029-2030, non lue) |

Le WMM2025 et l'IGRF-14 sont des modèles du champ interne de la Terre : ils ne contiennent ni le champ crustal local ni les perturbations d'orages. Une boussole posée près d'un véhicule, d'un grillage ou sur un sol basaltique lit autre chose ; l'erreur de 0,36 degré ne couvre pas ce cas.

## Licence (le texte exact)

| Source | Texte cité |
|---|---|
| NCEI-WMM | « The WMM source code is in the public domain and not licensed or under copyright. » ; « The information and software may be used freely by the public. » ; et, pour la réutilisation de matériel du gouvernement américain par des tiers, un avis d'attribution (17 U.S.C. 403) |
| NCEI-META | « Not subject to copyright protection within the United States. » ; « WMM2025 and associated softwares are all free and publically available » ; « NOAA and NCEI cannot provide any warranty as to the accuracy, reliability, or completeness of furnished data. » Citation proposée : « NOAA NCEI Geomagnetic Modeling Team; British Geological Survey. 2024: World Magnetic Model 2025 », DOI 10.25921/aqfd-sd83 |
| NCEI-IGRF | « The software code is in the public domain and not licensed or under copyright. » |
| BGS (data.gov.uk) | champ licence : « Not set » ; le pied de page du site dit : contenu sous Open Government Licence v3.0 « sauf indication contraire » ; contrainte d'accès : le droit d'auteur sur les documents dérivés des travaux du BGS appartient à NERC, la reproduction ou le stockage demande l'autorisation préalable du titulaire ; utilisation « at the customer's own risk » |
| BGS-WMM | aucune licence énoncée sur la page ; liens « Terms of use », « Intellectual Property Rights », « Copyright » non ouverts ; pied de page « © UKRI » |

**Ce que cela dit pour Surculus.** Le **code** WMM et IGRF de NOAA est « dans le domaine public » : c'est écrit, ce n'est pas une déduction (la spec § 9 disait « licence non déclarée, domaine public par déduction » : la mention existe sur la page NCEI, pour le code ; pour les coefficients, la fiche NCEI dit « non soumis au droit d'auteur aux États-Unis » et « gratuit »). **Le côté BGS n'est pas aussi net** : la fiche BGS ne dit pas « domaine public » et réserve les droits de NERC. Le WMM étant un produit conjoint, la formule « public domain » vaut pour la distribution NCEI ; je n'ai pas trouvé de texte qui règle la distribution du fichier de coefficients à partir du BGS. **Pour un dépôt public GPL-3.0-or-later (Marculus, Groma), la prudence est de reprendre `WMM.COF` depuis NCEI, avec la citation ci-dessus, et de le dire dans `NOTICE`.** Je ne tranche pas la compatibilité juridique : à vérifier si le dépôt le demande.

## Implémentations libres en Java ou Kotlin

| Candidat | Ce qui est établi | Ce qui ne l'est pas |
|---|---|---|
| `android.hardware.GeomagneticField` (Android) | intégré, hors ligne. Résumé de recherche de la doc Android : la classe utilise WMM-2020, « valid until 2025 », et « should produce acceptable results for several years after that » | page non relue mot à mot ; le modèle réellement embarqué dépend de la version d'Android de l'appareil (minSdk 26 : appareils anciens, modèle ancien) ; aucune garantie WMM2025 |
| GeographicLib (Karney) | modèles `wmm2025` (2025-2030) et `igrf14` (1900-2030) pris en charge par la version C++ ; licence MIT, « Copyright (c) 2008-2023, Charles Karney » ; Maven Central `net.sf.geographiclib:GeographicLib-Java` 2.1, licence MIT | **le paquet Java n'a pas de classe de modèle magnétique** : 13 classes (Geodesic, GeoMath, PolygonArea, etc.), aucune pour le magnétisme (package-summary). Inutilisable tel quel pour la déclinaison |
| Orekit (`GeoMagneticFieldFactory`) | la Javadoc (versions 6.x à 9.x) montre `getWMM(year)` et un format modifié de fichier de coefficients | la version actuelle, la présence du WMM2025 dans les données d'Orekit et la licence (je crois Apache-2.0, **non vérifié** : le dépôt a répondu « Access Denied ») ; dépendance lourde pour une application Android |
| Port Java de J. St. Ledger | cité par NCEI dans sa liste de portages, révisé le 2020-04-15 (résumé de recherche) | licence et prise en charge du WMM2025 non vérifiées |
| Go (`westphae/geomag`), Rust (`kinavis-wmm`) | embarquent les coefficients WMM2025 ; kinavis-wmm refuse les dates hors 2025.0-2030.0 au lieu d'extrapoler | pas Java ni Kotlin ; licences non vues ; utiles comme second calcul de contrôle |

**Piste à instruire en ADR (proposition de l'acquéreur, pas une décision)** : un petit module Kotlin pur écrit par la mission, d'après les équations du rapport WMM et le code C de NOAA (public), avec `WMM.COF` en ressource, qui **refuse** une date hors validité (ou l'affiche et l'enregistre) au lieu de l'extrapoler en silence, et testé contre les valeurs de test de NOAA fournies avec `WMM.COF` et le tableau ci-dessous. Cela évite d'importer une dépendance sans WMM2025 (GeographicLib Java, Android) ou d'une licence non vérifiée (Orekit). Je n'ai pas lu le code C de NOAA ni mesuré l'effort du port.

## Procédure de mise à jour

1. WMM2025 expire le 2029-12-31 (NCEI) ; le successeur sort « tous les 5 ans » (NCEI-META : « produced at 5-year intervals ; update frequency as needed »). Surveiller https://ncei.noaa.gov/products/world-magnetic-model à l'automne 2029.
2. Télécharger le nouveau `WMM.COF` (les liens de téléchargement passent d'abord par une page de sondage volontaire, NCEI-WMM) et ses valeurs de test.
3. Remplacer le fichier de ressources, rejouer les valeurs de test et le tableau ci-dessous recalculé à la même date par le calculateur, mettre à jour la période de validité codée.
4. Distribuer avec l'application suivante. Une application non mise à jour après 2029 ne doit pas extrapoler en silence : après la fin de validité, la déclinaison est affichée comme « hors validité » et enregistrée avec ce drapeau (à décider par Pascal ; l'erreur croît, mais le modèle cesse d'être garanti).
5. Pour mémoire, l'IGRF-14 (1900-2030) est un repli équivalent jusqu'en 2030, avec fichier `igrf14coeffs.txt`.

## Déclinaison en France au 2026-10-15 : cas de test

Calculateur officiel NOAA (API du calculateur de déclinaison), modèle WMM-2025 (version 1.2.1 de l'API), date 2026-10-15 = année décimale 2026,7863, altitude 0 km, **déclinaison positive vers l'est, en degrés**. Comparaison : service web BGS (WMM 2025) et IGRF (modèle nommé « IGRF2025 » par l'API NOAA). Appels du 2026-10-15.

| Lieu | Latitude, longitude (degrés, ordre lat, lon) | WMM2025 NOAA | Incertitude 1 sigma (NOAA) | Variation séculaire (degré/an) | WMM2025 BGS | IGRF (NOAA) |
|---|---|---|---|---|---|---|
| Brest | 48,39 ; -4,49 | **+0,0444** | 0,3633 | +0,1706 | +0,045 | +0,0103 |
| Paris | 48,86 ; 2,35 | **+2,0863** | 0,3669 | +0,1508 | non appelé | non appelé |
| Couchey (démo sommieR) | 47,26 ; 4,99 | **+2,8004** | 0,3601 | +0,1353 | non appelé | non appelé |
| Strasbourg | 48,58 ; 7,75 | **+3,4997** | 0,3668 | +0,1323 | +3,500 | +3,4977 |
| Bordeaux | 44,84 ; -0,58 | **+1,4030** | 0,3491 | +0,1446 | non appelé | non appelé |
| Marseille | 43,30 ; 5,37 | **+2,8265** | 0,3444 | +0,1187 | non appelé | +2,8106 |

Lecture : les valeurs de test à comparer sont celles de la colonne en gras (tolérance à fixer par Pascal : le module doit retrouver le calculateur au centième de degré près, ce qui est une proposition de l'acquéreur ; l'incertitude du modèle est de 0,35 degré). Les coordonnées des villes sont des centres approximatifs choisis par l'acquéreur (pas des repères géodésiques). Les valeurs NOAA des colonnes Paris, Couchey, Strasbourg, Bordeaux, Marseille ont été relues par l'outil de lecture en résumé (le JSON brut n'a été restitué que pour Brest et pour deux cas IGRF), à reconfirmer au besoin avec l'appel API ci-dessus.

**Conséquence pour Surculus** : la déclinaison varie de 3,5 degrés d'ouest en est du pays et change de 0,12 à 0,17 degré par an. À 30 m du piquet, 0,36 degré d'erreur de modèle déplace un plant de 0,19 m (calcul de l'acquéreur : 30 m x tan 0,36 degré), avant l'erreur de lecture de la boussole.

## Écarts relevés entre sources

1. **Fin de validité du WMM2025** : NCEI « expires on December 31, 2029 » et métadonnées « 2025-01-01 à 2029-12-31 », mais BGS « valid for the period 2025.0 to 2030.0 » et le fichier `WMM.COF` s'intitule 2025-2030. La spec § 3 dit « jusqu'à fin 2029 » : cohérent avec NCEI, le plus prudent. Le calculateur NCEI accepte les dates jusqu'au 2029-12-31.
2. **Licence** : « domaine public » (NCEI, pour le code et les logiciels) contre licence « Not set » et droits NERC réservés (fiche BGS). Voir plus haut ; non résolu.
3. **Précision en France** : aucune valeur chiffrée dans les pages d'accueil NCEI et BGS ; la valeur 0,26 degré et la formule viennent d'une page NCEI-ERR lue par résumé (la carte de la page est légendée WMM2020, pas WMM2025 ; la page dit que le rapport WMM2025 devait paraître au printemps 2025). Concorde avec l'incertitude renvoyée par le calculateur (0,34 à 0,37 degré) : même source, donc pas une vérification indépendante.
4. **NOAA contre BGS** : à 0,001 degré près (Brest 0,0444 / 0,045 ; Strasbourg 3,4997 / 3,500). Même modèle, deux services : cela contrôle le calcul, pas le modèle.
5. **WMM contre IGRF** : 0,034 degré à Brest, 0,002 à Strasbourg, 0,016 à Marseille. Deux modèles faits à partir de jeux de données voisins : écart bien inférieur à l'incertitude de 0,36 degré. Un modèle ne se trompe pas pour une autre raison que l'autre, il se trompe pour la même (champ interne seul).
6. **Android intégré** : WMM-2020 d'après la documentation, donc ni WMM2025 ni IGRF-14 : ne pas s'y fier pour les cas de test (résumé de recherche, à relire).

## Ce que je n'ai pas trouvé

- Le rapport technique WMM2025 (annoncé publié le 2025-08-19 par NESDIS, non lu) et la carte d'erreur WMM2025 ; seule la formule de la page NCEI-ERR est lue.
- Les valeurs de test officielles de `WMM.COF` (le fichier n'a pas été téléchargé : les liens passent par un sondage) : à récupérer pour l'ADR.
  **Mise à jour du 2026-10-16 (mission 005)** : récupérées dans l'archive officielle https://www.ncei.noaa.gov/sites/default/files/2024-12/WMM2025COF.zip (`WMM2025_TestValues.txt`, 100 valeurs) ; le `WMM.COF` de l'archive est identique à celui de Groma (sha256 dans `NOTICE`).
- Une bibliothèque Java ou Kotlin libre dont la prise en charge du WMM2025 et la licence soient établies.
- Un texte du BGS qui règle la distribution des coefficients (voir licence).
- La page Android exacte (lue en résumé).
- Les déclinaisons au 2026-10-15 par un second modèle indépendant (le modèle EMM ou un relevé magnétique local) : non cherché, hors périmètre.
