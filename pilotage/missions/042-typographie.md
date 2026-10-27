# Mission 042 — Les styles et les tailles de texte de Marculus

> Fiche écrite par l'orchestrateur le 2026-10-24, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Demandée par Pascal le 2026-10-24 :
> « utilise la meme police que marculus taille, etc », puis, entre trois issues posées par
> l'orchestrateur (à l'identique, styles aux tailles de Surculus, essai au soleil) :
> « 2, les styles de Marculus et groma aux tailles de Surculus », **puis, le même jour,
> avant tout lancement : « Je veux dans groma et Surculus les tailles de police de
> Marculus »**. Cette fiche suit la seconde décision.
> **Lancement :** le 2026-10-27, depuis `main` (a0186c7, missions 040 à 044 fusionnées).

## Mission

**En une phrase (quoi, et pourquoi) :** Remplacer chaque taille de texte écrite à la main dans Surculus (`fontSize = …sp`) par le style de `MaterialTheme.typography` que Marculus donne au même genre de texte, avec les tailles par défaut de Material 3 comme Marculus (aucune `Typography` propre), pour que Surculus, Marculus et Groma aient le même texte.

**Redite par l'agent :** Remplacer, dans Surculus, les 83 tailles de texte écrites à la main par les styles de Material 3 que Marculus donne au même genre de texte (donc les mêmes tailles qu'à Marculus et à Groma), sans `Typography` propre, afin que les trois applications aient le même texte, en gardant chaque bouton à 48 dp au moins ; les textes sans équivalent dans Marculus restent à la décision de Pascal.

## Vérifié à la source

Constats vérifiés par l'orchestrateur le 2026-10-24.

| Constat | Lu à | Source du constat |
|---|---|---|
| Police : aucune des trois applications ne déclare de `fontFamily` ni de `Typography` : police système du téléphone | `marculus` v0.65.8 `ui/theme/Theme.kt` ; `groma/android/app/src/main/kotlin` ; Surculus `ui/theme/Theme.kt` (038) | orchestrateur |
| Marculus écrit ses textes en styles : `bodySmall` (61), `titleSmall` (23), `bodyMedium` (18), `titleMedium` (16), `labelSmall` (10), `bodyLarge` (6), `labelMedium` (4), `titleLarge` (3) ; 2 `fontSize` seulement | `marculus/app/src/main/kotlin` (décompte `grep`) | orchestrateur |
| Groma de même : `bodySmall` (23), `bodyMedium` (8), `titleLarge` (6), `titleMedium` (5), `bodyLarge` (3), `labelLarge` (2) ; 5 `fontSize` | `groma/android/app/src/main/kotlin` | orchestrateur |
| Surculus écrit ses tailles à la main, aucun style : 18 sp (28), 20 sp (14), 16 sp (12), 14 sp (10), 22 sp (5), 28 sp (3), 24 sp (2), 44 sp (2), et le cap de guidage en 72 sp | `app/src/main/kotlin` (décompte `grep`, `main` avant la 040) ; `docs/adr/0008-bibliotheques-communes.md` l. 71-79 | orchestrateur |

Règles, avec leur auteur :
- **Les tailles de police de Marculus** : Pascal, 2026-10-24 (« Je veux dans groma et Surculus les tailles de police de Marculus »). Marculus n'a pas de `Typography` propre : ce sont les valeurs par défaut de Material 3 (BOM 2026.02.01), comme pour les couleurs de la 038.
- **Le texte rapetisse, c'est su et assumé** : la plupart des 18-20 sp passent à 12-16 sp. L'orchestrateur l'a dit à Pascal avant sa décision (compromis payé par le forestier en forêt, `CLAUDE.md`, « plier ou tenir ») ; l'essai au soleil et avec des gants le jugera.
- **Le style suit Marculus** : pour chaque genre de texte (titre d'écran, texte d'un bouton, ligne de liste, message, aide), le style que Marculus emploie pour le même genre. Quand Marculus n'a pas d'équivalent (le cap de guidage en 72 sp, les grands chiffres de l'écran d'essai en 44 sp), l'agent ne choisit pas : il le met dans le tableau et Pascal tranche.
- **Garde-fou = test vu rouge** et **bilan des écarts avant rendu** : Pascal, bilan du 2026-10-21.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : `Typography` de Material 3, `MaterialTheme(typography = …)` | |
| A-t-il les moyens ? | oui | |

