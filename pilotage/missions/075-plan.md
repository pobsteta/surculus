# Mission 075 — Plan sans code : le bouton micro comme celui de Marculus

> Rédigé par l'`afficheur` le 2026-10-29, depuis `main` (093edf3). Aucun code. Le code ne part
> qu'après les réponses de Pascal (section 6), dans la même branche.

**Redite.** Poser le micro hors poche à portée du pouce, sans défiler, rond, rouge à l'écoute, barré quand il
ne peut pas servir, et faire que « J'ÉCOUTE » n'écarte plus rien sous le doigt.

## 1. La fiche d'affichage

- **Question** : « Puis-je parler maintenant, et la machine m'écoute-t-elle ? » Pour le forestier en forêt, gants,
  une main libre, téléphone debout ou couché.
- **Une seule chose en grand** : le micro (rond, 112 dp comme Marculus), toujours au même endroit.
- **Comparaison** : l'état du bouton lui-même, avant / pendant : gris-clair prêt, **rouge** à l'écoute, **barré** si
  impossible. Pas de couleur nouvelle : ce sont les rôles `primaryContainer`, `error`, `surfaceVariant` de Marculus.
- **Source et date** : sans objet (état local de la dictée, `EcouteApp`) ; aucun chiffre figé à dater.
- **Lieu** : gant, soleil, pluie. Cible de 112 dp, contraste fort, texte « J'ÉCOUTE » en grand, lisible sans lire de détail.
- **1664** : une idée (parler), un bouton, un bloc de deux lignes, trois couleurs (neutre, rouge, gris).

## 2. Constats mesurés (captures c07 et c08, paysage 2316 × 1080 à 450 dpi, soit environ 823 × 384 dp)

- Le bouton est dans la colonne qui **défile** (`EcranControle.kt` l. 342 et 468 à 474), tout en bas, après le plan :
  en paysage, il faut défiler pour le voir (c07 : seul le bas du plan, puis les boutons).
- Le bloc « J'ÉCOUTE » est dans la colonne **fixe au-dessus** de la zone qui défile (l. 339 à 341) : quand il apparaît,
  il **pousse** toute la zone vers le bas, d'environ 190 px, soit 68 dp (c08). Le bouton fuit sous le doigt : cause
  mécanique du glissement, avant même la question du composable (déjà réglée par la #121).
- Bouton actuel : contour, hauteur 56 dp, icône 32 dp, plein `primary` à l'écoute. Marculus (`DicteeVocale.kt`
  l. 340 à 400) : FAB rond de 112 dp, icône 48 dp, `error` à l'écoute, `primaryContainer` au repos, `surfaceVariant` et
  `MicOff` quand le micro ou le modèle manquent.
- Hauteur utile couché : environ 384 dp moins le bandeau (environ 40 dp) : 112 dp de bouton en bas coûte un tiers de l'écran.

## 3. Où le placer (question 1 du plan)

Principe : **hors de la zone qui défile**, donc jamais « sous le pli », et **place réservée**, pas en surimpression :
un FAB posé par-dessus cacherait Terminer ou Annuler sous un pouce ganté.

| Orientation | Proposition | Coût |
|---|---|---|
| Debout (portrait) | Pied fixe sous la zone qui défile, micro à droite (côté pouce droit), « Mode poche » à gauche dans le même pied | environ 128 dp de hauteur sur 800 : acceptable |
| Couché (paysage) | **Colonne fixe à droite**, micro en bas de la colonne (128 dp de large) ; la zone qui défile prend le reste à gauche | 128 dp de largeur sur 823 : la hauteur est gardée, l'enjeu couché |

Le plan couché reste sous les boutons dans la zone qui défile (rappel de la 067) ; boutons d'état, Pointer, Annuler,
Terminer ne bougent pas : ils perdent seulement la largeur de la colonne. Le bouton ne se montre que sur la vue
Plant, jamais en mode poche (décision du 2026-10-28).

## 4. Forme, taille, couleurs, contrastes

- Rond, 112 dp, icône 48 dp (valeurs de Marculus, `TAILLE_MICRO`, `ICONE_MICRO`), cible de 112 dp : au-delà de
  toute exigence de cible gantée (ADR 0008 ne fixe pas de taille ; `DimensionsControle.CIBLE_MIN` mesure le minimum).
- Trois états, une fonction pure testable `AspectMicro.pour(affichage, prêt)` : **PRÊT** (`primaryContainer`, icône
  `onPrimaryContainer`), **ÉCOUTE** (`error`, icône `onError`, ouverture comprise), **BARRÉ** (`surfaceVariant`,
  `MicOff`, icône `onSurfaceVariant`). « Prêt » = permission accordée et modèle installé (déjà lus par
  `EcouteApp.etatPermission` et `modeleInstalle`).
