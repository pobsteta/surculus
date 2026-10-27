# Mission 047 — Le récepteur GNSS dans les Paramètres, comme Marculus et Groma

> Fiche écrite par l'orchestrateur le 2026-10-24, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Demandée par Pascal le 2026-10-24 :
> « Utiliser un récepteur GNSS doit être dans paramètres comme dans marculus et groma ».
> Renumérotée le 2026-10-27 (044 sur la branche non fusionnée `mission/043-parametres`, f947899) : les numéros 043 à 045 sont pris par les missions fusionnées ce jour-là.
> **Lancement :** après la 046 (écran Paramètres), sous le plafond humain.

## Mission

**En une phrase (quoi, et pourquoi) :** Mettre dans l'écran Paramètres une section « Récepteur GNSS » reprise de Groma (« Utiliser un récepteur RTK », appareil Bluetooth ou TCP, corrections NTRIP), qui démarre le récepteur à l'ouverture de la carte d'une tournée et l'arrête à la sortie, et laisser à l'écran d'essai la seule mesure, pour que le forestier règle son récepteur une fois et l'ait sur la carte sans passer par l'essai.

**Redite par l'agent :** une section « Récepteur GNSS » dans Paramètres (appareil Bluetooth ou TCP, corrections NTRIP), le récepteur démarrant à l'ouverture de la carte et s'arrêtant à la sortie de la tournée, l'essai ne gardant que la mesure, pour régler le récepteur une fois et l'avoir sur la carte sans passer par l'essai.

## Vérifié à la source

| Constat | Lu à | Source du constat |
|---|---|---|
| Groma : `EcranParametres` appelle `SectionRtk()` ; « Utiliser un récepteur RTK », « Démarré à l'ouverture de la carte, arrêté à la sortie du projet. » | `groma`, `android/app/src/main/kotlin/groma/app/ui/EcranParametres.kt` l. 102 ; `ui/SectionRtk.kt` ; `res/values/strings.xml` l. 187-199 | orchestrateur |
| Marculus : `SectionRtk(reglages)` dans Paramètres ; « Récepteur externe RTK », « Utiliser un récepteur GNSS externe (mosaic-X5, UM980…) au lieu du GNSS interne. » | `marculus`, `ui/parametres/ParametresScreen.kt` l. 319 ; `res/values/strings.xml` l. 315-316 | orchestrateur |
| Surculus a déjà `ConfigRtk` (copie de Groma, `actif`, transport, appareil, caster, point de montage, compte) et `ServiceGnssRtk` ; ses réglages s'éditent aujourd'hui dans l'écran d'essai | `gnss/ConfigRtk.kt` ; `gnss/ServiceGnssRtk.kt` ; `essai/EcranEssaiGnss.kt` l. 185-255 | orchestrateur |
| La carte et le guidage lisent `SourcePosition.fix` : récepteur RTK s'il tourne, sinon GNSS du téléphone | `carte/Commun.kt` l. 43-62 | orchestrateur |

Règles, avec leur auteur :
- **Le récepteur se règle dans Paramètres, comme dans Marculus et Groma** : Pascal, 2026-10-24.
- **Démarré à l'ouverture de la carte, arrêté à la sortie de la tournée** : comportement de Groma, repris ; à confirmer par Pascal à l'essai.
- **Aucune règle GNSS ne change** : politique du fix, fraîcheur, refus (ADR 0003) restent ceux de la 003.
- **Garde-fou = test vu rouge** et **bilan des écarts avant rendu** : Pascal, bilan du 2026-10-21.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : `SectionRtk` de Groma, `ConfigRtk` et `ServiceGnssRtk` déjà là | |
| A-t-il les moyens ? | oui | |

**Temps de la main :** **appuyer** : avant de coder, l'agent rend la liste des réglages qui quittent l'écran d'essai et de ce qui y reste ; Pascal valide.

## Ma réponse d'abord

_Rempli par Pascal seul, avant de lire le travail de l'agent. L'orchestrateur ne le remplit pas._

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** changer la politique du fix ou le badge ; ajouter un récepteur ou un protocole ; toucher au thème, à la recherche ou au plein écran.

## Harnais

- **Peut :** ajouter la section à `parametres/EcranParametres.kt` (046) ; retirer les réglages de `essai/EcranEssaiGnss.kt` en gardant la mesure ; démarrer et arrêter `ServiceGnssRtk` à l'entrée et à la sortie de la carte d'une tournée ; les chaînes ; les tests.
- **Ne doit pas :** toucher à `main` ; modifier `fixtures/` ; ajouter une dépendance ; changer `core/gnss`.
- **Sources autorisées :** cette fiche ; ADR 0003 ; `groma` et `marculus` (`~/dev/`, en lecture).

## Tests

- Le démarrage et l'arrêt suivent la navigation (logique en Kotlin pur si possible : ouvrir la carte avec `actif` vrai démarre ; sortir de la tournée arrête ; `actif` faux ne démarre jamais). **Vu rouge** une fois.
- Commande du projet : `.claude/test-command`, au code 0.
- **Bilan des écarts.**

## Terrain

- **Échantillon que Pascal vérifiera :** la release qui suit la fusion (plus de variante fumée) et le récepteur de Pascal : régler dans Paramètres, ouvrir une tournée, voir le badge passer en RTK sur la carte sans passer par l'essai ; sortir, le récepteur s'arrête.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal. Critère d'arrêt : 3 allers-retours. Point de suivi : à la liste des réglages, puis à l'ouverture de la PR.

## Comment saura-t-on que ça marche ?

Pascal règle son récepteur une fois dans Paramètres, et chaque carte de tournée s'ouvre en RTK.