**Temps de la main :** **appuyer**. **Décisions de Pascal du 2026-10-27** (« 1. oui, 2. code directement ») :
- l'agent **code directement**, sans tableau validé avant : pour chaque texte, le style que Marculus emploie pour le même genre de texte. Le tableau ci-dessous (2026-10-24) sert de guide ; il date d'avant les missions 040 à 045, l'agent l'applique aussi aux textes ajoutés depuis (liste des tournées, recherche, vue Kanban, « À propos », menu « ⋮ »). Il rend dans la PR le tableau final (fichier:ligne, taille avant, style après) ;
- **trois exceptions gardées à leur taille** : la distance à la cible du guidage (72 sp, `EcranGuidage.kt`) et les deux grands chiffres de l'écran d'essai GNSS, qualité du fix et précision (44 sp, `EcranEssaiGnss.kt`) ; tout autre texte sans équivalent dans Marculus est rendu dans la PR, sans être choisi par l'agent ;
- Pascal juge sur la release qui suit la fusion (plus de variante fumée).

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent. L'orchestrateur ne le remplit pas._

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :**
  - changer une couleur ou une police ;
  - écrire une `Typography` propre à Surculus ;
  - toucher aux couleurs de la 038, à la navigation de la 040, à la recherche de la 041, à la logique de la vue Kanban (043) ou des réglages (044) ;
  - ajouter une police embarquée.

## Harnais

- **Peut :** remplacer les `fontSize` (et les `fontWeight` qui vont avec) de `app/src/main/kotlin` par des `style = MaterialTheme.typography.…` ; écrire les tests.
- **Ne doit pas :** toucher à `main` ; modifier `fixtures/` ; ajouter une dépendance.
- **Sources autorisées :** cette fiche ; `marculus` et `groma` (`~/dev/`, en lecture) ; la doc de Compose Material 3.

## Tableau des styles (guide du 2026-10-24 ; plus à valider avant le code)

_Dressé par l'agent le 2026-10-24, sans toucher au code. 83 occurrences de `fontSize =` dans `app/src/main/kotlin` (base : branche de la 041, après la 038 et la 040). Tailles par défaut de Material 3 (BOM 2026.02.01) : displayLarge 57, displayMedium 45, headlineMedium 28, headlineSmall 24, titleLarge 22, titleMedium 16 (Medium), titleSmall 14 (Medium), bodyLarge 16, bodyMedium 14, bodySmall 12, labelLarge 14 (Medium), labelMedium 12 (Medium), labelSmall 11 (Medium)._

### Les styles que Marculus donne à chaque genre de texte

Fichiers de `~/dev/marculus/app/src/main/kotlin/io/github/pobsteta/marculus/ui/` (Marculus v0.65.8).

| Genre | Style retenu | Taille M3 | Marculus (fichier:ligne) |
|---|---|---|---|
| Titre d'écran, nom d'une carte de liste | `titleLarge` | 22 | `contextes/ListeContextesScreen.kt:691` |
| Titre de section | `titleMedium` | 16 | `parametres/ParametresScreen.kt:264` |
| Sous-titre | `titleSmall` | 14 | `contextes/CreationContexteScreen.kt:295` |
| Texte d'un bouton | aucun style (le bouton impose `labelLarge`) | 14 | `contextes/CreationContexteScreen.kt:289`, `ListeContextesScreen.kt:196` |
| Texte d'une pastille (`FilterChip`) | aucun style (`labelLarge`) | 14 | `contextes/ListeContextesScreen.kt:259` |
| Entrée de menu déroulant | aucun style (`bodyLarge`) | 16 | `contextes/ListeContextesScreen.kt:202` |
| Texte saisi dans un champ | aucun `textStyle` (`bodyLarge`) | 16 | `feuille/FeuilleMartelageScreen.kt:523` |
| Message d'erreur | aucun style (`bodyLarge`), couleur `error` | 16 | `contextes/CreationContexteScreen.kt:467` |
| Ligne de détail, message d'état | `bodyMedium` | 14 | `contextes/ListeContextesScreen.kt:711`, `statut/StatutHistoriqueScreen.kt:272` |
| Aide, description, texte secondaire | `bodySmall` | 12 | `parametres/ParametresScreen.kt:265`, `ListeContextesScreen.kt:695` |
| Badge d'état (GNSS) | 12 sp Medium écrit à la main = `labelMedium` | 12 | `gnss/BadgeFix.kt:117` ; `carte/CarteScreen.kt:795` |
| Légende, petit libellé | `labelSmall` | 11 | `carte/CarteScreen.kt:762` |

