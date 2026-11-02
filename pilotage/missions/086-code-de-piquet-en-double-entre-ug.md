# Mission 086 — Code de piquet en double entre UG

> Fiche écrite par l'orchestrateur le 2026-11-02, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> après la fusion de la 085 (PR #150, 66a18d6 : ADR 0003 amendée, qui fait foi) et le découpage décidé par Pascal le
> 2026-11-02 (« trois missions » : 086 homonymes, 087 ouvrir sans position, 088 `.sursync` v2). Depuis `main`.
> Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Quand le code lu sur le piquet existe dans plusieurs UG de la tournée, laisser la
position trancher si une seule de ces placettes est à 15 m au plus, et sinon faire choisir l'UG à l'agent, au lieu
d'ouvrir la première trouvée sans rien dire, pour qu'aucun contrôle ne soit rattaché à la mauvaise placette.

**Redite par l'agent :** _(testeur, 2026-11-02)_ Je dois écrire les tests qui diront si, quand un code de piquet
existe dans plusieurs UG, la position tranche (une seule placette à 15 m au plus, borne incluse), sinon si l'agent
doit choisir l'UG, jamais la « première trouvée », pour qu'aucun contrôle ne soit rattaché en silence à la mauvaise
placette. _(dev, 2026-11-02)_ Je dois faire que, quand le code lu existe dans plusieurs UG, Surculus rattache la seule placette à 15 m au plus si elle est unique, et sinon fasse choisir l'UG à l'agent (le choix rattache la placette choisie, par identité au-delà de 15 m), sans jamais prendre la première trouvée, pour qu'aucun contrôle ne soit rattaché en silence à la mauvaise placette.

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui (ADR 0003, amendement du 2026-11-02, point 3 ; spec § 4.3 point 3 et § 6) | |
| A-t-il les moyens ? | oui | le `testeur` écrit d'abord les tests |

**Temps de la main :** appuyer : la règle est écrite et tranchée ; la mission corrige un défaut de rattachement.

## Ma réponse d'abord

_Rempli par Pascal seul._

## L'Homme

- **Agents :** `testeur` d'abord (tests depuis l'ADR, la spec et cette fiche, **sans lire** `Controle.kt`,
  `SessionControle.kt`, `ArriveeControle.kt`, `EcranGuidage.kt`), puis `dev`.
- **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** l'ouverture sans position (087), le journal v2, l'export, la `.sursync` (088).

## La règle (ADR 0003, amendement du 2026-11-02, point 3 : l'ADR fait foi)

- Code lu présent dans **une seule** placette de la tournée : inchangé (15 m, ou rattaché par identité au-delà).
- Code lu présent dans **plusieurs** placettes (UG différentes) :
  - **une seule** de ces placettes à **15 m au plus** (borne incluse) de la position mesurée : c'est elle, sans question ;
  - **aucune** à 15 m au plus, ou **plusieurs** à 15 m au plus : l'agent **choisit l'UG** (« P01 : UG 12 ou UG 14 ? ») ;
    le contrôle est alors rattaché à la placette choisie (par identité si elle est à plus de 15 m).
- Sans code lu : inchangé (la plus proche à 15 m au plus, égalité départagée par le code).
- Le choix de l'UG ne se devine jamais : pas de « première trouvée ».

## Harnais

- **Testeur peut :** écrire des tests dans `core/src/test/` (règle du rattachement par l'API publique) et `app/src/test/`
  (l'écran d'arrivée propose le choix de l'UG, le choix ouvre la bonne placette) ; ajouter une fixture de tournée
  avec deux placettes de même code dans deux UG si besoin (dans `fixtures/`, nommée, sourcée « cas de test ») ; vus
  rouges sur `main`.
- **Dev peut :** modifier `core/.../placette/Controle.kt` (`Rattachement`), `Verdict` (nouveau cas « à choisir »),
  `SessionControle.ouvrir`, `ArriveeControle`, l'écran d'arrivée (`EcranGuidage` / contrôle) et les textes ; il ne
  réécrit pas les tests du testeur (s'il en juge un faux, il le dit dans la PR).
- **Ne doivent pas :** toucher à l'ouverture sans position, au format du journal, à l'export, à la `.sursync` ; toucher à
  `docs/` (si l'ADR ou la spec semblent faux, le dire) ; toucher à `main`.
- **Sources autorisées :** ADR 0003 (amendement du 2026-11-02), spec § 4.3 et § 6, fiche 085, sommieR (`R/registre6.R`
  l. 85 : code unique dans l'UG seulement).

## Valeur provisoire (chiffre, réglage)

Aucune nouvelle (15 m inchangé, borne incluse). Les textes de l'écran de choix sont proposés dans la PR ; Pascal les
tranche.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** 085 (fusionnée, #150, 66a18d6). La 087 part de celle-ci fusionnée.
- **Ordre de fusion :** 086 → 087 → 088.
- **PR en brouillon jusqu'à :** la fin du travail du `dev`.
- **CI rouge attendue :** les tests du `testeur`, entre son push et celui du `dev` (« rouge justifiée »).

## Tests

- Code unique : inchangé (tests existants verts).
- Deux homonymes, une seule à 15 m au plus (14,9 m et 15,0 m inclus) : rattachée sans question.
- Deux homonymes, aucune à 15 m au plus : choix de l'UG demandé ; le choix ouvre la placette choisie, par identité.
- Deux homonymes, les deux à 15 m au plus : choix de l'UG demandé.
- Jamais de « première trouvée » : l'ordre des placettes dans la tournée ne change pas le résultat.
- Placette en Somalie : un fix hors de France reste refusé avant tout choix.
- `.claude/test-command` au code 0.

## Terrain

- **Échantillon que Pascal vérifiera :** sur la release, une tournée avec deux « P01 » dans deux UG : l'écran de choix,
  capturé par l'orchestrateur.

## Qui veille, qui tranche

- **Relecteur :** `relecteur` (règle contre l'ADR, textes contre le code), puis Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Point de suivi :** au push du `testeur`, puis à la PR.
- **Qui tranche :** Pascal (textes, fusion).

## Comment saura-t-on que ça marche ?

Avec deux placettes de même code, Surculus ne rattache jamais en silence la mauvaise ; la capture de l'écran de choix le
montre.
