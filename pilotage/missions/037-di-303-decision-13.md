# Mission 037 — DI-303 suit la décision 13

> Fiche écrite par l'orchestrateur le 2026-10-23, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Décidée par Pascal le 2026-10-21
> (décision 13, `pilotage/journal.md:114`) ; préalable à la 013 (fiche 013, l. 59-61) ;
> lancée au cycle 9 (« on continue », Pascal, 2026-10-23).

## Mission

**En une phrase (quoi, et pourquoi) :** Retirer de DI-303 (`fixtures/dictee/modele.json`) le champ `hypothese` et citer à sa place la décision 13 de Pascal comme source, sans lire de code, pour que la 013 n'hérite d'aucune règle supposée.

**Redite par l'agent :** retirer le champ `hypothese` de DI-303 et citer la décision 13 de Pascal (2026-10-21, journal l. 114) dans sa `source`, sans toucher à l'entrée ni à l'attendu.

## Vérifié à la source

Constats vérifiés par l'orchestrateur sur `main` (82be444), le 2026-10-23.

| Constat | Lu à | Source du constat |
|---|---|---|
| Texte de la décision 13 : « l'empreinte SHA-256 du modèle est comparée sans tenir compte de la casse (DI-303) » | `pilotage/journal.md:114` | orchestrateur |
| DI-303 porte encore `hypothese` (« Règle supposée, non écrite dans l'ADR 0004 […] À confirmer par Pascal. ») ; c'est le seul cas de `fixtures/dictee/` qui en porte | `fixtures/dictee/modele.json:47-58` ; `grep -c '"hypothese"' fixtures/dictee/*.json` | orchestrateur |
| `hypothese` est facultatif ; il énonce une règle supposée, non écrite, à confirmer par Pascal | `fixtures/README.md:108` | orchestrateur |
| Le test de forme contrôle `hypothese` et les sources « à confirmer » | `core/src/test/kotlin/io/github/pobsteta/surculus/core/dictee/DicteeFixturesBienFormeesTest.kt:236-245` | orchestrateur |
| La 013 demande ce retrait avant de partir | `pilotage/missions/013-dictee-vosk.md:59-61` | orchestrateur |

Règles, avec leur auteur :
- **Décision 13** : Pascal, 2026-10-21 (journal l. 114).
- **Le testeur écrit les tests à partir de la spec, sans lire le code.** CLAUDE.md.
- **Garde-fou = test vu rouge** et **bilan des écarts avant rendu** : Pascal, bilan du 2026-10-21.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : format des cas et des sources de `fixtures/README.md` | |
| A-t-il les moyens ? | oui : tout est sur `main` | |

**Temps de la main :** **appuyer** : l'agent montre le cas DI-303 avant et après dans son rapport.

## L'Homme

- **Agent :** `testeur` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :**
  - changer l'entrée ou l'attendu de DI-303, ou tout autre cas ;
  - écrire la décision 13 dans l'ADR 0004 ou la spec (s'il juge qu'elle y manque, il le dit à l'orchestrateur) ;
  - aucun code de production.

## Harnais

- **Peut :** modifier les champs `hypothese` et `source` de DI-303 seulement ; adapter `fixtures/README.md` s'il cite DI-303 comme exemple d'hypothèse.
- **Ne doit pas :** lire `core/src/main/` ni `app/src/` ; retirer `@Disabled` de `DicteeRejeuTest` ; toucher à `main`.
- **Sources autorisées :** `pilotage/journal.md:114` ; `docs/adr/0004-stack-kotlin-compose-vosk.md` ; `docs/spec.md` § 4.3 ; `fixtures/README.md`.

## Tests

- `DicteeFixturesBienFormeesTest` reste vert ; **vu rouge** une fois (par exemple `hypothese: true` sur DI-303, non commité).
- Plus aucun `"hypothese"` dans `fixtures/dictee/`.
- Commande du projet : `.claude/test-command`.
- **Bilan des écarts** : le rapport liste chaque fichier modifié.

## Terrain

- Sans objet : aucune règle ne change, la décision 13 est déjà prise.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.
- **Point de suivi :** à l'ouverture de la PR.

## Comment saura-t-on que ça marche ?

La 013 branche DI-303 sans le modifier, et il passe au vert.