### A. Les textes qui ont un équivalent dans Marculus

« Graisse » = celle écrite dans Surculus (`—` : aucune). Le style fixe sa propre graisse (voir question 2). « Après » = taille M3 du style. Chemins relatifs à `app/src/main/kotlin/io/github/pobsteta/surculus/`.

| Surculus (fichier:ligne) | Texte | Graisse | Avant | Style | Après |
|---|---|---|---|---|---|
| `sauvegarde/EcranRestauration.kt:68` | titre d'écran | — | 28 | `titleLarge` | 22 |
| `EcranRestauration.kt:70` | bouton « choisir » (h 72 dp) | — | 20 | bouton | 14 |
| `EcranRestauration.kt:72` | résultat de la restauration | — | 20 | `bodyMedium` | 14 |
| `tournee/EcranTournees.kt:136` | bouton importer dossier | — | 18 | bouton | 14 |
| `EcranTournees.kt:139` | bouton importer zip | — | 18 | bouton | 14 |
| `EcranTournees.kt:143` | libellé de l'interrupteur Wi-Fi | — | 18 | `bodyLarge` (défaut) | 16 |
| `EcranTournees.kt:146` | message d'import (rouge/noir) | — | 16 | `bodyLarge` (défaut) | 16 |
| `EcranTournees.kt:153` | « aucune tournée » | — | 18 | `bodyMedium` | 14 |
| `EcranTournees.kt:162` | texte du champ de recherche | — | 18 | `bodyLarge` (défaut, `textStyle` retiré) | 16 |
| `EcranTournees.kt:168` | pastille « rendues » | — | 16 | pastille | 14 |
| `EcranTournees.kt:179` | « N autres tournées… » | — | 14 | `bodySmall` | 12 |
| `EcranTournees.kt:192` | message de liste vide | — | 18 | `bodyMedium` | 14 |
| `EcranTournees.kt:243` | entrée de menu « essai » | — | 18 | menu | 16 |
| `EcranTournees.kt:247` | entrée de menu « restauration » | — | 18 | menu | 16 |
| `EcranTournees.kt:264` | nom de la forêt (carte de tournée) | Bold | 24 | `titleLarge` | 22 |
| `EcranTournees.kt:266` | tournée illisible (rouge) | — | 16 | `bodyMedium` | 14 |
| `EcranTournees.kt:271` | « préparée le… par… » | — | 16 | `bodyMedium` | 14 |
| `EcranTournees.kt:273` | « N saisies » | — | 16 | `bodyMedium` | 14 |
| `EcranTournees.kt:283` | état de l'ortho (coloré) | Bold | 18 | `bodyMedium` | 14 |
| `EcranTournees.kt:285` | bouton « relancer l'ortho » (h 48 dp) | — | 14 | bouton | 14 (inchangé) |
| `essai/EcranEssaiGnss.kt:160` | source du lien (en marche, connecté…) | — | 16 | `bodyMedium` | 14 |
| `EcranEssaiGnss.kt:162` | erreur (rouge) | — | 16 | `bodyLarge` (défaut) | 16 |
| `EcranEssaiGnss.kt:163` | erreur NTRIP (rouge) | — | 16 | `bodyLarge` (défaut) | 16 |
| `EcranEssaiGnss.kt:171` | bouton « démarrer » (h 64 dp) | — | 20 | bouton | 14 |
| `EcranEssaiGnss.kt:175` | bouton « arrêter » (h 64 dp) | — | 20 | bouton | 14 |
| `EcranEssaiGnss.kt:180` | permissions manquantes (rouge) | Bold | 16 | `bodyLarge` (défaut) | 16 |
| `EcranEssaiGnss.kt:183` | « enregistré » | — | 14 | `bodyMedium` | 14 (inchangé) |
| `EcranEssaiGnss.kt:185` | titre « Réglages » | Bold | 22 | `titleMedium` | 16 |
| `EcranEssaiGnss.kt:197` | bouton « choisir le récepteur » (h 56 dp) | — | 18 | bouton | 14 |
| `EcranEssaiGnss.kt:204` | entrée de menu (récepteur) | — | 18 | menu | 16 |
| `EcranEssaiGnss.kt:229` | bouton « charger les points » (h 48 dp) | — | 16 | bouton | 14 |
| `EcranEssaiGnss.kt:234` | entrée de menu (point de montage) | — | 18 | menu | 16 |
| `EcranEssaiGnss.kt:255` | aide du compte d'essai | — | 14 | `bodySmall` | 12 |
| `EcranEssaiGnss.kt:273` | âge du fix (sur fond coloré) | — | 20 | `bodyLarge` (défaut) | 16 |
| `EcranEssaiGnss.kt:274` | âge des corrections | — | 20 | `bodyLarge` (défaut) | 16 |
| `EcranEssaiGnss.kt:275` | drapeaux « ▲ … » | Bold | 20 | `bodyLarge` (défaut) | 16 |
| `EcranEssaiGnss.kt:276` | Lambert-93 X | — | 18 | `bodyLarge` (défaut) | 16 |
| `EcranEssaiGnss.kt:277` | Lambert-93 Y | — | 18 | `bodyLarge` (défaut) | 16 |
| `EcranEssaiGnss.kt:279` | « hors de France » | Bold | 18 | `bodyLarge` (défaut) | 16 |
| `carte/EcranGuidage.kt:68` | « ouverture de la carte… » | — | 20 | `bodyLarge` (défaut) | 16 |
| `EcranGuidage.kt:70` | carte refusée (rouge) | — | 18 | `bodyLarge` (défaut) | 16 |
| `EcranGuidage.kt:71` | bouton « retour » (h 56 dp) | — | 18 | bouton | 14 |
| `EcranGuidage.kt:76` | placette introuvable (rouge) | — | 18 | `bodyLarge` (défaut) | 16 |
| `EcranGuidage.kt:143` | bouton « retour » (h 56 dp) | — | 18 | bouton | 14 |
| `EcranGuidage.kt:147` | « guidage vers CODE » | — | 20 | `titleMedium` | 16 |
| `EcranGuidage.kt:150` | « hors de France » (rouge) | Bold | 22 | `bodyLarge` (défaut) | 16 |
| `EcranGuidage.kt:152` | « sans position » (rouge) | Bold | 22 | `bodyLarge` (défaut) | 16 |
| `EcranGuidage.kt:162` | « arrivé » (vert) | Bold | 24 | `titleLarge` | 22 |
| `EcranGuidage.kt:169` | « nord en haut » | — | 12 | `bodySmall` | 12 (inchangé) |
| `EcranGuidage.kt:184` | bouton « rattacher » (h 64 dp) | — | 20 | bouton | 14 |
| `EcranGuidage.kt:186` | moyenne en cours | — | 16 | `bodyMedium` | 14 |
| `EcranGuidage.kt:192` | bouton « son » (h 56 dp) | — | 18 | bouton | 14 |
| `EcranGuidage.kt:194` | « rien enregistré » | — | 11 | `labelSmall` | 11 (inchangé) |
| `EcranGuidage.kt:204` | rattaché, identité (orange) | Bold | 18 | `bodyLarge` (défaut) | 16 |
| `EcranGuidage.kt:206` | rattaché (vert) | Bold | 18 | `bodyLarge` (défaut) | 16 |
| `EcranGuidage.kt:216` | verdict refus (rouge) | Bold | 18 | `bodyLarge` (défaut) | 16 |
| `EcranGuidage.kt:218` | verdict à confirmer (orange) | — | 18 | `bodyLarge` (défaut) | 16 |
| `carte/EcranCarte.kt:84` | « ouverture de la carte… » | — | 20 | `bodyLarge` (défaut) | 16 |
| `EcranCarte.kt:86` | carte refusée (rouge) | — | 18 | `bodyLarge` (défaut) | 16 |
| `EcranCarte.kt:87` | bouton « retour » (h 56 dp) | — | 18 | bouton | 14 |
| `EcranCarte.kt:192` | bouton « retour » (h 56 dp) | — | 18 | bouton | 14 |
| `EcranCarte.kt:196` | « hors de France » (rouge) | Bold | 14 | `bodyLarge` (défaut) | 16 (grossit de 2) |
| `EcranCarte.kt:198` | permission refusée (rouge) | — | 14 | `bodyLarge` (défaut) | 16 (grossit de 2) |
| `EcranCarte.kt:279` | légende « jamais » (rouge) | Bold | 14 | `bodyMedium` | 14 (inchangé) |
| `EcranCarte.kt:280` | légende « à programmer » (orange) | Bold | 14 | `bodyMedium` | 14 (inchangé) |
| `EcranCarte.kt:281` | légende « à jour » (vert) | Bold | 14 | `bodyMedium` | 14 (inchangé) |
| `EcranCarte.kt:283` | légende « inconnu » (noir) | Bold | 14 | `bodyMedium` | 14 (inchangé) |
| `EcranCarte.kt:287` | « N anomalies » (orange) | — | 14 | `bodyMedium` | 14 (inchangé) |
| `EcranCarte.kt:293` | bouton « ma position » (h 64 dp) | — | 16 | bouton | 14 |
| `EcranCarte.kt:303` | bouton « guider vers… » (h 64 dp) | — | 18 | bouton | 14 |
| `EcranCarte.kt:307` | bouton « liste des placettes » (h 64 dp) | — | 18 | bouton | 14 |
| `EcranCarte.kt:312` | attribution de l'ortho | — | 11 | `labelSmall` | 11 (inchangé) |
| `EcranCarte.kt:328` | libellé d'une case de couche (ligne h 48 dp) | — | 16 | `bodyLarge` (défaut) | 16 (inchangé) |
| `EcranCarte.kt:336` | bouton « fermer la liste » (h 56 dp) | — | 18 | bouton | 14 |
| `EcranCarte.kt:344` | code de la placette (ligne de liste h 64 dp) | Bold | 22 | `titleLarge` | 22 (inchangé) |
| `EcranCarte.kt:345` | distance (ligne de liste) | — | 22 | `titleLarge` | 22 (inchangé) |
| `carte/Commun.kt:72` | `BadgeFix` : qualité du fix (sur fond coloré) | Bold | 18 | `labelMedium` | 12 |

