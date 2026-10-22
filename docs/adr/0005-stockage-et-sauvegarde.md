# ADR 0005 — Stockage et sauvegarde : journal en ajout seul, durable, sauvegardé hors de l'appareil par trois voies

- **Date :** 2026-10-15
- **Statut :** acceptée par Pascal (2026-10-15), après critique du `contradicteur`
- **Proposée par :** Claude (orchestrateur)
- **Tranchée par :** Pascal

## La décision en une phrase

Chaque saisie est une ligne ajoutée à un journal Room durable (`synchronous=FULL`,
jamais modifiée ni effacée, sans migration destructive) ; Surculus en écrit une
sauvegarde `.sursync` dans le stockage partagé (MediaStore) à chaque fin d'unité de
travail et au moins toutes les 20 saisies ou 5 minutes, et la fait sortir de l'appareil
automatiquement par trois voies — carte SD ou clé USB, téléphone du binôme, envoi
quand le réseau passe ; les photos sont écrites dès la prise dans le stockage partagé ;
à l'ouverture d'une journée, Surculus signale sans bloquer les saisies jamais sorties.

## Contexte

Brief : « le journal des saisies est sauvegardé hors de l'appareil à chaque fin de
placette ; une tournée ne se perd pas avec le téléphone ». Le bénéficiaire est le
gestionnaire, qui attend la tournée dans sommieR, et l'agent, qui devrait tout
ressaisir.

**Erreur corrigée** (l'orchestrateur s'était trompé) : la première version écrivait la
sauvegarde sur le même téléphone, ce qui ne tient pas la promesse du brief, et
présentait le journal de Marculus comme un journal immuable, ce qu'il n'est pas (« la
dernière écriture gagne », `MergeDao.kt`) : rejeu, corrections et conflits sont du code
nouveau.

## Menaces et parades

| Menace | Parade | Protégé ? |
|---|---|---|
| Batterie, coupure | transaction par saisie, `PRAGMA synchronous=FULL` (WAL) | oui, saisie validée durable |
| Base corrompue, migration ratée | `exportSchema = true`, schémas versionnés, chaque migration testée, **aucun repli destructif** | oui |
| Désinstallation | `.sursync` et photos dans le stockage partagé (MediaStore) | oui |
| Téléphone cassé | copie automatique sur **carte SD ou clé USB** (dossier choisi une fois) | oui si une carte ou une clé est en place |
| Téléphone perdu | envoi au **téléphone du binôme** quand il est à portée ; **envoi quand le réseau passe** vers un serveur WebDAV choisi par l'employeur | oui si l'une des deux voies a abouti |
| Aucune voie n'a abouti | signalé à l'ouverture de la journée, sans bloquer | non : risque dit et inscrit |

## Règles

