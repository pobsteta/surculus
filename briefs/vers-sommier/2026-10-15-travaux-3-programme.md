# Brief — Travaux, lot 3 : le programme prévu, venu du plan d'action de nemetonshiny

> **Gelé** (orchestrateur, 2026-10-15) : ne pas déposer avant que la correspondance des codes de sommieR (nomenclature interne, `docs/referentiels/correspondance-sommier.md`) et la table des types d'action soient revues avec Pascal.


*Établi le 15 octobre 2026 depuis le dépôt `surculus`, à partir de sommieR
0.31.1 (`fd92078`) et de nemetonshiny (`9144139`). Va avec le brief
`vers-nemetonshiny/2026-10-15-plan-action-vers-surculus.md`. À déposer dans
`specs/` de sommieR sous le nom `brief_travaux-3_programme.md`.*

## Pourquoi ce lot existe

Le brief `travaux-1` a laissé ouverte la question du « programme
prévisionnel » : `prevu` est un fait constaté à la réception, mais rien ne
dit à l'agent, sur le terrain, ce qui était prévu. Or ce programme existe
déjà : c'est le **plan d'action par UGF de nemetonshiny**
(`data/action_plan.json`), avec ses types (`plantation`, `regeneration`,
`protection`, `cloisonnement`, `depressage`, `entretien`, `desserte`…), ses
années cibles et ses statuts (*proposée*, *validée*, *planifiée*,
*réalisée*, *abandonnée*).

Ce lot fait entrer ce programme dans la tournée de travaux, comme un décor
hors chaîne, et relie chaque intervention réalisée à l'action qu'elle
exécute.

## Ce que la vérification a établi

- **nemetonshiny sait déjà exporter son plan** :
  `export_action_plan_gpkg()` écrit une couche `actions` (une ligne par
  action, géométrie de son UGF, `annee_civile` calculée) et une couche
  `ugf`. Les identifiants d'UGF (`ug_id`) sont ceux de nemetonshiny, pas
  les UUID d'UG de sommieR.
- **Son export vers Marculus exclut volontairement la plantation**
  (`MARCULUS_CONTEXT_ACTION_TYPES`) : rien n'y est désigné tige par tige.
  C'est la place de Surculus.
- **sommieR n'a ni programme ni lien vers une action** : `registre6_travaux()`
  porte `prevu` et `motif_ecart`, pas d'identifiant d'action.

## Décisions de conception

1. **Le programme est un décor, hors chaîne**, comme le contour d'une
   détection : il aide à proposer, il ne prouve rien. Seule l'intervention
   réalisée entre au registre.
2. **Une intervention peut citer son action** : `registre6_travaux()` gagne
   `action_id` (texte, l'identifiant d'action de nemetonshiny). Avec
   `action_id`, `prevu` vaut `prevu` par défaut.
3. **Les UG se rattachent par la géométrie** : une action est versée dans
   l'UG de sommieR qui contient le point intérieur de son UGF ; une action
   dont l'UGF chevauche plusieurs UG est versée dans chacune, avec la part
   de surface.
4. **Correspondance des types** : une table `SOMMIER_TYPES_ACTION` propose
   le code de travaux par défaut (`plantation` → PL, `regeneration` → RN,
   `protection` → PI, `cloisonnement` → CL, `depressage` → NT, `entretien`
   → DG, `desserte` → DS) ; l'agent peut en choisir un autre.
5. **Le retour vers nemetonshiny passe par un fichier**, pas par la base :
   sommieR écrit la liste des actions réalisées ; nemetonshiny la lit et
   fait passer les statuts, avec son audit.

## Livrables

- `sommier_projet_qfield_travaux(..., programme = NULL)` : `programme` est
  le GeoPackage écrit par `export_action_plan_gpkg()` ; seules les actions
  *validées* ou *planifiées* des types de `SOMMIER_TYPES_ACTION` sont
  versées, dans une couche `programme` (MULTIPOLYGON : `action_id`, `type`,
  `code_propose`, `annee_civile`, `surface_ha`, `ug`, `commentaire`).
- `registre6_travaux(action_id = NULL)` ; couches `travaux_*` du projet
  QField : champ `action_id` ; registre 6 en version suivante.
- `sommier_actions_realisees(con, foret_id, chemin)` : écrit un CSV
  `action_id, travaux_id, code_travaux, date_reception, quantite, unite`
  pour nemetonshiny.
- Bilan de gestion : les hectares prévus au plan et réalisés, par année,
  à côté du graphique prévu / reporté / non prévu.

## Critères d'acceptation

- Un plan exporté par nemetonshiny verse ses actions de plantation dans la
  tournée ; une action *proposée* ou *abandonnée* n'y est pas.
- Une plantation relevée sur une action importée porte son `action_id`, et
  `sommier_actions_realisees()` la rend.
- Une tournée sans programme se prépare et s'importe comme en 0.31.1.

## Questions ouvertes

- Une action réalisée en plusieurs fois (deux tranches de plantation) :
  plusieurs `travaux` pour un même `action_id`, et c'est nemetonshiny qui
  décide quand l'action est *réalisée* ?
- Les UGF de nemetonshiny et les UG de sommieR divergent-elles assez souvent
  pour justifier une table de correspondance plutôt qu'un rattachement
  géométrique ?
