# Mission 003 — Reprise provisoire du GNSS de Groma

> Fiche écrite par l'orchestrateur le 2026-10-15 (`/mission`, porte n° 2), à partir du
> gabarit `imperatrice/skills/mission/fiche-mission.md`. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Reprendre provisoirement dans Surculus, avec attribution GPL, le code GNSS de Groma (`groma-core` : NMEA, NTRIP, géodésie, azimuts ; application : transport Bluetooth et service RTK), avec horodatage du fix, âge des corrections et détection des positions simulées, pour que Surculus lise le ZED-F9P dès la tranche 1.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? (sources, conventions nommées ci-dessous) | oui : ADR 0003 ; Groma en lecture seule (`groma-core`, `app/.../gnss/`) | |
| A-t-il les moyens ? (outils, données d'essai, tests) | oui : tests JVM sur trames NMEA enregistrées ; essai sur appareil par Pascal | |

**Temps de la main :** **appuyer** : code repris, ajouts bornés par l'ADR 0003.

## L'Homme

- **Agent :** `dev`
- **Modèle :** sonnet
- **Ce qui ne lui est pas demandé :** pas d'extraction en bibliothèque commune (ADR 0008, plus tard) ; pas d'écran.

## Harnais

- **Peut :** copier les fichiers nécessaires dans un paquet `gnss` (dans `:core` pour le pur, `:app` pour le transport), garder les en-têtes de licence et l'origine ; ajouter l'horodatage, l'âge des corrections, `isMock()` ; garder l'interface `Transport` ouverte au BLE ; ne pas reprendre la normalisation silencieuse des azimuts hors plage.
- **Ne doit pas :** sortir du périmètre de cette phrase ; inventer un code (travaux, essence, état) ; toucher à `main` ; modifier sommieR, Marculus ou Groma ; trancher une question de sens (la remonter à l'orchestrateur).
- **Sources autorisées :** ADR 0003 ; Groma (`Nmea.kt`, `Ntrip.kt`, `Gnss.kt`, `Geodesie.kt`, `Projections.kt`, `Azimut.kt`, `gnss/Transports.kt`, `gnss/ServiceGnssRtk.kt`, `gnss/SourcePosition.kt`, `docs/terrain.md`).

## Tests

- **Comment saura-t-on que c'est juste :** tests repris de Groma verts ; fix trop vieux refusé ; corrections trop vieilles rétrogradées ; position simulée détectée. Commande du projet : `.claude/test-command`.

## Terrain

- **Échantillon que Pascal vérifiera :** Pascal connecte son ZED-F9P et voit un fix RTK avec Centipède.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal.
- **Point de suivi :** à la remise du plan (missions tenues) et à l'ouverture de la PR.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

le badge RTK fixe s'affiche sur l'appareil de Pascal.