- Contrastes : à **mesurer, pas à supposer**. ADR 0008 (l. 100) vise 11:1 pour `onError` et 7:1 pour
  `onPrimaryContainer` et `onSurfaceVariant` avec les jetons de `terrain-style` ; mais le rouge #B00020 mesuré dans
  l'inventaire de l'ADR donne 6,96:1 en texte sur `surface`, et blanc sur rouge 7,33:1. Un test calcule chaque couple
  (icône / fond du bouton, et fond du bouton / fond de l'écran) avec le test de contraste existant de `terrain`. Si
  `primaryContainer` sur le fond de l'écran passe sous 3:1 (contraste d'un élément graphique, WCAG 1.4.11),
  filet de 2 dp en `onSurface`, comme les cadres déjà en place (l. 492, 530).
- Le rouge ne porte pas seul le sens : l'icône barrée et la description TalkBack (« Micro »/« J'écoute ») restent.

## 5. « J'ÉCOUTE » sans déplacer le bouton (question 2 du plan)

Cause : le bloc est un frère au-dessus de la zone qui défile. Règle : **l'apparition du bloc ne change la taille ni
la position d'aucun autre élément.**

- Le micro, sa colonne ou son pied sont **hors du flux** que le bloc modifie (voir § 3) : ils ne bougent plus de toute
  façon, même si le reste bouge.
- Pour que le reste ne saute pas non plus, le bloc doit s'afficher **en surimpression** (opaque, cadre 2 dp comme
  aujourd'hui) à un endroit fixe, ou occuper une place réservée. Trois choix, à trancher (question 2) :
  - **a. Surimpression en haut** de la zone qui défile : zéro place perdue, rien ne bouge ; couvre un instant le numéro
    du plant pendant que l'on parle.
  - **b. Place réservée** (deux lignes, environ 72 dp) toujours vide hors dictée : rien ne bouge, mais 72 dp perdus
    couché, en permanence.
  - **c. Dans le pied/la colonne du micro**, à côté du bouton : ne couvre rien, mais le portrait a la place (pied
    large), pas le paysage (colonne de 128 dp : « J'ÉCOUTE » en grand n'y tient pas).
- Le bouton doit rester un **seul composable à la même identité** (acquis de la #121) ; `onDispose { relache() }` suit
  le bouton : en le sortant de `VuePlantEcran` pour l'attacher à la structure de l'écran, il ne doit pas se recomposer
  à chaque changement de vue sous le doigt (test sur la vue Plant seule).

## 6. Questions de sens pour Pascal (une par une, chacune avec ma proposition)

1. **Où est le micro couché ?** Proposition : colonne fixe à droite, micro en bas (128 dp de large). Autres : pied fixe
   comme debout (perd 128 dp de hauteur sur 384), ou micro flottant par-dessus (cache les boutons).
2. **Où s'affiche « J'ÉCOUTE » ?** Proposition : **a**, en surimpression en haut de la zone qui défile, car la
   hauteur couchée est rare et le résultat « entendu / compris » ne se lit qu'une seconde. Autres : b, c.
3. **112 dp pour de bon, couché aussi ?** Proposition : oui, comme Marculus, c'est la cible gantée ; si l'essai de
   Pascal dit « trop gros », on réduira à 96 dp, valeur à décider avec lui, pas par moi.
4. **Un micro barré se touche-t-il encore ?** Aujourd'hui l'appui sur le micro demande la permission (l. 362 à 366).
   Proposition : oui, barré mais actif ; l'appui déclenche la demande ou le message d'aide, et les boutons qui réparent
   (Réglages, Installer) restent sous la ligne de dictée. Autre : inerte, avec seulement la ligne d'état.
5. **Quelle place pour « Mode poche » ?** Proposition : à gauche du micro dans le pied debout ; couché, en tête de la
   zone qui défile, sans changer de libellé. Autre : le garder où il est (haut de la liste des actions).

## 7. Tests prévus (à écrire rouges avant le code, au code seulement)

- Fonction pure de disposition : la position du micro ne dépend pas de la présence du bloc d'écoute (sans Compose).
- `AspectMicro.pour` : les trois états, couleurs et icône (`MicOff`) selon permission et modèle ; l'ouverture compte
  comme écoute.
- Contraste des couples de couleurs du micro, avec le test de `terrain` rejoué dans Surculus.
- Limite dite dans la PR : sans test Compose instrumenté, « le bouton ne bouge pas sous le doigt » et « visible sans
  défiler, debout et couché » ne sont prouvés que sur la release, par capture.

## 8. Le test des dix secondes

**Qui** : Pascal, téléphone couché puis debout, gants, release installée, écran de contrôle d'une placette.
**Ce qu'il doit avoir compris après dix secondes** : « le micro est là, rond, en bas à droite, sans défiler » ; il le
tient, il devient rouge et « J'ÉCOUTE » s'affiche **sans que le micro ni les boutons bougent** ; il le lâche, ça se
ferme. Barré : il comprend que la dictée n'est pas prête sans lire de détail.
**Question de contrôle** : comprend-on la situation en dix secondes ?

## 9. Pièges écartés

Pas de quarante indicateurs, pas de couleur nouvelle, pas de rouge seul (icône et description), pas d'image qui
accuse (le barré dit « pas prêt », pas « faute »), pas d'état figé sans source (état lu en direct de `EcouteApp`).
