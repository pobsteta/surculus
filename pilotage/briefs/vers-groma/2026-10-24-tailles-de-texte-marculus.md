# Brief `surculus` → `groma` — les tailles de texte de Marculus

> **Statut** : ouvert, 2026-10-24 · **Émis par** : session `surculus` (orchestrateur), sur
> décision de Pascal du 2026-10-24 · **Urgence** : faible.

## Décision de Pascal

« Je veux dans groma et Surculus les tailles de police de Marculus »

## Ce que l'orchestrateur de Surculus a lu dans Groma (à vérifier par la session `groma`)

1. Groma suit déjà Marculus pour l'essentiel : aucune `Typography` propre, ses textes sont en
   styles de `MaterialTheme.typography` (`bodySmall` 23 fois, `bodyMedium` 8, `titleLarge` 6,
   `titleMedium` 5, `bodyLarge` 3, `labelLarge` 2), comme Marculus (BOM Compose 2026.02.01).
2. Il reste cinq tailles écrites à la main, lues le 2026-10-24 sur la copie locale :
   - `android/app/src/main/kotlin/groma/app/ui/EcranCarte.kt` l. 368-369 (boutons de zoom),
     l. 398 (`11.sp`, blanc) et l. 406 (`10.sp`) ;
   - `android/app/src/main/kotlin/groma/app/ui/Gnss.kt` l. 64 (`12.sp`, badge du fix).

## Ce qui est demandé

Remplacer ces cinq tailles par le style que Marculus emploie pour le même genre de texte, ou
dire pourquoi l'une d'elles doit rester (par exemple un texte dessiné sur la carte).

## Critère d'acceptation

Il ne reste dans `android/app/src/main/kotlin` aucun `fontSize =` sans raison écrite à côté.

## Côté Surculus

Mission 042 (`pilotage/missions/042-typographie.md`) : mêmes styles et mêmes tailles que
Marculus, sans `Typography` propre.
