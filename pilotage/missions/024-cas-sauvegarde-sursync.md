# Mission 024 — Cas d'acceptation de la sauvegarde `.sursync`

> Fiche écrite par l'orchestrateur le 2026-10-20, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Décidée par Pascal le 2026-10-20 :
> le `testeur` écrit les cas de la 009 avant que le `dev` ne code, pour que la sauvegarde
> ne s'auto-évalue pas (défaut relevé sur la 006).

## Mission

**En une phrase (quoi, et pourquoi) :** Écrire, à partir de la spec (§ 4.8) et de l'ADR 0005, sans lire de code, les cas d'acceptation sourcés de la sauvegarde `.sursync` et de sa restauration (journal restauré à la ligne près, version N-1 lue et N+1 refusée, fichier tronqué ou altéré refusé, nom de fichier sans `:` ni nom de propriétaire ou d'agent, tournée existante jamais écrasée, déclenchement par unité de travail et toutes les N saisies ou M minutes), pour que la mission 009 ait ses tests avant son code.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : format des cas de `fixtures/journal/` et `fixtures/acceptation-export/` (missions 019, 020), `fixtures/README.md` | |
| A-t-il les moyens ? | oui : spec § 4.8, ADR 0005, journaux de `fixtures/journal/` comme entrées | |

**Temps de la main :** **appuyer** : l'agent rend la liste des cas avant de les écrire.

## L'Homme

- **Agent :** `testeur` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** le format binaire du `.sursync` (`docs/formats/sursync-v1.md` et son JSON Schema sont l'affaire du `dev` de la 009) ; les voies hors appareil (010) ; aucun code de production.

## Harnais

- **Peut :** créer `fixtures/sauvegarde/` (cas au format JSON des missions 019 et 020, identifiants `SV-NN`, chacun avec sa `source`), compléter `fixtures/README.md`, écrire des tests Kotlin désactivés (`@Disabled("en attente de la mission 009")`) si l'interface attendue se décrit sans lire de code.
- **Ne doit pas :** lire `data/src/` ni `app/src/` ; modifier un cas existant ; inventer une règle absente de la spec ou de l'ADR 0005 (il la remonte à l'orchestrateur, avec sa proposition) ; fixer N et M (valeurs de la 009, à mesurer) ; toucher à `main`.
- **Sources autorisées :** spec § 4.8 et § 6 ; ADR 0005 ; `pilotage/journal.md` ; Marculus `ExportFichier.kt` en lecture seule, pour les noms de fichier.

## Tests

- Chaque cas cite sa source (section de la spec, ligne de l'ADR 0005 ou inscription datée du journal). Les cas restent verts ou désactivés : `.claude/test-command` passe.

## Terrain

- Pascal lit la liste des cas et y reconnaît une journée de tournée perdue puis retrouvée.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Le `dev` de la 009 branche ces cas sans les modifier, et ils passent au vert.
