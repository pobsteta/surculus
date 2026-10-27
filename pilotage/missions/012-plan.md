# Mission 012 — Plan de l'afficheur (rendu avant tout code, le 2026-10-27)

> Temps de la main : **appuyer**. Aucun code n'est écrit à ce stade. Sources lues : fiche 012
> (reprise du 2026-10-27 comprise), `CLAUDE.md`, spec § 4.2, 4.3, 4.5, § 5 (journal), § 6, ADR 0004,
> code de `main` à 3c01f4b. Les `fichier:ligne` sont ceux de cette branche.

## 1. Redite

Réaliser l'écran qui permet au forestier de contrôler une placette (pointer les plants autour du
piquet, dire leur état aux boutons de volume ou à la voix, voir les totaux et les refus avec leurs
codes), ouvert depuis la carte ou à l'arrivée du guidage, en écrivant chaque saisie au journal de
la tournée, pour qu'il contrôle sans regarder l'écran et que la liste des placettes sache ce qui
est fait.

## 2. Question de l'écran, pour qui (fiche d'affichage)

* **Question** : « où en suis-je sur cette placette, et quel plant vient ensuite ? »
* **Pour** : le forestier debout dans la placette, gants, une main libre, parfois téléphone en poche.
* **Une seule chose en grand** : le numéro du plant courant et son état (sain / atteint / mort /
  absent), lisibles à bout de bras.
* **Comparaison** : plants dits / plants pointés (« 7 sur 12 »), état courant / état précédent.
* **Source et date en petit** : tournée, contrôle du jour, dernière sauvegarde.
* **1664** : une idée par vue, six lignes, six mots par ligne, quatre couleurs.

## 3. Inventaire de l'existant (ce qui sert la 012)

### Domaine du contrôle (mission 004), `core/.../core/placette/`
* Rattachement 15 m, code du piquet : `Controle.kt:43-66` (`Rattachement.rattacher`, `DISTANCE_MAX_M`).
* Un contrôle par placette et par jour, jour civil Paris : `Controle.kt:68-94` (`JourCivil`, `UnControleParJour.ecrire`, `ControleEcrit`).
* France et emprise : `Controle.kt:107-137` (`EmpriseEtFrance`).
* Pointage et alerte hors rayon + 0,10 m : `Plant.kt:131-160` (`ControleRayon`, `Pointage.pointer`, `confirmeHorsRayon`).
* Identité, regarnis : `Plant.kt:51-129` (`Identite.verifier`, `Regarnis.verifier`, `remplacesAvant`).
* Numérotation de Groma, provisoire puis figée : `Numerotation.kt:48-118`.
* États, causes, détail, hauteur : `Observation.kt:15-143` (`EtatDsf`, `GroupeCause`, `Observations.valider`, `NonObserve`).
* Totaux : `Totaux.kt:43` (`Totaux.deduire`).
* Refus domaine : `Verdict.kt:75-193` (`RefusPlacette`, 25 cas, chacun avec un `motif`), alerte `AlertePlacette.HorsRayon` `Verdict.kt:59`.
* Repères (distance horizontale, azimut, déclinaison, convergence) : `core/reperes/Reperes.kt:104` (`orienter`), `:141-170` (`PlantDerive.deriver`).

### Journal (008, 031, 032), `data/.../data/journal/`
* `JournalRepository.saisir(objetUuid, type, valeur, operateur, remplaceUuid, fix, codeTournee)` : `db/JournalRepository.kt:65` ; refus d'un code vide (`tourneeObligatoire`) : `:73` ; `annuler(uuidCible, operateur)` : `:114`.
* Lecture par tournée : `lignes(codeTournee)` `:134`, `projection(codeTournee)` `:140`, `etat(objet, type, codeTournee = "")` `:144` (le défaut `""` est le piège que la fiche demande de prouver par test).
* Types de ligne : `LigneJournal.kt:4` (`TypeLigne`) ; projection et `EtatCle` : `Projection.kt:7-60`.
* Instance de l'application, `tourneeObligatoire = true` : `app/.../sauvegarde/JournalApp.kt:30-37`. Identifiant d'appareil opaque `:18`.
* **Aucun format de `valeur` n'est écrit en code** pour `controle`, `pointage`, `etat` (la spec § 5 le décrit en une phrase par type, sans clés JSON). Voir question Q1.

