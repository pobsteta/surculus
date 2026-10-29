# Mission 073 — Le journal refuse aussi `INSERT OR REPLACE`

> Fiche écrite par l'orchestrateur le 2026-10-29 (cycle 14), à partir du gabarit
> `imperatrice/skills/mission/fiche-mission.md`, sur le brief de Ramulus du 2026-10-28
> (`~/dev/briefs/vers-surculus/2026-10-28-ramulus-insert-or-replace.md`) et l'ordre de Pascal du
> 2026-10-28 (« Code les briefs si il y en a qui ne sont pas codés »). Depuis `main` (afcdbd1). Une
> mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Ajouter aux tables en ajout seul du journal un déclencheur
`BEFORE INSERT` qui refuse une ligne dont l'`uuid` existe déjà, avec son test, pour qu'un
`INSERT OR REPLACE` ne puisse plus effacer une saisie du forestier sans passer par le déclencheur
`BEFORE DELETE`.

**Redite par l'agent :** (à remplir par l'agent avant d'agir)

## Ce qui est constaté

- Ramulus (PR n° 11 de Ramulus, `RamulusDatabase.kt`) : avec `recursive_triggers` à OFF, réglage par
  défaut de SQLite, `INSERT OR REPLACE` sur une clé présente efface l'ancienne ligne **sans** déclencher
  `BEFORE DELETE` (documentation SQLite, « The ON CONFLICT clause », REPLACE : « delete triggers fire if
  and only if recursive triggers are enabled »). Vérifié par Ramulus avec `sqlite3` et sous Robolectric.
- Surculus : `data/src/main/kotlin/io/github/pobsteta/surculus/data/journal/db/JournalDatabase.kt`,
  `DECLENCHEURS` (l. 105 à 114), ne pose que `BEFORE UPDATE` et `BEFORE DELETE` sur `journal` et
  `refus_journal`. Le DAO insère en `OnConflictStrategy.ABORT` (`JournalDao.kt` l. 15 et 18) : le trou
  n'est pas atteint par le code actuel, mais la seconde garde promise par l'ADR 0005 (« DAO et
  déclencheurs ») ne tient pas seule. Vérifié par l'orchestrateur à la lecture, pas par un test.

## Ce qui est à faire

1. **Test d'abord, vu rouge** : sur une base ouverte comme en production (`OUVERTURE`), insérer une
   ligne dans `journal`, puis exécuter en SQL brut un `INSERT OR REPLACE` de même `uuid` ; attendu : refus
   (`SQLiteConstraintException` ou équivalent), ligne d'origine intacte, compte inchangé. Même cas pour
   `refus_journal`. Vérifier dans le test que `PRAGMA recursive_triggers` vaut 0. Montrer le rouge avant
   la correction (sortie dans la PR).
2. Ajouter à `DECLENCHEURS` un `BEFORE INSERT … WHEN EXISTS (SELECT 1 FROM <table> WHERE uuid = NEW.uuid)
   BEGIN SELECT RAISE(ABORT, 'journal en ajout seul'); END` pour chaque table en ajout seul (vérifier la
   clé réelle de `refus_journal`). Recréés à chaque ouverture comme les autres (`IF NOT EXISTS`) : rien
   à migrer, mais le dire dans la PR. Pas de règle sur `projection` (reconstructible, réécrite en REPLACE).
3. Vérifier que l'insertion normale (`inserer`, `insererRefus`), la restauration `.sursync` et l'import
   restent verts.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui, Ramulus l'a fait | |
| A-t-il les moyens ? | oui (Robolectric déjà en place dans `:data`) | |

**Temps de la main :** **appuyer**.

## L'Homme

- **Agent :** `dev`.
- **Ce qui ne lui est pas demandé :** changer le schéma, la projection, le DAO ou la sauvegarde ;
  réécrire l'ADR 0005 (l'orchestrateur y ajoutera un renvoi daté si Pascal le veut).

## Harnais

- **Peut :** modifier `JournalDatabase.kt` et les tests de `:data` ; pousser sur
  `mission/073-journal-insert-or-replace` ; ouvrir la PR.
- **Ne doit pas :** modifier `fixtures/`, `docs/`, `pilotage/` (sauf la redite de cette fiche) ; toucher
  à `main` ; trancher une question de sens.

## Tests

- `.claude/test-command` au code 0 ; le nouveau test vu **rouge** sans le déclencheur, vert avec.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. Critère d'arrêt : 3 allers-retours.
- **Qui tranche :** Pascal. **Fusion : autorisation à demander à Pascal.**

## Comment saura-t-on que ça marche ?

Le test rouge puis vert dans la PR ; après fusion, le brief de Ramulus passe dans
`~/dev/briefs/traites/`. Pas d'écran : pas de capture.
