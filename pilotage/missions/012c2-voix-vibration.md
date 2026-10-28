# Mission 012-C2 — La voix et la vibration du mode poche

> Fiche écrite par l'orchestrateur le 2026-10-28, à partir du gabarit
> `imperatrice/skills/mission/fiche-mission.md`. Seconde PR de la 012-C (C1 fusionnée en b9cf9d6,
> PR #113). Fiche mère : `pilotage/missions/012c-volume-poche.md` (neuf décisions de Pascal sur le
> plan) ; plan : `pilotage/missions/012c-plan.md` (§ 7 la voix, § 8 la vibration).
> **Lancement :** le 2026-10-28, depuis `main` (b9cf9d6), sur l'ordre de Pascal (« Oui, lance la
> C2 »). **Code direct**, le plan est tranché. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Faire dire et faire sentir au téléphone chaque geste du
mode poche (synthèse vocale `TextToSpeech` en français du numéro et de l'état, vibration d'un
motif par événement), pour que le forestier, téléphone en poche, sache sans regarder quel plant il
vient de dire et dans quel état.

**Redite par l'agent :** Brancher sur le mode poche de la C1 une annonce vocale française
(`TextToSpeech`, textes et motifs du plan § 7 et § 8) et une vibration par événement, avec repli écrit dans la bande et bandeau
si la voix française manque, pour que le forestier, téléphone en poche, sache sans regarder quel plant il vient de dire et dans
quel état.

## Décisions de Pascal à respecter (fiche 012-C, 2026-10-28)

- **2.** La voix annonce le plant suivant après un sain ou un mort : « numéro 7, sain. Suivant, 8. »
- **4.** Rappel vocal « mode poche actif » à 5 min sans geste (la sortie à 15 min et le retour
  ignoré sont dans la C1).
- **6.** « Placette déjà terminée » à la voix, **à chaque geste**, avant le numéro.
- **7.** « Atteint » en poche : la voix dit « atteint », jamais « cause ».
- **8.** Sans voix française installée : mode poche permis, bandeau « Pas de voix française :
  regardez l'écran », annonces écrites dans la bande réservée par la C1, vibration gardée ;
  jamais de silence.
- **9.** Périmètre C2 seul : voix, vibration, `VIBRATE` au manifeste. Rien ne change au classeur
  de gestes, aux seuils ni au mode poche de la C1 (sauf pour y brancher la voix et la vibration).

Les textes exacts des annonces et les motifs de vibration sont ceux du plan (§ 7, § 8). Une
annonce interrompt la précédente (`QUEUE_FLUSH`). `TextToSpeech` n'a aucun lien avec Vosk (048).

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : plan § 7 et § 8, C1 fusionnée (`ClasseurGestes`, `ModePoche`, `PontPoche`, bande) | |
| A-t-il les moyens ? | oui ; essai sur le téléphone de Pascal sur la release, puis à Couchey | |

**Temps de la main :** **appuyer**. Code direct.

## L'Homme

- **Agent :** `afficheur`.
- **Ce qui ne lui est pas demandé :** la dictée (012-D) ; changer les gestes, les seuils, le mode
  poche ou la session ; l'essai écran éteint (014).

## Harnais

- **Peut :** `app/` (adaptateur `TextToSpeech`, textes purs, motifs purs, manifeste `VIBRATE`,
  branchement sur le pont de la C1) ; ajouts minimes à `:core` annoncés dans la PR.
- **Ne doit pas :** modifier `fixtures/` ; toucher à `main` ; modifier `pilotage/journal.md` ;
  inventer un code ; écrire un `fontSize` ; trancher une question de sens.
- **Sources autorisées :** spec § 4.3 ; ADR 0004, 0008 ; fiches 012-C, 012-B, 048, 063 ;
  Marculus et Groma en lecture seule.

## Tests

- Texte exact de chaque annonce (fonction pure) : sain, mort, atteint, absent, suivant, annulation,
  refus, « placette déjà terminée » avant le numéro, « mode poche actif », « mode poche terminé »,
  « dictée pas encore disponible ».
- Rappel à 5 min sans geste, une seule fois par période ; aucun rappel après la sortie.
- Voix française absente ou initialisation en échec : texte dans la bande, bandeau à l'entrée,
  vibration gardée, jamais de silence.
- Motifs de vibration tous distincts ; appareil sans vibreur : rien ne casse.
- **Placette en Somalie** : un fix hors de France ne pointe rien, et rien n'est annoncé comme écrit.
- Chaque test **vu rouge** une fois, en sabotant le code. `bash .claude/test-command` au code 0.

## Terrain

- **Échantillon :** release qui suit la fusion, sur le téléphone de Pascal : en mode poche, chaque
  geste entendu et senti ; sans voix française, le bandeau. Puis à Couchey, gants, téléphone en poche.
- **Test des dix secondes** (Pascal) : sans regarder, dire quel plant on vient de dire et son état.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. Critère d'arrêt : 3 allers-retours.
- **Qui tranche :** Pascal. **Fusion : autorisation à demander à Pascal**, après l'essai de la C1
  sur le téléphone (décision de Pascal du 2026-10-28).

## Comment saura-t-on que ça marche ?

À Couchey, téléphone en poche, Pascal entend et sent chaque saisie, et sait sans regarder où il en
est.