| Sujet | Règle |
|---|---|
| Journal | une ligne par saisie : `uuid` (v7, ordonné), `objet_uuid`, `type` (dont `annulation`), `valeur`, `remplace_uuid`, `operateur`, `appareil`, `sequence` (compteur local monotone), `saisi_le_utc`, `heure_gnss` (dernier fix, si connu), `monotone_ms`, `demarrage` et `age_fix_ms` (horloge monotone, numéro de démarrage, âge du fix GNSS au moment de la saisie mesuré sur l'horloge monotone ; nul si aucun fix, ajoutés le 2026-10-17, arbitrage de Pascal sur le plan 008) ; `code_tournee` (tournée de la saisie ; `''` : ligne non rattachée, antérieure à la version 3 de la base, gardée au journal mais absente de toute sauvegarde ; mission 031, décision de Pascal du 2026-10-21) ; aucune mise à jour ni suppression, garanti par le DAO et par des déclencheurs SQLite |
| Projection | l'état courant se calcule par clé (`code_tournee`, `objet_uuid`, `type`) : un état n'efface pas un pointage, et une autre tournée n'influe jamais (mission 032, décision de Pascal du 2026-10-21) ; `saisir` refuse un code de tournée vide ; table de projection reconstructible depuis le journal, pas de rejeu à chaque geste |
| Ordre total | départage déterministe par (`saisi_le_utc`, `appareil`, `sequence`, `uuid`) ; deux corrections concurrentes d'une même ligne sont **signalées comme conflit**, pas résolues en silence |
| Temps | jour civil en **Europe/Paris** (règle « un contrôle par placette et par jour », `visite_le`) ; durées mesurées sur l'horloge monotone (`elapsedRealtime`) ; un saut d'horloge (plus de 60 s d'écart à l'horloge monotone dans un même démarrage) est détecté et noté, l'heure brute n'est jamais réécrite ; le jour civil d'un contrôle se calcule sur l'heure GNSS quand le fix a au plus 5 s (`age_fix_ms`, horloge monotone ; fix d'âge négatif ignoré), sinon sur l'heure du téléphone (arbitrages de Pascal du 2026-10-17, colonne `age_fix_ms` validée par lui) |
| Sauvegarde | à chaque fin de placette, de constat, de vide ou d'intervention, **et** toutes les **N = 20 saisies** ou les **M = 5 minutes** (au premier des deux ; décision de Pascal, 2026-10-21) ; écrite par **MediaStore avec `IS_PENDING`** sous `Documents/Surculus/` (pas le SAF, que Marculus a abandonné : fichier vu à 0 octet par câble) ; sous Android 8-9, qui n'a pas de MediaStore pour les documents : écriture dans le dossier public `Documents/Surculus/` (`WRITE_EXTERNAL_STORAGE` limitée à l'API 28, nom `.part` renommé une fois complet), pour que la sauvegarde survive aussi à la désinstallation (un repli dans le dossier privé de l'application ne le ferait pas ; validé par Pascal, 2026-10-21 ; la demande de `WRITE_EXTERNAL_STORAGE` à l'exécution sur les API 26 à 28 est une dette de la mission qui branchera la sauvegarde sur les saisies) ; 5 dernières gardées par tournée |
| Voies hors appareil | (1) dossier SAF choisi une fois sur **carte SD ou clé USB-OTG** ; (2) **téléphone du binôme** (Bluetooth ou Wi-Fi direct, appareil apparié) ; (3) **envoi opportuniste** vers un serveur WebDAV/Nextcloud réglé par l'employeur, dès que le réseau passe ; chaque envoi réussi est inscrit au journal |
| Photos | écrites **dès la prise** par MediaStore dans `Pictures/Surculus/<code-tournée>/`, empreinte au journal ; une « archive du jour » (journal et photos du jour) suit les mêmes voies |
| Ouverture de journée | Surculus montre le nombre de saisies jamais sorties de l'appareil ; l'agent répond « copié » ou « pas copié », inscrit au journal ; **ne bloque pas** |
| Restauration | sur un autre appareil, par le sélecteur de fichier (`ACTION_OPEN_DOCUMENT`) : même état, à la ligne près (test) ; après une réinstallation, même voie |
| Format `.sursync` | publié dans `docs/formats/sursync-v1.md` avec un JSON Schema et des fixtures (`fixtures/sursync/`) ; JSON Lines compressé gzip ; lit la version N-1, refuse N+1 avec un message ; porte l'identifiant de tournée et la version de sommieR (pas l'empreinte du `travaux.gpkg`, qui change à chaque régénération) ; **complété par la mission 009 (décisions de Pascal du 2026-10-21)** : une dernière ligne `{"sha256": ...}` porte l'empreinte SHA-256 du contenu décompressé qui la précède (pied absent : `fichier_tronque` ; pied faux : `empreinte_differente`, avertissement visible, toutes les lignes lisibles restaurées) ; un fichier abîmé n'est jamais refusé en entier (lignes saines gardées, fautives refusées et signalées), seuls une version N+1 ou illisible, ou un fichier de 0 octet, le sont ; un fichier de même nom n'est jamais écrasé (suffixe `-2`, `-3`) ; la rotation ne supprime que la plus ancienne de la **même tournée** ; l'`<appareil>` du nom est un identifiant opaque ; pas de sauvegarde sans saisie nouvelle |
| Export | `travaux.gpkg` écrit une seule fois depuis le journal ; déterministe par le **contenu des tables** (horodatages `last_change` fixés depuis le journal) ; même résultat quel que soit l'ordre de fusion (test) |
| Tracés | les sommets d'un tracé GNSS forment une seule ligne de géométrie, pas une ligne par époque |
| Sauvegarde Android | `allowBackup` et `dataExtractionRules` fixés explicitement : **ni nuage Google ni transfert d'appareil à appareil** |
| Confidentialité | fichiers nommés par le **code de la tournée**, sans nom de propriétaire ni d'agent ; finalité du champ `operateur` et des horodatages écrite (traçabilité des saisies, non le contrôle de l'agent) ; `.sursync` non chiffré, pour que le successeur et sommieR le lisent |

## Le crible FRANCE

| Lettre | Question | Réponse (une ligne) | Ce que ça coûte |
|---|---|---|---|
| **F**rugale | La plus simple qui tienne ? | Un format unique sert à la sauvegarde, à la restauration et à la fusion. | Trois voies hors appareil à coder et à tester. |
| **R**esponsable | Qui fait quoi ? | Surculus sauvegarde et envoie sans qu'on le demande ; l'agent place une carte ou apparie son binôme ; l'employeur règle le serveur. | L'employeur doit fournir un serveur WebDAV pour la troisième voie. |
| **A**nticipateur | Si l'imprévu arrive ? | Tableau des menaces ci-dessus ; horloge fausse détectée ; conflits signalés. | Si aucune voie n'aboutit, la journée reste exposée (dit à l'ouverture). |
| **N**ovateur | Toutes les idées ? | SD, binôme, réseau, nuage, Auto Backup examinés ; conventions JSON Lines, CloudEvents et geodiff lues. | — |
| **C**oût | Coût pour l'ensemble ? | Pas de service tiers imposé ; quelques centaines de Ko par sauvegarde (à mesurer sur la démo Couchey par le `testeur`). | Code de projection, d'ordre total et de restauration ; serveur de l'employeur. |
| **É**cologique | Que réutilise-t-on, qui réutilisera ? | MediaStore comme Marculus ; format ouvert, documenté et testé. | — |

## Valeurs sacrifiées

- **Frugalité** : trois voies hors appareil au lieu d'une copie manuelle. Accepté pour
  tenir la promesse du brief.
- **Confidentialité par chiffrement** : le `.sursync` n'est pas chiffré. Accepté pour que
  le successeur et sommieR le lisent ; les noms de fichier ne disent ni le propriétaire ni
  l'agent.
- **Anticipation complète** : sans carte, sans binôme et sans réseau, une journée reste
  exposée. Accepté : Surculus le dit sans bloquer.

## À faire hors code

- Pascal informe les employeurs des agents (ONF, coopératives, experts) de la finalité des
  horodatages et des positions (information des salariés, CSE).

## Sources

- `docs/brief.md` (appareil perdu) ; `docs/spec.md` § 3, § 4.8 ; ADR 0006 (fusion, à venir).
- Marculus : `ExportFichier.kt` (MediaStore, `IS_PENDING`, SAF abandonné, `:` remplacé dans
  les noms), `AndroidManifest.xml` l. 29 (`allowBackup`), `MarculusDatabase.kt` l. 37 et
  52-55 (repli destructif, `exportSchema = false`), `MergeDao.kt`, `SauvegardeRepository.kt`,
  `ExportCsv.kt` l. 48 (dates en UTC).
- Android : SAF https://developer.android.com/training/data-storage/shared/documents-files ;
  MediaStore https://developer.android.com/training/data-storage/shared/media ;
  Auto Backup https://developer.android.com/identity/data/autobackup ;
  `SystemClock` https://developer.android.com/reference/android/os/SystemClock ;
  performances SQLite https://developer.android.com/topic/performance/sqlite-performance-best-practices
- SQLite, `PRAGMA synchronous` : https://www.sqlite.org/pragma.html#pragma_synchronous
- RFC 9562 (UUID v7) : https://www.rfc-editor.org/rfc/rfc9562 ; JSON Lines : https://jsonlines.org/ ;
  CloudEvents : https://github.com/cloudevents/spec
- OWASP MASTG-KNOW-0042 (permissions de stockage, API 33) :
  https://mas.owasp.org/MASTG/knowledge/android/MASVS-STORAGE/MASTG-KNOW-0042/
- Éditions Tissot, géolocalisation et information des salariés (source secondaire) :
  https://www.editions-tissot.fr/actualite/representants-du-personnel-ce/geolocalisation-votre-consultation-et-linformation-des-salaries-est-obligatoire

## Question de contrôle

Cette décision tient-elle nos six valeurs, et que sommes-nous prêts à payer pour cela ?

## Après la mission 032 (2026-10-21, ajout, rien n'est effacé plus haut)

La mission 032 (PR #56) précise le journal et sa projection. Le détail et les auteurs sont au
`pilotage/journal.md` (numéros de ligne lus sur `main` le 2026-10-22) :

- plan du `dev` (projection par clé tournée, objet, type ; migration Room 3→4 ; refus du code
  vide), l. 172 ; confirmé par le `relecteur`, l. 183 ;
- décisions de Pascal : l. 173 (`remplace_autre_tournee`, dépôt strict par défaut) et l. 184
  (une seule anomalie, lignes sans tournée, numéros de version) ;
- migration 3→4 acceptée par l'orchestrateur, l. 174.

L'ADR 0006 (fusion), citée à la section Sources, est réservée et non écrite : voir l'[index](README.md).
