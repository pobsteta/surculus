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
