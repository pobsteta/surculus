# Mission 058 — Effacer les tuiles d'ortho préchargées avant la 055

> Fiche écrite par l'orchestrateur le 2026-10-27, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Décision de Pascal du 2026-10-27
> (« 4 oui »), sur la question de la relecture de la PR #97 (055).
> **Lancement :** le 2026-10-27, depuis `main` (52b457e, 055 fusionnée), en parallèle de la 057.

## Mission

**En une phrase (quoi, et pourquoi) :** Effacer, dans le dossier de chaque tournée, les tuiles d'ortho IGN préchargées avant la mission 055 (`tuiles/` et `ortho-terminee.txt`), qui ne sont plus lues, pour qu'elles n'occupent plus le téléphone et ne partent pas vers sommieR avec l'export de la tournée.

**Redite par l'agent :** Supprimer une seule fois, en tâche de fond, `tuiles/` et `ortho-terminee.txt` du dossier de chaque tournée (plus lus depuis la 055), et les exclure de l'export zip, pour libérer le téléphone et ne rien envoyer de superflu à sommieR.

## Vérifié à la source

| Constat | Lu à | Source du constat |
|---|---|---|
| Depuis la 055, les tuiles préchargées (`<dossier de tournée>/tuiles/`, `ortho-terminee.txt`) sont laissées sur le disque et ne sont plus lues | compte rendu du `dev` de la 055 ; `app/.../carte/Ortho.kt` l. 11 (`main` 52b457e) | dev, PR #97 |
| `ExportZip.ecrire` zippe tout le dossier de la tournée (`walkTopDown`), donc ces tuiles partiraient vers sommieR à l'export (jusqu'à ~30 000 tuiles par tournée) | `gpkg/src/main/kotlin/io/github/pobsteta/surculus/gpkg/ExportZip.kt` l. 27-35 | relecteur, PR #97 |

Règles, avec leur auteur :
- **Effacer les anciennes tuiles** : Pascal, 2026-10-27.
- **N'effacer que ces deux chemins**, nommés exactement, jamais un autre fichier de la tournée (`travaux.gpkg`, `DCIM/`, `ortho.mbtiles`, `ortho.gpkg`, journal, photos). Un test le prouve sur un dossier de tournée complet.
- **Quand** : une seule fois, au démarrage ou à l'ouverture de la liste des tournées, en tâche de fond, sans bloquer l'écran ; idempotent (rien à faire la deuxième fois).
- **Garde-fou à l'export** : `ExportZip` exclut aussi `tuiles/` et `ortho-terminee.txt` s'ils existent encore, pour qu'un dossier non nettoyé ne parte jamais avec eux. Test : un dossier avec `tuiles/` donne un zip sans `tuiles/` ; la CI d'import (F1 à F5) reste verte.
- **Garde-fou = test vu rouge** et **bilan des écarts avant rendu** : Pascal, bilan du 2026-10-21.

**Temps de la main :** **appuyer** (effacement irréversible). Fusion autorisée par Pascal si relecture prête et CI verte.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent. L'orchestrateur ne le remplit pas._

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** la carte (057) ; vider le cache d'osmdroid ; toucher aux fichiers de sommieR.

## Harnais

- **Peut :** ajouter une petite fonction de nettoyage (Kotlin pur, testable sur un dossier temporaire), l'appeler au démarrage ; modifier `gpkg/.../ExportZip.kt` pour l'exclusion ; les tests.
- **Ne doit pas :** toucher à `main` ; modifier `fixtures/`, `docs/` ou `pilotage/journal.md` ; ajouter une dépendance.

## Tests

- JVM : nettoyage d'un dossier de tournée complet → seuls `tuiles/` et `ortho-terminee.txt` disparaissent ; deuxième passage sans effet ; dossier sans tuiles inchangé ; `ExportZip` exclut `tuiles/`. **Vu rouge** une fois.
- Commande du projet : `.claude/test-command`, au code 0 (la CI d'import sommieR rejoue l'export de Couchey).

## Terrain

- **Échantillon que Pascal vérifiera :** la release qui suit la fusion : la tournée Couchey s'ouvre et sa carte marche ; le stockage occupé par Surculus baisse si des tuiles avaient été préchargées.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal. Critère d'arrêt : 3 allers-retours.

## Comment saura-t-on que ça marche ?

Après la mise à jour, plus aucun dossier de tournée ne contient `tuiles/`, et un export ne les emporte jamais.
