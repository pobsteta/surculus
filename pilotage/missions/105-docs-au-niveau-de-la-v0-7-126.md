# Mission 105 — Docs au niveau de la v0.7.126

> Fiche écrite par l'orchestrateur le 2026-11-09, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> après la décision de Pascal au bilan du 2026-11-09 (test du successeur, « Oui, mission archiviste »). Depuis `main`.

## Mission

**En une phrase (quoi, et pourquoi) :** Mettre le README, la spec, l'ADR 0003, `pilotage/equipe.md` et le bac à sable
du kanban au niveau de la v0.7.126, pour qu'un successeur sache en une minute ce que fait Surculus aujourd'hui.

**Redite par l'agent :**
- *Archiviste :* _à remplir_

## Les trois questions

| Question | Réponse | Conséquence |
|---|---|---|
| Redire en une phrase ? | oui | |
| Sait-il comment ? | oui : son rapport du bilan du 2026-11-09 (journal, même date) | |
| Les moyens ? | oui : journal, kanban (Livraisons), code de `main` | |

**Temps de la main :** appuyer.

## Ma réponse d'abord

_Rempli par Pascal seul._

## L'Homme

- **Agent :** `archiviste`, sonnet.
- **Ce qui ne lui est pas demandé :** l'ADR 0010 (attend deux renseignements de Pascal) ; la liste de contrôle (mission
  106) ; changer une décision ; toucher au code.

## Harnais

- **Peut :**
  1. README et `docs/spec.md` : une ligne « version couverte : v0.7.126, date » ; ce qui est livré de la 084 à la 103
     (ajouts datés, renvoi aux Livraisons du kanban plutôt que copie) ; le README dit en trois lignes l'état réel, dont
     la mauvaise nouvelle (rien vérifié en forêt).
  2. ADR 0003 l. 197 : passage du futur au fait (ajout daté, rien effacé).
  3. `pilotage/equipe.md` : tableau des missions jusqu'à la 106 ; règle du bilan du 2026-11-09 (rubrique « textes et
     bornes décidés au plan » des fiches d'écran, tranchée par Pascal avant le code).
  4. Kanban, bac à sable : retirer les entrées vérifiées périmées dans le code (citer fichier:ligne dans la PR, pour
     chacune), dater les autres. Ne rien retirer de ce qui dépend de Pascal.
- **Ne doit pas :** effacer une ligne du journal ; modifier `app/`, `core/`, `gpkg/`, `data/` ; trancher une question
  de sens (la poser dans la PR).
- **Sources :** journal (bilan du 2026-11-09), kanban, code de `main`, `docs/`.

## Valeur provisoire

Aucune.

## Dépend de / rouge attendu jusqu'à

Aucune dépendance (la 106 peut être fusionnée avant ou après). Une PR, en brouillon jusqu'à la fin.

## Tests

CI verte (docs seules).

## Terrain

Sans objet.

## Qui veille, qui tranche

- **Relecteur :** `relecteur` (chaque entrée retirée du bac à sable revérifiée dans le code), puis Pascal.
- **Critère d'arrêt :** 3 allers-retours. **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Le test de la minute passe : README et spec disent la version couverte et le livré ; le bac à sable n'a plus d'entrée
périmée.
