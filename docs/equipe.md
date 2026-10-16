# Équipe de Surculus

> Produit par l'orchestrateur avec `/mission`, **validé par Pascal** avant tout démarrage.
> Appliquer la règle de staffing de `docs/methode-imperatrice.md` : un agent par mission
> qui passe les trois questions, jamais un agent « par principe ».

- **Statut :** validée (porte n° 2)
- **Validée par :** Pascal, le 2026-10-15 (PR #13)

## Plafond humain

- **PR ouvertes en même temps, au plus :** 3
- **Rythme de relecture de Pascal :** un créneau par jour

_Régler l'ambition sur le rythme qu'on peut tenir, et les Hommes avant la toupie._

## Le vivier (catalogue, pas une équipe)

| Agent | A / plan | Modèle | Écrit du code ? |
|---|---|---|---|
| `contradicteur` | Abstraction · Polos | opus | non |
| `acquereur` | Acquisition · Ponos | sonnet | non (référentiels et données) |
| `archiviste` | Archivage · Pontos | sonnet | non (docs) |
| `analyste` | Analyse · Ponos | sonnet | non |
| `afficheur` | Affichage · Ponos | sonnet | oui (interface) |
| `dev` | réalisation · Ponos | sonnet | oui |
| `testeur` | réalisation · Ponos | sonnet | oui (tests) |
| `relecteur` | contrôle · C3 | opus | non (lecture seule) |

## Missions des tranches 0 et 1

Agents mobilisés : `dev` (10 missions), `afficheur` (2), `testeur` (1, plus un test
d'acceptation écrit depuis la spec à chaque mission de développement), et à chaque PR le
`relecteur`. Ni `archiviste`, ni `analyste`, ni `acquereur` : aucune mission ne les
demande dans ces tranches (le `bilan` les rappellera).

| Mission | Agent | Modèle | Temps de la main | Les 3 questions | Cycle |
|---|---|---|---|---|---|
| [001](missions/001-squelette-gradle-et-ci.md) Squelette Gradle et CI | `dev` | opus | tenir | 3 oui | 1 |
| [002](missions/002-fixtures-sourcees-et-tests-d-acceptation-core.md) Fixtures sourcées et tests du domaine | `testeur` | sonnet | appuyer | 3 oui (tests posés après 001) | 1 |
| [003](missions/003-reprise-provisoire-gnss-groma.md) Reprise provisoire du GNSS de Groma | `dev` | sonnet | appuyer | 3 oui | 2 |
| [004](missions/004-domaine-controle-de-placette.md) Domaine du contrôle de placette | `dev` | opus | tenir | 3 oui | 2 |
| [005](missions/005-chaine-des-reperes.md) Chaîne des repères | `dev` | opus | tenir | 3 oui | 2 |
| [006](missions/006-module-gpkg-lecture-et-export.md) Module GeoPackage : lecture et export | `dev` | opus | tenir | 3 oui | 3 |
| [007](missions/007-ci-import-sommier.md) CI : import sommieR rejoué | `dev` | sonnet | appuyer | 3 oui (après 006) | 3 |
| [008](missions/008-journal-en-ajout-seul.md) Journal en ajout seul et projection | `dev` | opus | tenir | 3 oui | 3 |
| [009](missions/009-sauvegarde-sursync-et-restauration.md) Sauvegarde `.sursync` et restauration | `dev` | sonnet | appuyer | 3 oui | 4 |
| [011](missions/011-ecrans-tournees-carte-guidage.md) Écrans : tournées, carte, guidage | `afficheur` | sonnet | appuyer | 3 oui (après 003, 005, 006) | 4 |
| [013](missions/013-dictee-vosk.md) Dictée Vosk à grammaire fermée | `dev` | sonnet | appuyer | 3 oui | 4 |
| [012](missions/012-ecran-controle-de-placette.md) Écran du contrôle de placette | `afficheur` | sonnet | appuyer | 3 oui (après 004, 005, 008) | 5 |
| [010](missions/010-voies-hors-appareil.md) Voies de sauvegarde hors appareil | `dev` | sonnet | ne pas lâcher | **non à la 3e** : serveur WebDAV d'essai et second appareil à fournir | 5 |
| [014](missions/014-essai-boutons-ecran-eteint.md) Essai : boutons écran éteint | `dev` | sonnet | ne pas lâcher, puis tenir | **non à la 3e** : deux appareils Android 14 et 16 à désigner | 5 |
| [015](missions/015-icone-de-l-application.md) Icône « S » gothique | `dev` | sonnet | appuyer | oui (demandée par Pascal le 2026-10-16) | 1 |
| [016](missions/016-badges-releases-automatiques.md) Badges et releases automatiques | `dev` | sonnet | appuyer | oui (demandée par Pascal le 2026-10-16) | 1 |

Plafond : au plus **3 PR ouvertes** ; un cycle ne démarre que quand le précédent est
fusionné ou en revue sous le plafond. Aucune mission n'est « lâchée » : aucune n'a encore
réussi deux fois.
