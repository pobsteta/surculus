# Mission 085 — Ouvrir un contrôle sans position : ADR, spec et formats

> Fiche écrite par l'orchestrateur le 2026-11-02, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> après le changement de cap de Pascal du 2026-11-02, la critique du `contradicteur` et les réponses de Pascal (journal
> du 2026-11-02, branche `pilotage/cycle-17`). Depuis `main`. Une mission = une phrase = une branche = une PR. Le code
> suivra dans la mission 086 (`testeur` puis `dev`), qui part de cette PR fusionnée.

## Mission

**En une phrase (quoi, et pourquoi) :** Écrire dans l'ADR 0003, la spec, les formats et le brief `vers-sommier` la
règle décidée par Pascal pour ouvrir un contrôle sans aucune position GNSS sur le code lu du piquet, pour que la mission
086 et tout successeur codent une règle unique, sourcée et testable.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui (critique du contradicteur, décisions de Pascal ci-dessous) | |
| A-t-il les moyens ? | oui | |

**Temps de la main :** tenir : la règle touche la justesse des données et le format du journal.

## Ma réponse d'abord

_Rempli par Pascal seul._

## L'Homme

- **Agent :** `archiviste`.
- **Modèle :** opus (justesse des données, formats).
- **Ce qui ne lui est pas demandé :** écrire du code, des tests ou des fixtures (mission 086).

## La règle décidée par Pascal (2026-11-02)

1. **Sans aucun fix** reçu pendant **30 s** (valeur provisoire, mesurée à l'essai en forêt), récepteur actif,
   localisation et permission accordées, Surculus propose « Ouvrir sans position ». Localisation coupée ou permission
   refusée : il demande de l'activer, pas d'ouverture sans position.
2. Le contrôle s'ouvre seulement avec un **code lu** sur le piquet qui existe dans la tournée ; **sans code, rien ne
   s'ouvre**.
3. **Code présent dans plusieurs UG** : l'agent **choisit l'UG** (« P01 : UG 12 ou UG 14 ? »), avec ou sans position
   (corrige aussi le cas avec position, aujourd'hui `firstOrNull` dans `core/.../placette/Controle.kt` l. 55).
4. Avant d'ouvrir sans position, Surculus **affiche la placette à confirmer** (« P35-01 · UG 12 · parcelle 4 · 25
   plants pointés : ouvrir sans position ? »).
5. Le contrôle **garde la position de la placette créée par sommieR** (« par défaut si pas de position, on garde cette
   position », Pascal) ; pas de position mesurée.
6. Un fix reçu, **même unique, médiocre ou simulé**, reste une position et suit la règle actuelle (15 m, ou code lu
   qui l'emporte). **Hors de France : refusé**, avec une phrase qui dit quoi faire (« position aberrante : couper puis
   rallumer la localisation »), le refus écrit au journal.
7. **Export** : dans `positions_controles`, une ligne par contrôle, géométrie **NULL** (pas un POINT EMPTY),
   `precision_m` NULL, nouvelle colonne `sans_position_mesuree` (booléen), `rattache_par_identite` vrai (définition
   élargie : le code lu a désigné la placette, la position mesurée ne l'a pas fait) ; `controles` porte la position du
   piquet (l'import 0.31.1 lit `placette_uuid` et jette cette géométrie, `R/travaux_qfield.R` l. 296-305).
8. **Journal des saisies** : **format v2**, qui admet l'absence de position mesurée quand `sans_position_mesuree` est
   vrai ; Surculus relit toujours le v1 (principe N-1, comme `.sursync`).
9. L'**installation d'une placette** exige toujours une position (inchangé).

## Harnais

- **Peut :** amender `docs/adr/0003-gnss-et-reperes.md` (amendement daté, contexte « règle des 15 m de l'import »
  corrigé, crible, valeurs sacrifiées, tests attendus) ; `docs/spec.md` (§ 4.3 arrivée, § 5 `positions_controles`,
  table des tests) ; `docs/formats/journal-valeurs-v1.md` et un nouveau `docs/formats/journal-valeurs-v2.md` ; le brief
  `pilotage/briefs/vers-sommier/2026-10-22-position-mesuree-du-controle.md` (le compléter : certains contrôles n'auront
  pas de position mesurée) ; ajouter à cette fiche la liste des fichiers de code, fixtures et tests que la 086 devra
  toucher (relevé du contradicteur : `Couches.kt` l. 112-120, `fixtures/acceptation-export/schema-couches-ajoutees.json`,
  `fixtures/regles/rattachement-15m.json`, CI F4).
- **Ne doit pas :** changer la règle (toute question de sens remonte à l'orchestrateur) ; écrire du code ; toucher à
  `main`.
- **Sources autorisées :** ADR 0003, spec, formats, brief cité, sommieR dans `~/dev/sommieR` (lecture), conventions
  citées par le contradicteur (Darwin Core `coordinateUncertaintyInMeters`, `georeferenceSources` ; QField
  `@position_horizontal_accuracy` ; GeoPackage 1.3 sur les points vides).

## Valeur provisoire (chiffre, réglage)

- **Valeur provisoire :** 30 s d'attente sans fix avant « Ouvrir sans position ».
- **Mesurée par :** Pascal, à l'essai en forêt sous couvert (premier fix à froid du récepteur), date à fixer.
- **Gel :** avant la publication de la tranche 1 ; marquée « provisoire » dans l'ADR et la spec.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** aucune ; la 086 dépend de celle-ci.
- **Ordre de fusion :** 085 → 086.
- **CI rouge attendue :** aucune (documentation).

## Tests

- Pas de code ; la PR liste les tests d'acceptation que la 086 devra rendre verts (aucun fix + code ; code en double ;
  fix hors de France + code refusé ; localisation coupée ; un seul fix ; confirmation ; journal v2 relu après
  redémarrage ; v1 toujours relu ; export à géométrie NULL).

## Terrain

- **Échantillon que Pascal vérifiera :** l'amendement de l'ADR, lu en une minute.

## Qui veille, qui tranche

- **Relecteur :** `relecteur` (fidélité aux décisions, sources), puis Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Un successeur qui lit l'ADR 0003 amendé et la spec sait, sans le journal, ce que fait Surculus sans position.
