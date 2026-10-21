# Mission 034 — Fermer les trous de mémoire relevés au bilan

> Fiche écrite par l'orchestrateur le 2026-10-21, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Décidée par Pascal au bilan du
> 2026-10-21 (journal), d'après le rapport de l'`archiviste`.

## Mission

**En une phrase (quoi, et pourquoi) :** Mettre à jour le README, la spec, les ADR 0004, 0005 et 0009, et ajouter un index des ADR, pour qu'un successeur trouve en moins d'une minute comment construire l'application, ce qui est décidé et ce qui ne l'est pas encore.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Vérifié à la source

Règle du bilan du 2026-10-21 : l'orchestrateur a vérifié chaque constat ci-dessous, sur `main` (8e73ac7 et suivants).

| Constat | Où | Auteur de la règle |
|---|---|---|
| Le README dit « JDK 17 et SDK Android requis » ; la CI utilise Temurin 21 | `README.md` ; `.github/workflows/ci.yml` | archiviste, bilan |
| Le README annonce les écrans de tournée « à venir » ; la 011 est fusionnée | `README.md` ; PR #51 | archiviste, bilan |
| Spec : « [ADR à venir] » aux l. 43, 141 et 236 alors que l'ADR 0003, l'ADR 0004 et `docs/formats/sursync-v1.md` existent ; la question 6 du § 10 est tranchée par l'ADR 0004 | `docs/spec.md` | archiviste, bilan |
| L'ADR 0004 ne dit pas qu'osmdroid est archivé et copie une commande de tests périmée | `docs/adr/0004-*.md` ; `.claude/test-command` | archiviste, bilan |
| L'ADR 0009, l. 110, dit « Wi-Fi seulement à prévoir dans la 011 » | `docs/adr/0009-carte-hors-ligne.md` | décisions (a) à (d) de Pascal, journal du 2026-10-21 |
| Les ADR 0006, 0008 et 0010 sont citées (code GNSS, `docs/publier.md`) mais absentes | `docs/adr/` | kanban, bac à sable |
| `docs/publier.md` ne décrit pas l'essai de fumée sur émulateur | `docs/publier.md` ; `.github/workflows/release.yml` | archiviste, bilan |

Le relecteur vérifie ces lignes avant d'accepter la fiche.

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

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Quelqu'un qui découvre le dépôt construit l'APK de debug et lance les tests en suivant seulement le README.
