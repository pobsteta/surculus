# Mission 018 — Écran d'essai GNSS

> Fiche écrite par l'orchestrateur le 2026-10-16, à la demande de Pascal (arbitrage de la
> mission 003), à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`.

## Mission

**En une phrase (quoi, et pourquoi) :** Ajouter à Surculus un écran d'essai GNSS (choix du récepteur Bluetooth apparié, point de montage et compte NTRIP, demande des permissions, démarrage et arrêt du service RTK, affichage de la qualité du fix, de la précision, de l'âge du fix et des corrections, des drapeaux « rétrogradé », « refusé » et « position simulée », et de la position en Lambert-93), pour que Pascal essaie le ZED-F9P avec Centipède dès le cycle 2, sans attendre les écrans de la mission 011.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : service `gnss` de la mission 003 ; écrans de configuration RTK de Groma (lecture seule) ; Compose | |
| A-t-il les moyens ? | oui après la fusion de la 003 ; essai par Pascal (ZED-F9P, compte Centipède) | |

**Temps de la main :** **appuyer**.

## L'Homme

- **Agent :** `afficheur` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** la carte, le guidage, les tournées (mission 011) ; aucune règle métier.

## Harnais

- **Peut :** un écran Compose « Essai GNSS » ouvert depuis l'activité principale ; la configuration RTK de la 003 (`ConfigRtk`) ; les permissions Bluetooth, localisation et notifications demandées à l'usage ; la lisibilité au soleil (contraste, gros caractères), parce que l'essai se fait dehors.
- **Ne doit pas :** coder en dur un compte ou un mot de passe, le compte public de Centipède compris (saisi par l'agent ou prérempli comme valeur d'exemple affichée, jamais enregistré sans action) ; modifier `core.gnss` sauf pour corriger un défaut, alors signalé ; toucher à `main`.

## Tests

- `.claude/test-command` vert ; un test Compose ou un test unitaire du modèle d'état affiché (fix refusé, rétrogradé, simulé) ; capture d'écran rendue à l'orchestrateur.

## Terrain

- Pascal, sur son téléphone avec le ZED-F9P et Centipède :
  - il voit « RTK fixe » et une précision centimétrique ;
  - il coupe la 4G : l'écran passe à « autonome » après 30 s ;
  - il éteint le récepteur : le fix disparaît après 5 s ;
  - il active une position simulée : l'écran l'affiche ;
  - Couchey tombe en France.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.
