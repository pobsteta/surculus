> Statut (archiviste Marculus, 2026-10-24) : § 1 traité le 2026-10-24 (PR marculus #8) ; § 2 en attente de `terrain-style`.

# Brief Marculus — qui utilise `terrain`, et le style commun

*Établi le 22 octobre 2026 depuis le dépôt `surculus`, à partir de Marculus (`87c3cf4`),
Groma et `terrain` (v0.5.0, `ee98731`). Va avec l'ADR 0008 de Surculus (acceptée le 2026-10-23,
`docs/adr/0008-bibliotheques-communes.md`).*

## 1. Corriger le README

`README.md` de Marculus, l. 106-115, dit la bibliothèque `terrain` « partagée avec Groma et
Surculus ». C'est faux à ce jour :

- **Groma** ne déclare `terrain` dans aucun de ses fichiers de build ;
- **Surculus** recopie provisoirement `groma-core` (`docs/dependances.md` de Surculus) et
  ne déclare pas `terrain`.

Demandé : écrire « utilisée par Marculus ; Groma et Surculus doivent l'adopter » (ou la
forme que choisit Marculus), et la tenir à jour quand l'une des deux l'adopte.

## 2. Prendre les jetons du style commun

Pascal a décidé le 2026-10-22 que Surculus prend son style commun avec Marculus et Groma
par `terrain`, sans copie de `Theme.kt`, et qu'il **ne passe jamais devant la lisibilité au
soleil**. Le 2026-10-23, il a accepté l'ADR 0008 de Surculus : les trois applications
prennent **les mêmes jetons** (question 1), tenus par le module Kotlin pur `terrain-style`
(brief `vers-terrain` du 2026-10-23), avec un contraste du texte selon WCAG 2.2, 1.4.6
(7:1, 4,5:1 pour le grand texte).

Constat qui concerne Marculus : le texte blanc sur `VertVif` #3E7A40 (couleur `primary` de
`ui/theme/Theme.kt` l. 11 et 15) donne 5,17:1, sous les 7:1 que demande le texte des
boutons (18-20 sp). Blanc sur `VertMarculus` donne 9,80:1.

Demandé, **une fois `terrain-style` publié** : remplacer les cinq rôles de `Theme.kt` par
les jetons de `terrain-style`, recopiés 1:1 dans le `ColorScheme`, avec un test qui
vérifie l'égalité. Le bouton principal change de vert : c'est le coût accepté par Pascal.
Le mode sombre de Marculus reste son choix ; s'il le garde, `terrain-style` fournit aussi
les rôles sombres.

## Critère d'acceptation

- Le README de Marculus ne nomme comme utilisateurs de `terrain` que les applications qui
  la déclarent dans leur build.
- Le `ColorScheme` clair de Marculus est égal, rôle par rôle, aux jetons de
  `terrain-style`, et un test le vérifie.
