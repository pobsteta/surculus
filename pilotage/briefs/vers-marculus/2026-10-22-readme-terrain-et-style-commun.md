# Brief Marculus — qui utilise `terrain`, et le style commun

*Établi le 22 octobre 2026 depuis le dépôt `surculus`, à partir de Marculus (`87c3cf4`),
Groma et `terrain` (v0.5.0, `ee98731`). Va avec l'ADR 0008 de Surculus (proposée,
`docs/adr/0008-bibliotheques-communes.md`).*

## 1. Corriger le README

`README.md` de Marculus, l. 106-115, dit la bibliothèque `terrain` « partagée avec Groma et
Surculus ». C'est faux à ce jour :

- **Groma** ne déclare `terrain` dans aucun de ses fichiers de build ;
- **Surculus** recopie provisoirement `groma-core` (`docs/dependances.md` de Surculus) et
  ne déclare pas `terrain`.

Demandé : écrire « utilisée par Marculus ; Groma et Surculus doivent l'adopter » (ou la
forme que choisit Marculus), et la tenir à jour quand l'une des deux l'adopte.

## 2. Pour information : le style commun

Pascal a décidé le 2026-10-22 que Surculus prend le style de Marculus et Groma par `terrain`,
sans copier `Theme.kt`, et que ce style **ne passe jamais devant la lisibilité au soleil** :
thème clair forcé, contraste du texte d'au moins 7:1 (WCAG 2.2, critère 1.4.6).

Constat qui concerne Marculus : le texte blanc sur `VertVif` #3E7A40 (couleur `primary` de
`ui/theme/Theme.kt` l. 11) donne 5,17:1, sous les 7:1 ; `VertVif` sur crème donne 4,37:1.
Blanc sur `VertMarculus` donne 9,80:1. Marculus n'est pas tenu par la règle de Surculus ;
c'est à Pascal de dire si la règle vaut pour les trois applications.

Rien n'est demandé à Marculus sur ce point tant que l'ADR 0008 n'est pas tranchée.

## Critère d'acceptation

Le README de Marculus ne nomme comme utilisateurs de `terrain` que les applications qui la
déclarent dans leur build.
