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

**Redite par l'agent :** rendre visibles et justes les états du récepteur GNSS et de l'installation de la voix (erreur du récepteur sur la
carte, arrêt propre à la tâche retirée, « pas encore demandée » distinct de « refusée », échec d'installation Vosk même
sur une `Error`, test JVM), pour que le forestier sache pourquoi il n'a pas de position ou de dictée.

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

## Reprise, aller-retour 1/3 (2026-10-29)

Relecture de la #129 (ac464f1) : « à reprendre ». Décision de Pascal du 2026-10-29 : phrase « Corrections
RTK indisponibles : la position est moins précise. » **gardée**.

1. **Conflit** : fusionner `main` dans la branche (la fiche existe aussi sur `main`, erreur de
   l'orchestrateur) en gardant la redite et cette rubrique ; la CI doit tourner et être verte.
2. **Fenêtre d'autorisation fermée sans réponse** (`SectionRecepteur.kt` l. 103-111,
   `AvertissementsRecepteur.kt`) : ne mémoriser un refus que si `shouldShowRequestPermissionRationale` est
   vrai juste après la réponse ; sinon l'état reste « pas encore demandée ». Test du cas, vu rouge. KDoc de
   `JAMAIS_DEMANDEE` d'accord avec le code.
3. Corriger le compte des tests annoncé (9 et non 10) ; relancer `CancellationException` dans
   `SuiviInstallation` (hygiène) ; dire dans la PR que le test de `onTaskRemoved` n'est qu'un garde-fou.
Hors reprise (bac à sable) : `IssueInstallation.Echec` affiche `e.message` brut (texte technique).

## Reprise, aller-retour 2/3 (2026-10-30)

Relecture de la reprise (6844de9) : « prêt », avec une question de sens tranchée par Pascal le 2026-10-29 :
« Renvoyer aux réglages ».

1. Si le système répond « non » **sans rationale ni avant ni après** la demande (refus définitif déjà acquis,
   par exemple sous une version antérieure à la 052), l'état devient « refusée définitivement » et le texte
   renvoie aux réglages du téléphone ; une fenêtre fermée sans réponse (rationale fausse avant, fausse
   après, mais fenêtre montrée) reste distinguée si Android le permet — sinon dire dans la PR quel cas
   l'emporte et pourquoi. Tests des deux cas, vus rouges.
2. Mineur : KDoc du paramètre `dejaRefusee` d'`etatAutorisation` (`AvertissementsRecepteur.kt` l. 29) à
   aligner sur le code ; ne pas surévaluer `activite(ctx)` dans la PR (protection, pas correction).