### Dictée (013), `core/.../core/dictee/` et `app/.../dictee/`
* `AnalyseurDictee.analyser` / `.appliquer(phrase, etatCourant)` : `AnalyseurDictee.kt:27`, `:44-74` (refus contextuels, redite, correction).
* Commandes et résultat (`Refus(motif)`, `Accepte(correction, ligneAjoutee, confirmationVocale, mesuresConservees)`) : `Commande.kt:25-65`.
* Port moteur : `MoteurDictee.kt:13-37` ; moteur Vosk : `app/.../dictee/MoteurDicteeVosk.kt:27-57` (`demarrer`, `arreter`).
* Permission `RECORD_AUDIO` déclarée : `AndroidManifest.xml:17` ; elle n'est **demandée à l'exécution nulle part** (seul le commentaire `MoteurDicteeVosk.kt:25` la met à la charge de l'appelant, donc de la 012).
* `DicteeRejeuTest` est actif (aucun `@Disabled` dans `core/src/test/.../dictee/`).

### Modèle Vosk (048), `core/.../core/dictee/InstallateurModele.kt`
* `InstallateurModele` `:41` (`installer` `:106`, `reparer` `:66`, `supprimer` `:89`) ; vérification SHA-256 `VerificationModele.kt`.
* Interface de Paramètres : `app/.../parametres/SectionDicteeVocale.kt` ; import par fichier : `dictee/ModeleVoskImport.kt`. La 012 ne les touche pas : elle lit « modèle installé ou non » et renvoie vers Paramètres → Dictée vocale.

### Navigation et carte (061), `app/.../`
* `Navigation.kt:30-73` (`EtatNavigation` : `carte`, `centreId`, `placetteId` = guidage, `guider()` `:50`, `retour()` `:53`, sérialisation `:62-72`). **Aucune destination « contrôle »** : il faut un champ (par ex. `controle: Boolean`) et son retour (contrôle → guidage ou carte).
* Aiguillage des écrans : `MainActivity.kt:98-109` (le commentaire `:44` annonce « le contrôle viendra après le guidage »).
* Carte : placette choisie `EcranCarte.kt:308-309` ; bouton « naviguer vers » `:459-465` (le bouton « Contrôler » se place à côté) ; bas de carte `:524-536`.
* Guidage : arrivée `EcranGuidage.kt:139-143` (`DetecteurArrivee`, signal sonore seul, aucune ouverture), moyenne de position puis rattachement 15 m ou code lu `:145-162` (état `Confirmation`) ; cercle de 30 m `:253`.
* Seuils : `Guidage.kt:24` (`SEUIL_ARRIVEE_M = 30`), `:27` (réarmement 40 m), `:63-76` (`DetecteurArrivee`), `:78-87` (`MoyennePosition`).
* Récepteur : `MainActivity.kt:85-89` (`PilotageRecepteur`, tourne tant que carte ou guidage est ouvert, `Navigation.kt:38`) ; pour que le contrôle garde le GNSS, `recepteurDemande` doit couvrir la nouvelle destination.
* Bandeau : `ui/BandeauCompact.kt` (053) ; thème : `ui/theme/` (051) ; palette : `carte/Palette.kt`.

### Liste des placettes et `placettesControlees`
* `DepotTournees.placettesControlees(ctx, id): Set<String> = emptySet()` : `tournee/DepotTournees.kt:82` (commentaire `:76-81` : « à brancher avec la 012 »).
* Usage : `EcranPlacettes.kt:86-87,103-104,152` (ordre « à faire » puis « autres » ; statut `CONTROLEE`). `cibleNavigation` de la carte lit aussi `controlees` (`EcranCarte.kt:462`).
* Hypothèse « `code_tournee` = nom du dossier » : `DepotTournees.kt:65-73` (`saisies`) et `JournalApp.depot`. La 012 s'en sert : `LigneTournee.id` est le nom du dossier (`DepotTournees.kt:123-135`). Le journal n'impose rien d'autre (la clé de projection est (`code_tournee`, `objet_uuid`, `type`), `Projection.kt:7`).
* Compteur du Kanban (043) : `compterSaisies(depot.projection(id))`, donc il verra les saisies de la 012 sans changement.
* Lecture de la tournée : `LecteurTournee.kt:189-226` lit déjà `plants` et `etats_plants` de sommieR (plants des passages précédents).
* Opérateur : `tournee.projet.operateur` (`DepotTournees.kt:45`, `RechercheTournees.kt:19`) est la source naturelle de `operateur`.

### Sauvegarde (009), `data/.../data/sauvegarde/`
* Règle de déclenchement (N = 20 saisies, M = 5 min monotones, fins de placette) : `Declenchement.kt:12-44` (`RegleDeclenchement`, `SuiviDeclenchement`, événements `SAISIE`, `FIN_PLACETTE`).
* Service : `Sauvegarde.kt:28-52` (`ServiceSauvegarde.sauvegarder(codeTournee, maintenantUtcMs)`), stockage `StockageMediaStore.kt:37`.
* **Rien ne les relie encore dans l'application** : `ServiceSauvegarde` et `SuiviDeclenchement` ne sont instanciés nulle part dans `app/src/main` (seul `EcranRestauration` lit ce stockage). Il faudra créer l'instance (version sommieR : `versionSommier` d'où ? Q6).

### Export et codes de refus
* Refus de l'export : `gpkg/.../Refus.kt:19-134` (`RefusGpkg`, `motif` stables : `hors_referentiel`, `uuid_en_double`, `hors_france`, `controle_en_double`, `totaux_discordants`, `jour_incoherent`, `uuid_invalide`, …). `RefusGpkg.Identite` `:91-95` convertit déjà un `RefusPlacette` en motif (`PlacetteChangee` devient `uuid_en_double`).
* Correspondance `RefusPlacette` vers code de la spec § 6 : aujourd'hui seulement dans `gpkg/src/test/.../acceptation/BranchementExport.kt` (test) ; la 012 la passe en production (décision de Pascal du 2026-10-17).
* `RefusPlacette` a déjà un `motif` et un `message` (`Verdict.kt:75`) ; il faudra vérifier que ces `motif` sont ceux de la spec § 6 ou ajouter la table.

### Absent du code (à créer)
* Gestion des touches du volume (Marculus `MainActivity.kt:75-120` : `onKeyDown`, `startTracking`, `onKeyLongPress`, `onKeyUp`, objet `ToucheVolume`) : rien dans Surculus.
* Synthèse vocale, vibration, écran maintenu allumé (`keepScreenOn`), mode poche : rien (`grep` de `TextToSpeech`, `VIBRATE`, `keepScreenOn` : 0 résultat).
* Tests d'écran : l'app n'a que des tests JVM (`:app:testFumeeUnitTest`) ; pas de Robolectric dans `app`. La logique doit donc vivre en Kotlin pur, testable sans Compose.

## 4. Verdict : la mission est trop grosse pour une seule PR

La phrase de mission porte **huit** idées (écran, pointage, états et causes, regarnis et totaux,
refus avec codes, volume et poche, voix, journal et sauvegarde, deux entrées). Un relecteur et
Pascal ne peuvent pas relire cela d'un bloc, et la porte n° 3 s'applique « une mission = une PR ».
**Proposition de découpage en quatre missions**, chacune une phrase (l'ordre est celui des
dépendances ; c'est Pascal qui tranche) :

| N° proposé | Une phrase | Ce que ça livre seul |
|---|---|---|
| **012-A** | Écrire en Kotlin pur la session de contrôle d'une placette (saisir, corriger, annuler, regarnir, totaux, refus avec leurs codes de la spec § 6) sur le journal de la tournée, et brancher `placettesControlees` et le déclenchement de la sauvegarde. | Aucun écran. La liste des placettes sait ce qui est fait ; la table des boutons est testée ; la correspondance des refus passe en production. |
| **012-B** | Réaliser l'écran du contrôle (plan des plants autour du piquet, pointage relatif, états, causes, regarnis, totaux, refus lisibles), ouvert par « Contrôler » sur la carte et à l'arrivée du guidage. | Contrôle complet **au toucher**, utilisable à l'écran. |
| **012-C** | Faire saisir le contrôle sans regarder l'écran : boutons de volume selon la table de la spec, mode poche qui ignore le tactile, écran allumé, vibration et synthèse vocale du numéro et de l'état. | Le geste du brief, sans dictée. |
| **012-D** | Brancher la dictée sur le plant courant (permission `RECORD_AUDIO`, refus dits à la voix, modèle manquant renvoyant vers Paramètres). | Le périmètre élargi du 2026-10-24. |

Si Pascal préfère moins de PR : **A+B** puis **C+D** (deux PR), mais pas moins : A+B+C+D donnerait
une PR de plus de 2 000 lignes que personne ne relit.

## 5. Plan détaillé en étapes livrables, avec les tests

Chaque étape = un commit livrable, tests verts (`.claude/test-command`), **vu rouge au moins une
fois** sur la règle la plus dangereuse. Les fichiers de `fixtures/` ne sont jamais modifiés.

### Étape 1 (012-A) — La session de contrôle et le journal
Contenu : une classe Kotlin pure `SessionControle` (dans `:core` ou `:data`) qui prend un horloge,
un dépôt de journal (port), la tournée et la placette, et expose : `pointer`, `dire(etat, cause, detail, hauteur)`,
`nonObserve`, `regarni(remplace)`, `annuler`, `totaux`, `plantCourant`, `suivant`. Le format de
`valeur` JSON des lignes `controle`, `pointage`, `etat` est écrit en code et documenté (Q1).
Tests :
* table des gestes (spec § 4.3) : volume haut court, bas court, haut long, bas long, double appui bas, **en placette**, comme fonction pure (geste, contexte) vers action ;
* **double appui court sur volume bas annule la dernière saisie**, et une saisie annulée ne rend pas la placette « Contrôlée » (`placettesControlees` lit la projection `projection(code)`, `annulée = 0`, comme `compterSaisies`) ;
* **lecture avec le code de la tournée ouverte** : un test prouve qu'un oubli du code lirait `""` et rendrait un autre état (relecture 032) ;
* **date incohérente** (`CLAUDE.md`) : plantation plus âgée que l'itinéraire prévu, jour civil du fix GNSS (âge ≤ 5 s) sinon heure du téléphone ; `jour_incoherent` ;
* cas `fixtures/regles/plants/` (`etats-causes`, `identite`, `numerotation`, `regarni`, `totaux-controle`) et `fixtures/regles/un-controle-par-jour.json`, `rattachement-15m.json`, `emprise-et-france.json` : rejoués par `RejeuReglesPlacetteTest` (déjà là) **via** la session ;
* `fixtures/export/scenario-couchey.json` : la session rejoue le scénario, l'export (`ExportAcceptationTest`) le relit sans écart avec `registre-attendu-couchey.json` ;
* **placette de Somalie** : un test vérifie qu'une placette française tombe en France ; un fix hors de France ne pointe rien (`Recentrage`, `Guidage.versLambert93`) ;
* **codes de refus** : pour chaque `RefusPlacette`, le code de la spec § 6 ; `hors_referentiel`, `uuid_en_double`, `hors_france` au minimum ; la table passe de `BranchementExport.kt` au code de production, le test d'acceptation l'appelle depuis la production ;
* `placettesControlees` : placette avec un état courant, placette annulée, placette d'une autre tournée (n'influe pas), lignes non rattachées `""`.
* Sauvegarde : `SuiviDeclenchement` appelé à chaque saisie ; `fixtures/sauvegarde/declenchement.json` ; première saisie (20 / 5 min / fin de placette).

### Étape 2 (012-B) — L'écran et le pointage
Contenu : `EcranControle` (Compose), état en Kotlin pur ; `BandeauCompact` avec flèche de retour ;
styles de texte de Marculus (aucun `fontSize`) ; thème Système / Clair / Sombre ; textes neutres
sans plaque (062) ; cibles d'au moins 48 dp ; plan des plants autour du piquet (dessin, comme le
`Radar` du guidage) ; saisie de la distance et de l'azimut (Q3) ; boutons d'état ; cause en
quatre groupes ; totaux ; refus en clair **avec leur code** ; arrivée avec moyenne de position et
confirmation d'identité, **réutilisés** de `EcranGuidage.kt:145-162`, pas recopiés.
Entrées : `Navigation.kt` gagne `controle` (+ sérialisation, retour vers guidage ou carte) ;
bouton « Contrôler » sur la carte quand une placette est choisie (`EcranCarte.kt:459-465`) ;
ouverture automatique à l'arrivée du guidage (`EcranGuidage.kt:139-143`, moins de 30 m) avec
retour possible au guidage et **sans réouverture en boucle** (réarmement à 40 m, déjà dans
`DetecteurArrivee`).
Tests : `NavigationTest` (ouvrir, retour, rotation, récepteur GNSS gardé) ; table de l'état de
l'écran (plant courant, « 7 sur 12 ») ; un test par refus montré ; pointage hors rayon : alerte
et confirmation, pas un refus ; capture d'écran du téléphone sur la release (règle du 2026-10-27).

### Étape 3 (012-C) — Volume, poche, écran allumé, vibration, voix de sortie
Contenu : `dispatchKeyEvent` / `onKeyDown`, `onKeyLongPress`, `onKeyUp` de l'activité (modèle :
Marculus `MainActivity.kt:75-120`, lecture seule), reconnaissance des appuis court, long et
double dans un classeur en Kotlin pur (horloge injectée) ; mode poche (tactile ignoré, sortie
par un geste à fixer, Q4) ; `FLAG_KEEP_SCREEN_ON` tant que le contrôle est ouvert ; vibration
(un motif par état) ; `TextToSpeech` en français (« numéro 7, sain »).
Tests : classeur de gestes (court, long, double, deux touches ensemble ignorées, rebond),
**mode poche ignore l'écran tactile** (fonction pure « le toucher est-il pris en compte ? »),
annonce vocale pour chaque état (texte exact), cas sans voix française installée (message à
l'écran, jamais de silence). L'essai écran éteint reste à la 014 (ni fait ni promis ici).

### Étape 4 (012-D) — La dictée dans l'écran
Contenu : demande de `RECORD_AUDIO` à l'exécution, refus expliqué à l'écran et à la voix ;
`MoteurDicteeVosk` démarré par « volume bas, appui long » ; `AnalyseurDictee.appliquer` sur le
plant courant ; **chaque refus dit à la voix**, dont « hauteur » sans état (« l'état d'abord ») ;
modèle manquant : l'écran le dit et renvoie vers Paramètres → Dictée vocale.
Tests : `fixtures/dictee/` (cas DI-…) rejoués à travers l'application des commandes à la session
(redite sans ligne, correction avec `remplace_uuid`, « non observé » sans mesure) ; DI-303 reste
celui de la 048 ; refus de permission ; modèle absent ; commande « vide » (Q7).

## 6. Questions de sens et de périmètre que la spec ne tranche pas

À Pascal ; je n'en tranche aucune.

* **Q1. Format de `valeur` des lignes `controle`, `pointage`, `etat`.** La spec § 5 donne le
  contenu en mots (« placette, jour civil, opérateur », etc.), pas les clés JSON, et aucune mission
  ne l'écrit. Qui le décide et où (un fichier de `docs/formats/` à côté de `sursync-v1.md`) ?
  Proposition : la 012-A l'écrit en code et en note de format, à valider à la porte n° 3. Question
  liée : **qui transforme le journal en `Saisies` pour l'export** (§ 4.8, mission 010) ? Elle devra
  relire ce format.
* **Q2. Quand une placette est-elle « Contrôlée » ?** (a) dès la première saisie d'un plant (la
  ligne `controle` naît alors), (b) dès l'ouverture de l'écran, (c) à un bouton « Terminer ».
  Proposition : (a), conforme à « une saisie annulée ne rend pas la placette Contrôlée ».
  Dépendances : « Terminer » est aussi ce qui **fige les numéros** (§ 6) et déclenche
  `FIN_PLACETTE` de la sauvegarde ; faut-il ce bouton ? Et le lendemain : une placette contrôlée
  hier est-elle « à faire » aujourd'hui (un contrôle par jour, spec § 4.3) ?
