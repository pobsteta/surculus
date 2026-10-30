# Mission 072 — L'icône du micro, comme sur Marculus

> Fiche écrite par l'orchestrateur le 2026-10-29 (cycle 13), sur demande de Pascal à l'essai de la
> release v0.7.80 : « Il faut remplacer cette phrase par l'icone du micro comme sur marculus. »
> Depuis `main` (afcdbd1). Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Remplacer le texte « Tenir pour parler » du bouton micro hors
poche par l'icône du micro de Marculus, pour que le forestier reconnaisse le bouton d'un coup d'œil.

**Redite par l'agent :** (à remplir par l'agent avant d'agir)

## Ce qui est à faire

1. Dans `BoutonMicro` (`app/.../controle/EcranControle.kt`), afficher l'icône au lieu du texte.
   Marculus utilise `Icons.Filled.Mic` (et `Icons.Filled.MicOff` quand le micro n'est pas prêt) :
   `~/dev/marculus/app/src/main/kotlin/io/github/pobsteta/marculus/ui/feuille/DicteeVocale.kt` l. 364,
   à lire pour la taille et la couleur. Reprendre la même icône, de taille lisible avec des gants.
2. Le texte reste en **description d'accessibilité** (« Tenir pour parler », « J'écoute » pendant
   l'écoute) : lecteur d'écran et tests le trouvent toujours.
3. Garder **un seul composable** sous le doigt (commentaire en place : en changer pendant l'appui
   enverrait un Cancel) ; seules l'icône et les couleurs changent selon l'état.
4. Si `material-icons-extended` n'est pas déjà une dépendance, ne pas l'ajouter pour une icône :
   reprendre le tracé de `Mic` en vecteur dans les ressources (licence Apache 2.0, la citer).

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | |
| A-t-il les moyens ? | oui | |

**Temps de la main :** **appuyer**.

## L'Homme

- **Agent :** `dev`.
- **Ce qui ne lui est pas demandé :** le plantage de la dictée en release (mission 071) ; déplacer le
  bouton (écart noté : il faut faire défiler pour le voir, question à Pascal plus tard) ; le mode poche.

## Harnais

- **Peut :** modifier `EcranControle.kt`, les ressources et les tests de l'écran ; pousser sur
  `mission/072-icone-micro` ; ouvrir la PR.
- **Ne doit pas :** modifier `fixtures/`, `docs/`, `pilotage/` ; toucher à `main` ; ajouter une
  dépendance lourde ; trancher une question de sens.

## Tests

- `.claude/test-command` au code 0 ; un test vérifie que le bouton porte la description « Tenir pour
  parler » et plus de texte visible (vu rouge une fois).

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. Critère d'arrêt : 3 allers-retours.
- **Qui tranche :** Pascal. **Fusion : autorisation à demander à Pascal.** Capture au téléphone après fusion.

## Comment saura-t-on que ça marche ?

Sur la release installée, le bouton à côté de « Mode poche » montre le micro de Marculus, sans phrase.
