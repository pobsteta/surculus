# Mission 064 — La carte utilisable en paysage

> Fiche écrite par l'orchestrateur le 2026-10-28, à partir du gabarit
> `imperatrice/skills/mission/fiche-mission.md`. Décision de Pascal du 2026-10-28 (« Adapter au
> paysage », puis « lance la mission paysage en parallèle »), après la capture de la v0.7.67 en
> paysage : la colonne de boutons de la carte est coupée (maison tronquée, cible absente).
> **Lancement :** le 2026-10-28, depuis `main` (6bcb50d). Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Adapter la mise en page de la carte (et du guidage s'il a
le même défaut) à l'écran couché, pour que les six boutons (Contrôler, naviguer vers, +, −, maison,
cible), le bandeau, la légende et la ligne de la placette restent tous visibles et touchables en
paysage, sans bloquer l'application en portrait.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Vérifié à la source

| Constat | Lu à | Source |
|---|---|---|
| En paysage (2316 × 1080, 450 dpi), la colonne de boutons déborde : maison coupée, cible invisible | captures `scratchpad/captures/v067-carte-sombre.png` et `v067-carte-clair.png`, v0.7.67 | orchestrateur |
| Six boutons flottants de 48 dp et plus, en une colonne à droite | `carte/EcranCarte.kt` (`BandeauCarteGuidageTest` : six boutons) | relecteur de la #108 |

Règles :
- **Pas de blocage en portrait** (Pascal, 2026-10-28) : l'écran suit la rotation.
- **En portrait, rien ne change.**
- Cibles d'au moins 48 dp ; styles de texte de Marculus (aucun `fontSize`) ; couleurs de la 063.
- Avant d'inventer : regarder comment Marculus et Groma tiennent leurs boutons de carte en paysage
  (lecture seule, `fichier:ligne` dans la PR). S'ils ne le font pas, choisir la solution la plus
  simple (deux colonnes, ou boutons répartis) et la dire dans la PR.
- Vérifier aussi en paysage l'écran du contrôle (012-B1) et l'écran Placettes ; corriger seulement
  ce qui est coupé, le dire.

**Temps de la main :** **appuyer** (code direct ; un choix de mise en page qui change ce que voit le
forestier en portrait remonte avant d'être codé).

## L'Homme

- **Agent :** `afficheur` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** le pointage, la boussole, le plan des plants (012-B2, en
  cours en parallèle : **ne pas toucher aux fichiers de `controle/`** sauf pour un débordement en
  paysage, et le dire, pour limiter les conflits).

## Harnais

- **Peut :** `carte/EcranCarte.kt`, `carte/EcranGuidage.kt`, leurs tests ; ajustements de mise en
  page minimes ailleurs si un écran est coupé en paysage.
- **Ne doit pas :** bloquer l'orientation ; changer le portrait ; toucher à `main` ; modifier
  `fixtures/` ou `pilotage/journal.md` ; trancher une question de sens.

## Tests

- Fonction pure de disposition (hauteur disponible → une ou deux colonnes, ou équivalent) testée
  pour le portrait et le paysage du téléphone de Pascal (1080 × 2316 px, 450 dpi) et pour un petit
  écran ; les six boutons tiennent dans la hauteur visible ; cibles ≥ 48 dp. **Vu rouge** une fois.
- `.claude/test-command` au code 0.

## Terrain

- **Échantillon :** release après fusion ; captures en paysage et en portrait de la carte (et du
  guidage), sur le téléphone de Pascal.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Fusion : autorisation à demander à Pascal.**

## Comment saura-t-on que ça marche ?

Téléphone couché, Pascal voit et touche les six boutons de la carte.
