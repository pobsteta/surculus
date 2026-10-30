# Mission 083 — Le bandeau d'écoute ne cache plus « Plant précédent »

> Fiche écrite par l'orchestrateur le 2026-10-30, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> après la décision de Pascal du 2026-10-30 (relecture de la 080 : « Corriger l'écran, test strict ») et son accord
> du même jour pour écrire la fiche. Depuis `main`. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Au contrôle, téléphone debout, placer le bandeau d'écoute « J'ÉCOUTE » pour
qu'il ne couvre plus la flèche « Plant précédent » ni le numéro du plant, pendant l'écoute et les 6 s qui suivent, pour
que le forestier puisse toujours revenir au plant d'avant.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? (sources, conventions nommées ci-dessous) | oui (spec § « Bandeau d'écoute », missions 075 et 076) | |
| A-t-il les moyens ? (outils, données d'essai, tests) | oui | le test de rendu de la 080 voit déjà le défaut |

**Temps de la main :** appuyer.

## Ma réponse d'abord

_Rempli par Pascal seul._

## L'Homme

- **Agent :** `afficheur`.
- **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** changer la durée du bandeau (6 s), son texte ou ce qu'il dit ; toucher au mode
  poche, au bouton micro (112 dp), à la carte ; écrire d'autres tests que la levée de la marque.

## Ce qui est constaté

- Test de rendu de la 080 (`app/src/test/.../rendu/RenduControleTest.kt`, l. 132 à 160) : debout, le bloc d'écoute,
  posé par-dessus le haut de la zone qui défile, recouvre « Plant précédent » pendant l'écoute et dans le délai de
  6 s. Marqué « DÉFAUT CONNU … mission 083 » (`CLIQUABLES_DU_DEFAUT_083`) ; le test casse s'il ne le voit plus.
- Seul « Mode poche » peut être couvert, pendant l'écoute et les 6 s (décision 2 de Pascal sur la 080).

## Harnais

- **Peut :** modifier la disposition de l'écran de contrôle (`EcranControle` et ses composants d'écoute) ; retirer la
  marque du défaut connu dans `RenduControleTest.kt` (`CLIQUABLES_DU_DEFAUT_083`, `enteteConnu`, le contrôle « le
  défaut n'est plus vu ») **et rien d'autre dans les tests de rendu** ; ajouter un test si une règle nouvelle le
  demande.
- **Ne doit pas :** élargir l'exception « Mode poche » ; relâcher une règle du banc (`BancRendu`) ; modifier
  `docs/`, `pilotage/` (sauf la redite), `fixtures/` ; toucher à `main`.
- **Sources autorisées :** `docs/spec.md` (§ dictée, bandeau d'écoute, boutons), fiches 075, 076 et 080,
  `RenduControleTest.kt`, `BancRendu.kt`.

## Valeur provisoire (chiffre, réglage)

Aucune nouvelle. Si la solution fixe une hauteur ou une position (en dp), la dire dans la PR, marquée provisoire jusqu'à
l'essai ganté de Pascal.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** 080 (fusionnée, #142, 76731ab).
- **Ordre de fusion :** une seule PR.
- **PR en brouillon jusqu'à :** la fin du travail de l'`afficheur`.
- **CI rouge attendue :** aucune. Le test casse si l'écran est corrigé sans lever la marque, et inversement : les deux
  vont dans le même commit.

## Tests

- `RenduControleTest` vert **sans** marque de défaut connu, sur les trois dispositions (debout plein écran, couché
  avec et sans plein écran) et les phases de l'écoute ; `.claude/test-command` au code 0.
- **Preuve du rouge** : sur une branche `preuve/083-…` (PR brouillon « ne pas fusionner »), le seul retour de l'ancienne
  position du bandeau rend le test rouge. Lien du run dans la PR.

## Terrain

- **Échantillon que Pascal vérifiera :** sur la release, debout, appui sur le micro : « Plant précédent » reste visible
  et cliquable pendant l'écoute et les 6 s ; capture par l'orchestrateur.

## Qui veille, qui tranche

- **Relecteur :** `relecteur` (vérifie que seule la marque a été retirée des tests, et le rouge de la preuve), puis
  Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Point de suivi :** à la PR.
- **Qui tranche :** Pascal (la disposition, la fusion).

## Comment saura-t-on que ça marche ?

Le rapport de rendu en artefact de la CI n'a plus de ligne « DÉFAUT CONNU » ; la capture debout pendant l'écoute
montre « Plant précédent » dégagé.
