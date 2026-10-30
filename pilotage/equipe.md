# Équipe de Surculus

> Produit par l'orchestrateur avec `/mission`, **validé par Pascal** avant tout démarrage.
> Appliquer la règle de staffing de `imperatrice/skills/methode/SKILL.md` : un agent par mission
> qui passe les trois questions, jamais un agent « par principe ».

- **Statut :** validée (porte n° 2)
- **Validée par :** Pascal, le 2026-10-15 (PR #13)

## Plafond humain

- **PR ouvertes en même temps, au plus :** 3
- **Missions de code en parallèle, au plus :** 2 (bilan du 2026-10-21, décision de Pascal)
- **PR de pilotage** (journal, kanban, fiches) : **lâchées**, hors plafond. L'orchestrateur
  les fusionne quand le `relecteur` les juge prêtes et que la CI est verte ; une PR qui
  porte une décision au nom de Pascal reste à Pascal (bilan du 2026-10-21).
- **Terrain avant la 012** : les vérifications de terrain en attente sont faites par
  Pascal et inscrites au journal avant de lancer la 012 (bilan du 2026-10-21).
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
| [017](missions/017-icone-s-new-rocker.md) Icône « S » en New Rocker | `dev` | sonnet | appuyer | oui (demandée par Pascal le 2026-10-16) | 1 |
| [018](missions/018-ecran-essai-gnss.md) Écran d'essai GNSS | `afficheur` | sonnet | appuyer | 3 oui (après 003) | 2 |
| [019](missions/019-scenarios-ajout-seul.md) Scénarios du journal en ajout seul | `testeur` | sonnet | appuyer | 3 oui | 3 |
| [023](missions/023-methode-en-plugin-et-pilotage.md) Méthode en plugin, pilotage rangé | orchestrateur | — | appuyer | 3 oui (demandée par Pascal le 2026-10-20) | — |

## Règles adoptées au bilan du 2026-10-21 (décision de Pascal)

Elles répondent aux trois causes du Pareto (`analyste`, journal du 2026-10-21) :

1. **Vérifié à la source.** Chaque fiche porte une section « Vérifié à la source » : pour
   chaque fait ou prérequis, le `fichier:ligne` lu ou la commande lancée ; pour chaque
   règle, son auteur (Pascal, spec, relecteur). Le `relecteur` refuse une fiche sans elle.
2. **Garde-fou = test vu rouge.** Un garde-fou d'ADR ou de spec n'existe que s'il a un test
   nommé, montré rouge une fois.
3. **Bilan des écarts avant rendu.** L'agent colle dans son rapport la liste des
   assertions, fixtures et fichiers hors périmètre qu'il a modifiés. Un hook automatique
   relèverait du plugin `imperatrice`.

Plafond : au plus **3 PR ouvertes** ; un cycle ne démarre que quand le précédent est
fusionné ou en revue sous le plafond. Seules les PR de pilotage sont « lâchées » (bilan du
2026-10-21) ; aucune mission de code ne l'est. Le tableau ci-dessus date de la porte
n° 2 : l'état à jour des missions est dans `kanban.md`.

## Règles adoptées au bilan du 2026-10-30 (décision de Pascal)

1. **Le `testeur` revient** sur chaque mission qui change un comportement : il écrit ses tests depuis la
   fiche, sans lire le code (écart du journal l. 665 : absent depuis la 012-D2).
2. **Le `relecteur` contrôle le texte** : chaque chiffre et chaque libellé du corps de PR est vérifié contre
   le code avant le verdict (≈ 25 allers-retours sur ≈ 51 pour un texte faux depuis le 2026-10-21 ; la 078
   en est l'exemple, « avec / sans plein écran » inversé).
3. **Une fiche ne part que sur sa branche de mission**, jamais en double sur une branche de pilotage
   (fiches en double du cycle 15, quatre conflits).
4. **Rythme inchangé** (« On continue comme avant ») : plafonds tels quels ; le terrain attend une date fixée
   par Pascal.

## Missions validées au `/mission` du 2026-10-30 (porte n° 2, « Oui, les trois »)

| Mission | Agent | Modèle | Temps de la main | Les 3 questions |
|---|---|---|---|---|
| [079](missions/079-readme-et-spec-au-niveau-du-livre.md) README et spec au niveau du livré | `archiviste` | sonnet | appuyer | 3 oui |
| [080](missions/080-test-de-rendu-des-ecrans.md) Test de rendu des écrans | `afficheur` | sonnet | tenir (plan d'abord) | non à la 2e, partiel à la 3e (outil de test Compose absent de `app`) |
| [081](missions/081-aucun-texte-technique-a-l-ecran.md) Aucun texte technique à l'écran | `testeur` puis `dev` | sonnet | appuyer | 3 oui |

Hors équipe, au Planifié : ADR 0010 (attend la sauvegarde de la clé par Pascal), 049, photos (mesures sur appareil),
tranche 3 (date de plantation), 010 et 014 (moyens).

