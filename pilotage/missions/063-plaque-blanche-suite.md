# Mission 063 — En thème sombre, lire les couleurs d'état sans plaque blanche

> Fiche écrite par l'orchestrateur le 2026-10-27, à partir du gabarit `fiche-mission.md` du
> skill `imperatrice:mission`. Demandée par Pascal le 2026-10-27 : « lance la suite de la plaque
> blanche ». Suite de la 062 (textes neutres, fusionnée en 4f755f4, vérifiée sur la v0.7.64) et
> de la plaque blanche de la 051 (« 1 on verra plus tard », 2026-10-27).
> **Lancement :** le 2026-10-27, depuis `main` (4f755f4).

## Mission

**En une phrase (quoi, et pourquoi) :** En thème sombre, remplacer la plaque blanche
(`plaqueEtat()`) qui reste sous les textes et dessins en couleur d'état (légende de la carte,
anomalies, messages et verdicts du guidage, radar, message de l'accueil) et rendre lisibles les
pastilles vert foncé sur fond noir, par des couleurs d'état propres au thème sombre que Pascal
aura choisies sur un tableau de contrastes, pour que le forestier lise l'état d'un coup d'œil
dans les deux thèmes sans îlot blanc sur fond noir.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Vérifié à la source

| Constat | Lu à | Source |
|---|---|---|
| `plaqueEtat()` : plaque blanche en sombre seulement ; teintes d'état sous 4,5:1 sur le fond sombre | `carte/Palette.kt` l. 34-42 (`main` 4f755f4) | orchestrateur |
| Usages restants | `EcranCarte.kt` l. 512 (légende), 521 (anomalies), 533 (message rouge) ; `EcranGuidage.kt` l. 95-99, 168-187 (verdicts, « arrivé », radar de 260 dp), 223-237 ; `EcranTournees.kt` l. 171 | orchestrateur, `grep plaqueEtat` |
| Pastilles vert foncé presque invisibles sur fond noir (anneaux « À jour ») | capture `scratchpad/captures/v064-placettes.png` et `v064-carte.png`, v0.7.64 | orchestrateur |
| « six textes oubliés » et « aplat du radar » | relecture de la PR #95, journal l. 462 et 486 | relecteur |

Règles et décisions en vigueur (à respecter, ou à faire changer par Pascal) :
- **Contraste minimal 4,5:1** sur le fond réel, vérifié par un test (spec § 3, Pascal « 1 oui », 2026-10-27).
- **ADR 0008** : couleurs générées par Material Color Utilities depuis `VertMarculus` ; si une
  couleur générée échoue au test, **pas de retouche à la main, Pascal choisit l'issue** (2026-10-23).
- **Pastilles de la 033 inchangées** et **« légende en noir à côté de sa pastille »** (spec § 3,
  ADR 0008) : notées « à revoir avec la lisibilité en thème sombre » (2026-10-27). Toute couleur
  nouvelle en sombre change ces deux règles : **amendement de la spec à faire valider par Pascal**.
- **Règle du 1664** : quatre couleurs au plus.
- **En thème clair, rien ne change.**
- **« Essai au soleil par Pascal avant la fusion »** : « à revoir » dans la spec ; l'agent dit dans
  son plan comment il propose de le traiter (plus de variante fumée : essai sur la release, règle
  du 2026-10-27).

**Temps de la main :** **appuyer**. **Plan d'abord, sans code** : l'agent rend à l'orchestrateur
le plan ci-dessous ; Pascal tranche les couleurs et les amendements ; puis le code.

## Le plan attendu (avant tout code)

1. **Inventaire** de chaque usage de `plaqueEtat()` et de chaque pastille, avec `fichier:ligne`,
   le texte affiché et le fond réel dessous (Surface, carte, ortho).
2. **Tableau des couleurs d'état en sombre** : pour vert, orange, rouge, noir (et « inconnu ») —
   la couleur actuelle, son contraste sur le fond sombre réel, puis **deux ou trois options** au
   plus avec leur contraste calculé, dont au moins :
   - (a) variantes claires générées par Material Color Utilities (ADR 0008), sans retouche ;
   - (b) texte neutre (`onSurface`) à côté d'une pastille de couleur, sans plaque ;
   - et, pour les pastilles sur la carte, un liseré clair.
   Pour chaque option : ce que ça change à la légende, aux pastilles, au radar ; risque au soleil.
