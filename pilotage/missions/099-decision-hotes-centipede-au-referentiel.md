# Mission 099 — La décision sur `caster.centipede.fr` inscrite au référentiel

> Fiche écrite par l'orchestrateur le 2026-11-07, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> après la décision de Pascal du même jour (« Fusionner + décision au référentiel », PR #164). Depuis `main` (7a5e3f0).

## Mission

**En une phrase (quoi, et pourquoi) :** Inscrire dans `docs/referentiels/referentiel-centipede.md` la décision de
Pascal du 2026-11-07 de garder `caster.centipede.fr` comme hôte Centipède, parce que la rubrique « Reste pour
Pascal » la laisse encore ouverte alors que le code (`CASTERS_CENTIPEDE`, 098) y renvoie.

**Redite par l'agent :** « inscrire au référentiel Centipède la décision de Pascal du 2026-11-07 de garder
`caster.centipede.fr`, pour que le renvoi du code trouve un verdict et non une question ouverte » (redite de
l'`acquereur`, recopiée par l'orchestrateur : son harnais ne lui permet d'écrire que dans le référentiel).

## Ce qui est demandé

- Rubrique « Hôtes du caster », « Reste pour Pascal » : remplacer la question ouverte par la décision : « Gardé par
  décision de Pascal, 2026-11-07 (PR #163, `pilotage/journal.md`) », avec ses raisons : README de
  CentipedeRTK/RtkBaseVar, même table des points de montage que `crtk.net` (constat daté), réglage par défaut de
  `ConfigRtk` (`app/src/main/kotlin/io/github/pobsteta/surculus/gnss/ConfigRtk.kt`).
- Garder la piste ouverte non tranchée : la confirmation écrite de l'équipe Centipède n'est pas demandée.
- Rien d'autre ne change.

## L'Homme et le harnais

- **Agent :** `acquereur` (seul à écrire dans `docs/referentiels/`), sonnet ; temps de la main : appuyer.
- **Peut :** écrire dans le référentiel. **Ne doit pas :** toucher au code, à `main`, inventer une source.

## Valeur provisoire, dépendances

- Aucune valeur provisoire. **Dépend de :** 097 (#163), 098 (#164). **CI rouge attendue :** aucune.

## Tests, terrain

- Diff limité à la rubrique ; CI verte. Aucun écran.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal (fusion).

## Comment saura-t-on que ça marche ?

Le successeur qui suit le renvoi de `CASTERS_CENTIPEDE` lit la décision, sa date et ses raisons.
