# Mission 082 — Plus aucun message d'exception brut à l'écran

> Fiche écrite par l'orchestrateur le 2026-10-30, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> après la décision de Pascal du 2026-10-30 (relecture de la 081 : « Coordonnées ici, e.message après ») et son accord
> du même jour pour écrire la fiche. Depuis `main`. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Remplacer les huit `e.message` encore montrés tels quels au forestier
(chargement de la carte, liste et import des tournées, dictée, récepteur GNSS et NTRIP) par une phrase en français qui
dit quoi faire, le détail technique allant au journal Android, pour que le forestier lise du français partout où
Surculus lui parle.

**Redite par l'agent :** 
- *Testeur (2026-11-02) :* ma mission est d'écrire les tests qui échouent tant qu'un des huit textes montrés au forestier
  (carte, liste et import des tournées, dictée, récepteur GNSS, NTRIP) laisse passer un nom d'exception, un texte
  anglais brut, un `_`, un chemin ou un UUID, pour que le `dev` ait un critère net et que le travail ne s'auto-évalue pas.
- *Dev (2026-11-02) :* ma mission est de remplacer, aux huit endroits (et dans le refus du module `gpkg`), le message brut
  de l'exception par une phrase française qui dit quoi faire, le détail technique allant au `Log.w`, pour que le forestier
  ne lise que du français et que les neuf tests du `testeur` passent au vert.

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

## Décision de Pascal du 2026-11-02 (après le push du testeur)

Les tests du `testeur` montrent que le refus du module `gpkg` (`gpkg/.../Refus.kt:26`, « Ce fichier n'est pas un
GeoPackage lisible : [SQLITE_NOTADB] File opened that is not a database file … ») met à l'écran le détail SQLite
anglais. Pascal : « Élargir la fiche ». Le `dev` **peut** modifier ce refus : la phrase française reste, le détail
technique sort de l'écran et va au journal Android (ou à un champ non affiché). Les quatre tests du `testeur` sur
`DepotTournees.ligne` restent tels quels. Les autres refus du module `gpkg` qui recopient un détail technique sont dans le
même cas ; le `dev` les liste dans la PR.

## Reprise, aller-retour 1/3 (2026-11-02)

Relecture de la #146 (8f1379d) : « à reprendre ». Décisions de Pascal du 2026-11-02, posées une à une :

1. **Refus « pas un GeoPackage »** : « Ajouter l'action » : la phrase dit quoi faire (par exemple « l'exporter de nouveau
   depuis sommieR ») ; le `dev` propose la phrase exacte, Pascal la tranche.
2. **Refus maison avec `_`** (« ni identifiant d'application GPKG, ni table gpkg_contents », « tables gpkg_contents ou
   gpkg_spatial_ref_sys absentes ») : « Les traduire » : phrase française qui dit quoi faire, détail au journal.
   Le `testeur` remet d'abord le cas « fichier vide » (et un fichier sans tables), vu rouge.
3. **Récepteur** : « Aligner sur la carte » : l'Essai GNSS reprend les phrases de la carte
   (`strings_tournees.xml:111-112`, « Liaison avec le récepteur GNSS perdue : nouvelle tentative en cours. »,
   « Corrections RTK indisponibles : la position est moins précise. »), plus l'action à faire là où elle manque ;
   « injoignable » disparaît.
4. **Contrôles d'intégrité de l'export** (`ExportTravaux:487-494`) : « Les laisser, noter » : hors de la 082 (bac à sable).

À reprendre aussi (relecteur) :
- la liste des tournées bégaie (« Illisible : tournée illisible : … », préfixe `tournees_illisible`,
  `EcranTournees.kt:354`, `VueKanban.kt:162`) : plus de redite ; le tableau de la PR donne chaque phrase **composée
  telle qu'elle s'affiche**, préfixe compris (carte, tournée refusée, illisible) ;
- `ServiceGnssRtk.kt:84` (gestionnaire d'exceptions) : rien n'y réessaie ; phrase générique qui dit quoi faire
  (par exemple « arrêter puis relancer le récepteur »), la promesse « réessaie » seulement là où la boucle retente ;
- mineurs : imports au lieu des noms complets ; la phrase de dictée ne renvoie pas à « Paramètres » pour le micro
  (l'autorisation se règle dans Android) ; liens vers la fiche et le run de CI dans le corps.

Hors périmètre (bac à sable) : après « Libérer le récepteur », un message « liaison coupée » peut rester affiché alors que
le service est arrêté (`ServiceGnssRtk.kt:141`, non vérifié sur appareil).

Complément du 2026-11-02 : tests du `testeur` (c4f02a9, 10 de plus, 7 rouges sur 8d1d52e). Décision de Pascal sur
`recepteur_aucun_point` (« Aucun point de montage (caster injoignable ?) », `strings_recepteur.xml:28`) : « Le
réécrire » : phrase française qui dit quoi faire, sans « caster » ni « injoignable » ; le `dev` propose la phrase exacte.

## Reprise, aller-retour 2/3 (2026-11-02)

Relecture de la #146 (698556c) : « prête pour Pascal », trois questions de sens. Décisions de Pascal du 2026-11-02 :

1. **Refus « pas un GeoPackage »** : « Réécrire » : plus de redite, de double « : » ni de majuscule après le préfixe.
   Proposition du relecteur : « ce n'est pas une tournée de sommieR : l'exporter de nouveau depuis sommieR » ; le `dev`
   propose la phrase exacte, composée avec chaque préfixe (« Illisible : », « Carte impossible : », « Tournée refusée : »).
2. **Carte, service du récepteur interrompu** : « Dans la 082 » : la carte ne promet plus « nouvelle tentative » quand
   rien ne retente (`ErreurRecepteurCarte.pour`, `enMarche` resté vrai après le gestionnaire d'exceptions) ; elle dit la
   phrase de l'Essai GNSS (« Le service du récepteur GNSS s'est interrompu : … »).
3. **« travaux.gpkg » à l'écran** : « Admis, phrase corrigée » : le nom du fichier reste ; la phrase d'`ImportTournee.kt:77`
   devient juste (« dossier », pas « fichier ») et dit quoi faire, par exemple « aucun fichier travaux.gpkg dans ce
   dossier : choisir le dossier de la tournée exportée par sommieR ». Elle entre au tableau de la PR.

Mineurs : ponctuation finale homogène ; imports en ordre ; alias `DETAIL_OUVERTURE` / `DETAIL_LECTURE` retirés.
Hors 082 : le libellé « Caster » de Paramètres (bac à sable).

Ordre : `testeur` d'abord (phrase composée sans double « : » ni majuscule après le préfixe ; carte après interruption du
service ; refus « aucun travaux.gpkg » qui dit quoi faire), puis `dev`.
