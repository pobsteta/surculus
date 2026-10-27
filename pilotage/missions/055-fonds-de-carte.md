# Mission 055 — Les fonds de carte de Marculus, chargés en ligne

> Fiche écrite par l'orchestrateur le 2026-10-27, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Demandée par Pascal le 2026-10-27 :
> « supprime aussi la phrase au dessus de la zone de recherche, comme dans marculus », puis
> « Comment sont gérées les orthos dans marculus ? Fais la même chose dans surculus », et
> « (b) exactement comme marculus ».
> **Lancement :** après la fusion de la 054 (même écran d'accueil) ; fusion autorisée par Pascal
> (« j'autorise tout ») si relecture prête et CI verte.

## Mission

**En une phrase (quoi, et pourquoi) :** Faire comme Marculus : un menu « Fond de carte » sur la carte avec Plan IGN, Ortho IGN, OpenStreetMap et l'Ortho chantier (fichier de la tournée), les trois premiers chargés en ligne au fur et à mesure, et retirer le téléchargement à l'avance de l'ortho IGN, son état par tournée et la case « Ortho : Wi-Fi seulement », pour que Surculus montre et choisisse ses fonds comme Marculus.

**Redite par l'agent :** _à remplir par l'agent avant d'agir._

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
