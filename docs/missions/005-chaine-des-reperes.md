# Mission 005 — Chaîne des repères

> Fiche écrite par l'orchestrateur le 2026-10-15 (`/mission`, porte n° 2), à partir du
> gabarit `docs/modeles/fiche-mission.md`. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Écrire dans `:core` la chaîne des repères (unités d'azimut validées avant normalisation, déclinaison par portage Kotlin du code public NOAA sur `WMM.COF`, convergence des méridiens, point dérivé depuis le piquet, refus des dates hors validité et des fichiers qui mêlent 2154 et 9794), pour placer chaque plant sans erreur de nord.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? (sources, conventions nommées ci-dessous) | oui : ADR 0003 ; `modele-geomagnetique.md` ; `groma-core` (`Azimut.kt`, `Projections.kt`) | |
| A-t-il les moyens ? (outils, données d'essai, tests) | oui : fixtures sourcées de 002 | |

**Temps de la main :** **tenir** : justesse des données, erreur de signe invisible sur le terrain.

## L'Homme

- **Agent :** `dev`
- **Modèle :** opus
- **Ce qui ne lui est pas demandé :** pas de GNSS (003), pas d'écran.

## Harnais

- **Peut :** copier `Declinaison.kt` et `WMM2025.COF` de Groma avec attribution GPL (arbitrage de Pascal du 2026-10-16 : Groma porte déjà le WMM2025, testé contre les 100 valeurs NCEI), appliquer autour les bornes de Surculus (2025-01-01 au 2029-12-31), réutiliser la convergence copiée par la 003 ; pente et unités de tournée comprises.
- **Ne doit pas :** sortir du périmètre de cette phrase ; inventer un code (travaux, essence, état) ; toucher à `main` ; modifier sommieR, Marculus ou Groma ; trancher une question de sens (la remonter à l'orchestrateur).
- **Sources autorisées :** ADR 0003 ; `docs/referentiels/modele-geomagnetique.md`, `referentiel-centipede.md` ; NCEI WMM2025.

## Tests

- **Comment saura-t-on que c'est juste :** fixtures de repères de 002 vertes (Brest, Strasbourg, Couchey) ; grades et degrés hors plage refusés ; date hors validité refusée. Commande du projet : `.claude/test-command`.

## Terrain

- **Échantillon que Pascal vérifiera :** mesure à Couchey de l'azimut d'une base de 50 m entre deux points RTK (point au sol n° 2 de l'`acquereur`).

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal.
- **Point de suivi :** à la remise du plan (missions tenues) et à l'ouverture de la PR.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

la déclinaison calculée à Couchey vaut +2,80° au 2026-10-15 à 0,05° près, et l'essai de terrain concorde.
