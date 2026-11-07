# Mission 100 — Un double appui sur « Ouvrir sans position » n'ouvre qu'un contrôle

> Fiche écrite par l'orchestrateur le 2026-11-07, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> après la décision de Pascal du même jour (cycle 19, « Double appui « Ouvrir sans position » »). Depuis `main`
> (30c2335). Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Faire qu'un double appui sur « Ouvrir sans position », au guidage comme à
l'écran de contrôle, n'ouvre et n'écrive qu'un seul contrôle, parce qu'avec des gants deux appuis arrivent et que les
données rendues à sommieR ne doivent pas en garder la trace.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Le constat

- `app/src/main/kotlin/io/github/pobsteta/surculus/controle/OuvertureSansPosition.kt` (30c2335), l. 116 : le bouton de la
  confirmation (`ConfirmationSansPosition`) n'a pas de verrou pendant l'ouverture ; relevé par le `relecteur` de la
  #162 (« double appui … lance deux ouvertures, risque déjà présent dans la 087 »).
- **Ce qui s'écrit alors n'est pas vérifié** (deux lignes au journal ? refus « un contrôle existe déjà ce jour-là »,
  `SessionControle.kt` l. 542, levé en exception ? rien ?). C'est la première tâche du `testeur`.
- Règle (spec § 4, § 6) : un contrôle par placette et par jour civil (Europe/Paris) dans une tournée.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui une fois le défaut montré | **appuyer** |
| A-t-il les moyens ? | oui : `BancRendu`, `BancControle`, tests de la 087 et de la 089 | le `testeur` montre d'abord le défaut |

**Temps de la main :** appuyer. Si le `testeur` montre une écriture double ou un plantage, l'orchestrateur le dit à
Pascal avant que le `dev` code.

## Ma réponse d'abord

_Rempli par Pascal seul._

## L'Homme

- **Agents :** `testeur` d'abord (tests depuis la spec et cette fiche, **sans lire** `OuvertureSansPosition.kt`,
  `EcranControle.kt`, `EcranGuidage.kt` ni `SessionControle.kt` ; il peut lire les tests de la 087 et de la 089 pour
  leurs bancs), puis `dev`.
- **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** les autres mineurs de la 089 (rotation, `debutSansFix`) ; d'autres boutons.

## Harnais

- **Testeur peut :** tests dans `app/src/test/` et `core/src/test/` ; vus rouges.
- **Dev peut :** `OuvertureSansPosition.kt` et ce qu'il faut de `EcranControle.kt` / `EcranGuidage.kt` pour le verrou ;
  ses tests.
- **Ne doivent pas :** toucher à `docs/`, à `main` ; réécrire les tests de l'autre.

Si un test du `testeur` contredit la spec, **le `dev` s'arrête et le dit tout de suite.**

## Valeur provisoire (chiffre, réglage)

- Aucune.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** 089 (#162, dc62557). **PR en brouillon jusqu'à :** la fin du `dev`. **CI rouge attendue :** les
  tests du `testeur`, entre son push et celui du `dev`.

## Tests

- Deux appuis rapides sur « Ouvrir sans position » à la confirmation, au guidage puis à l'écran de contrôle : un seul
  contrôle ouvert, une seule écriture au journal, aucun plantage.
- Le bouton ne répond plus pendant l'ouverture ; après un refus, il répond de nouveau.
- `.claude/test-command` au code 0 ; CI verte.

## Terrain

- **Échantillon :** sur la release, au guidage sans fix (micro-ondes), deux appuis rapides sur « Ouvrir sans
  position » à la confirmation : un seul contrôle ; puis le rendu ne porte qu'une ligne pour cette placette.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Arrêt de la relecture :** 3 allers-retours. **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Avec des gants, l'agent peut appuyer deux fois : la tournée rendue ne porte qu'un contrôle.
