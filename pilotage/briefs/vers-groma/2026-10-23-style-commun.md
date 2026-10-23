# Brief Groma — prendre les jetons du style commun

*Établi le 23 octobre 2026 depuis le dépôt `surculus`, à partir de Groma (`6948444`).
Va avec l'ADR 0008 de Surculus, acceptée par Pascal le 2026-10-23
(`docs/adr/0008-bibliotheques-communes.md`).*

## Pourquoi

Pascal a décidé que Marculus, Groma et Surculus prennent **les mêmes jetons** de
couleur (ADR 0008 de Surculus, question 1), tenus par le module Kotlin pur `terrain-style`
de la bibliothèque `terrain` (brief `vers-terrain` du 2026-10-23), avec un contraste du
texte selon WCAG 2.2, 1.4.6 (7:1, 4,5:1 pour le grand texte).

Constat : `android/app/src/main/kotlin/groma/app/ui/Theme.kt` l. 10-29 recopie les cinq
rôles de Marculus ; le texte blanc sur `VertVif` #3E7A40 (`primary`) donne 5,17:1, sous
les 7:1 que demande le texte des boutons.

## Demandé, une fois `terrain-style` publié

- Déclarer `terrain-style` dans le build de Groma (Groma ne déclare aujourd'hui aucun
  module de `terrain`).
- Remplacer les rôles de `Theme.kt` par les jetons de `terrain-style`, recopiés 1:1 dans
  le `ColorScheme`, avec un test qui vérifie l'égalité.
- Le mode sombre reste le choix de Groma ; s'il le garde, `terrain-style` fournit aussi
  les rôles sombres.

Le bouton principal change de vert : c'est le coût accepté par Pascal.

## Critère d'acceptation

Le `ColorScheme` clair de Groma est égal, rôle par rôle, aux jetons de `terrain-style`,
et un test le vérifie.
