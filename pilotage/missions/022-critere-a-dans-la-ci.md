# Mission 022 — Export de Surculus dans la CI d'import sommieR

> Fiche écrite par l'orchestrateur le 2026-10-17, à partir du gabarit
> `imperatrice/skills/mission/fiche-mission.md`. Décidée par Pascal à la relecture de la PR #34 (006) :
> sans elle, le critère (a) de l'ADR 0007 reste « non mesuré » après la fusion de la 006.

## Mission

**En une phrase (quoi, et pourquoi) :** Brancher `./gradlew :gpkg:exporterScenario` (tournée Couchey, `fixtures/export/scenario-couchey.json`) dans `.github/workflows/import-sommier.yml`, pour que la CI rejoue `sommier_importer_qfield_travaux()` sur un vrai export de Surculus, avec et sans ses six couches, et mesure enfin le critère (a) de l'ADR 0007 au lieu de l'annoncer « non mesuré ».

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : `ci/sommier/README.md`, ADR 0007 (critère de CI), commande donnée par la 006 | |
| A-t-il les moyens ? | oui : workflow de la 007 et outil `exporterScenario` de la 006, tous deux sur `main` | |

**Temps de la main :** **appuyer** : l'agent rend son plan avant d'écrire.

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** modifier `:gpkg` ou le scénario ; modifier sommieR.

## Harnais

- **Peut :** modifier `import-sommier.yml` et `ci/sommier/` ; installer le JDK dans ce travail.
- **Ne doit pas :** affaiblir un contrôle existant (auto-test du retrait, critère (b), « import qui n'écrit rien = écart ») ; toucher à `main` ; épingler une action autrement que par SHA.

## Tests

- Le travail « import sommieR » passe sans l'avertissement « critère (a) non mesuré » ; un export privé d'une saisie (ou dont une couche ajoutée change le bilan) le fait échouer, démontré une fois dans la PR.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Le résumé du job sur `main` affiche un critère (a) mesuré et vert.
