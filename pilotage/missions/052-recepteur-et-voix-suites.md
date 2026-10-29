# Mission 052 — Les suites du récepteur et de la voix

> Fiche écrite par l'orchestrateur le 2026-10-29 (cycle 15), à partir du gabarit
> `imperatrice/skills/mission/fiche-mission.md`, sur les décisions de Pascal du 2026-10-27 (journal :
> « 1 oui, 2 oui… » sur les 047 et 048, mineurs de la #92 « vont à la mission 052 ») et son choix du
> cycle 15. Depuis `main`. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Rendre visibles et justes les états du récepteur GNSS et de
l'installation de la voix (erreur du récepteur sur la carte, arrêt propre quand la tâche est retirée,
autorisation jamais demandée distinguée d'un refus, erreur d'installation qui ne laisse pas l'écran « en
cours »), pour que le forestier sache en forêt pourquoi il n'a pas de position ou pas de dictée.

**Redite par l'agent :** (à remplir par l'agent avant d'agir)

## Ce qui est à faire (cinq points, tous tirés du journal)

1. **Erreur du récepteur visible sur la carte** (décision du 2026-10-27, 047) : quand le récepteur externe
   ou le service GNSS est en erreur, la carte le dit (texte court, en français, sans code), pas seulement
   Paramètres.
2. **`onTaskRemoved`** (même décision) : le service du récepteur s'arrête proprement quand l'utilisateur
   retire la tâche ; pas de service orphelin ni de notification restée.
3. **Autorisation jamais demandée** affichée « refusée » dans Paramètres : distinguer « pas encore
   demandée » de « refusée » (et « refusée définitivement »).
4. **`SuiviInstallationVosk`** : une `Error` (pas seulement une `Exception`) laisserait l'écran « en cours »
   (`SuiviInstallationVosk.kt` l. 94 au 2026-10-27, relecture de la #92) : l'état doit revenir à « échec ».
5. **Test JVM de `SuiviInstallationVosk`** (même relecture) : il n'en a pas.

Tests d'abord, vus rouges pour 3, 4 et 5 ; pour 1 et 2, le test le plus proche possible sans Compose, et le
dire. Chaque texte nouveau, proposé en français, sans code technique.

**Temps de la main :** **appuyer**.

## L'Homme

- **Agent :** `dev`.
- **Ce qui ne lui est pas demandé :** la dictée elle-même, le mode poche, le choix du récepteur, le
  téléchargement du modèle.

## Harnais

- **Peut :** modifier le récepteur, Paramètres, `SuiviInstallationVosk`, la carte (le seul message d'erreur)
  et leurs tests ; pousser sur `mission/052-recepteur-et-voix-suites` ; ouvrir la PR.
- **Ne doit pas :** modifier `docs/`, `pilotage/` (sauf la redite), `fixtures/` ; toucher à `main` ; trancher
  une question de sens.

## Tests

- `.claude/test-command` au code 0 ; nouveaux tests vus **rouges** avant la correction.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. Critère d'arrêt : 3 allers-retours. **Fusion : autorisation de
  Pascal.**

## Comment saura-t-on que ça marche ?

Sur la release : Paramètres d'un téléphone neuf montre « pas encore demandée » ; récepteur externe débranché,
la carte le dit ; tâche retirée, plus de notification du récepteur. Captures de l'orchestrateur.
