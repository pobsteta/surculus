# Mission 087 — Ouvrir un contrôle sans position

> Fiche écrite par l'orchestrateur le 2026-11-02, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> après la fusion de la 085 (#150, ADR 0003 amendée, qui fait foi) et de la 086 (#151, 709c7bd : homonymes entre UG),
> et le découpage décidé par Pascal le 2026-11-02 (086 → 087 → 088). Depuis `main`. Une mission = une phrase = une
> branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Permettre d'ouvrir un contrôle quand le téléphone ne donne aucune position au
pied du piquet, sur le seul code lu et confirmé, à la position de la placette créée par sommieR, et l'écrire au journal
(format v2) et à l'export comme « sans position mesurée », pour que le forestier puisse travailler sous couvert dense
sans que la donnée mente.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | à moitié : la règle est écrite (ADR 0003, amendement du 2026-11-02, points 1 à 10 ; spec § 4.3, § 5, § 6 ; `docs/formats/journal-valeurs-v2.md`), la façon de la coder touche plusieurs couches | **tenir** : le `dev` rend un plan d'abord |
| A-t-il les moyens ? | oui | le `testeur` écrit d'abord les tests |

**Temps de la main :** tenir : la mission touche la justesse des données (journal, export, CI d'import).

## Ma réponse d'abord

_Rempli par Pascal seul._

## L'Homme

- **Agents :** `testeur` d'abord (tests depuis l'ADR, la spec, les formats et cette fiche, **sans lire** le code de
  l'arrivée, de la session, du journal et de l'export), puis `dev`, qui **rend un plan** (court, fichiers et étapes)
  avant de coder ; l'orchestrateur le présente à Pascal.
- **Modèle :** opus (justesse des données, formats).
- **Ce qui ne lui est pas demandé :** la `.sursync` v2 (088) ; la parcelle (bac à sable) ; les homonymes (fait en 086).

## La règle (ADR 0003, amendement du 2026-11-02 : l'ADR fait foi)

Rappel, sans remplacer l'ADR :
- sans **aucun fix** pendant **30 s** (provisoire), récepteur actif, localisation et permission accordées : Surculus
  propose « Ouvrir sans position » ; localisation coupée ou permission refusée : il demande de l'activer ;
- **code lu obligatoire**, connu de la tournée ; homonymes : règle de la 086 (sans position, rien ne tranche : choix de
  l'UG) ;
- **confirmation** avant d'ouvrir : « code · UG · nombre de plants pointés : ouvrir sans position ? » (**sans
  parcelle** : décision de Pascal du 2026-11-02, la tournée ne la porte pas) ;
- position du contrôle = **piquet de sommieR** ; pas de position mesurée ; un fix reçu ensuite est **ignoré** ;
- un fix reçu, même unique, médiocre ou simulé, reste une position (règle actuelle) ; **hors de France : refusé**, phrase
  qui dit quoi faire, refus au **journal technique** ;
- **journal des saisies v2** (`FormatValeurs.VERSION` = 2, lecteur 1 et 2), positions mesurées `null` avec
  `sans_position_mesuree` vrai ; chaque valeur ignorée à la lecture tracée au journal technique ;
- **export** : `positions_controles` une ligne, géométrie **NULL**, `precision_m`, `source_gnss`, `referentiel_source`,
  `simulee` NULL, `sans_position_mesuree` et `rattache_par_identite` vrais ; `controles` à la position du piquet ;
- installation d'une placette : toujours une position.

**P35-02 de Couchey** (décision de Pascal du 2026-11-02, « en faire le cas sans position ») : P35-02 devient un contrôle
ouvert sans position sur son code ; l'export lui écrit une ligne `positions_controles` à géométrie NULL ; F4 vérifie
cette ligne et la position du piquet dans `controles`, et la CI d'import prouve la règle de bout en bout. ADR 0007 (texte
F4, l. 52) et `ci/sommier/README.md` (l. 54-66) à amender en conséquence : **par l'orchestrateur ou l'`archiviste`**, pas
par le `dev` (qui le signale dans la PR).

