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
* Toute autre valeur de `v` (absente, `0`, `3`...) : valeur illisible, ignorée. C'est une **règle nouvelle de la v2** :
  la v1 ne la donne pas, et le lecteur v1 (`ValeurControle.lire`) ne regarde pas `v`.
* **Choix de format** (proposé par le `relecteur` de la PR #150, non tranché par Pascal) : chaque valeur ignorée à la
  lecture est tracée au **journal technique** (`JournalTechnique`), jamais en silence.

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

## Une seule version du format

**Choix de format** (proposé par le `relecteur` de la PR #150, non tranché par Pascal) : une seule version pour tout le
format. `FormatValeurs.VERSION` passe à `2` : Surculus écrit `"v":2` dans **toutes** ses valeurs (`controle`,
`pointage`, `etat`), même si `pointage` et `etat` ne changent pas ; le lecteur accepte `1` et `2`.

## Ce qui sort du journal des saisies

Le refus d'un fix hors de France (ADR 0003, amendement du 2026-11-02, point 6) est écrit au **journal technique**
(`JournalTechnique`, diagnostic), pas au journal des saisies : la liste des types de ligne ne change pas (décision de
Pascal du 2026-11-02).

## Sauvegarde `.sursync`

Un Surculus ancien ne lit pas `v` : il lirait un contrôle v2 **avec** position (la clé en plus est ignorée) et
**ignorerait sans le dire** un contrôle v2 sans position (`position_mesuree_x` à `null`). Décision de Pascal du
2026-11-02 (« nouvelle version ») : dès que le journal écrit des valeurs v2, la sauvegarde passe à la **version
suivante** de son format ; un Surculus ancien la refuse (`version_future`, « mettre l'application à jour »,
[`sursync-v1.md`](sursync-v1.md), « Version ») ; un Surculus à jour lit toujours la version précédente. Le numéro de
cette version n'est pas tranché (spec § 4.8).
