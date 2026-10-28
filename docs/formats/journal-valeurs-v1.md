# Format de la `valeur` des lignes du journal, version 1

Ce que porte la colonne `valeur` des lignes que la session de contrôle écrit (mission 012-A, spec § 5, « Journal des
saisies »). Format ouvert : du JSON, lisible sans Surculus (test du successeur). C'est la note que la **mission
d'export** relit pour transformer le journal en couches `controles`, `plants` et `etats_plants` : elle n'a pas à
deviner une clé. Le code qui écrit et lit ces valeurs est unique : `core/.../core/session/Valeurs.kt`
(`FormatValeurs`, `ValeurControle`, `ValeurPointage`, `ValeurEtat`) ; ses tests rejouent chaque exemple ci-dessous.

La liste fermée des cinq types de ligne de la spec § 5 ne change pas : `controle`, `pointage`, `etat`, `annulation`,
`saut_horloge`. La session écrit les trois premiers et des `annulation` (sans `valeur`).

## Règles communes

* Chaque `valeur` est **un objet JSON** à plat (sauf `numeros`), clés dans l'ordre ci-dessous, sans espace : la même
  saisie donne le même texte au caractère près (une ligne reçue deux fois à l'identique est ignorée, spec § 5).
* `v` : version du format (entier, `1`).
* Une valeur illisible (JSON invalide, clé obligatoire absente ou mal typée) est **ignorée** par les lecteurs : jamais
  devinée, jamais complétée.
* Unités : distances **horizontales** en mètres ; azimuts en degrés [0, 360[, sauf `azimut_mag` qui est dans
  `unite_azimut` ; positions en **Lambert-93** (`x` = Est, `y` = Nord, mètres) ; instants en millisecondes depuis
  l'époque Unix, UTC (suffixe `_ms`) ; jours civils `AAAA-MM-JJ` en fuseau Europe/Paris.
* Les clés reprennent les noms de colonnes de la spec § 5 quand ils existent.
* `operateur` est celui de la tournée (`projet.operateur`), aussi porté par la colonne `operateur` de la ligne.

## `controle` (objet : l'UUID du contrôle)

Un contrôle par placette et par jour civil (spec § 4.3). Écrit à la première saisie du jour, **réécrit** (même objet,
`remplace_uuid` = la ligne précédente) quand l'agent termine la placette.

| Clé | Type | Sens |
|---|---|---|
| `v` | entier | `1` |
| `placette_uuid` | texte | `placettes.uuid` |
| `code_placette` | texte | code lu sur le piquet (`placettes.code_placette`) |
| `jour_civil` | texte | jour du contrôle, Europe/Paris |
| `operateur` | texte | `projet.operateur` |
| `termine` | booléen | `true` quand l'agent a terminé la placette (« Contrôlée ») |
| `visite_le_utc_ms` | entier | l'instant qui fonde `jour_civil` : l'heure GNSS si le fix a au plus 5 s, sinon l'heure du téléphone ; il ne peut pas contredire le jour |
| `position_x`, `position_y` | réel | position du contrôle : celle du piquet si rattaché par identité, la position mesurée sinon |
| `position_mesuree_x`, `position_mesuree_y` | réel | position GNSS mesurée (moyennée), toujours gardée à part |
| `rattache_par_identite` | booléen | vrai seulement si le code lu a rattaché le contrôle à un piquet à plus de 15 m |
| `repere_epsg` | entier | `2154` ou `9794`, celui du fichier de la tournée |
| `numeros` | objet | seulement si `termine` : UUID de plant vers numéro, **figés** (spec § 6) |

```json
{"v":1,"placette_uuid":"11111111-1111-4111-8111-111111111111","code_placette":"P35-01","jour_civil":"2026-10-27","operateur":"Agent d'essai","termine":true,"visite_le_utc_ms":1792000000000,"position_x":846164.76,"position_y":6686123.26,"position_mesuree_x":846164.76,"position_mesuree_y":6686123.26,"rattache_par_identite":false,"repere_epsg":2154,"numeros":{"019...a":1,"019...b":2}}
```

Une placette est « **Contrôlée** » quand un contrôle courant (non annulé) porte `termine: true`, pour toute la tournée.
Annuler la ligne de fin la rend « à faire » : la ligne précédente (`termine: false`) redevient courante.

## `pointage` (objet : l'UUID du plant)

L'observation brute et les termes dérivés (ADR 0003).

| Clé | Type | Sens |
|---|---|---|
| `v` | entier | `1` |
| `controle_uuid` | texte | le contrôle du jour |
| `placette_uuid` | texte | la placette |
| `distance_horizontale_m` | réel | distance horizontale du piquet au plant, **observation primaire** |
| `azimut_mag` | réel | azimut magnétique tel que lu, dans `unite_azimut`, **observation primaire** |
| `unite_azimut` | texte | `deg` ou `gr` (couche `plants.unite_azimut`) |
| `source_azimut` | texte | `boussole_telephone` ou `saisie` |
| `declinaison_deg`, `convergence_deg` | réel | termes dérivés enregistrés (WMM2025 ; convergence Lambert-93) |
| `azimut_geo_deg` | réel | azimut magnétique + déclinaison, [0, 360[ ; sert à la numérotation de Groma |
| `azimut_grille_deg` | réel | azimut géographique moins convergence, [0, 360[ |
| `jour_declinaison` | texte | jour civil de la déclinaison |
| `x`, `y` | réel | position du plant en Lambert-93 |
| `repere_epsg` | entier | `2154` ou `9794` |
| `essence` | texte ou `null` | code ONF de `essences.csv` |
| `remplace_uuid` | texte ou `null` | pour un regarni : le plant mort ou absent qu'il remplace (couche `plants.remplace_uuid`) ; à ne pas confondre avec la colonne `remplace_uuid` de la ligne du journal |
| `hors_rayon_confirme` | booléen | vrai si le plant est au-delà du rayon + 0,10 m et que l'agent l'a confirmé |
| `pointe_le_utc_ms` | entier | heure du téléphone au pointage |

Le **numéro** du plant n'est pas écrit dans le pointage : il est provisoire pendant le premier contrôle (recalculé par
la règle de Groma à chaque pointage), figé dans `controle.numeros` à « Terminer ». Un plant pointé après coup prend
le numéro suivant, sans renuméroter. Les plants d'un passage précédent gardent leur numéro (couche `plants` de sommieR).

## `etat` (objet : l'UUID de la ligne d'`etats_plants`)

Une ligne par (contrôle, plant). Corriger un état réécrit la même ligne (même objet, `remplace_uuid` de la ligne du
journal = la ligne corrigée).

| Clé | Type | Sens |
|---|---|---|
| `v` | entier | `1` |
| `controle_uuid` | texte | le contrôle du jour |
| `plant_uuid` | texte | le plant |
| `etat` | texte | `sain`, `atteint`, `mort`, `absent` (`etats-plants.yaml`), ou `non_observe` |
| `cause_groupe` | texte ou `null` | l'un des quatre groupes du DSF |
| `cause_detail` | texte ou `null` | détail libre ; seul « abroutissement » est reconnu |
| `hauteur_cm` | entier ou `null` | hauteur en cm, strictement positive |

Un plant `non_observe` ne porte ni cause, ni détail, ni hauteur et sort des totaux. Les totaux (`nb_total`,
`nb_vivants`, `nb_abroutis`, `h_moy_cm`) ne sont **jamais écrits** dans le journal : ils se déduisent des états
(`Totaux.deduire`).

## `annulation`

Pas de `valeur`. La colonne `remplace_uuid` désigne la ligne annulée ; l'objet est celui de cette ligne (spec § 5).
La session annule la dernière ligne annulable : un pointage, un état, ou la fin de la placette.

## Codes de refus

Un refus de la session porte un code stable (`CodesRefus`, `core/.../core/placette/Verdict.kt`), celui de la spec § 6 et
de l'export : `hors_referentiel` (état, cause ou essence hors des référentiels), `uuid_en_double`, `hors_france`,
`point_hors_foret`, et sinon le motif du domaine (`hors_15_m`, `code_inconnu`, `cause_sur_etat`, `hauteur_sur_absent`,
`plant_remplace_ni_mort_ni_absent`, `plant_deja_remplace`, `date_hors_validite`...). « vide » n'a pas de code : la
session répond « pas encore disponible » et ne trace rien.
