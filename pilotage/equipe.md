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

Hors équipe, au Planifié : ADR 0010 (proposée, PR #180 ; attend les actions de Pascal, journal du 2026-11-10), 049, photos (mesures sur appareil),
tranche 3 (date de plantation), 010 et 014 (moyens).

## Missions de la 082 à la 106 (ajout du 2026-11-09, mission 105)

Le tableau de la porte n° 2 s'arrêtait à la 081. Les missions suivantes sont nées de décisions de Pascal au fil des
cycles (journal) ; le **temps de la main** est celui de la fiche ; l'état à jour de chacune est dans `kanban.md`, pas ici.
Le `relecteur` relit chaque PR, le `testeur` écrit d'abord ses tests sans lire le code (règle du bilan du 2026-10-30).

| Mission | Agent | Temps de la main |
|---|---|---|
| [082](missions/082-aucun-message-d-exception-a-l-ecran.md) Aucun message d'exception à l'écran | `testeur` puis `dev` | appuyer |
| [083](missions/083-bandeau-ne-cache-plus-plant-precedent.md) Bandeau qui ne cache plus « Plant précédent » | `testeur` puis `afficheur` | appuyer |
| [084](missions/084-refus-de-lecture-en-francais.md) Refus de lecture en français | `testeur` puis `dev` | appuyer |
| [085](missions/085-ouvrir-sans-position-adr-et-spec.md) Ouvrir sans position : ADR et spec | `archiviste` | tenir |
| [086](missions/086-code-de-piquet-en-double-entre-ug.md) Code de piquet en double entre UG | `testeur` puis `dev` | appuyer |
| [087](missions/087-ouvrir-un-controle-sans-position.md) Ouvrir un contrôle sans position | `testeur` puis `dev` | tenir |
| [088](missions/088-sauvegarde-sursync-v2.md) Sauvegarde `.sursync` v2 | `testeur` puis `dev` | tenir |
| [089](missions/089-ouvrir-sans-position-depuis-le-guidage.md) Ouvrir sans position depuis le guidage | `testeur` puis `dev` | tenir |
| [090](missions/090-rendre-la-tournee-depuis-le-telephone.md) Rendre la tournée depuis le téléphone | `testeur` puis `dev` | tenir |
| [091](missions/091-besoin-concurrence-et-qualite-du-fix.md) Besoin, concurrence et qualité du fix | `testeur` puis `dev` | tenir |
| [092](missions/092-referentiel-des-concurrences.md) Référentiel des concurrences | `acquereur` | appuyer |
| 093 Rendre les points hors forêt | — | abandonnée (kanban, 2026-11-06) |
| [094](missions/094-referentiel-de-la-source.md) Référentiel de la source | `dev` (plan) puis `testeur` | tenir |
| [095](missions/095-terminer-plant-sans-etat.md) « Terminer » refusé tant qu'un plant n'a pas d'état | `testeur` puis `dev` | appuyer |
| [096](missions/096-tests-muets-session-controle.md) Tests muets de `SessionControle` | `testeur` | appuyer |
| [097](missions/097-hotes-centipede-sources.md) Hôtes Centipède sourcés | `acquereur` | appuyer |
| [098](missions/098-kdoc-hotes-centipede.md) Commentaire de `CASTERS_CENTIPEDE` | `dev` | appuyer |
| [099](missions/099-decision-hotes-centipede-au-referentiel.md) Décision sur les hôtes au référentiel | `acquereur` | appuyer |
| [100](missions/100-double-appui-ouvrir-sans-position.md) Double appui sur « Ouvrir sans position » | `testeur` puis `dev` | appuyer |
| [101](missions/101-seconde-ouverture-reprend-le-controle.md) Un contrôle par placette et par jour, création atomique | `testeur` puis `dev` | tenir |
| [102](missions/102-libelles-des-codes-lisibles.md) Libellés des codes lisibles à « Terminer » | `testeur` puis `afficheur` | tenir |
| [103](missions/103-placette-introuvable-en-francais.md) Placette introuvable en français | `testeur` puis `dev` | appuyer |
| 104 Audit des gardes de rendu et de forme (fiche sur sa branche) | `testeur` | appuyer |
| [105](missions/105-docs-au-niveau-de-la-v0-7-126.md) Docs au niveau de la v0.7.126 | `archiviste` | appuyer |
| [106](missions/106-liste-de-controle-avant-relecture.md) Liste de contrôle avant relecture | `archiviste` | appuyer |

Les missions 010 et 014 restent en attente de moyens ; la 049, les photos et la tranche 3 au Planifié (kanban).

## Règles adoptées au bilan du 2026-11-09 (décision de Pascal, journal du même jour)

1. **Terrain d'abord** : essai en forêt d'ici le 2026-11-15 ; aucune mission d'écran d'ici là, seulement les dettes du bilan.
2. **Liste de contrôle** de l'orchestrateur (section suivante, mission 106), cause n° 1 du Pareto.
3. **Textes et bornes décidés au plan** (cause n° 2, journal l. 1151 point 3) : toute fiche d'écran porte une rubrique
   « textes et bornes décidés au plan », **tranchée par Pascal avant le code**. Décision de Pascal
   (2026-11-09) : l'orchestrateur l'ajoute à chaque fiche d'écran ; le `dev` ou l'`afficheur` la remplit au plan ; Pascal la
   tranche avant le code ; le `relecteur` met « à reprendre » toute PR d'écran dont la fiche n'a pas les textes décidés.
4. **Audit des gardes** (cause 3, « une garde n'est prouvée que vue rouge ») : mission 104 du `testeur`.

## Liste de contrôle de l'orchestrateur (bilan du 2026-11-09, décision de Pascal)

Avant chaque relecture et chaque question à Pascal : [`liste-de-controle.md`](liste-de-controle.md). Les manquements
s'inscrivent au journal en ligne `écart` et sont comptés au bilan.
