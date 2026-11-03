# Format `.sursync`, version 2

> **Version écrite par Surculus depuis la mission 088** (décisions de Pascal du 2026-11-02, « nouvelle version » et
> « v2 partagée », mission 085 ; spec § 4.8). Une seule v2, partagée : elle porte les **valeurs v2 du journal**
> (contrôles ouverts sans position mesurée, [`journal-valeurs-v2.md`](journal-valeurs-v2.md)) et **réserve** le type
> de ligne `photo`, que la mission des photos codera plus tard dans ce même format.

Ce document ne recopie pas la v1 : il dit **ce qui change**. Tout ce qu'il ne dit pas vaut tel que l'écrit
[`sursync-v1.md`](sursync-v1.md), qui reste la définition de la v1, toujours lue. Schéma :
[`sursync-v2.schema.json`](sursync-v2.schema.json). Exemples : `fixtures/sursync/exemple-v2*` (voir plus bas).

## Repris de la v1, sans changement

Chaque règle ci-dessous est celle de la section citée de [`sursync-v1.md`](sursync-v1.md) :

- **Structure** (« Structure ») : gzip de JSON Lines UTF-8 ; en-tête, saisies dans l'ordre total du journal, pied
  `{"sha256": ...}` ; mêmes champs d'en-tête et de saisie, mêmes types ; `valeur` recopiée au caractère près ;
  instants en millisecondes Unix, UTC ; une seule tournée par fichier.
- **Empreinte SHA-256** du contenu décompressé (« Structure », 3).
- **Fichier abîmé** (« Un fichier abîmé n'est pas refusé en entier ») : mêmes signalements (`fichier_tronque`,
  `ligne_illisible`, `empreinte_differente`, `ligne_autre_tournee`, `uuid_contenu_different`), mêmes effets.
- **Nom du fichier, non-écrasement, rotation** (« Nom du fichier ») : le nom ne porte pas la version ; un nom pris
  n'est jamais écrasé ; on garde les 5 dernières sauvegardes de chaque tournée, v1 et v2 confondues, triées par
  horodatage du nom.
- **Quand et où** (« Quand et où »), **Restauration** (union sur l'`uuid`, idempotente), **Confidentialité**.
- **Limites** (« Limites de la version 1 ») : `refus_journal` n'est pas sauvegardé ; une tournée par fichier.

## Ce qui change

### Version

`version` vaut **2** : un entier JSON. Écrite en texte (`"2"`), la version n'est pas lue (`version_illisible`).

Surculus écrit **toujours** en v2 depuis la mission 088, même quand le journal de la tournée ne porte que des valeurs
v1 : depuis la mission 087, le journal écrit ses valeurs en v2.

La règle de la v1 (« Version ») s'applique avec *N* = 2 : un lecteur de version 2 lit les versions 1 et 2.

| Fichier | Lecteur v2 (Surculus 088 et suivants) | Lecteur v1 (Surculus ancien) |
|---|---|---|
| version 1 | lu, à l'identique de ce que donnait un lecteur v1 | lu |
| version 2 | lu | refusé en entier, motif `version_future`, message « mettre l'application à jour » |
| version 3 ou plus | refusé en entier, motif `version_future`, message « mettre l'application à jour » | idem |
| version absente, non entière (`1.5`, `"2"`), nulle ou négative, en-tête illisible, 0 octet, non gzip | refusé en entier, motif `version_illisible` | idem |

Un refus en entier ne change rien au journal local, et l'écran le dit. C'est ce refus qui empêche un Surculus ancien de
perdre **sans le dire** un contrôle sans position : il ne lit pas le champ `v` des valeurs, et ignorerait un contrôle
dont `position_mesuree_x` est `null` ([`journal-valeurs-v2.md`](journal-valeurs-v2.md), « Sauvegarde `.sursync` »).

Prix connu : le jour d'une version 3, un lecteur v3 refusera les fichiers v1 (`version_illisible`, règle *N*-1).

### Valeurs du journal

Le champ `valeur` d'une saisie est le texte JSON de la valeur, en v1 ([`journal-valeurs-v1.md`](journal-valeurs-v1.md))
ou en v2 ([`journal-valeurs-v2.md`](journal-valeurs-v2.md)), recopié au caractère près. Un contrôle sans position
mesurée y garde son drapeau `sans_position_mesuree`, ses positions mesurées à `null` et sa position du piquet
(Lambert-93, EPSG:2154, x = Est, y = Nord, en mètres) ; restauré, il revient « sans position », à l'identique.

### Le type `photo`

Le type de ligne `photo` est **réservé** (spec § 4.8, amendement du 2026-10-28) : il entre dans la liste des types du
schéma, mais sa `valeur` n'est pas encore définie et Surculus 088 n'en écrit aucune. Un lecteur 088 qui en reçoit une
l'**écarte et la signale** `ligne_illisible`, comme tout type hors de sa liste, et restaure les autres lignes : rien
n'est perdu sans être dit (décision de Pascal du 2026-11-03). Le message propre aux photos viendra avec la mission
des photos.

## Exemples

Dans `fixtures/sursync/`, refaits par `exemple-v2-fabrication.sh` (sh, gzip `-n -9`, sha256sum) :

- `exemple-v2.jsonl` : le contenu décompressé, lisible : en-tête en version 2 et deux contrôles en valeurs v2, l'un
  ouvert sans position mesurée, l'autre avec ;
- `exemple-v2.sursync` : le même, en gzip ;
- `exemple-v2-empreinte-fausse.sursync` : une valeur du contrôle avec position changée, pied inchangé ;
- `exemple-v2-tronque.sursync` : l'exemple amputé de ses 10 derniers octets.

Un successeur les vérifie avec `zcat` et `sha256sum`, sans Surculus.
