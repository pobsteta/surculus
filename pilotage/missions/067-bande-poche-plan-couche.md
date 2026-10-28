# Mission 067 — La bande du mode poche toujours visible, le plan couché sous les boutons

> Fiche écrite par l'orchestrateur le 2026-10-28 (cycle 13), à partir du gabarit
> `imperatrice/skills/mission/fiche-mission.md`, sur décision de Pascal : « Oui, l'afficheur ».
> Depuis `main` (fee5b57, release v0.7.74). Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Garder la bande du mode poche entière et visible en haut de
l'écran du contrôle quel que soit le défilement, et empêcher le cercle du plan de chevaucher les
boutons d'état téléphone couché, pour que le forestier voie toujours comment sortir du mode poche et
touche le bon bouton.

**Redite par l'agent :** _à écrire par l'agent avant d'agir._

## Ce qui a été vu (captures de l'orchestrateur, v0.7.73 et v0.7.74, 2026-10-28)

1. **Bande coupée.** L'écran du contrôle était défilé vers le bas quand « Mode poche » a été touché
   (le bouton est en bas). La bande (`controle_poche_aide`, « 3 s sur volume haut pour sortir ») est
   alors à moitié sous la barre verte ; le toucher étant ignoré en poche, on ne peut plus remonter.
   Vu debout et couché.
2. **Plan couché.** En paysage, le cercle du plan de la placette est dessiné par-dessus les boutons
   Sain, Atteint, Mort, Absent.

## Décisions de Pascal à respecter

- Mode poche : toucher ignoré, sortie par 3 s sur volume haut, à « Terminer », après 15 min (012-C,
  décisions du 2026-10-27 et 2026-10-28) ; ne rien changer à ces règles.
- Couleurs en sombre : règles de la 063 (texte `onSurface`, erreurs en `colorScheme.error`, aucune
  plaque blanche, contraste ≥ 4,5:1).
- Le plan est « nord en haut » (012-B) : ne pas le tourner.

## Ce qui est attendu

- La bande du mode poche est **hors de la zone qui défile** (fixée sous la barre, ou l'écran remonte
  en haut à l'entrée en poche) : entière et lisible dès l'entrée, debout et couché, quelle que soit la
  position du défilement. L'agent choisit la solution la plus simple et la dit dans la PR.
- Couché, le plan ne recouvre aucun bouton : il prend la place qui reste (taille réduite, ou mis à
  côté des boutons) ; debout, rien ne change.
- Aucun changement de comportement du mode poche, des gestes, de la voix ni de la session.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui | |
| A-t-il les moyens ? | oui ; la vérification finale se fait sur le téléphone de Pascal | |

**Temps de la main :** **appuyer**.

## L'Homme

- **Agent :** `afficheur`.
- **Ce qui ne lui est pas demandé :** la dictée (D2) ; les autres écrans ; la carte.

## Harnais

- **Peut :** modifier l'écran du contrôle (`app/.../controle/`) et ses tests ; pousser sur
  `mission/067-bande-poche-plan-couche` ; ouvrir la PR.
- **Ne doit pas :** modifier `fixtures/`, `docs/`, `pilotage/journal.md` ; toucher à `main` ; changer
  la logique du mode poche ou de la session ; trancher une question de sens.

## Tests

- Une fonction pure ou un test qui fixe la règle de disposition (paysage : le plan ne chevauche pas
  les boutons ; la bande est hors de la zone défilante), **vu rouge** une fois.
- `.claude/test-command` au code 0.

## Terrain

- **Échantillon :** release qui suit la fusion, capture du téléphone par l'orchestrateur : entrée en
  poche après défilement (debout et couché), plan couché.
- **Test des dix secondes** (Pascal) : en poche, sans chercher, lire comment sortir.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. Critère d'arrêt : 3 allers-retours.
- **Qui tranche :** Pascal. **Fusion : autorisation à demander à Pascal.**

## Comment saura-t-on que ça marche ?

Sur la release, l'écran défilé jusqu'à « Mode poche », un appui : la bande « 3 s sur volume haut pour
sortir » se lit en entier ; téléphone couché, le cercle du plan ne touche aucun bouton.