### B. Les textes sans équivalent dans Marculus (Pascal tranche)

| Surculus (fichier:ligne) | Texte | Graisse | Avant | Pourquoi pas d'équivalent | Options (taille M3) |
|---|---|---|---|---|---|
| `EcranGuidage.kt:156` | **distance à la cible** (grand chiffre du guidage) | Bold | 72 | Marculus n'a aucun chiffre de cette taille | `displayLarge` 57 ; `displayMedium` 45 ; `headlineMedium` 28 ; ou garder 72 en exception |
| `EcranGuidage.kt:164` | **cap** (« cap : NE 45° ») | Bold | 28 | 2e information du guidage, sans équivalent | `headlineMedium` 28 (inchangé) ; `titleLarge` 22 |
| `EcranEssaiGnss.kt:271` | **qualité du fix** (grand, fond coloré) | Bold | 44 | pas de grands chiffres dans Marculus | `displayMedium` 45 ; `headlineMedium` 28 ; ou exception 44 |
| `EcranEssaiGnss.kt:272` | **précision** (grand, fond coloré) | Bold | 44 | idem | idem |
| `EcranTournees.kt:239` | glyphe **« ⋮ »** du menu de la barre (dans un `IconButton`) | Bold | 28 | Marculus utilise une `Icon`, pas un glyphe texte ; la 040 a pris un glyphe pour n'ajouter aucune dépendance (`material-icons-core`) | `headlineMedium` 28 (inchangé) ; ou icône (dépendance, exclue par la fiche) |
| `EcranTournees.kt:169` | glyphe **« ✓ »** de la pastille « rendues » | Bold | 16 | Marculus met une `Icon` Check de 18 dp (`ListeContextesScreen.kt:261`) | le style de la pastille (`labelLarge` 14) |

