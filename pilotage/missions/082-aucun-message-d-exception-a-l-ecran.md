# Mission 082 — Plus aucun message d'exception brut à l'écran

> Fiche écrite par l'orchestrateur le 2026-10-30, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> après la décision de Pascal du 2026-10-30 (relecture de la 081 : « Coordonnées ici, e.message après ») et son accord
> du même jour pour écrire la fiche. Depuis `main`. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Remplacer les huit `e.message` encore montrés tels quels au forestier
(chargement de la carte, liste et import des tournées, dictée, récepteur GNSS et NTRIP) par une phrase en français qui
dit quoi faire, le détail technique allant au journal Android, pour que le forestier lise du français partout où
Surculus lui parle.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? (sources, conventions nommées ci-dessous) | oui (081 : causes connues en français, phrase générique, détail au `Log.w`) | |
| A-t-il les moyens ? (outils, données d'essai, tests) | oui | le `testeur` écrit d'abord les tests |

**Temps de la main :** appuyer.

## Ma réponse d'abord

_Rempli par Pascal seul._

## L'Homme

- **Agents :** `testeur` d'abord (tests depuis cette fiche et la spec, **sans lire le code qui produit les textes**),
  puis `dev` (rend les tests verts).
- **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** changer les refus déjà en français (`e.refus.message`) ; changer les textes de la
  081 ; ajouter une cause que le code ne détecte pas déjà.

## Ce qui est constaté (relecture de la 081, kanban)

Sur `main` (76731ab), `e.message` (ou `e.javaClass.simpleName`, `e.toString()`) montré à l'écran :

| Endroit | Ce que voit le forestier |
|---|---|
| `carte/Commun.kt:94` | refus de chargement de la carte |
| `tournee/DepotTournees.kt:145` | ligne d'une tournée illisible dans la liste |
| `tournee/EcranTournees.kt:144` | refus d'import d'une tournée |
| `dictee/MoteurDicteeVosk.kt:45`, `:52` | erreur de dictée |
| `gnss/ServiceGnssRtk.kt:84`, `:140` | erreur du récepteur |
| `gnss/ServiceGnssRtk.kt:170` | erreur NTRIP |

## Harnais

- **Testeur peut :** écrire des tests dans `core/src/test/` et `app/src/test/` qui vérifient, par l'API publique des
  messages ou de l'état affiché, qu'aucun de ces huit textes ne contient un nom d'exception, un `_`, un chemin, un
  UUID ou un texte anglais d'exception, quelle que soit l'exception levée ; les pousser en premier sur
  `mission/082-aucun-message-d-exception-a-l-ecran`.
- **Dev peut :** modifier ces huit endroits et les ressources de textes ; mettre le détail au `Log.w` ; s'appuyer sur
  ce que la 081 a fait (`Libelles`, `EchecInstallation`). Il ne réécrit pas les tests du testeur (s'il en juge un faux,
  il le dit dans la PR).
- **Ne doivent pas :** modifier `fixtures/`, `docs/`, `pilotage/` (sauf la redite) ; toucher à `main`.
- **Sources autorisées :** `docs/spec.md`, fiche 081, cette fiche.

## Valeur provisoire (chiffre, réglage)

Aucune. Les phrases nouvelles sont listées dans la PR (endroit, cause, phrase) ; Pascal les tranche à la relecture.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** aucune autre mission ; sur la branche, le `dev` part du commit du `testeur`.
- **Ordre de fusion :** une seule PR.
- **PR en brouillon jusqu'à :** la fin du travail du `dev`.
- **CI rouge attendue :** les tests du `testeur`, entre son push et celui du `dev` (« rouge justifiée »).

## Tests

- Tests du `testeur` vus rouges sur `main`, verts après le `dev` ; `.claude/test-command` au code 0.

## Terrain

- **Échantillon que Pascal vérifiera :** sur la release, un import de tournée refusé (fichier qui n'est pas un
  GeoPackage) et une erreur de récepteur (Bluetooth coupé), capturés par l'orchestrateur.

## Qui veille, qui tranche

- **Relecteur :** `relecteur` (contrôle de chaque texte contre le code), puis Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Point de suivi :** au push du `testeur`, puis à la PR.
- **Qui tranche :** Pascal (les textes, la fusion).

## Comment saura-t-on que ça marche ?

`git grep -n "e.message\|simpleName\|e.toString()"` dans `app/src/main` ne trouve plus rien qui aille à l'écran ; les
deux captures sont en français.
