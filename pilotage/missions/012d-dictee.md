# Mission 012-D — La dictée branchée sur l'écran du contrôle

> Fiche écrite par l'orchestrateur le 2026-10-28, à partir du gabarit
> `imperatrice/skills/mission/fiche-mission.md`. Quatrième des missions de la 012
> (A session, 63b0659 ; B1 écran, 6bcb50d ; B2 pointage, f955af6 ; C1 volume et poche, b9cf9d6 ;
> C2 voix et vibration, PR #114 en attente ; **D dictée**).
> Plan d'origine : `pilotage/missions/012-plan.md` (§ 4 et § 5, étape 4).
> **Lancement :** le 2026-10-28, depuis `main` (5bfb1c5), **en plan d'abord, sans code** (décision
> de Pascal : « Préparer la 012-D »). Le code part après la fusion de la #114 et les réponses de
> Pascal, pour ne pas toucher l'écran du contrôle en même temps que la C2.
> Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Brancher la dictée hors ligne (Vosk, grammaire fermée) sur
le plant courant de l'écran du contrôle, ouverte par « volume bas, appui long », chaque refus dit à
la voix, pour que le forestier dise la cause d'un plant atteint, sa hauteur ou une correction sans
regarder l'écran ni retirer ses gants.

**Redite par l'agent :** Brancher le micro de Vosk sur le plant courant de l'écran du contrôle (appui long sur volume bas en mode poche, bouton « Micro » hors du mode), confronter chaque phrase de la grammaire fermée à l'état du plant puis l'écrire par la session, et dire à la voix ce qui est compris ou refusé, pour que le forestier, gants aux mains et téléphone en poche, dise la cause, la hauteur ou une correction sans regarder l'écran. Cette étape ne rend que le plan : `pilotage/missions/012d-plan.md`, sans code.

## Décisions de Pascal à respecter

1. **La 012 branche la dictée et l'import du modèle sur l'écran du contrôle** (2026-10-24, journal,
   mission 013).
2. **« hauteur » sur un plant mort est acceptée** ; « hauteur » sur un plant sans état est refusée et
   la voix demande l'état d'abord (2026-10-24, spec § 4.3).
3. **« vide » et « fin du vide » reconnus**, tracé du contour en tranche 2, avec un message « pas
   encore disponible » (2026-10-27, Q6 du plan de la 012).
4. **Grammaire, mots de remplacement et somme SHA-256 du modèle** : spec § 4.3 (« climat »,
   « mangé », « remplace » ; comparaison sans tenir compte de la casse, DI-303).
5. **Gestes de volume en mode poche seulement** ; hors du mode, le volume règle le son (2026-10-28,
   décision 1 de la 012-C).
6. **« Atteint » en poche, cause vide** ; elle s'ajoute à l'écran ou à la dictée (décision 7 de la
   012-C).
7. **Sans voix française, mode poche permis** avec un bandeau, annonces écrites, vibration gardée
   (décision 8 de la 012-C).
8. **Couleurs en sombre** : règles de la 063.

## Ce que le plan doit rendre (rien de codé)

Un fichier `pilotage/missions/012d-plan.md`, sur cette branche, avec :

- **l'ouverture et la fermeture de l'écoute** : « volume bas, appui long » en mode poche ; et hors
  du mode poche, puisque le volume y règle le son (bouton à l'écran ?) ; fin de l'écoute (une
  phrase, un délai, un geste) ; ce que l'agent entend ou sent au début et à la fin ;
- **l'application des commandes** : `AnalyseurDictee` (013) vers `SessionControle.appliquer`
  (012-A), commande par commande de la grammaire de la spec § 4.3 : laquelle écrit une ligne,
  laquelle avance au plant suivant, « remplace », « annule », « suivant », « non observé » sans
  mesure, redite sans ligne, correction avec `remplace_uuid` ;
- **les refus** : texte exact dit à la voix pour chacun (mot non reconnu, « hauteur » sans état,
  hauteur hors de 1 à 400 cm, « vide », cause sur un plant absent, placette déjà terminée) ; et
  sans voix française (décision 7) ;
- **la permission `RECORD_AUDIO`** : quand elle est demandée, ce qui arrive au refus et au refus
  définitif, en mode poche et hors du mode ;
- **le modèle absent ou refusé** : ce que dit l'écran et la voix, le renvoi vers Paramètres → Dictée
  vocale (048), sans jamais bloquer la saisie au volume ;
- **la voix de sortie et le micro** : `TextToSpeech` (C2) ne doit pas être entendu par Vosk ;
  ordre des deux, et ce qui arrive si une annonce est en cours quand l'écoute s'ouvre ;
- **ce qui dépend de la #114** (C2, non fusionnée) et ce qui n'en dépend pas, lu sur la branche
  `mission/012c2-voix-vibration` ;
- **les sources** : spec § 4.3 ; fiches 012, 012-A, 012-C, 013, 036, 048 ; ADR 0004 ;
  `fixtures/dictee/` ; Marculus et Groma en lecture seule s'ils ont une dictée ;
- **les questions de sens** à poser à Pascal, une par une, avec une proposition pour chacune ;
- **le découpage** si la mission tient en plus d'une PR.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | en partie : moteur et analyse existent (013, 048), le branchement à l'écran et l'ouverture hors du mode poche ne sont pas décrits | plan d'abord |
| A-t-il les moyens ? | oui pour le plan ; le code dépend de la C2 (#114) et se vérifiera sur le téléphone de Pascal, puis à Couchey | |

**Temps de la main :** **appuyer**. **Plan d'abord, sans code** : l'agent rend son plan, Pascal
tranche, puis il code après la fusion de la #114.

## L'Homme

- **Agent :** `afficheur`.
- **Ce qui ne lui est pas demandé :** changer la grammaire ou le moteur de la 013 ; tracer les vides
  (tranche 2) ; les photos ; le cheminement ; l'essai écran éteint (014).

## Harnais

- **Peut (pour le plan) :** lire tout le dépôt et les branches `mission/012c2-voix-vibration` ;
  écrire `pilotage/missions/012d-plan.md` ; pousser sur `mission/012d-dictee`.
- **Ne doit pas :** écrire du code avant la décision de Pascal ; modifier `fixtures/` ; toucher à
  `main` ; modifier `pilotage/journal.md` ; inventer un code ; trancher une question de sens.
- **Sources autorisées :** spec ; ADR 0004, 0008 ; fiches citées ci-dessus ; Marculus et Groma en
  lecture seule.

## Tests (pour le code, après le plan)

- `fixtures/dictee/` (cas DI-…) rejoués à travers `SessionControle`, sans modifier `fixtures/` ;
  DI-303 reste celui de la 048.
- Chaque refus a son texte exact testé ; « hauteur » sans état ; « vide ».
- Permission refusée ; modèle absent : la saisie au volume continue.
- **Placette en Somalie** : un fix hors de France ne pointe rien, même par la dictée.
- **Vu rouge** une fois. `.claude/test-command` au code 0.

## Terrain

- **Échantillon :** release qui suit la fusion, capture du téléphone pendant l'écoute ; puis à
  Couchey, une placette dite à la voix, téléphone en poche, gants, vent.
- **Test des dix secondes** (Pascal) : sans regarder, savoir si la dictée a compris.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. Critère d'arrêt : 3 allers-retours.
- **Qui tranche :** Pascal. **Fusion : autorisation à demander à Pascal.**

## Comment saura-t-on que ça marche ?

À Couchey, Pascal contrôle une placette déjà pointée, téléphone en poche, dit « atteint » au
volume puis « insectes » à la voix, et entend la cause confirmée ; la tournée rendue porte la cause.
