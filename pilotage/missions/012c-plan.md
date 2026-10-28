# Mission 012-C — Plan de l'afficheur (rendu avant tout code, le 2026-10-28)

> Temps de la main : **appuyer**. Aucun code, aucun test, ni `docs/`, ni le journal. Sources lues : fiche 012-C,
> `CLAUDE.md`, spec § 4.3, ADR 0004 (lignes « Boutons de volume » et « Geste annuler »), `Gestes.kt` et `SessionControle`
> (`appliquer`, `dire`, `suivant`, `annuler`, `terminer`), `ControleurPlacette.kt`, `EcranControle.kt`, `MainActivity.kt`,
> `AndroidManifest.xml`, fiche 014, Marculus `MainActivity.kt:75-120` (lecture seule). Les références sont celles de
> `main` à f955af6.

## 1. Redite

Faire dire l'état de chaque plant au seul volume, téléphone en poche : un classeur de touches (court, long, double,
3 s), un mode poche qui ignore le tactile et garde l'écran allumé, une voix et une vibration qui répondent « numéro 7,
sain », pour que le forestier, gants aux mains, n'ait jamais à regarder l'écran.

## 2. Ce que l'écran montre (fiche d'affichage)

* **Question** : « quel plant ai-je dit, dans quel état, et suis-je encore en mode poche ? » Pour le forestier debout
  dans la placette, une main libre, téléphone en poche, sans réseau.
* **Une seule chose en grand** : la vue **Plant** existante (« Plant n° 7 », son état). Le mode poche n'ajoute pas de vue :
  il ajoute **un bouton « Mode poche »** sur la vue Plant et, tant qu'il dure, **une bande unique « MODE POCHE · 3 s sur
  volume haut pour sortir »** en haut (couleurs de la 063 : texte `onSurface`, rien de plus).
* **Comparaison** : « 7 sur 12 » (déjà là) ; le dernier mot dit, en petit : « dit : n° 7, sain ».
* **Source et date** : inchangées (code de la placette, jour). La voix répète ce que le journal a écrit, pas ce que le
  doigt a visé : une seule source, la `SessionControle`.
* **Lieu** : voix et vibration d'abord, écran seulement pour vérifier. Pas de réseau (`TextToSpeech` du téléphone).
* **1664** : une idée par vue, six lignes, quatre couleurs : le mode poche ne crée aucune couleur nouvelle.
* **Pièges écartés** : pas de réglage de plus (seuils fixes), pas de compteur de gestes, pas d'écran qui accuse.

## 3. La table des gestes, fonction pure (geste, contexte) -> action

La table de la spec (§ 4.3, colonne « en placette ») **existe déjà** : `GestesPlacette.action(geste)` dans `Gestes.kt`
(012-A) et `SessionControle.appliquer`. La 012-C ne la change pas ; elle ajoute (a) le **classeur** qui transforme des
touches en `Geste`, (b) le **contexte**, (c) la **sortie du mode poche**. Fonction visée :
`GestesPoche.action(geste, contexte) -> ActionPoche`, en Kotlin pur dans `:core`.

Six gestes (un de plus que la spec) : `VOLUME_HAUT_COURT`, `VOLUME_BAS_COURT`, `VOLUME_HAUT_LONG`, `VOLUME_BAS_LONG`,
`VOLUME_BAS_DOUBLE_COURT`, et **`VOLUME_HAUT_3S`** (sortie du mode poche, décision de Pascal du 2026-10-27).

| Geste | Plant courant à dire | Plus de plant à dire (« Tous dits ») | Après « Terminer » (saisie permise, décision 3) |
|---|---|---|---|
| haut court | sain, puis plant suivant | refus : « plus de plant à dire » | sain, puis suivant, **avec l'alerte** « placette déjà terminée » |
| bas court | mort, puis plant suivant | idem refus | mort, puis suivant, avec l'alerte |
| haut long (relâché entre 0,5 s et 3 s) | atteint, **plant gardé** (cause à l'écran, décision 4) | idem refus | atteint, avec l'alerte |
| bas long (pris à 0,5 s) | demande la dictée : 012-D absente, voir § 5 | idem | idem |
| bas double court | annule la dernière saisie | annule (« rien à annuler » si rien) | annule, y compris la fin de placette (elle redevient « à faire ») |
| haut 3 s | **sort du mode poche**, rien d'écrit | idem | idem |

Cas particuliers, dits une fois pour toutes :

* **Regarni en attente** (vue Pointer) : le pointage exige la boussole, la distance tapée et l'essence : il **ne se fait
  pas en poche**. Le bouton « Mode poche » n'existe que sur la vue Plant ; quitter la vue Plant sort du mode poche. Les gestes
  n'y sont donc jamais reçus. Un plant dit « mort » en poche se remplace à l'écran, après sortie.
* **Plant « atteint »** : en poche, aucune cause n'est possible (tactile ignoré). Le plant reste sans cause (facultative,
  spec § 4.3) ; la voix dit « atteint », jamais « cause ». La cause se met à l'écran après sortie, ou à la dictée (012-D).
