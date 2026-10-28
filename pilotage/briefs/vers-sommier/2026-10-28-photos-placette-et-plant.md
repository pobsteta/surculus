# Brief `surculus` → `sommieR` — photos de placette et de plant

> **Statut** : brouillon, 2026-10-28, à envoyer après la validation de l'amendement de la spec
> par Pascal (porte n° 1) · **Émis par** : session `surculus` (orchestrateur) · **Urgence** : faible.

## Décisions de Pascal (2026-10-28)

« Placette + plant au besoin » ; « on part sur une phot nord en haut, ajouter les metadonnées
sur l'exif, GNSS du téléphone actif » ; « Le fix en cours de Surculus » ; « Repère de boussole,
sans blocage » ; puis, après critique du `contradicteur` : « Placette au sud, plant vu d'en
haut », « À l'agent, au passage suivant », « Proposée à chaque contrôle », « Le repère réel »,
« EXIF magnétique, flèche géographique », « 2048 px, JPEG 85 », « Format v2, export sans
blocage ».

## Ce que Surculus rendra dans la tournée

1. **Sans rien changer à sommieR** : chaque photo est une ligne de la couche `photos`
   (`uuid`, `constat_uuid`, `fichier`, `prise_le`) dont le `constat_uuid` est l'`uuid` du
   contrôle (`controles`). Lu dans `R/travaux_qfield.R` (copie locale, commit f0229b9) :
   `controler_tournee_travaux` vérifie déjà la présence des photos d'un contrôle
   (`controler_photos_constat`) et `photo(k$uuid)` les dépose avec le contrôle. À vérifier
   par la session `sommieR`.
2. **Couche ajoutée** `photos_terrain` (ignorée par l'import 0.31.1, comme les autres couches
   ajoutées, ADR 0007) : ses champs sont ceux de `docs/spec.md` § 5 (sujet, placette, plant,
   azimut de visée magnétique, déclinaison, modèle, précision, source et référentiel du GNSS,
   âge du fix, position simulée, empreinte SHA-256, position du fix).
3. **Fichiers** : JPEG seulement, `DCIM/<photo_uuid>.jpg`, 2048 px de côté long. **EXIF** :
   position dans le repère réel de la source (`GPSMapDatum` « ETRS89 » en RTK Centipède,
   « WGS-84 » en GNSS autonome), heure UTC du fix, `GPSHPositioningError`, azimut de visée
   magnétique (`GPSImgDirectionRef = M`), `DateTimeOriginal` à l'heure de Paris avec
   `OffsetTimeOriginal`. Aucune position quand le fix a plus de 5 s, est simulé ou tombe hors de France.
4. **Garanties de l'export** : jamais de ligne de `photos` sans `constat_uuid`, ni de photo d'un
   contrôle non exporté ou annulé (lu dans `R/qfield.R` l. 629-647 : une ligne orpheline fait
   refuser toute la tournée, une photo d'un contrôle absent est perdue sans message). Les lignes de
   `photos` versées par sommieR à l'aller restent telles quelles ; les photos reprises de la
   tournée précédente ne sont jamais réexportées.

## Ce qui est demandé

- Confirmer que l'import 0.31.1 accepte plusieurs photos par contrôle et des fichiers JPEG avec
  EXIF GPS sous `DCIM/` (extension reconnue par `SOMMIER_TYPES_PHOTO`).
- **Photos à l'aller** : verser dans la tournée d'aller les photos du contrôle précédent de chaque
  placette (fichiers dans `DCIM/`, couches `photos` et `photos_terrain`), pour que l'agent les
  voie au passage suivant. En attendant, Surculus reprend celles de la tournée rendue précédente
  (ADR 0007, amendée le 2026-10-28).
- Lire `GPSMapDatum`, `GPSHPositioningError` et `OffsetTimeOriginal` (`R/photos.R` ne lit
  aujourd'hui que `DateTimeOriginal`, latitude et longitude) ; ne pas tenir une position ETRS89
  pour du WGS 84.
- Ajouter `photos_terrain` au lot 2 (`2026-10-15-travaux-2-plants.md`).

## Critère d'acceptation

Une tournée de démonstration avec une photo de placette et une photo de plant passe
`sommier_importer_qfield_travaux()` sans faute, et les deux photos sont déposées avec leur contrôle.
