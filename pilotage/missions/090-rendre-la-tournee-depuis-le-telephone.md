# Mission 090 — Rendre la tournée depuis le téléphone

> Fiche écrite par l'orchestrateur le 2026-11-03, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> après la décision de Pascal du 2026-11-03 (« Mission 090, avant l'essai », puis « 090 d'abord ») : le `dev` de la 088
> a relevé qu'aucun écran n'appelle l'export (`ExportTravaux` n'est appelé que par des tests). Depuis `main` (7c8f969,
> v0.7.106). Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Permettre à l'agent de rendre sa tournée depuis le téléphone, en écrivant le
`<tournée>.zip` (le `travaux.gpkg` complété par les saisies du journal, vérifié, et `DCIM/`) que l'import de sommieR
lit, pour qu'une tournée saisie en forêt revienne enfin au sommier sans ordinateur entre les deux.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | à moitié : l'export existe et il est éprouvé par la CI d'import (`gpkg/.../ExportTravaux.kt`, `ValidationExport.kt`, critères F1 à F5), le passage du journal aux saisies existe dans des tests (`SessionEtExportCoucheyTest`, `ExporterScenario`) ; ni l'écran, ni le passage journal → saisies dans l'application, ni l'état « rendue » n'existent | **tenir** : le `dev` rend un plan d'abord |
| A-t-il les moyens ? | oui : tournées d'essai, CI d'import, téléphone de Pascal | le `testeur` écrit d'abord les tests |

**Temps de la main :** tenir : c'est le chemin par lequel la donnée revient au sommier (justesse des données).

## Ma réponse d'abord

_Rempli par Pascal seul._

## L'Homme

- **Agents :** `testeur` d'abord (tests depuis la spec, l'ADR 0007 et cette fiche, **sans lire** le code de l'export,
  du journal et des écrans de tournée), puis `dev`, qui **rend un plan** avant de coder ; l'orchestrateur le présente à
  Pascal.
- **Modèle :** opus.
- **Ce qui ne lui est pas demandé :** les photos (aucune n'est encore prise : `DCIM/` reste tel que la tournée l'a
  livré) ; l'envoi par le réseau (WebDAV) ; la fusion de plusieurs opérateurs (ADR 0006, non écrite) ; ouvrir sans
  position depuis le guidage (089).

## La règle (spec § 4.8, § 5, § 6 et ADR 0007 : ils font foi)

- Export `<tournée>.zip` : `travaux.gpkg` complété, `DCIM/` ; décompressé, c'est le dossier que lit l'import de sommieR.
- Les saisies du journal de **cette** tournée seulement (lignes non annulées, corrections appliquées) deviennent les
  lignes ajoutées : contrôles, plants, positions (`positions_controles`, une ligne par contrôle, géométrie NULL sans
  position mesurée), comme les tests de la 087 et la CI d'import l'exigent.
- **Vérification complète** avant d'écrire (§ 6, `ValidationExport`) ; une faute bloque l'export et **la liste des
  fautes renvoie à la saisie** (placette, plant), en français, sans motif technique (règle de la 068 et de la 082).
- La tournée passe **rendue** et reste consultable ; une tournée n'est jamais écrasée (§ 4.1).
- L'export ne touche pas au journal : rendre deux fois donne le même contenu (export rejouable et déterministe, § 5).

## Questions que le plan du `dev` doit proposer (Pascal tranche)

1. Où le `.zip` est écrit et comment il sort du téléphone (dossier public, partage Android, les deux).
2. Une tournée rendue peut-elle encore recevoir des saisies (et alors, que devient le `.zip` déjà écrit) ?
3. Où se trouve le bouton « Rendre la tournée », et ce que l'agent voit pendant et après (nombre de contrôles, fautes).
4. Un contrôle commencé mais non terminé : exporté, refusé, ou signalé ?

## Harnais

- **Testeur peut :** tests dans `core/`, `gpkg/`, `data/`, `app/src/test/` ; fixtures sous `fixtures/` ; vus rouges.
- **Dev peut :** ce que son plan nomme et que Pascal accepte ; a priori le passage journal → saisies (hors des tests),
  l'état « rendue » (`DepotTournees.kt`), l'écran et ses textes, l'écriture du `.zip`.
- **Ne doivent pas :** modifier le contrat du `travaux.gpkg` ni les couches de sommieR (ADR 0007) ; toucher à `docs/`
  (signaler) ; toucher à `main`.
- **Sources autorisées :** spec § 4.1, § 4.8, § 5, § 6 ; ADR 0005 et 0007 ; `ci/sommier/README.md` ; fiches 087 et 088.

## Valeur provisoire (chiffre, réglage)

- Aucune.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** 087 et 088 (dans `main`, 7c8f969).
- **PR en brouillon jusqu'à :** la fin du travail du `dev`.
- **CI rouge attendue :** les tests du `testeur`, entre son push et celui du `dev`.

## Tests

- Une tournée d'essai avec des contrôles saisis au journal (dont un sans position, un corrigé, un annulé) se rend en un
  `.zip` dont le `travaux.gpkg` passe la validation et contient exactement les lignes attendues.
- Une faute (par exemple un point hors de France) bloque l'export et la liste dit quelle placette, quel plant.
- Rendre deux fois : même contenu ; la tournée est marquée rendue et reste consultable.
- Le `.zip` produit par le chemin de l'application passe la CI d'import (F1 à F5), si le `dev` peut l'y brancher.
- `.claude/test-command` au code 0 ; CI verte.

## Terrain

- **Échantillon que Pascal vérifiera :** sur la release, la tournée d'essai de Couchey : un contrôle saisi, « Rendre la
  tournée », le `.zip` retrouvé sur le téléphone, copié sur le PC, ouvert dans QGIS, puis importé dans sommieR.

## Qui veille, qui tranche

- **Relecteur :** `relecteur` (contre la spec, l'ADR 0007 et la CI d'import), puis Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Point de suivi :** au push du `testeur`, au plan du `dev`, puis à la PR.
- **Qui tranche :** Pascal (plan, questions 1 à 4, textes, fusion).

## Comment saura-t-on que ça marche ?

Une tournée saisie sur le téléphone est rendue en `.zip`, et ce `.zip` s'importe dans sommieR 0.31.1 sans une faute.