* **Sain et mort** passent au suivant (`session.suivant()` saute les plants déjà dits et les remplacés) ; **atteint** reste
  sur le plant, comme dans la spec.
* **« Absent »** n'a pas de geste en placette (la spec le réserve au cheminement) : il se met à l'écran.
* **Hors mode poche**, les touches de volume gardent leur rôle d'Android (question 1) : aucun geste.
* **Deux touches ensemble** : ignorées toutes les deux jusqu'à relâchement complet (raccourci d'accessibilité d'Android évité,
  ADR 0004). Rien d'écrit, vibration de refus.
* **Somalie** : les gestes ne pointent jamais (ils disent l'état d'un plant déjà pointé), mais un test verrouille qu'un fix
  hors de France ne pointe rien, même par le volume (fiche).

**Trou dans la session (à dire, pas à contourner).** Le bas court écrit « mort » **et avance** au plant suivant ; le double
appui bas = deux appuis courts rapprochés. Si le premier écrit tout de suite, le second annule bien l'état, mais le plant
courant est déjà le suivant (`annuler` ne ramène pas `courant`, `SessionControle.kt:343-350`) : l'agent croit annuler le
plant 7 et se retrouve sur le 8. Deux voies, question 3.

## 4. Le conflit : « haut long = atteint » contre « haut 3 s = sortie »

Même touche, deux durées. Règle proposée : **on agit au relâchement, jamais pendant l'appui, sauf pour avertir.**

| Durée de l'appui sur volume haut | Ce qui est ressenti pendant l'appui | Ce qui est enregistré |
|---|---|---|
| moins de 0,5 s | rien | sain (au relâchement) |
| de 0,5 s à moins de 3 s | **un tic** à 0,5 s (« atteint armé ») | atteint (au relâchement) |
| 3 s ou plus | un **long bourdon** à 3 s, le tic ayant déjà eu lieu | **rien** |

* À 3 s, le mode poche se ferme **immédiatement** (retour sensible à la main) ; le relâchement qui suit est mangé.
* **Rien n'est jamais enregistré** pendant un appui de 3 s : « atteint » n'est écrit qu'au relâchement, après avoir
  vérifié que l'appui a duré moins de 3 s. Il n'y a donc **rien à annuler**. Choix de sûreté : sortir par erreur ne coûte
  qu'un bouton à reappuyer ; écrire « atteint » par erreur coûte une donnée fausse.
* Pourquoi cela tient avec des gants : l'écart est de **un à six** (0,5 s contre 3 s) et chaque seuil est **senti** sans
  regarder (le tic dit « atteint pris si je lâche », le bourdon dit « je sors »).
* Limite honnête : un forestier qui veut « atteint » et tient trop longtemps sort du mode poche sans rien écrire. Il le sait
  par le bourdon et la voix « mode poche terminé » ; l'état n'est pas dit, la voix ne ment pas.
* Seuils fixes (aucun réglage) : long 0,5 s (comme le long appui du système et le push-to-talk de Marculus), sortie 3 s,
  fenêtre du double appui 0,35 s, rebond 40 ms. Constantes nommées, horloge injectée ; on les essaie à Couchey, gants,
  avant de les graver (question 5).

