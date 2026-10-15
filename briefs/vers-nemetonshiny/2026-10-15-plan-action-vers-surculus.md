# Brief nemetonshiny — le plan d'action vers la tournée de travaux (Surculus)

*Établi le 15 octobre 2026 depuis le dépôt `surculus`, à partir de
nemetonshiny (`9144139`) et de sommieR 0.31.1 (`fd92078`). Va avec le brief
sommieR `travaux-3` (`briefs/vers-sommier/2026-10-15-travaux-3-programme.md`).*

## Objet

Surculus, l'application Android de la tournée de travaux, propose sur le
terrain les actions **prévues** au plan d'action d'un projet : plantation,
régénération, protection, cloisonnement, dépressage, entretien, desserte.
Les saisies vont à sommieR, qui les inscrit au registre 6. nemetonshiny
**fournit le prévu** et **reçoit le réalisé**, sans jamais recevoir les
saisies elles-mêmes.

C'est le pendant de l'export vers Marculus, qui exclut volontairement la
plantation (`MARCULUS_CONTEXT_ACTION_TYPES`) : ce que Marculus ne couvre
pas, Surculus le couvre.

## Ce qui existe

- `service_action_plan.R` : plan par UGF (`data/action_plan.json`), types
  `ACTION_PLAN_TYPES`, statuts `ACTION_PLAN_STATUTS` et leurs transitions
  (`is_valid_status_transition()`), audit de chaque changement.
- `export_action_plan_gpkg()` : couche `actions` (géométrie de l'UGF,
  `annee_civile`) et couche `ugf`. **C'est ce fichier que sommieR lira**
  (brief `travaux-3`) : aucun nouveau format à inventer.

## Demandé

1. **Un export « vers la tournée de travaux »** dans le module Plan
   d'action : le même `export_action_plan_gpkg()`, filtré sur les statuts
   *validée* et *planifiée* et sur les types de travaux (tout sauf
   `coupe_rase`, `eclaircie`, `observation`), nommé
   `<projet>-programme-travaux.gpkg`. Aucune logique nouvelle : un filtre
   et un bouton.
2. **Relire le réalisé** : importer le CSV écrit par
   `sommier_actions_realisees()` (`action_id, travaux_id, code_travaux,
   date_reception, quantite, unite`) et proposer, pour chaque action
   citée, le passage à *réalisée*, en respectant
   `is_valid_status_transition()` et en écrivant l'audit
   (`op = update`, utilisateur, date). Une action citée par plusieurs
   travaux est proposée une fois, avec la somme des quantités.
3. **Ne jamais passer une action à *réalisée* d'après le téléphone** : seul
   le fichier de sommieR fait foi, parce que seul sommieR a validé
   l'import.

## Tests

- L'export ne contient ni action *proposée* ni *abandonnée*, ni coupe.
- Le CSV d'exemple fait passer deux actions à *réalisée*, laisse intacte une
  action inconnue, et refuse une transition invalide (déjà *abandonnée*).
- Clés i18n FR/EN pour le bouton et les messages ; `testServer()` sur le
  module.

## Hors périmètre

Les saisies de terrain, les plants, les contrôles de placette : ils restent
dans sommieR. nemeton n'est pas concerné.
