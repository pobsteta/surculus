# Mission 012-D2 — Le micro : tenir pour parler, en poche et hors poche

> Fiche écrite par l'orchestrateur le 2026-10-28 (cycle 13), à partir du gabarit
> `imperatrice/skills/mission/fiche-mission.md`. Deuxième PR de la 012-D (D1 : cœur de la dictée,
> PR #116, fbc2f93). Plan d'origine : `pilotage/missions/012d-plan.md` (f734cef), fiche
> `pilotage/missions/012d-dictee.md`. Une mission = une phrase = une branche = une PR.
> **Dépend de :** la 067 (PR #118, même écran du contrôle) : ne commencer le code qu'après sa
> fusion, branche rebasée sur `main`.

## Mission

**En une phrase (quoi, et pourquoi) :** Brancher le micro de Vosk sur l'écran du contrôle en « tenir
pour parler » (volume bas tenu en mode poche, bouton micro tenu hors du mode poche, comme Marculus),
chaque phrase passée à `ApplicateurDictee` et confirmée à la voix, pour que le forestier dise la
cause, la hauteur ou une correction gants aux mains, téléphone en poche.

**Redite par l'agent :** _à écrire par l'agent avant d'agir._

## Décisions de Pascal à respecter (2026-10-28, questions posées une par une)

1. **En mode poche : volume bas tenu = micro ouvert tant que la touche est tenue** ; pas de bouton
   à l'écran en poche (le toucher reste ignoré). Pascal : « en mode poche ajoute le bouton micro
   comme dans marculus pour parler », puis « Volume bas tenu seulement ». Le plan proposait une
   écoute qui reste ouverte : **remplacé** par « tenir pour parler ».
2. **Hors du mode poche : un bouton micro, tenu pour parler, comme Marculus**
   (`~/dev/marculus/.../ui/feuille/DicteeVocale.kt`, `BoutonMicroPtt`, lecture seule).
3. **Q3 — la voix confirme aussi hors du mode poche** pendant la dictée.
4. **Q6 — permission `RECORD_AUDIO`** demandée à l'appui sur « Mode poche » et sur le bouton micro,
   l'écran sous les yeux, jamais en poche ; le mode poche entre quand même si elle est refusée ;
   refus définitif : message qui renvoie aux réglages de l'application.
5. **Q7 — un bruit ou `[unk]` seul : « Pas compris. »**, sauf dans la seconde qui suit la vibration
   d'ouverture.
6. **Q8 — « remplace » en poche : mis en attente** ; à la sortie du mode poche, la vue Plant
   propose « Remplace le plant n° X : pointez le regarni » avec « Pointer » en grand.
7. **Q11 — délais tels quels, en constantes nommées, à mesurer** : 5 s de garde-fou si la voix ne
   finit pas, vibration d'ouverture 200 ms, « un instant » si le modèle met plus de 2 s à charger.
   Le délai « 10 s sans phrase » du plan tombe (tenir pour parler).
8. **D1 (déjà fusionnée)** : Q4 seul « suivant » avance, Q5 garder si permis, Q9 alerte devant toute
   phrase comprise, Q10 textes de `TextesDictee`.
9. **012-C** : gestes de volume en mode poche seulement ; bas court = mort après 0,35 s ; double bas =
   annule ; haut 3 s = sortie ; seuils tels quels. **Couleurs** : règles de la 063.

## Ce qui est à faire

- **Classeur de gestes (C1)** : il ne signale pas le relâchement après un appui long (plan § 3.1).
  Ajouter le signal « volume bas relâché après un long », sans changer les seuils ni les autres
  gestes ; tests du classeur étendus.
- **Écoute** : ouverte au long bas (0,5 s) ou à l'appui sur le bouton micro, fermée au relâchement
  (Vosk rend son résultat final à la fermeture) ; fermée aussi à la sortie du mode poche,
  `onPause`/`onStop`, erreur du moteur ; jamais rouverte seule.
- **Anti-larsen** : micro coupé pendant une annonce ; `MoteurVoix` reçoit un signal de fin
  (`UtteranceProgressListener.onDone`), garde-fou 5 s (plan § 8).
- **Vibrations** : deux motifs nouveaux, ouverture et fermeture, distincts des neuf de la C2
  (plan § 3.3).
- **Modèle absent ou refusé** : l'écran et la voix le disent, renvoi vers Paramètres → Dictée
  vocale ; la saisie au volume continue (plan § 7).
- **Libellés des causes** : un seul libellé pour l'écran et la voix (doublon
  `ControleurPlacette.libelle` / `TextesDictee`, « Champignons » / « champignon », relevé à la D1).
- **Bande** : « J'ÉCOUTE » pendant l'écoute, puis `entendu / compris` ou `refusé : …`.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : plan rendu, questions tranchées, Marculus en modèle | |
| A-t-il les moyens ? | oui ; temps de chargement du modèle jamais mesuré, à mesurer sur le téléphone de Pascal | |

**Temps de la main :** **appuyer**.

## L'Homme

- **Agent :** `afficheur`.
- **Ce qui ne lui est pas demandé :** changer la grammaire, le moteur Vosk ou `ApplicateurDictee`
  (sauf pour y brancher le libellé commun) ; tracer les vides ; photos ; cheminement.

## Harnais

- **Peut :** modifier `app/.../controle/`, le classeur de gestes, `MoteurVoix`, les vibrations et
  leurs tests ; pousser sur `mission/012d2-micro` ; ouvrir la PR.
- **Ne doit pas :** modifier `fixtures/`, `docs/`, `pilotage/journal.md` ; toucher à `main` ;
  changer un seuil de la C1 ; inventer un code ; trancher une question de sens.
- **Sources autorisées :** spec § 4.3 ; ADR 0004 ; plan et fiches 012-C, 012-D ; Marculus en
  lecture seule (`DicteeVocale.kt`, `MainActivity.kt` l. 88, `PttController.kt`).

## Tests

- Classeur : relâchement après un long bas ; aucun autre geste changé.
- Machine d'écoute en fonction pure : ouverture, fermeture au relâchement, à la sortie de poche, à
  `onStop`, sur erreur ; jamais de réouverture seule ; micro coupé pendant une annonce, rouvert si
  la touche est encore tenue, garde-fou 5 s.
- Permission refusée et refus définitif ; modèle absent : la saisie au volume continue.
- Motifs de vibration : onze distincts, chacun de moins d'une seconde.
- `[unk]` dans la seconde après l'ouverture : ignoré ; après : « Pas compris. ».
- **Placette en Somalie** : un fix hors de France ne pointe rien, même par la dictée.
- **Vu rouge** une fois. `.claude/test-command` au code 0.

## Terrain

- **Échantillon :** release qui suit la fusion ; capture de la bande « J'ÉCOUTE » ; Pascal tient
  volume bas en poche et dit « atteint », puis « insectes ».
- **Test des dix secondes** (Pascal, plan § 13) : sans sortir le téléphone, savoir que le micro
  écoutait, ce qu'il a compris, quand il a cessé, et qu'une phrase fausse est refusée.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. Critère d'arrêt : 3 allers-retours.
- **Qui tranche :** Pascal. **Fusion : autorisation à demander à Pascal.**

## Comment saura-t-on que ça marche ?

À Couchey, téléphone en poche, Pascal tient volume bas, dit « atteint » puis « insectes », entend
« Numéro 7, atteint. » puis « Numéro 7, insectes. » ; la tournée rendue porte la cause.
