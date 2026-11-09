# Liste de contrôle de l'orchestrateur

> Mission 106. Décision de Pascal au bilan du 2026-11-09 (« Oui, la liste »), cause n° 1 du Pareto :
> 26 incidents sur 51 sont des affirmations non vérifiées à leur source (rapport de l'`analyste`, journal l. 1149).
> **Propriétaire :** l'orchestrateur, qui l'applique. **Mise à jour :** à chaque bilan. **Version :** 2026-11-09.

## Quand

Avant chaque **relecture** confiée au `relecteur`, avant chaque **question à Pascal**, avant chaque
**livraison** annoncée. Une case non cochée se dit : « non vérifié ».

Les numéros de ligne ci-dessous sont ceux de `pilotage/journal.md` au 2026-11-09 (le journal est en ajout seul :
elles ne bougent pas).

## Les points

### 1. Le corps de la PR est relu contre le diff

Chaque phrase affichée à l'utilisateur et chaque chiffre du corps sont comparés au code actuel, pas à celui du premier jet.
- *Incidents :* journal l. 800 (PR #137, corps seul à reprendre) ; l. 1113 (PR #170, corps périmé, M1) ; l. 1126 (PR #172,
  phrases à lister dans la PR) ; l. 1141-1142 (PR #174, anciens textes dans le corps).
- *Vérifier :* `gh pr view N --json body -q .body`, puis `gh pr diff N` (ou `git diff origin/main...origin/<branche>`, sans checkout) et `grep -n` de chaque phrase citée.

### 2. Une preuve annoncée est ouverte dans l'artefact avant d'être écrite

Image, rapport ou rendu promis : on l'ouvre (téléchargé, vu) avant de l'écrire dans la fiche, le corps ou la question.
- *Incident :* l. 1138 (PR #174, « rendu-des-ecrans » promis, le test n'écrivait aucune image) ; l. 1147 (décision de Pascal).
- *Vérifier :* `gh api repos/pobsteta/surculus/actions/runs/ID/artifacts -q '.artifacts[].name'`, puis `gh run download ID -n NOM` et ouvrir le fichier.

### 3. La cible `adb` est vérifiée avant d'installer

- *Incident :* l. 1145 (release installée sur l'émulateur par erreur de cible, téléphone débranché).
- *Vérifier :* `adb devices -l`, puis toujours `adb -s <série du téléphone> install ...` (jamais sans `-s`) ;
  après coup, `adb -s <série> shell dumpsys package io.github.pobsteta.surculus | grep versionName` (`applicationId`, `app/build.gradle.kts:67` ; la variante « fumée » ajoute `.fumee`).

### 4. Le fait d'un agent est cité avec sa ligne de code avant d'en faire une question

Un constat d'agent (« une seconde ouverture », « aucun écran n'affiche... ») se retrouve dans le fichier, avec
`fichier:ligne`, avant d'être posé à Pascal. Sinon la question dit « non vérifié ».
- *Incident :* l. 1094 (« je me suis trompé » : « ligne en trop » présentée sans vérification, question de la 101 fondée sur un fait faux).
- *Vérifier :* `grep -n MOTIF chemin/du/fichier` et noter `fichier:ligne` dans la question.

### 5. Chaque chiffre d'une question à Pascal est vérifié

Version, date, nombre de tests, numéro de PR.
- *Incident :* l. 1152 (« v0.7.104 » écrit sans vérification ; la dernière release était v0.7.100).
- *Vérifier :* `gh release list --limit 3` ; `gh pr view N` ; `date +%F` ; `git log -1 --format=%h` ; nombre de tests : rapports XML `app/build/test-results/**/*.xml` (attributs `tests`, `failures`) ou la ligne Gradle « N tests completed ».

### 6. Une consigne d'essai donnée à Pascal est rejouée ou lue dans le code

Une marche à suivre sur le téléphone se confronte au code de l'écran concerné avant d'être envoyée.
- *Incident :* l. 1110 (consigne « couper la localisation » : l'écran affiche alors « activer la localisation » au lieu du bouton,
  `ArriveeSansPosition.kt:41-42`).
- *Vérifier :* lire le fichier cité, suivre la consigne pas à pas sur l'émulateur ou dans le code.

## Comment le bilan compte les manquements

Tout manquement constaté s'inscrit au journal en une ligne `écart` au format complet :
`attendu : ... · vu : ... · décision : ...`, en citant le numéro du point de cette liste (« liste, point 4 »).
L'`analyste` compte ces lignes au prochain bilan ; la cause n° 1 doit descendre sous 26 incidents.
Une ligne `écart` se compte quand elle est complète. La l. 850 du journal l'est (datée, avec attendu, vu, décision) ; son défaut, relevé par l'`analyste`, est une décision « creuser » sans échéance : la décision d'un `écart` porte une échéance ou un propriétaire.

## Ce que cette liste ne fait pas

Elle ne remplace ni le `relecteur`, ni la décision de Pascal. Elle ne couvre pas la cause n° 2 (décision de sens non préparée)
ni la cause n° 3 (garde aveugle : une garde n'est prouvée que vue rouge), traitées par ailleurs.
