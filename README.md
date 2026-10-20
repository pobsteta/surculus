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

> **État : en développement (tranche 1).** Le domaine (contrôle de placette, repères,
> journal des saisies) et l'export GeoPackage sont faits et testés ; les écrans de
> tournée et de contrôle sont à venir. L'APK publié contient un écran d'essai GNSS.
> L'avancement est dans [`pilotage/kanban.md`](pilotage/kanban.md).

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

JDK 17 et SDK Android requis.

```sh
./gradlew :core:test :gpkg:test :data:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

C'est la commande de tests du projet (`.claude/test-command`), rejouée par la CI.

| Module | Rôle |
|---|---|
| `:core` | Domaine en Kotlin pur : plants et états, totaux d'un contrôle, numérotation, repères et déclinaison magnétique (WMM2025), GNSS (NMEA, NTRIP) |
| `:gpkg` | Lecture d'une tournée et écriture du `travaux.gpkg` de sommieR (sqlite-jdbc) |
| `:data` | Journal des saisies en ajout seul (Room) |
| `:app` | Écrans Jetpack Compose, service GNSS |

Architecture : [`docs/architecture.md`](docs/architecture.md) · décisions :
[`docs/adr/`](docs/adr/) · référentiels sourcés : [`docs/referentiels/`](docs/referentiels/) ·
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