* **Q3. Comment l'agent entre la distance et l'azimut d'un plant ?** Clavier numérique à l'écran
  (gants), boussole du téléphone, ou lecture d'une boussole externe ? La spec dit « distance
  horizontale et azimut magnétique bruts » sans dire le geste. En mode poche, le pointage est
  impossible sans écran : le premier passage est donc **forcément à l'écran** ; est-ce voulu ?
  Pente et unités (degrés ou grades) viennent de l'ouverture de la tournée (ADR 0003).
* **Q4. Mode poche : comment y entrer et en sortir ?** La spec dit qu'il « ignore l'écran
  tactile », pas comment le quitter. Proposition : bouton d'entrée en grand, sortie par appui long
  de 2 s sur volume haut **ou** verrouillage téléphone. À décider (risque : agent enfermé).
* **Q5. Portée de « volume haut, appui long = atteint (la cause se dicte ensuite) »** quand la
  dictée (012-D) n'est pas encore livrée : la cause se saisit à l'écran (hors poche). Acceptable
  pour la 012-C ?
* **Q6. Sauvegarde : version de sommieR de l'en-tête** (`ServiceSauvegarde.versionSommier`,
  `Sauvegarde.kt:31`) : d'où la lire (couche `projet` du `travaux.gpkg`) ? Et la sauvegarde
  automatique sur les saisies se déclenche-t-elle en tâche de fond, ou sur un bouton ? (§ 4.8
  parle d'un déclenchement automatique ; l'écriture MediaStore peut être lente.)
