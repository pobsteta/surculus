# Mission 055 — Les fonds de carte de Marculus, chargés en ligne

> Fiche écrite par l'orchestrateur le 2026-10-27, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Demandée par Pascal le 2026-10-27 :
> « supprime aussi la phrase au dessus de la zone de recherche, comme dans marculus », puis
> « Comment sont gérées les orthos dans marculus ? Fais la même chose dans surculus », et
> « (b) exactement comme marculus ».
> **Lancement :** après la fusion de la 054 (même écran d'accueil) ; fusion autorisée par Pascal
> (« j'autorise tout ») si relecture prête et CI verte.
> **Élargie le 2026-10-27 par Pascal** : « Peux-tu comme dans marculus et la vue carte ajouter les couches dans un
> menu dans un bandeau supérieur avec aussi la flèche retour. Mettre les mêmes boutons que dans la vue carte de
> marculus (+, -, cible...) », puis « oui, aussi sur le guidage comme dans marculus ». La 055 devient « la carte de
> Marculus ».

## Mission

**En une phrase (quoi, et pourquoi) :** Faire comme Marculus : un menu « Fond de carte » sur la carte avec Plan IGN, Ortho IGN, OpenStreetMap et l'Ortho chantier (fichier de la tournée), les trois premiers chargés en ligne au fur et à mesure, et retirer le téléchargement à l'avance de l'ortho IGN, son état par tournée et la case « Ortho : Wi-Fi seulement », pour que Surculus montre et choisisse ses fonds comme Marculus.

**Redite par l'agent :** Je remplace le préchargement de l'ortho IGN par un menu « Fond de carte » comme celui de Marculus (Plan IGN, Ortho IGN, OpenStreetMap en ligne, Ortho chantier si le fichier de la tournée existe), je retire l'état de l'ortho par tournée et la case « Wi-Fi seulement », puis (élargissement) je donne à la carte le bandeau avec flèche de retour, le menu « Couches » et les boutons zoom / recentrer de Marculus, et au guidage le bandeau avec flèche : pour que Pascal change de fond et de couches d'un appui, sans rien préparer avant.

## Vérifié à la source

Constats vérifiés par l'orchestrateur le 2026-10-27.

| Constat | Lu à | Source du constat |
|---|---|---|
| Marculus : `enum Fond { PLAN_IGN("Plan IGN"), IGN("Ortho IGN"), OSM("OpenStreetMap"), ORTHO("Ortho chantier") }`, attributions IGN et OSM ; couches WMTS de la Géoplateforme (`SourceGeoplateforme`, URL construites par `terrain-carte`) ; menu de la carte avec la section « Fond de carte » | `marculus` `app/src/main/kotlin/io/github/pobsteta/marculus/ui/carte/CarteScreen.kt` l. 141, 185-209, 416-430, 655 ; `res/values/strings.xml` l. 194 (33a254a) | orchestrateur |
| Marculus : l'Ortho chantier vient du GeoPackage du contexte, servie hors ligne (`GpkgTileModule`, `getUsesDataConnection() = false`) ; aucun préchargement ni réglage Wi-Fi | `ui/carte/GpkgTileModule.kt` ; `data/.../GpkgRepository.kt` l. 192-238 | orchestrateur |
| Surculus : `OrthoIgn` (WMTS Géoplateforme) ; `TuilesHorsLigne` (tuiles préchargées dans le dossier de la tournée) ; `FichierOrtho` (`ortho.mbtiles` / `ortho.gpkg` lu en priorité) ; `ReglagesOrtho` (« Wi-Fi seulement », WorkManager) ; état de l'ortho par tournée (`EtatOrtho`) | `app/src/main/kotlin/io/github/pobsteta/surculus/carte/Ortho.kt` l. 13-14, 58, 89-179, 212, 313 ; `tournee/EcranTournees.kt` l. 107, 170 (`main` c95afcf) | orchestrateur |
| Décision de Pascal du 2026-10-21 : ortho IGN préchargée à l'ouverture de la tournée, état visible, emprise en France, attribution datée | `docs/adr/0009-carte-hors-ligne.md` l. 8-33 | orchestrateur |

Règles, avec leur auteur :
- **Exactement comme Marculus** : menu « Fond de carte » à quatre fonds, chargement en ligne au fur et à mesure, plus de téléchargement à l'avance ni de case « Wi-Fi seulement » : Pascal, 2026-10-27. Cette décision **remplace** l'option (d) de l'ADR 0009 ; l'orchestrateur ajoute un amendement daté à l'ADR dans la même PR.
- **Coût accepté par Pascal** (dit avant sa décision) : sans réseau, seules l'Ortho chantier et les zones déjà vues (cache d'osmdroid) s'affichent.
- **Ortho chantier** = le fichier tuilé du dossier de la tournée (`FichierOrtho`, déjà lu), proposé seulement quand il existe.
- **Fond par défaut** : celui de Marculus à l'ouverture de sa carte (l'agent le lit et le cite) ; le choix est gardé sur le téléphone si Marculus le garde.
- **Attributions** IGN et OSM affichées comme Marculus.
- **Garder** : l'emprise en France (test de la placette en Somalie, `CLAUDE.md`), le cache d'osmdroid.
- **Retirer** : préchargement (`TuilesHorsLigne`, travail WorkManager), `ReglagesOrtho`, la case de l'accueil, l'état de l'ortho dans la liste des tournées et ses chaînes ; l'agent dit ce qu'il fait des tuiles déjà préchargées sur les téléphones (les laisser lisibles, ou les effacer).
- **Aucune dépendance nouvelle sans le dire** : `terrain-carte` (utilisé par Marculus) n'est pas dans Surculus ; l'agent reprend les URL de la Géoplateforme avec le code existant (`OrthoIgn`) sauf s'il montre qu'il faut la dépendance.
- **Styles de Marculus** (042), cibles d'au moins 48 dp, bandeau (053).
- **Garde-fou = test vu rouge** et **bilan des écarts avant rendu** : Pascal, bilan du 2026-10-21.
- **Carte de Marculus (élargissement, Pascal, 2026-10-27)** : `BandeauCompact` en haut de la carte avec la flèche de retour (même action que le retour système) et, à droite, l'icône `Layers` ; le menu a deux sections (`TitreSection`) : « Fond de carte » (choix unique) puis « Couches affichées » (cases : Cadastre IGN, puis les couches déjà dessinées : forêt, UG, parcelles, placettes) ; la légende des placettes reste ; trois `SmallFloatingActionButton` (zoom avant, zoom arrière, recentrer : position GNSS si disponible, sinon emprise de la tournée), d'au moins 48 dp, aux libellés de Marculus ; le bouton « Retour » de la carte disparaît ; « Guider » et le reste restent. Marges de la carte posées par l'écran, comme les autres écrans à bandeau (053). Source : `marculus` `CarteScreen.kt` l. 141-143, 611-618, 634-670, 870-878 ; `strings.xml` l. 189-198.
- **Guidage (Pascal, 2026-10-27 : « oui, aussi sur le guidage comme dans marculus »)** : `BandeauCompact` avec la flèche de retour (même action que le retour système, qui ramène à la carte) ; titre = le texte actuel `guidage_vers`, qui quitte le corps ; plus de bouton « Retour » (états d'erreur compris : la flèche du bandeau) ; **pas** de menu « Couches » ni de zoom / recentrer (le guidage n'a pas de carte) ; distance (72 sp), cap, radar, messages et bip inchangés.
- **Barre d'état** : la carte et le guidage ayant un bandeau, tous les écrans en ont un ; les icônes de la barre d'état sont claires partout (`iconesBarreEtatClaires`, appel de `MainActivity` mis à jour, test et KDoc avec).

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : modèle Marculus | |
| A-t-il les moyens ? | oui ; `terrain-carte` à éviter sauf nécessité dite | |

