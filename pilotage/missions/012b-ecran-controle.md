# Mission 012-B — L'écran du contrôle de placette, au toucher

> Fiche écrite par l'orchestrateur le 2026-10-27, à partir du gabarit
> `imperatrice/skills/mission/fiche-mission.md`. Deuxième des quatre missions de la 012
> (A session, fusionnée en 63b0659 ; **B écran** ; C volume et poche ; D dictée). Plan d'origine :
> `pilotage/missions/012-plan.md` (branche `mission/012-ecran-controle-de-placette`, § 5, étape 2).
> **Lancement :** le 2026-10-27, depuis `main` (63b0659). Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Réaliser l'écran du contrôle de placette au toucher (plan
des plants autour du piquet, pointage avec l'azimut lu à la boussole du téléphone et la distance
tapée, états, causes, regarnis, totaux, refus lisibles avec leur code, « Terminer »), branché sur
la `SessionControle` de la 012-A et ouvert par « Contrôler » sur la carte et à l'arrivée du
guidage, pour que Pascal contrôle une placette de bout en bout sur le téléphone et la voie passer
« Contrôlée » dans l'écran « Placettes ».

**Redite par l'agent :** Réaliser l'écran où le forestier pointe les plants (azimut lu par la boussole du téléphone, distance tapée), dit états et causes, pose les regarnis, voit « 7 sur 12 » et les refus avec leur code, puis termine la placette, ouvert par « Contrôler » sur la carte ou à l'arrivée du guidage et branché sur la `SessionControle` de la 012-A, pour que Pascal contrôle une placette de bout en bout sur son téléphone et la voie passer « Contrôlée » dans « Placettes » (plan d'abord, sans code : `pilotage/missions/012b-plan.md`).

## Décisions de Pascal à respecter (2026-10-27)

1. **Deux portes** : bouton « Contrôler » sur la carte quand une placette est choisie ; ouverture
   automatique à l'arrivée du guidage (moins de 30 m), avec retour au guidage, sans réouverture en
   boucle (réarmement à 40 m, `DetecteurArrivee`).
2. **« Contrôlée » à « Terminer »** ; « Terminer » sans état dit ce jour-là est refusé (déjà dans la
   session : `RienASaisir`) ; l'écran le dit clairement.
3. **Azimut lu par la boussole du téléphone**, distance horizontale tapée (réponse revue de Pascal,
   question 3). La session reçoit `SourceAzimut.BOUSSOLE_TELEPHONE`.
4. **Cause d'un plant atteint saisie à l'écran** (gros boutons des groupes de cause) tant que la
   dictée n'est pas livrée.
5. **Regarni** : « remplace » sur le plant courant mort ou absent, puis pointage du regarni.
6. **État précédent** affiché en petit et en gris à côté de l'état du jour.
7. **Saisie après « Terminer »** le même jour : autorisée, avec l'alerte « placette déjà terminée »
   à l'écran (la voix viendra avec la 012-C) ; la placette reste « Contrôlée ».
8. **Permission de stockage** (`WRITE_EXTERNAL_STORAGE`, Android 8 et 9 seulement) demandée à la
   première ouverture de l'écran, refus expliqué ; la sauvegarde ne bloque jamais la saisie.
9. **« vide »** : bouton absent ou message « pas encore disponible » (la session répond déjà
   `PasEncoreDisponible`).
10. **Couleurs en sombre** : celles décidées pour la 063 (texte en `onSurface`, état porté par une
    pastille cernée de blanc, erreurs en `colorScheme.error`, aucune plaque blanche). Si la 063
    n'est pas encore fusionnée, suivre ces règles sans dépendre de son code, et le dire.

## À proposer à Pascal dans le plan (rien de tranché)

- **Amendement de la spec § 1** (« Matériel de pointage à nommer par Pascal ») **et de l'ADR 0003**
  (boussole de visée) : texte exact proposé pour « azimut à la boussole du téléphone ».
- **Risque de la boussole du téléphone** en forêt (métal, inclinaison, étalonnage) : comment
  l'écran le montre (précision du capteur Android, appel à l'étalonnage) et **faut-il un clavier en
  secours pour l'azimut** (proposition de l'orchestrateur : oui, `SourceAzimut.SAISIE`, unité
  déclarée de la tournée).
- Azimut **magnétique** : le capteur Android donne un cap magnétique ; la session ajoute la
  déclinaison (WMM2025). Dire comment on s'assure de ne pas l'ajouter deux fois.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : spec § 4.2, § 4.3 ; `SessionControle` (012-A) ; `EcranGuidage` (arrivée, moyenne, rattachement) ; `BandeauCompact` | |
| A-t-il les moyens ? | oui : 012-A fusionnée ; téléphone de Pascal pour la capture sur la release | |

**Temps de la main :** **appuyer**. **Plan d'abord, sans code** : l'agent rend son plan (écran,
navigation, capteur, amendement proposé, questions), Pascal tranche, puis il code.

