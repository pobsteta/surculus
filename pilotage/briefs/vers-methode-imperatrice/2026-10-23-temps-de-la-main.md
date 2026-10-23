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

## Demandé dans le plugin

1. Remplacer les trois termes dans :
   - `skills/methode/SKILL.md` (l. 58-60, règle de staffing ; l. 76-85, les trois temps) ;
   - `skills/mission/SKILL.md` (description l. 3 si elle les nomme, l. 29-31, l. 36-38) ;
   - `skills/mission/fiche-mission.md` l. 20 ;
   - `skills/bilan/SKILL.md` l. 35 ;
   - `skills/boucle/SKILL.md` l. 36 ;
   - `agents/dev.md` l. 25 ;
   - le README, s'il les nomme.
2. **Corriger le double sens d'« appuyer »** relevé dans Surculus : la règle de staffing
   (`skills/methode/SKILL.md` l. 59, `skills/mission/SKILL.md` l. 29) dit « Appuyer :
   l'agent rend d'abord un plan, relu avant exécution », alors que le tableau des temps
   (l. 84) dit « Appuyer : l'agent travaille dans sa branche ; le relecteur puis Pascal
   valident la PR ». Le plan d'abord est la définition de « tenir ». Proposition de
   l'orchestrateur, à trancher par Pascal dans ce dépôt : « Non à la 2e question →
   **prendre la main** : l'agent rend un plan d'abord ».
3. **Version** : un renommage est une rupture selon le README (l. 83) → **`1.0.0`**.
4. **Transition** : pendant une version, `agents/dev.md` et `/boucle` reconnaissent
   l'ancien et le nouveau terme dans une fiche (« tenir » = « prendre la main »), pour
   que les projets en service (Surculus : `pilotage/equipe.md` et 36 fiches) aient le
   temps de migrer. Un test le vérifie.

## Ce qui suivra dans Surculus

Après la publication de la `1.0.0` : migration de `pilotage/equipe.md` et des fiches de
mission (mission de doc, à l'orchestrateur). Rien ne change avant.

## Critère d'acceptation

`grep -rniw "tenir\|appuyer\|lâcher"` dans `skills/` et `agents/` ne renvoie plus que
« Plier ou tenir » et la règle de transition ; la règle de staffing et le tableau des
temps donnent la même définition de chaque terme ; la `1.0.0` est publiée.
