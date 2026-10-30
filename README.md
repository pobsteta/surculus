# Surculus

[![CI](https://github.com/pobsteta/surculus/actions/workflows/ci.yml/badge.svg)](https://github.com/pobsteta/surculus/actions/workflows/ci.yml)
[![Release](https://github.com/pobsteta/surculus/actions/workflows/release.yml/badge.svg)](https://github.com/pobsteta/surculus/actions/workflows/release.yml)
[![Version](https://img.shields.io/github/v/release/pobsteta/surculus?sort=semver&label=version)](https://github.com/pobsteta/surculus/releases)
![Couverture](https://raw.githubusercontent.com/pobsteta/surculus/badges/jacoco.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.21-7F52FF?logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-min%2026%20%C2%B7%20cible%2036-3DDC84?logo=android&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
[![Licence](https://img.shields.io/badge/licence-GPL--3.0-blue)](LICENSE)
[![Méthode](https://img.shields.io/github/v/release/pobsteta/methode-imperatrice?sort=semver&label=m%C3%A9thode%20imperatrice)](https://github.com/pobsteta/methode-imperatrice/releases)

Application Android de **suivi des travaux de plantation en forêt**, hors ligne :
elle suit **plant par plant**, sur des placettes fixes, ce qui a repris d'un passage à
l'autre, et relève les travaux faits, pour les rendre au sommier forestier (sommieR).

> **État : en développement (tranche 1).** Le contrôle de placette (mission 012, A à D, dernière
> PR fusionnée le 2026-10-28, journal du même jour) est livré : pointage, gestes de volume et
> mode poche, dictée « tenir pour parler ». Les essais en forêt (essai ganté de Pascal, Couchey)
> ne sont pas faits (journal, 2026-10-30). **Ce qui est livré, en cours ou planifié se lit dans
> [`pilotage/kanban.md`](pilotage/kanban.md)**, seule source de l'avancement ; les écrans ne sont
> pas listés ici. **La dernière version installable** est la dernière
> [release](https://github.com/pobsteta/surculus/releases) (badge « version » ci-dessus).

## Pour qui, pour quoi

- **Le gestionnaire forestier** qui tient le sommier et décide des regarnis,
  dégagements et protections : savoir quel plant est mort, abrouti ou regarni, et
  pourquoi, d'un passage à l'autre.
- **Le propriétaire**, qui paie ces travaux et doit parfois justifier une aide : taux de
  reprise et densité de vivants par essence objectif, toujours avec leur effectif.

Sur le terrain : l'agent ouvre la tournée préparée par sommieR, il est guidé jusqu'au
piquet de chaque placette puis d'un plant au suivant, il dit l'état de chaque plant à la
voix ou aux boutons de volume, sans regarder l'écran et sans réseau, et il trace au GNSS
l'intervention faite. La tournée complétée retourne à sommieR, qui l'importe.

Le détail : [`docs/brief.md`](docs/brief.md), puis [`docs/spec.md`](docs/spec.md).

## Installer

Télécharger l'APK de la dernière [release](https://github.com/pobsteta/surculus/releases)
(Android 8.0 ou plus récent) et l'installer. Les versions et la signature sont décrites
dans [`docs/publier.md`](docs/publier.md).

## Construire et tester

JDK 17 ou plus (bytecode cible 17, `core/build.gradle.kts` ; testé avec 21, comme la CI,
`.github/workflows/ci.yml`) et SDK Android requis.

```sh
./gradlew :core:test :gpkg:test :data:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

C'est la commande de tests du projet (`.claude/test-command`, qui fait foi), rejouée par la CI.

La carte repose sur osmdroid, **archivé** : constat, date et suite dans
[l'ADR 0009](docs/adr/0009-carte-hors-ligne.md) (le réexamen de l'ADR 0004 n'est pas fait).

| Module | Rôle |
|---|---|
| `:core` | Domaine en Kotlin pur : plants et états, totaux d'un contrôle, numérotation, repères et déclinaison magnétique (WMM2025), GNSS (NMEA, NTRIP) |
| `:gpkg` | Lecture d'une tournée et écriture du `travaux.gpkg` de sommieR (sqlite-jdbc) |
| `:data` | Journal des saisies en ajout seul (Room) |
| `:app` | Écrans Jetpack Compose, service GNSS |

Architecture : [`docs/architecture.md`](docs/architecture.md) · décisions :
[`docs/adr/`](docs/adr/README.md) (index : ce qui est décidé, ce qui ne l'est pas encore) · référentiels sourcés : [`docs/referentiels/`](docs/referentiels/) ·
cas de test partagés avec sommieR : [`fixtures/`](fixtures/).

## Comment ce projet est développé

Surculus est développé par une équipe d'agents IA orchestrée dans Claude Code, selon
**la méthode de l'Impératrice** de Pascal Obstétar : *l'IA propose, l'homme tranche et
assume.* La méthode, les agents, les commandes et les garde-fous viennent du plugin
[`imperatrice`](https://github.com/pobsteta/methode-imperatrice), déclaré dans
[`.claude/settings.json`](.claude/settings.json) ; Claude Code propose de l'installer à
l'ouverture du dépôt.

La trace de ce pilotage est dans [`pilotage/`](pilotage/) : l'équipe
([`equipe.md`](pilotage/equipe.md)), les missions ([`missions/`](pilotage/missions/)),
l'avancement ([`kanban.md`](pilotage/kanban.md)), la mémoire en ajout seul
([`journal.md`](pilotage/journal.md)) et les messages aux autres dépôts
([`briefs/`](pilotage/briefs/)). Les consignes de l'orchestrateur sont dans
[`CLAUDE.md`](CLAUDE.md).

## Licence

GPL-3.0-or-later ([`LICENSE`](LICENSE)) ; provenance des éléments repris de tiers :
[`NOTICE`](NOTICE).
