# Mission 038 — Thème commun : le vert de Marculus

> Fiche écrite par l'orchestrateur le 2026-10-24, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`, puis **réécrite le même jour** après
> la décision de Pascal rapportée par la session `marculus`
> (`~/dev/briefs/vers-surculus/2026-10-24-marculus-palette-commune-vert-marculus.md`) :
> « je veux que Marculus, groma et surculus utilise terrain-style mais avec un terrain-style
> qui conserve les couleurs actuelles de vert de marculus ». ADR 0008, amendement du
> 2026-10-24 (soir). La première version (jetons de `d840329`, règle 7:1, palette d'état)
> est dans l'historique git de ce fichier.
> **Lancement : tout de suite**, avant l'essai au soleil de Couchey (Pascal, 2026-10-24 :
> « Coder d'abord ») ; **valeurs recopiées de Marculus v0.65.0**, sans attendre
> `terrain-style` v0.6.1, qui n'est pas publiée (Pascal, 2026-10-24 : « Valeurs de Marculus »).

## Mission

**En une phrase (quoi, et pourquoi) :** Donner à Surculus le thème de Marculus v0.65.0 (VertVif `#3E7A40`, VertMarculus `#374742`, Crème `#F5EBD8`, les autres rôles aux valeurs par défaut de Material 3), écrit en valeurs littérales pour le clair et le sombre, appliqué en thème clair forcé et vérifié rôle par rôle par un test, pour que Surculus ait le même vert que Marculus et Groma avant même la publication de `terrain-style` v0.6.1.

**Redite par l'agent :** Je recopie dans Surculus le thème de Marculus v0.65.0 (VertVif, VertMarculus, Crème, le reste par défaut de Material 3) en valeurs littérales, clair et sombre, je l'applique en clair forcé, et un test vérifie chaque rôle ; le but est que Surculus ait le même vert que Marculus et Groma sans attendre `terrain-style` v0.6.1.

## Vérifié à la source

Constats vérifiés par l'orchestrateur le 2026-10-24, sur `main` (5e9a9fb).

| Constat | Lu à | Source du constat |
|---|---|---|
| Thème de Marculus : `VertMarculus = 0xFF374742`, `VertVif = 0xFF3E7A40`, `Creme = 0xFFF5EBD8` ; clair : `primary` VertVif, `onPrimary` blanc, `primaryContainer` VertMarculus, `onPrimaryContainer` Crème, `secondary` VertMarculus ; sombre : idem sauf `secondary` Crème ; tout le reste par défaut (`lightColorScheme()` / `darkColorScheme()`) | `marculus`, tag `v0.65.0`, `app/src/main/kotlin/io/github/pobsteta/marculus/ui/theme/Theme.kt` l. 10-28 | orchestrateur |
| Même BOM Compose des deux côtés : `composeBom = "2026.02.01"`, donc mêmes valeurs par défaut | Surculus `gradle/libs.versions.toml` l. 24 ; Marculus l. 5 | orchestrateur |
| `terrain-style` v0.6.1 n'existe pas : dernier tag v0.6.0 ; sur `main` (`c13e539`), `StyleTerrain.clair` a toujours `primary = 0xFF202F2B` | `terrain`, `git tag`, `terrain-style/src/main/kotlin/fr/nemeton/terrain/style/StyleTerrain.kt` l. 24 | orchestrateur |
| Surculus n'a pas de thème : `MaterialTheme { … }` sans `colorScheme` | `app/src/main/kotlin/io/github/pobsteta/surculus/MainActivity.kt:42` | orchestrateur |
| Le brief demande l'égalité rôle par rôle avec `StyleTerrain` v0.6.1, clair et sombre | brief ci-dessus, « Critère d'acceptation » et point 1 | session `marculus` |

Règles, avec leur auteur :
- **Vert de Marculus pour les trois applications ; la règle 7:1 et la génération par MCU tombent** : Pascal, 2026-10-24 (ADR 0008, amendement du soir).
- **Valeurs de Marculus en attendant v0.6.1 ; code avant Couchey** : Pascal, 2026-10-24.
- **Thème clair forcé**, pas de couleur dynamique : Pascal, 2026-10-22 (ADR 0008, valeurs sacrifiées). Le choix Système / Clair / Sombre de Marculus n'est pas dans cette mission (bac à sable).
- **Garde-fou = test vu rouge** et **bilan des écarts avant rendu** : Pascal, bilan du 2026-10-21.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : Compose Material 3, `ColorScheme`, test JVM | |
| A-t-il les moyens ? | oui : toutes les valeurs sont dans la fiche ; aucune dépendance nouvelle | |

**Temps de la main :** **appuyer** : l'agent code puis rend ; la relecture et l'essai au soleil de Pascal avant la fusion tiennent la qualité.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent. L'orchestrateur ne le remplit pas._

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :**
  - ajouter `terrain-style` ou JitPack en dépendance du thème (ce sera une petite mission à la publication de v0.6.1) ;
  - toucher à la palette d'état (`carte/Palette.kt`), aux pastilles, au marqueur de position ou à la légende de la carte ;
  - ajouter le choix Système / Clair / Sombre ou la couleur dynamique ;
  - retoucher une valeur de Marculus (s'il en faut une autre, il le dit).

## Harnais

- **Peut :** écrire le thème de Surculus (`ui/theme/` ou équivalent, avec la source de chaque valeur en commentaire) et le brancher dans `MainActivity.kt` ; ajouter un test JVM et, s'il manque, la configuration de test du module `app` ; mettre à jour `docs/dependances.md` si une ligne le demande.
- **Ne doit pas :** toucher à `main` ; modifier `fixtures/` ; ajouter une dépendance d'exécution.
- **Sources autorisées :** cette fiche ; `docs/adr/0008-bibliotheques-communes.md` ; le dépôt `marculus` (`~/dev/marculus`, en lecture, tag `v0.65.0`) ; la doc de Compose Material 3.

## Tests

- **Égalité** : un test JVM vérifie, **rôle par rôle et pour tous les rôles** du `ColorScheme`, que le thème clair et le thème sombre de Surculus valent : les cinq rôles ci-dessus en **valeurs littérales** écrites dans le test, et pour chaque autre rôle la valeur de `lightColorScheme()` / `darkColorScheme()` de la BOM 2026.02.01. **Vu rouge** une fois (un rôle changé, non commité).
- **Thème appliqué** : un test (ou, à défaut, une capture jointe à la PR) montre que l'application emploie le schéma clair même quand le téléphone est en sombre.
- Commande du projet : `.claude/test-command`.
- **Bilan des écarts** : le rapport liste chaque fichier modifié, et chaque texte de l'application dont la couleur change visiblement (notamment ce qui était violet par défaut, dont le bouton « Retour »).

## Terrain

- **Échantillon que Pascal vérifiera :** l'APK de la branche, au soleil, sur les écrans Tournées, Carte, Guidage et Essai GNSS ; résultat noté au journal **avant la fusion** (spec § 3).

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours ; au-delà, la décision remonte à Pascal.
- **Point de suivi :** à l'ouverture de la PR.

## Comment saura-t-on que ça marche ?

Surculus a le vert de Marculus sur tous ses écrans, le test d'égalité est vert dans la CI, et Pascal juge les écrans lisibles au soleil.
