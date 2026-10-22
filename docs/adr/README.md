# Index des ADR

> Créé le 2026-10-22 (mission 034). Source unique de la liste des décisions d'architecture ;
> chaque ADR porte son statut en en-tête, qui fait foi. Une ADR ne s'efface pas : on ajoute une
> section datée. Mise à jour par l'`archiviste` à chaque nouvelle ADR.

| N° | Titre | Statut (lu à l'en-tête) |
|---|---|---|
| [0001](0001-application-dediee-plutot-que-qfield.md) | Application Android dédiée plutôt qu'un projet QField enrichi | acceptée (2026-10-15) |
| [0002](0002-protocole-constat-de-reprise.md) | Protocole de constat de reprise | acceptée (2026-10-15) |
| [0003](0003-gnss-et-reperes.md) | GNSS et repères | acceptée (2026-10-15) |
| [0004](0004-stack-kotlin-compose-vosk.md) | Stack : Kotlin, Compose, Room, module GeoPackage JVM, Vosk | acceptée (2026-10-15) ; réexamen face à osmdroid archivé **non fait** (bac à sable) |
| [0005](0005-stockage-et-sauvegarde.md) | Stockage et sauvegarde | acceptée (2026-10-15), complétée après la 032 |
| 0006 | Fusion des tournées | **réservée, non écrite** ; citée par l'ADR 0005 (Sources) ; le renvoi depuis la spec (§ 4, l. 236) a été ajouté par la mission 034 |
| [0007](0007-couches-ajoutees-au-contrat-sommier.md) | Couches ajoutées au contrat `travaux.gpkg` | acceptée (2026-10-15) |
| [0008](0008-bibliotheques-communes.md) | Style commun avec Marculus et Groma, par la bibliothèque `terrain` (les autres briques communes : ADR ultérieure) | proposée (2026-10-22) |
| [0009](0009-carte-hors-ligne.md) | Carte et ortho hors ligne | acceptée (2026-10-21) |
| 0010 | Clé de signature et distribution | **réservée, non écrite** ; citée par `docs/publier.md` ; la clé reste à Pascal |
| [0011](0011-ecriture-geopackage-sur-appareil.md) | Écriture du GeoPackage sur l'appareil | acceptée (2026-10-17) |
| 0012 | Briques communes (GNSS, géodésie, journal, GeoPackage, voix, carte) vers `terrain` | **réservée, non écrite** (2026-10-22) : renvoyée par l'ADR 0008 après l'essai d'automne 2026 et une table des écarts ; les en-têtes des copies (`core/.../{gnss,geo}/*.kt`, `Declinaison.kt`, `app/.../gnss/*.kt`, leurs tests, `NOTICE` l. 38), l'ADR 0003 l. 69 et la fiche 003 l. 26 disent encore « ADR 0008 » |

Les ADR 0006, 0008, 0010 et 0012 sont des décisions d'architecture : elles passent par le
`contradicteur`, puis Pascal tranche (`CLAUDE.md`, « Le déroulé »). Aucun contenu n'est supposé ici.
