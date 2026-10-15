# Référentiel des positions RTK du réseau Centipède

- **Propriétaire** : Pascal Obstétar
- **Date de consultation** : 2026-10-15
- **Constitué par** : agent `acquereur`
- **Décision éclairée** : le référentiel (et la réalisation) à enregistrer pour la position GNSS brute d'un plant ou d'un point de travaux, et la conversion vers Lambert-93 (EPSG:2154) avant écriture (spec § 3, ligne « Position GNSS brute » ; champ `referentiel_source` de la couche `plants`). Rien n'est tranché ici : la décision revient à Pascal (ADR « GNSS et repères »).

## Réponse courte

1. **Le réseau diffuse ses corrections dans le référentiel RGF93.** La documentation Centipède impose, pour toute base, la saisie des coordonnées **RGF93** du rapport de calcul de l'IGN, « et pas les coordonnées ITRF2014 ou ETRS89 situées au-dessus ». Un mobile RTK en solution fixe est donc positionné dans le même cadre que les bases (déduction ci-dessous).
2. **La réalisation (v1, v2, v2b) n'est écrite nulle part dans la documentation Centipède que j'ai lue.** La documentation recommande le code **EPSG:4171** (RGF93 v1, 2D) pour les relevés GNSS ; l'IGN et l'EPSG donnent RGF93 v2b comme système en vigueur depuis 2021. L'écart entre réalisations est de l'ordre du centimètre.
3. **L'écart avec WGS 84 en 2026 est d'environ 1 mètre** (0,98 m par le calcul ci-dessous ; « près de 1 m » selon l'IGN), croissant de 2,6 cm par an. Il compte pour l'écart entre un point RTK et un point autonome, pas pour deux points RTK.
4. **Pour passer en EPSG:2154, aucune transformation de datum** : les paramètres de décalage officiels de l'EPSG sont nuls (précision annoncée 1 m).

## Sources et pages exactes

| Sigle | Page | Ce qu'elle établit |
|---|---|---|
| C-SYS | https://docs.centipede.fr/docs/centipede/4_Systeme2reference.html, « Systèmes de référence » (sections « Réseau Géodésique Français 1993 (RGF93) », « Pense-bête », « EPSG France métropolitaine », « Transformation d'un point d'un système de coordonnées de référence à un autre ») | RGF93 est « la réalisation française de l'ETRS89 » ; « compatible avec le système mondial ITRS et à fortiori le système usuel WGS 84 » (pas de valeur chiffrée) ; ETRS89 « coïncide avec le système mondial ITRS à l'époque 1989.0 » et « est fixe par rapport à la partie stable de la plaque Eurasie » ; exactitude horizontale de 1 à 2 cm par rapport aux systèmes mondiaux ; **recommande EPSG:4171** pour les relevés GNSS, EPSG:2154 ou 5698 pour la cartographie ; « des coordonnées géographiques n'ont aucun sens » sans leur système géodésique |
| C-PARAM | https://docs.centipede.fr/docs/base/param_positionnement.html, « Paramétrage du positionnement dans la base » | « utiliser les coordonnées RGF93 en bas du texte, et pas les coordonnées ITRF2014 ou ETRS89 situées au-dessus » ; ordre latitude, longitude, hauteur ; l'époque et la version de RGF93 ne sont pas mentionnées |
| C-FR | https://docs.centipede.fr/docs/base/positionnement-france.html, « Positionnement France / DOM-TOM » | procédure : fichier RINEX, service en ligne de l'IGN (« calculs GNSS Réseau en ligne », stations du Réseau Géodésique Permanent), position de la base lue à la fin du rapport, section « ====== RGF93 ====== » ; exactitude estimée exigée inférieure à 10 mm en E et N, 20 mm en hauteur ; ni v1 ni v2b, ni époque |
| C-EU | https://docs.centipede.fr/docs/base/positionnement-Europe.html | hors France, la procédure passe par ITRF2020 puis ETRF2000 ; écart entre ETRF2000 et le système national dit « < 1 cm » pour le RTK |
| C-CASTER | https://docs.centipede.fr/docs/centipede/3_connect_caster.html | caster `crtk.net`, port 2101, NTRIP v1 ou v2, format RTCM3 ; points de montage NEAR (MSM7), NEAR4 (MSM4) ou base nommée ; **ne dit rien du référentiel de la position du mobile** |
| CRAIG | https://www.data.gouv.fr/datasets/bases-gnss-centipede-craig (Licence Ouverte 2.0, mise à jour affichée le 2026-10-15) | chaque base est un point **en WGS 84** (couche de localisation) ; l'attribut « coordonnées RGF93 diffusées sur le réseau Centipède » (bases actives) est le référentiel de diffusion. Les points sont en WGS 84, « pas en RGF93 » : un point de la couche CRAIG n'est donc pas la coordonnée diffusée |
| EPSG-9782 | https://epsg.io/9782 | ETRS89-FRA [RGF93 v2b], géographique 2D, époque d'ancrage 2019, axes latitude puis longitude ; « Replaces ETRS89-FRA [RGF93 v2] (CRS code 9777) from 2021-01-05 » ; le code 9784 est la variante à axes inversés (usage SIG de l'IGN) |
| EPSG-9794 | https://epsg.io/9794 | « ETRS89-FRA [RGF93 v2b] / Lambert-93 », base 9782 ; transformation par défaut vers WGS 84 : 9792 |
| EPSG-9792 | https://epsg.io/9792 | transformation de 9782 (v2b) vers WGS 84 par translations géocentriques **nulles** (X, Y, Z = 0 m), précision 1,0 m ; « Approximation at the 1m level assuming that ETRS89 is equivalent to WGS 84 » ; « for medium and low accuracy applications » |
| EPSG-2154 | https://epsg.io/2154 | « ETRS89-FRA [RGF93 v1] / Lambert-93 », chaîne PROJ `+proj=lcc +lat_0=46.5 +lon_0=3 +lat_1=49 +lat_2=44 +x_0=700000 +y_0=6600000 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs` ; transformation par défaut vers WGS 84 : 1671, même hypothèse, 1,0 m ; version 13.102 de la base EPSG, révision 2025-10-31 |
| IGN-2026 | IGN, présentation GÉOPOS, mars 2026, https://cnig.gouv.fr/IMG/pdf/ign_2026-03-26_geopos_refgeodesique_sig_v2.pdf et présentation du 2026-05-18 https://cnig.gouv.fr/IMG/pdf/reunion_publique_besoin_systeme_de_reference_europeen_260518-2.pdf | **non lus : le site répond 403 à l'outil de lecture.** Je n'en ai que le résumé d'une recherche web : dérive d'environ 2,6 cm par an, écart ITRS - ETRS89 « près de 1 m » ; RGF93 v1 = ETRF93 (1993.0), v2 = ETRF2000 (2009.0), v2b = ETRF2000 (2019.0) ; « les coordonnées en usage en France métropolitaine sont aujourd'hui exprimées en RGF93 v2b ». **À relire par Pascal ou par une session qui y accède** |
| CNIG-2020 | présentation GÉOPOS, 2020-10-14, https://cnig.gouv.fr/IMG/documents_wordpress/2021/11/1_Geopos-14octobre2020-Registres.pdf | **non lue (403)** ; résumé de recherche : transformation v1 vers v2 acceptée par l'EPSG avec paramètres nuls et incertitude de 5 cm à 1 sigma (écarts observés : 5 cm sur le RGP, 10 cm sur le RBF), aucune transformation directe v1 vers v2b publiée à cette date |
| PROJ4J | https://github.com/locationtech/proj4j | licence Apache-2.0 ; module `proj4j-epsg` sous la licence de distribution de la base EPSG (« As of 1.2.2 version, proj4-core contains no EPSG Licensed files ») ; accepte les paramètres `+towgs84` ; **grilles de décalage (`nadgrids`) : non mentionnées sur la page**, non vérifiées ; version courante non lue |

## Ce que le mobile reçoit : une déduction à vérifier

La documentation Centipède ne dit pas dans quel référentiel est exprimée la position du mobile. Je le déduis : en RTK, le mobile est positionné **par rapport à la base** à partir de la position que la base annonce ; si la base annonce ses coordonnées RGF93 (C-PARAM), le mobile en solution fixe a des coordonnées RGF93. Les phrases « RGF93 compatible WGS 84 » et le champ `GGA` NMEA, qui n'indique pas de référentiel, ne contredisent pas cela : le récepteur affiche des latitudes et longitudes sans nom de système, et un téléphone ou une bibliothèque les étiquette « WGS 84 » par défaut. **C'est donc une étiquette qui est fausse, pas un nombre.** La spec § 3 le dit déjà (« RGF93 (ETRS89) si RTK corrigé par Centipède ») ; deux précisions : (a) l'adjectif « ETRS89 » est à éviter dans le champ, car la documentation Centipède oppose explicitement les coordonnées « ETRS89 » du rapport aux coordonnées « RGF93 » à saisir ; (b) la version n'est pas établie. Cette déduction n'est pas démontrée par écrit : voir « Points au sol » dans le rapport de mission pour la vérifier.

## L'écart avec WGS 84 en 2026

- ETRS89 coïncide avec l'ITRS en 1989.0 et est fixe par rapport à la plaque Eurasie (C-SYS). WGS 84 suit l'ITRS (réalisation récente), donc s'éloigne de l'ETRS89 de la vitesse de la plaque : environ 2,6 cm par an (IGN-2026, résumé).
- Entre 1989.0 et l'époque du 2026-10-15 (2026,79), 37,8 ans x 2,6 cm = **0,98 m**. Calcul de l'acquéreur, cohérent avec « près de 1 m » (IGN-2026) et avec l'approximation « au niveau du mètre » de l'EPSG (EPSG-9792). Les composantes nord et est ne sont pas relevées.
- Un témoignage GeoRezo parle de 80 cm observés (message de liste, non vérifiable) : même ordre de grandeur.
- Les réalisations de RGF93 entre elles : de l'ordre de 1 à 5 cm (CNIG-2020, résumé), soit 20 à 100 fois moins que l'écart avec WGS 84.
- L'écart augmente de 2,6 cm chaque année : environ 1,05 m en 2027, 1,1 m en 2028. Il n'est pas figé dans WGS 84 de 2026.

**Conséquence pour Surculus** : un point RTK (RGF93) et un point du téléphone en autonome (WGS 84) pris au même endroit diffèrent d'environ 1 m dans le même sens, avant la précision du téléphone. L'essai de terrain d'automne 2026 (spec § 7, tranche 1, critère d, « 0,5 m près ») doit donc comparer deux opérateurs **de même source** (deux RTK fixes) ou accepter un biais de 1 m entre une source RTK et une source autonome.

## Conversion vers EPSG:2154 et proj4j

| Cas | Ce qu'il faut faire | Pourquoi |
|---|---|---|
| RTK fixe Centipède (RGF93) | projeter latitude et longitude directement en Lambert-93, **sans transformation de datum** | les coordonnées sont déjà RGF93 ; la projection Lambert-93 est définie sur l'ellipsoïde GRS 1980 (EPSG-2154) |
| Autonome (téléphone, WGS 84) | idem : transformation nulle | les transformations par défaut de l'EPSG de WGS 84 vers RGF93 sont des translations nulles, précision 1 m (EPSG-2154, EPSG-9792) ; l'erreur d'environ 1 m est inférieure à la précision d'un téléphone et est assumée par le choix de l'EPSG |
| Ne pas faire | appliquer une transformation temporelle ITRF vers ETRF, ou « corriger » de 1 m, à un point RTK | ce serait décaler d'un mètre un point déjà dans le bon cadre |

- **proj4j** : la chaîne PROJ de EPSG:2154 porte `+towgs84=0,0,0,0,0,0,0` (EPSG-2154) ; une conversion 4326 vers 2154 est donc une pure projection. Le même résultat est attendu pour un point étiqueté 4171 (RGF93 v1) ou 9782 (v2b). **À vérifier par un test dans la mission** (je n'ai pas exécuté proj4j) : (a) qu'EPSG:2154 est bien présent dans la version de `proj4j-epsg` retenue ; (b) qu'EPSG:9794, 9782 et 4171 ne sont pas obligatoires (je ne sais pas s'ils sont présents) ; (c) que le cas Brest et Strasbourg donne le résultat attendu de la spec (convergence des méridiens environ -5 degrés à Brest, +3 degrés à Strasbourg).
- Nom à enregistrer : le contrat de sommieR est EPSG:2154 (`docs/spec.md` § 5) : on n'y touche pas. EPSG:9794 est la même projection sur la réalisation v2b ; en pratique les deux désignent des coordonnées indiscernables au centimètre, mais ce n'est pas à Surculus de changer le code du contrat.
- **Si on veut une précision meilleure que le mètre sur une source autonome**, il faudrait une transformation avec vitesse de plaque (hors EPSG par défaut). Pas utile pour un téléphone ; non recherchée.

## Écarts relevés entre sources

1. **RGF93, mais quelle version ?** C-SYS recommande EPSG:4171 (v1, 2D). L'IGN (résumé) et l'EPSG (EPSG-9782) donnent v2b comme système en vigueur depuis le 2021-01-05. C-PARAM et C-FR disent « RGF93 » sans version. La réalisation réellement utilisée par le service de calcul de l'IGN n'est pas écrite dans ces pages. **Arbitrage à demander à l'équipe Centipède ou à lire dans l'en-tête d'un rapport de l'IGN.** Effet pour Surculus : de l'ordre du centimètre, sans conséquence au 0,5 m visé.
2. **« WGS 84 » ou « RGF93 »** : la couche de localisation du CRAIG est en WGS 84 (CRAIG), alors que la position diffusée est en RGF93 : deux étiquettes pour un même point, à un mètre près. Ne pas lire la coordonnée du CRAIG comme coordonnée diffusée.
3. **ETRS89 contre RGF93** : la spec § 3 écrit « RGF93 (ETRS89) » ; C-PARAM oppose les deux (coordonnées ETRS89 du rapport à ne pas saisir). Numériquement l'écart est de quelques centimètres. Le champ `referentiel_source` peut garder la valeur `rgf93`.
4. **Écart avec WGS 84** : chiffré « près de 1 m » (IGN-2026), « 1 m » (EPSG), 0,80 m (témoignage), 0,98 m (calcul). Concordance sur l'ordre de grandeur, pas sur le chiffre : tous partent de la même physique (la dérive de la plaque). Je n'ai pas de mesure indépendante.

## Ce que je n'ai pas trouvé

- La version de RGF93 (v1, v2, v2b) et l'époque de coordonnées dans la documentation Centipède.
- Un texte Centipède ou du CRAIG qui dise en toutes lettres dans quel référentiel est exprimée la position d'un mobile en RTK fixe.
- Les présentations de l'IGN de 2026 (403).
- Le contenu exact de `proj4j-epsg` (présence de 2154, 4171, 9782, 9794 ; grilles NTv2).
- Les composantes nord et est de l'écart WGS 84 contre RGF93 en France.
