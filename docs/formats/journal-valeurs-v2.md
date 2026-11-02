# Format de la `valeur` des lignes du journal, version 2

Décision de Pascal du 2026-11-02 (ADR 0003, « Amendement du 2026-11-02 : ouvrir un contrôle sans position », point
8 ; mission 085 pour l'écrit, 086 pour le code). Ce document ne décrit que **ce qui change** par rapport à la
[version 1](journal-valeurs-v1.md), qui reste la référence pour tout le reste (règles communes, `pointage`, `etat`,
`annulation`, codes de refus) : un lien plutôt qu'une copie.

Pourquoi une v2 : en v1, `position_mesuree_x` et `position_mesuree_y` sont obligatoires ; une valeur où elles
manquent est **illisible et ignorée** (v1, « Règles communes »). Un contrôle ouvert sans aucun fix, écrit en v1, serait
donc perdu au relu (relevé du `contradicteur` du 2026-11-02).

## Lecture : Surculus relit toujours la v1 (principe N-1)

* `"v":1` : la valeur se lit selon la v1, à l'identique ; `sans_position_mesuree` y vaut **faux** (la clé n'existe
  pas en v1).
* `"v":2` : la valeur se lit selon ce document.
* Toute autre valeur de `v` : valeur illisible, ignorée (règle commune de la v1).

Comme pour `.sursync` (`docs/formats/sursync-v1.md`), le lecteur de la version N lit la version N-1.

## `controle` en v2

Les clés de la v1, dans le même ordre, plus une : `sans_position_mesuree`, placée juste après
`rattache_par_identite`.

| Clé | Type | Sens en v2 (seules les lignes qui changent) |
|---|---|---|
| `v` | entier | `2` |
| `position_x`, `position_y` | réel | position du contrôle : celle du piquet si rattaché par identité **ou sans position mesurée** (position de la placette créée par sommieR, `placettes` de la tournée), la position mesurée sinon |
| `position_mesuree_x`, `position_mesuree_y` | réel, ou `null` | position GNSS mesurée (moyennée) ; **`null` si et seulement si `sans_position_mesuree` est vrai** |
| `rattache_par_identite` | booléen | vrai si le code lu a désigné la placette et que la position mesurée ne l'a pas fait : au-delà de 15 m (comme en v1), **et toujours quand `sans_position_mesuree` est vrai** (définition élargie, décision de Pascal du 2026-11-02) |
| `sans_position_mesuree` | booléen, obligatoire | vrai quand aucun fix n'a été reçu et que le contrôle a été ouvert sur le code lu, après confirmation de la placette (ADR 0003, amendement du 2026-11-02, points 1 à 5) |
| `visite_le_utc_ms` | entier | inchangé ; sans fix, c'est l'heure du téléphone (v1 : « l'heure GNSS si le fix a au plus 5 s, sinon l'heure du téléphone ») |

**Cohérence**, vérifiée à la lecture ; une valeur incohérente est illisible et ignorée, jamais complétée :

* `sans_position_mesuree` vrai ⇒ `position_mesuree_x` et `position_mesuree_y` valent `null`, et
  `rattache_par_identite` est vrai ;
* `sans_position_mesuree` faux ⇒ `position_mesuree_x` et `position_mesuree_y` sont des réels (comme en v1).

Exemple, contrôle terminé ouvert sans position :

```json
{"v":2,"placette_uuid":"11111111-1111-4111-8111-111111111111","code_placette":"P35-01","jour_civil":"2026-11-02","operateur":"Agent d'essai","termine":true,"visite_le_utc_ms":1793620000000,"position_x":846164.76,"position_y":6686123.26,"position_mesuree_x":null,"position_mesuree_y":null,"rattache_par_identite":true,"sans_position_mesuree":true,"repere_epsg":2154,"numeros":{"019...a":1,"019...b":2}}
```

Le même contrôle avec une position mesurée à 3 m du piquet s'écrit comme en v1, avec `"v":2` et
`"sans_position_mesuree":false`.

Comme en v1, le code qui écrit et lit ces valeurs est unique (`core/.../core/session/Valeurs.kt`) et ses tests
rejouent chaque exemple de ce document (mission 086).

## Ce qui ne change pas

* Les types de ligne, `pointage`, `etat`, `annulation` et les codes de refus : v1.
* La position portée par le contrôle reste en Lambert-93, dans le repère de la tournée (`repere_epsg`).

## Points à trancher (question ouverte de la mission 085, non décidés)

1. **Version des autres valeurs** : `pointage` et `etat` ne changent pas. Surculus les écrit-il en `"v":2` (une seule
   version pour tout le format, plus simple à relire) ou garde-t-il `"v":1` (seul `controle` change) ?
2. **Refus « hors de France » écrit au journal** (ADR 0003, amendement, point 6) : la liste des types de ligne est
   fermée (`controle`, `pointage`, `etat`, `annulation`, `saut_horloge`, et `photo` en `.sursync` v2). Quel type porte
   ce refus, et avec quelle valeur ?
3. **Ancien Surculus** : le lecteur v1 (`ValeurControle.lire`, `core/.../core/session/Valeurs.kt`) ne regarde pas `v`.
   Il lit un contrôle v2 **avec** position (la clé en plus est ignorée), mais **ignore sans le dire** un contrôle v2
   sans position (`position_mesuree_x` à `null`). Une sauvegarde `.sursync` restaurée sur un téléphone resté à une
   ancienne version perdrait ces contrôles. Faut-il une nouvelle version de `.sursync`, pour qu'un ancien Surculus
   refuse le fichier au lieu de perdre des lignes ?
