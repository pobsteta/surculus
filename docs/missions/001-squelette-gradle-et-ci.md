# Mission 001 — Squelette Gradle et CI

> Fiche écrite par l'orchestrateur le 2026-10-15 (`/mission`, porte n° 2), à partir du
> gabarit `docs/modeles/fiche-mission.md`. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Créer le squelette Gradle de Surculus (`:core`, `:gpkg`, `:data`, `:app`, identifiant `io.github.pobsteta.surculus`) et une CI GitHub qui joue `.claude/test-command`, pour que toute mission suivante s'écrive sur une base qui compile et se teste.

**Redite par l'agent (`dev`, 2026-10-16) :** Poser le squelette Gradle de Surculus (`:core` Kotlin pur, `:gpkg` JVM, `:data` bibliothèque Android Room, `:app` Compose réduit à une activité vide, identifiant `io.github.pobsteta.surculus`) et une CI GitHub qui joue `.claude/test-command` et contrôle l'alignement 16 Ko de l'APK de debug, pour que chaque mission suivante parte d'une base qui compile et se teste.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? (sources, conventions nommées ci-dessous) | oui : ADR 0004, Marculus (`gradle/libs.versions.toml`, `.github/workflows/ci.yml`), `groma-core/build.gradle.kts` | |
| A-t-il les moyens ? (outils, données d'essai, tests) | oui : critère vérifiable (CI verte sur un test « placette en France ») | |

**Temps de la main :** **tenir** : la mission fixe la stack et la structure pour toute la durée du projet ; l'agent rend d'abord un plan (arborescence, versions, CI) relu par l'orchestrateur.

## L'Homme

- **Agent :** `dev`
- **Modèle :** opus (engage l'architecture)
- **Ce qui ne lui est pas demandé :** aucune fonction métier ; pas d'écran au-delà d'une activité vide ; pas de publication.

## Harnais

- **Peut :** créer les modules, `settings.gradle.kts`, `gradle/libs.versions.toml` (versions de Marculus, Vosk 0.3.75, JUnit Jupiter), `.github/workflows/ci.yml` (JDK de Marculus), un test JUnit dans `:core` ; contrôle `zipalign -P 16` sur l'APK de debug.
- **Ne doit pas :** sortir du périmètre de cette phrase ; inventer un code (travaux, essence, état) ; toucher à `main` ; modifier sommieR, Marculus ou Groma ; trancher une question de sens (la remonter à l'orchestrateur).
- **Sources autorisées :** ADR 0004 ; `docs/architecture.md` ; `docs/dependances.md` ; Marculus et `groma-core` en lecture seule.

## Tests

- **Comment saura-t-on que c'est juste :** `./gradlew :core:test :gpkg:test :app:assembleDebug` vert en local et en CI ; un test de `:core` vérifie qu'un point Lambert-93 de Couchey (x = 846 164,76 m, y = 6 686 123,26 m : place de dépôt PD-01 de sommieR `R/demo.R` l. 318-321, lon 4.933034, lat 47.259315, reprojetée par pyproj ; corrigé le 2026-10-16, la fiche portait par erreur y ≈ 6 676 000) tombe dans l'emprise de la France métropolitaine. Commande du projet : `.claude/test-command`.

## Terrain

- **Échantillon que Pascal vérifiera :** l'APK de debug s'installe sur un téléphone de Pascal et s'ouvre.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal.
- **Point de suivi :** à la remise du plan (missions tenues) et à l'ouverture de la PR.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

la CI de la PR est verte et Pascal installe l'APK.
