# Mission 065 — Le jour civil du contrôle pris sur l'heure du fix GNSS

> Fiche écrite par l'orchestrateur le 2026-10-28, à partir du gabarit
> `imperatrice/skills/mission/fiche-mission.md`. Origine : réserve du `relecteur` sur la PR #108
> (012-B1, « jour civil sur l'heure du téléphone, écart à la spec § 4.4 ») ; décision de Pascal du
> 2026-10-28 : « jour civil sur l'heure du téléphone accepté, mission courte pour que le fix GNSS
> porte son heure UTC, avant l'essai d'automne ».
> **Lancement :** le 2026-10-28, depuis `main` (f955af6). Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Fournir à la session de contrôle le dernier fix GNSS avec
son heure UTC, pour que le jour civil d'un contrôle se calcule sur l'heure GNSS quand le fix a au
plus 5 s, comme le veut la spec, et non sur l'heure du téléphone, qui peut être fausse en forêt.

**Redite par l'agent :** Faire que l'écran du contrôle donne à la session le dernier fix GNSS avec son heure UTC et son instant
monotone (téléphone : `Location.time` ; récepteur : heure de la GGA + date de la RMC), pour que le jour civil
d'un contrôle vienne de l'heure GNSS quand le fix a au plus 5 s, et de l'heure du téléphone sinon.

## Ce qui existe

- La règle est écrite : spec § 5 (journal), « Le jour civil d'un contrôle se calcule sur l'heure
  GNSS quand le fix a au plus 5 s au moment de la saisie (âge mesuré sur l'horloge monotone,
  enregistré avec la ligne), sinon sur l'heure du téléphone », et « Un fix GNSS d'âge négatif
  (postérieur à la saisie) est ignoré ».
- Le port existe : `HorlogeSession.fix()` (`core/.../session/Ports.kt:14`) ;
  `SauvegardeApp.horloge(context, dernierFix)` (`app/.../sauvegarde/SauvegardeApp.kt:37`).
- **Le défaut** : l'écran du contrôle passe `{ null }` (`app/.../controle/EcranControle.kt:147`) :
  la session ne voit jamais de fix, le jour civil vient toujours de l'heure du téléphone.

## Décisions de Pascal à respecter

1. Le jour civil reste en Europe/Paris (ADR 0005) ; le jour UTC reste vérifié (spec § 5,
   « un contrôle par placette et par jour, deux fois »).
2. Pas de mode de position simulée pour vérifier : le contrôle de bout en bout se fera à Couchey.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : spec § 5 ; `HorlogeSession` ; récepteur GNSS de la 047 et de la 061 (fix courant de la carte et du guidage) | |
| A-t-il les moyens ? | oui : tout se vérifie par des tests, sans téléphone | |

**Temps de la main :** **appuyer**. Code direct, pas de plan.

## L'Homme

- **Agent :** `dev`.
- **Ce qui ne lui est pas demandé :** changer la règle de la spec ; toucher à l'écran au-delà du
  branchement ; la 012-C (volume, poche, voix).

## Harnais

- **Peut :** `app/` (branchement du dernier fix, conversion vers `FixSession` avec l'heure UTC du
  fix et son instant sur l'horloge monotone) ; `:core` seulement si la règle n'y est pas déjà
  testée, annoncé dans la PR.
- **Ne doit pas :** modifier `fixtures/` ; toucher à `main` ; modifier `pilotage/journal.md` ;
  inventer un code ; trancher une question de sens.
- **Sources autorisées :** spec § 4.4 et § 5 ; ADR 0005 ; fiches 012-A, 012-B, 047, 061 ; Groma en
  lecture seule (heure du fix NMEA).

## Tests

- Fix de 3 s : le jour civil vient de l'heure GNSS, même quand l'heure du téléphone est fausse et
  tombe un autre jour (cas de minuit, Europe/Paris, été et hiver).
- Fix de plus de 5 s, fix absent, fix d'âge négatif : heure du téléphone.
- L'âge du fix est mesuré sur l'horloge monotone et enregistré avec la ligne.
- Le branchement : un test qui échoue si l'écran repasse `{ null }`.
- **Placette en Somalie** : un fix hors de France ne change pas la règle de l'heure, mais ne pointe
  rien (rien de cassé).
- **Vu rouge** une fois. `.claude/test-command` au code 0.

## Terrain

- **Échantillon :** à Couchey, une placette contrôlée avec fix ; la ligne `controle` du journal
  porte l'âge du fix et le jour civil attendu.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. Critère d'arrêt : 3 allers-retours.
- **Qui tranche :** Pascal. **Fusion : autorisation à demander à Pascal.**

## Comment saura-t-on que ça marche ?

Un contrôle saisi avec un fix frais porte le jour de l'heure GNSS, même quand l'horloge du
téléphone est déréglée.