**Le classeur** (`ClasseurGestes`, `:core`, Kotlin pur, horloge injectée) reçoit des événements
`(touche, appuyée ou relâchée, instant en ms)` et rend des sorties : `Geste(g)`, `Tic`, `Bourdon`, `Refus`. Il ne garde
que `repeatCount == 0` (Android répète le `onKeyDown` pendant qu'on tient) et expose `echeance()` pour que l'Activity
programme un `Handler` (le 3 s et la fin de la fenêtre du double appui n'ont pas d'événement tactile). Cas testés : court,
long, double, 3 s, deux touches ensemble ignorées, rebond, appui interminable (30 s : une seule sortie), relâchement
perdu (remise à zéro à `onPause`).

## 5. « Volume bas, appui long = ouvre la dictée » tant que la 012-D n'est pas livrée

La session répond déjà `SuiteSession.DicteeDemandee` et `TextesControle` écrit « La dictée n'est pas encore
disponible. » Ce que fait la 012-C :

* **rien n'est écrit au journal** ;
* la **voix** dit « dictée pas encore disponible » ; la **vibration** est celle du refus ; l'écran affiche le même message
  en information (pas en rouge : ce n'est pas une faute du forestier) ;
* le geste est **pris à 0,5 s** (comme `onKeyLongPress` du push-to-talk de Marculus), pas au relâchement, pour que la 012-D
  n'ait qu'à brancher l'écoute à cet endroit. À la 012-D de décider si le micro se ferme au relâchement.

## 6. Le mode poche

**Définition** : drapeau tenu hors Compose (comme le registre `ouverts` de `EcranControle.kt`, pour survivre à la rotation),
posé par le bouton « Mode poche », levé par les sorties ci-dessous.

| Ce qu'il fait | Mécanisme |
|---|---|
| Garde l'écran allumé | `FLAG_KEEP_SCREEN_ON` posé à l'entrée, retiré à la sortie et à `onStop` (pas de batterie perdue après la tournée) |
| Ignore le tactile | `dispatchTouchEvent` de l'Activity consomme sans transmettre ; **fonction pure** `toucherPris(modePoche, evenement) -> Boolean` (testée sans Android) |
| Garde le volume | `onKeyDown` et `onKeyUp` consomment les deux touches (relâchement compris, pour masquer l'affichage du volume du système, comme Marculus) |
| Retour système | **ignoré** en poche (une poche appuie sur « retour ») ; question 4 |
| Plein écran de la 044 | respecté |

**Sorties** (décision 2) : (1) haut 3 s ; (2) « Terminer » : dès que la session répond `Fait`, le mode est levé et la vue
Terminée s'affiche (le bouton n'est touchable qu'après sortie par 1 ; ce cas couvre aussi toute fin venue d'ailleurs, p. ex.
une fin à la voix) ; (3) l'**application quitte le premier plan** (`onStop`) : sortie par prudence ; (4) **sortie
automatique après 15 minutes sans geste** (question 4).

**Appel entrant.** L'Activity passe en `onPause` : les gestes ne sont plus reçus, l'écran d'appel prend le tactile et le
volume du système sonne de nouveau. Au retour (`onResume`) : le mode poche **reste actif**, le classeur est remis à zéro, et
la voix dit « mode poche actif » avec deux vibrations, pour que le forestier sache qu'il y est encore. Rien n'a été écrit
pendant l'appel. (Si l'appel a mis l'application en `onStop`, c'est la sortie 3 : le forestier est prévenu par l'écran
au retour.)

**Verrouillage de l'écran** (bouton marche/arrêt). `onPause` aussi : le volume n'est pas reçu écran éteint (la mission 014
mesure si une `MediaSession` le permettrait, ADR 0004). **Aucune promesse** de recevoir le volume écran verrouillé ; la
fiche d'essai de Couchey le notera.

**Rotation.** L'état est hors Compose : il traverse la rotation. Le tactile reste ignoré, la bande se redessine, son texte
reste en `onSurface`.

**Risque d'agent « enfermé »** (tactile ignoré, sortie par une seule touche) : trois filets. (1) Si la touche volume haut est
défaillante, le bouton marche/arrêt puis le retour de l'écran lèvent le mode (sortie par `onStop`). (2) Sortie automatique à
15 minutes sans geste. (3) Rappel vocal après 5 minutes sans geste : « mode poche actif, 3 secondes sur volume haut pour
sortir », une fois. Le tactile n'est ignoré que dans l'Activity de Surculus ; jamais sur l'écran d'appel ni sur les boutons
du système.

**Capteur de proximité** : hors périmètre, mentionné pour mémoire.

## 7. La voix (`TextToSpeech`)

**Moteur** : `android.speech.tts.TextToSpeech`, voix du système, locale `Locale.FRANCE`, **aucune relation avec Vosk**
(Vosk écoute, la 048 ne parle pas ; ni modèle, ni micro, ni flux partagés). Si la 012-D ouvre le micro, la voix se tait avant
(`stop()`), à régler alors.

**Textes exacts :**

| Situation | Annonce |
|---|---|
| sain dit | « numéro 7, sain. Suivant, 8. » |
| mort dit | « numéro 7, mort. Suivant, 8. » |
| atteint dit | « numéro 7, atteint. » (plant gardé : pas de « suivant ») |
| dernier plant dit | « numéro 12, sain. Tous les plants sont dits. » |
| geste annulé | « annulé. » puis « numéro 7. » si le plant remis courant est connu (question 3) |
| rien à annuler | « rien à annuler. » |
| plus de plant à dire | « plus de plant à dire. » |
| dictée absente | « dictée pas encore disponible. » |
| deux touches ensemble | « une touche à la fois. » |
| après « Terminer » | « placette déjà terminée. Numéro 7, sain. » (l'alerte **d'abord**, à chaque geste) |
| autre refus de la session | « refusé. Regardez l'écran. » (code et motif restent à l'écran, jamais dits) |
| entrée en mode poche | « mode poche actif. Numéro 7. » |
| sortie du mode poche | « mode poche terminé. » |
| retour d'appel ou de verrouillage | « mode poche actif. » |