**Bilan des écarts :** A = 77 textes, B = 6 textes (83 au total). Dans A, la plupart reculent (boutons 16-20 à 14, messages 18-20 à 14-16, titres 22-28 à 16-22) ; seuls `EcranCarte.kt:196` et `:198` grossissent (14 à 16, de 2 sp) ; une vingtaine restent à la même taille.

### C. Les boutons dont la hauteur ou la cible tactile dépend de la taille du texte

Tous les boutons de Surculus ont une **hauteur fixe** (`Modifier.height(…)`), indépendante du texte : réduire le texte ne les rétrécit pas.

| Élément | Hauteur | Risque sous 48 dp ? |
|---|---|---|
| Boutons plein écran : `EcranRestauration.kt:69` (72), `EcranTournees.kt:135,138` (64), `EcranEssaiGnss.kt:169,174` (64) et `:195` (56), `EcranGuidage.kt:71,143` (56), `:183` (64), `:191` (56), `EcranCarte.kt:87,192,336` (56), `:293,299,307` (64) | 56 à 72 dp | non |
| `EcranTournees.kt:285` (« relancer l'ortho ») ; `EcranEssaiGnss.kt:228` (« charger les points ») | 48 dp | non, pile au minimum ; ne doit pas descendre |
| Ligne de case `EcranCarte.kt:326` ; ligne de liste `EcranCarte.kt:340` | 48 et 64 dp | non |
| Barre d'accueil `EcranTournees.kt:226` | 56 dp | non |
| **Composants Material sans hauteur fixe** : `FilterChip` (`EcranTournees.kt:165-169`), `DropdownMenuItem` (`EcranTournees.kt:243,247` ; `EcranEssaiGnss.kt:204,234`), champ de recherche (`EcranTournees.kt:157`), `IconButton` « ⋮ » (`:238`), `Switch` (`:144`) | hauteur de Material 3 (chip visuel 32 dp avec zone tactile 48 dp ; menu 48 dp ; champ 56 dp ; IconButton 48 dp) | non : Material 3 garantit 48 dp de zone tactile (`minimumInteractiveComponentSize`), quel que soit le texte. Non mesuré ici (rien n'a été lancé). |

### Questions à trancher

1. **Les grands chiffres sans équivalent** (distance du guidage 72 sp ; cap 28 sp ; qualité et précision de l'essai 44 sp) : style de Material 3 (`displayLarge` 57, `displayMedium` 45, `headlineMedium` 28) ou exception écrite, gardée à la taille actuelle ? Ce sont les chiffres lus au soleil, à bout de bras, en marchant : le prix d'une réduction est payé par le forestier. **Proposition de l'agent : exceptions conservées** (72, 44, 28), listées dans le test qui interdit `fontSize =`.
2. **La graisse.** Les styles de Marculus portent leur graisse (`titleMedium`, `titleSmall`, `labelLarge` en Medium ; `body*` en Normal). Surculus écrit `Bold` à 25 endroits du tableau, surtout sur des états colorés (rouge, orange, vert). Marculus n'ajoute `Bold` qu'à de rares endroits (`ListeContextesScreen.kt:336,378,647`). Suivre Marculus (retirer les `Bold`) ou garder `Bold` sur les alertes ? **Proposition : garder `Bold` sur les alertes colorées** (drapeaux, hors de France, verdicts, légende de la carte), le retirer ailleurs.
3. **« ⋮ » et « ✓ »** : glyphes texte. Garder `headlineMedium` (28) pour « ⋮ » et le style de la pastille pour « ✓ » ?
4. **Boutons à 14 sp** (de 16-20 sp) dans des boutons de 56 à 72 dp : le texte devient petit dans un grand bouton. C'est la conséquence directe de « les tailles de Marculus » (ses boutons n'ont pas de hauteur fixe). Garder les hauteurs (cible tactile) avec le texte à 14 sp ? **Proposition : oui, hauteurs inchangées.**
5. **Deux textes grossissent** (`EcranCarte.kt:196,198`, 14 à 16 sp, via `bodyLarge`) : acceptable, ou `bodyMedium` (14, inchangé) ? **Proposition : `bodyMedium`**, que Marculus emploie pour un message d'état.
6. **Marculus écrit lui-même 2 `fontSize`** (`gnss/Skyplot.kt:105` en 7 sp, `gnss/BadgeFix.kt:117` en 12 sp). Je ne les copie pas ; le badge de Surculus (`Commun.kt:72`) suit `labelMedium` (12), la taille de son badge dans Marculus.

_Rien n'est codé tant que Pascal n'a pas validé ce tableau._

## Tests

- Un test JVM vérifie que `SurculusTheme` n'a pas de `Typography` propre (celle de `MaterialTheme` vaut `Typography()` de Material 3), comme le `ThemeTest` de la 038 pour les couleurs. **Vu rouge** une fois.
- Un test (ou un `grep` dans la CI) montre qu'il ne reste aucun `fontSize =` dans `app/src/main/kotlin`, sauf les exceptions du tableau validé.
- Commande du projet : `.claude/test-command`, au code 0.
- **Bilan des écarts** : chaque texte dont la taille change, de combien ; les cibles tactiles qui passeraient sous 48 dp (un bouton ne doit pas rapetisser avec son texte).

## Terrain

- **Échantillon que Pascal vérifiera :** la variante fumée, au soleil et avec des gants, à côté de Marculus : même taille de texte, et chaque bouton encore facile à toucher.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours ; au-delà, la décision remonte à Pascal.
- **Point de suivi :** à la remise du tableau, puis à l'ouverture de la PR.

## Comment saura-t-on que ça marche ?

Posés côte à côte, Surculus et Marculus ont le même texte, il ne reste aucune taille écrite à la main, et Pascal lit encore chaque écran au soleil.
