# Mission 089 — Ouvrir sans position depuis le guidage

> Fiche écrite par l'orchestrateur le 2026-11-07, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> après les décisions de Pascal du 2026-11-03 (« Mission à part », après la 088, avant l'essai en forêt ; « 090
> d'abord ») et du 2026-11-07 (« Écrire la fiche 089 »). Depuis `main` (39df25e). Une mission = une phrase = une
> branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Donner au guidage le champ du code et la voie « Ouvrir sans position » quand
aucun fix n'arrive, comme le bouton « Contrôler » de la carte, pour que l'agent sous couvert ne reste pas bloqué
sur un écran qui attend une distance qui ne viendra pas.

**Redite par l'agent (testeur) :** écrire les tests qui diront si, au guidage sans fix, l'agent peut saisir le code et
ouvrir sans position après 30 s selon la règle de la 087 (code requis, placette confirmée, localisation exigée, retour
aux 15 m dès qu'un fix arrive), sans rien changer avec un fix.

## Le constat

- `app/src/main/kotlin/io/github/pobsteta/surculus/carte/EcranGuidage.kt` (39df25e) : le champ « Code lu sur le piquet »
  et le bouton « Contrôler » ne s'affichent que si `cap?.arrive == true` (l. 207-222), c'est-à-dire avec un fix à
  moins de 30 m. Sans fix, l'écran montre l'attente de la position et rien d'autre.
- La carte de la placette, elle, garde « Contrôler » sans position : le 2026-11-03, Pascal a ouvert P35-03 sans
  position par ce bouton (« Ouvrir sans position », puis la confirmation ; journal). Le 2026-11-07 (v0.7.111), la carte
  de P35-01 montrait « Contrôler » avec le badge « Sans position ».
- La règle d'ouverture sans position existe déjà (087, spec § 4.3 points 1 à 10, § 6 l. 649) : 30 s sans aucun fix
  (**provisoire**), récepteur actif, localisation et permission accordées ; code lu connu de la tournée exigé
  (`code_requis`) ; placette affichée à confirmer ; position du contrôle = piquet. Fiche 087, l. 100-101 : ce trou du
  guidage est laissé à la 089.

## Ce qui est attendu (à préciser par le plan du `dev`)

- Au guidage, sans fix : après le même délai et aux mêmes conditions que la carte, l'agent peut saisir le code et
  ouvrir sans position, avec la même confirmation de la placette.
- Avec un fix : rien ne change (distance, bip à 30 m, rattachement à 15 m ou par le code).
- Localisation coupée ou permission refusée : la même demande que sur la carte, pas d'ouverture.
- Réutiliser la règle de la 087, ne pas la recopier : une seule source pour le délai, `code_requis` et la confirmation.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | en partie : la règle existe (087), sa place dans le guidage est à concevoir | **tenir** : plan d'abord |
| A-t-il les moyens ? | oui : tests Robolectric de l'écran, `PositionSimulee` ; l'essai réel sans fix est à Pascal | le `testeur` écrit d'abord les tests |

**Temps de la main :** tenir (écran de terrain, parcours de l'agent sous couvert) ; le `dev` rend un plan d'abord.

## Ma réponse d'abord

_Rempli par Pascal seul._

## L'Homme

- **Agents :** `testeur` d'abord (tests depuis la spec § 4.3, § 6 et cette fiche, **sans lire** `EcranGuidage.kt` ni
  le code de l'arrivée), puis `dev` (plan d'abord, puis code).
- **Modèle :** sonnet pour le `testeur` ; opus pour le `dev` (parcours de terrain, justesse du rattachement).
- **Ce qui ne lui est pas demandé :** changer le délai de 30 s ; changer la règle des 15 m ; changer l'écran de la
  carte ; le format du journal ou de l'export.

## Questions au plan du `dev` (l'orchestrateur les posera à Pascal)

1. Où vont le champ du code et « Ouvrir sans position » dans le guidage (sous l'attente de la position, ou à la place) ?
2. Le champ du code est-il visible dès le guidage sans fix, ou seulement après les 30 s ?
3. Le compte des 30 s : depuis l'ouverture du guidage, ou partagé avec la carte ?

## Harnais

- **Testeur peut :** tests dans `app/src/test/` et `core/src/test/` ; vus rouges.
- **Dev peut :** `EcranGuidage.kt`, ce qu'il faut de l'arrivée sans position (087) pour la partager, ressources ; ses
  tests.
- **Ne doivent pas :** toucher à `docs/` (l'orchestrateur amende la spec) ; toucher à `main` ; réécrire les tests de
  l'autre.
- **Sources autorisées :** spec § 4.3, § 6 ; ADR 0003 (amendement du 2026-11-02) ; fiches 087 et 089.

Si un test du `testeur` contredit la spec ou une décision, **le `dev` s'arrête et le dit tout de suite.**

## Valeur provisoire (chiffre, réglage)

- **30 s** sans aucun fix avant « Ouvrir sans position » : reprise de la 087, toujours provisoire ; mesurée par Pascal
  à l'essai en forêt sous couvert (premier fix à froid) ; gel avant la publication de la tranche 1. La 089 ne la change
  pas et la lit à la même source.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** 087 (#152, 7c8f969).
- **PR en brouillon jusqu'à :** la fin du travail du `dev`.
- **CI rouge attendue :** les tests du `testeur`, entre son push et celui du `dev`.

## Tests

- Guidage sans fix, avant 30 s : pas d'ouverture sans position.
- Guidage sans fix, après 30 s, récepteur actif, localisation accordée : « Ouvrir sans position » proposé ; sans code,
  refus `code_requis` (bouton grisé) ; avec un code de la tournée : la placette est affichée à confirmer, puis le
  contrôle s'ouvre sans position mesurée.
- Code inconnu de la tournée : refusé, rien n'est écrit.
- Localisation coupée ou permission refusée : demande de l'activer, pas d'ouverture.
- Un fix arrive pendant l'attente : retour à la règle des 15 m.
- Avec un fix : comportement d'aujourd'hui inchangé (arrivée à 30 m, rattachement).
- `.claude/test-command` au code 0 ; CI verte.

## Terrain

- **Échantillon que Pascal vérifiera :** sur la release, téléphone sans fix (micro-ondes éteint, comme le
  2026-11-03), « Naviguer vers la placette » sur une placette de la Colombière : après 30 s, code saisi, placette
  confirmée, contrôle ouvert sans position. Puis en forêt sous couvert.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Qui tranche :** Pascal (plan, textes de l'écran, fusion).

## Comment saura-t-on que ça marche ?

Sous couvert, sans fix, l'agent qui suit le guidage ouvre sa placette au code lu sans revenir à la carte.
