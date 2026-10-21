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

## Reprise après la relecture de la PR #39 (2026-10-20)

Verdict du `relecteur` : **à reprendre**. Le critère (a) est mesuré et vert (2 contrôles,
P35-01 et P35-02, « Conforme »), aucun contrôle n'est affaibli, mais l'échec n'a jamais été
montré.

**Erreur de la fiche, reconnue par l'orchestrateur :** « un export privé d'une saisie le fait
échouer » est faux : le témoin « sans » est le même export privé de ses six couches, donc
les deux bilans perdent la même saisie et le job reste vert.

**Décision de Pascal (2026-10-20) : montrer les deux échecs**, chacun par un commit
temporaire poussé sur la PR, puis retiré ; le lien de chaque run rouge est écrit dans la
description de la PR :
- (i) **scénario sans aucune saisie** : l'import n'écrit rien, donc écart ;
- (ii) **une couche ajoutée qui modifie une couche de sommieR**, donc le bilan : le critère (a)
  diffère entre « avec » et « sans ».

**À corriger aussi :**
- `ci/sommier/README.md`, point 4 : retirer la phrase sur le `::warning::` qui n'existe plus ;
- résumé du job : n'afficher que les compteurs (travaux, placettes, contrôles, déjà
  présents, entrées écrites), le bilan entier reste dans l'artefact.

**Hors mission (ne pas toucher) :** la doc d'`ExporterScenario.kt` (l. 51) cite
`tournee-sans` au lieu de `tournee-depart` ; elle sera corrigée à part.

**Rappel de méthode :** le temps de la main est « appuyer » : rendre en quelques lignes le
plan des deux démonstrations avant de pousser.
