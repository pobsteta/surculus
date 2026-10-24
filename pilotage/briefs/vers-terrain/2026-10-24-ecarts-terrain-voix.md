# Brief `terrain` — écarts de `terrain-voix` vus par Surculus

*Établi le 24 octobre 2026 depuis le dépôt `surculus`, à partir de `terrain` v0.6.0
(`d840329`), après la mission 013 de Surculus (PR #75, dictée Vosk). Envoyé sur décision de
Pascal (2026-10-24, « oui pour les 3 »).*

## Pourquoi

Le 2026-10-24, Pascal a décidé que Surculus prend `terrain-voix` (« prendre terrain-voix
dans surculus »). La mission 013 l'a branché sur les cas d'acceptation de la dictée de
Surculus (`fixtures/dictee/`). Cinq comportements de `terrain-voix` ne tiennent pas ces cas.
Surculus les couvre aujourd'hui dans son propre code (`core/.../dictee/VerificationModele.kt`,
`AnalyseurDictee.kt`). Les corriger dans `terrain` servirait aussi Marculus et Groma.

## Constats (à `d840329`)

| # | Constat | Lu à | Effet |
|---|---|---|---|
| 1 | `installer` efface le dossier du modèle **sur tout échec**, et l'efface même avant de décompresser | `terrain-voix/src/main/kotlin/fr/nemeton/terrain/voix/InstallationModele.kt` l. 80 et 83 | un import refusé, ou une archive illisible, détruit le modèle déjà installé |
| 2 | Avec `sha256 = null` (valeur par défaut), l'empreinte n'est **pas contrôlée** | même fichier, l. 18, 24 et 76 | un appelant qui oublie l'empreinte installe n'importe quelle archive |
| 3 | Une archive **vide** est acceptée si l'empreinte donnée est celle du contenu vide ; le témoin est posé sur un dossier vide | même fichier, l. 75-83 | modèle « installé » sans modèle |
| 4 | Aucun contrôle du **nom** du modèle, ni de la **forme** de l'empreinte attendue (64 caractères hexadécimaux) : une empreinte vide ou mal formée n'est refusée que parce que la comparaison échoue | même fichier, l. 75-78 | refus sans message juste |
| 5 | `GrammaireFermee.jetons` **supprime** les `[unk]` : « sain [unk] » devient « sain » | `Langue.kt` l. 100-101 | une phrase en partie inconnue devient une commande approchée |
| 6 | `VocabulaireVosk.motsConnus` rend **tous** les candidats comme connus quand `graph/Gr.fst` est absent ou d'un autre format | `VocabulaireVosk.kt` l. 27, 30, 39 et 49 | « tout est connu » et « illisible » se confondent : le garde-fou ne bloque jamais |

## Demandé

Pour chaque constat, que `terrain` choisisse sa correction et l'écrive dans son ADR ou sa
spec. Ce que Surculus attend, et teste déjà de son côté :

1. un import refusé **laisse intact** le modèle déjà installé (décompresser à côté, puis
   remplacer) ;
2. l'empreinte est **obligatoire**, ou son absence est un choix explicite de l'appelant ;
3. une archive vide est refusée ;
4. la forme de l'empreinte attendue est contrôlée, avec un message qui le dit ;
5. une phrase qui contient `[unk]` peut être refusée entière (une fonction qui le dit, ou
   `jetons` documenté comme approximatif) ;
6. `motsConnus` distingue « illisible » de « tout est connu ».

## Critère d'acceptation

- Chaque constat a un test dans `terrain-voix` qui le montre corrigé, ou une ligne de
  l'ADR de `terrain` qui le garde et dit pourquoi.
- Côté Surculus : le test « ECART terrain-voix » de
  `core/src/test/kotlin/io/github/pobsteta/surculus/core/dictee/InstallationModeleTest.kt`
  s'inverse à la montée de version (son commentaire donne l'assertion), et les 162 cas de
  `DicteeRejeuTest` restent verts.
