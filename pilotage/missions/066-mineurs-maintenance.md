# Mission 066 — Mineurs et maintenance

> Fiche écrite par l'orchestrateur le 2026-10-28 (cycle 12), à partir du gabarit
> `imperatrice/skills/mission/fiche-mission.md`, sur décision de Pascal : « Mineurs et
> maintenance ». Depuis `main` (fbc2f93). Une mission = une phrase = une branche = une PR.
> Les numéros de ligne viennent du kanban et peuvent avoir bougé : l'agent les vérifie.

## Mission

**En une phrase (quoi, et pourquoi) :** Corriger les commentaires, la documentation de test et le
test sans date relevés aux relectures précédentes, sans changer le comportement de l'application,
pour que le successeur ne lise rien de faux dans le code.

**Redite par l'agent :** Corriger sans toucher au comportement quatre choses fausses ou incomplètes (KDoc de `Navigation.kt`, commentaire « ECART terrain-voix », doc d'`ExporterScenario.kt`, test de date incohérente sans date) pour que le successeur ne lise rien de faux.

## Ce qui est à faire

1. **KDoc périmés de `app/.../Navigation.kt`** (relevés l. 36 et 61) : vérifier chaque KDoc contre
   le code actuel (fichiers et lignes cités, comportement décrit) et corriger ceux qui sont faux.
   Ne pas citer de numéro de ligne d'un autre fichier quand un nom de fonction suffit.
2. **Commentaire « ECART terrain-voix »** de
   `core/src/test/.../dictee/InstallationModeleTest.kt` (l. 86, « Brief vers `terrain` : à venir ») :
   le faire pointer vers pobsteta/terrain#23 (décision de Pascal, 2026-10-24).
3. **Doc d'`ExporterScenario.kt`** (`gpkg/src/test/.../outil/`, l. 54) : elle cite
   `ci/sommier/sortie/tournee-sans` ; vérifier dans `ci/` et le workflow d'import le nom réel du
   dossier (relevé : `tournee-depart`) et corriger la doc **seulement** si elle est fausse.
4. **Test « plantation plus âgée que l'itinéraire » sans date** : trouver le test (règle « date
   incohérente », spec § 6 et `fixtures/`), et dire dans la PR ce qui lui manque. Si la date peut
   être ajoutée sans modifier `fixtures/` ni une règle, l'ajouter ; sinon, ne rien changer et le
   dire (la règle elle-même est en tranche 3, Pascal, PR #106).

Chaque point déjà corrigé dans `main` est signalé dans la PR comme « déjà fait », sans changement.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | |
| A-t-il les moyens ? | oui | |

**Temps de la main :** **appuyer**.

## L'Homme

- **Agent :** `dev`.
- **Ce qui ne lui est pas demandé :** changer un comportement, une règle, un écran ; la release
  v0.7.58 restée en brouillon (orchestrateur) ; toucher à la dictée ou au mode poche (#114, D2).

## Harnais

- **Peut :** modifier commentaires, KDoc, doc de test et le test du point 4 ; pousser sur
  `mission/066-mineurs-maintenance` ; ouvrir la PR.
- **Ne doit pas :** modifier `fixtures/`, `docs/`, `pilotage/journal.md` ; toucher à `main` ;
  changer le code de production hors commentaires ; trancher une question de sens.

## Tests

- `.claude/test-command` au code 0. Pas de nouveau comportement, donc pas de « vu rouge » exigé,
  sauf au point 4 si un test est complété (le voir échouer une fois).

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. Critère d'arrêt : 3 allers-retours.
- **Qui tranche :** Pascal. **Fusion : autorisation à demander à Pascal.**

## Comment saura-t-on que ça marche ?

La PR ne change que des commentaires et un test ; chaque point relevé au kanban est fait ou dit
« déjà fait » ; la carte « Mineurs » et la carte « Maintenance » quittent le kanban.
