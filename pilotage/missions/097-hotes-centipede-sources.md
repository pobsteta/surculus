# Mission 097 — Les hôtes du caster Centipède, chacun avec sa source

> Fiche écrite par l'orchestrateur le 2026-11-07, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> après la décision de Pascal du même jour (« Mission à l'acquéreur »). Depuis `main` (c3c5985). Une mission = une
> phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Trouver et citer une source publiée pour chaque nom d'hôte du caster Centipède
que Surculus reconnaît, dans `docs/referentiels/referentiel-centipede.md`, parce que ce nom décide si une position
est marquée RGF93 dans les données rendues à sommieR.

**Redite par l'agent :** « citer une source publiée pour chaque nom d'hôte du caster Centipède que Surculus reconnaît, pour décider si une position est marquée RGF93 » (redite de l'`acquereur`, recopiée par l'orchestrateur : son harnais ne lui permet d'écrire que dans le référentiel).

## Le constat

- `core/src/main/kotlin/io/github/pobsteta/surculus/core/session/BesoinEtFix.kt` l. 100-107 (094) :
  `CASTERS_CENTIPEDE = {"caster.centipede.fr", "crtk.net"}`, reconnus par le nom seul, jamais par une adresse IP
  (décision de Pascal, 2026-11-06).
- `crtk.net` : nommé par la documentation Centipède (référentiel, l. 23, source C-CASTER).
- `caster.centipede.fr` : **aucune source** ; c'est le réglage par défaut de `ConfigRtk`, hérité de Groma.
- Règle (spec § 3) : RTK flottant ou fixe avec un flux Centipède vu → `RGF93` ; caster non Centipède → `null`.

## Ce qui est demandé

1. Pour `crtk.net` et `caster.centipede.fr` : chercher des sources publiées (documentation Centipède, dépôts du
   projet Centipède, annonces, forum officiel, autres guides de référence), en comparer au moins deux, au CQQCOQP.
2. Dire pour chaque nom : source(s) exacte(s) (URL, page, date de consultation), ce qu'elle dit (nom d'hôte, port,
   points de montage), et s'il s'agit du même service (alias) ou d'un autre.
3. Signaler tout autre nom d'hôte officiel de Centipède trouvé (sans l'ajouter au code).
4. Inscrire le résultat dans `docs/referentiels/referentiel-centipede.md` (tableau des sources et une rubrique
   « Hôtes du caster »), sans rien inventer : un nom sans source est écrit « sans source trouvée ».

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : recherche et comparaison de sources | **appuyer** |
| A-t-il les moyens ? | oui : WebSearch, WebFetch, le référentiel existant | |

**Temps de la main :** appuyer.

## Ma réponse d'abord

_Rempli par Pascal seul._

## L'Homme

- **Agent :** `acquereur` (le seul qui écrit dans `docs/referentiels/`).
- **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** toucher au code (`CASTERS_CENTIPEDE`, `ConfigRtk`) ; décider de retirer ou
  d'ajouter un nom. Si `caster.centipede.fr` reste sans source, l'orchestrateur pose la question à Pascal.

## Harnais

- **Peut :** écrire dans `docs/referentiels/referentiel-centipede.md`.
- **Ne doit pas :** écrire ailleurs ; toucher à `main` ; citer une source sans l'avoir lue.

## Valeur provisoire (chiffre, réglage)

- Aucune.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** 094 (#159, aa02f2c).
- **CI rouge attendue :** aucune.

## Tests

- Chaque nom d'hôte de `CASTERS_CENTIPEDE` figure au référentiel avec sa source, ou « sans source trouvée ».
- Au moins deux sources comparées par nom quand elles existent.
- CI verte (aucun code touché).

## Terrain

- Aucun écran.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Qui tranche :** Pascal (garder, retirer ou ajouter un nom ; fusion).

## Comment saura-t-on que ça marche ?

Un successeur lit, pour chaque nom qui marque une position RGF93, d'où vient ce nom.
