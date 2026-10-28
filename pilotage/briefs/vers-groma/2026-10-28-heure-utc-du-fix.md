# Brief `surculus` → `groma` — l'heure UTC complète du fix GNSS (date de la RMC, horloge monotone)

> **Statut** : ouvert, 2026-10-28 · **Émis par** : session `surculus` (orchestrateur), sur
> décision de Pascal du 2026-10-28 · **Urgence** : faible.

## Décision de Pascal

À la question « renvoie-t-on ces améliorations à Groma ? » (relecture de la PR
pobsteta/surculus#112) : « Brief vers Groma ».

## Ce que Surculus a changé dans le code repris de Groma

Origine : `android/groma-core/src/main/kotlin/groma/core/Nmea.kt`, état au commit f12b644
(2026-10-16), reprise provisoire dans Surculus (`core/.../gnss/Nmea.kt`, `Gnss.kt`) jusqu'à
l'extraction commune de l'ADR 0008. Mission 065, fusionnée dans Surculus le 2026-10-28 (4d53d09).

1. **Date de la RMC** : `TrameRmc` garde `instantUtcMs`, l'instant UTC complet (date `ddmmyy`
   et heure), seulement quand le statut vaut `A`.
2. **Heure UTC complète du fix** : `FixGnss.heureUtcMs` = heure du jour de la GGA, jour de la
   RMC. Quand GGA et RMC encadrent minuit UTC, on prend le jour (veille, même jour, lendemain)
   qui met l'heure de la GGA au plus près de celle de la RMC. Sans RMC valide : `null`.
3. **Horloge monotone** : `LecteurNmea` reçoit en option une horloge monotone
   (`SystemClock.elapsedRealtime`) et note `FixGnss.recuMonotoneMs` à la réception de chaque fix.
   Le paramètre est placé **avant** l'horloge murale, pour que `LecteurNmea { … }` désigne
   toujours l'horloge murale.

Pourquoi : la GGA ne porte que l'heure du jour, et l'horloge du téléphone peut être fausse en
forêt. Surculus en a besoin pour dater un contrôle (jour civil Europe/Paris sur l'heure GNSS
quand le fix a au plus 5 s, âge mesuré sur l'horloge monotone).

Tests côté Surculus : `core/src/test/.../gnss/HeureUtcDuFixTest.kt` (date de la RMC, centièmes,
minuit UTC dans les deux sens, RMC absente ou invalide, GGA sans heure, instant monotone).

## Ce qui est demandé

Reprendre ces trois changements dans `groma-core`, ou dire pourquoi Groma ne les veut pas, pour
que les deux copies ne s'écartent pas avant l'extraction commune.

## Critère d'acceptation

`groma-core` donne pour un fix l'instant UTC complet et l'instant monotone de réception, avec les
cas de `HeureUtcDuFixTest` verts ; ou une réponse qui explique le refus.

## À vérifier sur le récepteur réel

Le récepteur RTK émet-il la RMC au statut `A` ? Sans elle, Surculus revient à l'heure du téléphone.
