# Mission 062 — En thème sombre, pas de plaque blanche sous les textes neutres

> Fiche écrite par l'orchestrateur le 2026-10-27, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Demandée par Pascal le 2026-10-27, après
> les captures de la v0.7.62 : « dans la carte, dans le bandeau du bas où il est marqué
> "Placette P35-02 - 10.0 km" supprime le fond blanc et mets la police en Blanc quand on est en
> thème sombre ». Première pièce de la reprise de la plaque blanche de la 051 (« on verra plus
> tard »).
> **Lancement :** le 2026-10-27, depuis `main` (a456352).

## Mission

**En une phrase (quoi, et pourquoi) :** En thème sombre, retirer la plaque blanche (`plaqueEtat()`) sous les textes neutres, qui ne portent pas une couleur d'état, et les écrire dans la couleur du texte du thème (blanc en sombre), en commençant par la ligne de la placette choisie au bas de la carte, pour que le forestier les lise au lieu d'un gris clair sur blanc.

**Redite par l'agent :** En thème sombre, la ligne « Placette P35-01 · 10,0 km » de la carte et la ligne « n placettes · k à faire » de l'écran Placettes perdent leur plaque blanche et prennent la couleur de texte du thème (onSurface, blanc en sombre), pour que le forestier les lise ; rien d'autre ne change.

## Vérifié à la source

| Constat | Lu à | Source du constat |
|---|---|---|
| Ligne « Placette P35-01 · 10,0 km » au bas de la carte : texte gris clair sur la plaque blanche, presque illisible en sombre | capture du 2026-10-27, `scratchpad/captures/v062-carte.png` ; `carte/EcranCarte.kt` l. 533-539 (`main` a456352) | orchestrateur |
| Ligne « 3 placettes · 1 à faire » de l'écran « Placettes » : même défaut | capture `v062-placettes.png` ; `tournee/EcranPlacettes.kt` l. 94 | orchestrateur |
| `Modifier.plaqueEtat()` pose une plaque blanche en sombre seulement (051) | `carte/Palette.kt` l. 41 | orchestrateur |

Règles :
- **Ligne de la placette choisie (bas de la carte) : pas de fond blanc, texte blanc en thème sombre** : Pascal, 2026-10-27. En clair, rien ne change.
- **Même traitement pour « n placettes · k à faire »** : Pascal, 2026-10-27 : « idem dans l'affichage des placettes, dans le bandeau du haut "3 placettes - 1 à faire" supprimer le fond blanc et mettre la police en blanc ».
- **La couleur vient du thème** (`MaterialTheme.colorScheme.onSurface` ou `onBackground`), pas d'un blanc écrit en dur, pour que le clair reste juste.
- **Ne change pas** les textes en couleur d'état (légende, anomalies, alertes, verdicts du guidage, messages rouges) ni le radar : leur sort attend une décision de Pascal (suite de la plaque blanche).
- **Contraste** : le texte retenu fait au moins 4,5:1 sur le fond réel, en clair comme en sombre (`ContrasteTest` ou un test voisin).
- **Garde-fou = test vu rouge** et **bilan des écarts avant rendu** : Pascal, bilan du 2026-10-21.

**Temps de la main :** **lâcher**. Fusion selon l'autorisation de Pascal (relecture prête, CI verte) ; vérification par capture sur la release.

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** la légende, les pastilles, le radar, les messages en couleur d'état.

## Harnais

- **Peut :** modifier `carte/EcranCarte.kt` (ligne de la placette choisie), `tournee/EcranPlacettes.kt` (ligne du compte), les tests.
- **Ne doit pas :** toucher à `main` ; modifier `fixtures/`, `docs/` ou `pilotage/journal.md` ; changer une couleur du thème ou de la palette d'état.

## Tests

- Lecture des sources : les deux lignes n'utilisent plus `plaqueEtat()` et prennent leur couleur du thème ; contraste ≥ 4,5:1 en clair et en sombre. **Vu rouge** une fois. `.claude/test-command` au code 0.

## Terrain

- **Échantillon :** la release qui suit la fusion, téléphone en sombre : capture de la carte (ligne « Placette … » en blanc, sans fond blanc) et de l'écran « Placettes » ; en clair, inchangé.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. Critère d'arrêt : 3 allers-retours.

## Comment saura-t-on que ça marche ?

En sombre, Pascal lit d'un coup d'œil « Placette P35-02 · 10,0 km » en blanc sur la carte.
