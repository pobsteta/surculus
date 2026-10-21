# Mission 024 — Cas d'acceptation de la sauvegarde `.sursync`

> Fiche écrite par l'orchestrateur le 2026-10-20, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Décidée par Pascal le 2026-10-20 :
> le `testeur` écrit les cas de la 009 avant que le `dev` ne code, pour que la sauvegarde
> ne s'auto-évalue pas (défaut relevé sur la 006).

## Mission

**En une phrase (quoi, et pourquoi) :** Écrire, à partir de la spec (§ 4.8) et de l'ADR 0005, sans lire de code, les cas d'acceptation sourcés de la sauvegarde `.sursync` et de sa restauration (journal restauré à la ligne près, version N-1 lue et N+1 refusée, fichier tronqué ou altéré refusé, nom de fichier sans `:` ni nom de propriétaire ou d'agent, tournée existante jamais écrasée, déclenchement par unité de travail et toutes les N saisies ou M minutes), pour que la mission 009 ait ses tests avant son code.

**Redite par l'agent (reprise du 2026-10-21) :** je reprends les cas SV de la sauvegarde `.sursync` pour les aligner sur les six décisions de Pascal du 2026-10-21 (fichier abîmé : lignes saines gardées et fautives signalées, jamais de refus en entier sauf version N+1 ou illisible ; nom d'appareil opaque ; empreinte signalée ; jamais d'écrasement ; rotation par tournée ; pas de sauvegarde sans saisie), sans lire de code, afin que la 009 ait des tests justes avant son code.

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

## Reprise après la relecture de la PR #40 (2026-10-21)

Verdict du `relecteur` : **à reprendre**. Les cas sont sérieux et sourcés, mais SV-24
contredit un arbitrage déjà rendu.

**Décisions de Pascal du 2026-10-21, numérotées comme au journal** (source :
« pilotage/journal.md, 2026-10-21 ») :
1. `<appareil>` dans le nom du fichier est un **identifiant opaque**. Sa forme n'est pas
   fixée : un segment non vide, sans `:` ni nom de personne.
2. Le `.sursync` porte une **empreinte SHA-256** de son contenu. Une empreinte fausse est
   **signalée** (avertissement), elle ne fait pas refuser le fichier en entier (voir 5).
3. Un fichier de même nom n'est **jamais écrasé** : le nouveau est suffixé.
4. La rotation des 5 dernières sauvegardes ne supprime que **la plus ancienne de la même
   tournée**.
5. **Fichier abîmé : on garde les lignes saines.** Un `.sursync` tronqué, à ligne
   illisible, à empreinte fausse ou à uuid en double au contenu différent n'est **jamais
   refusé en entier**. Les lignes valides sont restaurées, les lignes fautives refusées et
   signalées, comme dans le journal (JR-13, JR-14 : les deux versions d'un même uuid à
   contenu différent sont refusées et signalées par l'anomalie `uuid_contenu_different`).
   Seule une version N+1, ou une version illisible, fait refuser le fichier.
6. **Pas de sauvegarde sans saisie nouvelle**, même quand M minutes sont passées (SV-54
   n'est plus une hypothèse, SV-52 en dépend).

**À corriger :**
- SV-24 et SV-25 : à aligner sur la décision 5 et sur JR-13 et JR-14. Citer JR-13, pas JR-12.
  `uuid_contenu_different` reste un code d'anomalie, pas un motif de refus.
- SV-23 : une empreinte fausse donne un avertissement, avec les lignes valides restaurées.
- SV-42 : citer la décision 4.
- SV-30 : desserrer le gabarit (segment d'appareil non vide sans `:`, horodatage sans `:`),
  ou marquer en `hypothese` ce qui reste non décidé. Retirer la glose « UUID court ou
  empreinte ».
- SV-33 : la forme du nom reste libre ; seuls les caractères interdits sont vérifiés. Ce
  choix de l'orchestrateur suffit à la 009.
- Codes de refus et d'avertissement, fixés par l'orchestrateur : `version_future`,
  `version_illisible` (refus du fichier) ; `fichier_tronque`, `ligne_illisible`,
  `empreinte_differente` (signalement, lignes saines gardées).
- Test de forme : retirer ou rendre utile la vérification des noms, qui ne lit qu'une
  entrée ; détecter N et M à un chiffre ; corriger « cinq familles » en six.
- Écrire la redite de la mission dans cette fiche.
- SV-55 (photos) est accepté, puisqu'il est sourcé.
