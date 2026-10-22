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
* **Champs facultatifs d'un cas** : `hypothese` (un texte qui énonce la règle supposée, non écrite, à confirmer par Pascal ; jamais un booléen) ; `operation: "lecture"` (cas de relecture, joué par `ExportTournee.relire` : EX-11) ; `saisies.travaux` (travaux saisis, avec leur `code_travaux`, contrôlés contre la couche `codes` de `tournee_depart.codes` : EX-33).
* **Trous voulus dans la numérotation** : EX-28 (date de travaux antérieure à la plantation) et EX-32 (export sans réseau) ont été retirés par Pascal le 2026-10-17 ; ne pas les réattribuer.
* **Non couverts** : critère de CI (a) de l'ADR 0007 (import R, CI de la 007) ; fumée de l'APK sur émulateur ; essai sur ARM.

## Sauvegarde `.sursync` (`sauvegarde/`)

Cas d'acceptation de la sauvegarde et de sa restauration, écrits par le `testeur` (mission 024) à partir de la spec (§ 4.8), de l'ADR 0005 et des décisions de Pascal du 2026-10-21 (`pilotage/journal.md`), sans lire le code, pour que la mission 009 ait ses tests avant son code. `FixturesSauvegardeBienFormeesTest` (`:core`) vérifie leur forme ; la 009 les rejoue sans les modifier. Identifiants `SV-NN`.

