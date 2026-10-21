# Mission 011 — Écrans : tournées, carte et guidage

> Fiche écrite par l'orchestrateur le 2026-10-15 (`/mission`, porte n° 2), à partir du
> gabarit `imperatrice/skills/mission/fiche-mission.md`. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Réaliser les écrans Tournées (ouvrir une tournée sommieR en dossier ou `.zip`), Carte (couches, ortho, badge du fix) et Guidage (vers la placette, signal sonore, cercle à 30 m, rattachement par identité), pour que l'agent arrive au piquet sans chercher.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? (sources, conventions nommées ci-dessous) | oui : spec § 4.1, 4.2, 4.3 (arrivée) ; ADR 0003 ; Marculus (contextes, carte) et Groma (guidage) en lecture seule | |
| A-t-il les moyens ? (outils, données d'essai, tests) | oui une fois 003, 005 et 006 fusionnées | |

**Temps de la main :** **appuyer**.

## L'Homme

- **Agent :** `afficheur`
- **Modèle :** sonnet
- **Ce qui ne lui est pas demandé :** pas le contrôle des plants (012).

## Harnais

- **Peut :** écrans Compose, osmdroid, réutilisation des écrans de Marculus.
- **Ne doit pas :** sortir du périmètre de cette phrase ; inventer un code (travaux, essence, état) ; toucher à `main` ; modifier sommieR, Marculus ou Groma ; trancher une question de sens (la remonter à l'orchestrateur).
- **Sources autorisées :** spec § 4.1-4.3 ; ADR 0003, 0004, **0009** (carte et ortho hors ligne) ; Groma (`TuilesHorsLigne.kt`, `TelechargementTuiles.kt`, `carte/Fonds.kt`, `groma-core/…/ZoneTuiles.kt`) en lecture seule.
- **Exigences de l'ADR 0009 :** ortho WMTS IGN préchargée à l'ouverture ; état de l'ortho visible dans la liste des tournées (complète, partielle n / N, absente) ; emprise testée en France métropolitaine ; attribution datée du téléchargement ; `ortho.mbtiles` ou `ortho.gpkg` en EPSG:3857 du dossier lu en priorité ; option « Wi-Fi seulement ».

## Tests

- **Comment saura-t-on que c'est juste :** tests d'interface sur la logique de guidage (distance, azimut, seuil de 30 m) ; refus et rattachement à 15 m. Commande du projet : `.claude/test-command`.

## Terrain

- **Échantillon que Pascal vérifiera :** Pascal retrouve un piquet de la démo Couchey ou de sa plantation d'essai.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal.
- **Point de suivi :** à la remise du plan (missions tenues) et à l'ouverture de la PR.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

on comprend en dix secondes où est la placette et à quelle distance.
