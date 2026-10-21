# Mission 026 — CI d'import : bilan, ajouts seuls, schéma inchangé

> Fiche écrite par l'orchestrateur le 2026-10-21, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Décidée par Pascal le 2026-10-21
> après la critique du `contradicteur` : le critère (a) de l'ADR 0007 (bilans « avec » et
> « sans » égaux) ne mesure presque rien, et deviendra faux au lot 2 de sommieR.
> Lancée après la fusion de la PR #39 (mission 022).

## Mission

**En une phrase (quoi, et pourquoi) :** Remplacer, dans la CI d'import sommieR, la comparaison « avec / sans » du critère (a) par trois contrôles qui mesurent vraiment l'export de Surculus : (F1) le bilan d'import égale le bilan d'export de Surculus, déjà présents compris ; (F2) les couches de sommieR de l'export ne diffèrent de la tournée de départ que par des ajouts, aux UUID attendus ; (F3) leur schéma complet (DDL de `sqlite_master`, déclencheurs, index, lignes de `gpkg_*`, `application_id`, `user_version`) est identique à celui de la tournée de départ. L'ADR 0007 est amendée en conséquence, pour que la donnée du forestier arrive au registre sans rien abîmer de la tournée.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : critique du `contradicteur` du 2026-10-21 (journal), `sqldiff --primarykey`, `ci/sommier/` | |
| A-t-il les moyens ? | oui une fois la PR #39 fusionnée (export de Surculus dans la CI) | |

**Temps de la main :** **appuyer** : l'agent rend son plan avant d'écrire.

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** le registre attendu et l'auto-test par sabotage (missions 027 et 028) ; modifier `:gpkg` (sauf écrire le bilan d'export en JSON s'il ne l'est pas déjà, à dire au plan) ; modifier sommieR.

## Harnais

- **Peut :** modifier `.github/workflows/import-sommier.yml`, `ci/sommier/`, `docs/adr/0007-*.md` (ligne « Critère de CI » et crible) ; retirer ou déclasser le témoin « sans » en vérification de fumée.
- **Ne doit pas :** affaiblir l'auto-test du retrait, le critère (b) ou la règle « un import qui n'écrit rien est un écart » ; épingler une action autrement que par SHA ; toucher à `main`.
- **Sources autorisées :** ADR 0007 ; critique du `contradicteur` (journal, 2026-10-21) ; sommieR `R/travaux_qfield.R` en lecture seule ; documentation de `sqldiff`.

## Tests

- Chacun des trois contrôles est montré rouge une fois dans la PR, par un commit temporaire puis retiré : F1 (une saisie de moins dans l'export que dans le bilan d'export), F2 (un `UPDATE` d'une ligne existante de `controles`), F3 (un `CREATE TRIGGER` sur `controles`).
- Le run final est vert, et le résumé du job affiche les trois contrôles.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Le résumé du job sur `main` montre F1, F2 et F3 verts, et chacun a été vu rouge une fois.

> **Note du 2026-10-21 (orchestrateur) :** F2 est comparé en R (RSQLite) et non avec `sqldiff`, absent du runner. Choix de moyen accepté, voir `pilotage/journal.md` à cette date.