**Temps de la main :** **appuyer**.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent. L'orchestrateur ne le remplit pas._

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** d'autres couches (cadastre, desserte) ; changer le guidage ; le thème (051) ; le menu « ⋮ » (054).

## Harnais

- **Peut :** modifier `carte/Ortho.kt`, `carte/EcranCarte.kt`, `carte/EcranGuidage.kt` (fond seulement), `tournee/EcranTournees.kt`, `tournee/DepotTournees.kt`, `core/.../carte/` (état de l'ortho), les chaînes, les tests.
- **Ne doit pas :** toucher à `main` ; modifier `fixtures/` ou `pilotage/journal.md` ; modifier l'ADR 0009 (l'orchestrateur l'amende) ; ajouter une dépendance sans le dire.
- **Sources autorisées :** cette fiche ; `CLAUDE.md` ; ADR 0009 ; `marculus` (`~/dev/`, en lecture).

## Tests

- Tests JVM : les quatre fonds et leurs libellés ; l'Ortho chantier proposée seulement si le fichier existe ; le fond par défaut ; l'emprise en France ; plus aucune référence au préchargement ni à « Wi-Fi seulement ». **Vu rouge** une fois.
- Commande du projet : `.claude/test-command`, au code 0.
- **Bilan des écarts** : ce qui change à l'écran, ce qui disparaît (liste des tournées, accueil), écarts avec Marculus.

## Terrain

- **Échantillon que Pascal vérifiera :** la release qui suit la fusion : l'accueil n'a plus la case « Wi-Fi seulement » ; sur la carte de Couchey, le menu « Fond de carte » propose Plan IGN, Ortho IGN, OpenStreetMap ; les tuiles arrivent avec le réseau ; en mode avion, les zones déjà vues restent.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal. Critère d'arrêt : 3 allers-retours. Point de suivi : à l'ouverture de la PR.

## Comment saura-t-on que ça marche ?

Pascal change de fond sur la carte comme dans Marculus, d'un appui, sans avoir rien préparé avant.
