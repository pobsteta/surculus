# Mission 033 — Placettes colorées selon `etat_suivi` sur la carte

> Fiche écrite par l'orchestrateur le 2026-10-22, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Décidée par Pascal le 2026-10-21
> (mission 011, décision (c)), réduite aux couleurs des placettes le 2026-10-22.

## Mission

**En une phrase (quoi, et pourquoi) :** Lire `etat_suivi` dans la couche `placettes` de la tournée et colorer chaque placette sur la carte selon sa valeur (`jamais`, `a_jour`, `a_programmer`), pour que le forestier voie en dix secondes, en forêt, quelles placettes il lui reste à contrôler.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Vérifié à la source

Constats vérifiés par l'orchestrateur sur `main` (162fb8c) et sommieR 0.31.1 (`fd92078`, `/home/pascal/dev/sommieR`), le 2026-10-22.

| Constat | Lu à | Source du constat |
|---|---|---|
| sommieR calcule `etat_suivi` : `jamais` sans contrôle, `a_jour` si le dernier besoin est vide ou `aucun`, sinon `a_programmer` | sommieR `R/travaux_qfield.R:334-337` | orchestrateur |
| sommieR écrit `etat_suivi`, `dernier_controle`, `dernier_besoin` dans la couche `placettes` de la tournée | sommieR `R/travaux_qfield.R:85-90` ; `docs/spec.md:248` | orchestrateur |
| Le schéma de sommieR 0.31.1 recopié dans les fixtures déclare `etat_suivi` | `fixtures/acceptation-export/schema-sommier-0.31.1.json:206` | orchestrateur |
| `:gpkg` lit `placettes` sans `etat_suivi` | `gpkg/src/main/kotlin/io/github/pobsteta/surculus/gpkg/LecteurTournee.kt:151-160` ; `PlacetteTournee` dans `Tournee.kt:28` | orchestrateur |
| La carte ne colore pas encore les placettes | `app/src/main/kotlin/io/github/pobsteta/surculus/carte/EcranCarte.kt:67` | orchestrateur |
| La palette a quatre couleurs au plus, déjà utilisées pour la qualité du fix GNSS (vert, orange, rouge) | `app/src/main/kotlin/io/github/pobsteta/surculus/carte/Palette.kt:13-25` | orchestrateur |
| La spec demande des placettes colorées selon `etat_suivi` | `docs/spec.md:85` | orchestrateur |
| La tournée de sommieR ne porte aucun travaux à dessiner (modèle vide, travaux passés dans `suivis` sans géométrie) ; l'app ne saisit encore ni travaux ni vides | sommieR `R/travaux_qfield.R:55-80` ; aucune saisie de travaux dans `app/`, `data/`, `core/` | orchestrateur |

Règles, avec leur auteur :
- **Couleurs selon `etat_suivi`, travaux par code et vides : mission 033** — Pascal, 2026-10-21 (journal, mission 011, (c)).
- **La 033 se réduit aux couleurs des placettes ; travaux et vides au bac à sable** — Pascal, 2026-10-22.
- **L'écran sert le forestier en forêt (gants, soleil, pluie, une main libre)** — CLAUDE.md.
- **Garde-fou = test vu rouge ; bilan des écarts avant rendu** — Pascal, bilan du 2026-10-21.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : écran Carte de la 011, `Palette.kt`, lecteur de `:gpkg` | |
| A-t-il les moyens ? | oui : `etat_suivi` est dans la tournée ; la lecture est à ajouter dans `:gpkg` (quelques lignes, dans cette mission) | |

**Temps de la main :** **appuyer** : l'agent rend son plan avant d'écrire. Le plan dit :
- la couleur ou la forme proposée pour chaque valeur, et comment elle se distingue des couleurs du fix GNSS (même palette, sens différent) ; **Pascal valide ce choix avant le code** ;
- ce que montre une valeur absente ou inconnue (`etat_suivi` nul, ou une valeur qu'une version future de sommieR ajouterait) ;
- s'il ajoute une légende, et où ;
- les tests : la lecture dans `:gpkg`, la correspondance valeur → couleur.

## L'Homme

- **Agent :** `afficheur` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :**
  - les travaux et les vides sur la carte ;
  - recalculer `etat_suivi` dans Surculus après un contrôle (la valeur vient de sommieR ; ce qu'affiche une placette contrôlée pendant la tournée est une question à remonter, pas à trancher) ;
  - modifier sommieR.

## Harnais

- **Peut :** ajouter `etat_suivi` (et, s'il le justifie, `dernier_controle`, `dernier_besoin`) à `PlacetteTournee` et à sa lecture dans `:gpkg` ; modifier `EcranCarte.kt`, `Palette.kt` ; ajouter des tests.
- **Ne doit pas :** inventer une valeur d'`etat_suivi` absente de sommieR ; casser un test existant ; dépasser quatre couleurs sans le dire ; toucher à `main` ; trancher une question de sens.
- **Sources autorisées :** sommieR 0.31.1 en lecture seule ; `docs/spec.md` ; `docs/adr/0009-carte-hors-ligne.md`.

## Tests

- Lecture : une tournée de test avec les trois valeurs, une valeur nulle et une valeur inconnue ; une tournée sans colonne `etat_suivi` (ancienne tournée) se lit toujours.
- Correspondance valeur → couleur testée ; **vu rouge** une fois.
- Commande du projet : `.claude/test-command`.
- **Bilan des écarts** : chaque fichier, assertion ou fixture modifiés, avec la raison.

## Terrain

- **Échantillon que Pascal vérifiera :** la carte de Couchey sur le téléphone, en plein jour : en dix secondes, dit-il quelles placettes restent à contrôler ?

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.
- **Point de suivi :** à la remise du plan (choix des couleurs soumis à Pascal), puis à l'ouverture de la PR.

## Comment saura-t-on que ça marche ?

Sur la carte de Couchey, les placettes `jamais`, `a_jour` et `a_programmer` se distinguent d'un coup d'œil, sans confusion avec le badge du fix.
