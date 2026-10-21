# Mission 032 — État par tournée, tournée obligatoire à la saisie

> Fiche écrite par l'orchestrateur le 2026-10-21, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Décidée par Pascal le 2026-10-21, à la
> relecture de la PR #50 : le travail touche le domaine (projection du journal), pas
> l'écran, et sort donc de la 012. À fusionner avant la 012.

## Mission

**En une phrase (quoi, et pourquoi) :** Calculer l'état courant du journal par tournée et faire refuser à `saisir` un code de tournée vide, pour que l'état d'un plant dans une tournée ne dépende jamais d'une autre tournée et qu'aucune saisie ne sorte des sauvegardes sans signal.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : projection de la 008 (`data/journal/`), champ `code_tournee` de la 031, spec § 5 | |
| A-t-il les moyens ? | oui : 008 et 031 fusionnées | |

**Temps de la main :** **appuyer** : l'agent rend son plan avant d'écrire. Le plan dit
en particulier comment `saisir` refuse un code vide en production sans casser les tests
existants qui s'appuient sur le défaut `""`. Il ne retouche ni les assertions ni les
fixtures sans l'accord de Pascal.

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** aucun écran (012) ; aucun branchement de la sauvegarde sur les saisies ; aucune fusion entre tournées (ADR 0006).

## Harnais

- **Peut :** modifier `data/` (journal, projection, `JournalRepository`) ; amender la spec § 5 et l'ADR 0005.
- **Ne doit pas :** modifier ou supprimer une ligne du journal ; changer le schéma Room sans le dire au plan ; casser un test existant ; modifier les fixtures du testeur sans accord ; toucher à `main` ; trancher une question de sens (la remonter à l'orchestrateur).
- **Sources autorisées :** spec § 5 ; ADR 0005 ; fiches 008 et 031 ; journal du 2026-10-21.

## Règles (journal du 2026-10-21)

- **État par tournée (décision de Pascal, « projection »).** L'état courant d'un plant est calculé par tournée. Chaque tournée part de ce que sommieR livre dans son `travaux.gpkg`, et le journal d'une autre tournée n'influe jamais.
- **Tournée obligatoire (dette du `relecteur` de la 031, endossée par Pascal).** En production, `saisir` refuse un code de tournée vide.

## Tests

- Un même `objet_uuid` saisi dans deux tournées : l'état de chacune ne dépend que de ses propres lignes.
- `saisir` avec un code vide est refusé en production.
- Tous les tests existants restent verts. Commande du projet : `.claude/test-command`.

## Terrain

- **Échantillon que Pascal vérifiera à la fusion :** la sortie du test « même `objet_uuid` dans deux tournées » (états distincts affichés dans le rapport de test), et un appel de `saisir` sans tournée refusé. La vérification au téléphone (marquer un plant mort dans A, le voir intact dans B) passe à la 012, qui apporte l'écran.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

À la 012, sur un téléphone avec deux tournées qui partagent un plant, marquer ce plant mort dans A ne change pas son état dans B.
