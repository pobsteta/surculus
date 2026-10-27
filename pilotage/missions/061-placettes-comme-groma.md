# Mission 061 — Forêt, placettes à faire, puis carte : la navigation de Groma

> Fiche écrite par l'orchestrateur le 2026-10-27, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Demandée par Pascal le 2026-10-27 :
> « Dans Groma, si je sélectionne une forêt, j'arrive sur les placettes à faire. Dans surculus
> je veux retrouver la même chose, d'abord un affichage dont le titre est la forêt où la tournée
> est à faire, si je clique sur la forêt, j'arrive sur les placettes à faire, pas sur la carte,
> quand je clique sur une placette, là la carte s'affiche, où bien quand je clique sur le bouton
> cible comme dans groma ça affiche la carte, avec tous les boutons, naviguer vers, +, -, maison
> et cible. » Puis « 1 oui, 2 oui » aux deux questions de l'orchestrateur, et « Les boutons dans
> surculus sous la carte "Ma position" et "Placettes" ne seront plus nécessaires ».
> **Lancement :** après la fusion de la 060 (même écran de carte).

## Mission

**En une phrase (quoi, et pourquoi) :** Reprendre l'enchaînement de Groma : l'accueil montre chaque tournée sous le nom de sa forêt, un appui ouvre l'écran « Placettes » (bandeau avec retour, forêt en titre, icône carte ; placettes à faire d'abord), un appui sur une placette ou sur l'icône carte ouvre la carte avec les boutons naviguer vers, +, −, maison et cible, pour que le forestier choisisse sa placette avant la carte, comme dans Groma.

**Redite par l'agent :** La mission ajoute entre l'accueil et la carte un écran « Placettes » (forêt en titre, placettes à faire d'abord, appui sur une placette ou icône carte) et dote la carte des boutons naviguer vers, +, −, maison et cible, pour que le forestier choisisse sa placette avant la carte, comme dans Groma.

## Vérifié à la source

| Constat | Lu à | Source du constat |
|---|---|---|
| Groma : `EcranPlacettes` : `TopAppBar` avec flèche de retour (`ArrowBack`), icône carte (`Icons.Filled.Place`, `onCarte`), menu ⋮ ; ligne « %1$d placettes · %2$d à faire » ; liste en `ElevatedCard`, un appui ouvre la placette ; état `a_faire` / `provisoire` / `envoye` / `definitif` | `groma` `android/app/src/main/kotlin/groma/app/ui/EcranPlacettes.kt` l. 60-133 ; `res/values/strings.xml` l. 26 (0458303) | orchestrateur |
| Groma : carte avec bandeau (retour, menu des couches) et boutons `SmallFloatingActionButton` : prochaine placette (« Aller à la prochaine placette à faire »), +, −, `Icons.Filled.Home` (« Cadrer le projet »), `Icons.Filled.LocationOn` (« Suivi de la position ») | `groma` `ui/EcranCarte.kt` l. 323-389 ; `strings.xml` l. 103-111 | orchestrateur |
| Surculus : un appui sur une tournée ouvre directement la carte ; la carte a en bas « Ma position » (retiré par la 057) et « Placettes » ; l'accueil titre la tournée par `projet.foret ?: projet.nom`, vides dans la tournée de sommieR, d'où « tournee-AAAAMMJJ-HHMMSS » | `tournee/DepotTournees.kt` l. 30 ; capture du téléphone du 2026-10-27 | orchestrateur |

Règles, avec leur auteur (Pascal, 2026-10-27) :
- **Accueil** : titre de chaque tournée = **nom de la forêt**, lu dans la couche `foret` de la tournée quand `projet.foret` est vide ; le nom du dossier seulement en dernier recours.
- **Écran « Placettes »** (nouveau) : bandeau `BandeauCompact` (053) avec flèche de retour vers l'accueil, la forêt en titre, une icône carte ; sous le bandeau « n placettes · k à faire » ; liste des placettes, **à faire d'abord** (contrôle prévu dans cette tournée et pas encore saisi), puis les autres grisées en bas ; pastille d'état comme sur la carte (033).
- **Un appui sur une placette** ouvre la carte centrée sur elle ; **l'icône carte** ouvre la carte cadrée sur toute la tournée.
- **Carte** : bandeau (retour vers « Placettes », menu « Couches » de la 055) ; boutons, comme Groma : **naviguer vers** (lance le guidage vers la placette choisie, sinon vers la **prochaine placette à faire**), **+**, **−**, **maison** (cadre toute la tournée), **cible** (recentre sur la position, jamais hors de France : `Recentrage.cible`, 055). Les boutons du bas « Ma position » et « **Placettes** » disparaissent.
- **Guidage** : son retour ramène à la carte (inchangé).
- **Styles de Marculus** (042), cibles d'au moins 48 dp, `BandeauCompact` ; la légende des placettes reste sur la carte.
- **Garde-fou = test vu rouge** et **bilan des écarts avant rendu** : Pascal, bilan du 2026-10-21.

**Temps de la main :** **appuyer**. Fusion autorisée si relecture prête et CI verte (« j'autorise tout ») ; vérification par capture du téléphone sur la release, puis Pascal.

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** l'écran de contrôle de placette (012) ; le parcours ordonné de Groma (« Placette n sur m ») ; changer le guidage.

## Harnais

- **Peut :** ajouter `tournee/EcranPlacettes.kt` (ou `placettes/`), la destination dans `Navigation.kt` (Tournées → Placettes → Carte → Guidage, retours), modifier `carte/EcranCarte.kt`, `tournee/EcranTournees.kt`, `tournee/DepotTournees.kt` (nom de forêt), lire la couche `foret` par le module `gpkg`, les chaînes, les tests.
- **Ne doit pas :** toucher à `main` ; modifier `fixtures/`, `docs/` ou `pilotage/journal.md` ; ajouter une dépendance.
- **Sources autorisées :** cette fiche ; `CLAUDE.md` ; `groma` et `marculus` (`~/dev/`, en lecture).

## Tests

- `NavigationTest` : Tournées → Placettes → Carte (depuis une placette et depuis l'icône) → Guidage, et les retours dans l'ordre inverse ; rotation.
- JVM : tri des placettes (à faire d'abord) et compte « n · k à faire » ; titre de forêt (projet, sinon couche `foret`, sinon dossier) ; « naviguer vers » sans placette → prochaine à faire, aucune à faire → bouton inactif ; recentrage jamais hors de France.
- **Vu rouge** une fois. `.claude/test-command` au code 0.

## Terrain

- **Échantillon :** la release qui suit la fusion, tournée `couchey-demo-avec-ortho.zip` : l'accueil dit « Forêt communale de Couchey » ; un appui ouvre la liste des 3 placettes ; un appui sur P35-01 ouvre la carte centrée dessus ; maison cadre la tournée, naviguer vers lance le guidage.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. Critère d'arrêt : 3 allers-retours.

## Comment saura-t-on que ça marche ?

Pascal ouvre Surculus comme Groma : la forêt, puis ses placettes à faire, puis la carte d'un appui.