* Le **numéro dit est celui du journal** (`PlantSession.numero`), lu après l'écriture, pas celui visé avant.
* Chaque annonce **interrompt** la précédente (`QUEUE_FLUSH`) : trois gestes enchaînés, on entend le dernier.
* **Sans voix française installée** (`isLanguageAvailable` sous `LANG_AVAILABLE`, ou initialisation en échec) : **jamais de
  silence**. Le texte de l'annonce s'affiche **dans la bande du mode poche** (information, ou erreur pour un refus), la
  vibration reste, et à l'entrée un bandeau dit : « Pas de voix française : regardez l'écran. » Le forestier est prévenu avant
  de mettre le téléphone en poche. Rien d'écrit de plus au journal.
* **Volume de la voix** : flux média, que le forestier règle **avant** d'entrer en poche (les touches sont alors consommées).

## 8. La vibration

Permission `VIBRATE` : **permission normale**, sans fenêtre de demande ; elle est à déclarer au manifeste (elle n'y est pas
aujourd'hui). `VibrationEffect.createWaveform`. Sur un appareil sans vibreur : pas de vibration, la voix et l'écran restent.

| Événement | Motif (ms : pause, vibre, pause, vibre…) |
|---|---|
| sain | 0, 60 |
| atteint | 0, 60, 80, 60 |
| mort | 0, 400 |
| annulé | 0, 60, 80, 400 |
| refus, dictée absente, deux touches | 0, 40, 40, 40, 40, 40 |
| tic « atteint armé » (0,5 s) | 0, 30 |
| bourdon de sortie (3 s) | 0, 600 |
| entrée en mode poche | 0, 100, 100, 100 |
| placette déjà terminée (avant l'état) | 0, 400, 100, 60 |

Un motif par événement, jamais deux qui se couvrent (le dernier remplace). Les motifs sont des constantes dont un test
vérifie qu'ils sont **tous distincts** et de moins d'une seconde.

## 9. Les couleurs (063)

Rien de nouveau : la bande « Mode poche » en `onSurface` sur le fond du thème, avec un filet ; le refus en
`colorScheme.error` ; pastilles d'état cernées ; aucune plaque blanche. Contraste du texte >= 4,5:1 testé en clair et en
sombre, comme pour la 012-B1.

## 10. Les sources

* Spec § 4.3 (la table, le mode poche, le double appui) ; ADR 0004 (écran maintenu allumé en tranche 1, `MediaSession` à
  l'essai de la 014, double appui bas « à confirmer à l'essai »).
* Marculus `MainActivity.kt:75-120` (lecture seule) : `onKeyDown`, `onKeyLongPress`, `onKeyUp`, `event.startTracking()`,
  relâchement consommé pour masquer l'affichage du volume, porteur `ToucheVolume` que l'écran branche et débranche. On
  reprend l'idée, pas le code.
* `Gestes.kt` et `SessionControle.appliquer` (012-A) : la table existe, on ne la change pas.

## 11. Les questions pour Pascal (une par une, chacune avec sa proposition)

1. **Les gestes de volume sont-ils actifs seulement en mode poche ?** *Proposition : oui.* Hors du mode poche, le volume
   règle le son comme d'habitude (et la voix) ; on n'écrit pas un état en réglant le volume par mégarde.
2. **La voix annonce-t-elle le plant suivant après un sain ou un mort ?** *Proposition : oui* (« numéro 7, sain. Suivant,
   8. ») : sans écran, c'est la seule façon de savoir quel plant on dira ensuite (le suivant saute les plants déjà dits).
3. **Double appui bas : écrire tout de suite puis annuler, ou attendre la fenêtre ?** *Proposition : attendre 0,35 s avant
   d'écrire un bas court*, pour que le double appui n'écrive jamais « mort ». Un « mort » arrive 0,35 s plus tard (la voix le
   confirme). L'autre voie (écrire tout de suite, et faire revenir `courant` au plant annulé) change la logique de la
   session 012-A, que la fiche interdit de toucher sans ton accord.
4. **Filets contre l'agent « enfermé »** : sortie automatique après 15 minutes sans geste, rappel vocal à 5 minutes, retour
   système ignoré en poche. *Proposition : les trois, sans réglage.*
5. **Les seuils** (long 0,5 s, sortie 3 s, double 0,35 s, rebond 40 ms). *Proposition : les graver tels quels, en constantes
   nommées, les essayer gants à Couchey, puis les rouvrir une fois si besoin.*
6. **Alerte « placette déjà terminée » : à chaque geste, ou une seule fois ?** *Proposition : à chaque geste*, avant le
   numéro. Plus bavard, mais le forestier ne regarde pas l'écran et un seul avertissement se perd.
7. **Plant « atteint » en poche : la cause reste vide.** *Proposition : oui*, la voix dit seulement « atteint » ; la cause se
   met à l'écran ou à la dictée (décision 4), sans nouveau geste.
8. **Sans voix française installée : entrer quand même en mode poche ?** *Proposition : oui, avec le bandeau* « Pas de voix
   française : regardez l'écran » et les annonces écrites dans la bande ; la vibration suffit déjà à confirmer.

## 12. Le découpage proposé (deux PR, sous le plafond humain)

* **012-C1 — Le classeur et le mode poche.** `ClasseurGestes` et `GestesPoche` (`:core`, tests : court, long, double, 3 s,
  deux touches, rebond, appui interminable, remise à zéro), `toucherPris` (pur), bouton « Mode poche » et bande,
  `FLAG_KEEP_SCREEN_ON`, tactile ignoré, sorties (3 s, Terminer, `onStop`, 15 minutes), câblage à `SessionControle.appliquer`,
  retours à l'écran seulement. Capture : la vue Plant en mode poche. Vérifiable sans casque ni vibreur.
* **012-C2 — La voix et la vibration.** Adaptateur `TextToSpeech`, textes exacts (purs), cas sans voix française, motifs de
  vibration (purs, tous distincts), `VIBRATE` au manifeste, annonce à chaque geste, alerte « déjà terminée ». Branché sur le C1.
* Une seule PR est possible mais mélange deux essais sur appareil ; je recommande deux PR, la seconde ne partant qu'après
  la fusion de la première (une seule version sur le téléphone).
* Ni l'une ni l'autre ne touche à la dictée (012-D), à l'essai écran éteint (014) ni à `fixtures/`.
* Rouge d'abord : un test écrit avant le code, vu rouge une fois (fiche). `.claude/test-command` au code 0.

## 13. Le test des dix secondes (pour la fin de la mission, à faire par Pascal)

Téléphone en mode poche, main dans la poche : **en dix secondes, sans regarder, dire quel plant on vient de dire et son
état** (« le 7, sain »), puis faire un « atteint » et un « annuler ».
Question de contrôle : *comprend-on la situation en dix secondes ?*

## 14. Ce qui manque et n'est pas à moi

* Le retour de `courant` au plant annulé (question 3), si Pascal choisit « écrire tout de suite ».
* Le volume écran verrouillé : la mission 014 le mesure ; la 012-C n'en dépend pas.
