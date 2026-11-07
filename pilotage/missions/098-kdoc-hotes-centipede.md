# Mission 098 — Le commentaire des hôtes Centipède renvoie au référentiel

> Fiche écrite par l'orchestrateur le 2026-11-07, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> après la décision de Pascal du même jour (« Garder, avec sa source », PR #163). Depuis `main` (02436c5). Une
> mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Réécrire le commentaire de `CASTERS_CENTIPEDE` pour qu'il renvoie, pour chaque
nom d'hôte, à sa source dans `docs/referentiels/referentiel-centipede.md`, parce qu'il dit encore que
`caster.centipede.fr` n'est qu'un réglage hérité de Groma.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Le constat

- `core/src/main/kotlin/io/github/pobsteta/surculus/core/session/BesoinEtFix.kt` l. 100-104 : « `caster.centipede.fr`,
  réglage par défaut de `ConfigRtk` …, hérité de Groma, que cette page ne nomme pas ».
- Depuis la 097 (#163, 02436c5), le référentiel, rubrique « Hôtes du caster », donne : `crtk.net` (documentation
  Centipède, C-CASTER) ; `caster.centipede.fr` (README de CentipedeRTK/RtkBaseVar, MAVProxy ; même table des points
  de montage que `crtk.net`, constat daté du 2026-11-07). Pascal garde les deux noms.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | **appuyer** |
| A-t-il les moyens ? | oui : un commentaire ; pas de comportement changé, donc pas de mission `testeur` | |

**Temps de la main :** appuyer.

## Ma réponse d'abord

_Rempli par Pascal seul._

## L'Homme

- **Agent :** `dev` seul (aucun comportement ne change : rien à tester de plus).
- **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** changer `CASTERS_CENTIPEDE`, `ConfigRtk` ou un test.

## Harnais

- **Peut :** le commentaire de `CASTERS_CENTIPEDE` dans `BesoinEtFix.kt`.
- **Ne doit pas :** toucher au code exécutable, aux tests, à `docs/`, à `main`.

## Valeur provisoire (chiffre, réglage)

- Aucune.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** 097 (#163, 02436c5). **CI rouge attendue :** aucune.

## Tests

- `git diff` : seules des lignes de commentaire changent. `.claude/test-command` au code 0 ; CI verte.

## Terrain

- Aucun écran.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal (fusion).

## Comment saura-t-on que ça marche ?

Un successeur qui lit `CASTERS_CENTIPEDE` trouve, pour chaque nom, le chemin de sa source.
