# ADR 0011 — Écriture du GeoPackage sur l'appareil

- **Date :** 2026-10-17
- **Statut :** proposée, à critiquer par le `contradicteur`
- **Proposée par :** Claude (orchestrateur)
- **Tranchée par :** Pascal

## La décision en une phrase

_À trancher par Pascal après critique : une des trois voies ci-dessous._

## Contexte

L'export `travaux.gpkg` est écrit **sur le téléphone**, à la fin de la tournée (spec § 4.8).
L'ADR 0004 a placé la lecture et l'écriture du GeoPackage dans un module JVM `:gpkg`
(geopackage-java MIT + sqlite-jdbc) pour le tester sans appareil.

**Erreur corrigée** (l'orchestrateur s'était trompé, relevé par le `dev` au plan de la
mission 006, le 2026-10-17) : `sqlite-jdbc` 3.53.4.0 n'embarque **aucune bibliothèque
native Android** (son jar ne porte que Linux, Musl, macOS, Windows et FreeBSD). Tel quel,
`:gpkg` tourne en CI mais pas sur le téléphone. L'ADR 0004 ne l'avait pas vérifié.

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

Options envisagées :

1. **(a) Une interface SQL minimale, une implémentation par plateforme.** `:gpkg` reste
   du Kotlin JVM sans Android ; il parle à une interface (`exécuter`, `requête`,
   `transaction`, `déclarer une fonction`) ; implémentée par `sqlite-jdbc` en CI et par le
   SQLite d'Android (`android.database.sqlite` ou `androidx.sqlite`) sur l'appareil ; en-tête
   et géométrie GeoPackage encodés en Java pur (`mil.nga.sf`, MIT) ou à la main
   (OGC 12-128 § 2.1.3). Un seul code métier testé en CI ; la couche d'accès diffère.
2. **(b) geopackage-android sur l'appareil, geopackage-java en CI**, comme Marculus
   (`geopackage-android` 6.7.4) : deux bibliothèques de la même famille (NGA, MIT) ; le
   chemin de l'appareil n'est pas celui que la CI teste.
3. **(c) Pilote SQLite embarqué d'AndroidX** (`androidx.sqlite:sqlite-bundled`), même API
   sur la JVM et sur Android : un seul chemin ; à vérifier : déclaration de fonctions SQL
   (`ST_*`), taille ajoutée à l'APK, alignement 16 Ko des natives, maturité.

## Le crible FRANCE

_À remplir après la critique du `contradicteur`._

## Valeurs sacrifiées

_À remplir._

## Sources

- Plan de la mission 006 (2026-10-17, journal).
- ADR 0004, 0005, 0007 ; `docs/spec.md` § 4.8, § 5.
- sommieR 0.31.1 `inst/qgis/travaux.gpkg` (déclencheurs rtree).
- Marculus `gradle/libs.versions.toml` l. 9 et 48 (`geopackage-android` 6.7.4).

## Question de contrôle

Cette décision tient-elle nos six valeurs, et que sommes-nous prêts à payer pour cela ?
