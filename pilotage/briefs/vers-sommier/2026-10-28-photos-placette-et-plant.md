# Brief `surculus` → `sommieR` — photos de placette et de plant

> **Statut** : brouillon, 2026-10-28, à envoyer après la validation de l'amendement de la spec
> par Pascal (porte n° 1) · **Émis par** : session `surculus` (orchestrateur) · **Urgence** : faible.

## Décisions de Pascal (2026-10-28)

« Placette + plant au besoin » ; « on part sur une photo nord en haut, ajouter les métadonnées
sur l'exif, GNSS du téléphone actif » ; « Le fix en cours de Surculus » ; « Repère de boussole,
sans blocage ».

## Ce que Surculus rendra dans la tournée

1. **Sans rien changer à sommieR** : chaque photo est une ligne de la couche `photos`
   (`uuid`, `constat_uuid`, `fichier`, `prise_le`) dont le `constat_uuid` est l'`uuid` du
   contrôle (`controles`). Lu dans `R/travaux_qfield.R` (copie locale, commit f0229b9) :
   `controler_tournee_travaux` vérifie déjà la présence des photos d'un contrôle
   (`controler_photos_constat`) et `photo(k$uuid)` les dépose avec le contrôle. À vérifier
   par la session `sommieR`.
2. **Couche ajoutée** `photos_terrain` (ignorée par l'import 0.31.1, comme les autres couches
   ajoutées, ADR 0007) : sujet (`placette` ou `plant`), `plant_uuid`, azimut de visée et sa
   référence, précision, source GNSS, âge du fix, empreinte SHA-256, position du fix.
3. **EXIF** de chaque fichier : position WGS 84 et heure UTC du fix, précision, azimut de visée.

## Ce qui est demandé

- Confirmer que l'import 0.31.1 accepte plusieurs photos par contrôle et des fichiers JPEG avec
  EXIF GPS sous `DCIM/` (extension reconnue par `SOMMIER_TYPES_PHOTO`).
- Dire si sommieR veut lire `photos_terrain` (rattacher une photo à un plant, montrer la photo de
  placette au passage suivant dans la tournée d'aller), et sous quel nom de couche ou de champ.

## Critère d'acceptation

Une tournée de démonstration avec une photo de placette et une photo de plant passe
`sommier_importer_qfield_travaux()` sans faute, et les deux photos sont déposées avec leur contrôle.
