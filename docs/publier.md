# Publier : versions, releases, badges

Mission 016. Repris de Marculus, comme l'a arbitré Pascal le 2026-10-16, avec deux décisions
de Pascal le même jour (a et b ci-dessous).

## Ce qui se passe à chaque fusion dans `main`

`release.yml` a deux jobs, `permissions: {}` au niveau du workflow.

**`construire`** (`contents: read`, checkout sans conservation du jeton)
1. Calcule la prochaine version SemVer d'après les messages de commit
   (`mathieudutour/github-tag-action`, préfixe `v`) **sans poser le tag**.
2. Lance `.claude/test-command` et le contrôle 16 Ko (`scripts/verifier-16ko.sh`).
3. Si les secrets de signature sont présents : compile `:app:assembleRelease` avec
   `-PappVersionName=<version>` et `-PappVersionCode=<numéro d'exécution>`, et refait le contrôle
   16 Ko sur l'APK release.
4. Dépose en artefact les notes et, le cas échéant, `surculus-vX.Y.Z-AAAAMMJJ-HHMM.apk`.

**`publier`** (`contents: write`, ni checkout ni Gradle) : crée le tag `vX.Y.Z` sur le commit
fusionné et la release GitHub. Tests rouges : `publier` ne part pas, ni tag ni release.

`badges.yml` rejoue les tests de `:core` et `:gpkg` avec JaCoCo et pousse le badge de couverture
sur la branche **`badges`** (jamais sur `main`). `ci.yml` reste en lecture seule.

## Décisions de Pascal (2026-10-16)

**(a) Pas d'APK sans clé.** Si les secrets de signature sont absents, la release part avec ses
notes **mais sans APK**. Les notes de la release et le résumé du job le disent en toutes lettres.
Le repli sur la clé de debug est supprimé pour les releases. Raison : chaque runner GitHub génère
sa propre clé de debug, **différente à chaque exécution**. Deux APK debug successifs n'ont donc
pas la même signature et ne se mettent pas à jour l'un par-dessus l'autre : il faudrait
désinstaller (et perdre les données locales) à chaque version. Mieux vaut pas d'APK qu'un APK qui
piège le forestier. En local, `./gradlew :app:assembleRelease` garde le repli debug, pour les essais.

**(b) Première version `v0.1.0`, automatique.** Quand aucun tag `v*` n'existe, le workflow pose
`v0.1.0` (sans cela, l'action partirait de 0.0.1). Ensuite le calcul normal reprend. Aucun geste
manuel. Les notes de cette première release seront vides, mis à part la mention sur l'APK
(la liste des commits n'a pas de point de départ).

## Commits conventionnels

| Message | Effet sur la version |
|---|---|
| `feat: ...` | mineur (0.1.0 -> 0.2.0) |
| `fix: ...` | patch (0.1.0 -> 0.1.1) |
| `feat!: ...` ou pied `BREAKING CHANGE:` | majeur |
| tout autre (`docs:`, `build:`...) | patch |

Le titre du commit de fusion de la PR compte : le soigner à la fusion. Chaque fusion donne une
version, même pour de la documentation (défaut `patch`), comme dans Marculus.

## Deux fusions pendant qu'une release tourne

Le groupe de concurrence `release` ne lance qu'une release à la fois et ne les annule pas en
cours. Si deux fusions arrivent pendant qu'une release tourne, GitHub **garde en attente
seulement la plus récente et annule l'autre**. La release en attente part ensuite : elle calcule
la version depuis le dernier tag, donc ses notes couvrent les commits des deux fusions. Rien
n'est perdu, mais la fusion annulée n'a pas de version propre (pas de tag à son nom).

## Ce que Pascal doit faire pour la clé

Sans les quatre secrets, pas d'APK (décision a).

1. Créer le keystore, hors du dépôt (`*.keystore` est ignoré par le dépôt) :
   `keytool -genkeypair -v -keystore surculus-release.keystore -alias surculus -keyalg RSA -keysize 4096 -validity 10000`
2. Le garder en lieu sûr, avec ses mots de passe : **le perdre interdit toute mise à jour**
   des appareils déjà installés.
3. Créer quatre secrets dans Settings > Secrets and variables > Actions du dépôt :
   - `RELEASE_KEYSTORE_BASE64` : `base64 -w0 surculus-release.keystore` ;
   - `RELEASE_KEYSTORE_PASSWORD` ;
   - `RELEASE_KEY_ALIAS` (`surculus` ci-dessus) ;
   - `RELEASE_KEY_PASSWORD`.

Un des quatre manquant équivaut à l'absence de clé. Décision de fond (conservation de la clé,
distribution) : ADR 0010, à venir.

En local avec clé : passer `RELEASE_KEYSTORE`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`,
`RELEASE_KEY_PASSWORD` dans l'environnement. Si `RELEASE_KEYSTORE` pointe vers un fichier absent,
Gradle avertit et signe en debug. Aucun secret n'est dans le dépôt.

## Le badge de couverture : qui peut écrire quoi

`badges.yml` a deux jobs. Le premier (lit le dépôt, exécute Gradle) n'a que `contents: read` ;
il dépose le SVG en artefact. Le second (`contents: write`) ne récupère que cet artefact, ne
compile rien, et pousse sans forçage vers `refs/heads/badges`. Le workflow n'a pas d'autre
droit, et sa seule cible d'écriture est cette branche. Les actions du job qui écrit sont
épinglées par SHA (version en commentaire).

La branche `badges` n'existe qu'après le premier passage de `badges.yml` : le badge du README
s'affiche cassé jusque-là. Si une protection de branche couvre toutes les branches, exclure `badges`.
