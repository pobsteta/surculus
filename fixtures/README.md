# Fixtures de Surculus

Cas d'acceptation de la tranche 1, écrits par le `testeur` à partir de `docs/spec.md`,
des ADR 0002, 0003 et 0005 et de `docs/referentiels/`, sans lire le code. Chaque cas
porte sa **source**. Les règles sur les plants sont dans `regles/plants/` (rejouées en R
par sommieR, brief `vers-sommier`).

## Format

Un fichier = une règle ou une famille de règles :

```json
{
  "fichier": "...", "regle": "...",
  "cas": [
    {"id": "R3", "description": "...", "entree": {...}, "attendu": {...},
     "source": "où la valeur attendue a été prise"}
  ]
}
```

Champs facultatifs d'un cas : `tolerance_proposee` (tolérance à confirmer par Pascal),
`question_ouverte`, `a_recouper_avec_circe_pascal`, `note`, `limite`.

## Unités

Degrés décimaux, ordre (latitude, longitude) pour le GNSS ; (x = E, y = N) en mètres
pour EPSG:2154 ; distances horizontales ; azimuts en degrés [0, 360[ ou grades [0, 400[
selon l'unité déclarée à l'ouverture de la tournée ; pente en `pct` ou `deg` selon
l'unité déclarée une fois par tournée.

## Provenance des valeurs

* Convergence, conversion WGS 84 vers EPSG:2154 : pyproj (version notée dans la source).
* Déclinaison : calculateur NOAA WMM2025, relevée dans `docs/referentiels/modele-geomagnetique.md`.
* Jour civil : fuseau Europe/Paris (tzdata), changement d'heure du 25 octobre 2026.
* Numérotation : cas copiés de Groma (`fixtures/numerotation/`, commit 6f953e1), sans modification.
* Les listes d'états, de causes et d'essences ne sont pas copiées : les tests les lisent dans `docs/referentiels/`.