3. **Amendement proposé de la spec § 3 et de l'ADR 0008** (texte exact), à soumettre à Pascal.
4. **Découpage** si la mission dépasse une PR relisible.
5. **Questions pour Pascal**, sans en trancher aucune.

## L'Homme

- **Agent :** `afficheur` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** les textes neutres (062, faits) ; l'écran du contrôle (012) ;
  le thème lui-même (couleurs Material de Marculus, 038, 051) ; le badge du fix (blanc sur teinte).

## Harnais

- **Peut (après le plan validé) :** `carte/Palette.kt`, les écrans qui appellent `plaqueEtat()`,
  les tests ; proposer le texte d'amendement de `docs/spec.md` et de l'ADR 0008 (dans la PR, à
  valider par Pascal).
- **Ne doit pas :** coder avant la validation du plan ; retoucher à la main une couleur générée ;
  changer le thème clair ; toucher à `main` ; modifier `fixtures/` ou `pilotage/journal.md`
  autrement qu'en ajout ; trancher une question de sens.
- **Sources autorisées :** spec § 3 ; ADR 0008 ; fiches 033, 038, 051, 062 ; Marculus en lecture
  seule (comment il traite ses couleurs d'état en sombre).

## Tests

- Contraste ≥ 4,5:1 de chaque texte et pastille d'état sur son fond réel, **en clair et en
  sombre** ; aucun `plaqueEtat()` restant si l'option retenue le supprime ; **vu rouge** une fois.
  `.claude/test-command` au code 0.

## Terrain

- **Échantillon :** la release qui suit la fusion, téléphone en sombre : captures de l'accueil, de
  l'écran Placettes, de la carte (légende, pastilles) et du guidage (radar) ; en clair, inchangé.
  Essai au soleil selon la décision de Pascal.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. Critère d'arrêt : 3 allers-retours.
- **Qui tranche :** Pascal (couleurs, amendements). **Fusion : autorisation à demander à Pascal.**

## Comment saura-t-on que ça marche ?

En sombre, plus aucun îlot blanc ; Pascal lit sur la carte « 1 travaux à prévoir » et distingue
les trois pastilles d'un coup d'œil.

## Décisions de Pascal sur le plan (2026-10-27, questions posées une par une)

1. **Option (b)** : en sombre, aucun texte en couleur d'état ; texte en `onSurface`, l'état porté par
   la pastille cernée d'un filet blanc ; radar en `onSurface` et pastilles cernées.
2. **Les 7 textes rouges sans plaque** (`EcranCarte.kt:124,369,372,374`, `EcranPlacettes.kt:80`,
   `VueKanban.kt:162`, `EcranTournees.kt:352`) entrent dans la 063.
3. **Messages d'erreur et de refus** en sombre : `colorScheme.error` (couleur du thème, comme Marculus).
4. **Légende** : `onSurface` en sombre seulement ; en clair, elle reste en noir.
5. **Filet blanc des pastilles de la liste Placettes** : en sombre seulement (disque noir « inconnu » compris).
6. **L'écran d'essai GNSS** entre dans la 063 : il perd sa copie de la palette.
7. **Essai au soleil** : par Pascal sur la release après la fusion, noté au journal ; un échec rouvre la mission.
8. **Amendement validé** (spec § 3 et ADR 0008, daté du 2026-10-27) : thème clair inchangé ; en
   sombre, texte d'état en `onSurface`, pastilles cernées de blanc (carte, liste, radar, essai GNSS),
   erreurs en `colorScheme.error`, plus de plaque blanche ; test de contraste en clair et en sombre
   (texte ≥ 4,5:1, pastille ≥ 3:1, WCAG 1.4.11) ; aucune couleur d'état nouvelle, l'option E ne
   s'applique pas à la palette d'état.

Une seule PR. Fusion : autorisation à demander à Pascal.
