# Mission 108 — la version calendaire AAAA.M.N, comme nemeton

> Fiche écrite par l'orchestrateur le 2026-11-11, à partir du gabarit `fiche-mission.md` du skill
> `imperatrice:mission`. Une mission = une phrase = une branche
> (`mission/108-version-calendaire`) = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** `release.yml` numérote chaque release `vAAAA.M.N`, où AAAA.M sont l'année
et le mois de la release (mois sans zéro devant) et N le rang de la release dans ce mois, en repartant de 1 chaque
mois. Il le fait à la place du SemVer tiré des commits, pour que le numéro dise la date de l'APK, comme dans nemeton
(demande de Pascal du 2026-11-11, « Comme nemeton »).

**Redite par l'agent :** faire numéroter chaque release `vAAAA.M.N` par `release.yml` (année et mois de Paris, rang N dans le mois, repartant
de 1 chaque mois) à la place du SemVer tiré des commits, pour que le numéro de l'APK dise sa date, comme nemeton.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | redite exigée avant d'agir |
| Sait-il comment faire ? | oui | la règle de nemeton est citée ci-dessous ; le calcul tient en quelques lignes de shell |
| A-t-il les moyens ? | oui | le workflow ne s'exécute que sur `main` : la preuve passe par un test du calcul, pas par une release |

**Temps de la main :** appuyer. La mission touche la chaîne de publication, comme la 107.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## L'Homme

- **Agent :** `dev`.
- **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :**
  - changer quoi que ce soit au découpage `tester` / `signer` / `publier`, ni à l'environnement `release`, ni aux secrets (mission 107, PR #182) ;
  - changer le `versionCode` Android : il reste `github.run_number` ;
  - toucher aux tags existants (`v0.1.0` à `v0.7.x`) : on ne les renomme ni ne les supprime.

## Harnais

- **Branche de départ :** `mission/108-version-calendaire`, partie de la branche de la 107
  (`mission/107-secrets-dans-l-environnement-release`, tête 6bc02f0). Décision de Pascal du 2026-11-11, « Partir de
  la #182 » : la PR 108 vise `main` et se fusionne juste après la #182. Si la 107 bouge, on rebase la 108 dessus.
- **Peut :**
  - modifier le job `tester` de `.github/workflows/release.yml` (le calcul de version et ses sorties `new_tag`, `new_version`, `changelog`) et le commentaire d'en-tête ;
  - ajouter un script de calcul (par exemple `scripts/prochaine-version.sh`) et son test ;
  - modifier `docs/publier.md`, rubriques sur le calcul de version, la table des commits et la première version ;
  - corriger les commentaires qui citent l'ancienne forme (`app/src/main/kotlin/io/github/pobsteta/surculus/tournee/ReglagesAffichage.kt` l. 20-21, `0.7.41`).
- **Ne doit pas :**
  - mettre une valeur de secret, même factice, dans le dépôt ;
  - faire lire un secret `RELEASE_*` au job `tester` ou au script (la garde `SecretsDeSignatureDansEnvironnementTest` doit rester verte) ;
  - modifier `ci.yml`, `import-sommier.yml` ou `badges.yml` ;
  - toucher à `main`.
- **Sources autorisées :**
  - `.github/workflows/*.yml`, `docs/publier.md`, `app/build.gradle.kts` (lecture) ;
  - la règle de nemeton, à lire dans `~/dev/nemeton/.github/workflows/r.yml` l. 42-57 (format `^[0-9]{4}\.([1-9]|1[0-2])\.[1-9][0-9]*$`) et ses tags (`git -C ~/dev/nemeton tag`) ;
  - la documentation de GitHub Actions pour ce qu'il remplace dans `mathieudutour/github-tag-action` (ouvrir la page avant de la citer).

## La règle

- Tag `v` + `AAAA.M.N`. Exemples : `v2026.11.1`, `v2026.11.2`, puis `v2026.12.1`, puis `v2027.1.1`.
- AAAA.M : année et mois de la date du lancement du workflow, **à l'heure de Paris** (valeur provisoire, ci-dessous).
- N : 1 + le plus grand N des tags `vAAAA.M.*` du même mois ; 1 s'il n'y en a aucun. Les tags `v0.x.y` sont ignorés
  pour le rang. Un trou dans la suite (tag supprimé) n'est pas comblé : on part du plus grand.
- `new_version` vaut `AAAA.M.N` (sans `v`), `new_tag` vaut `vAAAA.M.N`. Ils restent les seules sources de la version
  pour `signer` (`-PappVersionName`) et `publier` (tag, titre, nom de l'APK).
- Le calcul se fait dans `tester`, sans poser le tag, comme aujourd'hui.
- Les notes de version (`changelog`) gardent la liste des commits depuis le tag précédent, quel qu'il soit (la
  première release calendaire part du dernier `v0.7.x`). La forme exacte est laissée au dev, qui la montre dans la PR.
- La « première version v0.1.0 automatique » (décision b du 2026-10-16) devient « première version du mois » : sans
  tag du mois, N = 1. Le dev dit dans la PR ce que devient la décision b dans `docs/publier.md`.

## Valeur provisoire (chiffre, réglage)

- **Fuseau du mois :** Europe/Paris. Raison : une fusion le 30 novembre à 23 h 30 à Paris est une release de novembre
  pour Pascal. À confirmer par Pascal à la relecture.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** la mission 107 (PR #182), dont elle part. Ordre de fusion : #182, puis 108.
- **PR en brouillon jusqu'à :** la #182 fusionnée et la 108 rebasée sur `main`.
- **CI rouge attendue :** aucune.

## Tests

- **Comment saura-t-on que c'est juste :** un test exécute le calcul sur des listes de tags et une date données, sans
  réseau ni dépôt GitHub, et vérifie au moins :
  - aucun tag du mois, seulement des `v0.7.x` → `2026.11.1` ;
  - `v2026.11.1` et `v2026.11.2` présents → `2026.11.3` ;
  - `v2026.11.22` et `v2026.10.19` présents, date de décembre → `2026.12.1` ;
  - janvier → `2027.1.1` (pas de zéro devant le mois) ;
  - trou `v2026.11.1`, `v2026.11.3` → `2026.11.4` ;
  - un tag `v2026.11.10` après `v2026.11.9` → `2026.11.11` (tri numérique, pas alphabétique) ;
  - 30 novembre, 23 h 30 à Paris (22 h 30 UTC) → novembre ; 1er décembre, 0 h 30 à Paris (30 novembre, 23 h 30 UTC) → décembre ;
  - chaque résultat vérifie l'expression régulière de nemeton.
- Ce test est vu rouge avant le script (ou sur un calcul fautif volontaire), puis vert.
- `SecretsDeSignatureDansEnvironnementTest` reste vert.
- `.claude/test-command` sort au code 0 ; `actionlint` passe s'il est disponible.

## Terrain

- **Échantillon que Pascal vérifiera :**
  - la release qui suit la fusion s'appelle `v2026.11.1` (ou le premier rang libre du mois) ;
  - l'APK s'installe par-dessus la `0.7.132` du téléphone, sans désinstaller (le `versionCode` monte toujours) ;
  - la boîte « À propos » affiche « Version 2026.11.1 ».

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal (porte n° 3).
- **Critère d'arrêt de la relecture :** 3 allers-retours ; au-delà, la décision remonte à Pascal.
- **Point de suivi :** à la PR ouverte, puis à la première release qui suit la fusion.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

La release qui suit la fusion porte le tag `v2026.11.N`, l'APK s'installe par-dessus l'ancien, et « À propos » affiche
la même version.
