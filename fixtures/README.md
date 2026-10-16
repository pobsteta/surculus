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

Champs facultatifs d'un cas :

* `tolerance_proposee` : la tolérance de `attendu` (`tolerance_deg`, `tolerance_m`) est une proposition, à confirmer par Pascal.
* `a_recouper_avec_circe_pascal` : valeur de convergence à recouper avec Circé (IGN), par Pascal.
* `note`, `limite` : précisions (par exemple une valeur reprise d'un autre document et non recalculée).
* `copie_de_groma` : cas copié sans modification de `fixtures/numerotation/` de Groma ; la source donne le fichier et le commit.
* `adapte_de_groma` : cas adapté d'un cas de Groma (ici : sans masquage).
* `verifier_que_XYZ_est_absent_de` : le test vérifie que le code inventé « XYZ » est absent du fichier de référentiel indiqué.

Champs facultatifs d'un fichier :

* `unites` : unités des entrées du fichier.
* `tolerance_proposee_deg` : tolérance proposée valable pour tout le fichier.
* `non_reprises` : cas de Groma écartés, avec la raison.
* `decision` : arbitrage de Pascal appliqué au fichier (ici : pas de masquage pour les plants).
* `convention_xy` : convention des coordonnées locales (x vers l'est, y vers le nord géographique, mètres depuis le piquet).

Dans `plants/numerotation.json`, la liste `arbres` de Groma est **renommée `plants`** et placée dans `entree`, avec `parametres`.
Un azimut saisi s'écrit toujours `azimut_mag` et `unite_azimut` (`deg` ou `gr`), comme la couche `plants` (spec § 5).

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

## Note pour sommieR (rejeu en R)

* `h_moy_cm` est arrondie à l'entier le plus proche, **demi vers le haut** (T9c : 40 et 45 cm donnent 43). `round()` de R arrondit au pair (`round(42.5)` donne 42) : utiliser `floor(x + 0.5)` pour rejouer ce cas.
* Un cas qui attend `{"refuse": true}` ou `{"accepte": false}` doit être refusé ; le libellé du refus n'est pas fixé.
