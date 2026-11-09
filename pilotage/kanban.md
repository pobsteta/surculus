# Kanban de Surculus

> Cinq colonnes (fiche n° 8). Chaque carte porte l'Homme (qui), la Mission (quoi, quand)
> et les Moyens (comment, avec quoi), et renvoie à sa fiche dans `pilotage/missions/`.
> **Une carte qui ne bouge pas est souvent la mauvaise nouvelle à dire tout de suite.**
> Tenu à jour par l'orchestrateur à chaque fin de cycle.

## Bac à sable

_Trié et daté le 2026-11-09 (mission 105, `archiviste`). « Revérifié » = relu dans le code de `main` ce jour-là, avec
`fichier:ligne` ; « non revérifié » = gardé tel quel, à vérifier avant d'en faire une question. Les entrées qui dépendent de
Pascal ou d'un autre dépôt sont gardées. Retirées ce jour-là (preuves dans la PR de la 105) : thème Système / Clair / Sombre,
mineurs de la 074 (affichage non vu à l'écran), contrôles d'intégrité de l'export,
double appui (mineurs de la 089), renvoi de l'ADR 0003 au « codé par la 086 »._

- **Mineurs de la 084** (relecture de la #172, « Bac à sable », Pascal, 2026-11-08) : critère d'action lexical du test (`RefusDeLectureEnFrancaisTest.kt:42`, passerait sans la consigne) ; « latitude et longitude semblent inversées » pour des x/y Lambert-93 (`Refus.kt:57`, comme `FautesDuRendu.kt:32`) ; détail technique entre crochets (`GpkgRefuse`) ou parenthèses (`Resultat.kt:28`). *(Revérifié 2026-11-09 : test lexical `RefusDeLectureEnFrancaisTest.kt:42`, message `Refus.kt:57`, parenthèses `Resultat.kt:27-28`.)*
- **Deux sessions vivantes de la même placette** (relecture de la #168, « Bac à sable », Pascal, 2026-11-07) : un même plant dit en même temps peut avoir deux lignes `etat` (`SessionControle.ecrireObservation`, `existant` lu hors verrou ; vérifié) ; un besoin ou une concurrence en attente peuvent se perdre si l'autre session crée le contrôle (non vérifié). Rares depuis le verrou d'écran de la 100. *(Revérifié 2026-11-09 : `existant` lu hors verrou, `core/.../session/SessionControle.kt:531`, le verrou `VERROU_CONTROLE` n'entourant que la création, l. 442 et 552 ; la perte du besoin ou de la concurrence reste non vérifiée.)*

- **Référentiel Centipède, l. 6** (relecture de la #165) : « Rien n'est tranché ici » contredit la décision inscrite l. 63 ; à aligner par l'`acquereur`. *(Revérifié 2026-11-09 : `docs/referentiels/referentiel-centipede.md:6` et `:63`.)*
- **Mineurs de la 089** (relecture de la #162) : confirmation du guidage perdue à la rotation ; `debutSansFix` écrit pendant la composition ; vue sans fix de l'écran de contrôle sans test de rendu. *(Revérifié 2026-11-09 : `confirmation` en `remember` et non `rememberSaveable`, `EcranGuidage.kt:189` ; `debutSansFix` écrit pendant la composition, `EcranGuidage.kt:194` ; `VueSansFix` privée, `EcranControle.kt:356`, sans test de rendu dans `app/src/test/.../rendu/`. Le double appui, ancienne quatrième clause, est retiré : gardé par `RenduDoubleAppuiEcranControleTest.kt` et `RenduDoubleAppuiSansPositionTest.kt` (mission 100).)*




- **Parcelle de la placette** (relecture de la 086, « sans parcelle pour l'instant », Pascal, 2026-11-02) : `placettes`
  ne porte pas la parcelle ; à demander à sommieR (une colonne), pas à deviner par jointure. La confirmation de la 087
  s'écrit sans parcelle d'ici là.
- **Placette visée par le guidage** (relecture de la 086) : pourrait départager ou être mise en avant entre homonymes ;
  l'ADR ne retient que la position. Signalé, non recommandé.

- **Test de la 082** : `gabarit()` ne lit que `strings_tournees.xml`, d'où le doublon `carte_recepteur_service_interrompu`
  / `rtk_service_interrompu` ; le faire lire les deux fichiers (`testeur`), puis une seule ressource. La carte reconnaît
  l'interruption par son texte (`DEBUT_SERVICE_INTERROMPU`) : un indicateur dans `EtatRtk` serait plus solide. *(Revérifié 2026-11-09 : `gabarit()` lit `strings_tournees.xml` seul, `AucunMessageBrutTest.kt:162,210-211` ; doublon `strings_tournees.xml:116` / `strings.xml:10` ; texte comparé par `DEBUT_SERVICE_INTERROMPU`, `ErreurRecepteurCarte.kt:35,46`.)*

- **Libellé « Caster » dans Paramètres** (`recepteur_caster`, `recepteur_ntrip_desc`, relecture de la 082) : mot
  technique, hors de la 082. *(Revérifié 2026-11-09 : `strings_recepteur.xml:24-25`.)*

- **Après « Libérer le récepteur »** (relecture de la 082, non vérifié sur appareil) : `travail.cancel()` peut remettre
  « liaison coupée » dans l'état alors que le service est arrêté (`ServiceGnssRtk.kt:141`). *(2026-11-09 : le `travail?.cancel()` est maintenant à `ServiceGnssRtk.kt:148` et `:261` ; le défaut lui-même, non vérifié sur appareil, n'est pas revérifié.)*

- **Mineur M2 de la 076** (depuis le cycle 15) : plus aucun test ne vérifie que `echeance()` retient la plus
  proche des deux échéances (bandeau, résultat final) ; une ligne dans le premier test du bandeau. *(2026-11-09 : `echeance()` prend `minOrNull()`, `MachineEcoute.kt:215-219` ; test du « plus proche » non revérifié.)*

- **Questions de la relecture de la 081** (Pascal, « au bac à sable », 2026-10-30) : `ConnectException` (serveur qui
  refuse) dit « pas de réseau », coupure en plein téléchargement (`SocketException`, SSL) sur la phrase générique ;
  signe moins « - » ou « − » (U+2212) ; tests manquants : appel de `refusServeur` dans `source`, priorité
  d'`EchecInstallation` sur une enveloppe. *(Revérifié 2026-11-09 pour `ConnectException` : « pas de réseau », `SuiviInstallationVosk.kt:42` ; le reste non revérifié.)*

- **Message d'échec de la voix hors de vue** (capture v0.7.99, 2026-10-30) : au premier appui sur « Retélécharger »,
  rien de visible ; le message s'écrit en bas de Paramètres. À vérifier, puis le faire voir sans défiler. *(Non revérifié le 2026-11-09 ; texte formé à `SectionDicteeVocale.kt:89`.)*

_Idées, à trier au prochain `/mission`._

- **Refus encore techniques à l'écran** (relecture de la 068) : `non_observe` avec un `_`, UUID de plant
  dans plusieurs messages de `RefusPlacette`. *(Non revérifié le 2026-11-09 ; la 103 a traité la placette introuvable, pas ces messages.)*
- **Mineurs de la 052** : paramètres de rationale inutilisés dans `refusAMemoriser` (`noterRationales`) ;
  `IssueInstallation.Echec` affiche `e.message` brut (texte technique). *(Revérifié 2026-11-09 : `refusAMemoriser` ignore ses deux paramètres de rationale, `AvertissementsRecepteur.kt:52` ; `issue.cause` formaté à `SectionDicteeVocale.kt:89`.)*
- **Mineurs de la 059** : KDoc d'`ExamenOrtho` et d'`OrthoChantierMentionTest` périmées ; `FichierOrtho.examiner`
  testé par garde de source seulement. *(2026-11-09 : garde de source seule, `CarteSuitesSourcesTest.kt:59-63` ; KDoc non relues.)*
- **Mineurs de la 068** : libellé du bouton d'arrivée en Kotlin (`TextesControle.boutonArrivee`)
  et non en ressource ; indentation de `PointageControleTest` vers la l. 280. *(Revérifié 2026-11-09 : libellé en Kotlin, `ControleurPlacette.kt:450` ; indentation non revérifiée.)*
- **Mineurs de la 071** : lien de la FAQ de JNA dans `proguard-rules.pro`,
  `-dontwarn com.sun.jna.platform.**` sans effet ; anciens tests UPDATE/DELETE du journal en
  `assertFailsWith<Exception>` (relecture de la 073). *(Revérifié 2026-11-09 : `-dontwarn com.sun.jna.platform.**` à `proguard-rules.pro:32` ; `assertFailsWith<Exception>` à `JournalDurableTest.kt:70-73`, `JournalRobustesseTest.kt:142`.)*
- **Question pour Pascal (ADR 0011)** : jouer l'essai de fumée aussi sur les PR (`ci.yml`, environ
  10 min d'émulateur), pour qu'un R8 cassé bloque la fusion et pas seulement la release. *(Revérifié 2026-11-09 : `.github/workflows/ci.yml` ne contient ni émulateur ni essai de fumée ; la question reste posée.)*
- **Mot à Ramulus** : sous Robolectric (SQLite 3.44.3), `BEFORE DELETE` refuse déjà le REPLACE,
  contrairement à son brief (073) ; à envoyer si Pascal le veut.

- **Orchestrateur + `contradicteur`** : ADR 0006 (fusion, avant la tranche 4), 0010
  (voir la carte « clé de signature » au Planifié), 0012 (briques communes vers `terrain`,
  réservée, après l'essai d'automne et une table des écarts ; entrée : réponse de Marculus
  du 2026-10-17, `~/dev/briefs/traites/2026-10-23-traite-terrain-gnss-publie.md`) ;
  réexamen de l'ADR 0004 (osmdroid archivé depuis le 2024-11-20, ADR 0009) : la 034 n'y
  ajoute qu'un renvoi daté.
- **Dettes de la 009** : essai de restauration sur deux téléphones après la 012 (la
  permission `WRITE_EXTERNAL_STORAGE` est demandée par la 012-B1, la sauvegarde est
  branchée sur les saisies par la 012-A).

- **Dettes de la 011** : état « rendue » et nombre de saisies dans la liste des tournées (export, 032) ; l'essai sur
  téléphone est sur la carte « terrain avant la 012 ». *(Revérifié 2026-11-09 : la vue en colonnes est faite,
  `VueKanban.kt:55` et `:167` ; la vue en liste ne l'est pas : `EcranTournees.kt:363` écrit `tournees_saisies` avec
  `t.controles.size`, `strings_tournees.xml:59` = « %1$d contrôles prévus · ouverte » en dur, même sous le filtre
  « Rendues », `EcranTournees.kt:180`.)*
- **Référentiels** : lire le JORF du 10/10/2026 (textes 7 et 8) avant de chiffrer avec le
  barème 2024 (« en vigueur NON vérifié »).
- **Tests (relecture de la 027)** : une seule emprise France, celle de la spec § 10.9
  (domaine d'usage d'EPSG:2154) ; `FixturesBienFormeesTest` (l. 138) a sa propre boîte.
  `fixtures/README.md` (l. 79) : ajouter `remplace_autre_tournee` aux codes d'anomalie
  (`testeur`). *(Revérifié 2026-11-09 : le code est absent de `fixtures/README.md`.)*
- **Emprise France** : y min vaut 6 005 294,71 dans le code (PROJ, densify 50) et
  6 005 281,2 dans `fixtures/acceptation-export/repere-et-france.json` (epsg.io), soit
  13,5 m d'écart, sans effet sur le test de la Corse ; laquelle fait foi (Pascal) ? *(Revérifié 2026-11-09 : `PointLambert93.kt:49` = 6 005 294,71 ; `repere-et-france.json:249` = 6005281.2 ; les deux restent différents.)*
- **Carte, après la 012** : l'état d'une placette contrôlée pendant la tournée, « mission
  à part, après 012 » (Pascal, 2026-10-22 ; l'option proposait de faire primer l'état local
  sur `etat_suivi`) ; travaux et vides au bac à sable (Pascal, même jour), à dessiner quand
  l'app les saisira (la tournée de sommieR n'en porte aucun).
- **Pascal (ADR 0008)** : note de Marculus du 2026-10-24 (`~/dev/briefs/vers-surculus/`) :
  `terrain-style` v0.6.0 fait disparaître le vert, Marculus l'a annulé ; le thème actuel de
  Marculus (5,17:1) a été jugé « parfait » au soleil. L'ADR 0008 (mêmes jetons, 7:1)
  tient-elle ? Proposition de l'orchestrateur : trancher au retour de Couchey, avant la 038.
  Marculus a aussi envoyé un brief à `terrain` pour une palette qui reste verte, et ajouté
  un choix Système / Clair / Sombre (« utile peut-être pour Surculus »).
- **Missions 010 et 014** : en attente de moyens (serveur WebDAV d'essai, appareils
  désignés).
- **Pascal** : écrire à Centipède (version de RGF93) ; informer les employeurs des
  agents (finalité des horodatages et positions) ; plantation
  d'essai d'automne 2026 ; successeur ;
  coût (surfaces, prix d'un regarni, ressaisie) ; revérifier les art. 3-1.1 et 3-2 du
  fascicule 34 ; URL du guide des chênaies continentales.
- **Briefs envoyés par issue et par note dans `~/dev/briefs/vers-*/` le 2026-10-23**
  (ordre de Pascal ; désormais les deux canaux) : style commun vers
  `terrain` (pobsteta/terrain#1, `terrain-style`, à faire en premier), Marculus
  (pobsteta/marculus#2) et Groma (pobsteta/groma#57) ; temps de la main vers la méthode
  (pobsteta/methode-imperatrice#18 ; Pascal y tranche les points 3 à 5, et corrige le
  livre). La mission dans `terrain` compte dans le plafond humain (Pascal, 2026-10-23).
  Surculus migre `equipe.md` et ses fiches après la 1.0.0 du plugin.
- **Pascal (dans sommieR)** : déposer le brief
  `vers-sommier/2026-10-22-position-mesuree-du-controle.md` (le registre ne garde pas la
  position mesurée du contrôle, décision du 2026-10-22).
- **Pascal (dans sommieR)** : mettre à jour les briefs `vers-sommier` (couches
  ajoutées, données des plantations à constater, décision « hors entreprise », table de
  correspondance des codes, `montant_eur` facultatif).

## Validé

- **104** audit des gardes de rendu et de forme (`testeur`, appuyer, bilan du 2026-11-09, cause 3) : PR #177, reprise 1/3 en relecture (57 gardes, 5 muettes corrigées).

- **Restes des captures du cycle 15** (depuis le cycle 15) : 052 tâche retirée (récepteur RTK Bluetooth
  allumé et choisi dans Paramètres ; « pas maintenant », Pascal, 2026-10-30) ; puis essai ganté de
  Pascal (112 ou 96 dp au contrôle ; boutons de carte de 40 à 48 dp depuis la 078, v0.7.95). Vus le 2026-10-30 sur v0.7.88 : 059, 074 « 10,0 km », 052 erreur sur la carte,
  075 debout ; sur v0.7.90 : 052 « pas encore demandées », 074 « hors rayon » au décimètre.

- **Orchestrateur puis Pascal, release v0.7.82** (depuis le cycle 14) : installer, capturer
  l'icône du micro (072) ; tenir le micro, puis le volume bas en poche, ouvre l'écoute (071, ARM
  jamais éprouvé) ; `adb logcat -b crash` sans `ecoute-vosk` ; puis les essais de la D2 (gants,
  rotation en poche, retour des Paramètres, chargement du modèle). Téléphone non branché le
  2026-10-29 au matin.

- **Pascal, essai de la release v0.7.62** (2026-10-27) : accueil titré par la forêt, écran
  « Placettes » (P35-03 seule à faire), carte sur l'Ortho chantier de
  `couchey-demo-avec-ortho.zip`, bandeau, cinq boutons, Paramètres (thème en liste,
  récepteur, voix), en mode avion ; et les essais de la v0.7.45 jamais rapportés au journal :
  vue Kanban glissée avec des gants et carte restée après réouverture, recherche, import de
  `Download/Surculus/couchey-demo-sommier.zip` par le menu « ⋮ » (fichier déplacé de
  `Download/` dans `Download/Surculus/` le 2026-10-27, journal). Vérifié par capture de
  l'orchestrateur le 2026-10-27 : accueil, liste et carte conformes ; texte gris sur la
  plaque blanche en sombre illisible (mission 062 lancée).
- **Pascal, essai de la release v0.7.67** (2026-10-28) : 062 vérifiée par capture de
  l'orchestrateur sur la v0.7.64, 063 sur la v0.7.67, en sombre et en clair ; essai au soleil de la 063 (pastilles pâlies de la
  liste à 2,3-2,6:1 acceptées sous réserve de cet essai) ; contrôle 012-B1 de bout en bout
  à Couchey, sur le terrain (pas de position simulée), avec la tournée d'essai
  `Download/Surculus/couchey-essai-plants.zip` (non importée, données d'essai).
- **sommieR** : démo de Marsannay-la-Côte (pobsteta/sommieR#65) et ortho tuilée EPSG:3857
  (pobsteta/sommieR#66), briefs envoyés, sans réponse ; session pilotée par Pascal.

## Planifié

- **Essai en forêt** d'ici le 2026-11-15 (Pascal) : ZED-F9P avec Centipède, ouverture par le code depuis le guidage, 30 s sans fix, rendu puis import sommieR. Aucune mission d'écran d'ici là.
- **ADR 0010** : attend de Pascal l'emplacement de la copie de la clé et le détenteur des mots de passe.

_En attente sous le plafond humain (3 PR en revue)._

**Suite décidée de la 012 (2026-10-27) :** la 012-D2 (au Terminé), puis :


- **049** accueil retrouvé au retour (recherche, vue colonnes) ; ne pas réutiliser la clé
  `vue_kanban`.
- **Test de rendu des écrans** (Robolectric ou émulateur en CI) : le bandeau caché de la 055
  a échappé aux tests qui lisent les sources.
- **Photos de placette et de plant** : spec et ADR 0007 amendées, validées par Pascal
  (PR #115, porte n° 1, 2026-10-28) ; brief vers sommieR envoyé. Mission de code à découper,
  précédée des mesures sur appareil de la spec § 10, point 10 (`.nomedia`, boussole tenue
  verticale, masquage de la position, poids). Hors code, Pascal : ajouter les photos à
  l'information des salariés (ADR 0005).
- **Plants anciens dessinés** sur le plan de la placette (décision 8 de la 012-B), mission
  à part.
- **Tranche 3** : règle « date incohérente » (travaux sur une plantation plus âgée que
  l'itinéraire), avec le relevé des interventions (Pascal, PR #106) ; son test attend la date de
  plantation et l'itinéraire dans la tournée (report décidé par Pascal le 2026-10-28, PR #117).
- **Pascal, terrain** (bilan du 2026-10-21 ; la 012 a démarré sans l'attendre, décision du
  2026-10-27) : APK sur Couchey (import, ortho,
  mode avion, guidage, test des dix secondes) ; restauration de `exemple-v1.sursync` ;
  comptes de Couchey contre QGIS (029) ; **essai au soleil** de la palette, le même que celui de la 063 au Validé (ADR 0008 : badge
  du fix, texte orange, pastilles sur ortho claire, marqueur noir de la position ; le bouton
  « Retour » est devenu la flèche du bandeau), en clair et en sombre. Chaque essai au
  journal.
- **Pascal, clé de signature** : dire au journal où est la copie hors du poste et qui
  détient les mots de passe, puis l'ADR 0010.

## Terminé

- **106** liste de contrôle avant relecture (`archiviste`, deux reprises, PR #176, 3cf605a) : `pilotage/liste-de-controle.md`, six points reliés à des incidents du journal, inscrite dans `CLAUDE.md` (décision de Pascal) ; point 7 (rubrique au plan) ajouté par la 105.
- **105** docs au niveau de la v0.7.126 (`archiviste`, une reprise, PR #178, 054ac67) : README et spec avec version couverte, ADR 0003 corrigé par ajout daté, `equipe.md` de la 082 à la 106, bac à sable trié (5 retraits prouvés, le reste daté).

- **103** placette introuvable en français, sans UUID (`testeur` puis `dev`, une reprise, PR #174, 843667d, v0.7.126) : « cette placette n'est pas dans la tournée : revenir en arrière et choisir une autre placette » ; au guidage sans position, « …la contrôler sans guidage, ou demander qu'elle soit positionnée dans sommieR » ; UUID au journal Android. Preuve : rendus `placette-introuvable-*` de la CI, vus par Pascal le 2026-11-09.

- **084** refus de lecture d'une tournée en français (`testeur` puis `dev`, une reprise, PR #172, a0d6f66, v0.7.124) : repère, couche absente, point hors de France, chacun avec la consigne « demander à celui qui l'a préparée de l'exporter de nouveau depuis sommieR » ; détail technique au journal Android seul. Capturé le 2026-11-09 sur le téléphone : tournée en EPSG:4326 refusée « (Lambert-93 attendu) ».

- **102** libellés des codes lisibles à « Terminer » (`testeur` puis `afficheur`, tenir, plan d'abord, PR #170, 418a625, v0.7.122) : rectangles à coins de 12 dp, labelSmall seul (« Pas de 10 sp »), mots entiers, « CL » seul debout ; garde de forme sur l'image rendue (une reprise). Capturé le 2026-11-08 debout et couché sur P35-01 ; DG « Dégagement / (manuel… » et RN « Travaux en… » gardés ainsi par Pascal.

- **100** double appui sur « Ouvrir sans position » (`testeur` puis `dev`, une reprise, PR #167, f9c9924, v0.7.119) et **101** création du contrôle du jour atomique (`testeur` puis `dev` opus, plan, recadrée, PR #168, 4cdff95). Double appui essayé par Pascal sur la v0.7.120 le 2026-11-08 : un seul contrôle.

- **089** ouvrir sans position depuis le guidage (`testeur` puis `dev` opus, tenir, plan tranché par Pascal, PR #162, dc62557) : v0.7.113 installée le 2026-11-07 ; localisation coupée capturée ; fix perdu : bloc et « Ouvrir sans position » vus par Pascal. Reste : ouverture par le code depuis le guidage, en forêt.
- **097** hôtes Centipède sourcés (`acquereur`, #163), **098** commentaire de `CASTERS_CENTIPEDE` (`dev`, #164), **099** décision au référentiel (`acquereur`, #165) : `caster.centipede.fr` gardé par Pascal.

- **094** référentiel de la source (`testeur` puis `dev`, PR #159, une reprise, aa02f2c), **095** « Terminer » refusé
  tant qu'un plant n'a pas d'état (`testeur` puis `dev`, PR #158, 351c427) : release v0.7.111 installée le 2026-11-07 ;
  P35-01 de la Colombière : « Terminer » grisé avec « Plant n° 2 sans état : dire son état ou « non observé ». »
  (capturé), puis rendu avec `referentiel_source` = `WGS84`. Reste à Pascal : essai au ZED-F9P (RTK fixe → RGF93,
  NTRIP coupé → null, DGPS → null).
- **096** quatre tests muets de `SessionControleTest` lancés (`testeur` seul, PR #160, 39df25e) : 43 cas sur 43, aucun
  défaut révélé.

- **091** besoin, concurrence et qualité du fix (`testeur` puis `dev`, tenir, PR #156, deux reprises, 7e82a8a) et
  **092** référentiel des concurrences (`acquereur`, #157, 957ced7) : fusionnées ; release v0.7.109 installée le
  2026-11-05 (relancée une fois : émulateur de la CI non démarré). Écran « Terminer » de P35-03 capturé : besoin
  « Aucun » et 15 codes, concurrence Faible, Moyenne, Forte, « Terminer » grisé avec « Choisir le besoin et la
  concurrence pour terminer. » ; quitté par « Reprendre » sans rien écrire. Reste : l'essai de Pascal sur P35-01
  (RG, Forte, rendu, import sommieR) et l'essai ganté des boutons de 64 dp.

- **090** rendre la tournée depuis le téléphone : fusionnée (#155, 67ff17a), release v0.7.107 ; bouton et
  confirmation vus le 2026-11-03. Reste : un vrai rendu, avec la 091.

- **087** ouvrir un contrôle sans position et **088** `.sursync` v2 : fusionnées ensemble (#153 et #154 dans la
  branche 087, puis #152 dans `main`, 7c8f969), release v0.7.106 installée le 2026-11-03 ; essais : refus de la v2 par
  la 0.7.102, restauration sur la 0.7.106, « Activez la localisation… », ouverture sans position vue par Pascal
  (micro-ondes). Reste : la mesure des 30 s à l'essai en forêt.

- **086** code de piquet en double entre UG (`testeur` puis `dev`, PR #151, 709c7bd, un aller-retour) : fusionnée le
  2026-11-02 ; **capture à faire** (écran « P01 : quelle UG ? », tournée à deux « P01 »).

- **082** plus aucun message d'exception brut à l'écran (`testeur` puis `dev`, PR #146, a04eb64, deux allers-retours) :
  fusionnée le 2026-11-02 ; **captures à faire** (import d'un fichier vide refusé, récepteur coupé).

- **083** le bandeau d'écoute ne cache plus « Plant précédent » (`testeur` puis `afficheur`, PR #148, c081f8a, deux
  allers-retours) : fusionnée le 2026-11-02 ; preuve #147 rouge (18 tests) fermée sans fusion ; spec amendée par la
  PR #149 ; **captures debout et couché après une vraie phrase à faire** (téléphone non branché le 2026-11-02).

- **080** test de rendu des écrans (`afficheur`, tenir, PR #142, 76731ab, trois allers-retours) : fusionnée le
  2026-10-30 ; rouge vérifié sur 076, 078 et 060 (PR de preuve #143 à #145 fermées sans fusion) ; aucun
  changement de rendu, donc pas de capture ; défaut connu 083 marqué dans le test.

- **081** aucun texte technique à l'écran (`testeur` puis `dev`, PR #140, deux allers-retours) : fusionnée le
  2026-10-30 ; capturées sur v0.7.99 le 2026-10-30 (code inconnu, « pas de réseau » en mode avion).

- **079** README et spec au niveau du livré (`archiviste`, PR #141, bce9fd2, deux allers-retours) : fusionnée le
  2026-10-30 ; le test de la minute sera refait au prochain bilan.

- **077** « cliquez sur le bouton « Rattacher » » (`dev`, PR #136, 9d01de8) et **078** six boutons de carte
  en une colonne, de 48 à 32 dp (`afficheur`, PR #137, f780287, deux allers-retours) : capturées sur v0.7.95
  le 2026-10-30.

- Cycle 15, 2026-10-29 et 30 : **059** Ortho chantier sourcée et datée (PR #128, 9e5e6d4) ; **075** bouton
  micro comme Marculus (PR #130, 7ca1bf8, trois relectures) ; **052** suites du récepteur et de la voix
  (PR #129, ba6f497, trois relectures) ; **074** une seule façon d'écrire une distance (PR #131) ; brief
  vers sommieR (pobsteta/sommieR#68) — fusionnées sur la décision de Pascal ; captures à faire.

- Cycle 14, 2026-10-29 : **071** la dictée ne ferme plus la release, règles R8 de JNA et de Vosk,
  essai de fumée vu rouge puis vert (PR #125, 079919b, v0.7.82) ; **072** icône du micro hors
  poche (PR #123, 0106da6) ; **073** le journal refuse `INSERT OR REPLACE`, brief de Ramulus
  (PR #124, b73c493) ; **068** message d'arrivée en français, sans motif ni décimales, bouton
  « Rattacher » / « Recommencer » selon le champ (PR #126, 093edf3, deux relectures) — fusionnées
  sur la décision de Pascal.

- **012-D2** le micro, tenir pour parler (PR #121, afcdbd1, cycle 13) : essais au téléphone à faire
  (bouton tenu avec des gants, rotation en poche, retour des Paramètres, chargement du modèle).

- **069** thème pris de `terrain-style` (bb2b519, tag v0.7.1), plus de copie (PR #122, 07aba21) ;
  **070** citation de Purfürst corrigée et vérifiée dans l'ADR 0003 (PR #120, dcaa1da) ; briefs
  Marculus et Ramulus traités (cycle 13).

- **067** bande du mode poche toujours visible, plan couché sous les boutons (PR #118, 8a7d379,
  cycle 13) ; captures à faire sur la release.

- **066** mineurs et maintenance (PR #117, 7b2c46d, cycle 13) : KDoc de `Navigation.kt`, renvoi
  terrain#23, doc d'`ExporterScenario`, test `dernier_controle` renommé ; release v0.7.58 déjà
  publiée (vérifié le 2026-10-28).

- **012-C1** mode poche et gestes de volume (PR #113, b9cf9d6) : 9 essais bons sur la v0.7.73
  (Pascal, 2026-10-28). **012-C2** la voix et la vibration (PR #114, fee5b57), v0.7.74 installée,
  voix entendue par Pascal ; cycle 12. **Captures** 064, 012-B2, 012-C1 faites (cycle 12).

- **012-D1** le cœur de la dictée (`ApplicateurDictee`, `TextesDictee`, `:core`), PR #116
  fusionnée le 2026-10-28 (fbc2f93), sur l'autorisation de Pascal.

- Cycle 11, 2026-10-27 et 28 : **062** textes neutres sans plaque en sombre (PR #105) ;
  **012-A** session de contrôle sur le journal de la tournée (PR #106) ; **063** couleurs
  d'état sans plaque blanche, spec § 3 et ADR 0008 amendées (PR #107) ; **012-B1** écran du
  contrôle (deux portes, arrivée, états, causes, Terminer) (PR #108) ; **064** carte en
  paysage (PR #110) ; **012-B2** pointage à la boussole du téléphone, plan, regarnis, spec
  § 1 et ADR 0003 amendées (PR #109) ; **065** jour civil sur l'heure du fix GNSS (PR #112),
  brief vers Groma (pobsteta/groma#68) ; **012-C1** saisie au volume en mode poche (PR #113,
  trois relectures) — fusionnées sur l'autorisation de Pascal ; releases
  jusqu'à v0.7.68 ; amendement « photos » de la spec et de l'ADR 0007 (PR #115, porte n° 1).

- Cycle 10, 2026-10-24 et 27 : **038** thème vert de Marculus (PR #81) ; **040** accueil sur
  la liste des tournées (PR #82) ; **041** recherche et « Rendues » (PR #83) ; **043** vue
  Kanban (PR #84) ; **044** « À propos » et « Plein écran » (PR #86) ; **045** toutes les
  licences (PR #88) ; **042** styles et tailles de Marculus (PR #89) ; **046** écran
  Paramètres (PR #90) ; **047** récepteur GNSS dans Paramètres (PR #91) ; **048** voix Vosk,
  téléchargement et fichier (PR #92) ; **053** bandeau de Marculus (PR #93) ; **050** sans
  case « Vue Kanban » (PR #94) ; **051** choix du thème (PR #95) ; **054** ouvertures dans le
  menu « ⋮ » (PR #96) ; **055** fonds de carte de Marculus en ligne (PR #97) ; **056** thème
  en liste (PR #98) ; **057** suites de la carte (PR #101) ; **058** anciennes tuiles effacées
  (PR #100) ; **060** bandeau de la carte visible (PR #102) ; **061** forêt, placettes à
  faire, puis carte, comme Groma (PR #103) ; spec et ADR 0009 amendées (PR #99) ; ADR 0008
  amendée (PR #81, #95) ; fiche 038 réécrite (PR #80) ; briefs vers sommieR (#85, #99) et
  Groma (pobsteta/groma#67, tailles de texte) ; pilotage (#87) — fusionnées sur l'ordre ou
  l'autorisation de Pascal ; release v0.7.62.

- Cycle 9, 2026-10-23 et 24 : **037** DI-303 suit la décision 13 (PR #73) ; **013** dictée
  Vosk à grammaire fermée, `terrain-voix` épinglé (PR #75) ; **039** cas de « hauteur »
  (PR #78) ; ADR 0008 amendée, fiche 038 (PR #74) ; décisions (PR #76) ; brief vers
  `terrain`, pobsteta/terrain#23 (PR #77) — Pascal.

- Cycle 8, 2026-10-22 et 23 : **028** registre attendu (F4) et sabotages (F5) dans la CI
  d'import (PR #64) ; **036** cas des décisions 11 et 12 de la dictée (PR #63) ; **033**
  placettes colorées selon `etat_suivi`, légende avec décomptes (PR #65) ; brief et spec
  amendés, style commun et liste du départ (PR #68) ; **ADR 0008** style commun acceptée,
  briefs vers `terrain`, Marculus et Groma (PR #69) ; pilotage (PR #62, #66, #67) — Pascal.

- Cycle 7, 2026-10-22 : **034** trous de mémoire du bilan, README, spec, index des ADR
  (PR #58) ; **035** test de forme des CSV de `docs/referentiels/`, vu rouge 9 sur 9
  (PR #59) ; pilotage (PR #60) — Pascal.

- Cycle 6, 2026-10-22 : **027** registre attendu du scénario Couchey (PR #55) ; **032** état
  par tournée, tournée obligatoire (PR #56) ; pilotage et bilan (PR #53, #54) ; outillage
  `ANDROID_HOME` (PR #57) — Pascal.

- Cycle 5 (suite), 2026-10-21 : **011** écrans Tournées, Carte et Guidage (PR #51) ;
  **030** référentiels de la PR #4 revérifiés (PR #52) ; pilotage (PR #50) — Pascal.

- Cycle 5, 2026-10-21 : **009** sauvegarde `.sursync` et restauration (PR #45) ;
  **029** géométries de `foret`, `ug`, `parcelles` (PR #47) ; **026** critères F1, F2, F3
  en CI d'import (PR #46) ; **031** tournée dans chaque ligne du journal (PR #49) ;
  pilotage (PR #44, #48) — Pascal.

- Cycle 4, 2026-10-21 : **022** critère (a) mesuré en CI, deux échecs montrés (PR #39) ;
  **024** cas de la sauvegarde `.sursync` (PR #40) ; **025** cas de la dictée et
  vocabulaire Vosk (PR #41) ; ADR 0009 carte hors ligne, fiches 026 à 028 (PR #42) — Pascal.

- Cycle 3, 2026-10-17 : **006** module GeoPackage (PR #34), **007** CI import sommieR
  (PR #33), **008** journal en ajout seul (PR #35), **019** et **021** scénarios du journal
  (PR #31, #35), **020** cas de l'export (PR #36), arbitrages (PR #37) — Pascal.

- 2026-10-20 : **023** la méthode passe au plugin `imperatrice` v0.1.0 (GPL-3.0, CI et
  releases automatiques, pobsteta/methode-imperatrice#1), le pilotage dans `pilotage/`,
  README centré sur l'application (PR #38) — Pascal.

- Cycle 2, 2026-10-16 : **003** GNSS de Groma, fraîcheur du fix, lint dans la commande de
  tests (PR #24) ; **005** chaîne des repères, WMM2025 copié de Groma (PR #25) ; **018**
  écran d'essai GNSS (PR #27) ; **004** domaine du contrôle de placette (PR #28) ; clé de
  signature et premières releases avec APK (v0.4.1, v0.5.0) — Pascal.

- Cycle 1, 2026-10-16 : **001** squelette Gradle et CI (PR #16), **002** fixtures sourcées
  et tests d'acceptation (PR #19), **015** icône « S » (PR #18), **016** badges et
  releases automatiques (PR #20, v0.1.0 sans APK) ; hook garde_main (PR #17) — Pascal.

- ADR 0002 à 0005 et 0007, commande de tests, référentiels essences, WMM, Centipède
  (PR #11, #12) — Pascal, 2026-10-15.
- Porte n° 1 franchie le 2026-10-15 : brief (PR #5), références des sources (PR #6),
  ADR 0001 (PR #7), référentiels de plantation (PR #8), spec v1 (PR #9) — Pascal.

## Abandonné

_Avec la raison, en une ligne._

- 2026-11-06 : **093** rendre les points hors forêt : abandonnée sans PR ; Pascal garde le refus (`PointHorsForet`) et
  fait essayer sur une tournée à la Colombière (Dijon).

- 2026-10-21 : PR #4 (ébauche des référentiels, 2026-10-15) fermée : antérieure à la porte
  n° 1, codes travaux contraires à la décision du 2026-10-15 ; ses cinq documents utiles
  passent à la mission 030.

## Livraisons

_Une ligne par cycle, ajoutée sous la précédente à la fin de chaque `/boucle`, et une
par `/bilan` : la livraison que Pascal a retenue, pour qui, et ce qu'elle apporte de plus
que la précédente du même rythme. Un cycle sans livraison neuve livre sa mauvaise
nouvelle, car
« une mauvaise nouvelle est une livraison sur laquelle on apprend » (Pascal) : on
l'inscrit avec ce qu'elle nous apprend. Rubrique ouverte le 2026-10-22 (plugin
imperatrice 0.3.0) : les cycles 1 à 8 ne sont pas inscrits rétroactivement._

```
- cycle N · livraison · pour qui · ce qu'elle apporte de plus que la précédente
- cycle N · mauvaise nouvelle : … · pour qui · ce qu'elle nous apprend
- bilan du AAAA-MM-JJ · livraison · pour qui · ce qu'elle apporte de plus que la précédente
- bilan du AAAA-MM-JJ · mauvaise nouvelle : … · pour qui · ce qu'elle nous apprend
```

- cycle 8 · la carte colore les placettes selon `etat_suivi` (jamais, à jour, travaux à
  prévoir), avec une légende qui les compte (033) · pour le forestier en forêt · avant, toutes
  les placettes avaient la même couleur : il voit d'un coup d'œil celles à contrôler
  d'abord. *(Retenue par Pascal le 2026-10-23.)*
- cycle 9 · mauvaise nouvelle : le moteur de dictée (013) et ses 167 cas sont prêts, mais
  le forestier ne peut pas encore dicter en forêt, aucun écran ne l'appelle avant la 012 · pour
  le forestier en forêt · découper une fonction sans son écran ne livre rien sur le terrain.
  *(Proposée par l'orchestrateur, close par Pascal le 2026-10-24 : « fusionne et clos ».)*
- cycle 10 · Surculus s'ouvre comme Groma et ressemble à Marculus : la forêt, ses placettes à
  faire, puis une carte avec les fonds de Marculus, l'Ortho chantier par défaut et les
  boutons de Groma ; Paramètres, thème et voix comme Marculus · pour le forestier en forêt ·
  avant, la carte s'ouvrait seule, sans choix de fond ni de placette ; il retrouve ses gestes
  de Groma et de Marculus. *(Retenue par Pascal le 2026-10-27 : « 1 oui ».)*
- cycle 12 · le contrôle téléphone en poche : l'agent dit l'état de chaque plant au volume, gants
  aux mains, entend le numéro et l'état et sent la vibration · pour le forestier en forêt · avant, il
  regardait l'écran à chaque plant (v0.7.74). *(Retenue par Pascal le 2026-10-28.)*
- cycle 13 · le mode poche qui dit comment en sortir : la bande « 3 s sur volume haut pour sortir »
  reste lisible quel que soit le défilement, et couché, le plan ne cache plus les boutons d'état ·
  pour le forestier en forêt · avant, la bande pouvait être coupée sous la barre sans moyen de
  remonter. *(Retenue par Pascal le 2026-10-28 ; livrée à la fusion et à la capture de la 067.)*
- cycle 14 · la dictée qui marche sur la release : le forestier tient le micro (reconnu à son icône)
  ou le volume bas en poche et dit l'état du plant · pour le forestier en forêt · avant, la v0.7.80
  se fermait au premier appui. *(Retenue par Pascal le 2026-10-29 ; livrée à la capture au téléphone
  de la release qui porte la 071, sinon le cycle livre sa mauvaise nouvelle. Livrée le 2026-10-29 :
  v0.7.83, micro tenu et volume bas en poche, « compris : Numéro 7, sain. », sans plantage.)*
- cycle 15 · le contrôle d'une main gantée : micro rond toujours à portée, rouge à l'écoute, sans saut
  d'écran ; distances écrites pareil partout (« 10,0 km », « 7,9 m ») ; l'ortho dit sa source, la carte dit
  quand le récepteur lâche · pour le forestier en forêt · avant, il fallait défiler pour trouver le micro et
  l'écran sautait sous le doigt. *(Retenue par Pascal le 2026-10-30 ; livrée à la capture de la release qui
  porte 052, 059, 074 et 075, sinon le cycle livre sa mauvaise nouvelle. Livrée en partie le 2026-10-30 :
  v0.7.88, debout tenu ; couché, « ÉCOUTE FERMÉE » reste après une dictée et cache « Mode poche » :
  mauvaise nouvelle, reprise 076 fusionnée le 2026-10-30 (#132) ; livrée debout et couché sur v0.7.90, « Mode poche » visible après une dictée.)*
- bilan du 2026-10-30 · le forestier contrôle une placette du piquet au dernier plant sans regarder l'écran (voix,
  volume, micro, gants aux mains) et retrouve ses boutons de carte au même endroit (v0.7.95) · pour le forestier en
  forêt · au bilan du 2026-10-21, la 012 n'existait pas : il ne pouvait pas contrôler une placette. Mauvaise nouvelle
  jointe : rien n'est vérifié en forêt ; le signe concret du brief (10 placettes en moins de 10 min, tournée
  réimportée dans sommieR) n'est pas mesuré ; ce qu'elle nous apprend : une livraison vue au téléphone n'est pas une
  livraison vue au terrain. *(Retenue par Pascal le 2026-10-30 : « Oui, avec la mauvaise nouvelle ».)*
- cycle 17 · la tournée rendue depuis le téléphone revient dans sommieR (P35-03 : besoin DG → PL, import sommieR 0.31.1
  le 2026-11-06), avec le besoin, la concurrence, la qualité du fix et son référentiel (`referentiel_source`), et sans
  plant oublié à « Terminer » (v0.7.111) · pour le forestier et pour sommieR · avant, la tournée ne revenait pas dans
  sommieR. Mauvaise nouvelle jointe : un point hors forêt empêche toujours le rendu, et rien n'est vérifié en forêt
  (essai à la maison, Colombière) ; ce qu'elle nous apprend : l'aller-retour est prouvé, pas encore au terrain.
  *(Retenue par Pascal le 2026-11-07 : « Oui, avec la mauvaise nouvelle ».)*
- cycle 18 · sous couvert, sans fix, l'agent ouvre sa placette depuis le guidage avec le code lu, sans revenir à la carte
  (089, v0.7.113) ; chaque nom de caster qui fait marquer une position RGF93 a sa source (097 à 099) ; les quatre
  tests muets de `SessionControleTest` tournent (096) · pour le forestier en forêt et pour la justesse des données ·
  avant, le guidage restait bloqué tant qu'aucun fix n'arrivait. Mauvaise nouvelle jointe : l'ouverture complète
  depuis le guidage (code, confirmation, contrôle « Sans position ») n'est vérifiée ni au téléphone ni en forêt ; ce
  qu'elle nous apprend : le micro-ondes montre le bloc et le bouton, pas le geste entier ; l'essai qui compte est sous
  couvert. *(Retenue par Pascal le 2026-11-07 : « Oui, avec la mauvaise nouvelle ».)*
- cycle 19 · un double appui ganté sur « Ouvrir sans position » n'ouvre plus qu'un contrôle (100, v0.7.119), et « un
  contrôle par placette et par jour » tient dans les données, même en cas de course (101) · pour le forestier et pour
  la justesse des données rendues à sommieR · avant, deux appuis pouvaient ouvrir deux contrôles. Mauvaise nouvelle
  jointe : rien n'est vérifié au téléphone ; l'orchestrateur a deux fois relayé un constat d'agent sans le vérifier (fix
  figé, « ligne en trop »), et la 101 a été lancée sur un fait faux puis recadrée ; ce qu'elle nous apprend : un
  chiffre de test se lit dans le code avant d'être traduit en défaut. *(Retenue par Pascal le 2026-11-07 : « Oui, avec
  la mauvaise nouvelle ».)*
- cycle 20 · à « Terminer », les libellés des 15 codes sont en mots entiers, sans rien de rogné par le bord (102,
  v0.7.122, capturé debout et couché sur P35-01 ; « CL » seul debout) ; le double appui n'ouvre qu'un contrôle (100
  et 101, essayé par Pascal sur la v0.7.120) · pour le forestier qui choisit le besoin, gants aux mains · avant, les
  libellés étaient coupés au milieu d'un mot et rognés par le bord rond. Mauvaise nouvelle jointe : l'`afficheur` a
  contourné la garde des styles, la garde de forme du `testeur` ne gardait rien (constante comparée à elle-même), et
  la consigne de l'orchestrateur pour l'essai du double appui était fausse (couper la localisation) ; ce qu'elle nous
  apprend : une garde n'est prouvée que vue rouge. *(Retenue par Pascal le 2026-11-08 : « Oui, avec la mauvaise
  nouvelle ».)*
- cycle 21 · quand une tournée est refusée à l'import (repère autre que Lambert-93, couche manquante, position hors de
  France), le message est en français et dit quoi faire : demander une nouvelle exportation depuis sommieR (084,
  v0.7.124, capturé le 2026-11-09) · pour le forestier qui ouvre une tournée sans réseau · avant, des codes EPSG, des
  `srs_id` et des coordonnées brutes à l'écran. *(Retenue par Pascal le 2026-11-09 : « Oui, telle quelle ».)*
- cycle 22 · quand la placette demandée manque à la tournée ou n'a pas de position, le guidage et le contrôle disent en
  français quoi faire (revenir en arrière et choisir une autre placette, ou la contrôler sans guidage, ou demander
  qu'elle soit positionnée dans sommieR), sans l'identifiant technique (103, v0.7.126, rendus de la CI vus le
  2026-11-09) · pour le forestier en forêt · avant, « Carte impossible : placette 3f2a9c1e-… ». Mauvaise nouvelle
  jointe : la preuve promise par la fiche (rendus de la CI) n'existait pas au premier passage, et l'orchestrateur a
  d'abord installé la release sur l'émulateur sans vérifier la cible ; ce qu'elle nous apprend : une preuve annoncée se
  vérifie dans l'artefact avant la relecture. *(Retenue par Pascal le 2026-11-09 : « Oui, avec la mauvaise nouvelle ».)*
- bilan du 2026-11-09 · depuis le 30 octobre, la boucle est fermée : le forestier rend sa tournée depuis le téléphone
  et sommieR la réimporte (aller-retour prouvé le 2026-11-06 sur une copie de base) ; il peut ouvrir un contrôle sans
  position sous couvert, n'ouvre qu'un contrôle par placette et par jour, et lit en français les libellés et les refus
  (v0.7.100 → v0.7.126) · pour le forestier et pour sommieR · de plus que le bilan du 30 octobre (contrôle sans regarder
  l'écran) : les données reviennent dans sommieR. Mauvaise nouvelle jointe : rien n'a été vérifié en forêt, le ZED-F9P
  attend depuis le 16 octobre, et 51 % des échecs viennent d'affirmations non vérifiées à la source (7 de
  l'orchestrateur) ; ce qu'elle nous apprend : le terrain d'abord, d'ici le 2026-11-15, et vérifier à la source avant
  d'écrire. *(Retenue par Pascal le 2026-11-09 : « Oui, avec la mauvaise nouvelle » ; « v0.7.104 » de la question
  corrigé en v0.7.100.)*
