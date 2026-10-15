# Mission 006 — Module GeoPackage : lecture et export

> Fiche écrite par l'orchestrateur le 2026-10-15 (`/mission`, porte n° 2), à partir du
> gabarit `docs/modeles/fiche-mission.md`. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Écrire le module JVM `:gpkg` qui lit une tournée sommieR 0.31.1 et écrit l'export `travaux.gpkg` complété (couches de sommieR intactes, couches de plants conformes au standard et à l'extension Schema, version unique, repère lu dans le fichier, export déterministe), pour tenir le contrat avec sommieR.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? (sources, conventions nommées ci-dessous) | oui : ADR 0004, 0007 ; spec § 5 ; brief lot 2 ; sommieR `inst/qgis/travaux.gpkg` et `R/travaux_qfield.R` | |
| A-t-il les moyens ? (outils, données d'essai, tests) | oui : la démo Couchey de sommieR comme donnée d'essai | |

**Temps de la main :** **tenir** : le contrat avec sommieR est le cœur du projet.

## L'Homme

- **Agent :** `dev`
- **Modèle :** opus
- **Ce qui ne lui est pas demandé :** pas de journal (008), pas de CI R (007).

## Harnais

- **Peut :** utiliser geopackage-java et sqlite-jdbc ; lire `inst/qgis/travaux.gpkg` et la démo Couchey de sommieR.
- **Ne doit pas :** sortir du périmètre de cette phrase ; inventer un code (travaux, essence, état) ; toucher à `main` ; modifier sommieR, Marculus ou Groma ; trancher une question de sens (la remonter à l'orchestrateur).
- **Sources autorisées :** ADR 0004, 0007 ; `docs/spec.md` § 5 ; OGC 12-128 Req. 118-119 ; sommieR en lecture seule.

## Tests

- **Comment saura-t-on que c'est juste :** lecture de la démo Couchey ; schéma des couches de sommieR inchangé ; `gpkg_contents` liste toutes les couches ajoutées ; deux exports du même état identiques par contenu ; mélange 2154/9794 refusé. Commande du projet : `.claude/test-command`.

## Terrain

- **Échantillon que Pascal vérifiera :** Pascal ouvre l'export dans QGIS et voit placettes, plants et contrôles.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal.
- **Point de suivi :** à la remise du plan (missions tenues) et à l'ouverture de la PR.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

les tests `:gpkg:test` sont verts et QGIS ouvre l'export.
