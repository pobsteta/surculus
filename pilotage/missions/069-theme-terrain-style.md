# Mission 069 — Le thème pris de `terrain-style`, plus de copie

> Fiche écrite par l'orchestrateur le 2026-10-28 (cycle 13), sur l'ordre de Pascal « Code les briefs
> si il y en a qui ne sont pas codés ». Source : brief `marculus` → `surculus` du 2026-10-24
> (`~/dev/briefs/vers-surculus/2026-10-24-marculus-palette-commune-vert-marculus.md`) ; carte du
> bac à sable « `terrain-style` v0.6.1 : à sa publication, remplacer la copie littérale du thème
> (038) par la dépendance ; le test d'égalité doit rester vert ». Depuis `main`.
> Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Remplacer la copie littérale du thème vert de Marculus
(mission 038, `ui/theme/Theme.kt`) par la dépendance à `terrain-style`, avec un test d'égalité rôle
par rôle, pour que les trois applications du projet nemeton tiennent leurs couleurs d'une seule
source.

**Redite par l'agent :** Remplacer la copie littérale du thème vert de Marculus par la dépendance à `terrain-style` épinglée au tag v0.7.1, et prouver par un test rôle par rôle que les couleurs de Surculus restent exactement celles du brief, pour que les trois applications tiennent leurs couleurs d'une seule source.

## Décisions de Pascal à respecter

- « je veux que Marculus, groma et surculus utilise terrain-style mais avec un terrain-style qui
  conserve les couleurs actuelles de vert de marculus » (2026-10-24, brief cité).
- ADR 0008 et son amendement du 2026-10-24 (soir) : mêmes jetons, la règle 7:1 et la génération MCU
  tombent.
- Choix Système / Clair / Sombre et règles de la 063 : inchangés.

## Version

Le brief vise v0.6.1, mais au tag v0.6.1 (7c9a5fd) `terrain-style` n'a pas encore les valeurs de
Marculus : elles arrivent avec « terrain-style : valeurs du style de Marculus » (a9a95d7), publiées
au tag **v0.7.1** (dernier tag, `fbf0987` « contrat Float et StyleTexte »). L'agent vérifie dans
`~/dev/terrain` (lecture seule) quel tag porte les couleurs de la table du brief, épingle ce tag
(JitPack, comme `terrain-voix`), et le dit dans la PR. `terrain-voix` reste épinglé à d840329 :
ne pas le changer dans cette mission.

## Ce qui est attendu

- `Theme.kt` construit les `ColorScheme` clair et sombre à partir de `terrain-style` ; la copie
  littérale disparaît.
- `ThemeTest` vérifie l'égalité rôle par rôle avec les valeurs du brief (`primary` #3E7A40,
  `onPrimary` #FFFFFF, `primaryContainer` #374742, `onPrimaryContainer` #F5EBD8, `secondary`
  #374742 en clair, #F5EBD8 en sombre ; les autres rôles aux valeurs par défaut), pour que le test
  rougisse si `terrain-style` change.
- Aucune couleur visible ne change par rapport à la v0.7.77 ; si une seule change, l'agent
  s'arrête et le dit.

## Les trois questions

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase ? | oui | |
| Sait-il comment faire ? | oui (`terrain-voix` montre le chemin JitPack) | |
| A-t-il les moyens ? | oui ; `terrain-style` public au tag | |

**Temps de la main :** **appuyer**.

## L'Homme

- **Agent :** `dev`.
- **Pas demandé :** l'ADR 0008 (l'orchestrateur la date après fusion) ; les autres écrans ; la
  palette d'état ; `terrain-voix`.

## Harnais

- **Peut :** `gradle/libs.versions.toml`, `app/build.gradle.kts`, `ui/theme/`, leurs tests ;
  pousser sur `mission/069-theme-terrain-style` ; ouvrir la PR.
- **Ne doit pas :** `fixtures/`, `docs/`, `pilotage/journal.md`, `main` ; changer une couleur ;
  trancher une question de sens.

## Tests

- `ThemeTest` rôle par rôle, **vu rouge** une fois (une valeur faussée). `.claude/test-command` au
  code 0, CI verte (JitPack résolu).

## Terrain

- Capture de l'orchestrateur sur la release, clair et sombre : rien ne doit changer à l'œil.

## Qui veille, qui tranche

- **Relecteur**, puis Pascal. 3 allers-retours. **Fusion : autorisation de Pascal.**

## Comment saura-t-on que ça marche ?

Le thème de Surculus vient de `terrain-style`, le test d'égalité est vert, et la capture de la
release est identique à la v0.7.77.
