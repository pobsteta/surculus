# Mission 019 — Scénarios du journal en ajout seul

> Fiche écrite par l'orchestrateur le 2026-10-17, à partir du gabarit
> `docs/modeles/fiche-mission.md`. Accompagne la mission 008 (règle : le `testeur` écrit
> les tests depuis la spec, sans lire le code).

## Mission

**En une phrase (quoi, et pourquoi) :** Écrire, à partir de l'ADR 0005, de la spec § 5 (« Journal des saisies ») et des arbitrages du 2026-10-17, et sans lire le code, les scénarios sourcés du journal (annulation et annulation d'annulation, corrections concurrentes signalées, deux racines, un état qui n'efface pas un pointage, remplacement inconnu, ordre total indépendant de l'ordre d'arrivée, horloge reculée d'une heure, saut d'horloge noté, jour civil au changement d'heure du 25 octobre 2026 calculé sur l'heure GNSS quand elle existe), pour que le journal de la mission 008 ne s'auto-évalue pas.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : format des fixtures de la 002 (`fixtures/README.md`) | |
| A-t-il les moyens ? | oui : ADR 0005, spec § 5, tzdata | |

**Temps de la main :** **appuyer** : l'agent rend la liste des cas avant d'écrire.

## L'Homme

- **Agent :** `testeur` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** aucun code de production ; ne lit pas le code de la 008.

## Harnais

- **Peut :** écrire `fixtures/journal/*.json`, au format des fixtures existantes : chaque cas a un `id`, une description, une suite de lignes de journal en entrée, et un état attendu par (objet, type), avec les conflits et les sauts notés ; compléter `fixtures/README.md`.
- **Ne doit pas :** écrire de test Kotlin (la 008 branchera ses tests sur ces fixtures) ; inventer une règle absente de l'ADR, de la spec ou des arbitrages, qu'il remonte plutôt.

## Tests

- Chaque règle de la spec § 5, « Journal des saisies », et de l'ADR 0005 (journal, projection, ordre total, temps) a au moins un cas ; chaque heure attendue porte sa source (tzdata).

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.
