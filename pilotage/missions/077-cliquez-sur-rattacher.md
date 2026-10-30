# Mission 077 — « Cliquez sur le bouton « Rattacher » »

> Fiche écrite par l'orchestrateur le 2026-10-30, à partir du gabarit
> `imperatrice/skills/mission/fiche-mission.md`, sur la décision de Pascal du 2026-10-30 après la capture
> de la v0.7.90 : « au lieu de "toucher" rattacher, mettre "cliquez sur le bouton "rattacher" ». Depuis
> `main` (d8263ec). Une mission = une phrase = une branche = une PR. Suite de la mission 068 (PR #126).

## Mission

**En une phrase (quoi, et pourquoi) :** Remplacer « puis touchez « Rattacher » » par « puis cliquez sur le
bouton « Rattacher » » dans le message d'arrivée à plus de 15 m, pour que le forestier sache sans hésiter
qu'il s'agit du bouton sous le champ.

**Redite par l'agent :** Remplacer « puis touchez « Rattacher » » par « puis cliquez sur le bouton « Rattacher » » dans le message hors 15 m (`RefusPlacette.HorsQuinzeMetres`), pour que le forestier identifie sans hésiter le bouton sous le champ.

## Ce qui est constaté

Capture de l'orchestrateur, v0.7.90, 2026-10-30, contrôle de P35-03 à 10,0 km : « À 10,0 km de la placette
la plus proche (plus de 15 m). Lisez le code sur le piquet, saisissez-le dans le champ « Code lu sur le
piquet », puis touchez « Rattacher ». » Le texte vit à un seul endroit : `RefusPlacette.HorsQuinzeMetres`
(`core/.../placette/Verdict.kt` l. 190) ; `ArriveeControleTest` l. 78 le vérifie en partie.

## Ce qui est à faire

1. Texte exact voulu : « … saisissez-le dans le champ « Code lu sur le piquet », puis cliquez sur le
   bouton « Rattacher ». » Rien d'autre ne change dans la phrase.
2. Test d'abord, vu rouge : le message exact (égalité, pas `contains`) pour une distance donnée.
3. Chercher toute autre copie de la phrase (tests, fixtures, docs) et le dire dans la PR ; ne rien changer
   hors du harnais, mais signaler.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | |
| A-t-il les moyens ? | oui | |

**Temps de la main :** **appuyer**.

## L'Homme

- **Agent :** `dev`.
- **Ce qui ne lui est pas demandé :** changer les autres messages, le bouton (« Rattacher » / « Recommencer »,
  mission 068), le motif `hors_15_m` ; toucher `fixtures/`.

## Harnais

- **Peut :** modifier `Verdict.kt` et les tests qui lisent ce message ; pousser sur
  `mission/077-cliquez-sur-rattacher` ; ouvrir la PR.
- **Ne doit pas :** modifier `fixtures/`, `docs/`, `pilotage/` (sauf la redite de cette fiche) ; toucher à
  `main` ; trancher une question de sens.

## Tests

- `.claude/test-command` au code 0 ; le nouveau test vu **rouge** avant la correction.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. Critère d'arrêt : 3 allers-retours.
- **Qui tranche :** Pascal. **Fusion : autorisation à demander à Pascal.**

## Comment saura-t-on que ça marche ?

Sur la release suivante, contrôle d'une placette loin du piquet : le message dit « puis cliquez sur le
bouton « Rattacher » » ; capture de l'orchestrateur.
