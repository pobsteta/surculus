# Mission 017 — Icône : « S » en New Rocker

> Fiche écrite par l'orchestrateur le 2026-10-16, à la demande de Pascal, à partir du
> gabarit `imperatrice/skills/mission/fiche-mission.md`. Reprend la mission 015 (PR #18).

## Mission

**En une phrase (quoi, et pourquoi) :** Redessiner le « S » de l'icône de Surculus avec la police gothique brisée New Rocker (OFL) au lieu d'UnifrakturCook, en gardant l'hexagone et l'encre de Marculus et Groma, parce que Pascal trouve le « S » actuel trop proche du « G » de Groma sur l'écran du téléphone.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : `app/icone/generer_icone.py` (mission 015) ; planche de Pascal (variante 4) | |
| A-t-il les moyens ? | oui : New Rocker, `google/fonts`, `ofl/newrocker/NewRocker-Regular.ttf` | |

**Temps de la main :** **appuyer**.

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.

## Harnais

- **Peut :** changer la police et son URL dans `app/icone/generer_icone.py`, régénérer les cinq PNG, mettre à jour la ligne de police dans `docs/dependances.md` (New Rocker, OFL-1.1, téléchargée par le script, non versionnée) et le `.gitignore` si le nom du fichier change. La hauteur de capitale reste celle du « M » de Marculus ; le « S » est centré dans l'hexagone par sa boîte englobante, et sa hauteur visible est comparable à celle du « G » de Groma.
- **Ne doit pas :** changer l'hexagone, l'encre ou le fond ; versionner la police ; toucher à `main`.

## Tests

- `.claude/test-command` vert ; le PNG xxxhdpi est montré à côté du « G » de Groma et du « M » de Marculus ; le rendu est reproductible (relancer le script ne change aucun PNG).

## Terrain

- Pascal distingue d'un coup d'œil Surculus et Groma sur l'écran d'accueil de son téléphone.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.
