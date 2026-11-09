# Mission 106 — Liste de contrôle avant relecture

> Fiche écrite par l'orchestrateur le 2026-11-09, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> après la décision de Pascal au bilan du 2026-11-09 (cause n° 1 du Pareto, 51 % des échecs, « Oui, la liste »).
> Depuis `main`.

## Mission

**En une phrase (quoi, et pourquoi) :** Écrire `pilotage/liste-de-controle.md`, une liste courte que l'orchestrateur
applique avant chaque relecture et avant chaque question à Pascal, pour qu'aucune affirmation n'atteigne Pascal sans
avoir été vérifiée à sa source (cause n° 1 du bilan : 26 incidents sur 51).

**Redite par l'agent :**
- *Archiviste :* j'écris `pilotage/liste-de-controle.md`, une liste de six points au plus, chacun relié à un incident du journal et à une commande de vérification, que l'orchestrateur applique avant chaque relecture et avant chaque question à Pascal, et je la lie depuis `pilotage/equipe.md`.

## Les trois questions

| Question | Réponse | Conséquence |
|---|---|---|
| Redire en une phrase ? | oui | |
| Sait-il comment ? | oui : rapport de l'`analyste` (journal, bilan du 2026-11-09, cause 1, avec ses exemples) | |
| Les moyens ? | oui : journal | |

**Temps de la main :** appuyer.

## Ma réponse d'abord

_Rempli par Pascal seul._

## L'Homme

- **Agent :** `archiviste`, sonnet.
- **Ce qui ne lui est pas demandé :** modifier le plugin `imperatrice` ; une liste longue (dix points au plus, chacun
  vérifiable en une commande ou une lecture).

## Harnais

- **Peut :** écrire `pilotage/liste-de-controle.md`, avec au moins ces points, tirés des incidents du journal : corps
  de PR relu contre le diff (chaque phrase affichée, chaque chiffre) ; preuve annoncée ouverte dans l'artefact avant de
  l'écrire ; cible `adb` vérifiée (`adb devices`, `-s` du téléphone) avant d'installer ; fait d'un agent cité avec sa
  ligne de code avant d'en faire une question ; chiffre d'une question à Pascal vérifié (version, date, nombre). Pour
  chaque point : l'incident d'origine (ligne du journal) et la commande de vérification. Ajouter le lien depuis
  `pilotage/equipe.md`. Dire comment le bilan comptera les manquements (ligne `écart` au journal).
- **Ne doit pas :** effacer le journal ; modifier le code ; changer une règle de `CLAUDE.md` (la proposer dans la PR).
- **Sources :** journal (lignes citées par l'`analyste` au bilan du 2026-11-09), `CLAUDE.md`.

## Valeur provisoire

Aucune.

## Dépend de / rouge attendu jusqu'à

Aucune dépendance. Une PR, en brouillon jusqu'à la fin.

## Tests

CI verte (docs seules).

## Terrain

Sans objet.

## Qui veille, qui tranche

- **Relecteur :** `relecteur` (chaque point relié à un incident réel du journal), puis Pascal.
- **Critère d'arrêt :** 3 allers-retours. **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Au prochain bilan, l'`analyste` compte les affirmations non vérifiées ; la cause 1 descend sous 26.
