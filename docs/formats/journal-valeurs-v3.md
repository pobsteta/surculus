# Format de la `valeur` des lignes du journal, version 3

Décisions de Pascal du 2026-11-04 (mission 091, « v3, tests ajustés », « À l'écran, obligatoire », « D'accord » pour
la qualité du fix). Ce document ne décrit que **ce qui change** par rapport à la
[version 2](journal-valeurs-v2.md), elle-même écrite par rapport à la [version 1](journal-valeurs-v1.md) : un lien
plutôt qu'une copie.

Pourquoi une v3 : un contrôle sans `besoin` fait passer sa placette « à jour » dans sommieR 0.31.1
(`R/travaux_qfield.R` l. 335-337, tag v0.31.1), et la position mesurée d'un contrôle ne disait ni sa précision ni sa
source. La v3 ajoute ces champs. Comme la v2, elle change de numéro pour qu'un Surculus ancien ne les perde pas **sans
le dire** (voir « Sauvegarde `.sursync` »).

## Lecture : Surculus relit les v1, v2 et v3

* `"v":1` et `"v":2` : la valeur se lit selon la v1 ou la v2, à l'identique ; `besoin`, `concurrence`, `precision_m`,
  `source_gnss` et `simulee` y valent **`null`** (« on ne sait pas »), **jamais** une valeur par défaut.
* `"v":3` : la valeur se lit selon ce document.
* Toute autre valeur de `v` (absente, `0`, `4`...) : illisible, ignorée et tracée au journal technique (règle de la v2).

Le journal ne suit pas la règle *N*-1 de la sauvegarde : un journal sur un téléphone porte des valeurs v1, v2 et v3
mêlées, et aucune ne doit être perdue (plan de la 091, risque « journaux déjà sur les téléphones »).

## `controle` en v3

Les clés de la v2, plus cinq. `precision_m`, `source_gnss` et `simulee` se placent juste après
`sans_position_mesuree` ; `besoin` et `concurrence` à la fin de la valeur. La mission 094 ajoute une sixième clé,
`referentiel_source`, **en dernier**, après `concurrence` (ajout compatible, sans changer de version : décision de
Pascal du 2026-11-06).

| Clé | Type | Sens |
|---|---|---|
| `v` | entier | `3` |
| `precision_m` | réel > 0, ou `null` | précision horizontale de la position mesurée, en mètres : la **plus mauvaise** des fix moyennés à l'arrivée (une moyenne ne vaut pas mieux que son pire fix) ; `null` si un fix n'en donne pas (récepteur sans trame GST) ou sans position mesurée ; **jamais 0** |
| `source_gnss` | texte, ou `null` | `telephone`, `recepteur_autonome`, `recepteur_dgps`, `recepteur_rtk_flottant`, `recepteur_rtk_fixe` : la plus mauvaise source des fix moyennés, après rétrogradation d'une correction trop vieille (indicateur de qualité GGA, NMEA 0183, lu par `core/.../gnss/Nmea.kt`) ; les qualités GGA 3 (PPS), 6 (estimée), 7 (manuelle) et 8 (simulation) comptent `recepteur_autonome`, et une moyenne qui mêle des fix du téléphone et du récepteur compte `telephone`, pour ne jamais dire une source meilleure qu'elle n'est (règle ratifiée par Pascal le 2026-11-04) ; `null` sans position mesurée |
| `simulee` | booléen, ou `null` | vrai si un fix moyenné est marqué simulé par Android (`Location.isMock`) ou porte la qualité GGA 8 ; `null` sans position mesurée |
| `besoin` | texte, ou `null` | le travail qui s'impose : un code de la couche `codes` de la tournée, ou `aucun` (sommieR 0.31.1, `R/registre6.R` l. 137-138) ; obligatoire pour terminer un contrôle (décision de Pascal du 2026-11-04) |
| `concurrence` | texte, ou `null` | `faible`, `moyenne` ou `forte` ([`docs/referentiels/concurrences.csv`](../referentiels/concurrences.csv), codes de sommieR 0.31.1, `R/registre6.R` l. 64) ; obligatoire pour terminer |
| `referentiel_source` | texte, ou `null` | référentiel de la source de la position mesurée (spec § 3, ADR 0003) : `WGS84` pour un fix du téléphone ou d'un récepteur en qualité GGA autonome (1, et 3, 6, 7, 8) ; `RGF93` pour un fix RTK (flottant, fixe) corrigé par un flux NTRIP **Centipède vu par Surculus** (dernier message RTCM d'au plus 30 s, `PolitiqueFix.ageMaxCorrectionsS`) ; **`null`** quand on ne sait pas : tout fix DGPS, même avec un flux Centipède vu (il peut se corriger par EGNOS) ; RTK sans flux vu par Surculus (NTRIP désactivé, corrections du récepteur seul, EGNOS), corrections de plus de 30 s ou d'âge inconnu (champ 13 de la trame GGA vide), caster qui n'est pas Centipède (reconnu par son nom, `crtk.net` ou `caster.centipede.fr`, jamais par son adresse IP), et sans position mesurée. Position moyennée : un seul fix `null` donne `null`, sinon un seul `WGS84` donne `WGS84`, sinon `RGF93` (décisions de Pascal du 2026-11-06, mission 094) |

