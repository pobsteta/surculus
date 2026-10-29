# Mission 059 — L'Ortho chantier dit sa source et sa date

> Fiche écrite par l'orchestrateur le 2026-10-29 (cycle 15), à partir du gabarit
> `imperatrice/skills/mission/fiche-mission.md`, sur la décision de Pascal du 2026-10-27
> (« 2 oui », journal ; ADR 0009, amendement du 2026-10-27, l. 188-189) et son choix du cycle 15.
> Depuis `main`. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Afficher sur la carte la source et la date de l'Ortho chantier
quand elle est le fond choisi, et dire « ortho illisible » quand son fichier ne s'ouvre pas, pour respecter
la Licence Ouverte 2.0 et pour que le forestier sache de quand date l'image qu'il regarde et pourquoi elle
manque.

**Redite par l'agent :** (à remplir par l'agent avant d'agir)

## Ce qui est établi

- ADR 0009 l. 44 : « La Licence Ouverte 2.0 exige la source et une date. » ; l. 188-189 : la source et la date
  de l'Ortho chantier s'affichent (Licence Ouverte, mission 059).
- Spec § (import, l. 105-112) : l'Ortho chantier est `ortho.mbtiles` ou `ortho.gpkg` en EPSG:3857, livré par
  sommieR. La carte affiche déjà « © IGN Géoplateforme, Licence Ouverte Etalab 2.0 » pour les fonds en ligne
  (capture v0.7.83).

## Ce qui est à faire

1. Lire la source et la date dans les métadonnées du fichier (MBTiles : table `metadata`, clés
   `attribution`, `description`, date éventuelle ; GeoPackage : `gpkg_contents.last_change`, description).
   Dire dans la PR ce que porte réellement le fichier d'essai `couchey-demo-avec-ortho.zip`, et ce que sommieR
   écrit (lire sommieR sans le modifier).
2. Afficher sous la carte, à la place de l'attribution actuelle quand l'Ortho chantier est le fond :
   source + date (ex. « Ortho chantier · © IGN, BD ORTHO® · prise de vue 2024 · Licence Ouverte 2.0 »).
   **Si la date n'est pas dans le fichier : ne pas l'inventer** ; écrire ce qui est sûr et poser la question
   de sens à Pascal (texte à afficher, et brief éventuel vers sommieR).
3. Fichier présent mais illisible : message « ortho illisible » visible sur la carte (formulation à proposer,
   en français, sans code technique), repli sur un autre fond selon la règle de l'ADR 0009 sans planter.
4. Tests d'abord, vus rouges : métadonnées lues sur un MBTiles et un GeoPackage de test ; attribution
   construite ; fichier corrompu → état « illisible », pas d'exception.

**Temps de la main :** **appuyer**.

## L'Homme

- **Agent :** `dev`.
- **Ce qui ne lui est pas demandé :** changer le choix du fond par défaut, le téléchargement des tuiles en
  ligne, sommieR, `fixtures/` (sauf un fichier de test neuf sous `src/test`).

## Harnais

- **Peut :** modifier la carte et la lecture de l'ortho dans `:app` / `:core`, leurs tests ; pousser sur
  `mission/059-ortho-sourcee` ; ouvrir la PR.
- **Ne doit pas :** modifier `docs/`, `pilotage/` (sauf la redite), `fixtures/` ; toucher à `main` ; inventer
  une date ou une source ; trancher une question de sens.

## Tests

- `.claude/test-command` au code 0 ; nouveaux tests vus **rouges** avant la correction.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. Critère d'arrêt : 3 allers-retours. **Fusion : autorisation de
  Pascal.**

## Comment saura-t-on que ça marche ?

Sur la release, tournée `couchey-demo-avec-ortho` : la carte sur l'Ortho chantier montre source et date ;
capture de l'orchestrateur.
