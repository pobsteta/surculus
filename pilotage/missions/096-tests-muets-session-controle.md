# Mission 096 — Les quatre tests muets de SessionControleTest lancés pour de vrai

> Fiche écrite par l'orchestrateur le 2026-11-07, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> après la décision de Pascal du même jour (« Mission de réparation »). Depuis `main` (aa02f2c). Une mission = une
> phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Réécrire les quatre tests de `SessionControleTest.kt` que JUnit 5 ne lance
jamais, pour qu'ils tournent et disent vrai, parce qu'un test qui ne tourne pas laisse croire qu'une règle est gardée.

**Redite par l'agent :** faire en sorte que les quatre tests de `SessionControleTest.kt` que JUnit 5 sautait sans rien dire (corps rendant autre chose que `Unit`) soient réellement lancés, sans changer leurs assertions, pour que les règles qu'ils croyaient garder le soient vraiment ; et dire lesquels échouent, sans toucher au code.

## Le constat

- JUnit 5 ne lance pas une méthode `@Test` qui rend une valeur autre que `Unit` ; il la saute sans rien dire.
  Un test écrit `fun `…`() = with(BancSession()) { … }` dont la dernière expression rend une valeur (par exemple
  `assertIs<…>(…)` ou un `assertFailsWith`) rend cette valeur : il n'est jamais lancé.
- Relevés pendant la 095 dans `core/src/test/kotlin/io/github/pobsteta/surculus/core/session/SessionControleTest.kt`
  (sur `main`, aa02f2c) :
  - l. 183 : « hauteur sans etat est refusee, l'etat d'abord, mais acceptee sur un plant mort » ;
  - l. 213 : « plant hors rayon plus 0,10 m demande confirmation et n'ecrit rien, confirme il est ecrit » ;
  - l. 431 : « remplace sur un plant absent est accepte, sur un plant sain ou sans etat refuse » ;
  - l. 606 : « le contexte pose l'operateur de la tournee sur chaque ligne ».
- Le même piège a déjà touché trois tests de la 095 (corrigés par le `testeur`).

## Décision de Pascal (2026-11-07)

**« Mission de réparation »** : le `testeur` seul réécrit les quatre tests pour qu'ils tournent, puis dit lesquels
passent. Un test qui échoue une fois lancé est peut-être un vrai défaut : on ne touche pas au code, on le dit.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : corps de test en bloc `{ … }` rendant `Unit` | **appuyer** |
| A-t-il les moyens ? | oui : le rapport JUnit dit quels tests ont tourné | |

**Temps de la main :** appuyer.

## Ma réponse d'abord

_Rempli par Pascal seul._

## L'Homme

- **Agent :** `testeur` seul. Il lit le test et la spec ; il n'a pas à lire le code de `SessionControle` pour
  réparer la forme d'un test.
- **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** changer une assertion ou l'intention d'un test ; toucher au code de production ;
  chercher le piège dans les autres fichiers ou modules (hors périmètre, décision de Pascal).

## Harnais

- **Testeur peut :** modifier `SessionControleTest.kt` ; lancer les tests.
- **Ne doit pas :** toucher au code de production, à `docs/`, à `main`.
- **Sources autorisées :** spec § 4, § 6 ; le test lui-même.
- **Vérification exigée :** dans le rapport JUnit de `:core:test`
  (`core/build/test-results/test/TEST-*SessionControleTest.xml`), le nombre de cas lancés égale le nombre de
  `@Test` du fichier ; les quatre noms y figurent. Les autres tests `= with(...)` du même fichier sont vérifiés de la
  même façon : s'il en manque un dans le rapport, il est réparé aussi (même fichier, même piège).

Si un test réparé échoue, **le `testeur` s'arrête et le dit tout de suite** : quel test, quel message, ce qu'il
croit que la spec exige. Il ne change ni l'assertion ni le code ; l'orchestrateur pose la question à Pascal.

## Valeur provisoire (chiffre, réglage)

- Aucune.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** 095 (#158) et 094 (#159), fusionnées.
- **CI rouge attendue :** aucune, sauf si un test réparé révèle un défaut (alors on s'arrête).

## Tests

- Les quatre tests apparaissent dans le rapport JUnit et passent.
- Nombre de cas du rapport = nombre de `@Test` du fichier.
- `.claude/test-command` au code 0 ; CI verte.

## Terrain

- Aucun écran touché : pas d'installation ni de capture.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Qui tranche :** Pascal (fusion ; tout défaut révélé).

## Comment saura-t-on que ça marche ?

Le rapport JUnit compte autant de cas que le fichier compte de `@Test`, et tous sont verts.
