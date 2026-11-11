# Mission 107 — les secrets de signature dans l'environnement « release »

> Fiche écrite par l'orchestrateur le 2026-11-10, à partir du gabarit `fiche-mission.md` du skill
> `imperatrice:mission`. Une mission = une phrase = une branche
> (`mission/107-secrets-dans-l-environnement-release`) = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** le job de `release.yml` qui signe l'APK lit les quatre
secrets de signature dans l'environnement GitHub « release », limité à `main` et approuvé par
Pascal. Aujourd'hui, ce sont des secrets du dépôt, que tout workflow lancé depuis une branche
peut lire. Avec l'environnement, la clé et ses mots de passe ne sortent plus que pour une
release voulue par Pascal (ADR 0010, décision 1 de Pascal du 2026-11-10).

**Redite par l'agent :** je fais lire les quatre secrets `RELEASE_*` au seul job de `release.yml` qui signe l'APK, en lui faisant déclarer l'environnement `release` (limité à `main`, approuvé par Pascal), pour que la clé ne sorte que pour une release voulue ; un test garde cette règle, et je ne crée ni l'environnement ni les secrets.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | redite exigée avant d'agir |
| Sait-il comment faire ? | oui | documentation GitHub des environnements, citée ci-dessous |
| A-t-il les moyens ? | en partie | il ne peut ni créer l'environnement ni déplacer les secrets : c'est Pascal (rubrique « Dépend de ») |

**Temps de la main :** appuyer. La mission est courte, mais elle touche la chaîne de publication.
Une erreur de sa part peut casser une release, ou laisser la clé exposée sans que personne le voie.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent._

## L'Homme

- **Agent :** `dev`.
- **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :**
  - créer l'environnement, déplacer ou lire les secrets (Pascal) ;
  - modifier la signature dans `app/build.gradle.kts` ;
  - toucher à l'ADR 0010 (PR #180).

## Harnais

- **Peut :**
  - modifier `.github/workflows/release.yml` ;
  - modifier la rubrique « Ce que Pascal doit faire pour la clé » de `docs/publier.md`, l. 60-80, en laissant intacte la phrase qui renvoie à l'ADR 0010 (l. 74-76, que la #180 modifie aussi) ;
  - ajouter un test qui lit les workflows.
- **Ne doit pas :**
  - mettre une valeur de secret, même factice, dans le dépôt ;
  - modifier `ci.yml`, `import-sommier.yml` ou `badges.yml`, sauf pour en retirer une lecture des secrets `RELEASE_*` s'il en trouve une, en le signalant ;
  - toucher à `main`.
- **Sources autorisées :**
  - `.github/workflows/*.yml` ;
  - `docs/publier.md` ;
  - l'ADR 0010 sur la branche `pilotage/adr-0010-cle-de-signature` (`git show origin/pilotage/adr-0010-cle-de-signature:docs/adr/0010-cle-de-signature-et-distribution.md`) ;
  - documentation GitHub : « Managing environments for deployment » (protection rules, deployment branches, environment secrets) et la syntaxe `jobs.<job_id>.environment`. Ouvrir chaque page avant de la citer.

## Valeur provisoire (chiffre, réglage)

Aucune. Le nom de l'environnement, `release`, est décidé par Pascal.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** aucune mission. Il dépend en revanche d'**actions de Pascal**, à faire avant la fusion :
  - créer l'environnement `release` ;
  - le limiter à la branche `main` ;
  - se mettre en approbateur obligatoire ;
  - y créer les quatre secrets : `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`.
- **Ordre de fusion :** les actions de Pascal → la 107 → la première release signée, approuvée par Pascal → Pascal supprime les quatre secrets du dépôt. Sans cette suppression, la protection ne sert à rien. La #180 peut fusionner avant ou après la 107 ; celle qui passe en second se met à jour sur `main`.
- **PR en brouillon jusqu'à :** Pascal dit au journal que l'environnement et ses secrets sont en place.
- **CI rouge attendue :** aucune. La CI des PR ne lit pas les secrets de signature.

## Tests

- **Comment saura-t-on que c'est juste :**
  - Un test lit les workflows et échoue si :
    - le job de `release.yml` qui reçoit un secret `RELEASE_*` ne déclare pas `environment: release` ;
    - un autre job ou un autre workflow lit un secret `RELEASE_*`.
  - Ce test est vu rouge sur `main` avant la modification.
  - Le comportement « pas d'APK sans clé » (`docs/publier.md` l. 27) est conservé : un environnement sans secrets donne une release sans APK, pas un échec silencieux. L'agent le montre par la lecture du workflow et le dit.
  - `.claude/test-command` sort au code 0. Si `actionlint` est disponible, il passe aussi.

## Terrain

- **Échantillon que Pascal vérifiera :**
  - la première release après la fusion attend son approbation dans l'onglet Actions ;
  - une fois approuvée, l'APK est signé de la même clé : il s'installe par-dessus la version du téléphone, sans désinstaller ;
  - l'empreinte de `apksigner verify --print-certs` est égale à celle de l'ADR 0010.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal (porte n° 3).
