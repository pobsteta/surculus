# Mission 063 — Plan (avant tout code)

> Rendu par l'`afficheur` le 2026-10-27, à partir de la fiche `063-plaque-blanche-suite.md` (branche
> `mission/063-plaque-blanche-suite`, partie de `main` 4f755f4). **Aucun code, aucune PR.** Pascal tranche les
> couleurs et les amendements ; rien n'est tranché ici.

## Redite

Remplacer, en thème sombre seulement, la plaque blanche `plaqueEtat()` et les teintes d'état illisibles (vert, orange,
rouge, noir sous 4,5:1 sur le fond sombre, pastilles vert foncé presque invisibles) par la solution de couleurs que
Pascal aura choisie sur le tableau ci-dessous, pour que le forestier lise l'état d'un coup d'œil sans îlot blanc
thème clair inchangé.

## Question de l'écran, pour qui

« Quel est l'état de mes placettes, d'un coup d'œil, dehors, en thème sombre ? » : le forestier en forêt, une main,
gants. Une seule chose à montrer par vue : la légende de la carte (« 1 travaux à prévoir »), le verdict du guidage, la
pastille de la liste. Comparaison : les trois états côte à côte (jamais / à prévoir / à jour).

## 1. Inventaire

Fonds réels (thème sombre, rôles Material 3 par défaut, BOM 2026.02.01) : `surface` = `background` #141218 (lu par
`ContrasteTest.kt` l. 41 comme `SchemaSombre.background`) ; cartes de la liste = `surfaceContainer` #211F26 (valeur M3
par défaut, **non relue dans le dépôt**, à vérifier au code) ; boutons de la carte = `surfaceContainerHigh` #2B2930.

### Usages de `plaqueEtat()` (tous sur Surface, sauf la légende, sur la carte)

| Où (`fichier:ligne`, dossier `app/src/main/kotlin/io/github/pobsteta/surculus/`) | Texte / dessin | Teinte | Fond réel dessous |
|---|---|---|---|
| `carte/EcranCarte.kt:512-518` | légende : « N jamais », « N travaux à prévoir », « N à jour », « N inconnu » (une seule plaque pour la ligne) | rouge, orange, vert, noir | plaque blanche sur `surface` (bande sous la carte, capture v064-carte : bandeau blanc) |
| `carte/EcranCarte.kt:521` | « N anomalies » | orange | `surface` |
| `carte/EcranCarte.kt:533` | placette sans position / hors France | rouge | `surface` |
| `carte/EcranGuidage.kt:95`, `:99` | refus de chargement | rouge | `surface` |
| `carte/EcranGuidage.kt:168`, `:170` | hors France / sans position | rouge | `surface` |
| `carte/EcranGuidage.kt:174-178` | distance 72 sp (noir, ou vert si arrivé) | noir / vert | `surface` |
| `carte/EcranGuidage.kt:181` | « arrivé » | vert | `surface` |
| `carte/EcranGuidage.kt:187` + `Radar` `:243-263` | **radar 260 dp** : cercle de 30 m, ligne, point agent (noir / vert), centre rouge | noir, vert, rouge | plaque blanche pleine largeur = le plus gros îlot |
| `carte/EcranGuidage.kt:223`, `:225` | rattachement par identité (orange) / rattaché (vert) | orange, vert | `surface` |
| `carte/EcranGuidage.kt:235`, `:237` | verdict refusé (rouge) / à confirmer (orange) | rouge, orange | `surface` |
| `tournee/EcranTournees.kt:171` | message de l'accueil | rouge si refus, sinon noir | `surface` |

### Teintes d'état SANS plaque, donc illisibles aujourd'hui en sombre (trouvaille, hors fiche)

