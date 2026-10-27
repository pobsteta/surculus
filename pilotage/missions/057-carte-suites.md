# Mission 057 — Suites de la carte : marqueur hors de France, « Ma position », Ortho chantier par défaut

> Fiche écrite par l'orchestrateur le 2026-10-27, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Décisions de Pascal du 2026-10-27
> (« 1 oui, 2 oui, 3 oui, 4 oui, 5 oui »), sur les questions de la relecture de la PR #97 (055).
> **Lancement :** le 2026-10-27, depuis `main` (52b457e, 055 fusionnée).

## Mission

**En une phrase (quoi, et pourquoi) :** Sur la carte, ne plus dessiner le marqueur de position quand le fix tombe hors de France, retirer le bouton « Ma position » qui double le bouton recentrer, et ouvrir la carte sur l'Ortho chantier quand la tournée en a une (sinon le Plan IGN), pour que le forestier voie une carte juste et utilisable même sans réseau.

**Redite par l'agent :** Sur la carte, ne plus dessiner le marqueur de position quand le fix tombe hors de France, retirer le bouton « Ma position » (doublon du recentrage) et ouvrir sur l'Ortho chantier quand la tournée en a une (le fond choisi à la main l'emporte), sinon sur le Plan IGN.

## Vérifié à la source

| Constat | Lu à | Source du constat |
|---|---|---|
| Le marqueur de position est dessiné au fix même hors de France ; le message `carte_hors_france` est affiché | `carte/EcranCarte.kt` l. 341, 410 (`main` 52b457e) | relecteur, PR #97 |
| « Ma position » (bouton du bas, 64 dp) et le bouton recentrer passent tous deux par `Recentrage.cible` | `EcranCarte.kt` l. 412-417, 441-449 ; `core/.../carte/FondsCarte.kt` l. 125-139 | relecteur, PR #97 |
| Fond par défaut : Plan IGN, comme Marculus ; l'Ortho chantier n'est jamais mémorisée | `EcranCarte.kt` ; `docs/adr/0009-carte-hors-ligne.md`, amendement du 2026-10-27 | relecteur, PR #97 |

Règles, avec leur auteur (Pascal, 2026-10-27) :
- **Marqueur masqué quand le fix est hors de France** (`Guidage.versLambert93 == null`) ; le message rouge reste.
- **« Ma position » retiré** ; le bouton recentrer (`MyLocation`) reste le seul ; « Guider » et le reste de la carte ne bougent pas.
- **Ortho chantier par défaut quand le fichier de la tournée existe**, sinon Plan IGN ; le choix fait à la main reste mémorisé comme aujourd'hui pour les autres fonds. L'écart avec Marculus est dit dans la PR.
- **Aucun `fontSize`**, cibles d'au moins 48 dp, `BandeauCarteGuidageTest`, `RecentrageTest`, `EmpriseCarteTest` verts.
- **Garde-fou = test vu rouge** et **bilan des écarts avant rendu** : Pascal, bilan du 2026-10-21.

**Temps de la main :** **appuyer**. Fusion autorisée par Pascal si relecture prête et CI verte (« j'autorise tout »).

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent. L'orchestrateur ne le remplit pas._

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** effacer les anciennes tuiles (058) ; le guidage ; la plaque blanche.

## Harnais

- **Peut :** modifier `carte/EcranCarte.kt`, `core/.../carte/FondsCarte.kt`, les chaînes, les tests.
- **Ne doit pas :** toucher à `main` ; modifier `fixtures/`, `docs/` ou `pilotage/journal.md` ; ajouter une dépendance.

## Tests

- JVM : fond par défaut (fichier présent → Ortho chantier, absent → Plan IGN, choix mémorisé respecté) ; marqueur non affiché pour un fix en Somalie ; plus de « Ma position » (lecture des sources). **Vu rouge** une fois.
- Commande du projet : `.claude/test-command`, au code 0.

## Terrain

- **Échantillon que Pascal vérifiera :** la release qui suit la fusion : une tournée avec `ortho.mbtiles` s'ouvre sur l'Ortho chantier ; plus de bouton « Ma position » ; en mode avion, la carte reste lisible.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal. Critère d'arrêt : 3 allers-retours.

## Comment saura-t-on que ça marche ?

En forêt, sans réseau, Pascal ouvre une tournée avec ortho et voit l'ortho tout de suite.
