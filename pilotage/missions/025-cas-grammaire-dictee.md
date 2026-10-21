# Mission 025 — Cas d'acceptation de la dictée à grammaire fermée

> Fiche écrite par l'orchestrateur le 2026-10-20, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Décidée par Pascal le 2026-10-20 :
> le `testeur` écrit les cas de la 013 avant que le `dev` ne code.

## Mission

**En une phrase (quoi, et pourquoi) :** Écrire, à partir de la spec (§ 4.3, grammaire fermée) et de l'ADR 0004, sans lire de code, les cas d'acceptation sourcés de l'analyse des phrases dictées (chaque phrase de la grammaire et la commande attendue, nombres de « hauteur » aux bornes, phrases hors grammaire refusées, chaque mot présent dans le vocabulaire de `vosk-model-small-fr-0.22`, modèle à mauvaise empreinte SHA-256 refusé), pour que la mission 013 ait ses tests avant son code.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : format des cas des missions 019 et 020, `fixtures/README.md` ; spec de dictée de Marculus en lecture seule | |
| A-t-il les moyens ? | oui : le modèle se télécharge, son vocabulaire s'en extrait (`graph/words.txt`) | |

**Temps de la main :** **appuyer** : l'agent rend la liste des cas avant de les écrire.

## L'Homme

- **Agent :** `testeur` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** le moteur Vosk, l'interface `MoteurDictee` ; un banc d'essai audio ; aucun code de production.

## Harnais

- **Peut :** créer `fixtures/dictee/` (cas au format JSON, identifiants `DI-NN`, chacun avec sa `source`), compléter `fixtures/README.md`, écrire des tests Kotlin désactivés (`@Disabled("en attente de la mission 013")`) si l'interface attendue se décrit sans lire de code.
- **Ne doit pas :** lire `core/src/main/` ni `app/src/` ; versionner le modèle ; inventer un mot de grammaire ou une commande absente de la spec (il la remonte, avec sa proposition, par exemple la borne haute de « hauteur ») ; toucher à `main`.
- **Sources autorisées :** spec § 4.3 et § 6 ; ADR 0004 ; `docs/referentiels/etats-plants.yaml` (causes) ; Marculus `VoiceGrammar.kt` et sa spec de dictée en lecture seule.

## Tests

- Chaque cas cite sa source. Un mot de la grammaire absent du vocabulaire du modèle est signalé à l'orchestrateur, pas contourné. `.claude/test-command` passe.

## Terrain

- Pascal lit la liste des phrases et dit s'il les prononcerait ainsi en forêt.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Le `dev` de la 013 branche ces cas sans les modifier, et ils passent au vert.

## Reprise après la remise de la PR #41 (2026-10-21)

Le testeur a montré que trois mots de la grammaire sont absents du vocabulaire du modèle,
et que la décision 4 transmise par l'orchestrateur contredisait la spec § 6. **Erreur de
l'orchestrateur :** l'option qu'il recommandait (« abrouti refusé sur un mort ») citait à
tort la spec § 4.3.

**Décisions de Pascal du 2026-10-21, numérotées comme au journal** (source :
« pilotage/journal.md, 2026-10-21 ») :
1. « hauteur » de 1 à 400 cm.
2. Une phrase, une commande.
3. Frottis, hylobe, gel, sécheresse et concurrence restent hors grammaire.
4. **Corrigée :** le mot du détail « abroutissement » est accepté sur un plant **sain,
   atteint ou mort**, et refusé sur un absent, un non observé ou sans état (spec § 6 et
   arbitrage de la 002).
5. **Mots remplacés :** « abiotique » devient « climat », « abrouti » devient « mangé »,
   « regarni » devient « remplace ». Les trois sont dans le vocabulaire du modèle. La
   spec § 4.3 est amendée sur cette branche par l'orchestrateur.

**Choix techniques de l'orchestrateur :**
- Correspondance des mots vers `cause_groupe` : insectes → entomologique, champignon →
  pathologique, animaux → animaux, climat et indéterminé →
  abiotique_complexe_indetermine (référentiel `etats-plants.yaml`).
- Nombres : la forme à traits d'union (« quarante-cinq ») et la forme à espaces
  (« quarante cinq ») sont acceptées toutes les deux, puisque Vosk peut rendre l'une ou
  l'autre. Les espaces en tête, en queue ou doublés sont normalisés avant l'analyse.
- Le SHA-256 de l'archive n'est pas inscrit dans `docs/dependances.md` par cette mission :
  la 013 le recoupera sur un second téléchargement.

**À faire :** aligner tous les cas sur ces décisions (les trois mots, l'abroutissement sur
un mort, les nombres à espaces, la normalisation des espaces). Le test actif ne doit plus
consigner d'absence au vocabulaire : chaque mot de la grammaire y est. Écrire la redite de
la mission dans cette fiche.
