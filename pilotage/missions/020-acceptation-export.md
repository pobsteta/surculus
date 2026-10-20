# Mission 020 — Cas d'acceptation de l'export GeoPackage

> Fiche écrite par l'orchestrateur le 2026-10-17, à partir du gabarit
> `imperatrice/skills/mission/fiche-mission.md`. Accompagne la mission 006 (règle : le `testeur` écrit
> les tests depuis la spec, sans lire le code). Demandée par Pascal à la relecture de la
> PR #34 : la fiche 006 ne prévoyait aucun test indépendant, erreur de l'orchestrateur.

## Mission

**En une phrase (quoi, et pourquoi) :** Écrire, à partir de l'ADR 0007, de l'ADR 0011, de la spec (§ 5 et § 6) et des arbitrages du 2026-10-17, et sans lire le code de la 006, les cas d'acceptation sourcés de l'export `travaux.gpkg` (couches de sommieR intactes, six couches ajoutées présentes et typées, version unique, repère lu dans la tournée, export déterministe, mélange 2154/9794 refusé, placette française en France, un contrôle par placette et par jour avec la double règle jour civil de Paris et jour UTC, jour civil repris du journal), pour que le module `:gpkg` ne s'auto-évalue pas.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : format des fixtures de la 002 et de la 019 (`fixtures/README.md`), règle des interfaces déclarées dans le code de test (journal, 2026-10-16) | |
| A-t-il les moyens ? | oui : ADR 0007 et 0011, spec, brief lot 2, modèle `inst/qgis/travaux.gpkg` et `R/travaux_qfield.R` de sommieR 0.31.1 en lecture seule | |

**Temps de la main :** **appuyer** : l'agent rend la liste des cas avant d'écrire.

## L'Homme

- **Agent :** `testeur` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** aucun code de production ; ne lit ni la branche `mission/006-module-gpkg-lecture-et-export` ni la PR #34.

## Harnais

- **Peut :** écrire `fixtures/acceptation-export/*.json` (chaque cas : un `id`, une description, la source de la règle, l'entrée — tournée de départ et saisies —, le résultat attendu : export accepté avec contenu attendu, ou refusé avec motif) ; compléter `fixtures/README.md` ; écrire un test Kotlin dans `gpkg/src/test/` qui appelle une interface déclarée dans le code de test et reste `@Disabled` jusqu'à ce que la 006 le branche.
- **Ne doit pas :** lire le code de la 006 ; inventer une règle absente des ADR, de la spec ou des arbitrages (il la remonte) ; inventer un code (travaux, essence, état) ; toucher à `main`.
- **Sources autorisées :** ADR 0004, 0007, 0011 ; `docs/spec.md` ; `pilotage/journal.md` (inscriptions du 2026-10-17) ; `pilotage/briefs/vers-sommier/` ; sommieR 0.31.1 en lecture seule ; `fixtures/` de `main`.

## Tests

- Chaque exigence de l'ADR 0007 sur l'export et chaque règle de la spec qui touche l'export a au moins un cas ; chaque cas cite sa source.
- Décisions de Pascal du 2026-10-17 à couvrir :
  - un contrôle par placette et par jour civil Europe/Paris, **et** par jour UTC tant que l'import de sommieR compte en UTC (exemple : 23 h 30 le 20 et 00 h 30 le 21, heure de Paris, refusés) ;
  - le jour civil d'un contrôle est celui du journal : heure GNSS si le fix a au plus 5 s, sinon heure du téléphone ; `visite_le` porte cet instant.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.
- **Point de suivi :** à la remise de la liste des cas, puis quand le `dev` de la 006 les a branchés.
- **Plafond humain :** la PR de la 020 s'ouvre quand une place se libère (trois PR déjà en revue) ; d'ici là, la branche est poussée sans PR.

## Comment saura-t-on que ça marche ?

Les cas de la 020 sont branchés par la 006, actifs et verts dans `:gpkg:test`, sans qu'aucun ait été modifié par le `dev`.
