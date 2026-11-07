# Mission 094 — Le référentiel de la source de chaque position mesurée

> Fiche écrite par l'orchestrateur le 2026-11-06, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> après l'essai de bout en bout de Pascal sur la tournée de la Colombière et sa décision du même jour. Depuis `main`
> (7e82a8a). Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Enregistrer au journal, pour chaque position mesurée d'un contrôle, le
référentiel de sa source (`RGF93` si un flux NTRIP Centipède corrigeait le fix, `WGS84` sinon) et l'écrire dans
`referentiel_source` du rendu, pour que la position dise dans quel repère elle a été mesurée, comme l'exige la spec.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Le constat (essai du 2026-11-06)

- Le rendu `tournee-20261106-184318.zip` porte `referentiel_source` NULL dans `positions_controles`, pour une position
  mesurée (précision 3,79 m, source `telephone`).
- La spec l'exige : § 5 (l. 481, `positions_controles`) et § 3 (l. 88) : « **RGF93** (ETRS89) si RTK corrigé par
  Centipède, dont les bases diffusent des coordonnées RGF93 ; **WGS 84** (EPSG:4326) si autonome ». ADR 0003, l. 94 :
  « enregistré tel quel ».
- Aucun code ne le remplit : `PositionMesuree.referentielSource` vaut `null` par défaut
  (`gpkg/src/main/kotlin/io/github/pobsteta/surculus/gpkg/Saisies.kt` l. 82) ; seul `ExportTravaux.kt` l. 241 le lit.
  Le journal v3 (`docs/formats/journal-valeurs-v3.md`) ne porte pas la clé. La 091 a ajouté précision, source et
  « simulée », pas le référentiel.

## Décision de Pascal (2026-11-06)

**« Selon le flux NTRIP »** : `RGF93` si un flux NTRIP Centipède est actif au moment du fix (DGPS, RTK flottant ou
fixe) ; `WGS84` sinon (téléphone, récepteur autonome, DGPS par EGNOS sans NTRIP). Pour une position moyennée, la règle
de la 091 vaut : le plus mauvais des fix (un seul fix `WGS84` → `WGS84`), à confirmer par le plan.

## Décisions de Pascal sur le plan du `dev` (2026-11-06)

1. **DGPS ou RTK sans flux NTRIP vu par Surculus** (NTRIP désactivé, corrections reçues par le récepteur seul, EGNOS,
   caster qui n'est pas Centipède) : `null`, jamais une valeur devinée. Cela remplace « `WGS84` sinon » pour ces cas.
2. **Format** : ajout compatible en v3 ; clé `referentiel_source` à la fin de la valeur du contrôle ; journal et
   `.sursync` restent en v3 ; une valeur sans la clé se lit `null`.
3. **Corrections de plus de 30 s** (fix rétrogradé) : `null`.

Le format est écrit par l'orchestrateur dans `docs/formats/journal-valeurs-v3.md` ; la spec § 3 (l. 88) est amendée.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | à moitié : la règle est décidée, mais savoir au moment du fix si le flux NTRIP est actif (`ConfigRtk.ntrip`, état du service RTK) et le format (clé ajoutée au journal : v4, ou ajout compatible dans la v3 ?) sont à proposer | **tenir** : le `dev` rend un plan d'abord |
| A-t-il les moyens ? | oui | le `testeur` écrit les tests après le plan accepté (le format en dépend) |

**Temps de la main :** tenir : justesse des données et format du journal.

## Ma réponse d'abord

_Rempli par Pascal seul._

## L'Homme

- **Agents :** `dev` (plan d'abord), puis `testeur` (tests depuis la spec, cette fiche et le plan accepté, **sans
  lire** le code), puis `dev` (code).
- **Modèle :** opus.
- **Ce qui ne lui est pas demandé :** transformer une position d'un référentiel à l'autre ; toucher aux photos (leur
  `referentiel_source` suit la même règle : le plan dit s'il est déjà rempli).

## Questions que le plan du `dev` doit proposer (Pascal tranche)

1. Comment Surculus sait, pour chaque fix, qu'un flux NTRIP Centipède le corrigeait (état du service RTK, âge des
   corrections, qualité GGA), et ce qu'il écrit s'il ne le sait pas (NULL plutôt qu'une valeur fausse ?).
2. Le format : nouvelle clé `referentiel_source` dans les valeurs du journal (v4 et `.sursync` v4 selon la règle N-1,
   ou ajout compatible en v3 ?), sa place dans l'ordre des clés, et les journaux anciens (NULL, jamais deviné).
3. Les tests existants qui figent l'ordre ou la liste des clés v3 (grep `"simulee":` et `"repere_epsg"` dans
   `*/src/test/`) : liste exacte, confiée au `testeur`.

Si un test du `testeur` contredit la décision ou un format, **le `dev` s'arrête et le dit tout de suite ; il ne reste
pas muet.**

## Harnais

- **Testeur peut :** tests dans `core/`, `gpkg/`, `data/`, `app/src/test/` ; fixtures (`fixtures/sursync/`) ; vus
  rouges.
- **Dev peut :** ce que son plan nomme et que Pascal accepte.
- **Ne doivent pas :** toucher à `docs/` (l'orchestrateur rédige le format et amende la spec) ; toucher à `main`.
- **Sources autorisées :** spec § 3, § 5, § 6 ; ADR 0003 ; `docs/formats/` ; fiche 091 ; Centipède
  (https://docs.centipede.fr/docs/centipede/4_Systeme2reference.html).

## Valeur provisoire (chiffre, réglage)

- Aucune, sauf si le plan en propose une (âge maximal des corrections, par exemple).

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** 091 (#156, 7e82a8a).
- **PR en brouillon jusqu'à :** la fin du travail du `dev`.
- **CI rouge attendue :** les tests du `testeur`, entre son push et celui du `dev`.

## Tests

- Une position mesurée au téléphone sort avec `referentiel_source` = `WGS84`.
- Un fix corrigé par un flux NTRIP actif (DGPS, RTK flottant, RTK fixe) sort avec `RGF93` ; le même fix sans flux vu
  par Surculus (DGPS par EGNOS, corrections du récepteur seul, corrections de plus de 30 s) sort avec `null`
  (décisions de Pascal du 2026-11-06 sur le plan, ci-dessous).
- Une position moyennée qui mêle les deux sort avec la règle décidée.
- Sans position mesurée : NULL (087). Contrôle d'un journal ancien : NULL, jamais deviné.
- `.claude/test-command` au code 0 ; CI verte, import sommieR vert.

## Terrain

- **Échantillon que Pascal vérifiera :** sur la release, un contrôle de la tournée de la Colombière rendu :
  `referentiel_source` = `WGS84` pour le téléphone.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Qui tranche :** Pascal (plan, format, fusion).

## Comment saura-t-on que ça marche ?

Le rendu d'un contrôle fait au téléphone porte `referentiel_source` = `WGS84`, et un contrôle fait au RTK corrigé par
Centipède porte `RGF93`.
