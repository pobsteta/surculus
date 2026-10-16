# Publier : versions, releases, badges

Mission 016. Repris de Marculus, comme l'a arbitré Pascal le 2026-10-16.

## Ce qui se passe à chaque fusion dans `main`

1. `release.yml` calcule la prochaine version SemVer d'après les messages de commit
   (action `mathieudutour/github-tag-action`, préfixe `v`) **sans poser le tag**.
2. Il lance `.claude/test-command`, puis compile `:app:assembleRelease` avec
   `-PappVersionName=<version>` et `-PappVersionCode=<numéro d'exécution>`.
3. Il vérifie les pages de 16 Ko de l'APK release (`scripts/verifier-16ko.sh`).
4. Seulement si tout est vert : tag `vX.Y.Z`, release GitHub, notes (liste des commits)
   et `surculus-vX.Y.Z-AAAAMMJJ-HHMM.apk`. Tests rouges : ni tag, ni release.
5. `badges.yml` rejoue les tests de `:core` et `:gpkg` avec JaCoCo et pousse le badge de
   couverture sur la branche **`badges`** (jamais sur `main`). Le README pointe dessus.

`ci.yml` reste en lecture seule. Chaque fusion donne une version, même pour un commit de
documentation (défaut `patch`) : c'est le comportement de Marculus.

## Commits conventionnels

| Message | Effet sur la version |
|---|---|
| `feat: ...` | mineur (0.1.0 -> 0.2.0) |
| `fix: ...` | patch (0.1.0 -> 0.1.1) |
| `feat!: ...` ou pied `BREAKING CHANGE:` | majeur |
| tout autre (`docs:`, `build:`...) | patch |

Le titre du commit de fusion de la PR compte : le soigner à la fusion.

## Ce que Pascal doit faire pour la clé

Tant que le secret `RELEASE_KEYSTORE_BASE64` n'existe pas, l'APK est **signé avec la clé de
debug**, et les notes de la release le disent. Décision de fond (conservation de la clé,
distribution) : ADR 0010, à venir.

1. Créer le keystore, hors du dépôt (`*.keystore` est ignoré par le dépôt) :
   `keytool -genkeypair -v -keystore surculus-release.keystore -alias surculus -keyalg RSA -keysize 4096 -validity 10000`
2. Le garder en lieu sûr, avec ses mots de passe : **le perdre interdit toute mise à jour**
   des appareils déjà installés.
3. Créer quatre secrets dans Settings > Secrets and variables > Actions du dépôt :
   - `RELEASE_KEYSTORE_BASE64` : `base64 -w0 surculus-release.keystore` ;
   - `RELEASE_KEYSTORE_PASSWORD` ;
   - `RELEASE_KEY_ALIAS` (`surculus` ci-dessus) ;
   - `RELEASE_KEY_PASSWORD`.

**Risque** : un APK signé debug ne se met pas à jour en APK signé release (signatures
différentes) : il faut désinstaller d'abord, et les données locales de l'application sont
perdues. Créer la clé **avant** de distribuer des APK aux équipes évite ce coût.

En local, sans clé : `./gradlew :app:assembleRelease` signe en debug ; avec clé, passer
`RELEASE_KEYSTORE`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`
dans l'environnement. Aucun secret n'est dans le dépôt.

## Le badge de couverture : qui peut écrire quoi

`badges.yml` a deux jobs. Le premier (lit le dépôt, exécute Gradle) n'a que `contents: read` ;
il dépose le SVG en artefact. Le second (`contents: write`) ne récupère que cet artefact, ne
compile rien, et pousse sans forçage vers `refs/heads/badges`. Le workflow n'a pas d'autre
droit, et sa seule cible d'écriture est cette branche.

## À la première fusion

La branche `badges` n'existe pas avant le premier passage de `badges.yml` : le badge de
couverture du README s'affiche cassé jusque-là. Si la protection de branche s'applique à
toutes les branches, exclure `badges`.