* **Familles** : `restauration.json` (SV-01 à 08), `versions.json` (10 à 13), `fichiers-refuses.json` (20 à 25), `noms-fichier.json` (30 à 34), `ecrasement.json` (40 à 42), `declenchement.json` (50 à 55). Les numéros 09, 14 à 19, 26 à 29, 35 à 39, 43 à 49 sont des trous voulus : ne pas les réattribuer sans le dire.
* **`journal_de`** : `fichier#id`, le journal d'entrée est celui du cas `JR-NN` cité de `fixtures/journal/`. `variantes` rejoue le cas avec chaque variante.
* **Opérations** : `sauvegarder_puis_restaurer`, `restaurer_deux_fois`. `alteration` décrit comment le fichier sauvegardé est abîmé avant restauration.
* **Attendu** : `{"accepte": true, ...}` (fichier pris, lignes saines restaurées, éventuels `signalements`) ou `{"accepte": false, "motif": "<code>", "journal_local_inchange": true}` (refus en entier).
* **Fichier abîmé : on garde les lignes saines** (décision 5). Un `.sursync` tronqué, à ligne illisible, à empreinte fausse ou à uuid en double au contenu différent n'est jamais refusé en entier : les lignes valides sont restaurées, les lignes fautives refusées et signalées, comme dans le journal (JR-13, JR-14). Seule une version N+1, ou une version illisible (y compris un fichier de 0 octet), fait refuser le fichier.
* **Codes** (fixés par l'orchestrateur, lus par les tests ; les messages affichés ne le sont pas) : refus du fichier `version_future`, `version_illisible` ; signalements `fichier_tronque`, `ligne_illisible`, `empreinte_differente` (avertissements) et `uuid_contenu_different` (anomalie du journal, JR-13, jamais un motif de refus).
* **Décisions de Pascal du 2026-10-21** (`pilotage/journal.md`) : 1, l'`<appareil>` du nom est un identifiant opaque (segment non vide, sans `:` ni nom de personne, forme libre) ; 2, le `.sursync` porte une empreinte SHA-256 de son contenu (la 009 l'ajoute au format, l'ADR 0005 sera complétée), une empreinte fausse est signalée sans refus du fichier ; 3, un fichier de même nom n'est jamais écrasé (le nouveau est suffixé) ; 4, la rotation des 5 dernières ne supprime que la plus ancienne de la même tournée ; 5, voir ci-dessus ; 6, pas de sauvegarde sans saisie nouvelle, même après M minutes.
* **N et M** : paramètres non fixés (valeurs de la 009, à mesurer) ; les cas les écrivent `N`, `M`.
* **Non couverts** : le format binaire (`docs/formats/sursync-v1.md`, affaire de la 009) ; les trois voies hors appareil (010) ; SV-34 (MediaStore `IS_PENDING`) ne se vérifie que sur appareil ou émulateur ; pas de test Kotlin désactivé, l'interface attendue ne se décrit pas sans lire le code.

## Exemples du format `.sursync` (`fixtures/sursync/`, mission 009)

Exemples du format publié dans `docs/formats/sursync-v1.md`, tirés du journal `fixtures/journal/projection.json#JR-02` (écrits par la mission 009, rejoués par `FormatEtServiceTest`) : `exemple-v1.jsonl` (contenu décompressé, lisible), `exemple-v1.sursync` (le même, en gzip), `exemple-v1-tronque.sursync` (10 derniers octets coupés), `exemple-v1-empreinte-fausse.sursync` (une valeur changée, pied inchangé). Les cas d'acceptation, eux, sont dans `sauvegarde/`.

## Dictée (`fixtures/dictee/`, mission 025)

Cas d'acceptation de la mission 013, écrits depuis `docs/spec.md` § 4.3, l'ADR 0004 et les décisions de Pascal
du 2026-10-21 (`pilotage/journal.md`), **sans lire de code**. Identifiants `DI-NN`. Même format que ci-dessus ; l'entrée est
`phrase` (texte rendu par Vosk : minuscules, sans ponctuation), le résultat `{"accepte": bool, "commande": {...}}`.

| Fichier | Contenu |
|---|---|
| `phrases.json` | les 17 phrases de la grammaire et leur commande (types : `etat`, `cause`, `detail`, `hauteur`, `suivant`, `regarni`, `vide_debut`, `vide_fin`, `annule`) |
| `hauteur.json` | « hauteur » de 1 à 400 cm, formes à traits d'union et à espaces (« quarante-cinq », « quarante cinq »), bornes et refus (0, 401, sans nombre, deux nombres) |
| `refus.json` | phrases hors grammaire (dont les anciens mots « abiotique », « abrouti », « regarni »), deux commandes dans une phrase, casse, ponctuation, accents, détails hors grammaire |
| `contexte.json` | la commande face à l'état courant du plant (« mangé » sur sain, atteint ou mort, refusé sur absent, non observé ou sans état ; cause refusée sur sain et absent ; « remplace » accepté sur mort ou absent seulement ; correction d'un état déjà saisi...) |
| `normalisation.json` | espaces en tête, en queue ou doublés : normalisés avant l'analyse ; espaces seuls : refusés (`refus.json`) |
| `vocabulaire.json` + `vocabulaire-vosk-model-small-fr-0.22.txt` | liste des 135 774 symboles du modèle (un par ligne ; six ne sont pas des mots : `<eps>`, `#0`, `<s>`, `</s>`, `!SIL`, `[unk]`, soit 135 768 mots) et cas « mot dans le vocabulaire » |
| `modele.json` | nom du modèle et empreinte SHA-256 vérifiés (contenu d'essai de quelques octets, jamais le modèle) |

* Le modèle `vosk-model-small-fr-0.22` n'a **pas** de `graph/words.txt` : la table des mots est incorporée à `graph/Gr.fst` (table de symboles OpenFst). La liste versionnée en est extraite ; le modèle lui-même n'est pas versionné. Les 135 774 entrées comptent six symboles FST qui ne sont pas des mots : `<eps>`, `#0`, `<s>`, `</s>`, `!SIL`, `[unk]`.
* **Régénérer la liste** (test du successeur ; bibliothèque standard de Python seule ; recoupée octet pour octet par le relecteur le 2026-10-21) : `python3 -I fixtures/dictee/outils/extraire_vocabulaire.py vosk-model-small-fr-0.22.zip > fixtures/dictee/vocabulaire-vosk-model-small-fr-0.22.txt`. L'archive vient de `https://alphacephei.com/vosk/models/vosk-model-small-fr-0.22.zip` (SHA-256 `cabf6180e177eb9b3a9a9d43a437bd5e549f3a7d09525e5d69a3fed787be12ad`, 42 233 323 octets, confirmé par un second téléchargement) ; le script écrit le décompte et le SHA-256 sur la sortie d'erreur.
* Correspondance mot dicté vers `cause_groupe` : choix technique de l'orchestrateur (`pilotage/journal.md`, 2026-10-21, « Choix techniques » des missions 024 et 025), selon `docs/referentiels/etats-plants.yaml` : insectes = entomologique, champignon = pathologique, animaux = animaux, climat et indéterminé = abiotique_complexe_indetermine.
* **Normalisation** : choix technique de l'orchestrateur (`pilotage/journal.md`, 2026-10-21, inscription « Choix technique, omis à l'inscription précédente ») : la grammaire reçoit le texte de Vosk en minuscules, sans ponctuation ; les espaces en tête, en queue ou doublés sont normalisés ; toute autre forme (majuscule, ponctuation, accent faux) est refusée.
* **« remplace »** (décision n° 9 de Pascal, 2026-10-21) : refusé sans plant courant mort ou absent (spec § 6 : un regarni remplace un plant mort ou absent). **Correction** (décision n° 10) : dicter un nouvel état sur un plant qui en a déjà un dans le contrôle est une correction du journal (`remplace_uuid`), annoncée par la voix ; l'attendu porte alors `"correction": true` et `"annonce_vocale": true` (`false` pour une première saisie).
* **Mots remplacés** (décision n° 5 de Pascal, 2026-10-21) : « abiotique » devient « climat », « abrouti » « mangé », « regarni » « remplace », les anciens étant absents du modèle. Chaque mot de la grammaire est maintenant dans le vocabulaire : plus aucune absence consignée.
* « mangé » porte le détail « abroutissement » sur un plant sain, atteint ou mort, refusé sur absent, non observé ou sans état (spec § 6, décision n° 4 corrigée).
* Le SHA-256 de l'archive n'est pas inscrit dans `docs/dependances.md` par la 025 : la 013 le recoupera sur un second téléchargement (le test désactivé `l'empreinte du modèle est inscrite` l'attend).
* Tests : `core/src/test/kotlin/.../core/dictee/`. `DicteeFixturesBienFormeesTest` est actif (cas bien formés, codes dans les référentiels, vocabulaire). `DicteeRejeuTest` est `@Disabled("en attente de la mission 013")` : la 013 branche `ImplementationDictee.dictee()` (port `Dictee`) et retire `@Disabled`, sans modifier les cas.

## Registre attendu du scénario Couchey

`export/registre-attendu-couchey.json` (mission 027) donne, pour chaque contrôle de
`export/scenario-couchey.json`, les valeurs que le registre doit contenir après import :
`nb_total`, `nb_vivants`, `nb_abroutis`, `h_moy_cm`, `visite_le`, la placette par son code
et le point en WGS 84 (`lon`, `lat`, `tolerance_m` = 0,5 m, décision de Pascal du
2026-10-21). Les valeurs sont écrites à la main depuis la spec, chacune avec son calcul ;
les coordonnées des placettes viennent de sommieR (`R/demo.R` l. 363-364, commit fd92078, sommieR 0.31.1 (DESCRIPTION)),
hypothèse : la CI importe cette version. Chaque point doit tomber dans le rectangle de `docs/spec.md` § 10, point 9 (domaine d'usage d'EPSG:2154).
La comparaison à l'export relève de la mission 028.

## Copies voulues de l'emprise de la France

Ajouté le 2026-10-22. Le rectangle du domaine d'usage d'EPSG:2154 existe à plusieurs endroits, **voulus** :

* `core/.../core/geo/PointLambert93.kt` : `EmpriseLambert93.FRANCE_METROPOLITAINE` (Lambert-93, mètres) et
  `EmpriseDegres.FRANCE_METROPOLITAINE` (degrés, `BBOX[41.15,-9.86,51.56,10.38]`). La source (registre EPSG, `projinfo`)
  est écrite dans le commentaire du code, qui fait foi ;
* `fixtures/acceptation-export/repere-et-france.json` : **recopie les bornes en Lambert-93** (l. 245 et suivantes, avec
  leur source epsg.io). Elles **ne sont pas identiques** à celles du code pour y min : 6 005 294,71 dans le code
  (PROJ, `densify_pts=50`) contre 6 005 281,2 dans la fixture (epsg.io, densify 100), soit 13,5 m d'écart. Cause
  probable : la source et la densification différentes. Question ouverte, à trancher par Pascal (bac à sable du kanban) ;
* `fixtures/export/registre-attendu-couchey.json` (l. 19, mission 027) : **recopie le rectangle en degrés**
  (41,15 ; 51,56 ; -9,86 ; 10,38), avec sa note de source ;
* `fixtures/regles/emprise-et-france.json` : des cas (Brest, etc.) qui **ne recopient pas le rectangle** ; ils vérifient
  qu'un point français y tombe et qu'un point hors de France n'y tombe pas.

Si le rectangle change (contour officiel ADMIN-EXPRESS, spec § 10 question 9), on modifie le code, puis les deux recopies :
`repere-et-france.json` et `registre-attendu-couchey.json`.
