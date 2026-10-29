# Mission 071 — La dictée ne plante plus la release

> Fiche écrite par l'orchestrateur le 2026-10-29 (cycle 13), à partir du gabarit
> `imperatrice/skills/mission/fiche-mission.md`, après l'essai de Pascal sur la release v0.7.80
> (afcdbd1) : « quand je maintiens le volume bas, l'application se ferme, quand je maintiens le
> bouton Tenir pour parler, l'application se ferme ». Depuis `main` (afcdbd1). Une mission = une
> phrase = une branche = une PR. Urgent : la D2 est inutilisable sur la release.

## Mission

**En une phrase (quoi, et pourquoi) :** Garder dans la release les classes de JNA et de Vosk que R8
retire, et faire attraper ce cas par l'essai de fumée, pour que tenir le micro ou le volume bas
ouvre la dictée au lieu de fermer l'application.

**Redite par l'agent :** (à remplir par l'agent avant d'agir)

## Ce qui est constaté

Journal de plantage du téléphone (`adb logcat -b crash`), v0.7.80, quatre fois entre 06:53 et 06:58 :

```
FATAL EXCEPTION: ecoute-vosk
Process: io.github.pobsteta.surculus
java.lang.UnsatisfiedLinkError: Can't obtain peer field ID for class com.sun.jna.Pointer
	at com.sun.jna.Native.initIDs(Native Method)
	at com.sun.jna.Native.<clinit>(…)
	at org.vosk.LibVosk.<clinit>(…)
```

`app/proguard-rules.pro` garde `org.sqlite.**` mais ni `com.sun.jna.**` ni `org.vosk.**` : le code
natif de JNA cherche ses classes et ses champs par leur nom, R8 les renomme. Les tests unitaires et
`assembleDebug` ne passent pas par R8, d'où le vert de la CI.

## Ce qui est à faire

1. Ajouter à `app/proguard-rules.pro` les règles de garde de JNA et de Vosk, chacune commentée comme
   celles de sqlite-jdbc (pourquoi, d'où vient la règle : documentation de JNA ou de vosk-android
   0.3.75, citée). Garder le moins possible, mais assez pour que `LibVosk` se charge.
2. Faire éprouver ce chargement par l'**essai de fumée** du type de build `fumee` (calqué sur la
   release, même R8) : un test instrumenté qui charge `org.vosk.LibVosk` (ou crée l'objet Vosk le plus
   léger qui passe par JNA), sans modèle téléchargé. Vérifier qu'il **échoue** sans les règles du
   point 1 (le dire dans la PR, avec la sortie), puis qu'il passe avec. Les règles de garde vont dans
   `proguard-rules.pro` (la release), **pas** dans `proguard-fumee.pro`, sinon l'essai ne prouve rien.
3. Dire dans la PR comment l'essai de fumée tourne (workflow, émulateur) et s'il a tourné.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | |
| A-t-il les moyens ? | oui, sauf peut-être un émulateur en local | si l'essai de fumée ne tourne qu'en CI, le dire |

**Temps de la main :** **appuyer**.

## L'Homme

- **Agent :** `dev`.
- **Ce qui ne lui est pas demandé :** l'icône du bouton micro (mission 072) ; changer la dictée, le
  mode poche ou un texte ; la sortie du mode poche au bouton marche.

## Harnais

- **Peut :** modifier `app/proguard-rules.pro`, ajouter un test instrumenté de fumée et ce qu'il lui
  faut dans le workflow ; pousser sur `mission/071-dictee-release-r8` ; ouvrir la PR.
- **Ne doit pas :** modifier `fixtures/`, `docs/`, `pilotage/` ; toucher à `main` ; mettre la règle de
  garde dans `proguard-fumee.pro` ; trancher une question de sens.

## Tests

- `.claude/test-command` au code 0, et l'essai de fumée vu **rouge** sans les règles, puis vert.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. Critère d'arrêt : 3 allers-retours.
- **Qui tranche :** Pascal. **Fusion : autorisation à demander à Pascal.**

## Comment saura-t-on que ça marche ?

Sur la release suivante installée au téléphone, tenir « Tenir pour parler » ou le volume bas en poche
ouvre l'écoute ; `adb logcat -b crash` ne montre plus `ecoute-vosk`.
