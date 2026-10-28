# Mission 012 — Écran du contrôle de placette

> Fiche écrite par l'orchestrateur le 2026-10-15 (`/mission`, porte n° 2), à partir du
> gabarit `imperatrice/skills/mission/fiche-mission.md`. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Réaliser l'écran du contrôle de placette (plan des plants, pointage relatif au piquet, saisie des états aux boutons de volume selon la table de la spec, mode poche, écran maintenu allumé, synthèse vocale et vibration), pour contrôler une placette sans regarder l'écran.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? (sources, conventions nommées ci-dessous) | oui : spec § 4.3 ; ADR 0004 ; Marculus (boutons de volume) en lecture seule | |
| A-t-il les moyens ? (outils, données d'essai, tests) | oui une fois 004, 005, 008, 029, 031 et 032 fusionnées (032 ne l'est pas encore) | |

**Temps de la main :** **appuyer**.

## L'Homme

- **Agent :** `afficheur`
- **Modèle :** sonnet
- **Ce qui ne lui est pas demandé :** pas le moteur de dictée lui-même (013), pas l'essai écran éteint (014).

## Harnais

- **Peut :** écran Compose et gestion des touches dans l'activité.
- **Ne doit pas :** sortir du périmètre de cette phrase ; inventer un code (travaux, essence, état) ; toucher à `main` ; modifier sommieR, Marculus ou Groma ; trancher une question de sens (la remonter à l'orchestrateur).
- **Sources autorisées :** spec § 4.3 ; ADR 0004 ; Marculus `MainActivity`.

## Tests

- **Comment saura-t-on que c'est juste :** table des boutons ; annulation par double appui sur volume bas ; mode poche ignore l'écran tactile. Commande du projet : `.claude/test-command`.
- **Codes de refus (décision de Pascal, 2026-10-17, relecture de la PR #34) :** un refus montré à l'agent porte les codes stables de la spec § 6 (`hors_referentiel`, `uuid_en_double`, `hors_france`, …), y compris pour les refus rendus par le domaine (`Observations.valider`, `Totaux.deduire`, `PointDerive.deriver`) ; la correspondance, aujourd'hui écrite seulement dans `gpkg/src/test/.../acceptation/BranchementExport.kt`, passe en code de production.

- **Tournée (journal du 2026-10-21) :** chaque saisie passe le code de la tournée ouverte. Le refus d'un code vide et le calcul de l'état par tournée relèvent de la mission 032 (domaine), qui est un prérequis. L'écran lit toujours l'état avec le code de la tournée ouverte : `etat(objet, type, codeTournee)` garde un défaut `""` (lignes non rattachées) pour les tests existants, et un oubli lirait silencieusement un autre état ; un test le prouve (relecture de la 032, 2026-10-21).

## Terrain

- **Échantillon que Pascal vérifiera :** Pascal contrôle une placette déjà pointée, gants aux mains, chronométrée (signe du brief).

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal.
- **Point de suivi :** à la remise du plan (missions tenues) et à l'ouverture de la PR.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

moins de 10 minutes en médiane sur 10 placettes déjà pointées (critère de la tranche 1).

## Périmètre élargi le 2026-10-24

Décision de Pascal (2026-10-24, « 2. oui », relecture de la PR #75) : la 012 **branche la
dictée** de la 013 sur l'écran du contrôle, car aucune autre mission ne la relie à l'interface.

- Brancher `MoteurDicteeVosk` et `AnalyseurDictee.appliquer` (mission 013, PR #75) sur le
  plant courant ; demande de `RECORD_AUDIO` à l'exécution, avec refus expliqué à l'écran
  et à la voix.
- **Refus dits à la voix** : chaque refus de `AnalyseurDictee.appliquer` est annoncé par la
  synthèse vocale, dont « hauteur » sans état (« l'état d'abord », décision de Pascal du
  2026-10-24, spec § 4.3).
- **Import du modèle** par fichier (`ModeleVoskImport`), depuis un bouton de l'application,
  sans réseau ; refus visible d'un fichier altéré.
- La phrase de mission sera réécrite au lancement, avec ce périmètre, et redite par
  l'agent.
- **Échantillon terrain ajouté** : dicter une placette en forêt, vent compris ; importer le
  zip officiel, puis un zip altéré, qui doit être refusé.

## Reprise au lancement, le 2026-10-27

Décisions de Pascal du 2026-10-27 : « 1 oui, 2 oui » : la 012 est **lancée sans attendre
l'essai de terrain de Couchey** (condition du bilan du 2026-10-21 levée), et le formulaire du
contrôle s'ouvre par **deux portes**.

**Phrase de mission réécrite :** Réaliser l'écran du contrôle de placette de la spec § 4.3
(plan des plants autour du piquet, pointage relatif, états et causes aux boutons de volume et
à la voix, regarnis, totaux, refus avec leurs codes, mode poche, écran maintenu allumé,
synthèse vocale et vibration), ouvert depuis la carte et à l'arrivée du guidage, et écrire
chaque saisie au journal de la tournée, pour que le forestier contrôle une placette sans
regarder l'écran et que la liste des placettes sache ce qui est fait.

**Entrées (deux portes, comme Groma) :**
- un bouton **« Contrôler »** sur la carte quand une placette est choisie (mission 061 : la
  placette choisie est affichée au bas de la carte) ;
- **ouverture automatique en arrivant** sur la placette pendant le guidage (moins de 30 m, le
  cercle de la placette de la spec § 4.2), avec possibilité de revenir au guidage.
- Retour : du contrôle vers la carte (ou le guidage), comme les autres retours de la 061.

**Ce qui a changé depuis l'écriture de la fiche (à respecter) :**
- **Import du modèle Vosk** : il est fait dans Paramètres (mission 048, PR #92) ; la 012 ne
  l'ajoute pas. Si le modèle manque, l'écran le dit et renvoie vers Paramètres → Dictée vocale.
- **Saisies au journal** : chaque saisie passe le code de la tournée ouverte (031, 032) ; la
  012 branche `DepotTournees.placettesControlees` (aujourd'hui vide, mission 061) pour que
  la placette passe « Contrôlée » dans l'écran « Placettes » et sorte des « à faire » ; le
  code de tournée reste le nom du dossier (hypothèse gardée jusqu'à la 012, décision du
  2026-10-27) : dis si le journal impose autre chose.
- **Sauvegarde** : déclencher la sauvegarde `.sursync` sur les saisies (dette de la 009 et de
  la 011, bac à sable du kanban).
- **Écran** : `BandeauCompact` (053) avec flèche de retour, styles de texte de Marculus (042,
  aucun `fontSize`), thème Système / Clair / Sombre (051), textes neutres sans plaque
  (062), cibles d'au moins 48 dp ; en mode poche, l'écran tactile est ignoré.
- **Récepteur GNSS** : il tourne déjà (démarré à la première carte, 061) ; le pointage
  utilise le fix existant, jamais une position hors de France (`Recentrage`, `Guidage`).

**Agent :** `afficheur` (sonnet). **Temps de la main :** appuyer : l'agent rend d'abord à
l'orchestrateur son **plan découpé** (écran, journal, voix, poche) ; si la mission est trop
grosse pour une seule PR, il le dit et propose un découpage, que Pascal tranche.

**Tests (en plus de ceux plus haut) :** les cas d'acceptation du testeur déjà écrits
(`fixtures/dictee/`, cas DI-…, `fixtures/export/scenario-couchey.json`) passent sans modifier
`fixtures/` ; la « date incohérente » (règle de `CLAUDE.md`) ; une saisie annulée ne rend pas
la placette « Contrôlée » ; vu rouge une fois.

**Fusion :** autorisation de Pascal à demander à la livraison (non couverte par « j'autorise
tout »). Vérification par capture du téléphone sur la release (règle du 2026-10-27).
