# Mission 023 — La méthode en plugin, le pilotage dans `pilotage/`

> Fiche écrite par l'orchestrateur le 2026-10-20, à partir du gabarit
> `imperatrice/skills/mission/fiche-mission.md`. Décidée par Pascal le même jour (option 1 :
> plugin séparé, rangement dans `pilotage/`, README centré sur le produit).

## Mission

**En une phrase (quoi, et pourquoi) :** Sortir la méthode de l'Impératrice (agents,
commandes, hooks, gabarits, texte de la méthode) dans le plugin `imperatrice` du dépôt
`pobsteta/methode-imperatrice`, et ranger la trace du pilotage de Surculus dans
`pilotage/`, pour que le dépôt montre d'abord l'application et que la méthode serve tous
les projets sans copie.

**Redite par l'orchestrateur :** la méthode vit dans un plugin, Surculus la déclare et
n'en garde que sa trace (`pilotage/`) et ses règles métier (`CLAUDE.md`).

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : format des plugins Claude Code, `claude plugin validate` | |
| A-t-il les moyens ? | oui : dépôt `methode-imperatrice` créé par Pascal le 2026-10-20 | |

**Temps de la main :** **appuyer**. Réalisée par l'orchestrateur lui-même : elle touche
`.claude/` et le journal, que les agents de réalisation n'ont pas le droit de toucher.

## Harnais

- **Peut :** déplacer `docs/equipe.md`, `docs/kanban.md`, `docs/journal.md` (par `git mv`,
  contenu intact), `docs/missions/` et `briefs/` dans `pilotage/` ; retirer `.claude/agents/`,
  `.claude/skills/`, `.claude/hooks/`, `docs/methode-imperatrice.md`, `docs/modeles/` ;
  mettre à jour les renvois de chemins ; réécrire `README.md`.
- **Ne doit pas :** toucher au code ni aux règles des fixtures (seul le texte `source`
  qui cite le journal change de chemin) ; réécrire une inscription du journal.

## Tests

- Plugin : `claude plugin validate .`, tests des hooks (`tests/test_garde_main.py`,
  `tests/test_journal_ajout_seul.py`), installation réelle dans un dossier vide.
- Surculus : aucun lien Markdown cassé ; plus aucun renvoi vers les anciens chemins hors
  du journal ; tests de `:core` et `:gpkg` verts (les fixtures ont changé de texte).

## Terrain

- **Échantillon que Pascal vérifiera :** ouvrir Surculus dans Claude Code après la
  fusion, accepter l'installation du plugin, lancer `/imperatrice:boucle` : elle doit lire
  `pilotage/kanban.md`, et un commit sur `main` doit toujours être refusé.

## Qui veille, qui tranche

- **Relecteur :** Pascal (PR #1 de `methode-imperatrice`, puis la PR de Surculus).
- **Qui tranche :** Pascal. **Ordre de fusion :** le plugin d'abord, Surculus ensuite ;
  entre les deux, Surculus n'a plus ni agents ni hooks.

## Comment saura-t-on que ça marche ?

Un visiteur du dépôt comprend en dix secondes ce que fait Surculus ; une session Claude
Code dans Surculus a les mêmes agents, commandes et garde-fous qu'avant.