`Palette.Rouge` (2,54:1 sur `surface`) en texte nu : `carte/EcranCarte.kt:124` (refus de chargement),
`:369`, `:372`, `:374` (emprise hors France, hors France, permission refusée) ; `tournee/EcranPlacettes.kt:80`
`tournee/VueKanban.kt:162` ; `tournee/EcranTournees.kt:352` (« illisible »). Le critère « aucun `plaqueEtat()` restant »
ne les attrape pas : la mission doit les inclure (question 1).
Autre copie de la palette, hors périmètre de la fiche : `essai/EcranEssaiGnss.kt:59-61` (écran d'essai GNSS).

### Pastilles

| Où | Forme | Contour | Constat en sombre |
|---|---|---|---|
| Carte, `carte/EcranCarte.kt:157-198` (`pastille()`) | disque plein (jamais, à prévoir), anneau (à jour), disque noir « ? » (inconnu) | **filet blanc de 3 dp** déjà peint | lisibles sur l'ortho (capture v064-carte), mais petites ; l'anneau vert cerné de blanc se voit |
| Liste Placettes, `tournee/EcranPlacettes.kt:171-183` (`PastilleEtat`) | mêmes formes, 28 dp | **aucun filet** | anneau « À jour » vert 0B6B2E sur 141218 = 2,79:1, **presque invisible** (capture v064-placettes) ; disque noir « inconnu » 1,13:1 : invisible ; grisées à 55 % : pire |
| Badge du fix, `carte/Commun.kt:64-73` | blanc sur teinte | — | **non touché** (fiche) : blanc/vert 6,65, blanc/orange 5,03, blanc/rouge 7,33 |

## 2. Tableau des couleurs d'état en sombre

Contrastes calculés (formule de `contraste()` de `Palette.kt` : (L haute + 0,05) / (L basse + 0,05), luminance WCAG
calcul refait en Python 3, non commité). Les valeurs « actuelles sur `surface` » retrouvent celles
de la spec (vert 2,8, orange 3,7, rouge 2,5, noir 1,1) : le calcul est le bon.

### Aujourd'hui (texte directement sur le fond)

| État | Couleur | sur `surface` #141218 | sur #211F26 | sur #2B2930 | sur la plaque blanche |
|---|---|---|---|---|---|
| vert | #0B6B2E | **2,79** | 2,45 | 2,16 | 6,65 |
| orange | #B45300 | **3,70** | 3,24 | 2,86 | 5,03 |
| rouge | #B00020 | **2,54** | 2,22 | 1,96 | 7,33 |
| noir | #000000 | **1,13** | 1,29 | 1,46 | 21 |
| « inconnu » | noir (pastille « ? ») | 1,13 | — | — | — |

Aucune teinte n'atteint 4,5:1 sur aucun fond sombre. Référence de texte neutre : `onSurface` #E6E0E9 sur `surface`
**14,35** ; blanc sur `surface` 18,59.

### Les options (trois au plus, comme demandé)

| | (a) Variantes claires générées par Material Color Utilities (ADR 0008) | (b) Texte neutre `onSurface` + pastille de couleur (+ liseré clair) | (c) Plaque **colorée** à texte blanc (modèle du badge du fix) |
|---|---|---|---|
| Principe | chaque état prend une **variante au ton 80** de sa teinte (ton 80 = rôle `primary`/`error` du schéma sombre de Material) | le texte reste `onSurface`, l'état est porté par une **pastille** de la couleur actuelle cernée d'un liseré blanc, comme sur la carte | le texte devient blanc sur un aplat de la **couleur actuelle** (comme `BadgeFix`) ; pour noir : texte `onSurface`, sans plaque |
| Vert / orange / rouge | **valeur non calculée** (voir ci-dessous). Seule certaine : le rouge de Material, `error` sombre = **#F2B8B5**, 10,89 sur `surface` (9,54 sur #211F26) | texte 14,35 ; pastille + liseré blanc : blanc/teinte 6,65 / 5,03 / 7,33 ; liseré blanc/`surface` 18,59 | blanc/teinte **6,65 / 5,03 / 7,33** (identiques au clair) |
| Noir | `onSurface` 14,35 | `onSurface` 14,35 | `onSurface` 14,35 |
| Contraste attendu | **par construction** : ton 80 (luminance 0,567) sur `surface` (ton 5,6) = **10,9:1**, 8,4 sur #2B2930 ; donc toujours ≥ 4,5, mais c'est une borne, pas une valeur | ≥ 4,5 partout | ≥ 4,5 partout ; mais la plaque ne se détache de `surface` qu'à 2,79 / 3,70 / 2,54 (1.4.11 demande 3:1 si le contour porte le sens ; ici le texte le porte) |
| Légende de la carte | texte coloré clair, sans plaque, sans pastille obligatoire | `● 1 travaux à prévoir` : pastille + texte `onSurface` ; la règle « légende en noir à côté de sa pastille » **tient**, avec `onSurface` au lieu de noir | trois plaques colorées côte à côte (rouge, orange, vert) : une idée, quatre couleurs : tient dans le 1664 |
| Pastilles (carte, liste) | la **liste** prendrait les variantes claires ; la carte gardant ses couleurs 033 + filet blanc : deux couleurs pour un même état entre deux écrans | **inchangées** (033 gardée) ; on ajoute le **liseré blanc de 2-3 dp à la liste Placettes** (comme la carte) | inchangées |
| Radar | tracé en variantes claires sur `surface`, plus de plaque | cercle et ligne en `onSurface`, centre et agent en pastille cernée de blanc ; plus de plaque | non applicable (un dessin n'est pas un texte) : il faudrait (a) ou (b) pour le radar |
| Changement de la spec | oui : la palette d'état a **deux jeux** (clair, sombre) ; « pastilles de la 033 inchangées » vaut pour le clair seulement | faible : « noir » devient « `onSurface` » en sombre ; ajout du liseré à la liste | oui : un aplat coloré remplace le texte coloré dans les messages |
| Risque au soleil (Pascal doit l'essayer sur la release) | le soleil lave les tons clairs sur noir : orange pâle et rouge rose (F2B8B5) risquent de se **ressembler** (état distingué par la teinte seule) | le plus sûr : l'état est redit par la forme du disque et par le mot ; le texte est le plus contrasté (14,35) | aplats saturés lisibles au soleil (comme le badge), mais **un aplat par message** : plus de couleur à l'écran ; le « inconnu » reste à part |
| Respect de l'ADR 0008 | oui s'il n'y a **aucune retouche** ; échec du test = Pascal choisit | oui (aucune couleur nouvelle) | oui (aucune couleur nouvelle) |

**Mes constats, sans trancher.** (b) est la seule qui ne crée aucune couleur nouvelle, garde la règle de la 033 et la
légende « texte neutre à côté de sa pastille », et recoupe ce que Marculus fait déjà (voir plus bas). (a) est la
suite logique de l'ADR 0008 mais ne se juge qu'une fois les valeurs générées. (c) ne résout pas le radar.
Une combinaison est possible : **(b) pour les textes, la légende et la liste ; pour le radar, `onSurface` + pastilles cernées**.

### Option (a) : je n'ai pas pu exécuter Material Color Utilities

Ni la bibliothèque (Java/Kotlin/JS/Python), ni un artefact ne sont présents sur cette machine (recherche dans
`~/.gradle`, `~/.m2`, pip) ; l'ADR 0008 rappelle d'ailleurs qu'il n'existe pas d'artefact Java officiel. Je ne donne
donc **aucune valeur** pour le vert et l'orange. Méthode, à jouer par le développeur (ou le script qui a généré les
jetons de `terrain-style`, mêmes réglages : `SPEC_2021`, niveau de contraste 0,5) :
1. Pour chaque état, partir de la teinte actuelle (#0B6B2E, #B45300, #B00020) comme **couleur personnalisée** de MCU
   (`CustomColor`/`Blend.harmonize` vers `VertMarculus` #374742 si Pascal veut l'harmonisation).
2. `Hct.from(argb)`, puis fixer le **ton à 80** en gardant teinte et chroma (`Hct.setTone(80)`), lire `toInt()`. Le rouge
   peut être pris tel quel dans le rôle `error` sombre (#F2B8B5, valeur publiée de Material 3).
3. Figer les trois valeurs dans `Palette.kt` avec le commit de MCU, la version de schéma, le niveau de contraste ; le test
   rejoue le contraste sur `surface` **et** `surfaceContainerHigh` ; **échec = pas de retouche, Pascal choisit**.
4. Remarque : la chroma d'un orange ou d'un vert au ton 80 peut être **rabotée** par la gamme sRGB ; teinte et
   chroma de sortie ne seront pas celles de la couleur de départ.

### Ce que fait Marculus (lecture seule, `~/dev/marculus`)

- Thème sombre = mêmes cinq rôles que Surculus, le reste par défaut : `ui/theme/Theme.kt:10-28`.
- Il **ne change pas** ses couleurs d'état en sombre : `ui/gnss/BadgeFix.kt:86-90` (vert #2E7D32, jaune-vert #9E9D24,
  bleu #1565C0, gris #616161, rouge #B71C1C) sont des **aplats à texte blanc**, identiques clair et sombre (modèle de l'option (c))
  `ui/gnss/Skyplot.kt:34-38` colore des points sans texte.
- Pour un **texte** d'erreur, il utilise le rôle du thème : `MaterialTheme.colorScheme.error` (`ui/feuille/DicteeVocale.kt:349`,
  `ui/contextes/ListeContextesScreen.kt:472`, `ui/contextes/CreationContexteScreen.kt:467`, `ui/feuille/FeuilleMartelageScreen.kt:561`),
  donc #F2B8B5 en sombre : c'est l'option (a) pour le rouge, déjà en production chez lui. Marculus n'a ni orange ni vert en texte.
- Il n'a pas de plaque blanche.

## 3. Amendement proposé (texte exact, à valider par Pascal ; il dépend de l'option choisie)

Le texte est écrit pour la **combinaison (b) + radar à pastilles** (ma proposition). Pour (a) ou (c), seule la phrase
« Couleurs d'état en sombre » change ; le reste tient.

**`docs/spec.md` § 3, puce « Affichage »** — remplacer la fin de la puce, de « couleurs des pastilles des placettes
(mission 033, PR #65) inchangées. **À revoir** … avant la fusion ». » par :

> couleurs des pastilles des placettes (mission 033, PR #65) inchangées **dans les deux thèmes** ; en thème sombre
> (amendé le 2026-10-XX, décision de Pascal, mission 063 ; *remplace* « à revoir avec la lisibilité en thème sombre »
> et la plaque blanche de la mission 051) : **aucun texte n'est écrit en couleur d'état** ; l'état se lit sur une
> pastille de sa couleur, cernée d'un liseré blanc, et le texte à côté est en `onSurface` (14,35:1) ; le noir de
> « légende en noir à côté de sa pastille » devient `onSurface`, **dans les deux thèmes** (noir 21:1 en clair, le
> clair n'en change pas) ; le radar est tracé en `onSurface` et en pastilles cernées de blanc ; le contraste de
> chaque texte et de chaque pastille (3:1 contre le fond, WCAG 1.4.11) est vérifié par un test, en clair et en sombre.
> **Essai au soleil** : par Pascal sur la **release installée après la fusion** (pas de variante fumée, règle du
> 2026-10-27), noté au journal ; un échec rouvre cette puce.

**`docs/adr/0008-bibliotheques-communes.md`** — ajouter à la fin de la section « Amendements » :

> ### Amendement du 2026-10-XX (mission 063, décision de Pascal)
>
> - **La palette d'état ne porte plus de texte en thème sombre** (la plaque blanche de la mission 051 est supprimée) :
>   le texte d'état est en `onSurface`, l'état est porté par la pastille et son liseré blanc. Aucune couleur nouvelle :
>   l'option E (Material Color Utilities) n'est **pas** appliquée à la palette d'état ; elle reste celle des rôles du
>   thème. *Précise* le paragraphe « La palette d'état … reste propre à Surculus » et la décision 6 (« légende en noir
>   à côté de sa pastille » : `onSurface` au lieu de noir). L'écart de l'option E sur l'orange (4,78:1 en clair) est
>   levé : l'orange ne porte plus de texte.

*(Si Pascal choisit (a) : « La palette d'état prend, en sombre seulement, des variantes au ton 80 générées par Material
Color Utilities, valeurs figées avec le commit, `SPEC_2021` et le niveau de contraste 0,5 ; le test juge ; échec :
Pascal choisit. » Si (c) : « En sombre, un message d'état est un aplat de la couleur d'état à texte blanc, comme le
badge du fix. »)*

## 4. Découpage

Une PR relisible, si le périmètre reste « textes + légende + radar + liseré de la liste » (13 appels
`plaqueEtat()` dans 3 fichiers, `Palette.kt`, `EcranPlacettes.kt`, un test). Je propose **deux PR** seulement si Pascal
retient (a), parce que la génération des valeurs (script, commit de MCU, figeage) est une étape à part :
- 063a : la méthode de génération et les trois valeurs figées avec leur test (échec = retour à Pascal)
- 063b : l'usage dans les écrans, la suppression de `plaqueEtat()`, le radar.
Les sept textes rouges sans plaque (question 1) font partie de la même PR dans tous les cas (même remède, mêmes tests).

## 5. Tests prévus (une fois le plan validé)

Contraste ≥ 4,5:1 de chaque texte et ≥ 3:1 de chaque pastille sur son fond réel, en clair et en sombre (fonds lus de
`SchemaClair` / `SchemaSombre`, y compris `surfaceContainer*`) ; un test qui échoue si `plaqueEtat` ou `Palette.Blanc`
comme plaque reste ; **vu rouge** une fois (retirer le liseré ou remettre une teinte sous le seuil, voir le test
échouer) ; `.claude/test-command` au code 0. Capture sur la release après fusion : accueil, Placettes, carte (légende,
pastilles), guidage (radar), clair inchangé (règle « capture après fusion d'écran »).

## 6. Questions pour Pascal (aucune tranchée), avec ma proposition

1. **Les sept textes rouges sans plaque** (inventaire) : les inclure dans la mission ? *Proposition : oui*, ils sont à 2,54:1 aujourd'hui.
2. **Quelle option de couleur** : (a), (b), (c) ou la combinaison ? *Proposition : (b) pour les textes, la légende et la liste ; radar en `onSurface` + pastilles cernées* (aucune couleur nouvelle, la 033 tient, Marculus fait pareil pour ses badges).
3. **Le noir de la légende** passe à `onSurface` dans les **deux** thèmes, ou seulement en sombre ? *Proposition : en sombre seulement*, pour que « rien ne change en clair » reste vrai, via le rôle `onSurface` qui vaut #1D1B20 (16,23:1) en clair.
4. **Le liseré blanc sur la liste Placettes** (anneau vert à 2,79:1 aujourd'hui) : l'ajouter dans les deux thèmes ou en sombre ? *Proposition : en sombre seulement.*
5. **Le disque noir « inconnu »** (1,13:1 sur `surface`, invisible) : liseré blanc (comme sur la carte), ou autre ? *Proposition : liseré blanc, même règle que les autres.*
6. **L'écran d'essai GNSS** (`essai/EcranEssaiGnss.kt:59-61`, copie de la palette) : dans la mission ou hors ? *Proposition : hors, écran de contrôle, tracer une mission à part si elle est utile.*
7. **L'essai au soleil** : sur la release après fusion, par vous ; ce qu'il doit montrer : les trois états de la légende distingués à bout de bras en plein jour. *Proposition : oui, avec un retour au journal, avant de refermer la puce de la spec.*
8. **Les amendements** de la spec § 3 et de l'ADR 0008 (texte ci-dessus) : à valider avec la date.
9. **Si (a)** : accepter que l'orange et le rouge clairs puissent se ressembler au soleil (rouge #F2B8B5 pâle) ? *Proposition : n'y aller que si l'option (b) ne convient pas.*

## Test des dix secondes (pour la PR de code, à faire par Pascal, sur la release)

Pascal ouvre la carte en sombre, téléphone à bout de bras, dix secondes. Il doit avoir compris : **« 1 placette à
prévoir, 2 à jour, 0 jamais passée »**, sans îlot blanc, en distinguant les trois pastilles. Puis l'écran Placettes :
l'anneau « À jour » se voit.

Question de contrôle : *comprend-on la situation en dix secondes ?*

## Pièges du plan

Pas d'image qui accuse (le rouge reste réservé à « jamais » et aux refus) ; pas d'axe tronqué (le tableau montre tous
les fonds) ; date et source : calculs du 2026-10-27, fonds #141218/#211F26/#2B2930 = valeurs par défaut de Material 3
(`surface` lu dans `ContrasteTest.kt` l. 41 ; les deux autres non relus dans le dépôt, à vérifier au code).
