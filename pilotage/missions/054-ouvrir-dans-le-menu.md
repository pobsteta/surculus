# Mission 054 — « Ouvrir un dossier » et « Ouvrir un .zip » dans le menu « ⋮ »

> Fiche écrite par l'orchestrateur le 2026-10-27, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Demandée par Pascal le 2026-10-27 :
> « Ajoute les boutons "Ouvrir un dossier" et "Ouvrir un .zip" comme menu dans le menu
> "hamburger" ».
> **Lancement :** le 2026-10-27, depuis `main` (c95afcf, mission 050 fusionnée). La case
> « Ortho : Wi-Fi seulement » n'est **pas** dans cette mission : elle part avec la 055 (fonds de
> carte comme Marculus, décision de Pascal « (b) exactement comme marculus »).

## Mission

**En une phrase (quoi, et pourquoi) :** Remplacer les deux boutons « Ouvrir un dossier » et « Ouvrir un .zip » de l'accueil par deux entrées en tête du menu « ⋮ », avant Paramètres et À propos, comme l'« Importer » de Marculus, pour que l'accueil ne montre que la recherche et la liste des tournées.

**Redite par l'agent :** _à remplir par l'agent avant d'agir._

## Vérifié à la source

Constats vérifiés par l'orchestrateur le 2026-10-27.

| Constat | Lu à | Source du constat |
|---|---|---|
| Accueil de Surculus : deux `Button` (64 dp) « Ouvrir un dossier » et « Ouvrir un .zip » en tête, qui lancent `OpenDocumentTree` et `OpenDocument` | `tournee/EcranTournees.kt` l. 115-118, 160-170 ; `strings_tournees.xml` l. 34-35 (`main` c95afcf) | orchestrateur |
| Menu « ⋮ » : `ENTREES_MENU = [PARAMETRES, A_PROPOS]` (046) | `Navigation.kt` l. 7-9 | orchestrateur |
| Liste vide : « Aucune tournée. Ouvrez le dossier ou le .zip de sommieR. » | `strings_tournees.xml` l. 37 | orchestrateur |
| Marculus met son « Importer » en tête du menu de l'application, avant Référentiels, Paramètres, À propos | `marculus` `ui/contextes/ListeContextesScreen.kt` (menu, `R.string.lot_importer`) | orchestrateur |
| Le dossier `Download/Surculus` du téléphone de Pascal porte la tournée de démonstration Couchey | `pilotage/journal.md`, 2026-10-27 | orchestrateur |

Règles, avec leur auteur :
- **Les deux ouvertures deviennent des entrées du menu « ⋮ »**, avec leurs libellés actuels : Pascal, 2026-10-27.
- **Ordre du menu** : Ouvrir un dossier, Ouvrir un .zip, Paramètres, À propos (comme Marculus, proposition de l'orchestrateur).
- **Message de la liste vide** : « Aucune tournée. Ouvrez le dossier ou le .zip de sommieR depuis le menu ⋮. » (proposition de l'orchestrateur, pour qu'un premier lancement sache où chercher).
- **Le comportement de l'import ne change pas** (sélecteurs, contrôles, refus, message d'occupation pendant un import).
- **Le sélecteur s'ouvre d'abord dans `Download/Surculus`** si Android le permet (comme Groma, `DOSSIER_PAR_DEFAUT` vers `primary:Download/groma`), sinon l'agent le dit.
- **Styles de Marculus** (042), cibles d'au moins 48 dp (`CLAUDE.md`).
- **Garde-fou = test vu rouge** et **bilan des écarts avant rendu** : Pascal, bilan du 2026-10-21.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : `ENTREES_MENU` (046), sélecteurs existants | |
| A-t-il les moyens ? | oui | |

**Temps de la main :** **appuyer**.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent. L'orchestrateur ne le remplit pas._

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** la case « Ortho : Wi-Fi seulement » et l'ortho (055) ; le thème (051) ; la vue Kanban ; l'import lui-même.

## Harnais

- **Peut :** modifier `Navigation.kt` (`EntreeMenu`, `ENTREES_MENU`), `tournee/EcranTournees.kt`, `strings_tournees.xml`, les tests.
- **Ne doit pas :** toucher à `main` ; modifier `fixtures/` ou `pilotage/journal.md` ; ajouter une dépendance.
- **Sources autorisées :** cette fiche ; `CLAUDE.md` ; `marculus` et `groma` (`~/dev/`, en lecture).

## Tests

- `NavigationTest` : `ENTREES_MENU` vaut [Ouvrir un dossier, Ouvrir un .zip, Paramètres, À propos] ; lecture des sources : plus de bouton d'ouverture sur l'accueil. **Vu rouge** une fois.
- Commande du projet : `.claude/test-command`, au code 0.
- **Bilan des écarts.**

## Terrain

- **Échantillon que Pascal vérifiera :** la release qui suit la fusion : l'accueil n'a plus les deux boutons verts ; « ⋮ » → « Ouvrir un .zip » → `Download/Surculus/couchey-demo-sommier.zip` importe la tournée Couchey.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal. Critère d'arrêt : 3 allers-retours. Point de suivi : à l'ouverture de la PR.

## Comment saura-t-on que ça marche ?

Pascal importe Couchey depuis le menu « ⋮ », et l'accueil ne montre plus que la recherche et ses tournées.
