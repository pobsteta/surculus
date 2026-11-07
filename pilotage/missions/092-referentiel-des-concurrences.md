# Mission 092 — Référentiel des niveaux de concurrence

> Fiche écrite par l'orchestrateur le 2026-11-04, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> sur décision de Pascal (plan de la 091, « Acquéreur d'abord ») : le domaine de la concurrence (faible, moyenne,
> forte) n'est dans aucun référentiel de `docs/referentiels/`, et la règle du projet interdit de l'inventer. Depuis
> `main`. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Écrire `docs/referentiels/concurrences.csv`, le domaine des niveaux de
concurrence d'une plantation que Surculus fera dire à la fin d'un contrôle, sourcé et comparé aux conventions publiées,
pour que la 091 reprenne un référentiel et n'invente aucun code.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase ? | oui | |
| Sait-il comment faire ? | oui : comme `etats-plants.yaml` et `seuils-densites.csv` (sources citées, CQQCOQP, plusieurs sources comparées) | appuyer |
| A-t-il les moyens ? | oui : sommieR v0.31.1 en lecture (`git -C ~/dev/sommieR show v0.31.1:R/registre6.R`, l. 62-64), la toile | |

**Temps de la main :** appuyer.

## L'Homme

- **Agent :** `acquereur` (seul dossier où il écrit : `docs/referentiels/`).
- **Modèle :** sonnet.

## Ce qui est attendu

- `docs/referentiels/concurrences.csv` : une ligne par niveau, avec au moins `code`, `libelle`, `definition` (ce qui
  distingue un niveau d'un autre sur le terrain, si une source le dit), `source`.
- **Point de départ obligatoire, puisque c'est le contrat** : sommieR v0.31.1, `SOMMIER_CONCURRENCES <- c("faible",
  "moyenne", "forte")` (`R/registre6.R` l. 64), que l'import valide (`valider_choix`). Les codes de Surculus doivent
  être exactement ceux-là, sinon l'import refuse.
- **Comparer** avec au moins deux conventions publiées (DSF, INRAE / RENFOR, guide « Réussir la plantation
  forestière », ONF, CNPF) : comment elles nomment et définissent la concurrence herbacée ou ligneuse au contrôle d'une
  plantation. Citer la page.
- Si une convention publiée définit d'autres niveaux ou d'autres critères : **ne pas trancher**, le dire dans le rapport
  (la question va à Pascal et, le cas échéant, à sommieR par un brief).
- Mettre à jour `docs/referentiels/README.md` (une ligne).

## Harnais

- **Peut :** écrire dans `docs/referentiels/` seulement ; lire sommieR, la spec, la toile.
- **Ne doit pas :** toucher au code, aux tests, à `main`.

## Dépend de / rouge attendu jusqu'à

- Aucune dépendance. La 091 l'attend pour son test d'égalité.

## Qui veille, qui tranche

- **Relecteur :** `relecteur` (sources vérifiables, codes égaux à ceux de sommieR), puis Pascal.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Le fichier existe, chaque ligne cite sa source, ses codes sont exactement ceux de sommieR v0.31.1, et la comparaison
avec les conventions publiées est écrite.
