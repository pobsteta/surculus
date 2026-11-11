# Publier : versions, releases, badges

Mission 016. Repris de Marculus, comme l'a arbitré Pascal le 2026-10-16, avec deux décisions
de Pascal le même jour (a et b ci-dessous ; la numérotation SemVer d'origine est remplacée par la
version calendaire, mission 108).

## Ce qui se passe à chaque fusion dans `main`

`release.yml` a trois jobs, `permissions: {}` au niveau du workflow.

**`tester`** (`contents: read`, checkout sans conservation du jeton, **ni environnement ni secret**)
1. Calcule la prochaine version calendaire `AAAA.M.N` (voir « La version calendaire » plus bas,
   `scripts/prochaine-version.sh`) **sans poser le tag**, et les notes de version (commits depuis le dernier tag `v*`).
2. Lance `.claude/test-command` et le contrôle 16 Ko de l'APK debug (`scripts/verifier-16ko.sh`).
3. Joue l'essai de fumée sur émulateur (voir plus bas), **avant l'approbation et sans aucune clé** :
   le type de build `fumee` se signe en debug quand `RELEASE_KEYSTORE` est absent
   (`app/build.gradle.kts`). Pascal approuve donc une release déjà éprouvée sur émulateur.

**`signer`** (`contents: read`, `environment: release`, part après `tester`) : attend l'approbation
de Pascal, fait son propre checkout, puis
1. si les secrets de signature sont présents : compile `:app:assembleRelease` avec
   `-PappVersionName=<version>` et `-PappVersionCode=<numéro d'exécution>` et refait le contrôle
   16 Ko sur l'APK release ;
2. dépose en artefact les notes et, le cas échéant, `surculus-vAAAA.M.N-AAAAMMJJ-HHMM.apk`.

Aucun APK ne passe de `tester` à `signer` : l'APK release est recompilé dans `signer`, de sorte
que l'APK debug (non signé de la clé) ne peut pas passer pour l'APK signé.

**`publier`** (`contents: write`, ni checkout ni Gradle, aucun secret de signature) : crée le tag
`vAAAA.M.N` sur le commit fusionné et la release GitHub. Il dépend de `tester` et de `signer`. Tests
rouges : `publier` ne part pas, ni tag ni release. **Si Pascal n'approuve pas, ou rejette,
`signer` ne s'exécute pas : ni tag ni release.** Pendant l'attente, la release occupe le groupe
de concurrence : une fusion suivante attend derrière elle (voir plus bas).

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

**(b) Première version `v0.1.0`, automatique. Remplacée par la mission 108 (2026-11-11), à
confirmer par Pascal.** La décision d'origine : quand aucun tag `v*` n'existe, le workflow pose
`v0.1.0`, sans geste manuel. Elle devient « première version **du mois** » : sans tag du mois,
le rang est 1 (`v2026.11.1`). Sans aucun tag `v*` dans le dépôt, la première release est donc
`vAAAA.M.1` et non plus `v0.1.0`, avec des notes sans liste de changements (pas de point de départ).
Les tags `v0.1.0` à `v0.7.x` existants restent tels quels.

## La version calendaire

Mission 108, comme nemeton (`r.yml`). Plus de SemVer ni de messages de commit conventionnels
pour numéroter : le numéro dit la date de l'APK.

- Tag `vAAAA.M.N`, version `AAAA.M.N` (sans `v`) : `v2026.11.1`, `v2026.11.2`, puis `v2026.12.1`,
  puis `v2027.1.1`. Forme : `^[0-9]{4}\.([1-9]|1[0-2])\.[1-9][0-9]*$` (mois sans zéro devant).
- AAAA.M : année et mois du lancement du workflow, **à l'heure de Paris** (une fusion le
  30 novembre à 23 h 30 à Paris est une release de novembre). Valeur provisoire, à confirmer par Pascal.
- N : 1 + le plus grand N des tags `vAAAA.M.*` du même mois (comparaison numérique), 1 s'il n'y en
  a aucun. Les tags `v0.x.y` sont ignorés. Un trou (tag supprimé) n'est pas comblé.
- Le `versionCode` Android reste le numéro d'exécution du workflow (`github.run_number`) : il monte
  toujours, l'APK s'installe par-dessus l'ancien.
- Le calcul est `scripts/prochaine-version.sh`, vérifié par `VersionCalendaireTest` (listes de
  tags et dates données, sans réseau). Il se fait dans `tester`, sans poser le tag.
