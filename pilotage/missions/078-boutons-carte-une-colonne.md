# Mission 078 — Les six boutons de la carte sur une seule colonne

> Fiche écrite par l'orchestrateur le 2026-10-30, à partir du gabarit
> `imperatrice/skills/mission/fiche-mission.md`, sur la décision de Pascal du 2026-10-30 : « Dans la carte
> mettre les 6 boutons sur une seule colonne comme dans groma quitte à réduire la taille des boutons », puis,
> à la question de la taille : « Colonne pleine hauteur, 40 dp ». Revient sur « Oui, deux colonnes »
> (2026-10-28, PR #110, mission 064). Depuis `main` (d8263ec). Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Ranger toujours les six boutons de la carte en une seule colonne à
droite, comme dans Groma, sur toute la hauteur de l'écran sous la barre de titre, à 48 dp quand elle tient
et jamais sous 40 dp, pour que le forestier trouve chaque bouton à la même place debout comme couché.

**Redite par l'agent :** Je range les six boutons de la carte en une seule colonne pleine hauteur à droite (48 dp si elle tient, jamais moins de 40 dp, textes du haut et du bas raccourcis avant elle), pour que le forestier retrouve chaque bouton au même endroit debout comme couché.

## Ce qui est constaté

- Aujourd'hui (mission 064, `DispositionBoutons`, `EcranCarte.kt` l. 491) : boutons de 48 dp minimum, écart
  8 dp, marge 8 dp, placés dans la seule zone de la carte ; couché, la carte n'a qu'environ 243 dp de haut
  sur le S23 Ultra de Pascal (densité 450, 2316 × 1080 px ; capture v0.7.90), d'où 3 + 3.
- Groma (`~/dev/groma/android/app/src/main/kotlin/groma/app/ui/EcranCarte.kt` l. 357-391) : une seule
  `Column` de `SmallFloatingActionButton` (40 dp), écart 8 dp, marge 12 dp, en bas à droite.
- Six boutons à 40 dp, écart 8 dp, marges 12 dp : 304 dp. L'écran couché, sous la barre de titre (~48 dp),
  offre environ 336 dp : la colonne tient si elle descend sur la ligne de légende et la ligne de la placette,
  pas si elle reste dans la carte.

## Ce qui est à faire

1. **Une seule colonne, toujours** (debout, couché, avec ou sans bandeau d'alerte). Fin de la disposition en
   deux colonnes de la 064.
2. **Pleine hauteur** : la colonne occupe le bord droit, de sous la barre de titre jusqu'en bas de l'écran.
   Les lignes du bas (légende « jamais / à prévoir / à jour », « Placette … · distance », attribution) et
   les bandeaux d'alerte en haut de la carte s'arrêtent avant la colonne (ils raccourcissent, ils ne passent
   pas dessous) ; aucun texte n'est caché par un bouton.
3. **Taille** : 48 dp quand les six tiennent ; sinon la taille baisse juste ce qu'il faut, **jamais sous
   40 dp** (décision de Pascal). Lecture de l'orchestrateur, à confirmer par Pascal à la relecture :
   « quitte à réduire » veut dire réduire seulement quand il le faut. Si même à 40 dp la colonne ne tient pas
   (petit téléphone couché, grand texte), le dire dans la PR avec le calcul ; ne pas inventer de repli.
4. Le calcul reste en Kotlin pur et testé en JVM (`DispositionBoutons` ou son successeur) : tests d'abord,
   vus rouges, pour au moins 243 dp de carte / 336 dp d'écran couché, l'écran debout, et un écran trop court.
5. Ordre des boutons inchangé ; « + » et « − » l'un sous l'autre.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui (Compose, mise en page de `EcranCarte`) | |
| A-t-il les moyens ? | oui | capture sur le téléphone par l'orchestrateur après fusion |

**Temps de la main :** **appuyer**.

## L'Homme

- **Agent :** `afficheur`.
- **Ce qui ne lui est pas demandé :** changer les boutons eux-mêmes (icônes, actions, couleurs), la barre de
  titre, les textes, les pastilles de la carte ; toucher `fixtures/`.

## Harnais

- **Peut :** modifier `EcranCarte.kt`, `DispositionBoutons.kt` et leurs tests ; pousser sur
  `mission/078-boutons-carte-une-colonne` ; ouvrir la PR.
- **Ne doit pas :** modifier `fixtures/`, `docs/`, `pilotage/` (sauf la redite de cette fiche) ; toucher à
  `main` ; trancher une question de sens.

## Tests

- `.claude/test-command` au code 0 ; les nouveaux tests vus **rouges** avant la correction.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. Critère d'arrêt : 3 allers-retours.
- **Qui tranche :** Pascal. **Fusion : autorisation à demander à Pascal.**

## Comment saura-t-on que ça marche ?

Sur la release suivante, carte de `couchey-essai-plants`, debout puis couché, avec le bandeau « Récepteur
GNSS non démarré » affiché : six boutons en une colonne, aucun coupé, aucun texte caché ; captures de
l'orchestrateur. Puis l'essai ganté de Pascal.

## Reprise, aller-retour 1/3 (2026-10-30)

Relecture de la #137 (6953d02) : code conforme, « à reprendre » pour le corps de la PR. Décisions de Pascal du
2026-10-30, posées une à une après la relecture :

1. **Repli** : quand les six boutons ne tiennent pas à 40 dp, « Descendre sous 40 dp » : la taille continue de
   baisser jusqu'à ce que les six tiennent.
2. **Plancher absolu** : « 32 dp ». En dessous seulement, le dernier bouton peut être écrasé (le dire dans la PR).
3. **Taille quand ça tient** : « La plus grande qui tient » (48 dp au plus), lecture de la fiche confirmée.

À faire :

- `DispositionBoutons` : taille de 48 à **32 dp** ; écart (8 dp) et marge (12 dp) inchangés. Tests d'abord, vus
  rouges par une assertion (pas seulement à la compilation) : 336 dp → 45 dp ; ≈ 292 dp (couché sans plein écran)
  → la plus grande taille qui tient, sous 40 dp ; 260 dp → 32 dp ; plus court → 32 dp. Le test couché fixe la
  valeur attendue (mineur du relecteur).
- Corps de la PR à corriger : le cas trop court **écrase les derniers boutons** (Maison, Recentrer), il ne déborde
  pas en haut ; le calcul couché donné **avec et sans plein écran** (barre d'état, barre de gestes) ; le cas
  « 243 dp de carte » de la fiche devenu sans objet avec la colonne pleine hauteur, le dire ; `tient()` : l'utiliser
  ou le retirer, le dire.
- Capture de l'orchestrateur après fusion : couché, avec et sans plein écran.
