# Mission 076 — Le bandeau d'écoute qui s'efface

> Fiche écrite par l'orchestrateur le 2026-10-30 (cycle 15), à partir du gabarit
> `imperatrice/skills/mission/fiche-mission.md`, sur la décision de Pascal du 2026-10-30 après la
> capture de la v0.7.88 : « Bandeau qui s'efface ». Depuis `main` (395b4e8). Une mission = une phrase =
> une branche = une PR. Suite de la mission 075 (PR #130).

## Mission

**En une phrase (quoi, et pourquoi) :** Faire disparaître le bandeau d'écoute quelques secondes après la
fin de l'écoute, pour que « Mode poche » et le haut de l'écran redeviennent visibles, debout comme
couché, sans que le forestier ait à faire quoi que ce soit.

**Redite par l'agent :** Le bandeau d'écoute reste pendant l'écoute puis quelques secondes après sa fermeture (compris ou refusé), puis s'efface seul, pour que « Mode poche » et le haut de l'écran redeviennent visibles, debout comme couché.

## Ce qui est constaté

Capture de l'orchestrateur, v0.7.88, 2026-10-30, contrôle de P35-03 :

- Micro tenu 5 s sans parler, puis lâché : le bandeau encadré affiche « ÉCOUTE FERMÉE » et **ne part
  plus** (vérifié 10 s après, puis après une rotation).
- Couché, ce bandeau (posé par-dessus le haut, décision de Pascal pour la 075) recouvre « Mode poche »
  (bandeau `[173,192][774,294]`, bouton `[915,209][1135,266]` en px) : le bouton répond encore, mais on
  ne le voit plus. Debout, il recouvre le numéro du plant et ses flèches.
- Cause : `Ecoute.kt` l. 263, l'état `FERMEE`/`SUSPENDUE` garde `AffichageEcoute.FERMEE` dès qu'une
  écoute a eu lieu (antérieur à la 075) ; `EcranControle.kt` l. 392 montre le bloc tant que
  `affichage != RIEN || compris != null`.

## Ce qui est à faire

1. Décision de Pascal : le bandeau s'affiche **pendant l'écoute**, puis **quelques secondes** avec ce qui
   a été entendu et compris (ou refusé), **puis disparaît** ; « Mode poche » réapparaît. Debout comme
   couché.
2. La durée : proposer une valeur dans la PR, avec sa raison (le temps de lire « compris : Numéro 7,
   atteint. » d'un coup d'œil) ; Pascal la tranche à la relecture. Une constante nommée, un seul endroit.
3. Le temps passe par l'horloge de la machine d'écoute (`avancer`, `echeance`), pas par un `delay` dans
   Compose, pour que le test le pilote sans attendre. Une nouvelle écoute pendant l'affichage le relance
   normalement ; un nouvel affichage remet le compte à zéro.
4. Tests d'abord, vus rouges : après une écoute fermée, avant l'échéance, le bandeau est montré ; après
   l'échéance, il ne l'est plus (affichage et `compris` qui ne retiennent plus le bloc). Le même après un
   refus (« refusé : … »).

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui (machine d'écoute à horloge injectée) | |
| A-t-il les moyens ? | oui | |

**Temps de la main :** **appuyer**.

## L'Homme

- **Agent :** `afficheur`.
- **Ce qui ne lui est pas demandé :** changer la disposition de la 075 (bandeau par-dessus le haut, micro
  112 dp, colonne couchée), les textes de l'écoute, les vibrations, le mode poche (sa bande est à part,
  elle ne s'efface pas) ; toucher `fixtures/`.

## Harnais

- **Peut :** modifier `Ecoute.kt`, `EcranControle.kt`, `EcouteApp` et leurs tests ; pousser sur
  `mission/076-bandeau-qui-s-efface` ; ouvrir la PR.
- **Ne doit pas :** modifier `fixtures/`, `docs/`, `pilotage/` (sauf la redite de cette fiche) ; toucher
  à `main` ; trancher une question de sens (la durée est proposée, pas décidée).

## Tests

- `.claude/test-command` au code 0 ; les nouveaux tests vus **rouges** avant la correction.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. Critère d'arrêt : 3 allers-retours.
- **Qui tranche :** Pascal (la durée, la fusion). **Fusion : autorisation à demander à Pascal.**

## Comment saura-t-on que ça marche ?

Sur la release suivante, contrôle d'une placette, couché : tenir le micro, lâcher ; le bandeau part après
la durée retenue et « Mode poche » est visible ; capture de l'orchestrateur, debout et couché.

## Reprise, aller-retour 1/3 (2026-10-30)

Relecture de la #132 (69b60af) : « prêt pour Pascal », sans bloquant. Décision de Pascal du 2026-10-30 :
« 6 s ».

1. **Pascal** : `DUREE_BANDEAU_MS` passe à **6 s**, plus long que l'attente du résultat final de Vosk
   (`RESULTAT_FINAL_MS`, 5 s) : une fois parti, le bandeau ne revient jamais pour un résultat tardif. Dire
   dans la PR et dans le commentaire de la constante pourquoi elle doit rester au-dessus de
   `RESULTAT_FINAL_MS`.
2. **Test (mineur M1 du relecteur)** : un résultat final arrivé juste avant l'attente maximale (≈ 5 s après
   le lâcher) trouve le bandeau encore montré, sans qu'il ait disparu entre le lâcher et ce résultat ; et
   une garde qui casse si `DUREE_BANDEAU_MS <= RESULTAT_FINAL_MS`.
3. Mettre à jour le corps de la PR (durée tranchée, retour du bandeau supprimé par la durée).
