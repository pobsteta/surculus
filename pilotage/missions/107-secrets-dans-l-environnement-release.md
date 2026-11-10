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