**Cohérence**, vérifiée à la lecture ; une valeur incohérente ou hors domaine est illisible, ignorée et tracée, jamais
complétée (proposition du `testeur` de la 091, retenue ici) :

* les règles de cohérence de la v2 tiennent ;
* `sans_position_mesuree` vrai ⇒ `precision_m`, `source_gnss`, `simulee` et `referentiel_source` valent `null` ;
* `precision_m` présente ⇒ strictement positive ;
* `source_gnss`, `concurrence`, `referentiel_source` hors de leur domaine ⇒ illisible (`referentiel_source` : exactement
  `RGF93` ou `WGS84`, sans espace) ;
* `referentiel_source` présent sans `source_gnss` ⇒ illisible (la qualité du fix se dit en entier, 091) ;
* une valeur v3 écrite avant la 094, sans la clé `referentiel_source`, se lit avec `null`, jamais une valeur devinée ;
  un Surculus d'avant la 094 ignore la clé et rend `null`, sans rien perdre du reste ;
* `besoin` est un texte non vide sans espaces autour ; son appartenance à la couche `codes` se vérifie à la saisie et à
  l'export (`hors_referentiel`), pas à la lecture du journal.

Un contrôle peut être écrit en v3 sans `besoin` ni `concurrence` (`null`) tant qu'il n'est pas terminé ; un contrôle
**terminé** sans eux est refusé au rendu : « P35-01 : contrôle sans besoin ni concurrence : l'annuler et le refaire »
(décision de Pascal du 2026-11-04).

Exemple, contrôle terminé avec une position mesurée par un récepteur RTK fixe corrigé par Centipède :

```json
{"v":3,"placette_uuid":"11111111-1111-4111-8111-111111111111","code_placette":"P35-01","jour_civil":"2026-11-03","operateur":"Agent d'essai","termine":true,"visite_le_utc_ms":1793706400000,"position_x":846164.76,"position_y":6686123.26,"position_mesuree_x":846165.36,"position_mesuree_y":6686122.86,"rattache_par_identite":false,"sans_position_mesuree":false,"precision_m":0.02,"source_gnss":"recepteur_rtk_fixe","simulee":false,"repere_epsg":2154,"numeros":{"019...a":1,"019...b":2},"besoin":"aucun","concurrence":"faible","referentiel_source":"RGF93"}
```

Les positions restent en Lambert-93, dans le repère de la tournée (`repere_epsg`) : x = Est, y = Nord, en mètres.

## Ce qui ne change pas

* Les types de ligne, `pointage`, `etat`, `annulation` : v1 et v2. Surculus écrit `"v":3` dans **toutes** ses valeurs
  (une seule version pour tout le format, choix de la v2).

## Sauvegarde `.sursync`

Un Surculus de la v2 lirait un contrôle v3 en ignorant les clés qu'il ne connaît pas : il perdrait le `besoin` sans le
dire, puis rendrait la placette « à jour ». Dès que le journal écrit des valeurs v3, la sauvegarde passe donc en **v3**
([`sursync-v3.md`](sursync-v3.md)), qu'un Surculus v2 refuse (`version_future`, « mettre l'application à jour »).
