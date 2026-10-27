# Mission 048 — Charger la voix Vosk depuis Paramètres : téléchargement et fichier

> Fiche écrite par l'orchestrateur le 2026-10-27, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Demandée par Pascal le 2026-10-24 :
> « Ajoute dans paramètres la possibilité de charger une voix pour vosk comme dans Marculus »
> (journal de la branche non fusionnée `mission/042-typographie`), puis « 4. les deux » le
> 2026-10-27 (téléchargement et import par fichier).
> **Lancement :** après la 046 (écran Paramètres), sous le plafond humain.

## Mission

**En une phrase (quoi, et pourquoi) :** Ajouter dans Paramètres une section « Dictée vocale » qui installe le modèle Vosk de deux façons, par téléchargement comme Marculus ou par un fichier choisi sur le téléphone comme aujourd'hui, avec son état (installé ou non), sa taille et sa suppression, pour que le forestier prépare la dictée au bureau avec du réseau, ou sans réseau à partir d'un fichier.

**Redite par l'agent :** _à remplir par l'agent avant d'agir._

## Vérifié à la source

Constats vérifiés par l'orchestrateur le 2026-10-27.

| Constat | Lu à | Source du constat |
|---|---|---|
| Marculus télécharge `vosk-model-small-fr-0.22` (Apache 2.0, ~42 Mo) et gère son cycle de vie ; bouton « Télécharger (%1$d Mo) » | `marculus` `app/src/main/kotlin/io/github/pobsteta/marculus/voice/ModeleVosk.kt` l. 11 ; `ui/parametres/ParametresScreen.kt` ; `res/values/strings.xml` l. 395 | orchestrateur |
| Surculus installe aujourd'hui le modèle par un fichier, via `terrain-voix` (`InstallationModele.installer(archive, source)`) | `app/src/main/kotlin/io/github/pobsteta/surculus/dictee/ModeleVoskImport.kt` l. 47 | orchestrateur |
| `terrain-voix` efface le modèle déjà installé sur tout échec d'`installer` (écart signalé à `terrain`, pobsteta/terrain#23) | `pilotage/journal.md` (2026-10-24, mission 013) | orchestrateur |
| Le branchement de la dictée sur l'écran du contrôle reste à la 012 | `pilotage/journal.md` (2026-10-24, « 1. oui, 2. oui ») | orchestrateur |

Règles, avec leur auteur :
- **Les deux : téléchargement et fichier** : Pascal, 2026-10-27.
- **Même modèle que Marculus et que la 013** (`vosk-model-small-fr-0.22`), vérifié par l'empreinte que `terrain-voix` contrôle déjà ; l'URL de téléchargement est celle de Marculus, citée dans le code.
- **Hors réseau d'abord** : un échec de téléchargement ne doit jamais effacer un modèle déjà installé ; si `terrain-voix` l'efface, l'agent le dit et le contourne (téléchargement dans un fichier temporaire, installation seulement si l'archive est complète).
- **L'écran sert le forestier en forêt** : boutons d'au moins 48 dp, progression lisible, styles de la 042 (`CLAUDE.md`).
- **Garde-fou = test vu rouge** et **bilan des écarts avant rendu** : Pascal, bilan du 2026-10-21.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : téléchargement repris de Marculus, installation par `terrain-voix` (013) | |
| A-t-il les moyens ? | à vérifier : permission `INTERNET` (déjà là pour l'ortho ?) ; aucune dépendance nouvelle | il le dit avant d'ajouter quoi que ce soit |

**Temps de la main :** **appuyer**.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent. L'orchestrateur ne le remplit pas._

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** brancher la dictée sur l'écran du contrôle (012) ; la synthèse vocale ; d'autres langues ; modifier `terrain-voix`.

## Harnais

- **Peut :** ajouter la section à `parametres/EcranParametres.kt` (046) ; un petit module de téléchargement en Kotlin, testable sans réseau (source injectée) ; réutiliser `ModeleVoskImport` ; les chaînes ; les tests.
- **Ne doit pas :** toucher à `main` ; modifier `fixtures/` ou `pilotage/journal.md` ; ajouter une dépendance sans le dire.
- **Sources autorisées :** cette fiche ; `CLAUDE.md` ; `marculus` (`~/dev/`, en lecture) ; le code de la 013.

## Tests

- Tests JVM, sans réseau : téléchargement complet → installé ; téléchargement interrompu → modèle précédent intact ; archive à mauvaise empreinte → refusée, modèle précédent intact ; suppression → non installé. **Vu rouge** une fois.
- Commande du projet : `.claude/test-command`, au code 0.
- **Bilan des écarts** : ce qui change à l'écran, écarts avec Marculus.

## Terrain

- **Échantillon que Pascal vérifiera :** sur la release qui suit la fusion : Paramètres → Dictée vocale → Télécharger, la progression avance, l'état passe à « installé » ; couper le réseau en cours de téléchargement : l'état d'avant reste ; installer depuis un fichier fonctionne aussi.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours ; au-delà, la décision remonte à Pascal.
- **Point de suivi :** à l'ouverture de la PR.

## Comment saura-t-on que ça marche ?

Au bureau, Pascal télécharge la voix en un appui ; en forêt sans réseau, un échec ne lui a jamais fait perdre la voix déjà installée.