- **Critère d'arrêt de la relecture :** 3 allers-retours ; au-delà, la décision remonte à Pascal.
- **Point de suivi :** à la PR ouverte, puis à la première release qui suit la fusion.
- **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

La release qui suit la fusion attend l'approbation de Pascal. Une fois approuvée, elle publie un
APK signé qui s'installe par-dessus la version du téléphone. Une fois les secrets du dépôt
supprimés, `gh secret list` (au niveau du dépôt) n'affiche plus aucun `RELEASE_*`.

## Reprise 1/3 (2026-11-10) : scinder le job

Décision de Pascal, le 2026-11-10 : « Scinder dans la 107 ». L'approbation ne doit pas retarder les tests.

- **Redite :** `release.yml` passe à trois jobs : `tester` (version, tests, contrôle 16 Ko debug, sans environnement ni secret),
  `signer` (`environment: release`, tout ce qui lit `RELEASE_*`, APK et notes) et `publier` (dépend des deux, aucun secret).
- **Choix :** `signer` fait son propre checkout (`persist-credentials: false`) et recompile l'APK release ; aucun APK ne
  passe d'un job à l'autre, donc l'APK debug non signé ne peut pas passer pour l'APK signé. Le coût est un second réglage
  Gradle/SDK dans `signer`, sans rejouer les tests. La version et le journal des changements passent par les sorties de `tester`.
- **Si Pascal n'approuve pas ou rejette :** `signer` ne part pas, `publier` non plus : ni tag ni release (écrit dans
  `release.yml` et `docs/publier.md`).
- **Test :** seul `signer` lit `RELEASE_*` et déclare `environment: release` ; `tester` existe et n'en déclare aucun.
  Vu rouge sur 1ffddbd (2 échecs sur 7), puis vert.

## Reprise 2/3 (2026-11-10)

Le relecteur juge `release.yml` juste (`actionlint` 1.7.7, code 0, sans `shellcheck`) : il n'a pas bougé. Pascal a tranché :
la fumée reste dans `signer`, après l'approbation, sur le type de build `fumee` calqué sur la release (ADR 0011).

- **Redite :** la reprise ne touche que `docs/publier.md` et le test.
- **Doc (B1-B3) :** l'ordre en toutes lettres (environnement et secrets, puis fusion, puis approbation de la première release,
  puis suppression des secrets du dépôt et release suivante vérifiée), sa raison (un environnement référencé mais absent
  est créé sans règle de protection ni secret), les deux vérifications (`gh secret list --env release` avant la fusion ;
  release suivante encore signée après la suppression), les libellés de GitHub relus sur les pages « Managing environments
  for deployment » et « Reviewing deployments » le 2026-11-10 (sans « Prevent self-review »).
- **Doc (M1-M3) :** la fumée est jouée toujours (repli debug sans clé) ; renvoi par le nom des étapes ; concurrence : la
  release suivante attend tout entière, Pascal rejette ce qu'il ne veut pas ; aucun délai d'attente écrit.
- **Test (M4) :** lit aussi `secrets['RELEASE_…']`, `secrets[…]` dynamique, `toJSON(secrets)`, `secrets: inherit`, et garde
  `publier: needs: [tester, signer]`. Chaque garde vue rouge sur un cas fautif.
- **Test (M5) :** rejoué sur le `release.yml` de `origin/main` : rouge (3 échecs sur 10).
- **Non vérifié :** la page « Using secrets in GitHub Actions » ouverte ne dit pas que le secret d'environnement prime sur le secret
  du dépôt du même nom ; la doc écrite n'affirme que ce qui est sûr (le job voit aussi les secrets du dépôt).

## Reprise 3/3 (2026-11-10)

Pascal a changé de cap après la remarque sur le type de build `fumee` : la fumée passe dans `tester`, avant l'approbation,
sans aucun secret (signée en debug sans `RELEASE_KEYSTORE`, `app/build.gradle.kts`).

- **Redite :** « Activer KVM » et « Essai de fumée » quittent `signer` pour `tester`. `signer` ne garde que la clé, l'APK
  release, son contrôle 16 Ko, l'APK, les notes et l'artefact.
- **Vérifié :** la fumée ne lit aucune sortie de `signer` (elle prend `steps.version.outputs.new_version`, de `tester`, et
  `github.run_number`) ; son échec fait échouer `tester`, donc `signer` (`needs: tester`) ne part pas : ni approbation demandée,
  ni tag, ni release. Commentaires et `docs/publier.md` : la fumée éprouve le type `fumee`, pas l'APK publié.
- **Test :** la fumée est dans `tester` et absente de `signer` ; aucune ligne de `tester` ne mentionne `RELEASE_`. Vu rouge sur
  179fb57 (1 échec sur 12), puis vert.
- **`actionlint` :** non passé dans ce worktree : l'exécution du binaire téléchargé (1.7.7, somme SHA-256 vérifiée contre le
  fichier officiel des sommes) a été refusée par le système. Le relecteur l'avait passé sur la version de la reprise 2/3.
