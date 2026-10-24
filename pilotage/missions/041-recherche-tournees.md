# Mission 041 — Recherche et puce « Rendues » dans la liste des tournées

> Fiche écrite par l'orchestrateur le 2026-10-24, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Demandée par Pascal le 2026-10-24 :
> « il faut comme dans marculus et groma une zone de recherche et le bouton terminé ou en
> cours ou à définir pour ce qui correspond le mieux à surculus », puis « Rendues » et
> « Mission 041 » (à part, sur la branche de la 040).

## Mission

**En une phrase (quoi, et pourquoi) :** Ajouter en tête de la liste des tournées une zone de recherche (forêt, opérateur, date, nom) et une puce « Rendues (n) » qui bascule entre tournées ouvertes et rendues, comme dans Groma et Marculus, pour que le forestier retrouve vite la tournée du jour sans voir celles déjà rendues.

**Redite par l'agent :** Mettre en tête de la liste des tournées une zone de recherche (forêt, opérateur, date, nom du dossier, sans accents ni casse) et une puce « Rendues (n) » qui bascule entre tournées ouvertes et rendues, pour retrouver vite la tournée du jour ; l'état « rendue » reste porté par un seul point (`LigneTournee.rendue`, toujours faux d'ici l'export), sans rien inventer dans le fichier.

## Vérifié à la source

Constats vérifiés par l'orchestrateur le 2026-10-24.

| Constat | Lu à | Source du constat |
|---|---|---|
| Groma : `OutlinedTextField` (icône `Search`, une ligne) et `FilterChip` « Terminés (n) » sur une même ligne ; `partition` en terminés / en cours ; si la recherche trouve dans l'autre liste, une ligne le signale ; recherche sans accents ni casse (`normaliser`) | `groma`, `android/app/src/main/kotlin/groma/app/ui/EcranProjets.kt` l. 78-79, 110-140, 317-320 | orchestrateur |
| Marculus : même motif, puce « Martelés (n) », et message quand rien ne correspond dans la liste affichée | `marculus`, `ui/contextes/ListeContextesScreen.kt` l. 126, 226-310 ; `res/values/strings.xml` l. 29, 49-55 | orchestrateur |
| Spec : la liste montre forêt, opérateur, date de préparation, nombre de saisies, état (ouverte, rendue) ; la tournée passe *rendue* à l'export | `docs/spec.md` § 4.1 (l. 86-87) et l. 259 | orchestrateur |
| L'état « rendue » n'existe pas encore dans le code : `EcranTournees.kt` l. 165 (« l'état "rendue" vient avec l'export ») ; `LigneTournee` n'a pas d'état | `app/src/main/kotlin/io/github/pobsteta/surculus/tournee/EcranTournees.kt:165` ; `tournee/DepotTournees.kt:26-35` | orchestrateur |
| Champs lisibles : `ProjetLu(nom, foret, operateur, prepareLe, …)` | `gpkg/src/main/kotlin/io/github/pobsteta/surculus/gpkg/Tournee.kt:50-57` | orchestrateur |

Règles, avec leur auteur :
- **La limite entre en cours et terminé est « rendue »**, le mot de la spec § 4.1 : Pascal, 2026-10-24.
- **On n'invente pas la marque « rendue »** : elle vient avec la mission d'export. D'ici là, l'état est porté par un seul point du code (par exemple `LigneTournee.rendue`), qui vaut « ouverte » pour toutes, et la puce affiche « Rendues (0) ».
- **L'écran sert le forestier en forêt** : cibles d'au moins 48 dp, texte lisible au soleil (`CLAUDE.md`).
- **Garde-fou = test vu rouge** et **bilan des écarts avant rendu** : Pascal, bilan du 2026-10-21.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : modèle à recopier dans Groma | |
| A-t-il les moyens ? | oui ; l'icône `Search` est dans `material-icons-core` si la 040 l'a ajoutée, sinon il le dit | |

**Temps de la main :** **appuyer**.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent. L'orchestrateur ne le remplit pas._

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :**
  - écrire la marque « rendue » ou l'export (mission d'export) ; s'il voit un moyen de la déduire du fichier, il le dit sans le coder ;
  - un tri, un mode Kanban, d'autres filtres ;
  - toucher à la navigation de la 040 ou au thème de la 038.

## Harnais

- **Peut :** modifier `tournee/EcranTournees.kt` et `tournee/DepotTournees.kt` ; ajouter les chaînes à `strings.xml` ; écrire la fonction de recherche en Kotlin pur, testable sans Android.
- **Ne doit pas :** toucher à `main` ; modifier `fixtures/` ; ajouter une dépendance.
- **Sources autorisées :** cette fiche ; `docs/spec.md` § 4.1 ; `groma` et `marculus` (`~/dev/`, en lecture).

## Tests

- Tests JVM de la recherche : sans accents ni casse (« Couchey » trouvé par « couchéy » et « COUCHEY ») ; sur chaque champ (forêt, opérateur, date, nom du dossier) ; tournée refusée (`tournee == null`) trouvée par le nom du dossier ; recherche vide = tout. Tests du partage ouvertes / rendues. **Vu rouge** une fois.
- Commande du projet : `.claude/test-command`, au code 0.
- **Bilan des écarts** : ce qui change à l'écran.

## Terrain

- **Échantillon que Pascal vérifiera :** la variante fumée, avec deux tournées au moins : chercher par forêt et par opérateur, avec des gants si possible ; la puce affiche « Rendues (0) ».

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours ; au-delà, la décision remonte à Pascal.
- **Point de suivi :** à l'ouverture de la PR.

## Comment saura-t-on que ça marche ?

Pascal tape trois lettres de la forêt et ne voit plus que sa tournée ; quand l'export existera, les tournées rendues passeront derrière la puce sans autre changement d'écran.
