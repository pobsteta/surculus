# Format `.sursync`, version 3

> **Version écrite par Surculus depuis la mission 091** (décision de Pascal du 2026-11-04, « v3, tests ajustés »).
> Elle porte les **valeurs v3 du journal** ([`journal-valeurs-v3.md`](journal-valeurs-v3.md) : besoin, concurrence,
> précision, source et caractère simulé du fix). Le type de ligne `photo` reste réservé, comme en v2.

Ce document dit **ce qui change** par rapport à la [v2](sursync-v2.md), qui renvoie elle-même à la [v1](sursync-v1.md)
pour tout le reste (structure, empreinte, fichier abîmé, nom, rotation, restauration, confidentialité, limites).
**Seul le numéro de version monte** : l'en-tête, les saisies et le pied ont les mêmes champs que la v2 (schéma
[`sursync-v2.schema.json`](sursync-v2.schema.json), avec `version` = 3).

## Version

`version` vaut **3**, un entier JSON. Surculus écrit **toujours** en v3 depuis la mission 091, même quand le journal ne
porte que des valeurs v1 ou v2.

La règle *N*-1 de la v1 (« Version ») s'applique avec *N* = 3 (décision de Pascal du 2026-11-04, « Garder la règle ») :

| Fichier | Lecteur v3 (Surculus 091 et suivants) | Lecteur v2 (Surculus 088 à 090) |
|---|---|---|
| version 1 | **refusé**, motif `version_illisible` (inférieure à *N*-1) | lu |
| version 2 | lu | lu |
| version 3 | lu | refusé en entier, motif `version_future`, message « mettre l'application à jour » |
| version 4 ou plus | refusé en entier, `version_future` | idem |
| version absente, non entière, nulle ou négative, en-tête illisible, 0 octet, non gzip | refusé en entier, `version_illisible` | idem |

Prix, accepté par Pascal : un `.sursync` v1 (écrit avant la mission 088) ne se restaure plus. Les sauvegardes se
réécrivent en version courante à chaque saisie, et aucun vrai contrôle n'était en jeu au 2026-11-04.

Un refus en entier ne change rien au journal local, et l'écran le dit. C'est ce refus qui empêche un Surculus de la v2
de perdre sans le dire le besoin d'un contrôle ([`journal-valeurs-v3.md`](journal-valeurs-v3.md), « Sauvegarde »).

## Valeurs du journal

Le champ `valeur` d'une saisie est le texte JSON de la valeur, en v1, v2 ou v3, recopié au caractère près. Restauré, un
contrôle garde son besoin, sa concurrence et la qualité de son fix à l'identique ; un contrôle v1 ou v2 les a à `null`.

## Exemples

Dans `fixtures/sursync/`, refaits par `exemple-v3-fabrication.sh` (sh, gzip `-n -9`, sha256sum) :

- `exemple-v3.jsonl` : le contenu décompressé : en-tête en version 3 ; un contrôle v2 terminé sans besoin (P35-03) ;
  trois contrôles v3 : P35-01 mesuré par un récepteur RTK fixe, P35-02 ouvert sans position, P35-04 mesuré par un
  téléphone dont la position est simulée ;
- `exemple-v3.sursync` : le même, en gzip ;
- `exemple-v3-empreinte-fausse.sursync` et `exemple-v3-tronque.sursync` : les cas abîmés, comme en v2.

Un successeur les vérifie avec `zcat` et `sha256sum`, sans Surculus.
