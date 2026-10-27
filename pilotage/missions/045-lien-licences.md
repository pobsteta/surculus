# Mission 045 — Un lien vers toutes les licences dans « À propos »

> Fiche écrite par l'orchestrateur le 2026-10-27, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Décision de Pascal du 2026-10-27
> (« c. (b) ») sur la question Q3 de la relecture de la PR #86 (mission 044).
> **Lancement :** le 2026-10-27, depuis `main` (a0186c7, 044 fusionnée).

## Mission

**En une phrase (quoi, et pourquoi) :** Ajouter à la boîte « À propos » un bouton qui ouvre la liste complète des dépendances et de leurs licences (`docs/dependances.md` sur GitHub), en gardant le texte actuel des licences principales, pour que toutes les licences soient accessibles sans allonger la boîte.

**Redite par l'agent :** Ajouter à la boîte « À propos » un bouton qui ouvre `docs/dependances.md` sur GitHub (branche main), en gardant le texte des licences principales, pour que toutes les licences soient accessibles sans allonger la boîte. _(Recopiée par l'orchestrateur depuis le compte rendu du `dev`.)_

## Vérifié à la source

Constats vérifiés par l'orchestrateur le 2026-10-27.

| Constat | Lu à | Source du constat |
|---|---|---|
| La boîte « À propos » affiche les licences principales (`apropos_licences`) et un bouton « Ouvrir sur GitHub » (`apropos_github`, 48 dp) | `app/src/main/res/values/strings_tournees.xml` l. 10-11 ; `tournee/EcranTournees.kt` (boîte « À propos », mission 044) | orchestrateur |
| La liste est juste mais partielle : n'y figurent pas desugar_jdk_libs, JNA, sqlite-jdbc, AndroidX, terrain-voix, le WMM | relecture de la PR #86, `pilotage/journal.md` (2026-10-27) | `relecteur` |
| La liste complète est dans `docs/dependances.md` | `docs/dependances.md` | orchestrateur |

Règles, avec leur auteur :
- **Licences principales gardées, plus un lien vers `docs/dependances.md` sur GitHub** : Pascal, 2026-10-27.
- **Le lien pointe sur la branche `main`** du dépôt (`https://github.com/pobsteta/surculus/blob/main/docs/dependances.md`), comme le bouton GitHub actuel pointe sur le dépôt.
- **L'écran sert le forestier en forêt** : bouton d'au moins 48 dp, texte en 18 sp comme les boutons voisins (`CLAUDE.md`).
- **Garde-fou = test vu rouge** et **bilan des écarts avant rendu** : Pascal, bilan du 2026-10-21.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : même geste que le bouton « Ouvrir sur GitHub » | |
| A-t-il les moyens ? | oui | |

**Temps de la main :** **appuyer**.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent. L'orchestrateur ne le remplit pas._

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** réécrire le texte des licences principales, la phrase de description (gardée par Pascal) ou le plein écran ; compléter `docs/dependances.md`.

## Harnais

- **Peut :** modifier la boîte « À propos » dans `tournee/EcranTournees.kt`, `strings_tournees.xml`, et le petit fichier Kotlin pur de la 044 (`ReglagesAffichage.kt`) si l'URL y va.
- **Ne doit pas :** toucher à `main` ; modifier `fixtures/` ou `pilotage/journal.md` ; ajouter une dépendance.
- **Sources autorisées :** cette fiche ; `CLAUDE.md` ; `docs/dependances.md`.

## Tests

- Test JVM : l'URL des licences est exactement celle de la règle ci-dessus. **Vu rouge** une fois.
- Commande du projet : `.claude/test-command`, au code 0.
- **Bilan des écarts** : ce qui change à l'écran.

## Terrain

- **Échantillon que Pascal vérifiera :** sur la release qui suit la fusion (plus de variante fumée : décision de Pascal du 2026-10-27), « À propos » → le nouveau bouton ouvre `docs/dependances.md` dans le navigateur.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours ; au-delà, la décision remonte à Pascal.
- **Point de suivi :** à l'ouverture de la PR.

## Comment saura-t-on que ça marche ?

Depuis « À propos », Pascal ouvre la liste complète des licences en un appui, et la boîte ne s'est pas allongée.
