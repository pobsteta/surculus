# Format `.sursync`, version 1

Sauvegarde du journal d'une tournée de Surculus (ADR 0005, spec § 4.8). Format ouvert : n'importe quel
outil qui sait lire du gzip et du JSON peut le lire (test du successeur). Schéma :
[`sursync-v1.schema.json`](sursync-v1.schema.json). Exemples : `fixtures/sursync/`. Cas d'acceptation :
`fixtures/sauvegarde/` (SV-01 à SV-55).

## Structure

Un fichier gzip dont le contenu est du **JSON Lines** (UTF-8, une ligne JSON par ligne, chaque ligne terminée par
`\n`) :

1. **En-tête** : `format` (`"sursync"`), `version` (entier, 1 ici), `code_tournee`, `version_sommier`
   (version de sommieR de la tournée, ou `null` ; jamais l'empreinte du `travaux.gpkg`, qui change à chaque
   régénération), `appareil` (identifiant **opaque** de l'appareil qui écrit), `cree_le_utc_ms`, `nb_lignes`.
2. **Saisies**, dans l'ordre total du journal (`saisi_le_utc`, `appareil`, `sequence`, `uuid`) : les treize champs
   d'une ligne du journal, tels quels (spec § 5). `valeur` est le **texte** JSON de la valeur, recopié au caractère
   près (c'est ce qui garantit une restauration « à la ligne près »). Les instants sont des **millisecondes depuis
   l'époque Unix, UTC** (suffixe `_ms`).
3. **Pied** : `{"sha256": "<64 chiffres hexadécimaux minuscules>"}`, SHA-256 des octets **décompressés** de toutes
   les lignes précédentes (en-tête et saisies, fins de ligne `\n` comprises).

Le journal est en ajout seul : le fichier contient toutes les lignes, y compris les annulations et les lignes
`saut_horloge`, sans les interpréter. La projection se recalcule depuis les lignes.

## Version

La version courante est 1. Un lecteur de version *N* lit les versions *N*-1 et *N* et **refuse** les autres :

| Fichier | Résultat |
|---|---|
| version *N*+1 ou plus | refusé en entier, motif `version_future`, message « mettre l'application à jour » |
| version absente, non entière, nulle ou négative, en-tête illisible, fichier de 0 octet ou non gzip | refusé en entier, motif `version_illisible` |
| version inférieure à *N*-1 | refusé, motif `version_illisible` |

Il n'existe pas de version 0 : le format v1 est le premier. La lecture de *N*-1 se teste avec un fichier v1 lu par
un lecteur de version 2 (paramètre de test).

## Un fichier abîmé n'est pas refusé en entier

Seuls les refus ci-dessus rejettent le fichier. Sinon les lignes saines sont restaurées et chaque défaut est
signalé (codes lus par les tests ; les messages affichés ne le sont pas) :

| Défaut | Signalement | Effet |
|---|---|---|
| gzip coupé, ou pied absent | `fichier_tronque` | lignes lisibles en entier avant la coupure restaurées |
| ligne JSON illisible ou incomplète (champ absent ou mal typé, type hors liste) | `ligne_illisible` | ligne refusée, les autres restaurées |
| pied présent mais faux | `empreinte_differente` | toutes les lignes lisibles restaurées, avertissement visible (décision de Pascal, 2026-10-21, n° 7) |
| même `uuid` deux fois, contenu différent | `uuid_contenu_different` | les deux versions refusées, les autres restaurées |
| même `uuid` deux fois, à l'identique | aucun | la seconde est ignorée |

Les signalements se cumulent (un fichier tronqué n'a plus de pied et signale aussi `fichier_tronque`).

## Nom du fichier

`<code-tournée>-<appareil>-<horodatage>.sursync`, par exemple `COUCHEY-2026-a3f9c2d1-20261014T090507Z.sursync`.

- `<code-tournée>` : le code de la tournée, assaini (accents retirés, tout caractère hors lettres, chiffres, `.`,
  `_`, `-` remplacé par `_`) ; jamais de nom de propriétaire ni d'agent ;
- `<appareil>` : identifiant **opaque** de 8 caractères hexadécimaux, tiré une fois par installation ;
- `<horodatage>` : `yyyyMMddTHHmmssZ`, UTC, **sans `:`**.

Un nom déjà pris n'est **jamais écrasé** : le nouveau reçoit un suffixe (`...Z-2.sursync`, `-3`...). On garde les
5 dernières sauvegardes **de chaque tournée** ; la sixième supprime la plus ancienne de la même tournée, jamais
celle d'une autre.

## Quand et où

- Écrit sous `Documents/Surculus/` du stockage partagé par MediaStore avec `IS_PENDING` (fichier invisible tant
  qu'il n'est pas complet). Android 8 et 9 : dossier public `Documents/Surculus/` (permission
  `WRITE_EXTERNAL_STORAGE`, limitée à l'API 28), nom `.part` renommé une fois complet.
- Déclenché à chaque fin de placette, de constat, de vide ou d'intervention, **et** toutes les **20 saisies** ou
  les **5 minutes** (horloge monotone), au premier des deux (décision de Pascal, 2026-10-21). **Jamais sans saisie
  nouvelle** depuis la dernière sauvegarde.
- Les photos sont écrites dès la prise dans `Pictures/Surculus/<code-tournée>/`, avec leur empreinte SHA-256.

## Restauration

Par le sélecteur de fichier (`ACTION_OPEN_DOCUMENT`). **Union sur l'`uuid`** dans le journal local, jamais
d'écrasement : les lignes locales absentes du fichier restent, une ligne déjà présente à l'identique est ignorée,
la restauration est idempotente. Les lignes gardent leur `appareil` et leur `sequence` d'origine. Un conflit
(deux corrections concurrentes d'une même ligne) reste un conflit.

## Confidentialité

Non chiffré, pour que le successeur et sommieR le lisent. Il porte l'`operateur` de chaque saisie et les
horodatages (traçabilité des saisies, non contrôle de l'agent : ADR 0005).
