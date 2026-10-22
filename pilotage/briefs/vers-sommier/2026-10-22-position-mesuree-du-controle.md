# Brief — La position mesurée du contrôle de placette, jusqu'au registre

*Établi le 22 octobre 2026 depuis le dépôt `surculus`, à partir de sommieR
0.31.1 (`fd92078`). Complète le brief `2026-10-15-travaux-2-plants.md` (lot 2),
qui demandait déjà la couche `positions_controles`. Décision de Pascal du
2026-10-22 (mission 028, `pilotage/journal.md`). À déposer dans `specs/` de
sommieR sous le nom `brief_travaux-2b_position_controle.md`.*

## Le constat

Surculus mesure la position de chaque contrôle : la position GNSS moyennée,
sa précision, et le drapeau « rattaché par identité » quand l'agent a confirmé
le piquet hors des 15 m (`docs/spec.md` § 4.3 et l. 259, ADR 0003). Il l'écrit
dans la couche `positions_controles` de la tournée.

L'import de sommieR 0.31.1 ne la lit pas, et le registre ne peut pas la garder :

- `R/registre6.R:150-180` : `registre6_controle` n'a aucun champ de géométrie ;
- `R/travaux_qfield.R:262-277` : l'import appelle `sommier_controler_placette`
  sans position ;
- `inst/sql/003_geometrie.sql:26-31` : `entree_sommier.geom` ne vient que de
  `payload->'geometrie'`, donc reste nulle pour un contrôle ;
- `inst/sql/002_vues.sql:366-398` : `v_controle_plantation` n'expose aucune
  position ; `v_placette.geom` est le centre de la placette, pas la position
  mesurée.

## Ce que ça coûte

Le registre ne sait pas où le contrôle a réellement été fait. Un piquet
déplacé, un contrôle rattaché par identité à 20 m, ou une position perdue à
l'import ne se voient pas dans sommieR.

## Ce qui est demandé

1. Que `registre6_controle` accepte une `geometrie` (POINT, WGS 84 dans le
   payload, comme les autres entrées), avec `precision_m` et
   `rattache_par_identite`.
2. Que l'import de la tournée lise `positions_controles` (clé
   `controle_uuid`) et passe la position au contrôle.
3. Que `v_controle_plantation` expose cette position.

Le nom des champs et leur place dans le modèle relèvent de sommieR.

## Ce que Surculus fait en attendant

La CI d'import de Surculus (mission 028, critère F4) compare les totaux du
contrôle au registre, et la position mesurée à la couche
`positions_controles` de l'export, à 0,5 m près. Le résumé du job dit que la
position n'arrive pas au registre. Quand sommieR la gardera, F4 la comparera
au registre.
