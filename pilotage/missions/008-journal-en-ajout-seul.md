# Mission 008 — Journal en ajout seul et projection

> Fiche écrite par l'orchestrateur le 2026-10-15 (`/mission`, porte n° 2), à partir du
> gabarit `imperatrice/skills/mission/fiche-mission.md`. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Écrire dans `:data` le journal Room en ajout seul (UUID v7, séquence locale, `synchronous=FULL`, aucune migration destructive, schémas exportés) et sa projection par (objet, type) avec annulation, ordre total et conflits signalés, pour qu'aucune saisie validée ne se perde ni ne s'écrase.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? (sources, conventions nommées ci-dessous) | oui : ADR 0005 | |
| A-t-il les moyens ? (outils, données d'essai, tests) | oui : tests instrumentés et tests JVM de la projection | |

**Temps de la main :** **tenir** : modèle de données durable.

## L'Homme

- **Agent :** `dev`
- **Modèle :** opus
- **Ce qui ne lui est pas demandé :** pas de sauvegarde hors appareil (009, 010).

## Harnais

- **Peut :** écrire les entités, DAO et la projection ; tests JVM pour la projection pure.
- **Ne doit pas :** sortir du périmètre de cette phrase ; inventer un code (travaux, essence, état) ; toucher à `main` ; modifier sommieR, Marculus ou Groma ; trancher une question de sens (la remonter à l'orchestrateur).
- **Sources autorisées :** ADR 0005 ; Marculus en lecture seule (à ne pas recopier : repli destructif, `exportSchema = false`).

## Tests

- **Comment saura-t-on que c'est juste :** annulation ; deux corrections concurrentes signalées ; horloge reculée d'une heure ; jour civil Europe/Paris au changement d'heure du 25 octobre 2026 ; une migration testée. Commande du projet : `.claude/test-command`.

## Terrain

- **Échantillon que Pascal vérifiera :** Pascal coupe l'appareil en pleine saisie : la dernière saisie validée est là au redémarrage.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal.
- **Point de suivi :** à la remise du plan (missions tenues) et à l'ouverture de la PR.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

les tests de projection sont verts et l'essai de coupure réussit.
