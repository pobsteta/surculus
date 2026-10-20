# Mission 004 — Domaine du contrôle de placette

> Fiche écrite par l'orchestrateur le 2026-10-15 (`/mission`, porte n° 2), à partir du
> gabarit `imperatrice/skills/mission/fiche-mission.md`. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Écrire dans `:core` le domaine du contrôle de placette (plants, états et causes du DSF, non observé, regarnis liés au plant remplacé, numérotation par azimut géographique, totaux, règles du § 6), pour que les totaux rendus à sommieR découlent des plants et que les règles soient les mêmes en Kotlin et en R.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? (sources, conventions nommées ci-dessous) | oui : spec § 4.3, § 5, § 6 ; `etats-plants.yaml` ; `essences.csv` ; brief lot 2 | |
| A-t-il les moyens ? (outils, données d'essai, tests) | oui : tests de 002 | |

**Temps de la main :** **tenir** : modèle de données qui engage la durée ; plan relu avant code.

## L'Homme

- **Agent :** `dev`
- **Modèle :** opus (justesse des données)
- **Ce qui ne lui est pas demandé :** ni persistance, ni GeoPackage, ni écran.

## Harnais

- **Peut :** écrire les types et règles dans `:core` ; lire les fixtures de 002.
- **Ne doit pas :** sortir du périmètre de cette phrase ; inventer un code (travaux, essence, état) ; toucher à `main` ; modifier sommieR, Marculus ou Groma ; trancher une question de sens (la remonter à l'orchestrateur).
- **Sources autorisées :** `docs/spec.md` § 4.3, 5, 6 ; `docs/referentiels/etats-plants.yaml`, `essences.csv` ; `pilotage/briefs/vers-sommier/2026-10-15-travaux-2-plants.md` ; Groma `Numerotation.kt`.

## Tests

- **Comment saura-t-on que c'est juste :** tests de 002 verts ; couverture des règles du § 6 marquées tranche 1. Commande du projet : `.claude/test-command`.

## Terrain

- **Échantillon que Pascal vérifiera :** Pascal fait le décompte d'une placette papier de 12 plants et le compare aux totaux calculés.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal.
- **Point de suivi :** à la remise du plan (missions tenues) et à l'ouverture de la PR.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

les fixtures de règles passent.
