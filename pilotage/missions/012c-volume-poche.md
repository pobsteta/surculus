# Mission 012-C — Saisir le contrôle sans regarder l'écran

> Fiche écrite par l'orchestrateur le 2026-10-28, à partir du gabarit
> `imperatrice/skills/mission/fiche-mission.md`. Troisième des quatre missions de la 012
> (A session, 63b0659 ; B1 écran, 6bcb50d ; B2 pointage, f955af6 ; **C volume et poche** ; D dictée).
> Plan d'origine : `pilotage/missions/012-plan.md` (§ 5, étape 3).
> **Lancement :** le 2026-10-28, depuis `main` (f955af6), **en plan d'abord, sans code** (décision
> de Pascal : « Heure UTC + plan 012-C »). Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Faire saisir le contrôle d'une placette sans regarder
l'écran (boutons de volume selon la table de la spec, mode poche qui ignore le tactile, écran
maintenu allumé, vibration et synthèse vocale du numéro et de l'état), pour que le forestier, gants
aux mains et téléphone en poche, dise l'état de chaque plant d'une seule main.

**Redite par l'agent :** Rendre, sans code, le plan qui dit comment le forestier, téléphone en poche et gants aux mains, dira l'état de chaque plant au seul volume (gestes, mode poche, voix, vibration), avec le conflit « atteint » / sortie à 3 s résolu et les questions de sens posées à Pascal, pour qu'il tranche avant que le code s'écrive.

## Décisions de Pascal à respecter (2026-10-27)

1. **Entrée dans le mode poche** par un bouton à l'écran.
2. **Sortie du mode poche** par un appui de **3 s sur volume haut**, et à « Terminer ».
3. **Saisie après « Terminer »** : autorisée, avec l'alerte « placette déjà terminée » **à l'écran
   et à la voix** (la voix vient avec cette mission).
4. **Cause d'un plant atteint** saisie à l'écran tant que la dictée (012-D) n'est pas livrée.
5. **Couleurs en sombre** : règles de la 063 (texte `onSurface`, pastilles cernées, erreurs en
   `colorScheme.error`, aucune plaque blanche).
6. **Essence du regarni** : celle du remplacé, modifiable à l'écran, pas de touche « Aucune » (PR #109).

## Ce que le plan doit rendre (rien de codé)

Un fichier `pilotage/missions/012c-plan.md`, sur cette branche, avec :

- **la table des gestes** de la spec § 4.3 (volume haut ou bas, court, long, double) comme fonction
  pure (geste, contexte) → action, et ce que fait chaque geste **en placette** dans l'état actuel
  de l'écran (plant courant, plant suivant, regarni, après « Terminer ») ;
- **le conflit à résoudre** : « volume haut, appui long = atteint » contre « appui de 3 s sur volume
  haut = sortie du mode poche ». Comment les distinguer sans erreur avec des gants, et ce qui est
  enregistré quand l'appui dure 3 s (rien, ou « atteint » annulé) ;
- **« volume bas, appui long = ouvre la dictée »** tant que la 012-D n'est pas livrée : que se
  passe-t-il (message dit à la voix, rien) ;
- **le mode poche** : ce qu'il ignore (tactile), ce qu'il garde (écran allumé, volume), ce qui
  arrive à l'appel entrant, au verrouillage, à la rotation, et le risque d'agent « enfermé » ;
- **la voix** : texte exact de chaque annonce (« numéro 7, sain », refus, « placette déjà
  terminée »), cas sans voix française installée (message à l'écran, jamais de silence), et
  l'articulation avec la voix Vosk de la 048 (aucune, la synthèse est `TextToSpeech`) ;
- **la vibration** : un motif par état, et ce qui est permis sans autorisation ;
- **les sources** : Marculus `MainActivity.kt:75-120` (touches de volume, lecture seule), spec § 4.3,
  ADR 0004 ;
- **les questions de sens** à poser à Pascal, une par une, avec une proposition pour chacune ;
- **le découpage** si la mission tient en plus d'une PR.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | en partie : la table des gestes est **[proposée]** dans la spec et entre en conflit avec la sortie du mode poche | plan d'abord |
| A-t-il les moyens ? | oui pour le plan ; le code se vérifiera sur le téléphone de Pascal, puis à Couchey | |

**Temps de la main :** **appuyer**. **Plan d'abord, sans code** : l'agent rend son plan, Pascal
tranche à son retour, puis il code.

## L'Homme

- **Agent :** `afficheur`.
- **Ce qui ne lui est pas demandé :** la dictée (012-D) ; l'essai écran éteint (014) ; changer la
  logique de la session (s'il lui manque quelque chose, il le dit) ; cheminement ; photos.

## Harnais

- **Peut (pour le plan) :** lire tout le dépôt ; écrire `pilotage/missions/012c-plan.md` ; pousser
  sur `mission/012c-volume-poche`.
- **Ne doit pas :** écrire du code avant la décision de Pascal ; modifier `fixtures/` ; toucher à
  `main` ; modifier `pilotage/journal.md` ; inventer un code ; trancher une question de sens.
- **Sources autorisées :** spec ; ADR 0004, 0008 ; fiches 012, 012-A, 012-B, 012-B2, 048, 063 ;
  Marculus et Groma en lecture seule.

## Tests (pour le code, après le plan)

- Classeur de gestes en Kotlin pur, horloge injectée : court, long, double, appui de 3 s, deux
  touches ensemble ignorées, rebond.
- Mode poche : fonction pure « le toucher est-il pris en compte ? ».
- Annonce vocale pour chaque état, texte exact ; voix française absente.
- Double appui bas annule la dernière saisie ; une saisie annulée ne rend pas la placette
  « Contrôlée ».
- **Placette en Somalie** : un fix hors de France ne pointe rien, même par le volume.
- **Vu rouge** une fois. `.claude/test-command` au code 0.

## Terrain

- **Échantillon :** release qui suit la fusion, capture du téléphone en mode poche ; puis à
  Couchey, une placette dite au volume, téléphone en poche, gants.
- **Test des dix secondes** (Pascal) : sans regarder, savoir quel plant on vient de dire et son état.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. Critère d'arrêt : 3 allers-retours.
- **Qui tranche :** Pascal. **Fusion : autorisation à demander à Pascal.**

## Comment saura-t-on que ça marche ?

À Couchey, Pascal contrôle une placette déjà pointée, téléphone en poche, au volume seul, et
entend chaque numéro et chaque état.
