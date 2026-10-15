# ADR 0001 — Une application Android dédiée à toute la tournée, plutôt qu'un projet QField enrichi

- **Date :** 2026-10-15
- **Statut :** acceptée par Pascal (2026-10-15), après critique du `contradicteur`
- **Proposée par :** Claude (orchestrateur)
- **Tranchée par :** Pascal

## La décision en une phrase

Toute la tournée travaux (contrôle des plants, constat en cheminement, relevé des
interventions, installation des placettes) se fait dans une application Android dédiée,
Surculus, qui lit et rend le même `travaux.gpkg` que le projet QField de sommieR, au lieu
d'enrichir ce projet QField de formulaires ou d'un plugin.

## Contexte

Le brief validé (`docs/brief.md`) pose le besoin : suivre le **même plant** d'un passage
à l'autre sur des placettes fixes, à une main et sans regarder l'écran, guidé de plant en
plant, et faire en plus un constat de reprise en cheminement. sommieR prépare déjà un
projet QField `travaux` qui fait le contrôle par totaux.

Constat de Pascal sur QField avec des formulaires, plant par plant : trop lent (un
formulaire par plant), impossible à une main, plant non retrouvé d'un passage à l'autre.
**Correction après critique** : QField guide déjà vers un point (navigation, vue précise
sonore), sait pointer en distance et angle (COGO) et lie des plants à une placette par
relation ; ce qui lui manque, c'est la saisie aux boutons de volume, la dictée à
grammaire fermée hors ligne et le changement d'état d'un plant sans formulaire.

### Options envisagées

