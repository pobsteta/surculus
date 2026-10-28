# Mission 012-B2 — Pointer les plants à la boussole du téléphone

> Fiche écrite par l'orchestrateur le 2026-10-28, à partir du gabarit
> `imperatrice/skills/mission/fiche-mission.md`. Seconde moitié de la 012-B (décision de Pascal du
> 2026-10-27, « Deux PR ») ; la 012-B1 est fusionnée (6bcb50d, v0.7.67). **Fiche mère :**
> `pilotage/missions/012b-ecran-controle.md` (décisions de Pascal 1 à 10 et « Décisions de Pascal sur
> le plan » 1 à 12) ; **plan :** `pilotage/missions/012b-plan.md` (§ boussole, pointage, plan,
> amendements). Les deux sont dans `main`. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Ajouter à l'écran du contrôle le pointage des plants
(azimut lu à la boussole du téléphone en cap magnétique pur, clavier en secours avec choix degrés ou
grades, distance tapée), le plan des plants autour du piquet et les regarnis, écrire les
amendements validés de la spec et de l'ADR 0003, et solder les reprises de la B1, pour que Pascal
contrôle à Couchey une placette sans plant ancien, de bout en bout.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Ce qui est déjà décidé (ne pas rediscuter)

- Tout ce qui, dans la fiche mère, relève de la B2 : boussole du téléphone, cap magnétique pur
  (jamais `GeomagneticField` d'Android : la déclinaison est ajoutée **une seule fois**, par la
  session) ; visée à plat, bord haut vers le plant, cap à l'appui sur « Pointer » (moyenne de la
  dernière seconde) ; précision faible ou champ perturbé : avertir sans bloquer (« dessinez un 8 ») ;
  précision non gardée au journal (format v1 inchangé) ; clavier en secours (« Saisir au clavier »,
  `SourceAzimut.SAISIE`) avec sélecteur degrés / grades mémorisé par tournée ; regarni sur le plant
  courant mort ou absent ; plants anciens non dessinés (mention en petit) ; pas de rattachement par
  code seul.
- **Amendements** de la spec § 1, § 3 (l. 75) et § 10, et de l'ADR 0003 : texte du plan § 5,
  complété par les décisions (clavier en secours, unité choisie à côté du champ), daté du jour.
- **Reprises de la B1** (décisions de Pascal sur la #108, 2026-10-28) : partager la moyenne de 5 s
  entre guidage et contrôle ; laisser la session seule juge de « Terminer sans état » ; ne demander
  la permission de stockage qu'une fois ; passer les essences de la tournée à la session (regarnis,
  `hors_referentiel`).
- **Message sans fix** (Pascal, 2026-10-28) : « Pas de position GNSS : approchez-vous du piquet et
  attendez un fix. » à la place de « lisez le code du piquet, puis recommencez » ; le champ du code
  reste pour confirmer l'identité quand le fix est là.

## L'Homme

- **Agent :** `afficheur` — **Modèle :** sonnet. **Temps de la main :** appuyer (le plan est fait et
  tranché : code directement ; toute question de sens nouvelle remonte avant d'être codée).
- **Ce qui ne lui est pas demandé :** le paysage de la carte (mission à part) ; volume, poche, voix
  (012-C) ; dictée (012-D) ; positions des plants anciens (mission à part) ; heure UTC du fix
  (mission courte à part).

## Harnais

- **Peut :** `app/` ; ajouts minimes à `:core` annoncés ; `docs/spec.md` et `docs/adr/0003-*.md`
  pour les seuls amendements décidés.
- **Ne doit pas :** modifier `fixtures/` ; toucher à `main` ; modifier `pilotage/journal.md` ;
  inventer un code ; écrire un `fontSize` ; trancher une question de sens.

## Tests

- Cap magnétique pur : test de source interdisant `GeomagneticField` ; test de comportement
  `azimut_geo − azimut_mag = déclinaison` (vu rouge) ; source de l'azimut écrite au journal.
- Clavier : degrés et grades convertis, hors [0, 360[ ou [0, 400[ refusé ; unité mémorisée par tournée.
- Pointage hors rayon : confirmation, pas un refus ; regarni lié par `remplace_uuid` ; regarni d'un
  plant sain refusé.
- Placette en Somalie ; contrastes clair et sombre ; cibles ≥ 48 dp.
- Moyenne de 5 s partagée (un seul code) ; « Terminer » sans état jugé par la session seule.
- `.claude/test-command` au code 0.

## Terrain

- **Échantillon :** à Couchey, Pascal pointe trois plants sur P35-02 (sans plant ancien), en dit
  les états, termine, et voit « Contrôlée ». Captures sur la release après fusion.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. Critère d'arrêt : 3 allers-retours.
- **Fusion : autorisation à demander à Pascal.**

## Comment saura-t-on que ça marche ?

À Couchey, une placette sans plant ancien passe « Contrôlée » après trois plants pointés.
