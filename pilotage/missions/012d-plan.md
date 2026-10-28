# Mission 012-D — Plan de l'afficheur (rendu avant tout code, le 2026-10-28)

> Temps de la main : **appuyer**. Aucun code, aucun test, ni `docs/`, ni `fixtures/`, ni le journal. Sources lues :
> fiche 012-D, `CLAUDE.md`, spec § 4.3, ADR 0004, fiches 012, 012-C, 012-C2, 013, 048 ; `AnalyseurDictee.kt`,
> `Commande.kt`, `MoteurDictee.kt`, `Grammaire.kt`, `MoteurDicteeVosk.kt`, `ModeleVoskImport.kt`, `SectionDicteeVocale.kt`,
> `SessionControle` (`dire`, `preciser`, `remplace`, `suivant`, `annuler`, `vide`, `appliquer`), `Gestes.kt`,
> `ClasseurGestes.kt`, `ModePoche.kt` (`PontPoche`, `PocheApp`), `ControleurPlacette.kt`, `EcranControle.kt`,
> `MainActivity.kt`, `AndroidManifest.xml`, `fixtures/dictee/contexte.json` ; sur la branche `mission/012c2-voix-vibration`
> (PR #114, non fusionnée) : `VoixPoche.kt`, `VoixAndroid.kt`, le diff de `EcranControle`, `MainActivity`, `ControleurPlacette` ;
> Marculus en lecture seule : `voice/PttController.kt`, `ui/feuille/DicteeVocale.kt` (permission, anti-larsen),
> `ui/feuille/SessionMartelage.kt` (file de `TextToSpeech`, `onDone`), `terrain` `NoyauPtt`. Références : `main` à 5bfb1c5.
> Groma : aucune dictée utile trouvée (recherche de « vosk » et « dict » dans ses `.kt`).
> **Écart de fiche** : elle cite `pilotage/missions/012-plan.md` (§ 4 et § 5, étape 4) ; ce fichier n'existe pas dans le dépôt
> (ni sur `main`, ni sur les branches 012 distantes). Je me suis appuyé sur la fiche 012 (« Périmètre élargi le 2026-10-24 »).

## 1. Redite

Brancher le micro de Vosk sur le plant courant de l'écran du contrôle : un appui long sur volume bas en mode poche (ou un
bouton « Micro » à l'écran hors du mode) ouvre l'écoute, chaque phrase de la grammaire fermée est confrontée à l'état du plant
puis écrite par la session, et la voix dit ce qui a été compris ou pourquoi c'est refusé, pour que le forestier, gants aux mains
et téléphone en poche, dise la cause, la hauteur ou une correction sans regarder l'écran.

## 2. Ce que l'écran montre (fiche d'affichage)

* **Question** : « le téléphone m'écoute-t-il, et qu'a-t-il compris ? » Pour le forestier debout dans la placette, une main
  libre ou aucune, sans réseau.
* **Une seule chose en grand** : l'état de l'écoute, en deux mots, dans la bande du mode poche (ou en haut de la vue Plant hors
  du mode) : **« J'ÉCOUTE »** / **« ÉCOUTE FERMÉE »**. Rien d'autre ne grossit.
* **Comparaison** : *entendu / compris*, sur deux lignes : `entendu : « insectes »` puis `compris : n° 7, insectes`
  (ou, en couleur d'erreur, `refusé : une cause demande un plant atteint ou mort`). Le forestier compare ce qu'il a dit à ce que
  la machine a retenu : c'est la seule vérification possible sans relire le plan.
* **Source et date** : la `SessionControle` seule (code de la placette et jour restent en petit, comme aujourd'hui). La voix répète
  ce que le journal a écrit, pas ce que Vosk a entendu : si la ligne n'est pas écrite, la voix dit « refusé » ou « déjà ».
* **Lieu** : voix et vibration d'abord, écran seulement pour vérifier ; vent, gants, pas de réseau (Vosk et `TextToSpeech` du
  téléphone sont locaux).
* **1664** : la bande garde ses lignes (titre, aide, ligne voix de la C2, annonce) ; l'écoute ajoute **une ligne grande** et **une
  ligne petite** `entendu / compris`. Aucune couleur nouvelle : `onSurface` et la couleur d'erreur de la 063.
* **Pièges écartés** : pas de courbe de volume du micro, pas de liste des phrases possibles à l'écran (la grammaire est dans la
  spec), pas de score de confiance, pas de compteur de refus (l'image qui accuse). Pas d'écran de dictée séparé.

## 3. Ouvrir et fermer l'écoute

### 3.1 Le geste (mode poche)

La table existe : `Geste.VOLUME_BAS_LONG` → `ActionGeste.OuvrirDictee` → `SuiteSession.DicteeDemandee` ; le classeur le rend **à
0,5 s, pendant l'appui** (pas au relâchement ; le relâchement qui suit est ignoré, `ClasseurGestes.relache`, `longEmis`). La
012-D branche l'écoute sur ce geste, sans toucher aux seuils ni au classeur (fiche 012-C, décision 9).

Conséquence à dire : le classeur **ne signale pas le relâchement** du volume bas après un long. Un « push-to-talk » comme celui
de Marculus (on tient, on parle, on lâche) demanderait un nouveau signal du classeur : c'est une modification de la 012-C1, non
demandée. Le plan propose donc une écoute **qui se ferme d'elle-même ou par un nouvel appui long** (question 2).

### 3.2 Hors du mode poche

Le volume y règle le son (décision 5) : aucun geste de volume. Un **bouton « Micro »** sur la vue Plant, à côté de « Mode poche »,
même hauteur (`DimensionsControle.ACTION`, 56 dp), même forme ; il **bascule** : un appui ouvre, un appui ferme. Même moteur,
mêmes phrases, mêmes refus, mêmes annonces. En mode poche le tactile est ignoré : le bouton n'est pas touchable, le geste seul
ouvre. La voix et la bande parlent aussi hors du mode poche pendant l'écoute (question 3).

### 3.3 Ce que l'agent entend et sent

| Instant | Vibration | Voix | Écran |
|---|---|---|---|
| Appui long reconnu (0,5 s) | aucune encore | si une annonce parle : coupée net (`stop()`) | « J'ÉCOUTE… » (ouverture) |
| Micro réellement ouvert (`demarrer` revenu sans erreur) | **un coup de 200 ms** (nouveau motif OUVERTE) | rien (la parole est pour l'agent) | « J'ÉCOUTE » |
| Phrase comprise et écrite | motif de l'état (C2) | l'annonce du § 4 | `entendu / compris` |
| Phrase refusée | motif REFUS (C2) | le refus du § 5 | `entendu / refusé : …` en erreur |
| Fin de l'écoute | **deux coups de 200 ms** (nouveau motif FERMEE) | « écoute terminée. » | « ÉCOUTE FERMÉE » |

Les deux motifs nouveaux doivent rester **distincts des neuf de la C2** et de moins d'une seconde (le test de la C2 s'étend aux
onze). Valeurs proposées : OUVERTE `0, 200` ; FERMEE `0, 200, 150, 200`. Elles ne ressemblent à aucune des neuf (SAIN 60,
MORT 400, TIC 30, BOURDON 600, ATTEINT 60-80-60, ENTREE 100-100-100, REFUS 40 fois trois, ANNULE 60-80-400, DEJA_TERMINEE
400-100-60).

**Pourquoi la vibration vient après l'ouverture réelle et non avant** : Marculus joue un bip *avant* d'ouvrir le micro (« sans lui
l'opérateur parle avant que le micro soit ouvert », `PttController.kt`). Ici `MoteurDicteeVosk.demarrer` est **synchrone** : il
charge le modèle (premier appel) puis démarre l'enregistrement ; s'il revient sans appeler `surErreur`, le micro est ouvert. Le
signal qui suit est donc fidèle. Le temps de chargement du modèle (41 Mo) n'est **pas mesuré** : à mesurer sur le téléphone de
Pascal (question 11) ; pendant ce temps l'écran dit « J'ÉCOUTE… » sans vibrer, et la voix dit « un instant » si le chargement dépasse
2 s (seuil à confirmer à la mesure). `demarrer` se lance **hors du fil principal**, jamais dans le geste tactile.

**Risque connu, à essayer** : le vibreur peut être entendu du micro. Mitigation proposée : le premier résultat reçu dans la seconde
qui suit la vibration d'ouverture, s'il vaut seulement `[unk]` ou est vide, est ignoré sans refus. À confirmer au téléphone.

### 3.4 Fermer (question 2)

Proposition : l'écoute **reste ouverte pour enchaîner** (« atteint », puis « insectes », puis « hauteur quarante-cinq »,
comme dans le critère de la fiche) et se ferme quand :

1. **un nouvel appui long sur volume bas** (bascule : le même geste ouvre et ferme) ;
2. **10 secondes sans aucune phrase** (comprise ou refusée) ;
3. le mode poche se lève, ou `onPause`/`onStop` (appel, verrouillage, autre application) : le micro est libéré dans tous les
   cas, **sans réouverture automatique** au retour ;
4. une erreur du moteur (micro pris par une autre application, modèle illisible) ;
5. le bouton « Micro » hors du mode poche.

Pas de fermeture sur « suivant » : l'agent continue sur le plant d'après. Les gestes de volume restent actifs pendant l'écoute
(sain, mort, atteint, annule) ; le moteur est suspendu pendant leur annonce comme pour une phrase (§ 8).

## 4. L'application des commandes : `AnalyseurDictee` vers `SessionControle`

### 4.1 Le chaînon manquant

`SessionControle.appliquer` ne prend aujourd'hui qu'un `Geste`. Il n'existe aucune fonction qui prenne une **phrase** : la 012-D
en ajoute **une, pure, dans `:core`** (`ApplicateurDictee`, package `session`, sans Android) :

```
appliquer(phrase: String, session: SessionControle) -> IssueDictee
```

1. lit l'état du plant courant (`session.etat()` : `duJour` vers `EtatDicte`, `null` si pas d'état) ;
2. appelle `AnalyseurDictee.appliquer(phrase, etatCourant)` — **inchangé** (013) ;
3. selon la commande acceptée, appelle **une** méthode de la session (tableau ci-dessous) ;
4. rend une `IssueDictee` : ce qui a été écrit (ou non), le numéro dit, le numéro d'après, et la **clé de l'annonce** ; la voix
   et l'écran mettent les mots (fonction pure `TextesDictee`, dans `:core` pour être testée sans Android).

Elle ne contient aucune règle métier : les règles restent dans `AnalyseurDictee` (contexte) et `Observations.valider` (domaine).
Le texte du refus de l'analyseur est un libellé libre (`refus.json`) ; **la voix ne le lit pas tel quel** : elle dit les
textes du § 5, choisis d'après la commande et l'état.

### 4.2 Commande par commande

| Phrase | Résultat de l'analyseur | Appel de session | Ligne écrite ? | Avance ? |
|---|---|---|---|---|
| `sain` `atteint` `mort` `absent` sur plant **sans état** | `Accepte(Etat)` | `session.dire(code)` | oui, `etat` | non (question 4) |
| même mot que l'état déjà dit (**redite**) | `Accepte(ligneAjoutee=false, confirmationVocale=true)` | **aucun** | **non** ; mesures conservées | non |
| autre état sur plant qui en a un (**correction**) | `Accepte(correction=true, annonceVocale=true)` | `session.dire(code)` : la session pose `remplace_uuid` de la ligne précédente | oui, nouvelle ligne `etat` qui remplace | non |
| `non observé` (sans état, ou **correction**) | idem ; `mesuresConservees=false` après correction | `session.nonObserve()` (aucune mesure, spec § 5) | oui (sauf redite) | non |
| `insectes` `champignon` `animaux` `climat` `indéterminé` | `Accepte(Cause)` | `session.preciser(causeGroupe=…)` | oui, ligne `etat` complétée (remplace la précédente) | non (question 4) |
| `mangé` | `Accepte(Detail)` | `session.preciser(causeDetail="abroutissement")` | oui | non |
| `hauteur quarante-cinq` | `Accepte(Hauteur 45)` | `session.preciser(hauteurCm=45)` | oui | non |
| `suivant` | `Accepte(Suivant)` | `session.suivant()` | **non** (le courant change, c'est tout) | **oui** |
| `remplace` | `Accepte(Regarni)` | `session.remplace()` | **non** (le regarni s'écrit au pointage) | non |
| `annule` | `Accepte(Annule)` | `session.annuler()` | une ligne annulée (journal en ajout seul) | le courant se recale (`annuler`) |
| `vide`, `fin du vide` | `Accepte(VideDebut/VideFin)` | `session.vide()`, `session.finDuVide()` | **non** (rien n'est tracé, tranche 2) | non |

Points à dire, un à un :

* **Redite sans ligne.** L'analyseur dit « aucune ligne » mais c'est `ApplicateurDictee` qui doit **s'abstenir d'appeler**
  `dire` : `SessionControle.dire` ne compare pas à l'état existant et écrirait un doublon (`ecrireObservation`). Un test le verrouille
  (journal inchangé après une redite).
* **Correction avec `remplace_uuid`.** C'est déjà ce que fait la session : `ecrireObservation` cherche la ligne `etat` du plant dans
  ce contrôle et passe son `uuid` en `remplace`. Aucun code de journal nouveau. La **voix annonce** la correction (décisions 10 et 12).
* **Les mesures lors d'une correction (question 5).** `dire(code)` ne passe ni cause, ni détail, ni hauteur : une correction de
  « atteint » en « mort » **efface** la cause et la hauteur déjà dites. L'analyseur laisse `mesuresConservees = null`
  (« non déterminé par la spec ») hors « non observé » ; le choix est à Pascal.
* **« hauteur » sur un plant mort** : acceptée (décision 2) ; la session l'écrit (`Observations.refus` ne la refuse que sur absent).
  Test de non-régression : DI-171, DI-172, DI-174.
* **« remplace »** pose un regarni « en attente » ; **le pointage se fait à l'écran** (boussole, distance, essence : la 012-C l'a dit,
  pas en poche). En poche, la voix dit « numéro 7 sera remplacé. Pointez le regarni à l'écran. » et rien d'autre n'est possible
  tant que l'agent n'est pas sorti du mode (question 8).
* **« annule »** réutilise `session.annuler()` : même effet que le double appui bas. La voix ne dit que « annulé. » : la session ne
  dit pas quel plant est remis (même limite que la C2). Ne pas inventer un numéro.
* **« vide »** : la session répond `PasEncoreDisponible` ; aucune ligne.
* **Placette déjà terminée.** La saisie reste permise après « Terminer » avec alerte à chaque geste (fiche 012-C, décisions 3 et 6).
  La dictée fait **de même** : elle écrit, et la voix dit d'abord « placette déjà terminée. » puis l'annonce de la commande (question 9).
  La fiche range ce cas parmi les refus ; je le traite en alerte pour rester cohérent avec les gestes, c'est une question de sens.

### 4.3 Ce qui est identique aux gestes

Une phrase et un geste de volume passent par **le même verrou** que celui de l'écran (`Mutex` de `VueSession`) : ils arrivent dans
l'ordre, un par un. Le plant « courant » est toujours celui de la session ; la dictée ne choisit jamais un plant.

## 5. Les refus : le texte exact dit à la voix

Règle : **chaque refus se dit** (jamais de silence) ; la phrase la plus courte qui dit quoi faire ; jamais le code technique à la
voix (il reste à l'écran, comme pour les gestes : `TextesControle.message`). Un refus fait vibrer REFUS (C2) et ne change rien au
journal.

| Cas | Ce que la machine sait | Texte dit (voix) | Texte à l'écran (`refusé : …`) |
|---|---|---|---|
| Mot non reconnu, phrase hors grammaire, deux commandes (« atteint insectes »), `[unk]` | `Refus("hors grammaire")` | « Pas compris. » | « Pas compris. Une phrase, une commande. » |
| `hauteur` sans état | état courant `null`, commande `Hauteur` | « Hauteur refusée. Dites d'abord l'état. » | « Hauteur refusée : dites d'abord l'état du plant. » |
| `hauteur` sur plant absent ou non observé | état `absent` / `non_observe` | « Pas de hauteur sur un plant absent. » / « … non observé. » | idem en toutes lettres |
| `hauteur` hors de 1 à 400 (« hauteur zéro », « hauteur cinq cents », « hauteur » seul) | `Refus("hauteur : nombre de 1 à 400 attendu")` | « Hauteur de 1 à 400 centimètres. » | idem |
| `vide`, `fin du vide` | `PasEncoreDisponible` | « Vide : pas encore disponible. » | « « vide » : pas encore disponible. » (texte existant, `TextesControle.message`) |
| cause sur un plant sain | `Refus("une cause demande…")`, état `sain` | « Une cause demande un plant atteint ou mort. » | idem |
| cause sur un plant **absent** ou non observé | état `absent` / `non_observe` | « Pas de cause sur un plant absent. » / « … non observé. » | idem |
| cause sur un plant sans état | état `null` | « Dites d'abord l'état. » | « Dites d'abord l'état du plant. » |
| `mangé` sur plant absent, non observé ou sans état | `Refus("« mangé » demande…")` | « Mangé : plant sain, atteint ou mort seulement. » | idem |
| `remplace` sur plant ni mort ni absent | `Refus("« remplace » demande…")` | « Remplace : plant mort ou absent seulement. » | idem |
| plus de plant courant (fin de liste, aucun plant) | `SuiteSession.AucunPlantCourant` | « Plus de plant à dire. » (texte de la C2) | « Aucun plant à qui dire un état. » |
| placette déjà terminée | alerte (`avant.termine`) | « Placette déjà terminée. » **avant** l'annonce de la commande | `Placette déjà terminée. ` en préfixe (texte de la C2) |
| refus de la session (domaine, code de la spec § 6) | `SuiteSession.Refus` | « Refusé. Regardez l'écran. » (texte de la C2) | `code : message` |
| rien à annuler | `RienAAnnuler` | « Rien à annuler. » (texte de la C2) | « Rien à annuler. » |

Le choix « absent / non observé / sans état » demande à `ApplicateurDictee` de lire l'état du plant en plus du libellé de
l'analyseur, qui regroupe ces cas dans un seul refus (par exemple `hauteur refusée sans plant sain, atteint ou mort`). Le libellé de
l'analyseur reste libre (fixtures) : **les textes exacts ci-dessus vivent dans `TextesDictee`**, pas dans la 013.

### Annonces des commandes acceptées (même forme que les gestes de la C2)

| Cas | Voix |
|---|---|
| première saisie d'un état | « Numéro 7, atteint. » |
| redite | « Numéro 7, déjà atteint. » |
| correction | « Correction. Numéro 7, sain. » ; après un « non observé » : « Correction. Numéro 7, non observé, sans mesure. » |
| cause | « Numéro 7, insectes. » (`climat` et `indéterminé` : « autre ou inconnu », libellé de l'écran) |
| mangé | « Numéro 7, mangé. » |
| hauteur | « Numéro 7, hauteur 45 centimètres. » |
| suivant | « Numéro 8. » ; plus de plant : « Tous les plants sont dits. » |
| remplace | « Numéro 7 sera remplacé. Pointez le regarni à l'écran. » |
| annule | « Annulé. » |

Le numéro dit est celui du journal (`PlantSession.numero`), lu **après** l'écriture, comme pour les gestes.

### Sans voix française (décision 7 de la 012-C)

Les mêmes textes **s'écrivent dans la bande** (annonce en grand, en erreur pour un refus), la vibration reste, le bandeau « Pas de
voix française : regardez l'écran » est déjà là (C2). Vosk n'a **aucun lien** avec `TextToSpeech` : la dictée **marche** sans voix
française ; seul le retour est écrit. Sans voix, il n'y a plus de larsen possible : le moteur reste ouvert pendant l'annonce écrite
(§ 8).

## 6. La permission `RECORD_AUDIO`

Déjà déclarée au manifeste (ligne « Dictée hors ligne »). **Aucune demande à l'exécution n'existe aujourd'hui** dans le code de
Surculus (recherche de `RECORD_AUDIO` : seulement le manifeste) ; `MoteurDicteeVosk` suppose que l'appelant l'a obtenue.

**Quand la demander (question 6).** Proposition : **à l'appui sur « Mode poche » et sur « Micro »**, si le modèle est installé et la
permission manquante, jamais à l'ouverture de l'écran et jamais pendant le mode poche. Raisons : en mode poche le forestier n'a pas
le téléphone en main pour répondre à la fenêtre système ; la demande au moment de l'action se comprend (« pour dicter, il faut le
micro »). Marculus fait de même (demande au moment où la dictée devient active, `DicteeVocale.kt`).

| Situation | En entrant en mode poche | Au bas long (en poche) | Au bouton « Micro » (hors poche) |
|---|---|---|---|
| Permission accordée | normal | l'écoute s'ouvre | l'écoute s'ouvre |
| **Refus simple** (première fois) | le mode poche **entre quand même** ; bandeau « Micro refusé : la dictée est indisponible, le volume marche. » ; voix idem | voix « Dictée indisponible : micro refusé. » + vibration REFUS ; rien d'autre | message à l'écran, même texte ; le bouton redemande au prochain appui |
| **Refus définitif** (« ne plus demander », ou deuxième refus : la fenêtre ne s'ouvre plus) | idem, bandeau « Micro refusé définitivement : réglages de l'application » | voix « Dictée indisponible : micro refusé. Réglages du téléphone, applications, Surculus, autorisations. » (une fois par entrée en poche, ensuite le court) | le bouton devient **« Réglages »** et ouvre l'écran d'informations de l'application (`ACTION_APPLICATION_DETAILS_SETTINGS`, paquet de Surculus) |

La saisie au volume n'est **jamais** bloquée par une permission : seule la dictée l'est. Détection du refus définitif : après la
demande, résultat refusé **et** `shouldShowRequestPermissionRationale` faux. Un test pur (état de permission vers texte et action)
couvre les trois lignes ; la fenêtre système elle-même n'est pas testable sans appareil (essai sur le téléphone de Pascal).

## 7. Le modèle absent ou refusé

`ModeleVoskImport.estInstalle` répond (vérifie le témoin ; léger, ne lève pas). La 048 est déjà là : Paramètres → Dictée
vocale installe, vérifie la somme SHA-256 (DI-303), refuse un fichier altéré et conserve l'ancien modèle.

| Situation | Écran | Voix | Saisie au volume |
|---|---|---|---|
| Modèle non installé, à l'entrée en mode poche | bandeau « Dictée non installée : Paramètres, Dictée vocale. » | « Dictée non installée. » une fois | **inchangée** |
| Modèle non installé, appui long bas | même bandeau | « Dictée non installée. Paramètres, dictée vocale. » + REFUS | inchangée |
| Modèle non installé, bouton « Micro » | bouton **« Installer la dictée »** (même hauteur) qui mène à Paramètres → Dictée vocale (route existante, 046/048) | sans objet (hors poche) | inchangée |
| Modèle installé mais illisible (`Model()` lève, `surErreur`) | « Dictée impossible : modèle illisible. Rechargez-le dans Paramètres, Dictée vocale. » | idem, court | inchangée |
| Modèle refusé à l'installation (somme fausse, 048) | rien de nouveau ici : la 048 le dit dans Paramètres ; l'ancien modèle reste, la dictée marche avec lui, sinon ligne « non installé » | | inchangée |

Le renvoi vers Paramètres n'est **pas touchable en mode poche** (tactile ignoré) : en poche la voix dit où aller, le bouton n'existe
qu'après la sortie. Le modèle n'est jamais cherché sur le réseau depuis le contrôle (hors réseau d'abord, ADR 0004).

## 8. La voix de sortie et le micro : l'ordre

**Règle d'or : le micro est fermé pendant que le téléphone parle.** Sinon Vosk entend `TextToSpeech`, qui dit « atteint » ou
« insectes » : la machine se répond à elle-même (larsen). Marculus l'a appris (« anti-larsen : le micro est coupé AVANT toute
annonce TTS et rouvert après `onDone` », `VoskVoiceService`, `SessionMartelage.kt`).

Déroulé d'une phrase :

1. Vosk rend une phrase (`surPhrase`).
2. **`arreter()`** : le micro et le `Recognizer` sont fermés (le modèle chargé reste en mémoire, `MoteurDicteeVosk.arreter`).
3. La phrase est appliquée (verrou, fil d'arrière-plan, § 4).
4. La vibration part, puis l'annonce (`TextToSpeech`, `QUEUE_FLUSH`).
5. Quand l'annonce est finie (`UtteranceProgressListener.onDone`) : **`demarrer()`** si l'écoute doit rester ouverte (§ 3.4).
6. **Garde-fou** : si `onDone` ne vient jamais (moteur de synthèse en échec, comme le note Marculus), le micro se rouvre au bout de
   **5 secondes** : « sans callback de fin, le micro resterait fermé pour toujours ».

Annonce en cours quand l'écoute s'ouvre (le forestier vient de faire un geste, la voix dit « numéro 7, sain. Suivant, 8. ») :
**l'ouverture coupe la voix** (`stop()`), puis ouvre le micro. L'annonce coupée n'est pas rejouée : le journal l'a déjà écrite, et
l'agent qui ouvre l'écoute a décidé de parler. À l'inverse, **un geste de volume pendant l'écoute** suspend le micro le temps de son
annonce, puis le rouvre (même chemin que 2 à 5).

Sans voix française : pas de `TextToSpeech`, donc pas de larsen ; le micro **reste ouvert** pendant l'annonce écrite (pas de
`arreter()`), seule la vibration passe.

Vibration pendant l'écoute : très courte, elle peut être captée ; voir le risque au § 3.3. Elle part **après** `arreter()` (étape 4),
jamais pendant que le micro est ouvert, sauf l'ouverture elle-même.

## 9. Ce qui dépend de la #114 et ce qui n'en dépend pas

Lu sur `origin/mission/012c2-voix-vibration` (jamais fusionnée ici).

**Dépend de la #114** (la 012-D part donc **après** sa fusion) :

* `MoteurVoix` / `VoixAndroid` : il manque **la fin d'annonce** (`UtteranceProgressListener.onDone`) ; l'interface actuelle
  (`dire(texte): Boolean`) ne la rend pas. Le § 8 en a besoin. La 012-D ajoute `surFin` à `MoteurVoix` (ajout minime, annoncé dans la PR).
* `SortiesPoche.annoncer`, `AnnoncePoche`, `MotifsVibration` (les neuf motifs ; la 012-D en ajoute deux et étend le test des motifs
  distincts), `AnnoncesPoche` (textes réutilisés : « plus de plant à dire », « rien à annuler », « refusé. Regardez l'écran », ligne
  « placette déjà terminée »), `PocheApp.sorties`, `PocheApp.installer`.
* Le branchement de `EcranControle` (`PocheApp.pont.ecoute`, `surRappel`, bande avec `ligneVoix` et `annonce`) : la 012-D le
  prolonge ; deux PR qui touchent `EcranControle.kt` en même temps se heurteraient.
* `VIBRATE` au manifeste.
* `ActionGeste.OuvrirDictee` répond aujourd'hui « dictée pas encore disponible » (texte de la C2) : la 012-D **remplace** ce texte.

**Ne dépend pas de la #114** :

* `ApplicateurDictee`, `IssueDictee`, `TextesDictee` (`:core`, Kotlin pur) et leurs tests : ils lisent la session, l'analyseur et
  les fixtures. Ils peuvent être écrits **avant** la fusion de la #114.
* La machine d'écoute (états : fermée, ouverture, ouverte, suspendue, erreur ; ports : moteur, horloge, permission, voix ; en
  Kotlin pur, testée sans micro), la logique de permission, la logique « modèle absent ».
* `MoteurDicteeVosk`, `ModeleVoskImport`, `Grammaire` : tels quels.

## 10. Découpage proposé (deux PR, sous le plafond de 3)

* **012-D1 — Le cœur de la dictée, sans écran.** `:core` : `ApplicateurDictee`, `IssueDictee`, `TextesDictee` (textes du § 4 et § 5),
  lecture de l'état courant. Tests : rejeu de `fixtures/dictee/` (sans les modifier) **à travers la session**
  (comme `RejeuViaSessionTest` pour les règles) : chaque cas de `contexte.json` (DI-100 à DI-174) donne la même acceptation, la
  même ligne ajoutée ou non, le même `remplace_uuid` ; texte exact de chaque refus ; « hauteur » sans état puis sur mort ; « vide » ;
  redite sans ligne ; placette terminée ; **Somalie** (un fix hors de France ne pointe rien, même après une dictée). Peut partir
  avant la fusion de la #114 si Pascal l'autorise ; ne touche pas `EcranControle`.
* **012-D2 — L'écoute branchée.** `:app` : machine d'écoute et délais, permission `RECORD_AUDIO` (§ 6), modèle absent (§ 7), bouton
  « Micro », bande « J'ÉCOUTE » / `entendu / compris`, geste bas long, anti-larsen et `surFin` (§ 8), deux motifs de vibration,
  annonce « écoute terminée ». **Après la fusion de la #114 et de la D1.** Vérifiable sur la release puis à Couchey.
* Une seule PR est possible mais mélange trois essais sur appareil (écoute, permission, anti-larsen). Je recommande deux.
* Ni l'une ni l'autre ne touche à la grammaire, au moteur de la 013, aux vides (tranche 2), aux photos, au cheminement, ni à
  l'essai écran éteint (014), ni à `fixtures/`.
* Rouge d'abord : un test écrit avant le code, vu rouge une fois (fiche). `.claude/test-command` au code 0.

## 11. Les questions pour Pascal (une par une, chacune avec sa proposition)

1. **Hors du mode poche, comment ouvre-t-on l'écoute ?**
   *Proposition : un bouton « Micro » sur la vue Plant, à côté de « Mode poche », qui bascule.* Le volume y règle le son (décision 5),
   donc pas de geste ; sans bouton, la dictée ne serait possible qu'en poche. Autre voie : pas de dictée hors du mode poche (plus
   simple, mais l'agent qui veut dire une cause à voix haute sans entrer en mode poche ne le peut pas).
2. **Comment l'écoute se ferme-t-elle ?**
   *Proposition : elle reste ouverte pour enchaîner, et se ferme par un nouvel appui long bas, ou après 10 s sans phrase, ou à la
   sortie du mode poche / `onPause` / `onStop`.* C'est ce que demande le critère d'arrêt (« atteint » puis « insectes »). Autre voie :
   « une phrase, une écoute » (se ferme après la première phrase), plus sûre pour le micro mais il faut rouvrir pour chaque mot. Autre
   voie encore : « tenir pour parler » comme Marculus, qui exige que la C1 signale le relâchement (hors périmètre de la D).
3. **La voix parle-t-elle aussi hors du mode poche pendant l'écoute ?**
   *Proposition : oui*, la voix et la bande confirment chaque phrase tant que l'écoute est ouverte, poche ou non : l'agent qui dicte a
   les mains prises. La voix de la C2 est aujourd'hui liée au geste en mode poche seulement.
4. **Une phrase d'état ou de cause fait-elle avancer au plant suivant ?**
   *Proposition : non*, seul « suivant » avance. Raison : la grammaire a « suivant », et « sain » est souvent suivi de « mangé » ou
   « hauteur… ». *Incohérence à connaître* : au volume, sain et mort avancent ; à l'écran, sain, absent et une cause avancent. Une
   session mêlant volume et voix passe donc d'un comportement à l'autre. Autre voie : avancer comme l'écran (sain et absent), au prix
   de ne plus pouvoir dire « mangé » ou « hauteur » sur un plant sain.
5. **Quand on corrige un état par la voix, que deviennent la cause, le détail et la hauteur déjà dits ?** (« atteint » avec « insectes »
   et 45 cm, corrigé en « mort ».)
   *Proposition : on les garde quand le domaine les accepte* (cause et hauteur pour mort ou atteint ; hauteur pour sain), on les retire
   sinon (« absent », « non observé » : décision 12), et la voix le dit. Raison : le forestier qui corrige l'état ne veut pas redicter
   ce qu'il vient de mesurer. *Coût* : `dire` n'efface plus seul, il faut passer par `preciser`. Autre voie : tout effacer (le plus
   simple, `dire(code)`), mais l'agent perd la hauteur sans le voir. La spec laisse `mesures_conservees` non déterminé hors « non observé ».
6. **Quand demande-t-on le micro ?**
   *Proposition : à l'appui sur « Mode poche » et sur « Micro », jamais à l'ouverture de l'écran ni en poche*, avec le mode poche
   qui entre quand même si le micro est refusé. Autre voie : au premier appui long en poche (le forestier ne peut pas répondre à la
   fenêtre, téléphone en poche) ou à l'ouverture de la placette (demande avant que l'agent comprenne pourquoi).
7. **Un bruit ou `[unk]` seul est-il un refus dit à la voix ?**
   *Proposition : oui, « Pas compris. »*, sauf dans la seconde qui suit la vibration d'ouverture (risque de vibreur entendu), où un
   `[unk]` seul est ignoré. Raison : la fiche demande que chaque refus se dise ; à l'essai, si le vent fait parler le téléphone
   toutes les deux secondes, on se reposera la question avec des chiffres (pas d'invention d'un seuil de confiance). Autre voie :
   ignorer tous les `[unk]` seuls (silence), au prix de ne plus savoir si le micro a bien entendu.
8. **« Remplace » en poche : que se passe-t-il quand l'agent ressort du mode poche ?**
   *Proposition : la session garde le regarni en attente ; à la sortie, la vue Plant montre « Remplace le plant n° 7 : pointez le
   regarni » avec le bouton « Pointer » en grand (la vue Pointer existe, 012-B2).* Le pointage ne peut pas se dire (boussole, distance,
   essence). Autre voie : refuser « remplace » en mode poche (« Remplacer : sortez du mode poche »), mais la grammaire le reconnaît
   et la décision de Pascal du 2026-10-27 pose « remplace » comme commande de dictée.
9. **Dictée sur une placette déjà terminée : écrire avec alerte, comme les gestes, ou refuser ?**
   *Proposition : écrire avec l'alerte à chaque phrase* (« Placette déjà terminée. », puis l'annonce), identique aux gestes
   (012-C, décisions 3 et 6). La fiche liste « placette déjà terminée » parmi les refus ; c'est contraire à cette décision, donc je
   pose la question.
10. **Textes des refus du § 5.** *Proposition : tels quels.* Ils sont courts (moins de dix mots), disent quoi faire, et ne lisent jamais
    de code technique. Pascal les relit et corrige ce qu'il ne dirait pas.
11. **Délais : 10 s d'écoute sans phrase, 5 s de garde-fou si la voix ne finit pas, vibration 200 ms à l'ouverture, « un instant » après 2 s
    de chargement du modèle.**
    *Proposition : les graver en constantes nommées, les mesurer sur le téléphone de Pascal (temps de chargement du modèle d'abord,
    jamais mesuré) puis à Couchey, gants et vent, avant de les rouvrir une fois.* Même méthode que les seuils de la C1.
12. **Ordre des deux PR.** *Proposition : D1 (`:core`) dès que Pascal a répondu aux questions 4, 5, 9 et 10, sans attendre la #114 ;
    D2 après la fusion de la #114 et l'essai de la C2 sur le téléphone.* Autre voie : tout attendre pour une PR unique.

## 12. Les sources, à citer

* Spec § 4.3 (grammaire fermée, « hauteur » 1 à 400, refus sans état, SHA-256 sans tenir compte de la casse), § 5 (`non_observe` sans
  mesure) ; ADR 0004 (Vosk 0.3.75, `vosk-model-small-fr-0.22`, `MoteurDictee` remplaçable).
* Fiches 012 (« Périmètre élargi le 2026-10-24 »), 012-A (session), 012-C (neuf décisions), 012-C2, 013 (analyseur, moteur), 036
  (décisions 11 et 12), 048 (modèle).
* `fixtures/dictee/` : `contexte.json` (DI-100 à DI-174, y compris DI-134 à DI-139 redites, DI-130 à DI-132 corrections, DI-165 à
  DI-169 « non observé », DI-170 à DI-174 « hauteur »), `refus.json` (DI-60 et suivants), `hauteur.json`, `phrases.json`. DI-303
  (somme du modèle) reste celui de la 048.
* Marculus (lecture seule) : `PttController.kt`, `NoyauPtt` (terrain), `DicteeVocale.kt` (permission), `SessionMartelage.kt`
  (`onDone`, file d'annonces). On reprend l'idée, pas le code. Groma : aucune dictée utile.
* Convention publiée : aucune nécessaire (aucun code travaux, aucune essence, aucun état nouveau ; les groupes de cause sont ceux de
  `etats-plants.yaml`).

## 13. Le test des dix secondes (pour la fin de la mission, à faire par Pascal)

Debout dans une placette déjà pointée, téléphone en poche, une main libre : appui long sur volume bas, **dire « atteint » puis
« insectes »**, et **en dix secondes, sans sortir le téléphone**, savoir (1) que le téléphone écoutait (le coup de vibration), (2) ce
qu'il a compris (la voix : « numéro 7, atteint », « numéro 7, insectes »), (3) quand il a cessé d'écouter (les deux coups). Puis dire
une phrase fausse (« bonjour ») et reconnaître le refus à l'oreille.
**Question de contrôle :** *comprend-on la situation en dix secondes ?*

## 14. Ce qui manque et n'est pas à moi

* Le temps de chargement du modèle (41 Mo) et le délai réel d'ouverture du micro : non mesurés ; à mesurer sur le téléphone.
* Le fil d'exécution des rappels de `SpeechService` (principal ou arrière-plan) : à vérifier dans vosk-android 0.3.75 avant le code ;
  dans les deux cas l'application passe par le verrou de l'écran.
* Le retour de `courant` au plant annulé après « annule » (la session le dit mal, voir C2) : non touché.
* Le tracé des vides (tranche 2), l'essai écran éteint (014), les photos.
