# Mission 021 — Cas des remplacements anormaux du journal

> Fiche écrite par l'orchestrateur le 2026-10-17, à partir du gabarit
> `imperatrice/skills/mission/fiche-mission.md`. Complète la mission 019 après la relecture de la
> PR #35 (008) : le `dev` avait codé, sans arbitrage, une règle pour une correction qui
> désigne une ligne d'un autre objet. Pascal l'a tranchée ; le `testeur` écrit les cas,
> sans lire le code, et les verse sur la branche de la 008 (même PR, plafond humain atteint).

## Mission

**En une phrase (quoi, et pourquoi) :** Écrire, à partir des décisions de Pascal du 2026-10-17 et sans lire le code de la 008, les scénarios sourcés des remplacements anormaux du journal (une saisie dont `remplace_uuid` désigne une ligne d'un autre objet, ou une ligne d'un autre type du même objet : la ligne désignée garde sa valeur, la saisie devient tête de sa propre clé, une anomalie est rattachée à cette clé), pour que le journal de la 008 ne s'auto-évalue pas.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : format de `fixtures/journal/*.json` (mission 019), `fixtures/README.md` | |
| A-t-il les moyens ? | oui : ADR 0005, spec § 5, journal du 2026-10-17 | |

**Temps de la main :** **appuyer** : l'agent rend la liste des cas avant d'écrire.

## L'Homme

- **Agent :** `testeur` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** aucun code (ni de production, ni de test Kotlin) ; ne lit pas `data/src/`.

## Harnais

- **Peut :** ajouter des cas dans `fixtures/journal/` (fichier existant le plus proche, ou nouveau fichier au même format), compléter `fixtures/README.md` ; nouveaux identifiants JR- à la suite des existants.
- **Ne doit pas :** modifier un cas existant ; inventer une règle (il la remonte) ; toucher à `main`.

## Décisions de Pascal à couvrir (2026-10-17)

- Une saisie (correction ou annulation) dont `remplace_uuid` désigne une ligne connue d'**un autre objet** : la ligne désignée garde sa valeur ; la saisie est tête de sa propre clé ; anomalie `remplace_autre_objet` rattachée à cette clé (même traitement qu'un remplacement inconnu).
- Une saisie de type X dont `remplace_uuid` désigne une ligne de type Y **du même objet** : la clé Y garde sa valeur ; la saisie est tête de sa propre clé (objet, X) ; anomalie `remplace_autre_type` rattachée à cette clé.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Les nouveaux cas sont actifs et verts dans `:data:testDebugUnitTest`, branchés par le `dev` de la 008 sans être modifiés.
