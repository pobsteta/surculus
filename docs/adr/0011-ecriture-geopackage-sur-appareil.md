# ADR 0011 — Écriture du GeoPackage sur l'appareil

- **Date :** 2026-10-17
- **Statut :** acceptée par Pascal (2026-10-17), après critique du `contradicteur`
- **Proposée par :** Claude (orchestrateur)
- **Tranchée par :** Pascal

## La décision en une phrase

Surculus écrit le `travaux.gpkg` **avec `sqlite-jdbc` sur l'appareil comme en CI** (natifs
`natives-android`), par un **petit écrivain GeoPackage écrit dans `:gpkg`, sans
geopackage-java** : les fonctions `ST_*` qu'exigent les déclencheurs rtree de sommieR sont
déclarées en Kotlin (`org.sqlite.Function`), et l'APK release est essayé sur émulateur avant
chaque release.

## Contexte

L'export `travaux.gpkg` est écrit **sur le téléphone**, à la fin de la tournée (spec § 4.8).
L'ADR 0004 a placé la lecture et l'écriture du GeoPackage dans un module JVM `:gpkg`
(geopackage-java MIT + sqlite-jdbc) pour le tester sans appareil.

**Constat initial faux, corrigé par le `contradicteur`.** La première version disait que
`sqlite-jdbc` 3.53.4.0 n'a aucun natif Android (relevé du plan 006, repris sans vérification
par l'orchestrateur). C'est faux : Maven Central publie le classifieur
`sqlite-jdbc-3.53.4.0-natives-android.jar` (2 640 932 octets, 2026-08-26, vérifié par
l'orchestrateur) ; le README annonce Android à partir de l'API 24 ; les natifs Android sont
liés en pages de 16 Ko depuis 3.50.3.0 (issue #1296). Il faut les extraire dans `jniLibs` et
activer le *core library desugaring* (USAGE.md). L'ADR 0004 n'était donc pas fausse sur ce
point ; il lui manquait l'empaquetage Android.

Contraintes connues :
- les couches de sommieR portent un index rtree dont les déclencheurs appellent
  `ST_IsEmpty`, `ST_MinX`, `ST_MaxX`, `ST_MinY`, `ST_MaxY` : écrire dans `controles` exige que
  ces fonctions soient déclarées sur la connexion (constat du plan 006, sur
  `inst/qgis/travaux.gpkg` de sommieR 0.31.1) ;
- l'export doit être déterministe par le contenu (ADR 0005) et conforme à OGC 12-128
  (ADR 0007) ;
- le critère de la tranche 1 (l'export passe l'import de sommieR) est vérifié par la CI
  (mission 007) : le chemin de code qui écrit sur l'appareil devrait être celui que la CI
  teste.

Options envisagées (critique du `contradicteur`, 2026-10-17) :

1. **(a) SQLite d'Android** (`android.database.sqlite`) derrière une interface : **écartée**.
   AOSP compile SQLite sans `SQLITE_ENABLE_RTREE` et avec `SQLITE_OMIT_LOAD_EXTENSION`
   (`external/sqlite/dist/Android.bp`) ; `setCustomScalarFunction` n'existe qu'à partir de
   l'API 30 et ne traite que des chaînes.
2. **(b) geopackage-android sur l'appareil** : **écartée**. `RTreeIndexExtension.createFunction`
   lève toujours `UnsupportedOperationException` (fonctions qui rendent une valeur non prises en
   charge sur Android) ; dépôt « in maintenance mode ». Marculus n'est pas un précédent : il
   lit des entités et n'écrit que des tuiles (`GpkgRepository.kt`).
3. **(c) SQLite embarqué d'AndroidX** : R*Tree présent, mais aucune API de fonction SQL
   (`SQLiteConnection` : `prepare`, `inTransaction`, `close`) ; seule porte, une extension C
   (`addExtension`). Écartée pour ce seul motif.
4. **(d) `sqlite-jdbc` sur l'appareil et en CI** : même Java, même SQLite 3.53, mêmes options
   de compilation (R*Tree, fonctions `org.sqlite.Function`). **Retenue, sans geopackage-java**
   (arbitrage de Pascal) : geopackage-java n'est pas testé par NGA sur Android (dépendances
   ormlite-jdbc, tiff, nestedvm ; risques AWT, réflexion et R8) et apporte proj4j, retiré par
   l'ADR 0003.
5. **(e) Déclencheurs rtree suspendus pendant l'écriture**, index rempli en Kotlin, déclencheurs
   recréés à l'identique dans la même transaction : fonctionne avec tout SQLite à R*Tree.
   Gardée comme **repli** si la déclaration des `ST_*` posait problème.
6. **Écrire le GPKG hors du téléphone** depuis le `.sursync` : contredit la spec § 4.8, non
   retenue.

## Règles

| Sujet | Règle |
|---|---|
| Accès SQLite | `sqlite-jdbc` 3.53.4.0 (Apache-2.0) sur la JVM et sur Android ; natifs Android extraits du classifieur `natives-android` vers `jniLibs` par une tâche Gradle ; *core library desugaring* activé ; règles R8 pour `org.sqlite` (JNI) |
| Écrivain GeoPackage | dans `:gpkg`, sans geopackage-java ni proj4j : en-tête et géométrie OGC 12-128 § 2.1.3 (point, polygone, multipolygone), `gpkg_contents`, `gpkg_geometry_columns`, index rtree, extension Schema ; fonctions `ST_IsEmpty`, `ST_MinX`, `ST_MaxX`, `ST_MinY`, `ST_MaxY` déclarées sur la connexion |
| Repli | si une version de `sqlite-jdbc` ne déclarait plus les fonctions : voie (e) |
| Vérification | `.claude/test-command` en CI ; **test de fumée de l'APK release sur émulateur x86_64 dans le workflow release** (écrire une tournée de démonstration, la relire) : la release ne part pas s'il échoue (arbitrage de Pascal, rouvre l'ADR 0004 sur ce seul point) ; contrôle 16 Ko sur l'APK release |
| Comparaison | les tests comparent les fichiers par leur **contenu**, jamais par empreinte : l'en-tête SQLite (autovacuum) peut différer d'une version de SQLite à l'autre |

## Le crible FRANCE

| Lettre | Question | Réponse (une ligne) | Ce que ça coûte |
|---|---|---|---|
| **F**rugale | La plus simple qui tienne ? | Un seul SQLite, un seul code, aucune bibliothèque GeoPackage. | Écrire et tenir un petit écrivain GeoPackage. |
| **R**esponsable | Qui fait quoi ? | Le `dev` écrit `:gpkg` ; la CI joue les tests et la fumée sur émulateur ; Pascal essaie l'export sur son téléphone. | Pascal garde l'essai sur appareil réel (ARM). |
| **A**nticipateur | Si l'imprévu arrive ? | Repli (e) prêt ; fumée sur émulateur avant chaque release. | Dépendance à sqlite-jdbc (projet xerial, actif) ; ARM jamais exécuté en CI. |
| **N**ovateur | Toutes les idées ? | Six voies examinées, dont deux apportées par la critique. | — |
| **C**oût | Coût pour l'ensemble ? | Pas de dépendance lourde ; proj4j disparaît. | Natifs de `sqlite-jdbc` dans l'APK (taille à mesurer) ; quelques minutes de CI par release. |
| **É**cologique | Que réutilise-t-on, qui réutilisera ? | Standard OGC ; `sqlite-jdbc` libre ; écrivain réutilisable par Marculus et Groma. | Pas de mutualisation avec geopackage-android. |

## Valeurs sacrifiées

- **Écologie du code** : on n'utilise pas geopackage-java, bibliothèque existante ; accepté
  pour un seul chemin de code testé et sans proj4j.
- **Frugalité de la CI** : un émulateur au workflow release.
- **Indépendance** : un empaquetage Android hors du chemin officiel de `sqlite-jdbc`.

## Sources

- Plan de la mission 006 (2026-10-17, journal).
- ADR 0004, 0005, 0007 ; `docs/spec.md` § 4.8, § 5.
- sommieR 0.31.1 `inst/qgis/travaux.gpkg` (déclencheurs rtree).
- Marculus `gradle/libs.versions.toml` l. 9 et 48 (`geopackage-android` 6.7.4) ; `GpkgRepository.kt`.
- sqlite-jdbc : https://github.com/xerial/sqlite-jdbc (README, USAGE.md, Makefile) ;
  https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.53.4.0/ ; issue #1296.
- AOSP : https://android.googlesource.com/platform/external/sqlite/+/refs/heads/main/dist/Android.bp
- geopackage-android : https://github.com/ngageoint/geopackage-android (`RTreeIndexExtension.java`).
- AndroidX SQLite : https://developer.android.com/jetpack/androidx/releases/sqlite
- GeoPackage, extension rtree : https://www.geopackage.org/spec/ (annexe F.3).

## Question de contrôle

Cette décision tient-elle nos six valeurs, et que sommes-nous prêts à payer pour cela ?
