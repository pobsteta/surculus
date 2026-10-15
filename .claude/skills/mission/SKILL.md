---
name: mission
description: Règle de staffing de la méthode de l'Impératrice. Découpe la spec en missions d'une phrase, applique à chacune les trois questions (un Homme, une mission, des moyens), fixe le temps de la main et propose docs/equipe.md à la validation de Pascal.
---
# /mission — un Homme, une mission, des moyens

Tu es l'orchestrateur. Objectif : proposer à Pascal **l'équipe juste nécessaire**,
mission par mission. Porte n° 2 de la méthode. Relis d'abord la section « règle de
staffing » de `docs/methode-imperatrice.md`.

## Préalable
`docs/brief.md` et `docs/spec.md` sont validés par Pascal (voir `docs/journal.md`).
Sinon : arrête-toi et propose `/brief`.

## Étapes

1. **Découper la spec en missions d'une phrase** (quoi, et pourquoi). Une mission qui
   demande deux phrases est deux missions. Ordonner : d'abord les référentiels et le
   squelette qui traverse tout le parcours (un parcours terrain complet, minimal), puis
   l'enrichissement.
2. **Pour chaque mission, poser les trois questions** et noter la réponse :
   - Peut-il redire la mission en une phrase, avec son but ?
   - Sait-il comment faire ? (sources, conventions, ADR nommées)
   - A-t-il les moyens ? (outils, données d'essai, tests ou critère vérifiable)

   | Résultat | Décision |
   |---|---|
   | Non à la 1re | pas d'agent : redécouper, ou poser la question à Pascal |
   | Non à la 2e | **appuyer** au mieux, ou **tenir** : l'agent rend un plan d'abord |
   | Non à la 3e | **ne pas lâcher** : créer d'abord la mission qui fournit le moyen manquant (référentiel → `acquereur`, tests → `testeur`) |
   | Trois oui | confier ; temps de la main selon l'enjeu |

3. **Choisir l'agent** dans le vivier (`docs/equipe.md`) : le plus simple qui suffit.
   Jamais un agent « par principe ». Modèle `sonnet` par défaut ; `opus` si la mission
   engage l'architecture ou la justesse des données.
4. **Fixer le temps de la main, par mission** : *tenir* pour ce qui engage la durée
   (modèle de données, codes, stack) ; *appuyer* par défaut ; *lâcher* seulement sur
   décision de Pascal, pour une mission déjà réussie deux fois.
5. **Appliquer le plafond humain** : pas plus de missions en revue simultanée que le
   plafond de `docs/equipe.md`. Le reste va en « planifié ».
6. **Écrire chaque fiche** dans `docs/missions/NNN-titre.md` à partir de
   `docs/modeles/fiche-mission.md`, harnais en cinq points compris (brief, harnais,
   tests, terrain, qui tranche). Associer à chaque mission de développement une mission
   `testeur` qui la précède ou l'accompagne.
7. **Mettre à jour `docs/equipe.md`** (tableau des missions) et `docs/kanban.md`
   (colonne « validé » après accord).

## Sortie

Présenter à Pascal, en dix secondes de lecture : le nombre de missions, les agents
mobilisés et pourquoi, les missions tenues, et **ce qui manque** (missions qui n'ont pas
passé les trois questions). Clore par :
**« Valides-tu cette équipe ? Qui fait quoi, d'ici quand ? »**
Rien ne démarre sans son « oui ». Ajouter une inscription à `docs/journal.md`.
