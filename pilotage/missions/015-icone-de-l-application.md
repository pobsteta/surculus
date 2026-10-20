# Mission 015 — Icône de l'application

> Fiche écrite par l'orchestrateur le 2026-10-16, à la demande de Pascal, à partir du
> gabarit `imperatrice/skills/mission/fiche-mission.md`. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Donner à Surculus une icône adaptative du même style que Marculus et Groma (hexagone bleu nuit, lettre gothique UnifrakturCook Bold), avec un « S » majuscule, générée par un script versionné, pour que les trois applications se reconnaissent et se distinguent sur le téléphone.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : script de Groma `android/app/icone/generer_icone.py`, ressources `mipmap-*` et `mipmap-anydpi-v26` de Groma | |
| A-t-il les moyens ? | oui : Pillow, police OFL téléchargée par le script, rendu comparable aux PNG de Groma et Marculus | |

**Temps de la main :** **appuyer**.

## L'Homme

- **Agent :** `dev`
- **Modèle :** sonnet
- **Ce qui ne lui est pas demandé :** aucun écran ; pas de changement de couleur ni de forme par rapport à Groma et Marculus.

## Harnais

- **Peut :** `app/icone/generer_icone.py` (repris de Groma, lettre « S », mêmes proportions : hauteur de capitale de Marculus), PNG `mipmap-{mdpi…xxxhdpi}/ic_launcher_foreground.png`, `mipmap-anydpi-v26/ic_launcher.xml` et `ic_launcher_round.xml` (avec `monochrome`, comme Groma), `values/colors.xml` (`ic_launcher_background` #FFFFFF), `android:icon` et `android:roundIcon` dans le manifeste ; ligne dans `docs/dependances.md` (police UnifrakturCook Bold, OFL-1.1, téléchargée par le script, non versionnée, comme Groma) ; `.gitignore` pour le `.ttf`.
- **Ne doit pas :** versionner la police ; modifier Groma ou Marculus ; toucher à `main`.
- **Sources autorisées :** Groma et Marculus en lecture seule ; Google Fonts (dépôt `google/fonts`, `ofl/unifrakturcook`).

## Tests

- **Comment saura-t-on que c'est juste :** `.claude/test-command` vert ; l'APK de debug contient les icônes (`aapt2 dump badging` ou liste du zip) ; le PNG xxxhdpi est montré à l'orchestrateur à côté de ceux de Groma et Marculus.

## Terrain

- **Échantillon que Pascal vérifiera :** sur son téléphone, l'icône « S » de Surculus voisine avec le « M » de Marculus et le « G » de Groma.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Pascal reconnaît Surculus sur l'écran d'accueil de son téléphone.
