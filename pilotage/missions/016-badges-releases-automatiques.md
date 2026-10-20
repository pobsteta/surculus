# Mission 016 — Badges, releases et montée de version automatiques

> Fiche écrite par l'orchestrateur le 2026-10-16, à la demande de Pascal, à partir du
> gabarit `imperatrice/skills/mission/fiche-mission.md`. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Reprendre de Marculus les badges du README et la release automatique (version SemVer calculée depuis les commits conventionnels, tag `vX.Y.Z`, release GitHub avec ses notes et l'APK), pour que chaque fusion dans `main` produise sans geste manuel une version testée, numérotée et installable.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : Marculus `README.md` (badges), `.github/workflows/release.yml`, `.github/workflows/ci.yml` (jacoco), `app/build.gradle.kts` l. 16-18 (version injectée) | |
| A-t-il les moyens ? | oui : CI de Surculus (mission 001) ; secrets de signature fournis plus tard par Pascal | |

**Temps de la main :** **appuyer**.

## L'Homme

- **Agent :** `dev`
- **Modèle :** sonnet
- **Ce qui ne lui est pas demandé :** la décision sur la clé de signature et la distribution (ADR 0010, à venir) ; la publication sur un magasin.

## Harnais

- **Peut :**
  - `README.md` : badges CI, Release, Version (shields.io, tri semver), Couverture, Kotlin (2.4.21), Android (min 26 · cible 36), Compose, Licence (**GPL-3.0**, fichier `LICENSE`) ; pas de badge « Site » (Surculus n'a pas de site).
  - `.github/workflows/release.yml` repris de Marculus (arbitrage de Pascal du 2026-10-16 : **comme Marculus**) : `mathieudutour/github-tag-action` (préfixe `v`, défaut `patch`), APK release signé si le secret `RELEASE_KEYSTORE_BASE64` existe, sinon **release sans APK, dit dans les notes** (arbitrage de Pascal du 2026-10-16, chaque runner ayant sa propre clé de debug) ; **première version v0.1.0 automatique** ; nom `surculus-vX.Y.Z-AAAAMMJJ-HHMM.apk` ; la release ne part que si `.claude/test-command` et le contrôle 16 Ko sont verts dans le même workflow.
  - `app/build.gradle.kts` : `versionName` et `versionCode` injectés par `-PappVersionName` / `-PappVersionCode`, valeurs de repli en local ; signature release lue dans l'environnement (`RELEASE_KEYSTORE`, etc.), jamais de secret dans le dépôt.
  - Couverture JaCoCo de `:core` (et `:gpkg`), badge généré par la CI et **poussé sur une branche dédiée `badges`**, jamais sur `main` (règle « jamais de push direct sur main ») ; le README pointe vers cette branche.
  - `docs/dependances.md` : actions GitHub ajoutées, avec version et licence.
  - `docs/publier.md` : ce que fait la release, les commits conventionnels (`feat:` → mineur, `fix:` → patch, `!` → majeur), et ce que Pascal doit faire pour la clé (créer le keystore, les quatre secrets), avec le risque : un APK signé debug ne se met pas à jour en APK signé release sans désinstaller.
- **Ne doit pas :** pousser sur `main` ; créer ou versionner une clé ; inventer un secret ; toucher à `main`.
- **Sources autorisées :** Marculus en lecture seule ; documentation des actions utilisées.

## Tests

- **Comment saura-t-on que c'est juste :** `.claude/test-command` vert ; `actionlint` (ou équivalent) sans erreur sur les workflows ; build `assembleRelease` local avec `-PappVersionName=0.0.0-test -PappVersionCode=1`, versions lues dans l'APK (`aapt2 dump badging`) ; la CI de la PR verte. La première vraie release partira à la fusion.

## Terrain

- **Échantillon que Pascal vérifiera :** après la fusion, une release `v0.1.0` (ou suivante) apparaît sur GitHub avec son APK, et les badges du README sont tous verts ou renseignés.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

la fusion suivante dans `main` crée seule le tag, la release et son APK, sans geste de Pascal.
