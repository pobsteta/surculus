# Architecture

```
nemetonshiny ── plan d'action (export_action_plan_gpkg) ─┐ brief : le prévu
                                                          ▼
sommieR ── sommier_projet_qfield_travaux() ──► <tournée>/travaux.gpkg + DCIM/
   ▲                                                   │ copie (SAF, partage)
   │                                                   ▼
   │        Surculus (Android, hors ligne, copie privée de la tournée)
   │          :app   Compose : Tournées, Carte, Intervention, Placette, Rendre
   │          :data  Room (journal append-only), photos
   │          :gpkg  JVM : lecture/écriture du travaux.gpkg (sqlite-jdbc, ADR 0011)
   │          :core  Kotlin pur : règles du registre 6, plants, totaux,
   │                 numérotation, fusion par UUID, grammaire de dictée
   │          ── tranche 1 : groma-core provisoire, et terrain-voix (grammaire
   │             fermée, nombres en lettres, import du modèle ; adopté le 2026-10-24,
   │             mission 013, docs/dependances.md) ; ensuite bibliothèques
   │             communes (GNSS/NTRIP, géodésie, GeoPackage,
   │             carte, dictée, journal) partagées avec Marculus et Groma
   │                                                   │
   │                  <tournée>.zip (travaux.gpkg complété + DCIM/)
   └── sommier_importer_qfield_travaux() ◄─────────────┘
         registre 6 (travaux, placettes, contrôles ; plants après brief)
         └─► bilan de gestion, sommier imprimé, indicateurs ─► nemeton
```

## Modules

| Module | Rôle | Testé |
|---|---|---|
| `:core` | Domaine pur : codes et formes, règles d'import, plants et états, totaux d'un contrôle, taux de reprise et densité, numérotation des plants par azimut, fusion par UUID, grammaire et analyse de la dictée | JVM, avec `fixtures/` |
| `:gpkg` | Lecture d'une tournée et écriture du `travaux.gpkg` à l'export (écrivain GeoPackage maison sur sqlite-jdbc, JVM et appareil, ADR 0011) | JVM, et import R de sommieR rejoué en CI |
| `:data` | Journal Room des saisies en ajout seul (une ligne par saisie, UUID v7, séquence locale, ADR 0005), projection, sauvegardes `.sursync`, photos | JVM sous Robolectric (JUnit 4), pas instrumenté ; l'essai de coupure sur appareil reste à Pascal |
| `:app` | Écrans Compose (Tournées, Carte, Intervention, Placette, Rendre, Paramètres), service GNSS, dictée | appareil |

## Le journal et le GeoPackage

Le téléphone ne réécrit pas le GeoPackage à chaque toucher : les saisies
vivent dans le journal Room, append-only comme celui de Marculus, et le
`travaux.gpkg` de la tournée n'est complété qu'à l'export. Deux raisons :

* une saisie interrompue (batterie, appel) ne laisse jamais un GeoPackage à
  moitié écrit ;
* la fusion multi-opérateurs se fait sur le journal (union sur l'UUID), puis
  un seul export écrit le résultat.

L'export reste **déterministe** : la même tournée, le même journal, le même
`travaux.gpkg`. C'est ce qui permet de comparer en test l'export Kotlin au
GeoPackage attendu par sommieR.

## Le contrat avec sommieR

Le `travaux.gpkg` de sommieR 0.31.1 (Lambert-93) est la seule interface ;
le détail des couches, y compris celles qu'ajoute Surculus, est dans la
spécification (`docs/spec.md`, § 5) : elles sont ignorées par l'import
actuel, la tournée reste importable avant que sommieR ne les lise (brief
`pilotage/briefs/vers-sommier/2026-10-15-travaux-2-plants.md`).

## Une règle, deux langages

Les règles d'import sont écrites en R dans sommieR et en Kotlin dans
`:core`. Elles sont rejouées sur les mêmes cas : `fixtures/regles/*.json`,
avec le résultat attendu écrit à la main, à la manière de
`groma/fixtures/numerotation/`. Le brief sommieR demande d'y brancher ses
propres tests.

## Bibliothèques communes

Marculus et Groma portent aujourd'hui deux copies du même code GNSS
(`fr.marculus.core.NmeaParser`, `Ntrip`, `Geodesie` ; `groma.core.Nmea`,
`Ntrip`, `Geodesie`). Surculus ne fera pas une troisième copie : le brief
`vers-marculus` propose d'extraire ces briques en modules partagés, dont
Surculus dépend.