- Notes de version : « Changements depuis `<dernier tag v*>` » puis un commit par ligne (titre et
  hash court), fusions exclues. La première release calendaire part du dernier `v0.7.x`.
  Chaque fusion donne une release, même pour de la documentation. Les titres de commit ne
  pilotent plus rien : ils ne servent qu'à lire les notes.

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
3. Créer l'environnement `release`, ses règles et ses secrets (Settings > Environments), **avant**
   de fusionner la mission 107. Libellés de GitHub (« Managing environments for deployment »,
   vérifiés le 2026-11-10) :
   - Settings > Environments > **New environment**, nom `release` ;
   - **Deployment branches** (« Deployment branches and tags ») > **Selected branches and tags** >
     **Add deployment branch or tag rule** : Ref type **Branch**, motif `main` > **Add rule** ;
   - **Required reviewers** : Pascal. **Ne pas cocher « Prevent self-review »** : c'est Pascal qui
     fusionne et déclenche la release, il doit pouvoir l'approuver lui-même. Puis
     **Save protection rules** ;
   - sous **Environment secrets**, **Add Secret**, quatre fois :
     - `RELEASE_KEYSTORE_BASE64` : `base64 -w0 surculus-release.keystore` ;
     - `RELEASE_KEYSTORE_PASSWORD` ;
     - `RELEASE_KEY_ALIAS` (`surculus` ci-dessus) ;
     - `RELEASE_KEY_PASSWORD`.
4. Vérifier avant la fusion : `gh secret list --env release` montre les quatre noms.

**L'ordre, en toutes lettres.** (1) créer l'environnement, ses règles et ses secrets ; (2) **puis**
fusionner la mission 107 ; (3) **puis** attendre et approuver la première release (onglet Actions >
la release > **Review deployments** > **Approve and deploy**, ou **Reject**) ; (4) **puis**
supprimer les quatre secrets du dépôt (Settings > Secrets and variables > Actions > Repository
secrets) et vérifier la release suivante.

*Pourquoi cet ordre.* Un workflow qui référence un environnement absent **crée** cet environnement,
sans aucune règle de protection ni secret (GitHub, « Managing environments for deployment »). Fusionner
d'abord donnerait donc un environnement `release` ouvert à tous, sans approbation : la protection
serait fausse sans que rien ne le signale.

*Deux vérifications, pas une.* Tant que les secrets du dépôt existent, `signer` les voit aussi : une
première release signée ne prouve donc pas que les secrets d'environnement sont en place.
- Avant la fusion : `gh secret list --env release` montre les quatre noms.
- Après la suppression des secrets du dépôt : la release suivante porte toujours un APK signé
  (et `gh secret list`, au niveau du dépôt, n'affiche plus aucun `RELEASE_*`). Sinon, un secret
  manque dans l'environnement : le recréer là, pas dans le dépôt.

Sans la suppression de l'étape (4), un workflow lancé depuis une branche peut encore lire les secrets
du dépôt et la protection ne sert à rien.

Le job `signer` de `release.yml` déclare `environment: release` : chaque release attend
l'approbation de Pascal avant la signature. Sans approbation, ou si Pascal **rejette**, ni tag ni
release. Un environnement sans les quatre secrets donne toujours une release sans APK (décision a).

*Attente et concurrence.* Les tests (`tester`) de la **première** release tournent sans attendre
l'approbation. Tant qu'une approbation est en attente, la release occupe le groupe de concurrence :
la release suivante attend **tout entière**, `tester` compris, et GitHub n'en garde qu'une en attente
(la plus récente). Pascal **rejette** donc (**Reject**) une release qu'il ne veut pas, au lieu de la
laisser en attente, sans quoi il bloque les suivantes. Aucun délai maximal d'attente n'est écrit ici :
la documentation consultée n'en donne pas pour ce cas.

Un des quatre manquant équivaut à l'absence de clé. Décision de fond (conservation de la clé,
distribution) : ADR 0010, à venir (réservée, non écrite : voir [l'index des ADR](adr/README.md)).

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

## L'essai de fumée sur émulateur

Ajouté le 2026-10-22. Avant de publier, `.github/workflows/release.yml` (job `tester`, étapes « Activer KVM » puis
« Essai de fumée du type de build fumee sur émulateur ») lance l'application sur un
émulateur x86_64 : elle écrit une tournée de démonstration (sqlite-jdbc, fonctions `ST_*` en Kotlin,
index rtree) puis la relit. Il tourne sur le type de build `fumee`, calqué sur la release, avec
R8 et `proguard-fumee.pro`. **La release ne part pas si l'essai échoue** : `tester` échoue, donc `signer` (qui en dépend) ne part pas, sans même demander l'approbation de Pascal. L'essai éprouve le type `fumee`, signé en debug, et non l'APK signé qui sera publié ; il n'utilise aucun secret ni aucune sortie de `signer`.

Limites, dites dans le workflow : x86_64 seulement (le chemin ARM n'est jamais exécuté en CI,
Pascal l'essaie sur téléphone) ; `proguard-fumee.pro` garde aussi `:gpkg`, `:core`, `kotlin.**`
et `kotlinx.**`, donc l'essai n'éprouve pas R8 sur ces classes. La décision est dans l'[ADR 0011](adr/0011-ecriture-geopackage-sur-appareil.md) ;
le workflow fait foi pour le détail.
