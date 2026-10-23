# Brief méthode de l'Impératrice — renommer les trois temps de la main

*Établi le 23 octobre 2026 depuis le dépôt `surculus`, à partir de `methode-imperatrice`
(`b8bf9b1`, v0.3.0) et du livre imprimé (PDF KDP du 20 octobre 2026).*

## La décision de Pascal

Le 2026-10-23, Pascal a demandé une terminologie où la main apparaît, puis l'a choisie :

| Livre et plugin aujourd'hui | Nouveau terme |
|---|---|
| Tenir | **prendre la main** |
| Appuyer | **donner la main** (au sens d'aider) |
| Lâcher | **rendre la main** |

Il a retenu l'option (a) : **renommer partout, livre compris**. Le plugin et le livre
disent la même chose ; la correction du livre (« Les trois temps de la main », les fiches,
« Condition pour lâcher ») est à la charge de Pascal, pour une nouvelle édition.

« Plier ou tenir » (fiche n° 14) est une autre notion : il **ne change pas**.

## Demandé dans le plugin (découle de la décision de Pascal)

1. Remplacer les trois termes dans :
   - `skills/methode/SKILL.md` (l. 58-60, règle de staffing ; l. 62, « Trois oui : on
     lâche » ; l. 76-85, les trois temps) ;
   - `skills/mission/SKILL.md` (description l. 3 si elle les nomme, l. 29-31, l. 36-38,
     l. 53 « les missions tenues ») ;
   - `skills/mission/fiche-mission.md` l. 20 ;
   - `skills/bilan/SKILL.md` l. 35 ;
   - `skills/boucle/SKILL.md` l. 36 et l. 46 (« missions explicitement « lâchées » ») ;
   - `agents/dev.md` l. 25 ;
   - le README, s'il les nomme.

   Ne changent pas : « Plier ou tenir » ; les emplois courants des verbes, sans rapport
   avec les temps de la main (par exemple `skills/boucle/SKILL.md` l. 8 « rythme que
   Pascal peut tenir », l. 52 « Tenir la mémoire » ; `skills/bilan/SKILL.md` l. 27
   « rythme tenu », l. 30 « appuyé sur un fait », citation du livre vérifiée par
   `tests/test_alignement_livre.py` ; `skills/methode/SKILL.md` l. 31 « Tenu par » ;
   `agents/acquereur.md` l. 22 ; `skills/demarrer/gabarit-claude.md` l. 9 « Tu tiens
   l'étape ») ; l'historique du dépôt (`docs/brief.md`, `pilotage/missions/`, journal).
2. **Version** : un renommage est une rupture selon le README (l. 83) → **`1.0.0`**.

## Proposé par l'orchestrateur, à trancher par Pascal dans ce dépôt

3. **Le double sens d'« appuyer »**, relevé dans Surculus :
   - `skills/methode/SKILL.md` l. 59 (règle de staffing, 2e question) : « **Appuyer** :
     l'agent rend d'abord un plan, relu avant exécution » ;
   - `skills/methode/SKILL.md` l. 84 (tableau des temps) : « **Appuyer** (défaut) |
     L'agent travaille dans sa branche (worktree) ; le relecteur puis Pascal valident la
     PR » ;
   - `skills/mission/SKILL.md` l. 29 est seulement ambiguë : « **appuyer** au mieux, ou
     **tenir** : l'agent rend un plan d'abord ».

   Le plan d'abord est la définition de « tenir » (l. 83). Proposition : « Non à la 2e
   question → **prendre la main** : l'agent rend un plan d'abord ». Dans Surculus, la
   fiche 033 (l. 42) écrit déjà « appuyer : l'agent rend son plan avant d'écrire ».
4. **Transition** : pendant une version, `agents/dev.md` et `/boucle` reconnaissent
   l'ancien et le nouveau terme dans une fiche (« tenir » = « prendre la main »), pour
   que les projets en service aient le temps de migrer ; un test le vérifie.

5. **« On revient en arrière, on ne lâche pas »** (`skills/methode/SKILL.md` l. 88,
   `agents/dev.md` l. 15) répond à « Ne pas lâcher » (3e question, l. 60). Question de
   sens pour Pascal : la renommer (« on ne rend pas la main ») ou la garder comme emploi
   courant ? L'orchestrateur propose de la renommer, pour que la 3e question et cette
   phrase disent la même chose.

## Ce qui suivra dans Surculus

Après la publication de la `1.0.0` : migration de `pilotage/equipe.md`, des fiches de
mission et des cartes du kanban (mission de doc, à l'orchestrateur), et du `CLAUDE.md`
du dépôt (l. 48-49 « tenir, appuyer, lâcher », l. 55 « lâchées »), à modifier par Pascal.
Rien ne change avant.

## Critère d'acceptation

Dans `skills/` et `agents/`, **aucune occurrence de « tenir », « appuyer » ou « lâcher »
(formes fléchies comprises) n'est employée au sens des temps de la main**, hors règle de
transition si Pascal la retient. Pour le vérifier :
`grep -rniE "\b(tenir|tiens|tient|tenu|appuy|lâch|lach)" skills/ agents/`, puis chaque ligne
renvoyée est soit « Plier ou tenir », soit un emploi courant (point 1), soit la phrase
du point 5 si Pascal la garde. La règle de staffing et le tableau des temps donnent la
même définition de chaque terme ; la `1.0.0` est publiée.
