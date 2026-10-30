# Mission 079 — README et spec au niveau de ce qui est livré

> Fiche écrite par l'orchestrateur le 2026-10-30, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> après le bilan du 2026-10-30 (test du successeur : non) et la validation de Pascal (porte n° 2, « Oui, les trois »).
> Depuis `main`. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Mettre `README.md` et `docs/spec.md` au niveau de ce qui est livré (contrôle de
placette 012, mode poche, volume bas tenu, micro, bandeau d'écoute, formats de distance, colonne de six boutons de carte),
pour qu'un successeur sache en une minute ce qu'est Surculus et où il en est.

**Redite par l'agent :** Je mets `README.md` et `docs/spec.md` (et la seule mention de `terrain-voix` dans `docs/architecture.md`) au niveau de ce qui est livré, chaque ajout daté et renvoyé à sa décision du journal ou à sa fiche, sans rien inventer, pour qu'un successeur sache en une minute ce qu'est Surculus et où il en est.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? (sources, conventions nommées ci-dessous) | oui | |
| A-t-il les moyens ? (outils, données d'essai, tests) | oui | |

**Temps de la main :** appuyer — documentation seule, chaque ajout renvoie à sa décision datée.

## Ma réponse d'abord

_Rempli par Pascal seul._

## L'Homme

- **Agent :** `archiviste`.
- **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** écrire ou modifier un ADR (0006, 0010, 0012 restent à Pascal) ; changer le brief ;
  toucher au code, aux tests, à `fixtures/`, à `pilotage/` (sauf la redite de cette fiche) ; décider d'un comportement
  que le journal ne tranche pas.

## Harnais

- **Peut :** modifier `README.md`, `docs/spec.md`, `docs/architecture.md` (seulement la mention de `terrain-voix` à côté
  de `groma-core`) ; pousser sur `mission/079-readme-et-spec-au-niveau-du-livre` ; ouvrir la PR.
- **Ne doit pas :** inventer une décision ; recopier le kanban dans le README (un lien suffit) ; effacer une décision
  ancienne de la spec : on la remplace par un ajout daté qui renvoie à la nouvelle ; toucher à `main`.
- **Sources autorisées :** `pilotage/journal.md` (décisions de Pascal et lignes `cap`), les fiches
  `pilotage/missions/012*` à `078*`, `docs/brief.md`, `docs/adr/`, le constat de l'`archiviste` au bilan du 2026-10-30
  (journal, même date).

## À faire (tiré du constat du bilan)

1. `README.md` l. 17-23 : la 012 est livrée ; liste des écrans remplacée par un lien vers le kanban ; dire où trouver
   la dernière release.
2. `docs/spec.md` l. 176-187 : gestes de volume « [proposé] » → ce qui est livré et décidé (volume bas tenu = écoute
   tant que tenu, mode poche ; 012-C1, 012-D2), avec la date de chaque décision de Pascal.
3. Spec : micro (072, 075), bandeau d'écoute qui s'efface après 6 s (076), formats de distance (074 : mètres entiers,
   U+202F, km à une décimale au-delà de 1 000 m, décimètre pour le pointage et le hors rayon), six boutons de carte en
   une colonne de 48 à 32 dp (078), « cliquez sur le bouton « Rattacher » » (077).
4. `docs/architecture.md` l. 21-23 : `terrain-voix` adopté (`docs/dependances.md` l. 19).

## Valeur provisoire (chiffre, réglage)

Aucune nouvelle. Les valeurs reprises (6 s, 48 à 32 dp) sont des décisions de Pascal datées ; les recopier avec leur date.

## Dépend de / rouge attendu jusqu'à

Aucune.

## Tests

- `.claude/test-command` au code 0 (aucun code changé) ; chaque ajout à la spec cite sa ligne de journal ou sa fiche.

## Terrain

- **Échantillon que Pascal vérifiera :** le test de la minute sur le README, et la section des gestes de la spec face
  au téléphone.

## Qui veille, qui tranche

- **Relecteur :** `relecteur` (contrôle de chaque chiffre et libellé contre le journal et le code), puis Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Point de suivi :** à la PR.
- **Qui tranche :** Pascal. **Fusion : autorisation à demander à Pascal.**

## Comment saura-t-on que ça marche ?

Au prochain bilan, l'`archiviste` refait le test de la minute : le README ne contredit plus le kanban, et la spec décrit
les gestes livrés.