| # | Option | Sort |
|---|---|---|
| 1 | Projet QField enrichi de formulaires (sommieR seul) | Écartée : ni une main, ni sans regard, un formulaire par plant. |
| 2 | QField + plugin QML/JavaScript | Écartée : un plugin n'a pas accès aux boutons de volume (demande « looking for funding », 22 avril 2026) et ne charge pas de reconnaissance vocale native. |
| 3 | **Application dédiée Kotlin**, reprenant Marculus (dictée Vosk, boutons de volume, journal) et Groma (GNSS, NTRIP, géodésie) | **Retenue.** |
| 4 | Papier ou tableur, saisie au bureau | Écartée : c'est ce qu'on veut supprimer. |
| 5 | Saisie entièrement audio, transcrite au bureau | Écartée : aucun retour sur le terrain (refus, totaux, alerte). |
| 6 | Surculus pour le seul contrôle, QField pour interventions et installation | Écartée par Pascal : deux applications et un récepteur Bluetooth à basculer (une liaison SPP ne sert qu'une application à la fois). |
| 7 | Hybride : application compagnon (dictée, boutons) qui pilote un plugin QField en local, la voie étudiée par Groma | Écartée : une application en arrière-plan reçoit mal les boutons de volume ; l'accès d'un plugin au réseau local n'est pas établi ; deux applications à faire tenir ensemble. |
| 8 | ODK Collect / KoboToolbox (Entities pour le suivi longitudinal) | Écartée : ni GeoPackage ni Lambert-93 natifs, serveur Central requis, pas de dictée à grammaire fermée ni de boutons de volume. |
| 9 | Mergin Maps | Écartée : bâti sur QGIS comme QField, mêmes limites de saisie (non examinée en détail). |
| 10 | Application web hors ligne | Écartée : un navigateur n'expose pas les boutons de volume, et Web Bluetooth ne fait pas de Bluetooth classique (SPP), donc pas de ZED-F9P. |

L'essai d'un plugin QField chronométré a été proposé ; Pascal tranche **sans essai** :
boutons de volume et dictée sont aujourd'hui hors de portée d'un plugin, l'essai ne
changerait pas la décision.

## Le crible FRANCE

| Lettre | Question | Réponse (une ligne) | Ce que ça coûte |
|---|---|---|---|
| **F**rugale | La plus simple qui tienne ? | Non pour le code, oui pour le geste : seule option qui tient la saisie à une main, sans regard, et une seule application sur le terrain. | On réécrit ce que QField fait déjà : carte, couches, tracé des interventions, navigation, NTRIP (QField 4.2). |
| **R**esponsable | Qui fait quoi ? | sommieR prépare et relit ; Surculus saisit ; l'équipe d'agents développe, Pascal tranche. | Un seul mainteneur pour trois applications Kotlin (Marculus, Groma, Surculus) ; successeur non désigné. |
| **A**nticipateur | Si l'imprévu arrive ? | Si Surculus s'arrête, on retombe au contrôle par totaux dans QField : **le suivi plant par plant s'arrête avec Surculus** (risque accepté par Pascal). Les données restent dans un GeoPackage ouvert, lisible par tout SIG. | Dépendance à Android et au format de sommieR 0.31.1 ; une tournée en cours est dans le stockage privé de Surculus. |
| **N**ovateur | Toutes les idées ? | Dix options examinées, dont plugin, hybride, ODK, web ; dictée et boutons de volume déjà éprouvés dans Marculus. | — |
| **C**oût | Coût pour l'ensemble ? | Réutilise Marculus et Groma (GPL-3.0-or-later), mais en partie seulement. | Mise à jour annuelle du SDK cible (exigence Google Play) ; écriture GeoPackage sous Android (déclencheurs R*Tree et `ST_IsEmpty`, contournés par Groma) ; transport Bluetooth et service RTK à extraire de l'application Groma (absents de `groma-core`) ; azimuts de Groma **magnétiques**, à corriger de la déclinaison pour le nord géographique du brief. |
| **É**cologique | Que réutilise-t-on, qui réutilisera ? | GeoPackage ouvert, code GPL, bibliothèques communes à extraire pour Marculus, Groma et Surculus. | Un forestier hors de l'écosystème connaît QField, pas Surculus ; **Groma va vers un plugin QField pendant que Surculus s'en écarte**. |

## Valeurs sacrifiées

- **Frugalité du code** : on réécrit le relevé d'interventions et la navigation que QField
  fait déjà. Accepté pour avoir une seule application et un seul récepteur sur le terrain,
  et parce que le geste (une main, sans regard) est le problème même du brief.
- **Anticipation** : le suivi plant par plant n'a pas de repli hors de Surculus. Accepté
  par Pascal ; les données restent ouvertes.
- **Écologie, au sens de la réutilisation par d'autres** : une application de plus à côté
  de QField, et une divergence avec Groma. Atténué par le contrat de format.

## Clause de révision

Revoir cette ADR si QField livre la saisie vocale (« speech-driven data entry », en
recherche de financement avec un co-financeur annoncé le 27 mars 2026) ou l'accès des
plugins aux boutons de volume.

## Sources

- `docs/brief.md`, « Pourquoi pas QField » (constat de Pascal, 15 octobre 2026).
- `docs/spec.md` (brouillon), § 4.1, 4.3, 4.5, 4.6 ; `docs/architecture.md`.
- QField, réglages (boutons de volume pour numériser) : https://docs.qfield.org/how-to/general/settings/
- QField, plugins : https://docs.qfield.org/how-to/advanced-how-tos/plugins/ ; API : https://api.qfield.org/
- QField, navigation : https://docs.qfield.org/how-to/navigation-and-positioning/navigation/
- QField, COGO : https://docs.qfield.org/how-to/advanced-how-tos/cogo-framework/
- QField, relations : https://docs.qfield.org/how-to/project-setup/relation-reference-widget/
- QField 4.2 (NTRIP intégré) : https://qfield.org/blog/2026/06/09/qfield-4.2-coral-sea-reaching-sub-centimeter-accuracy-out-of-the-box/
- Demande « Allow Plugins to optionally access Volume Keys » : https://ideas.qfield.org/app-feature-requests/p/allow-plugins-to-optionally-access-volume-keys-while-use-volume-keys-to-digitize
- Demande « speech-driven data entry » : https://ideas.qfield.org/app-feature-requests/p/integration-of-speech-driven-data-entry
- vocalpoint-qfield-plugin : https://github.com/SeqLaz/vocalpoint-qfield-plugin
- ODK Entities : https://docs.getodk.org/entities-intro/
- Google Play, niveau d'API cible : https://developer.android.com/google/play/requirements/target-sdk
- Groma, `docs/terrain.md` (stockage QField, plugin, R*Tree, Bluetooth SPP, azimut magnétique).
- Marculus (`LICENSE`, GPL-3.0-or-later, `792b5aa`) ; Groma (GPL-3.0-or-later).

## Question de contrôle

Cette décision tient-elle nos six valeurs, et que sommes-nous prêts à payer pour cela ?
