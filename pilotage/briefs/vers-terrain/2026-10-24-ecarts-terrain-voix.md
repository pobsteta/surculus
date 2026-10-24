# Brief `terrain` — écarts de `terrain-voix` vus par Surculus

*Établi le 24 octobre 2026 depuis le dépôt `surculus`, à partir de `terrain` v0.6.0
(`d840329`), après la mission 013 de Surculus (PR #75, dictée Vosk). Envoyé sur décision de
Pascal (2026-10-24, « oui pour les 3 »).*

## Pourquoi

Le 2026-10-24, Pascal a décidé que Surculus prend `terrain-voix` (« prendre terrain-voix
dans surculus »). La mission 013 l'a branché sur les cas d'acceptation de la dictée de
Surculus (`fixtures/dictee/`). Quatre comportements d'`InstallationModele` ne tiennent pas
ces cas ; Surculus les couvre dans son propre code
(`core/src/main/kotlin/io/github/pobsteta/surculus/core/dictee/VerificationModele.kt`,
appelé avant `installer`) et les teste. Un cinquième point, un choix documenté de
`VocabulaireVosk`, est seulement signalé. Les corriger dans `terrain` servirait aussi
Marculus et Groma.

## Constats (à `d840329`)

Chemins sous `terrain-voix/src/main/kotlin/fr/nemeton/terrain/voix/`.

| # | Constat | Lu à | Effet | Surculus |
|---|---|---|---|---|
| 1 | `installer` efface le dossier du modèle **avant de décompresser** et **sur tout échec** ; `telecharger` aussi sur échec | `InstallationModele.kt` l. 67, 80 et 83 | un import refusé, ou une archive illisible, détruit le modèle déjà installé | fixé par le test « ECART terrain-voix » de `InstallationModeleTest.kt` ; une empreinte fausse n'atteint pas `installer`, mais une archive à bonne empreinte et illisible efface encore l'ancien modèle |
| 2 | Avec `sha256 = null` (valeur par défaut), l'empreinte n'est **pas contrôlée** | `InstallationModele.kt` l. 18, 24 et 76 | un appelant qui oublie l'empreinte installe n'importe quelle archive | passe toujours une empreinte (`ModeleVoskImport.kt`) |
| 3 | Une archive **vide** est acceptée (avec `sha256 = null`, ou avec l'empreinte du contenu vide) ; le dossier est créé vide par `mkdirs` puis le témoin posé | `InstallationModele.kt` l. 75-83 et 86 | modèle « installé » sans modèle | refuse le contenu vide (cas DI-305) |
| 4 | La **forme** de l'empreinte attendue (64 caractères hexadécimaux) n'est pas contrôlée : une empreinte vide ou mal formée n'est refusée que parce que la comparaison échoue, avec le message « Empreinte SHA-256 inattendue » | `InstallationModele.kt` l. 75-78 | refus sans message juste | contrôle la forme (cas DI-306, DI-307) |
| 5 | *(signalé)* `motsConnus` rend **tous** les candidats comme connus quand `graph/Gr.fst` est absent, d'un autre format ou illisible ; c'est un choix documenté (« tout accepter si illisible ») | `VocabulaireVosk.kt` l. 23, 27, 30, 39, 42 et 49 | « tout est connu » et « illisible » se confondent : le garde-fou ne bloque jamais | n'utilise pas `VocabulaireVosk` (`docs/dependances.md`) ; le vocabulaire est prouvé par un test sur la liste versionnée |

## Demandé

Pour chaque constat, que `terrain` choisisse sa correction, ou garde le comportement, et
l'écrive dans son ADR ou sa spec. Ce que Surculus attend :

1. un import refusé **laisse intact** le modèle déjà installé (décompresser à côté, puis
   remplacer) ;
2. l'empreinte est **obligatoire**, ou son absence est un choix explicite de l'appelant ;
3. une archive vide est refusée ;
4. la forme de l'empreinte attendue est contrôlée, avec un message qui le dit ;
5. *(pour Marculus et Groma)* que `motsConnus` permette de distinguer « illisible » de
   « tout est connu », si `terrain` juge le choix actuel à revoir.

## Critère d'acceptation

- Chaque constat a un test dans `terrain-voix` qui le montre corrigé, ou une ligne de
  l'ADR de `terrain` qui le garde et dit pourquoi.
- Côté Surculus : le test « ECART terrain-voix » de
  `core/src/test/kotlin/io/github/pobsteta/surculus/core/dictee/InstallationModeleTest.kt`
  s'inverse à la montée de version (son commentaire donne l'assertion), et les 162 cas de
  `DicteeRejeuTest` restent verts.
