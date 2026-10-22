# Mission 036 — Cas des décisions 11 et 12 de la dictée

> Fiche écrite par l'orchestrateur le 2026-10-22, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Décidée par Pascal le 2026-10-21
> (« les cas de 11 et 12 sont à ajouter par le testeur avant la 013 ») ; lancée au cycle 8.

## Mission

**En une phrase (quoi, et pourquoi) :** Ajouter à `fixtures/dictee/contexte.json` les cas d'acceptation des décisions 11 et 12 de Pascal (redire l'état qu'a déjà le plant n'ajoute aucune ligne et la voix confirme ; « non observé » sur un plant qui a déjà un état est une correction et ses mesures tombent avec lui), sans lire de code, pour que la 013 ait tous ses tests avant d'écrire une ligne.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Vérifié à la source

Constats vérifiés par l'orchestrateur sur `main` (162fb8c), le 2026-10-22.

| Constat | Lu à | Source du constat |
|---|---|---|
| Texte des décisions 11 et 12 | `pilotage/journal.md:114` | orchestrateur |
| Texte de la décision 10 (correction `remplace_uuid`, annoncée par la voix) | `pilotage/journal.md:111` | orchestrateur |
| Aucun cas n'existe encore pour 11 ou 12 | `grep -c "n° 11\|n° 12" fixtures/dictee/*.json fixtures/README.md` : 0 partout | orchestrateur |
| Les cas de contexte vont de DI-100 à DI-133 ; le format de la correction porte `correction` et `annonce_vocale` | `fixtures/dictee/contexte.json:6` (note), l. 218-232 (cas de la décision 10) ; `fixtures/README.md:150` | orchestrateur |
| « non observé » est un mot de la grammaire ; un plant non observé ne porte aucune mesure | `docs/spec.md:133` ; `docs/spec.md:416` | orchestrateur |
| Le rejeu est désactivé jusqu'à la 013 ; le test de forme des fixtures est actif | `fixtures/README.md:154` ; `core/src/test/kotlin/io/github/pobsteta/surculus/core/dictee/DicteeRejeuTest.kt:19` | orchestrateur |

Règles, avec leur auteur :
- **Décisions 10, 11 et 12** : Pascal, 2026-10-21 (journal l. 111 et 114).
- **Le testeur écrit les tests à partir de la spec, sans lire le code.** CLAUDE.md.
- **Garde-fou = test vu rouge** et **bilan des écarts avant rendu** : Pascal, bilan du 2026-10-21.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : format des cas de `contexte.json` et de `fixtures/README.md` (section Dictée) | |
| A-t-il les moyens ? | oui : tout est sur `main` | |

**Temps de la main :** **appuyer** : l'agent rend la liste des cas (identifiant, état courant, phrase, attendu) avant de les écrire.

## L'Homme

- **Agent :** `testeur` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :**
  - modifier un cas existant (un cas existant qui contredit 11 ou 12 se remonte à l'orchestrateur) ;
  - trancher ce que la voix dit mot pour mot, ou ce que deviennent les mesures dans le journal au-delà de ce que dit la décision 12 : une question de sens se remonte ;
  - aucun code de production.

## Harnais

- **Peut :** ajouter des cas à `fixtures/dictee/contexte.json` (identifiants à la suite, `DI-134` et au-delà, chacun avec sa `source`) ; compléter `fixtures/README.md` (section Dictée) ; adapter le test de forme si un nouveau champ d'attendu l'exige, en le disant.
- **Ne doit pas :** lire `core/src/main/` ni `app/src/` ; retirer `@Disabled` de `DicteeRejeuTest` ; modifier un cas existant ; toucher à `main`.
- **Sources autorisées :** `pilotage/journal.md` (décisions 10 à 12) ; spec § 4.3, § 5 et § 6 ; `fixtures/README.md`.

## Tests

- Au moins : décision 11 sur chaque état (sain, atteint, mort, absent, non observé) redit à l'identique ; décision 12 sur un plant sain, atteint avec hauteur et cause, mort avec cause ; et le cas « non observé » sur un plant sans état (première saisie, pas une correction).
- `DicteeFixturesBienFormeesTest` reste vert ; **vu rouge** une fois sur un cas mal formé (non commité).
- Commande du projet : `.claude/test-command`.
- **Bilan des écarts** : le rapport liste chaque fichier modifié et chaque champ d'attendu nouveau.

## Terrain

- **Échantillon que Pascal vérifiera :** la liste des nouveaux cas, en une ligne chacun : « plant atteint, je dis "atteint" : aucune ligne, la voix confirme ». Il dit si c'est ce qu'il attend en forêt.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.
- **Point de suivi :** à la remise de la liste des cas, puis à l'ouverture de la PR.

## Comment saura-t-on que ça marche ?

La 013 branche ces cas sans les modifier, et ils passent au vert.
