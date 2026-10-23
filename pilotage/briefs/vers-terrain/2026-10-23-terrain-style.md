# Brief `terrain` — module `terrain-style`, jetons du style commun

*Établi le 23 octobre 2026 depuis le dépôt `surculus`, à partir de `terrain` v0.5.0
(`ee98731`). Va avec l'ADR 0008 de Surculus, acceptée par Pascal le 2026-10-23
(`docs/adr/0008-bibliotheques-communes.md`).*

## Pourquoi

Pascal veut un style commun pour Marculus, Groma et Surculus (cohérence du projet
nemeton), qui ne passe jamais devant la lisibilité au soleil, et qui vient de `terrain`,
pas d'une copie de `Theme.kt`. Aujourd'hui, Marculus et Groma ne fixent que cinq rôles
Material 3 ; le reste est le défaut de Material 3, et le texte blanc sur `VertVif`
#3E7A40 ne donne que 5,17:1.

## Demandé

Un module **Kotlin pur, JVM 17**, `terrain-style`, sans Android ni Compose (règle de
`terrain/README.md` l. 20) :

1. **Tous les rôles** du schéma de couleurs Material 3, en ARGB (`Long`), en thème clair ;
   en sombre aussi si Marculus ou Groma garde son mode sombre.
2. **Valeurs générées une fois** par Material Color Utilities (Google, Apache-2.0) depuis
   la couleur de départ `VertMarculus` #374742, au **niveau de contraste 0,5**, schéma
   **`SPEC_2021`**, puis **figées** dans le code. Pas de dépendance à l'exécution. Sont
   inscrits dans le module : le script de génération, le commit de Material Color
   Utilities, la version du schéma, le niveau de contraste et la couleur de départ.
3. **Un test de contraste** (WCAG 2.2, luminance relative) sur **chaque couple** rôle
   « on » / rôle de fond, et sur `primary`, `secondary`, `tertiary`, `error` employés en
   texte sur `surface`, `surfaceContainer*` et `surfaceDim`. Seuil : **7:1**. La règle
   décidée par Pascal est WCAG 2.2, 1.4.6 ; appliquer 7:1 à tous les jetons, grand texte
   compris, est une **déduction de l'orchestrateur** : le jeton ne sait pas à quelle taille
   il servira. Le test est juge : si une valeur générée échoue, elle ne
   passe pas.
4. **Une fonction publique de contraste** (rapport entre deux ARGB), pour que chaque
   application rejoue le test sur sa propre palette d'état.
5. Un tag publié, construit par JitPack. Surculus l'épinglera par SHA de commit, au moins
   7 jours après sa publication (ADR 0008, lettre A).

## Hors périmètre

- Le `MaterialTheme` Compose : chaque application recopie les rôles dans son
  `ColorScheme` (option A écartée).
- La palette d'état de Surculus (pastilles, badge, alertes) : elle reste dans Surculus.
- Les autres briques (GNSS, journal, GeoPackage…) : ADR 0012 de Surculus, réservée.

## Ouvert

- **Le successeur de `terrain`** : à désigner par Pascal (ADR 0008, question 3).
- JitPack construit sans les tests (`jitpack.yml`, `-x test`) ; la CI de `terrain`
  (`.github/workflows/ci.yml`) les joue déjà à chaque push et chaque PR. Surculus rejoue
  quand même le test de contraste sur les jetons reçus.
- **À vérifier** : dans `SPEC_2021`, la cible de 7:1 de `primary`, `secondary` et
  `tertiary` se mesure contre une seule surface ; contre `surfaceDim` ou
  `surfaceContainerHighest`, ils peuvent tomber sous 7:1.
- **Si une valeur générée échoue au test** : ne pas la retoucher à la main. Rendre la liste
  des couples en échec ; Pascal choisit l'issue (autre niveau de contraste de Material
  Color Utilities, ou moins de couples testés).

## Critère d'acceptation

`./gradlew :terrain-style:test` passe ; tous les couples listés au point 3 tiennent 7:1 ;
le script de génération, rejoué avec les réglages inscrits, redonne les mêmes valeurs.
