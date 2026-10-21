# Fixtures de Surculus

Cas d'acceptation de la tranche 1, écrits par le `testeur` à partir de `docs/spec.md`,
des ADR 0002, 0003 et 0005 et de `docs/referentiels/`, sans lire le code. Chaque cas
porte sa **source**. Les règles sur les plants sont dans `regles/plants/` (rejouées en R
par sommieR, brief `vers-sommier`).

## Format

Un fichier = une règle ou une famille de règles :

```json
{
  "fichier": "...", "regle": "...",
  "cas": [
    {"id": "R3", "description": "...", "entree": {...}, "attendu": {...},
     "source": "où la valeur attendue a été prise"}
  ]
}
```

Champs facultatifs d'un cas :

* `tolerance_proposee` : la tolérance de `attendu` (`tolerance_deg`, `tolerance_m`) est une proposition, à confirmer par Pascal.
* `a_recouper_avec_circe_pascal` : valeur de convergence à recouper avec Circé (IGN), par Pascal.
* `note`, `limite` : précisions (par exemple une valeur reprise d'un autre document et non recalculée).
* `copie_de_groma` : cas copié sans modification de `fixtures/numerotation/` de Groma ; la source donne le fichier et le commit.
* `adapte_de_groma` : cas adapté d'un cas de Groma (ici : sans masquage).
* `verifier_que_XYZ_est_absent_de` : le test vérifie que le code inventé « XYZ » est absent du fichier de référentiel indiqué.

Champs facultatifs d'un fichier :

* `unites` : unités des entrées du fichier.
* `tolerance_proposee_deg` : tolérance proposée valable pour tout le fichier.
* `non_reprises` : cas de Groma écartés, avec la raison.
* `decision` : arbitrage de Pascal appliqué au fichier (ici : pas de masquage pour les plants).
* `convention_xy` : convention des coordonnées locales (x vers l'est, y vers le nord géographique, mètres depuis le piquet).

Dans `plants/numerotation.json`, la liste `arbres` de Groma est **renommée `plants`** et placée dans `entree`, avec `parametres`.
Un azimut saisi s'écrit toujours `azimut_mag` et `unite_azimut` (`deg` ou `gr`), comme la couche `plants` (spec § 5).

## Unités

Degrés décimaux, ordre (latitude, longitude) pour le GNSS ; (x = E, y = N) en mètres
pour EPSG:2154 ; distances horizontales ; azimuts en degrés [0, 360[ ou grades [0, 400[
selon l'unité déclarée à l'ouverture de la tournée ; pente en `pct` ou `deg` selon
l'unité déclarée une fois par tournée.

## Provenance des valeurs

* Convergence, conversion WGS 84 vers EPSG:2154 : pyproj (version notée dans la source).
* Déclinaison : calculateur NOAA WMM2025, relevée dans `docs/referentiels/modele-geomagnetique.md`.
* Jour civil : fuseau Europe/Paris (tzdata), changement d'heure du 25 octobre 2026.
* Numérotation : cas copiés de Groma (`fixtures/numerotation/`, commit 6f953e1), sans modification.
* Les listes d'états, de causes et d'essences ne sont pas copiées : les tests les lisent dans `docs/referentiels/`.

## Note pour sommieR (rejeu en R)

* `h_moy_cm` est arrondie à l'entier le plus proche, **demi vers le haut** (T9c : 40 et 45 cm donnent 43). `round()` de R arrondit au pair (`round(42.5)` donne 42) : utiliser `floor(x + 0.5)` pour rejouer ce cas.
* Un cas qui attend `{"refuse": true}` ou `{"accepte": false}` doit être refusé ; le libellé du refus n'est pas fixé.

## Journal des saisies (`journal/`)

Scénarios du journal en ajout seul (ADR 0005, spec § 5), écrits par le `testeur` sans lire le code, pour que la mission 008 ne s'auto-évalue pas. `FixturesBienFormeesTest` les vérifie (forme, familles, identifiants) depuis la mission 008, dont les tests (`:data`) les rejouent. Identifiants `JR-NN`, uniques dans tout `fixtures/`.

### Entrée

* `entree.lignes` : lignes de journal, avec les champs de l'ADR (`uuid`, `objet_uuid`, `type`, `valeur`, `remplace_uuid`, `operateur`, `appareil`, `sequence`, `saisi_le_utc`, `heure_gnss`, `monotone_ms`, `demarrage`, `age_fix_ms`). `age_fix_ms` est l'âge en millisecondes du fix GNSS à la saisie, mesuré sur l'horloge monotone ; son absence vaut nul (âge inconnu, fix inutilisable). Les `uuid` sont symboliques (`U1`, `U2`) ; leur ordre lexicographique tient lieu d'ordre des uuid v7. Les heures sont en ISO 8601 UTC avec `Z` (la base stocke des millisecondes).
* `entree.ordres_d_arrivee` : permutations d'indices de `lignes` ; le résultat attendu doit être identique pour chacune. Absent, l'ordre de `lignes` est l'ordre d'arrivée.
* **Jour civil (`jour-civil.json`), forme à plat** : l'entrée n'a pas de `lignes` mais directement `saisi_le_utc`, `heure_gnss` et `age_fix_ms`. Un `age_fix_ms` absent vaut nul (aucun fix utilisable). Le jour civil se calcule sur l'heure du fix **telle qu'enregistrée** (`heure_gnss`, non corrigée de l'âge), si l'âge est compris entre 0 et 5000 ms inclus ; un âge de 5001 ms ou négatif (fix postérieur à la saisie) est écarté et le jour se calcule sur `saisi_le_utc`.

### Attendu

* `projection` : par clé `"objet|type"` : `courant` (uuid de la ligne montrée, `null` si aucune), `conflit`, `tetes` (lignes actives, dans l'ordre total), `anomalies` (anomalies **de projection**, par clé, par exemple `remplace_inconnu`). En conflit, `courant` est la dernière tête selon l'ordre total. Une annulation agit sur la clé de la ligne annulée. Une clé sans ligne retenue est absente de `projection`.
* `cles` : ensemble des clés que la projection doit contenir, quand le cas le précise.
* `ordre_total` : liste des uuid dans l'ordre (`saisi_le_utc`, `appareil`, `sequence` numérique, `uuid`).
* `lignes_retenues` : nombre de lignes qui entrent dans la projection ; `lignes_ignorees` : uuid des doublons à l'identique ; `lignes_refusees` : uuid dont les deux versions différentes sont refusées.
* `anomalies` (premier niveau) : anomalies **du journal**, hors clé, par exemple `uuid_contenu_different`. À ne pas confondre avec `projection[clé].anomalies`.
* Libellés des anomalies : `remplace_inconnu`, `remplace_autre_objet`, `remplace_autre_type` et `uuid_contenu_different` sont **fixés**, comme codes d'anomalie lus par les tests (ce sont des anomalies signalées, non des textes d'écran) ; décision de l'orchestrateur. Les messages affichés ne sont pas fixés.
* `sauts_notes` (horloge) : liste de `{entre, ecart_s, heure_avant, heure_apres}`. `ecart_s` est signé : variation de `saisi_le_utc` moins variation de `monotone_ms`, entre deux lignes consécutives rangées par `sequence` dans le couple (appareil, démarrage) ; noté si sa valeur absolue dépasse strictement 60 s. `heure_avant` et `heure_apres` sont le `saisi_le_utc` des deux lignes.
* `heures_brutes_reecrites` : toujours `false` ; l'heure brute des lignes n'est jamais réécrite.
* `jour_civil` : jour civil Europe/Paris, `AAAA-MM-JJ`.

### Remplacements anormaux (`remplacements-anormaux.json`, JR-65 à JR-74)

Décisions de Pascal du 2026-10-17 (mission 021). Une saisie dont `remplace_uuid` désigne une ligne **connue** d'un autre objet (`remplace_autre_objet`), ou d'un autre type du même objet (`remplace_autre_type`) : la ligne désignée garde sa valeur, la saisie est tête de sa propre clé, l'anomalie est rattachée à cette clé (comme `remplace_inconnu`). Une annulation qui désigne la ligne d'un autre objet n'annule rien ; elle reste au journal, tête de la clé (objet de l'annulation, `annulation`). Si l'objet et le type diffèrent, seule `remplace_autre_objet` est signalée. Une anomalie de remplacement disparaît quand la saisie anormale est corrigée par une correction normale (JR-70, JR-74) : la nouvelle tête n'est plus douteuse, l'erreur reste tracée au journal (décision de Pascal du 2026-10-17, relecture de la PR #35). JR-71 et JR-72 reprennent la combinaison de JR-08 (même objet pour `pointage` et `etat`), peu réaliste d'après la spec § 5.

### Divers

* `a_confirmer_par_pascal` : cas qui repose sur une déduction non écrite dans la spec (aucun pour l'instant).
* Sources des heures : tzdata Europe/Paris, fin de l'heure d'été le 2026-10-25 à 01:00 UTC.

## Scénario d'export (`export/`)

`export/scenario-couchey.json` (mission 006) est un **scénario de saisies**, non un cas d'acceptation : il est rejoué par l'outil `:gpkg:exporterScenario` (classes de test de `:gpkg`, jamais dans l'APK) sur la tournée de départ de sommieR, pour que l'export de Surculus contienne des contrôles, des plants et des états. La CI de l'import (mission 007) le joue sur la démo Couchey qu'elle régénère à chaque exécution : les placettes y sont désignées **par leur code** (`P35-01`), jamais par un UUID. Les plants sont donnés par leur observation brute (`distance_m` horizontale, `azimut` et `unite_azimut`) ; leur position est dérivée par la chaîne des repères de `:core`. Les UUID de l'export dérivent du scénario (UUID v3 du nom). Les codes d'essence sont validés contre `docs/referentiels/essences.csv`. Les valeurs de `concurrence` et de `besoin` sont celles que l'import de sommieR 0.31.1 accepte (`faible`, `moyenne`, `forte` ; `aucun`).

## Export du GeoPackage (`acceptation-export/`)

Cas d'acceptation de l'export `travaux.gpkg`, écrits par le `testeur` (mission 020) à partir des ADR 0007 et 0011, de la spec (§ 5 et § 6) et des arbitrages du 2026-10-17, sans lire le code de la mission 006. Ils ne sont pas lus par `FixturesBienFormeesTest` ; `ExportAcceptationTest` (`gpkg/src/test/`) les joue, `@Disabled` jusqu'à ce que la 006 branche l'export dans `BranchementExport.kt`. Identifiants `EX-NN`.

* **Entrée** : `tournee_depart` (`repere_epsg`, `placettes` avec `x` et `y` dans le repère, `plants_tournee_precedente`) et `saisies` (`controles`, chacun avec `uuid`, `placette_uuid`, `visite_le` UTC, `jour_civil`, `position`, `plants`). Pas de gpkg binaire : la tournée est décrite par son contenu minimal.
* **`visite_le` et `jour_civil`** : l'instant retenu par le journal et son jour civil Europe/Paris, fournis par l'appelant. L'export ne les corrige pas : un jour civil qui contredit `visite_le` est refusé (`jour_incoherent`, EX-27) ; le jour UTC se lit dans `visite_le`.
* **Identifiants** : tous les `uuid` sont de vrais UUID en minuscules (`00000000-0000-4000-8000-0000000XNNNN`, X = a placette, b plant, c contrôle, d travaux ; NNNN = numéro). Dans les descriptions et notes, `CT-001`, `PLA-01`, `PL-001`, `TV-001` désignent ces UUID par leur lettre et leur numéro (`c0001`, `b0001`…) ; seul EX-34 emploie un identifiant invalide.
* **Attendu** : `{"accepte": true, ...}` avec le contenu attendu, ou `{"accepte": false, "motif": "<code>"}`. Le test compare le contenu, jamais une empreinte du fichier (ADR 0011).
* **Codes de refus** (fixés, comme les codes d'anomalie du journal ; les messages affichés ne le sont pas) : `melange_reperes` (2154 et 9794 dans un même fichier), `controle_en_double` (deux contrôles d'une même placette qui partagent le jour de Paris **ou** le jour UTC ; l'export ne fusionne jamais), `hors_france`, `hors_referentiel` (état, cause ou essence absents de `docs/referentiels/`, ou `code_travaux` absent de la couche `codes` de la tournée), `uuid_en_double` (UUID de plant en double), `uuid_invalide` (identifiant qui n'est pas un UUID, sommieR 0.31.1 `R/valider.R` l. 10), `jour_incoherent` (jour civil fourni qui contredit le jour de Paris de `visite_le` : l'export refuse, il ne corrige pas en silence).
* **Fichiers de référence** : `schema-sommier-0.31.1.json` (relevé du modèle de sommieR), `schema-couches-ajoutees.json` (spec § 5 ; classes de type proposées), `valeurs-permises.json`.
* **Variantes** : `entrees_variantes` (liste de `{"uuid_controle": ...}`) rejoue l'`entree` avec l'UUID du premier contrôle remplacé tel quel ; chaque variante doit être refusée avec le motif attendu (EX-34d : majuscules, espaces).
* **Champs facultatifs d'un cas** : `hypothese` (le cas suppose une règle non écrite, à confirmer par Pascal) ; `operation: "lecture"` (cas de relecture, joué par `ExportTournee.relire` : EX-11) ; `saisies.travaux` (travaux saisis, avec leur `code_travaux`, contrôlés contre la couche `codes` de `tournee_depart.codes` : EX-33).
* **Trous voulus dans la numérotation** : EX-28 (date de travaux antérieure à la plantation) et EX-32 (export sans réseau) ont été retirés par Pascal le 2026-10-17 ; ne pas les réattribuer.
* **Non couverts** : critère de CI (a) de l'ADR 0007 (import R, CI de la 007) ; fumée de l'APK sur émulateur ; essai sur ARM.

## Dictée (`fixtures/dictee/`, mission 025)

Cas d'acceptation de la mission 013, écrits depuis `docs/spec.md` § 4.3, l'ADR 0004 et les décisions de Pascal
du 2026-10-21 (`pilotage/journal.md`), **sans lire de code**. Identifiants `DI-NN`. Même format que ci-dessus ; l'entrée est
`phrase` (texte rendu par Vosk : minuscules, sans ponctuation), le résultat `{"accepte": bool, "commande": {...}}`.

| Fichier | Contenu |
|---|---|
| `phrases.json` | les 17 phrases de la grammaire et leur commande (types : `etat`, `cause`, `detail`, `hauteur`, `suivant`, `regarni`, `vide_debut`, `vide_fin`, `annule`) |
| `hauteur.json` | « hauteur » de 1 à 400 cm, bornes et refus (0, 401, sans nombre, deux nombres) |
| `refus.json` | phrases hors grammaire, deux commandes dans une phrase, casse, ponctuation, accents, détails hors grammaire |
| `contexte.json` | la commande face à l'état courant du plant (« abrouti » sur sain ou atteint seulement, cause refusée sur sain et absent...) |
| `vocabulaire.json` + `vocabulaire-vosk-model-small-fr-0.22.txt` | liste des 135 774 mots du modèle (un par ligne) et cas « mot dans le vocabulaire » |
| `modele.json` | nom du modèle et empreinte SHA-256 vérifiés (contenu d'essai de quelques octets, jamais le modèle) |

* Le modèle `vosk-model-small-fr-0.22` n'a **pas** de `graph/words.txt` : la table des mots est incorporée à `graph/Gr.fst` (table de symboles OpenFst). La liste versionnée en est extraite ; le modèle lui-même n'est pas versionné.
* Correspondance mot dicté vers `cause_groupe` (proposée par le `testeur`, à confirmer par Pascal) : insectes = entomologique, champignon = pathologique, animaux = animaux, abiotique et indéterminé = abiotique_complexe_indetermine.
* **Trois mots de la grammaire sont absents du modèle** : « abiotique », « abrouti », « regarni » (`mots_absents_du_modele` dans `vocabulaire.json`). Le test de la 013 « chaque mot dans le vocabulaire » ne peut passer qu'après décision de Pascal.
* Tests : `core/src/test/kotlin/.../core/dictee/`. `DicteeFixturesBienFormeesTest` est actif (cas bien formés, codes dans les référentiels, vocabulaire). `DicteeRejeuTest` est `@Disabled("en attente de la mission 013")` : la 013 branche `ImplementationDictee.dictee()` (port `Dictee`) et retire `@Disabled`, sans modifier les cas.
