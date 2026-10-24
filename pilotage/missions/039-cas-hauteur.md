# Mission 039 — Cas de « hauteur » sans état et sur un plant mort

> Fiche écrite par l'orchestrateur le 2026-10-24, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Décidée par Pascal le 2026-10-24
> (« oui pour les 3 ») ; règles de Pascal du même jour (« oui, refus » ; « 1. oui »).
> Lancée après la fusion de la PR #76, qui écrit ces règles dans la spec § 4.3.

## Mission

**En une phrase (quoi, et pourquoi) :** Ajouter à `fixtures/dictee/contexte.json` les cas d'acceptation des deux règles de Pascal du 2026-10-24 sur « hauteur » (refusée sur un plant qui n'a pas encore d'état ; acceptée sur un plant mort), sans lire de code, pour que ces règles soient tenues par les cas du testeur et pas seulement par un test du `dev`.

**Redite par l'agent :** _à remplir par l'agent avant d'agir._

## Vérifié à la source

Constats vérifiés par l'orchestrateur le 2026-10-24.

| Constat | Lu à | Source du constat |
|---|---|---|
| Les deux règles, écrites dans la spec § 4.3 | `docs/spec.md` § 4.3 (après la PR #76) ; `pilotage/journal.md`, lignes « oui, refus » et « 1. oui » du 2026-10-24 | orchestrateur |
| Les cas de « hauteur » existants (DI-120 à DI-123) portent sur sain, atteint, absent et non observé, pas sur un plant mort ni sans état | `fixtures/dictee/contexte.json:161-215` ; relecture de la PR #75 | relecteur |
| Les cas de contexte vont jusqu'à DI-169 | `fixtures/dictee/contexte.json:597` | orchestrateur |
| `DicteeRejeuTest` est actif et rejoue tous les cas | PR #75 (fusionnée, afdca07) | orchestrateur |

Règles, avec leur auteur :
- **« hauteur » sans état refusée, la voix demande l'état d'abord ; acceptée sur un plant mort** : Pascal, 2026-10-24.
- **Le testeur écrit les tests à partir de la spec, sans lire le code.** CLAUDE.md.
- **Garde-fou = test vu rouge** et **bilan des écarts avant rendu** : Pascal, bilan du 2026-10-21.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : format des cas de `contexte.json` et de `fixtures/README.md` (section Dictée) | |
| A-t-il les moyens ? | oui, après la fusion de la #76 | |

**Temps de la main :** **appuyer** : l'agent montre la liste des cas (identifiant, état courant, phrase, attendu) dans son rapport.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent. L'orchestrateur ne le remplit pas._

## L'Homme

- **Agent :** `testeur` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :**
  - modifier un cas existant ;
  - trancher les mots exacts de la voix (« l'état d'abord » est l'intention ; le texte de l'annonce relève de la 012) ;
  - aucun code de production.

## Harnais

- **Peut :** ajouter des cas à `fixtures/dictee/contexte.json` (à partir de DI-170, chacun avec sa `source`) ; compléter `fixtures/README.md` (section Dictée) si un champ nouveau l'exige, en le disant.
- **Ne doit pas :** lire `core/src/main/` ni `app/src/` ; modifier un cas existant ou un test ; toucher à `main`.
- **Sources autorisées :** `docs/spec.md` § 4.3, § 5 et § 6 ; `pilotage/journal.md` (2026-10-16 et 2026-10-24) ; `fixtures/README.md`.

## Tests

- Au moins : « hauteur quarante-cinq » sur un plant sans état (refus) ; « hauteur quarante-cinq » sur un plant mort (accepté) ; et, pour borner, sur un plant mort avec cause.
- `DicteeRejeuTest` et `DicteeFixturesBienFormeesTest` restent verts. **Vu rouge** une fois (un attendu inversé, non commité).
- Commande du projet : `.claude/test-command`.
- **Bilan des écarts** : le rapport liste chaque fichier modifié et chaque cas ajouté. Si un cas nouveau est rouge contre le code de la 013, il le rapporte sans rien changer.

## Terrain

- Sans objet : la règle est décidée ; l'essai vocal relève de la 012.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours ; au-delà, la décision remonte à Pascal.
- **Point de suivi :** à l'ouverture de la PR.

## Comment saura-t-on que ça marche ?

Les nouveaux cas passent au vert dans `DicteeRejeuTest`, sans changer une ligne de code.