## Harnais

- **Testeur peut :** tests dans `core/`, `gpkg/`, `app/src/test/` ; fixtures (`fixtures/regles/rattachement-15m.json`,
  `fixtures/acceptation-export/*`, `fixtures/export/scenario-couchey.json`, `fixtures/export/registre-attendu-couchey.json`)
  pour les cas de la règle, sourcés « ADR 0003, amendement du 2026-11-02 » ; tests de la CI d'import (`ci/sommier/`)
  pour F4 et un sabotage F5 (ligne sans position retirée ou géométrie remplie) ; vus rouges sur `main`.
- **Dev peut :** tout le code listé dans la fiche 085, section « Pour la mission 086 : ce qu'elle devra toucher » (hors
  sauvegarde `.sursync`, homonymes déjà faits) : `Controle.kt` (`Rattache.positionMesuree` nullable), `Valeurs.kt`,
  `SessionControle.kt`, `ArriveeControle.kt`, `MoyenneArrivee.kt`, `EcranGuidage.kt`, `ContexteDeTournee.kt`,
  `Couches.kt`, `Saisies.kt`, `ExportTravaux.kt`, `ValidationExport.kt`, `TextesEchec.kt`, ressources ; `ci/sommier/*.R`
  pour lire la géométrie NULL. Il ne réécrit pas les tests du testeur.
- **Ne doivent pas :** toucher à `docs/` (signaler) ; à la `.sursync` ; inventer une valeur (30 s reste provisoire) ;
  toucher à `main`.
- **Sources autorisées :** ADR 0003 et 0007, spec, `docs/formats/journal-valeurs-v1.md` et `-v2.md`, fiche 085, sommieR
  (`~/dev/sommieR`, lecture, tag v0.31.1).

## Valeur provisoire (chiffre, réglage)

- **Valeur provisoire :** 30 s sans aucun fix avant « Ouvrir sans position ».
- **Mesurée par :** Pascal, à l'essai en forêt sous couvert (premier fix à froid), date à fixer.
- **Gel :** avant la publication de la tranche 1 ; la constante est nommée et marquée provisoire dans le code.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** 085 (#150) et 086 (#151, 709c7bd), fusionnées.
- **Ordre de fusion :** 087 → 088. **La 087 ne part sur les téléphones qu'avec la 088** (un Surculus ancien perdrait
  sans le dire une sauvegarde qui contient un contrôle sans position) : à rappeler dans la PR.
- **PR en brouillon jusqu'à :** la fin du travail du `dev`.
- **CI rouge attendue :** les tests du `testeur` (dont F4 dans « Import sommieR »), entre son push et celui du `dev`.

## Tests

Les 10 tests d'acceptation de la fiche 085 (sauf le 3, fait en 086 pour le cas avec position ; le cas sans position
avec homonymes est à couvrir ici), plus : fix reçu après une ouverture sans position ignoré ; refus hors de France au
journal technique, absent du journal des saisies ; `"v":3` ignoré et tracé ; P35-02 de Couchey en contrôle sans
position, F4 vert, sabotage F5 rouge. `.claude/test-command` au code 0.

## Terrain

- **Échantillon que Pascal vérifiera :** sur la release (avec la 088), localisation active mais aucun fix (téléphone
  sous un abri ou en intérieur profond), un code lu : la proposition après 30 s, la confirmation, l'ouverture ; puis
  l'export lu dans QGIS (ligne à géométrie vide).

## Qui veille, qui tranche

- **Relecteur :** `relecteur` (règle contre l'ADR, journal et export contre les formats, F4), puis Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Point de suivi :** au push du `testeur`, au plan du `dev`, puis à la PR.
- **Qui tranche :** Pascal (plan, textes, fusion).

## Comment saura-t-on que ça marche ?

La CI d'import passe avec P35-02 en contrôle sans position, et sur le téléphone sans fix, un code lu ouvre le contrôle
après confirmation.