* **Q7. « vide » et « fin du vide »** (§ 4.5, tranche 2) sont dans la grammaire de la dictée.
  Dans la 012-D : refus dit à la voix (« vide pas encore disponible ») ou ignorés ? Proposition :
  refus dit, rien n'est tracé.
* **Q8. Regarni** : « remplace » (voix) désigne le plant courant mort ou absent comme remplacé ;
  le regarni est ensuite pointé comme un nouveau plant. Est-ce bien le geste voulu (le plant
  suivant devient « le regarni de n° X », pointé distance et azimut), plutôt que de choisir le
  plant remplacé dans une liste ?
* **Q9. Passages suivants** : les plants et états de sommieR (`LecteurTournee.kt:189-226`) sont
  la base ; la saisie du jour est ajoutée par le journal. La lecture « état précédent » à côté de
  l'état du jour (comparaison de l'affichage) est-elle voulue à l'écran, ou trop chargée (règle
  du 1664) ?
* **Q10. Opérateur** : `operateur` de chaque ligne = `projet.operateur` de la tournée. Confirmer
  (pas de compte, confidentialité ADR 0005).
* **Q11. Hors du périmètre de la 012**, à ne pas faire sans décision : dictée « vide » ; essai
  écran éteint (014) ; cheminement (§ 4.4, colonne « en cheminement » de la table des boutons) ;
  alertes de reprise (§ 4.5) ; photos.

## 7. Test des dix secondes (pour l'étape 2, à faire par Pascal)

* **Qui** : Pascal, quelqu'un qui n'a jamais vu l'écran, sur le téléphone, en plein soleil,
  une main gantée.
* **Ce qu'il doit avoir compris après dix secondes** : le numéro du plant dont on parle, son état
  (ou « pas encore dit »), et combien de plants restent (« 7 sur 12 »).
* **Question de contrôle** : comprend-on la situation en dix secondes ?

## 8. Pièges visés (fiche n° 7)

Quarante indicateurs : non (six lignes, six mots). Image qui accuse : aucun classement
d'agent, un refus dit la règle sans reproche. Image figée sans date : tournée, jour du
contrôle, dernière sauvegarde en petit. Axe tronqué : le plan est à l'échelle (rayon de la
placette en mètres), pas d'échelle décorative.
