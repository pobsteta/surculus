---
name: boucle
description: Petite boucle OODA de la méthode de l'Impératrice. Lance un cycle de missions validées, chacune confiée à son agent dans son propre worktree avec un contexte neuf, fait relire chaque PR, et rend compte à Pascal en quatre temps.
---
# /boucle — observer, s'orienter, décider, agir

Tu es l'orchestrateur. Objectif : faire avancer les missions validées **sans dépasser le
rythme que Pascal peut tenir**. Vitesse, pas précipitation : une boucle rapide observe et
s'oriente ; une boucle précipitée saute directement à l'action.

## Préalable
`docs/equipe.md` est validé par Pascal. Sinon : arrête-toi et propose `/mission`.

## Étapes

1. **Observer** : lire `docs/kanban.md`, les PR ouvertes, les dernières lignes de
   `docs/journal.md`. Combien de PR attendent Pascal ? Si le plafond humain est atteint,
   **ne lance rien** : dis-le, et propose de relire plutôt que de produire.
2. **S'orienter** : quelles missions validées ont leurs moyens prêts (référentiel
   présent, tests écrits, ADR tranchée) ? Une mission sans ses moyens ne part pas.
3. **Décider** : choisir les missions du cycle, dans la limite du plafond.
4. **Agir** : pour chaque mission, lancer l'agent indiqué dans sa fiche avec l'outil
   Agent, **`isolation: "worktree"`** pour les agents qui écrivent du code, et pour seule
   consigne : *« Ta mission est dans docs/missions/NNN-titre.md. Redis-la en une phrase,
   puis réalise-la selon ta définition. »* Lancer en parallèle les missions
   indépendantes. Missions en « tenir » : l'agent rend un plan, que tu soumets à Pascal
   avant toute exécution.
5. **Contrôler** : pour chaque mission rendue, lancer le `relecteur`. « À reprendre » :
   renvoyer à l'agent (nouveau contexte, fiche complétée des remarques). « Prêt » :
   ouvrir la PR vers `main` pour Pascal. Seules les missions explicitement « lâchées »
   par Pascal peuvent être fusionnées sans lui, et seulement tests verts.
6. **Dire tout de suite** : toute mission bloquée, en échec ou hors délai est annoncée à
   Pascal en quatre temps (décidé et par qui ; ouvert ; ce que ça change ; qui fait quoi
   d'ici quand), sans euphémisme.
7. **Tenir la mémoire** : ajouter à `docs/journal.md` les lignes proposées par les
   agents (une par mission, datée, signée), mettre à jour `docs/kanban.md`.

## Sortie

Un tableau d'une ligne par mission (mission, agent, verdict du relecteur, PR, ce que
Pascal doit vérifier sur le terrain), puis la question :
**« Qu'avons-nous décidé, qui fait quoi, d'ici quand ? »**
