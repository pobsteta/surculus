# Mission 029 — `:gpkg` lit les géométries de `foret`, `ug` et `parcelles`

> Fiche écrite par l'orchestrateur le 2026-10-21, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Décidée par Pascal le 2026-10-21 :
> la mission 011 (afficheur) s'est arrêtée sur son prérequis, parce que `:gpkg` ne lit
> que l'enveloppe de `foret` et seulement le nombre d'objets de `ug` et `parcelles`.
> La 011 reprend après la fusion de cette mission.

## Mission

**En une phrase (quoi, et pourquoi) :** Exposer dans `:gpkg`, en lecture seule, les géométries de `foret`, `ug` et `parcelles` de la tournée (polygones et multipolygones en Lambert-93, trous compris, avec l'identifiant de chaque objet) et l'emprise des `ug`, pour que la carte de la 011 trace ces couches et que le préchargement de l'ortho porte sur l'emprise des UG plus une marge.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : `Geometrie.kt` (types `Polygone`, `MultiPolygone`, `GeometrieGpkg.decoder`), `LecteurTournee.kt` (lecture de `placettes` et de l'emprise de `foret`), modèle de sommieR en lecture seule | |
| A-t-il les moyens ? | oui : tournée de démo Couchey et `TourneeDeTest.kt` dans les tests de `:gpkg` | |

**Temps de la main :** **appuyer** : l'agent rend son plan avant d'écrire.

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** aucun écran, aucun dessin de carte, aucun préchargement de tuiles (mission 011) ; aucune écriture dans ces couches ; aucun changement de l'export.

## Harnais

- **Peut :** modifier `gpkg/src/main/…/LecteurTournee.kt`, `Tournee.kt`, `Geometrie.kt` (décodage complet des anneaux s'il manque) et leurs tests.
- **Ne doit pas :** changer le comportement de l'export (`ExportTravaux`, `ValidationExport`) ni casser un test existant de `:gpkg` ; écrire dans la tournée ; inventer un attribut que sommieR ne produit pas (les colonnes lues viennent du modèle de sommieR, cité) ; toucher à `main` ; trancher une question de sens (la remonter à l'orchestrateur).
- **Sources autorisées :** ADR 0003 et 0009 ; spec § 4.2 ; sommieR (modèle de tournée, `R/travaux_qfield.R`, `inst/qgis/travaux.qgs`) en lecture seule.

## Tests

- Les géométries de `foret`, `ug` et `parcelles` de la démo Couchey sont lues, avec le bon nombre d'objets, chacun avec son identifiant.
- Ordre et unités explicites : x = E, y = N, en mètres Lambert-93. Le centre de l'emprise des UG de Couchey tombe en France, et une emprise aux axes inversés est refusée (la placette en Somalie).
- Un polygone à trou garde son trou ; un multipolygone garde toutes ses parties.
- Une géométrie nulle ou vide est signalée, sans faire échouer la lecture des autres objets.
- Tous les tests existants de `:gpkg` restent verts sans être modifiés. Commande du projet : `.claude/test-command`.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Un test de `:gpkg` lit les géométries des UG de Couchey et rend leur emprise en France ;
la 011 peut reprendre sans toucher à `:gpkg`.