## L'Homme

- **Agent :** `afficheur` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** touches de volume, mode poche, écran maintenu allumé,
  vibration, synthèse vocale (012-C) ; dictée (012-D) ; changer la logique de la session (s'il lui
  manque quelque chose, il le dit) ; cheminement, alertes de reprise, photos.

## Harnais

- **Peut :** `app/` (écran, navigation, carte, guidage, permissions, manifeste) ; ajouts minimes à
  `:core` si l'écran en a besoin, annoncés dans la PR ; proposer l'amendement de la spec et de
  l'ADR 0003 dans la PR, **après validation de Pascal**.
- **Ne doit pas :** modifier `fixtures/` ; toucher à `main` ; modifier `pilotage/journal.md`
  autrement qu'en ajout ; inventer un code (état, cause, essence, refus) ; écrire un `fontSize`
  (styles de Marculus) ; trancher une question de sens.
- **Sources autorisées :** spec ; ADR 0003, 0004, 0008 ; fiches 012, 012-A, 061, 062, 063 ;
  Groma et Marculus en lecture seule.

## Tests

- `NavigationTest` : ouvrir le contrôle par les deux portes, retour, rotation, récepteur GNSS gardé
  tant que le contrôle est ouvert ; pas de réouverture en boucle à l'arrivée.
- État de l'écran en Kotlin pur : plant courant, « 7 sur 12 », état précédent, alerte « déjà
  terminée », refus montré avec son code (un test par refus), « Terminer » refusé sans état.
- Pointage hors rayon : alerte et confirmation, pas un refus.
- Azimut : source boussole du téléphone écrite au journal ; déclinaison non ajoutée deux fois.
- **Placette en Somalie** : un fix hors de France ne pointe rien.
- Contraste ≥ 4,5:1 des textes de l'écran en clair et en sombre ; cibles ≥ 48 dp.
- **Vu rouge** une fois. `.claude/test-command` au code 0.

## Terrain

- **Échantillon :** release qui suit la fusion, capture du téléphone : écran du contrôle ouvert
  depuis la carte, une placette de Couchey terminée, puis « Contrôlée » dans l'écran « Placettes ».
- **Test des dix secondes** (Pascal, en plein soleil, gants) : le numéro du plant dont on parle,
  son état, combien restent.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. Critère d'arrêt : 3 allers-retours.
- **Qui tranche :** Pascal. **Fusion : autorisation à demander à Pascal.**

## Comment saura-t-on que ça marche ?

Pascal termine une placette sur son téléphone et la voit passer « Contrôlée ».

## Décisions de Pascal sur le plan (2026-10-27, questions posées une par une)

1. **Deux PR** : **012-B1** (navigation et deux portes, arrivée et moyenne, états et causes,
   « Terminer », « Contrôlée », alerte « déjà terminée », permission de stockage) sur la branche
   `mission/012b-ecran-controle` ; **012-B2** (boussole, pointage, plan dessiné, regarnis,
   amendements de la spec § 1, § 3, § 10 et de l'ADR 0003) sur une branche à part, après B1.
2. **Clavier en secours pour l'azimut** : oui, petit lien « Saisir au clavier » sous la vue
   Pointer, `SourceAzimut.SAISIE`.
3. **Unité de l'azimut tapé** : sélecteur degrés / grades à côté du champ, mémorisé par tournée ;
   la boussole du téléphone reste en degrés.
4. **Précision faible ou champ perturbé** : avertir sans bloquer (« dessinez un 8 ») ; seuils à
   fixer à l'essai en forêt.
5. **Visée** : téléphone à plat, bord haut vers le plant, cap pris à l'appui sur « Pointer »
   (moyenne de la dernière seconde).
6. **Précision de la boussole au journal** : non pour l'instant (format v1 inchangé).
7. **Boutons d'état** : couleur et forme (sain vert ●, atteint orange ▲, mort rouge ✖, absent
   noir ○), règles de la 063 en sombre.
8. **Plants anciens** : non dessinés sur le plan, mention en petit ; mission à part pour leurs
   positions.
9. **Rattachement par code seul sans fix** : pas en 012-B.
10. **Moyenne de 5 s** lancée toute seule à l'ouverture, avec décompte.
11. **« Vide »** : pas de bouton (déjà décidé : « pas encore disponible » à la voix, 012-D).
12. **Sortie sans « Terminer »** : sans confirmation ; la placette reste « à faire ».

**Ajouts à la 012-B1** (décision de Pascal sur la #107, 2026-10-27) : un test des 4 libellés de la
légende (symbole en tête, retiré par `substringAfter(' ')`) et le test de sources de la 063
(`SansPlaqueSourcesTest`) passé en motif multiligne.

La 063 est fusionnée (20740b3) : la B1 part de `main` à jour et utilise son code de couleurs.
