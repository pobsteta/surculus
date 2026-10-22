# Mission 034 — Fermer les trous de mémoire relevés au bilan

> Fiche écrite par l'orchestrateur le 2026-10-21, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Décidée par Pascal au bilan du
> 2026-10-21 (journal), d'après le rapport de l'`archiviste`.

## Mission

**En une phrase (quoi, et pourquoi) :** Mettre à jour le README, la spec, les ADR 0004, 0005 et 0009, et ajouter un index des ADR, pour qu'un successeur trouve en moins d'une minute comment construire l'application, ce qui est décidé et ce qui ne l'est pas encore.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Vérifié à la source

Règle du bilan du 2026-10-21. L'orchestrateur a vérifié chaque constat par `grep` sur `main` (ce7d027), le 2026-10-21 ; le `relecteur` les a recontrôlés tous les sept (six exacts, un exact en partie).

| Constat | Lu à | Source du constat |
|---|---|---|
| Le README annonce les écrans de tournée « à venir » ; la 011 est fusionnée (PR #51) | `README.md:19` | archiviste, bilan |
| Le README dit « JDK 17 » ; la CI utilise Temurin 21 | `README.md:45` ; `.github/workflows/ci.yml:26` | archiviste, bilan |
| Spec : « [ADR à venir] » alors que l'ADR 0003, l'ADR 0004 et `docs/formats/sursync-v1.md` existent ; la l. 459 (bibliothèques communes) reste vraie, l'ADR 0008 n'existant pas ; la question 6 du § 10 est tranchée par l'ADR 0004 | `docs/spec.md:7`, `:43`, `:141`, `:236`, `:459` | archiviste, bilan |
| L'ADR 0004 cite osmdroid sans dire qu'il est archivé ; elle copie la commande de tests à la l. 15 (sans `:data`), corrigée plus bas par une mention datée | `docs/adr/0004-stack-kotlin-compose-vosk.md:11`, `:15`, `:78-84` | archiviste, bilan ; relecteur |
| L'ADR 0009 dit encore « Wi-Fi seulement à prévoir dans la 011 » | `docs/adr/0009-carte-hors-ligne.md:110` | décisions (a) à (d) de Pascal, journal du 2026-10-21 |
| Les ADR 0006, 0008 et 0010 sont citées mais absentes de `docs/adr/` | `docs/adr/0005-stockage-et-sauvegarde.md:89` (0006) ; en-têtes de `app/src/main/kotlin/io/github/pobsteta/surculus/gnss/*.kt` et de `core/src/main/kotlin/io/github/pobsteta/surculus/core/{gnss,geo}/*.kt` (0008) ; `docs/publier.md:75` (0010) | archiviste, bilan |
| `docs/publier.md` ne décrit pas l'essai de fumée sur émulateur | `grep -n -i "fumée\|émulateur" docs/publier.md` : aucune ligne | archiviste, bilan |

Règles de la fiche, avec leur auteur :
- **On ajoute, on ne réécrit pas.** Règle du sommier, méthode de l'Impératrice (`CLAUDE.md`, « Les sources de vérité »).
- **Les ADR 0006, 0008 et 0010 ne sont pas écrites par cette mission.** C'est une décision d'architecture, qui revient à Pascal, après le `contradicteur` (`CLAUDE.md`, « Le déroulé », point 1).
- **La clé de signature reste à Pascal.** Décision de Pascal au bilan du 2026-10-21.
- **Le réexamen de l'ADR 0004 face à osmdroid archivé n'est pas fait ici.** Cette mission ajoute seulement un renvoi daté vers l'ADR 0009. Le réexamen reste au bac à sable, pour le `contradicteur`.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : rapport de l'`archiviste` au bilan (journal du 2026-10-21) ; règle du sommier (on ajoute, on ne réécrit pas) | |
| A-t-il les moyens ? | oui ; pour l'ADR 0005, après la fusion de la #56 | ne touche pas à l'ADR 0005 avant |

**Temps de la main :** **appuyer** : l'agent rend la liste de ses modifications avant de les écrire.

## L'Homme

- **Agent :** `archiviste` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :**
  - écrire les ADR 0006, 0008 et 0010 : ce sont des décisions, qui demandent un `contradicteur` et Pascal ; l'index les dit « réservées, non écrites » ;
  - toucher au code, aux tests, aux fixtures ou aux référentiels ;
  - trancher la question de la clé de signature, qui revient à Pascal.

## Harnais

- **Peut :**
  - modifier `README.md`, `docs/spec.md` (seulement les renvois et les balises « à venir »), `docs/publier.md` et `docs/dependances.md` (versions remplacées par des renvois) ;
  - ajouter à `docs/adr/0004`, `docs/adr/0009` (et `docs/adr/0005` après la #56) une section datée « Après … », sans effacer ce qui est écrit ;
  - créer `docs/adr/README.md` (index des ADR) ;
  - ajouter à `fixtures/README.md` une note sur les copies voulues de l'emprise France.
- **Ne doit pas :**
  - réécrire une décision ou effacer une ligne d'une ADR ;
  - copier une information au lieu de la lier ;
  - toucher à `main` ;
  - trancher une question de sens (la remonter à l'orchestrateur).
- **Sources autorisées :** le dépôt ; le journal ; le rapport de l'`archiviste` au bilan.

## Tests

- Le test de la minute, refait sur le résultat : prérequis de build, état des écrans, statut d'osmdroid et liste des ADR se trouvent chacun en moins d'une minute.
- Bilan des écarts avant rendu (règle du bilan) : l'agent liste chaque fichier modifié.

## Terrain

- **Échantillon que Pascal vérifiera :** il refait lui-même le test de la minute sur le README fusionné : prérequis de build, état des écrans, statut d'osmdroid et liste des ADR.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.
- **Point de suivi :** à la remise de la liste des modifications (appuyer), puis à l'ouverture de la PR.

## Comment saura-t-on que ça marche ?

Quelqu'un qui découvre le dépôt construit l'APK de debug et lance les tests en suivant seulement le README.
