# Mission 060 — Le bandeau de la carte doit se voir

> Fiche écrite par l'orchestrateur le 2026-10-27, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Signalé par Pascal le 2026-10-27 sur la
> v0.7.57 : « dans la vue carte il n'y a pas le bandeau en haut avec le bouton de choix du fond
> de carte ». Capture du téléphone de l'orchestrateur, même soir : la carte couvre tout le haut
> de l'écran, sans bandeau ni flèche ni icône « Couches ». Défaut de la mission 055, passé au
> travers des tests (lecture des sources), du `relecteur` et de l'orchestrateur.
> **Lancement :** après la fusion de la PR #101 (057, même écran).

## Mission

**En une phrase (quoi, et pourquoi) :** Faire que le bandeau de la carte (flèche de retour, titre, icône « Couches ») s'affiche réellement au-dessus de la carte, et prouver par un test qu'il ne peut plus être recouvert, pour que le forestier puisse revenir en arrière et changer de fond.

**Redite par l'agent :** _à remplir par l'agent avant d'agir._

## Vérifié à la source

| Constat | Lu à | Source du constat |
|---|---|---|
| Le code pose bien `BandeauCarte(...)` puis la carte dans une `Column`, la carte dans une `Box(Modifier.weight(1f))` avec `AndroidView(Modifier.fillMaxSize())` | `carte/EcranCarte.kt` l. 302-353 (`main` après la #97) | orchestrateur |
| Sur le téléphone de Pascal (v0.7.57), la carte occupe tout l'écran jusqu'en haut ; aucun bandeau visible | capture du 2026-10-27, `scratchpad/captures/surculus-carte.png` | orchestrateur |
| Hypothèse à vérifier, non prouvée : osmdroid `MapView` dessine au-delà de ses bords quand rien ne la limite (`clipToBounds` absent), et recouvre le bandeau | — | orchestrateur |

Règles :
- **Le bandeau se voit, au-dessus de la carte, en portrait et en paysage, plein écran ou non.**
- **Trouver la cause** et la dire dans la PR (citer la doc ou le code d'osmdroid / Compose) avant de corriger ; ne pas masquer le symptôme.
- **Même vérification sur le guidage** (bandeau de la 055).
- **Un garde-fou qui aurait vu le défaut** : si un test d'interface n'est pas possible sans émulateur, l'agent le dit et propose ce qui s'en approche le plus (au minimum un test qui exige `clipToBounds` ou l'équivalent retenu).
- **Garde-fou = test vu rouge** et **bilan des écarts avant rendu** : Pascal, bilan du 2026-10-21.

**Temps de la main :** **appuyer**. Fusion autorisée par Pascal si relecture prête et CI verte (« j'autorise tout ») ; vérification sur la release après fusion par l'orchestrateur (capture du téléphone) puis Pascal.

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** la navigation comme Groma (061) ; les boutons de la carte.

## Harnais

- **Peut :** modifier `carte/EcranCarte.kt`, `carte/EcranGuidage.kt`, `ui/BandeauCompact.kt` si la cause y est, les tests.
- **Ne doit pas :** toucher à `main` ; modifier `fixtures/`, `docs/` ou `pilotage/journal.md` ; ajouter une dépendance.

## Tests

- Le garde-fou ci-dessus ; `BandeauCarteGuidageTest` vert. **Vu rouge** une fois. `.claude/test-command` au code 0.

## Terrain

- **Échantillon :** la release qui suit la fusion, capture du téléphone sur la carte de Couchey : bandeau vert en haut, flèche, titre, icône « Couches ».

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. Critère d'arrêt : 3 allers-retours.

## Comment saura-t-on que ça marche ?

Sur la carte, Pascal voit le bandeau et ouvre le menu « Couches ».
