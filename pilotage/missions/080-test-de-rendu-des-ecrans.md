# Mission 080 — Test de rendu des écrans

> Fiche écrite par l'orchestrateur le 2026-10-30, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> après le bilan du 2026-10-30 (causes A et C du Pareto, 55 % des écarts) et la validation de Pascal (porte n° 2).
> Depuis `main`. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Écrire un test qui rend la carte et le contrôle de placette, debout et couché,
et échoue si un bandeau ou un texte cache un bouton, pour attraper avant la capture du téléphone ce que seul le téléphone
montrait jusqu'ici.

**Redite par l'agent :** Je rends d'abord un plan (outil, écrans, assertions, preuve par un défaut passé, limites, questions), sans code, pour qu'un test de rendu de la carte et du contrôle, debout et couché, échoue quand un bandeau ou un texte cache un bouton, avant que le téléphone ne le montre.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? (sources, conventions nommées ci-dessous) | **non** : aucun test de rendu Compose dans le dépôt | **tenir** : plan d'abord |
| A-t-il les moyens ? (outils, données d'essai, tests) | **partiel** : Robolectric 4.17 seulement dans `data` ; aucun outil de test Compose dans `app` | le plan nomme la dépendance à ajouter |

**Temps de la main :** **tenir** — l'agent rend d'abord un plan (`pilotage/missions/080-plan.md`, sur cette branche) ;
l'orchestrateur le présente à Pascal ; le code ne commence qu'après son « oui ».

## Ma réponse d'abord

_Rempli par Pascal seul._

## L'Homme

- **Agent :** `afficheur`.
- **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** changer un écran ; corriger un défaut que le test trouverait (le dire, une
  mission à part le corrigera) ; l'essai de fumée sur émulateur (ADR 0011, question à Pascal au bac à sable).

## Harnais

- **Peut :** ajouter des tests dans `app/src/test/` ; ajouter à `app/build.gradle.kts` et `gradle/libs.versions.toml`
  les dépendances **de test** nommées dans le plan validé (licence et source citées, `docs/dependances.md` mis à jour) ;
  rendre testable une composable sans changer son rendu (paramètre d'état, `testTag`) ; pousser sur
  `mission/080-test-de-rendu-des-ecrans` ; ouvrir la PR.
- **Ne doit pas :** ajouter une dépendance hors `testImplementation` ; changer ce que l'écran affiche ; modifier
  `fixtures/`, `docs/` (sauf `dependances.md`), `pilotage/` (sauf la redite et le plan) ; toucher à `main`.
- **Sources autorisées :** `app/src/main/kotlin/.../carte/EcranCarte.kt`, `DispositionBoutons.kt`,
  `controle/EcranControle.kt`, leurs tests ; `data/build.gradle.kts` (Robolectric existant) ; les écarts du journal
  qui fondent la mission (bandeau caché de la 055, « ÉCOUTE FERMÉE » de la 075, bande du mode poche de la 012-C1).

## Le plan attendu (première tâche)

1. L'outil : Robolectric + `compose-ui-test` en JVM (recommandé, pas d'émulateur en CI), ou autre ; avec ses versions,
   sa licence, son coût en temps de CI.
2. Les écrans et tailles rendus : carte et contrôle, 2316 × 1080 px à densité 450, debout et couché, avec et sans
   plein écran, avec le bandeau « Récepteur GNSS non démarré ».
3. Les assertions : aucun nœud cliquable recouvert par un texte ou un autre nœud ; six boutons entiers ; et la preuve
   que le test aurait vu rouge les trois défauts passés (rejouer la 055, la 075 ou la 012-C1 sur un commit ancien).
4. Ce qu'il ne peut pas voir (rendu réel de la `MapView`, polices du téléphone).

## Valeur provisoire (chiffre, réglage)

- **Valeur provisoire :** les tailles d'écran rendues (2316 × 1080, densité 450 ; hauteur sans plein écran ≈ 306 dp,
  mesurée le 2026-10-30 sur v0.7.95).
- **Mesurée par :** la capture de l'orchestrateur du 2026-10-30 (journal).
- **Gel :** aucun ; un autre téléphone ajoutera ses tailles.

## Dépend de / rouge attendu jusqu'à

Aucune. Le rouge des défauts passés se montre sur un commit ancien, pas dans la CI de la PR.

## Tests

- Le test passe sur `main` actuel ; il est vu rouge sur au moins un défaut passé rejoué ; `.claude/test-command` au
  code 0 ; durée ajoutée à la CI annoncée.

## Terrain

- **Échantillon que Pascal vérifiera :** la sortie du test (image ou liste des nœuds) pour la carte couchée, face à la
  capture du 2026-10-30.

## Qui veille, qui tranche

- **Relecteur :** `relecteur` (le plan, puis la PR), puis Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Point de suivi :** au plan, puis à la PR.
- **Qui tranche :** Pascal (le plan, la dépendance de test, la fusion).

## Comment saura-t-on que ça marche ?

À la prochaine mission d'écran, un recouvrement est vu rouge en CI avant la capture du téléphone.
